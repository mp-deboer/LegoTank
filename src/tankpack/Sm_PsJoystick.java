package tankpack;

import tankpack.enums.Direction;
import tankpack.enums.PsComponent;

public class Sm_PsJoystick extends Sm_PsJoystick_Generated
{
	private final boolean localDebug = false;
	
	private String stickName;
	
	public Sm_PsJoystick(Driver_Communication dc, Driver_PsController dpc, PsComponent psX, PsComponent psY,
			boolean debug)
	{
		stickName = psX.name().substring(0, psX.name().indexOf('_'));
		
		// Inject custom variables into Generated vars
		super.vars.dpc = dpc;
		super.vars.stickX = psX;
		super.vars.stickY = psY;
		super.vars.stickIdleEvent = stickName + "Idle";
		super.vars.stickUsedEvent = stickName + "Used";
		
		initializeAndStart(dc, stickName, debug);
	}
	
	@Override
	protected void dispatchEvent(String eventName)
	{
		// Disable left stick events when follow mode is active
		if (eventName.equals("STARTFOLLOW") && super.vars.stickX.equals(PsComponent.Leftstick_X))
			super.vars.enabled = false;
		// Enable left stick again when follow mode stops
		else if (eventName.equals("STOPFOLLOW") && super.vars.stickX.equals(PsComponent.Leftstick_X))
			super.vars.enabled = true;
		
		super.dispatchEvent(eventName);
	}
	
	@Override
	protected void calculateAndFireStickEvent()
	{
		float xVal = super.vars.dpc.getValue(super.vars.stickX);
		float yVal = super.vars.dpc.getValue(super.vars.stickY);
		
		int degree = calculateDegree(xVal, yVal);
		int percentage = calculatePercentage(xVal, yVal);
		Direction direction = calculateDirection(degree);
		String event = (stickName + direction.name()).toUpperCase();
		
		if (super.vars.percentage != percentage || super.vars.direction != direction)
		{
			if (localDebug)
				System.out.println(super.vars.stickX.name() + ": Degree & percentage: " + degree + "°, " + percentage
						+ "%, event: " + event + "(" + percentage + ")");
			
			super.vars.percentage = percentage;
			super.vars.direction = direction;
			
			super.fireEvent(event, percentage);
		}
	}
	
	private int calculatePercentage(float x, float y)
	{
		x = Math.abs(x);
		y = Math.abs(y);
		
		// Convert 0..1 to 0..100, rounded to nearest multiple of 5
		int percentage = Math.round(Math.max(x, y) * 20) * 5;
		
		return percentage;
	}
	
	private int calculateDegree(float x, float y)
	{
		double angle;
		
		// source:
		// http://forum.processing.org/two/discussion/4136/how-do-i-translate-joystick-values-to-degree-of-arc/p1
		angle = Math.atan2(y, x);
		if (angle < 0)
			angle += (2.0 * Math.PI);
		angle = (angle + (0.5 * Math.PI)) % (2.0 * Math.PI);
		
		return ((int) Math.toDegrees(angle));
	}
	
	private Direction calculateDirection(int degree)
	{
		// 360 degrees divided over 8 directions gives 45 degrees per direction
		// So forward is 45 degrees wide with 0 as center degree
		if (degree >= 338 || degree <= 22)
			return Direction.Forward;
		else if (degree >= 23 && degree <= 67)
			return Direction.ForwardRight;
		else if (degree >= 68 && degree <= 112)
			return Direction.Right;
		else if (degree >= 113 && degree <= 157)
			return Direction.BackwardRight;
		else if (degree >= 158 && degree <= 202)
			return Direction.Backward;
		else if (degree >= 203 && degree <= 247)
			return Direction.BackwardLeft;
		else if (degree >= 248 && degree <= 292)
			return Direction.Left;
		else // if (degree >= 293 && degree <= 337)
			return Direction.ForwardLeft;
	}
}
