package session_management;

public class SessionData {
	public static String DELIMITER = "_";
	int version;
	String message;
	long expiresOn;
	
	public SessionData(int version, String message, long expiresOn){
		this.version = version;
		this.message = message;
		this.expiresOn = expiresOn;
	}
	
	public String toString(){
		return String.valueOf(version) + DELIMITER + message + DELIMITER + String.valueOf(expiresOn);
	}
	
	public SessionData(String session_data){
		String[] data_fields = session_data.split(DELIMITER);
		
		this.version = Integer.valueOf(data_fields[0].trim());
		this.message = data_fields[1].trim();
		this.expiresOn = Long.valueOf(data_fields[2].trim()); 
	}
	
	public int getVersion(){
		return version;
	}
	
	public void setVersion(int version){
		this.version = version;
	}
	
	public String getMessage(){
		return message;
	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public long getExpiresOn(){
		return expiresOn;
	}
	
	public void setExpiresOn(long expiresOn){
		this.expiresOn = expiresOn;
	}
}
