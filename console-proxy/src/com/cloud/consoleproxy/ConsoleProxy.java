/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.consoleproxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLServerSocket;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.console.AuthenticationException;
import com.cloud.console.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;

public class ConsoleProxy {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxy.class);
	
	public static final int KEYBOARD_RAW = 0;
	public static final int KEYBOARD_COOKED = 1;

	public static Object context;
	
	// this has become more ugly, to store keystore info passed from management server (we now use management server managed keystore to support
	// dynamically changing to customer supplied certificate)
	public static byte[] ksBits;
	public static String ksPassword;
	
	public static Method authMethod;
	public static Method reportMethod;
	public static Method ensureRouteMethod;

	static Hashtable<String, ConsoleProxyViewer> connectionMap = new Hashtable<String, ConsoleProxyViewer>();
	static int tcpListenPort = 5999;
	static int httpListenPort = 80;
	static int httpCmdListenPort = 8001;
	static String jarDir = "./applet/";
	static boolean compressServerMessage = true;
	static int viewerLinger = 180;
	static int reconnectMaxRetry = 5;
	static int readTimeoutSeconds = 90;
	static int keyboardType = KEYBOARD_RAW;
	static String factoryClzName;
	static boolean standaloneStart = false;

	private static void configLog4j() {
		URL configUrl = System.class.getResource("/conf/log4j-cloud.xml");
		if(configUrl == null)
			configUrl = System.class.getClassLoader().getSystemResource("log4j-cloud.xml");
		
		if(configUrl == null)
			configUrl = System.class.getClassLoader().getSystemResource("conf/log4j-cloud.xml");
			
		if(configUrl != null) {
			try {
				System.out.println("Configure log4j using " + configUrl.toURI().toString());
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}

			try {
				File file = new File(configUrl.toURI());
				
				System.out.println("Log4j configuration from : " + file.getAbsolutePath());
				DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
			} catch (URISyntaxException e) {
				System.out.println("Unable to convert log4j configuration Url to URI");
			}
			// DOMConfigurator.configure(configUrl);
		} else {
			System.out.println("Configure log4j with default properties");
		}
	}

	private static void configProxy(Properties conf) {
		s_logger.info("Configure console proxy...");
		for(Object key : conf.keySet()) {
			s_logger.info("Property " + (String)key + ": " + conf.getProperty((String)key));
		}
		
		String s = conf.getProperty("consoleproxy.tcpListenPort");
		if (s!=null) {
			tcpListenPort = Integer.parseInt(s);
			s_logger.info("Setting tcpListenPort=" + s);
		}
		
		s = conf.getProperty("consoleproxy.httpListenPort");
		if (s!=null) {
			httpListenPort = Integer.parseInt(s);
			s_logger.info("Setting httpListenPort=" + s);
		}
		
		s = conf.getProperty("premium");
		if(s != null && s.equalsIgnoreCase("true")) {
			s_logger.info("Premium setting will override settings from consoleproxy.properties, listen at port 443");
			httpListenPort = 443;
			factoryClzName = "com.cloud.consoleproxy.ConsoleProxySecureServerFactoryImpl";
		} else {
			factoryClzName = ConsoleProxyBaseServerFactoryImpl.class.getName();
		}
		
		s = conf.getProperty("consoleproxy.httpCmdListenPort");
		if (s!=null) {
			httpCmdListenPort = Integer.parseInt(s);
			s_logger.info("Setting httpCmdListenPort=" + s);
		}
		s = conf.getProperty("consoleproxy.jarDir");
		if (s!=null) {
			jarDir = s;
			s_logger.info("Setting jarDir=" + s);
		}
		s = conf.getProperty("consoleproxy.viewerLinger");
		if (s!=null) {
			viewerLinger = Integer.parseInt(s);
			s_logger.info("Setting viewerLinger=" + s);
		}
		s = conf.getProperty("consoleproxy.compressServerMessage");
		if (s!=null) {
			compressServerMessage = Boolean.parseBoolean(s);
			s_logger.info("Setting compressServerMessage=" + s);
		}
		
		s = conf.getProperty("consoleproxy.reconnectMaxRetry");
		if (s!=null) {
			reconnectMaxRetry = Integer.parseInt(s);
			s_logger.info("Setting reconnectMaxRetry=" + reconnectMaxRetry);
		}
		
		s = conf.getProperty("consoleproxy.readTimeoutSeconds");
		if (s!=null) {
			readTimeoutSeconds = Integer.parseInt(s);
			s_logger.info("Setting readTimeoutSeconds=" + readTimeoutSeconds);
		}
	}
	
	public static ConsoleProxyServerFactory getHttpServerFactory() {
		try {
			Class<?> clz = Class.forName(factoryClzName);
			try {
				ConsoleProxyServerFactory factory = (ConsoleProxyServerFactory)clz.newInstance();
				factory.init(ConsoleProxy.ksBits, ConsoleProxy.ksPassword);
				return factory;
			} catch (InstantiationException e) {
				s_logger.error(e.getMessage(), e);
				return null;
			} catch (IllegalAccessException e) {
				s_logger.error(e.getMessage(), e);
				return null;
			}
		} catch (ClassNotFoundException e) {
			s_logger.warn("Unable to find http server factory class: " + factoryClzName);
			return new ConsoleProxyBaseServerFactoryImpl();
		}
	}
	
	public static boolean authenticateConsoleAccess(String host, String port, String vmId, String sid, String ticket) {
		if(standaloneStart)
			return true;
		
		if(authMethod != null) {
			Object result;
			try {
				result = authMethod.invoke(ConsoleProxy.context, host, port, vmId, sid, ticket);
			} catch (IllegalAccessException e) {
				s_logger.error("Unable to invoke authenticateConsoleAccess due to IllegalAccessException" + " for vm: " + vmId, e);
				return false;
			} catch (InvocationTargetException e) {
				s_logger.error("Unable to invoke authenticateConsoleAccess due to InvocationTargetException " + " for vm: " + vmId, e);
				return false;
			}
			
			if(result != null && result instanceof Boolean) {
				return ((Boolean)result).booleanValue();
			} else {
				s_logger.error("Invalid authentication return object " + result + " for vm: " + vmId + ", decline the access");
				return false;
			}
			
		} else {
			s_logger.warn("Private channel towards management server is not setup. Switch to offline mode and allow access to vm: " + vmId);
			return true;
		}
	}
	
	public static void reportLoadInfo(String gsonLoadInfo) {
		if(reportMethod != null) {
			try {
				reportMethod.invoke(ConsoleProxy.context, gsonLoadInfo);
			} catch (IllegalAccessException e) {
				s_logger.error("Unable to invoke reportLoadInfo due to " + e.getMessage());
			} catch (InvocationTargetException e) {
				s_logger.error("Unable to invoke reportLoadInfo due to " + e.getMessage());
			}
		} else {
			s_logger.warn("Private channel towards management server is not setup. Switch to offline mode and ignore load report");
		}
	}
	
	public static void ensureRoute(String address) {
		if(ensureRouteMethod != null) {
			try {
				ensureRouteMethod.invoke(ConsoleProxy.context, address);
			} catch (IllegalAccessException e) {
				s_logger.error("Unable to invoke ensureRoute due to " + e.getMessage());
			} catch (InvocationTargetException e) {
				s_logger.error("Unable to invoke ensureRoute due to " + e.getMessage());
			}
		} else {
			s_logger.warn("Unable to find ensureRoute method, console proxy agent is not up to date");
		}
	}
	
	public static void startWithContext(Properties conf, Object context, byte[] ksBits, String ksPassword) {
		s_logger.info("Start console proxy with context");
		if(conf != null) {
			for(Object key : conf.keySet()) {
				s_logger.info("Context property " + (String)key + ": " + conf.getProperty((String)key));
			}
		}
		
		configLog4j();
		Logger.setFactory(new ConsoleProxyLoggerFactory());
		
		// Using reflection to setup private/secure communication channel towards management server
		ConsoleProxy.context = context;
		ConsoleProxy.ksBits = ksBits;
		ConsoleProxy.ksPassword = ksPassword;
		try {
			Class<?> contextClazz = Class.forName("com.cloud.agent.resource.consoleproxy.ConsoleProxyResource");
			authMethod = contextClazz.getDeclaredMethod("authenticateConsoleAccess", String.class, String.class, String.class, String.class, String.class);
			reportMethod = contextClazz.getDeclaredMethod("reportLoadInfo", String.class);
			ensureRouteMethod = contextClazz.getDeclaredMethod("ensureRoute", String.class);
		} catch (SecurityException e) {
			s_logger.error("Unable to setup private channel due to SecurityException", e);
		} catch (NoSuchMethodException e) {
			s_logger.error("Unable to setup private channel due to NoSuchMethodException", e);
		} catch (IllegalArgumentException e) {
			s_logger.error("Unable to setup private channel due to IllegalArgumentException", e);
		} catch(ClassNotFoundException e) {
			s_logger.error("Unable to setup private channel due to ClassNotFoundException", e);
		}
		
		// merge properties from conf file
		InputStream confs = ConsoleProxy.class.getResourceAsStream("/conf/consoleproxy.properties");
		Properties props = new Properties();
		if (confs == null) {
			s_logger.info("Can't load consoleproxy.properties from classpath, will use default configuration");
		} else {
			try {
				props.load(confs);
				
				for(Object key : props.keySet()) {
					// give properties passed via context high priority, treat properties from consoleproxy.properties
					// as default values
					if(conf.get(key) == null)
						conf.put(key, props.get(key));
				}
			}  catch (Exception e) {
				s_logger.error(e.toString(), e);
			}
		}
		
		start(conf);
	}

	public static void start(Properties conf) {
		System.setProperty("java.awt.headless", "true");
		
		configProxy(conf);
		
		ConsoleProxyServerFactory factory = getHttpServerFactory();
		if(factory == null) {
			s_logger.error("Unable to load console proxy server factory");
			System.exit(1);
		}
		
		if(httpListenPort != 0) {
			startupHttpMain();
		} else {
			s_logger.error("A valid HTTP server port is required to be specified, please check your consoleproxy.httpListenPort settings");
			System.exit(1);
		}
		
	    if(httpCmdListenPort > 0) {
	    	startupHttpCmdPort();
	    } else {
	    	s_logger.info("HTTP command port is disabled");
	    }
	    
		ViewerGCThread cthread = new ViewerGCThread(connectionMap);
		cthread.setName("Viewer GC Thread");
		cthread.start();
		
		if(tcpListenPort > 0) {
			SSLServerSocket srvSock = null;
			try {
				srvSock = factory.createSSLServerSocket(tcpListenPort);
			    s_logger.info("Listening for TCP on port " + tcpListenPort);
			} catch (IOException ioe) {
				s_logger.error(ioe.toString(), ioe);
				System.exit(1);
			}
			
			if(srvSock != null) {
				while (true) {
				    Socket conn = null;
				    try {
				        conn = srvSock.accept();
				        String srcinfo = conn.getInetAddress().getHostAddress() + ":" + conn.getPort();
				        s_logger.info("Accepted connection from " + srcinfo);
				        conn.setSoLinger(false,0);
				        ConsoleProxyClientHandler worker = new ConsoleProxyClientHandler(conn);
				        worker.setName("Proxy Thread " + worker.getId() + " <" + srcinfo);
				        worker.start();
				    } catch (IOException ioe2) {
						s_logger.error(ioe2.toString(), ioe2);
						try {
						    if (conn != null) {
						        conn.close();
						    }
						} catch (IOException ioe) {}
				    } catch (Throwable e) {
				    	// Something really bad happened
				    	// Terminate the program
				    	s_logger.error(e.toString(), e);
				    	System.exit(1);
				    }
				}
			} else {
		    	s_logger.warn("TCP port is enabled in configuration but we are not able to instantiate server socket.");
			}
		} else {
	    	s_logger.info("TCP port is disabled for applet viewers");
		}
	}
	
	private static void startupHttpMain() {
		try {
			ConsoleProxyServerFactory factory = getHttpServerFactory();
			if(factory == null) {
				s_logger.error("Unable to load HTTP server factory");
				System.exit(1);
			}
			
			HttpServer server = factory.createHttpServerInstance(httpListenPort);
			server.createContext("/getscreen", new ConsoleProxyThumbnailHandler());
			server.createContext("/resource/", new ConsoleProxyResourceHandler());
			server.createContext("/ajax", new ConsoleProxyAjaxHandler());
			server.createContext("/ajaximg", new ConsoleProxyAjaxImageHandler());
			server.setExecutor(new ThreadExecutor()); // creates a default executor
			server.start();
		} catch(Exception e) {
			s_logger.error(e.getMessage(), e);
			System.exit(1);
		}
	}
	
	private static void startupHttpCmdPort() {
		try {
		    s_logger.info("Listening for HTTP CMDs on port " + httpCmdListenPort);
			HttpServer cmdServer = HttpServer.create(new InetSocketAddress(httpCmdListenPort), 2);
			cmdServer.createContext("/cmd", new ConsoleProxyCmdHandler());
			cmdServer.setExecutor(new ThreadExecutor()); // creates a default executor
			cmdServer.start();
		} catch(Exception e) {
			s_logger.error(e.getMessage(), e);
			System.exit(1);
		}
	}
	
	public static void main(String[] argv) {
		standaloneStart = true;
		configLog4j();
		Logger.setFactory(new ConsoleProxyLoggerFactory());
		
		InputStream confs = ConsoleProxy.class.getResourceAsStream("/conf/consoleproxy.properties");
		Properties conf = new Properties();
		if (confs == null) {
			s_logger.info("Can't load consoleproxy.properties from classpath, will use default configuration");
		} else {
			try {
				conf.load(confs);
			}  catch (Exception e) {
				s_logger.error(e.toString(), e);
			}
		}
		start(conf);
	}

	static ConsoleProxyViewer createViewer() {
		ConsoleProxyViewer viewer = new ConsoleProxyViewer();
		viewer.compressServerMessage = compressServerMessage;
		return viewer;
	}
	
	static void initViewer(ConsoleProxyViewer viewer, String host, int port, String tag, String sid, String ticket) throws AuthenticationException {
		ConsoleProxyViewer.authenticationExternally(host, String.valueOf(port), tag, sid, ticket);
		
		viewer.host = host;
		viewer.port = port;
		viewer.tag = tag;
		viewer.passwordParam = sid;
		
		viewer.init();
	}
	
	static ConsoleProxyViewer getVncViewer(String host, int port, String sid, String tag, String ticket) throws Exception {
		ConsoleProxyViewer viewer = null;
		
		boolean reportLoadChange = false;
		synchronized (connectionMap) {
			viewer = connectionMap.get(host + ":" + port);
			if (viewer == null) {
				viewer = createViewer();
				initViewer(viewer, host, port, tag, sid, ticket);
				connectionMap.put(host + ":" + port, viewer);
				s_logger.info("Added viewer object " + viewer);
				
				reportLoadChange = true;
			} else if (!viewer.rfbThread.isAlive()) {
				s_logger.info("The rfb thread died, reinitializing the viewer " +
						viewer);
				initViewer(viewer, host, port, tag, sid, ticket);
				
				reportLoadChange = true;
			} else if (!sid.equals(viewer.passwordParam)) {
				s_logger.warn("Bad sid detected(VNC port may be reused). sid in session: " + viewer.passwordParam + ", sid in request: " + sid);
				initViewer(viewer, host, port, tag, sid, ticket);
				
				reportLoadChange = true;
					
				/*
				throw new AuthenticationException ("Cannot use the existing viewer " +
						viewer + ": bad sid");
				*/
			}
		}
		
		if(viewer != null) {
			if (viewer.status == ConsoleProxyViewer.STATUS_NORMAL_OPERATION) {
				// Do not update lastUsedTime if the viewer is in the process of starting up
				// or if it failed to authenticate.
				viewer.lastUsedTime = System.currentTimeMillis();
			}
		}
		
		if(reportLoadChange) {
			ConsoleProxyStatus status = new ConsoleProxyStatus();
			status.setConnections(ConsoleProxy.connectionMap);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String loadInfo = gson.toJson(status);
			
			ConsoleProxy.reportLoadInfo(loadInfo);
			if(s_logger.isDebugEnabled())
				s_logger.debug("Report load change : " + loadInfo);
		}
		
		return viewer;
	}
	
	static ConsoleProxyViewer getAjaxVncViewer(String host, int port, String sid, String tag, String ticket, String ajaxSession) throws Exception {
		boolean reportLoadChange = false;
		synchronized (connectionMap) {
			ConsoleProxyViewer viewer = connectionMap.get(host + ":" + port);
//			s_logger.info("view lookup " + host + ":" + port + " = " + viewer);

			if (viewer == null) {
				viewer = createViewer();
				viewer.ajaxViewer = true;
				
				initViewer(viewer, host, port, tag, sid, ticket);
				connectionMap.put(host + ":" + port, viewer);
				s_logger.info("Added viewer object " + viewer);
				reportLoadChange = true;
			} else if (!viewer.rfbThread.isAlive()) {
				s_logger.info("The rfb thread died, reinitializing the viewer " +
						viewer);
				initViewer(viewer, host, port, tag, sid, ticket);
				reportLoadChange = true;
			} else if (!sid.equals(viewer.passwordParam)) {
				s_logger.warn("Bad sid detected(VNC port may be reused). sid in session: " + viewer.passwordParam + ", sid in request: " + sid);
				initViewer(viewer, host, port, tag, sid, ticket);
				reportLoadChange = true;
				
				/*
				throw new AuthenticationException ("Cannot use the existing viewer " +
						viewer + ": bad sid");
				*/
			} else {
				if(ajaxSession == null || ajaxSession.isEmpty())
					ConsoleProxyViewer.authenticationExternally(host, String.valueOf(port), tag, sid, ticket);
			}
			
			if (viewer.status == ConsoleProxyViewer.STATUS_NORMAL_OPERATION) {
				// Do not update lastUsedTime if the viewer is in the process of starting up
				// or if it failed to authenticate.
				viewer.lastUsedTime = System.currentTimeMillis();
			}
			
			if(reportLoadChange) {
				ConsoleProxyStatus status = new ConsoleProxyStatus();
				status.setConnections(ConsoleProxy.connectionMap);
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String loadInfo = gson.toJson(status);
				
				ConsoleProxy.reportLoadInfo(loadInfo);
				if(s_logger.isDebugEnabled())
					s_logger.debug("Report load change : " + loadInfo);
			}
			return viewer;
		}
	}
	
	public static void removeViewer(ConsoleProxyViewer viewer) {
		synchronized (connectionMap) {
			for(Map.Entry<String, ConsoleProxyViewer> entry : connectionMap.entrySet()) {
				if(entry.getValue() == viewer) {
					connectionMap.remove(entry.getKey());
					return;
				}
			}
		}
	}
	
	static void waitForViewerToStart(ConsoleProxyViewer viewer) throws Exception {
		if (viewer.status == ConsoleProxyViewer.STATUS_NORMAL_OPERATION) {
			return;
		}

		Long startTime = System.currentTimeMillis();
		int delay = 500;
		
		while (System.currentTimeMillis() < startTime + 30000 &&
				viewer.status != ConsoleProxyViewer.STATUS_NORMAL_OPERATION) {
			if (viewer.status == ConsoleProxyViewer.STATUS_AUTHENTICATION_FAILURE) {
				throw new Exception ("Authentication failure");
			}
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// ignore
			}
			delay = (int)(delay * 1.5);
		}
		
		if (viewer.status != ConsoleProxyViewer.STATUS_NORMAL_OPERATION) {
			throw new Exception ("Cannot start VncViewer");
		}
		
		s_logger.info("Waited " +
				(System.currentTimeMillis() - startTime) + "ms for VncViewer to start");
	}

	static class ThreadExecutor implements Executor {
	     public void execute(Runnable r) {
	         new Thread(r).start();
	     }
	}

    static class ViewerGCThread extends Thread {
    	Hashtable<String, ConsoleProxyViewer> connMap;
    	long lastLogScan = 0L;
    	
    	public ViewerGCThread(Hashtable<String, ConsoleProxyViewer> connMap) {
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
    		    	ConsoleProxyViewer viewer;
    		    	
    		    	synchronized (connMap) {
	    		        key  = e.nextElement();
	    		        viewer  = connMap.get(key);
    		    	}

	    		    long seconds_unused = (System.currentTimeMillis() - viewer.lastUsedTime) / 1000;
	    		         
	    		    if (seconds_unused > viewerLinger / 2 && viewer.clientStream != null) {
	    		    	s_logger.info("Pinging client for " + viewer +
	    		    			" which has not been used for " + seconds_unused + "sec");
	    		    	byte[] bs = new byte[2];
	    		        bs[0] = (byte)250;
	    		        bs[1] = 3;
	    		        viewer.writeToClientStream(bs);
	    		    }
	    		    
	    		    if (seconds_unused < viewerLinger) {
	    		      	continue;
	    		    }
	    		    
    		    	synchronized (connMap) {
    		    		connMap.remove(key);
    		    	}
	    		    // close the server connection
	    		    s_logger.info("Dropping " + viewer +
	    		        		 " which has not been used for " +
	    		        		 seconds_unused + " seconds");
	    		    viewer.dropMe = true;
	    		    synchronized (viewer) {
		    		    if (viewer.clientStream != null) {
		    		    	try {
		    		    		viewer.clientStream.close();
		    		    	} catch (IOException ioe) {
		    		    		// ignored
		    		    	}
				    		viewer.clientStream = null;
				    		viewer.clientStreamInfo = null;
		    		    }
	    		        if (viewer.rfb != null) {
	    		        	viewer.rfb.close();
	    		        }
    		    	}

	    		    // report load change for removal of the viewer
    				ConsoleProxyStatus status = new ConsoleProxyStatus();
    				status.setConnections(ConsoleProxy.connectionMap);
    				Gson gson = new GsonBuilder().setPrettyPrinting().create();
    				String loadInfo = gson.toJson(status);
    				
    				ConsoleProxy.reportLoadInfo(loadInfo);
    				if(s_logger.isDebugEnabled())
    					s_logger.debug("Report load change : " + loadInfo);
    		    }
    			try {
    				Thread.sleep(30000);
    			} catch (InterruptedException exp) {
    				// Ignore
    			}
    		}
    	}
    }
}
