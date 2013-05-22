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

package com.cloud.bridge.service.core.ec2;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;

public class EC2AddressFilterSet {
	protected final static Logger logger = Logger.getLogger(EC2KeyPairFilterSet.class);
	
	protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();    
	
	private Map<String,String> filterTypes = new HashMap<String,String>();
	
	public EC2AddressFilterSet() {
		// -> use these values to check that the proper filter is passed to this type of filter set
		filterTypes.put( "instance-id", "String" );
		filterTypes.put( "public-ip", "String" );
	}

	public void addFilter( EC2Filter param ) {	
		String filterName = param.getName();
		String value = (String) filterTypes.get( filterName );
		
        if ( value == null || value.equalsIgnoreCase("null") ) {
            throw new EC2ServiceException( ClientError.InvalidFilter, "Filter '" + filterName + "' is invalid");
        }

		// ToDo we could add checks to make sure the type of a filters value is correct (e.g., an integer)
		filterSet.add( param );
	}
	
	public EC2Filter[] getFilterSet() {
		return filterSet.toArray(new EC2Filter[0]);
	}


    public EC2DescribeAddressesResponse evaluate( EC2DescribeAddressesResponse response) throws ParseException	{
		EC2DescribeAddressesResponse resultList = new EC2DescribeAddressesResponse();
		
		boolean matched;
		
        EC2Address[] addresses = response.getAddressSet();
		EC2Filter[] filterSet = getFilterSet();
		for ( EC2Address address : addresses ) {
			matched = true;
            for (EC2Filter filter : filterSet) {
                if (!filterMatched(address, filter)) {
                    matched = false;
                    break;
                }
            }
			if (matched == true)
				resultList.addAddress(address);

		}
		return resultList;
	}

	private boolean filterMatched( EC2Address address, EC2Filter filter ) throws ParseException {
		String filterName = filter.getName();
		String[] valueSet = filter.getValueSet();
		
		if ( filterName.equalsIgnoreCase("instance-id")) {
			return containsString(address.getAssociatedInstanceId(), valueSet);
		} else if ( filterName.equalsIgnoreCase("public-ip")) {
			return containsString(address.getIpAddress(), valueSet);
		}
		return false;
	}
	
	private boolean containsString( String lookingFor, String[] set ){
		if (lookingFor == null) 
			return false;
		
		for (String filter: set) {
			if (lookingFor.matches( filter )) return true;
		}
		return false;
	}

}


