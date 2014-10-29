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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.cloud.bridge.service.UserContext;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.util.EC2RestAuth;

public class EC2SnapshotFilterSet {

    protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();

    private Map<String, String> filterTypes = new HashMap<String, String>();

    public EC2SnapshotFilterSet() {
        // -> use these values to check that the proper filter is passed to this type of filter set
        filterTypes.put("owner-alias", "string");
        filterTypes.put("owner-id", "string");
        filterTypes.put("snapshot-id", "string");
        filterTypes.put("start-time", "xsd:dateTime");
        filterTypes.put("status", "string");
        filterTypes.put("volume-id", "string");
        filterTypes.put("volume-size", "string");
        filterTypes.put("tag-key", "string");
        filterTypes.put("tag-value", "string");
    }

    public void addFilter(EC2Filter param) {
        String filterName = param.getName();
        if (!filterName.startsWith("tag:")) {
            String value = (String)filterTypes.get(filterName);
            if (value == null || value.equalsIgnoreCase("null")) {
                throw new EC2ServiceException(ClientError.InvalidFilter, "Filter '" + filterName + "' is invalid");
            }
        }
        // ToDo we could add checks to make sure the type of a filters value is correct (e.g., an integer)
        filterSet.add(param);
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
    public EC2DescribeSnapshotsResponse evaluate(EC2DescribeSnapshotsResponse sampleList) throws ParseException {
        EC2DescribeSnapshotsResponse resultList = new EC2DescribeSnapshotsResponse();
        boolean matched;

        EC2Snapshot[] snapshotSet = sampleList.getSnapshotSet();
        EC2Filter[] filterSet = getFilterSet();
        for (EC2Snapshot snap : snapshotSet) {
            matched = true;
            for (EC2Filter filter : filterSet) {
                if (!filterMatched(snap, filter)) {
                    matched = false;
                    break;
                }
            }

            if (matched)
                resultList.addSnapshot(snap);
        }

        return resultList;
    }

    private boolean filterMatched(EC2Snapshot snap, EC2Filter filter) throws ParseException {
        String filterName = filter.getName();
        String[] valueSet = filter.getValueSet();

        if (filterName.equalsIgnoreCase("owner-alias")) {
            return containsString(UserContext.current().getAccessKey(), valueSet);
        } else if (filterName.equalsIgnoreCase("owner-id")) {
            String owner = new String(snap.getDomainId() + ":" + snap.getAccountName());
            return containsString(owner, valueSet);
        } else if (filterName.equalsIgnoreCase("snapshot-id")) {
            return containsString(snap.getId().toString(), valueSet);
        } else if (filterName.equalsIgnoreCase("start-time")) {
            return containsTime(snap.getCreated(), valueSet);
        } else if (filterName.equalsIgnoreCase("status")) {
            if (snap.getState().equalsIgnoreCase("backedup"))
                return containsString("completed", valueSet);
            else if (snap.getState().equalsIgnoreCase("creating") || snap.getState().equalsIgnoreCase("backingup"))
                return containsString("pending", valueSet);
            else
                return containsString("error", valueSet);
        } else if (filterName.equalsIgnoreCase("volume-id")) {
            return containsString(snap.getVolumeId().toString(), valueSet);
        } else if (filterName.equalsIgnoreCase("volume-size")) {
            return containsLong(snap.getVolumeSize(), valueSet);
        } else if (filterName.equalsIgnoreCase("tag-key")) {
            EC2TagKeyValue[] tagSet = snap.getResourceTags();
            for (EC2TagKeyValue tag : tagSet)
                if (containsString(tag.getKey(), valueSet))
                    return true;
            return false;
        } else if (filterName.equalsIgnoreCase("tag-value")) {
            EC2TagKeyValue[] tagSet = snap.getResourceTags();
            for (EC2TagKeyValue tag : tagSet) {
                if (tag.getValue() == null) {
                    if (containsEmptyValue(valueSet))
                        return true;
                } else {
                    if (containsString(tag.getValue(), valueSet))
                        return true;
                }
            }
            return false;
        } else
            return false;
    }

    private boolean containsString(String lookingFor, String[] set) {
        if (null == lookingFor)
            return false;

        for (String s : set) {
            //System.out.println( "contsinsString: " + lookingFor + " " + set[i] );
            if (lookingFor.matches(s))
                return true;
        }
        return false;
    }

    private boolean containsEmptyValue(String[] set) {
        for (int i = 0; i < set.length; i++)
            if (set[i].isEmpty())
                return true;
        return false;
    }

    private boolean containsLong(long lookingFor, String[] set) {
        for (String s : set) {
            //System.out.println( "contsinsInteger: " + lookingFor + " " + set[i] );
            int temp = Integer.parseInt(s);
            if (lookingFor == temp)
                return true;
        }
        return false;
    }

    private boolean containsTime(Calendar lookingFor, String[] set) throws ParseException {
        lookingFor.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date lookingForDate = lookingFor.getTime();
        for (String s : set) {
            //System.out.println( "contsinsCalendar: " + lookingFor + " " + set[i] );
            Calendar toMatch = EC2RestAuth.parseDateString(s);
            toMatch.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date toMatchDate = toMatch.getTime();
            if (0 == lookingForDate.compareTo(toMatchDate))
                return true;
        }
        return false;
    }
}
