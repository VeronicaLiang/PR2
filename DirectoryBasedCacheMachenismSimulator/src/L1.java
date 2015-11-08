import java.util.LinkedList;

/*
 * L1 entity, every processor has a l1 cache
 * And every l1 cache has a directory to manage its block coherence as a Home.
 */
public class L1 {
	/*
	 * L1's Directory
	 */
	Directory directory;
	/*
	 * L1's sets container
	 */
	LinkedList setsList = new LinkedList();
	L1(int numberOfSetInL1,int a1){
		//Initialize the sets in the cache
		for(int i=0;i<numberOfSetInL1;i++){
			Set set = new Set(a1);
			setsList.add(set);
		}
	}
}
