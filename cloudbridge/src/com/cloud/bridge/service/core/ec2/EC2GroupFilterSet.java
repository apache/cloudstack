/*
 * Copyright 2011 Cloud.com, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.service.core.ec2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.bridge.service.exception.EC2ServiceException;


public class EC2GroupFilterSet {

	protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();    

	private Map<String,String> filterTypes = new HashMap<String,String>();

	public EC2GroupFilterSet() 
	{
		// -> use these values to check that the proper filter is passed to this type of filter set
		filterTypes.put( "description",             "string" );
		filterTypes.put( "group-id",                "string" );
		filterTypes.put( "group-name",              "string" );
		filterTypes.put( "ip-permission.cidr",      "string" );
		filterTypes.put( "ip-permission.from-port", "string" );
		filterTypes.put( "ip-permission.to-port",   "string" ); 
		filterTypes.put( "ip-permission.protocol",  "string" ); 
		filterTypes.put( "owner-id",                "string" );
	}


	public void addFilter( EC2Filter param ) 
	{	
		String filterName = param.getName();
		String value = (String) filterTypes.get( filterName );

		if (null == value)
			throw new EC2ServiceException( "Unsupported filter [" + filterName + "] - 1", 501 );

		if (null != value && value.equalsIgnoreCase( "null" ))
			throw new EC2ServiceException( "Unsupported filter [" + filterName + "] - 2", 501 );

		// ToDo we could add checks to make sure the type of a filters value is correct (e.g., an integer)
		filterSet.add( param );
	}


	public EC2Filter[] getFilterSet() {
		return filterSet.toArray(new EC2Filter[0]);
	}


	/**
	 * For a filter to match a snapshot just one of its values has to match the volume.
	 * For a snapshot to be included in the instance response it must pass all the defined filters.
	 * 
	 * @param sampleList - list of snapshots to test against the defined filters
	 * @return EC2DescribeSnapshotsResponse
	 * @throws ParseException 
	 */
	public EC2DescribeSecurityGroupsResponse evaluate( EC2DescribeSecurityGroupsResponse sampleList ) throws ParseException 
	{
		EC2DescribeSecurityGroupsResponse resultList = new EC2DescribeSecurityGroupsResponse();
		boolean matched;

		EC2SecurityGroup[] groupSet = sampleList.getGroupSet();
		EC2Filter[]     filterSet   = getFilterSet();
		for (EC2SecurityGroup group : groupSet) {
			matched = true;
			for (EC2Filter filter : filterSet) {
				if (!filterMatched( group, filter)) {
					matched = false;
					break;
				}
			}

			if (matched) resultList.addGroup( group );
		}

		return resultList;
	}


	private boolean filterMatched( EC2SecurityGroup sg, EC2Filter filter ) throws ParseException
	{
		String filterName = filter.getName();
		String[] valueSet = filter.getValueSet();
		EC2IpPermission[] permissionSet = sg.getIpPermissionSet();
		boolean result = false;

		if ( filterName.equalsIgnoreCase( "description" )) 
			return containsString( sg.getDescription(), valueSet );	
		else if (filterName.equalsIgnoreCase( "group-id" )) 
			return containsString( sg.getId(), valueSet );	
		else if (filterName.equalsIgnoreCase( "group-name" )) 
			return containsString( sg.getName(), valueSet );	
		else if (filterName.equalsIgnoreCase( "ip-permission.cidr" )) {
			for (EC2IpPermission perm : permissionSet) {
				result = containsString(perm.getCIDR(), valueSet);
				if (result) return true;
			}
			return false;			
		} else if (filterName.equalsIgnoreCase( "ip-permission.from-port" )) {
			for (EC2IpPermission perm : permissionSet) {
				result = containsInteger(perm.getFromPort(), valueSet);
				if (result) return true;
			}
			return false;		
		} else if (filterName.equalsIgnoreCase( "ip-permission.to-port" )) {
			for (EC2IpPermission perm : permissionSet) {
				result = containsInteger( perm.getToPort(), valueSet );
				if (result) return true;
			}
			return false;		
		} else if (filterName.equalsIgnoreCase( "ip-permission.protocol" )) {
			for (EC2IpPermission perm : permissionSet) {
				result = containsString( perm.getProtocol(), valueSet );
				if (result) return true;
			}
			return false;	
		} else if (filterName.equalsIgnoreCase( "owner-id" )) {	
			String owner = new String( sg.getDomainId() + ":" + sg.getAccountName()); 
			return containsString( owner, valueSet );	
		}
		else return false;
	}


	private boolean containsString( String lookingFor, String[] set )
	{
		if (null == lookingFor) return false;

		for (String s : set) {
			//System.out.println( "contsinsString: " + lookingFor + " " + set[i] );
			if (lookingFor.matches( s )) return true;
		}
		return false;
	}


	private boolean containsInteger( int lookingFor, String[] set )
	{
		for (String s : set) {
			//System.out.println( "contsinsInteger: " + lookingFor + " " + set[i] );
			int temp = Integer.parseInt( s );
			if (lookingFor == temp) return true;
		}
		return false;
	}
}
