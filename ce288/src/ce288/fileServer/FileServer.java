package ce288.fileServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.FileFormat;
import ce288.tasks.FileFormatException;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;
import ce288.tasks.TaskStatus;

public class FileServer {

	public static final Logger logger = LoggerFactory.getLogger(FileServer.class);

	/**
	 * Section size is 10MB
	 */
	public static final long DEFAULT_SECTION_SIZE = 10485760;

	public static final int PORT = 12345;

	private InetAddress address;

	private String path;

	private TaskRepositoryInterface stub;

	private HashMap<String, List<UUID>> tasks;

	public FileServer(String path, String addr) {
		logger.info("File server started.");
		tasks = new HashMap<>();
		setPath(path);
		Thread fileServerThread = new Thread(new FileServerThread(this));
		fileServerThread.setDaemon(true);
		fileServerThread.start();

		try {
			Registry registry = LocateRegistry.getRegistry();
			stub = (TaskRepositoryInterface) registry.lookup("TaskRepository");
		} catch (IOException | NotBoundException e) {
			logger.error(e.getMessage(), e);
		}

		// Set the external IP address
		try {
			setAddress(addr);
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
		}

		// Receive commands from user
		new Thread(new ConsoleThread(this)).start();
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(String addr) throws UnknownHostException {
		address = InetAddress.getByName(addr);
	}

	public String getPath() {
		synchronized (path) {
			return path;
		}
	}

	public void setPath(String path) {
		synchronized (path) {
			this.path = path;
		}
	}

	public void addFile(String filename) throws FileNotFoundException, FileFormatException, RemoteException, IOException {
		addFile(filename, DEFAULT_SECTION_SIZE);
	}

	public void addFile(String filename, long sectionSize) throws FileNotFoundException, FileFormatException, RemoteException, IOException {
		String fullPath = this.path + File.separator + filename;
		File file = new File(fullPath);
		long size = file.length();

			FileFormat format = preprocess(fullPath);
			for (long pos = 0; pos < size; pos += sectionSize) {
			long length = Math.min(sectionSize, size - pos);
			Task task = new Task(format, address, filename, pos, length);
			stub.addTask(task);
			if (tasks.containsKey(filename)) {
				tasks.get(filename).add(task.getId());
			} else {
				List<UUID> ids = new ArrayList<UUID>();
				ids.add(task.getId());
				tasks.put(filename, ids);
			}
			logger.debug("Added task {} for file {}.", task.getId(), filename);
		}
	}

	public TaskStatus getStatus(UUID taskId) throws RemoteException {
		return stub.getStatus(taskId);
	}

	public Map<String, List<UUID>> getTasks() {
		return tasks;
	}

	private FileFormat preprocess(String filename) throws FileFormatException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		boolean isHeader;
		do {
			String headline = reader.readLine();
			isHeader = headline.length() > 22 && !Character.isDigit(headline.charAt(1))
					&& !Character.isDigit(headline.charAt(2))
					&& !Character.isDigit(headline.charAt(5))
					&& !Character.isDigit(headline.charAt(8))
					&& !Character.isDigit(headline.charAt(9))
					&& !Character.isDigit(headline.charAt(11))
					&& !Character.isDigit(headline.charAt(15))
					&& !Character.isDigit(headline.charAt(20))
					&& !Character.isDigit(headline.charAt(22));
			for (FileFormat format : FileFormat.values()) {
				if (headline.indexOf(format.getMark()) > 0) {
					reader.close();
					logger.info("File format is: {}", format);
					return format;
				}
			}
		} while (isHeader);
		reader.close();

		throw new FileFormatException("Could not identify file format for file " + filename);
	}

	public static void main(String[] args) {
		if (args.length >= 2) {
			new FileServer(args[0], args[1]);
		} else {
			logger.error("USAGE: java ce288.fileServer.FileServer FILES_PATH IP_ADDRESS");
		}
	}

}
