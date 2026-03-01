package tankpack;

public class Tracks
{
	public Tracks(CommunicationDriver cd, PsController psController)
	{
		Left_Stick_X lS_X = new Left_Stick_X(cd, psController);
		Left_Stick_Y lS_Y = new Left_Stick_Y(cd, psController);
		Left_Stick_Degree lS_Deg = new Left_Stick_Degree(cd);
		Left_Stick_Direction lS_Dir = new Left_Stick_Direction(cd);
		Left_Stick_Percentage lS_Perc = new Left_Stick_Percentage(cd);
		Tracks_Direction tr_Dir = new Tracks_Direction(cd);
		Tracks_Speed tr_Speed = new Tracks_Speed(cd);
		
		Thread thrLS_X = new Thread(lS_X);
		Thread thrLS_Y = new Thread(lS_Y);
		Thread thrLS_Deg = new Thread(lS_Deg);
		Thread thrLS_Dir = new Thread(lS_Dir);
		Thread thrLS_Perc = new Thread(lS_Perc);
		Thread thrTr_Dir = new Thread(tr_Dir);
		Thread thrTr_Speed = new Thread(tr_Speed);
		
		thrLS_X.start();
		thrLS_Y.start();
		thrLS_Deg.start();
		thrLS_Dir.start();
		thrLS_Perc.start();
		thrTr_Dir.start();
		thrTr_Speed.start();
	}
	
	public class Left_Stick_X extends StateMachine
	{
		// Own variables
		private int newValue;
		private int currentValue;
		private PsComponent stick;
		protected PsController psC;
		
		private String[] State =
		{
				"Left_Stick_X", "Decide"
		};
		private String[] Event =
		{
				"getLeftXvalue", "setLeftXvalue", "leftXdoNothing"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Decide", null
				},
				{
						"Left_Stick_X", "Left_Stick_X"
				},
				{
						null, "Left_Stick_X"
				}
		};
		
		public Left_Stick_X(CommunicationDriver cd, PsController psController)
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
			this.stick = PsComponent.Leftstick_X;
			this.psC = psController;
			
			processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
					getSensitives());
		}
		
		@Override
		public void run()
		{
			while (true)
			{
				// State == Left_Stick_X
				if (currentState.equals(State[0]))
				{
					sleep(100);
					
					// check for event setLeftXvalue
					if (checkForEvent())
					{
						// handle setLeftXvalue
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					// If there is no setLeftXvalue event, attempt to fire getLeftXvalue
					else if (attemptFireEvent(Event[0]))
					{
						// handle getLeftXvalue
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
				}
				
				// State == Decide
				else if (currentState.equals(State[1]))
				{
					if (currentValue != newValue)
					{
						// Fire event setLeftXvalue
						fireNextEvent(Event[1], newValue);
						waitForEvent();
						executeEvent();
					}
					else if (currentValue == newValue)
					{
						// Fire event leftXdoNothing
						fireNextEvent(Event[2]);
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
			
			// State == Left_Stick_X
			if (currentState.equals(State[0]))
			{
				// Event = getLeftXvalue
				if (nextEvent.equals(Event[0]))
				{
					newValue = getValue(stick);
					error = false;
					changeState = true;
				}
				// Event = setLeftXvalue
				else if (nextEvent.equals(Event[1]))
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
			
			// State == Decide
			else if (currentState.equals(State[1]))
			{
				// Event = setLeftXvalue
				if (nextEvent.equals(Event[1]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = leftXdoNothing
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
		
		private int getValue(PsComponent c)
		{
			try
			{
				// round down from 0.0 - 0.5 and round up from 0.5 - 1.0
				double tmpValue = psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData();
				
				// 2026-01-02 MdB: Ignore noise
				if (tmpValue <= 0.06f && tmpValue >= -0.06f) return 0;
				else if (tmpValue > 0.0f)
					return ((int) (psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData() * 100 + 0.5));
				// tmpValue < 0.0
				else return ((int) (psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData() * 100 - 0.5));
			}
			catch (NullPointerException e)
			{
				System.out.println(this.getClass().getName() + ": Caught a NullPointerException in getValue()");
				
				return 0;
			}
		}
	}
	
	public class Left_Stick_Y extends StateMachine
	{
		// Own variables
		private int newValue;
		private int currentValue;
		private PsComponent stick;
		protected PsController psC;
		
		private String[] State =
		{
				"Left_Stick_Y", "Decide"
		};
		private String[] Event =
		{
				"getLeftYvalue", "setLeftYvalue", "leftYdoNothing"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Decide", null
				},
				{
						"Left_Stick_Y", "Left_Stick_Y"
				},
				{
						null, "Left_Stick_Y"
				}
		};
		
		public Left_Stick_Y(CommunicationDriver cd, PsController psController)
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
			this.stick = PsComponent.Leftstick_Y;
			this.psC = psController;
			
			processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
					getSensitives());
		}
		
		@Override
		public void run()
		{
			while (true)
			{
				// State == Left_Stick_X
				if (currentState.equals(State[0]))
				{
					sleep(100);
					
					// check for event setLeftYvalue
					if (checkForEvent())
					{
						// handle setLeftYvalue
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					// If there is no setLeftYvalue event, attempt to fire getLeftYvalue
					else if (attemptFireEvent(Event[0]))
					{
						// handle getLeftYvalue
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
				}
				
				// State == Decide
				else if (currentState.equals(State[1]))
				{
					if (currentValue != newValue)
					{
						// Fire event setLeftYvalue
						fireNextEvent(Event[1], newValue);
						waitForEvent();
						executeEvent();
					}
					else if (currentValue == newValue)
					{
						// Fire event leftYdoNothing
						fireNextEvent(Event[2]);
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
			
			// State == Left_Stick_Y
			if (currentState.equals(State[0]))
			{
				// Event = getLeftYvalue
				if (nextEvent.equals(Event[0]))
				{
					newValue = getValue(stick);
					error = false;
					changeState = true;
				}
				// Event = setLeftYvalue
				else if (nextEvent.equals(Event[1]))
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
			
			// State == Decide
			else if (currentState.equals(State[1]))
			{
				// Event = setLeftYvalue
				if (nextEvent.equals(Event[1]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = leftYdoNothing
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
		
		private int getValue(PsComponent c)
		{
			try
			{
				// round down from 0.0 - 0.5 and round up from 0.5 - 1.0
				double tmpValue = psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData();
				
				// 2026-01-02 MdB: Ignore noise
				if (tmpValue <= 0.06f && tmpValue >= -0.06f) return 0;
				else if (tmpValue > 0.0f)
					return ((int) (psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData() * 100 + 0.5));
				// tmpValue < 0.0
				else return ((int) (psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData() * 100 - 0.5));
			}
			catch (NullPointerException e)
			{
				System.out.println(this.getClass().getName() + ": Caught a NullPointerException in getValue()");
				
				return 0;
			}
		}
	}
	
	public class Left_Stick_Degree extends StateMachine
	{
		// Own variables
		private int xValue;
		private int yValue;
		private int degree;
		
		// State-Machine variables
		private int data;
		
		private String[] State =
		{
				"Waiting", "Decide"
		};
		private String[] Event =
		{
				"setLeftXvalue",
				"setLeftYvalue",
				"leftStickNoDegree",
				"leftStickDegree",
				"trackCheck",
				"trackReset",
				"trackResetCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Decide", null
				},
				{
						"Decide", null
				},
				{
						"trackCheck", "trackCheck"
				},
				{
						null, "trackCheck"
				},
				{
						"trackResetCheck", "Waiting"
				},
				{
						"leftStickNoDegree", null
				},
				{
						"Waiting", null
				}
		};
		
		public Left_Stick_Degree(CommunicationDriver cd)
		{
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
			xValue = 0;
			yValue = 0;
			degree = 0;
			
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
					// nextEvent == setLeftXvalue
					if (nextEvent.equals(Event[0]))
					{
						xValue = data;
						// Wait for next Event: setLeftYvalue
						// checkForEvent(Event[1]);
					}
					// nextEvent == setLeftYvalue
					else if (nextEvent.equals(Event[1]))
					{
						yValue = data;
						// Wait for next Event: setLeftXvalue
						// checkForEvent(Event[0]);
					}
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Decide
				else if (currentState.equals(State[1]))
				{
					if (xValue == 0 && yValue == 0)
					{
						// Fire event leftStickNoDegree
						fireEvent(Event[2]);
						
						// Wait for next Event: trackCheck
						waitForNextEvent(Event[4]);
					}
					else
					{
						degree = calculateDegree(xValue, yValue);
						// Fire event leftStickDegree
						fireEvent(Event[3], degree);
						
						// Wait for next Event: trackCheck
						waitForNextEvent(Event[4]);
					}
					
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
			this.data = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				// Event = setLeftXvalue
				if (nextEvent.equals(Event[0]))
				{
					xValue = data;
					
					error = false;
					changeState = true;
				}
				
				// Event = setLeftYvalue
				else if (nextEvent.equals(Event[1]))
				{
					yValue = data;
					
					error = false;
					changeState = true;
				}
				
				// Event = trackReset
				else if (nextEvent.equals(Event[5]))
				{
					// Fire next event 'leftStickNoDegree'
					fireNextEvent(Event[2]);
					// Wait for follow-up event 'trackCheck'
					waitForNextEvent(Event[4]);
					
					// Fire next event 'trackResetCheck'
					fireNextEvent(Event[6]);
					
					// stay in the same state after trackReset
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
				// Event = trackCheck
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
		
		private int calculateDegree(int x, int y)
		{
			double angle;
			
			// source:
			// http://forum.processing.org/two/discussion/4136/how-do-i-translate-joystick-values-to-degree-of-arc/p1
			angle = Math.atan2(y, x);
			if (angle < 0) angle += (2.0 * Math.PI);
			angle = (angle + (0.5 * Math.PI)) % (2.0 * Math.PI);
			
			return ((int) Math.toDegrees(angle));
		}
	}
	
	public class Left_Stick_Direction extends StateMachine
	{
		// Own variables
		private int degrees;
		
		private String[] State =
		{
				"Waiting", "Halt", "Decide"
		};
		private String[] Event =
		{
				"leftStickNoDegree",
				"leftStickDegree",
				"trackForward",
				"trackBackward",
				"trackLeft",
				"trackRight",
				"trackLeftBack",
				"trackRightBack",
				"trackRotateLeft",
				"trackRotateRight",
				"trackStop",
				"trackCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Halt", null /* trackStop */, null
				},
				{
						"Decide", null, null
				// trackForward | trackRight | trackRotateRight | trackRightback | trackBackward | trackLeftBack |
				// trackRotateRight | trackLeft
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, null, "trackCheck"
				},
				{
						null, "trackCheck", null
				},
				{
						null, "Waiting", "Waiting"
				}
				
		};
		
		public Left_Stick_Direction(CommunicationDriver cd)
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
			degrees = 0;
			
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
					// fire event trackStop
					fireEvent(Event[10]);
					waitForEvent();
					
					// Wait for next Event trackCheck
					waitForNextEvent(Event[11]);
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Decide
				else if (currentState.equals(State[2]))
				{
					// 360 degrees divided over 8 directions gives 45 degrees per direction
					// So forward is 45 degrees wide with 0 as center degree
					if (degrees >= 338 || degrees <= 22)
					{
						// fire Event trackForward
						fireEvent(Event[2]);
					}
					else if (degrees >= 23 && degrees <= 67)
					{
						// fire Event trackRight
						fireEvent(Event[5]);
					}
					else if (degrees >= 68 && degrees <= 112)
					{
						// fire Event trackRotateRight
						fireEvent(Event[9]);
					}
					else if (degrees >= 113 && degrees <= 157)
					{
						// fire Event trackLeftBack
						fireEvent(Event[6]);
					}
					else if (degrees >= 158 && degrees <= 202)
					{
						// fire Event trackBackward
						fireEvent(Event[3]);
					}
					else if (degrees >= 203 && degrees <= 247)
					{
						// fire Event trackRightBack
						fireEvent(Event[7]);
					}
					else if (degrees >= 248 && degrees <= 292)
					{
						// fire Event trackRotateLeft
						fireEvent(Event[8]);
					}
					else if (degrees >= 293 && degrees <= 337)
					{
						// fire Event trackLeft
						fireEvent(Event[4]);
					}
					waitForEvent();
					
					// Wait for next Event trackCheck
					waitForNextEvent(Event[11]);
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
			degrees = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				// Event = leftStickNoDegree
				if (nextEvent.equals(Event[0]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = leftStickDegree
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
				// Event = trackCheck
				if (nextEvent.equals(Event[11]))
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
				// Event = trackCheck
				if (nextEvent.equals(Event[11]))
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
	
	public class Left_Stick_Percentage extends StateMachine
	{
		// Own variables
		private int xValue;
		private int yValue;
		private int percentage;
		
		// State-Machine variables
		private int data;
		
		private String[] State =
		{
				"Waiting", "Decide"
		};
		private String[] Event =
		{
				"setLeftXvalue",
				"setLeftYvalue",
				"leftStickNoPercentage",
				"leftStickPercentage",
				"trackSpeedCheck",
				"trackReset",
				"trackResetCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Decide", null
				},
				{
						"Decide", null
				},
				{
						"trackSpeedCheck", "trackSpeedCheck"
				},
				{
						null, "trackSpeedCheck"
				},
				{
						"trackResetCheck", "Waiting"
				},
				{
						"leftStickNoPercentage", null
				},
				{
						"Waiting", null
				}
		};
		
		public Left_Stick_Percentage(CommunicationDriver cd)
		{
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
			xValue = 0;
			yValue = 0;
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
					// nextEvent == setLeftXvalue
					if (nextEvent.equals(Event[0]))
					{
						xValue = data;
						// Wait for next Event: setLeftYvalue
						// checkForEvent(Event[1]);
					}
					// nextEvent == setLeftYvalue
					else if (nextEvent.equals(Event[1]))
					{
						yValue = data;
						// Wait for next Event: setLeftXvalue
						// checkForEvent(Event[0]);
					}
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Decide
				else if (currentState.equals(State[1]))
				{
					if (xValue == 0 && yValue == 0)
					{
						// Fire event leftStickNoPercentage
						fireEvent(Event[2]);
						
						// Wait for next Event: trackSpeedCheck
						waitForNextEvent(Event[4]);
					}
					else
					{
						percentage = calculatePercentage(xValue, yValue);
						// Fire event leftStickPercentage
						fireEvent(Event[3], percentage);
						
						// Wait for next Event: trackSpeedCheck
						waitForNextEvent(Event[4]);
					}
					
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
			this.data = data;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				// Event = setLeftXvalue
				if (nextEvent.equals(Event[0]))
				{
					xValue = data;
					
					error = false;
					changeState = true;
				}
				
				// Event = setLeftYvalue
				else if (nextEvent.equals(Event[1]))
				{
					yValue = data;
					
					error = false;
					changeState = true;
				}
				
				// Event = trackReset
				else if (nextEvent.equals(Event[5]))
				{
					// Fire next event 'leftStickNoPercentage'
					fireNextEvent(Event[2]);
					// Wait for follow-up event 'trackSpeedCheck'
					waitForNextEvent(Event[4]);
					
					// Fire next event 'trackResetCheck'
					fireNextEvent(Event[6]);
					
					// stay in the same state after trackReset
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
				// Event = trackSpeedCheck
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
		
		private int calculatePercentage(int x, int y)
		{
			x = Math.abs(x);
			y = Math.abs(y);
			return Math.max(x, y);
		}
	}
	
	public class Tracks_Direction extends StateMachine
	{
		// Own variables
		private String previousState;
		
		private String[] State =
		{
				"Waiting",
				"Forward",
				"Backward",
				"Left",
				"Leftback",
				"Right",
				"Rightback",
				"RotateLeft",
				"RotateRight",
				"Stop"
		};
		private String[] Event =
		{
				"trackForward",
				"trackBackward",
				"trackLeft",
				"trackLeftBack",
				"trackRight",
				"trackRightBack",
				"trackRotateLeft",
				"trackRotateRight",
				"trackStop",
				"leftTrackForward",
				"leftTrackBackward",
				"leftTrackStop",
				"rightTrackForward",
				"rightTrackBackward",
				"rightTrackStop",
				"trackCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Forward", null /* leftTrackForward */, null, null, null, null, null, null, null, null
				},
				{
						"Backward", null, null /* leftTrackBackward */, null, null, null, null, null, null, null
				},
				{
						"Left", null, null, null /* leftTrackStop */, null, null, null, null, null, null
				},
				{
						"Leftback", null, null, null, null /* leftTrackStop */, null, null, null, null, null
				},
				{
						"Right", null, null, null, null, null /* leftTrackForward */, null, null, null, null
				},
				{
						"Rightback", null, null, null, null, null, null /* leftTrackBackward */, null, null, null
				},
				{
						"RotateLeft", null, null, null, null, null, null, null /* leftTrackBackward */, null, null
				},
				{
						"RotateRight", null, null, null, null, null, null, null, null /* leftTrackForward */, null
				},
				{
						"Stop", null, null, null, null, null, null, null, null, null
				/* leftTrackStop */
				},
				{
						null,
						"rightTrackForward",
						null,
						null,
						null,
						"rightTrackStop",
						null,
						null,
						"rightTrackBackward",
						null
				},
				{
						null,
						null,
						"rightTrackBackward",
						null,
						null,
						null,
						"rightTrackStop",
						"rightTrackForward",
						null,
						null
				},
				{
						null,
						null,
						null,
						"rightTrackForward",
						"rightTrackBackward",
						null,
						null,
						null,
						null,
						"rightTrackStop"
				},
				{
						null, "trackCheck", null, "trackCheck", null, null, null, "trackCheck", null, null
				},
				{
						null, null, "trackCheck", null, "trackCheck", null, null, null, "trackCheck", null
				},
				{
						null, null, null, null, null, "trackCheck", "trackCheck", null, null, "trackCheck"
				},
				{
						null,
						"Waiting",
						"Waiting",
						"Waiting",
						"Waiting",
						"Waiting",
						"Waiting",
						"Waiting",
						"Waiting",
						"Waiting"
				}
		};
		
		public Tracks_Direction(CommunicationDriver cd)
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
			previousState = State[0];
			
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
				
				// State == Forward
				else if (currentState.equals(State[1]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackForward
					fireEvent(Event[9]);
					waitForEvent();
					
					// Fire next event rightTrackForward
					fireNextEvent(Event[12]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Backward
				else if (currentState.equals(State[2]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackBackward
					fireEvent(Event[10]);
					waitForEvent();
					
					// Fire next event rightTrackBackward
					fireNextEvent(Event[13]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Left
				else if (currentState.equals(State[3]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackStop
					fireEvent(Event[11]);
					waitForEvent();
					
					// Fire next event rightTrackForward
					fireNextEvent(Event[12]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == LeftBack
				else if (currentState.equals(State[4]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackStop
					fireEvent(Event[11]);
					waitForEvent();
					
					// Fire next event rightTrackBackward
					fireNextEvent(Event[13]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Right
				else if (currentState.equals(State[5]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackForward
					fireEvent(Event[9]);
					waitForEvent();
					
					// Fire next event rightTrackStop
					fireNextEvent(Event[14]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == RightBack
				else if (currentState.equals(State[6]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackBackward
					fireEvent(Event[10]);
					waitForEvent();
					
					// Fire next event rightTrackStop
					fireNextEvent(Event[14]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == RotateLeft
				else if (currentState.equals(State[7]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackBackward
					fireEvent(Event[10]);
					waitForEvent();
					
					// Fire next event rightTrackForward
					fireNextEvent(Event[12]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == RotateRight
				else if (currentState.equals(State[8]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackForward
					fireEvent(Event[9]);
					waitForEvent();
					
					// Fire next event rightTrackBackward
					fireNextEvent(Event[13]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Stop
				else if (currentState.equals(State[9]) && !currentState.equals(previousState))
				{
					previousState = currentState;
					
					// fire event leftTrackStop
					fireEvent(Event[11]);
					waitForEvent();
					
					// Fire next event rightTrackStop
					fireNextEvent(Event[14]);
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State was already the same as the new direction
				else if (currentState.equals(previousState))
				{
					// do nothing
					
					// Fire next event trackCheck
					fireNextEvent(Event[15]);
					
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
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Waiting
			if (currentState.equals(State[0]))
			{
				switch (nextEvent)
				{
					case "trackForward":
					case "trackBackward":
					case "trackLeft":
					case "trackLeftBack":
					case "trackRight":
					case "trackRightBack":
					case "trackRotateLeft":
					case "trackRotateRight":
					case "trackStop":
						error = false;
						changeState = true;
					break;
				
					default:
						// Event error handling here
						System.out.println(
								this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
						error = true;
					break;
				}
			}
			
			// State == Forward | Backward | Left | LeftBack | Right | RightBack | RotateLeft | RotateRight | Stop
			else
			{
				for (int i = 1; i < State.length; i++)
				{
					if (currentState.equals(State[i]))
					{
						// Event = trackCheck
						if (nextEvent.equals(Event[15]))
						{
							error = false;
							changeState = true;
						}
						
						else
						{
							// Event error handling here
							System.out.println(this.getClass().getName()
									+ ": Unknown event detected in executeEvent(): " + nextEvent);
							error = true;
						}
					}
				}
			}
			
			if (!error && changeState)
			{
				currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
			}
			nextEvent = null;
		}
	}
	
	public class Tracks_Speed extends StateMachine
	{
		// Own variables
		private int speed;
		
		private String[] State =
		{
				"Waiting", "Halt", "Move"
		};
		private String[] Event =
		{
				"leftStickNoPercentage",
				"leftStickPercentage",
				"setLeftTrackWanted",
				"setRightTrackWanted",
				"trackSpeedCheck"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next state or event in 'cells'
				{
						"Halt", null /* setLeftTrackWanted */, null
				},
				{
						"Move", null, null
				/* setLeftTrackWanted */
				},
				{
						null, "setRightTrackWanted", "setRightTrackWanted"
				},
				{
						null, "trackSpeedCheck", "trackSpeedCheck"
				},
				{
						null, "Waiting", "Waiting"
				}
		};
		
		public Tracks_Speed(CommunicationDriver cd)
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
					// Fire setLeftTrackWanted
					fireEvent(Event[2], 0);
					waitForEvent();
					
					// Fire setRightTrackWanted
					fireNextEvent(Event[3], 0);
					
					// Fire trackSpeedCheck
					fireNextEvent(Event[4]);
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Move
				else if (currentState.equals(State[2]))
				{
					// Fire setLeftTrackWanted
					fireEvent(Event[2], speed);
					waitForEvent();
					
					// Fire setRightTrackWanted
					fireNextEvent(Event[3], speed);
					
					// Fire trackSpeedCheck
					fireNextEvent(Event[4]);
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
				// Event = leftStickNoPercentage
				if (nextEvent.equals(Event[0]))
				{
					error = false;
					changeState = true;
				}
				
				// Event = leftStickPercentage
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
				// Event = trackSpeedCheck
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
}
