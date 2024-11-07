package textInterface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import RISC.RiscRegisters;
import RISC.RiscvParse;
import YAAL.YaalParse;
import backEndParsing.AbsToMachine;
import intermediateRepresentation.intermediateRepresentation;
import util.Pair;
import operation_syntax.OperationException;
import parse.ParseException;
import parse.ParseInterface;
import parse.ParseOutput;
import virtual_machine.ExecVM;
import virtual_machine.IOFunctions;
import virtual_machine.MemoryException;

public class TextInterface {

	private static Scanner _scanner = new Scanner(System.in);
	private static ExecVM _execVM;
	private static IOFunctions _fo = new TextInterface_IOF();

	static ParseOutput _parsedInput;
	static List<intermediateRepresentation> _instructions;

	public static void main(String args[]) throws IOException, ParseException, MemoryException {

		String RISCcommands = 
						" l) load instructions from file \n" +
						" i) enter single instruction \n" +
						" e) execute all instructions \n" + 
						" n) execute next instruction \n" + 
						" r) view altered registers \n" + 
						" m) view altered  main memory \n" +
						" p) view program counter \n" +
						" c) disassemble to machine code \n" +
						" a) disassemble to abstract instructions \n" +
						" s) view symbol table \n" + 
						" t) view return address stack \n" +
						" q) quit";

		String YAALcommands = 
						" l) load instructions from file \n" +
						" e) execute all instructions \n" + 
						" n) execute next instruction \n" + 
						" r) view altered registers \n" + 
						" m) view altered  main memory \n" +
						" p) view program counter \n" +
						" a) disassemble to abstract instructions \n" +
						" s) view symbol table \n" + 
						" t) view return address stack \n" + 
						" q) quit";

		//SET VIRTUAL MACHINE AND INSTRUCTION TYPE
		System.out.println("Load Risc-V or Yaal virtual machine? (r / y)");
		String instructionType = _scanner.nextLine();

		//Sets list of commands to print
		String commands;
		if (instructionType.equals("r")) {
			commands = RISCcommands;
		}else if (instructionType.equals("y")){
			commands = YAALcommands;
		}else {
			throw new RuntimeException("Invalid architecture");
		}

		System.out.println(commands);

		while (_scanner.hasNext()) {
			String command = _scanner.nextLine();
			command = command.trim();

			if (command.equals("l")) {
				System.out.println("Text file of instructions: ");
				String filename = _scanner.nextLine();
				String source = new String(Files.readAllBytes(Paths.get(filename)));
				initializeExecutionEnvironment(source, instructionType);

			}else if (command.equals("i")) {
				System.out.println("Enter instruction: ");
				String source = _scanner.nextLine();
				initializeExecutionEnvironment(source, instructionType);	

			}else if (command.equals("e")) {

				long startTime = System.currentTimeMillis();
				while (_execVM.running()) {
					try {
						_execVM.tick();
					} catch (OperationException | MemoryException e) {
						System.out.println(e.getMessage() + " on line " + _execVM.getPC());
					}
				}
				long endTime = System.currentTimeMillis();
				long runTime = endTime - startTime;
				System.out.println("Execution complete in " + runTime + "ms");



			}else if (command.equals("n")) {
				try {
					if (_execVM.running()) {
						_execVM.tick();
					}else {
						System.out.println("No instruction to execute");
					}
				}catch (MemoryException | OperationException e) {
					System.out.println(e.getMessage() + " on line " + _execVM.getPC());

				}

			}else if (command.equals("r")) {
				try {
					//System.out.println("Registers: " + _execVM.prinTemps() + "..." +  _execVM.printGlobals());
					System.out.println("Registers: " + showRegs(_execVM.getGlobalsMap()).toString());
				}catch (NullPointerException e) {
					System.out.println(e.getMessage() + " on line " + _execVM.getPC());
				}

			}else if (command.equals("m")) {
				try {
					System.out.println("Main memory: " + _execVM.printMainMem());
				}catch (NullPointerException e) {
					System.out.println(e.getMessage() + " on line " + _execVM.getPC());
				}

			}else if (command.equals("p")) {
				try {
					System.out.println("PC: " + _execVM.getPC());
				}catch (NullPointerException e) {
					System.out.println(e.getMessage() + " on line " + _execVM.getPC());
				}

			}else if (command.equals("c") && instructionType.equals("r")) {
				ArrayList<Integer> machineCode = AbsToMachine.parseAbstracts(_instructions, _parsedInput.getSymbolTable(), _parsedInput.getData());
				System.out.println("Machine Code: " + printMachineCode(machineCode));

			}else if (command.equals("a")) {
				System.out.println("Abstract Instructions:" + printAbstracts(_instructions));

			}else if (command.equals("q")) {
				System.out.println("Execution environment exited");
				break;

			}else if (command.equals("t")) {
				System.out.println(_execVM.showTempsStack().toString());

			} else if(command.equals("s")) {
				System.out.println(_parsedInput.getSymbolTable().toString());
				
			}else if(command.equals("k")) {
				System.out.println("Press any key...");
				int k = _scanner.nextLine().charAt(0);
				_execVM.logKeyEvent(k);
			} else {
				System.out.println("Invalid command: " + command + "\n" + commands);
				continue;
			}

			System.out.println("...");
		}

		_scanner.close(); 
	}

	private static void initializeExecutionEnvironment(String source, String instructionType)
			throws ParseException, MemoryException {

		ParseInterface parser = null;

		if (instructionType.equals("r")) {
			parser = new RiscvParse();
		}else if (instructionType.equals("y")) {
			parser = new YaalParse();
		}

		//PARSE INPUT
		try {
			_parsedInput = parser.parseSource(source);
		}catch (ParseException e) {
			System.out.println(e.getMessage() + " on line " + e.getLineNumber());
		}

		_instructions = new ArrayList<>();
		for (Pair<intermediateRepresentation, Integer> pair : _parsedInput.getInstructions().getList()) {
			_instructions.add(pair.first());
		}

		//INITIALIZE EXECUTION ENVIRONMENT
		_execVM = new ExecVM(_fo);
		_execVM.loadInstructions(_instructions);
		_execVM.loadSymbolTable(_parsedInput.getSymbolTable());
		
		if(instructionType.equals("r")) {
			 _execVM.loadGlobals(makeRiscRegs(), false);
			 _execVM.loadData(_parsedInput.getData());
			 _execVM.loadROs(RiscRegisters.READ_ONLY);
			 
		}
	}

	private static String printAbstracts(List<intermediateRepresentation> instructions) {
		String s = "";
		int j = 0;
		for (intermediateRepresentation i : instructions) {
			s += "\n" + (j++) + ": "+ i ;
		}
		return s;
	}

	private static String printMachineCode(List<Integer> instructions) {
		String s = "";
		int j = 0;
		for (int i : instructions) {
			s += "\n" + (j++) + ": " + String.format("%08x", i);
		}
		return s;
	}
	
	private static Map<String, Integer> showRegs(Map<String, Integer> registers){
		Map<String, Integer> nonZeroRegs = new TreeMap<>();
		for(String j : registers.keySet()) {
			int value = registers.get(j);
			if(value != 0) {
				nonZeroRegs.put(j,value);
			}
		}
		return nonZeroRegs;
	}

	private static Map<String, Integer> makeRiscRegs() {
		Map<String, Integer> regList = new TreeMap<>();
		for (String reg : RiscRegisters.REGISTERS) {
			int value = 0;
			if (reg.equals("sp")) {
				value = _execVM.getOS().getStackPointer();
			}
			regList.put(reg, value);
		}
		return regList;
	}
}
