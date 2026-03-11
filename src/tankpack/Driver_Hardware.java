package tankpack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tankpack.enums.MotorType;
import tankpack.enums.SensorPosition;

public class Driver_Hardware
{
	// define GPIO constants
	private final int PIN_LED2 = 18; // GPIO_1 / wPi = 1; BCM = 18
	private final int PIN_BTN = 4; // GPIO_7 / wPi = 7; BCM = 4
	
	// define LED1 constants
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
	
	// Own variables
	private GpioHandler gpioHandler = new GpioHandler(PIN_BTN, PIN_LED2);
	FileOutputStream fos;
	FileInputStream fis;
	
	private static final Logger logger = LogManager.getLogger(Driver_Hardware.class);
	
	public Driver_Hardware()
	{
	}
	
	public void initialise()
	{
		// Initialise SPI channel with retry
		int retryDelayMs = 1000; // wait time between attempts
		int attempt = 0;
		File spiDev = new File("/dev/spidev0.0");
		
		while (true)
		{
			try
			{
				if (spiDev.exists())
				{
					fos = new FileOutputStream(spiDev);
					fis = new FileInputStream(spiDev);
					logger.info("SPI channel " + spiDev.getPath() + " opened successfully.");
					break; // success → exit loop
				}
				else
				{
					attempt++;
					logger.error("SPI device not found yet (attempt " + attempt + "). Retrying in " + retryDelayMs
							+ "ms...");
				}
			}
			catch (FileNotFoundException e)
			{
				attempt++;
				logger.error(
						"SPI device not ready yet (attempt " + attempt + "). Retrying in " + retryDelayMs + "ms...");
			}
			
			try
			{
				Thread.sleep(retryDelayMs);
			}
			catch (InterruptedException ie)
			{
				Thread.currentThread().interrupt(); // Propagate interrupt
				logger.error("Interrupted while trying to open SPI device", ie);
			}
		}
	}
	
	public void start()
	{
		// Start GpioHandler thread
		gpioHandler.start();
	}
	
	public void shutdown()
	{
		// Set speed of all motors to 0
		for (int i = 0; i < MotorType.values().length; i++)
			setMotorSpeed(MotorType.values()[i], 0);
		
		// Turn LEDs off
		setLed1Off();
		setLed2Off();
		
		// Close SPI streams
		try
		{
			fos.close();
			fis.close();
		}
		catch (IOException e)
		{
			logger.error("Failed to close SPI streams.");
		}
		
		// Wait for LED2 off to take effect
		simplySleep(500L);
		
		// To allow a clean shutdown, shutdown GPIO handler as well
		gpioHandler.requestShutdown();
		try
		{
			gpioHandler.join();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt(); // Propagate interrupt
		}
		
		logger.debug("Hardware shutdown complete.");
	}
	
	// Sleep without requiring try/catch
	private void simplySleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt(); // Propagate interrupt
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// Button & LED2 functions
	////////////////////////////////////////////////////////////////////////////////
	
	public void setLed2Off()
	{
		gpioHandler.setDesiredLed2State(false);
	}
	
	public void setLed2On()
	{
		gpioHandler.setDesiredLed2State(true);
	}
	
	public boolean getButtonState()
	{
		return gpioHandler.getButtonState();
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// SPI functions
	////////////////////////////////////////////////////////////////////////////////
	
	public void setLed1Off()
	{
		send(LED1OFF);
	}
	
	public void setLed1On()
	{
		send(LED1ON);
	}
	
	public void toggleLed1()
	{
		send(LED1TOGGLE);
	}
	
	public void setMotorSpeed(MotorType motor, int speed)
	{
		switch (motor)
		{
			case LeftTrack -> send(determineByte(LEFTTRACK, speed));
			case RightTrack -> send(determineByte(RIGHTTRACK, speed));
			case Turret -> send(determineByte(TURRET, speed));
			default -> logger.error("setMotorSpeed not configured for MotorType " + motor);
		}
	}
	
	private void send(byte cmd)
	{
		byte[] b = { cmd };
		
		try
		{
			fos.write(b);
		}
		catch (IOException e)
		{
			logger.error("Failed to write byte.");
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
			logger.error("Speed must be between -100 and 100!");
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
				logger.error("Something is wrong with your speed determining.");
				return 0;
			}
		}
	}
	
	public int readSensor(SensorPosition sensor)
	{
		switch (sensor)
		{
			case Right -> send(IR1);
			case Middle -> send(IR2);
			case Left -> send(IR3);
			default -> logger.error("readSensor not configured for SensorPosition " + sensor);
		}
		
		// Get and return sensor output, convert unsigned byte to unsigned int
		return Byte.toUnsignedInt(receive());
	}
	
	private byte receive()
	{
		int value;
		
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
			logger.error("Receive failed.");
			return (byte) -1; // Caller checks if (receive() & 0xFF) == 255 for error, assuming 255 isn't valid data
		}
		return (byte) value;
	}
}