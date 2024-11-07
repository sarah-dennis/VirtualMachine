package window;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import virtual_machine.IOFunctions;

/**
 * Class used as intermediary between the graphics input/output class and the Execution
 * used for dealing with read/print syscalls.
 */
public class GraphicsFunctObj implements IOFunctions{
	
	private static ErrorConsole _ec;
	private ScreenGrid _screen;
	private TextField _input;
	private Button submitter;
	private Runner runner;
	
	private boolean partOne = true; //set true if we're doing "part one" of the read
	private boolean printed = false; //set true if the read prompt has already printed
	private String inputResult;

	/**
	 * Constructor initializes the member variables of the class
	 * @param iof - the console used to handle input/output, error messages.
	 */
	public GraphicsFunctObj(ErrorConsole iof) {
		_ec = iof;
	}
	
	/**
	 * Provided so that the VM has its own way of reporting errors, 
	 * without throwing exceptions and halting execution.
	 * @param message - the content of the error
	 */
	public void reportError(String message) {
		_ec.reportError(message, null, null);
	}
	
	/**
	 * Attaches a runner so that reading stops the virtual machine
	 * @param newRun - the runner being attached
	 */
	void setRunner(Runner newRun) {
		runner = newRun;
	}
	
	/**
	 * Prints integer to the error field
	 * @param i - the int to be printed
	 */
	public void printInt(int i) {
		_ec.printPrompt(""+i);
	}

	/**
	 * Reads integer input from user. NOTE: only works if program is not being animated.
	 * @param prompt - tells user the destination of the integer input
	 * @return the integer provided by the user, or 0 if retrieved input is invalid.
	 */
	public Integer readInt(String prompt) {
		Integer num = 0;
		if (partOne) {
			if (!printed) {
				_ec.printPrompt(prompt);
				printed = true;
			}
			readInput();
			return null;
		} else {
			String result = inputResult;
			try {
				num = Integer.parseInt(result);
				_ec.finishRead(String.valueOf(num));
			} catch (NumberFormatException e) {
				_ec.reportError("Invalid input: "+result+"; using 0 as input", 0, null);
			}
			partOne = true;
			printed = false;
			inputResult = null;
			return num;
		}
	}
	
	/**
	 * Reads string input from user and returns byte array version of that string.
	 * NOTE: this will only work if the program is not being animated.
	 * @param message - to tell user the destination of the string.
	 * @return byte array of input string.
	 */
	public byte[] readString(String message) {
		if (partOne) {
			if (!printed) {
				_ec.printPrompt(message);
				printed = true;
			}
			readInput();
			return null;
		} else {
			partOne = true;
			printed = false;
			byte[] result = inputResult.getBytes();
			_ec.finishRead(inputResult);
			inputResult = null;
			return result;
		}
	}

	/**
	 * Prints out a string to the input/output area.
	 * @param s - the string being printed.
	 */
	public void printString(String s) {
		_ec.printPrompt(s);
	}

	/**
	 * Get one char from input box (if more than one char is entered, it only reads the first one)
	 * @param message - the input message to tell the user where the char is going
	 */
	public Character readChar(String message) {
		if (partOne) {
			if (!printed) {
				_ec.printPrompt(message);
				printed = true;
			}
			readInput();
			return null;
		} else {
			partOne = true;
			Character result = inputResult.charAt(0);
			inputResult = null;
			printed = false;
			_ec.finishRead(String.valueOf(result));
			return result;
		}
	}
	
	/**
	 * Prints out one char.
	 * @param c - the character being printed.
	 */
	public void printChar(char c) {
		_ec.printPrompt(String.valueOf(c));
	}
	
	/**
	 * Convenience method attaches the ScreenGrid for graphics, allowing for a 'clear screen' method in the VM.
	 * @param screen - the ScreenGrid to be used. 
	 */
	public void attachScreen(ScreenGrid screen) {
		_screen = screen;
	}
	
	/**
	 * Updates the attached ScreenGrid according to the contents within memory. 
	 */
	public void updateScreen() {
		if (_screen != null) {
			_screen.paintAll();
		}
	}
	
	/**
	 * Private helper does the hard part of reading input
	 * @param prompt - the message that will be displayed in the popup window
	 * @return the String that the user typed in.
	 */
	private void readInput() {
		_input.setDisable(false);
		submitter.setDisable(false);
		runner.setReading(true);
	}
	
	/**
	 * Attaches the TextField for the input to the gfo
	 * @param input - the TextField where the user will enter input
	 */
	void loadInputBox(TextField input, Button submitButton) {
		_input = input;
		submitter = submitButton;
		submitter.setOnAction(event -> {
			inputResult = _input.getText();
			runner.setReading(false);
			partOne = false;
			_input.clear();
			_input.setDisable(true);
			submitter.setDisable(true);
		});
	}
}
