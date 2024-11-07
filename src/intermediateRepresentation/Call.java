package intermediateRepresentation;

import java.util.List;

public class Call extends intermediateRepresentation {

	private String _symbol;
	private List<Abs_Expression> _args;
	private Temporary _destReg;
	
	public Call (Temporary destReg, String symbol, List<Abs_Expression> args) {
		_symbol = symbol;
		_args = args;
		_destReg = destReg;
	}

	public String getSymbol() {
		return _symbol;
	}
	
	public List<Abs_Expression> getArgs() {
		return _args;
	}
	
	public Temporary getDestReg() {
		return _destReg;
	}
	
	@Override 
	public String toString() {
		if(_args != null) {
			return "Call (" + _symbol.toString() + ": " + _args.toString() + ")";
		}else {
			return "Call (" + _symbol.toString() + ")";
		}
	}
}
