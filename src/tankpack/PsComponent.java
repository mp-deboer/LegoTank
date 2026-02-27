package tankpack;

//PS3 components index constants
// 2025-12-13 MdB: Added support for PS5 controller
public enum PsComponent
{
	// ps3Val and ps5Val
	Select(0, 9),
	L3(1, 10),
	R3(2, 11),
	Start(3, 8),
	Up_Arrow(4, 21),
	Right_Arrow(5, 21),
	Down_Arrow(6, 21),
	Left_Arrow(7, 21),
	L2(8, 6),
	R2(9, 7),
	L1(10, 4),
	R1(11, 5),
	Triangle(12, 3),
	Circle(13, 2),
	Cross(14, 1),
	Square(15, 0),
	PS_button(16, 12),
	Leftstick_X(17, 15),
	Leftstick_Y(18, 16),
	Rightstick_X(19, 17),
	Rightstick_Y(20, 20),
	L2_Sensor(29, 18),
	R2_Sensor(30, 19);
	
	private int ps3Index;
	private int ps5Index;
	
	PsComponent(int ps3Index, int ps5Index)
	{
		this.ps3Index = ps3Index;
		this.ps5Index = ps5Index;
	}
	
	public int getIndex(boolean isPs5)
	{
		return isPs5 ? ps5Index : ps3Index;
	}
}