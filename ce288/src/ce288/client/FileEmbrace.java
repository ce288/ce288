/**
 * 
 */
package ce288.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.fileServer.FileServer;
import ce288.tasks.Result;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

public class FileEmbrace extends AbstractFileAnalyser {

	public static final Logger logger = LoggerFactory.getLogger(FileEmbrace.class);

	public FileEmbrace() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(UUID clientId, InputStream in, Task task, TaskRepositoryInterface stub)
			throws FileAnalyserException {
		int lineCounter = 0;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		logger.debug("Task start={} length={}", task.getPosition(), task.getLength());

		try {
			String line;
			Result result = new Result(task.getId());
			Calendar lastCalendar = null;
			Calendar calendar = Calendar.getInstance();
			calendar.clear();
			// If the section is at the beginning, then skip the header lines
			boolean header = (task.getPosition() == 0);
			while ((line = reader.readLine()) != null) {
				lineCounter += 1;

				// Discard lines at the beginning of the file that are header
				// lines. After a non-header line is found, no other line is
				// considered a header
				header = FileServer.isHeader(line) && header;
				if (header) {
					logger.debug("Skipping line at {} because is header", lineCounter);
					continue;
				}

				// Discard line due to truncation at the start
				if (lineCounter == 1) {
					logger.debug("Skipping line because is section start");
					continue;
				}

				// Reads the time values
				String[] words = line.trim().split("\\s+", 6);
				if (words.length < 6) {
					String msg = String.format("Line %d is incomplete: %s", lineCounter, line);
					logger.debug("@{} - {}", task.getPosition(), msg);
					result.addLog(task.getPosition(), msg);
					continue;
				}
				try {
					int day = Integer.valueOf(words[0]);
					int month = Integer.valueOf(words[1]);
					int year = Integer.valueOf(words[2]);
					int hour = Integer.valueOf(words[3]);
					int min = Integer.valueOf(words[4]);
					calendar.set(year, month, day, hour, min);
					if (lastCalendar == null) {
						lastCalendar = Calendar.getInstance();
						lastCalendar.clear();
						lastCalendar.set(year, month, day, hour, min);
					} else {
						lastCalendar.add(Calendar.MINUTE, 1);
						if (!calendar.equals(lastCalendar)) {
							String msg = String
									.format("Line %d is not sequential, expected %02d %02d %04d  %02d %02d: %s",
											lineCounter,
											lastCalendar.get(Calendar.DAY_OF_MONTH),
											lastCalendar.get(Calendar.MONTH),
											lastCalendar.get(Calendar.YEAR),
											lastCalendar.get(Calendar.HOUR_OF_DAY),
											lastCalendar.get(Calendar.MINUTE), line);
							logger.debug("@{} - {}", task.getPosition(), msg);
							result.addLog(task.getPosition(), msg);
							lastCalendar = calendar;
						}
					}
				} catch (NumberFormatException e) {
					String msg = String.format("line %d - Invalid date or time: %s",
							lineCounter, line);
					logger.debug("@{} - {}", task.getPosition(), msg);
					result.addLog(task.getPosition(), msg);
				}

			}
			stub.setResult(clientId, task.getId(), result);
		} catch (IOException e) {
			try {
				stub.setFailure(clientId, task.getId(), "Error while processing stream");
			} catch (RemoteException e1) {
				throw new FileAnalyserException("Error while setting failure in remote server", e);
			}
			throw new FileAnalyserException("Error while processing stream", e);
		}
	}

}
