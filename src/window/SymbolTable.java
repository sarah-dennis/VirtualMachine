package window;

import java.util.HashSet;
import java.util.Set;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Class window.SymbolTable handles the display aspects of the symbol table produced during parsing.
 * It is essentially a wrapper for a TableView object that displays SymbolEntrys. 
 */
public class SymbolTable {
	
	/**
	 * Inner class SymbolEntry is an object with specified Properties, which can then be used in cell factories.
	 */
	public class SymbolEntry {
		public final SimpleStringProperty symbol;
		public final SimpleIntegerProperty line;
		public final SimpleBooleanProperty isBreakPt;
		private String symb;
		private int lin;
		private boolean isBP;
		int codeline;
		
		SymbolEntry(int line, String sym, int sourceLine) {
			this.symbol = new SimpleStringProperty(sym);
			this.line = new SimpleIntegerProperty(line);
			this.isBreakPt = new SimpleBooleanProperty(false);
			symb = sym;
			lin = line;
			isBP = false;
			codeline = sourceLine;
			isBreakPt.addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue) {
						_events.addBreak((Integer)codeline);
					} else {
						_events.removeBreak((Integer)codeline);
					}
				}
			});
		}
		/**
		 * Getters and setters provided for the two properties, in order to ensure that the
		 * CellValueFactory works properly
		 */
		public SimpleStringProperty symbolProperty() {
			return this.symbol;
		}
		public SimpleIntegerProperty lineProperty() {
			return this.line;
		}
		public ObservableBooleanValue isBreakPtProperty() {
			return this.isBreakPt;
		}
		public String getSymbol() {
			return symb;
		}
		public int getLine() {
			return lin;
		}
		public boolean getIsBreakPt() {
			return isBP;
		}
		public void setSymbol(String newValue) {
			symbol.set(newValue);
			symb = newValue;
		}
		public void setLine(int newValue) {
			line.set(newValue);
			lin = newValue;
		}
		public void setIsBreakPt(boolean newValue) {
			isBreakPt.set(newValue);
			isBP = newValue;
		}
		/**
		 * Overridden methods to use in sorting, display, etc.
		 */
		@Override
		public boolean equals(Object o) {
			SymbolEntry other = (SymbolEntry)o;
			return (this.lin == other.getLine());
		}
		@Override
		public String toString() {
			String symbName = symb;
			if (this.isBP) {
				symbName = "*" + symb;
			}
			return "<" + symbName + "," + lin + ">; ";
		}
	}

	TableView<SymbolEntry> _table = new TableView<SymbolEntry>();
	private ObservableList<SymbolEntry> _data = FXCollections.observableArrayList();
	private parse.SymbolTable st;
	private EventLists _events;
	
	/**
	 * Constructor sets up the TableView for the display, creates the columns and sets the MouseEvent handler. 
	 * @param events - the EventLists object where breakpoints are handled
	 * @param clicker - the event handler that tells what will happen when you click the "symbol" column.
	 */
	SymbolTable(EventLists events, EventHandler<MouseEvent> clicker) {
		_events = events;
		_table = new TableView<SymbolEntry>();
		_table.setEditable(true);
		_table.setPrefSize(200, 350);
		TableColumn<SymbolEntry, String> symbols = new TableColumn<SymbolEntry, String>("Symbol");
		symbols.setPrefWidth(150);
		symbols.setCellValueFactory(
				new PropertyValueFactory<SymbolEntry, String>("symbol"));
		symbols.setCellFactory(new CustomCellFactory<String>(clicker));
		TableColumn<SymbolEntry, Integer> lines = new TableColumn<SymbolEntry, Integer>("#");
		lines.setMinWidth(50);
		lines.setMaxWidth(50);
		lines.setResizable(false);
		lines.setCellValueFactory(
				new PropertyValueFactory<SymbolEntry,Integer>("line"));
		
		TableColumn<SymbolEntry, Boolean> brkpts = new TableColumn<>("*");
		brkpts.setMaxWidth(50);
		brkpts.setMinWidth(50);
		brkpts.setResizable(false);
		
		//-------------------------------------------------
		brkpts.setCellFactory(new Callback<TableColumn<SymbolEntry, Boolean>, TableCell<SymbolEntry, Boolean>>() {
            @Override
            public TableCell<SymbolEntry, Boolean> call(TableColumn<SymbolEntry, Boolean> p) {
                final CheckBoxTableCell<SymbolEntry, Boolean> ctCell = new CheckBoxTableCell<>();
                ctCell.setSelectedStateCallback(new Callback<Integer, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(Integer index) {
                        return _table.getItems().get(index).isBreakPtProperty();
                    }
                });
                return ctCell;
            }
        });
		//-------------------------------------------------------
		
		_table.setItems(_data);

		_table.getColumns().add(brkpts);
		_table.getColumns().add(symbols);
		_table.getColumns().add(lines);
		_table.getSortOrder().add(lines);
	}
	
	/**
	 * Given a symbol table (generated during parsing), updates the TableView for display
	 * @param table - the table generated by parsing that contains the labels and source lines.
	 */
	public void updateTable(parse.SymbolTable table) {
		st = table;
		Set<SymbolEntry> testSet = new HashSet<SymbolEntry>();
		Set<String> tableEntries = table.getLabels();
		if (tableEntries != null && !tableEntries.isEmpty()) {
			for (String label : tableEntries) {
				testSet.add(new SymbolEntry(table.getSourceLine(label)+1, label, table.getCodeLine(label)));
			}
		}
		_events.clearBreaks();
		_data.clear();
		if (!testSet.isEmpty()) {
			for (SymbolEntry test : testSet) {
				_data.add(test);
			}
		}
		_table.sort();
	}
	
	/**
	 * Clears the display table. Since we had problems with the display rendering "ghost" data,
	 * our way of clearing the symbol table here is to reinitialize the table. 
	 */
	public void clear() {
		_events.clearBreaks();
		_data = FXCollections.observableArrayList();
		_table.setItems(_data);
	}
	
	/**
	 * Custom class used to create cells for the TableView display. This program uses it to 
	 * generate cells with a custom MouseEvent, to highlight the line in code where the symbol is declared. 
	 * (template provided by the Internet, source unknown)
	 * @param <T> - the form of raw data displayed in the TableCell, i.e. String or Integer
	 */
	class CustomCellFactory<T> implements Callback<TableColumn<SymbolEntry,T>, TableCell<SymbolEntry,T>> {
		EventHandler<MouseEvent> click;
		
		public CustomCellFactory(EventHandler<MouseEvent> click) {
			this.click = click;
		}
		
		public final TableCell<SymbolEntry,T> call(TableColumn<SymbolEntry,T> p) {
			TableCell<SymbolEntry, T> cell = new TableCell<SymbolEntry, T>() {
	               @Override
	               protected void updateItem(T item, boolean empty) {
	                  // calling super here is very important - don't skip this!
	                  super.updateItem(item, empty);
	                  if(item != null) {
	                      setText(""+item);
	                  }
	               }
			};
			if(click != null) {
		         cell.setOnMouseClicked(click);
		    }
			return cell;
		}
	}
	
	/**
	 * Prints out the table for debugging
	 */
	public void printTable() {
		String printString = "";
		if (_data.isEmpty()) {
			printString = "Empty symbol table";
		} else {
			for (SymbolEntry entry : _data) {
				printString += entry.toString();
			}
		}
		System.out.println(printString);
	}
	
	/** convenience method returns the parse SymbolTable for use in disassembly **/
	public parse.SymbolTable getParseTable() {
		return st;
	}
}