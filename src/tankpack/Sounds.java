package tankpack;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sounds
{
	private final String location = "sound/";
	
	public Sounds(CommunicationDriver cd)
	{
		// Other sounds
		SimpleSound connected = new SimpleSound(cd, "foundController", false, "connected.wav");
		SimpleSound disconnected = new SimpleSound(cd, "lostController", false, "disconnected.wav");
		SimpleSound shot = new SimpleSound(cd, "circlePressed", false, "tank_shot.wav");
		SimpleSound horn = new SimpleSound(cd, "squarePressed", false, "bike_horn.wav");
		SimpleSound chirp = new SimpleSound(cd, "trianglePressed", false, "car_chirp.wav");
		
		Thread thrConnect = new Thread(connected);
		Thread thrDisconnect = new Thread(disconnected);
		Thread thrShot = new Thread(shot);
		Thread thrHorn = new Thread(horn);
		Thread thrChirp = new Thread(chirp);
		
		thrConnect.start();
		thrDisconnect.start();
		thrShot.start();
		thrHorn.start();
		thrChirp.start();
	}
	
	public class SimpleSound extends StateMachine
	{
		// Own variables
		private boolean earlyFinish;
		private String fileLocation;
		private Clip clip;
		
		private String[] State =
		{
				"Standby", "Playing"
		};
		private String[] Event =
		{
				"triggerEvent", "Done"
		};
		private String[][] SensitivityTable =
		{
				// State1 and State2 in columns, Event1 and Event2 in rows
				// Next event / state in 'cells'
				{
						"Playing", null
				},
				{
						null, "Standby"
				}
		};
		
		public SimpleSound(CommunicationDriver cd, String trigger, boolean early, String wavefile)
		{
			this.Event[0] = trigger;
			this.Event[1] = trigger + this.Event[1];
			
			// StateMachine variables
			debug = false;
			
			super.State = this.State;
			super.Event = this.Event;
			super.SensitivityTable = this.SensitivityTable;
			
			this.cd = cd;
			currentState = State[0];
			nextEvent = null;
			
			// Declare own variables:
			earlyFinish = early;
			fileLocation = location + wavefile;
			clip = getClip();
			
			processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
					getSensitives());
		}
		
		@Override
		public void run()
		{
			while (true)
			{
				// State == Standby
				if (currentState.equals(State[0]))
				{
					waitForEvent();
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				// State == Playing
				else if (currentState.equals(State[1]))
				{
					if (earlyFinish)
					{
						sleep((int) (((double) clip.getMicrosecondLength() / 1000.0) * 0.75));
						
						// fire event Done
						fireEvent(Event[1]);
						waitForEvent();
					}
					else
					{
						while (!checkForEvent())
						{
							if (!clip.isRunning())
							{
								// fire event Done
								fireEvent(Event[1]);
								waitForEvent();
							}
						}
					}
					
					executeEvent();
					cd.updateProcess(processID, getStateNr(currentState), getSensitives());
				}
				
				else
				{
					// Unknown state error handling here
					System.out
							.println(this.getClass().getName() + ": Unknown State detected in run(): " + currentState);
					sleep(1);
				}
				
				Thread.yield();
			}
		}
		
		@Override
		public void giveEvent(String event)
		{
			nextEvent = event;
		}
		
		@Override
		protected void executeEvent()
		{
			boolean error = false;
			boolean changeState = false;
			
			// State == Standby
			if (currentState.equals(State[0]))
			{
				// Event = triggerEvent
				if (nextEvent.equals(Event[0]))
				{
					playSound();
					
					error = false;
					changeState = true;
				}
				
				else
				{
					// Event error handling here
					System.out.println(
							this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
					error = true;
				}
			}
			
			// State == Playing
			else if (currentState.equals(State[1]))
			{
				// Event = Done
				if (nextEvent.equals(Event[1]))
				{
					if (clip.isOpen()) clip.close();
					
					error = false;
					changeState = true;
				}
				
				else
				{
					// Event error handling here
					System.out.println(
							this.getClass().getName() + ": Unknown event detected in executeEvent(): " + nextEvent);
					error = true;
				}
			}
			else
			{
				// Unknown state error handling here
				System.out.println(
						this.getClass().getName() + ": Unknown State detected in executeEvent(): " + currentState);
				error = true;
			}
			
			if (!error && changeState)
			{
				currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
			}
			nextEvent = null;
		}
		
		private AudioInputStream getAudioIn(String file)
		{
			try
			{
				URL url = this.getClass().getClassLoader().getResource(file);
				// Allocate a AudioInputStream piped from a wave file
				return AudioSystem.getAudioInputStream(url);
			}
			catch (UnsupportedAudioFileException | IOException e)
			{
				e.printStackTrace();
				return null;
			}
			
		}
		
		private Clip getClip()
		{
			try
			{
				// Allocate a sound Clip resource via the static method AudioSystem.getClip()
				return AudioSystem.getClip();
			}
			catch (LineUnavailableException e)
			{
				e.printStackTrace();
				return null;
			}
			
		}
		
		private void playSound()
		{
			try
			{
				clip.open(getAudioIn(fileLocation));
				
				assertMaxVolume();
			}
			catch (LineUnavailableException | IOException e)
			{
				e.printStackTrace();
			}
			
			clip.start();
			sleep(5);
		}
		
		private void assertMaxVolume()
		{
			FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			if (volume.getMaximum() != volume.getValue())
			{
				if (debug) System.out.println("Setting volume to max: " + volume.getMaximum());
				if (debug) System.out.println("Volume min: " + volume.getMinimum());
				
				volume.setValue(volume.getMaximum());
				if (debug) System.out.println("Volume is set to: " + volume.getValue());
			}
		}
	}
}
