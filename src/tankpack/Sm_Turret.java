package tankpack;

public class Sm_Turret extends Sm_Turret_Generated
{
	public Sm_Turret(Driver_Communication dc, boolean debug)
	{
		initializeAndStart(dc, debug);
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Custom dispatchEvent, we only expect events that set speed
		super.vars.speed = eventData;
		
		super.dispatchEvent(eventName);
	}
}
