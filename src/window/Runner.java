package window;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.Window;
import operation_syntax.OperationException;
import virtual_machine.ExecVM;
import virtual_machine.MemoryException;

public class Runner extends AnimationTimer {
	
	private ExecVM _vm;
	private static ErrorConsole _ec;
	private ScreenGrid screen;
	private ErrorConsole.Highlighter coloring;
	private EventLists _events;
	private MainToolBar _tb;
	private MemoryGraphics memory;
	private RegisterGraphics regFile;
	
	boolean animation = false; //Set true if the program should run with animation
	boolean reading = false; //Set true if the program is paused for reading input
	
	/* Booleans created to keep track of run preferences: */
	boolean checkMemChanged;
	boolean checkPcChanged;
	boolean checkRegsChanged;
	boolean checkCallOccurred;
	boolean checkRetOccurred;
	boolean checkEventOccurred;
	
	private int speed = 100; //speed is number of instructions that will occur each time the handle method is called
	// (number of instructions per 1/60th of a second).
	private int runSpeed = speed;
	
	private List<String> regsToWatch = new ArrayList<>();
	private List<Integer> memToWatch = new ArrayList<>();
	
	/**
	 * Constructor creates the Runner and passes in the elements necessary from the main class
	 * @param vm - the ExecVM that does the actual execution
	 * @param iof - the place where errors get reported from execution
	 * @param grid - the screen that gets painted in YAAL mode
	 * @param method - the highlighter to highlight error lines
	 * @param events - the lists where event input is held
	 * @param tb - the tool bar where the finishing stuff gets put
	 * @param rf - the graphical register file
	 * @param mem - the graphical memory system
	 */
	public Runner(ExecVM vm, ErrorConsole iof, ScreenGrid grid, ErrorConsole.Highlighter method,
			EventLists events, MainToolBar tb, RegisterGraphics rf, MemoryGraphics mem) {
		_vm = vm;
		_ec = iof;
		screen = grid;
		coloring = method;
		_events = events;
		_tb = tb;
		regFile = rf;
		memory = mem;
	}
	
	/**
	 * Attaches the new ExecVM to the class so that the runner doesn't have to be re-created upon mode switch
	 * @param vm - the new ExecVM being added to the runner.
	 */
	public void loadVM(ExecVM vm) {
		_vm = vm;
	}
	
	/**
	 * Sets the runner's default run preferences according to whether graphics are enabled.
	 * @param simpleMode - true if graphics/events enabled
	 */
	public void setDefaults(boolean simpleMode) {
		if (simpleMode) {
			checkMemChanged = true;
			checkPcChanged = false;
			checkRegsChanged = false;
			checkCallOccurred = true;
			checkRetOccurred = false;
			checkEventOccurred = true;
		} else {
			checkMemChanged = true;
			checkPcChanged = false;
			checkRegsChanged = true;
			checkCallOccurred = false;
			checkRetOccurred = true;
			checkEventOccurred = false;
		}
	}
	
	/**
	 * Starts the animation mode, which highlights lines and updates the display more often.
	 */
	public void startAnimator() {
		_events.clearEvents();
		animation = true;
		speed = 1;
		start();
	}
	
	/**
	 * Starts the regular run mode, which does not highlight and updates the display less often. 
	 */
	public void startRunner() {
		_events.clearEvents();
		animation = false;
		speed = runSpeed;
		start();
	}
	
	/**
	 * Function is called by AnimationTimer every 1/60th of a second when user calls start().
	 * This function handles the actual execution and deals with updating displays. 
	 * @param currentNano - current time in nanoseconds, passed in by whatever calls the handle method.
	 */
	@Override
	public void handle(long currentNano) {
		if (_vm.running()) {
			boolean stateChange = false;
			boolean eventocc = false;
			boolean rchange = false;
			boolean mchange = false;
			int loops = 0;
			do {
				loops++;
				try {
					if (!_events.keysEmpty()) {
						int keyVal = _events.getNextKey(); 
						_vm.logKeyEvent(keyVal);
					}
					if (!_events.clicksEmpty()) {
						int clickPos = _events.getNextClick();
						_vm.logClickEvent(clickPos);
					}
					if (animation) {
						coloring.colorLine(_vm.getPC());
					}
					_vm.tick();
					if (_vm.regChange() != null && _vm.regChange().equals("sp")) {
						boolean keepGoing = regFile.testSP();
						if (!keepGoing) {
							_ec.reportError("no more memory available - stack pointer and heap pointer have collided.", null, null);
							stopRunning();
						}
					}
					if (_vm.callOccur() != null && _vm.callOccur().equals("_update_graphics")) {
						break;
					}
					if ((checkMemChanged || !memToWatch.isEmpty()) && !stateChange) {
						stateChange = _vm.memoryChange() != null;
						mchange = true;
					}
					if (checkPcChanged && !stateChange) {
						stateChange = _vm.pcChange();
					}
					if ((checkRegsChanged || !regsToWatch.isEmpty()) && !stateChange) {
						stateChange = (_vm.regChange() != null);
						rchange = true;
					}
					if (checkCallOccurred && !stateChange) {
						stateChange = (_vm.callOccur() != null);
						rchange = true;
					}
					if (checkRetOccurred && !stateChange) {
						stateChange = _vm.returnOccur();
						rchange = true;
					}
					if (checkEventOccurred && !stateChange) {
						stateChange = (_vm.eventOccur() != null);
					}
				} catch (OperationException | MemoryException e) {
					_ec.reportError(e.toString(), _vm.getPC(), line -> coloring.colorLine(line));
					stopRunning();
				} catch (IllegalStateException y) {
					String eMessage = "Screen error: " + y.toString();
					_ec.reportError(eMessage, null, null);
				}
				if (loops == speed) {
					break;
				}
			} while (!reading && !stateChange && (!_events.breaksContain(_vm.getPC()) && _vm.running()));
			if (mchange) {
				memory.display(0);
				Integer changedMem = _vm.memoryChange();
				if (changedMem != null) {
					screen.paintOne(changedMem);
					if (memToWatch.contains(changedMem)) {
						memory.markWatcher(changedMem);
					}
				}
			}
			if (rchange) {
				regFile.updateDisplay();
				String changedReg = _vm.regChange();
				if (changedReg != null && regsToWatch.contains(changedReg)) {
					regFile.markWatcher(changedReg);
				}
			}
			if (eventocc) {
				//Left blank for future possibilities
			}
			if (_events.breaksContain(_vm.getPC())) {
				stop();
				_ec.logErrors("Breakpoint reached");
				_tb.setStatus("stopped");
				memory.display(0);
				regFile.updateDisplay();
			}
		} else {
			stopRunning(); //when it gets to the end of the program, stop running
			_tb.setStatus("finished");
		}
	}
	
	/**
	 * Convenience method gives us the ability to stop the program and log errors at the same time
	 */
	void stopRunning() {
		stop();
		_ec.logErrors("Program execution");
		_tb.setStatus("stopped");
		memory.display(0);
		regFile.updateDisplay();
	}
	
	/**
	 * Sets the flag that tells the runner's handle loop whether to execute
	 * @param read - use true if the VM is trying to read something, else use false.
	 */
	public void setReading(boolean read) {
		reading = read;
	}

	/**
	 * Creates the MenuItem to handle the run preference for memory.
	 * If the memory preference is on, the loop inside handle will stop
	 * and update graphics if memory has changed as of the most recent instruction execution.
	 * @return the CheckMenuItem that toggles the run preference option.
	 */
	public CheckMenuItem setRunPrefMem() {
		CheckMenuItem memPref = new CheckMenuItem("Update display when memory changes");
		memPref.setSelected(checkMemChanged);
	    memPref.selectedProperty().addListener(new ChangeListener<Boolean>() {
	        @SuppressWarnings("rawtypes")
			public void changed(ObservableValue ov,
	        Boolean old_val, Boolean new_val) {
	            checkMemChanged = new_val;
	        }
	    });
	    return memPref;
	}
	
	/**
	 * Creates the MenuItem to handle the run preference for registers.
	 * If this preference is on, the loop inside the handle method will stop
	 * and update graphics if one or more register values has changed in the most recent instruction.
	 * @return the CheckMenuItem that toggles the run preference option.
	 */
	public CheckMenuItem setRunPrefReg() {
		CheckMenuItem regPref = new CheckMenuItem("Update display when registers change");
		regPref.setSelected(checkRegsChanged);
	    regPref.selectedProperty().addListener(new ChangeListener<Boolean>() {
	        @SuppressWarnings("rawtypes")
			public void changed(ObservableValue ov,
	        Boolean old_val, Boolean new_val) {
	            checkRegsChanged = new_val;
	        }
	    });
	    return regPref;
	}
	
	/**
	 * Creates the MenuItem to handle the run preference for the PC.
	 * If this preference is on, the loop inside the handle method will stop
	 * and update graphics if the program counter has changed in the most recent instruction.
	 * In most cases, this will act the same way as the regular animator, but without highlighting lines.
	 * @return the CheckMenuItem that toggles the run preference option.
	 */
	public CheckMenuItem setRunPrefPC() {
		CheckMenuItem pcPref = new CheckMenuItem("Update display when program counter changes");
		pcPref.setSelected(checkPcChanged);
	    pcPref.selectedProperty().addListener(new ChangeListener<Boolean>() {
	        @SuppressWarnings("rawtypes")
			public void changed(ObservableValue ov,
	        Boolean old_val, Boolean new_val) {
	            checkPcChanged = new_val;
	        }
	    });
	    return pcPref;
	}
	
	/**
	 * Creates the MenuItem to handle the run preference for calls.
	 * If this preference is on, the loop inside the handle method will stop and 
	 * update graphics if the most recent instruction was a call.
	 * @return the CheckMenuItem that toggles the run preference option.
	 */
	public CheckMenuItem setRunPrefCall() {
		CheckMenuItem callPref = new CheckMenuItem("Update display when call occurs");
		callPref.setSelected(checkCallOccurred);
	    callPref.selectedProperty().addListener(new ChangeListener<Boolean>() {
	        @SuppressWarnings("rawtypes")
			public void changed(ObservableValue ov,
	        Boolean old_val, Boolean new_val) {
	            checkCallOccurred = new_val;
	        }
	    });
	    return callPref;
	}
	
	/**
	 * Creates the MenuItem to handle the run preference for returns.
	 * If this preference is on, the loop inside the handle method will stop and update 
	 * graphics if the most recent instruction was a return.
	 * @return the CheckMenuItem that toggles the run preference option.
	 */
	public CheckMenuItem setRunPrefRet() {
		CheckMenuItem retPref = new CheckMenuItem("Update display when return occurs");
		retPref.setSelected(checkRetOccurred);
	    retPref.selectedProperty().addListener(new ChangeListener<Boolean>() {
	        @SuppressWarnings("rawtypes")
			public void changed(ObservableValue ov,
	        Boolean old_val, Boolean new_val) {
	            checkRetOccurred = new_val;
	        }
	    });
	    return retPref;
	}
	
	/**
	 * Creates the MenuItem to handle the run preference for events.
	 * If this preference is on, the loop inside the handle method will stop
	 * and update graphics if an event has occurred, as of the most recently executed instruction.
	 * @return the CheckMenuItem that toggles the run preference option.
	 */
	public CheckMenuItem setRunPrefEvent() {
		CheckMenuItem eventPref = new CheckMenuItem("Update display when event occurs");
		eventPref.setSelected(checkEventOccurred);
	    eventPref.selectedProperty().addListener(new ChangeListener<Boolean>() {
	        @SuppressWarnings("rawtypes")
			public void changed(ObservableValue ov,
	        Boolean old_val, Boolean new_val) {
	            checkEventOccurred = new_val;
	        }
	    });
	    return eventPref;
	}
	
	/**
	 * Creates the MenuItem that manages the speed preference.
	 * @param mainStage - the stage used in constructing the box for user input
	 * @return the MenuItem that sets the speed preference.
	 */
	public MenuItem setRunPrefSpeed(Stage mainStage) {
		MenuItem speedPref = new MenuItem("Set runtime speed - current: " + runSpeed);
		speedPref.setOnAction(event -> {
			if (!MainTest.isProgramRunning()) {
				String prompt = "Run speed is the approximate number of\ninstructions that are executed per 1/60th "
					+ "\nof a second.\nCurrent speed: " + runSpeed + ";\nInput new speed: ";
				int newSpeed = readSetting(mainStage.getOwner(), prompt, speed, "speed");
				runSpeed = newSpeed;
				speedPref.setText("Set runtime speed - current: " + speed);
			}
		});
		return speedPref;
	}
	
	/**
	 * Creates a pop-up to get user input for setting a watchpoint for a register name.
	 * @param mainStage - the stage where the pop-up will show up
	 */
	public void setRegWatchPt(Stage mainStage) {
		if (!MainTest.isProgramRunning()) {
			String prompt = "If one or more register watchpoints are set,\nthe program will highlight "
					+ "\nwhenever those registers change.\n"
					+ "Current watchpoints: \n" + String.join("\n", regsToWatch);
			InputTextPrompt prompter = new InputTextPrompt(mainStage.getOwner(), prompt);
			String watchReg = prompter.getResult();
			regsToWatch.add(watchReg);
		}
	}
	
	/**
	 * Creates a pop-up to get user input for setting a watchpoint for a memory address.
	 * @param mainStage - the stage where the pop-up will show up
	 */
	public void setMemWatchPt(Stage mainStage) {
		if (!MainTest.isProgramRunning()) {
			List<String> tempMem = new ArrayList<String>();
			for (Integer mem : memToWatch) {
				tempMem.add(String.valueOf(mem));
			}
			String prompt = "If one or more memory watchpoints are set,\n the program will highlight \n"
					+ "whenever those memory addresses change."
					+ "Current watchpoints: \n" + String.join("\n", tempMem);
			InputTextPrompt prompter = new InputTextPrompt(mainStage.getOwner(), prompt);
			String watchMem = prompter.getResult();
			try {
				int memPoint = Integer.parseInt(watchMem);
				memToWatch.add(memPoint);
			} catch (NumberFormatException e) {
				_ec.reportError("Invalid memory address for watchpoint: " + watchMem, null, null);
			}
		}
	}
	
	/**
	 * Clears all the watchpoints.
	 */
	public void clearWatchers() {
		regsToWatch.clear();
		memToWatch.clear();
	}
	
	/**
	 * Private method creates a pop-up using the InputTextPrompt to read an integer setting
	 * @param owner - the window owner of the pop-up stage
	 * @param prompt - the prompt to tell user info about the setting
	 * @param currentVal - the current value of the setting (so that invalid input doesn't change anything)
	 * @param intendedSetting - the setting that is being changed
	 * @return the new value that the setting will have
	 */
	static int readSetting(Window owner, String prompt, int currentVal, String intendedSetting) {
		int newVal = currentVal;
		InputTextPrompt reader = new InputTextPrompt(owner, prompt);
		String result = reader.getResult();
		try {
			newVal = Integer.parseInt(result);
		} catch (NumberFormatException e) {
			_ec.reportError("Invalid input for " + intendedSetting + ": " + result, null, null);
		}
		return newVal;
	}
}
