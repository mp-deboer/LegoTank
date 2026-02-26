package tankpack;

import java.util.ArrayList;

public abstract class StateMachine implements Runnable
{
	// State-machine variables
	protected boolean debug;
	protected CommunicationDriver cd;
	protected int processID;
	protected String currentState;
	protected String nextEvent;
	
	protected String[] State;
	protected String[] Event;
	protected String[][] SensitivityTable;
	
	@Override
	public void run()
	{
	}
	
	protected void executeEvent()
	{
	}
	
	public void giveEvent(String event)
	{
	}
	
	public void giveEvent(String event, boolean data)
	{
	}
	
	public void giveEvent(String event, int data)
	{
	}
	
	protected boolean attemptFireEvent(String event)
	{
		if (debug) System.out.printf("%-20s: Attempting to fire event '%s' without any data.\n",
				this.getClass().getName(), event);
		
		if (cd.eventAllowed(event))
		{
			cd.executeEvent(event);
			
			if (debug)
				System.out.printf("%-20s: Attempt successful, fired event '%s'.\n", this.getClass().getName(), event);
			
			return true;
		}
		else
		{
			if (debug) System.out.printf("%-20s: Attempt failed for event '%s'.\n", this.getClass().getName(), event);
			
			return false;
		}
	}
	
	protected void fireEvent(String event)
	{
		if (debug) System.out.printf("%-20s: Firing event '%s' without any data.\n", this.getClass().getName(), event);
		
		while (!cd.eventAllowed(event))
		{
			sleep(5);
		}
		
		cd.executeEvent(event);
		
		if (debug) System.out.printf("%-20s: Fired event '%s'.\n", this.getClass().getName(), event);
	}
	
	protected void fireEvent(String event, boolean data)
	{
		if (debug)
			System.out.printf("%-20s: Firing event '%s' with data: %B\n", this.getClass().getName(), event, data);
		
		while (!cd.eventAllowed(event))
		{
			sleep(5);
		}
		
		cd.executeEvent(event, data);
		
		if (debug) System.out.printf("%-20s: Fired event '%s'.\n", this.getClass().getName(), event);
	}
	
	protected void fireEvent(String event, int data)
	{
		if (debug)
			System.out.printf("%-20s: Firing event '%s' with data: %d\n", this.getClass().getName(), event, data);
		
		while (!cd.eventAllowed(event))
		{
			sleep(5);
		}
		
		cd.executeEvent(event, data);
		
		if (debug) System.out.printf("%-20s: Fired event '%s'.\n", this.getClass().getName(), event);
	}
	
	protected void fireNextEvent(String followUpEvent)
	{
		if (debug) System.out.printf("%-20s: Firing follow-up event '%s' without any data.\n",
				this.getClass().getName(), followUpEvent);
		
		// prepare for waitForEvent() - nextEvent will be set by executeEvent
		nextEvent = null;
		
		// update Communication Driver with the nextEvent as the only sensitive event
		String[] sensitive =
		{
				followUpEvent
		};
		cd.updateProcess(processID, getStateNr(currentState), sensitive);
		
		while (!checkForEvent())
		{
			if (cd.eventAllowed(followUpEvent))
			{
				cd.executeEvent(followUpEvent);
				waitForEvent();
			}
		}
		
		if (debug) System.out.printf("%-20s: Fired event '%s'.\n", this.getClass().getName(), followUpEvent);
	}
	
	protected void fireNextEvent(String followUpEvent, boolean followUpData)
	{
		if (debug) System.out.printf("%-20s: Firing follow-up event '%s' with data: %B\n", this.getClass().getName(),
				followUpEvent, followUpData);
		
		// prepare for waitForEvent() - nextEvent will be set by executeEvent
		nextEvent = null;
		
		// update Communication Driver with the nextEvent as the only sensitive event
		String[] sensitive =
		{
				followUpEvent
		};
		cd.updateProcess(processID, getStateNr(currentState), sensitive);
		
		while (!checkForEvent())
		{
			if (cd.eventAllowed(followUpEvent))
			{
				cd.executeEvent(followUpEvent, followUpData);
				waitForEvent();
			}
		}
		
		if (debug) System.out.printf("%-20s: Fired event '%s'.\n", this.getClass().getName(), followUpEvent);
	}
	
	protected void fireNextEvent(String followUpEvent, int followUpData)
	{
		if (debug) System.out.printf("%-20s: Firing follow-up event '%s' with data: %d\n", this.getClass().getName(),
				followUpEvent, followUpData);
		
		// prepare for waitForEvent() - nextEvent will be set by executeEvent
		nextEvent = null;
		
		// update Communication Driver with the nextEvent as the only sensitive event
		String[] sensitive =
		{
				followUpEvent
		};
		cd.updateProcess(processID, getStateNr(currentState), sensitive);
		
		while (!checkForEvent())
		{
			if (cd.eventAllowed(followUpEvent))
			{
				cd.executeEvent(followUpEvent, followUpData);
				waitForEvent();
			}
		}
		
		if (debug) System.out.printf("%-20s: Fired event '%s'.\n", this.getClass().getName(), followUpEvent);
	}
	
	protected boolean checkForEvent()
	{
		sleep(5); // wait for possible event
		return (nextEvent != null);
	}
	
	protected boolean checkForEvent(String event)
	{
		String[] sensitive =
		{
				event
		};
		cd.updateProcess(processID, getStateNr(currentState), sensitive);
		
		sleep(10); // wait for possible event
		
		if (nextEvent != null)
		{
			return (nextEvent.equals(event));
		}
		else
		{
			return false;
		}
	}
	
	protected void waitForEvent()
	{
		if (debug) System.out.printf("%-20s: Waiting for an event.\n", this.getClass().getName());
		
		while (nextEvent == null)
		{
			synchronized (cd)
			{
				try
				{
					cd.wait();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		if (debug) System.out.printf("%-20s: Received event: %s\n", this.getClass().getName(), nextEvent);
	}
	
	protected void waitForNextEvent(String followUpEvent)
	{
		if (debug)
			System.out.printf("%-20s: Waiting for follow-up event: %s\n", this.getClass().getName(), followUpEvent);
		
		// prepare for waitForEvent() - nextEvent will be set by executeEvent
		nextEvent = null;
		
		// update Communication Driver with the nextEvent as the only sensitive event
		String[] sensitive =
		{
				followUpEvent
		};
		cd.updateProcess(processID, getStateNr(currentState), sensitive);
		
		waitForEvent();
	}
	
	protected String[] getSensitives()
	{
		int currentStateNr = getStateNr(currentState);
		ArrayList<String> tmpSensitives = new ArrayList<String>();
		
		for (int i = 0; i < SensitivityTable.length; i++)
		{
			if (SensitivityTable[i][currentStateNr] != null)
			{
				tmpSensitives.add(Event[i]);
			}
		}
		
		return tmpSensitives.toArray(new String[tmpSensitives.size()]);
	}
	
	protected int getStateNr(String state)
	{
		for (int i = 0; i < State.length; i++)
		{
			if (State[i].equals(state)) return i;
		}
		
		return -1;
	}
	
	protected int getEventNr(String event)
	{
		for (int i = 0; i < Event.length; i++)
		{
			if (Event[i].equals(event)) return i;
		}
		
		return -1;
	}
	
	protected void sleep(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
}
