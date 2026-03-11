package tankpack;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public abstract class StateMachine
{
	// State-machine variables
	private int logLevel;
	private int processId;
	private Driver_Communication dc;
	protected Logger logger;
	
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
		dc.updateProcess(processId, getCurrentStateString(), getAllowedEventsForCurrentState(), logLevel);
	}
	
	protected void initializeAndStart(Driver_Communication dc)
	{
		// No name postfix
		initializeAndStart(dc, "");
	}
	
	protected void initializeAndStart(Driver_Communication dc, String namePostfix)
	{
		this.dc = dc;
		
		String[] events = getAllEventNames();
		String[] states = getAllStateNames();
		
		String smName = this.getClass().getName() + namePostfix;
		logger = LogManager.getLogger(smName);
		
		// If namePostfix is set, get log level from class
		if (!namePostfix.isEmpty())
		{
			Logger tmpLogger = LogManager.getLogger(this.getClass());
			Configurator.setLevel(logger.getName(), tmpLogger.getLevel());
		}
		
		logLevel = logger.getLevel().intLevel();
		
		processId = dc.registerNewProcess(this, smName, events, states, logLevel);
		
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
		
		logger.debug("Adding event to queue: " + event);
		
		dc.addQueuedEvent(event, logLevel);
	}
	
	protected void fireEvent(String event, int data)
	{
		event = event.toUpperCase();
		
		logger.debug("Adding event to queue: " + event + " (" + data + ")");
		
		dc.addQueuedEvent(event, data, logLevel);
	}
}
