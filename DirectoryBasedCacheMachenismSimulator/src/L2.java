import java.util.LinkedList;

/*
 * L2 entity, shared cache by all processors
 */
public class L2 {
	LinkedList setsList = new LinkedList();
	L2(int numberOfSetInL2,int a2){
		//Initialize the sets in the cache
		for(int i=0;i<numberOfSetInL2;i++){
			Set set = new Set(a2);
			setsList.add(set);
		}
	}
}
