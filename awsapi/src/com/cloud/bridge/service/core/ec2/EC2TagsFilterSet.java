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

public class EC2TagsFilterSet {
    protected final static Logger logger = Logger.getLogger(EC2TagsFilterSet.class);

    protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();

    private Map<String,String> filterTypes = new HashMap<String,String>();

    public EC2TagsFilterSet() {
        filterTypes.put( "resource-id", "String" );
        filterTypes.put( "resource-type", "String" );
        filterTypes.put( "key", "String" );
        filterTypes.put( "value", "String" );
    }

    public void addFilter( EC2Filter param ) {
        String filterName = param.getName();
        String value = (String) filterTypes.get( filterName );

        if ( value == null || value.equalsIgnoreCase("null") ) {
            throw new EC2ServiceException( ClientError.InvalidFilter, "Filter '" + filterName + "' is invalid");
        }

        filterSet.add( param );
    }

    public EC2Filter[] getFilterSet() {
        return filterSet.toArray(new EC2Filter[0]);
    }

    public EC2DescribeTagsResponse evaluate( EC2DescribeTagsResponse sampleList) throws ParseException	{
        EC2DescribeTagsResponse resultList = new EC2DescribeTagsResponse();

        boolean matched;

        EC2ResourceTag[] tagSet = sampleList.getTagsSet();
        EC2Filter[] filterSet = getFilterSet();
        for (EC2ResourceTag tag : tagSet) {
            matched = true;
            for (EC2Filter filter : filterSet) {
                if (!filterMatched(tag, filter)) {
                    matched = false;
                    break;
                }
            }
            if (matched == true)
                resultList.addTags(tag);
        }
        return resultList;
    }

    private boolean filterMatched( EC2ResourceTag tag, EC2Filter filter ) throws ParseException {
        String filterName = filter.getName();
        String[] valueSet = filter.getValueSet();

        if ( filterName.equalsIgnoreCase("resource-id")) {
            return containsString(tag.getResourceId(), valueSet);
        } else if ( filterName.equalsIgnoreCase("resource-type")) {
            return containsString(tag.getResourceType(), valueSet);
        } else if ( filterName.equalsIgnoreCase("key")) {
            return containsString(tag.getKey(), valueSet);
        } else if ( filterName.equalsIgnoreCase("value")) {
            return containsString(tag.getValue(), valueSet);
        } else
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
