package virtual_machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MemorySystem {
	
	private Map<Integer, Integer> _mainMem;
	
	private static final int WORDSIZE = 32;
	
	public MemorySystem() {
		_mainMem = new HashMap<Integer, Integer>();
	}
	
	public void putMemory(int address, int size, int value) throws MemoryException {
		int baseAddress = address / 4;
		int offset = address % 4;
		
		if (offset % size != 0) {
			throw new MemoryException("Address " + address + " is not word alligned");
		}else if (baseAddress < 0) {
			throw new MemoryException("Cannot access negative memory address " + address);
		}

		int sizeN = size * 8;
		int offsetK = offset * 8;
		
		int mask = (-1 >>> (WORDSIZE - sizeN)) << offsetK;
		
		int valueV = (value << offsetK) & mask;
		int valueU = _mainMem.getOrDefault(baseAddress, 0) & (~mask);
		
		int insertValue = valueU | valueV;
		_mainMem.put(baseAddress, insertValue); 
	}
	
	public int getMemory(int address, int size) throws MemoryException {
		int baseAddress = address / 4;
		int offset = address % 4;
		
		if (offset % size != 0) {
			throw new MemoryException("Address" + address + "is not word alligned");
		}else if (baseAddress < 0) {
			throw new MemoryException("Cannot access negative memory address " + address);
		}
		
		int baseValue = _mainMem.getOrDefault(baseAddress, 0);
		
		if (size == 4) { //word
			return baseValue;
			
		}else if (size == 2) { //half word
			return (baseValue >>> (offset * 8)) & 0xFFFF;
			
		}else if (size == 1) { //byte
			return (baseValue >>> (offset * 8)) & 0xFF;
			
		}else {
			throw new MemoryException("Requesting invalid number of bytes");
		}
	}
	
	public String printMainMem() {
		ArrayList<String> nonEmpties = new ArrayList<>();
		for(Integer i : _mainMem.keySet()) {
			nonEmpties.add(i + ": " + _mainMem.get(i));
		}
		return nonEmpties.toString();
	}
}
