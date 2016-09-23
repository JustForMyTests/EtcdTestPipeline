package EtcdTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
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

public class IOTest {
	static class Control {
	    public volatile String currentTenantID;
	}
	final static Control var = new Control();
	
	final static String logFile = "./server-files/wlp/usr/servers/defaultServer/TestLog";

	static class MyThread implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}

	static class WriteThread extends MyThread {
		String theType;
		List<String> allValues;
		int total = 0;
		int start=0;
		String[] cluster;
		String prefix = "http://localhost:9080/mgt/csfim/v1/tenants/";
		String logSuffix = "?log=simple";
		String addrSuffix = "&address=";
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		public WriteThread(int num, int theStart, String[] theCluster) {
			total = num;
			start = theStart;
			cluster = theCluster;
		}
		
		public void run() {
			CloseableHttpResponse response = null;
			String vh = "V";
			String tID = "T";
			int count;
			try {
				int rand = (int) Math.floor(Math.random() * cluster.length);
				String address = cluster[rand];
				// Write V0 -> T 0123401234...
				for(int i = 0; i < total; i++){
					count = i%5 + start;
					vh = "V0";
					//tID = "TXXXXXXXX" + count;
					tID = "T" + count;
					var.currentTenantID = tID;
					response = httpClient.execute(new HttpPut(URI.create(prefix + tID + "/vhost/" + vh + logSuffix + addrSuffix + address)));
					Thread.sleep((long)50);
					HttpEntity ent = response.getEntity();
					if(ent != null) {
						ent.getContent().close();
					}
				}
			} catch (Exception e){
				e.printStackTrace();
			} finally {
				try {
					if(response != null) {
						response.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	// Fix the vhost to V0
	static class ReadThread extends MyThread {
		String type;
		int total = 0;
		String[] cluster;
		String prefix = "http://localhost:9080/mgt/csfim/v1/tenants/";
		String logSuffix = "?log=simple";
		String addrSuffix = "&address=";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		public ReadThread(String theType, int num, String[] theCluster){
			type = theType;
			total = num;
			cluster = theCluster;
		}
		
		public void run() {
			CloseableHttpResponse response = null;
			try {
				for(int i=0; i< total; i++){
					int rand = (int) Math.floor(Math.random() * cluster.length);
					String address = cluster[rand];
					// Continuously read from V0
					if(type.equals("vhost")) {
						String vh = "V0";
						response = httpClient.execute(new HttpGet(URI.create(prefix + "vhost/" + vh + logSuffix + addrSuffix + address)));
					}
					// eg. T 0011223344
					else if(type.equals("tenant")) {
						String tID = var.currentTenantID;
						response = httpClient.execute(new HttpGet(URI.create(prefix + tID + "/vhost" + logSuffix + addrSuffix + address)));
					}
					HttpEntity ent = response.getEntity();
					if(ent != null){
						ent.getContent().close();
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					if(response != null) {
						response.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/***************************          Main         ******************************/
	
	public static void main (String[] args) throws Exception {
		//System.out.println("This file is in " + System.getProperty("user.dir") + " or " + new File(".").getCanonicalPath());
		// Delete file content in logFile
		PrintWriter pw = new PrintWriter(new File(logFile).getCanonicalPath());
		pw.close();
		
		//Start threads to run CRUD on the database
		ExecutorService executor = Executors.newFixedThreadPool(3);
		
		List<MyThread> allThreads = new ArrayList<MyThread>();
		// A thread for writing, a thread for reading from the /vhost directory, and another to read from the /tenant directory
		allThreads.add(new WriteThread(100, 0, args));
		allThreads.add(new WriteThread(100, 5, args));
		allThreads.add(new ReadThread("vhost", 500, args));
		//allThreads.add(new ReadThread("tenant", 500, args));
		
		for(int i=0; i< allThreads.size(); i++) {
			executor.execute(allThreads.get(i));
		}
		
		executor.shutdown();
		while(!executor.isTerminated()) {
			//wait til all tasks are complete
		}
		System.out.println("All threads have finished executing");
		
		// Then read from the log file and interpret the results
		BufferedReader br = new BufferedReader(new FileReader(new File(logFile).getCanonicalPath()));
		// Interpret results
		ArrayList<LogResponse> allResponses = new ArrayList<LogResponse>();
		int index = 0;
		String temp;
		while ((temp = br.readLine()) != null) {
			if(!temp.isEmpty()) {
				allResponses.add(new LogResponse(temp, "start", index));
				allResponses.add(new LogResponse(temp, "end", index));
				index ++;
			}
		}
		// Sort all responses by time
		Collections.sort(allResponses);
		
		// Process the list of responses, sequentially, calculating mean/max/stddev and check for consistency in CRUD
		HashMap<String, ArrayList<Long>> allDurations = new HashMap<String, ArrayList<Long>>();
		int[] listOfCorrectV = new int[4];
		int[] listOfWrongV = new int[4];
		int[] listOfCorrectT = new int[4];
		int[] listOfWrongT = new int[4];
		HashMap<Integer, Integer> listOfWrongIndices = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> resStarted = new HashMap<Integer, Integer>();
		int writeStartedIndex = -1;	// -1 to represent the current response is not in between a write action
		String expected = "";
		
		for(int i=0; i< allResponses.size(); i++){
			LogResponse res = allResponses.get(i);
			// Record the duration of each different method (to calculate mean, etc)
			int resIndex = res.index;
			String action = res.action;
			String type = res.type;
			boolean isStart = type.equals("start");
			if(!isStart) {//Only 'end' responses hold the duration
				long duration = res.duration;
				ArrayList<Long> list = allDurations.get(action);
				if(list == null){
					ArrayList<Long> newList = new ArrayList<Long>();
					newList.add(duration);
					allDurations.put(action, newList);
				} else {
					list.add(duration);
					allDurations.put(action, list);
				}
			}
			if(action.equals("setVT")) {//If the action was a 'write'
				// If the current response is the start/end of a 'write' action
				if(isStart){
					writeStartedIndex = resIndex;
					expected = res.params.split(" ")[1];
				} else {
					writeStartedIndex = -1;
				}
			} else if (action.equals("readV")) {
				if(isStart) {
					if(writeStartedIndex > 0){
						// Note down a read action that started after the start of a write action (either type 2 or 3)
						resStarted.put(resIndex, 2);
					} else {
						// Note down a read action that started before the start of a write action (either type 0 or 1)
						resStarted.put(resIndex, 0);
					}
				} else {
					// Remove the response's index from the table
					int currentType = resStarted.remove(resIndex);
					if(res.result != null){
						if(writeStartedIndex > 0) {  // The response ends inside a write segment
							if(currentType == 0) {	//Response started before/after a write segment, ended inside
								//Type 1 response
								if(res.result.equals(expected)){
									listOfCorrectV[1]++;
								} else {
									listOfWrongV[1]++;
									listOfWrongIndices.put(resIndex, 1);
								}
							} else {
								//Type 2 response
								if(res.result.equals(expected)){
									listOfCorrectV[2]++;
								} else {
									listOfWrongV[2]++;
									listOfWrongIndices.put(resIndex, 2);
								}
							}
						} else {	// The response ends after/before a write segment
							if(currentType == 2) {	//Response started in write segment, ended outside
								//Type 3 response
								if(res.result.equals(expected)){
									listOfCorrectV[3]++;
								} else {
									listOfWrongV[3]++;
									listOfWrongIndices.put(resIndex, 3);
								}
							} else {
								//Type 0 response
								if(res.result.equals(expected)){
									listOfCorrectV[0]++;
								} else {
									listOfWrongV[0]++;
									listOfWrongIndices.put(resIndex, 0);
								}
							}
						}
					}
				}
			} else if(action.equals("readT")) {
				if(isStart) {
					if(writeStartedIndex > 0){
						// Note down a read action that started after the start of a write action (either type 2 or 3)
						resStarted.put(resIndex, 2);
					} else {
						// Note down a read action that started before the start of a write action (either type 0 or 1)
						resStarted.put(resIndex, 0);
					}
				} else {
					// Remove the response's index from the table
					int currentType = resStarted.remove(resIndex);
					if(res.result != null){
						if(writeStartedIndex > 0) {  // The response ends inside a write segment
							if(currentType == 0) {	//Response started before/after a write segment, ended inside
								//Type 1 response
								if(res.params.equals(expected)){
									if(res.result.contains("\"V0\"")){
										listOfCorrectT[1]++;
									} else {
										listOfWrongT[1]++;
										listOfWrongIndices.put(resIndex, 1);
									}
								}
							} else {
								//Type 2 response
								if(res.params.equals(expected)){
									if(res.result.contains("\"V0\"")){
										listOfCorrectT[2]++;
									} else {
										listOfWrongT[2]++;
										listOfWrongIndices.put(resIndex, 2);
									}
								}
							}
						} else {	// The response ends after/before a write segment
							if(currentType == 2) {	//Response started in write segment, ended outside
								//Type 3 response
								if(res.params.equals(expected)){
									if(res.result.contains("\"V0\"")){
										listOfCorrectT[3]++;
									} else {
										listOfWrongT[3]++;
										listOfWrongIndices.put(resIndex, 3);
									}
								}
							} else {
								//Type 0 response
								if(res.params.equals(expected)){
									if(res.result.contains("\"V0\"")){
										listOfCorrectT[0]++;
									} else {
										listOfWrongT[0]++;
										listOfWrongIndices.put(resIndex, 0);
									}
								}
							}
						}
					}
				}
			}
			// Print out the response
			res.printString();
		}
		System.out.println("");
		for(int i=0; i< 4; i++){
			System.out.format("%-10s%-5s%-10s\n", "readV", "" + i, listOfCorrectV[i] + "/" + listOfWrongV[i]);
			System.out.format("%-10s%-5s%-10s\n", "readT", "" + i, listOfCorrectT[i] + "/" + listOfWrongT[i]);
		}
		
		//Calculate the mean
		HashMap<String, Double> meanMap = new HashMap<String, Double>();
		HashMap<String, Long> medianMap = new HashMap<String, Long>();
		for(Map.Entry<String, ArrayList<Long>> entry : allDurations.entrySet()){
			String currentAction = entry.getKey();
			ArrayList<Long> currentList = entry.getValue();
			long median = getMedian(currentList);
			long total = 0;
			for(int i=0; i< currentList.size(); i++) {
				total += currentList.get(i);
			}
			double mean = total/(double)currentList.size();
			meanMap.put(currentAction, mean);
			medianMap.put(currentAction, median);
		}
		//Calculate standard deviation and max value
		System.out.println("");
		for(Map.Entry<String, ArrayList<Long>> entry : allDurations.entrySet()){
			String currentAction = entry.getKey();
			ArrayList<Long> currentList = entry.getValue();
			long max = 0;
			double totalVar = 0;
			double mean = meanMap.get(currentAction);
			long median = medianMap.get(currentAction);
			for(int i=0; i< currentList.size(); i++) {
				long num = currentList.get(i);
				max = Math.max(num, max);
				totalVar = totalVar + (num - mean) * (num - mean);
			}
			double stddev = Math.sqrt(totalVar/currentList.size());
			System.out.format("%-10s\n%-10s\n%-10s\n%-10s\n%-10s\n", 
					currentAction + " ->", "Mean: " + mean, "Median: " + median, "Max: " + max, "StdDev: " + String.format("%.5g", stddev));
		}
		System.out.println("");
		for(Map.Entry<Integer, Integer> entry : listOfWrongIndices.entrySet()){
			System.out.println(entry.getKey() + " (" + entry.getValue() + ")");
		}
		br.close();
		
	}
	
	// Helper functions

	public static long getMedian(List<Long> theList) {
		Collections.sort(theList);
		int size = theList.size();
		long median = 0;
		if(size%2 == 0) {
			median = (theList.get(size/2) + theList.get(size/2 - 1))/2;
		} else {
			median = theList.get((size-1)/2);
		}
		return median;
	}
	
}





