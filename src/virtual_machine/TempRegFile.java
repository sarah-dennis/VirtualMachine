package virtual_machine;

import java.util.Map;

import intermediateRepresentation.Temporary;

public class TempRegFile {
	
	private Temporary _destination;
	private int _ra;
	private Map<String, Integer> _temps;
	
	public TempRegFile(Temporary rv, int ra, Map<String, Integer> temps) {
		_destination = rv;
		_ra = ra;
		_temps = temps;
	}
	
	public Map<String, Integer> getTemps(){
		return _temps;
	}
	
	public Temporary getDestReg() {
		return _destination;
	}
	
	public int returnAddress() {
		return _ra;
	}
	
	@Override
	public String toString() {
		return _temps.toString();
	}
}
