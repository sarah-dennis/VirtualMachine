package operation_syntax;

public class OperationException extends Exception {

	private static final long serialVersionUID = 1L;
	private String _operation;
	
	public OperationException(String op) {
		_operation = op;
	} 
	
	public String getOperation() {
		return _operation;
	}
	
	public String getOpMessage() {
		return "Invalid operation " + _operation;
	}
}
