package tankpack;

import java.io.IOException;

import tankpack.util.BashCmd;
import tankpack.util.BashCmd.BashResult;

public class Sm_WifiMngr extends Sm_WifiMngr_Generated
{
	BashCmd bashCmd;
	private String ssid;
	private String password;
	
	public Sm_WifiMngr(Driver_Communication dc)
	{
		initializeAndStart(dc);
		bashCmd = new BashCmd(super.logger);
	}
	
	@Override
	protected void readWifiSettings()
	{
		// Todo: read from file at /home/pi
		
		ssid = "TankConnect";
		password = "raspberrytank";
		
		super.logger.debug("Read WiFi setting: " + ssid);
	}
	
	@Override
	protected boolean checkConnection()
	{
		boolean isConnected = false;
		
		// Check the Wi-Fi connection state
		String command = "sudo wpa_cli -i wlan0 status";
		BashResult result = executeCommand(command, true);
		
		if (result != null)
			isConnected = result.stdout().contains("wpa_state=COMPLETED");
		
		super.logger.debug("Connected status: " + isConnected);
		
		return isConnected;
	}
	
	@Override
	protected void scanWifi()
	{
		String command = "sudo nmcli device wifi rescan";
		executeCommand(command, false);
		
		super.logger.debug("Started scanning for WiFi networks");
	}
	
	@Override
	protected void connectWifi()
	{
		// Check the Wi-Fi connection state
		String command = "sudo nmcli device wifi connect " + ssid + " password " + password;
		executeCommand(command, false);
		
		super.logger.debug("Connecting to WiFi network: " + ssid);
	}
	
	private BashResult executeCommand(String command, boolean sync)
	{
		try
		{
			return bashCmd.executeBashCommand(command, sync);
		}
		catch (IOException | InterruptedException e)
		{
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt(); // propagate interrupt
			else // another error occurred
			{
				super.logger.error("Command '" + command + "' resulted in error: " + e.getMessage(), e);
			}
		}
		
		return null;
	}
}
