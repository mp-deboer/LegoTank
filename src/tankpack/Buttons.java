package tankpack;

public class Buttons
{
	private final int BUTTONAMOUNT = 17;
	
	public Buttons(CommunicationDriver cd, PsController psController)
	{
		PsComponent[] PsComponents = PsComponent.values();
		PsButton[] buttons = new PsButton[BUTTONAMOUNT];
		Thread[] buttonThreads = new Thread[BUTTONAMOUNT];
		
		// Start threads for all buttons, except for L2 (8) and R2 (9)
		for (int i = 0; i < BUTTONAMOUNT; i++)
		{
			if (i != 8 && i != 9)
			{
				buttons[i] = new PsButton(cd, psController, PsComponents[i]);
				buttonThreads[i] = new Thread(buttons[i]);
				buttonThreads[i].start();
			}
		}
	}
	
	public class PsButton extends StateMachine
	{
		// Own variables
		protected PsComponent button;
		protected PsController psC;
		
		private String[] State =
		{
				"Waiting", "Released", "Pressed", "AutoRelease"
		};
		
		private String[] Event =
		{
				"foundController", "buttonPressed", "buttonReleased", "lostController",
		};
		
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// NextState in 'cells'
				{
						"Released", null, null, null
				},
				{
						null, "Pressed", null, null
				},
				{
						null, null, "Released", "Waiting"
				},
				{
						null, "Waiting", "AutoRelease", "exampleReleased"
				}
				
		};
		
		public PsButton(CommunicationDriver cd, PsController psController, PsComponent button)
		{
			String type = button.toString().toLowerCase();
			this.Event[1] = type + "Pressed";
			this.Event[2] = type + "Released";
			this.SensitivityTable[SensitivityTable.length - 1][SensitivityTable[0].length - 1] = type + "Released";
			
			// StateMachine variables
			super.State = this.State;
			super.Event = this.Event;
			super.SensitivityTable = this.SensitivityTable;
			
			debug = false;
			this.cd = cd;
			currentState = State[0];
			nextEvent = null;
			
			// Declare own variables:
			this.button = button;
			this.psC = psController;
			
			processID = cd.registerNewProcess(this, (type + "Button"), Event, State, getStateNr(currentState),
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
				
				// State == Released
				else if (currentState.equals(State[1]))
				{
					if (checkForEvent()) // check for lostController
					{
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					else if (isPressed(button))
					{
						// fire buttonPressed
						fireEvent(Event[1]);
						waitForEvent();
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					else
					// when there is no event and button is not pressed
					{
						sleep(100);
					}
				}
				
				// State == Pressed
				else if (currentState.equals(State[2]))
				{
					if (checkForEvent()) // check for lostController
					{
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					else if (!isPressed(button))
					{
						// fire buttonReleased
						fireEvent(Event[2]);
						waitForEvent();
						executeEvent();
						cd.updateProcess(processID, getStateNr(currentState), getSensitives());
					}
					else
					// when there is no event and button is not released
					{
						sleep(100);
					}
				}
				
				// State == autoRelease
				else if (currentState.equals(State[3]))
				{
					// Fire buttonReleased
					fireEvent(Event[2]);
					waitForEvent();
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				else
				{
					// Unknown state error handling here
					System.out
							.println(this.getClass().getName() + ": Unknown State detected in run(): " + currentState);
					break;
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
			
			// State == Released
			else if (currentState.equals(State[1]))
			{
				// Event = buttonPressed
				if (nextEvent.equals(Event[1]))
				{
					error = false;
					changeState = true;
				}
				// Event = lostController
				else if (nextEvent.equals(Event[3]))
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
			// State == Pressed
			else if (currentState.equals(State[2]))
			{
				// Event = buttonReleased
				if (nextEvent.equals(Event[2]))
				{
					error = false;
					changeState = true;
				}
				// Event = lostController
				else if (nextEvent.equals(Event[3]))
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
			// State == autoRelease
			else if (currentState.equals(State[3]))
			{
				// Event = buttonReleased
				if (nextEvent.equals(Event[2]))
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
		
		protected boolean isPressed(PsComponent c)
		{
			return (psC.getComps()[c.getIndex(psC.getIsPs5())].getPollData() == 1.0f);
		}
	}
}
