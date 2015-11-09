import java.util.ArrayList;
import java.util.LinkedList;

/*
 * The simulator is trace driven. That is memory load and store operations will specified in an
 * input trace file whose name is specified as the second command line input.
 */
public class Simulator {
	
	/*
	 * The power of processors with a root of 2
	 */
	int p =0;
	/*
	 * The power of the size of every l1 with a root of 2 
	 */
	int n1 = 0;
	/*
	 * The power of the size of every l2 with a root of 2
	 */
	int n2 = 0;
	/*
	 * The size of a block
	 */
	int b = 0;
	/*
	 * The power of the associativity of l1 with a root of 2 
	 */
	int a1 = 0;
	/*
	 * The power of the associativity of l2 with a root of 2 
	 */
	int a2 = 0;
	/*
	 * The number of delay cycles caused by communicating between two nodes(a node consists of a processor and l1 cache)
	 */
	int C = 0;
	/*
	 * The number of cycles caused by a l2 hit(The l1 hit is satisfied in the same cycle in which it is issued)
	 */
	int d = 0;
	/*
	 * The number of cycles caused by a memory access
	 */
	int d1 = 0;
	
	LinkedList writeBuffer = new LinkedList();//Stores the blocks that are going te be flushed back to memory
	ArrayList processorsList = new ArrayList();
	
	Memory memory;
	/*
	 * If a access to L2 is a miss, then it will require sending a message t on-chip memory controller which
	 * is co-located with tile0, a memory access, which assumed to take d1 cycles(uniform memory access), and a message
	 * back from the memory controller.
	 */
	MemoryController memoryController;
	
	public Simulator(int p,int n1,int n2 ,int b,int a1,int a2,int C,int d,int d1){
		this.p=p;
		this.n1=n1;
		this.n2=n2;
		this.b=b;
		this.a1=a1;
		this.a2=a2;
		this.C=C;
		this.d=d;
		this.d1=d1;
		initializeUnits();
		simulate();
	}
	void initializeUnits(){
		//Initialize processors===============================================================
		int base = 2;
		//the size of l1
		int sizeOfl1 = (int) Math.pow(base, n1);
		//the number of blocks in the l1=the size of l1/the size of a block
		int numberOfBlocksInL1 = sizeOfl1/b;
		
		//the the associativity of l1
		int associativityOfL1 = (int) Math.pow(base, a1);
		//so the number of sets in the l1=the number of blocks in the l1/the associativity of l1
		int numberOfSetInL1 = numberOfBlocksInL1/associativityOfL1;
		
		
		//the size of l1
		int sizeOfl2 = (int) Math.pow(base, n2);
		//the number of blocks in the l2=the size of l2/the size of a block
		int numberOfBlocksInL2 = sizeOfl2/b;
		
		//the the associativity of l2
	    int associativityOfL2 = (int) Math.pow(base, a2);
		//so the number of sets in the l2=the number of blocks in the l2/the associativity of l2
		int numberOfSetInL2 = numberOfBlocksInL2/associativityOfL2;
		
		
	    int processorsNumber = (int) Math.pow(base, this.p);
	    for(int i=0;i<processorsNumber;i++){
	    	Processor processor = new Processor(numberOfSetInL1,numberOfSetInL2,associativityOfL1,a2);
	    	processorsList.add(processor);
	    }
	    //Initialize memory===============================================================
	    //TODO load benchmarks into memory
	    String memoryDataFilePath = "";
	    memory = Memory.getInstance(memoryDataFilePath);
	    
	}
	void simulate(){
		//Infinite loop(clock cycles)
		while(true){
			//TODO 
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int p = Integer.parseInt(args[0]);//The power of processors with a root of 2
		int n1 = Integer.parseInt(args[1]);//The power of the size of every l1 with a root of 2 
		int n2 = Integer.parseInt(args[2]);//The power of the size of every l2 with a root of 2 
		int b = Integer.parseInt(args[3]);//The size of a block
		int a1 = Integer.parseInt(args[4]);//The power of the associativity of l1 with a root of 2 
		int a2 = Integer.parseInt(args[5]);//The power of the associativity of l2 with a root of 2 
		int C = Integer.parseInt(args[6]);//The number of delay cycles caused by communicating between two nodes(a node consists of a processor and l1 cache)
		int d = Integer.parseInt(args[7]);//The number of cycles caused by a l2 hit(The l1 hit is satisfied in the same cycle in which it is issued)
		int d1 = Integer.parseInt(args[8]);//The number of cycles caused by a memory access
	}

}
