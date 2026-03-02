package tankpack;

import tankpack.enums.MotorType;

public class Sm_Motor extends Sm_Motor_Generated
{
	private final boolean localDebug = false;
	private final long MAXSPEEDINCREASEPERSECOND = 20L; // in percent / second
	
	private String speedEvent;
	private MotorType motorType;
	
	private int maxSpeedStep;
	
	public Sm_Motor(Driver_Communication dc, Driver_Hardware dh, MotorType m, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.dh = dh;
		
		// Store motor type
		motorType = m;
		
		// Define custom trigger events, assert upper case
		speedEvent = m.getSpeedEvent().toUpperCase();
		
		// Calculate max speed steps
		maxSpeedStep = (int) (MAXSPEEDINCREASEPERSECOND * Main.DOPERIOD / 1000L);
		
		// Assert step is at least 1
		if (maxSpeedStep <= 0)
			maxSpeedStep = 1;
		
		// Assert speed of motor is 0
		super.vars.dh.setMotorSpeed(m, 0);
		
		initializeAndStart(dc, m.name(), debug);
	}
	
	@Override
	protected String translateEventName(String baseName)
	{
		String translatedName = baseName;
		
		if (baseName.equals("SETSPEEDEVENT"))
			translatedName = speedEvent;
		
		return translatedName;
	}
	
	@Override
	protected String translateToBaseEventName(String eventName)
	{
		String translatedName = eventName;
		
		if (eventName.equals(speedEvent))
			translatedName = "SETSPEEDEVENT";
		
		return translatedName;
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Forward eventData to targetSpeed
		if (eventName.equals(speedEvent))
		{
			super.vars.targetSpeed = eventData;
		}
		
		super.dispatchEvent(eventName);
	}
	
	@Override
	protected void adjustSpeed()
	{
		int targetSpeed = super.vars.targetSpeed;
		
		if (super.vars.currentSpeed != targetSpeed)
		{
			super.vars.currentSpeed = calculateNextSpeed(super.vars.currentSpeed, targetSpeed, maxSpeedStep);
			
			if (localDebug)
				System.out.printf("Setting speed of %s to %d (maxSpeedStep = %d)\n", motorType, super.vars.currentSpeed,
						maxSpeedStep);
			
			// Set actual speed of motor to currentSpeed
			super.vars.dh.setMotorSpeed(motorType, super.vars.currentSpeed);
		}
	}
	
	private int calculateNextSpeed(int current, int target, int step)
	{
		int diff = target - current;
		
		// If decelerating / ramping towards 0, multiplier = 3, otherwise 1
		int multiplier = (Integer.signum(current) != 0 && Integer.signum(diff) == -Integer.signum(current)) ? 3 : 1;
		
		int nextSpeed = current + Integer.signum(diff) * step * multiplier;
		
		if (Integer.signum(diff) != Integer.signum(target - nextSpeed))
			nextSpeed = target; // Overshoot fix
			
		return nextSpeed;
	}
}
