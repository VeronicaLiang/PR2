/*
 * Block entity
 */
public class Block {
	String tag;
	String data;
	int state;// MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
	
}
