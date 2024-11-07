package virtual_machine;

import java.util.ArrayList;
import java.util.Arrays;

import util.Pair;

public class OperatingSystem {
	
	private int _memAllocPointer;
	private int _stackPointer;
	private Pair<Integer, Integer> _graphicsRange;
	
	private static final ArrayList<String> systemOps = new ArrayList<>(Arrays.asList(
						"_read", "_read_string", "_print", "_print_string", "_update_graphics",
						"_random", "_allocate","_stop", "_print_char", "_read_char", "_schedule",
						"_set_key_handler", "_set_click_handler", "_clear_graphics", "_clear_memory")); 
	
	public static ArrayList<String> getSystemOps() {
		return systemOps;
	}
	
	public OperatingSystem(){
		_memAllocPointer = 4000; //memory grows up
		_stackPointer = 80000;   //stack grows down
	}
	
	public int requestMemory(int n) throws MemoryException {
		final int WORD_SIZE = 4;
		int a = _memAllocPointer;
		_memAllocPointer += n + ((WORD_SIZE - (n % WORD_SIZE)) % WORD_SIZE);
		
		if(_memAllocPointer >= _stackPointer || a - n < 0) {
			throw new MemoryException("Not enough memory remaining to assign " + n + " bytes");
		}
		return a;
	}
	
	public int getStackPointer() {
		return _stackPointer;
	}
	
	public int getMemAllocPointer() {
		return _memAllocPointer;
	}
	
	public void setMemAllocPointer(int hp) {
		_memAllocPointer = hp;
	}
	
	public boolean isSystemOp(String op) {
		return systemOps.contains(op);
	}

	public void setGraphicsRange(int graphicsStartIndex, int graphicsEndIndex) {
		_graphicsRange = new Pair<Integer, Integer>(graphicsStartIndex, graphicsEndIndex);
	}

	public Pair<Integer, Integer> getGraphicsRange() {
		return _graphicsRange;
	}
	
	public void setSP(int sp) throws MemoryException {
		if(sp <= _memAllocPointer) {
			throw new MemoryException("Cannot set stack pointer less than heap pointer");
		}
		_stackPointer = sp;
	}
}
