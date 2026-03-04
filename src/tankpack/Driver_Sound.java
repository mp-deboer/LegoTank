package tankpack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

public class Driver_Sound
{
	private static final boolean debug = false;
	private static final boolean highDebug = false;
	private static final String BASE_PATH = "sound/";
	
	// Map to store running clips. Using ConcurrentHashMap to prevent ConcurrentModificationException upon shutdown
	// Where the exception is caused due to clips closing themselves while shutdown is iterating over them in parallel
	private static final Map<String, Clip> clips = new ConcurrentHashMap<>();
	
	// Maps to store pre-loaded sound data
	private static Map<String, AudioFormat> formats = new HashMap<>();
	private static Map<String, byte[]> audioData = new HashMap<>();
	private static Map<String, Long> clipLengthMs = new HashMap<>();
	
	// Initialise fade-in / out variables
	private static final List<ActiveSound> activeSounds = new ArrayList<>();
	private static final float MAX_GAIN_DB = 6.0f;
	private static final float MIN_GAIN_DB = -24.0f;
	private static final float SILENT = -80.0f;
	private static final long ALMOSTDONETHRESHOLD = 2500L; // Amount of ms before 'done'
	
	public Driver_Sound()
	{
	}
	
	public static void preload(boolean loop, String... soundIds)
	{
		for (String id : soundIds)
		{
			String file = BASE_PATH + id + ".wav";
			try
			{
				URL url = Driver_Sound.class.getClassLoader().getResource(file);
				AudioInputStream ais = AudioSystem.getAudioInputStream(url);
				
				// If pre-loading a clip intended for looping, add to active sounds and keep the clip running
				if (loop)
				{
					Clip clip = AudioSystem.getClip();
					clip.open(ais);
					
					// Add to activeSounds at silent volume (kept running)
					ActiveSound as = new ActiveSound(clip, id, SILENT); // Stay silent until faded in
					as.clip.loop(Clip.LOOP_CONTINUOUSLY); // Start looping forever
					activeSounds.add(as);
				}
				else // If pre-loading a clip intended for "playOneShot", only pre-load the data & close the clip
				{
					AudioFormat format = ais.getFormat();
					byte[] data = ais.readAllBytes();
					
					formats.put(id, format);
					audioData.put(id, data);
					
					// Temporarily open clip to get its length (amount of DO cycles), then close right away
					Clip clip = AudioSystem.getClip();
					clip.open(format, data, 0, data.length);
					clipLengthMs.put(id, (clip.getMicrosecondLength() / 1000L));
					clip.close();
					
					if (highDebug)
						System.out.println("Measured length in ms of clip '" + id + "': " + clipLengthMs.get(id));
				}
				ais.close();
				
				if (debug)
					System.out.println("Preloaded file: " + file);
			}
			catch (Exception e)
			{
				if (debug)
					System.out.println("Failed to load file: " + file);
				e.printStackTrace();
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// One shot / single run functions
	////////////////////////////////////////////////////////////////////////////////
	
	public void playOneShot(String soundId)
	{
		if (formats.containsKey(soundId))
		{
			try
			{
				Clip clip = AudioSystem.getClip();
				AudioFormat format = formats.get(soundId);
				byte[] data = audioData.get(soundId);
				clip.open(format, data, 0, data.length);
				
				// Auto-close when playback naturally ends (or is stopped early)
				clip.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.STOP)
					{
						clip.close();
						clips.remove(soundId); // Clean map entry
						if (debug)
							System.out.println("Clip auto-closed: " + soundId);
					}
				});
				
				assertMaxVolume(clip);
				clip.start();
				
				clips.put(soundId, clip);
				
				if (debug)
					System.out.println("Started clip mapped to: " + soundId);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private static void assertMaxVolume(Clip clip)
	{
		FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		if (volume.getMaximum() != volume.getValue())
		{
			if (debug)
				System.out.println("Setting volume to max: " + volume.getMaximum());
			
			volume.setValue(volume.getMaximum());
		}
	}
	
	public boolean almostDone(String soundId, long playTime)
	{
		Clip clip = clips.get(soundId);
		if (clip != null && clipLengthMs.containsKey(soundId))
		{
			long clipLength = clipLengthMs.get(soundId);
			
			if (highDebug)
				System.out.printf("Time remaining for '%s': %4d\r", soundId,
						Math.max((clipLength - playTime - ALMOSTDONETHRESHOLD), 0));
			
			// Return true when clip will take less than ALMOSTDONETHRESHOLD to finish or when clip is not running
			return (playTime >= (clipLength - ALMOSTDONETHRESHOLD)) || !clip.isRunning();
		}
		else
			// If the clip has already been closed / does not exist, then it is done (return true)
			return true;
	}
	
	public boolean isPlaying(String soundId)
	{
		Clip clip = clips.get(soundId);
		if (clip != null)
			return clip.isRunning();
		else
			return false;
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// Loop functionality
	////////////////////////////////////////////////////////////////////////////////
	
	// Custom class for sound fading
	private static class ActiveSound
	{
		Clip clip;
		String id;
		float targetGainDB;
		float currentGainDB;
		float fadeStep;
		
		ActiveSound(Clip clip, String id, float startGainDB)
		{
			this.clip = clip;
			this.id = id;
			this.currentGainDB = startGainDB;
			this.targetGainDB = startGainDB;
			this.fadeStep = MAX_GAIN_DB - MIN_GAIN_DB; // set to max by default
			setVolume();
		}
		
		void setVolume()
		{
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), currentGainDB)));
			}
		}
		
		// Apply fadeStep and set volume
		void updateVolume()
		{
			if (currentGainDB != targetGainDB)
			{
				currentGainDB = calculateNextGain(currentGainDB, targetGainDB, fadeStep);
				
				if (debug)
					if (currentGainDB == targetGainDB) // separate if statement to fix "dead code" warning
						System.out.println("Volume of '" + this.id + "' reached target: " + currentGainDB);
					
				if (highDebug)
					if (currentGainDB != targetGainDB) // separate if statement to fix "dead code" warning
						System.out.printf("Volume of '%s' set to: %.2f; fadeStep = %.2f\n", id, currentGainDB,
								fadeStep);
					
				setVolume();
			}
			
			// Snap to SILENT once currentGainDB is at (or below) MIN
			if ((currentGainDB != SILENT) && (currentGainDB <= MIN_GAIN_DB) && (targetGainDB <= MIN_GAIN_DB))
			{
				currentGainDB = SILENT;
				targetGainDB = SILENT;
				setVolume();
				
				if (debug)
					System.out.println("Faded out and silenced: " + id);
			}
		}
	}
	
	public void update()
	{
		Iterator<ActiveSound> it = activeSounds.iterator();
		while (it.hasNext())
		{
			ActiveSound as = it.next();
			as.updateVolume();
		}
	}
	
	public void fadeIn(String soundId, float fadeInTime)
	{
		// Loops are already running as starting and stopping them on a Raspberry PI causes the system to hang.
		// Therefore, just set targetGain to max for fading them back in.
		
		for (ActiveSound as : activeSounds)
		{
			if (as.id.equals(soundId))
			{
				// If targetGainDB is already set to MAX, skip
				if (as.targetGainDB != MAX_GAIN_DB)
				{
					// Jump from SILENT to MIN_GAIN_DB, then fade to MAX via update calls
					as.currentGainDB = MIN_GAIN_DB;
					as.targetGainDB = MAX_GAIN_DB;
					
					// Calculate fadeStep
					as.fadeStep = calculateFadeStep(fadeInTime);
				}
				
				// If fade-in time is set to 0 or lower, set volume to max (no fade in)
				if (fadeInTime <= 0f)
				{
					as.currentGainDB = MAX_GAIN_DB - 1;
					as.targetGainDB = MAX_GAIN_DB;
					
					as.fadeStep = 1;
				}
				
				// Update and set volume // execute first fade step
				as.updateVolume();
				
				return;
			}
		}
		
		// If not found, log error
		if (debug)
			System.out.println("Loop not preloaded: " + soundId);
	}
	
	public void fadeOut(String soundId, float fadeOutTime)
	{
		// If active, set target volume to MIN (fade out)
		for (ActiveSound as : activeSounds)
		{
			if (as.id.equals(soundId))
			{
				// If targetGainDB is already set to SILENT, skip
				if (as.targetGainDB != SILENT)
				{
					as.targetGainDB = MIN_GAIN_DB;
					
					// If fadeOutTime is 0 or lower, set volume to MIN (no fade out)
					if (fadeOutTime <= 0f)
						as.currentGainDB = MIN_GAIN_DB;
					else
						// Calculate fadeStep
						as.fadeStep = calculateFadeStep(fadeOutTime);
					
					// Update and set volume
					as.updateVolume();
				}
				return;
			}
		}
	}
	
	private float calculateFadeStep(float fadeTime)
	{ // fadeTime in milliseconds
		if (fadeTime <= Main.DOPERIOD)
			return (MAX_GAIN_DB - MIN_GAIN_DB);
		
		float steps = (fadeTime) / (float) Main.DOPERIOD;
		return (MAX_GAIN_DB - MIN_GAIN_DB) / steps;
	}
	
	private static float calculateNextGain(float current, float target, float step)
	{
		float diff = target - current;
		float nextGain = current + Math.signum(diff) * step;
		
		if (Math.signum(diff) != Math.signum(target - nextGain))
			nextGain = target; // Overshoot fix
			
		return nextGain;
	}
}
