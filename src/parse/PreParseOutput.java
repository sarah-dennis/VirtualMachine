package parse;

import java.util.List;

import util.Pair;

public class PreParseOutput {

	private SymbolTable _st;
	private List<Pair<String, Integer>> _code;
	private Data _data;

	//RISC constructor
	public PreParseOutput(SymbolTable s, List<Pair<String, Integer>> c, Data d) {
		_st = s;
		_code = c;
		_data = d;
	}

	//YAAL constructor
	public PreParseOutput(SymbolTable s, List<Pair<String, Integer>> c) {
		_st = s;
		_code = c;
		_data = null;
	}

	public SymbolTable getSymbolTable() {
		return _st;
	}

	public List<Pair<String, Integer>> getCodeLines() {
		return _code;
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
