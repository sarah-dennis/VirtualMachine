package textInterface;

import java.util.Scanner;

import virtual_machine.IOFunctions;

public class TextInterface_IOF implements IOFunctions{

	public void printInt(int i) {
		System.out.println(i);
	}

	public Integer readInt(String message) {
		System.out.println(message);
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		if(scanner.hasNextInt()) {
			int i = scanner.nextInt();
			return i;
		}else {
			System.err.println("Error: Non-integer entered, using default value 0");
			return 0;
		}
		
	}

	public byte[] readString(String message) {
		System.out.println(message);
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		if(scanner.hasNextLine()) {
			String i = scanner.nextLine();
			return i.getBytes();
		}else {
			return null;
		}
	}

	public void printString(String s) {
		System.out.println(s);
	}

	public void printChar(char c) {
		System.out.print(c);	
	}

	public Character readChar(String message) {
		System.out.println(message);
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		if(scanner.hasNextByte()) {
			byte i = scanner.nextByte();
			return (char) i;
		}else {
			return 0;
		}
	}

	public void reportError(String message) {
		System.err.println(message);
	}
}
