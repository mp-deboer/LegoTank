package tankpack;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tankpack.util.BashCmd;
import tankpack.util.BashCmd.BashResult;

// Separate thread class for handling GPIO bash commands asynchronously.
// This offloads the main DO loop.
public class GpioHandler extends Thread
{
	private final int pinBtn;
	private final int pinLed2;
	private final long pollIntervalMs;
	
	private static final Logger logger = LogManager.getLogger(GpioHandler.class);
	
	// Shared state: use volatile for visibility across threads
	private volatile boolean currentButtonState = false;
	private volatile boolean desiredLed2State = false;
	
	// Make sure LED2 is off upon start (this also creates a reference to the LED process)
	private volatile boolean led2StateChanged = true;
	
	// Current LED2 process (to kill when changing state)
	private Process currentLed2Process = null;
	
	// Shutdown flag
	private volatile boolean shutdownRequested = false;
	
	private BashCmd bashCmd;
	
	public GpioHandler(int pinBtn, int pinLed2)
	{
		this.pinBtn = pinBtn;
		this.pinLed2 = pinLed2;
		this.pollIntervalMs = 200L; // set to 200ms polling
		this.setName(this.getClass().getSimpleName());
		
		bashCmd = new BashCmd(logger);
	}
	
	@Override
	public void run()
	{
		while (!shutdownRequested)
		{
			try
			{
				// Poll button state synchronously
				currentButtonState = getGpioState(pinBtn);
				
				// Check and apply LED2 state change if needed
				if (led2StateChanged)
				{
					killGpioProcess(currentLed2Process, pinLed2);
					currentLed2Process = setGpioState(desiredLed2State, pinLed2);
					led2StateChanged = false;
				}
				
				// Sleep for next poll cycle
				Thread.sleep(pollIntervalMs);
			}
			catch (InterruptedException e)
			{
				// Ignore, only stop upon shutdownRequested
			}
		}
		
		// Cleanup on shutdown
		killGpioProcess(currentLed2Process, pinLed2);
	}
	
	// Public getter for button state (read by Driver_Hardware.getButtonState())
	public boolean getButtonState()
	{
		return currentButtonState;
	}
	
	// Public setter for LED2 state (called by Driver_Hardware.setLed2On/Off())
	public void setDesiredLed2State(boolean state)
	{
		if (state != desiredLed2State)
		{
			desiredLed2State = state;
			led2StateChanged = true;
		}
	}
	
	// Request shutdown (called from Driver_Hardware.shutdown())
	public void requestShutdown()
	{
		shutdownRequested = true;
		this.interrupt(); // Interrupt sleep if waiting
	}
	
	////////////////////////////////////////////////////////////////////////////////
	// Bash execution methods
	////////////////////////////////////////////////////////////////////////////////
	
	private void killGpioProcess(Process gpioProcess, int gpioPin)
	{
		// Kill existing process if running (releases the GPIO line)
		if (gpioProcess != null)
		{
			logger.debug("Terminating existing gpioset process");
			
			gpioProcess.destroy();
			
			try
			{
				gpioProcess.waitFor(); // Wait for clean exit/release
			}
			catch (InterruptedException e)
			{
				// Ignore
				// Thread.currentThread().interrupt(); // Propagate interrupt
			}
			gpioProcess = null;
		}
		else
		{
			// First call, assert gpioset command is not running / GPIO line of PIN_LED is free
			String killCommand = "pkill -f \"gpioset.*" + gpioPin + "\"";
			
			try
			{
				// Run command & wait for completion
				bashCmd.executeBashCommand(killCommand, true);
			}
			catch (Exception e)
			{
				// ignore if interrupt
				if (!(e instanceof InterruptedException))
					logger.error(e.getMessage(), e);
			}
		}
	}
	
	// Function to set state of LED via Bash command, which requires killing the process afterwards
	private Process setGpioState(boolean state, int pin)
	{
		String command = "gpioset -l -c gpiochip0 " + pin + "=" + (state ? "1" : "0");
		
		try
		{
			// Execute bash command without waiting for completion
			BashResult result = bashCmd.executeBashCommand(command, false);
			return result.process();
		}
		catch (IOException | InterruptedException e)
		{
			// ignore if interrupt
			if (!(e instanceof InterruptedException))
				logger.error(e.getMessage(), e);
		}
		
		return null;
	}
	
	// Function to get state of BTN via Bash command
	private boolean getGpioState(int pin)
	{
		String command = "gpioget -l --numeric -c gpiochip0 " + pin;
		
		try
		{
			// Execute bash command and wait for completion
			BashResult result = bashCmd.executeBashCommand(command, true);
			
			// Return true if stdout equals to "1"
			if (!result.stdout().isEmpty())
				return result.stdout().get(0).equals("1");
			else // empty stdout, return false
				return false;
		}
		catch (IOException | InterruptedException e)
		{
			// ignore if interrupt
			if (!(e instanceof InterruptedException))
				logger.error(e.getMessage(), e);
		}
		
		return false;
	}
}
