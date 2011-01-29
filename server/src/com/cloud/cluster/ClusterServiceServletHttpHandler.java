package com.cloud.cluster;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ChangeAgentAnswer;
import com.cloud.agent.api.ChangeAgentCommand;
import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.RemoteMethodConstants;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

public class ClusterServiceServletHttpHandler implements HttpRequestHandler {
    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletHttpHandler.class);
    
    private final Gson gson;
    private final ClusterManager manager;

    public ClusterServiceServletHttpHandler(ClusterManager manager) {
    	this.manager = manager;

		gson = GsonHelper.getBuilder().create();
    }
    
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context)
		throws HttpException, IOException {
		
		try {
			if(s_logger.isTraceEnabled())
				s_logger.trace("Start Handling cluster HTTP request");
				
			parseRequest(request);
			handleRequest(request, response);
			
			if(s_logger.isTraceEnabled())
				s_logger.trace("Handle cluster HTTP request done");
			
		} catch(Throwable e) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("Unexpected exception " + e.toString());
			
			writeResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void parseRequest(HttpRequest request) throws IOException {
		if(request instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest)request;
			
			String body = EntityUtils.toString(entityRequest.getEntity());
			if(body != null) {
		        String[] paramArray = body.split("&");
		        if(paramArray != null) {
			        for (String paramEntry : paramArray) {
			            String[] paramValue = paramEntry.split("=");
			            if (paramValue.length != 2)
			                continue;
			            
			            String name = URLDecoder.decode(paramValue[0]);
			            String value = URLDecoder.decode(paramValue[1]);
			            
			            if(s_logger.isTraceEnabled())
			            	s_logger.trace("Parsed request parameter " + name + "=" + value);
			            request.getParams().setParameter(name, value);
			        }
		        }
			}
		}
	}
	
	private void writeResponse(HttpResponse response, int statusCode, String content) {
		if(content == null)
			content = "";
		response.setStatusCode(statusCode);
        BasicHttpEntity body = new BasicHttpEntity();
        body.setContentType("text/html; charset=UTF-8");
        
        byte[] bodyData = content.getBytes();
        body.setContent(new ByteArrayInputStream(bodyData));
        body.setContentLength(bodyData.length);
        response.setEntity(body);
	}
	
	protected void handleRequest(HttpRequest req, HttpResponse response) {
		String method = (String)req.getParams().getParameter("method");
		
		int nMethod = RemoteMethodConstants.METHOD_UNKNOWN;
		String responseContent = null;
		try {
			if(method != null)
				nMethod = Integer.parseInt(method);
			
			switch(nMethod) {
			case RemoteMethodConstants.METHOD_EXECUTE :
				responseContent = handleExecuteMethodCall(req);
				break;
				
			case RemoteMethodConstants.METHOD_EXECUTE_ASYNC :
				responseContent = handleExecuteAsyncMethodCall(req);
				break;
				
			case RemoteMethodConstants.METHOD_ASYNC_RESULT :
				responseContent = handleAsyncResultMethodCall(req);
				break;
				
			case RemoteMethodConstants.METHOD_UNKNOWN :
			default :
				assert(false);
				s_logger.error("unrecognized method " + nMethod);
				break;
			}
		} catch(Throwable e) {
			s_logger.error("Unexpected exception when processing cluster service request : ", e);
		}
		
		if(responseContent != null) {
			writeResponse(response, HttpStatus.SC_OK, responseContent);
		} else {
			writeResponse(response, HttpStatus.SC_BAD_REQUEST, null);
		}
	}
	
	private String handleExecuteMethodCall(HttpRequest req) {
		String agentId = (String)req.getParams().getParameter("agentId");
		String gsonPackage = (String)req.getParams().getParameter("gsonPackage");
		String stopOnError = (String)req.getParams().getParameter("stopOnError");
		
		if(s_logger.isDebugEnabled())
			s_logger.debug("|->" + agentId + " " + gsonPackage);
		
    	Command [] cmds = null;
    	try {
    		cmds = gson.fromJson(gsonPackage, Command[].class);
    	} catch(Throwable e) {
    		assert(false);
    		s_logger.error("Excection in gson decoding : ", e);
    	}
    	
    	if (cmds.length == 1 && cmds[0] instanceof ChangeAgentCommand) {  //intercepted
    		ChangeAgentCommand cmd = (ChangeAgentCommand)cmds[0];

    		if (s_logger.isDebugEnabled()) {
    			s_logger.debug("Intercepting command for agent change: agent " + cmd.getAgentId() + " event: " + cmd.getEvent());
    		}
    		boolean result = false;
    		try {
    			result = manager.executeAgentUserRequest(cmd.getAgentId(), cmd.getEvent());
    			if (s_logger.isDebugEnabled()) {
    				s_logger.debug("Result is " + result);
    			}
    			
    		} catch (AgentUnavailableException e) {
    			s_logger.warn("Agent is unavailable", e);
    			return null;
    		}
    		
    		Answer[] answers = new Answer[1];
    		answers[0] = new ChangeAgentAnswer(cmd, result);
    		return gson.toJson(answers);
    	}
    	
    	try {
    		long startTick = System.currentTimeMillis();
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Send |-> " + agentId + " " + gsonPackage + " to agent manager");
    		
        	Answer[] answers = manager.sendToAgent(Long.parseLong(agentId), cmds,
        		Integer.parseInt(stopOnError) != 0 ? true : false);
        	
        	if(answers != null) {
        		String jsonReturn =  gson.toJson(answers);
        		
        		if(s_logger.isDebugEnabled())
        			s_logger.debug("Completed |-> " + agentId + " " + gsonPackage + 
        				" in " + (System.currentTimeMillis() - startTick) + " ms, return result: " + jsonReturn);
        		
        		return jsonReturn;
        	} else {
        		if(s_logger.isDebugEnabled())
        			s_logger.debug("Completed |-> " + agentId + " " + gsonPackage + 
        				" in " + (System.currentTimeMillis() - startTick) + " ms, return null result");
        	}
    	} catch(AgentUnavailableException e) {
    	    s_logger.warn("Agent is unavailable", e);
    	} catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
        }
    	
		return null;
	}
	
	private String handleExecuteAsyncMethodCall(HttpRequest  req) {
		String agentId = (String)req.getParams().getParameter("agentId");
		String gsonPackage = (String)req.getParams().getParameter("gsonPackage");
		String stopOnError = (String)req.getParams().getParameter("stopOnError");
		String callingPeer = (String)req.getParams().getParameter("caller");
		
		if(s_logger.isDebugEnabled())
			s_logger.debug("Async " + callingPeer + " |-> " + agentId + " " + gsonPackage);
		
    	Command [] cmds = null;
    	try {
    		cmds = gson.fromJson(gsonPackage, Command[].class);
    	} catch(Throwable e) {
    		assert(false);
    		s_logger.error("Excection in gson decoding : ", e);
    	}
    	
    	Listener listener = new ClusterAsyncExectuionListener(manager, callingPeer);
    	long seq = -1;
    	try {
    		long startTick = System.currentTimeMillis();
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Send Async " + callingPeer + " |-> " + agentId + " " + gsonPackage + " to agent manager");
    		
    	    seq = manager.sendToAgent(Long.parseLong(agentId), cmds,
    	            Integer.parseInt(stopOnError) != 0 ? true : false, listener);
    	    
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Complated Async " + callingPeer + " |-> " + agentId + " " + gsonPackage + " in " + 
					+ (System.currentTimeMillis() - startTick) + " ms, returned seq: " + seq);
    	} catch (AgentUnavailableException e) {
    	    s_logger.warn("Agent is unavailable", e);
    	    seq = -1;
    	}
    	
		return gson.toJson(seq);
	}
	
	private String handleAsyncResultMethodCall(HttpRequest  req) {
		String agentId = (String)req.getParams().getParameter("agentId");
		String gsonPackage = (String)req.getParams().getParameter("gsonPackage");
		String seq = (String)req.getParams().getParameter("seq");
		String executingPeer = (String)req.getParams().getParameter("executingPeer");
		
		if(s_logger.isDebugEnabled())
			s_logger.debug("Async callback " + executingPeer + "." + agentId + " |-> " + gsonPackage);

    	Answer[] answers = null;
    	try {
    		answers = gson.fromJson(gsonPackage, Answer[].class);
    	} catch(Throwable e) {
    		assert(false);
    		s_logger.error("Excection in gson decoding : ", e);
    	}
    	
    	long startTick = System.currentTimeMillis();
    	if(manager.onAsyncResult(executingPeer, Long.parseLong(agentId), Long.parseLong(seq), answers)) {
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Completed local callback in " + (System.currentTimeMillis() - startTick) + 
    				" ms, return recurring=true, let async listener contine on");
    		
    		return "recurring=true";
    	}
    	
		if(s_logger.isDebugEnabled())
			s_logger.debug("Completed local callback in " + (System.currentTimeMillis() - startTick) + 
				" ms, return recurring=false, indicate to tear down async listener");
			
		return "recurring=false";
	}
}
