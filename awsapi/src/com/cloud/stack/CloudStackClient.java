// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.stack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.bridge.util.JsonAccessor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * CloudStackClient implements a simple CloudStack client object, it can be used to execute CloudStack commands 
 * with JSON response
 * 
 */
public class CloudStackClient {
    protected final static Logger logger = Logger.getLogger(CloudStackClient.class);
    
	private String _serviceUrl;
	
	private long _pollIntervalMs = 2000;			// 1 second polling interval
	private long _pollTimeoutMs = 600000;			// 10 minutes polling timeout

	public CloudStackClient(String serviceRootUrl) {
		assert(serviceRootUrl != null);
		
		if(!serviceRootUrl.endsWith("/"))
			_serviceUrl = serviceRootUrl + "/api?";
		else
			_serviceUrl = serviceRootUrl + "api?";
	}
	
	public CloudStackClient(String cloudStackServiceHost, int port, boolean bSslEnabled) {
		StringBuffer sb = new StringBuffer();
		if(!bSslEnabled) {
			sb.append("http://" + cloudStackServiceHost);
			if(port != 80)
				sb.append(":").append(port);
		} else {
			sb.append("https://" + cloudStackServiceHost);
			if(port != 443)
				sb.append(":").append(port);
		}
		
		//
		// If the CloudStack root context path has been from /client to some other name
		// use the first constructor instead
		//
		sb.append("/client/api");
		sb.append("?");
		_serviceUrl = sb.toString();
	}
	
	public CloudStackClient setPollInterval(long intervalMs) {
		_pollIntervalMs = intervalMs;
		return this;
	}
	
	public CloudStackClient setPollTimeout(long pollTimeoutMs) {
		_pollTimeoutMs = pollTimeoutMs;
		return this;
	}
	
	public <T> T call(CloudStackCommand cmd, String apiKey, String secretKey, boolean followToAsyncResult, 
		String responseName, String responseObjName, Class<T> responseClz)	throws Exception {
		
		assert(responseName != null);
		
		JsonAccessor json = execute(cmd, apiKey, secretKey);
		if(followToAsyncResult && json.tryEval(responseName + ".jobid") != null) {
			long startMs = System.currentTimeMillis();
	        while(System.currentTimeMillis() -  startMs < _pollTimeoutMs) {
				CloudStackCommand queryJobCmd = new CloudStackCommand("queryAsyncJobResult");
	        	queryJobCmd.setParam("jobId", json.getAsString(responseName + ".jobid"));
	        	
	        	JsonAccessor queryAsyncJobResponse = execute(queryJobCmd, apiKey, secretKey);

	    		if(queryAsyncJobResponse.tryEval("queryasyncjobresultresponse") != null) {
	    			int jobStatus = queryAsyncJobResponse.getAsInt("queryasyncjobresultresponse.jobstatus");
	    			switch(jobStatus) {
	    			case 2:
	    	    		throw new Exception(queryAsyncJobResponse.getAsString("queryasyncjobresultresponse.jobresult.errortext") + " Error Code - " + 
	    	    		queryAsyncJobResponse.getAsString("queryasyncjobresultresponse.jobresult.errorcode") );
	    	    		
	    			case 0 :
	            	    try { 
	            	    	Thread.sleep( _pollIntervalMs ); 
	            	    } catch( Exception e ) {}
	            	    break;
	            	    
	    			case 1 :
	    				if(responseObjName != null)
	    					return (T)(new Gson()).fromJson(queryAsyncJobResponse.eval("queryasyncjobresultresponse.jobresult." + responseObjName), responseClz);
	    				else
	    					return (T)(new Gson()).fromJson(queryAsyncJobResponse.eval("queryasyncjobresultresponse.jobresult"), responseClz);
	    				
	    			default :
	    				assert(false);
	                    throw new Exception("Operation failed - invalid job status response");
	    			}
	    		} else {
	                throw new Exception("Operation failed - invalid JSON response");
	    		}
	        }
	        
            throw new Exception("Operation failed - async-job query timed out");
		} else {
			if (responseObjName != null)
				return (T)(new Gson()).fromJson(json.eval(responseName + "." + responseObjName), responseClz);
			else
				return (T)(new Gson()).fromJson(json.eval(responseName), responseClz);
		}
	}

	// collectionType example :  new TypeToken<List<String>>() {}.getType();
	public <T> List<T> listCall(CloudStackCommand cmd, String apiKey, String secretKey, 
		String responseName, String responseObjName, Type collectionType) throws Exception {
		
		assert(responseName != null);
		
		JsonAccessor json = execute(cmd, apiKey, secretKey);
		
		

		if(responseObjName != null)
			try {
				return (new Gson()).fromJson(json.eval(responseName + "." + responseObjName), collectionType);
			} catch(Exception e) {
				// this happens because responseObjName won't exist if there are no objects in the list.
                                logger.debug("CloudSatck API response doesn't contain responseObjName:" + responseObjName +
                                        " because response is empty");
				return null;
			}
		return (new Gson()).fromJson(json.eval(responseName), collectionType);
	}

	public JsonAccessor execute(CloudStackCommand cmd, String apiKey, String secretKey) throws Exception {
		JsonParser parser = new JsonParser();
		URL url = new URL(_serviceUrl + cmd.signCommand(apiKey, secretKey));
		
		if(logger.isDebugEnabled())
			logger.debug("Cloud API call + [" + url.toString() + "]");
		
        URLConnection connect = url.openConnection();
        
        int statusCode;
        statusCode = ((HttpURLConnection)connect).getResponseCode();
        if(statusCode >= 400) {
        	logger.error("Cloud API call + [" + url.toString() + "] failed with status code: " + statusCode);
            String errorMessage = ((HttpURLConnection)connect).getResponseMessage();
            if(errorMessage == null){
                errorMessage = connect.getHeaderField("X-Description");
            }
            
            if(errorMessage == null){
                errorMessage = "CloudStack API call HTTP response error, HTTP status code: " + statusCode;
            }
            errorMessage = errorMessage.concat(" Error Code - " + Integer.toString(statusCode));

        	throw new IOException(errorMessage);
        }
        
        InputStream inputStream = connect.getInputStream(); 
		JsonElement jsonElement = parser.parse(new InputStreamReader(inputStream));
		if(jsonElement == null) {
        	logger.error("Cloud API call + [" + url.toString() + "] failed: unable to parse expected JSON response");
        	
        	throw new IOException("CloudStack API call error : invalid JSON response");
		}
		
		if(logger.isDebugEnabled())
			logger.debug("Cloud API call + [" + url.toString() + "] returned: " + jsonElement.toString());
		return new JsonAccessor(jsonElement);
	}
}
