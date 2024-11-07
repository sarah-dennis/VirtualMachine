package window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import RISC.RiscRegisters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import util.Pair;
import virtual_machine.ExecVM;
import virtual_machine.MemoryException;
import virtual_machine.OperatingSystem;

/**
 * Class represents the graphical displays of the register file, accessed via the Virtual Machine.
 */
public class RegisterGraphics {

	//REGISTER THINGS
	private Map<String, Integer> regVals; //map to represent risc regs/global vars
	private Map<String, Integer> tempVals; //map to represent local vars
	
	//DISPLAY THINGS
	private double regFontSize = 11;
	private String baseStyle = "-fx-font-size: ";
	private double pcFontSize = 13;
	private GridPane regGrid = new GridPane(); //GridPane to display global vars/risc regs
	private GridPane varGrid = new GridPane(); //GridPane to display local vars
	int globalDisplayIndex = 0;
	int localDisplayIndex = 0;
	private double padding = 10;
	private TextField pcView;
	private Button leftG;
	private Button rightG;
	private Button leftL;
	private Button rightL;
	private boolean simpleMode;
	private int radix;
	private String hexValFormat = "0x%08x";
	private String decValFormat = "%10s";
	
	//CANVAS THINGS
	private double CANVAS_WIDTH = 200;
	private double CANVAS_HEIGHT = 300;
	private double maxBarHeight = 280;
	private double barWidth = 80;
	private double barVGap = 10;
	private double barHGap = 15;
	private double percentFontSize = 18;
	private double pointerLabelSize = 14;
	private Canvas graphic = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
	private int totalStackSize;
	private int spStartVal;
	private int maStartVal;
	
	//IMPORTANT VALUE THINGS
	private Map<Integer, Pair<Text, TextField>> _regs;
	private List<String> overflowVars = new ArrayList<String>();
	private Map<Integer, Pair<Text, TextField>> _tempVars;
	private List<String> overflowTemps = new ArrayList<String>();
	private List<Integer> sourceLineList = new ArrayList<Integer>();
	private ExecVM _vm;
	private ErrorConsole _ec;
	
	/**
	 * Constructor initializes member variables. By default, the program counter display is initialized.
	 * loadVM should then take care of initializing the displays based on the mode. 
	 * @param error - the input/output console for handling errors
	 * @param vm - the virtual machine that houses the actual registers
	 * @param mode - the mode upon initialization
	 */
	public RegisterGraphics(ErrorConsole error, ExecVM vm, String mode) {
		_ec = error;
		_vm = vm;
		OperatingSystem _os = _vm.getOS();
		spStartVal = _os.getStackPointer();
		maStartVal = _os.getMemAllocPointer();
		totalStackSize = spStartVal - maStartVal;
		pcView = new TextField();
		pcView.setStyle(baseStyle + pcFontSize + "px;");
		pcView.setOnKeyPressed(key -> {
			if (key.getCode().toString().equals("ENTER")) {
				int newPC = Integer.parseInt(pcView.getText(), radix);
				_vm.changePC(newPC);
			}
		});
//		pcView.setEditable(false);
		regVals = _vm.getGlobalsMap();
		if (mode.equals("risc-v")) {
			_regs = makeRiscRegFile();
			simpleMode = false;
			radix = 16;
		} else {
			_regs = makeYaalGlobalVars();
			tempVals = _vm.getTempsMap();
			_tempVars = makeYaalTempVars();
			simpleMode = true;
			radix = 10;
		}
		updatePC();
	}
	
	/**
	 * Initializes the risc register display; assumes that the virtual machine has the correct registers.
	 * @return the map used to keep track of the texts and text fields for display
	 */
	private Map<Integer, Pair<Text, TextField>> makeRiscRegFile() {
		Map<Integer, Pair<Text, TextField>> riscMap = new HashMap<>();
		for (String regName : RiscRegisters.REGISTERS) {
			overflowVars.add(regName);
		}
		for (int i = 0; i < 32; i++) {
			String regName = overflowVars.get(i);
			Text display = new Text(String.format("%6s", regName));
			display.setStyle(baseStyle + regFontSize + "px;");
			TextField displayValue = new TextField();
			displayValue.setPrefColumnCount(12);
			displayValue.setStyle(baseStyle + regFontSize + "px;");
			String format = radix == 16 ? hexValFormat : decValFormat;
			String valText = "";
			if (!regName.equals("")) {
				valText = String.format(format, regVals.getOrDefault(regName, 0));
			}
			displayValue.setText(valText);
			Pair<Text, TextField> p = new Pair<>(display, displayValue);
			riscMap.put(i, p);
		}
		regGrid = new GridPane(); //re-initialize regGrid between mode switching
		regGrid.setGridLinesVisible(true);
		regGrid.setPadding(new Insets(padding));
		int valueIndex = 0;
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 16; c++) {
				Pair<Text, TextField> pair = riscMap.get(valueIndex);
				if ((c % 2) == 0) {
					Text displayName = pair.first();
					regGrid.add(displayName, c, r);
				} else {
					TextField displayVal = pair.second();
					regGrid.add(displayVal, c, r);
					valueIndex++;
				}
			}
		}
		return riscMap;
	}
	
	/**
	 * Initializes the global variable display for yaal mode. 
	 * @return the map used to keep track of the texts and text fields for display. 
	 */
	private Map<Integer, Pair<Text, TextField>> makeYaalGlobalVars() {
		Map<Integer, Pair<Text, TextField>> yaalGlobalMap = new HashMap<>();
		for (String regName : regVals.keySet()) {
			overflowVars.add(regName);
		}
		for (int i = 0; i < 16; i++) {
			String regName = "";
			String displayText = "";
			if (i < overflowVars.size()) {
				regName = overflowVars.get(i);
				displayText = regName.substring(1);
			} else {
				//add an empty string to fill up the register name list
				overflowVars.add(regName);
			}
			Text nameDisplay = new Text(String.format("%6s", displayText));
			nameDisplay.setStyle(baseStyle + regFontSize + "px;");
			TextField value = new TextField();
			value.setPrefColumnCount(8);
			value.setStyle(baseStyle + regFontSize + "px;");
			value.setAlignment(Pos.BASELINE_RIGHT);
			String valText = regName.equals("") ? "" : String.valueOf(regVals.get(regName));
			value.setText(valText);
			yaalGlobalMap.put(i, new Pair<Text, TextField>(nameDisplay, value));
		}
		regGrid = new GridPane();
		regGrid.setGridLinesVisible(true);
		regGrid.setPadding(new Insets(padding));
		int valueIndex = 0;
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 8; c++) {
				Pair<Text, TextField> pair = yaalGlobalMap.get(valueIndex);
				if ((c % 2) == 0) {
					Text displayName = pair.first();
					regGrid.add(displayName, c, r);
				} else {
					TextField displayVal = pair.second();
					regGrid.add(displayVal, c, r);
					valueIndex++;
				}
			}
		}
		return yaalGlobalMap;
	}
	
	/**
	 * Initializes the display used for temporary variables in yaal mode.
	 * @return the map to keep track of the display elements.
	 */
	private Map<Integer, Pair<Text, TextField>> makeYaalTempVars() {
		Map<Integer, Pair<Text, TextField>> yaalTempMap = new HashMap<>();
		for (String regName : tempVals.keySet()) {
			overflowTemps.add(regName);
		}
		for (int i = 0; i < 16; i++) {
			String regName = "";
			if (i < overflowTemps.size()) {
				regName = overflowTemps.get(i);
			} else {
				//add an empty string to fill up the register name list
				overflowTemps.add(regName);
			}
			Text nameDisplay = new Text(String.format("%6s", regName));
			nameDisplay.setStyle(baseStyle + regFontSize + "px;");
			TextField value = new TextField();
			value.setPrefColumnCount(8);
			value.setStyle(baseStyle + regFontSize + "px;");
			value.setAlignment(Pos.BASELINE_RIGHT);
			String valText = regName.equals("") ? "" : String.valueOf(regVals.get(regName));
			value.setText(valText);
			yaalTempMap.put(i, new Pair<Text, TextField>(nameDisplay, value));
		}
		varGrid = new GridPane();
		varGrid.setGridLinesVisible(true);
		varGrid.setPadding(new Insets(padding));
		int valueIndex = 0;
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 8; c++) {
				Pair<Text, TextField> pair = yaalTempMap.get(valueIndex);
				if ((c % 2) == 0) {
					Text displayName = pair.first();
					varGrid.add(displayName, c, r);
				} else {
					TextField displayVal = pair.second();
					varGrid.add(displayVal, c, r);
					valueIndex++;
				}
			}
		}
		return yaalTempMap;
	}
	
	/**
	 * Getter returns the pane that displays the global variables/registers
	 * @return the GridPane that shows the texts and text fields.
	 */
	public GridPane getRegGrid() {
		return regGrid;
	}
	
	/**
	 * Getter returns the pane that shows the local variables
	 * @return the GridPane that shows the texts and text fields.
	 */
	public GridPane getTempVarGrid() {
		return varGrid;
	}
	
	/**
	 * Convenience method lumps together the various update methods.
	 */
	public void updateDisplay() {
		if (simpleMode) {
			updateTempGrid(0);
		}
		updateGlobalGrid(0);
		updatePC();
	}
	
	/**
	 * Creates the list of RISC-V registers to be loaded into 
	 * the virtual machine's global register map.
	 * @return the list that will be turned into global registers in the virtual machine
	 */
	public Map<String, Integer> makeRiscRegs() {
		Map<String, Integer> regList = new TreeMap<>();
		for (String reg : RiscRegisters.REGISTERS) {
			int value = 0;
			if (reg.equals("sp")) {
				value = _vm.getOS().getStackPointer();
			}
			regList.put(reg, value);
		}
		return regList;
	}
	
	/**
	 * Checks to make sure that all the global variable names are correct - 
	 * used in yaal mode, where the global variables are flexible.
	 */
	private void updateGlobalNames() {
		if (simpleMode) {
			regVals = _vm.getGlobalsMap();
			while (overflowVars.contains("")) {
				overflowVars.remove("");
			}
			for (String regName : regVals.keySet()) {
				if (!overflowVars.contains(regName)) {
					overflowVars.add(regName);
				}
			}
			overflowVars.add("");
			while ((overflowVars.size() % 16) != 0) {
				overflowVars.add("");
			}
		}
	}
	
	/**
	 * Checks to make sure that the local variable names are up to date.
	 * Used in Yaal mode. 
	 */
	private void updateLocalNames() {
		if (simpleMode) {
			tempVals = _vm.getTempsMap();
			overflowTemps.clear();
			for (String regName : tempVals.keySet()) {
				if (!overflowTemps.contains(regName)) {
					overflowTemps.add(regName);
				}
			}
			overflowTemps.add("");
			while ((overflowTemps.size() % 16) != 0) {
				overflowTemps.add("");
			}
		}
	}
	
	/**
	 * Updates the box displaying global variables/risc registers. Can also scroll the display. 
	 * @param pgAmt - the number of variables to scroll past.
	 */
	private void updateGlobalGrid(int pgAmt) {
		if (simpleMode) {
			globalDisplayIndex += pgAmt;
			updateGlobalNames();
		} else {
			regVals = _vm.getGlobalsMap();
			updateGraphic();
		}
		int regIndex = globalDisplayIndex;
		int index = 0;
		while (index < _regs.size()) {
			Pair<Text, TextField> displayPair = _regs.get(index);
			String regName = overflowVars.get(regIndex);
			Text displayName = displayPair.first();
			if (simpleMode) {
				String displayNameText = !regName.equals("") ? regName.substring(1) : "";
				displayName.setText(String.format("%6s", displayNameText));
			} else {
				displayName.setText(String.format("%6s", regName));
			}
			TextField displayVal = displayPair.second();
			int newVal = regVals.getOrDefault(regName, 0);
			String valText;
			if (simpleMode) {
				if (regName.equals("")) {
					valText = "";
				} else {
					valText = String.valueOf(newVal);
				}
			} else {
				String format = radix == 16 ? hexValFormat : decValFormat;
				valText = String.format(format, newVal);
			}
			displayVal.setText(valText);
			regIndex++;
			index++;
		}
		if ((leftG != null && rightG != null)) {
			if (globalDisplayIndex == 0) {
				leftG.setDisable(true);
			} else {
				leftG.setDisable(false);
			}
			int lastVarIndex = globalDisplayIndex + 15;
			if (overflowVars.get(lastVarIndex).equals("")) {
				rightG.setDisable(true);
			} else {
				rightG.setDisable(false);
			}
		}
	}
	
	/**
	 * Updates the box displaying the temporaries/local variables. Also scrolls the display.
	 * @param pgAmt - the number of registers by which to scroll.
	 */
	private void updateTempGrid(int pgAmt) {
		updateLocalNames();
		localDisplayIndex += pgAmt;
		int regIndex = localDisplayIndex;
		int index = 0;
		while (index < _tempVars.size()) {
			Pair<Text, TextField> displayPair = _tempVars.get(index);
			String regName = overflowTemps.get(regIndex);
			Text displayName = displayPair.first();
			displayName.setText(String.format("%6s", regName));
			TextField displayVal = displayPair.second();
			int newVal = tempVals.getOrDefault(regName, 0);
			String valText = "";
			if (!regName.equals("")) {
				valText = String.valueOf(newVal);
			}
			displayVal.setText(valText);
			regIndex++;
			index++;
		}
		if ((leftL != null && rightL != null)) {
			if (localDisplayIndex == 0) {
				leftL.setDisable(true);
			} else {
				leftL.setDisable(false);
			}
			int lastTempIndex = localDisplayIndex + 15;
			if (overflowTemps.get(lastTempIndex).equals("")) {
				rightL.setDisable(true);
			} else {
				rightL.setDisable(false);
			}
		}
	}
	
	/**
	 * Updates the program counter according to the list containing the source lines.
	 * If the source lines have not been loaded in, then displays the current value of the program counter plus one.
	 */
	private void updatePC() {
		int pcDisplay;
		if (_vm.getPC() >= sourceLineList.size()) {
			//this should only happen if sourceLineList hasn't been loaded yet, meaning the PC should still be at 0.
			pcDisplay = _vm.getPC()+1;
		} else {
			pcDisplay = sourceLineList.get(_vm.getPC());
		}
		String pcText;
		if (radix == 16) {
			pcText = String.format("0x%08x", pcDisplay);
		} else {
			pcText = String.valueOf(pcDisplay);
		}
		pcView.setText(pcText);
	}
	
	/**
	 * Returns the pc text field, which should be kept separate from the other register fields
	 * @return the text field that shows the value in the program counter
	 */
	public TextField getPCView() {
		return pcView;
	}
	
	/**
	 * Resets the register displays. Assumes the virtual machine has already been reset
	 */
	public void reset() {
		try {
			if (simpleMode) {
				_vm.loadGlobals(ScreenGrid.getGraphicsConstants(), true);
				_vm.loadROs(ScreenGrid.getGraphicsConstants().keySet());
			} else {
				_vm.loadGlobals(makeRiscRegs(), false);
				_vm.loadROs(RiscRegisters.READ_ONLY);
			}
		} catch (MemoryException e) {
			_ec.reportError("Problem loading globals: " + e.toString(), null, null);
		}
		updateGlobalGrid(0);
		updatePC();
		if (simpleMode) {
			updateTempGrid(0);
		} else {
			updateGraphic();
		}
	}
	
	/**
	 * Switches the display to be prepared for the new mode - re-initializes the display maps, etc.
	 * @param newVM - the new virtual machine containing the correct register maps
	 * @param mode - the mode being switched to.
	 */
	public void switchMode(ExecVM newVM, String mode) {
		_vm = newVM;
		overflowVars.clear();
		regVals = _vm.getGlobalsMap();
		if (mode.equals("risc-v")) {
			simpleMode = false;
			_regs = makeRiscRegFile();
			radix = 16;
		} else {
			simpleMode = true;
			tempVals = _vm.getTempsMap();
			_regs = makeYaalGlobalVars();
			_tempVars = makeYaalTempVars();
			radix = 10;
		}
		updateDisplay();
	}
	
	/**
	 * Loads in the source lines, used by the program counter to display the correct line number.
	 * @param newLineList - the list of source line numbers.
	 */
	public void loadLineList(List<Integer> newLineList) {
		sourceLineList = newLineList;
	}
	
	/**
	 * Used by the main class to scroll the global variable display.
	 * If the last variable in the display is an empty string, that indicates the next page is also empty,
	 * so no scrolling happens.
	 * @param pgAmt - the number of registers by which to scroll the display.
	 */
	public void scrollGlobal(int pgAmt) {
		int lastVarIndex = globalDisplayIndex + 15;
		if (overflowVars.get(lastVarIndex).equals("")) {
			pgAmt = 0;
		}
		updateGlobalGrid(pgAmt);
	}
	
	/**
	 * Used by the main class to scroll the local variable display.
	 * If the last variable in the display is an empty string, that indicates the next page is also empty,
	 * so no scrolling happens.
	 * @param pgAmt - the number of registers by which to scroll the display.
	 */
	public void scrollTemp(int pgAmt) {
		int lastVarIndex = localDisplayIndex + 15;
		if (overflowTemps.get(lastVarIndex).equals("")) {
			pgAmt = 0;
		}
		updateTempGrid(pgAmt);
	}
	
	/**
	 * Sets the buttons that will be attached to the global display, so that this object can handle 
	 * disabling them when bounds change.
	 * @param left - the button that scrolls left
	 * @param right - the button that scrolls right
	 */
	public void setGlobalButtons(Button left, Button right) {
		leftG = left;
		rightG = right;
	}
	
	/**
	 * Sets the buttons that will be attached to the the local display, so 
	 * this object can handle disabling them when bounds change.
	 * @param left - the button that scrolls left
	 * @param right - the button that scrolls right
	 */
	public void setLocalButtons(Button left, Button right) {
		leftL = left;
		rightL = right;
	}
	
	/**
	 * Resizes the graphical objects
	 * @param scale - the percentile scale by which to resize things
	 */
	public void resize(double scale) {
		double newPadding = padding * scale;
		double newregFontSize = regFontSize * scale;
		double newpcFontSize = pcFontSize * scale;
		regGrid.setPadding(new Insets(newPadding));
		varGrid.setPadding(new Insets(newPadding));
		pcView.setStyle(baseStyle + newpcFontSize + "px;");
		resizeDisplay(_regs, newregFontSize);
		if (simpleMode) {
			resizeDisplay(_tempVars, newregFontSize);
		} else {
			resizeGraphic(scale);
		}
	}
	
	/**
	 * Resizes the register file display to match the rest of the window.
	 * @param varMap - the set of register displays being resized
	 * @param fontSize - the new size of the font
	 */
	private void resizeDisplay(Map<Integer, Pair<Text, TextField>> varMap, double fontSize) {
		for (Map.Entry<Integer, Pair<Text, TextField>> entry : varMap.entrySet()) {
			Pair<Text, TextField> displayPair = entry.getValue();
			displayPair.first().setStyle(baseStyle + fontSize + "px;");
			displayPair.second().setStyle(baseStyle + fontSize + "px;");
		}
	}
	
	/**
	 * Sets the radix that the registers display in
	 * @param rad - the new radix being set
	 */
	public void setDisplayRadix(int rad) {
		if (simpleMode) {
			radix = 10;
		} else {
			radix = rad;
		}
		updateDisplay();
	}
	
	/**
	 * Sets the virtual machine's stack pointer and the display's stack pointer. 
	 * @param newSP - the new value of the stack pointer
	 */
	public void setStackPointer(int newSP) {
		if (newSP < 60000) {
			_ec.printPrompt("Warning: setting the stack pointer below 60,000 is unadvised. Proceed with caution.");
		} else if (newSP % 4 != 0) {
			newSP = newSP - (newSP % 4);
			_ec.printPrompt("Warning: cannot set stack pointer to non word-aligned value. "
					+"Setting stack pointer to " + newSP + " instead.");
		}
		_vm.setStackPointer(newSP);
		spStartVal = newSP;
		regVals.put("sp", newSP);
		updateGraphic();
	}
	
	/**
	 * Function checks to make sure that the stack pointer never gets below the memory allocation pointer
	 * @return true if the stack pointer is still above the memory allocation pointer. 
	 */
	public boolean testSP() {
		if (!simpleMode) {
			int currentSP = _vm.getGlobalsMap().get("sp");
			int currentMA = _vm.getOS().getMemAllocPointer();
			if (currentSP <= currentMA) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}
	
	/**
	 * If a register has been set as a watchpoint in Runner, then Runner will know when that register changes.
	 * To indicate that a watched register has changed, the name of the register is marked in purple.
	 * @param regName - the watched register being changed
	 */
	public void markWatcher(String regName) {
		boolean watched = false;
		if (overflowVars.contains(regName)) {
			for (Map.Entry<Integer, Pair<Text, TextField>> entry : _regs.entrySet()) {
				Text nameDisplay = entry.getValue().first();
				if (nameDisplay.getText().equals(regName)) {
					nameDisplay.setStyle("-fx-background-color: violet;");
					watched = true;
				} else {
					nameDisplay.setStyle("-fx-background-color: f0d8ff;");
				}
			}
			if (!watched) {
				//Should only print if the watched register is not currently displayed.
				_ec.printPrompt("Watched register " + regName + " has changed.");
			}
		} else if (simpleMode && overflowTemps.contains(regName)) {
			for (Map.Entry<Integer, Pair<Text, TextField>> entry : _tempVars.entrySet()) {
				Text nameDisplay = entry.getValue().first();
				if (nameDisplay.getText().equals(regName)) {
					nameDisplay.setStyle("-fx-background-color: violet;");
					watched = true;
				} else {
					nameDisplay.setStyle("-fx-background-color: f0d8ff;");
				}
			}
			if (!watched) {
				_ec.printPrompt("Watched register " + regName + " has changed.");
			}
		}
	}
	
	/**
	 * Allows access to the canvas object for display purposes
	 * @return the canvas where the SP illustration is drawn
	 */
	public Canvas spGraphic() {
		if (!simpleMode) {
			updateGraphic();
		}
		return graphic;
	}
	
	/**
	 * Updates the stack pointer illustration according to current values.
	 */
	void updateGraphic() {
		GraphicsContext gc = graphic.getGraphicsContext2D();
		gc.setFill(Color.web("#f4f4f4")); //set off-white background (same color as -fx-inner-control)
		gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
		//fill in the bar 'shadow'
		gc.setFill(Color.web("#f0d8ff"));
		gc.fillRect(barHGap, barVGap, barWidth, maxBarHeight);
		//calculate the new position of SP and memory allocation pointer
		//Assumes that if sp is null, then the register map hasn't been initialized, so automatically sets sp's initial value.		
		double currentSPVal = regVals.getOrDefault("sp", spStartVal);
		double currentMAVal = _vm.getOS().getMemAllocPointer();
		double spLost = spStartVal - currentSPVal;
		double percentSpShrink = spLost / totalStackSize;
		double spYPos = barVGap + (percentSpShrink * maxBarHeight);
		double percentMaGrowth = 1 - ((currentMAVal - maStartVal) / maStartVal);
		double maYPos = barVGap + (percentMaGrowth * maxBarHeight);
		double newBarHeight = maYPos - spYPos;
		//calculate percent of stack available
		int percentStackAvail = (int)((newBarHeight / maxBarHeight) * 100);
		//fill in the new bar position
		gc.setFill(Color.VIOLET);
		gc.fillRect(barHGap, spYPos, barWidth, newBarHeight);
		//figure out labels and text position
		gc.setFont(Font.font("Courier New", FontWeight.BOLD, percentFontSize));
		gc.setFill(Color.PURPLE);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.fillText(String.format("%1$s%2$c", percentStackAvail, '%'), ((barWidth / 2) + barHGap), (CANVAS_HEIGHT / 2));
		//label top and bottom of bar with values. 
		gc.setFont(Font.font("Courier New", FontWeight.BOLD, pointerLabelSize));
		gc.setTextAlign(TextAlignment.LEFT);
		String spLabel = String.format("%,d", ((int)currentSPVal));
		String heapLabel = String.format("%,d", ((int)currentMAVal));
		double labelXPos = (CANVAS_WIDTH / 2) + (barVGap / 2);
		gc.fillText("SP: " + spLabel, labelXPos, spYPos, barWidth);
		gc.fillText("Heap: " + heapLabel, labelXPos, maYPos, barWidth);
	}
	
	/**
	 * Resizes the stack pointer illustration to match the scale of the rest of the screen
	 * @param scale - the percentage scale of the new size
	 */
	private void resizeGraphic(double scale) {
		CANVAS_WIDTH = 200 * scale;
		CANVAS_HEIGHT = 300 * scale;
		maxBarHeight = 280 * scale;
		barWidth = 80 * scale;
		barVGap = 10 * scale;
		barHGap = 15 * scale;
		percentFontSize = 18 * scale;
		pointerLabelSize = 14 * scale;
		graphic = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
		updateGraphic();
	}
	
//	NOTE: this is an old version; will need to be updated before it is actually implemented.
//	public void updateFromDisplay() {
//		for (int i = 0; i < REGISTER_COUNT; i++) {
//			TextField register = _regs.get(i);
//			String toParse = register.getText().substring(2);
//			int fieldVal = Integer.parseUnsignedInt(toParse, 16); //or use parseInt();
//			_vm.putValueInReg(i, fieldVal);
//		}
//	}
}