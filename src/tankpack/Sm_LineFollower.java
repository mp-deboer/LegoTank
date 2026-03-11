package tankpack;

import tankpack.enums.SensorPosition;

public class Sm_LineFollower extends Sm_LineFollower_Generated
{
	public boolean localDebug; // public so Main can read
	public String debugMessage;
	
	public Sm_LineFollower(Driver_Communication dc, Sm_Sensor[] sensors)
	{
		// Inject custom variables into Generated vars
		super.vars.sensors = sensors;
		super.vars.followSpeed = 10;
		super.vars.maxEasyTurnCount = 3000 / (int) Main.DOPERIOD; // set max time for easy turns to 3s
		
		initializeAndStart(dc);
		
		// Get debug setting from logger
		localDebug = super.logger.isDebugEnabled();
		super.vars.localDebug = localDebug;
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Use R2Stick value as followSpeed (minimal 10)
		if (eventName.equals("R2STICK"))
			super.vars.followSpeed = Math.max(10, eventData);
		
		super.dispatchEvent(eventName);
	}
	
	@Override
	protected void debug()
	{
		int blackThreshold = super.vars.sensors[0].vars.BLACK;
		int leftValue = super.vars.sensors[SensorPosition.Left.ordinal()].vars.colorValue;
		int middleValue = super.vars.sensors[SensorPosition.Middle.ordinal()].vars.colorValue;
		int rightValue = super.vars.sensors[SensorPosition.Right.ordinal()].vars.colorValue;
		
		debugMessage = String.format("%s%3d%-6s %s%3d%-6s %s%3d%-6s, %-20s", (leftValue > blackThreshold) ? "[" : " ",
				leftValue, (leftValue > blackThreshold) ? "]" : " ", (middleValue > blackThreshold) ? "[" : " ",
				middleValue, (middleValue > blackThreshold) ? "]" : " ", (rightValue > blackThreshold) ? "[" : " ",
				rightValue, (rightValue > blackThreshold) ? "]" : " ", super.stateId.name());
	}
}
