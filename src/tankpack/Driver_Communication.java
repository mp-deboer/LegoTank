package tankpack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Driver_Communication
{
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
	private record QueuedEvent(String name, DataType type, Object data, int logLevel)
	{}
	
	private Deque<QueuedEvent> eventQueue = new ArrayDeque<>();
	
	private static final Logger logger = LogManager.getLogger(Driver_Communication.class);
	
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
		
		logger.debug("Communication shutdown complete.");
	}
	
	public synchronized int registerNewProcess(StateMachine object, String name, String[] events, String[] states,
			int logLevel)
	{
		processCounter++;
		
		// Add process, leave allowed events empty; processes will update their initial allowed events upon start
		processes.add(new ProcessInfo(processCounter, object, name, events, states, new String[0]));
		
		if (logLevel >= Level.DEBUG.intLevel())
			logger.debug("Process registered: " + name);
		
		if (logLevel >= Level.TRACE.intLevel())
		{
			logger.trace("Events:");
			for (int i = 0; i < events.length; i++)
				logger.trace("    - " + events[i]);
		}
		
		return (processCounter);
	}
	
	public void printAllProcesses()
	{
		logger.info("All processes:");
		
		for (int i = 0; i < processes.size(); i++)
			logger.info(String.format("%2d - %s", processes.get(i).id, processes.get(i).name));
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
	
	public void executeEvent(String event, int logLevel)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event, logLevel);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearAllowedEvents(index, logLevel);
			
			if (logLevel >= Level.DEBUG.intLevel())
				logger.debug(String.format("Dispatching event to '%s': %s", processes.get(index).name, event));
			
			processes.get(index).object.dispatchEvent(event);
		}
		
		finishedEvent();
		// notifyWaitingThreads();
	}
	
	public void executeEvent(String event, int data, int logLevel)
	{
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(event, logLevel);
		
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			clearAllowedEvents(index, logLevel);
			
			if (logLevel >= Level.DEBUG.intLevel())
				logger.debug(
						String.format("Dispatching event to '%s': %s (%d)", processes.get(index).name, event, data));
			
			processes.get(index).object.dispatchEvent(event, data);
		}
		
		finishedEvent();
		// notifyWaitingThreads();
	}
	
	public void executeDoEvent()
	{
		String doEvent = "DO";
		int index;
		Integer[] tmpProcessIndex = getSensitiveProcesses(doEvent, Level.INFO.intLevel());
		
		// Execute queuedEvents before DO, only happens at startup
		executeQueuedEvents();
		
		// Iterate over processes accepting event "DO" and dispatch the event
		for (int i = 0; i < tmpProcessIndex.length; i++)
		{
			index = tmpProcessIndex[i];
			
			processes.get(index).object.dispatchEvent(doEvent);
		}
		
		// Execute queuedEvents resulting from the DO-iteration (also executes follow-up events)
		executeQueuedEvents();
		
		// notifyWaitingThreads();
	}
	
	private void executeQueuedEvents()
	{
		while (!eventQueue.isEmpty())
		{
			QueuedEvent qe = eventQueue.removeFirst();
			handleQueuedEvent(qe.name, qe.type, qe.data, qe.logLevel);
		}
	}
	
	private void handleQueuedEvent(String queuedEvent, DataType queuedDataType, Object data, int logLevel)
	{
		if (!queuedEvent.equals(""))
		{
			if (eventAllowed(queuedEvent))
			{
				if (logLevel >= Level.DEBUG.intLevel())
				{
					if (!queuedDataType.equals(DataType.None))
						logger.debug("Executing queuedEvent: " + queuedEvent + " (" + data + ")");
					else // if queuedDataType == DataType.None
						logger.debug("Executing queuedEvent: " + queuedEvent);
				}
				
				if (queuedDataType.equals(DataType.Integer))
					executeEvent(queuedEvent, (Integer) data, logLevel);
				else // if (queuedDataType.equals(DataType.None))
					executeEvent(queuedEvent, logLevel);
			}
			else
			{
				logger.error("queuedEvent " + queuedEvent + " is not allowed.");
			}
		}
	}
	
	public void addQueuedEvent(String event, int logLevel)
	{
		eventQueue.addLast(new QueuedEvent(event, DataType.None, 0, logLevel));
	}
	
	public void addQueuedEvent(String event, int data, int logLevel)
	{
		eventQueue.addLast(new QueuedEvent(event, DataType.Integer, data, logLevel));
	}
	
	public synchronized void updateProcess(int processId, String newState, String[] newAllowedEvents, int logLevel)
	{
		for (int i = 0; i < processes.size(); i++)
		{
			ProcessInfo p = processes.get(i);
			if (p.id == processId)
			{
				processes.set(i, new ProcessInfo(p.id, p.object, p.name, p.events, p.states, newAllowedEvents));
				
				if (logLevel >= Level.DEBUG.intLevel())
					logger.debug(String.format("New State for Process: %s (%d): %s", p.name, p.id, newState));
				
				if (logLevel >= Level.TRACE.intLevel())
				{
					logger.trace("New allowed events:");
					
					for (int k = 0; k < newAllowedEvents.length; k++)
						logger.trace("    - " + newAllowedEvents[k]);
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
	
	private Integer[] getSensitiveProcesses(String event, int logLevel)
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
						if (!event.equals("DO") && logLevel >= Level.TRACE.intLevel())
							logger.trace(String.format("Found sensitive process '%s' (%d) on event: %s",
									processes.get(i).name, processes.get(i).id, allowedEvents[k]));
						
						tmpSensitiveProcessIDs.add(i);
						break;
					}
				}
			}
		}
		return tmpSensitiveProcessIDs.toArray(new Integer[tmpSensitiveProcessIDs.size()]);
	}
	
	private void clearAllowedEvents(int index, int logLevel)
	{
		ProcessInfo p = processes.get(index);
		
		if (logLevel >= Level.TRACE.intLevel())
			logger.trace(String.format("Clearing allowed events of process '%s' (%d)", p.name, p.id));
		
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
