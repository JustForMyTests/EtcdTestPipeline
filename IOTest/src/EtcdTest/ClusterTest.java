package EtcdTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


public class ClusterTest {
	
	public static void main (String[] args) throws Exception {
		if(args.length == 0){
			throw new Exception("Not enough arguments!");
		} else {
			ExecutorService executor = Executors.newFixedThreadPool(args.length);
			List<ClusterThread> allThreads = new ArrayList<ClusterThread>();
			long start = System.currentTimeMillis();
			for(int i=0; i< args.length; i++){
				BufferedReader file = new BufferedReader(new FileReader(args[i]));
				List<String> fileArray = new ArrayList<String>();
				String type = file.readLine().trim();
				String temp;
				while ((temp = file.readLine()) != null) {
					if(!temp.isEmpty()) {
						fileArray.add(temp);
					}
				}
				file.close();
				ClusterThread thread = new ClusterThread(type, fileArray, start);
				allThreads.add(thread);
			}
			for(int i=0; i < allThreads.size(); i++){
				executor.execute(allThreads.get(i));
			}
			executor.shutdown();
			while(!executor.isTerminated()) {
				//wait til all tasks are complete
			}
			System.out.println("All threads have finished executing");
		}
	}
	
}

class ClusterThread implements Runnable {
	String theType;
	List<String> allValues;
	long testStart;
	int counter = 0;
	
	String prefix = "http://localhost:9080/mgt/csfim/v1/tenants/";
	String[] threeNodes = {"localhost:4101", "localhost:4201", "localhost:4301"};
	
	CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public ClusterThread(String type, List<String> toDo, long startTime) throws Exception {
		allValues = toDo;
		testStart = startTime;
		theType = type;
	}
	
	public void run() {
		CloseableHttpResponse response = null;
		try {
			for(int i=0; i < allValues.size(); i++) {
				int rand = (int) Math.floor(Math.random() * 3);
				String address = threeNodes[rand];
				String currentCmd = allValues.get(i);
				long start = System.currentTimeMillis() - testStart;
				if(theType.equals("read")){
					response = httpClient.execute(new HttpGet(URI.create(prefix + "vhost/" + currentCmd + "?address=" + address)));
				} else if(theType.equals("write")) {
					String[] value = currentCmd.split(" ");
					String tID = value[0];
					String vh = value[1];
					response = httpClient.execute(new HttpPut(URI.create(prefix + vh + "/vhost/" + tID + "?address=" + address)));
					Thread.sleep(2);
				} else if (theType.equals("account")) {
					response = httpClient.execute(new HttpGet(URI.create(prefix + currentCmd + "/account" + "?address=" + address)));
				} else {
					throw new Exception(theType + ": Not a valid command!");
				}
				String result = "NULL";
				HttpEntity ent = response.getEntity();
				long end = System.currentTimeMillis() - testStart;
				if (ent != null) {
					result = "";
					InputStream in = ent.getContent();
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String output;
					while ((output = br.readLine()) != null) {
						result += output;
					}
				}
				System.out.println("[" + start + " - " + end + "] Node 4" + (rand+1) + "01 > " + theType + " " + currentCmd + ": The result is " + result);
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			try {
				if(response != null) {
					response.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

