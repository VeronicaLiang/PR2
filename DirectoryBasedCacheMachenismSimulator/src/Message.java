
/*
 * 
 */
public class Message {
	String localNode;
	String remoteNode;
	String homeNode;
	String messageType;
	String dataAddress;
	
	final static String READ_MISS = "readmiss";
	final static String READ_MISS_L1 = "readmissl1";
	final static String READ_MISS_L2 = "readmissl2";
	final static String FETCH_DATA_MEMORY = "fetchdatamemory";
	final static String DATA_VALUE_REPLY = "datavaluereply";
	final static String DATA_IN_REMOTE = "datainremote";
	final static String MODIFIED_DATA_REMOTE = "modifieddataremote";
	final static String WRITE_REQUEST = "writerequest";
	final static String WRITE_GRANTED = "writegranted";
	final static String INVALIDATE_NOTE = "invalidatenote";
	//final static String FATCH = "fatch";
	//final static String MEMORY_TO_CACHE = "memorytocache";
	//final static String READ_INVALIDATE = "readinvalidate";
	//final static String WRITE_MISS_L1 = "writemissl1";
	//final static String WRITE_INVALIDATE = "writeinvalidate";
}
