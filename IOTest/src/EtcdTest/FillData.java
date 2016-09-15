package EtcdTest;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class FillData {
	
	static int vhostNum = 0;
	static int tenantNum = 0;
	static String address = "";
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static SecureRandom rnd = new SecureRandom();
	static final String prefix = "http://localhost:9080/mgt/csfim/v1/tenants/";
	
	public static void main (String[] args) throws Exception {
		if(args.length != 2) {
			throw new Exception("Error in usage: FillData [address] [vhostNum]");
		} else {
			address = args[0];
			vhostNum = Integer.parseInt(args[1]);
			tenantNum = vhostNum/10;
		}
		CloseableHttpClient httpClient = HttpClients.createDefault();
		//To fill a number of data into the database
		fillUpData(httpClient, 0);
		
		//To fill the database to the brim
		//fillToCapacity(httpClient, 1);
		
		//To fill up the value in a key-value pair
		//fillMultValue(httpClient, 1);
		
		System.out.println("All done!");
		httpClient.close();
	}
	
	public static String randomString( int len ){
	   StringBuilder sb = new StringBuilder( len );
	   for( int i = 0; i < len; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length())));
	   return sb.toString();
	}
	
	public static void fillUpData(CloseableHttpClient httpClient, int startVhostID) {
		int vhostID = startVhostID;
		for(int i=0; i< vhostNum; i++){
			int num = vhostNum/20;
			if(i != 0 && i%num == 0){
				System.out.println("Completed: " + i/num + "/20");
			}
			try{
				//Randomly generated 8-char string for vhost and randomly generated number between 0 and 99 for tenantID
				//vhostName = randomString(8);
				int tenantID = (int)Math.floor(Math.random() * tenantNum);
				
				//Eg. "http://localhost:9080/mgt/csfim/v1/tenants/T67/vhost/
				HttpPut newMap = 
						new HttpPut(URI.create(prefix + "T" + tenantID + "/vhost/" + "V" + vhostID + "?address=" + address));
				CloseableHttpResponse response = httpClient.execute(newMap);
				HttpEntity ent = response.getEntity();
				if(ent != null){
					ent.getContent().close();
				}
				vhostID++;
			} 
			catch (Exception e){
				e.printStackTrace();
			}
			
		}
	}
	
	public static void fillToCapacity(CloseableHttpClient httpClient, int startIndex){
		int num = startIndex;
		long start = System.currentTimeMillis();
		String vPrefix = "VXXXXXXXXX";	//10-byte total vhost
		String tPrefix = "TXXXXXXXXX";	//10-byte total tenant
		while(true){
			if(num%10000 == 0){
				long end = System.currentTimeMillis();
				float dur = ((float)(end - start))/1000;
				start = end;
				System.out.println("Completed: " + num + " in " + dur + "seconds");
			}
			try{
				int charLength = String.valueOf(num).length();
				if(charLength > 9) {
					throw new Exception("Too small size!");
				}
				String vhostPrefix = vPrefix.substring(0, 10-charLength);
				String tenantPrefix = tPrefix.substring(0, 10-charLength);
				//Eg. "http://localhost:9080/mgt/csfim/v1/tenants/TXXXXXXX12/vhost/VXXXXXXX12
				HttpPut newMap = 
						new HttpPut(URI.create(prefix + tenantPrefix + num + "/vhost/" + vhostPrefix + num + "?address=" + address));
				CloseableHttpResponse response = httpClient.execute(newMap);
				HttpEntity ent = response.getEntity();
				if(ent != null){
					ent.getContent().close();
				}
				num++;
			} 
			catch (Exception e){
				e.printStackTrace();
				System.err.println("Final Count: " + num);
				break;
			}
		}
	}
	
	public static void fillUpValue(CloseableHttpClient httpClient, int startIndex) {
		int num = startIndex;
		long start = System.currentTimeMillis();
		String vPrefix = "VXXXXXXXXX";	//10-byte total vhost
		String tID = "TXXXXXXXX0";	//10-byte total tenant
		while(true){
			if(num%1000 == 0){
				long end = System.currentTimeMillis();
				float dur = ((float)(end - start))/1000;
				start = end;
				System.out.println("Completed: " + num + " in " + dur + "seconds");
			}
			try{
				int charLength = String.valueOf(num).length();
				if(charLength > 9) {
					throw new Exception("Too small size!");
				}
				String vhostPrefix = vPrefix.substring(0, 10-charLength);
				//Eg. "http://localhost:9080/mgt/csfim/v1/tenants/TXXXXXXXX0/vhost/VXXXXXXX12
				HttpPut newMap = 
						new HttpPut(URI.create(prefix + tID + "/vhost/" + vhostPrefix + num + "?address=" + address));
				CloseableHttpResponse response = httpClient.execute(newMap);
				HttpEntity ent = response.getEntity();
				if(ent != null){
					ent.getContent().close();
				}
				num++;
			}
			catch (Exception e) {
				e.printStackTrace();
				System.err.println("Final Count: " + num);
				break;
			}
		}
	}
	
	public static void fillMultValue(CloseableHttpClient httpClient, int startIndex) {
		int num = startIndex;
		long start = System.currentTimeMillis();
		String vPrefix = "VXXXXXXXXX";	//10-byte total vhost
		String tPrefix = "TXXXXXXXXX";	//10-byte total tenant
		while(true){
			int tNum = num/2400;
			if(num%1000 == 0){
				long end = System.currentTimeMillis();
				float dur = ((float)(end - start))/1000;
				start = end;
				System.out.println("Completed: " + num + " in " + dur + "seconds");
			}
			if(num%2400 == 0){
				System.out.println(tNum + "iteration completed ...");
			}
			try{
				int charLength = String.valueOf(num).length();
				if(charLength > 9) {
					throw new Exception("Too small size!");
				}
				String vhostPrefix = vPrefix.substring(0, 10-charLength);
				String tenantPrefix = tPrefix.substring(0, 10-String.valueOf(tNum).length());
				//Eg. "http://localhost:9080/mgt/csfim/v1/tenants/TXXXXXXXX0/vhost/VXXXXXXX12
				HttpPut newMap = 
						new HttpPut(URI.create(prefix + tenantPrefix + tNum + "/vhost/" + vhostPrefix + num + "?address=" + address));
				CloseableHttpResponse response = httpClient.execute(newMap);
				HttpEntity ent = response.getEntity();
				if(ent != null){
					ent.getContent().close();
				}
				num++;
			}
			catch (Exception e) {
				e.printStackTrace();
				System.err.println("Final Count: " + num);
				break;
			}
		}
	}
	
}
