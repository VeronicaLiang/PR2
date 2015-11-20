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
	int p = 0;
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
	 * The number of delay cycles caused by communicating between two nodes(a
	 * node consists of a processor and l1 cache)
	 */
	int C = 0;
	/*
	 * The number of cycles caused by a l2 hit(The l1 hit is satisfied in the
	 * same cycle in which it is issued)
	 */
	int d = 0;
	/*
	 * The number of cycles caused by a memory access
	 */
	int d1 = 0;

	LinkedList writeBuffer = new LinkedList();// Stores the blocks that are
												// going te be flushed back to
												// memory
	HashMap processorsTable = new HashMap();
	HashMap<Integer, ArrayList<Message>> messageQueue = new HashMap<Integer, ArrayList<Message>>();

	Memory memory;
	/*
	 * If a access to L2 is a miss, then it will require sending a message t
	 * on-chip memory controller which is co-located with tile0, a memory
	 * access, which assumed to take d1 cycles(uniform memory access), and a
	 * message back from the memory controller.
	 */
	// Message memoryController;

	TLB tlb;

	public Simulator(String inputFile, int p, int n1, int n2, int b, int a1, int a2, int C, int d, int d1) {
		this.p = p;
		this.n1 = n1;
		this.n2 = n2;
		this.b = b;
		this.a1 = a1;
		this.a2 = a2;
		this.C = C;
		this.d = d;
		this.d1 = d1;
		Hashtable<String, ArrayList> commands = initializeUnits(inputFile);
		int clockcycle = 1;
		boolean finish = false;
		while (!finish) {
			// execute all the commands in the message queue
			if (messageQueue.containsKey(clockcycle)) {
				ArrayList<Message> msgs = messageQueue.get(clockcycle);
				for (int i = 0; i < msgs.size(); i++) {
					// execute all the messages in the message queue
					Message msg = msgs.get(i);
					String address = msg.dataAddress;

					if (msg.messageType.equals(Message.MISS_L1)) {
						// l1 miss, check whether the home has the block or not
						String coreid = msg.homeNode;
						Processor processor = (Processor) processorsTable.get(coreid);
						if (processor.l2.directory.blocktable.contains(address)) {
							// if the home node has the block, then check its
							// state
							if (processor.l2.directory.blocktable.get(address).state == 1) {
								// miss, and block is exclusive in another cache
								// one remote has the block modified. send a
								// message to request modified value
								Message message = new Message();
								message.homeNode = msg.homeNode;
								message.messageType = Message.DATA_IN_REMOTE;
								message.dataAddress = address;
								message.localNode = msg.localNode;
								message.remoteNode = processor.l2.directory.blocktable.get(address).owner;
								message.blockStatus = msg.blockStatus;
								int manhattanDistance = getManhattanDistance(coreid, msg.localNode, p);
								int executeCycle = clockcycle + manhattanDistance * C;
								if (messageQueue.containsKey(executeCycle)) {
									messageQueue.get(executeCycle).add(message);
								} else {
									ArrayList<Message> al = new ArrayList<Message>();
									al.add(message);
									messageQueue.put(executeCycle, al);
								}
								System.out.println(coreid + ": L2 Hit, but block is exclusive,"
										+ " return Remote node id to " + msg.localNode + ". This is a small message.");
							} else {
								// miss, and block is shared
								// if the state of the block is either shared or
								// modified, send it to the requesting node.
								Message message = new Message();
								message.homeNode = msg.homeNode;
								if (msg.blockStatus == Directory.SHARED_STATE) {
									message.messageType = Message.DATA_VALUE_REPLY;
								} else {
									message.messageType = Message.WRITE_REQUEST;
								}
								message.hit = msg.hit;
								message.dataAddress = address;
								message.localNode = msg.localNode;
								message.remoteNode = msg.remoteNode;
								message.blockStatus = msg.blockStatus;
								int manhattanDistance = getManhattanDistance(coreid, msg.localNode, p);
								int executeCycle = clockcycle + manhattanDistance * C + d;
								if (messageQueue.containsKey(executeCycle)) {
									messageQueue.get(executeCycle).add(message);
								} else {
									ArrayList<Message> al = new ArrayList<Message>();
									al.add(message);
									messageQueue.put(executeCycle, al);
								}
								processor.l2.directory.blocktable.get(address).state = Directory.SHARED_STATE;
								System.out.println(coreid + ": L2 Hit, and block is shared, return data to "
										+ msg.localNode + ". This is a large message.");
							}
						} else {
							// miss, and block is uncached
							// if the home node does not have the block, then
							// send message to tail 0 to fetch data
							Message message = new Message();
							message.homeNode = msg.homeNode;
							message.messageType = Message.FETCH_DATA_FROM_MEM_CTRL;
							message.dataAddress = address;
							message.localNode = msg.localNode;
							message.blockStatus = msg.blockStatus;
							int manhattanDistance = getManhattanDistance(msg.homeNode, "0", p);
							int executeCycle = clockcycle + manhattanDistance * C;
							if (messageQueue.containsKey(executeCycle)) {
								messageQueue.get(executeCycle).add(message);
							} else {
								ArrayList<Message> al = new ArrayList<Message>();
								al.add(message);
								messageQueue.put(executeCycle, al);
							}

							System.out.println(coreid + ": L2 miss, send a request to Memory Controller to "
									+ " fetch data from memory. This is a small message.");
						}
					} else if (msg.messageType.equals(Message.FETCH_DATA_FROM_MEM_CTRL)) {
						// read miss, and block is uncached, home node send a
						// request to mem ctrl
						Message message = new Message();
						message.homeNode = msg.homeNode;
						message.messageType = Message.RETURN_DATA_FROM_MEM_CTRL;
						message.dataAddress = address;
						message.localNode = msg.localNode;
						message.blockStatus = msg.blockStatus;
						int manhattanDistance = getManhattanDistance(msg.homeNode, "0", p);
						int executeCycle = clockcycle + manhattanDistance * C + d1;
						if (messageQueue.containsKey(executeCycle)) {
							messageQueue.get(executeCycle).add(message);
						} else {
							ArrayList<Message> al = new ArrayList<Message>();
							al.add(message);
							messageQueue.put(executeCycle, al);
						}

						System.out.println(
								"0: Get request from " + msg.homeNode + ", fetch data from memory and return to "
										+ msg.homeNode + ". This is a large message.");

					} else if (msg.messageType.equals(Message.RETURN_DATA_FROM_MEM_CTRL)) {
						// read miss, and block is uncached, home node send a
						// request to mem ctrl, and get result, return data to
						// local node
						Message message = new Message();
						message.homeNode = msg.homeNode;
						message.messageType = Message.DATA_VALUE_REPLY;
						message.dataAddress = address;
						message.localNode = msg.localNode;
						message.remoteNode = msg.remoteNode;
						message.blockStatus = msg.blockStatus;
						int manhattanDistance = getManhattanDistance(msg.homeNode, msg.localNode, p);
						int executeCycle = clockcycle + manhattanDistance * C;
						if (messageQueue.containsKey(executeCycle)) {
							messageQueue.get(executeCycle).add(message);
						} else {
							ArrayList<Message> al = new ArrayList<Message>();
							al.add(message);
							messageQueue.put(executeCycle, al);
						}
						String coreid = msg.homeNode;
						Processor processor = (Processor) processorsTable.get(coreid);
						storeBlockToCache(); // store the block into l2, no need
												// to set status for l2
						processor.l2.directory.blocktable.get(address).state = msg.blockStatus;

						if (msg.blockStatus == Directory.SHARED_STATE) {
							System.out.println(
									msg.homeNode + ": Get data from 0, save to L2," + " set directory status to shared,"
											+ " and return to " + msg.localNode + ". This is a large message.");
						} else {
							// TODO need add owner here
							System.out.println(msg.homeNode + ": Get data from 0, save to L2,"
									+ " set directory status to exclusive," + " and return to " + msg.localNode
									+ ". This is a large message.");
						}

					} else if (msg.messageType.equals(Message.DATA_VALUE_REPLY)) {
						// Local node get data, then write data into l1, and set
						// state of block to shared
						String coreid = msg.localNode;
						Processor processor = (Processor) processorsTable.get(coreid);
						storeBlockToCache(); // store the block into l1
						setBlockStatus(msg.blockStatus); // set state of block
															// to
															// msg.blockStatus
						if (msg.blockStatus == Directory.SHARED_STATE) {
							System.out.println(
									coreid + ": Get data from " + msg.homeNode + ", save to L1, set status to shared.");
						} else {
							System.out.println(coreid + ": Get data from " + msg.homeNode
									+ ", save to L1, set status to exclusive.");
						}

					} else if (msg.messageType.equals(Message.DATA_IN_REMOTE)) {
						// the local node has to send a message to a remote node
						// that has the block modified.
						Message message = new Message();
						message.homeNode = msg.homeNode;
						message.messageType = Message.MODIFIED_DATA_REMOTE;
						message.dataAddress = address;
						message.localNode = msg.localNode;
						message.remoteNode = msg.remoteNode;
						message.blockStatus = msg.blockStatus;
						int manhattanDistance = getManhattanDistance(msg.localNode, msg.remoteNode, p);
						int executeCycle = clockcycle + manhattanDistance * C;
						if (messageQueue.containsKey(executeCycle)) {
							messageQueue.get(executeCycle).add(message);
						} else {
							ArrayList<Message> al = new ArrayList<Message>();
							al.add(message);
							messageQueue.put(executeCycle, al);
						}
						System.out.println(
								msg.localNode + ": Get Remote node from home node, send request to Remote node:"
										+ msg.remoteNode + ". This is a small message.");
					} else if (msg.messageType.equals(Message.MODIFIED_DATA_REMOTE)) {
						// a node is requesting a modified block in this node.
						String coreid = msg.remoteNode;
						Processor processor = (Processor) processorsTable.get(coreid);
						Message message = new Message();
						message.homeNode = msg.homeNode;
						message.messageType = Message.DATA_VALUE_REPLY;
						message.dataAddress = address;
						message.localNode = msg.localNode;
						message.remoteNode = msg.remoteNode;
						message.blockStatus = msg.blockStatus;
						int manhattanDistance = getManhattanDistance(msg.localNode, msg.remoteNode, p);
						int executeCycle = clockcycle + manhattanDistance * C;
						if (messageQueue.containsKey(executeCycle)) {
							messageQueue.get(executeCycle).add(message);
						} else {
							ArrayList<Message> al = new ArrayList<Message>();
							al.add(message);
							messageQueue.put(executeCycle, al);
						}

						if (msg.blockStatus == Directory.SHARED_STATE) {
							setBlockStatus(msg.blockStatus); // set state of
																// block
							// to
							// msg.blockStatus
							System.out.println(msg.remoteNode + ": Get data and send to Local Node:" + msg.localNode
									+ ". This is a large message.");
						} else {
							setBlockStatus(Directory.INVALID_STATE); // set
																		// state
																		// of
																		// block
							System.out.println(msg.remoteNode + ": Get data and send to Local Node:" + msg.localNode
									+ ", set block status to invalid. This is a large message.");
						}

						Message message1 = new Message();
						message1.homeNode = msg.homeNode;
						message1.messageType = Message.SET_DIR_STATUS;
						message1.dataAddress = address;
						message1.localNode = msg.homeNode;
						message1.remoteNode = msg.remoteNode;
						message1.blockStatus = msg.blockStatus;
						int manhattanDistance1 = getManhattanDistance(msg.homeNode, msg.remoteNode, p);
						int executeCycle1 = clockcycle + manhattanDistance1 * C;
						if (messageQueue.containsKey(executeCycle1)) {
							messageQueue.get(executeCycle).add(message1);
						} else {
							ArrayList<Message> al = new ArrayList<Message>();
							al.add(message1);
							messageQueue.put(executeCycle, al);
						}
						System.out.println(msg.remoteNode + ": Send request to Home:" + msg.homeNode
								+ " to change the status of directory to shared. This is a large message.");
					} else if (msg.messageType.equals(Message.SET_DIR_STATUS)) {
						String coreid = msg.homeNode;
						Processor processor = (Processor) processorsTable.get(coreid);
						processor.l2.directory.blocktable.get(address).state = msg.blockStatus;
						if (msg.blockStatus == Directory.SHARED_STATE) {
							storeBlockToCache();
							System.out.println(coreid + ": Set state of block to shared in directory.");
						} else {
							// TODO change owner to local node
							System.out.println(coreid + ": Set state of block to exclusive in directory.");
						}

					} else if (msg.messageType.equals(Message.WRITE_REQUEST)) {
						// the home node change the block's owner to the
						// requesting node.
						String coreid = msg.homeNode;
						Processor processor = (Processor) processorsTable.get(coreid);
						// set the state to exclusive in directory
						processor.l2.directory.blocktable.get(address).state = Directory.MODIFIED_STATE;
						processor.l2.directory.blocktable.get(address).owner = msg.localNode;
						// the home node sends a message with all the sharers to
						// the requesting node
						Message message = new Message();
						message.homeNode = msg.homeNode;
						message.messageType = Message.WRITE_GRANTED;
						message.dataAddress = address;
						message.localNode = msg.localNode;
						message.blockStatus = msg.blockStatus;
						message.sharers = processor.l2.directory.blocktable.get(address).sharers;

						int manhattanDistance = getManhattanDistance(msg.homeNode, msg.localNode, p);
						int executeCycle = clockcycle + manhattanDistance * C;
						if (messageQueue.containsKey(executeCycle)) {
							messageQueue.get(executeCycle).add(message);
						} else {
							ArrayList<Message> al = new ArrayList<Message>();
							al.add(message);
							messageQueue.put(executeCycle, al);
						}
						// if we can update data now, uncomment following line
						// or we have to update cache at the end
						// storeBlockToCache();
						if (msg.hit) {
							System.out
									.println(coreid + ": Return sharers list to local node, and set state to exclusive."
											+ " This is a small message.");
						} else {
							System.out.println(
									coreid + ": Return sharers list and data to local node, and set state to exclusive."
											+ " This is a large message.");
						}

					} else if (msg.messageType.equals(Message.WRITE_GRANTED)) {
						// change the local block's state to modified
						String coreid = msg.localNode;

						// send invalidating messages to each sharers.
						for (int n = 0; n < msg.sharers.size(); n++) {
							if (!msg.sharers.get(n).equals(msg.localNode)){
								Message message1 = new Message();
								message1.homeNode = msg.homeNode;
								message1.messageType = Message.INVALIDATE_NOTE;
								message1.dataAddress = address;
								message1.localNode = msg.localNode;
								message1.remoteNode = msg.sharers.get(n);
								int manhattanDistance = getManhattanDistance(msg.remoteNode, msg.localNode, p);
								int executeCycle = clockcycle + manhattanDistance * C;
								if (messageQueue.containsKey(executeCycle)) {
									messageQueue.get(executeCycle).add(message1);
								} else {
									ArrayList<Message> al = new ArrayList<Message>();
									al.add(message1);
									messageQueue.put(executeCycle, al);
								}
							}
							
						}

						setBlockStatus(msg.blockStatus); // set local block
															// status to
															// exclusive
						System.out.println(coreid + ": sends invalidating messages to each sharers."
								+ " This is a small message.");

					} else if (msg.messageType.equals(Message.INVALIDATE_NOTE)) {
						// change the local block's state to modified
						String coreid = msg.remoteNode;
						setBlockStatus(msg.blockStatus); // set local block
															// status to invalid
						System.out.println(coreid + ": Set state of block to invalid.");
					}
				}
			}

			// extract all commands need to operate in this clock cycle
			ArrayList instructions = new ArrayList();
			instructions = commands.get(String.valueOf(clockcycle));
			for (int i = 0; i < instructions.size(); i++) {
				TraceItem cur = (TraceItem) instructions.get(i);
				Processor processor = (Processor) processorsTable.get(cur.coreid);
				String address = cur.address;
				if (cur.operationFlag == 0) {
					// Issue a read operation
					// readOperation(String cacheLevel, String coreid, String
					// address, int currentclockcycle)
					readOperation(cur.coreid, address, clockcycle);
				} else if (cur.operationFlag == 1) {
					// Issue a write operation
					writeOperation("L1", cur.coreid, address, clockcycle);
				}
			}
			clockcycle++;
			// TODO need to check when all operations are finished then set up
			// the finish flag = true
		}

	}

	int getBlockStatus() {
		// TODO get state of block in node
		return 0;
	}

	void setBlockStatus(int blockStatus) {
		// TODO set state of block in node to some state
	}

	void storeBlockToCache() {
		// TODO store block to cache
	}

	Hashtable<String, ArrayList> initializeUnits(String inputFile) {
		// Initialize
		// processors===============================================================
		int base = 2;
		// the size of l1
		int sizeOfl1 = (int) Math.pow(base, n1);
		// the number of blocks in the l1=the size of l1/the size of a block
		int numberOfBlocksInL1 = sizeOfl1 / ((int) Math.pow(base, b));

		// the the associativity of l1
		int associativityOfL1 = (int) Math.pow(base, a1);
		// so the number of sets in the l1=the number of blocks in the l1/the
		// associativity of l1
		// int numberOfSetInL1 = numberOfBlocksInL1/associativityOfL1;
		int numberOfSetInL1 = (int) Math.pow(base, (n1 - a1 - b));

		// the size of l1
		int sizeOfl2 = (int) Math.pow(base, n2);
		// the number of blocks in the l2=the size of l2/the size of a block
		int numberOfBlocksInL2 = sizeOfl2 / ((int) Math.pow(base, b));

		// the the associativity of l2
		int associativityOfL2 = (int) Math.pow(base, a2);
		// so the number of sets in the l2=the number of blocks in the l2/the
		// associativity of l2
		// int numberOfSetInL2 = numberOfBlocksInL2/associativityOfL2;
		int numberOfSetInL2 = (int) Math.pow(base, (n2 - a2 - b));

		int processorsNumber = (int) Math.pow(base, this.p);
		for (int i = 0; i < processorsNumber; i++) {
			Processor processor = new Processor(numberOfSetInL1, numberOfSetInL2, associativityOfL1, a2);
			processorsTable.put(i + "", processor);
		}
		// Initialize memory=====Need memory to keep ultimate states of a block
		memory = Memory.getInstance();

		// load benchmarks, run and trace all the states
		String line = null;
		// Use a hashtable to record all commands from the trace file. The key
		// is the clock cycle, so that in each cycle
		// the commands that need to operate can be easily extracted.
		Hashtable<String, ArrayList> commands = new Hashtable<String, ArrayList>();
		try {
			FileReader filereader = new FileReader(inputFile);
			BufferedReader bufferedreader = new BufferedReader(filereader);
			while ((line = bufferedreader.readLine()) != null) {
				String[] ss = line.split(" ");
				TraceItem item = new TraceItem();
				item.cycle = Integer.parseInt(ss[0]);
				item.coreid = ss[1];
				item.operationFlag = Integer.parseInt(ss[2]);
				item.address = hexToBinary(ss[3].substring(2));
				boolean ccexist = commands.containsKey(ss[0]);
				if (ccexist) {
					commands.get(ss[0]).add(item);
				} else {
					ArrayList tmp = new ArrayList();
					tmp.add(item);
					commands.put(ss[0], tmp);
				}
				// traceList.add(item);
				// add this memory address to memory
				MemoryBlock block = new MemoryBlock();
				memory.addMemoryBlock(item.address, block);
				System.out.println("read trace file line->" + "  cycle-" + item.cycle + "  coreid-" + item.coreid
						+ "  operationFlag-" + item.operationFlag + "  address-" + item.address);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return commands;
	}

	void readOperation(String coreid, String address, int currentclockcycle) {
		// Issue a read operation
		Processor processor = (Processor) processorsTable.get(coreid);
		int l2indexbit = (int) (Math.log(p) / Math.log(2));
		boolean l1readHit = hitOrMiss(address, processor, n1, a1, b, 0, "l1");
		if (l1readHit) {
			System.out.println(coreid + ": L1 read hit");
		} else {
			// l1 miss, then we need to check l2 at home node
			Message message = new Message();
			String homecoreid = address.substring(19 - l2indexbit + 1, 20);
			message.homeNode = homecoreid;
			message.messageType = Message.MISS_L1;
			message.dataAddress = address;
			message.localNode = coreid;
			message.blockStatus = Directory.SHARED_STATE;
			int manhattanDistance = getManhattanDistance(coreid, homecoreid, p);
			int executeCycle = manhattanDistance * C + currentclockcycle;
			if (messageQueue.containsKey(executeCycle)) {
				messageQueue.get(executeCycle).add(message);
			} else {
				ArrayList<Message> al = new ArrayList<Message>();
				al.add(message);
				messageQueue.put(executeCycle, al);
			}
			System.out.println(coreid + ": L1 miss, send a message to the home node:" + homecoreid
					+ " to request data. This is a small message.");
		}
	}

    // TODO check whether this calculation is correct
	int getManhattanDistance(String coreid, String homecoreid, int p) {
		int edge = (int) Math.pow(2, p/2);
        int core_x = Integer.parseInt(coreid)/edge;
        int core_y = Integer.parseInt(coreid)%edge;
        int home_x = Integer.parseInt(homecoreid)/edge;
        int home_y = Integer.parseInt(homecoreid)%edge;

        int dist = Math.abs(core_x - core_y) + Math.abs(home_x - home_y);

		return dist;
	}

	// TODO I change the method name to hit or miss
	Boolean hitOrMiss(String add, Processor pro, int n, int a, int b, int l2index, String l) {
		boolean hit = false;
		int setindexbit = n - a - b - l2index;
		int assobit = a;
		String setloc = add.substring(32 - setindexbit - 12, 20 - l2index);
		String assoloc = add.substring(32 - setindexbit - assobit - 12, 32 - setindexbit - 12);
		Block cached = new Block();
		if (l == "l1") {
			cached = pro.l1.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(Integer.parseInt(assoloc, 2));
		} else if (l == "l2") {
			String l2id = add.substring(19 - l2index + 1, 20);
			Processor home = (Processor) processorsTable.get(l2id);
			cached = home.l2.setsList.get(Integer.parseInt(setloc, 2)).blockList.get(Integer.parseInt(assoloc, 2));
		}

		if (cached.tag == add.substring(0, 32 - setindexbit - assobit - 12 + 1)) {
			if (cached.data == 0) {
				// no cache, a read-miss
			} else {
				hit = true;
			}
		}

		return hit;
	}

	String hexToBinary(String hex) {
		String value = new BigInteger(hex, 16).toString(2);
		String zero_pad = "0";
		for (int i = 1; i < 32 - value.length(); i++)
			zero_pad = zero_pad + "0";
		return zero_pad + value;
	}

	void writeOperation(String cacheLevel, String coreid, String address, int currentclockcycle) {
		// Issue a read operation
		Processor processor = (Processor) processorsTable.get(coreid);
		int l2indexbit = (int) (Math.log(p) / Math.log(2));
		boolean l1writeHit = hitOrMiss(address, processor, n1, a1, b, 0, "l1");
		if (l1writeHit) {
			// the block's state is shared or exclusive in local cache
			int blockStatus = getBlockStatus(); // get block status
			if (blockStatus == 1) {
				// if block status is exclusive
				System.out.println(coreid + ": L1 write hit, write the data.");
			} else {
				// if block status is shared
				Message message = new Message();
				String homecoreid = address.substring(19 - l2indexbit + 1, 20);
				message.homeNode = homecoreid;
				message.messageType = Message.WRITE_REQUEST;
				message.dataAddress = address;
				message.localNode = coreid;
				message.blockStatus = Directory.MODIFIED_STATE;
				message.hit = true;
				int manhattanDistance = getManhattanDistance(coreid, homecoreid, p);
				int executeCycle = manhattanDistance * C + currentclockcycle;
				if (messageQueue.containsKey(executeCycle)) {
					messageQueue.get(executeCycle).add(message);
				} else {
					ArrayList<Message> al = new ArrayList<Message>();
					al.add(message);
					messageQueue.put(executeCycle, al);
				}
				System.out.println(
						coreid + ": L1 write hit, but status of block is shared," + " send a message to the home node:"
								+ homecoreid + " to request sharers list. This is a small message.");
			}

		} else {
			// write miss, send a message to the home node

			Message message = new Message();
			String homecoreid = address.substring(19 - l2indexbit + 1, 20);
			message.homeNode = homecoreid;
			message.messageType = Message.MISS_L1;
			message.dataAddress = address;
			message.localNode = coreid;
			message.blockStatus = Directory.MODIFIED_STATE;
			message.hit = false;
			int manhattanDistance = getManhattanDistance(coreid, homecoreid, p);
			int executeCycle = manhattanDistance * C + currentclockcycle;
			if (messageQueue.containsKey(executeCycle)) {
				messageQueue.get(executeCycle).add(message);
			} else {
				ArrayList<Message> al = new ArrayList<Message>();
				al.add(message);
				messageQueue.put(executeCycle, al);
			}
			System.out.println(coreid + ": L1 write miss, send a message to the home node:" + homecoreid
					+ " to request sharers list and data. This is a small message.");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputFile = args[0];
		int p = Integer.parseInt(args[1]);// The power of processors with a root
											// of 2
		int n1 = Integer.parseInt(args[2]);// The power of the size of every l1
											// with a root of 2
		int n2 = Integer.parseInt(args[3]);// The power of the size of every l2
											// with a root of 2
		int b = Integer.parseInt(args[4]);// The size of a block
		int a1 = Integer.parseInt(args[5]);// The power of the associativity of
											// l1 with a root of 2
		int a2 = Integer.parseInt(args[6]);// The power of the associativity of
											// l2 with a root of 2
		int C = Integer.parseInt(args[7]);// The number of delay cycles caused
											// by communicating between two
											// nodes(a node consists of a
											// processor and l1 cache)
		int d = Integer.parseInt(args[8]);// The number of cycles caused by a l2
											// hit(The l1 hit is satisfied in
											// the same cycle in which it is
											// issued)
		int d1 = Integer.parseInt(args[9]);// The number of cycles caused by a
											// memory access
		Simulator simulator = new Simulator(inputFile, p, n1, n2, b, a1, a2, C, d, d1);
	}

}
