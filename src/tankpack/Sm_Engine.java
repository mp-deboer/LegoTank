package tankpack;

public class Sm_Engine extends Sm_Engine_Generated
{
	private final long MAXSPEEDINCREASEPERSECOND = 40L; // in percent / second
	private int maxSpeedStep;
	
	public Sm_Engine(Driver_Communication dc, Driver_Sound ds)
	{
		// Inject custom variables into Generated vars
		super.vars.ds = ds;
		
		// Pre-load engine sounds first
		Driver_Sound.preload(false, "cyclone_accelerate", "cyclone_slow", "cyclone_start", "cyclone_stop");
		Driver_Sound.preload(true, "cyclone_idle", "cyclone_lowrpm", "cyclone_run", "cyclone_run1");
		
		// Define max speed step
		maxSpeedStep = (int) (MAXSPEEDINCREASEPERSECOND * Main.DOPERIOD / 1000L);
		
		// Assert step is at least 1
		if (maxSpeedStep <= 0)
			maxSpeedStep = 1;
		
		initializeAndStart(dc);
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Forward eventData to targetSpeed
		super.vars.targetSpeed = eventData;
		
		super.dispatchEvent(eventName);
	}
	
	@Override
	protected void adjustSpeed()
	{
		if (super.vars.currentSpeed != super.vars.targetSpeed)
		{
			super.vars.currentSpeed = calculateNextSpeed(super.vars.currentSpeed, super.vars.targetSpeed, maxSpeedStep);
			
			super.logger.debug(
					String.format("Engine speed set to %d (maxSpeedStep = %d)", super.vars.currentSpeed, maxSpeedStep));
			
			// Fire engineSpeed event
			super.fireEvent("engineSpeed", super.vars.currentSpeed);
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
}
