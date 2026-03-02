package tankpack;

import tankpack.enums.SingleSoundId;

public class Sm_Sound extends Sm_Sound_Generated
{
	private String triggerName;
	
	// Shared constructor
	public Sm_Sound(Driver_Communication dc, Driver_Sound ds, SingleSoundId s, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.ds = ds;
		super.vars.soundId = s.getSoundId();
		
		// Preload sound
		Driver_Sound.preload(s.getSoundId());
		
		// Define custom trigger event, assert upper case
		this.triggerName = s.getTriggerEvent().toUpperCase();
		
		initializeAndStart(dc, ":" + s.name(), debug);
	}
	
	@Override
	protected String translateEventName(String baseName)
	{
		String translatedName = baseName;
		
		if (baseName.equals("STARTSOUNDEVENT"))
			translatedName = triggerName;
		
		return translatedName;
	}
	
	@Override
	protected String translateToBaseEventName(String eventName)
	{
		String translatedName = eventName;
		
		if (eventName.equals(triggerName))
			translatedName = "STARTSOUNDEVENT";
		
		return translatedName;
	}
}
