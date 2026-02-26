package tankpack;

public class Class extends StateMachine
{
	// Own variables
	
	private String[] State =
	{
			"State1", "State2"
	};
	private String[] Event =
	{
			"Event1", "Event2"
	};
	private String[][] SensitivityTable =
	{
			// State1 and State2 in columns, Event1 and Event2 in rows
			// Next event / state in 'cells'
			{
					"State2", "State2"
			},
			{
					"State1", "State1"
			}
	};
	
	public Class(CommunicationDriver cd)
	{
		// StateMachine variables
		debug = false;
		
		super.State = this.State;
		super.Event = this.Event;
		super.SensitivityTable = this.SensitivityTable;
		
		this.cd = cd;
		currentState = State[0];
		nextEvent = null;
		
		// Declare own variables:
		
		processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
				getSensitives());
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// State == State1
			if (currentState.equals(State[0]))
			{
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			
			// State == State2
			else if (currentState.equals(State[1]))
			{
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
	}
	
	@Override
	public void giveEvent(String event, boolean data)
	{
	}
	
	@Override
	public void giveEvent(String event, int data)
	{
	}
	
	@Override
	public void giveEvent(String event, Controller data)
	{
	}
	
	@Override
	protected void executeEvent()
	{
		boolean error = false;
		boolean changeState = false;
		
		// State == State1
		if (currentState.equals(State[0]))
		{
			// Event = Event1
			if (nextEvent.equals(Event[0]))
			{
				error = false;
				changeState = true;
			}
			
			// Event = Event2
			else if (nextEvent.equals(Event[1]))
			{
				error = false;
				changeState = false;
			}
			
			else
			{
				// Event error handling here
				System.out.println(this.getClass().getName() + ": Unknown event detected in executeEvent(): "
						+ nextEvent);
				error = true;
			}
		}
		
		// State == State2
		else if (currentState.equals(State[1]))
		{
			// Event = Event1
			if (nextEvent.equals(Event[0]))
			{
				error = false;
				changeState = false;
			}
			
			// Event = Event2
			else if (nextEvent.equals(Event[1]))
			{
				error = false;
				changeState = true;
			}
			
			else
			{
				// Event error handling here
				System.out.println(this.getClass().getName() + ": Unknown event detected in executeEvent(): "
						+ nextEvent);
				error = true;
			}
		}
		else
		{
			// Unknown state error handling here
			System.out.println(this.getClass().getName() + ": Unknown State detected in executeEvent(): " + nextEvent);
			error = true;
		}
		
		if (!error && changeState)
		{
			currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
		}
		nextEvent = null;
	}
}
