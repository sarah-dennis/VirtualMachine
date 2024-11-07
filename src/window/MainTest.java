package window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import operation_syntax.OperationException;
import parse.Data;
import parse.ParseException;
import parse.ParseInterface;
import parse.ParseOutput;
import RISC.RiscRegisters;
import RISC.RiscvParse;
import YAAL.YaalParse;
import util.Pair;
import backEndParsing.*;
import intermediateRepresentation.*;
import virtual_machine.*;

/**
 * This class launches the application and deals with the graphics. 
 * load/save file functions retrieved from java-buddy.blogspot.com
 * rich text editor supplied by RichTextFX on GitHub
 * @version 7.9.18
 */
public class MainTest extends Application {
	
	private double sizeScale = 1; //the scale of the current sizing things (percent value)
	private final int label_font_size = 16; //the starting font size of labels
	private final int small_font_size = 10; //starting font size of small text (page number counters)
	private boolean hexOrBin = false; //set true if output is in hex, false if in binary

	/* Booleans that toggle view panes in the editor: */
	private boolean symOrStack = false; //true if the symbol table is showing; false if sp graphic is showing
	private boolean memView = true; //true if memory is showing, false if registers showing
	//Note that memView is toggled immediately upon startup; hence it will appear as the opposite in the window. 
	private boolean abstractView = false; //set true if the middle pane of intermediate code is showing
	private boolean extraView = true; //true if the extra pane (machine code in risc, 'screen' in yaal) is showing
	
	/* Booleans that deal with running the program */
	boolean assembled = false; //true if the program has been assembled (if the parsed instructions have been loaded to VM)
	static boolean programRunning = false; //true if either of the AnimationTimer objects are running 
	private boolean animated = false; //set true if running the program highlights the lines
	
	public ErrorConsole ec = new ErrorConsole(); //handles output, as well as error reporting

	private String[] MODES = {"yaal", "risc-v"}; //new modes should be added to this list
	String currentMode = MODES[1]; //to set initial mode, just change this value
	private String defaultMessage = "main:\n    "; //note that if you change initial mode, the default message should change
	
	private CodeArea _input = new CodeArea(defaultMessage); //the area where the 'assembly language' is displayed/edited
	private CodeArea _output = new CodeArea("output"); //the area where machine code is displayed/edited
	private CodeArea _middle = new CodeArea("abstract"); //the area where the abstract intermediate representation is displayed
	private Text columnCounter = new Text(" 0:0"); //the object that displays the column position of the caret, if applicable
	//the list of source code lines, so that the animation/program counter are accurate to the graphical display
	private List<Integer> sourceLines = new ArrayList<>();
	
	/* These are the custom objects involved in the execution/display of the program */
	GraphicsFunctObj gfo = new GraphicsFunctObj(ec); //handles interaction between graphics/execution
	ExecVM vm = new ExecVM(gfo); //holds the instructions, the data, the memory, and the registers
	RegisterGraphics regFile; //the graphical representation of the registers
	MemoryGraphics memory; //the graphical representation of the memory system
	ScreenGrid gridView; //the 'screen' that displays memory graphically
	EventLists events = new EventLists(); //the class used to keep track of key/click events in graphics and breakpoints.
	Runner runner; //the custom animation timer that runs the program
	
	SymbolTable symTab = new SymbolTable(events, makeTableClickHandler()); //the graphical version of the symbol table
	
	private Button switchMemReg; //switches the view between registers and memory
	private Button switchMachineOutput; //switches the machine code between hexadecimal and decimal numbers
	private Button switchSymStack; //switches view between symbol table and stack pointer graphic
	private MainToolBar tb = new MainToolBar(); //the tool bar holding all the coolest buttons (execution actions and assembly)
	private StackPane symTabStack = new StackPane(); //stackpane where the symbol table/stack pointer graphic is shown
	
	//_window: The outside BorderPane; houses the editor areas as well as Symbol Table, Register, and Main Memory displays.
	BorderPane _window = new BorderPane();
	//_editor: the inner BorderPane holds code areas showing assembly, machine code, and 'error console'
	BorderPane _editor;
	ScrollPane scroller;
	
	Subscription highlightSub; //used for syntax highlighting in the _input pane
	//workingFile: The file in which the user is working. If this file is never initialized/opened, 
	//the application will not save changes upon exit.
	private File workingFile;
	private boolean savedRecently = false; //set true if recent changes have been saved to a file
//	private File workingDirectory = new File(System.getProperty("user.home"), ".risc-y-business/work_files");
	
	public static void main(String[] args) {
		launch();
	}
	
	@Override //----------- SECTION: application start ----------------------------------
	public void start(Stage stage) throws Exception { 
		//Step One: initialize the major objects needed: virtual machine, execution, memory and register displays, etc.
		ec = new ErrorConsole();
		gfo = new GraphicsFunctObj(ec);
		vm = new ExecVM(gfo);
		regFile = new RegisterGraphics(ec, vm, currentMode);
		memory = new MemoryGraphics(ec, vm, currentMode);
		gridView = new ScreenGrid(vm, ec, events);
		gfo.attachScreen(gridView);
		runner = new Runner(vm, ec, gridView, line -> highlightLine(line), events, tb, regFile, memory);
		gfo.setRunner(runner);
		//Now that these things have been initialized, load the globals and update the screen
		if (currentMode.equals(MODES[0])) {
			vm.loadGlobals(ScreenGrid.getGraphicsConstants(), true);
			vm.loadROs(ScreenGrid.getGraphicsConstants().keySet());
			runner.setDefaults(true);
		} else if (currentMode.equals(MODES[1])) {
			vm.loadGlobals(regFile.makeRiscRegs(), false);
			vm.loadROs(RiscRegisters.READ_ONLY);
			runner.setDefaults(false);
		}
		regFile.updateDisplay();
		
		//Text editor section - the left shows the assembly language code, 
		//while the right displays machine code in either bits or hex digits
		_input.setParagraphGraphicFactory(LineNumberFactory.get(_input));
		_output.setParagraphGraphicFactory(LineNumberFactory.get(_output));

		_middle.setId("abstract-area");
		_middle.setEditable(false);
		
		StackPane.setAlignment(columnCounter, Pos.BOTTOM_LEFT);
		
		highlightSub = _input.multiPlainChanges()
				.successionEnds(Duration.ofMillis(500))
				.subscribe(ignore -> _input.setStyleSpans(0, HighlightedInput.computeHighlighting(_input.getText())));

		//Navigation buttons: must be declared somewhere where they can access _window,
		//before the rest of the panes get set
		
		//Initialize the button to be added to _output's StackPane
		switchMachineOutput = new Button("0x0a");
		switchMachineOutput.setOnAction(event -> {
			String source = _output.getText();
			if (hexOrBin) {
				setText(_output, changeToBin(source));
			} else {
				setText(_output, changeToHex(source));
			}
			if (hexOrBin) {
				switchMachineOutput.setText("1010");
			} else {
				switchMachineOutput.setText("0x0a");
			}
			ec.logErrors("Machine Code Conversion");
		});
		StackPane.setAlignment(switchMachineOutput, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(switchMachineOutput, new Insets(5));
		//Initialize the button that switches memory/register view
		switchMemReg = new Button("M");
		switchMemReg.setOnAction(event -> {
			_window.setBottom(regMem(currentMode, sizeScale));
		});
		StackPane.setAlignment(switchMemReg, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(switchMemReg, new Insets(5));
		switchSymStack = new Button("SP");
		switchSymStack.setOnAction(event -> {
			changeSymPane(sizeScale);
		});
		StackPane.setAlignment(switchSymStack, Pos.TOP_RIGHT);
		StackPane.setMargin(switchSymStack, new Insets(0, 5, 0, 5));
		
		//Initialize the tool bar's button actions
		createButtonBar();
		//Set up the editor pane layout
		makeEditorPane(currentMode, sizeScale);
		
		//create register file/memory display area
		_window.setBottom(regMem(currentMode, sizeScale));
		//create symbol table display area
		_window.setRight(symtabBox(sizeScale));
		//Set the user effects when changes happen in _input
		_input.setOnKeyPressed(event -> {
			String code = event.getCode().toString();
			if (!code.equals("UP") && !code.equals("LEFT") && !code.equals("DOWN") && !code.equals("RIGHT")) {
				//if the key pressed was a letter, symbol, the space key, etc, then the file will need to be reassembled.
				if (workingFile != null && savedRecently) {
					savedRecently = false;
					String oldTitle = stage.getTitle();
					int endIndx = oldTitle.length() - 8;
					stage.setTitle(oldTitle.substring(0, endIndx));
				}
				assembled = false;
				tb.setStatus("not assembled");
			}
			//Hack-job way of implementing auto-indent.
			if (code.equals("ENTER")) {
				int pos = _input.getCaretPosition();
				_input.insertText(pos, "    ");
			}
			updateColumnCounter();
		});
		_output.setOnKeyPressed(event -> {
			String code = event.getCode().toString();
			if (!code.equals("UP") && !code.equals("LEFT") && !code.equals("DOWN") && !code.equals("RIGHT")) {
				assembled = false;
				tb.setStatus("not assembled");
			}
		});
		
		_window.setTop(menuBarMaker(stage));
		
		scroller = new ScrollPane();
		scroller.setContent(_window);
		scroller.setFitToHeight(true);
		scroller.setFitToWidth(true);

		Scene scene = new Scene(scroller);
		scene.setOnKeyPressed(event -> {
			int keyVal = KeyToAscii.getAscii(event.getCode());
			//Note: for some reason, the space bar does not get received by the scene.
			events.addKey(keyVal);
		});
		scene.getStylesheets().add(MainTest.class.getResource("/resources/run-window.css").toExternalForm());
		scene.getStylesheets().add(HighlightedInput.getStyleClassName());
		
		stage.setTitle("RISC-y Business (" + currentMode + ")");
		stage.setScene(scene);
		stage.setOnCloseRequest(event -> {
			//For convenience in debugging, the application does not bother the user 
			//if the user hasn't bothered to save a new file or open an old one.
			if (workingFile != null && !(_input.getText().equals(defaultMessage))) {
				if (!savedRecently) {
					QuitBox qbox = new QuitBox(stage);
					if (!qbox.getCanQuit()) {
						event.consume();
					} else {
						highlightSub.unsubscribe();
						System.exit(0);
					}
				}
			}
		});
		//stage.setMaximized(true);
		stage.show();
	}
	//	END START FUNCTION
//_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_=_
	
//------ SECTION: Editor pane, tool bar, and MenuBar (including mode switching) -----------
	
	/**
	 * Sets/resets the editor pane to reflect the given mode or view.
	 * @param mode - the mode which the new editor pane will display in
	 * @param scale - the percent scale of the display size
	 */
	private void makeEditorPane(String mode, double scale) {
		String inputText = _input.getText();
		String midText = _middle.getText();
		String outText = _output.getText();
		vm.resetALL();
		if (mode.equals(MODES[0])) {
			gridView.clearGrid();
		} else {
			setText(_output, "output");
		}
		_input.setEditable(true);
		ec.clear();
		_editor = new BorderPane();
		_editor.setMinSize((800 * scale), (475 * scale));
		VirtualizedScrollPane<CodeArea> inPane = new VirtualizedScrollPane<CodeArea>(_input);
		VBox innerBox = new VBox();
		VBox.setVgrow(inPane, Priority.ALWAYS);
		innerBox.getChildren().addAll(tb.getToolBar(), inPane);
		StackPane inBox = editorField(innerBox);
		BorderPane rightBox = new BorderPane();
		VirtualizedScrollPane<CodeArea> midPane = new VirtualizedScrollPane<CodeArea>(_middle);
		VirtualizedScrollPane<CodeArea> outPane = new VirtualizedScrollPane<CodeArea>(_output);
		outPane.setPrefWidth(400 * scale);
		VirtualizedScrollPane<CodeArea> errPane = new VirtualizedScrollPane<CodeArea>(ec.getErrorField());
		StackPane outButPane = new StackPane();
		outButPane.getChildren().addAll(outPane, switchMachineOutput);
		if (abstractView) {
			setText(_middle, midText);
			if (extraView) {
				if (currentMode.equals(MODES[0])) {
					midPane.setPrefWidth(300 * scale);
					rightBox.setRight(gridView.getDisplay());
				} else if (currentMode.equals(MODES[1])) {
					midPane.setMinWidth(200 * scale);
					rightBox.setRight(outButPane);
				}
			} else {
				midPane.setPrefWidth(400 * scale);
			}
			rightBox.setCenter(midPane);
			rightBox.setBottom(errPane);
		} else {
			if (extraView) {
				if (currentMode.equals(MODES[0])) {
					rightBox.setCenter(gridView.getDisplay());
				} else if (currentMode.equals(MODES[1])) {
					setText(_output, outText);
					rightBox.setCenter(outButPane);
				}
				rightBox.setBottom(errPane);
			} else {
				errPane.setPrefWidth(200 * scale);
				rightBox.setCenter(errPane);
			}
		}
		_editor.setCenter(inBox);
		_editor.setRight(rightBox);
		_window.setCenter(_editor);
		tb.setStatus("not assembled");
		setText(_input, inputText);
	}
	
	private StackPane editorField(VBox inBox) {
		StackPane pane = new StackPane();
		pane.getChildren().add(inBox);
		pane.getChildren().add(columnCounter);		
		return pane;
	}
	
	/**
	 * Initializes the tool bar button functions, using convenience functions
	 * supplied by the MainToolBar object.
	 */
	private void createButtonBar() {
		tb.setStart(event -> startProgram());
		tb.setPause(event -> pauseProgram());
		tb.setStep(event -> stepProgram());
		tb.setStop(event -> stopProgram());
		tb.setReset(event -> resetProgram());
		tb.setAssemble(event -> assemble());
		tb.setDisassemble(event -> {
			if (currentMode.equals(MODES[1])) {
				disassembleRiscV();
			} //Note: add other modes in else branches
		});
		tb.setMode(currentMode);
	}
	
	/**
	 * Creates the menu bar for the window, with File, View, Program, and Options menus. 
	 * @param stage - the stage where the menu will be located (required for save/load functions)
	 * @return the MenuBar with all appropriate menus
	 */
	private MenuBar menuBarMaker(final Stage stage) {
		MenuBar bar = new MenuBar();
		
		//Create file menu with save, save as, open, clear, and quit options
		Menu fileMenu = new Menu("File");
		MenuItem save = new MenuItem("Save");
		save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
		save.setOnAction(event -> {
			if (workingFile == null) {
				String source = _input.getText();
				saveFileAs(source, stage);
			} else {
				SaveFile(_input.getText(), workingFile);
				savedRecently = true;
				stage.setTitle(stage.getTitle() + " - Saved");
			}
		});
		MenuItem saveAs = new MenuItem("Save As");
		saveAs.setOnAction(event -> {
			String source = _input.getText();
			saveFileAs(source, stage);
		});
		MenuItem load = new MenuItem("Open");
		load.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
		load.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
			fileChooser.getExtensionFilters().add(extFilter);
			
//			if (!workingDirectory.exists()) {
//				workingDirectory.mkdirs();
//			}
//			fileChooser.setInitialDirectory(workingDirectory);
//			
			//Show load file dialog
			File file = fileChooser.showOpenDialog(stage);
			if(file != null){
				clear();
				setText(_input, readFile(file));
				stage.setTitle("RISC-y Business (" + currentMode + "): " + file.getName() + " - Saved");
			}
			workingFile = file;
			savedRecently = true;
		});
		MenuItem create = new MenuItem("Open new");
		create.setOnAction(event -> {
			MainTest newWindow = new MainTest();
			try {
				newWindow.start(new Stage());
			} catch (Exception e) {
				ec.reportError(e.toString(), 0, null);
				ec.logErrors("Opening new window");
			}
		});
		MenuItem clear = new MenuItem("Clear");
		clear.setOnAction(event -> clear());
		clear.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));
		MenuItem quit = new MenuItem("Quit");
		quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
		quit.setOnAction(event -> {
			System.out.println(savedRecently);
			if (!savedRecently) {
				QuitBox qbox = new QuitBox(stage);
				if (qbox.getCanQuit()) {
					highlightSub.unsubscribe();
					System.exit(0);
				}
			} else {
				highlightSub.unsubscribe();
				System.exit(0);
			}
		});
		fileMenu.getItems().addAll(save, saveAs, load, create, clear, quit);
		
		//Create view menu with main memory display radix, memory grid display, and intermediate code view options.
		Menu viewMenu = new Menu("View");
		Menu memoryView = new Menu("Display Radix");
		MenuItem decRadix = new MenuItem("10");
		decRadix.setOnAction(event -> {
			memory.setDisplayRadix(10);
			regFile.setDisplayRadix(10);
		});
		MenuItem hexRadix = new MenuItem("16");
		hexRadix.setOnAction(event -> {
			memory.setDisplayRadix(16);
			regFile.setDisplayRadix(16);
		});
		memoryView.getItems().addAll(hexRadix, decRadix);
		MenuItem showAbstractView = new MenuItem("Toggle intermediate code view");
		showAbstractView.setOnAction(event -> {
			abstractView = !abstractView;
			makeEditorPane(currentMode, sizeScale);
		});
		MenuItem toggleMachine = new MenuItem("Toggle machine code/screen view");
		toggleMachine.setOnAction(event -> {
			extraView = !extraView;
			makeEditorPane(currentMode, sizeScale);
		});
		MenuItem zoomIn = new MenuItem("Zoom in");
		zoomIn.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
		zoomIn.setOnAction(event -> {
			sizeScale += 0.25;
			zoomer(sizeScale);
		});
		MenuItem zoomOut = new MenuItem("Zoom out");
		zoomOut.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN));
		zoomOut.setOnAction(event -> {
			sizeScale += -0.25;
			zoomer(sizeScale);
		});
		Menu displayMenu = new Menu("Display");
		displayMenu.getItems().addAll(memoryView, zoomIn, zoomOut);
		viewMenu.getItems().addAll(showAbstractView, toggleMachine, displayMenu);
		
		//Create Program menu to add accelerators for assemble, disassemble, and run. 
		Menu system = new Menu("Program");
		MenuItem assembler = new MenuItem("Assemble");
		assembler.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
		assembler.setOnAction(event -> assemble());
		MenuItem disassembler = new MenuItem("Disassemble");
		disassembler.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
		disassembler.setOnAction(event -> {
			if (currentMode.equals(MODES[1])) {
				disassembleRiscV();
			}
			//Add other modes in else branches
		});
		MenuItem spValue = new MenuItem("Change stack pointer value");
		spValue.setOnAction(event -> {
			if (!programRunning && !currentMode.equals(MODES[0])) {
				int oldSP = vm.getOS().getStackPointer();
				String prompt = "Current sp: " + oldSP + ";\nEnter new stack pointer:";
				int newSP = Runner.readSetting(stage.getOwner(), prompt, oldSP, "stack pointer");
				regFile.setStackPointer(newSP);
			} else if (programRunning) {
				ec.reportError("Cannot set new stack pointer while running", 0, null);
			} else if (currentMode.equals(MODES[0])) {
				ec.reportError("Stack pointer not visible", 0, null);
			}
		});
		CheckMenuItem animationRunnerItem = new CheckMenuItem("Run program with animation");
		animationRunnerItem.setSelected(animated);
		animationRunnerItem.setOnAction(event -> {
			animationRunnerItem.setSelected(!animated);
			animated = !animated;
		});
		MenuItem setRegWatcher = new MenuItem("Set register as watchpoint");
		setRegWatcher.setOnAction(event -> runner.setRegWatchPt(stage));
		MenuItem setMemWatcher = new MenuItem("Set memory address as watchpoint");
		setMemWatcher.setOnAction(event -> runner.setMemWatchPt(stage));
		MenuItem clearWatchPoints = new MenuItem("Clear all watchpoints");
		clearWatchPoints.setOnAction(event -> runner.clearWatchers());
		//Note: run preference MenuItems are created and initialized within the Runner object.
		Menu runPrefs = new Menu("Set 'run' preferences");
		runPrefs.getItems().addAll(runner.setRunPrefMem(), runner.setRunPrefReg(), runner.setRunPrefPC(),
				runner.setRunPrefCall(), runner.setRunPrefRet(), runner.setRunPrefEvent(), runner.setRunPrefSpeed(stage));
		system.getItems().addAll(assembler, disassembler, spValue, animationRunnerItem,
				setRegWatcher, setMemWatcher, clearWatchPoints, runPrefs);
		
		//Creates options menu, with option for changing mode
		Menu optionMenu = new Menu("Options");
		Menu modeMenu = makeModeMenu();
		optionMenu.getItems().add(modeMenu);
		
		//Help menu opens a web browser to display the documentation and examples.
		Menu helpMenu = new Menu("Help");
		final WebView browser = new WebView();
		final WebEngine engine = browser.getEngine();
		Stage helpStage = new Stage();
		helpStage.initOwner(stage.getOwner());
		Scene helpScene = new Scene(browser);
		helpScene.getStylesheets().add(MainTest.class.getResource("/resources/run-window.css").toExternalForm());
		helpStage.setScene(helpScene);
		MenuItem examples = new MenuItem("Examples");
		examples.setOnAction(event -> {
			engine.load(MainTest.class.getResource("/helpWebpage/yaal_examples.html").toExternalForm());
			helpStage.show();
			helpStage.toFront();
		});
		MenuItem yaalInfo = new MenuItem("YAAL User's Guide");
		yaalInfo.setOnAction(event -> {
			engine.load(MainTest.class.getResource("/helpWebpage/documentation.txt").toExternalForm());
			helpStage.show();
			helpStage.toFront();
		});
		helpMenu.getItems().addAll(yaalInfo, examples);
		
		bar.getMenus().addAll(fileMenu, createEditMenu(), viewMenu, system, optionMenu, helpMenu);
		return bar;
	}
	
	/**
	 * Initializes the Mode menu, where user can change the mode of the application between Risc-V or YAAL.
	 * (placed in a side method to handle the toggle group separately)
	 * @return the Menu that displays the mode options.
	 */
	private Menu makeModeMenu() {
		Menu moder = new Menu("Mode");
		final ToggleGroup groupMode = new ToggleGroup();
		for (String modeName : MODES) {
			RadioMenuItem modeItem = new RadioMenuItem(modeName);
			if (modeName.equals(currentMode)) {
				modeItem.setSelected(true);
			} else {
				modeItem.setSelected(false);
			}
			modeItem.setUserData(modeName);
			modeItem.setToggleGroup(groupMode);
			moder.getItems().add(modeItem);
		}
		groupMode.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle new_toggle) {
				if (groupMode.getSelectedToggle() != null) {
					String newMode = (String) groupMode.getSelectedToggle().getUserData();
					switchMode(newMode);
				}
			}
		});
		return moder;
	}
	
	/**
	 * Switches the application from the current mode to a different mode.
	 * @param newMode - the name of the application's new mode.
	 */
	private void switchMode(String newMode) {
		if (!currentMode.equals(newMode)) {
			String inputText = _input.getText();
			makeEditorPane(newMode, sizeScale);
			if (newMode.equals(MODES[0])) {
				try {
					vm.loadGlobals(ScreenGrid.getGraphicsConstants(), true);
					defaultMessage = "function main()\n\t";
					vm.loadROs(ScreenGrid.getGraphicsConstants().keySet());
				} catch (MemoryException e) {
					ec.reportError("Problem in switching mode: " + e.toString(), 0, null);
				}
				symOrStack = true;
			} else if (newMode.equals(MODES[1])) {
				Map<String, Integer> forRisc = regFile.makeRiscRegs();
				try {
					vm.loadGlobals(forRisc, false);
					defaultMessage = "main:\n\t";
					vm.loadROs(RiscRegisters.READ_ONLY);
				} catch (MemoryException e) {
					ec.reportError("Problem in switching mode: " + e.toString(), 0, null);
				}
				symOrStack = !symOrStack;
			} else {
				ec.printPrompt("Mode does not exist: " + newMode);
				//Shouldn't ever reach this point
			}
			clear();
			tb.setMode(newMode);
			gridView = new ScreenGrid(vm, ec, events);
			gfo.attachScreen(gridView);
			runner.loadVM(vm);
			memory.loadVM(vm, newMode);
			regFile.switchMode(vm, newMode);
			//this is just to toggle the reg/mem pane again so that switch doesn't change to memory when it should be registers
			memView = !memView;
			_window.setBottom(regMem(newMode, sizeScale));
			_window.setRight(symtabBox(sizeScale));
			currentMode = newMode;
			setText(_input, inputText);
		}
	}
	
	/**
	 * Helper method zooms the screen according to scale
	 * @param scale - the percentage by which to scale the screen
	 */
	private void zoomer(double scale) {
		double newFontSize = 13 * scale;
		for (Node n : scroller.lookupAll(".root")) {
			n.setStyle("-fx-font-size: " + newFontSize + "px;");
		}
		tb.resize(scale);
		gridView.resize(scale);
		regFile.resize(scale);
		memory.resize(scale);
		makeEditorPane(currentMode, scale);
		memView = !memView;
		_window.setBottom(regMem(currentMode, scale));
		symOrStack = !symOrStack;
		_window.setRight(symtabBox(scale));
		gridView.paintAll();
	}
	
	/**
	 * Creates the edit menu. Most of the menu options are already built into the 
	 * rich text editor, so the menu items are empty, but at least this way the user knows
	 * that they exist.
	 * @return the completed edit menu, populated with the appropriate options.
	 */
	private Menu createEditMenu() {
		Menu editMenu = new Menu("Edit");
		MenuItem undo = new MenuItem("Undo");
		undo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
		MenuItem redo = new MenuItem("Redo");
		redo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN));
		MenuItem cut = new MenuItem("Cut");
		cut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
		MenuItem copy = new MenuItem("Copy");
		copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
		MenuItem paste = new MenuItem("Paste");
		paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
		MenuItem del = new MenuItem("Cut line");
		del.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN));
		del.setOnAction(event -> {
			_input.selectLine();
			_input.cut();
		});
		MenuItem comment = new MenuItem("Comment selection");
		comment.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN));
		comment.setOnAction(event -> commentSelectedPars(_input.getSelection()));
		editMenu.getItems().addAll(undo, redo, cut, copy, paste, del, comment);
		return editMenu;
	}
	
	/**
	 * Comments out selected paragraphs, similar to Eclipse's Ctrl+'/'.
	 * The caret stays in the same spot that it was in before the comment toggle.
	 * @param sel - the index range of the current selection
	 */
	private void commentSelectedPars(IndexRange sel) {
		int anchor = sel.getStart();
		int endCaretPos = sel.getEnd();
		int currentCaretPos = anchor;
		Pair<Integer, Boolean> result;
		do {
			_input.displaceCaret(currentCaretPos);
			result = toggleComment(_input.getCurrentParagraph());
			currentCaretPos = result.first() + 1;
		} while (currentCaretPos < endCaretPos);
		int caretDiff = 0;
		if (result.second()) {
			caretDiff = 1;
		} else {
			caretDiff = -1;
		}
		_input.moveTo(endCaretPos + caretDiff);
	}
	
	/**
	 * Inserts a '#' at the start of the paragraph to comment out the line.
	 * If the current paragraph is already commented, removes the '#' to uncomment.
	 * Also moves the caret position to the end of the line.
	 * @param currentPar - the paragraph to be commented out
	 * @return the new position of the caret
	 */
	private Pair<Integer, Boolean> toggleComment(int currentPar) {
		String parText = _input.getText(currentPar);
		boolean parCommented;
		if (parText.charAt(0) == '#') {
			_input.deleteText(currentPar, 0, currentPar, 1);
			parCommented = false;
		} else {
			_input.insertText(currentPar, 0, "#");
			parCommented = true;
		}
		int parLen = _input.getParagraphLength(currentPar);
		_input.moveTo(currentPar, parLen);
		return new Pair<Integer, Boolean>(_input.getCaretPosition(), parCommented);
	}
	
//----------------------------------------------------------------------------------------

	// SECTION: Save and Load

	/**
	 * Turns String content into File to be saved on the system.
	 * @param content - the content going into the file
	 * @param file - the file being written to
	 */
	private void SaveFile(String content, File file){
		try (FileWriter fileWriter = new FileWriter(file)){
			fileWriter.write(content);
		} catch (IOException ex) {
			Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Creates and writes to new save file using FileChooser
	 * @param source - the content being written to the new file
	 * @param stage - the window where the FileChooser will be displayed
	 */
	private void saveFileAs(String source, Stage stage) {
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		fileChooser.getExtensionFilters().add(extFilter);

//		if (!workingDirectory.exists()) {
//			workingDirectory.mkdirs();
//		}
//		fileChooser.setInitialDirectory(workingDirectory);

		//Show save file dialog
		workingFile = fileChooser.showSaveDialog(stage);
		if (workingFile != null) {
			SaveFile(source, workingFile);
			stage.setTitle("RISC-y Business (" + currentMode + "): " + workingFile.getName() + " - Saved");
			savedRecently = true;
		}
	}

	/**
	 * Reads file and creates String representation of its contents, to be displayed in _input pane.
	 * @param file - the file being loaded
	 * @return String version of the file contents
	 */
	private String readFile(File file){
		StringBuilder stringBuffer = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))){
			String text;
			while ((text = bufferedReader.readLine()) != null) {
				stringBuffer.append(text + "\n");
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		return stringBuffer.toString();
	}
	
//--------------------- SECTION: Assemble and Disassemble ---------------------------------------------------------
	
	/**
	 * Assembles the text displayed in the _input pane, then displays the assembled code in the correct panes.
	 */
	private void assemble() {
		symTab.clear();
		assembled = false;
		resetProgram();
		//Assembly: source string is tokenized and then turned into abstract instruction list
		ParseOutput pair;
		ParseInterface parser;
		try {
			if (currentMode.equals(MODES[0])) {
				parser = new YaalParse();
			} else { //if (currentMode.equals(MODES[1])) { ...
				parser = new RiscvParse();
			}
			pair = parser.parseSource(_input.getText());
			List<intermediateRepresentation> instructions = new ArrayList<intermediateRepresentation>();
			sourceLines = new ArrayList<Integer>();
			for (Pair<intermediateRepresentation, Integer> p : pair.getInstructions().getList()) {
				instructions.add(p.first());
				sourceLines.add(p.second());
			}
			regFile.loadLineList(sourceLines);

			parse.SymbolTable st = pair.getSymbolTable();
			symTab.updateTable(st);

			//Abstract instruction list is then turned into string to display, and assembled into machine code
			setText(_middle, absToString(instructions));
			if (currentMode.equals(MODES[1])) {
				setText(_output, abstractAssembleRiscV(instructions, pair.getData()));
			}
			_input.setStyleSpans(0, HighlightedInput.computeHighlighting(_input.getText()));

			//instructions and symbol table are then loaded into virtual machine and execution, in order to run the program. 
			vm.loadInstructions(instructions);
			vm.loadSymbolTable(st);
			//Now that assembly is complete, housekeeping
			assembled = true;
			
			tb.setStatus("assembled");
			
			if (currentMode.equals(MODES[0])) {
				vm.loadGlobals(ScreenGrid.getGraphicsConstants(), true);
				vm.loadROs(ScreenGrid.getGraphicsConstants().keySet());
				regFile.updateDisplay(); //after assembly, the registers will show up, so will need to update.
			} else if (currentMode.equals(MODES[1])) {
				vm.loadData(pair.getData());
				vm.loadGlobals(regFile.makeRiscRegs(), false);
				regFile.updateDisplay();
				vm.loadROs(RiscRegisters.READ_ONLY);
			}
		} catch (ParseException e) {
			ec.reportError(e.toString(), e.getLineNumber(), line -> highlightLine(line));
		} catch (MemoryException e) {
			ec.reportError("Problem loading globals: " + e.toString(), 0, null);
		} catch (Exception e) {
			ec.reportError("Unknown error in assembly: " + e.toString(), 0, null);
			e.printStackTrace();
		}
		ec.logErrors("Assembly");
	}

	/**
	 * Disassembles the code in the _output pane. Since the _output is only visible 
	 * in Risc-V mode, this function only works for Risc-V mode.
	 */
	private void disassembleRiscV() {
		//Disassembly: the machine code source is translated from binary to decimal, 
		//then passed into parser and turned into abstract instructions.
		//TODO: static reference?
		String source = _output.getText();
		MachineToAbs abs = new MachineToAbs();
		String decMachine = changeToDec(source);
		if (decMachine != null) {
			try {
				ArrayList<intermediateRepresentation> instructions = abs.machineToAbs(decMachine);

				//the abstract instructions are turned into assembly language and prepared for display
				setText(_middle, absToString(instructions));
				setText(_input, abstractDisassembleRiscV(instructions));
				
				//instructions and symbol table are then loaded into virtual machine and execution for running the program. 
				vm.loadInstructions(instructions);
				vm.loadSymbolTable(null);
				//Now that disassembly is complete, housekeeping.
				tb.setStatus("assembled");
			} catch (ParseException e) {
				ec.reportError(e.toString(), e.getLineNumber(), l -> highlightLine(l));
			}
		}
		ec.logErrors("Disassembly");
	}
	
	/**
	 * Turns intermediate code into a String representation for display, complete with line numbers.
	 * @param instructions - the list of intermediate instructions to be displayed 
	 * @return the String version of the intermediate code
	 */
	private String absToString(List<intermediateRepresentation> instructions) {
		String s = "";
		int i = 0;
		for (intermediateRepresentation abIn : instructions) {
			i++;
			String line = String.format("%2s", i);
			line += ": " + abIn.toString() + "\n";
			s += line;
		}
		return s;
	}
	
	/**
	 * Turns the intermediate code into machine code for displaying in the _output pane.
	 * @param instructions - the list of intermediate code to fully disassemble
	 * @return the String version of the machine code to display.
	 * @throws ParseException if the instructions have invalid syntax
	 */
	private String abstractAssembleRiscV(List<intermediateRepresentation> instructions, Data data) throws ParseException {
		ArrayList<Integer> machine = AbsToMachine.parseAbstracts(instructions, symTab.getParseTable(), data);
		String s = "";
		for (int cog : machine) {
			String b = String.format("%32s", Integer.toBinaryString(cog)).replace(' ', '0');
			s+= b + "\n";
		}
		return s;
	}
	
	/**
	 * Reverse-assembles Risc-V language from intermediate code. 
	 * @param absIns - the list of intermediate code to turn into Risc-V language.
	 * @return the String version of the Risc-V language.
	 */
	//Note that this is only really important if we care about disassembling in RISC. 
	private String abstractDisassembleRiscV(ArrayList<intermediateRepresentation> absIns) {
		//TODO: static reference?
			ArrayList<String> assembled = (new UnparseAbsInstr()).unparseMain(absIns);
			return String.join("", assembled);
	}
	
//----------------------- SECTION: Number Conversions ---------------------------------------------------------
	
	/**
	 * Takes a string of numbers and converts each line to a hexadecimal representation of those numbers, separated by line.
	 * @param source - the string to be converted to hexadecimal
	 * @return the string of hexadecimal numbers to display
	 */
	private String changeToHex(String source) {
		int errors = 0;
		String fin = "";
		int i = 0;
		if (source != null && !source.equals("")) {
			String[] t = source.split("\n");
			for (String line : t) {
				String bitLine = "0x";
				try {
					int dec = Integer.parseUnsignedInt(line, 2);
					bitLine += String.format("%08x", dec);
				} catch (NumberFormatException e) {
					ec.reportError("invalid binary value: " + line, i, l -> highlightMachine(l));
					errors++;
					fin = source;
					break;
				}				
				fin += (bitLine + "\n");
				i++;
			}
		} else {
			ec.reportError("no code to convert", 0, null);
			errors++;
		}
		if (errors == 0) {
			hexOrBin = !hexOrBin;
		}
		return fin;
	}
	
	/**
	 * Takes a string of numbers and converts it to a string of binary numbers, separated by line.
	 * @param source - the string of numbers to convert
	 * @return the new binary numbers to display
	 */
	private String changeToBin(String source) {
		int errors = 0;
		String fin = "";
		int l = 0;
		if (source != null && !source.equals("")) {
			String[] t = source.split("\n");
			for (String line : t) {
				line = line.substring(2); //cuts off the "0x" at the beginning
				String bin = "";
				for (int i = 0; i < line.length(); i++) {
					//parses the hex one digit at a time, since one hex digit is 4 binary digits
					String b = String.valueOf(line.charAt(i)); 
					try {
						int num = Integer.parseUnsignedInt(b, 16);
						b = Integer.toBinaryString(num);
					} catch (NumberFormatException e) {
						ec.reportError("invalid hexadecimal value: " + line, l, f -> highlightMachine(f));
						errors++;
						fin = source;
						break;
					}
					while (b.length() < 4) {
						b = "0" + b;
					}
					bin += b;
				}
				fin += (bin + "\n");
				l++;
			}
		} else {
			ec.reportError("no code to convert", 0, null);
			errors++;
		}
		if (errors == 0) {
			hexOrBin = !hexOrBin;
		}
		return fin;
	}

	/**
	 * Converts a string of numbers to decimal representation, to be used for disassembly.
	 * @param source - the string to be converted
	 * @return the string of decimal numbers to disassemble.
	 */
	private String changeToDec(String source) {
		String s = "";
		String[] t = source.split("\n");
		int i = 0;
		for (String line : t) {
			int num;
			try {
				if (hexOrBin) {
					line = line.substring(2);
					num = Integer.parseUnsignedInt(line, 16);
				} else {
					num = Integer.parseUnsignedInt(line, 2);
				}
				s += (num + "\n");
			} catch (NumberFormatException e) {
				ec.reportError("invalid machine code: " + line, i, l -> highlightMachine(l));
				return null;
			}
			i++;
		}
		return s;
	}
	
//---------------- SECTION: Memory and Registers ---------------------------------------------------------------
	
	/**
	 * Creates the pane to display the memory, alongside the memory navigation buttons.
	 * This version of the display shows memory at word-aligned, byte-addressed locations.
	 * @param scale - the percentage scale of the display
	 * @return the box to be displayed, showing GridPane with memory contents and VBox with navigation controls. 
	 */
	private HBox memoryPane(double scale) {
		HBox box = new HBox();
		box.setSpacing(10 * scale);
		GridPane mem = new GridPane();
		double regularPadding = 10 * scale;
		mem.setPadding(new Insets(regularPadding, regularPadding, (5 * scale), regularPadding));
		mem.setGridLinesVisible(true);
		for (int i = 0; i < 4; i++) {
			mem.add(new Text(" address"), (i*5), 0);
			mem.add(new Text(" +3"), i*5+1, 0);
			mem.add(new Text(" +2"), i*5+2, 0);
			mem.add(new Text(" +1"), i*5+3, 0);
			mem.add(new Text(" +0"), i*5+4, 0);
		}
		int addIndex = 0;
		for (int r = 1; r < 6; r++) {
			int i = 0;
			for (int c = 0; c < 20; c++) {
				if (c % 5 == 0) {
					Text display = memory.getAddDisplay(addIndex + (i*5));
					GridPane.setHalignment(display, HPos.CENTER);
					mem.add(display, c, r);
				} else {
					TextField t = memory.getValDisplays(addIndex + (i*5)).get((c%5) - 1);
					mem.add(t, c, r);
					if (c%5 == 4) {
						i++;
					}
				}
			}
			addIndex++;
		}
		mem.getRowConstraints().add(new RowConstraints(20));
		box.getChildren().addAll(mem, memButtons(scale));
		return box;
	}
	
	/**
	 * Creates the memory display, with navigation controls. This version displays memory at integer addresses.
	 * @param scale - the percentage scale of the memory display
	 * @return the box to be displayed, holding GridPane with memory contents and VBox with navigation controls.
	 */
	private HBox simpleMemDisplay(double scale) {
		HBox box = new HBox();
		box.setSpacing(10 * scale);
		GridPane grid = new GridPane();
		double regularPadding = 10 * scale;
		grid.setPadding(new Insets(regularPadding, regularPadding, (5 * scale), regularPadding));
		grid.setGridLinesVisible(true);
		int addressIndex = 0;
		int valueIndex = 0;
		for (int c = 0; c < 10; c++) {
			for (int r = 0; r < 4; r++) {
				if ((c%2) == 0) {
					Text display = memory.getAddDisplay(addressIndex);
					GridPane.setHalignment(display, HPos.CENTER);
					grid.add(display, c, r);
					addressIndex++;
				} else {
					TextField valueDisplay = memory.getValDisplays(valueIndex).get(0);
					grid.add(valueDisplay, c, r);
					valueIndex++;
				}
			}
		}
		box.getChildren().addAll(grid, memButtons(scale));
		return box;
	}
	
	/**
	 * Creates and sets the buttons to navigate the memory display.
	 * @param scale - the percentage size of the screen display
	 * @return the box holding the various controls associated with memory display
	 */
	private VBox memButtons(double scale) {
		VBox box = new VBox();
		double hScale = 12 * scale;
		double wScale = 15 * scale;
		box.setPadding(new Insets(hScale, wScale, (hScale - 10), wScale));
		box.setSpacing(10 * scale);
		Label buttons = new Label("Scroll Memory");
		double fsize = label_font_size * scale;
		buttons.setStyle("-fx-font-size: " + fsize + "px;");
		Text pageNum = new Text("Page: "+(memory.getDisplayIndex() / MemoryGraphics.DISPLAY_RANGE));
		double smallSize = small_font_size * scale;
		pageNum.setStyle("-fx-font-size: " + smallSize + "px;");
		HBox buttBox = new HBox();
		buttBox.setSpacing(10 * scale);
		int memPageRange = MemoryGraphics.DISPLAY_RANGE * 4;
		Button left = new Button("<---");
		left.setOnAction(event -> {
				int currentDisplay = memory.getDisplayIndex();
				if (currentDisplay < memPageRange) {
					memory.display(0);
				} else {
					memory.display(memPageRange * -1);
				}
				pageNum.setText("Page: "+(memory.getDisplayIndex() / MemoryGraphics.DISPLAY_RANGE));
		});
		Button right = new Button("--->");
		int maxRight = (int) MemoryGraphics.MEMORY_RANGE - memPageRange;
		right.setOnAction(event -> {
				int currentDisplay = memory.getDisplayIndex();
				if (currentDisplay > maxRight) {
					memory.display(maxRight);
				} else {
					memory.display(memPageRange);
				}
				pageNum.setText("Page: "+(memory.getDisplayIndex() / MemoryGraphics.DISPLAY_RANGE));
			});
		buttBox.getChildren().addAll(left, right);
		TextField goTo = new TextField();
		goTo.setPromptText("Go to address: ");
		goTo.setOnKeyPressed(event -> {
				if (event.getCode().toString().equals("ENTER")) {
					String goAddress = goTo.getText();
					int addressInt = memory.getDisplayIndex();
					try {
						if (currentMode.equals(MODES[0])) {
							addressInt = Integer.parseInt(goAddress);
							addressInt = addressInt * 4;
						} else {
							addressInt = Integer.parseInt(goAddress, memory.radix);
						}
					} catch (NumberFormatException e) {
						ec.reportError("invalid memory address: ", 0, null);
					}
					if (addressInt >= 0 && addressInt < MemoryGraphics.MEMORY_RANGE) {
						memory.go(addressInt);
						pageNum.setText("Page: "+(memory.getDisplayIndex() / MemoryGraphics.DISPLAY_RANGE));
					}
					goTo.clear();
				}
			});
		box.getChildren().addAll(buttons, buttBox, goTo, pageNum);
		return box;
	}
	
	/**
	 * Creates the pane to display the register file. This version has only 32 registers.
	 * @return the GridPane that displays the register contents.
	 */
	private HBox regfile(double scale) {
		HBox box = new HBox();
		GridPane rf = regFile.getRegGrid();
		VBox labelBox = new VBox();
		double hScale = 10 * scale;
		double wScale = 15 * scale;
		labelBox.setPadding(new Insets(hScale, wScale, hScale, wScale));
		Label regLabel = new Label("   Registers");
		labelBox.getChildren().addAll(regLabel, rf);
		box.getChildren().add(labelBox);
		return box;
	}
	
	/**
	 * Creates the display for the register contents, including navigation controls. This version of the register
	 * display shows a potentially unlimited number of registers.
	 * @param scale - the percentage scale that the screen will be displayed
	 * @return the pane containing the GridPane for register contents and VBox for navigation controls.
	 */
	private HBox simpleRegFile(double scale) {
		HBox box = new HBox();
		GridPane globals = regFile.getRegGrid();
		VBox buttonBoxG = new VBox();
		double heightScale = 10 * scale;
		double widthScale = 12 * scale;
		buttonBoxG.setPadding(new Insets(heightScale, widthScale, (heightScale + 10), widthScale));
		buttonBoxG.setSpacing(5 * scale);
		Label gLabel = new Label("Global");
		int simpleDisplayRange = 16;
		Text pageNum = new Text("Page: "+regFile.globalDisplayIndex/simpleDisplayRange);
		double smallSize = small_font_size * scale;
		pageNum.setStyle("-fx-font-size: " + smallSize + "px;");
		Button leftG = new Button("<--");
		leftG.setOnAction(event -> {
			regFile.scrollGlobal(simpleDisplayRange*(-1));
			pageNum.setText("Page: " + (regFile.globalDisplayIndex/simpleDisplayRange));
		});
		Button rightG = new Button("-->");
		rightG.setOnAction(event -> { 
			regFile.scrollGlobal(simpleDisplayRange);
			pageNum.setText("Page: " + (regFile.globalDisplayIndex/simpleDisplayRange));
		});
		regFile.setGlobalButtons(leftG, rightG);
		buttonBoxG.getChildren().addAll(gLabel, leftG, rightG, pageNum);
		HBox.setHgrow(buttonBoxG, Priority.ALWAYS);
		GridPane locals = regFile.getTempVarGrid();
		VBox buttonBoxL = new VBox();
		buttonBoxL.setPadding(new Insets(heightScale, (widthScale + 20), (heightScale + 10), widthScale));
		buttonBoxL.setSpacing(5 * scale);
		Label lLabel = new Label("Local");
		Text localPage = new Text("Page: "+regFile.localDisplayIndex/simpleDisplayRange);
		localPage.setStyle("-fx-font-size: " + smallSize + "px;");
		Button leftL = new Button("<--");
		leftL.setOnAction(event -> {
			regFile.scrollTemp(simpleDisplayRange*(-1));
			localPage.setText("Page: "+regFile.localDisplayIndex/simpleDisplayRange);
		});
		Button rightL = new Button("-->");
		rightL.setOnAction(event -> {
			regFile.scrollTemp(simpleDisplayRange);
			localPage.setText("Page: "+regFile.localDisplayIndex/simpleDisplayRange);
		});
		regFile.setLocalButtons(leftL, rightL);
		buttonBoxL.getChildren().addAll(lLabel, leftL, rightL, localPage);
		box.getChildren().addAll(globals, buttonBoxG, locals, buttonBoxL);
		return box;
	}
	
	/**
	 * Creates the outer pane that contains either registers or memory, and the button used to switch between views. 
	 * @param mode - the current mode that the application is in (to display either regular or 'simple' versions)
	 * @param scale - the scale that things should be zoomed
	 * @return the pane to display register and memory contents.
	 */
	private StackPane regMem(String mode, double scale) {
		StackPane stack = new StackPane();
		if (memView) {
			if (mode.equals(MODES[0])) {
				stack.getChildren().add(simpleRegFile(scale));
			} else {
				stack.getChildren().add(regfile(scale));
			}
			switchMemReg.setText("M");
		} else {
			if (mode.equals(MODES[0])) {
				stack.getChildren().add(simpleMemDisplay(scale));
			} else {
				stack.getChildren().add(memoryPane(scale));
			}
			switchMemReg.setText("R");
		}
		memView = !memView;
		stack.getChildren().add(switchMemReg);
		return stack;
	}
	
	/**
	 * Public method wraps around updating memory and register displays
	 * so that it can be passed into animators, rather than passing in the memory/register objects.
	 * @param zero - dummy parameter doesn't do anything, but helps with lambda format.
	 */
	private void updateGraphicsDisplays(int zero) {
		regFile.updateDisplay();
		memory.display(0);
	}
	
//-------------------- SECTION: Symbol Table ----------------------------------------------------------------------
	
	/**
	 * Creates the pane to display the symbol table, the program counter, and the breakpoint setter.
	 * @param scale - the percentage scale of the screen display
	 * @return the pane displaying symbol table, etc.
	 */
	private VBox symtabBox(double scale) {
		changeSymPane(scale);
		VBox stbox = new VBox();
		double heightScale = 12 * scale;
		double widthScale = 15 * scale;
		stbox.setPadding(new Insets(heightScale, widthScale, heightScale, widthScale));
		stbox.setStyle("-fx-background-color: #f9caf2");
		stbox.setSpacing(10 * scale);
		stbox.setFillWidth(true);
		stbox.setPrefHeight(450 * scale);
		Label prc = new Label("Program Counter");
		double fsize = label_font_size * scale;
		prc.setStyle("-fx-font-size: " + fsize + "px;");
		TextField pc = regFile.getPCView();
		Label inputLabel = new Label("User Input:");
		HBox inputBox = new HBox();
		TextField inputField = new TextField();
		Button submit = new Button("Submit");
		inputField.setDisable(true);
		submit.setDisable(true);
		gfo.loadInputBox(inputField, submit);
		inputBox.getChildren().addAll(inputField, submit);
		stbox.getChildren().addAll(prc, pc);
		stbox.getChildren().addAll(symTabStack);
		stbox.getChildren().addAll(inputLabel, inputBox);
		return stbox;
	}
	
	/**
	 * Initializes the click handler associated with the symbol table
	 * @return - the handler that will be attached to the table cells.
	 */
	private EventHandler<MouseEvent> makeTableClickHandler() {
		return new EventHandler<MouseEvent>() {
			public void handle(MouseEvent t) {
				@SuppressWarnings("unchecked")
				TableCell<SymbolTable.SymbolEntry, String> targetCell = 
						(TableCell<SymbolTable.SymbolEntry, String>)t.getSource();
				int index = targetCell.getIndex();
				if (index < symTab._table.getItems().size()) {
					SymbolTable.SymbolEntry entry = symTab._table.getItems().get(index);
					int sourceLine = entry.getLine() - 1;
					int codeline = entry.codeline;
					symbolHighlight(sourceLine, codeline);
				}
			}
		};
	}
	
	/**
	 * Toggles the window's right pane between the symbol table and the stack pointer. 
	 * If the application is in YAAL mode, the stack pointer is not visible, thus
	 * the pane does not change.
	 * @param scale - the scale to use for sizing the various elements of the pane.
	 */
	private void changeSymPane(double scale) {
		symTabStack.getChildren().clear();
		VBox baseBox = new VBox();
		baseBox.setSpacing(10 * scale);
		if (symOrStack || currentMode.equals(MODES[0])) {
			Label title = new Label("Symbol Table");
			double fsize = label_font_size * scale;
			title.setStyle("-fx-font-size: " + fsize + "px;");
			baseBox.getChildren().addAll(title, symTab._table);
			switchSymStack.setText("SP");
		} else {
			Label title = new Label("Stack Pointer");
			double fsize = label_font_size * scale;
			title.setStyle("-fx-font-size: " + fsize + "px;");
			baseBox.getChildren().addAll(title, regFile.spGraphic());
			switchSymStack.setText("ST");
		}
		symTabStack.getChildren().add(baseBox);
		if (!currentMode.equals(MODES[0])) {
			symTabStack.getChildren().add(switchSymStack);
		}
		symOrStack = !symOrStack;
	}
	
	/**
	 * Highlights the line associated with a certain symbol (to be used by the symbol table)
	 * @param sourceline - the line number as it appears in the _input pane.
	 * @param codeLine - the corresponding line number as it appears in the intermediate and machine code.
	 */
	private void symbolHighlight(int sourceline, int codeLine) {
		unhighlightAll();
		if (currentMode.equals(MODES[1])) {
			highlightMachine(codeLine);
		}
		highlightAbstract(codeLine);
		_input.replaceText(sourceline, 0, sourceline, 0, "");
		_input.requestFollowCaret();
		_input.setStyle(sourceline, Collections.singleton("highlight-text"));
	}
	
//-------------------------- SECTION: RichText Appearance ----------------------------------------------
	
	/**
	 * Highlights a line of code and the corresponding lines of machine code and intermediate code.
	 * @param line - the line number in the intermediate code that is to be highlighted
	 * 		Note - if the code has not been assembled into intermediate code, the parameter line number should be
	 * 			   the source line number as it appears in the _input pane.
	 */
	public void highlightLine(int line) {
		unhighlightAll();
		if (assembled && line < sourceLines.size()) {
			if (!currentMode.equals(MODES[0])) {
				highlightMachine(line);
			}
			highlightAbstract(line);
			int sourceLine = sourceLines.get(line);
			_input.replaceText(sourceLine, 0, sourceLine, 0, "");
			_input.requestFollowCaret();
			_input.setStyle(sourceLine, Collections.singleton("highlight-text"));
		} else {
			_input.replaceText(line, 0, line, 0, "");
			_input.requestFollowCaret();
			_input.setStyle(line, Collections.singleton("highlight-text"));
		}
	}
	
	/**
	 * Highlights a line of machine code
	 * @param line - the paragraph to be highlighted
	 */
	private void highlightMachine(int line) {
		_output.replaceText(line, 0, line, 0, "");
		_output.requestFollowCaret();
		_output.setStyle(line, Collections.singleton("highlight-text"));
	}
	
	/**
	 * Highlights a single line of intermediate code.
	 * @param line - the paragraph to be highlighted.
	 */
	private void highlightAbstract(int line) {
		if (abstractView) {
			_middle.replaceText(line, 0, line, 0, "");
			_middle.requestFollowCaret();
			_middle.setStyle(line, Collections.singleton("highlight-text"));
		}
	}
	
	/**
	 * Removes the highlighting affect from all panes.
	 */
	public void unhighlightAll() {
		int p = _input.getParagraphs().size();
		for (int i = 0; i < p; i++) {
			_input.setStyle(i, Collections.singleton("unhighlight-text"));
		}
		int o = _output.getParagraphs().size();
		for (int i = 0; i < o; i++) {
			_output.setStyle(i, Collections.singleton("unhighlight-text"));
		}
		int a = _middle.getParagraphs().size();
		for (int i = 0; i < a; i++) {
			_middle.setStyle(i, Collections.singleton("unhighlight-abstract"));
		}
	}
	
	/**
	 * Convenience method used to quickly reset the text of a specified CodeArea.
	 * @param area - the area being affected.
	 * @param text - the text to place in said area.
	 */
	private void setText(CodeArea area, String text) {
		area.clear();
		area.replaceText(0, 0, text);
		updateColumnCounter();
	}
	
	/**
	 * Updates the caret position counter at the bottom of the editor pane
	 */
	private void updateColumnCounter() {
		int col = _input.getCaretColumn();
		int line = _input.getCurrentParagraph() + 1;
		columnCounter.setText(" " + line + ":" + col);
	}
	
//----------------------- SECTION: Program Execution --------------------------------------
	
	/**
	 * Starts the runner according to whether the runner should be animated or not.
	 */
	public void startProgram() {
		if (assembled) {
			programRunning = true;
			tb.setStatus("running");
			ec.clear();
			if (animated) {
				runner.startAnimator();
			} else {
				runner.startRunner();
			}
		} else {
			ec.printPrompt("program not assembled");
		}
	}
	
	/**
	 * Stops both animators without stopping the virtual machine
	 */
	public void pauseProgram() {
		programRunning = false;
		tb.setStatus("paused");
		runner.stopRunning();
	}
	
	/**
	 * Resets the Virtual Machine so that it is fresh and ready to execute the assembled program.
	 */
	public void resetProgram() {
		stopProgram();
		vm.resetALL();
		regFile.reset();
		memory.clear();
		events.clearEvents();
		if (currentMode.equals(MODES[0])) {
			gridView.clearGrid();
		}
		ec.clear();
		if (assembled) {
			tb.setStatus("assembled");
		} else {
			tb.setStatus("not assembled");
		}
	}
	
	/**
	 * Stops the animators as well as the virtual machine.
	 * Program must be reset in order to start again. 
	 */
	public void stopProgram() {
		runner.stopRunning();
		vm.stop();
		programRunning = false;
		unhighlightAll();
		_input.setStyleSpans(0, HighlightedInput.computeHighlighting(_input.getText()));
	}
	
	/**
	 * Moves the program one step forward and animates the affected changes - the register/memory location that has changed
	 * and the line of code that was just executed.
	 */
	private void stepProgram() {
		if (assembled && vm.running()) {
			if (!events.keysEmpty()) {
				int keyVal = events.getNextKey();
				vm.logKeyEvent(keyVal);
			}
			if (!events.clicksEmpty()) {
				int clickPos = events.getNextClick();
				vm.logClickEvent(clickPos);
			}
			try {
				vm.tick();
			} catch (OperationException | MemoryException e) {
				String eMessage = e.toString() + ": " + e.getMessage() + " execution";
				ec.reportError(eMessage, vm.getPC(), line -> highlightLine(line));
			}
			highlightLine(vm.getPC());
			if (vm.memoryChange() != null) {
				gridView.paintOne(vm.memoryChange());
			}
			updateGraphicsDisplays(0);
		} else if (assembled) {
			//assumes the program has finished, which is why vm.running() would return false.
			ec.logErrors("Program step");
			tb.setStatus("finished");
		} else {
			ec.printPrompt("program not assembled");
		}
	}
	
	/**
	 * Clears all fields and resets memory, registers, and the virtual machine.
	 */
	private void clear() {
		symTab.clear();
		resetProgram();
		setText(_input, defaultMessage);
		setText(_middle, "abstract");
		setText(_output, "output");
		_input.setEditable(true);
		hexOrBin = false;
	}
	
	/**
	 * Checks whether the program is running using local static boolean
	 * @return true if the animators are running
	 */
	static boolean isProgramRunning() {
		return programRunning;
	}
	
//---------------------------- SECTION: class: QuitBox -------------------------------------------
	/**
	 * Inner class handles the process of asking the user "Are you sure you want to quit?" upon exiting 
	 * the application, if changes have not been saved recently. 
	 * 
	 */
	private class QuitBox {
		boolean canQuit = true;
		QuitBox(Stage stage) {
			Stage miniStage = new Stage();
			miniStage.setTitle("Are you sure you want to quit?");
			miniStage.initOwner(stage.getOwner());
			miniStage.initModality(Modality.WINDOW_MODAL);
			VBox sceneBox = new VBox();
			Text message = new Text("You have unsaved changes. \nWould you like to save before you quit?");
			message.setFont(Font.font("Courier New", 14));
			sceneBox.setPadding(new Insets(25));
			sceneBox.getChildren().add(message);
			HBox buttonBox = new HBox();
			Button saver = new Button("Save");
			saver.setDefaultButton(true);
			saver.setOnAction(event -> {
				if (workingFile == null) {
					saveFileAs(_input.getText(), stage);
				} else {
					SaveFile(_input.getText(), workingFile);
				}
				canQuitTrue();
				miniStage.close();
			});
			Button noSave = new Button("Don't save");
			noSave.setOnAction(event -> {
				canQuitTrue();
				miniStage.close();
			});
			Button canceller = new Button("Cancel");
			canceller.setOnAction(event -> {
				canQuitFalse();
				miniStage.close();
			});
			buttonBox.setSpacing(10);
			buttonBox.setPadding(new Insets(10));
			buttonBox.getChildren().addAll(saver, noSave, canceller);
			buttonBox.setAlignment(Pos.BASELINE_RIGHT);
			sceneBox.getChildren().add(buttonBox);
			Scene theScene = new Scene(sceneBox);
			theScene.getStylesheets().add(MainTest.class.getResource("/resources/run-window.css").toExternalForm());
			miniStage.setScene(theScene);
			miniStage.setOnCloseRequest(event -> canQuitFalse());
			miniStage.showAndWait();
		}
		private void canQuitTrue() {
			canQuit = true;
		}
		private void canQuitFalse() {
			canQuit = false;
		}
		boolean getCanQuit() {
			return canQuit;
		}
	}
	
}
