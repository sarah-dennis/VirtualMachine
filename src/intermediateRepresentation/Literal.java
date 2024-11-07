package intermediateRepresentation;
public class Literal extends Abs_Expression {
	
	private int _value;

	public Literal(int x) {
		_value = x;
	}

	public int get() {
		return _value;
	}
	
	@Override
	public String toString() {
		return "" + _value;
	}
	
}