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

import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;

public class EC2GroupFilterSet {

    protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();

    private Map<String, String> filterTypes = new HashMap<String, String>();

    public EC2GroupFilterSet() {
        // -> use these values to check that the proper filter is passed to this type of filter set
        filterTypes.put("description", "string");
        filterTypes.put("group-id", "string");
        filterTypes.put("group-name", "string");
        filterTypes.put("ip-permission.cidr", "string");
        filterTypes.put("ip-permission.from-port", "string");
        filterTypes.put("ip-permission.to-port", "string");
        filterTypes.put("ip-permission.protocol", "string");
        filterTypes.put("ip-permission.group-name", "string");
        filterTypes.put("ip-permission.user-id", "string");
        filterTypes.put("owner-id", "string");
        filterTypes.put("tag-key", "string");
        filterTypes.put("tag-value", "string");
    }

    public void addFilter(EC2Filter param) {
        String filterName = param.getName();
        String value = (String)filterTypes.get(filterName);

        if (value == null || value.equalsIgnoreCase("null")) {
            throw new EC2ServiceException(ClientError.InvalidFilter, "Filter '" + filterName + "' is invalid");
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
    public EC2DescribeSecurityGroupsResponse evaluate(EC2DescribeSecurityGroupsResponse sampleList) throws ParseException {
        EC2DescribeSecurityGroupsResponse resultList = new EC2DescribeSecurityGroupsResponse();
        boolean matched;

        EC2SecurityGroup[] groupSet = sampleList.getGroupSet();
        EC2Filter[] filterSet = getFilterSet();
        for (EC2SecurityGroup group : groupSet) {
            List<EC2Filter> ipPermissionFilterSet = new ArrayList<EC2Filter>();
            matched = true;
            for (EC2Filter filter : filterSet) {
                if (filter.getName().startsWith("ip-permission"))
                    ipPermissionFilterSet.add(filter);
                else {
                    if (!filterMatched(group, filter)) {
                        matched = false;
                        break;
                    }
                }
            }
            if (matched) {
                if (ipPermissionFilterSet.isEmpty() || ipPermissionFilterMatched(group, ipPermissionFilterSet))
                    resultList.addGroup(group);
            }
        }
        return resultList;
    }

    private boolean filterMatched(EC2SecurityGroup sg, EC2Filter filter) throws ParseException {
        String filterName = filter.getName();
        String[] valueSet = filter.getValueSet();

        if (filterName.equalsIgnoreCase("description"))
            return containsString(sg.getDescription(), valueSet);
        else if (filterName.equalsIgnoreCase("group-id"))
            return containsString(sg.getId(), valueSet);
        else if (filterName.equalsIgnoreCase("group-name"))
            return containsString(sg.getName(), valueSet);
        else if (filterName.equalsIgnoreCase("owner-id")) {
            String owner = new String(sg.getDomainId() + ":" + sg.getAccountName());
            return containsString(owner, valueSet);
        } else if (filterName.equalsIgnoreCase("tag-key")) {
            EC2TagKeyValue[] tagSet = sg.getResourceTags();
            for (EC2TagKeyValue tag : tagSet)
                if (containsString(tag.getKey(), valueSet))
                    return true;
            return false;
        } else if (filterName.equalsIgnoreCase("tag-value")) {
            EC2TagKeyValue[] tagSet = sg.getResourceTags();
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
        } else if (filterName.startsWith("tag:")) {
            String key = filterName.split(":")[1];
            EC2TagKeyValue[] tagSet = sg.getResourceTags();
            for (EC2TagKeyValue tag : tagSet) {
                if (tag.getKey().equalsIgnoreCase(key)) {
                    if (tag.getValue() == null) {
                        if (containsEmptyValue(valueSet))
                            return true;
                    } else {
                        if (containsString(tag.getValue(), valueSet))
                            return true;
                    }
                }
            }
            return false;
        } else
            return false;
    }

    private boolean ipPermissionFilterMatched(EC2SecurityGroup sg, List<EC2Filter> ipPermissionFilterSet) throws ParseException {
        EC2IpPermission[] permissionSet = sg.getIpPermissionSet();

        for (EC2IpPermission perm : permissionSet) {
            boolean matched = false;
            for (EC2Filter filter : ipPermissionFilterSet) {
                String filterName = filter.getName();
                String[] valueSet = filter.getValueSet();
                if (filterName.equalsIgnoreCase("ip-permission.cidr"))
                    matched = containsString(perm.getCIDR(), valueSet);
                else if (filterName.equalsIgnoreCase("ip-permission.from-port")) {
                    if (perm.getProtocol().equalsIgnoreCase("icmp"))
                        matched = containsString(perm.getIcmpType(), valueSet);
                    else
                        matched = containsString(perm.getFromPort().toString(), valueSet);
                } else if (filterName.equalsIgnoreCase("ip-permission.to-port")) {
                    if (perm.getProtocol().equalsIgnoreCase("icmp"))
                        matched = containsString(perm.getIcmpCode(), valueSet);
                    else
                        matched = containsString(perm.getToPort().toString(), valueSet);
                } else if (filterName.equalsIgnoreCase("ip-permission.protocol"))
                    matched = containsString(perm.getProtocol(), valueSet);
                else if (filterName.equalsIgnoreCase("ip-permission.group-name")) {
                    EC2SecurityGroup[] userSet = perm.getUserSet();
                    for (EC2SecurityGroup user : userSet) {
                        if (containsString(user.getName(), valueSet)) {
                            matched = true;
                            break;
                        }
                    }
                } else if (filterName.equalsIgnoreCase("ip-permission.user-id")) {
                    EC2SecurityGroup[] userSet = perm.getUserSet();
                    for (EC2SecurityGroup user : userSet) {
                        if (containsString(user.getAccountName(), valueSet)) {
                            matched = true;
                            break;
                        }
                    }
                }
                if (!matched)
                    break;
            }
            if (matched)
                return true;
        }
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

}
