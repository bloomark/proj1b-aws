package session_management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
//import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

import rpc.RPCClient;
import rpc.RPCServer;

/**
 * Servlet implementation class SSMServlet
 */
public class SSMServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	//Cookie details
	public static long TIMEOUT = 30 * 1000; //Timeout in milliseconds
	public static String COOKIE_NAME = "CS5300PROJ1SESSION";
	
	//Session Stuff
	public static long DELTA = 1 * 1000;
	public static String globalSessionId = "0";
	public static ConcurrentHashMap<String, SessionData> sessionMap = new ConcurrentHashMap<String, SessionData>();
	public static long cleanerDaemonInterval = 150 * 1000;
	public static ServerViewTable serverViewTable = new ServerViewTable();
	public static String network_address = null;
	public static String DELIMITER = SessionData.DELIMITER;
	
	//AWS stuff
	public static String accessKey = "AKIAJQ5Q3LCIYWRDWZTQ";
	public static String secretKey = "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";
	public static String domain = "View";
	public static String attribute = "server_status";
	
	//Connect to simpleDB
	public static AWSCredentials awsCredentials = new BasicAWSCredentials(SSMServlet.accessKey, SSMServlet.secretKey);
    public static AmazonSimpleDB db = new AmazonSimpleDBClient(awsCredentials);
    
    //Fields for K-Resiliency
    public static int NUMBER_BACKUPS = 2;
    public static int NUMBER_BACKUP_REQUESTS = NUMBER_BACKUPS + 1;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
	}
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public SSMServlet() {
        super();
        // TODO Auto-generated constructor stub
        do{
        	getNetworkAddress();
        } while(network_address == null);
        System.out.println("SERVLET Local network address = " + network_address + "...");        
        
        System.out.println("SERVLET Bootstrapping server...");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.run();
        
        System.out.println("SERVLET Setting up cleaner task...");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MapCleanerDaemon(), 5*1000, cleanerDaemonInterval);
        
        System.out.println("SERVLET Starting RPC Server...");
        RPCServer rpc_server = new RPCServer();
        rpc_server.start();
        
        System.out.println("SERVLET Starting Gossip Thread...");
        Gossip gossip = new Gossip();
        gossip.start();        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Session Data
		String sessionID = null;
		Integer version = 1;
		long expiresOn = 0;
		String primary = null;
		String backup = null;
		String cookieContent = null;
		Cookie cookie = null;
		String[] backup_servers = null;
		
		PrintWriter o = response.getWriter();
		String action = request.getParameter("btn-submit");
		Boolean createNewCookie = true;
		
		synchronized(sessionMap){
			//Check to see if a cookie exists
			Cookie[] cookieList = request.getCookies();
			
			if(cookieList != null){
				for(int i=0; i<cookieList.length; i++){
					if(cookieList[i].getName().equals(COOKIE_NAME)){
						cookie = cookieList[i];
					}
				}
			}
			
			if(cookie != null){
				//A cookie exists
				createNewCookie = false;
				cookieContent = cookie.getValue();
				//String[] stringList = cookieContent.split(DELIMITER);
				String[] stringList = cookieContent.split(DELIMITER, 4);
				sessionID = stringList[0];
				version = Integer.parseInt(stringList[1]);
				version++;
				primary = stringList[2].trim();
				backup_servers = stringList[3].trim().split(DELIMITER);
				
				if(!primary.equals(network_address) && !stringList[3].trim().contains(network_address)){
					sessionMap.remove(sessionID);
					for(String backup_server : backup_servers){
						if(backup_server.trim().equals("NULL")) continue;
						
						SessionData remote_entry = readRemoteSessionData(sessionID, backup_server);
						
						if(remote_entry == null) continue;
						else sessionMap.put(sessionID, remote_entry);
					}
				}
				
				if(sessionMap.containsKey(sessionID)){
					//We know about this cookie
					//Check timeout
					long cookieTime = sessionMap.get(sessionID).getExpiresOn();
					long ts = System.currentTimeMillis();
					if(cookieTime > ts){
						//Cookie has not timed out
						sessionMap.get(sessionID).setExpiresOn(System.currentTimeMillis() + TIMEOUT);
						if(action != null){
							if(action.equals("Replace")){
								//Replace was clicked
								String newMessage = request.getParameter("newMessage");
								if(newMessage != null){
									if(newMessage.length() > 256){
										newMessage = newMessage.substring(0, 255);
									}
								}
								else{
									newMessage = " ";
								}
								sessionMap.get(sessionID).setMessage(newMessage);
							}
							else if(action.equals("Refresh")){
								//Refresh was clicked
							}
							else if(action.equals("Logout")){
								//Logout was clicked
								sessionMap.remove(sessionID);
								cookieContent = sessionID + DELIMITER + version + DELIMITER + "NULL" + DELIMITER + "NULL";
								Cookie session = new Cookie(COOKIE_NAME, cookieContent);
								session.setMaxAge(0);
								response.addCookie(session);
								o.println("Logged out!");
								return;
							}
						}
					}
					else{
						//Cookie has expired, create a new one
						createNewCookie = true;
					}
				}
				else{
					//Unknown cookie, create a new one
					sessionMap.remove(sessionID);
					cookieContent = sessionID + DELIMITER + version + DELIMITER + "NULL" + DELIMITER + "NULL";
					Cookie session = new Cookie(COOKIE_NAME, cookieContent);
					session.setMaxAge(0);
					response.addCookie(session);
					o.println("Failed to retrieve session info. :(");
					return;
				}
			}
			
			if(createNewCookie){
				version = 1;
				sessionID = getNewSessionId();
				//Increment timestamp by TIMEOUT milliseconds
				expiresOn = System.currentTimeMillis() + TIMEOUT;
				SessionData newTableEntry = new SessionData(1, "Hello, User!", expiresOn);
				sessionMap.put(sessionID, newTableEntry);
			}
			
			backup = writeRemoteSessionData(sessionID, sessionMap.get(sessionID));
			//System.out.println("BACKUP = " + backup);
			
			cookieContent = sessionID + DELIMITER + version + DELIMITER + network_address + DELIMITER + backup;
			Cookie session = new Cookie(COOKIE_NAME, cookieContent);
			session.setMaxAge((int)(TIMEOUT/1000));
			response.addCookie(session);
		}
		
		request.setAttribute("expiresOn", new Timestamp(sessionMap.get(sessionID).getExpiresOn()));
		request.setAttribute("message", sessionMap.get(sessionID).getMessage());
		request.getRequestDispatcher("index.jsp").forward(request, response);
		return;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	private String getNewSessionId(){
		globalSessionId = String.valueOf(Integer.valueOf(globalSessionId) + 1);
		return network_address + "." + globalSessionId;
	}
	
	private SessionData readRemoteSessionData(String sessionId, String server){
		String new_session_string = null;
		boolean contact_backup = false;
		
		if(!server.equals("NULL")){
			//There is a primary that we can contact
			new_session_string = RPCClient.SessionReadClient(sessionId, server).trim();
			if(new_session_string.equals("NULL") || new_session_string.equals("ERROR")){
				return null;
			}
		}
		else{
			return null;
		}
		
		//We have session data in a string, convert into an object of sessionData and return
		return new SessionData(new_session_string);
	}
	
	private String writeRemoteSessionData(String sessionId, SessionData sessionData){
		/*
		 * Pick a random server from the view, for now lets make it 127.0.0.1
		 * String server = random_entry_from_view
		 */
		
		String backup_server_string = new String();
		int successful_backups = 0;
		
		ArrayList<String> key_list = new ArrayList<String>();
		
		for(String key : ServerViewTable.serverViewTable.keySet()){
			if(ServerViewTable.serverViewTable.get(key).getStatus()){
				key_list.add(key);
			}
		}
		key_list.remove(network_address);
		//System.out.println("KEY LIST = " + key_list.toString());
		
		if(key_list.size() <= 0){
			for(int i=0; i<NUMBER_BACKUPS; i++){
				backup_server_string += "NULL_";
			}
			backup_server_string = backup_server_string.substring(0, backup_server_string.length()-1);
			//System.out.println("RETURNING " + backup_server_string);
			return backup_server_string;
		}
		
		long seed = System.nanoTime();
		Collections.shuffle(key_list, new Random(seed));
		
		//System.out.println("SHUFFLED KEY LIST = " + key_list.toString());
		
		int i=0;
		while(successful_backups < NUMBER_BACKUPS && i < key_list.size()){
			sessionData.expiresOn = System.currentTimeMillis() + TIMEOUT + DELTA;
			String result = RPCClient.SessionWriteClient(sessionId, sessionData.toString(), key_list.get(i)).trim();
			
			if(result.equals("OK")){
				backup_server_string += key_list.get(i) + DELIMITER;
				successful_backups++;
			} 
			i++;
		}
		
		if(successful_backups < NUMBER_BACKUPS){
			for(i=0; i<NUMBER_BACKUPS-successful_backups; i++)
				backup_server_string += "NULL" + DELIMITER;
		}
		
		if (backup_server_string.length() > 0) 
			backup_server_string = backup_server_string.substring(0, backup_server_string.length()-1);
		
		return backup_server_string;
	}
	
	public void getNetworkAddress(){
		/*
		 * From http://stackoverflow.com/questions/2939218/getting-the-external-ip-address-in-java
		 */
		URL whatismyip = null;
		try {
			whatismyip = new URL("http://checkip.amazonaws.com");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            network_address = in.readLine();
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
}
