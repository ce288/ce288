package ce288.fileServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.FileFormat;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

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
	
	private HashMap<String, UUID> tasks;
	
	public FileServer(String path, String addr) {
		logger.info("File server started.");
		setPath(path);
		new FileServerThread(this).start();

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
	
	public void addFile(String filename) throws FileNotFoundException, RemoteException {
		addFile(filename, DEFAULT_SECTION_SIZE);
	}

	public void addFile(String filename, long sectionSize) throws FileNotFoundException,
			RemoteException {
		File file = new File(filename);
		long size = file.length();

		if (size == 0L) {
			throw new FileNotFoundException(filename + " is not a valid file name.");
		}

		FileFormat format = preprocess(file);
		for (long pos = 0; pos < size; pos += sectionSize) {
			long length = Math.min(sectionSize, size - pos);
			Task task = new Task(format, address, filename, pos, length);
			stub.addTask(task);
			tasks.put(filename, task.getId());
			logger.debug("Added task {} for file {}.", task.getId(), filename);
		}
	}

	private FileFormat preprocess(File file) {
		// TODO definir o formato do arquivo
		return null;
	}

	public static void main(String[] args) {
		if (args.length >= 2) {
			new FileServer(args[0], args[1]);
		} else {
			logger.error("USAGE: java ce288.fileServer.FileServer FILES_PATH IP_ADDRESS");
		}
	}

}
