package YAAL;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import backEndParsing.MachineNumber;
import intermediateRepresentation.Abs_Expression;
import intermediateRepresentation.intermediateRepresentation;
import intermediateRepresentation.BinOp;
import intermediateRepresentation.Branch;
import intermediateRepresentation.Call;
import intermediateRepresentation.Jump;
import intermediateRepresentation.Literal;
import intermediateRepresentation.MemoryAddress;
import intermediateRepresentation.Move;
import intermediateRepresentation.Temporary;
import intermediateRepresentation.Return;
import intermediateRepresentation.Symbol;
import operation_syntax.Operation;
import parse.HiToLowTranslate;
import parse.Instructions;
import parse.ParseException;
import parse.ParseInterface;
import parse.ParseOutput;
import parse.ParseRegex;
import parse.PreParseOutput;
import parse.Preprocess;
import parse.SymbolTable;
import util.Pair;

public class YaalParse implements ParseInterface {

	public String yaalSyntax;

	private static YaalOperations ops = new YaalOperations();

	private SymbolTable _st;
	private HiToLowTranslate syntax;
	
	public ParseOutput parseSource(String source)
			throws ParseException {
		
		Instructions yaalCodes = new Instructions();

		syntax = new HiToLowTranslate();
		yaalSyntax = syntax.hiToLowSyntax(source);
		System.out.println(yaalSyntax);        //Uncomment for intermediate yaal debugging

		PreParseOutput p = 
				Preprocess.preprocess(yaalSyntax, "yaal");

		_st = p.getSymbolTable();
		List<Pair<String, Integer>> codeLines = p.getCodeLines();

		for (Pair<String, Integer> codePair : codeLines) {
			intermediateRepresentation ir = parseTextLine(codePair.first(), codePair.second());
			if(ir == null) {
				continue;
			}
			yaalCodes.add(new Pair<intermediateRepresentation, Integer>(ir, codePair.second()));
		}
		return new ParseOutput(_st, yaalCodes);
	}

	public intermediateRepresentation parseTextLine(String code, int sourceLine) throws ParseException {
		code = code.trim();
		Matcher ifMatcher = ParseRegex.IF.matcher(code);
		Matcher gotoMatcher = ParseRegex.GOTO.matcher(code);
		Matcher storeMatcher = ParseRegex.STORE.matcher(code);
		Matcher varMatcher = ParseRegex.ASSIGN.matcher(code);
		Matcher returnMatcher = ParseRegex.RETURN.matcher(code);
		Matcher callMatcher = ParseRegex.CALL.matcher(code);
		Matcher scheduleMatcher = ParseRegex.SCHEDULE.matcher(code);
		Matcher handleMatcher = ParseRegex.HANDLE.matcher(code);
		if (ifMatcher.matches()) {
			String rs1 = ifMatcher.group(1);
			String op = ifMatcher.group(2);
			String rs2 = ifMatcher.group(3);
			String sym = ifMatcher.group(4);
			return parseIf(rs1, op, rs2, sym, sourceLine);

		} else if (gotoMatcher.matches()) {
			String sym = gotoMatcher.group(1);
			return parseGoto(sym, sourceLine);

		} else if (storeMatcher.matches()) {			
			String rd = storeMatcher.group(1);
			String rest = storeMatcher.group(2);
			return parseStore(rd, rest, sourceLine);

		} else if (returnMatcher.matches()) {
			String sym = returnMatcher.group(1);
			return parseReturn(sym, sourceLine);

		} else if (varMatcher.matches()) {
			String dest = varMatcher.group(1);
			String op = varMatcher.group(2);
			return parseVariable(dest, op, sourceLine);

		} else if (callMatcher.matches()) {
			String funct = callMatcher.group(1);
			String args = callMatcher.group(2);
			return parseCall(null, funct, args, sourceLine);

		} else if (scheduleMatcher.matches()) {
			String funct = scheduleMatcher.group(1);
			String dest = scheduleMatcher.group(2);
			return parseSchedule(funct, dest, sourceLine);

		} else if (handleMatcher.matches()) {
			String handleType = handleMatcher.group(1);
			String funct = handleMatcher.group(2);
			return parseHandle(handleType, funct, sourceLine);

		} else {
			throw new ParseException("invalid instruction format ", sourceLine);
		}
	}

	private intermediateRepresentation parseIf(String rs1, String op, String rs2, String sym, int sourceLine) 
			throws ParseException {

		if (!syntax.getFunctMap().containsKey(sym) && !_st.containsSymbol(sym)) {
			throw new ParseException("Function does not exist " + sym, sourceLine);
		}

		Abs_Expression source = new BinOp(parseOp(op),
				parseElement(rs1, sourceLine), parseElement(rs2, sourceLine));
		return new Branch(source, new Symbol(sym));
	}

	private intermediateRepresentation parseGoto(String symbol, int sourceLine) 
			throws ParseException {

		if (!syntax.getFunctMap().containsKey(symbol) && !_st.containsSymbol(symbol)) {
			throw new ParseException("Function does not exist " + symbol, sourceLine);
		}
		return new Jump(parseElement("zero", sourceLine), new Symbol(symbol));
	}

	private intermediateRepresentation parseCall(String dest, String sym, String end, int sourceLine) 
			throws ParseException {

		if (!syntax.getFunctMap().containsKey(sym) && !_st.containsSymbol(sym)) {
			throw new ParseException("Function does not exist " + sym, sourceLine);
		}
		
		List<Abs_Expression> absArgs = new ArrayList<>();
		if (!end.isEmpty()) {
			String[] args = end.split(",");
			for (String arg : args) {
				absArgs.add(parseElement(arg, sourceLine));
			}
		}

		if (sym.equals("_stop")	|| sym.equals("_read") ||
			sym.equals("_read_string") || sym.equals("_read_char") || 
			sym.equals("_clear_graphics") || sym.equals("_clear_memory")) {
			if (absArgs.size() != 0) {
				throw new ParseException("not enough arguments for " + sym, sourceLine);
			}
		} else if (sym.equals("_print") || sym.equals("_print_string") ||
				sym.equals("_allocate") || sym.equals("_random") || sym.equals("_print_char")) {
			if (absArgs.size() > 1) {
				throw new ParseException("too many arguments for " + sym, sourceLine);
			}
		}


		if (dest == null) {
			return new Call(null, sym, absArgs);
		} 

		Abs_Expression destReg = parseElement(dest, sourceLine);

		if (destReg instanceof Temporary) {
			return new Call((Temporary) destReg, sym, absArgs);
		} else {
			throw new ParseException("invalid destination for call " + dest, sourceLine);
		}
	}

	private intermediateRepresentation parseStore(String val, String rest, int sourceLine)
			throws ParseException {

		String[] endOfStore = rest.split(";");
		String dest = endOfStore[0];
		String offset;
		if (endOfStore.length > 1) {
			offset = endOfStore[1];
		} else {
			offset = "0";
		}
		Abs_Expression source = new MemoryAddress(
				new BinOp(Operation.MULT, 
						new BinOp(Operation.ADD, parseElement(dest, sourceLine), parseElement(offset, sourceLine)), 
						new Literal(4)), MachineNumber.WORD_SIZE);
		return new Move(source, parseElement(val, sourceLine));
	}

	private intermediateRepresentation parseVariable(String dest, String operation, int sourceLine) 
			throws ParseException {

		Matcher opMatcher = ParseRegex.OPERATION.matcher(operation);
		Matcher loadMatcher = ParseRegex.LOAD.matcher(operation);
		Matcher callMatcher = ParseRegex.CALL.matcher(operation);

		Abs_Expression destination = parseElement(dest, sourceLine);
		if (!(destination instanceof Temporary)) {
			throw new ParseException("invalid destination " + dest, sourceLine);
		} else {
			if (opMatcher.matches()) {
				String rs1 = opMatcher.group(1);
				String op = opMatcher.group(2);
				String rs2 = opMatcher.group(3);
				Abs_Expression binOp = new BinOp(parseOp(op), 
						parseElement(rs1, sourceLine), 
						parseElement(rs2, sourceLine)
						);
				return new Move(destination, binOp);

			} else if (loadMatcher.matches()) {
				String left = loadMatcher.group(1);
				String[] endOfStore = left.split(";");
				String rs1 = endOfStore[0];
				String offset;
				if (endOfStore.length > 1) {
					offset = endOfStore[1];
				} else {
					offset = "0";
				}
				Abs_Expression source = new MemoryAddress(
						new BinOp(Operation.MULT, 
								(new BinOp(Operation.ADD, 
										parseElement(rs1, sourceLine), 
										parseElement(offset, sourceLine))
										), 
								new Literal(4)
								),
						MachineNumber.WORD_SIZE);
				return new Move(destination, source);

			} else if (callMatcher.matches()) {
				String funct = callMatcher.group(1);
				String args = callMatcher.group(2);
				return parseCall(dest, funct, args, sourceLine);

			} else {
				return new Move(destination, parseElement(operation, sourceLine));
			}
		}
	}

	private intermediateRepresentation parseReturn(String rest, int sourceLine) 
			throws ParseException {
		if (rest.isEmpty() || rest.matches("\\s*")) {
			return new Return();
		} else {
			return new Return(parseElement(rest, sourceLine));
		}
	}

	private intermediateRepresentation parseSchedule(String funct, String dest, int sourceLine) 
			throws ParseException {
		funct = funct.trim();
		if (!syntax.getFunctMap().containsKey(funct) && !_st.containsSymbol(funct)) {
			throw new ParseException("Function does not exist " + funct, sourceLine);
		}
		List<Abs_Expression> args = new ArrayList<Abs_Expression>();
		args.add(new Symbol(funct));
		args.add(parseElement(dest, sourceLine));
		return new Call(null, "_schedule", args);
	}

	private intermediateRepresentation parseHandle(String handleType, String funct, int sourceLine) 
			throws ParseException {
		if (!syntax.getFunctMap().containsKey(funct) && !_st.containsSymbol(funct)) {
			throw new ParseException("Function does not exist " + funct, sourceLine);
		}
		List<Abs_Expression> args = new ArrayList<Abs_Expression>();
		args.add(new Symbol(funct));
		if (handleType.equals("key")) {
			return new Call(null, "_set_key_handler", args);	
		} else if (handleType.equals("click")) {
			return new Call(null, "_set_click_handler", args);
		} else {
			throw new ParseException("invalid handle type " + handleType, sourceLine);
		}
	}

	private Abs_Expression parseElement(String r, int sourceLine) throws ParseException {

		r = r.trim();
		Matcher charMatcher = ParseRegex.CHAR.matcher(r);
		Matcher stringMatcher = ParseRegex.STRING.matcher(r);
		Matcher varMatcher = ParseRegex.VARIABLE.matcher(r);
		if (varMatcher.matches()) {
			if (r.charAt(0) == '_') {
				return new Temporary(r, true);
			//if no underscore, then no dollar sign was present in HiToLow
			} else if (!r.contains("_") && !(r.charAt(0) == '@') && !r.equals("zero")) {
				throw new ParseException("Variable missing dollar sign " + r, sourceLine);
			} else {
				return new Temporary(r);
			}
		} else if (r.matches("-?\\d+")) {
			return new Literal(Integer.parseInt(r));
		} else if (charMatcher.matches()) {
			String s = charMatcher.group(1);
			int code = 0;
			for (char c : s.toCharArray()) {
				code += (int) c;
			}
			return new Literal(code);
		} else if (stringMatcher.matches()) {
			String s = stringMatcher.group(1);
			s += "\0";
			return new Symbol(s);
		} else {
			throw new ParseException("invalid argument type " + r, sourceLine);
		}
	}

	private static Operation parseOp(String op) {
		return ops.opOf(op);
	}

	public String getYaalSyntax() {
		return yaalSyntax;
	}
}