package tankpack;

public class Main
{
	public static void main(String[] args)
	{
		@SuppressWarnings("unused")
		Main m = new Main();
	}
	
	public Main()
	{
		CommunicationDriver cd = new CommunicationDriver();
		
		// Start HWpermit before other processes to prevent false event-allows
		Mode mode = new Mode(cd);
		
		// Make a controller so other threads can get data from it
		PsController psController = new PsController(cd);
		
		// Buttons will start a thread for each button
		@SuppressWarnings("unused")
		Buttons buttons = new Buttons(cd, psController);
		
		// Tracks will start the threads necessary to control and move the tracks
		@SuppressWarnings("unused")
		Tracks tracks = new Tracks(cd, psController);
		
		// Turret will start the threads necessary to control the Turret
		@SuppressWarnings("unused")
		Turret turret = new Turret(cd, psController);
		
		// Motors will start the 3 Motor threads (left/right track and turret)
		@SuppressWarnings("unused")
		Motors mtrs = new Motors(cd);
		
		// Sounds will start all sound threads, reacting on the events
		@SuppressWarnings("unused")
		Sounds sounds = new Sounds(cd);
		
		HardwareDriver hwDr = new HardwareDriver(cd);
		Led2 led2 = new Led2(cd);
		
		// Add the non-concurrent Line Follow Thread
		LineFollower lF = new LineFollower(cd, hwDr);
		
		Thread thrHwDr = new Thread(hwDr);
		Thread thrMode = new Thread(mode);
		Thread thrLed2 = new Thread(led2);
		Thread thrPsController = new Thread(psController);
		Thread thrLF = new Thread(lF);
		
		thrHwDr.start();
		thrMode.start();
		thrLed2.start();
		thrPsController.start();
		thrLF.start();
		
		// cd.printAllProcesses();
	}
}
