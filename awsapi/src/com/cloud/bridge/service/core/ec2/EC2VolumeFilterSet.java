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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Date;

import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.util.EC2RestAuth;


public class EC2VolumeFilterSet {

	protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();    

	private Map<String,String> filterTypes = new HashMap<String,String>();


	public EC2VolumeFilterSet() 
	{
		// -> use these values to check that the proper filter is passed to this type of filter set
		filterTypes.put( "attachment.attach-time",           "xsd:dateTime" );
		filterTypes.put( "attachment.delete-on-termination", "null"         );
		filterTypes.put( "attachment.device",                "string"       );
		filterTypes.put( "attachment.instance-id",           "string"       );
		filterTypes.put( "attachment.status",                "set:attached|attaching|detached|detaching" );
		filterTypes.put( "availability-zone",                "string"       );
		filterTypes.put( "create-time",                      "xsd:dateTime" );
		filterTypes.put( "size",                             "integer"      );
		filterTypes.put( "snapshot-id",                      "string"       );
		filterTypes.put( "status",                           "set:creating|available|in-use|deleting|deleted|error" );
		filterTypes.put( "tag-key",                          "string"         );
		filterTypes.put( "tag-value",                        "string"         );
		filterTypes.put( "volume-id",                        "string"       );	
		//		filterTypes.put( "tag:*",                            "null" );
	}


	public void addFilter( EC2Filter param ) 
	{	
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


	/**
	 * For a filter to match a volume just one of its values has to match the volume.
	 * For a volume to be included in the volume response it must pass all the defined filters.
	 * 
	 * @param sampleList - list of volumes to test against the defined filters
	 * @return EC2DescribeVolumeResponse
	 * @throws ParseException 
	 */
	public EC2DescribeVolumesResponse evaluate( EC2DescribeVolumesResponse sampleList ) throws ParseException 
	{
		EC2DescribeVolumesResponse resultList = new EC2DescribeVolumesResponse();
		boolean matched;

		EC2Volume[] volumeSet = sampleList.getVolumeSet();
		EC2Filter[] filterSet = getFilterSet();
		for (EC2Volume vol : volumeSet) {
			matched = true;
			for (EC2Filter filter : filterSet) {
				if (!filterMatched( vol, filter )) {
					matched = false;
					break;
				}
			}

			if (matched) resultList.addVolume( vol );
		}

		return resultList;
	}


	private boolean filterMatched( EC2Volume vol, EC2Filter filter ) throws ParseException
	{
		String filterName = filter.getName();
		String[] valueSet = filter.getValueSet();

		if ( filterName.equalsIgnoreCase( "availability-zone" )) 
			return containsString( vol.getZoneName(), valueSet );	
		else if (filterName.equalsIgnoreCase( "create-time" )) 
			return containsTime(vol.getCreated(), valueSet );	
		else if (filterName.equalsIgnoreCase( "size" ))  
			return containsLong(vol.getSize(), valueSet );	
		else if (filterName.equalsIgnoreCase( "snapshot-id" )) 
			return containsString(String.valueOf(vol.getSnapshotId()), valueSet );	
		else if (filterName.equalsIgnoreCase( "status" )) 
			return containsString(vol.getState(), valueSet );	
		else if (filterName.equalsIgnoreCase( "volume-id" )) 
			return containsString(vol.getId().toString(), valueSet );	
        else if (filterName.equalsIgnoreCase( "attachment.attach-time" )) {
            if (vol.getAttached() != null)
                return containsTime(vol.getAttached(), valueSet );
            else if (vol.getInstanceId() != null)
                return containsTime(vol.getCreated(), valueSet);
            else
                return false;
        }	
		else if (filterName.equalsIgnoreCase( "attachment.device" )) 
			return containsDevice(vol.getDeviceId(), valueSet );	
		else if (filterName.equalsIgnoreCase( "attachment.instance-id" )) 
			return containsString(String.valueOf(vol.getInstanceId()), valueSet );
        else if ( filterName.equalsIgnoreCase( "attachment.status" ) ) {
            return containsString(vol.getAttachmentState(), valueSet );
        }
        else if (filterName.equalsIgnoreCase("tag-key"))
        {
            EC2TagKeyValue[] tagSet = vol.getResourceTags();
            for (EC2TagKeyValue tag : tagSet)
                if (containsString(tag.getKey(), valueSet)) return true;
            return false;
        }
        else if (filterName.equalsIgnoreCase("tag-value"))
        {
            EC2TagKeyValue[] tagSet = vol.getResourceTags();
            for (EC2TagKeyValue tag : tagSet){
                if (tag.getValue() == null) {
                    if (containsEmptyValue(valueSet)) return true;
                }
                else {
                    if (containsString(tag.getValue(), valueSet)) return true;
                }
            }
            return false;
        }
		else return false;
	}


	private boolean containsString( String lookingFor, String[] set )
	{
		if (null == lookingFor) return false;
		for (String s : set) {
			if (lookingFor.matches( s )) return true;
		}
		return false;
	}

    private boolean containsEmptyValue( String[] set )
    {
        for( int i=0; i < set.length; i++ )
            if (set[i].isEmpty()) return true;
        return false;
    }

	private boolean containsLong( long lookingFor, String[] set )
	{
		for (String s : set) {
			int temp = Integer.parseInt( s );
			if (lookingFor == temp) return true;
		}
		return false;
	}


	private boolean containsTime(String lookingForDate, String[] set ) throws ParseException
	{
        Calendar lookingFor = EC2RestAuth.parseDateString(lookingForDate);
        lookingFor.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date lookForDate = lookingFor.getTime();
        for (String s : set) {
            Calendar toMatch = EC2RestAuth.parseDateString(s);
            toMatch.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date toMatchDate = toMatch.getTime();
            if ( 0 == lookForDate.compareTo(toMatchDate)) return true;
        }
        return false;
    }


	private boolean containsDevice(String deviceId, String[] set )
	{
        if (deviceId == null)
        	return false;
	    Integer devId = new Integer(deviceId);
		for (String s : set) {
			switch( devId ) {
			case 1:
				if (( "/dev/sdb" ).matches( s )) return true;
				if (( "/dev/xvdb").matches( s )) return true;
				break;

			case 2:
				if (( "/dev/sdc"  ).matches( s )) return true;
				if (( "/dev/xvdc" ).matches( s )) return true;
				break;

			case 4:
				if (( "/dev/sde"  ).matches( s )) return true;
				if (( "/dev/xvde" ).matches( s )) return true;
				break;

			case 5:
				if (( "/dev/sdf"  ).matches( s )) return true;
				if (( "/dev/xvdf" ).matches( s )) return true;
				break;

			case 6:
				if (( "/dev/sdg"  ).matches( s )) return true;
				if (( "/dev/xvdg" ).matches( s )) return true;
				break;

			case 7:
				if (( "/dev/sdh"  ).matches( s )) return true;
				if (( "/dev/xvdh" ).matches( s )) return true;
				break;

			case 8:
				if (( "/dev/sdi"  ).matches( s )) return true;
				if (( "/dev/xvdi" ).matches( s )) return true;
				break;

			case 9:
				if (( "/dev/sdj"  ).matches( s )) return true;
				if (( "/dev/xvdj" ).matches( s )) return true;
				break;
			}
		}
		return false;
	}
}
