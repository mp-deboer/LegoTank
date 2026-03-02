package tankpack;

import tankpack.enums.SensorPosition;

public class Sm_Sensor extends Sm_Sensor_Generated
{
	public Sm_Sensor(Driver_Communication dc, Driver_Hardware dh, SensorPosition sp, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.dh = dh;
		super.vars.sensor = sp;
		super.vars.sensorWhiteEvent = sp.name() + "White";
		super.vars.sensorBlackEvent = sp.name() + "Black";
		
		initializeAndStart(dc, sp.name(), debug);
	}
}
