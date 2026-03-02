package tankpack;

public class Sm_PsController extends Sm_PsController_Generated
{
	public Sm_PsController(Driver_Communication dc, Driver_PsController dpc, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.dpc = dpc;
		
		initializeAndStart(dc, debug);
	}
}
