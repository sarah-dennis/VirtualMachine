package virtual_machine;

public class Task{
	
	private String _description;
	private long _timeStamp;
	private int _arg;
	private int _destPC;
	
	public Task(String descr, long waitTime, int arg, int destPC) {
		_description = descr;
		_timeStamp = (waitTime * 1000000) + System.nanoTime();
		_arg = arg;
		_destPC = destPC;
	}
	
	
	public long getTimeStamp() {
		return _timeStamp;
	}

	public int getDestPC() {
		return _destPC;
	}

	public int getArg() {
		return _arg;
	}

	public String getDescr() {
		return _description;
	}
}
