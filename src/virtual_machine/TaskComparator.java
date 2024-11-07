package virtual_machine;

import java.util.Comparator;

public class TaskComparator implements Comparator<Task> {

	@Override
	public int compare(Task o1, Task o2) {
		return Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
	}

}
