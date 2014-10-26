package ce288.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.Result;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

public class DumbAnalyser extends AbstractFileAnalyser {

	public final static Logger logger = LoggerFactory.getLogger(DumbAnalyser.class);
	
	@Override
	public void process(UUID clientId, InputStream in, Task task, TaskRepositoryInterface stub) {
		BufferedInputStream input = new BufferedInputStream(in);
		int num;
		byte[] buffer = new byte[8192];
		try {
			while ((num = input.read(buffer)) > 0) {
				logger.debug("Received {} bytes: {}", num, new String(buffer, 0, num).trim());
			}
			stub.setResult(clientId, task.getId(), new Result(task.getId()));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
