package tankpack.enums;

public enum MultiSoundId
{
	Select("selectPressed", true, "select-destination_commander", "select-driver_up", "select-good_to_go",
			"select-ready_to_roll", "select-unit_reporting", "select-vehicle_ready", "select-yes_sir");
	
	private final String triggerEvent;
	private final boolean playRandom;
	private final String[] soundIds;
	
	MultiSoundId(String trigger, boolean random, String... soundIds)
	{
		this.triggerEvent = trigger;
		this.playRandom = random;
		this.soundIds = soundIds;
	}
	
	public String getTriggerEvent()
	{
		return triggerEvent;
	}
	
	public boolean getPlayRandom()
	{
		return playRandom;
	}
	
	public String[] getSoundIds()
	{
		return soundIds;
	}
}