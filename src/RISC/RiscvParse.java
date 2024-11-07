package RISC;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import intermediateRepresentation.*;
import RISC.RiscOperations;
import operation_syntax.Operation;
import parse.Data;
import parse.Instructions;
import parse.ParseException;
import parse.ParseInterface;
import parse.ParseOutput;
import parse.ParseRegex;
import parse.PreParseOutput;
import parse.Preprocess;
import parse.SymbolTable;
import util.Pair;

public class RiscvParse implements ParseInterface {


	private static RiscOperations ops = new RiscOperations();

	private String pseudo;

	public Data _data;
	public SymbolTable symbolTable;
	public List<String> invalidLabels;

	public ParseOutput parseSource(String source)
			throws ParseException {

		Instructions riscCodes = new Instructions();
		PreParseOutput p = Preprocess.preprocess(source, "risc");		
		symbolTable = p.getSymbolTable();
		_data = p.getData();
		invalidLabels = new ArrayList<String>();
		List<Pair<String, Integer>> codeLines = p.getCodeLines();

		for (Pair<String, Integer> codePair : codeLines) {
			intermediateRepresentation ai;
			if (codePair.first().isEmpty() || codePair.first().matches("\\s*")) {
				continue;
			} else {
				ai = parseTextLine(codePair.first(), codePair.second());
			}
			riscCodes.add(new Pair<intermediateRepresentation, Integer>(ai, codePair.second()));
		}
		return new ParseOutput(symbolTable, riscCodes, _data);
	}


	public intermediateRepresentation parseTextLine(String code, int sourceLine) throws ParseException {

		pseudo = "";
		Pattern p = Pattern.compile("^([a-z]+)\\b(.*)"); // split into command and rest of line
		Matcher m = p.matcher(code);
		if (m.matches()) {
			String command = m.group(1);
			String rest = m.group(2);
			command = command.trim();
			rest = rest.trim();
			//Pseudo instructions: translate first, then parse
			if (command.equals("j")) {
				pseudo = "j";
				command = "jal";
				rest = "zero, " + rest;
			} else if (code.equals("jr ra")) {
				pseudo = "jr";
				command = "jalr";
				rest = "zero, ra, 0";
			} else if (command.equals("nop")) {
				pseudo = "nop";
				command = "addi";
				rest = "zero, zero, 0";
			} else if (command.equals("li")) {
				pseudo = "li";
				String[] rests = rest.split(","); //["rd,", "imm"]
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction li " + rest, sourceLine);
				}
				command = "addi";
				rest = rests[0] + ", zero, " + rests[1];

				// la is its own unique case...can't be directly translated to another RISC operation
			} else if (command.equals("la")) {
				pseudo = "la";
				String[] rests = rest.split(",");
				String rd = rests[0].trim();
				String symbol = rests[1].trim();
				return new Move(parseElement(rd, sourceLine), new DataLabel(symbol));

			} else if (command.equals("mv")) {
				pseudo = "mv";
				command = "addi";
				rest = rest + ", 0";
			} else if (command.equals("not")) {
				pseudo = "not";
				command = "xori";
				rest = rest + ", -1";
			} else if (command.equals("neg")) {
				pseudo = "neg";
				command = "sub";
				String[] rests = rest.split(","); //["rd", "rs"]
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction neg " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("seqz")) {
				pseudo = "seqz";
				command = "sltiu";
				rest = rest + ", 1";
			} else if (command.equals("snez")) {
				pseudo = "snez";
				command = "sltu";
				String[] rests = rest.split(",");
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction snez " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("sltz")) {
				pseudo = "sltz";
				command = "slt";
				rest = rest + ", zero";
			} else if (command.equals("sgtz")) {
				pseudo = "sgtz";
				command = "slt";
				String[] rests = rest.split(",");
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction sgtz " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("beqz")) {
				pseudo = "beqz";
				command = "beq";
				String[] rests = rest.split(",");
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction beqz " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("bnez")) {
				pseudo = "bnez";
				command = "bne";
				String[] rests = rest.split(",");
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction bnez " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("blez")) {
				pseudo = "blez";
				command = "bge";
				rest = "zero, " + rest;
			} else if (command.equals("bgez")) {
				pseudo = "bgez";
				command = "bge";
				String[] rests = rest.split(",");
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction bgez " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("bltz")) {
				pseudo = "bltz";
				command = "blt";
				String[] rests = rest.split(",");
				if (rests.length != 2) {
					throw new ParseException("Invalid format for instruction bltz " + rest, sourceLine);
				}
				rest = rests[0] + ", zero, " + rests[1];
			} else if (command.equals("bgtz")) {
				pseudo = "bgtz";
				command = "blt";
				rest = "zero, " + rest;
			} else if (command.equals("bgt")) {
				pseudo = "bgt";
				command = "blt";
				String rests[] = rest.split(",");
				if (rests.length != 3) {
					throw new ParseException("Invalid format for instruction bgt " + rest, sourceLine);
				}
				rest = rests[1] + ", " + rests[0] + ", " + rests[2];
			} else if (command.equals("ble")) {
				pseudo = "ble";
				command = "bge";
				String rests[] = rest.split(",");
				if (rests.length != 3) {
					throw new ParseException("Invalid format for instruction ble " + rest, sourceLine);
				}
				rest = rests[1] + ", " + rests[0] + ", " + rests[2];
			} else if (command.equals("ret")) {
				command = "jalr";
				rest = "zero, ra, 0";
			}

			//you can never have too many trims
			command = command.trim();
			rest = rest.trim();

			//parsing regular instructions
			if (command.equals("add") || command.equals("sub") || 
					command.equals("sll") || command.equals("srl") || 
					command.equals("sra") || command.equals("xor") ||
					command.equals("and") || command.equals("or") || 
					command.equals("slt") || command.equals("sltu") ||
					command.equals("mul") || command.equals("div") || 
					command.equals("rem")) {
				return parseRRR(command, rest, sourceLine);	

			} else if (command.equals("addi") || command.equals("slli") ||
					command.equals("srli") || command.equals("srai") ||
					command.equals("slti") || command.equals("ori") ||
					command.equals("andi") || command.equals("xori") ||
					command.equals("sltiu")) {
				return parseRRL(command, rest, sourceLine);

			} else if (command.equals("beq") || command.equals("bne") ||
					command.equals("blt") || command.equals("bge") ||
					command.equals("bgeu") || command.equals("bltu")) {
				return parseRRS(command, rest, sourceLine);

			} else if (command.equals("jal") || command.equals("jalr")) {
				return parseJump(command, rest, sourceLine);

			} else if (command.equals("lw") || command.equals("lb") || 
					command.equals("lh")) {
				return parseLW(command, rest, sourceLine);

			} else if (command.equals("sw") || command.equals("sb") || 
					command.equals("sh")) {
				return parseSW(command, rest, sourceLine);

			} else if(command.equals("ecall")) {
				return parseSysCall(command, rest, sourceLine);
			}else {
				throw new ParseException("invalid command: " + command, sourceLine);
			}
		} else {
			throw new ParseException("invalid characters at start of code line " + code + " " + code.length(), sourceLine);
		}
	}

	private intermediateRepresentation parseRRR(String command, String rest, int sourceLine) 
			throws ParseException {

		Matcher rrrMatcher = ParseRegex.REG_REG_REG.matcher(rest);
		if (rrrMatcher.matches()) {
			String rd = rrrMatcher.group(1);
			String rs1 = rrrMatcher.group(2);
			String rs2 = rrrMatcher.group(3);
			
			if (rd.matches("-?\\d+") || rs1.matches("-?\\d+") || rs2.matches("-?\\d+")) {
				throw new ParseException("invalid literal in command " + command, sourceLine);
			}

			Abs_Expression source = new BinOp(parseOp(command),
					parseElement(rs1, sourceLine), parseElement(rs2, sourceLine));
			return new Move(parseElement(rd, sourceLine), source);
		} else {
			if (!pseudo.isEmpty()) {
				command = pseudo;
			}
			throw new ParseException("bad syntax for command: " + command, sourceLine);
		}
	}

	private intermediateRepresentation parseRRL(String command, String rest, int sourceLine) 
			throws ParseException {

		Matcher rrlMatcher = ParseRegex.REG_REG_LIT.matcher(rest);
		if (rrlMatcher.matches()) {
			String rd = rrlMatcher.group(1);
			String rs1 = rrlMatcher.group(2);
			String imm = rrlMatcher.group(3);
			
			if (rd.matches("-?\\d+") || rs1.matches("-?\\d+") || !(imm.matches("-?\\d+"))) {
				throw new ParseException("invalid value in command " + command, sourceLine);
			}

			Abs_Expression source = new BinOp(parseOp(command),
					parseElement(rs1, sourceLine), parseElement(imm, sourceLine));
			return new Move(parseElement(rd, sourceLine), source);
		} else {
			if (!pseudo.isEmpty()) {
				command = pseudo;
			}
			throw new ParseException("bad syntax for command: " + command, sourceLine);
		}
	}

	private intermediateRepresentation parseRRS(String command, String rest, int sourceLine) 
			throws ParseException {

		Matcher rrsMatcher = ParseRegex.REG_REG_REG.matcher(rest);
		if (rrsMatcher.matches()) {
			String rs1 = rrsMatcher.group(1);
			String rs2 = rrsMatcher.group(2);
			String sym = rrsMatcher.group(3);
			
			if (rs1.matches("-?\\d+") || rs2.matches("-?\\d+") || sym.matches("-?\\d+")) {
				throw new ParseException("invalid literal in command " + command, sourceLine);
			}

			Abs_Expression source = new BinOp(parseOp(command),
					parseElement(rs1, sourceLine), parseElement(rs2, sourceLine));
			return new Branch(source, parseElement(sym, sourceLine));
		} else {
			if (!pseudo.isEmpty()) {
				command = pseudo;
			}
			throw new ParseException("bad syntax for command: " + command, sourceLine);
		}
	}

	private intermediateRepresentation parseJump(String command, String rest, int sourceLine) 
			throws ParseException {
		Matcher rsMatcher = ParseRegex.REG_SYMBOL.matcher(rest);
		Matcher rrlMatcher = ParseRegex.REG_REG_LIT.matcher(rest);
		Matcher pseudoMatcher = ParseRegex.RISC_SYMBOL.matcher(rest);
		if (rsMatcher.matches()) {
			String reg = rsMatcher.group(1);
			String symbol = rsMatcher.group(2);
			return new Jump(parseElement(reg, sourceLine), parseElement(symbol, sourceLine));

		} else if (rrlMatcher.matches()) {
			String rs1 = rrlMatcher.group(1);
			String rs2 = rrlMatcher.group(2);
			int offset = Integer.parseInt(rrlMatcher.group(3));

			if (rs1.equals("zero") && rs2.equals("ra") && offset == 0) {
				Abs_Expression dest = parseElement(rs2, sourceLine);
				return new Jump(parseElement(rs1, sourceLine), dest);
			} else {
				throw new ParseException("command not yet implemented: " + command, sourceLine);
			}

		} else if (pseudoMatcher.matches() && command.equals("jal")) {
			String symbol = pseudoMatcher.group(1);
			return new Jump(parseElement("ra", sourceLine), parseElement(symbol, sourceLine));

		} else {
			throw new ParseException("bad syntax for command: " + command, sourceLine);
		}
	}

	private intermediateRepresentation parseLW(String command, String rest, int sourceLine) 
			throws ParseException {

		Matcher rlrMatcher = ParseRegex.REG_LIT_REG.matcher(rest);
		if (rlrMatcher.matches()) {
			String rd = rlrMatcher.group(1);
			String offset = rlrMatcher.group(2);
			String rs1 = rlrMatcher.group(3);
			
			if (rs1.matches("-?\\d+")) {
				throw new ParseException("Invalid literal in sw " + rs1, sourceLine);
			}

			Abs_Expression source = new MemoryAddress(
					new BinOp(Operation.ADD, parseElement(offset, sourceLine), parseElement(rs1, sourceLine)), 
					addressBytes(command, sourceLine)
					);
			return new Move(parseElement(rd, sourceLine), source);
		} else {
			throw new ParseException("bad syntax for command: " + command, sourceLine);
		}
	}

	private intermediateRepresentation parseSW(String command, String rest, int sourceLine) 
			throws ParseException {

		Matcher rlrMatcher = ParseRegex.REG_LIT_REG.matcher(rest);
		if (rlrMatcher.matches()) {
			String rd = rlrMatcher.group(1);
			String offset = rlrMatcher.group(2);
			String rs1 = rlrMatcher.group(3);
			
			if (rs1.matches("-?\\d+")) {
				throw new ParseException("Invalid literal in sw " + rs1, sourceLine);
			}

			Abs_Expression source = new MemoryAddress(
					new BinOp(Operation.ADD, parseElement(offset, sourceLine), parseElement(rs1, sourceLine)), 
					addressBytes(command, sourceLine));
			return new Move(source, parseElement(rd, sourceLine));
		} else {
			if (!pseudo.isEmpty()) {
				command = pseudo;
			}
			throw new ParseException("bad syntax for command: " + command, sourceLine);
		}
	}


	private intermediateRepresentation parseSysCall(String command, String rest, int sourceLine) 
			throws ParseException {

		if (rest.isEmpty() || rest.matches("\\s*")) {
			return new SysCall();
		} else {
			throw new ParseException("illegal characters after syscall " + rest, sourceLine);
		}
	}



	private Abs_Expression parseElement(String r, int sourceLine) throws ParseException {
		r = r.trim();
		Matcher symMatcher = ParseRegex.RISC_SYMBOL.matcher(r);
		Matcher hexMatcher = ParseRegex.HEX.matcher(r);
		if (_data.containsLabel(r)) {
			return new DataLabel(r);
		} else if (RiscRegisters.REGISTERS.contains(r)) {
			return new Temporary(r, true);
		} else if (hexMatcher.matches()) {
			r = r.substring(2); //trim 0x
			return new Literal(Integer.parseInt(r, 16));
		} else if (r.matches("-?\\d+")) {
			return new Literal(Integer.parseInt(r));
		} else if (symMatcher.matches()) {
			if (symbolTable.containsSymbol(r) || _data.containsLabel(r)) {
				return new Symbol(r);
			} else {
				invalidLabels.add(r);
				throw new ParseException("label or register does not exist " + r, sourceLine);
			}
		} else {
			throw new ParseException("invalid argument type " + r, sourceLine);
		} 
	}

	private static int addressBytes(String f, int sourceLine) 
			throws ParseException {

		if (f.equals("lw") || f.equals("sw")) {
			return 4;
		} else if (f.equals("lh") || f.equals("sh")) {
			return 2;
		} else if (f.equals("lb") || f.equals("sb")) {
			return 1;
		} else {
			throw new ParseException("Invalid load store command: " + f, sourceLine);
		}
	}

	private static Operation parseOp(String op) {
		return ops.opOf(op);
	}
}