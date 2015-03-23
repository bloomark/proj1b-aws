package session_management;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;

public class ServerViewTable {
	//Thread safe session table
	protected static ConcurrentHashMap<String, ServerViewTableEntry> serverViewTable = new ConcurrentHashMap<String, ServerViewTableEntry>();
	
	//create or update and an server view table entry
	public void upsertViewTableEntry(String ipAddress, Boolean status, long timestamp)
	{
		if(serverViewTable.containsKey(ipAddress))//update
		{
			serverViewTable.get(ipAddress).updateViewTableEntry(status, timestamp);
		}
		else//create
		{
			ServerViewTableEntry newEntry = new ServerViewTableEntry(status, timestamp);
			serverViewTable.put(ipAddress, newEntry);
		}
	}
	
	/*
	 * Entire view table as a string for exchanging views
	 * format ipaddress1>status1+timestamp2,ipaddress2>status2+timestamp2,ipaddress3>status3+timestamp3
	 */
	@Override
	public String toString() {
				
		String returnString = new String();
		
		for(Map.Entry<String, ServerViewTableEntry> entry : ServerViewTable.serverViewTable.entrySet())
		{
			returnString += entry.getKey() + ">" + entry.getValue().toString() + ",";
		}
		
		if (returnString.length() > 0) //removing the last ,
		{
			returnString = returnString.substring(0, returnString.length()-1);
		}
		
		return returnString;
	}
	
	/*
	 * Merges a view as a string with the current view.
	 * ViewB needs to be in the toString format of ServerViewTbale class
	 */
	public void mergeViews(String viewB)
	{
		if(viewB == null || viewB.length() == 0)
			return;
		
		String[] ipToEntriesOfB = viewB.trim().split(java.util.regex.Pattern.quote(","));
		
		for(String ipEntry : ipToEntriesOfB)
		{
			//constructing view table entry of B	
			ServerViewTableEntry viewBEntry = constructTableEntryHelper(ipEntry);
			String ip = ipEntry.trim().split(java.util.regex.Pattern.quote(">"))[0];
					
			if(serverViewTable.containsKey(ip))
			{
				ServerViewTableEntry viewAEntry = serverViewTable.get(ip);
				if(viewBEntry.getTimestamp() > viewAEntry.getTimestamp())
					serverViewTable.put(ip, viewBEntry);
			}
			else
			{
				serverViewTable.put(ip, viewBEntry);
			}
		}
		
		System.out.println("Table after mreging - " + this.toString());
	}
	
	private ServerViewTableEntry constructTableEntryHelper(String entry)
	{
		String[] entries = entry.trim().split(java.util.regex.Pattern.quote(">"));
		
		String[] status_timestamp = entries[1].trim().split(java.util.regex.Pattern.quote(ServerViewTableEntry.DELIMITER));
		
		//Boolean status = Boolean.valueOf(status_timestamp[0]);
		Boolean status = null;
		if(status_timestamp[0].trim().equals("1")) status = true;
		else status = false;
		
		long timestamp = Long.valueOf(status_timestamp[1].trim());
		
		return new ServerViewTableEntry(status, timestamp);
	}
	
	public boolean isEmpty(){
		return serverViewTable.isEmpty();
	}
	
	public String getRandomKey(){
		/*
		 * From http://stackoverflow.com/questions/12385284/how-to-select-a-random-key-from-a-hashmap-in-java
		 */
		
		String randomIP = null;
		ArrayList<String> key_list = new ArrayList<String>();
		Random r = new Random();
		
		for(String key : serverViewTable.keySet()){
			if(serverViewTable.get(key).getStatus()){
				key_list.add(key);
			}
		}
		
		if(key_list.size() <= 1) return "NULL";
		
		do{
			randomIP = key_list.get(r.nextInt(key_list.size()));
			System.out.println("RandomIP = " + randomIP);
		} while(randomIP.equals(SSMServlet.network_address)); 
			
		return randomIP;
	}
}
