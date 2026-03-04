package tankpack;

public class Sm_TurretDrive extends Sm_TurretDrive_Generated
{
	private final boolean localDebug = false;
	private final long MAXSPEEDINCREASEPERSECOND = 25L; // in percent / second
	
	private int maxSpeedStep;
	
	public Sm_TurretDrive(Driver_Communication dc, Driver_Sound ds, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.ds = ds;
		
		// Pre-load turret sound first
		Driver_Sound.preload(true, "turret");
		
		// Define max speed step
		maxSpeedStep = (int) (MAXSPEEDINCREASEPERSECOND * Main.DOPERIOD / 1000L);
		
		// Assert step is at least 1
		if (maxSpeedStep <= 0)
			maxSpeedStep = 1;
		
		initializeAndStart(dc, debug);
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Custom dispatchEvent, we only expect events that set speed
		super.vars.targetSpeed = eventData;
		
		super.dispatchEvent(eventName);
	}
	
	@Override
	protected void adjustSpeed()
	{
		if (super.vars.currentSpeed != super.vars.targetSpeed)
		{
			super.vars.currentSpeed = calculateNextSpeed(super.vars.currentSpeed, super.vars.targetSpeed, maxSpeedStep);
			
			if (localDebug)
				System.out.printf("Turret speed set to %d (maxSpeedStep = %d)\n", super.vars.currentSpeed,
						maxSpeedStep);
			
			// Fire turretSpeed event
			super.fireEvent("turretSpeed", super.vars.currentSpeed);
			
			// Also set turret volume based on currentSpeed percentage, starting at 20% for the first 20%
			// super.vars.ds.setMaxVolume("turret", Math.max(20, super.vars.currentSpeed));
		}
	}
	
	private int calculateNextSpeed(int current, int target, int step)
	{
		int nextSpeed;
		int diff = target - current;
		
		// If decelerating / ramping towards 0, set speed to target (we only ramp up)
		if (Integer.signum(current) != 0 && Integer.signum(diff) == -Integer.signum(current))
		{
			nextSpeed = target;
		}
		// If accelerating, increase speed with step
		else
		{
			nextSpeed = current + Integer.signum(diff) * step;
			
			if (Integer.signum(diff) != Integer.signum(target - nextSpeed))
				nextSpeed = target; // Overshoot fix
		}
		
		return nextSpeed;
	}
	
	@Override
	protected int speedToVolume(int speed)
	{
		float percentage = (float) speed;
		
		// Convert speed with range 0-100 to volume percentage range: 20-100
		return Math.round(percentage / 100f * 80f + 20f);
	}
}
