package ce288.client;

import ce288.tasks.FileFormat;

public class AbstractFileAnalyserFactory {

	public static AbstractFileAnalyser getAnalyser(FileFormat format) {
		switch (format) {
		case EMBRACE:
			return new FileEmbrace();
			
		case IAGA_XYZF:
			return new DumbAnalyser();
			
		case IAGA_DHZF:
			return new DumbAnalyser();
			
		case IAGA_XYZG:
			return new DumbAnalyser();
			
		default:
			return new DumbAnalyser();
		}
	}
}
