package tankpack;

import java.util.ArrayList;

public class CommunicationDriver
{
	private final boolean debug = false;
	private final boolean highDebug = false;
	private boolean executingEvent;
	private int processCounter;
	private ArrayList<Integer> processID;
	private ArrayList<StateMachine> processObject;
	private ArrayList<String> processName;
	private ArrayList<String[]> processEvents;
	private ArrayList<String[]> processStates;
	private ArrayList<Integer> processCurrentState;
	private ArrayList<String[]> processSensitives;
	
	public CommunicationDriver()
	{
		executingEvent = false;
		processCounter = 0;
		processID = new ArrayList<Integer>();
		processObject = new ArrayList<StateMachine>();
		processName = new ArrayList<String>();
		processEvents = new ArrayList<String[]>();
		processStates = new ArrayList<String[]>();
		processCurrentState = new ArrayList<Integer>();
		processSensitives = new ArrayList<String[]>();
	}
	
	public synchronized int registerNewProcess(StateMachine object, String name, String[] events, String[] states,
			int currentState, String[] sensitives)
	{
		processCounter++;
		processID.add(processCounter);
		processObject.add(object);
		processName.add(name);
		processEvents.add(events);
		processStates.add(states);
		processCurrentState.add(currentState);
		processSensitives.add(sensitives);
		
		if (highDebug)
		{
			System.out.println("Process registered: " + name);
			System.out.println("Beginstate: " + currentState);
			
			System.out.println("Events:");
			for (int i = 0; i < events.length; i++)
			{
				System.out.println("    - " + events[i]);
			}
			
			System.out.println("Sensitives: ");
			for (int i = 0; i < sensitives.length; i++)
			{
				System.out.println("    - " + sensitives[i]);
			}
		}
		
		return (processCounter);
	}
	
	public void printAllProcesses()
	{
		for (int i = 0; i < processID.size(); i++)
		{
			System.out.println(processID.get(i) + " - " + processName.get(i));
		}
	}
	
	public synchronized boolean eventAllowed(String event)
	{
		boolean allowed = true;
		
		if (executingEvent)
		{
			return false;
		}
		else
		{
			for (int i = 0; i < processID.size() && allowed; i++)
			{
				if (hasEvent(i, event))
				{
					if (!isSensitive(i, event))
					{
						allowed = false;
					}
				}
			}
			
			if (allowed) startEvent();
			
			// if (debug) System.out.println("Debug CommunicationDriver: Event " + allowed + ": " + event);
			
			return allowed;
		}
	}
	
	public void executeEvent(String event)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearSensitives(index);
			processObject.get(index).giveEvent(event);
		}
		
		finishedEvent();
		notifyWaitingThreads();
	}
	
	public void executeEvent(String event, boolean data)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearSensitives(index);
			processObject.get(index).giveEvent(event, data);
		}
		
		finishedEvent();
		notifyWaitingThreads();
	}
	
	public void executeEvent(String event, int data)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearSensitives(index);
			processObject.get(index).giveEvent(event, data);
		}
		
		finishedEvent();
		notifyWaitingThreads();
	}
	
	public synchronized void updateProcess(int processID, int newState, String[] newSensitives)
	{
		int index = this.processID.indexOf(processID);
		
		processCurrentState.set(index, newState);
		processSensitives.set(index, newSensitives);
		
		if (highDebug)
		{
			System.out.println("Process: " + this.processName.get(index));
			System.out.println("New State: " + this.processCurrentState.get(index));
			
			System.out.println("New Sensitives: ");
			for (int i = 0; i < processSensitives.get(index).length; i++)
			{
				System.out.println("    - " + processSensitives.get(index)[i]);
			}
			
		}
	}
	
	private boolean hasEvent(int index, String event)
	{
		String[] tmpEvents = processEvents.get(index);
		
		if (tmpEvents != null)
		{
			for (int i = 0; i < tmpEvents.length; i++)
			{
				if (tmpEvents[i].equals(event))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean isSensitive(int index, String event)
	{
		String[] tmpSensitives = processSensitives.get(index);
		
		if (tmpSensitives != null)
		{
			for (int i = 0; i < tmpSensitives.length; i++)
			{
				if (tmpSensitives[i].equals(event))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private Integer[] getSensitiveProcesses(String event)
	{
		boolean confirmed;
		String[] tmpSensitives;
		ArrayList<Integer> tmpSensitiveProcessIDs = new ArrayList<Integer>();
		
		for (int i = 0; i < processSensitives.size(); i++)
		{
			confirmed = false;
			tmpSensitives = processSensitives.get(i);
			
			if (tmpSensitives != null)
			{
				for (int k = 0; k < tmpSensitives.length && !confirmed; k++)
				{
					if (tmpSensitives[k].equals(event))
					{
						if (debug) System.out.println("Debug CommunicationDriver: Found sensitive process: "
								+ processName.get(i) + " (" + i + ")" + " on event " + tmpSensitives[k]);
						
						tmpSensitiveProcessIDs.add(i);
						confirmed = true;
					}
				}
			}
		}
		return tmpSensitiveProcessIDs.toArray(new Integer[tmpSensitiveProcessIDs.size()]);
	}
	
	private void clearSensitives(int index)
	{
		
		if (debug) System.out.println("Debug CommunicationDriver: Clearing sensitives of index: " + index);
		
		processSensitives.set(index, null);
	}
	
	private void startEvent()
	{
		executingEvent = true;
	}
	
	private void finishedEvent()
	{
		executingEvent = false;
	}
	
	private void notifyWaitingThreads()
	{
		synchronized (this)
		{
			notifyAll();
		}
	}
}
