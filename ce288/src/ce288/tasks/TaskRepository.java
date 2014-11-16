package ce288.tasks;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ce288.tasks.Result.ResultLog;

public class TaskRepository extends UnicastRemoteObject implements TaskRepositoryInterface {

	private static final long serialVersionUID = -5529240669926777932L;

	public final static Logger logger = LoggerFactory.getLogger(TaskRepository.class);

	public static final long TASK_TIMEOUT = 100000;

	/**
	 * Store the Tasks that are being processed by some Client.
	 * 
	 * The keys are the Task ids, and the values are objects of the class ExecutionInfo. Each 
	 * ExecutionInfo contains the Client which got the Task, the time when it occured, the Task 
	 * object itself, and the timeout of the processing.
	 */
	private Map<UUID, ExecutionInfo> executingTasks;

	/**
	 * Store the Tasks that are available for the Clients to process.
	 * 
	 * Each new Task, added by the method addTask(), are appended to the tail of the list. Each 
	 * Client gets the Task at the head of the list with getNext(). When the processing fails, 
	 * either due a timeout or because of a notified failure by setFailure(), the Task returns at
	 * the head of the list to be processed as quickly as possible.
	 */
	private LinkedList<Task> pendingTasks;
	
	/**
	 * Store the result logs of each Task.
	 * 
	 * The keys are the Task ids, and the values are a list of ResultLog. 
	 */
	private Map<UUID, List<ResultLog>> results;

	/**
	 * Object used to control the access to the executingTasks, pendingTasks and results. 
	 */
	private static Object lock = new Object();

	public TaskRepository() throws RemoteException {
		super();
		executingTasks = new HashMap<UUID, ExecutionInfo>();
		pendingTasks = new LinkedList<Task>();
		results = new HashMap<UUID, List<ResultLog>>();
		new Thread(new ExpirationWatchdog(this, 2500)).start();

	}

	@Override
	public void addTask(Task task) throws RemoteException {
		synchronized (lock) {
			pendingTasks.add(task);
			logger.info("Added task {}", task);
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
				logger.info("Client {} executing task {}.", clientId, task);
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
				results.put(taskId, result.getLogs());
			}
		}
		logger.info("Client {} finished task {}.", clientId, taskId);
	}

	@Override
	public List<ResultLog> getResult(List<UUID> taskIds) throws RemoteException {
		ArrayList<ResultLog> list = new ArrayList<>();
		for (UUID taskId : taskIds) {
			if (results.containsKey(taskId)) {
				list.addAll(results.get(taskId));
				results.remove(taskId);
				logger.info("Removed task {}", taskId);
			} else {
				throw new RemoteException("There is no result for task ID " + taskId.toString());
			}
		}
		return list;
	}

	@Override
	public void setFailure(UUID clientId, UUID taskId, String msg) throws RemoteException {
		synchronized (lock) {
			if (executingTasks.containsKey(taskId)) {
				ExecutionInfo info = executingTasks.remove(taskId);
				pendingTasks.addFirst(info.getTask());
			}
		}
		logger.info("Client {} failed task {}.", clientId, taskId);
	}

	@Override
	public TaskStatus getStatus(UUID taskId) throws RemoteException {
		if (executingTasks.containsKey(taskId)) {
			return TaskStatus.EXECUTING;
		} else if (results.containsKey(taskId)) {
			return TaskStatus.FINISHED;
		} else {
			for (Task task : pendingTasks) {
				if (task.getId().equals(taskId)) {
					return TaskStatus.PENDING;
				}
			}
			return TaskStatus.FAILED;
		}
	}

}
