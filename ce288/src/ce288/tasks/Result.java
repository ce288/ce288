package ce288.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Result implements Serializable {

	private static final long serialVersionUID = -4098289025269204995L;

	private final UUID taskId;

	private ArrayList<ResultLog> logs;

	public Result(UUID taskId) {
		logs = new ArrayList<ResultLog>();
		this.taskId = taskId;
	}

	public void addLog(long pos, String msg) {
		logs.add(new ResultLog(pos, msg));
	}

	public List<ResultLog> getLogs() {
		return logs;
	}

	public UUID getTaskId() {
		return taskId;
	}

	public class ResultLog implements Serializable, Comparable<ResultLog> {

		private static final long serialVersionUID = -7315606130594463203L;
		private long pos;
		private String msg;

		public ResultLog(long pos, String msg) {
			this.pos = pos;
			this.msg = msg;
		}

		public long getPos() {
			return pos;
		}

		public void setPos(long pos) {
			this.pos = pos;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}

		@Override
		public int hashCode() {
			return (int) (pos) + msg.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ResultLog) {
				ResultLog other = (ResultLog) obj;
				if (other.pos == this.pos && other.msg.equals(this.msg)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int compareTo(ResultLog o) {
			if (o.pos != pos) {
				return (int) (o.pos - pos);
			} else {
				return msg.compareTo(o.msg);
			}
		}

	}
}
