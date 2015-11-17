import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;
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
	HashMap<Integer,ArrayList<Message>> messageQueue = new HashMap<Integer,ArrayList<Message>>();
	
	Memory memory;
	/*
	 * If a access to L2 is a miss, then it will require sending a message t on-chip memory controller which
	 * is co-located with tile0, a memory access, which assumed to take d1 cycles(uniform memory access), and a message
	 * back from the memory controller.
	 */
	//Message memoryController;
	
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
			if(messageQueue.containsKey(clockcycle)){
	    		ArrayList<Message> msgs = messageQueue.get(clockcycle);
	    		for(int i=0;i<msgs.size();i++){
	    			//execute all the messages in the message queue
	    			Message msg = msgs.get(i);
	    			String address = msg.dataAddress;
    				
	    			if(msg.messageType.equals(Message.READ_MISS)){
	    				//check whether the home has the block or not
	    				String coreid = msg.homeNode;
	    				Processor processor = (Processor) processorsTable.get(coreid);
	    				if(processor.l2.directory.blocktable.contains(address)){
	    					//if the home node has the block, then check its state
	    					if(processor.l2.directory.blocktable.get(address).state==0){
	    						//one remote has the block modified. send a message to request modified value
	    						Message message = new Message();
		    					message.homeNode = msg.homeNode;
		    					message.messageType = Message.DATA_IN_REMOTE;
		    					message.dataAddress = address;
		    					message.localNote = msg.localNote;
		    					message.remoteNode = processor.l2.directory.blocktable.get(address).owner;
		    					int manhattanDistance = 3;//TODO To calculate manhattan distance
	        					int executeCycle = clockcycle+manhattanDistance;
		    					if(messageQueue.containsKey(executeCycle)){
		    						messageQueue.get(executeCycle).add(message);
		    					}else{
		    						ArrayList<Message> al = new ArrayList<Message>();
		    						al.add(message);
		    						messageQueue.put(executeCycle, al);
		    					}
	    					}else{
	    						//if the state of the block is either shared or modified, send it to the requesting node.
	    	    				Message message = new Message();
	        					message.homeNode = msg.homeNode;
	        					message.messageType = Message.DATA_VALUE_REPLY;
	        					message.dataAddress = address;
	        					message.localNote = msg.localNote;
	        					message.remoteNode = msg.remoteNode;
	        					int manhattanDistance = 3;//TODO To calculate manhattan distance
	        					int executeCycle = clockcycle+manhattanDistance;
	        					if(messageQueue.containsKey(executeCycle)){
	        						messageQueue.get(executeCycle).add(message);
	        					}else{
	        						ArrayList<Message> al = new ArrayList<Message>();
	        						al.add(message);
	        						messageQueue.put(executeCycle, al);
	        					}
	    					}
	    				}else{
	    					//if the home node does not have the block, then fetch from memory
	    					Message message = new Message();
	    					message.homeNode = msg.homeNode;
	    					message.messageType = Message.FETCH_DATA_MEMORY;
	    					message.dataAddress = address;
	    					message.localNote = msg.localNote;
	    					int executeCycle = clockcycle+d1;
	    					if(messageQueue.containsKey(executeCycle)){
	    						messageQueue.get(executeCycle).add(message);
	    					}else{
	    						ArrayList<Message> al = new ArrayList<Message>();
	    						al.add(message);
	    						messageQueue.put(executeCycle, al);
	    					}
	    					
	    					System.out.println("The home node successfully fetched the block from memory, send a message to the remote requesting node. : address:"+address);
	    				}
	    			}else if(msg.messageType.equals(Message.FETCH_DATA_MEMORY)){
	    				String coreid = msg.homeNode;
	    				Processor processor = (Processor) processorsTable.get(coreid);
    					//put it into l2 cache in the home node, change the its state to shared.
    					if(processor.l2.directory.blocktable.contains(address)){
	    					//if the home node has the block, then check its state and add the requesting node as a sharer
    						processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
    						processor.l2.directory.blocktable.get(address).sharers.add(msg.localNote);
	    				}else{
	    					OwnerAndSharers os = new OwnerAndSharers();
	    					os.state=Directory.SHARED_STATE;
	    					os.sharers.add(msg.localNote);
	    					processor.l2.directory.blocktable.put(address, os);
	    				}
    					//send the newly fetched block to the requesting node
    					if(msg.homeNode.equals(msg.localNote)){
    						//store the block into l2 and l1? TODO 
    					}else{
    						Message message = new Message();
        					message.homeNode = msg.homeNode;
        					message.messageType = Message.DATA_VALUE_REPLY;
        					message.dataAddress = address;
        					message.localNote = msg.localNote;
        					int manhattanDistance = 3;//TODO To calculate manhattan distance
        					int executeCycle = clockcycle+manhattanDistance;
        					if(messageQueue.containsKey(executeCycle)){
        						messageQueue.get(executeCycle).add(message);
        					}else{
        						ArrayList<Message> al = new ArrayList<Message>();
        						al.add(message);
        						messageQueue.put(executeCycle, al);
        					}
    					}
    					
	    			}else if(msg.messageType.equals(Message.DATA_VALUE_REPLY)){
	    				String coreid = msg.localNote;
	    				Processor processor = (Processor) processorsTable.get(coreid);
    					//put it into l2 cache in the home node, change the its state to shared.
    					if(processor.l2.directory.blocktable.contains(address)){
	    					//if the home node has the block, then check its state and add the requesting node as a sharer
    						processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
    						processor.l2.directory.blocktable.get(address).sharers.add(msg.localNote);
	    				}else{
	    					OwnerAndSharers os = new OwnerAndSharers();
	    					os.state=Directory.SHARED_STATE;
	    					os.sharers.add(msg.localNote);
	    					processor.l2.directory.blocktable.put(address, os);
	    				}
    					//store the block into l2 and l1? TODO
    					
	    			}else if(msg.messageType.equals(Message.DATA_IN_REMOTE)){
	    				//the local node has to send a message to a remote node that has the block modified.
	    				Message message = new Message();
    					message.homeNode = msg.homeNode;
    					message.messageType = Message.MODIFIED_DATA_REMOTE;
    					message.dataAddress = address;
    					message.localNote = msg.localNote;
    					message.remoteNode = msg.remoteNode;
    					int manhattanDistance = 3;//TODO To calculate manhattan distance
    					int executeCycle = clockcycle+manhattanDistance;
    					if(messageQueue.containsKey(executeCycle)){
    						messageQueue.get(executeCycle).add(message);
    					}else{
    						ArrayList<Message> al = new ArrayList<Message>();
    						al.add(message);
    						messageQueue.put(executeCycle, al);
    					}
    					
    					
	    			}else if(msg.messageType.equals(Message.MODIFIED_DATA_REMOTE)){
	    				//a node is requesting a modified block in this node.
	    				String coreid = msg.remoteNode;
	    				Processor processor = (Processor) processorsTable.get(coreid);
	    				
    					//send the modified block to the requesting node and change the state of the block to shared in both nodes
	    				processor.l2.directory.blocktable.get(address).state=Directory.SHARED_STATE;
	    				
	    				Message message = new Message();
    					message.homeNode = msg.homeNode;
    					message.messageType = Message.DATA_VALUE_REPLY;
    					message.dataAddress = address;
    					message.localNote = msg.localNote;
    					message.remoteNode = msg.remoteNode;
    					int manhattanDistance = 3;//TODO To calculate manhattan distance
    					int executeCycle = clockcycle+manhattanDistance;
    					if(messageQueue.containsKey(executeCycle)){
    						messageQueue.get(executeCycle).add(message);
    					}else{
    						ArrayList<Message> al = new ArrayList<Message>();
    						al.add(message);
    						messageQueue.put(executeCycle, al);
    					}
    					//send a message to the home node to inform it to change its block's state to shared
    					Message message1 = new Message();
    					message1.homeNode = msg.homeNode;
    					message1.messageType = Message.DATA_VALUE_REPLY;
    					message1.dataAddress = address;
    					message1.localNote = msg.homeNode;
    					message1.remoteNode = msg.remoteNode;
    					int manhattanDistance1 = 3;//TODO To calculate manhattan distance
    					int executeCycle1 = clockcycle+manhattanDistance;
    					if(messageQueue.containsKey(executeCycle1)){
    						messageQueue.get(executeCycle).add(message1);
    					}else{
    						ArrayList<Message> al = new ArrayList<Message>();
    						al.add(message1);
    						messageQueue.put(executeCycle, al);
    					}
	    			}else if(msg.messageType.equals(Message.WRITE_REQUEST)){
	    				//the home node change the block's owner to the requesting node.
	    				String coreid = msg.homeNode;
	    				Processor processor = (Processor) processorsTable.get(coreid);
	    				processor.l2.directory.blocktable.get(address).state=Directory.MODIFIED_STATE;
	    				processor.l2.directory.blocktable.get(address).owner = msg.localNote;
	    				//the home node sends a message with all the sharers to the requesting node
	    				Message message1 = new Message();
    					message1.homeNode = msg.homeNode;
    					message1.messageType = Message.WRITE_GRANTED;
    					message1.dataAddress = address;
    					message1.localNote = msg.localNote;
    					
    					int manhattanDistance = 3;//TODO To calculate manhattan distance
    					int executeCycle = clockcycle+manhattanDistance;
    					if(messageQueue.containsKey(executeCycle)){
    						messageQueue.get(executeCycle).add(message1);
    					}else{
    						ArrayList<Message> al = new ArrayList<Message>();
    						al.add(message1);
    						messageQueue.put(executeCycle, al);
    					}
	    			}else if(msg.messageType.equals(Message.WRITE_REQUEST)){
	    				//change the local block's state to modified
	    				String coreid = msg.localNote;
	    				Processor processor = (Processor) processorsTable.get(coreid);
	    				processor.l2.directory.blocktable.get(address).state=Directory.MODIFIED_STATE;
	    				
	    				//send invalidating messages to each sharers.
	    				processor = (Processor) processorsTable.get(msg.homeNode);
	    				for(int n=0;n<processor.l2.directory.blocktable.get(address).sharers.size();n++){
	    					Message message1 = new Message();
	    					message1.homeNode = msg.homeNode;
	    					message1.messageType = Message.INVALIDATE_NOTE;
	    					message1.dataAddress = address;
	    					message1.localNote = msg.localNote;
	    					message1.remoteNode = processor.l2.directory.blocktable.get(address).sharers.get(n);
	    					int manhattanDistance = 3;//TODO To calculate manhattan distance
	    					int executeCycle = clockcycle+manhattanDistance;
	    					if(messageQueue.containsKey(executeCycle)){
	    						messageQueue.get(executeCycle).add(message1);
	    					}else{
	    						ArrayList<Message> al = new ArrayList<Message>();
	    						al.add(message1);
	    						messageQueue.put(executeCycle, al);
	    					}
	    				}
	    				
	    			}else if(msg.messageType.equals(Message.INVALIDATE_NOTE)){
	    				//change the local block's state to modified
	    				String coreid = msg.remoteNode;
	    				Processor processor = (Processor) processorsTable.get(coreid);
	    				processor.l2.directory.blocktable.get(address).state=Directory.INVALID_STATE;
	    				
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
					//readOperation(String cacheLevel, String coreid, String address, int currentclockcycle)
    				readOperation("L1",cur.coreid,address,clockcycle);
				}else if(cur.operationFlag == 1){
					// Issue a write operation
					writeOperation("L1", cur.coreid,address, clockcycle);
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
	    
	  // load benchmarks, run and trace all the states
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
				item.address=hexToBinary(ss[3].substring(2));
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
	}

	void readOperation(String cacheLevel, String coreid, String address, int currentclockcycle){
		// Issue a read operation
		Processor processor = (Processor) processorsTable.get(coreid);
		boolean readHit = false;
        int l2indexbit = (int) (Math.log(p)/Math.log(2));
        boolean l1readHit = readHit(address,processor,n1,a1,b,0, "l1");
        boolean l2readHit = readHit(address,processor,n1,a1,b,l2indexbit, "l2");
        if(l1readHit || l2readHit){
        	//check the directory if the block state is valid(either exclusive or shared)
        	if(processor.l2.directory.blocktable.get(address).state==1 || processor.l2.directory.blocktable.get(address).state==2){
        		//Sucessfully read a block in l1
        		System.out.println("Sucessfully read a block in l1->"+"  cycle-"+currentclockcycle+"  coreid-"+coreid+"  address-"+address);
        	}else{
        		//the block's state is invalid, need to send a message to the home directory for the new value.
        		Message message = new Message();
        		if(l1readHit){
        			int setindexbit = n1-a1-b-0;
            		String homecoreid = address.substring(19-0+1, 20);
    				message.homeNode = homecoreid;
    				message.messageType = Message.READ_MISS_L1;
        		}else{
        			int setindexbit = n1-a1-b-l2indexbit;
            		String homecoreid = address.substring(19-l2indexbit+1, 20);
    				message.homeNode = homecoreid;
    				message.messageType = Message.READ_MISS_L2;
        		}
				message.dataAddress = address;
				message.localNote = coreid;
				int manhattanDistance = 3;//TODO To calculate manhattan distance
				int executeCycle = manhattanDistance*C+currentclockcycle;
				if(messageQueue.containsKey(executeCycle)){
					messageQueue.get(executeCycle).add(message);
				}else{
					ArrayList<Message> al = new ArrayList<Message>();
					al.add(message);
					messageQueue.put(executeCycle, al);
				}
				System.out.println("The block's state is invalid, send a message to the home node. :"+cacheLevel+" cache:"+address);
        	}
        }else{
        	//read miss, send a message to the home node
        	Message message = new Message();
        	int setindexbit = n1-a1-b-0;
    		String homecoreid = address.substring(19-0+1, 20);
			message.homeNode = homecoreid;
			message.messageType = Message.READ_MISS;
			message.dataAddress = address;
			message.localNote = coreid;
			int manhattanDistance = 3;//TODO To calculate manhattan distance
			int executeCycle = manhattanDistance*C+currentclockcycle;
			if(messageQueue.containsKey(executeCycle)){
				messageQueue.get(executeCycle).add(message);
			}else{
				ArrayList<Message> al = new ArrayList<Message>();
				al.add(message);
				messageQueue.put(executeCycle, al);
			}
			System.out.println("The block's state is uncached, send a message to the home node. :"+cacheLevel+" cache:"+address);
        }

	}

    Boolean readHit(String add, Processor pro, int n, int a, int b, int l2index,String l){
        boolean hit = false;
        int setindexbit = n-a-b-l2index;
        int assobit = a;
        String setloc = add.substring(32-setindexbit-12,20-l2index);
        String assoloc = add.substring(32-setindexbit-assobit-12,32-setindexbit-12);
        Block cached = new Block();
        if(l == "l1") {
            cached = pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(Integer.parseInt(assoloc, 2));
        }else if (l == "l2"){
            String l2id = add.substring(19-l2index+1, 20);
            Processor home = (Processor) processorsTable.get(l2id);
            cached = home.l2.setsList.get(Integer.parseInt(setloc,2)).blockList.get(Integer.parseInt(assoloc, 2));
        }

        if(cached.tag == add.substring(0,32-setindexbit-assobit-12+1)){
            if (cached.data == 0) {
                // no cache, a read-miss
            }else{
                hit = true;
            }
        }

        return hit;
    }
    String hexToBinary(String hex) {
        String value = new BigInteger(hex, 16).toString(2);
        String zero_pad = "0";
        for(int i=1;i<32-value.length();i++) zero_pad = zero_pad + "0";
        return zero_pad + value;
    }

	void writeOperation(String cacheLevel, String coreid, String address, int currentclockcycle){
		// Issue a read operation
		Processor processor = (Processor) processorsTable.get(coreid);
		boolean readHit = false;
		int l2indexbit = (int) (Math.log(p) / Math.log(2));
		boolean l1readHit = readHit(address, processor, n1, a1, b, 0, "l1");
		boolean l2readHit = readHit(address, processor, n1, a1, b, l2indexbit,"l2");
		if (l1readHit || l2readHit) {
			// check the directory if the block state is valid(either exclusive or shared)
			if (processor.l2.directory.blocktable.get(address).state == 1) {
				// Sucessfully write a block in l1
				System.out.println("Sucessfully write a block in l1->"+ "  cycle-" + currentclockcycle + "  coreid-" + coreid+ "  address-" + address);
			} else {
				// the block's state is invalid or shared, need to send a message to the home directory for modified state(exclusive).
				Message message = new Message();
				int setindexbit = n1 - a1 - b - 0;
				String homecoreid = address.substring(19 - 0 + 1, 20);
				message.homeNode = homecoreid;
				message.messageType = Message.WRITE_REQUEST;
				message.dataAddress = address;
				message.localNote = coreid;
				int manhattanDistance = 3;// TODO To calculate manhattan distance
				int executeCycle = manhattanDistance * C + currentclockcycle;
				if (messageQueue.containsKey(executeCycle)) {
					messageQueue.get(executeCycle).add(message);
				} else {
					ArrayList<Message> al = new ArrayList<Message>();
					al.add(message);
					messageQueue.put(executeCycle, al);
				}
				
			}
		} else {
			// read miss, send a message to the home node
			Message message = new Message();
			int setindexbit = n1 - a1 - b - 0;
			String homecoreid = address.substring(19 - 0 + 1, 20);
			message.homeNode = homecoreid;
			message.messageType = Message.WRITE_REQUEST;
			message.dataAddress = address;
			message.localNote = coreid;
			int manhattanDistance = 3;// TODO To calculate manhattan distance
			int executeCycle = manhattanDistance * C + currentclockcycle;
			if (messageQueue.containsKey(executeCycle)) {
				messageQueue.get(executeCycle).add(message);
			} else {
				ArrayList<Message> al = new ArrayList<Message>();
				al.add(message);
				messageQueue.put(executeCycle, al);
			}
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
