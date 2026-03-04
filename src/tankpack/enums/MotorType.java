package tankpack.enums;

public enum MotorType
{
	LeftTrack("setSpeedLeft", "engineSpeed"),
	RightTrack("setSpeedRight", "engineSpeed"),
	Turret("setSpeedTurret", "turretSpeed");
	
	private final String speedEvent;
	private final String engineEvent;
	
	MotorType(String speedEvent, String engineEvent)
	{
		this.speedEvent = speedEvent;
		this.engineEvent = engineEvent;
	}
	
	public String getSpeedEvent()
	{
		return speedEvent;
	}
	
	public String getEngineEvent()
	{
		return engineEvent;
	}
}