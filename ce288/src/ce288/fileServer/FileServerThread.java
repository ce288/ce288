package ce288.fileServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileServerThread implements Runnable {

	public static final Logger logger = LoggerFactory.getLogger(FileServerThread.class);

	private FileServer parent;

	public FileServerThread(FileServer parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		Executor executor = Executors.newCachedThreadPool();
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(FileServer.PORT);
			while (true) {
				Socket socket = serverSocket.accept();
				logger.info("Received connection from {}.", socket.getInetAddress());
				executor.execute(new FileServerWorker(socket, parent.getPath()));
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
