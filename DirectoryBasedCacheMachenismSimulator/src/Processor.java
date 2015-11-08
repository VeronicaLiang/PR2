
public class Processor {
	L1 l1;
	L2 l2;
	Processor(int numberOfSetInL1,int numberOfSetInL2,int a1,int a2){
		//Initialize l1 and l2
		l1 = new L1(numberOfSetInL1,a1);
		l2 = new L2(numberOfSetInL2,a2);
	}
	
}
