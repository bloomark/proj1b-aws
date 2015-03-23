package session_management;

public class ServerViewTableEntry {
	Boolean status;
	long timestamp;
	public static String DELIMITER = "+";
	
	public ServerViewTableEntry(Boolean status, long time)
	{
		this.status = status;
		this.timestamp = time;
	}

	public Boolean getStatus() {
		return this.status;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public void setStatus(Boolean status) {
		this.status = status;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public void updateViewTableEntry(Boolean status, long timestamp){
		this.setStatus(status);
		this.setTimestamp(timestamp);
	}
	
	@Override
	public String toString() {
		if(this.status)
			return "1" + DELIMITER + String.valueOf(this.timestamp); 
		else
			return "0" + DELIMITER + String.valueOf(this.timestamp);
	}
}
