package intermediateRepresentation;

public class Return extends intermediateRepresentation {
	private Abs_Expression _value;
	
	public Return(Abs_Expression value) {
			_value = value;
	}
	
	public Return() {
		_value = null;
}
	
	public Abs_Expression getValue() {
		return _value;
	}
	@Override
	public String toString() {
		return "Return";
	}
}