package window;

import java.util.ArrayList;
import java.util.List;

public class EventLists {
	
	private List<Integer> clicks;
	private List<Integer> keys;
	private List<Integer> breaks;
	
	/**
	 * Constructs the lists to use, passing in the graphics SymbolTable where breakpoints are illustrated.
	 */
	public EventLists() {
		clicks = new ArrayList<Integer>();
		keys = new ArrayList<Integer>();
		breaks = new ArrayList<Integer>();
	}
	
	/**
	 * Tells whether there are click events in the list
	 * @return true if the click list is empty
	 */
	public boolean clicksEmpty() {
		return clicks.isEmpty();
	}
	
	/**
	 * Tells whether there are key events in the list
	 * @return true if the key list is empty
	 */
	public boolean keysEmpty() {
		return keys.isEmpty();
	}
	
	/**
	 * Tells whether there are breakpoints in the list
	 * @return true if the breakpoint list is empty
	 */
	public boolean breaksEmpty() {
		return breaks.isEmpty();
	}
	
	/**
	 * Grabs the next available click event from the queue
	 * @return the next click event in the queue
	 */
	public int getNextClick() {
		return clicks.remove(0);
	}
	
	/**
	 * Grabs the next available key event from the queue
	 * @return the next key event in the queue
	 */
	public int getNextKey() {
		return keys.remove(0);
	}
	
	/**
	 * Adds click event to the click queue
	 * @param clickPos - the int representing the location of the click event
	 */
	public void addClick(int clickPos) {
		clicks.add(clickPos);
	}
	
	/**
	 * Adds key event to the key queue
	 * @param keyCode - the ASCII code of the key that was typed
	 */
	public void addKey(int keyCode) {
		keys.add(keyCode);
	}
	
	/**
	 * Adds a breakpoint to the list of breakpoints
	 * @param point - the code line number of the breakpoint
	 */
	public void addBreak(int point) {
		breaks.add(point);
	}
	
	/**
	 * Removes a breakpoint from the list
	 * @param point - the code line number of the breakpoint
	 */
	public void removeBreak(Integer point) {
		breaks.remove(point);
	}
	
	/**
	 * Clears the list of breakpoints. 
	 */
	public void clearBreaks() {
		breaks.clear();
	}
	
	/**
	 * Clears the graphics' representation of the list of events (both key/click)
	 */
	public void clearEvents() {
		clicks.clear();
		keys.clear();
	}
	
	/**
	 * Returns true if the argument is contained in the list of breakpoints.
	 * @param pc - the line number that is being searched for in the breakpoints
	 * @return true if the line number is within the list of breakpoints. 
	 */
	public boolean breaksContain(int pc) {
		return breaks.contains(pc);
	}
}
