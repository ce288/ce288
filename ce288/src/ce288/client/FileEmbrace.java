/**
 * 
 */
package ce288.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import ce288.tasks.Result;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

public class FileEmbrace extends AbstractFileAnalyser {

	public FileEmbrace() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(UUID clientId, InputStream in, Task task, TaskRepositoryInterface stub) throws FileAnalyserException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		try {
			String line;
			Result result = new Result(task.getId());
			long pos = task.getPosition();
			while ((line = reader.readLine()) != null) {

				line = in.toString();
				System.out.println("Varlei");
				System.out.println(line);
				result.addLog(pos, "Varlei");
				pos += line.length();
			}
			stub.setResult(clientId, task.getId(), result);
		} catch (IOException e) {
			throw new FileAnalyserException("Error while processing stream", e);
		}
	} // close process

	public void teste(String message) {

		int contador = 0;
		while (contador < 40) {
			System.out.println(message);
			contador++;
		}

		return;

	} // close teste

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	} // close main()

} // close FileEmbrace
