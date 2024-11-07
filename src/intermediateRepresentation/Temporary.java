package intermediateRepresentation;

public class Temporary extends Abs_Expression {
	
	private String _register;
	private boolean _global;

	public Temporary(String reg) {
		_register = reg;
		_global = false;
	}
	
	public Temporary(String reg, boolean global) {
		_register = reg;
		_global = global;
	}
	
	public String get() {
		return _register;
	}
	
	public boolean isGlobal() {
		return _global;
	}

	@Override
	public String toString() {
		if(!_global) {
			return "T(" + _register + ")";
		}else {
			return "$" + _register ;
		}
		
	}
}