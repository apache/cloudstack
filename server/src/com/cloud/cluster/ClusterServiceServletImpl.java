package com.cloud.cluster;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.cloud.cluster.ClusterService;
import com.cloud.cluster.RemoteMethodConstants;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

public class ClusterServiceServletImpl implements ClusterService {
	private static final long serialVersionUID = 4574025200012566153L;

	private static final Logger s_logger = Logger.getLogger(ClusterServiceServletImpl.class);
    
    private String serviceUrl;
    
    private Gson gson;
    
    public ClusterServiceServletImpl() {
		gson = GsonHelper.getBuilder().create();
    }
    
    public ClusterServiceServletImpl(String serviceUrl) {
    	this.serviceUrl = serviceUrl;
    	
		gson = GsonHelper.getBuilder().create();
    }
    
    @Override
	public String execute(String callingPeer, long agentId, String gsonPackage, boolean stopOnError) throws RemoteException {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Post (sync-call) " + gsonPackage + " to " + serviceUrl + " for agent " + agentId + " from " + callingPeer);
    	
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(serviceUrl);
        
        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_EXECUTE));
        method.addParameter("agentId", Long.toString(agentId));
        method.addParameter("gsonPackage", gsonPackage);
        method.addParameter("stopOnError", stopOnError ? "1" : "0");
        
    	return executePostMethod(client, method);
    }
    
    @Override
	public long executeAsync(String callingPeer, long agentId, String gsonPackage, boolean stopOnError) throws RemoteException {
    	
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Post (Async-call) " + gsonPackage + " to " + serviceUrl + " for agent " + agentId + " from " + callingPeer);
    	
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(serviceUrl);
        
        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_EXECUTE_ASYNC));
        method.addParameter("agentId", Long.toString(agentId));
        method.addParameter("gsonPackage", gsonPackage);
        method.addParameter("stopOnError", stopOnError ? "1" : "0");
        method.addParameter("caller", callingPeer);
        
    	String result = executePostMethod(client, method);
    	if(result == null) {
    		s_logger.error("Empty return from remote async-execution on " + serviceUrl);
    		throw new RemoteException("Invalid result returned from async-execution on peer : " + serviceUrl);
    	}
    	
    	try {
    		return gson.fromJson(result, Long.class);
    	} catch(Throwable e) {
    		s_logger.error("Unable to parse executeAsync return : " + result);
    		throw new RemoteException("Invalid result returned from async-execution on peer : " + serviceUrl);
    	}
    }
    
	public boolean onAsyncResult(String executingPeer, long agentId, long seq, String gsonPackage) throws RemoteException {
		if(s_logger.isDebugEnabled())
			s_logger.debug("Forward Async-call answer to remote listener, agent: " + agentId
				+ ", excutingPeer: " + executingPeer
				+ ", seq: " + seq + ", gsonPackage: " + gsonPackage);
		
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(serviceUrl);
        
        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_ASYNC_RESULT));
        method.addParameter("agentId", Long.toString(agentId));
        method.addParameter("gsonPackage", gsonPackage);
        method.addParameter("seq", Long.toString(seq));
        method.addParameter("executingPeer", executingPeer);
        
    	String result = executePostMethod(client, method);
    	if(result.contains("recurring=true")) {
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Remote listener returned recurring=true");
    		return true;
    	}
    	
		if(s_logger.isDebugEnabled())
			s_logger.debug("Remote listener returned recurring=false");
    	return false;
	}
	
	private String executePostMethod(HttpClient client, PostMethod method) {
        int response = 0;
        String result = null;
        try {
        	long startTick = System.currentTimeMillis();
        	response = client.executeMethod(method);
        	if(response == HttpStatus.SC_OK) {
        		result = method.getResponseBodyAsString();
        		if(s_logger.isDebugEnabled())
        			s_logger.debug("POST " + serviceUrl + " response :" + result + ", responding time: " 
        				+ (System.currentTimeMillis() - startTick) + " ms");
        	} else {
            	s_logger.error("Invalid response code : " + response + ", from : " 
            		+ serviceUrl + ", method : " + method.getParameter("method") 
            		+ " responding time: " + (System.currentTimeMillis() - startTick));
        	}
        } catch (HttpException e) {
        	s_logger.error("HttpException from : " + serviceUrl + ", method : " + method.getParameter("method"));
        } catch (IOException e) {
        	s_logger.error("IOException from : " + serviceUrl + ", method : " + method.getParameter("method"));
        } catch(Throwable e) {
        	s_logger.error("Exception from : " + serviceUrl + ", method : " + method.getParameter("method") + ", exception :", e);
        }
    	return result;
	}
	
	// for test purpose only
	public static void main(String[] args) {
		ClusterServiceServletImpl service = new ClusterServiceServletImpl("http://localhost:9090/clusterservice");
		try {
			String result = service.execute("test", 1, "{ p1:v1, p2:v2 }", true);
			System.out.println(result);
		} catch (RemoteException e) {
		}
	}
}
