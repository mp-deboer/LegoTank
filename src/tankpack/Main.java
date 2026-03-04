package tankpack;

import java.lang.Math;

import tankpack.enums.*;

public class Main
{
	public static final long DOPERIOD = 100L; // changing this requires recompiling all
	private static boolean debug; // set via argument
	private boolean lfOn = false;
	private boolean shutdownHook = false;
	
	// Objects shared between functions:
	private Driver_Communication dc;
	private Driver_Hardware dh;
	private Driver_PsController dpc;
	private Driver_Sound ds;
	private Sm_LineFollower lF;
	
	public static void main(String[] args)
	{
		if (args.length > 0)
			debug = Boolean.parseBoolean(args[0]); // Parses "true" (case-insensitive) as true
		else
			debug = false;
		
		@SuppressWarnings("unused")
		Main m = new Main();
	}
	
	// Suppress unused warnings for complete scope of Main()
	@SuppressWarnings("unused")
	private Main()
	{
		// Initialise drivers
		Driver_Communication dc = new Driver_Communication();
		Driver_PsController dpc = new Driver_PsController();
		Driver_Hardware dh = new Driver_Hardware();
		Driver_Sound ds = new Driver_Sound();
		dh.initialise();
		
		// Initialise PsController State Machine
		Sm_PsController psController = new Sm_PsController(dc, dpc, false);
		
		// For each PS button, create a button StateMachine
		Sm_PsButton[] buttons = new Sm_PsButton[PsComponent.BUTTONAMOUNT];
		for (int i = 0; i < PsComponent.BUTTONAMOUNT; i++)
			buttons[i] = new Sm_PsButton(dc, dpc, PsComponent.values()[i], false);
		
		// Initialise a joystick StateMachine for each PS joystick
		Sm_PsJoystick joystickLeft = new Sm_PsJoystick(dc, dpc, PsComponent.Leftstick_X, PsComponent.Leftstick_Y,
				false);
		Sm_PsJoystick joystickRight = new Sm_PsJoystick(dc, dpc, PsComponent.Rightstick_X,
				null /* we are not interested in the right Y-axis */, false);
		
		// Initialise a stick StateMachine for each PS stick
		Sm_PsStick stickL2 = new Sm_PsStick(dc, dpc, PsComponent.L2_Sensor, false);
		Sm_PsStick stickR2 = new Sm_PsStick(dc, dpc, PsComponent.R2_Sensor, false);
		
		// Initialise Tracks & Turret State machines
		Sm_Tracks tracks = new Sm_Tracks(dc, false);
		Sm_Turret turret = new Sm_Turret(dc, false);
		
		// Initialise sound objects
		// For each SingleSoundId, create a Sound object
		Sm_Sound[] singleSounds = new Sm_Sound[SingleSoundId.values().length];
		for (int i = 0; i < SingleSoundId.values().length; i++)
			singleSounds[i] = new Sm_Sound(dc, ds, SingleSoundId.values()[i], false);
		
		// For each MultiSoundId, create a Sound object
		Sm_Sound[] multiSounds = new Sm_Sound[MultiSoundId.values().length];
		for (int i = 0; i < MultiSoundId.values().length; i++)
			multiSounds[i] = new Sm_Sound(dc, ds, MultiSoundId.values()[i], false);
		
		// Special sound state machines
		Sm_Engine engine = new Sm_Engine(dc, ds, false);
		Sm_TurretDrive turretDrive = new Sm_TurretDrive(dc, ds, false);
		
		// Initialise LineFollowing objects
		// For each SensorPosition, create a Sensor object
		Sm_Sensor[] sensors = new Sm_Sensor[SensorPosition.values().length];
		for (int i = 0; i < SensorPosition.values().length; i++)
			sensors[i] = new Sm_Sensor(dc, dh, SensorPosition.values()[i], false);
		Sm_LineFollower lF = new Sm_LineFollower(dc, sensors, false);
		
		// Initialise hardware objects
		Sm_Led1 led1 = new Sm_Led1(dc, dh, false);
		Sm_Led2 led2 = new Sm_Led2(dc, dh, false);
		
		// For each MotorType, create a Motor object
		Sm_Motor[] motors = new Sm_Motor[MotorType.values().length];
		for (int i = 0; i < MotorType.values().length; i++)
			motors[i] = new Sm_Motor(dc, dh, MotorType.values()[i], false);
		
		// Done, print all registered processes and start DO loop
		dc.printAllProcesses();
		
		// Save objects used at other functions locally
		this.dc = dc;
		this.dh = dh;
		this.dpc = dpc;
		this.ds = ds;
		this.lF = lF;
		
		// Add startup event, executed at first DO, triggering related sound
		dc.addQueuedEvent("STARTUP", false);
		
		// Start GpioHandler
		dh.start();
		
		// Start DO loop
		timedFireDoEvent();
	}
	
	private void timedFireDoEvent()
	{
		// First, get reference to current thread and add a CTRL-C hook
		Thread myThread = Thread.currentThread();
		addShutdownHook(myThread);
		
		// Then, initialise and start (interruptible) DO loop
		long period = DOPERIOD;
		long sleepTime = period;
		
		long start;
		long end;
		long duration;
		
		// Run until thread is interrupted
		try
		{
			while (!myThread.isInterrupted())
			{
				if (sleepTime < 0)
				{
					// Set start time before print
					start = System.currentTimeMillis();
					
					System.out.println("! Warning: Event DO took longer than intended (" + Math.abs(sleepTime)
							+ "ms longer than " + period + "ms).");
				}
				else
				{
					// Sleep the calculated time
					Thread.sleep(sleepTime);
					
					// Set start time after sleep
					start = System.currentTimeMillis();
				}
				
				// Print DO loop time. Add a carriage to prevent spamming lines
				// When LineFollower is running and in debug mode, append its debug message
				if (debug)
				{
					System.out.printf("DO loop time: %3d", (period - sleepTime));
					
					if (lF.localDebug)
					{
						if (!lF.stateId.name().equals("IDLE"))
						{
							lfOn = true;
							System.out.printf("%s", "; " + lF.debugMessage);
						}
						else if (lfOn)
						{
							// When LineFollow state is back to IDLE:
							// Add new line to prevent having partly overwritten LF debug / fix weird console lines
							lfOn = false;
							System.out.println();
						}
					}
					System.out.printf("\r");
				}
				
				ds.update(); // also call sound update every tick
				dc.executeDoEvent();
				
				// Measure execution time
				end = System.currentTimeMillis();
				duration = end - start;
				
				// Calculate sleep time for next iteration
				sleepTime = period - duration;
			}
		}
		catch (InterruptedException e)
		{
			System.out.println(e.getMessage());
		}
		
		shutdown();
		
		// In case we do not exit, there is probably a thread still running, use the code below to identify the threads
		// that are still running (threads with daemon = false are the culprit)
		
		// Thread.getAllStackTraces().forEach((thread, stack) -> System.out
		// .println(thread.getName() + " daemon: " + thread.isDaemon() + " stack: " + Arrays.toString(stack)));
	}
	
	private void shutdown()
	{
		long start = System.currentTimeMillis();
		
		System.out.println();
		System.out.println("Shutting down:");
		System.out.println("- Playing shutdown sound");
		ds.playOneShot("shutdown");
		
		System.out.println("- Calling Hardware driver shutdown.");
		dh.shutdown();
		
		System.out.println("- Calling PsController driver shutdown.");
		dpc.shutdown();
		
		System.out.println("- Calling Sound driver shutdown.");
		ds.shutdown();
		
		System.out.println("- Calling Communication driver shutdown.");
		dc.shutdown();
		
		System.out.println("- Waiting for shutdown sound to finish.");
		while (ds.isPlaying("shutdown"))
			simplySleep(10L);
		
		long end = System.currentTimeMillis();
		
		System.out.println("- Done, time taken: " + (end - start) + "ms");
	}
	
	private void addShutdownHook(Thread myThread)
	{
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutdown hook triggered.");
			shutdownHook = true;
			
			myThread.interrupt();
			try
			{
				// Wait max 15 seconds to allow for a graceful shutdown
				myThread.join(15000);
			}
			catch (InterruptedException ignored)
			{
			}
		}));
	}
	
	// Sleep without requiring try/catch
	private void simplySleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException ignored)
		{
		}
	}
}
