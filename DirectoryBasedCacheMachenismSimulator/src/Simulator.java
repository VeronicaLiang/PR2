import java.util.ArrayList;
import java.util.LinkedList;


public class Simulator {
	
	/*
	 * The number of processors
	 */
	int p =0;
	/*
	 * The size of every l1
	 */
	int n1 = 0;
	/*
	 * The size of l2
	 */
	int n2 = 0;
	/*
	 * The size of a block
	 */
	int b = 0;
	/*
	 * The associativity of l1
	 */
	int a1 = 0;
	/*
	 * The associativity of l2;
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
		
		//the number of blocks in the l1=the size of l1/the size of a block
		int numberOfBlocksInL1 = n1/b;
		
		//so the number of sets in the l1=the number of blocks in the l1/the associativity of l1
		int numberOfSetInL1 = numberOfBlocksInL1/a1;
		
		//the number of blocks in the l1=the size of l1/the size of a block
		int numberOfBlocksInL2 = n2/b;
				
		//so the number of sets in the l1=the number of blocks in the l1/the associativity of l1
		int numberOfSetInL2 = numberOfBlocksInL2/a2;
		
		int base = 2;
	    int processorsNumber = (int) Math.pow(base, this.p);
	    for(int i=0;i<processorsNumber;i++){
	    	Processor processor = new Processor(numberOfSetInL1,numberOfSetInL2,a1,a2);
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
