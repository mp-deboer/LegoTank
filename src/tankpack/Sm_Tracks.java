package tankpack;

public class Sm_Tracks extends Sm_Tracks_Generated
{
	public Sm_Tracks(Driver_Communication dc)
	{
		initializeAndStart(dc);
	}
	
	@Override
	protected void dispatchEvent(String eventName, int eventData)
	{
		// Custom dispatchEvent, we only expect events that set speed
		super.vars.speed = eventData;
		
		super.dispatchEvent(eventName);
	}
}
