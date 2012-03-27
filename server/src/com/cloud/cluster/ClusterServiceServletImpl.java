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

package com.cloud.cluster;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

public class ClusterServiceServletImpl implements ClusterService {
    private static final long serialVersionUID = 4574025200012566153L;
    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletImpl.class);
    
    private String _serviceUrl;

    private int _requestTimeoutSeconds;
    protected static HttpClient s_client = null;
    
    public ClusterServiceServletImpl() {
    }

    public ClusterServiceServletImpl(String serviceUrl, int requestTimeoutSeconds) {
    	s_logger.info("Setup cluster service servlet. service url: " + serviceUrl + ", request timeout: " + requestTimeoutSeconds + " seconds");
    	
        this._serviceUrl = serviceUrl;
        this._requestTimeoutSeconds = requestTimeoutSeconds;
    }
    
    @Override
    public String execute(ClusterServicePdu pdu) throws RemoteException {

        HttpClient client = getHttpClient();
        PostMethod method = new PostMethod(_serviceUrl);

        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_DELIVER_PDU));
        method.addParameter("sourcePeer", pdu.getSourcePeer());
        method.addParameter("destPeer", pdu.getDestPeer());
        method.addParameter("pduSeq", Long.toString(pdu.getSequenceId()));
        method.addParameter("pduAckSeq", Long.toString(pdu.getAckSequenceId()));
        method.addParameter("agentId", Long.toString(pdu.getAgentId()));
        method.addParameter("gsonPackage", pdu.getJsonPackage());
        method.addParameter("stopOnError", pdu.isStopOnError() ? "1" : "0");
        method.addParameter("pduType", Integer.toString(pdu.getPduType()));

        try {
        	return executePostMethod(client, method);
        } finally {
        	method.releaseConnection();
        }
    }

    @Override
    public boolean ping(String callingPeer) throws RemoteException {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Ping at " + _serviceUrl);
        }

        HttpClient client = getHttpClient();
        PostMethod method = new PostMethod(_serviceUrl);

        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_PING));
        method.addParameter("callingPeer", callingPeer);
        
        try {
	        String returnVal =  executePostMethod(client, method);
	        if("true".equalsIgnoreCase(returnVal)) {
	            return true;
	        }
	        return false;
        } finally {
        	method.releaseConnection();
        }
    }

    private String executePostMethod(HttpClient client, PostMethod method) {
        int response = 0;
        String result = null;
        try {
            long startTick = System.currentTimeMillis();
            response = client.executeMethod(method);
            if(response == HttpStatus.SC_OK) {
                result = method.getResponseBodyAsString();
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("POST " + _serviceUrl + " response :" + result + ", responding time: "
                            + (System.currentTimeMillis() - startTick) + " ms");
                }
            } else {
                s_logger.error("Invalid response code : " + response + ", from : "
                        + _serviceUrl + ", method : " + method.getParameter("method")
                        + " responding time: " + (System.currentTimeMillis() - startTick));
            }
        } catch (HttpException e) {
            s_logger.error("HttpException from : " + _serviceUrl + ", method : " + method.getParameter("method"));
        } catch (IOException e) {
            s_logger.error("IOException from : " + _serviceUrl + ", method : " + method.getParameter("method"));
        } catch(Throwable e) {
            s_logger.error("Exception from : " + _serviceUrl + ", method : " + method.getParameter("method") + ", exception :", e);
        }

        return result;
    }
    
    private HttpClient getHttpClient() {

    	if(s_client == null) {
    		MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
    		mgr.getParams().setDefaultMaxConnectionsPerHost(4);
    		
    		// TODO make it configurable
    		mgr.getParams().setMaxTotalConnections(1000);
    		
	        s_client = new HttpClient(mgr);
	        HttpClientParams clientParams = new HttpClientParams();
	        clientParams.setSoTimeout(_requestTimeoutSeconds * 1000);
	        
	        s_client.setParams(clientParams);
    	}
    	return s_client;
    }

    // for test purpose only
    public static void main(String[] args) {
/*
        ClusterServiceServletImpl service = new ClusterServiceServletImpl("http://localhost:9090/clusterservice", 300);
        try {
            String result = service.execute("test", 1, "{ p1:v1, p2:v2 }", true);
            System.out.println(result);
        } catch (RemoteException e) {
        }
*/        
    }
}
