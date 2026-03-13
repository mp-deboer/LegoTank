package tankpack;

import tankpack.enums.MotorType;

public class Sm_Motor extends Sm_Motor_Generated
{
	private final long MAXSPEEDINCREASEPERSECOND = 100L; // in percent / second
	
	private String speedEvent;
	private String engineEvent;
	private MotorType motorType;
	
	private int maxSpeedStep;
	private int sign = 1; // sign is -1 or 1, used to convert engineSpeed to targetSpeed
	
	public Sm_Motor(Driver_Communication dc, Driver_Hardware dh, MotorType m)
	{
		// Inject custom variables into Generated vars
		super.vars.dh = dh;
		
		// Store motor type
		motorType = m;
		
		// Define custom trigger events, assert upper case
		speedEvent = m.getSpeedEvent().toUpperCase();
		engineEvent = m.getEngineEvent().toUpperCase();
		
		// Calculate max speed steps
		maxSpeedStep = (int) (MAXSPEEDINCREASEPERSECOND * Main.DOPERIOD / 1000L);
		
		// Assert step is at least 1
		if (maxSpeedStep <= 0)
			maxSpeedStep = 1;
		
		// Assert speed of motor is 0
		super.vars.dh.setMotorSpeed(m, 0);
		
		initializeAndStart(dc, m.name());
	}
	
	@Override
	protected String translateEventName(String baseName)
	{
		String translatedName = baseName;
		
		if (baseName.equals("SETSPEEDEVENT"))
			translatedName = speedEvent;
		else if (baseName.equals("SETENGINESPEEDEVENT"))
			translatedName = engineEvent;
		
		return translatedName;
	}
	
	@Override
	protected String translateToBaseEventName(String eventName)
	{
		String translatedName = eventName;
		
		if (eventName.equals(speedEvent))
			translatedName = "SETSPEEDEVENT";
		else if (eventName.equals(engineEvent))
			translatedName = "SETENGINESPEEDEVENT";
		
		return translatedName;
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Forward eventData to target- or engineSpeed
		if (eventName.equals(speedEvent))
		{
			super.vars.targetSpeed = eventData;
			
			// Correct sign based on targetSpeed
			if (eventData > 0)
				sign = 1;
			else if (eventData < 0)
				sign = -1;
		}
		else if (eventName.equals(engineEvent))
		{
			super.vars.engineSpeed = eventData;
		}
		
		super.dispatchEvent(eventName);
	}
	
	@Override
	protected void adjustSpeed()
	{
		int targetSpeed;
		if (super.vars.targetSpeed != 0 && super.vars.engineSpeed >= 0)
		{
			// EngineSpeed enabled, correct direction, sign is set at dispatchEvent(speedEvent)
			targetSpeed = sign * super.vars.engineSpeed;
		}
		else // EngineSpeed disabled / -1, use the default targetSpeed
			targetSpeed = super.vars.targetSpeed;
		
		if (super.vars.currentSpeed != targetSpeed)
		{
			super.vars.currentSpeed = calculateNextSpeed(super.vars.currentSpeed, targetSpeed, maxSpeedStep);
			
			super.logger.debug(String.format("Setting speed of %s to %d (maxSpeedStep = %d)", motorType,
					super.vars.currentSpeed, maxSpeedStep));
			
			// Set actual speed of motor to currentSpeed
			super.vars.dh.setMotorSpeed(motorType, super.vars.currentSpeed);
		}
	}
	
	private int calculateNextSpeed(int current, int target, int step)
	{
		int diff = target - current;
		
		int nextSpeed = current + Integer.signum(diff) * step;
		
		if (Integer.signum(diff) != Integer.signum(target - nextSpeed))
			nextSpeed = target; // Overshoot fix
			
		return nextSpeed;
	}
}
