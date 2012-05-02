package com.cloud.test.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;


import com.cloud.test.demo.response.ApiConstants;
import com.cloud.test.demo.response.CloudStackIpAddress;
import com.cloud.test.demo.response.CloudStackPortForwardingRule;
import com.cloud.test.demo.response.CloudStackUserVm;
import com.cloud.test.demo.response.CloudStackServiceOffering;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class CloudStackHttpClient {
    
    
    /*public static void main (String[] args) {
        
        try {
            //list serviceOfferings
            String requestUrl = "http://localhost:8096/client/api?command=listServiceOfferings&issystem=false&response=json";
            List<CloudStackServiceOffering> offerings = execute(requestUrl, "listserviceofferingsresponse", "serviceoffering", new TypeToken<List<CloudStackServiceOffering>>() {}.getType());
            
            for(CloudStackServiceOffering offering : offerings){
                System.out.println("id: "+ offering.getId());
                System.out.println("name: "+ offering.getName());
            }
            
            //list VMs
            requestUrl = "http://localhost:8096/client/api?command=listVirtualMachines&listAll=true&response=json";
            List<CloudStackUserVm> vmList = execute(requestUrl, "listvirtualmachinesresponse", "virtualmachine", new TypeToken<List<CloudStackUserVm>>() {}.getType() );
            
            for(CloudStackUserVm vm : vmList){
                System.out.println("id: "+ vm.getId());
                System.out.println("state: "+ vm.getState());
            }
            /*
            //list PF rules
            requestUrl = "http://localhost:8096/client/api?command=listPortForwardingRules&response=json";
            List<CloudStackPortForwardingRule> pfList = execute(requestUrl, "listportforwardingrulesresponse", "portforwardingrule", new TypeToken<List<CloudStackPortForwardingRule>>() {}.getType() );
            
            for(CloudStackPortForwardingRule pf : pfList){
                System.out.println("id: "+ pf.getId());
            }

            //list IP
            requestUrl = "http://localhost:8096/client/api?command=listPublicIpAddresses&response=json";
            List<CloudStackIpAddress> ipList = execute(requestUrl, "listpublicipaddressesresponse", "publicipaddress", new TypeToken<List<CloudStackIpAddress>>() {}.getType() );
            
            for(CloudStackIpAddress ipaddress : ipList){
                System.out.println("id: "+ ipaddress.getId());
            }
            
            requestUrl = "http://localhost:8096/client/api?command=deployVirtualMachine&response=json&serviceofferingid=afd925fa-995a-41d2-97f2-5ff475393946&zoneid=97356452-8f0d-429a-8b10-c8694d8a194c&templateid=83ad7959-c0ee-4625-b235-f9a66c0826d8&size=0";
            CloudStackUserVm vm = execute(requestUrl, true, "deployvirtualmachineresponse", "virtualmachine", CloudStackUserVm.class);
            System.out.println("id: "+ vm.getId());
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }*/
    
    
    public <T> List<T> execute(String request, String responseName, String responseObjName, Type collectionType){
        JsonElement jsonElement;
        try {
            jsonElement = execute(request);

            JsonElement response = getChildElement(jsonElement,responseName);
            JsonElement responseObj = getChildElement(response,responseObjName);

            if(responseObj == null){
                return (new Gson()).fromJson(new JsonArray(), collectionType);
            }
            
            if(responseObj.isJsonArray()){
                JsonArray responseObjElementArray = responseObj.getAsJsonArray();
                for(JsonElement responseObjElement : responseObjElementArray){
                    System.out.println(responseObjName + ":" + responseObjElement.toString());
                }
                return (new Gson()).fromJson(responseObjElementArray, collectionType);
           }            
        } catch (Exception e) {
            // TODO Auto-generated catch block
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
            long _pollTimeoutMs = 600000;
            long _pollIntervalMs = 2000;            // 1 second polling interval
            
            try{
                JsonElement jsonElement = execute(request);
                if(followToAsyncResult) {
                    
                    long startMs = System.currentTimeMillis();
    
                    String queryJobResult = "http://localhost:8096/client/api?command=queryAsyncJobResult&response=json&jobId=";
    
                    JsonElement response = getChildElement(jsonElement, responseName);
                    JsonElement jobIdEle = getChildElement(response, "jobid");
                    
                    String jobId = jobIdEle.getAsString();
                    queryJobResult = queryJobResult+jobId;
                    
                    while(System.currentTimeMillis() -  startMs < _pollTimeoutMs) {
    
                        JsonElement queryAsyncJobResponse = execute(queryJobResult);
                        JsonElement response2 = getChildElement(queryAsyncJobResponse, "queryasyncjobresultresponse");
                        
    
                        if(response2 != null) {
                            JsonElement jobStatusEle = getChildElement(response2, "jobstatus");
                            int jobStatus = jobStatusEle.getAsInt();
                            switch(jobStatus) {
                            case 2:
                                throw new Exception();//queryAsyncJobResponse.getAsString("queryasyncjobresultresponse.jobresult.errorcode") + " " + 
                                    //queryAsyncJobResponse.getAsString("queryasyncjobresultresponse.jobresult.errortext"));
                                
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
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
    
    private JsonElement execute(String request) throws Exception {
        JsonParser parser = new JsonParser();
        URL url = new URL(request);
        
        System.out.println("Cloud API call + [" + url.toString() + "]");
        
        URLConnection connect = url.openConnection();
        
        int statusCode;
        statusCode = ((HttpURLConnection)connect).getResponseCode();
        if(statusCode >= 400) {
            System.out.println("Cloud API call + [" + url.toString() + "] failed with status code: " + statusCode);
            throw new IOException("CloudStack API call HTTP response error, HTTP status code: " + statusCode);
        }
        
        InputStream inputStream = connect.getInputStream(); 
        JsonElement jsonElement = parser.parse(new InputStreamReader(inputStream));
        if(jsonElement == null) {
            System.out.println("Cloud API call + [" + url.toString() + "] failed: unable to parse expected JSON response");
            
            throw new IOException("CloudStack API call error : invalid JSON response");
        }
        
        System.out.println("Cloud API call + [" + url.toString() + "] returned: " + jsonElement.toString());
        return jsonElement;

    }

}
