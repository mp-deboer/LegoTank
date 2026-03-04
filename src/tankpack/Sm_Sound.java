package tankpack;

import java.util.Random;
import tankpack.enums.MultiSoundId;
import tankpack.enums.SingleSoundId;

public class Sm_Sound extends Sm_Sound_Generated
{
	private static final Random rand = new Random();
	
	private String triggerName;
	private String[] soundIds;
	private int index;
	private boolean playRandom;
	
	// Constructor for MultiSound (random or sequential)
	public Sm_Sound(Driver_Communication dc, Driver_Sound ds, MultiSoundId s, boolean debug)
	{
		this(dc, ds, "Multi:" + s.name(), s.getTriggerEvent(), s.getSoundIds(), s.getPlayRandom(), debug);
	}
	
	// Constructor for SingleSound (treat as multi with 1 sound)
	public Sm_Sound(Driver_Communication dc, Driver_Sound ds, SingleSoundId s, boolean debug)
	{
		this(dc, ds, "Single:" + s.name(), s.getTriggerEvent(), new String[] { s.getSoundId() }, false, debug);
	}
	
	// Shared constructor
	private Sm_Sound(Driver_Communication dc, Driver_Sound ds, String name, String triggerName, String[] sounds,
			boolean random, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.ds = ds;
		
		// Preload sounds
		Driver_Sound.preload(false, sounds);
		
		// Define custom trigger event, assert upper case
		this.triggerName = triggerName.toUpperCase();
		this.soundIds = sounds;
		this.playRandom = random;
		
		initializeAndStart(dc, name, debug);
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
	
	@Override
	protected String pickSound()
	{
		String sound;
		
		if (soundIds.length == 1)
		{
			// Fixed single sound
			sound = soundIds[0];
		}
		else if (playRandom)
		{
			// Update index (to a random number within bounds of soundIds) for next call
			index = rand.nextInt(soundIds.length);
			sound = soundIds[index];
		}
		else
		{
			// Not random, increment index / play sequentially
			sound = soundIds[index];
			index = (index + 1) % soundIds.length;
		}
		
		return sound;
	}
}
