package parse;

import java.util.ArrayList;
import java.util.List;

import intermediateRepresentation.intermediateRepresentation;
import util.Pair;

public class Instructions {
	
	List<Pair<intermediateRepresentation, Integer>> _instructions;

	public Instructions() {
		_instructions = new ArrayList<Pair<intermediateRepresentation, Integer>>();
	} 

	public boolean add(Pair<intermediateRepresentation, Integer> p) {
			_instructions.add(p);
			return true;
	}
	
	public List<Pair<intermediateRepresentation, Integer>> getList() {
		return _instructions;
	}

}
