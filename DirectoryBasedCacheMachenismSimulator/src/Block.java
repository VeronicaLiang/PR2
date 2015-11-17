import java.util.ArrayList;

/*
 * Block entity
 */
public class Block {
	String tag;
	// use 0 indicates there is no data cached
	int data = 0;
	/*
	 * MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
	 */
	int state;
	
}
