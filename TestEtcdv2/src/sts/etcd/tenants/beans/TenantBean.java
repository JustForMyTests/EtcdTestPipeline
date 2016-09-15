package sts.etcd.tenants.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class TenantBean {
	
	protected String id = null;
	protected Boolean enabled = true;
	protected Boolean deleted = false;
	protected Set<String> vhosts = new HashSet<String>();
	
	public TenantBean(String tenantString) {
		JSONParser parser = new JSONParser();
		try{
			JSONObject obj = (JSONObject) parser.parse(tenantString);
			id = (String)obj.get("id");
			enabled = (Boolean)obj.get("enabled");
			deleted = (Boolean)obj.get("deleted");
			JSONArray allVhosts = (JSONArray) obj.get("vhosts");
			for(int i=0; i< allVhosts.size(); i++){
				vhosts.add((String) allVhosts.get(i)); 
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public TenantBean() {
		
	}

	public String getID() {
		return id;
	}
	
	public Boolean isEnabled() {
		return enabled;
	}
	
	public Boolean isDeleted() {
		return deleted;
	}
	
	public List<String> getVhosts() {
		List<String> list = new ArrayList<String>();
		list.addAll(vhosts);
		return list;
	}
	
	public String setID(String theID) {
		id = theID;
		return id;
	}
	
	public Boolean setEnabled(Boolean enable) {
		enabled = enable;
		return enabled;
	}
	
	public Boolean setDeleted(Boolean delete) {
		deleted = delete;
		return deleted;
	}
	
	public Boolean addVHost(String newVhost) {
		return vhosts.add(newVhost);
	}
	
	public Boolean deleteVhost(String vhost) {
		return vhosts.remove(vhost);
	}
	
	public String toString(){
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("enabled", enabled);
		obj.put("deleted", deleted);
		JSONArray allVhosts = new JSONArray();
		allVhosts.addAll(vhosts);
		obj.put("vhosts", allVhosts);
		return obj.toString();
	}
}
