package ce288.tasks;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import ce288.tasks.Result.ResultLog;

public interface TaskRepositoryInterface extends Remote {
	
	public Task getNext(UUID clientId) throws RemoteException;
	
	public void setResult(UUID clientId, UUID taskId, Result result) throws RemoteException;
	
	public List<ResultLog> getResult(List<UUID> taskIds) throws RemoteException;
	
	public void setFailure(UUID clientId, UUID taskId, String msg) throws RemoteException;
	
	public void addTask(Task task) throws RemoteException;
	
	public TaskStatus getStatus(UUID taskId) throws RemoteException;
	
}
