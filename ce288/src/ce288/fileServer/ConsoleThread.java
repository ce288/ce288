package ce288.fileServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.FileFormatException;
import ce288.tasks.TaskStatus;

public class ConsoleThread implements Runnable {

	public static final Logger logger = LoggerFactory.getLogger(ConsoleThread.class);

	private FileServer parent;

	public ConsoleThread(FileServer parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		String line;
		Scanner in = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			line = in.nextLine().trim().toLowerCase();
			Iterator<String> tokens = Arrays.asList(line.split("\\s+")).iterator();
			String command = tokens.next();
			if (command.startsWith("exit")) {
				break;
			} else if (command.startsWith("add")) {
				processAdd(tokens);
			} else if (command.startsWith("ip")) {
				processIp(tokens);
			} else if (command.startsWith("path")) {
				processPath(tokens);
			} else if (command.startsWith("status")) {
				processStatus(tokens);
			} else if (command.startsWith("tasks")) {
				processTasks(tokens);
			} else if (command.startsWith("results")) {
				processResults(tokens);
			} else if (command.startsWith("help")) {
				System.out.println("Command list:");
				System.out.println("  add path [SECTION_SIZE]");
				System.out.println("  ip [NEW_IP]");
				System.out.println("  path [NEW_PATH]");
				System.out.println("  results [OUTPUT_FILE_PATH]");
				System.out.println("  status");
				System.out.println("  tasks [nostatus]");
			} else {
				System.out.println("Invalid command, type help to display all available commands.");
			}
		}
		in.close();
		System.out.println("Bye.");
	}

	private void processAdd(Iterator<String> tokens) {
		if (tokens.hasNext()) {
			String filename = tokens.next();
			try {
				if (tokens.hasNext()) {
					parent.addFile(filename, Long.parseLong(tokens.next()));
				} else {
					parent.addFile(filename);
				}
			} catch (RemoteException e) {
				System.out.println("Could not connect to remote service.");
				logger.error(e.getMessage(), e);
			} catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
			} catch (FileFormatException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println("Could not read " + filename);
			}
		} else {
			System.out.println("Invalid syntax: add FILENAME");
		}
	}

	private void processIp(Iterator<String> tokens) {
		if (tokens.hasNext()) {
			String addr = tokens.next();
			try {
				parent.setAddress(addr);
			} catch (UnknownHostException e) {
				System.out.println("Invalid address: " + addr);
			}
		}
		System.out.println("Local address is " + parent.getAddress().toString());
	}

	private void processPath(Iterator<String> tokens) {
		if (tokens.hasNext()) {
			String path = tokens.next();
			parent.setPath(path);
		}
		System.out.println("Path is " + parent.getPath());
	}

	private void processStatus(Iterator<String> tokens) {
		if (tokens.hasNext()) {
			try {
				UUID taskId = UUID.fromString(tokens.next());
				TaskStatus status = parent.getStatus(taskId);
				System.out.println("Task " + taskId.toString() + " status is " + status.toString());
			} catch (RemoteException e) {
				System.out.println("Could not connect to remote service.");
			}
		}
	}

	private void processTasks(Iterator<String> tokens) {
		Map<String, List<UUID>> tasks = parent.getTasks();
		List<String> l = new ArrayList<String>(tasks.keySet());
		Collections.sort(l);
		for (String filename : l) {
			System.out.println(filename + ":");
			for (UUID taskId : tasks.get(filename)) {
				String strId = taskId.toString();
				System.out.print("  " + strId);
				if (!tokens.hasNext() || !tokens.next().startsWith("nostatus")) {
					try {
						TaskStatus status = parent.getStatus(taskId);
						System.out.print(": " + status.toString());
					} catch (RemoteException e) {
						System.out.print("Could not connect to remote service.");
					}
				}
				System.out.print("\n");
			}
		}
	}

	private void processResults(Iterator<String> tokens) {
		if (tokens.hasNext()) {
			try {
				String filename = tokens.next();
				if (tokens.hasNext()) {
					String outputPath = tokens.next();
					parent.getResults(filename, outputPath);
				} else {
					parent.getResults(filename);
				}
			} catch (IOException e) {
				System.out.println("ERROR: " + e.getMessage());
			}

		} else {
			System.out.println("Invalid syntax: results FILENAME");
		}
	}

}
