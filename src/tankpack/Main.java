package tankpack;

import java.lang.Math;

import tankpack.enums.*;

public class Main
{
	public static final long DOPERIOD = 100L; // changing this requires recompiling all
	private static boolean debug; // set via argument
	private boolean lfOn = false;
	
	// Objects shared between functions:
	private Driver_Communication dc;
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
		this.lF = lF;
		
		// Start GpioHandler
		dh.start();
		
		// Start DO loop
		timedFireDoEvent();
	}
	
	private void timedFireDoEvent()
	{
		long period = DOPERIOD;
		long sleepTime = period;
		
		long start;
		long end;
		long duration;
		
		try
		{
			while (true)
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
		
		// In case we do not exit, there is probably a thread still running, use the code below to identify the threads
		// that are still running (threads with daemon = false are the culprit)
		
		// Thread.getAllStackTraces().forEach((thread, stack) -> System.out
		// .println(thread.getName() + " daemon: " + thread.isDaemon() + " stack: " + Arrays.toString(stack)));
	}
}
