import java.util.HashMap;

/*
 * Translation Lookaside Buffer
 */
public class TLB {
	private static TLB tlb;
	private TLB(){}
	public static TLB getInstance(){
		if(tlb==null){
			tlb = new TLB();
		}
		return tlb;
	}
	private HashMap buffer = new HashMap();
	public boolean isAvailableInTLB(String physicalAddress){
		return buffer.containsKey(physicalAddress);
	}
	public String getVirtualAddress(String physicalAddress){
		return (String) buffer.get(physicalAddress);
	}
	public String translatePhysicalToVirtual(String physicalAddress,Processor processor){
		//TODO
		return (String) buffer.get(physicalAddress);
	}
}
