package tankpack.enums;

public enum SingleSoundId
{
	BikeHorn("squarePressed", "bike_horn"),
	CarChirp("trianglePressed", "car_chirp"),
	Connected("foundController", "connected"),
	Disconnected("lostController", "disconnected"),
	TankShot("circlePressed", "tank_shot");
	
	private final String triggerEvent;
	private final String soundId;
	
	SingleSoundId(String trigger, String soundId)
	{
		this.triggerEvent = trigger;
		this.soundId = soundId;
	}
	
	public String getTriggerEvent()
	{
		return triggerEvent;
	}
	
	public String getSoundId()
	{
		return soundId;
	}
}