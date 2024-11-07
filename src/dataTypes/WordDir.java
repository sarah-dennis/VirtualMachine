package dataTypes;

public class WordDir extends DataType{
	private int _x;
	public WordDir(int x) {
		_x = x;
	}
	public int getValue() {
		return _x;
	}
	
	@Override
	public String toString() {
		return String.valueOf(_x);
	}
}
