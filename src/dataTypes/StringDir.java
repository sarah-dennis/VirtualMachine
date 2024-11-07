package dataTypes;

public class StringDir extends DataType{
	
	private String _s;
	
	public StringDir(String s) {
		_s = s;
	}
	
	public byte[] getStringBytes () {
		return _s.getBytes();
	}
	
	@Override
	public String toString() {
		return _s;
	}
	
}
