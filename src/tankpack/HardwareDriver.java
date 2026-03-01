package tankpack;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class HardwareDriver extends StateMachine
{
	// define LED constants
	private final byte LED1ON = (byte) 10;
	private final byte LED1OFF = (byte) 11;
	private final byte LED1TOGGLE = (byte) 12;
	// define Motor constants
	private final byte MAXSPEED = (byte) 31;
	private final byte TURRET = (byte) 64;
	private final byte LEFTTRACK = (byte) 128;
	private final byte RIGHTTRACK = (byte) 192;
	private final byte FORWARD = (byte) 0;
	private final byte BACKWARD = (byte) 32;
	private final byte HALT = (byte) 0;
	// define sensor constants
	private final byte IR1 = (byte) 1;
	private final byte IR2 = (byte) 2;
	private final byte IR3 = (byte) 3;
	
	private static final int PIN_LED = 18; // GPIO_1 / wPi = 1; BCM = 18
	
	// Own variables
	int value;
	FileOutputStream fos;
	FileInputStream fis;
	
	// State-machine variables
	boolean boolData;
	int intData;
	
	private String[] State =
	{
			"Driver"
	};
	private String[] Event =
	{
			"setLED1on",
			"setLED1off",
			"setLED2on",
			"setLED2off",
			"setLeftTrackSpeed",
			"setRightTrackSpeed",
			"setTurretSpeed",
			"toggleLED1",
			"toggleLED2"
	};
	private String[][] SensitivityTable =
	{
			// State1 and State2 in columns, Event1 and Event2 in rows
			// Next state in 'cells'
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			},
			{
					"Driver"
			}
	};
	
	public HardwareDriver(CommunicationDriver cd)
	{
		// StateMachine variables
		debug = false;
		super.State = this.State;
		super.Event = this.Event;
		super.SensitivityTable = this.SensitivityTable;
		
		this.cd = cd;
		currentState = State[0];
		nextEvent = null;
		boolData = false;
		intData = 0;
		
		// Declare own variables:
		value = 0;
		try
		{
			fos = new FileOutputStream("/dev/spidev0.0");
			fis = new FileInputStream("/dev/spidev0.0");
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Error: Unable to open /dev/spidev0.0.");
		}
		
		// Initialise LED state
		setLed2(false, debug);
		
		processID = cd.registerNewProcess(this, this.getClass().getName(), Event, State, getStateNr(currentState),
				getSensitives());
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// State == Driver
			if (currentState.equals(State[0]))
			{
				waitForEvent();
				executeEvent();
				cd.updateProcess(processID, getStateNr(currentState), getSensitives());
			}
			
			else
			{
				// Unknown state error handling here
				System.out.println(this.getClass().getName() + ": Unknown State detected in run(): " + currentState);
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
	public void giveEvent(String event, boolean data)
	{
		nextEvent = event;
		boolData = data;
	}
	
	@Override
	public void giveEvent(String event, int data)
	{
		nextEvent = event;
		intData = data;
	}
	
	@Override
	protected void executeEvent()
	{
		boolean error = false;
		boolean changeState = false;
		
		// State == Driver
		if (currentState.equals(State[0]))
		{
			// Event = setLED1on
			if (nextEvent.equals(Event[0]))
			{
				send(LED1ON);
				
				error = false;
				changeState = false;
			}
			
			// Event = setLED1off
			else if (nextEvent.equals(Event[1]))
			{
				send(LED1OFF);
				
				error = false;
				changeState = false;
			}
			
			// Event = setLED2on
			else if (nextEvent.equals(Event[2]))
			{
				// LED2 on
				setLed2(true, debug);
				
				error = false;
				changeState = false;
			}
			
			// Event = setLED2off
			else if (nextEvent.equals(Event[3]))
			{
				// LED2 Off
				setLed2(false, debug);
				
				error = false;
				changeState = false;
			}
			
			// Event = setLeftTrackSpeed
			else if (nextEvent.equals(Event[4]))
			{
				send(determineByte(LEFTTRACK, intData));
				
				error = false;
				changeState = false;
			}
			
			// Event = setRightTrackSpeed
			else if (nextEvent.equals(Event[5]))
			{
				send(determineByte(RIGHTTRACK, intData));
				
				error = false;
				changeState = false;
			}
			
			// Event = setTurretSpeed
			else if (nextEvent.equals(Event[6]))
			{
				send(determineByte(TURRET, intData));
				
				error = false;
				changeState = false;
			}
			
			// Event = toggleLED1
			else if (nextEvent.equals(Event[7]))
			{
				send(LED1TOGGLE);
				
				error = false;
				changeState = false;
			}
			
			// Event = toggleLED2
			else if (nextEvent.equals(Event[8]))
			{
				// unsupported
				// LED2.toggle();
				
				error = true;
				changeState = false;
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
			System.out
					.println(this.getClass().getName() + ": Unknown State detected in executeEvent(): " + currentState);
			error = true;
		}
		
		if (!error && changeState)
		{
			currentState = SensitivityTable[getEventNr(nextEvent)][getStateNr(currentState)];
		}
		nextEvent = null;
	}
	
	private void send(byte cmd)
	{
		byte[] b =
		{
				cmd
		};
		
		try
		{
			fos.write(b);
		}
		catch (IOException e)
		{
			System.out.println("Error: Failed to write byte.");
		}
	}
	
	private byte determineByte(byte track, int speed)
	{
		// adjust speed percentage to MAXSPEED resolution (also, s must be
		// between 0 and -MAXSPEED / +MAXSPEED after this)
		
		if (speed == 0)
		{
			return (byte) (track + HALT);
		}
		else if (speed > 0 && speed <= (100 / MAXSPEED))
		{
			return (byte) (track + FORWARD + 1);
		}
		else if (speed < 0 && speed >= (-100 / MAXSPEED))
		{
			return (byte) (track + BACKWARD + 1);
		}
		else if (speed > 100 || speed < -100)
		{
			System.err.println("Error: Speed must be between -100 and 100!");
			return (byte) (track + HALT);
		}
		else
		{
			speed = (int) ((double) speed / 100 * MAXSPEED);
			if (speed < 0)
			{
				// make speed positive
				speed *= -1;
				
				return (byte) (track + BACKWARD + speed);
			}
			else if (speed > 0)
			{
				return (byte) (track + FORWARD + speed);
			}
			else
			{
				System.err.println("Error: Something is wrong with your speed determining.");
				return 0;
			}
		}
	}
	
	// Line Follower commands:
	public void sendByte(byte track, int speed)
	{
		send(determineByte(track, speed));
	}
	
	public byte readSensor(SensorPosition p)
	{
		if (p == SensorPosition.Right)
		{
			send(IR1);
		}
		else if (p == SensorPosition.Middle)
		{
			send(IR2);
		}
		else
		// if p == SensorPosition.Left
		{
			send(IR3);
		}
		
		// Get and return sensor output
		return receive();
	}
	
	private byte receive()
	{
		try
		{
			value = fis.read();
			if (value == -1)
			{
				throw new IOException("Unexpected EOF");
			}
		}
		catch (IOException e)
		{
			System.out.println("Error: Receive failed.");
			return (byte) -1; // Caller checks if (receive() & 0xFF) == 255 for error, assuming 255 isn't valid data
		}
		return (byte) value;
	}
	
	// Function to set state of LED via Bash command, which requires killing the process afterwards
	private void setLed2(boolean state, boolean verbose)
	{
		String command = "gpioset -l -c gpiochip0 " + PIN_LED + "=" + (state ? "1" : "0");
		
		// Announce command to be executed
		if (verbose) System.out.println("+ " + command);
		
		try
		{
			// Run command, add ', "-x"' to run in verbose mode
			String[] cmdArray =
			{
					"bash", "-c", command
			};
			Process process = Runtime.getRuntime().exec(cmdArray);
			
			// Read stderr only for errors (non-blocking with timeout)
			new Thread(() -> {
				try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
				{
					String line;
					while ((line = stderr.readLine()) != null)
					{
						// Print stderr to Java's stderr
						System.err.println("gpioset error: " + line);
					}
					
					// Close streams
					stderr.close();
				}
				catch (IOException e)
				{
					// ignore
				}
			}).start();
			
			// Short delay to allow set
			sleep(300);
			
			// Finally, kill the process
			process.destroy();
		}
		catch (IOException e)
		{
			System.err.println("Error (setLed): " + e.getMessage());
			e.printStackTrace();
		}
		
		return;
	}
}