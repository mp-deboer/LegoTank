package tankpack;

// Anti-Concurrent thread
public class LineFollower extends StateMachine
{
	public enum Direction
	{
		Forward, Backward, Left, Right, RotateLeft, RotateRight, Stop
	}
	
	// Motor constants
	private final byte LEFTTRACK = (byte) 128;
	private final byte RIGHTTRACK = (byte) 192;
	private final int SPEED = 50;
	
	// Sensor constants
	private final int BLACK = 175;
	private final int LEFT = 0;
	private final int MIDDLE = 1;
	private final int RIGHT = 2;
	
	// Direction Defines
	private final int MAXLATESTS = 15;
	private final int CERTAINTY = 3;
	private Direction direction;
	private SensorPosition latest;
	private boolean latestWasForward;
	private int latestCounter;
	private int leftCounter;
	private int middleCounter;
	private int rightCounter;
	private int leftCounter2;
	private int rightCounter2;
	private int leftValue;
	private int middleValue;
	private int rightValue;
	
	// initialize composites
	public Sensor[] sensor = new Sensor[3];
	private HardwareDriver hwDriver;
	private String[] State =
	{
			"Waiting", "Follow"
	};
	private String[] Event =
	{
			"startFollow", "stopFollow", "stopFollowCheck"
	};
	private String[][] SensitivityTable =
	{
			// State1 and State2 in columns, Event1 and Event2 in rows
			// Next event / state in 'cells'
			{
					"Follow", null
			},
			{
					null, "stopFollowCheck"
			},
			{
					null, "Waiting"
			}
	};
	
	public LineFollower(CommunicationDriver cd, HardwareDriver hwDr)
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
		direction = Direction.RotateRight;
		latest = SensorPosition.Middle;
		latestWasForward = false;
		leftCounter = 0;
		middleCounter = 0;
		rightCounter = 0;
		leftCounter2 = 0;
		rightCounter2 = 0;
		leftValue = 0;
		middleValue = 0;
		rightValue = 0;
		latestCounter = 0;
		
		sensor[LEFT] = new Sensor(SensorPosition.Left);
		sensor[MIDDLE] = new Sensor(SensorPosition.Middle);
		sensor[RIGHT] = new Sensor(SensorPosition.Right);
		
		hwDriver = hwDr;
		
		processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
				getSensitives());
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			
			// State == Follow
			else if (currentState.equals(State[1]))
			{
				while (nextEvent == null)
				{
					leftValue = sensor[LEFT].getColorValue();
					middleValue = sensor[MIDDLE].getColorValue();
					rightValue = sensor[RIGHT].getColorValue();
					
					if (middleValue > BLACK)
					{
						middleCounter++;
						leftCounter = 0;
						rightCounter = 0;
						if (middleCounter > CERTAINTY)
						{
							latestCounter = 0;
							latestWasForward = true;
							this.direction = Direction.Forward;
							sleep(3);
							
							if (leftValue > BLACK)
							{
								leftCounter2++;
								rightCounter2 = 0;
								if (leftCounter2 > CERTAINTY)
								{
									latest = SensorPosition.Left;
								}
							}
							else if (rightValue > BLACK)
							{
								rightCounter2++;
								leftCounter2 = 0;
								if (rightCounter2 > CERTAINTY)
								{
									latest = SensorPosition.Right;
								}
							}
						}
					}
					else if (rightValue > BLACK)
					{
						rightCounter++;
						leftCounter = 0;
						middleCounter = 0;
						rightCounter2 = 0;
						leftCounter2 = 0;
						if (rightCounter > CERTAINTY)
						{
							latestCounter = 0;
							latestWasForward = false;
							this.direction = Direction.RotateRight;
							latest = SensorPosition.Right;
						}
					}
					else if (leftValue > BLACK)
					{
						leftCounter++;
						middleCounter = 0;
						rightCounter = 0;
						rightCounter2 = 0;
						leftCounter2 = 0;
						if (leftCounter > CERTAINTY)
						{
							latestCounter = 0;
							latestWasForward = false;
							this.direction = Direction.RotateLeft;
							latest = SensorPosition.Left;
						}
					}
					else if (latest != SensorPosition.Middle)
					{
						latestCounter++;
						leftCounter = 0;
						middleCounter = 0;
						rightCounter = 0;
						rightCounter2 = 0;
						leftCounter2 = 0;
						if (latest == SensorPosition.Right)
						{
							if (latestWasForward && latestCounter < MAXLATESTS)
							{
								this.direction = Direction.Right;
							}
							else
							{
								this.direction = Direction.RotateRight;
							}
						}
						else if (latest == SensorPosition.Left)
						{
							if (latestWasForward && latestCounter < MAXLATESTS)
							{
								this.direction = Direction.Left;
							}
							else
							{
								this.direction = Direction.RotateLeft;
							}
						}
					}
					else
					{
						this.direction = Direction.RotateRight;
					}
					
					// Instruct motors with the new direction
					instructMotors(SPEED);
					
					if (debug)
					{
						System.out.printf("%s%3d%-8s %s%3d%-8s %s%3d%-8s, %-15s\r", (leftValue > BLACK) ? "[" : " ",
								leftValue, (leftValue > BLACK) ? "]" : " ", (middleValue > BLACK) ? "[" : " ",
								middleValue, (middleValue > BLACK) ? "]" : " ", (rightValue > BLACK) ? "[" : " ",
								rightValue, (rightValue > BLACK) ? "]" : " ", direction);
					}
					sleep(11);
				}
				
				latest = SensorPosition.Middle;
				direction = Direction.RotateRight;
				
				// Instruct motors to stop
				instructMotors(0);
				
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
		
		// State == Waiting
		if (currentState.equals(State[0]))
		{
			// Event = startFollow
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
		
		// State == Follow
		else if (currentState.equals(State[1]))
		{
			// Event = stopFollow
			if (nextEvent.equals(Event[1]))
			{
				// fire next event stopFollowCheck
				fireNextEvent(Event[2]);
				
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
			System.out.println(this.getClass().getName() + ": Unknown State detected in executeEvent(): " + nextEvent);
			error = true;
		}
		
		if (!error && changeState)
		{
			currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
		}
		nextEvent = null;
	}
	
	private void instructMotors(int s)
	{
		if (direction == Direction.Forward)
		{
			hwDriver.sendByte(LEFTTRACK, s);
			hwDriver.sendByte(RIGHTTRACK, s);
		}
		else if (direction == Direction.Backward)
		{
			hwDriver.sendByte(LEFTTRACK, -s);
			hwDriver.sendByte(RIGHTTRACK, -s);
		}
		else if (direction == Direction.Left)
		{
			hwDriver.sendByte(LEFTTRACK, 0);
			hwDriver.sendByte(RIGHTTRACK, s);
		}
		else if (direction == Direction.Right)
		{
			hwDriver.sendByte(LEFTTRACK, s);
			hwDriver.sendByte(RIGHTTRACK, 0);
		}
		else if (direction == Direction.RotateLeft)
		{
			hwDriver.sendByte(LEFTTRACK, -s);
			hwDriver.sendByte(RIGHTTRACK, s);
		}
		else if (direction == Direction.RotateRight)
		{
			hwDriver.sendByte(LEFTTRACK, s);
			hwDriver.sendByte(RIGHTTRACK, -s);
		}
		else
		// direction == stop
		{
			hwDriver.sendByte(LEFTTRACK, 0);
			hwDriver.sendByte(RIGHTTRACK, 0);
		}
	}
	
	/* LineFollower class is done, now the composite-classes */
	// LineFollower composite
	public class Sensor
	{
		// attributes
		@SuppressWarnings("unused")
		private int color;
		private SensorPosition position;
		
		// constructor
		public Sensor(SensorPosition p)
		{
			this.position = p;
		}
		
		// operations
		public int getColorValue()
		{
			int returnValue = hwDriver.readSensor(this.position);
			
			if (returnValue < 0)
			{
				returnValue += 256;
			}
			
			return returnValue;
		}
	}
	
	/* Done with LineFollowers composites */
}