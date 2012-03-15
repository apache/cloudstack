package com.cloud.consoleproxy;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * 
 * @author Kelven Yang
 * ConsoleProxyGCThread does house-keeping work for the process, it helps cleanup log files,
 * recycle idle client sessions without front-end activities and report client stats to external
 * management software 
 */
public class ConsoleProxyGCThread extends Thread {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyGCThread.class);
	
	private final static int MAX_SESSION_IDLE_SECONDS = 180;

	private Hashtable<String, ConsoleProxyClient> connMap;
	private long lastLogScan = 0;
	
	public ConsoleProxyGCThread(Hashtable<String, ConsoleProxyClient> connMap) {
		this.connMap = connMap;
	}
	
	private void cleanupLogging() {
		if(lastLogScan != 0 && System.currentTimeMillis() - lastLogScan < 3600000)
			return;
		
		lastLogScan = System.currentTimeMillis();
		
		File logDir = new File("./logs");
		File files[] = logDir.listFiles();
		if(files != null) {
			for(File file : files) {
				if(System.currentTimeMillis() - file.lastModified() >= 86400000L) {
					try {
						file.delete();
					} catch(Throwable e) {
					}
				}
			}
		}
	}
	
	@Override
    public void run() {
		while (true) {
			cleanupLogging();
			
			s_logger.info("connMap=" + connMap);
			Enumeration<String> e = connMap.keys();
		    while (e.hasMoreElements()) {
		    	String key;
		    	ConsoleProxyClient client;
		    	
		    	synchronized (connMap) {
    		        key  = e.nextElement();
    		        client  = connMap.get(key);
		    	}

    		    long seconds_unused = (System.currentTimeMillis() - client.getClientLastFrontEndActivityTime()) / 1000;
    		    if (seconds_unused < MAX_SESSION_IDLE_SECONDS) {
    		      	continue;
    		    }
    		    
		    	synchronized (connMap) {
		    		connMap.remove(key);
		    	}
		    	
    		    // close the server connection
    		    s_logger.info("Dropping " + client + " which has not been used for " + seconds_unused + " seconds");
    		    client.closeClient();

    		    // report load changes
				String loadInfo = new ConsoleProxyClientStatsCollector(connMap).getStatsReport(); 
				ConsoleProxy.reportLoadInfo(loadInfo);
				if(s_logger.isDebugEnabled())
					s_logger.debug("Report load change : " + loadInfo);
		    }
		    
			try { Thread.sleep(1000); } catch (InterruptedException ex) {}
		}
	}
}
