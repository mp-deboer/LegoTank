package tankpack;

public class Sm_GpioButton extends Sm_GpioButton_Generated
{
	public Sm_GpioButton(Driver_Communication dc, Driver_Hardware dh, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.dh = dh;
		
		initializeAndStart(dc, debug);
	}
}
