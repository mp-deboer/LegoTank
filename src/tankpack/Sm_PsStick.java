package tankpack;

import tankpack.enums.PsComponent;

public class Sm_PsStick extends Sm_PsStick_Generated
{
	public Sm_PsStick(Driver_Communication dc, Driver_PsController dpc, PsComponent stick)
	{
		String stickName = stick.name().substring(0, stick.name().indexOf('_'));
		
		// Inject custom variables into Generated vars
		super.vars.dpc = dpc;
		super.vars.stick = stick;
		super.vars.stickEvent = stickName + "Stick";
		
		initializeAndStart(dc, stickName);
	}
	
	@Override
	protected void calculateAndFireStickEvent()
	{
		float val = super.vars.dpc.getValue(super.vars.stick);
		
		int percentage = calculatePercentage(val);
		
		if (super.vars.percentage != percentage)
		{
			super.logger.debug(super.vars.stickEvent + ": Percentage: " + percentage + "%");
			
			super.vars.percentage = percentage;
			
			super.fireEvent(super.vars.stickEvent, percentage);
		}
	}
	
	private int calculatePercentage(float x)
	{
		// Convert -1..1 to 0..2
		x += 1f;
		
		// Convert 0..2 to 0..100, rounded to nearest multiple of 5
		return Math.round(x * 10) * 5;
	}
}
