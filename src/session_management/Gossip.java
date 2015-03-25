package session_management;

import java.util.Random;

import rpc.RPCClient;

public class Gossip extends Thread{
	public static int GOSSIP_SECS = 60 * 1000;
	
	Gossip(){
		System.out.println("GOSSIP Initializing...");
	}
	
	public void run(){
		try {
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Random generator = new Random();
		
		while(true){
			String server = SSMServlet.serverViewTable.getServerToGossipWith();
			System.out.println("GOSSIP With " + server);
			SSMServlet.serverViewTable.upsertViewTableEntry(SSMServlet.network_address, true, System.currentTimeMillis());
			if(server.equals("DB")){
				/*
				 * Gossip with DB
				 */
				String db_entries = new String();
				db_entries = SimpleDBManager.getDBEntriesAsString();
				
				SSMServlet.serverViewTable.mergeViews(db_entries);
				
				SimpleDBManager.overwriteDB();
			}
			else{
				/*
				 * Gossip with server
				 */
				mergeViewTable(server);
			}
			
			try {
				Thread.sleep((GOSSIP_SECS/2) + generator.nextInt( GOSSIP_SECS ));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void mergeViewTable(String server){
		/*
		 * Pick up a random IP address, and call the RPC mergeViewsClient
		 */
		String remote_server_view_string = RPCClient.ExchangeViewsClient(SSMServlet.serverViewTable.toString(), server).trim();
		if(!remote_server_view_string.equals("ERROR") || !remote_server_view_string.equals("NULL")){
			SSMServlet.serverViewTable.mergeViews(remote_server_view_string);
		}
	}
}
