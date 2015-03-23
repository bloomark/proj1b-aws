package session_management;

import java.util.ArrayList;

import aj.org.objectweb.asm.Attribute;

import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;

public class SimpleDBManager {
	public static String attribute = SSMServlet.attribute;
	public static String domain = SSMServlet.domain;
	
	public static boolean insertIntoSimpleDB(String address, String status, long lastSeen){
		ReplaceableAttribute replaceableAttribute = new ReplaceableAttribute()
		.withName(attribute)
		.withValue(status + "+" + String.valueOf(lastSeen))
		.withReplace(true);

		SSMServlet.db.putAttributes(new PutAttributesRequest().withDomainName(domain)
				.withItemName(address)
				.withAttributes(replaceableAttribute));
		
		return true;
	}
	
	public static String getDBEntriesAsString(){
		String db_entries = new String();
		String server_status = null;
		
		String qry = "select * from `" + domain + "`";
		SelectRequest selectRequest = new SelectRequest(qry);
		for (Item item : SSMServlet.db.select(selectRequest).getItems()) {
			for (com.amazonaws.services.simpledb.model.Attribute attribute : item.getAttributes()){
				server_status = attribute.getValue();
			}
			db_entries += item.getName() + ">" + server_status + ",";
		}
		
		//removing the last ,
		if (db_entries.length() > 0) {
			db_entries = db_entries.substring(0, db_entries.length()-1);
		}
		
		return db_entries;
	}
	
	public static boolean overwriteDB(){
		ArrayList<String> key_list = new ArrayList<String>(SSMServlet.serverViewTable.serverViewTable.keySet());
		
		for(String key : key_list){
			ServerViewTableEntry view_entry = SSMServlet.serverViewTable.serverViewTable.get(key);
			String status = (view_entry.getStatus()) ? "1" : "0";
			insertIntoSimpleDB(key, status, view_entry.getTimestamp());
		}
		
		return true;
	}
}
