package EtcdTest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

// LogResponse is the converted STSEtcdResponse, into more usable entry
// Type "start" represents the invoking of the method, with no duration and result element
// Type "end" represents the return of the method.
public class LogResponse implements Comparable<LogResponse> {
	
	private String logFile = "TestLog";
	
	protected String type;
	protected int index;
	protected String action;
	protected String status;
	protected long time;
	protected String params;
	protected String nodeLocation;
	
	// For type "end"
	protected long duration;
	protected String result;
	// For reads, this is the index of the node in question
	// For create/setTenantmapping, this is for the modifiedIndex of the new vhost mapping
	protected long relIndex;
	// For create/setTenantmapping, this is for the modifiedIndex of the old tenantID mapping (if applicable)
	protected long oldRelIndex;
	// For create/setTenantmapping, this is for the modifiedIndex of the new tenantID mapping (if applicable)
	protected long newRelIndex;
	
	public LogResponse(String jsonObj, String ver, int theIndex) {
		JSONParser parser = new JSONParser();
		try{
			JSONObject obj = (JSONObject) parser.parse(jsonObj);
			action = (String)obj.get("action");
			type = ver;
			index = theIndex;
			if(ver.equals("start")) {
				time = (Long)obj.get("start");
			} else if (ver.equals("end")) {
				time = (Long)obj.get("end");
				duration = (Long)obj.get("duration");
				result = (String)obj.get("result");
				relIndex = (Long)obj.get("relIndex");
				if(obj.containsKey("oldRelindex")){
					oldRelIndex = (Long)obj.get("oldRelIndex");
				}
				if(obj.containsKey("newRelindex")){
					newRelIndex = (Long)obj.get("newRelIndex");
				}
			}
			params = (String)obj.get("params");
			nodeLocation = (String)obj.get("nodeLocation");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int compareTo(LogResponse another) {
		Long one = this.time;
		Long two = another.time;
		return one.compareTo(two);
	}
	
	public void printString() {
		String indexDetails = index + ">";
		String timeDetails = "" + time;
		String resultDetails = "---";
		if(type.equals("end")) {
			timeDetails += " [" + duration + "]";
			if(result == null){
				resultDetails = "null";
			} else {
				resultDetails = result;
			}
		}
		String locDetails = "@" + nodeLocation;
		String actionDetails = ":" + action;
		String paramDetails = params;
		
		System.out.format("%-5s%-21s%-18s%-8s%-25s%-40s\n", indexDetails, timeDetails, locDetails, actionDetails, paramDetails, resultDetails);
	}
	
}
