package ce288.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.TaskRepository;
import ce288.tasks.TaskRepositoryInterface;

public class Server {

	public final static Logger logger = LoggerFactory.getLogger(Server.class);

	private TaskRepository tasks;

	public Server() {
		logger.info("Server started.");
		try {
			tasks = new TaskRepository();
			UnicastRemoteObject.unexportObject(tasks, true);
			TaskRepositoryInterface stub = (TaskRepositoryInterface) UnicastRemoteObject
					.exportObject(tasks, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("TaskRepository", stub);
		} catch (RemoteException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		new Server();
	}

}
