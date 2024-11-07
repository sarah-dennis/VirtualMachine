package window;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

/**
 * Class handles the tool bar, to make things easier for MainTest. 
 *
 */
public class MainToolBar {
	private ToolBar tb = new ToolBar();
	
	private Button start = new Button();
	private Button pause = new Button();
	private Button step = new Button();
	private Button stop = new Button();
	private Button reset = new Button();
	private Button assemble = new Button();
	private Button disassemble = new Button();

	private Button[] execButtons = {start, step, pause, stop, reset};
	
	private Text programStatus = new Text("Input: not assembled");
	private final int textSize = 11;
	private final int imageSize = 15;
	private String mode = "";
	
	private Tooltip[] tips = { 
			new Tooltip("Play: Run the program"), new Tooltip("Step through one instruction"),
			new Tooltip("Pause program"), new Tooltip("Stop the program"), new Tooltip("Reset the program"),
			new Tooltip("Assemble (Ctrl+Shift+A)"), new Tooltip("Disassemble (Ctrl+Shift+D)")
			};
	
	/**
	 * Interface for using lambda methods to set button actions
	 */
	public interface ButtonSetter {
		public void buttonAction(ActionEvent clicked);
	}
	
	/**
	 * Constructor simply sets the images for each button
	 */
	public MainToolBar() {
		programStatus.setStyle("-fx-font-size: " + textSize + "px;");
		start.setGraphic(new ImageView(new Image("/resources/start.png", imageSize, imageSize, false, false)));
		pause.setGraphic(new ImageView(new Image("/resources/pause.png", imageSize, imageSize, false, false)));
		step.setGraphic(new ImageView(new Image("/resources/step.png", imageSize, imageSize, false, false)));
		stop.setGraphic(new ImageView(new Image("/resources/stop.png", imageSize, imageSize, false, false)));
		reset.setGraphic(new ImageView(new Image("/resources/reset.png", imageSize, imageSize, false, false)));
		assemble.setGraphic(new ImageView(new Image("/resources/assemble.png", imageSize, imageSize, false, false)));
		disassemble.setGraphic(new ImageView(new Image("/resources/disassemble.png", imageSize, imageSize, false, false)));
		//run goes before skip on the tool bar
		tb.getItems().addAll(start, step, pause, stop, reset,
				new Separator(), assemble, disassemble, new Separator(), programStatus);
	}
	
	/**
	 * Sets the action function and the Tooltip for the start button
	 * @param action - the lambda function attached to the start button
	 */
	void setStart(ButtonSetter action) {
		start.setOnAction(event -> action.buttonAction(event));
		start.setTooltip(tips[0]);
	}
	
	/**
	 * Sets the action function and the Tooltip for the pause button
	 * @param action - the lambda function attached to the pause button
	 */
	void setPause(ButtonSetter action) {
		pause.setOnAction(event -> action.buttonAction(event));
		pause.setTooltip(tips[2]);
	}
	
	/**
	 * Sets the action function and the Tooltip for the step button
	 * @param action - the lambda function attached to the step button
	 */
	void setStep(ButtonSetter action) {
		step.setOnAction(event -> action.buttonAction(event));
		step.setTooltip(tips[1]);
	}
	
	/**
	 * Sets the action function and the tool tip for the stop button
	 * @param action - the lambda function attached to the stop button
	 */
	void setStop(ButtonSetter action) {
		stop.setOnAction(event -> action.buttonAction(event));
		stop.setTooltip(tips[3]);
	}
	
	/**
	 * Sets the action function and tool tip for the reset button
	 * @param action - the lambda function associated with the reset button
	 */
	void setReset(ButtonSetter action) {
		reset.setOnAction(event -> action.buttonAction(event));
		reset.setTooltip(tips[4]);
	}
	
	/**
	 * Sets the action function and the tool tip for the assemble button
	 * @param action - the lambda function associated with the assemble button
	 */
	void setAssemble(ButtonSetter action) {
		assemble.setOnAction(event -> action.buttonAction(event));
		assemble.setTooltip(tips[5]);
	}
	
	/**
	 * Sets the action function and tool tip for the disassemble button
	 * @param action - the lambda function attached to the disassemble button
	 */
	void setDisassemble(ButtonSetter action) {
		disassemble.setOnAction(event -> action.buttonAction(event));
		disassemble.setTooltip(tips[6]);
	}
	
	/**
	 * Getter method for the tool bar
	 * @return the tool bar object
	 */
	ToolBar getToolBar() {
		return tb;
	}
	
	/**
	 * Disables the buttons for running the program so that user cannot run the program
	 * if it has not been assembled yet. Adjusts the tool tips accordingly. 
	 * @param isDisable - true if the buttons will be disabled, false if the buttons can be used.
	 */
	void disableExec(boolean isDisable) {
		int i = 0;
		while (i < execButtons.length) {
			Button b = execButtons[i];
			b.setDisable(isDisable);
			i++;
		}
	}
	
	/**
	 * Tells whether the execution buttons are disabled
	 * @return true if all execution buttons are disabled
	 */
	public boolean isDisabled() {
		return start.isDisabled();
	}
	
	/**
	 * Sets the status bar and enables/disables the buttons accordingly
	 * Note: be careful to follow the correct syntax: lowercase letters and spaces between words.
	 * @param status - the new status of the current program
	 */
	public void setStatus(String status) {
		programStatus.setText("Input: " + status);
		if (status.equals("not assembled")) {
			disableExec(true);
			assemble.setDisable(false);
			disassemble.setDisable(false);
		} else if (status.equals("assembled")) {
			disableExec(false);
			pause.setDisable(true);
			stop.setDisable(true);
			assemble.setDisable(false);
			disassemble.setDisable(false);
		} else if (status.equals("running")) {
			start.setDisable(true);
			step.setDisable(true);
			pause.setDisable(false);
			stop.setDisable(false);
			reset.setDisable(true);
			assemble.setDisable(true);
			disassemble.setDisable(true);
		} else if (status.equals("paused") || status.equals("stopped") || status.equals("break")) {
			start.setDisable(false);
			step.setDisable(false);
			pause.setDisable(true);
			stop.setDisable(true);
			reset.setDisable(false);
			assemble.setDisable(false);
			disassemble.setDisable(false);
		} else if (status.equals("finished")) {
			start.setDisable(true);
			step.setDisable(true);
			pause.setDisable(false);
			stop.setDisable(false);
			reset.setDisable(false);
			assemble.setDisable(false);
			disassemble.setDisable(false);
		} else {
			programStatus.setText("status not supported");
		}
		if (mode.equals("yaal")) {
			disassemble.setDisable(true);
		}
		
	}
	
	/**
	 * Resizes the font and the buttons
	 * @param scale - the factor by which to resize the font and images.
	 */
	public void resize(double scale) {
		double newFontSize = textSize * scale;
		double newImageSize = imageSize * scale;
		programStatus.setStyle("-fx-font-size: " + newFontSize + "px;");
		start.setGraphic(new ImageView(new Image("/resources/start.png", newImageSize, newImageSize, false, false)));
		pause.setGraphic(new ImageView(new Image("/resources/pause.png", newImageSize, newImageSize, false, false)));
		step.setGraphic(new ImageView(new Image("/resources/step.png", newImageSize, newImageSize, false, false)));
		stop.setGraphic(new ImageView(new Image("/resources/stop.png", newImageSize, newImageSize, false, false)));
		reset.setGraphic(new ImageView(new Image("/resources/reset.png", newImageSize, newImageSize, false, false)));
		assemble.setGraphic(new ImageView(new Image("/resources/assemble.png", newImageSize, newImageSize, false, false)));
		disassemble.setGraphic(new ImageView(new Image("/resources/disassemble.png", newImageSize, newImageSize, false, false)));
	}
	
	/**
	 * Sets the mode so that if we're in YAAL the disassemble button is always disabled.
	 * @param modeName - the name of the new mode
	 */
	public void setMode(String modeName) {
		mode = modeName;
	}
}
