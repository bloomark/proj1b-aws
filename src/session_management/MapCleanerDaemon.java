package session_management;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;

public class MapCleanerDaemon extends TimerTask{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Running cleaner daemon...");
		
		synchronized(SSMServlet.sessionMap){
			if(SSMServlet.sessionMap != null){
				Iterator<String> itr = SSMServlet.sessionMap.keySet().iterator();
				ArrayList<String> sessionList = new ArrayList<String>();
				while(itr.hasNext()){
					String sessionID = itr.next();
					long ts = System.currentTimeMillis();
					if(SSMServlet.sessionMap.get(sessionID).getExpiresOn() <= ts){
						//Cookie has expired, delete it
						sessionList.add(sessionID);
					}
				}
				for(String sessionID : sessionList){
					SSMServlet.sessionMap.remove(sessionID);
					System.out.println("Removed session #" + sessionID);
				}
			}
		}
	}
}
