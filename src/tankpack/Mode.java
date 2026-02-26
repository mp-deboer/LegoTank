package tankpack;

public class Mode extends StateMachine
{
	// Own variables
	
	private String[] State =
	{
			"Off", "Control", "Follow"
	};
	private String[] Event =
	{
			"foundController",
			"lostContoller",
			"trackReset",
			"turretReset",
			"trackResetCheck",
			"turretResetCheck",
			"crossPressed",
			"startFollow",
			"stopFollow",
			"stopFollowCheck",
			"getLeftXvalue",
			"getLeftYvalue",
			"getRightXvalue"
	};
	private String[][] SensitivityTable =
	{
			// State1 and State2 in columns, Event1 and Event2 in rows
			// Next event / state in 'cells'
			{
					"Control", null, null
			},
			{
					null, "trackReset", "trackReset"
			},
			{
					null, "turretReset", "turretReset"
			},
			{
					null, "trackResetCheck | startFollow", "trackResetCheck"
			},
			{
					null, "turretResetCheck", "turretResetCheck"
			},
			{
					null, "Off", "Off"
			},
			{
					null, "trackReset", "stopFollow"
			},
			{
					null, "Follow", null
			},
			{
					null, null, "stopFollowCheck"
			},
			{
					null, null, "Control"
			},
			{
					null, "Control", null
			},
			{
					null, "Control", null
			},
			{
					null, "Control", "Follow"
			}
	};
	
	public Mode(CommunicationDriver cd)
	{
		// StateMachine variables
		debug = false;
		
		super.State = this.State;
		super.Event = this.Event;
		super.SensitivityTable = this.SensitivityTable;
		
		this.cd = cd;
		currentState = State[0];
		nextEvent = null;
		
		processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
				getSensitives());
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// State == Off
			if (currentState.equals(State[0]))
			{
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			
			// State == Control
			else if (currentState.equals(State[1]))
			{
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			
			// State == Follow
			else if (currentState.equals(State[2]))
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
		nextEvent = event;
	}
	
	@Override
	public void giveEvent(String event, int data)
	{
		// we don't need the integer
		nextEvent = event;
	}
	
	@Override
	protected void executeEvent()
	{
		boolean error = false;
		boolean changeState = false;
		
		// State == Off
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
		
		// State == Control
		else if (currentState.equals(State[1]))
		{
			// Event = lostController
			if (nextEvent.equals(Event[1]))
			{
				// fire next event trackReset
				fireNextEvent(Event[2]);
				// fire next event turretReset
				fireNextEvent(Event[3]);
				
				// wait for event trackResetCheck
				waitForNextEvent(Event[4]);
				// wait for event turretResetCheck
				waitForNextEvent(Event[5]);
				
				// handle turretResetCheck
				error = false;
				changeState = true;
			}
			
			// Event = crossPressed
			else if (nextEvent.equals(Event[6]))
			{
				// fire next event trackReset
				fireNextEvent(Event[2]);
				
				// wait for event trackResetCheck
				waitForNextEvent(Event[4]);
				
				// fire next event startFollow
				fireNextEvent("setLED1on");
				fireNextEvent(Event[7]);
				
				// handle startFollow
				error = false;
				changeState = true;
			}
			
			// Event = getLeftXvalue || getLeftYvalue || getRightXvalue
			else if (nextEvent.equals(Event[10]) || nextEvent.equals(Event[11]) || nextEvent.equals(Event[12]))
			{
				error = false;
				changeState = false;
			}
			
			else
			{
				// Event error handling here
				System.out.println(
						this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
				error = true;
			}
		}
		
		// State == Follow
		else if (currentState.equals(State[2]))
		{
			// Event = lostController
			if (nextEvent.equals(Event[1]))
			{
				// fire next event trackReset
				fireNextEvent(Event[2]);
				// fire next event turretReset
				fireNextEvent(Event[3]);
				
				// wait for event trackResetCheck
				waitForNextEvent(Event[4]);
				// wait for event turretResetCheck
				waitForNextEvent(Event[5]);
				
				// handle turretResetCheck
				error = false;
				changeState = true;
			}
			
			// Event = crossPressed
			else if (nextEvent.equals(Event[6]))
			{
				// fire next event stopFollow
				fireNextEvent("setLED1off");
				fireNextEvent(Event[8]);
				
				// wait for event stopFollowCheck
				waitForNextEvent(Event[9]);
				
				error = false;
				changeState = true;
			}
			
			// Event = getRightXvalue
			else if (nextEvent.equals(Event[12]))
			{
				error = false;
				changeState = false;
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
