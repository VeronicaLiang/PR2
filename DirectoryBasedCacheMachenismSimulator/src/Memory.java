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
	private HashMap<String,MemoryBlock> blocks = new HashMap<String,MemoryBlock>();
	
	public Object addMemoryBlock(String address,MemoryBlock block){
		return blocks.put(address, block);
	}
	public MemoryBlock getMemoryBlock(String address){
		return blocks.get(address);
	}
	
}
