package parse;

public class ParseOutput {
	
	private SymbolTable _st;
	private Instructions _instructs;
	private Data _data;
	
	//RISC-V constructor
	public ParseOutput(SymbolTable st, Instructions n, Data d) {
		_st = st;
		_instructs = n;
		_data = d;
	}
	
	//YAAL constructor
	public ParseOutput(SymbolTable st, Instructions n) {
		_st = st;
		_instructs = n;
		_data = null;
	}
	
	public SymbolTable getSymbolTable() {
		return _st;
	}
	
	public Instructions getInstructions() {
		return _instructs;
	}
	
	public Data getData() {
		if (_data == null) {
			System.err.println("Data not initialized in this run environment");
			return null;
		} else {
			return _data;
		}
	}

}
