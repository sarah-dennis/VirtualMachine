package parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.Pair;

public class SymbolTable {

	Map<String, Pair<Integer, Integer>> _symbolTable;

	public SymbolTable() {
		_symbolTable = new HashMap<String, Pair<Integer, Integer>>();
	} 

	public boolean add(String s, Pair<Integer, Integer> p) {
		if (_symbolTable.containsKey(s)) {
			return false;
		}
		else {
			_symbolTable.put(s, p);
			return true;
		}
	}
	
	public boolean containsSymbol(String s) {
		return _symbolTable.containsKey(s);
	}


	public Integer getSourceLine(String s) {
		if (_symbolTable.containsKey(s)) {
			return _symbolTable.get(s).first();
		} else {
			return null;
		}
	}

	public Integer getCodeLine(String s) {
		if (_symbolTable.containsKey(s)) {
			return _symbolTable.get(s).second();
		} else {
			return null;
		}
	}
	
	public Set<String> getLabels() {
		return _symbolTable.keySet();
	}
	
	@Override 
	public String toString() {
		String result = "{";
		int counter = 0;
		for (String l : getLabels()) {
			String num = getSourceLine(l).toString();
			result += l + "=" + num;
			if (!(counter == getLabels().size()-1)) {
				result += ", ";
			}
			counter++;
		}
		result += "}";
		return result;
	}

}
