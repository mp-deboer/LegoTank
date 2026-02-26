package tankpack;

public class Led2 extends StateMachine
{
	// Own variables
	int counter;
	
	private String[] State =
	{
			"Led2Off", "Led2On"
	};
	private String[] Event =
	{
			"leftStickPercentage",
			"leftStickDegree",
			"rightStickUsed",
			"circlePressed",
			"crossPressed",
			"downPressed",
			"l1Pressed",
			"l3Pressed",
			"leftPressed",
			"ps3Pressed",
			"r1Pressed",
			"r3Pressed",
			"rightPressed",
			"selectPressed",
			"squarePressed",
			"startPressed",
			"trianglePressed",
			"upPressed",
			"leftStickNoPercentage",
			"leftStickNoDegree",
			"rightStickNotUsed",
			"circleReleased",
			"crossReleased",
			"downReleased",
			"l1Released",
			"l3Released",
			"leftReleased",
			"ps3Released",
			"r1Released",
			"r3Released",
			"rightReleased",
			"selectReleased",
			"squareReleased",
			"startReleased",
			"triangleReleased",
			"upReleased",
			"setLED2on",
			"setLED2off"
	};
	private String[][] SensitivityTable =
	{
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"setLED2", "Led2On"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2Off", "setLED2"
			},
			{
					"Led2On", "Led2Off"
			},
			{
					"Led2On", "Led2Off"
			}
	};
	
	public Led2(CommunicationDriver cd)
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
		counter = 0;
		
		processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
				getSensitives());
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// State == Led2Off
			if (currentState.equals(State[0]))
			{
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			
			// State == Led2On
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
	public void giveEvent(String event, boolean data)
	{
		// we don't need to know whether the led is on or not
		nextEvent = event;
	}
	
	@Override
	public void giveEvent(String event, int data)
	{
		// we don't need any integer
		nextEvent = event;
	}
	
	@Override
	protected void executeEvent()
	{
		boolean error = false;
		boolean changeState = false;
		
		// State == Led2Off
		if (currentState.equals(State[0]))
		{
			switch (nextEvent)
			{
				case "circlePressed":
				case "crossPressed":
				case "downPressed":
				case "l1Pressed":
				case "l3Pressed":
				case "leftPressed":
				case "ps3Pressed":
				case "r1Pressed":
				case "r3Pressed":
				case "rightPressed":
				case "selectPressed":
				case "squarePressed":
				case "startPressed":
				case "trianglePressed":
				case "upPressed":
				case "leftStickPercentage":
				case "leftStickDegree":
				case "rightStickUsed":
					fireNextEvent("setLED2on");
					counter = 1;
					error = false;
					changeState = true;
				break;
			
				case "leftStickNoPercentage":
				case "leftStickNoDegree":
				case "rightStickNotUsed":
				case "circleReleased":
				case "crossReleased":
				case "downReleased":
				case "l1Released":
				case "l3Released":
				case "leftReleased":
				case "ps3Released":
				case "r1Released":
				case "r3Released":
				case "rightReleased":
				case "selectReleased":
				case "squareReleased":
				case "startReleased":
				case "triangleReleased":
				case "upReleased":
					error = false;
					changeState = false;
				break;
			
				default:
					// Event error handling here
					System.out.println(
							this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
					error = true;
				break;
			}
		}
		
		// State == Led2On
		else if (currentState.equals(State[1]))
		{
			switch (nextEvent)
			{
				case "circlePressed":
				case "crossPressed":
				case "downPressed":
				case "l1Pressed":
				case "l3Pressed":
				case "leftPressed":
				case "ps3Pressed":
				case "r1Pressed":
				case "r3Pressed":
				case "rightPressed":
				case "selectPressed":
				case "squarePressed":
				case "startPressed":
				case "trianglePressed":
				case "upPressed":
					error = false;
					changeState = false;
					counter++;
				break;
			
				// as these events can happen multiple times, don't increase counter
				case "leftStickPercentage":
				case "leftStickDegree":
				case "rightStickUsed":
					error = false;
					changeState = false;
				break;
			
				case "leftStickNoPercentage":
				case "leftStickNoDegree":
				case "rightStickNotUsed":
				case "circleReleased":
				case "crossReleased":
				case "downReleased":
				case "l1Released":
				case "l3Released":
				case "leftReleased":
				case "ps3Released":
				case "r1Released":
				case "r3Released":
				case "rightReleased":
				case "selectReleased":
				case "squareReleased":
				case "startReleased":
				case "triangleReleased":
				case "upReleased":
					error = false;
					if (counter > 1)
					{
						changeState = false;
						counter--;
					}
					else
					// counter <= 1
					{
						fireNextEvent("setLED2off");
						counter = 0;
						changeState = true;
					}
				break;
			
				default:
					// Event error handling here
					System.out.println(
							this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
					error = true;
				break;
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
			if (debug) System.out.println("Debug Led2: currentState executeEvent: " + currentState);
		}
		nextEvent = null;
	}
}
