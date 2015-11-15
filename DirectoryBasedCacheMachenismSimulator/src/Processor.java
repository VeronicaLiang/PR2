import java.util.ArrayList;
import java.util.HashMap;


public class Processor {
	L1 l1;
	L2 l2;
	
	Processor(int numberOfSetInL1,int numberOfSetInL2,int associativityOfL1,int associativityOfL2){
		//Initialize l1 and l2
		l1 = new L1(numberOfSetInL1,associativityOfL1);
		l2 = new L2(numberOfSetInL2,associativityOfL2);
	}

}
