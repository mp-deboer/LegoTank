package tankpack;

import java.lang.reflect.Constructor;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class PsController extends StateMachine
{
	// Own variables
	private Controller PsController;
	private Controller[] controllers;
	private Component[] components;
	private boolean isPs5;
	
	private String[] State =
	{
			"Disconnected", "Connected"
	};
	private String[] Event =
	{
			"foundController", "lostController"
	};
	private String[][] SensitivityTable =
	{
			// State1 and State2 in columns, Event1 and Event2 in rows
			// Next state in 'cells'
			{
					"Connected", null
			},
			{
					null, "Disconnected"
			}
	};
	
	public PsController(CommunicationDriver cd)
	{
		// StateMachine variables
		super.State = this.State;
		super.Event = this.Event;
		super.SensitivityTable = this.SensitivityTable;
		
		debug = false;
		this.cd = cd;
		currentState = State[0];
		nextEvent = null;
		
		// Declare controller variables:
		controllers = null;
		PsController = null;
		
		processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
				getSensitives());
	}
	
	public Component[] getComps()
	{
		return components;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// State == Disconnected
			if (currentState.equals(State[0]))
			{
				if (controllerAvailable())
				{
					components = PsController.getComponents();
					fireEvent("foundController");
					waitForEvent();
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				else
				{
					sleep(8000);
				}
			}
			// State == Connected
			else if (currentState.equals(State[1]))
			{
				while (PsController.poll())
				{
					sleep(100);
				}
				// When polling fails
				fireEvent("lostController");
				components = null;
				PsController = null;
				
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			else
			{
				// Unknown state error handling here
				System.out.println(this.getClass().getName() + ": Unknown State detected in run(): " + currentState);
				sleep(1);
			}
			
			Thread.yield();
		}
	}
	
	@Override
	public void giveEvent(String event)
	{
		nextEvent = event;
	}
	
	@Override
	protected void executeEvent()
	{
		boolean error = false;
		boolean changeState = false;
		
		// State == Disconnected
		if (currentState.equals(State[0]))
		{
			// Event = foundController
			if (nextEvent.equals(Event[0]))
			{
				error = false;
				changeState = true;
			}
			else
			{
				// Event error handling here
				System.out.println(
						this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
				error = true;
			}
		}
		
		// State == Connected
		else if (currentState.equals(State[1]))
		{
			// Event = lostController
			if (nextEvent.equals(Event[1]))
			{
				error = false;
				changeState = true;
			}
			else
			{
				// Event error handling here
				System.out.println(
						this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
				error = true;
			}
		}
		else
		{
			// Unknown state error handling here
			System.out
					.println(this.getClass().getName() + ": Unknown State detected in executeEvent(): " + currentState);
			error = true;
		}
		
		if (!error && changeState)
		{
			currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
		}
		nextEvent = null;
	}
	
	private boolean controllerAvailable()
	{
		Controller tmpController = null;
		
		try
		{
			controllers = createDefaultEnvironment().getControllers();
		}
		catch (ReflectiveOperationException e)
		{
			e.printStackTrace();
		}
		
		if (debug) System.out.println("controllers: " + controllers.length);
		
		for (int i = 0; i < controllers.length && tmpController == null; i++)
		{
			if (debug) System.out.println("controller type: " + controllers[i].getType());
			if (debug) System.out.println("controller name: " + controllers[i].getName());
			
			if (controllers[i].getType() == Controller.Type.STICK
					&& controllers[i].getName().contains("PLAYSTATION(R)3 Controller"))
			{
				tmpController = controllers[i];
				isPs5 = false;
			}
			// 2025-12-13 MdB: Added PS5 controller support
			else if (controllers[i].getType() == Controller.Type.GAMEPAD && controllers[i].getName()
					.contains("Sony Interactive Entertainment DualSense Wireless Controller"))
			{
				tmpController = controllers[i];
				isPs5 = true;
			}
		}
		
		if (tmpController == null)
		{
			PsController = null;
			return false;
		}
		else
		{
			if (PsController == null)
			{
				PsController = tmpController;
			}
			return true;
		}
	}
	
	public boolean getIsPs5()
	{
		return isPs5;
	}
	
	private static ControllerEnvironment createDefaultEnvironment() throws ReflectiveOperationException
	{
		// Find constructor (class is package private, so we can't access it directly)
		@SuppressWarnings("unchecked")
		Constructor<ControllerEnvironment> constructor = (Constructor<ControllerEnvironment>) Class
				.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0];
		
		// Constructor is package private, so we have to deactivate access control checks
		constructor.setAccessible(true);
		
		// Create object with default constructor
		return constructor.newInstance();
	}
}
