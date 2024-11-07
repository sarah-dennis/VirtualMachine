package RISC;

import java.util.HashMap;
import java.util.Map;

import operation_syntax.Operation;
import operation_syntax.OperationException;

public class RiscOperations{

	private static Map<String, Operation> _operations = new HashMap<>();

	public Map<String, Operation> getOperations() {
		return _operations;
	}
	
	public boolean isOperation(String op) throws OperationException {
		if (_operations.containsKey(op)) {
			return true;
		}else {
			throw new OperationException(op);
		}
	}

	public Operation opOf(String op) {
		return _operations.get(op);
	}

	public RiscOperations(){
		_operations.put("add", Operation.ADD);
		_operations.put("sub", Operation.SUB);
		_operations.put("sll", Operation.SHIFT_LEFT);
		_operations.put("srl", Operation.SHIFT_RIGHT_L);
		_operations.put("sra", Operation.SHIFT_RIGHT_A);
		_operations.put("xor", Operation.X_OR);
		_operations.put("and", Operation.AND);
		_operations.put("or", Operation.OR);
		_operations.put("slt", Operation.LESS);
		_operations.put("sltu", Operation.LESS_U);

		_operations.put("beq", Operation.EQUAL);
		_operations.put("bne", Operation.NOT_EQUAL);
		_operations.put("blt", Operation.LESS);
		_operations.put("bge", Operation.GREATER_EQUAL);
		_operations.put("bgeu", Operation.GREATER_EQUAL_U);
		_operations.put("bltu", Operation.LESS_U);

		_operations.put("addi", Operation.ADD);
		_operations.put("slli", Operation.SHIFT_LEFT);
		_operations.put("srli", Operation.SHIFT_RIGHT_L);
		_operations.put("srai", Operation.SHIFT_RIGHT_A);
		_operations.put("slti", Operation.LESS);
		_operations.put("ori", Operation.OR);
		_operations.put("xori", Operation.X_OR);
		_operations.put("andi", Operation.AND);
		_operations.put("sltiu", Operation.LESS_U);
		
		_operations.put("mul", Operation.MULT);
		_operations.put("div", Operation.DIV);
		_operations.put("rem", Operation.REM);
		_operations.put("divu", Operation.DIV_U);
		_operations.put("remu", Operation.REM_U);
		

	}

}
