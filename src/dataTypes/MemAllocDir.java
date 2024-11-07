package dataTypes;

public class MemAllocDir extends DataType{
	private int _size;

	public MemAllocDir(int size) {
		_size = size;
		
	}
	
	public int getSize() {
		return _size;
	}
	
	@Override
	public String toString() {
		return "Allocate(" + _size + ")";
	}
	

}
