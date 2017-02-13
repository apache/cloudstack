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
package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.exception.ExecutionException;
// http client handling
// for prettyFormat()

public class MockableVyosRouterResource extends VyosRouterResource {
    private HashMap<String, String> context;

    public void setMockContext(HashMap<String, String> context) {
        this.context = context;
    }

    /* Fake the calls to the Vyos Router */
    @Override
    protected String request(VyosRouterMethod method, VyosRouterCommandType commandType, ArrayList<String> commands) throws ExecutionException {
        if (method != VyosRouterMethod.SHELL && method != VyosRouterMethod.HTTPSTUB) {
            throw new ExecutionException("Invalid method used to access the Vyos Router.");
        }
        
        String response = "";
        
        //Allow the tests to be run against an actual vyos instance instead of returning mock results.
        if (context.containsKey("use_test_router") && context.get("use_test_router").equals("true")) {
        	//System.out.println("Testing against an actual Vyos instance.");
        	response = super.request(method, commandType, commands);
        	//System.out.println("response: "+response);
        } else {
        	
	    	switch (commandType) {
	    		case READ:
	    			System.out.println("Mock Read request");
	    			break;
	    			
	    		case WRITE:
	    			System.out.println("Mock Write request");
	    			break;
	    		
	    		default:
	    			System.out.println("ERROR command type not supported. Type: "+commandType);
	    			break;
	    	}        	
	    	System.out.println("Commands in current Request: ");
	        for (String curCommand : commands) {        	
	        		System.out.println(curCommand);
	        	
	        }
        }
        		
        
        return response;
    }
}