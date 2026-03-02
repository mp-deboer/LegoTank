package tankpack;

import java.util.Arrays;

public abstract class StateMachine
{
	// State-machine variables
	private boolean debug;
	private int processId;
	private Driver_Communication dc;
	
	// Constant functions (can be used in state machine diagrams)
	final protected boolean FALSE()
	{
		return false;
	}
	
	final protected boolean TRUE()
	{
		return true;
	}
	
	final protected long DOTIME()
	{
		return Main.DOPERIOD;
	}
	
	// State-machine functions
	protected abstract void dispatchEvent(String eventName);
	protected abstract String[] getAllEventNames();
	protected abstract String[] getAllStateNames();
	protected abstract String getCurrentStateString();
	protected abstract String[] getBaseAllowedEventsForCurrentState();
	protected abstract void startSm();
	
	protected String translateEventName(String baseName)
	{
		return baseName;
	}
	
	protected String translateToBaseEventName(String eventName)
	{
		return eventName;
	}
	
	protected String[] getAllowedEventsForCurrentState()
	{
		String[] base = getBaseAllowedEventsForCurrentState();
		return Arrays.stream(base).map(this::translateEventName).toArray(String[]::new);
	}
	
	protected void updateProcess()
	{
		dc.updateProcess(processId, getCurrentStateString(), getAllowedEventsForCurrentState(), debug);
	}
	
	protected void initializeAndStart(Driver_Communication dc, boolean debug)
	{
		// No name postfix
		initializeAndStart(dc, "", debug);
	}
	
	protected void initializeAndStart(Driver_Communication dc, String namePostfix, boolean debug)
	{
		this.dc = dc;
		this.debug = debug;
		
		String[] events = getAllEventNames();
		String[] states = getAllStateNames();
		
		processId = dc.registerNewProcess(this, this.getClass().getName() + namePostfix, events, states, debug);
		
		startSm();
	}
	
	protected void dispatchEvent(String eventName, int eventData)
	{
		// If not overridden, ignore eventData
		dispatchEvent(eventName);
	}
	
	protected void fireEvent(String event)
	{
		event = event.toUpperCase();
		dc.addQueuedEvent(event, debug);
	}
	
	protected void fireEvent(String event, int data)
	{
		event = event.toUpperCase();
		dc.addQueuedEvent(event, data, debug);
	}
}
