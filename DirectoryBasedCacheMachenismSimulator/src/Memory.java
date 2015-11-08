
public class Memory {
	private static Memory memory;
	private Memory(){}
	public static Memory getInstance(String memoryDataFilePath){
		if(memory==null){
			memory = new Memory();
		}
		return memory;
	}
}
