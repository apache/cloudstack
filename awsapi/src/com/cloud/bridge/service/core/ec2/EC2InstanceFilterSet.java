/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import com.cloud.bridge.service.EC2SoapServiceImpl;
import com.cloud.bridge.service.exception.EC2ServiceException;


public class EC2InstanceFilterSet {

	protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();    
	
	private Map<String,String> filterTypes = new HashMap<String,String>();


	public EC2InstanceFilterSet() 
	{
		// -> use these values to check that the proper filter is passed to this type of filter set
		filterTypes.put( "availability-zone",    "string"  );
		filterTypes.put( "hypervisor",           "string"  );
		filterTypes.put( "image-id",             "string"  );
		filterTypes.put( "instance-id",          "string"  );
		filterTypes.put( "instance-type",        "string"  );
		filterTypes.put( "instance-state-code",  "integer" );
		filterTypes.put( "instance-state-name",  "string"  );
		filterTypes.put( "ip-address",           "string"  );	
		filterTypes.put( "owner-id",             "string"  );	
		filterTypes.put( "root-device-name",     "string"  );
		filterTypes.put( "private-ip-address",   "string"  );
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
	 * For a filter to match an instance just one of its values has to match the volume.
	 * For an instance to be included in the instance response it must pass all the defined filters.
	 * 
	 * @param sampleList - list of instances to test against the defined filters
	 * @return EC2DescribeInstancesResponse
	 * @throws ParseException 
	 */
	public EC2DescribeInstancesResponse evaluate( EC2DescribeInstancesResponse sampleList ) throws ParseException 
	{
		EC2DescribeInstancesResponse resultList = new EC2DescribeInstancesResponse();
    	boolean matched;
    	
    	EC2Instance[] instanceSet = sampleList.getInstanceSet();
    	EC2Filter[]   filterSet   = getFilterSet();
    	for( int i=0; i < instanceSet.length; i++ )
    	{
    		matched = true;
    		for( int j=0; j < filterSet.length; j++ )
    		{
    			if (!filterMatched( instanceSet[i], filterSet[j] )) {
    				matched = false;
    				break;
    			}
    		}
    		
    		if (matched) resultList.addInstance( instanceSet[i] );
    	}

		return resultList;
	}
	
	
	private boolean filterMatched( EC2Instance vm, EC2Filter filter ) throws ParseException
	{
		String filterName = filter.getName();
		String[] valueSet = filter.getValueSet();
		
		// TODO: add test of security group the instance is in
	    if ( filterName.equalsIgnoreCase( "availability-zone" )) 
	    {
	    	 return containsString( vm.getZoneName(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "hypervisor" ))
	    {
	         return containsString( vm.getHypervisor(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "image-id" )) 
	    {	
	    	 return containsString( vm.getTemplateId(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "instance-id" )) 
	    {	
	    	 return containsString( vm.getId(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "instance-type" ))
	    {
	         return containsString( vm.getServiceOffering(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "instance-state-code" )) 
	    {
	    	 return containsInteger( EC2SoapServiceImpl.toAmazonCode( vm.getState()), valueSet );		
	    }
	    else if (filterName.equalsIgnoreCase( "instance-state-name" )) 
	    {
	    	 return containsString( EC2SoapServiceImpl.toAmazonStateName( vm.getState()), valueSet );		
	    }
	    else if (filterName.equalsIgnoreCase( "ip-address" )) 
	    {
	         return containsString( vm.getIpAddress(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "private-ip-address" )) 
	    {
	         return containsString( vm.getPrivateIpAddress(), valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "owner-id" )) 
	    {	
	    	 String owner = new String( vm.getDomainId() + ":" + vm.getAccountName()); 
	    	 return containsString( owner, valueSet );	
	    }
	    else if (filterName.equalsIgnoreCase( "root-device-name" )) 
	    {
	         return containsDevice( vm.getRootDeviceId(), valueSet );	
	    }
	    else return false;
	}
	
	
	private boolean containsString( String lookingFor, String[] set )
	{
		if (null == lookingFor) return false;
		
	    for( int i=0; i < set.length; i++ )
	    {
	    	//System.out.println( "contsinsString: " + lookingFor + " " + set[i] );
	    	if (lookingFor.matches( set[i] )) return true;
	    }
	    return false;
	}
	
	
	private boolean containsInteger( int lookingFor, String[] set )
	{
        for( int i=0; i < set.length; i++ )
        {
	    	//System.out.println( "contsinsInteger: " + lookingFor + " " + set[i] );
        	int temp = Integer.parseInt( set[i] );
        	if (lookingFor == temp) return true;
        }
		return false;
	}

	
	private boolean containsDevice( String deviceId, String[] set )
	{
	    Integer devId = new Integer(deviceId);
        for( int i=0; i < set.length; i++ )
        {
	    	//System.out.println( "contsinsDevice: " + deviceId + " " + set[i] );
        	switch( devId ) {
        	case 1:
       		     if (( "/dev/sdb" ).matches( set[i] )) return true;
    		     if (( "/dev/xvdb").matches( set[i] )) return true;
        		 break;
        		 
        	case 2:
       		     if (( "/dev/sdc"  ).matches( set[i] )) return true;
    		     if (( "/dev/xvdc" ).matches( set[i] )) return true;
        		 break;
        		 
        	case 4:
       		     if (( "/dev/sde"  ).matches( set[i] )) return true;
    		     if (( "/dev/xvde" ).matches( set[i] )) return true;
        		 break;
        		 
        	case 5:
      		     if (( "/dev/sdf"  ).matches( set[i] )) return true;
   		         if (( "/dev/xvdf" ).matches( set[i] )) return true;
       		     break;

        	case 6:
     		     if (( "/dev/sdg"  ).matches( set[i] )) return true;
  		         if (( "/dev/xvdg" ).matches( set[i] )) return true;
      		     break;

        	case 7:
    		     if (( "/dev/sdh"  ).matches( set[i] )) return true;
 		         if (( "/dev/xvdh" ).matches( set[i] )) return true;
     		     break;

        	case 8:
    		     if (( "/dev/sdi"  ).matches( set[i] )) return true;
 		         if (( "/dev/xvdi" ).matches( set[i] )) return true;
     		     break;

        	case 9:
    		     if (( "/dev/sdj"  ).matches( set[i] )) return true;
 		         if (( "/dev/xvdj" ).matches( set[i] )) return true;
     		     break;
        	}
        }
		return false;
	}	
}
