package ce288.tasks;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRepository extends UnicastRemoteObject implements TaskRepositoryInterface {

	private static final long serialVersionUID = -5529240669926777932L;

	public final static Logger logger = LoggerFactory.getLogger(TaskRepository.class);

	public static final long TASK_TIMEOUT = 100000;

	private Map<UUID, ExecutionInfo> executingTasks;

	private Queue<Task> pendingTasks;

	private static Object lock = new Object();

	public TaskRepository() throws RemoteException {
		super();
		executingTasks = new HashMap<UUID, ExecutionInfo>();
		pendingTasks = new LinkedList<Task>();
		new Thread(new ExpirationWatchdog(this, 2500)).start();

	}

	@Override
	public void addTask(Task task) throws RemoteException {
		synchronized (lock) {
			pendingTasks.add(task);
		}
	}

	@Override
	public Task getNext(UUID clientId) throws RemoteException {
		Task task;
		synchronized (lock) {
			task = pendingTasks.poll();
			if (task != null) {
				long now = System.currentTimeMillis();
				ExecutionInfo info = new ExecutionInfo(now, now + TASK_TIMEOUT, clientId, task);
				executingTasks.put(task.getId(), info);
				logger.info("Client {} executing task {}.", clientId, task.getId());
			}
		}
		return task;
	}

	public void removeExpired() {
		synchronized (lock) {
			Set<UUID> keys = executingTasks.keySet();
			Iterator<UUID> iter = keys.iterator();
			while (iter.hasNext()) {
				UUID key = iter.next();
				if (executingTasks.get(key).isExpired()) {
					ExecutionInfo info = executingTasks.remove(key);
					logger.info("Task {} being executed by {} expired.", key, info.getClientId());
				}
			}
		}
	}

	@Override
	public void setResult(UUID clientId, UUID taskId, Result result) throws RemoteException {
		synchronized (lock) {
			if (executingTasks.containsKey(taskId)) {
				executingTasks.remove(taskId);
			}
		}
		logger.info("Client {} finished task {}.", clientId, taskId);
	}

	@Override
	public void setFailure(UUID clientId, UUID taskId, String msg) throws RemoteException {
		logger.info("Client {} failed task {}.", clientId, taskId);
	}

}
