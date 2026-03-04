package tankpack.enums;

public enum SingleSoundId
{
	BikeHorn("squarePressed", "bike_horn"),
	CarChirp("trianglePressed", "car_chirp"),
	Connected("foundController", "select-vehicle_ready"),
	Disconnected("lostController", "disconnected"),
	TankShot("circlePressed", "tank_shot"),
	WeaponReady("r3Pressed", "turret_ready"),
	Startup("startup", "startup"),
	Shutdown("n/a", "shutdown"), // No event linked to shutdown, just load the sound
	WifiConnected("wifiConnected", "wifi_connected"),
	WifiDisconnected("wifiDisconnected", "wifi_disconnected");
	
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