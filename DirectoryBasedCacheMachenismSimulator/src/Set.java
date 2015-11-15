import java.util.LinkedList;

/*
 * Set entity, every cache has a variety of sets, depends on what the associativity is.
 */
public class Set {
	Set(int associativity){
		for(int i=0;i<associativity;i++){
			Block block = new Block();
			blockList.add(block);
		}
	}
	LinkedList<Block> blockList = new LinkedList();

}
