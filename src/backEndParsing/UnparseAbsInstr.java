package backEndParsing;

import java.util.ArrayList;

import RISC.RiscRegisters;
import intermediateRepresentation.*;

public class UnparseAbsInstr {
	RiscRegisters REGISTERS = new RiscRegisters();

	public ArrayList<String> unparseMain(ArrayList<intermediateRepresentation> instructions){
		ArrayList<String> return_array = new ArrayList<String>();
		for(intermediateRepresentation i : instructions) {
			return_array.add(unparseInstr(i) + "\n");
		}
		return return_array;
	}
	
	private String unparseInstr(intermediateRepresentation i){
		String s = "";
		if(i instanceof Move) {
			Move m = (Move) i;
			Abs_Expression dest = m.getDestination();
			Abs_Expression source = m.getSource();
			
			String src = unparseExp(source);
			int x = src.indexOf(" ");
			s = src.substring(0, x) + " " + unparseExp(dest) + ", " + src.substring(x+1);
			
		}else if(i instanceof Call) {
			Call c = (Call) i;
			String symbol = c.getSymbol();
			s = "jal " + symbol;
			
		}else if(i instanceof Branch) {
			Branch b = (Branch) i;
			Abs_Expression dest = b.getDestination();
			Abs_Expression source = b.getRelOp();
			s = unparseExp(source) + ", " + unparseExp(dest);
			
		}else if(i instanceof Return) {
			s = "jr ra";
			
		}else if(i instanceof Jump) {
			Jump j = (Jump) i;
			s = "j " + unparseExp(j.getTarget());
		}else if(i instanceof Call) {
			s = "Call";
		}
		return s;
	}
	
	private String unparseExp(Abs_Expression e){
		String s = "";
		if(e instanceof BinOp) {
			BinOp b = (BinOp) e;
			s = b.getOp() + " " + unparseExp(b.getArg1()) + ", " + unparseExp(b.getArg2());
		}else if(e instanceof Literal) {
			Literal l = (Literal) e;
			s = "" + l.get();
		}else if(e instanceof Temporary) {
			Temporary r = (Temporary) e;
			s = r.get();
		}else if(e instanceof MemoryAddress) {
			MemoryAddress m = (MemoryAddress) e;
			s = unparseExp(m.getAddress());
		}else if(e instanceof Symbol) {
			Symbol y = (Symbol) e;
			s = y.get();
		}
		return s;
	}
}
