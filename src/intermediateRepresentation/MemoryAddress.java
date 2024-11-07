package intermediateRepresentation;

public class MemoryAddress extends Abs_Expression {
	
	private Abs_Expression _address;
	private int _size;
	
	public MemoryAddress(Abs_Expression address, int size) {
		_address = address;
		_size = size;
	}
	
	public Abs_Expression getAddress() {
		return _address;
	}
	
	public int getSize() {
		return _size;
	}
	
	@Override
	public String toString() {
		return "Mem" + byteAmt(_size)+ " (" +  _address + ")";
	}
	
	private String byteAmt(int size) {
		if(size == 1) {
			return "<byte>";
		}else if(size == 2) {
			return "<half>";
		}else {
			return "<word>";
		}
	}
}