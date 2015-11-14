import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import javax.xml.bind.DatatypeConverter;

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
	HashMap processorsTable = new HashMap();
	
	Memory memory;
	/*
	 * If a access to L2 is a miss, then it will require sending a message t on-chip memory controller which
	 * is co-located with tile0, a memory access, which assumed to take d1 cycles(uniform memory access), and a message
	 * back from the memory controller.
	 */
	MemoryController memoryController;
	
	TLB tlb;
	
	public Simulator(String inputFile ,int p,int n1,int n2 ,int b,int a1,int a2,int C,int d,int d1){
		this.p=p;
		this.n1=n1;
		this.n2=n2;
		this.b=b;
		this.a1=a1;
		this.a2=a2;
		this.C=C;
		this.d=d;
		this.d1=d1;
		Hashtable<String, ArrayList> commands = initializeUnits( inputFile);
		int clockcycle = 1;
		boolean finish = false;
		while(!finish){
			//execute all the commands in the message queue
			for(int i=0;i<processorsTable.size();i++){
		    	Processor processor = (Processor) processorsTable.get(i+"");
		    	if(processor.messageQueue.containsKey(clockcycle)){
		    		ArrayList<TraceItem> msgs = processor.messageQueue.get(clockcycle);
		    		for(int n=0;n<msgs.size();n++){
		    			//execute all the messages in the message queue
		    			TraceItem msg = msgs.get(n);
		    			//TODO
		    		}
		    	}
		    }
			
			
			// extract all commands need to operate in this clock cycle
			ArrayList instructions = new ArrayList();
			instructions = commands.get(String.valueOf(clockcycle));
			for(int i=0; i<instructions.size(); i++){
				TraceItem cur = (TraceItem) instructions.get(i);
				Processor processor = (Processor) processorsTable.get(cur.coreid);
				String address = cur.address;
				if(cur.operationFlag == 0){
					// Issue a read operation
					boolean readHitL1 = false;
					for(int j=0;j<processor.l1.setsList.size();j++){
						Set set = (Set) processor.l1.setsList.get(j);
						for(int n=0;n<set.blockList.size();n++){
							Block block = (Block) set.blockList.get(n);
							if(block.tag==address){
								//l1 hit
								readHitL1 = true;
								System.out.println("L1 read hit->");
								//MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
								if(block.state==0){
									//No node has a copy of the cache block
									System.out.println("L1 read hit and successfully read a block in L1 cache:"+address);
								}else if(block.state==1){
									//Exactly one node has a copy of the block, and it has written the block, so the memory copy is out of date,
									//The processor is called the owner of the block
									//check who is the owner, if this node is the owner, then it will cost no latency, but if the owner is a remote node,
									// put this in the message queue.
								}else if(block.state==2){
									//One or more nodes have the block cached, and the value in memory is up to data(as well as in all the caches)
									System.out.println("L1 read hit and successfully read a block in L1 cache:"+address);
								}
							}
						}
					}
					if(readHitL1==false){
						//add this command to the message queue
					}
				}else if(cur.operationFlag == 1){
					// Issue a write operation
				}
			}
			clockcycle++;
			//TODO need to check when all operations are finished then set up the finish flag = true
		}

	}

	Hashtable<String, ArrayList> initializeUnits(String inputFile){
		//Initialize processors===============================================================
		int base = 2;
		//the size of l1
		int sizeOfl1 = (int) Math.pow(base, n1);
		//the number of blocks in the l1=the size of l1/the size of a block
		int numberOfBlocksInL1 = sizeOfl1/((int) Math.pow(base, b));

		//the the associativity of l1
		int associativityOfL1 = (int) Math.pow(base, a1);
		//so the number of sets in the l1=the number of blocks in the l1/the associativity of l1
//		int numberOfSetInL1 = numberOfBlocksInL1/associativityOfL1;
		int numberOfSetInL1 = (int) Math.pow(base,(n1-a1-b));

		//the size of l1
		int sizeOfl2 = (int) Math.pow(base, n2);
		//the number of blocks in the l2=the size of l2/the size of a block
		int numberOfBlocksInL2 = sizeOfl2/((int) Math.pow(base, b));

		//the the associativity of l2
	    int associativityOfL2 = (int) Math.pow(base, a2);
		//so the number of sets in the l2=the number of blocks in the l2/the associativity of l2
//		int numberOfSetInL2 = numberOfBlocksInL2/associativityOfL2;
		int numberOfSetInL2 = (int) Math.pow(base,(n2-a2-b));


	    int processorsNumber = (int) Math.pow(base, this.p);
	    for(int i=0;i<processorsNumber;i++){
	    	Processor processor = new Processor(numberOfSetInL1,numberOfSetInL2,associativityOfL1,a2);
	    	processorsTable.put(i+"", processor);
	    }
	    //Initialize memory=====Need memory to keep ultimate states of a block
	    memory = Memory.getInstance();
	    
	    tlb = TLB.getInstance();
	    
//	    ArrayList traceList = new ArrayList();
	  //TODO load benchmarks, run and trace all the states
	    String line = null;
		// Use a hashtable to record all commands from the trace file. The key is the clock cycle, so that in each cycle
		// the commands that need to operate can be easily extracted.
		Hashtable<String, ArrayList> commands = new Hashtable<String, ArrayList>();
		try {
			FileReader filereader = new FileReader (inputFile);
			BufferedReader bufferedreader = new BufferedReader (filereader);
			while ((line = bufferedreader.readLine()) != null){
				String [] ss = line.split(" ");
				TraceItem item = new TraceItem();
				item.cycle =Integer.parseInt(ss[0]) ;
				item.coreid=ss[1];
				item.operationFlag=Integer.parseInt(ss[2]);
				item.address=ss[3];
//				byte [] test = new byte[40];
//				test = DatatypeConverter.parseHexBinary(ss[3]);
				boolean ccexist = commands.containsKey(ss[0]);
				if(ccexist){
					commands.get(ss[0]).add(item);
				}else{
					ArrayList tmp = new ArrayList();
					tmp.add(item);
					commands.put(ss[0],tmp);
				}
//				traceList.add(item);
				//add this memory address to memory
				MemoryBlock block = new MemoryBlock();
				memory.addMemoryBlock(item.address, block);
				System.out.println("read trace file line->"+"  cycle-"+item.cycle+"  coreid-"+item.coreid+"  operationFlag-"+item.operationFlag+"  address-"+item.address);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return commands;



//		for(int i = 0;i<traceList.size();i++){
//			TraceItem item = (TraceItem) traceList.get(i);
//			String coreid = item.coreid;
//			int operationFlag = item.operationFlag;
//			Processor processor = (Processor) processorsTable.get(coreid);
//			String address = item.address;
//			System.out.println("Start to look the block up in the processor's l1->"+"  -");
//			//Look the block up in the processor's l1
//			boolean isL1Hit = false;
//			for(int j=0;j<processor.l1.setsList.size();j++){
//				Set set = (Set) processor.l1.setsList.get(j);
//				for(int n=0;n<set.blockList.size();n++){
//					Block b = (Block) set.blockList.get(n);
//					if(b.tag==address){
//						//l1 hit
//						isL1Hit = true;
//						System.out.println("Get a l1 hit->"+"  -");
//						//check the block's state and owner
//						//MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
//						if(b.state==0){
//
//						}else if(b.state==1){
//
//						}else if(b.state==2){
//
//						}
//					}
//				}
//			}
//			boolean isL2Hit = false;
//			if(isL1Hit==false){
//				//Look the block up in the processor's l2
//				for(int j=0;j<processor.l2.setsList.size();j++){
//					Set set = (Set) processor.l2.setsList.get(j);
//					for(int n=0;n<set.blockList.size();n++){
//						Block b = (Block) set.blockList.get(n);
//						if(b.tag==address){
//							//l2 hit
//							isL1Hit = true;
//							System.out.println("Get a l2 hit->"+"  -");
//							//check the block's state and owner
//							//MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
//							if(b.state==0){
//
//							}else if(b.state==1){
//
//							}else if(b.state==2){
//
//							}
//						}
//					}
//				}
//			}
//			if(isL2Hit==false && isL1Hit==false){
//				//Load from memeory
//				System.out.println("Load from memeory and start count latency cycles->"+"  -");
//			}
//			if(operationFlag==0){//read operation
//				//TODO TODO
//				//Get the virtual address() through TLB
//				if(tlb.isAvailableInTLB(address)){//virtual address==cache flag:set index:block index
//					String virtualAddress = tlb.getVirtualAddress(address);
//					String indexes[] = virtualAddress.split(":");
//					int cacheFlag = Integer.parseInt(indexes[0]);
//					int setIndex = Integer.parseInt(indexes[1]);
//					int blockIndex = Integer.parseInt(indexes[2]);
//					
//				}else{
//					String virtualAddress = tlb.translatePhysicalToVirtual(address, processor);
//					String indexes[] = virtualAddress.split(":");
//					int cacheFlag = Integer.parseInt(indexes[0]);
//					int setIndex = Integer.parseInt(indexes[1]);
//					int blockIndex = Integer.parseInt(indexes[2]);
//				}
				//First read from l1
//				
//			}else if(operationFlag==1){//write operation
//				//TODO TODO
//			}else{
//				
//			}
//
//		}
	}

	void readOperation(TraceItem op){
		String coreid = op.coreid;
		Processor processor = (Processor) processorsTable.get(coreid);
		String address = op.address;
		boolean readHitL1 = false;
		boolean readHitL2 = false;
		if(readHitL1){

		}else if(readHitL2){

		}else{

		}

	}

	void writeOperation(TraceItem op){
		String coreid = op.coreid;
		Processor processor = (Processor) processorsTable.get(coreid);
		String address = op.address;
		boolean writeHitL1 = false;
		boolean writeHitL2 = false;
		if(writeHitL1){

		}else if(writeHitL2){

		}else{

		}

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputFile = args[0];
		int p = Integer.parseInt(args[1]);//The power of processors with a root of 2
		int n1 = Integer.parseInt(args[2]);//The power of the size of every l1 with a root of 2 
		int n2 = Integer.parseInt(args[3]);//The power of the size of every l2 with a root of 2 
		int b = Integer.parseInt(args[4]);//The size of a block
		int a1 = Integer.parseInt(args[5]);//The power of the associativity of l1 with a root of 2 
		int a2 = Integer.parseInt(args[6]);//The power of the associativity of l2 with a root of 2 
		int C = Integer.parseInt(args[7]);//The number of delay cycles caused by communicating between two nodes(a node consists of a processor and l1 cache)
		int d = Integer.parseInt(args[8]);//The number of cycles caused by a l2 hit(The l1 hit is satisfied in the same cycle in which it is issued)
		int d1 = Integer.parseInt(args[9]);//The number of cycles caused by a memory access
		Simulator simulator = new Simulator(inputFile, p, n1, n2 , b, a1, a2, C, d, d1);
	}

}
