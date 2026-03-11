package tankpack;

import java.lang.reflect.Constructor;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import tankpack.enums.PsComponent;

public class Driver_PsController
{
	private final boolean debug = false;
	
	// Own variables
	private Controller[] controllers;
	private Controller PsController = null;
	private Component[] components = null;
	private boolean isPs5;
	
	public Driver_PsController()
	{
	}
	
	public void shutdown()
	{
		controllers = null;
		PsController = null;
		components = null;
		
		if (debug)
			System.out.println("PsController shutdown complete.");
	}
	
	public synchronized boolean checkController()
	{
		Controller tmpController = null;
		
		try
		{
			// Redirect jInput info log (printed to stdErr for some reason) to void if debug is disabled
			if (!debug)
			{
				// Temporarily redirect error log, save original setting
				PrintStream originalErr = System.err;
				System.setErr(new PrintStream(new OutputStream()
				{
					@Override
					public void write(int b) throws IOException
					{
						// Do nothing
					}
				}));
				controllers = createDefaultEnvironment().getControllers();
				
				// Set error log setting back to original
				System.setErr(originalErr);
			}
			else // debug is enabled, do not redirect log
				controllers = createDefaultEnvironment().getControllers();
			
		}
		catch (ReflectiveOperationException e)
		{
			e.printStackTrace();
		}
		
		if (debug)
			System.out.println("controllers found: " + controllers.length);
		
		for (int i = 0; i < controllers.length && tmpController == null; i++)
		{
			if (debug)
				System.out.println("controller type: " + controllers[i].getType());
			if (debug)
				System.out.println("controller name: " + controllers[i].getName());
			
			if (controllers[i].getType() == Controller.Type.STICK
					&& controllers[i].getName().contains("PLAYSTATION(R)3 Controller"))
			{
				tmpController = controllers[i];
				isPs5 = false;
			}
			// 2025-12-13 MdB: Added PS5 controller support
			else if (controllers[i].getType() == Controller.Type.GAMEPAD
					&& (controllers[i].getName().equals("Sony Interactive Entertainment DualSense Wireless Controller")
							|| controllers[i].getName().equals("DualSense Wireless Controller")))
			{
				tmpController = controllers[i];
				isPs5 = true;
			}
		}
		
		if (tmpController != null)
		{
			PsController = tmpController;
			components = PsController.getComponents();
			controllers = null; // delete reference to other controllers
			return true;
		}
		else
		{
			PsController = null;
			components = null;
			return false;
		}
	}
	
	public synchronized boolean pollController()
	{
		if (PsController != null)
			return PsController.poll();
		else
			return false;
	}
	
	public boolean getIsPs5()
	{
		return isPs5;
	}
	
	public boolean isPressed(PsComponent c)
	{
		if (components != null && c != null)
		{
			float polledData = components[c.getIndex(isPs5)].getPollData();
			
			switch (c)
			{
				case Up_Arrow:
					// Up_Arrow only gives 0.25f, up and left gives 0.125f, up and right gives 0.375f
					return ((polledData == 0.125f) || (polledData == 0.25f) || (polledData == 0.375f));
				case Right_Arrow:
					// Right_Arrow only gives 0.5f, right and up gives 0.375f, right and down gives 0.625f
					return ((polledData == 0.375f) || (polledData == 0.5f) || (polledData == 0.625f));
				case Down_Arrow:
					// Down_Arrow only gives 0.75f, down and right gives 0.625f, down and left gives 0.875f
					return ((polledData == 0.625f) || (polledData == 0.75f) || (polledData == 0.875f));
				case Left_Arrow:
					// Left_Arrow only gives 1.0f, left and down gives 0.875f, left and up gives 0.125f
					return ((polledData == 0.875f) || (polledData == 1.0f) || (polledData == 0.125f));
				default:
					return ((polledData == 1.0f));
			}
		}
		else // components == null
			return false;
	}

	// Joystick ranges from -1..1, where values other than 0 means it is used
	public boolean isUsed(PsComponent c)
	{
		if (components != null && c != null)
		{
			float value = components[c.getIndex(isPs5)].getPollData();
			
			// Apply noise filter, requiring a minimum value before considered "used"
			return (value > 0.06f || value < -0.06f);
		}
		else
			return false;
	}
	
	// Stick ranges from -1..1, where values higher than -1 means it is used
	public boolean stickIsUsed(PsComponent c)
	{
		if (components != null && c != null)
		{
			float value = components[c.getIndex(isPs5)].getPollData();
			
			// Apply noise filter, requiring a minimum value before considered "used"
			return (value > (-1f + 0.06f));
		}
		else
			return false;
	}
	
	public float getValue(PsComponent c)
	{
		if (components != null && c != null)
		{
			float value = components[c.getIndex(isPs5)].getPollData();
			if (value > 0.06f || value < -0.06f)
				return value;
			else
				return 0;
		}
		else
			return 0;
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
