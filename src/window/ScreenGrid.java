package window;

import java.util.HashMap;
import java.util.Map;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import util.Pair;
import virtual_machine.MemoryException;
import virtual_machine.ExecVM;
import window.ErrorConsole;

public class ScreenGrid {

	private static double SCREEN_WIDTH = 400; //width of the stage (simulated screen)
	private static double SCREEN_HEIGHT = 400; //height of the stage (simulated screen)
	private static double BLOCK_WIDTH = 10; //height of block units
	private static double BLOCK_HEIGHT = 16; //width of block units
	private final static int COLUMNS = 40; //number of total columns (max x pos)
	private final static int ROWS = 25; //number of total rows (max y pos)
	private static double font_size = 16;
	private double screenScale = 1;
	@SuppressWarnings("unused")
	private final static int MEMORY_SIZE = COLUMNS * ROWS; //total number of memory addresses displayed on screen
	
	private ExecVM _vm;
	private ErrorConsole _ec;
	private EventLists _events;
	private Color bgColor = Color.web("#143800");
	private Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
	private HBox display = new HBox();
	
	/**
	 * Constructor initializes the virtual machine where the memory is located. 
	 * Also initializes the pane for the graphics.
	 * @param vm - the virtual machine whose memory is being accessed.
	 * @param iof - the input/output interface, for displaying errors.
	 * @param events - the list of events where the canvas' click events should be added.
	 */
	public ScreenGrid(ExecVM vm, ErrorConsole iof, EventLists events) {
		_vm = vm;
		_ec = iof;
		_events = events;
		canvas.setOnMouseClicked(event -> addClickEvent(event.getX(), event.getY()));
		display.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		display.getChildren().add(canvas);
	}
	
	/**
	 * Resizes the screen's canvas and the block sizes according to the given scale
	 * @param scale - the percent scale by which the screen should be resized
	 */
	public void resize(double scale) {
		SCREEN_WIDTH = 400 * scale;
		SCREEN_HEIGHT = 400 * scale;
		BLOCK_WIDTH = 10 * scale;
		BLOCK_HEIGHT = 16 * scale;
		font_size = 16 * scale;
		screenScale = scale;
		canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
		display.getChildren().add(canvas);
		display.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		clearGrid();
	}
	
	/**
	 * Returns the object where the ScreenGrid is used.
	 * @return - the BorderPane where the canvas is displayed.
	 */
	public HBox getDisplay() {
		display.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		return display;
	}
	
	/**
	 * Paints one address, so that the screen doesn't have to render everything when only one address has changed.
	 * @param address - the address being updated on the screen
	 */
	public void paintOne(Integer address) {
		Pair<Integer, Integer> gridRange = _vm.getOS().getGraphicsRange();
		if (gridRange != null && address != null) {
			int gridStart = gridRange.first() * 4;
			int gridEnd = gridRange.second() * 4;
			if ((address >= gridStart) && (address < gridEnd)) {
				GraphicsContext gc = canvas.getGraphicsContext2D();
				int value = 0;
				try {
					value = _vm.getValueInMainMem(address, 4);
					char c = (char) value;
					if (value < 32) {
						c = ' ';
					}
					address = address - gridStart;
					int x = (address/4) % COLUMNS;
					int y = (address/4) / COLUMNS;
					Pair<Integer, Integer> pos = new Pair<>(x, y);
					highlight((address/4), bgColor);
					renderChar(gc, c, pos);
				} catch (MemoryException e) {
					_ec.reportError("Problem drawing: " + e.toString(), 0, null);
					highlight((address/4), Color.RED);
				}
			}
		}
	}

	/**
	 * Draws the contents of memory on the canvas. If the value contained in the memory slot
	 * doesn't correspond to a valid CharacterIcon, the address is highlighted on screen, but
	 * left blank.
	 */
	public void paintAll() {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(bgColor);
		gc.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
		Pair<Integer, Integer> gridRange = _vm.getOS().getGraphicsRange();
		if (gridRange != null) {
			int address = gridRange.first() * 4;
			while (address < (gridRange.second()*4)) {
				try {
					int code = _vm.getValueInMainMem((address), 4);
					if (code > 0) {
						char charText = (char) code;
						if (code < 32) {
							charText = ' ';
						}
						int x = address % COLUMNS;
						int y = address / COLUMNS;
						Pair<Integer, Integer> position = new Pair<Integer, Integer>(x, y);
						renderChar(gc, charText, position);
					}
				} catch (MemoryException e) {
					_ec.reportError("Problem in screen render: " + e.getMessage(), 0, null);
				}
				address+= 4;
			}
		}
	}
	
	/**
	 * Colors the background of the address given to be a certain color
	 * @param address - the spot on screen to be colored
	 * @param color - the color with which to "highlight" said spot
	 */
	public void highlight(int address, Color color) {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		if (color == null) {
			gc.setFill(Color.GREY);
		} else {
			gc.setFill(color);
		}
		int x = address % COLUMNS;
		int y = address / COLUMNS;
		gc.fillRect(x*BLOCK_WIDTH, y*BLOCK_HEIGHT, BLOCK_WIDTH, BLOCK_HEIGHT);
	}
	
	/**
	 * Draws a single character on the screen at the specified x,y position
	 * @param gc - the graphics context for drawing the character
	 * @param c - the character being drawn
	 * @param position - the (x, y) coordinate on screen where the character will be drawn
	 */
	private void renderChar(GraphicsContext gc, char c, Pair<Integer, Integer> position) {
		gc.setStroke(Color.web("#51E800"));
		gc.setFill(Color.WHITE);
		gc.setFont(Font.font("Courier New", font_size));
		gc.setTextBaseline(VPos.TOP);
		gc.fillText(Character.toString(c), position.first()*BLOCK_WIDTH, position.second()*BLOCK_HEIGHT);
		gc.strokeText(Character.toString(c), position.first()*BLOCK_WIDTH, position.second()*BLOCK_HEIGHT);
	}
	
	/**
	 * Clears the grid, rather than attempting to render an empty memory system
	 */
	public void clearGrid() {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(bgColor);
		gc.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
	}
	
	/**
	 * Gives the numbers for the VM to have global constants for the graphics side.
	 * @return - a list of pairs for the VM to turn into global variables.
	 */
	public static Map<String, Integer> getGraphicsConstants() {
		Map<String, Integer> constants = new HashMap<String, Integer>();
		constants.put("_COLUMNS", 40);
		constants.put("_ROWS", 25);
		constants.put("_GRID_SIZE", 1000);
		return constants;
	}
	
	/**
	 * Adds a click event to the event list.
	 * @param x - the x position of where the click was received
	 * @param y - the y position of where the click was received
	 */
	private void addClickEvent(double x, double y) {
		int columnPos = (int)(x / (10 * screenScale));
		int rowPos = (int)(y / (16 * screenScale));
		int clickPos = (rowPos * COLUMNS) + columnPos;
		_events.addClick(clickPos);
	}
}
