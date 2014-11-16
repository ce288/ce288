package ce288.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.fileServer.FileServer;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

public class Client {

	public final static Logger logger = LoggerFactory.getLogger(Client.class);

	private UUID id;

	public Client() {
		id = UUID.randomUUID();
	}

	public void execute() {
		logger.info("Client {} started.", id);
		try {
			Registry registry = LocateRegistry.getRegistry();
			TaskRepositoryInterface stub = (TaskRepositoryInterface) registry
					.lookup("TaskRepository");
			while (true) {
				Task task = stub.getNext(id);
				if (task == null) {
					Thread.sleep(1000);
				} else {
					logger.info("Client started task {}.", task.getId());
					try {
						Socket socket = new Socket(task.getLocation(), FileServer.PORT);
						BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
								socket.getOutputStream()));

						// Send filename
						out.write(task.getFilename());
						out.newLine();

						// Send file start position
						long pos = Math.max(0, task.getPosition() - 100);
						out.write(Long.toString(pos));
						out.newLine();

						// Send section length
						out.write(Long.toString(task.getLength() + 200));
						out.newLine();

						out.flush();
						AbstractFileAnalyser analyser = AbstractFileAnalyserFactory
								.getAnalyser(task.getFormat());
						analyser.process(id, socket.getInputStream(), task, stub);
						socket.close();
						logger.info("Task {} finished.", task.getId());
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		} catch (IOException | NotBoundException | InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		Client client = new Client();
		client.execute();
	}
}
