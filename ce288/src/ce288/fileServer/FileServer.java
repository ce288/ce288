package ce288.fileServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.FileFormat;
import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;

public class FileServer {

	public static final Logger logger = LoggerFactory
			.getLogger(FileServer.class);

	/**
	 * Section size is 10MB
	 */
	public static final long SECTION_SIZE = 10485760;

	public static final int PORT = 12345;

	private InetAddress address;

	private TaskRepositoryInterface stub;

	public FileServer(String path) {
		logger.info("File server started.");
		new FileServerThread(path).start();

		try {
			Registry registry = LocateRegistry.getRegistry();
			stub = (TaskRepositoryInterface) registry.lookup("TaskRepository");
		} catch (IOException | NotBoundException e) {
			logger.error(e.getMessage(), e);
		}

		// Discover the external IP address
		address = null;
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface i = interfaces.nextElement();
				System.out.println(i);
				if (!i.isUp() || i.isLoopback() || i.isVirtual()) {
					continue;
				}
				Enumeration<InetAddress> addresses = i.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (!addr.isLoopbackAddress()) {
						setAddress(addr);
						break;
					}
				}
				break;
			}
		} catch (SocketException e) {
			logger.error(e.getMessage(), e);
		}
		if (address == null) {
			throw new RuntimeException("Unable to determine address");
		}

		// Receive commands from user
		String line;
		Scanner in = new Scanner(System.in);
		while (true) {
			System.out.println("> ");
			line = in.nextLine().trim();
			String[] tokens = line.split("\\s+");
			if (tokens.length >= 1) {
				if (tokens[0].equalsIgnoreCase("exit")) {
					break;
				} else if (tokens[0].equalsIgnoreCase("add")) {
					if (tokens.length >= 2) {
						processFile(tokens[1]);
					} else {
						System.out.println("Invalid syntax: add FILENAME");
					}
				}
			}
		}
		in.close();
	}

	public void setAddress(InetAddress address) {
		this.address = address;
		logger.debug("Local address is {}.", address.getHostAddress());
	}

	private void processFile(String filename) {
		File file = new File(filename);
		long size = file.length();

		FileFormat format = preprocess(file);
		for (long pos = 0; pos < size; pos += SECTION_SIZE) {
			long length = Math.min(SECTION_SIZE, size - pos);
			try {
				stub.addTask(new Task(format, address, filename, pos, length));
			} catch (RemoteException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private FileFormat preprocess(File file) {
		// TODO definir o formato do arquivo
		return null;
	}

	public static void main(String[] args) {
		if (args.length >= 1) {
			new FileServer(args[0]);
		} else {
			logger.error("Missing path argument.");
		}
	}

}
