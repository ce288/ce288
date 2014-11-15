package ce288.client;

import java.io.IOException;

public class FileAnalyserException extends IOException {

	private static final long serialVersionUID = -1907715201816123884L;
	
	public FileAnalyserException(String message) {
		super(message);
	}
	
	public FileAnalyserException(String message, Throwable cause) {
		super(message, cause);
	}

}
