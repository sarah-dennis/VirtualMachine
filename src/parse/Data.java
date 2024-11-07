package parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataTypes.DataType;

public class Data {

	private List<DataType> _dl;
	private Map<String, Integer> _da;
	
	public Data() {
		_dl = new ArrayList<DataType>();
		_da = new HashMap<String, Integer>();
	}
	
	public List<DataType> getDataList() {
		return _dl;
	}
	
	public Map<String, Integer> getDataAssignMap() {
		return _da;
	}
	
	public void addData(DataType d) {
		_dl.add(d);
	}
	
	public void assignData(String label, Integer d) {
		_da.put(label, d);
	}
	
	public boolean containsLabel(String label) {
		return _da.containsKey(label);
	}
	
	public int positionOf(DataType d) {
		return _dl.indexOf(d);
	}
	
	public String dataListToString() {
		String result = "[";
		for (DataType d : _dl) {
			if (_dl.get(_dl.size()-1) == d) {
				result += d.toString();
			} else {
				result += d.toString() + ", ";
			}
		}
		result += "]";
		return result;
	}
	
	public Integer getVal(String label) {
		if (containsLabel(label)) {
			return _da.get(label);
		} else {
			return null;
		}
	}
	
}
