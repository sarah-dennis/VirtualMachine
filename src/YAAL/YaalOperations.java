package YAAL;

import java.util.HashMap;
import java.util.Map;

import operation_syntax.Operation;

public class YaalOperations {
	
	private static Map<String, Operation> _operations = new HashMap<>();

	public Operation opOf(String op) {
		return _operations.get(op);
	}
	
	public YaalOperations() {
		_operations.put("+",Operation.ADD);
		_operations.put("-", Operation.SUB);
		_operations.put("*", Operation.MULT);
		_operations.put("/",Operation.DIV);
		_operations.put("%", Operation.REM);
		
		_operations.put("==", Operation.EQUAL);
		_operations.put("!=", Operation.NOT_EQUAL);
		_operations.put("<=", Operation.LESS_EQUAL);
		_operations.put("<", Operation.LESS);
		_operations.put(">=", Operation.GREATER_EQUAL);
		_operations.put(">", Operation.GREATER);
		
	}
	

}
