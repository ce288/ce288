package ce288.fileServer;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			System.out.println("> ");
			line = in.nextLine().trim().toLowerCase();
			Iterator<String> tokens = Arrays.asList(line.split("\\s+")).iterator();
			String command = tokens.next();
			if (command.startsWith("exit")) {
				System.out.println("Bye.");
				break;
			} else if (command.startsWith("add")) {
				processAdd(tokens);
			} else if (command.startsWith("ip")) {
				processIp(tokens);
			} else if (command.startsWith("path")) {
				processPath(tokens);
			} else if (command.startsWith("help")) {
				System.out.println("Command list:");
				System.out.println("  add path [section_size]");
				System.out.println("  ip [new_ip]");
				System.out.println("  path [new_path]");
			} else {
				System.out.println("Invalid command, type help to display all available commands.");
			}
		}
		in.close();
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
			} catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
			} catch (RemoteException e) {
				System.out.println("Could not connect to remote service.");
				logger.error(e.getMessage(), e);
			}
		} else {
			System.out.println("Invalid syntax: add FILENAME");
		}
	}
	
	private void processPath(Iterator<String> tokens) {
		if (tokens.hasNext()) {
			String path = tokens.next();
			parent.setPath(path);
		}
		System.out.println("Path is " + parent.getPath());
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

}
