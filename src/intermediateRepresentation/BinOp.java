package intermediateRepresentation;

import operation_syntax.Operation;

public class BinOp extends Abs_Expression {
	
	private Operation _operation;
	private Abs_Expression _arg1;
	private Abs_Expression _arg2; //only assign literals to arg2
	
	public BinOp(Operation op, Abs_Expression arg1, Abs_Expression arg2) {
		_operation = op;
		_arg1 = arg1;
		_arg2 = arg2;
	}

	public Operation getOp() {
	 	return _operation;
	}
	 
	public Abs_Expression getArg1() {
		return _arg1;
	}
	
	public Abs_Expression getArg2() {
		return _arg2;
	}
	
	@Override
	public String toString() {
		String s = _operation + "(" + _arg1.toString() + ", " + _arg2.toString() + ")";
		return s;
	}
}
