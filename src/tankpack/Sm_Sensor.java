package tankpack;

import tankpack.enums.SensorPosition;

public class Sm_Sensor extends Sm_Sensor_Generated
{
	private final boolean simulation = false;
	
	private String triggerBlack;
	private String triggerWhite;
	
	public Sm_Sensor(Driver_Communication dc, Driver_Hardware dh, SensorPosition sp)
	{
		// Inject custom variables into Generated vars
		super.vars.simulation = simulation;
		super.vars.dh = dh;
		super.vars.sensor = sp;
		super.vars.sensorWhiteEvent = sp.name() + "White";
		super.vars.sensorBlackEvent = sp.name() + "Black";
		
		// Define custom trigger event, assert upper case
		triggerBlack = sp.getBlackEvent().toUpperCase();
		triggerWhite = sp.getWhiteEvent().toUpperCase();
		
		initializeAndStart(dc, sp.name());
	}
	
	@Override
	protected String translateEventName(String baseName)
	{
		String translatedName = baseName;
		
		if (baseName.equals("SIMULATEBLACKEVENT"))
			translatedName = triggerBlack;
		else if (baseName.equals("SIMULATEWHITEEVENT"))
			translatedName = triggerWhite;
		
		return translatedName;
	}
	
	@Override
	protected String translateToBaseEventName(String eventName)
	{
		String translatedName = eventName;
		
		if (eventName.equals(triggerBlack))
			translatedName = "SIMULATEBLACKEVENT";
		else if (eventName.equals(triggerWhite))
			translatedName = "SIMULATEWHITEEVENT";
		
		return translatedName;
	}
}
