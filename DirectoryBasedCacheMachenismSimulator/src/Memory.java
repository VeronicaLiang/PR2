import java.util.ArrayList;
import java.util.HashMap;


public class Memory {
	private static Memory memory;
	private Memory(){}
	public static Memory getInstance(){
		if(memory==null){
			memory = new Memory();
		}
		return memory;
	}
	private HashMap blocks = new HashMap();
	
	public Object addMemoryBlock(String address,MemoryBlock block){
		return blocks.put(address, block);
	}
	public Object getMemoryBlock(String address,MemoryBlock block){
		return blocks.put(address, block);
	}
	
}
