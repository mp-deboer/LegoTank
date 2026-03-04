package tankpack.enums;

public enum SensorPosition
{
	Left("LEFT_ARROWPRESSED", "LEFT_ARROWRELEASED"),
	Middle("UP_ARROWPRESSED", "UP_ARROWRELEASED"),
	Right("RIGHT_ARROWPRESSED", "RIGHT_ARROWRELEASED");
	
	private final String blackEvent;
	private final String whiteEvent;
	
	SensorPosition(String blackEvent, String whiteEvent)
	{
		this.blackEvent = blackEvent;
		this.whiteEvent = whiteEvent;
	}
	
	public String getBlackEvent()
	{
		return blackEvent;
	}
	
	public String getWhiteEvent()
	{
		return whiteEvent;
	}
}