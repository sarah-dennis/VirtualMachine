package intermediateRepresentation;

public class Symbol extends Abs_Expression {
	
	private String _symbol;
	
	public Symbol(String s) {
		_symbol = s;
	}
	
	public String get() {
		return _symbol;
	}
	
	@Override
	public String toString() {
		return _symbol;
	}
}