package tankpack;

public class Motors
{
	public Motors(CommunicationDriver cd)
	{
		Motor leftTrack = new Motor(cd, "leftTrack");
		Motor rightTrack = new Motor(cd, "rightTrack");
		Motor turret = new Motor(cd, "turret");
		
		Thread thrLeft = new Thread(leftTrack);
		Thread thrRight = new Thread(rightTrack);
		Thread thrTurret = new Thread(turret);
		
		thrLeft.start();
		thrRight.start();
		thrTurret.start();
	}
	
	public class Motor extends StateMachine
	{
		// Own variables
		private final int ACCELERATEDELAY = 50; // in milliseconds
		private final int DECELERATEDELAY = 20; // in milliseconds
		private final int MAXSPEEDSTEP = 10; // in percent
		private String targetState;
		private int currentSpeed;
		private int targetSpeed;
		
		// State-Machine variable
		private int data;
		
		private String[] State =
		{
				"Halt", "GoingForward", "GoingBackward"
		};
		private String[] Event =
		{
				"Stop",
				"Forward",
				"Backward",
				"Accelerate",
				"FinalAccelerate",
				"Decelerate",
				"FinalDecelerate",
				"setDriverWanted",
				"setDriverSpeed"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Halt", "Halt", "Halt"
				},
				{
						"GoingForward", "GoingForward", "GoingForward"
				},
				{
						"GoingBackward", "GoingBackward", "GoingBackward"
				},
				{
						null, "GoingForward", "GoingBackward"
				},
				{
						null, "GoingForward", "GoingBackward"
				},
				{
						null, "GoingForward", "GoingBackward"
				},
				{
						null, "GoingForward", "GoingBackward"
				},
				{
						"Halt", "GoingForward", "GoingBackward"
				},
				{
						"Halt", "GoingForward", "GoingBackward"
				}
		};
		
		public Motor(CommunicationDriver cd, String type)
		{
			String typeFirstCapital = type.substring(0, 1).toUpperCase() + type.substring(1);
			
			// Add type in front of events
			for (int i = 0; i < (this.Event.length - 2); i++)
			{
				// turret uses Right and Left instead of Forward and Backward
				if (i == 1 && type.equals("turret"))
				{
					this.Event[i] = "turretRight";
				}
				else if (i == 2 && type.equals("turret"))
				{
					this.Event[i] = "turretLeft";
				}
				// for the other motors just add type in front of the event
				else
				{
					this.Event[i] = type + this.Event[i];
				}
			}
			this.Event[this.Event.length - 2] = "set" + typeFirstCapital + "Wanted";
			this.Event[this.Event.length - 1] = "set" + typeFirstCapital + "Speed";
			
			// StateMachine variables
			debug = false;
			
			super.State = this.State;
			super.Event = this.Event;
			super.SensitivityTable = this.SensitivityTable;
			
			this.cd = cd;
			currentState = State[0];
			nextEvent = null;
			data = 0;
			
			// Declare own variables:
			targetState = State[0];
			currentSpeed = 0;
			targetSpeed = 0;
			
			processID = cd.registerNewProcess(this, ("Motor_" + typeFirstCapital), Event, State,
					getStateNr(currentState), getSensitives());
		}
		
		@Override
		public void run()
		{
			while (true)
			{
				// State == Halt
				if (currentState.equals(State[0]) && (targetState == null || targetState.equals(currentState)))
				{
					waitForEvent();
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == GoingForward || State == GoingBackward || currentState != targetState
				else if (currentState.equals(State[1]) || currentState.equals(State[2])
						|| !targetState.equals(currentState))
				{
					if (checkForEvent())
					{
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					else
					{
						if (currentState.equals(targetState))
						{
							if (currentSpeed != targetSpeed)
							{
								if (currentSpeed > targetSpeed)
								{
									if (currentSpeed - targetSpeed > MAXSPEEDSTEP)
									{
										// attempt fire event 'Decelerate'
										if (attemptFireEvent(Event[5]))
										{
											executeEvent();
											cd.updateProcess(processID, getStateNr(currentState), getSensitives());
										}
									}
									// currentSpeed - targetSpeed <= MAXSPEEDSTEP
									else
									{
										// attempt fire event 'FinalDecelerate'
										if (attemptFireEvent(Event[6]))
										{
											executeEvent();
											cd.updateProcess(processID, getStateNr(currentState), getSensitives());
										}
									}
								}
								// currentSpeed < targetSpeed
								else
								{
									if (targetSpeed - currentSpeed > MAXSPEEDSTEP)
									{
										// attempt fire event 'Accelerate'
										if (attemptFireEvent(Event[3]))
										{
											executeEvent();
											cd.updateProcess(processID, getStateNr(currentState), getSensitives());
										}
									}
									// targetSpeed - currentSpeed <= MAXSPEEDSTEP
									else
									{
										// attempt fire event 'FinalAccelerate'
										if (attemptFireEvent(Event[4]))
										{
											executeEvent();
											cd.updateProcess(processID, getStateNr(currentState), getSensitives());
										}
									}
								}
							}
							// currentSpeed == targetSpeed
							else
							{
								sleep(100);
							}
						}
						// !currentDirection.equals(targetDirection)
						else
						{
							if (currentSpeed == 0)
							{
								currentState = targetState;
								cd.updateProcess(processID, getStateNr(currentState), getSensitives());
							}
							else if (currentSpeed > MAXSPEEDSTEP)
							{
								// attempt fire event 'Decelerate'
								if (attemptFireEvent(Event[5]))
								{
									executeEvent();
									cd.updateProcess(processID, getStateNr(currentState), getSensitives());
								}
							}
							// currentSpeed > 0 && currentSpeed <= MAXSPEEDSTEP
							else
							{
								// attempt fire event 'FinalDecelerate'
								if (attemptFireEvent(Event[6]))
								{
									executeEvent();
									cd.updateProcess(processID, getStateNr(currentState), getSensitives());
								}
							}
						}
					}
				}
				
				else
				{
					// Unknown state error handling here
					System.out
							.println(this.getClass().getName() + ": Unknown State detected in run(): " + currentState);
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
			nextEvent = event;
			this.data = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Halt
			if (currentState.equals(State[0]))
			{
				// Event = Stop
				if (nextEvent.equals(Event[0]))
				{
					targetState = State[0];
					
					error = false;
					changeState = true;
				}
				
				// Event = Forward
				else if (nextEvent.equals(Event[1]))
				{
					error = false;
					changeState = true;
					targetState = State[1];
				}
				
				// Event = Backward
				else if (nextEvent.equals(Event[2]))
				{
					error = false;
					changeState = true;
					targetState = State[2];
				}
				
				// Event = set...Wanted
				else if (nextEvent.equals(Event[7]))
				{
					targetSpeed = data;
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
			
			// State == GoingForward
			else if (currentState.equals(State[1]))
			{
				// Event = Stop
				if (nextEvent.equals(Event[0]))
				{
					if (currentSpeed == 0)
					{
						changeState = true;
					}
					else
					{
						changeState = false;
					}
					targetState = State[0];
					
					error = false;
				}
				
				// Event = Forward
				else if (nextEvent.equals(Event[1]))
				{
					targetState = State[1];
					changeState = false;
					error = false;
				}
				
				// Event = Backward
				else if (nextEvent.equals(Event[2]))
				{
					if (currentSpeed == 0)
					{
						changeState = true;
					}
					else
					{
						changeState = false;
					}
					targetState = State[2];
					
					error = false;
				}
				
				// Event = Accelerate
				else if (nextEvent.equals(Event[3]))
				{
					currentSpeed += MAXSPEEDSTEP;
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], currentSpeed);
					if (debug) System.out.println("Moving right with speed: " + currentSpeed);
					sleep(ACCELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = FinalAccelerate
				else if (nextEvent.equals(Event[4]))
				{
					currentSpeed = targetSpeed;
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], currentSpeed);
					if (debug) System.out.println("Moving right with speed: " + currentSpeed);
					sleep(ACCELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = Decelerate
				else if (nextEvent.equals(Event[5]))
				{
					currentSpeed -= MAXSPEEDSTEP;
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], currentSpeed);
					if (debug) System.out.println("Moving right with speed: " + currentSpeed);
					sleep(DECELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = FinalDecelerate
				else if (nextEvent.equals(Event[6]))
				{
					if (currentState.equals(targetState))
					{
						currentSpeed = targetSpeed;
					}
					else
					{
						currentSpeed = 0;
					}
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], currentSpeed);
					if (debug) System.out.println("Moving right with speed: " + currentSpeed);
					sleep(DECELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = set...Wanted
				else if (nextEvent.equals(Event[7]))
				{
					targetSpeed = data;
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
			
			// State == GoingBackward
			else if (currentState.equals(State[2]))
			{
				// Event = Stop
				if (nextEvent.equals(Event[0]))
				{
					if (currentSpeed == 0)
					{
						changeState = true;
					}
					else
					{
						changeState = false;
					}
					targetState = State[0];
					error = false;
				}
				
				// Event = Forward
				else if (nextEvent.equals(Event[1]))
				{
					if (currentSpeed == 0)
					{
						changeState = true;
					}
					else
					{
						changeState = false;
					}
					
					targetState = State[1];
					error = false;
				}
				
				// Event = Backward
				else if (nextEvent.equals(Event[2]))
				{
					targetState = State[2];
					changeState = false;
					error = false;
				}
				
				// Event = Accelerate
				else if (nextEvent.equals(Event[3]))
				{
					currentSpeed += MAXSPEEDSTEP;
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], -currentSpeed);
					if (debug) System.out.println("Moving left with speed: " + currentSpeed);
					sleep(ACCELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = FinalAccelerate
				else if (nextEvent.equals(Event[4]))
				{
					currentSpeed = targetSpeed;
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], -currentSpeed);
					if (debug) System.out.println("Moving left with speed: " + currentSpeed);
					sleep(ACCELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = Decelerate
				else if (nextEvent.equals(Event[5]))
				{
					currentSpeed -= MAXSPEEDSTEP;
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], -currentSpeed);
					if (debug) System.out.println("Moving left with speed: " + currentSpeed);
					sleep(DECELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = FinalDecelerate
				else if (nextEvent.equals(Event[6]))
				{
					if (currentState.equals(targetState))
					{
						currentSpeed = targetSpeed;
					}
					else
					{
						currentSpeed = 0;
					}
					
					// Fire event set...Speed[currentSpeed]
					fireNextEvent(Event[8], -currentSpeed);
					if (debug) System.out.println("Moving left with speed: " + currentSpeed);
					sleep(DECELERATEDELAY); // wait for motor to get to speed
					error = false;
					changeState = false;
				}
				
				// Event = set...Wanted
				else if (nextEvent.equals(Event[7]))
				{
					targetSpeed = data;
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
				System.out.println(
						this.getClass().getName() + ": Unknown State detected in executeEvent(): " + currentState);
				error = true;
			}
			
			if (!error && changeState)
			{
				currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
			}
			nextEvent = null;
		}
	}
}