package window;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import virtual_machine.MemoryException;
import virtual_machine.ExecVM;

/**
 * Class represents the graphical displays of the main memory, accessed via the virtual machine. 
 */
public class MemoryGraphics {
	
	private double fontSize = 11;
	private String memStyle = "-fx-font-size: ";
	
	private int displayStartIndex;
	static final int DISPLAY_RANGE = 20;
	static final long MEMORY_RANGE = 2147483647 + 1;
	
	public int radix; //initial display is in hex (see constructor)
	private String hexFormatLong = "%08x";
	private String hexFormatShort = "%02x";
	private String decFormatShort = "%03d";
	
	ExecVM _vm;
	private Map<Integer, List<TextField>> _memDisplays = new HashMap<>(DISPLAY_RANGE);
	private List<Text> _displayAddresses = new ArrayList<>(DISPLAY_RANGE);

	private boolean simpleMode; //true if we're in "simple mode" - displaying word size, in decimal rather than hex, etc.
	private ErrorConsole ec;
	
	/**
	 * Constructor initializes member variables. Using loadVM determines 
	 * whether it should be in simple mode and initializes the display list accordingly.
	 * @param console - the input/output console for handling errors in memory access.
	 * @param vm - the virtual machine that holds the data of the simulated memory.
	 * @param mode - the mode that this is initializing in. 
	 */
	public MemoryGraphics(ErrorConsole console, ExecVM vm, String mode) {
		ec = console;
		for (int i = 0; i < DISPLAY_RANGE; i ++) {
			String text;
			if (mode.equals("yaal")) {
				text = " " + i;
			} else {
				text = " " + String.format(hexFormatLong, i*4);
			}
			Text addDisplay = new Text(text);
			addDisplay.setStyle(memStyle + fontSize + "px;");
			_displayAddresses.add(addDisplay);
			List<TextField> placeholderList = new ArrayList<TextField>();
			_memDisplays.put(i, placeholderList);
		}
		loadVM(vm, mode);
		if (simpleMode) {
			displayStartIndex = 0;
		} else {
			displayStartIndex = vm.getOS().getStackPointer();
		}
	}
	
	/**
	 * Simultaneously updates the displayed contents of main memory and scrolls the memory display. 
	 * @param pageAmt - the number of addresses by which the display will shift
	 */
	public void display(int pageAmt) {
		if (pageAmt != 0) {
			//if we're scrolling the memory, make sure that the previously input memory values get remembered
			updateMemoryFromDisplay();
		}
		displayStartIndex += pageAmt;
		int address = displayStartIndex;
		int i = 0;
		int displayStop;
		displayStop = address + (DISPLAY_RANGE * 4);
		while (address < displayStop) {
			List<TextField> l = _memDisplays.get(i);
			if (simpleMode) {
				String aDisp = String.format("%9s", address/4);
				Text addressDisplay = _displayAddresses.get(i);
				addressDisplay.setText(aDisp);
				int wordInt = 0;
				try {
					wordInt = _vm.getValueInMainMem(address, 4);
				} catch (MemoryException e) {
					String errorMessage = e.getMessage() + " in graphics";
					ec.reportError(errorMessage, null, null);
				}
				l.get(0).setText(""+wordInt);
			} else {
				String aDisp = String.format(hexFormatLong, address);
				Text addressDisplay = _displayAddresses.get(i);
				addressDisplay.setText(aDisp);
				int k = 0;
				for (int j = l.size() - 1; j >= 0; j--) {
					int byteInt = 0;
					try {
						byteInt = _vm.getValueInMainMem(address + j, 1);
					} catch (MemoryException e) {
						String errorMessage = e.getMessage() + " in graphics";
						ec.reportError(errorMessage, null, null);
					}
					String format;
					if (radix == 10) {
						format = decFormatShort;
					} else {
						format = hexFormatShort;
					}
					l.get(k).setText(String.format(format, byteInt));
					k++;
				}
			}
			address += 4;
			i++;
		}
	}
	
	/**
	 * Returns the TextField displays that show the contents held within one word of memory. 
	 * @param index - the position of this display in the graphics window
	 * @return list of displays for one word-aligned address in memory. If this is 'simple mode,'
	 * the list will only have one address to represent the whole word. Otherwise, the list contains
	 * one TextField for each byte of the memory contents. 
	 */
	public List<TextField> getValDisplays(int index) {
		return _memDisplays.get(index);
	}
	
	/**
	 * Returns the Text representation of memory address being displayed
	 * @param index - the position of this address in the current display
	 * @return the Text showing the address of a memory location. 
	 */
	public Text getAddDisplay(int index) {
		return _displayAddresses.get(index);
	}
	
	/**
	 * Returns the address at the beginning of the current display
	 * @return the address at the start of the display
	 */
	public int getDisplayIndex() {
		return displayStartIndex;
	}
	
	/**
	 * Changes the radix of the memory content displays
	 * @param newRad - the new radix to use for display, i.e. 10 or 16.
	 */
	public void setDisplayRadix(int newRad) {
		radix = newRad;
		if (simpleMode) {
			radix = 10; //to prevent errors, radix is always ten in yaal mode
		}
		display(0);
	}
	
	/**
	 * Reads the values in the memory displays, checks whether those values have changed from what was in memory before, 
	 * and if those values have changed, changes the values in memory to reflect what's on the display. 
	 */
	private void updateMemoryFromDisplay() {
		try {
			for (int i = 0; i < DISPLAY_RANGE; i++) {
				List<TextField> valueDisplays = _memDisplays.get(i);
				String toParse = "";
				for (TextField t : valueDisplays) {
					toParse += t.getText();
				}
				int newVal = Integer.parseInt(toParse, radix);
				String s = _displayAddresses.get(i).getText().trim();
				int address;
				if (simpleMode) {
					address = Integer.parseInt(s) * 4;
				} else {
					address = Integer.parseInt(s, 16);
				}
				int memSize = 4;
				int oldVal = _vm.getValueInMainMem(address, memSize);
				if (oldVal != newVal) {
					_vm.putValueInMainMem(address, memSize, newVal);
				}
			}
		} catch (NumberFormatException | MemoryException e) {
			ec.reportError(e.toString() + " in display", null, null);
		}
	}
	
	/**
	 * updates the display, assuming the contents of memory are already reset.
	 */
	public void clear() {
//		_vm.resetMainMem(); //memory is reset in MainTest
		for (int i = 0; i < DISPLAY_RANGE; i++) {
			List<TextField> l = _memDisplays.get(i);
			for (TextField t : l) {
				t.setText(String.format("%02x", 0));
				//if this were truly robust it would get the display values from memory
				//but we just cleared memory, so rather than waste time looking up values, just blanket set them to zero
			}
		}
		display(0);
	}

	/**
	 * Moves the display index to start at the address given
	 * @param address - the memory slot that will appear at the start of the display
	 * 			Function automatically ensures that the address is word-aligned
	 */
	public void go(int address) {
		if (address != 0) {
			address = address - (address % 4);
		}
		int pgAmt = address - displayStartIndex;
		display(pgAmt);
	}
	
	/**
	 * Switches the memory display to go to Simple Mode - display radix is 10, 
	 * displays whole word rather than byte-by-byte, etc.
	 */
	private void switchToSimpleMode() {
		radix = 10;
		for (Integer i : _memDisplays.keySet()) {
			int value = 0;
			try {
				value = _vm.getValueInMainMem((i*4), 4);
			} catch (MemoryException e) {
				String errorMessage = e.getMessage() + " in graphics switch mode";
				ec.reportError(errorMessage, null, null);
			}
			List<TextField> newList = new ArrayList<TextField>();
			TextField newInput = new TextField("" + value);
			newInput.setOnKeyPressed(event -> {
				if (event.getCode().toString().equals("ENTER")) {
					updateOneValue(i, 0, newInput.getText());
				}
			});
			newInput.setStyle(memStyle + fontSize + "px;");
			newInput.setAlignment(Pos.BASELINE_RIGHT);
			newInput.setPrefColumnCount(12);
			newList.add(newInput);
			_memDisplays.put(i, newList);
		}
		display(0);
	}
	
	/**
	 * Switches display map to represent regular memory - displays contents/addresses in hexadecimal notation,
	 * displays contents in individual bytes, etc.
	 */
	private void switchFromSimpleMode() {
		radix = 16;
		for (Integer i : _memDisplays.keySet()) {
			List<TextField> newList = new ArrayList<TextField>(4);
			for (int j = 3; j >= 0; j--) { //j keeps track of address position
				TextField newInput = new TextField();
				int byteVal = 0;
				try {
					byteVal = _vm.getValueInMainMem(i + j, 1);
				} catch (MemoryException e) {
					String errorMessage = e.getMessage() + " in graphics switch mode";
					ec.reportError(errorMessage, null, null);
				}
				int addressPosition = j;
				String format = hexFormatShort;
				newInput.setText(String.format(format, byteVal));
				newInput.setOnKeyPressed(event -> {
					if (event.getCode().toString().equals("ENTER")) {
						updateOneValue(i, addressPosition, newInput.getText());
					}
				});
				newInput.setStyle(memStyle + fontSize + "px;");
				newInput.setAlignment(Pos.BASELINE_RIGHT);
				newInput.setPrefColumnCount(4);
				newList.add(newInput);
			}
			_memDisplays.put(i, newList);
		}
		display(0);
	}
	
	/**
	 * Sets the virtual machine to be a new, mode-specific virtual machine.
	 * Also initializes/sets simpleMode.
	 * @param vm - the new, mode-specific VirtualMachine
	 * @param mode - the mode being switched to
	 */
	public void loadVM(ExecVM vm, String mode) {
		_vm = vm;
		if (mode.equals("yaal")) {
			simpleMode = true;
			switchToSimpleMode();
		} else {
			simpleMode = false;
			switchFromSimpleMode();
		}
	}
	
	/**
	 * Used to update values as they are entered in the TextFields. 
	 * @param mapIndex - the position of this value in the display window, to fetch the address
	 * @param listIndex - the byte position of the new value, relative to the word-aligned address
	 * @param newVal - the string to be parsed and put into memory
	 */
	private void updateOneValue(int mapIndex, int listIndex, String newVal) {
		String textAddress = _displayAddresses.get(mapIndex).getText();
		int address = Integer.parseInt(textAddress.trim(), radix);
		int memSize;
		if (simpleMode) {
			memSize = 4;
			address = address * 4;
		} else {
			memSize = 1;
			address += listIndex;
		}
		try {
			int val = Integer.parseInt(newVal, radix);
			_vm.putValueInMainMem(address, memSize, val);
		} catch (NumberFormatException | MemoryException e){
			ec.reportError(e.getMessage(), null, null);
		}
	}
	
	/**
	 * Resizes the graphics of the memory display
	 * @param scale - the percentage scale of the new display
	 */
	public void resize(double scale) {
		double newFontSize = fontSize * scale;
		for (Text addressDisplay : _displayAddresses) {
			addressDisplay.setStyle(memStyle + newFontSize + "px;");
		}
		for (Map.Entry<Integer, List<TextField>> entry : _memDisplays.entrySet()) {
			List<TextField> valueDisplayList = entry.getValue();
			for (TextField valueDisplay : valueDisplayList) {
				valueDisplay.setStyle(memStyle + newFontSize + "px;");
			}
		}
	}
	
	/**
	 * The Runner class keeps track of the memory watchpoints and knows when those points change.
	 * To indicate that a watched memory address has changed, this method highlights said address label.
	 * @param watcher - the watched address that has been changed
	 */
	public void markWatcher(int watcher) {
		boolean watched = false;
		for (Text addressDisplay : _displayAddresses) {
			int address = Integer.parseInt(addressDisplay.getText(), radix);
			if (address == watcher) {
				addressDisplay.setStyle("-fx-background-color: violet;");
				watched = true;
			} else {
				addressDisplay.setStyle("-fx-background-color: #f0d8ff;");
			}
		}
		if (!watched) {
			ec.printPrompt("Watched memory address " + watcher + " has changed.");
		}
	}
}
