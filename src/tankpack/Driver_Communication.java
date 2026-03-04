package tankpack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class Driver_Communication
{
	private final boolean localDebug = false;
	
	public enum DataType
	{
		// Allowed datatypes for events
		None, Integer
	}
	
	private boolean executingEvent;
	private int processCounter;
	private ArrayList<ProcessInfo> processes;
	
	// New record to encapsulate per-process data
	private record ProcessInfo(int id, StateMachine object, String name, String[] events, String[] states,
			String[] allowedEvents)
	{}
	
	// Initialise Event queue variables
	private record QueuedEvent(String name, DataType type, Object data, boolean debug)
	{}
	
	private Deque<QueuedEvent> eventQueue = new ArrayDeque<>();
	
	public Driver_Communication()
	{
		executingEvent = false;
		processCounter = 0;
		processes = new ArrayList<>();
		
		eventQueue = new ArrayDeque<>();
	}
	
	public void shutdown()
	{
		processes.clear();
		eventQueue.clear();
		
		if (localDebug)
			System.out.println("Communication shutdown complete.");
	}
	
	public synchronized int registerNewProcess(StateMachine object, String name, String[] events, String[] states,
			boolean debug)
	{
		processCounter++;
		
		// Add process, leave allowed events empty; processes will update their initial allowed events upon start
		processes.add(new ProcessInfo(processCounter, object, name, events, states, new String[0]));
		
		if (debug)
		{
			System.out.println("Process registered: " + name);
			
			System.out.println("Events:");
			for (int i = 0; i < events.length; i++)
				System.out.println("    - " + events[i]);
		}
		
		return (processCounter);
	}
	
	public void printAllProcesses()
	{
		System.out.println("All processes:");
		
		for (int i = 0; i < processes.size(); i++)
			System.out.printf("%2d - %s\n", processes.get(i).id, processes.get(i).name);
	}
	
	public synchronized boolean eventAllowed(String event)
	{
		if (!executingEvent)
		{
			boolean allowed = true;
			
			for (int i = 0; i < processes.size() && allowed; i++)
				if (hasEvent(i, event))
					allowed = isAllowed(i, event);
				
			if (allowed)
				startEvent();
			
			return allowed;
		}
		else // already executing an event
		{
			return false;
		}
	}
	
	public void executeEvent(String event)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearAllowedEvents(index);
			
			if (localDebug)
				System.out.printf("Debug CommunicationDriver: Dispatching event to '%s': %s\n",
						processes.get(index).name, event);
			
			processes.get(index).object.dispatchEvent(event);
		}
		
		finishedEvent();
		// notifyWaitingThreads();
	}
	
	public void executeEvent(String event, int data)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearAllowedEvents(index);
			
			if (localDebug)
				System.out.printf("Debug CommunicationDriver: Dispatching event to '%s': %s (%d)\n",
						processes.get(index).name, event, data);
			
			processes.get(index).object.dispatchEvent(event, data);
		}
		
		finishedEvent();
		// notifyWaitingThreads();
	}
	
	public void executeDoEvent()
	{
		String doEvent = "DO";
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(doEvent);
		
		// Execute queuedEvents, should only happen at startup
		if (executeQueuedEvents() > 0)
			System.out
					.println("! Warning: Executed queued events before the DO event (should only happen on startup).");
		
		// Iterate over processes accepting event "DO" and dispatch the event
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			processes.get(index).object.dispatchEvent(doEvent);
		}
		
		// Execute queuedEvents in case the DO-iteration resulted in one or more
		executeQueuedEvents();
		
		// notifyWaitingThreads();
	}
	
	private int executeQueuedEvents()
	{
		int size = eventQueue.size();
		
		// Processing:
		while (!eventQueue.isEmpty())
		{
			QueuedEvent qe = eventQueue.removeFirst();
			handleQueuedEvent(qe.name(), qe.type(), qe.data(), qe.debug());
		}
		
		return size;
	}
	
	private void handleQueuedEvent(String queuedEvent, DataType queuedDataType, Object data, boolean debug)
	{
		if (!queuedEvent.equals(""))
		{
			if (eventAllowed(queuedEvent))
			{
				if (debug)
					System.out.print("Executing queuedEvent: " + queuedEvent);
				
				if (queuedDataType.equals(DataType.Integer))
				{
					if (debug) // Append data to debug line
						System.out.println(" (" + data + ")");
					
					executeEvent(queuedEvent, (Integer) data);
				}
				else // if (queuedDataType.equals(DataType.None))
				{
					if (debug) // No data, print newline
						System.out.println();
					
					executeEvent(queuedEvent);
				}
			}
			else
			{
				System.out.println("! Error: queuedEvent " + queuedEvent + " is not allowed.");
			}
		}
	}
	
	public void addQueuedEvent(String event, boolean debug)
	{
		if (debug)
			System.out.println("Adding event to queue: " + event);
		
		eventQueue.addLast(new QueuedEvent(event, DataType.None, 0, debug));
	}
	
	public void addQueuedEvent(String event, int data, boolean debug)
	{
		if (debug)
			System.out.println("Adding event to queue: " + event + " (" + data + ")");
		
		eventQueue.addLast(new QueuedEvent(event, DataType.Integer, data, debug));
	}
	
	public synchronized void updateProcess(int processId, String newState, String[] newAllowedEvents, boolean debug)
	{
		for (int i = 0; i < processes.size(); i++)
		{
			ProcessInfo p = processes.get(i);
			if (p.id == processId)
			{
				processes.set(i, new ProcessInfo(p.id, p.object, p.name, p.events, p.states, newAllowedEvents));
				
				if (debug)
				{
					System.out.printf("Debug CommunicationDriver: New State for Process: %s (%d): %s\n", p.name, p.id,
							newState);
					
					if (localDebug)
					{
						System.out.println("New allowed events:");
						
						for (int k = 0; k < newAllowedEvents.length; k++)
							System.out.println("    - " + newAllowedEvents[k]);
					}
				}
			}
		}
	}
	
	private boolean hasEvent(int index, String event)
	{
		String[] tmpEvents = processes.get(index).events;
		
		if (tmpEvents != null)
			for (int i = 0; i < tmpEvents.length; i++)
				if (tmpEvents[i].equals(event))
					return true;
				
		return false;
	}
	
	private boolean isAllowed(int index, String event)
	{
		String[] allowedEvents = processes.get(index).allowedEvents;
		
		if (allowedEvents != null)
			for (int i = 0; i < allowedEvents.length; i++)
				if (allowedEvents[i].equals(event))
					return true;
				
		return false;
	}
	
	private Integer[] getSensitiveProcesses(String event)
	{
		String[] allowedEvents;
		ArrayList<Integer> tmpSensitiveProcessIDs = new ArrayList<Integer>();
		
		for (int i = 0; i < processes.size(); i++)
		{
			allowedEvents = processes.get(i).allowedEvents;
			
			if (allowedEvents != null)
			{
				for (int k = 0; k < allowedEvents.length; k++)
				{
					if (allowedEvents[k].equals(event))
					{
						if (localDebug)
							if (!event.equals("DO")) // separate if statement to fix "dead code" warning
								System.out.printf(
										"Debug CommunicationDriver: Found sensitive process '%s' (%d) on event: %s\n",
										processes.get(i).name, processes.get(i).id, allowedEvents[k]);
							
						tmpSensitiveProcessIDs.add(i);
						break;
					}
				}
			}
		}
		return tmpSensitiveProcessIDs.toArray(new Integer[tmpSensitiveProcessIDs.size()]);
	}
	
	private void clearAllowedEvents(int index)
	{
		ProcessInfo p = processes.get(index);
		
		if (localDebug)
			System.out.printf("Debug CommunicationDriver: Clearing allowed events of process '%s' (%d)\n", p.name,
					p.id);
		
		processes.set(index, new ProcessInfo(p.id, p.object, p.name, p.events, p.states, null));
	}
	
	private void startEvent()
	{
		executingEvent = true;
	}
	
	private void finishedEvent()
	{
		executingEvent = false;
	}
	
	// private void notifyWaitingThreads()
	// {
	// synchronized (this)
	// {
	// notifyAll();
	// }
	// }
}
