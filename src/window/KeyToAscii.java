package window;

import javafx.scene.input.KeyCode;
import backEndParsing.MachineNumber;

public class KeyToAscii {
	/**
	 * Turns a KeyCode into an ASCII value that the programmer can use.
	 * @param key - the KeyCode that the screen received as input.
	 * @return the ASCII value of the key that was pressed
	 */
	public static int getAscii(KeyCode key) {
		int code = 0;
		if (key.equals(KeyCode.ENTER)) {
			return MachineNumber.ENTER_ASCII;
		} else if (key.equals(KeyCode.SPACE)) {
			return MachineNumber.SPACE_ASCII;
		} else if (key.isLetterKey()) {
			char name = key.getName().charAt(0);
			return (int) name;
		} else if (key.isDigitKey()) {
			char digit = key.toString().charAt(5);
			return (int) digit;
		}
		return code;
	}
}
