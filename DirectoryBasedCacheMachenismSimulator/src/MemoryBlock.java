import java.util.ArrayList;

/*
 * BlockAddress entity
 */
public class MemoryBlock {
	String address;
	String data;
	String owner;
	/*
	 * MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
	 */
	int state;
	ArrayList sharers = new ArrayList();
}
