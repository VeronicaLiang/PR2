import java.util.ArrayList;
import java.util.Hashtable;

/*
 * Directory entity
 */
public class Directory {
	public final static int INVALID_STATE = 0;
	public final static int MODIFIED_STATE = 1;
	public final static int SHARED_STATE = 2;
	
	Hashtable<String,OwnerAndSharers> blocktable = new Hashtable<String,OwnerAndSharers>();
	
}

