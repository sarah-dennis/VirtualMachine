package window;

import java.util.ArrayList;

import org.fxmisc.richtext.CodeArea;

/**
 * Class deals with the input and output - displaying errors, print statements.
 * Possibly collects input from user. 
 */
public class ErrorConsole {

	private CodeArea log = new CodeArea(DEFAULT_ERROR_MESSAGE);
	private static final String DEFAULT_ERROR_MESSAGE = "No messages;";
	private ArrayList<String> _messages;
	private int errorCount;
	
	/**
	 * Constructor initializes the output area and its display height;
	 * also initializes the list of messages to be displayed and the number of errors. 
	 */
	public ErrorConsole() {
		log.setEditable(false);
		//log.setId("error-field");
		//log.setPrefHeight(100);
		_messages = new ArrayList<String>();
		errorCount = 0;
	}
	
	/**
	 * Returns the CodeArea where output will be displayed
	 * @return the output CodeArea
	 */
	CodeArea getErrorField() {
		return log;
	}
	
	/**
	 * Prints an error and, if possible, highlights that line in the displays.
	 * @param message - the message from the exception, usually
	 * @param line - the line number where error occurred
	 * @param method - passed in from MainTest, lambda method for highlighting a line in displayed code.
	 */
	public void reportError(String message, Integer line, Highlighter method) {
		String displayMessage;
		if (method == null || line == null) {
			displayMessage = "Error: " + message + ";\n";
		} else {
			displayMessage = "Error: " + message + " @line " + (line+1) + ";\n";
			method.colorLine(line);
		}
		_messages.add(displayMessage);
		errorCount++;
		showOut();
	}
	
	/**
	 * Prints a message in the output field
	 * @param message to be printed
	 */
	public void printPrompt(String message) {
		message += "\n";
		_messages.add(message);
		showOut();
	}
	
	/**
	 * Upon completion of a read operation, appends the user input to the end of the original prompt.
	 * This allows the user to be sure that their input was read correctly. 
	 * @param result - the user input resulting from the read
	 */
	public void finishRead(String result) {
		String prompt = _messages.remove(_messages.size() - 1);
		String newPrompt = prompt.trim() + " \t" + result + "\n";
		_messages.add(newPrompt);
		showOut();
	}

	/**
	 * For use once program is finished - shows error list, cleans up the rest of it
	 * @param form - what kind of program just finished, i.e. assembly, program execution, etc.
	 */
	public void logErrors(String form) {
		String countMessage = form + " finished: " + errorCount + " errors found\n";
		_messages.add(countMessage);
		showOut();
		errorCount = 0;
		_messages.clear();
	}
	
	/**
	 * Clears error field, replaces default message, and resets message list/error count.
	 */
	public void clear() {
		_messages.clear();
		log.clear();
		//log.replaceText(0,0,DEFAULT_ERROR_MESSAGE);
		errorCount = 0;
	}
	
	/**
	 * shows all of the messages in the message list at the moment
	 */
	private void showOut() {
		String displayMessage = String.join("", _messages);
		log.clear();
		//log.replaceText(0,0,displayMessage);
	}
	
	/**
	 * dummy interface for passing lambda expressions into a function
	 */
	public interface Highlighter {
		public void colorLine(int i);
	}
}

