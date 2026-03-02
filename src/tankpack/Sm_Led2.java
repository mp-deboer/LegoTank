package tankpack;

public class Sm_Led2 extends Sm_Led2_Generated
{
	public Sm_Led2(Driver_Communication dc, Driver_Hardware dh, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.dh = dh;
		
		initializeAndStart(dc, debug);
	}
}
