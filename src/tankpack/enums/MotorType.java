package tankpack.enums;

public enum MotorType
{
	LeftTrack("setSpeedLeft"), RightTrack("setSpeedRight"), Turret("setSpeedTurret");
	
	private final String speedEvent;
	
	MotorType(String speedEvent)
	{
		this.speedEvent = speedEvent;
	}
	
	public String getSpeedEvent()
	{
		return speedEvent;
	}
}