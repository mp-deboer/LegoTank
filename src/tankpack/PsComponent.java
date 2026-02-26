package tankpack;

public enum PsComponent
{
	Select(0), L3(1), R3(2), Start(3), Up(4), Right(5), Down(6), Left(7), L2(8), R2(9), L1(10), R1(11), Triangle(
			12), Circle(13), Cross(14), Square(15), PS3(16), Leftstick_X(
					17), Leftstick_Y(18), Rightstick_X(19), Rightstick_Y(20), L2_Sensor(29), R2_Sensor(30);
	
	private int index;
	
	PsComponent(int index)
	{
		this.index = index;
	}
	
	public int getIndex()
	{
		return index;
	}
}