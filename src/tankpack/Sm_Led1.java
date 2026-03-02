package tankpack;

public class Sm_Led1 extends Sm_Led1_Generated
{
	public Sm_Led1(Driver_Communication dc, Driver_Hardware dh, boolean debug)
	{
		// Inject custom variables into Generated vars
		this.vars.dh = dh;
		
		initializeAndStart(dc, debug);
	}
}
