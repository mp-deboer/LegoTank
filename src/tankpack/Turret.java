package tankpack;

public class Turret
{
	public Turret(CommunicationDriver cd, PsController psController)
	{
		Right_Stick_X stick = new Right_Stick_X(cd, psController);
		Right_Stick_Percentage rsPerc = new Right_Stick_Percentage(cd);
		Right_Stick_Direction rsDir = new Right_Stick_Direction(cd);
		Turret_Speed tS = new Turret_Speed(cd);
		
		Thread thrStick = new Thread(stick);
		Thread thrRsPerc = new Thread(rsPerc);
		Thread thrRsDir = new Thread(rsDir);
		Thread thrTS = new Thread(tS);
		
		thrStick.start();
		thrRsPerc.start();
		thrRsDir.start();
		thrTS.start();
	}
	
	public class Right_Stick_X extends StateMachine
	{
		// Own variables
		private int newValue;
		private int currentValue;
		private PsComponent stick;
		protected PsController psC;
		
		private String[] State =
		{
				"Right_Stick_X", "Decide"
		};
		private String[] Event =
		{
				"getRightXvalue", "setRightXvalue", "rightXdoNothing"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Decide", null
				},
				{
						null, "Right_Stick_X"
				},
				{
						null, "Right_Stick_X"
				}
		};
		
		public Right_Stick_X(CommunicationDriver cd, PsController psController)
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
			newValue = 0;
			currentValue = 0;
			this.stick = PsComponent.Rightstick_X;
			this.psC = psController;
			
			processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
					getSensitives());
		}
		
		@Override
		public void run()
		{
			while (true)
			{
				// State == Right_Stick_X
				if (currentState.equals(State[0]))
				{
					sleep(100);
					
					// Attempt to fire getRightXvalue
					if (attemptFireEvent(Event[0]))
					{
						// handle getRightXvalue
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
				}
				
				// State == Decide
				else if (currentState.equals(State[1]))
				{
					if (currentValue != newValue)
					{
						// Fire event setRightXvalue
						fireEvent(Event[1], newValue);
						waitForEvent();
						executeEvent();
					}
					else if (currentValue == newValue)
					{
						// Fire event rightXdoNothing
						fireEvent(Event[2]);
						waitForEvent();
						executeEvent();
					}
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
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
			currentValue = data;
			nextEvent = event;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Right_Stick_X
			if (currentState.equals(State[0]))
			{
				// Event = getRightXvalue
				if (nextEvent.equals(Event[0]))
				{
					newValue = getValue(stick);
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
			
			// State == Decide
			else if (currentState.equals(State[1]))
			{
				// Event = setRightXvalue
				if (nextEvent.equals(Event[1]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = rightXdoNothing
				else if (nextEvent.equals(Event[2]))
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
		
		protected int getValue(PsComponent c)
		{
			try
			{
				// round down from 0.0 - 0.5 and round up from 0.5 - 1.0
				double tmpValue = psC.getComps()[c.getIndex()].getPollData();
				
				if (tmpValue == 0.0) return 0;
				else if (tmpValue > 0.0) return ((int) (psC.getComps()[c.getIndex()].getPollData() * 100 + 0.5));
				// tmpValue < 0.0
				else return ((int) (psC.getComps()[c.getIndex()].getPollData() * 100 - 0.5));
			}
			catch (NullPointerException e)
			{
				System.out.println(this.getClass().getName() + ": Caught a NullPointerException in getValue()");
				
				return 0;
			}
		}
	}
	
	public class Right_Stick_Percentage extends StateMachine
	{
		// Own variables
		private int percentage;
		
		private String[] State =
		{
				"Waiting", "Decide"
		};
		private String[] Event =
		{
				"setRightXvalue",
				"rightStickNotUsed",
				"rightStickUsed",
				"turretCheck",
				"turretSpeedCheck",
				"turretReset",
				"turretResetCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Decide", null
				},
				{
						"turretCheck", "turretCheck"
				},
				{
						null, "turretCheck"
				},
				{
						"turretSpeedCheck", "turretSpeedCheck"
				},
				{
						"turretResetCheck", "Waiting"
				},
				{
						"rightStickNotUsed", null
				},
				{
						"Waiting", null
				}
		};
		
		public Right_Stick_Percentage(CommunicationDriver cd)
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
			percentage = 0;
			
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
				
				// State == Decide
				else if (currentState.equals(State[1]))
				{
					if (percentage == 0)
					{
						// Fire event 'rightStickNotUsed'
						fireNextEvent(Event[1]);
						// Wait for follow-up event 'turretCheck'
						waitForNextEvent(Event[3]);
						// Wait for follow-up event 'turretSpeedCheck'
						waitForNextEvent(Event[4]);
						// handle turretSpeedCheck
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					// percentage != 0
					else
					{
						// Fire event rightStickUsed
						fireNextEvent(Event[2], percentage);
						// Wait for follow-up event 'turretCheck'
						waitForNextEvent(Event[3]);
						// Wait for follow-up event 'turretSpeedCheck'
						waitForNextEvent(Event[4]);
						// handle turretSpeedCheck
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
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
			percentage = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				// Event = setRightXvalue
				if (nextEvent.equals(Event[0]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = turretReset
				else if (nextEvent.equals(Event[5]))
				{
					// Fire next event 'rightStickNotUsed'
					fireNextEvent(Event[1]);
					// Wait for follow-up event 'turretCheck'
					waitForNextEvent(Event[3]);
					// Wait for follow-up event 'turretSpeedCheck'
					waitForNextEvent(Event[4]);
					
					// Fire next event 'turretResetCheck'
					fireNextEvent(Event[6]);
					
					// stay in the same state after turretReset
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
			
			// State == Decide
			else if (currentState.equals(State[1]))
			{
				// Event = turretSpeedCheck
				if (nextEvent.equals(Event[4]))
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
	
	public class Right_Stick_Direction extends StateMachine
	{
		// Own variables
		private int percentage;
		
		private String[] State =
		{
				"Waiting", "Halt", "Decide"
		};
		private String[] Event =
		{
				"rightStickNotUsed", "rightStickUsed", "turretStop", "turretLeft", "turretRight", "turretCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next state or event in 'cells'
				{
						"Halt", null, null
				},
				{
						"Decide", null, null
				},
				{
						null, "turretCheck", null
				},
				{
						null, null, "turretCheck"
				},
				{
						null, null, "turretCheck"
				},
				{
						null, "Waiting", "Waiting"
				}
		};
		
		public Right_Stick_Direction(CommunicationDriver cd)
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
			percentage = 0;
			
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
				
				// State == Halt
				else if (currentState.equals(State[1]))
				{
					// Fire turretStop
					fireEvent(Event[2]);
					
					// Fire next event: turretCheck
					fireNextEvent(Event[5]);
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Decide
				else if (currentState.equals(State[2]))
				{
					if (percentage > 0)
					{
						// Fire turretRight
						fireEvent(Event[4]);
					}
					// percentage < 0
					else
					{
						// Fire turretLeft
						fireEvent(Event[3]);
					}
					
					// Fire next event: turretCheck
					fireNextEvent(Event[5]);
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
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
			percentage = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				// Event = rightStickNotUsed
				if (nextEvent.equals(Event[0]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = rightStickUsed
				else if (nextEvent.equals(Event[1]))
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
			
			// State == Halt
			else if (currentState.equals(State[1]))
			{
				// Event = turretCheck
				if (nextEvent.equals(Event[5]))
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
			
			// State == Decide
			else if (currentState.equals(State[2]))
			{
				// Event = turretCheck
				if (nextEvent.equals(Event[5]))
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
				System.out.println(this.getClass().getName() + ": Unknown State detected in executeEvent()");
				error = true;
			}
			
			if (!error && changeState)
			{
				currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
			}
			nextEvent = null;
		}
	}
	
	public class Turret_Speed extends StateMachine
	{
		// Own variables
		private int speed;
		
		private String[] State =
		{
				"Waiting", "Halt", "Move"
		};
		private String[] Event =
		{
				"rightStickNotUsed", "rightStickUsed", "setTurretWanted", "turretSpeedCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next state or event in 'cells'
				{
						"Halt", null, null
				},
				{
						"Move", null, null
				},
				{
						null, "turretSpeedCheck", "turretSpeedCheck"
				},
				{
						null, "Waiting", "Waiting"
				}
		};
		
		public Turret_Speed(CommunicationDriver cd)
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
			speed = 0;
			
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
				
				// State == Halt
				else if (currentState.equals(State[1]))
				{
					// Fire setTurretWanted[0]
					fireEvent(Event[2], 0);
					waitForEvent();
					
					// Fire turretSpeedCheck
					fireNextEvent(Event[3]);
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Move
				else if (currentState.equals(State[2]))
				{
					if (speed > 0)
					{
						// Fire setTurretWanted[speed]
						fireEvent(Event[2], speed);
						waitForEvent();
					}
					else
					// speed < 0
					{
						// Fire setTurretWanted[-speed]
						fireEvent(Event[2], (speed * -1));
						waitForEvent();
					}
					
					// Fire turretSpeedCheck
					fireNextEvent(Event[3]);
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
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
			speed = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				// Event = rightStickNotUsed
				if (nextEvent.equals(Event[0]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = rightStickUsed
				else if (nextEvent.equals(Event[1]))
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
			
			// State == Halt | Move
			else if (currentState.equals(State[1]) || currentState.equals(State[2]))
			{
				// Event = turretSpeedCheck
				if (nextEvent.equals(Event[3]))
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
