package sts.etcd.tenants;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import mousio.etcd4j.responses.EtcdResponse;
import sts.etcd.response.STSEtcdResponse;
import sts.etcd.tenants.beans.TenantBean;

@Path("/tenants")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TenantCollection {
	
	/* Connects to the already set-up etcd server by "rebootEtcd.sh"
	 * 
	 * Append a query parameter address for the location of the database
	 * Otherwise, the default address is "localhost:4001"
	 * 
	 * Eg. .../mgt/csfim/v1/tenants/vhost?address=localhost:4101
	 * 
	 * Contains 2 directories, '/vhosts' and '/tenants'
	 * 
	 * '/vhosts' maps the virtual host name to its tenant ID
	 * 
	 * '/tenants' maps the tenantID to the tenantBean stored as JSON
	 */
	
	//@DefaultValue("4001") @QueryParam("address") String address;
	private EtcdClient client;
	private String location;
	private String logLevel;
	private boolean toPrint;
	
	public TenantCollection(
			@DefaultValue("localhost:4001") @QueryParam("address") String address, 
			@DefaultValue("none") @QueryParam("log") String log,
			@DefaultValue("false") @QueryParam("print") boolean print
			){
		location = address;
		logLevel = log;
		toPrint = print;
		URI clientURI = URI.create("http://" + location);
		client = new EtcdClient(clientURI);
	}
	
	String VHOST_DIR = "/vhosts/";
	String TENANT_DIR = "/tenants/";
	
	/* Returns the tenantID of the virtual host
	 * 
	 * Eg.: GET http://localhost:9080/mgt/csfim/v1/tenants/vhost/tralala.com
	 * 
	 * Returns: String tenantID "tenantID"
	 * 		OR: null if none
	 */
	@GET
	@Path("vhost/{vhostName}")
	public String getTenantID(@PathParam("vhostName") String vhostName) throws IOException, EtcdAuthenticationException, TimeoutException {
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("readV", vhostName, start, location);
		String tenantID = null;
		long relIndex = -1;
		
		try {
			EtcdKeysResponse result = client.get(VHOST_DIR + vhostName).send().get();
			relIndex = result.node.modifiedIndex;
			tenantID = result.node.value;
			
		}
		// no such key in the database
		catch(EtcdException e) {
			
		}
		res.setResult(tenantID, relIndex, System.currentTimeMillis());
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return tenantID;
	}
	
	/* Returns all vhosts
	 * 
	 * Eg.: GET http://localhost:9080/mgt/csfim/v1/tenants/vhost
	 * 
	 * Returns: JSONArray.toString() representation of the list of vhost(s)
	 * 		OR: "[]" if none
	 */
	@GET
	@Path("vhost")
	public String getAllVhosts() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
		List<String> list = new ArrayList<String>();
		JSONArray allVhosts = new JSONArray();
		
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("readAllV", "database", start, location);
		EtcdKeysResponse response = client.getDir(VHOST_DIR).recursive().send().get();
		
		long relIndex = response.node.modifiedIndex;
		List<EtcdNode> listOfNodes = response.node.nodes;
		for(int i=0; i< listOfNodes.size(); i++){
			EtcdNode node = listOfNodes.get(i);
			if(!node.key.equals(VHOST_DIR)){
				list.add(node.key.replace(VHOST_DIR, ""));
			}		
		}
		allVhosts.addAll(list);
		String result = allVhosts.toString();
		long end = System.currentTimeMillis();
		res.setResult(result, relIndex, end);
		if(logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return result;
	}
	
	/* Returns all tenantID(s)
	 * 
	 * Eg.: GET http://localhost:9080/mgt/csfim/v1/tenants
	 * 
	 * Returns: List<String> of tenantID(s)
	 * 		OR: empty list if none
	 */
	@GET
	public String getAllTenants() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
		List<String> list = new ArrayList<String>();
		JSONArray allTenants = new JSONArray();
		
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("readAllT", "database", start, location);
		EtcdKeysResponse response = client.getDir(TENANT_DIR).recursive().send().get();
		
		long relIndex = response.node.modifiedIndex;
		List<EtcdNode> listOfNodes = response.node.nodes;
		if(listOfNodes != null){
			for(int i=0; i< listOfNodes.size(); i++){
				EtcdNode node = listOfNodes.get(i);
				if(!node.key.equals(TENANT_DIR)){
					list.add(node.key.replace(TENANT_DIR, ""));
				}
			}
		}
		
		allTenants.addAll(list);
		String result = allTenants.toString();
		long end = System.currentTimeMillis();
		res.setResult(result, relIndex, end);
		if(logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return result;
	}
	
	/* Returns all vhost(s) that have the tenantID
	 * 
	 * Eg.: GET http://localhost:9080/mgt/csfim/v1/tenants/xyz/vhost
	 * 
	 * Returns: List<String> of tenantID(s) 
	 * 		OR: empty list if not available
	 */
	@GET
	@Path("{tenantID}/vhost")
	public String getVhostsOfTenant(@PathParam("tenantID") String tenantID) throws IOException, EtcdAuthenticationException, TimeoutException {
		List<String> list = new ArrayList<String>();
		JSONArray allVhostsOfTenant = new JSONArray();
		
		long start = System.currentTimeMillis();
		long relIndex = -1;
		long end = 0;
		String result = "[]";
		STSEtcdResponse res = new STSEtcdResponse("readT", tenantID, start, location);
		try{
			EtcdKeysResponse response = client.get(TENANT_DIR + tenantID).send().get();
			relIndex = response.node.modifiedIndex;
			String tenantString = response.node.value;
			TenantBean tenant = new TenantBean(tenantString);
			list = tenant.getVhosts();
		}
		// No such tenantID
		catch (EtcdException e) {
			relIndex = -1;
		}
		
		allVhostsOfTenant.addAll(list);
		result = allVhostsOfTenant.toString();
		end = System.currentTimeMillis();
		res.setResult(result, relIndex, end);
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return result;
	}
	
	/* Gets the tenant's account details
	 * 
	 * Eg.: GET http://localhost:9080/mgt/csfim/v1/tenants/xyz/account
	 * 
	 * Returns: String details of {tenantID}
	 */
	@GET
	@Path("{tenantID}/account")
	public String getTenantDetails(@PathParam("tenantID") String tenantID) throws IOException, EtcdAuthenticationException, TimeoutException {
		String tenantString = null;
		long relIndex = -1;
		long end;
		
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("readTAcc", tenantID, start, location);
		try{
			EtcdKeysResponse response = client.get(TENANT_DIR + tenantID).send().get();
			relIndex = response.node.modifiedIndex;
			tenantString = response.node.value;
		}
		catch (EtcdException e){
			
		}
		end = System.currentTimeMillis();
		res.setResult(tenantString, relIndex, end);
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return tenantString;
	}
	
	/* Creates the mapping of the vhost to the tenantID
	 * 
	 * Eg.: POST http://localhost:9080/mgt/csfim/v1/tenants/xyz/vhost/tralala.com
	 * 
	 * Returns: String created mapping "{vhost}:{tenantID}"
	 */
	@POST
	@Path("{tenantID}/vhost/{vhostName}")
	public String createTenantMapping(@PathParam("tenantID") String tenantID, @PathParam("vhostName") String vhostName) throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException  {
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("createVT", vhostName + " " + tenantID, start, location);
		TenantBean usr = null;
		long oldTenantIndex = -1;
		long newTenantIndex = -1;
		String result = null;
		
		EtcdKeysResponse response = client.put(VHOST_DIR + vhostName, tenantID).send().get();
		long relIndex = response.node.modifiedIndex;
		EtcdNode prevNode = response.prevNode;
		// If the previous value is not null (new vhost mapping) 
		// Or if it is not the same value as tenantID (no changes made)
		// update the tenantID entry
		if(prevNode != null){
			String prevTenantID = prevNode.value;
			if(!prevTenantID.equals(tenantID)){
				String prevTenantString = client.get(TENANT_DIR + prevTenantID).send().get().node.value;
				usr = new TenantBean(prevTenantString);
				usr.deleteVhost(vhostName);
				response = client.put(TENANT_DIR + prevTenantID, usr.toString()).send().get();
				oldTenantIndex = response.node.modifiedIndex;
			}
		}
		// Check if the 'tenantID' already exists
		try {
			String newTenantString = client.get(TENANT_DIR + tenantID).send().get().node.value;
			usr = new TenantBean(newTenantString);
			if(oldTenantIndex >= 0){
				result = "Updated existing vhost mapping to an existing tenantID";
			} else {
				result = "Added new vhost to an existing tenantID";
			}
		}
		// If not, create a new tenant
		catch (EtcdException e) {
			usr = new TenantBean();
			usr.setID(tenantID);
			if(oldTenantIndex >= 0){
				result = "Updated existing vhost mapping with a new tenantID";
			} else {
				result = "Created a new vhost and tenantID mapping";
			}
		} 
		usr.addVHost(vhostName);
		response = client.put(TENANT_DIR + tenantID, usr.toString()).send().get();
		newTenantIndex = response.node.modifiedIndex;
		
		res.setResult(result, relIndex, System.currentTimeMillis());
		res.setExtraIndices(oldTenantIndex, newTenantIndex);
		if(logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return vhostName + ":" + tenantID;
	}
	
	/* Creates the mapping of the vhost to the tenantID
	 * 
	 * Eg.: PUT http://localhost:9080/mgt/csfim/v1/tenants/xyz/vhost/tralala.com
	 * 
	 * Returns: String created mapping "{vhost}:{tenantID}"
	 */
	@PUT
	@Path("{tenantID}/vhost/{vhostName}")
	public String setTenantMapping(@PathParam("vhostName") String vhostName, @PathParam("tenantID") String tenantID) throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("setVT", vhostName + " " + tenantID, start, location);
		TenantBean usr = null;
		long oldTenantIndex = -1;
		long newTenantIndex = -1;
		String result = null;
		
		EtcdKeysResponse response = client.put(VHOST_DIR + vhostName, tenantID).send().get();
		long relIndex = response.node.modifiedIndex;
		EtcdNode prevNode = response.prevNode;
		// If the previous value is not null (new vhost mapping) 
		// Or if it is not the same value as tenantID (no changes made)
		// update the tenantID entry
		if(prevNode != null){
			String prevTenantID = prevNode.value;
			if(!prevTenantID.equals(tenantID)){
				String prevTenantString = client.get(TENANT_DIR + prevTenantID).send().get().node.value;
				usr = new TenantBean(prevTenantString);
				usr.deleteVhost(vhostName);
				response = client.put(TENANT_DIR + prevTenantID, usr.toString()).send().get();
				oldTenantIndex = response.node.modifiedIndex;
			}
		}
		// Check if the 'tenantID' already exists
		try {
			String newTenantString = client.get(TENANT_DIR + tenantID).send().get().node.value;
			usr = new TenantBean(newTenantString);
			if(oldTenantIndex >= 0){
				result = "Updated existing vhost mapping to an existing tenantID";
			} else {
				result = "Added new vhost to an existing tenantID";
			}
		}
		// If not, create a new tenant
		catch (EtcdException e) {
			usr = new TenantBean();
			usr.setID(tenantID);
			if(oldTenantIndex >= 0){
				result = "Updated existing vhost mapping with a new tenantID";
			} else {
				result = "Created a new vhost and tenantID mapping";
			}
		} 
		usr.addVHost(vhostName);
		//System.err.println(usr.toString().getBytes().length);
		response = client.put(TENANT_DIR + tenantID, usr.toString()).send().get();
		newTenantIndex = response.node.modifiedIndex;
		
		res.setResult(result, relIndex, System.currentTimeMillis());
		res.setExtraIndices(oldTenantIndex, newTenantIndex);
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return vhostName + ":" + tenantID;
	}
	
	/* Deletes the mapping of the vhost to the tenantID
	 * 
	 * Eg.: DELETE http://localhost:9080/mgt/csfim/v1/tenants/xyz/vhost/tralala.com
	 * 
	 * Returns: true if mapping can be deleted
	 * 		OR: false if mapping cannot be deleted
	 */
	@DELETE
	@Path("{tenantID}/vhost/{vhostName}")
	public Boolean deleteTenantMapping(@PathParam("tenantID") String tenantID, @PathParam("vhostName") String vhostName) throws IOException, EtcdAuthenticationException, TimeoutException  {
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("delVT", vhostName + " " + tenantID, start, location);
		long relIndex  = -1;
		
		//check if this mapping exists
		Boolean op = false;
		try{
			EtcdKeysResponse response = client.delete(VHOST_DIR + vhostName).prevValue(tenantID).send().get();
			relIndex = response.node.modifiedIndex;
			String tenantString = client.get(TENANT_DIR + tenantID).send().get().node.value;
			TenantBean tenant = new TenantBean(tenantString);
			tenant.deleteVhost(vhostName);
			client.put(TENANT_DIR + tenantID, tenant.toString()).send();
			op = true;
		}
		// No such mapping to delete in database
		catch (EtcdException e) {
			
		}
		res.setResult(op.toString(), relIndex, System.currentTimeMillis());
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return op;
	}
	
	/* Deletes the tenant from the directory
	 * 
	 * Eg.: DELETE http://localhost:9080/mgt/csfim/v1/tenants/xyz
	 * 
	 * Returns: true if tenant can be deleted
	 * 		OR: false if tenant cannot be deleted
	 */
	@DELETE
	@Path("{tenantID}")
	public Boolean deleteTenant(@PathParam("tenantID") String tenantID) throws IOException, EtcdAuthenticationException, TimeoutException {
		long start = System.currentTimeMillis();
		STSEtcdResponse res = new STSEtcdResponse("delT", tenantID, start, location);
		long relIndex = -1;
		//Test
		Boolean op = false;
		try{
			//Delete the entry in the /tenants directory
			EtcdKeysResponse response = client.delete(TENANT_DIR + tenantID).send().get();
			relIndex = response.prevNode.modifiedIndex;
			String tenantString = response.prevNode.value;
			//Then check through all vhosts mapping to the tenantID and delete them
			TenantBean usr = new TenantBean(tenantString);
			List<String> listOfVhosts = usr.getVhosts();
			for(int i=0; i< listOfVhosts.size(); i++){
				client.delete(VHOST_DIR + listOfVhosts.get(i)).send();
			}
			op = true;
		}
		catch (EtcdException e) {
			
		}
		res.setResult(op.toString(), relIndex, System.currentTimeMillis());
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}
		client.close();
		return op;
	}
	
	@PUT
	@Path("{tenantID}/vhost/{vhostName}/ttl/{timetl}")
	public String setTenantMappingTTL(
			@PathParam("vhostName") String vhostName, 
			@PathParam("tenantID") String tenantID,
			@PathParam("timetl") String timetl)
					throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
		long start = System.currentTimeMillis();
		int theTTL = Integer.parseInt(timetl);
		STSEtcdResponse res = new STSEtcdResponse("setVT", vhostName + " " + tenantID + " ttl:" + timetl, start, location);
		TenantBean usr = null;
		long oldTenantIndex = -1;
		long newTenantIndex = -1;
		String result = null;
		/*System.out.println("Step 1");
		EtcdKeysResponse response = client.put(VHOST_DIR + vhostName, tenantID).send().get();
		System.out.println("Step 2");
		response = client.put(VHOST_DIR + vhostName, tenantID).ttl(theTTL).prevExist(true).send().get();
		System.out.println("Step 3");*/
		EtcdKeysResponse response = client.put("vhosts/foo", "bar").send().get();

	    response = client.put("vhosts/foo2", "bar").prevExist(false).send().get();

	    response = client.put("vhosts/foo", "bar1").ttl(40).prevExist(true).send().get();
		/*if(response.node.expiration == null){
			System.out.println("yes");
		} else {
			System.out.println("no");
		}
		/*long relIndex = response.node.modifiedIndex;
		EtcdNode prevNode = response.prevNode;
		// If the previous value is not null (new vhost mapping) 
		// Or if it is not the same value as tenantID (no changes made)
		// update the tenantID entry
		if(prevNode != null){
			String prevTenantID = prevNode.value;
			if(!prevTenantID.equals(tenantID)){
				String prevTenantString = client.get(TENANT_DIR + prevTenantID).send().get().node.value;
				usr = new TenantBean(prevTenantString);
				usr.deleteVhost(vhostName);
				response = client.put(TENANT_DIR + prevTenantID, usr.toString()).ttl(theTTL).send().get();
				oldTenantIndex = response.node.modifiedIndex;
			}
		}
		// Check if the 'tenantID' already exists
		try {
			String newTenantString = client.get(TENANT_DIR + tenantID).send().get().node.value;
			usr = new TenantBean(newTenantString);
			if(oldTenantIndex >= 0){
				result = "Updated existing vhost mapping to an existing tenantID";
			} else {
				result = "Added new vhost to an existing tenantID";
			}
		}
		// If not, create a new tenant
		catch (EtcdException e) {
			usr = new TenantBean();
			usr.setID(tenantID);
			if(oldTenantIndex >= 0){
				result = "Updated existing vhost mapping with a new tenantID";
			} else {
				result = "Created a new vhost and tenantID mapping";
			}
		} 
		usr.addVHost(vhostName);
		System.err.println("TESTTESTETSTETSTETSTETSTETSTETSTETSTETST");
		response = client.put(TENANT_DIR + tenantID, usr.toString()).ttl(theTTL).send().get();
		newTenantIndex = response.node.modifiedIndex;
		
		res.setResult(result, relIndex, System.currentTimeMillis());
		res.setExtraIndices(oldTenantIndex, newTenantIndex);
		if(logLevel.equals("simple") || logLevel.equals("all")){
			res.toLog();
		}
		if(toPrint){
			System.out.println(res.toResponse());
		}*/
		client.close();
		return vhostName + ":" + tenantID + " for " + timetl + "sec";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
