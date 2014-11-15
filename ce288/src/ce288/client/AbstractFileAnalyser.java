package ce288.client;

import java.io.InputStream;
import java.util.UUID;

import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

public abstract class AbstractFileAnalyser {
	
	public abstract void process(UUID clientId, InputStream in, Task task, TaskRepositoryInterface stub) throws FileAnalyserException;

}
