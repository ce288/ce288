package ce288.fileServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
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

import ce288.client.Client;
import ce288.server.Server;
import ce288.tasks.FileFormat;
import ce288.tasks.FileFormatException;
import ce288.tasks.Result.ResultLog;
import ce288.tasks.Task;
import ce288.tasks.TaskRepository;
import ce288.tasks.TaskRepositoryInterface;
import ce288.tasks.TaskStatus;

public class FileServer {

	public static final Logger logger = LoggerFactory.getLogger(FileServer.class);

	/**
	 * Default section size is 10MB
	 */
	public static final long DEFAULT_SECTION_SIZE = 10485760;

	/**
	 * Network port where this process will listen to {@link Client} connection
	 * requests.
	 */
	public static final int PORT = 12345;

	/**
	 * IP address of this {@link FileServer}, which the {@link Client} instances
	 * will connect to.
	 */
	private InetAddress address;

	/**
	 * Path to the folder containing the sensor files
	 */
	private String path;

	/**
	 * Stub to the remote {@link TaskRepository} created by {@link Server}.
	 */
	private TaskRepositoryInterface stub;

	/**
	 * Store the {@link Task}s created by this instance since it was started.
	 * 
	 * The key is {@link Task#getFilename()}.
	 */
	private HashMap<String, List<UUID>> tasks;

	/**
	 * Creates a FileServer instance.
	 * 
	 * @param path
	 *            path to the folder containing the sensor files
	 * @param addr
	 *            IP address of this {@link FileServer}, which the
	 *            {@link Client} instances will connect to
	 */
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

	/**
	 * Gets the IP address the {@link Client} instances will connect to.
	 * 
	 * @return the IP address of this {@link FileServer}
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Sets the IP address the {@link Client} instances will connect to.
	 * 
	 * @param addr
	 *            the IP address of this {@link FileServer}
	 * @throws UnknownHostException
	 *             if addr is not valid
	 */
	public void setAddress(String addr) throws UnknownHostException {
		address = InetAddress.getByName(addr);
	}

	/**
	 * Gets the folder path where the files to be processed are located
	 * 
	 * @return the folder path where the files to be processed are located
	 */
	public String getPath() {
		synchronized (path) {
			return path;
		}
	}

	/**
	 * Sets the folder path where the files to be processed are located
	 * 
	 * @param path
	 *            the folder where the files to be processed are located
	 */
	public void setPath(String path) {
		synchronized (path) {
			this.path = path;
		}
	}

	/**
	 * Similar to {@link #getResults(String, String)}, but prints to screen
	 * only.
	 * 
	 * @param filename
	 *            the processed file
	 * @throws IOException
	 *             if there are no tasks associated to that filename, of if an
	 *             error occurred while printing results to outputPath
	 */
	public void getResults(String filename) throws IOException {
		getResults(filename, null);
	}

	/**
	 * Prints the {@link ResultLog} contents produced by the tasks relate to the
	 * filename, to a file defined by path, or to the {@link System#out} if path
	 * = null.
	 * 
	 * @param filename
	 *            the processed file
	 * @param outputPath
	 *            the output path, or null to print to screen
	 * @throws IOException
	 *             if there are no tasks associated to that filename, of if an
	 *             error occurred while printing results to outputPath
	 */
	public void getResults(String filename, String outputPath) throws IOException {
		if (!tasks.containsKey(filename)) {
			throw new FileNotFoundException("There are no tasks for file " + filename);
		}
		List<ResultLog> resultLogs = stub.getResult(tasks.get(filename));

		PrintStream out;
		if (outputPath != null) {
			out = new PrintStream(outputPath);
		} else {
			out = System.out;
		}

		out.println("offset\tmessage");
		for (ResultLog result : resultLogs) {
			out.printf("%d\t%s\n", result.getPos(), result.getMsg());
		}
		out.println("<<EOF>>");

		if (outputPath != null) {
			out.close();
		}

		tasks.remove(filename);
	}

	/**
	 * Similar to {@link #addFile(String, long)}, but with
	 * <code>sectionSize</code> = {@link #DEFAULT_SECTION_SIZE}.
	 * 
	 * @param filename
	 *            the filename without path, which is located in the folder
	 *            {@link #path}
	 * @throws FileNotFoundException
	 *             if the filename is not found in {@link #path}
	 * @throws FileFormatException
	 *             if the file is not one of the types defined by
	 *             {@link FileFormat}
	 * @throws RemoteException
	 *             if could not connect to remote {@link TaskRepository}
	 * @throws IOException
	 *             if could not read file
	 */
	public void addFile(String filename) throws FileNotFoundException, FileFormatException,
			RemoteException, IOException {
		addFile(filename, DEFAULT_SECTION_SIZE);
	}

	/**
	 * Create the tasks to process the file
	 * 
	 * @param filename
	 *            the filename without path, which is located in the folder
	 *            {@link #path}
	 * @param sectionSize
	 *            the number of bytes to be processed by each task
	 * @throws FileNotFoundException
	 *             if the filename is not found in {@link #path}
	 * @throws FileFormatException
	 *             if the file is not one of the types defined by
	 *             {@link FileFormat}
	 * @throws RemoteException
	 *             if could not connect to remote {@link TaskRepository}
	 * @throws IOException
	 *             if could not read file
	 */
	public void addFile(String filename, long sectionSize) throws FileNotFoundException,
			FileFormatException, RemoteException, IOException {
		String fullPath = this.path + File.separator + filename;
		File file = new File(fullPath);
		long size = file.length();

		if (tasks.containsKey(filename)) {
			tasks.remove(filename);
		}

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

	/**
	 * Retrieve the current status of the {@link Task} from the remote
	 * {@link TaskRepository}.
	 * 
	 * @param taskId
	 *            the {@link Task#getId()}
	 * @return the status of the task, one of PENDING, EXECUTING, FINISHED or
	 *         FAILED
	 * @throws RemoteException
	 *             if could not contact the remote {@link TaskRepository}
	 */
	public TaskStatus getStatus(UUID taskId) throws RemoteException {
		return stub.getStatus(taskId);
	}

	/**
	 * Gets the {@link FileServer#tasks}.
	 * 
	 * @return the {@link FileServer#tasks}
	 */
	public Map<String, List<UUID>> getTasks() {
		return tasks;
	}

	/**
	 * Read the first lines of the file to determine the file type.
	 * 
	 * @param filename
	 *            the file to be processed
	 * @return one of the {@link FileFormat} values
	 * @throws FileFormatException
	 *             if could not define the format
	 * @throws IOException
	 *             if could not read the file
	 */
	private FileFormat preprocess(String filename) throws FileFormatException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		boolean isHeader = false;
		do {
			String headline = reader.readLine();
			isHeader = isHeader(headline);
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

	/**
	 * Returns <code>false</code> if the line appears to be sensor data instead
	 * of a file header
	 * 
	 * @param line
	 *            the line of text
	 * @return <code>false</code> if the line appears to be sensor data instead
	 *         of a file header
	 */
	public static boolean isHeader(String line) {
		String[] words = line.trim().split("\\s+", 7);
		if (words.length < 6) {
			return true;
		}
		try {
			Double.valueOf(words[0]);
			Double.valueOf(words[1]);
			Double.valueOf(words[2]);
			Double.valueOf(words[3]);
			Double.valueOf(words[4]);
			Double.valueOf(words[5]);
		} catch (NumberFormatException e) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		if (args.length >= 2) {
			new FileServer(args[0], args[1]);
		} else {
			logger.error("USAGE: java ce288.fileServer.FileServer FILES_PATH IP_ADDRESS");
		}
	}

}
