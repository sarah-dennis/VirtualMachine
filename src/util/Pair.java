package util;


/** 
* generic immutable pairs
*
* @author Michael Siff
*/
public class Pair<T, U> {

	private T _first;
	private U _second;

	public Pair(T first, U second) {
		_first = first;
		_second = second;
	}

	public T first() { return _first; }
	public U second() { return _second; }

	public boolean equals(Pair<T, U> otherPair) {
		return _first.equals(otherPair.first()) &&
				_second.equals(otherPair.second());
	}
	
	public int hashCode() {
		return _first.hashCode()*31 + _second.hashCode()*31;
	}

	public String toString() {
		return "<" + _first + ", " + _second + ">";
	}

}
