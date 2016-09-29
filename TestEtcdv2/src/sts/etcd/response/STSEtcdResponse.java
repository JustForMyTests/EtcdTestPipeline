package sts.etcd.response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;

public class STSEtcdResponse {
	
	private String logFile;
	
	protected String action;
	protected String status;
	protected long start;
	protected long end;
	protected long duration;
	protected String params;
	protected String nodeLocation;
	protected String result;
	protected boolean isSet;
	
	// For create/setTenantmapping, this is for the modifiedIndex of the new vhost mapping
	protected long relIndex;
	// For create/setTenantmapping, this is for the modifiedIndex of the old tenantID mapping (if applicable)
	protected long oldRelIndex;
	// For create/setTenantmapping, this is for the modifiedIndex of the new tenantID mapping (if applicable)
	protected long newRelIndex;
	
	public STSEtcdResponse(String theAction, String theParams, long theStart, String location) {
		action = theAction;
		params = theParams;
		start = theStart;
		nodeLocation = location;
		isSet = false;
		System.out.println(System.getProperty("user.dir") + "/TestLog");
		logFile = System.getProperty("user.dir") + "/TestLog";
	}
	
	public void setResult(String theResult, long theRelIndex, long theEnd) {
		result = theResult;
		relIndex = theRelIndex;
		end = theEnd;
		duration = end - start;
	}
	
	public void setExtraIndices(long oldI, long newI) {
		oldRelIndex = oldI;
		newRelIndex = newI;
		isSet = true;
	}
	
	public String toResponse() {
		String timeDetails = "[" + start + " - " + end + "[" + duration + "]]";
		String locDetails = "@" + nodeLocation;
		String actionDetails = action + " on " + params + ":";
		String resultDetails;
		if(result == null){
			resultDetails = "none";
		} else {
			resultDetails = result;
		}
		return timeDetails + " " + locDetails + " " + actionDetails + " " + resultDetails;
	}
	
	public void toLog() throws IOException{
		BufferedWriter br = new BufferedWriter(new FileWriter(logFile, true));
		
		JSONObject obj = new JSONObject();
		obj.put("action", action);
		obj.put("start", start);
		obj.put("end", end);
		obj.put("duration", duration);
		obj.put("params", params);
		obj.put("nodeLocation", nodeLocation);
		obj.put("result", result);
		obj.put("relIndex", relIndex);
		if(isSet){
			obj.put("oldRelIndex", oldRelIndex);
			obj.put("newRelIndex", newRelIndex);
		}
		
		
		br.write(obj.toString() + "\n");
		br.close();
	}
	
}
