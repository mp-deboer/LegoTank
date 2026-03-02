package tankpack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.util.HashMap;
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
	
	public Driver_Sound()
	{
	}
	
	public static void preload(String... soundIds)
	{
		for (String id : soundIds)
		{
			String file = BASE_PATH + id + ".wav";
			try
			{
				URL url = Driver_Sound.class.getClassLoader().getResource(file);
				AudioInputStream ais = AudioSystem.getAudioInputStream(url);
				
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
	
	public boolean isPlaying(String soundId)
	{
		Clip clip = clips.get(soundId);
		if (clip != null)
			return clip.isRunning();
		else
			return false;
	}
}
