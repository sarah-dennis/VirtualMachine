package virtual_machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import RISC.RiscRegisters;
import dataTypes.*;
import intermediateRepresentation.*;
import operation_syntax.*;
import parse.Data;
import parse.SymbolTable;
import util.Pair;

public class ExecVM {

	//LOADED:
	private List<intermediateRepresentation> _instructions;
	private Map<String, Integer> _dataTable;
	private SymbolTable _st;
	private Set<String> _readOnlyVars;
	//INITIALIZED:
	private OperatingSystem _os;
	private IOFunctions _iof;
	private static Queue<Task> _eventQ;
	private Stack<TempRegFile> _registerStack;
	private Map<String, Integer> _tempRegFile;
	private Map<String, Integer> _globalRegFile;
	private MemorySystem _mainMem;
	private boolean _run;
	private int _pc;
	//STATE INFORMATION:
	private Integer _keyHandler;
	private Integer _clickHandler;
	private Integer _memoryState;
	private boolean _pcState;
	private String _regState;
	private String _callState;
	private boolean _returnState;
	private String _eventState;

	//SECTION: CONSTRUCTOR
	public ExecVM (IOFunctions iof) {
		_iof = iof;
		_registerStack = new Stack<>();
		_eventQ = new PriorityQueue<>(new TaskComparator());
		_keyHandler = null;
		_clickHandler = null;
		_os = new OperatingSystem();
		_tempRegFile = new HashMap<>();
		_globalRegFile = new HashMap<>();
		_mainMem = new MemorySystem();
		_dataTable = new HashMap<>();
		_readOnlyVars = new HashSet<>();
		stateReset();
		_run = true;
		_pc = 0;
	}
	public void resetALL() {
		stateReset();
		resetMainMem();
		clearGlobals();
		clearTemps();
		emptyQ();
		resetPC();
		_readOnlyVars = null;
		_keyHandler = null;
		_clickHandler = null;
		_registerStack.clear();
		_dataTable.clear();
		_run = true;
		_dataTable.clear();
	}

	//SECTION: CALL TO RUN
	public void tick() throws OperationException, MemoryException {
		stateReset();
		if(hasNextInstr()) {
			executeNextInstr();

		}else if(hasNextEvent()){
			Task t = _eventQ.peek();

			if(t.getTimeStamp() <= System.nanoTime()) {
				t = _eventQ.remove();
				_eventState = t.getDescr();
				changePC(t.getDestPC());
				putVariable(new Temporary("@0"), t.getArg());
			}

		}else if(_keyHandler != null || _clickHandler != null) {
			//wait for event
		}else {
			stop();
		}
	}

	//SECTION: INSTRUCTION UNWRAPPING
	public void executeNextInstr() throws OperationException, MemoryException {

		intermediateRepresentation i = getNextInstr();

		if (i instanceof Move) {

			Move m = (Move) i;
			Abs_Expression dest = m.getDestination();
			Abs_Expression source = m.getSource();

			//string constants
			if(source instanceof Symbol) {
				String s = ((Symbol) source).get();
				int address =  _os.requestMemory(s.length() + 1);
				source = new Literal(address);

				for(byte b: s.getBytes()) {
					putValueInMainMem(address, 1, b);
					address += 1;
				}
				putValueInMainMem(address, 1, '\0');
			}

			//store instruction
			if (dest instanceof MemoryAddress) {
				MemoryAddress a = (MemoryAddress) dest;
				int address = evaluateExp(a.getAddress());
				int size = a.getSize();
				int value = evaluateExp(source);
				putValueInMainMem(address, size, value);

				//arithmetic and load instructions
			}else {
				int value = evaluateExp(source);
				putVariable((Temporary) dest, value);
			}

		}else if (i instanceof Branch) {
			Branch b = (Branch) i;
			int dest = evaluateExp(b.getDestination());
			int result = evaluateExp(b.getRelOp());
			//only branch if comparison returns true
			if (result == 1) { 
				changePC(dest);
			}

		}else if (i instanceof Call) {
			String label = ((Call)i).getSymbol();
			_callState = label;
			List<Abs_Expression> args = ((Call)i).getArgs();
			Temporary destination = ((Call)i).getDestReg();

			if(_st.containsSymbol(label)){

				//save current registers to stack
				TempRegFile _currentTemps = new TempRegFile(destination, getPC(), getTempsMap());
				_registerStack.push(_currentTemps);

				//find argument values according to pre-call reg file
				List<Integer> evaluatedArgs = new ArrayList<>();
				for(Abs_Expression a : args) {
					evaluatedArgs.add(evaluateExp(a));
				}

				//put argument registers into new register file
				clearTemps();
				for(int n = 0; n < evaluatedArgs.size(); n++) {
					putVariable(new Temporary("@" + n), evaluatedArgs.get(n));
				}

				//change pc
				int newPC = _st.getCodeLine(((Call) i).getSymbol());
				changePC(newPC);

			}else if(_os.isSystemOp(label)) {

				Integer result =  evaluateCommand(label, args);
				if(result == null) {
					_pc -= 1;
				}else if(destination != null) {
					putVariable(destination, result);
				}
			}else {
				throw new OperationException(label);
			}

		}else if (i instanceof Return) {
			if(_registerStack.isEmpty()) {
				endPC();

			}else {
				int rv = 0;
				if(((Return)i).getValue() != null) {
					rv = evaluateExp(((Return)i).getValue());
				}		

				TempRegFile t = _registerStack.pop();
				setTemps(t.getTemps());
				changePC(t.returnAddress());

				Temporary destination = t.getDestReg();
				if(destination != null) {
					putVariable(destination, rv);
				}
			}

		}else if (i instanceof Jump) {
			Jump j = (Jump) i;

			Temporary regForPC = (Temporary) j.getRegForPC();

			putVariable(regForPC, getPC());

			int newPC = evaluateExp(j.getTarget());
			changePC(newPC);

		}else if (i instanceof SysCall) {
			int type = _globalRegFile.get(RiscRegisters.SYSCALL);
			_callState = "syscall";

			Integer result =  evaluateECall(type);
			if(result == null) {
				_pc -= 1;
			}else {
				_globalRegFile.put("a0", result);				
			}
		}
	}

	//SECTION: EXPRESSION UNWRAPPING
	int evaluateExp(Abs_Expression e) throws MemoryException, OperationException {
		int result = 0;
		if (e instanceof BinOp) {
			result = binOps((BinOp) e);

		}else if (e instanceof Literal) {
			result = ((Literal) e).get();

		}else if (e instanceof Temporary) {
			Temporary var = (Temporary) e;
			result = getVariable(var);

		}else if (e instanceof MemoryAddress) {
			MemoryAddress m = (MemoryAddress) e;
			result = getValueInMainMem(evaluateExp(m.getAddress()), m.getSize());

		}else if (e instanceof Symbol){
			result = _st.getCodeLine(((Symbol) e).get().trim());

		}else if(e instanceof DataLabel) {
			result = _dataTable.get(((DataLabel)e).getLabel().get());
		}
		return result;
	}
	private Integer evaluateECall(int type) throws MemoryException {
		int a0 = _globalRegFile.get("a0");
		switch(type) {

		case 1: //print int
			_iof.printInt(a0);
			break;

		case 4://print string
			String s = "";
			char l = (char) getValueInMainMem(a0, 1);
			while(l != '\0') {
				s += l;
				a0 += 1;
				l = (char) getValueInMainMem(a0, 1);
			}
			_iof.printString(s);
			break;

		case 5://read int
			return _iof.readInt("Enter an integer: ");

		case 8://read string
			byte[] string = _iof.readString("Enter a string: ");

			if(string != null) {
				int address =  _os.requestMemory(string.length + 1);
				putStringInMainMem(address, string);
				return address;
			}
			return null;

		case 9://request memory
			return _os.requestMemory(a0);

		case 10://quit
			stop();
			break;

		case 41: //random
			Random r = new Random();
			if(a0 > 0) {
				return r.nextInt(a0);
			}else {
				return r.nextInt();
			}
		}
		return 0;
	}
	private Integer evaluateCommand(String command, List<Abs_Expression> args) throws OperationException, MemoryException{

		if (command.equals("_stop")) {
			stop();

		}else if (command.equals("_print")){
			int a = evaluateExp(args.get(0));
			_iof.printInt(a);

		}else if(command.equals("_read")) {

			return _iof.readInt("Enter an integer: ");

		}else if (command.equals("_read_string")) {
			byte[] s = _iof.readString("Enter a string: ");
			int address =  _os.requestMemory(s.length + 1);
			putStringInMainMem(address, s);

			return address;

		}else if (command.equals("_read_char")) {
			char c = _iof.readChar("Enter a letter: ");
			int address =  _os.requestMemory(1);
			putValueInMainMem(address, 1, c);
			return address;

		}else if (command.equals("_print_string")){
			Abs_Expression e = args.get(0);

			if (e instanceof Temporary) {
				int address = getVariable((Temporary) e);
				String s = "";
				char l = (char) getValueInMainMem(address, 1);
				while(l != '\0') {
					s += l;
					address += 1;
					l = (char) getValueInMainMem(address, 1);
				}
				_iof.printString(s);

			}else {
				throw new OperationException("syntax for " + command);
			}

		}else if (command.equals("_print_char")){
			Abs_Expression e = args.get(0);

			if (e instanceof Temporary) {
				char c = (char) getVariable((Temporary) e);
				_iof.printChar(c);

			}else if (e instanceof Literal){
				Literal l = (Literal) e;
				_iof.printChar((char)l.get());

			}else {
				throw new OperationException("syntax for " + command);
			}

		}else if (command.equals("_allocate")) {
			int n = evaluateExp(args.get(0));
			return _os.requestMemory(n);

		}else if(command.equals("_random")) {
			Random random = new Random();
			int n = evaluateExp(args.get(0));
			return Math.abs(random.nextInt(n));

		}else if(command.equals("_schedule")) {
			Symbol function = (Symbol) args.get(0);
			String name = function.get();
			int destPC = evaluateExp(function);
			int waitTime = evaluateExp(args.get(1));
			_eventQ.add(new Task(name, waitTime, 0, destPC));

		}else if(command.equals("_set_key_handler")) {
			Symbol function = (Symbol) args.get(0);
			_keyHandler = evaluateExp(function);

		}else if(command.equals("_set_click_handler")) {
			Symbol function = (Symbol) args.get(0);
			_clickHandler = evaluateExp(function);

		}else if(command.equals("_clear_graphics")){
			Pair<Integer, Integer> graphicsRange = getOS().getGraphicsRange();
			for (int i = graphicsRange.first()*4; i < graphicsRange.second()*4; i+= 4) {
				putValueInMainMem(i, 4, 0);
			}
			((window.GraphicsFunctObj) _iof).updateScreen();

		}else if(command.equals("_clear_memory")) {
			resetMainMem();

		}else if(command.equals("_update_graphics")) {
			((window.GraphicsFunctObj) _iof).updateScreen();

		}else {
			throw new OperationException(command);
		}
		return 0;
	}

	//SECTION: EXECUTION & CALCULATION
	private int binOps(BinOp e) throws OperationException, MemoryException {
		Operation op = e.getOp();

		int arg1 = evaluateExp(e.getArg1());
		int arg2 = evaluateExp(e.getArg2());
		int result = 0;
		switch (op) {
		case ADD:
			result = arg1 + arg2;
			break;
		case SUB:
			result = arg1 - arg2;
			break;
		case MULT:
			result = arg1 * arg2;
			break;
		case DIV:
			try{
				result = arg1 / arg2;
			}catch(ArithmeticException e1){
				result = -1;
				_iof.reportError("Division by 0, defaulting to -1");
			}
			break;
		case DIV_U:
			try{
				result = Integer.divideUnsigned(arg1,arg2);
			}catch(ArithmeticException e1){
				result = (int) (Math.pow(2, 32) - 1);
				_iof.reportError("Division by 0, defaulting to " + result);
			}
			break;
		case REM:
			try{
				result = arg1 % arg2;
			}catch(ArithmeticException e1){
				result = arg1;
				_iof.reportError("Division by 0, defaulting to "+ result);
			}
			break;
		case REM_U:
			try{
				result = Integer.remainderUnsigned(arg1, arg2);
			}catch(ArithmeticException e1){
				result = arg1;
				_iof.reportError("Division by 0, defaulting to "+ result);
			}
			break;
		case SHIFT_LEFT:
			result = arg1 << arg2;
			break;
		case SHIFT_RIGHT_L:
			result = arg1 >>> arg2;
			break;
		case SHIFT_RIGHT_A:
			result = arg1 >> arg2;
			break;
			case OR:
				result = arg1 | arg2;
				break;
			case AND:
				result = arg1 & arg2;
				break;
			case X_OR:
				result = arg1 ^ arg2;
				break;
			case EQUAL:
				result = arg1 == arg2 ? 1 : 0;
				break;
			case NOT_EQUAL:
				result = arg1 != arg2 ? 1 : 0;
				break;
			case LESS:
				result = arg1 < arg2 ? 1 : 0;
				break;
			case LESS_U:
				result = Integer.compareUnsigned(arg1, arg2) < 0 ? 1 : 0;
				break;
			case LESS_EQUAL:
				result = arg1 <= arg2 ? 1 : 0;
				break;
			case LESS_EQUAL_U:
				result = Integer.compareUnsigned(arg1, arg2) <= 0 ? 1 : 0;
				break;
			case GREATER:
				result = arg1 > arg2 ? 1 : 0;
				break;
			case GREATER_U:
				result = Integer.compareUnsigned(arg1, arg2) > 0 ? 1 : 0;
				break;
			case GREATER_EQUAL:
				result = arg1 >= arg2 ? 1 : 0;
				break;
			case GREATER_EQUAL_U:
				result = Integer.compareUnsigned(arg1, arg2) >= 0 ? 1 : 0;
				break;

			default:
				throw new OperationException(op.toString());
		}
		return result;
	}

	//SECTION: RUN, STOP & HAS NEXT
	private boolean hasNextEvent() {
		return !_eventQ.isEmpty();
	}
	private void emptyQ() {
		_eventQ.clear();
	}
	public boolean hasNextInstr() {
		if(_pc < _instructions.size()) {
			return true;
		}else {
			return false;
		}
	}
	private intermediateRepresentation getNextInstr() {
		intermediateRepresentation next = _instructions.get(_pc);
		incPC(1);
		return next;
	}
	private void resetPC() {
		_pcState = true;
		_pc = 0;
	}
	public int getPC() {
		return _pc;
	}
	private void incPC(int x) {
		_pcState = true;
		_pc += x;
	}
	public void changePC(int dest) {
		_pcState = true;
		_pc = dest;
	}
	public void endPC() {
		_pcState = true;
		_pc = _instructions.size();
	}
	public void stop() {
		_run = false;
	}
	public void restart() {
		_run = true;
	}
	public boolean running() {
		return _run;
	}

	//SECTION: VARIABLE  OPERATIONS
	public int getVariable(Temporary t) {
		Integer value;
		if (t.isGlobal()) {
			value = _globalRegFile.get(t.get());
		}else {
			value = _tempRegFile.get(t.get());
		}
		if (value != null) {
			return value;
		}else {
			System.err.println("Error: Variable " + t + " undefined, defaulting to 0");
			return 0;
		}
	}
	private void putVariable(Temporary t, int value) {
		if (!_readOnlyVars.contains(t.get())) {
			if (t.isGlobal()) {
				_globalRegFile.put(t.get(), value);
			}else {
				_tempRegFile.put(t.get(), value);
			}
			_regState = t.get();
		}
	}
	public Map<String, Integer> getTempsMap() {
		return _tempRegFile;
	}
	private void clearTemps() {
		_tempRegFile = new HashMap<>();
	}
	private void setTemps(Map<String, Integer> regs) {
		_tempRegFile = regs;
	}
	public String prinTemps() {
		return _tempRegFile.toString();
	}
	public String showTempsStack() {
		return _registerStack.toString();
	}
	public Map<String, Integer> getGlobalsMap() {
		return _globalRegFile;
	}
	private void clearGlobals() {
		_globalRegFile = new HashMap<>();
	}
	public String printGlobals() {
		return _globalRegFile.toString();
	}
	public void setStackPointer(int sp){
		try {
			_os.setSP(sp);
			putVariable(new Temporary("sp"), sp);
		} catch (MemoryException e) {
			_iof.reportError("Cannot set stack pointer less than heap pointer");
		}

	}

	//SECTION: MAIN MEMORY OPERATIONS
	public OperatingSystem getOS() {
		return _os;
	}
	private void resetMainMem() {
		_mainMem = new MemorySystem();
		_os = new OperatingSystem();
	}
	public int getValueInMainMem(int address, int size) throws MemoryException {
		return _mainMem.getMemory(address, size);
	}
	public void putValueInMainMem(int address, int size, int value) throws MemoryException {
		_mainMem.putMemory(address, size, value);
		_memoryState = address;
	}
	public void putStringInMainMem(int address, byte[] bytes) throws MemoryException{
		int j = address;
		for (byte b : bytes) {
			putValueInMainMem(j, 1, b);
			j++;
		}
		putValueInMainMem(j, 1, '\0');
	}
	public String printMainMem() {
		return _mainMem.printMainMem();
	}

	//SECTION: DATA-IN FUNCTIONS FOR INTERFACE
	public void loadSymbolTable(SymbolTable st) {
		_st = st;
	}
	public void loadData(Data data) throws MemoryException {
		Map<String, Integer> dataLabels = data.getDataAssignMap();
		List<DataType> dataList = data.getDataList();
		List<Integer> addressList = new ArrayList<>();
		int address;
		for (DataType dataPoint : dataList) {
			if (dataPoint instanceof WordDir) {
				int x = ((WordDir)dataPoint).getValue();
				address = _os.requestMemory(4);
				putValueInMainMem(address, 4, x);
				addressList.add(address);
				
			}else if (dataPoint instanceof StringDir) {
				byte[] s = ((StringDir) dataPoint).getStringBytes();
				address = _os.requestMemory(s.length + 1);
				putStringInMainMem(address, s);
				
			}else if (dataPoint instanceof MemAllocDir) {
				address = _os.requestMemory(((MemAllocDir) dataPoint).getSize());
				addressList.add(address);
			}
		}

		for (Entry<String, Integer> entry : dataLabels.entrySet()) {
			_dataTable.put(entry.getKey(), addressList.get(entry.getValue()));
		}
	}
	public void loadInstructions(List<intermediateRepresentation> instructions) {
		_instructions = instructions;
	}
	public void loadGlobals(Map<String, Integer> specialVars, boolean graphics) throws MemoryException {
		if (specialVars != null) {
			for (String v: specialVars.keySet()) {
				_globalRegFile.put(v.toLowerCase(), specialVars.get(v));
			}
		}
		if (graphics) {
			int gridSize = getVariable(new Temporary("_grid_size", true));
			int graphicsStartIndex = _os.requestMemory(gridSize);
			_os.setGraphicsRange(graphicsStartIndex, graphicsStartIndex + gridSize);
			_globalRegFile.put("_grid_index", graphicsStartIndex);
		}
	}
	public void loadROs(Set<String> roVars) {
		_readOnlyVars = roVars;
	}
	public void logKeyEvent(int key) {
		if (_keyHandler != null) {
			Task t = new Task("key", 0, key, _keyHandler);
			_eventQ.add(t);
		}
	}
	public void logClickEvent(int coord) {
		if (_clickHandler != null) {
			Task t = new Task("click", 0, coord, _clickHandler);
			_eventQ.add(t);
		}
	}

	//SECTION: State information for interface
	private void stateReset() {
		_memoryState = null;
		_pcState = false;
		_regState = null;
		_callState = null;
		_returnState = false;
		_eventState = null;
	}
	public Integer memoryChange() {
		return _memoryState;
	}
	public boolean pcChange() {
		return _pcState;
	}
	public String regChange() {
		return _regState;
	}
	public String callOccur() {
		return _callState;
	}
	public boolean returnOccur() {
		return _returnState;
	}
	public String eventOccur() {
		return _eventState;
	}
}
