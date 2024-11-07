package parse;

public class ParseException extends Exception {


	private static final long serialVersionUID = -792400898877506310L;
	private int _lineNumber;
	
	public ParseException(String message, Integer lineNumber) {
		super(message);
		_lineNumber = lineNumber;
	}
	
	public Integer getLineNumber() {
		return _lineNumber;
	}
}
