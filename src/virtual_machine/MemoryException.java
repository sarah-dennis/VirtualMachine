package virtual_machine;

public class MemoryException extends Exception{

	private static final long serialVersionUID = 1L;
	String _message;

	public MemoryException(String message) {
		_message = message;
	}

	public String getMessage() {
		return _message;
	}
}
