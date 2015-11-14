import java.util.ArrayList;

/*
 * Block entity
 */
public class Block {
	String tag;
	String data;
	/*
	 * MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
	 */
	int state;
	/*
	 * nodeid:set index:block index  1:1:1
	 */
	String owner;
	ArrayList sharers = new ArrayList();
}
