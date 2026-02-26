package tankpack;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Spi;

public class HardwareDriver extends StateMachine
{
	// define SPI constants
	private final int SPICHANNEL = 0;
	private final int CLKSPEED = 500000;
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
	private final byte GETOUTPUT = (byte) 0;
	
	private final GpioController GPIO;
	private final GpioPinDigitalOutput CE; // Slave select pin
	private final GpioPinDigitalOutput LED2;
	
	// Own variables
	int value;
	
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
		GPIO = GpioFactory.getInstance();
		
		CE = GPIO.provisionDigitalOutputPin(RaspiPin.GPIO_10, "Slave Select", PinState.HIGH);
		CE.high();
		
		LED2 = GPIO.provisionDigitalOutputPin(RaspiPin.GPIO_01, "LED 2 (RPi)", PinState.HIGH);
		LED2.high();
		
		if (Spi.wiringPiSPISetup(SPICHANNEL, CLKSPEED) == -1)
		{
			System.out.println("Failed to setup the SPI.");
		}
		
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
				LED2.low();
				
				error = false;
				changeState = false;
			}
			
			// Event = setLED2off
			else if (nextEvent.equals(Event[3]))
			{
				// LED2 Off
				LED2.high();
				
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
				LED2.toggle();
				
				error = false;
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
		
		CE.setState(PinState.LOW);
		
		Spi.wiringPiSPIDataRW(SPICHANNEL, b, 1);
		
		CE.setState(PinState.HIGH);
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
		byte[] b =
		{
				GETOUTPUT
		};
		
		CE.setState(PinState.LOW);
		
		Spi.wiringPiSPIDataRW(SPICHANNEL, b, 1);
		
		CE.setState(PinState.HIGH);
		
		return b[0];
	}
}
