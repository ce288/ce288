package ce288.tasks;

public enum FileFormat {
	IAGA_DHZF("DHZF"), 
	IAGA_XYZF("XYZF"), 
	EMBRACE("EMBRACE"), 
	UNIVAP("UNIVAP"), 
	IAGA_XYZG("XYZG");
	
	private String mark;
	
	private FileFormat(String mark) {
		this.mark = mark;
	}
	
	public String getMark() {
		return mark;
	}
}
