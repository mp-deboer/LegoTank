package tankpack;

import tankpack.enums.PsComponent;

public class Sm_PsButton extends Sm_PsButton_Generated
{
	public Sm_PsButton(Driver_Communication dc, Driver_PsController dpc, PsComponent psc, boolean debug)
	{
		// Inject custom variables into Generated vars
		super.vars.dpc = dpc;
		super.vars.button = psc;
		super.vars.buttonPressedEvent = psc.name() + "Pressed";
		super.vars.buttonReleasedEvent = psc.name() + "Released";
		
		initializeAndStart(dc, psc.name(), debug);
	}
}
