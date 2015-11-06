import java.util.LinkedList;


public class Simulator {
	public Simulator(){}
	
	int p =0;//The number of processors
	int n1 = 0;//The size of every l1
	int n2 = 0;//The size of l2
	int b = 0;//The size of a block
	int a1 = 0;//The associativity of l1
	int a2 = 0;//The associativity of l2;
	int C = 0;//The number of delay cycles caused by communicating between two nodes(a node consists of a processor and l1 cache)
	int d = 0;//The number of cycles caused by a l2 hit(The l1 hit is satisfied in the same cycle in which it is issued)
	int d1 = 0;//The number of cycles caused by a memory access
	
	LinkedList writeBuffer = new LinkedList();//Stores the blocks that are going te be flushed back to memory
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int p = Integer.parseInt(args[0]);//The number of processors
		int n1 = Integer.parseInt(args[1]);//The size of every l1
		int n2 = Integer.parseInt(args[2]);//The size of l2
		int b = Integer.parseInt(args[3]);//The size of a block
		int a1 = Integer.parseInt(args[4]);//The associativity of l1
		int a2 = Integer.parseInt(args[5]);//The associativity of l2;
		int C = Integer.parseInt(args[6]);//The number of delay cycles caused by communicating between two nodes(a node consists of a processor and l1 cache)
		int d = Integer.parseInt(args[7]);//The number of cycles caused by a l2 hit(The l1 hit is satisfied in the same cycle in which it is issued)
		int d1 = Integer.parseInt(args[8]);//The number of cycles caused by a memory access
	}

}
