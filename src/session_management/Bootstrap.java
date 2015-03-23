package session_management;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

public class Bootstrap extends Thread{
	
	Bootstrap(){
		SSMServlet.serverViewTable.upsertViewTableEntry(SSMServlet.network_address, true, System.currentTimeMillis());
	}
	
	public void run(){
		String db_entries = new String();
		db_entries = SimpleDBManager.getDBEntriesAsString();
		
		SSMServlet.serverViewTable.mergeViews(db_entries);
		
		SimpleDBManager.overwriteDB();
	}
}
