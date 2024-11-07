package intermediateRepresentation;

public class DataLabel extends Abs_Expression{
	private Symbol _label;
	
	public DataLabel(String label) {
		_label = new Symbol(label);
	}
	
	public Symbol getLabel() {
		return _label;
	}
	
	@Override
	public String toString() {
		return _label.toString();
	}
}
