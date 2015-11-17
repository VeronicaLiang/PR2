import java.util.LinkedList;

/*
 * L2 entity, shared cache by all processors
 */
public class L2 {
	/*
	 * L2's Directory
	 */
	Directory directory = new Directory();
	LinkedList<Set> setsList = new LinkedList();
	L2(int numberOfSetInL2,int associativity){
		//Initialize the sets in the cache
		for(int i=0;i<numberOfSetInL2;i++){
			Set set = new Set(associativity);
			setsList.add(set);
		}
	}
}
