package rpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import session_management.SSMServlet;
import session_management.SessionData;

public class RPCServer extends Thread{
	public static int RPC_SERVER_PORT = 5300;
	public static int SOCKET_TIMEOUT = 2 * 1000;
	public static int MAX_PACKET_LENGTH = 512;
	public static String DELIMITER = SessionData.DELIMITER;
	
	DatagramSocket rpc_server_socket = null;
	
	public RPCServer(){
		System.out.println("RPC Server initializing...");
		try{
			rpc_server_socket = new DatagramSocket(RPC_SERVER_PORT);
		} catch(SocketException e){
			e.printStackTrace();
		}
		System.out.println("SERVER Initialized at Port " + rpc_server_socket.getLocalPort() + "...");
	}
	
	public void run(){
		while(true){
			String response_string = null;
			String response_value = null;
			String[] request_fields = null;
			byte[] outbuf = new byte[MAX_PACKET_LENGTH];
			byte[] inbuf = new byte[MAX_PACKET_LENGTH];
			DatagramPacket recv_pkt = new DatagramPacket(inbuf, inbuf.length);
			
			try{
				//TODO Server code
				/*
				 * Wait for RPC call from client on RPC_SERVER_PORT
				 * Look at the opcode and perform 
				 */
				System.out.println("SERVER Waiting for client request");
				rpc_server_socket.receive(recv_pkt);
				
				System.out.println("SERVER Received client request");
				
				/*
				 * request_fields[] - Split the message received in the datagram on the DELIMITER
				 * Indexes
				 * [0] - callId
				 * [1] - operation_code
				 */
				SSMServlet.serverViewTable.upsertViewTableEntry(recv_pkt.getAddress().getHostAddress(), true, System.currentTimeMillis());
				request_fields = sanitizeMessage(recv_pkt.getData());
				switch(Integer.valueOf(request_fields[1])){
					case 0:
						//Session Read
						// [2] - sessionId
						System.out.println("SERVER Received sessionRead request from server at " + recv_pkt.getAddress().getHostAddress() + "for sessionId #" + request_fields[2]);
						response_value = sessionRead(request_fields[2]);
						break;
					case 1:
						//Session Write
						// [2] - sessionId
						// [3] - sessionData in String format
						System.out.println("SERVER Received sessionWrite request from server at " + recv_pkt.getAddress().getHostAddress() + "for sessiondata ("  + request_fields[2] +") " + request_fields[3] + DELIMITER + request_fields[4] + DELIMITER + request_fields[5]);
						response_value = sessionWrite(request_fields[2], request_fields[3] + DELIMITER + request_fields[4] + DELIMITER + request_fields[5]);
						break;
					case 2:
						//Exchange Views
						// [2] - Remote server's view
						SSMServlet.serverViewTable.upsertViewTableEntry(SSMServlet.network_address, true, System.currentTimeMillis());
						System.out.println("SERVER Received viewMerge request from server at " + recv_pkt.getAddress().getHostAddress() + "for view ("  + request_fields[2] +")");
						response_value = mergeViews(request_fields[2]);
						break;
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			
			//Build and send response
			response_string = request_fields[0] + DELIMITER + response_value;
			outbuf = response_string.getBytes();
			InetAddress client_address = recv_pkt.getAddress();
			System.out.println("SERVER sending response string " + response_string + " to client IP "+ client_address.toString() + ":" + recv_pkt.getPort());
			DatagramPacket send_pkt = new DatagramPacket(outbuf, outbuf.length, client_address, recv_pkt.getPort());
			try {
				rpc_server_socket.send(send_pkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private String sessionRead(String sessionId){
		System.out.println("SERVER Received read request for #" + sessionId + "tralalala");
		synchronized(SSMServlet.sessionMap){
			if(SSMServlet.sessionMap.containsKey(sessionId)){
				/*
				 * Map has relevant data, just return toString of the data
				 */
				System.out.println("SERVER In sessionRead #" + sessionId + " is " + SSMServlet.sessionMap.get(sessionId).toString());
				return SSMServlet.sessionMap.get(sessionId).toString();
			}
			else {
				System.out.println("SERVER sessionID #" + sessionId + " not found");
				return "NULL";
			}
		}
	}
	
	private String sessionWrite(String sessionId, String sessionData){
		SessionData new_table_entry = new SessionData(sessionData);
		SSMServlet.sessionMap.put(sessionId, new_table_entry);
		
		return "OK";
	}
	
	private String mergeViews(String remote_server_view){
		SSMServlet.serverViewTable.mergeViews(remote_server_view);
		if(SSMServlet.serverViewTable.isEmpty()){
			return "NULL";
		}
		else{
			return SSMServlet.serverViewTable.toString();
		}
	}
	
	private String[] sanitizeMessage(byte[] inbuf){
		String request_string = null;
		try {
			request_string = new String(inbuf, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] request_fields = request_string.split(DELIMITER);
		
		for(int i=0; i<request_fields.length; i++){
			request_fields[i] = request_fields[i].trim();
		}
		
		return request_fields;
	}
}