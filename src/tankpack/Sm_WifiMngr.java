package tankpack;

import java.io.IOException;

import tankpack.util.BashCmd;
import tankpack.util.BashCmd.BashResult;

public class Sm_WifiMngr extends Sm_WifiMngr_Generated
{
	private final boolean localDebug = false;
	
	BashCmd bashCmd = new BashCmd(localDebug);
	private String ssid;
	private String password;
	
	public Sm_WifiMngr(Driver_Communication dc, boolean debug)
	{
		initializeAndStart(dc, debug);
	}
	
	@Override
	protected void readWifiSettings()
	{
		// Todo: read from file at /home/pi
		
		ssid = "TankConnect";
		password = "raspberrytank";
		
		if (localDebug)
			System.out.println("Read WiFi setting: " + ssid);
	}
	
	@Override
	protected boolean checkConnection()
	{
		boolean isConnected = false;
		
		// Check the Wi-Fi connection state
		String command = "sudo wpa_cli -i wlan0 status";
		BashResult result = executeCommand(command, true, "checkConnection");
		
		if (result != null)
			isConnected = result.stdout().contains("wpa_state=COMPLETED");
		
		if (localDebug)
			System.out.println("Connected status: " + isConnected);
		
		return isConnected;
	}
	
	@Override
	protected void scanWifi()
	{
		String command = "sudo nmcli device wifi rescan";
		executeCommand(command, false, "scanWifi");
		
		if (localDebug)
			System.out.println("Started scanning for WiFi networks");
	}
	
	@Override
	protected void connectWifi()
	{
		// Check the Wi-Fi connection state
		String command = "sudo nmcli device wifi connect " + ssid + " password " + password;
		executeCommand(command, false, "connectWifi");
		
		if (localDebug)
			System.out.println("Connecting to WiFi network: " + ssid);
	}
	
	private BashResult executeCommand(String command, boolean sync, String caller)
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
				System.err.println("Error (" + caller + "): " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
