package intermediateRepresentation;

public class Jump extends intermediateRepresentation {
	
	private Abs_Expression _savePC; //register only!
	private Abs_Expression _target;
	
	public Jump (Abs_Expression regForPC, Abs_Expression target) {
		_savePC = regForPC;
		_target = target;
	}
	
	public Abs_Expression getRegForPC() {
		return _savePC;
	}
	
	public Abs_Expression getTarget() {
		return _target;
	}
	
	@Override 
	public String toString() {
		String s = "Jump (" + _savePC.toString() + ", " + _target + ")";
		return s;
	}
}
