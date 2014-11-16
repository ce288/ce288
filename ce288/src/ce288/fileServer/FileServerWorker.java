package ce288.fileServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileServerWorker implements Runnable {

	public final static Logger logger = LoggerFactory.getLogger(FileServerWorker.class);

	private static final int BUFSIZE = 8192;

	private Socket socket;

	private String path;

	public FileServerWorker(Socket socket, String path) {
		this.socket = socket;
		this.path = path;
	}

	@Override
	public synchronized void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

			String line = in.readLine();
			if (line == null) {
				logger.error("Received invalid file request: null");
				socket.close();
				return;
			}
			String filename = path + File.separatorChar + line;

			line = in.readLine();
			if (line == null) {
				logger.error("Received invalid position request: null");
				socket.close();
				return;
			}
			long pos = Long.parseLong(line);

			line = in.readLine();
			if (line == null) {
				logger.error("Received invalid length request: null");
				socket.close();
				return;
			}
			int length = Integer.parseInt(line);

			RandomAccessFile file = new RandomAccessFile(filename, "r");
			length = (int) Math.min(length, file.length() - pos);
			file.seek(pos);
			FileChannel channel = file.getChannel();
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(length);
			byte[] byteArray = new byte[BUFSIZE];
			int numRead, numGet;
			int totalRead = 0;
			while ((numRead = channel.read(byteBuffer)) != -1 || totalRead >= length) {
				if (numRead == 0) {
					continue;
				}
				totalRead += numRead;
				while (byteBuffer.hasRemaining()) {
					numGet = Math.min(byteBuffer.remaining(), BUFSIZE);
					byteBuffer.get(byteArray, 0, numGet);
					out.write(byteArray, 0, numGet);
				}
				byteBuffer.clear();
			}
			file.close();

			out.flush();
			out.close();
			socket.close();
			logger.info("Closed connection from {} after {} bytes.", socket.getInetAddress().getHostName(), totalRead);
		} catch (IOException | NumberFormatException e) {
			logger.error(e.getMessage(), e);
		}

	}

}
