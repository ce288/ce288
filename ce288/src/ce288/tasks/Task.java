package ce288.tasks;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

public class Task implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1645817821321314542L;

	private UUID id;

	private FileFormat format;

	private String filename;

	private long position;

	private long length;
	
	private InetAddress location;

	public Task() {
		this.id = UUID.randomUUID();
	}
	
	public Task(FileFormat format, InetAddress location, String filename, long position, long length) {
		this();
		this.format = format;
		this.location = location;
		this.filename = filename;
		this.position = position;
		this.length = length;
	}

	public FileFormat getFormat() {
		return format;
	}

	public void setFormat(FileFormat format) {
		this.format = format;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public InetAddress getLocation() {
		return location;
	}

	public void setLocation(InetAddress location) {
		this.location = location;
	}

	public UUID getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return getId().toString();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Task) {
			Task other = (Task) obj;
			return other.id.equals(this.id);
		}
		return false;
	}
}
