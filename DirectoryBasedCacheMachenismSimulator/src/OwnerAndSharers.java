import java.util.ArrayList;


public class OwnerAndSharers{
		public OwnerAndSharers(){}
		/*
		 * MSI when state==0 then state-> invalid, when state==1 then state-> modified, when state==2 then state->shared
		 */
		int state;
		/*
		 * coreid
		 */
		String homeNode;
		/*
		 * coreid
		 */
		String owner;
		/*
		 * a list of coreids
		 */
		ArrayList<String> sharers = new ArrayList<String>();
}
