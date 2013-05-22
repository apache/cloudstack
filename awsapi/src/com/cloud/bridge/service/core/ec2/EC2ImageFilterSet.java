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

public class EC2ImageFilterSet {
    protected final static Logger logger = Logger.getLogger(EC2ImageFilterSet.class);

    protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();
    private Map<String,String> filterTypes = new HashMap<String,String>();

	public EC2ImageFilterSet() {
        // -> supported filters
        filterTypes.put( "architecture", "string" );
        filterTypes.put( "description",  "string" );
        filterTypes.put( "hypervisor",   "string" );
        filterTypes.put( "image-id",     "string" );
        filterTypes.put( "image-type",   "string" );
        filterTypes.put( "is-public",    "Boolean" );
        filterTypes.put( "name",         "string" );
        filterTypes.put( "owner-id",     "string" );
        filterTypes.put( "state",        "string" );
        filterTypes.put( "tag-key",      "string" );
        filterTypes.put( "tag-value",    "string" );
	}

    public void addFilter( EC2Filter param ) {
        String filterName = param.getName();
        if ( !filterName.startsWith("tag:") ) {
            String value = (String) filterTypes.get( filterName );
            if ( value == null || value.equalsIgnoreCase("null")) {
                throw new EC2ServiceException( ClientError.InvalidFilter, "Filter '" + filterName + "' is invalid");
            }
        }

        filterSet.add( param );
    }

    public EC2Filter[] getFilterSet() {
        return filterSet.toArray(new EC2Filter[0]);
    }

    public EC2DescribeImagesResponse evaluate( EC2DescribeImagesResponse sampleList) throws ParseException	{
        EC2DescribeImagesResponse resultList = new EC2DescribeImagesResponse();

        boolean matched;

        EC2Image[] imageSet = sampleList.getImageSet();
        EC2Filter[] filterSet = getFilterSet();
        for (EC2Image image : imageSet) {
            matched = true;
            for (EC2Filter filter : filterSet) {
                if (!filterMatched(image, filter)) {
                    matched = false;
                    break;
                }
            }
            if (matched == true)
                resultList.addImage(image);
        }
        return resultList;
    }

private boolean filterMatched( EC2Image image, EC2Filter filter ) throws ParseException {
        String filterName = filter.getName();
        String[] valueSet = filter.getValueSet();

        if ( filterName.equalsIgnoreCase( "architecture" ))
            return containsString( image.getArchitecture(), valueSet );
        if ( filterName.equalsIgnoreCase( "description" ))
            return containsString( image.getDescription(), valueSet );
        if ( filterName.equalsIgnoreCase( "hypervisor" ))
            return containsString( image.getHypervisor(), valueSet );
        if ( filterName.equalsIgnoreCase( "image-id" ))
            return containsString( image.getId(), valueSet );
        if ( filterName.equalsIgnoreCase( "image-type" ))
            return containsString( image.getImageType(), valueSet );
        if ( filterName.equalsIgnoreCase( "is-public" ))
            return image.getIsPublic().toString().equalsIgnoreCase(valueSet[0]);
        if ( filterName.equalsIgnoreCase( "name" ))
            return containsString( image.getName(), valueSet );
        if ( filterName.equalsIgnoreCase( "owner-id" )) {
            String owner = new String( image.getDomainId() + ":" + image.getAccountName());
            return containsString( owner, valueSet );
        }
        if ( filterName.equalsIgnoreCase( "state" ))
            return containsString( image.getState(), valueSet );
        else if (filterName.equalsIgnoreCase("tag-key"))
        {
            EC2TagKeyValue[] tagSet = image.getResourceTags();
            for (EC2TagKeyValue tag : tagSet)
                if (containsString(tag.getKey(), valueSet)) return true;
            return false;
        }
        else if (filterName.equalsIgnoreCase("tag-value"))
        {
            EC2TagKeyValue[] tagSet = image.getResourceTags();
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
        else if (filterName.startsWith("tag:"))
        {
            String key = filterName.split(":")[1];
            EC2TagKeyValue[] tagSet = image.getResourceTags();
            for (EC2TagKeyValue tag : tagSet){
                if (tag.getKey().equalsIgnoreCase(key)) {
                    if (tag.getValue() == null) {
                        if (containsEmptyValue(valueSet)) return true;
                    }
                    else {
                        if (containsString(tag.getValue(), valueSet)) return true;
                    }
                }
            }
            return false;
        }
        else return false;
    }

    private boolean containsString( String lookingFor, String[] set ) {
        if (lookingFor == null)
            return false;

        for (String filter: set) {
            if (lookingFor.matches( filter )) return true;
        }
        return false;
    }

    private boolean containsEmptyValue( String[] set ) {
        for( int i=0; i < set.length; i++ ) {
            if (set[i].isEmpty()) return true;
        }
        return false;
    }

}
