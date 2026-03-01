package tankpack;

//PS3 components index constants
// 2025-12-13 MdB: Added support for PS5 controller
public enum PsComponent
{
	// ps3Val and ps5Val
	Select(0, 8),
	L3(1, 11),
	R3(2, 12),
	Start(3, 9),
	Up_Arrow(4, 19),
	Right_Arrow(5, 19),
	Down_Arrow(6, 19),
	Left_Arrow(7, 19),
	L2(8, 6),
	R2(9, 7),
	L1(10, 4),
	R1(11, 5),
	Triangle(12, 2),
	Circle(13, 1),
	Cross(14, 0),
	Square(15, 3),
	PS_button(16, 10),
	Leftstick_X(17, 13),
	Leftstick_Y(18, 14),
	Rightstick_X(19, 16),
	Rightstick_Y(20, 17),
	L2_Sensor(29, 15),
	R2_Sensor(30, 18);
	
	private int ps3Val;
	private int ps5Val;
	
	PsComponent(int ps3Val, int ps5Val)
	{
		this.ps3Val = ps3Val;
		this.ps5Val = ps5Val;
	}
	
	public int getIndex(boolean isPs5)
	{
		return isPs5 ? ps5Val : ps3Val;
	}
}