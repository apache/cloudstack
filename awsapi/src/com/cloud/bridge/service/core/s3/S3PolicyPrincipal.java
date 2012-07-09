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
package com.cloud.bridge.service.core.s3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class S3PolicyPrincipal {

	private List<String> principalList = new ArrayList<String>();
	
	public S3PolicyPrincipal() {
	}

	public String[] getPrincipals() {
		return principalList.toArray(new String[0]);
	}
	
	public void addPrincipal(String principal) {
		principalList.add( principal );
	}
	
	public boolean contains(String findPrincipal) {
		Iterator<String> itr = principalList.iterator();
	    while( itr.hasNext()) 
	    {
	    	// -> "You can specify multiple principals, or a wildcard (*) to indicate all possible users."
	    	String onePrincipal = itr.next();
	    	if (onePrincipal.equals("*")) return true;
	    	if (onePrincipal.equals( findPrincipal )) return true;
	    }
	    return false;
	}

	public String toString() {
		
		StringBuffer value = new StringBuffer();
		Iterator<String> itr = principalList.iterator();
		
		value.append( "Principals: \n" );
		while( itr.hasNext()) {
			String onePrincipal = itr.next();
			value.append( onePrincipal );
			value.append( "\n" );
		}
		
		return value.toString();
	}
}
