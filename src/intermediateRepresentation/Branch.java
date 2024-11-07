package intermediateRepresentation;

public class Branch extends intermediateRepresentation {
	
	private Abs_Expression _relOp;
	private Abs_Expression _destination;
	
	public Branch(Abs_Expression relOp, Abs_Expression destination) {
		_relOp = relOp;
		_destination = destination;
	}
	
	public Abs_Expression getRelOp() {
		return _relOp;
	}
	
	public Abs_Expression getDestination() {
		return _destination;
	}
	
	@Override 
	public String toString() {
		String s = "Branch (" + _destination.toString() + ", " + _relOp.toString() + ")";
		return s;
	}
}


