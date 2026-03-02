package tankpack;

import java.io.IOException;
import tankpack.util.BashCmd;
import tankpack.util.BashCmd.BashResult;

// Separate thread class for handling GPIO bash commands asynchronously.
// This offloads the main DO loop.
public class GpioHandler extends Thread
{
	private final boolean debug;
	private final int pinLed2;
	
	// Shared state: use volatile for visibility across threads
	private volatile boolean desiredLed2State = false;
	
	// Make sure LED2 is off upon start (this also creates a reference to the LED process)
	private volatile boolean led2StateChanged = true;
	
	// Current LED2 process (to kill when changing state)
	private Process currentLed2Process = null;
	
	private BashCmd bashCmd;
	
	public GpioHandler(int pinLed2, boolean debug)
	{
		this.pinLed2 = pinLed2;
		this.debug = debug;
		this.setName("GpioHandlerThread");
		
		bashCmd = new BashCmd(debug);
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				// Check and apply LED2 state change if needed
				if (led2StateChanged)
				{
					killGpioProcess(currentLed2Process, pinLed2);
					currentLed2Process = setGpioState(desiredLed2State, pinLed2);
					led2StateChanged = false;
				}
				
				// Sleep 200ms
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				// Ignore, only stop upon shutdownRequested
			}
		}
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
	
	////////////////////////////////////////////////////////////////////////////////
	// Bash execution methods
	////////////////////////////////////////////////////////////////////////////////
	
	private void killGpioProcess(Process gpioProcess, int gpioPin)
	{
		// Kill existing process if running (releases the GPIO line)
		if (gpioProcess != null)
		{
			if (debug)
				System.out.println("Terminating existing gpioset process");
			
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
				// Thread.currentThread().interrupt(); // Propagate interrupt
				
				if (!(e instanceof InterruptedException))
				{
					System.err.println("! Error (killGpioProcess): " + e.getMessage());
					e.printStackTrace();
				}
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
			// Thread.currentThread().interrupt(); // Propagate interrupt
			
			if (!(e instanceof InterruptedException))
			{
				System.err.println("! Error (setGpioState): " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
