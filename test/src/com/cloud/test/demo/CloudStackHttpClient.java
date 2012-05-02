package com.cloud.test.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CloudStackHttpClient {
    
    public <T> List<T> execute(String request, String responseName, String responseObjName, Type collectionType){
        try {
            JsonElement jsonElement = execute(request);

            JsonElement response = getChildElement(jsonElement,responseName);
            JsonElement responseObj = getChildElement(response,responseObjName);

            if(responseObj == null){
                return (new Gson()).fromJson(new JsonArray(), collectionType);
            }
            
            if(responseObj.isJsonArray()){
                return (new Gson()).fromJson(responseObj.getAsJsonArray(), collectionType);
           }            
        } catch (Exception e) {
            e.printStackTrace();
            return (new Gson()).fromJson(new JsonArray(), collectionType);
        }

        return (new Gson()).fromJson(new JsonArray(), collectionType);
    }
    
    private JsonElement getChildElement(JsonElement parent, String childName){
        JsonObject obj = parent.getAsJsonObject();
        JsonElement child = obj.get(childName);
        return child;
    }
    
    public <T> T execute(String request, boolean followToAsyncResult, 
            String responseName, String responseObjName, Class<T> responseClz){
            
            assert(responseName != null);
            long _pollTimeoutMs = 600000;//10 mins timeout
            long _pollIntervalMs = 2000; // 1 second polling interval
            
            try{
                JsonElement jsonElement = execute(request);
                if(followToAsyncResult) {
                    
                    long startMs = System.currentTimeMillis();
                    
                    JsonElement response = getChildElement(jsonElement, responseName);
                    JsonElement jobIdEle = getChildElement(response, "jobid");
                    
                    String jobId = jobIdEle.getAsString();
                    String queryJobResult = Demo.getQueryAsyncCommandString(jobId);
                    
                    while(System.currentTimeMillis() -  startMs < _pollTimeoutMs) {
    
                        JsonElement queryAsyncJobResponse = execute(queryJobResult);
                        JsonElement response2 = getChildElement(queryAsyncJobResponse, "queryasyncjobresultresponse");
                        
    
                        if(response2 != null) {
                            JsonElement jobStatusEle = getChildElement(response2, "jobstatus");
                            int jobStatus = jobStatusEle.getAsInt();
                            switch(jobStatus) {
                            case 2:
                                JsonElement joberrorObj = getChildElement(response2,"jobresult");
                                JsonElement errorCodeObj = getChildElement(joberrorObj,"errorcode");
                                JsonElement errorTextObj = getChildElement(joberrorObj,"errortext");
                                throw new Exception("Error: "+errorCodeObj.getAsString() + " " + errorTextObj.getAsString());
                            case 0 :
                                try { 
                                    Thread.sleep( _pollIntervalMs ); 
                                } catch( Exception e ) {}
                                break;
                                
                            case 1 :
                                JsonElement jobresultObj = getChildElement(response2,"jobresult");
                                if(jobresultObj == null){
                                    return null;
                                }
                                
                                JsonElement responseObj = getChildElement(jobresultObj,responseObjName);
                                return (T)(new Gson()).fromJson(responseObj, responseClz);
    
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
                    
                    JsonElement response = getChildElement(jsonElement,responseName);
                    JsonElement responseObj = getChildElement(response,responseObjName);
                    if(responseObj == null){
                        return null;
                    }
                    return (T)(new Gson()).fromJson(responseObj, responseClz);
                }
            }catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    

    private JsonElement execute(String request) throws Exception {
        JsonParser parser = new JsonParser();
        URL url = new URL(request);
        
        //System.out.println("Cloud API call + [" + url.toString() + "]");
        
        URLConnection connect = url.openConnection();
        
        int statusCode;
        statusCode = ((HttpURLConnection)connect).getResponseCode();
        if(statusCode >= 400) {
            //System.out.println("Cloud API call + [" + url.toString() + "] failed with status code: " + statusCode);
            throw new IOException("CloudStack API call HTTP response error, HTTP status code: " + statusCode);
        }
        
        InputStream inputStream = connect.getInputStream(); 
        JsonElement jsonElement = parser.parse(new InputStreamReader(inputStream));
        if(jsonElement == null) {
            System.out.println("Cloud API call + [" + url.toString() + "] failed: unable to parse expected JSON response");
            
            throw new IOException("CloudStack API call error : invalid JSON response");
        }
        
        //System.out.println("Cloud API call + [" + url.toString() + "] returned: " + jsonElement.toString());
        return jsonElement;

    }

}
