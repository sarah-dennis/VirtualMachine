package virtual_machine;

public interface IOFunctions {
	
	void printInt(int i);
	Integer readInt(String message);
	
	void printChar(char c);
	Character readChar(String message);
	
	byte[] readString(String message);
	void printString(String s);
	
	public void reportError(String message);
	
}
