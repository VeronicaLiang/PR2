
/*
 * 
 */
public class Message {
	String remoteNodeAddress;
	String homeNodeAddress;
	String messageType;
	String dataAddress;
	
	final static String FATCH = "fatch";
	final static String READ_MISS_L1 = "readmissl1";
	final static String READ_INVALIDATE = "readinvalidate";
	final static String WRITE_MISS_L1 = "writemissl1";
	
	final static String WRITE_INVALIDATE = "writeinvalidate";
}
