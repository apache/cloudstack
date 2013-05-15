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

package com.cloud.agent.api.to;

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItem.TrafficType;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.net.NetUtils;


public class NetworkACLTO implements InternalIdentity {
    long id;
    String vlanTag;
    String protocol;
    int[] portRange;
    boolean revoked;
    boolean alreadyAdded;
    private List<String> cidrList;
    private Integer icmpType;
    private Integer icmpCode;
    private TrafficType trafficType;
    String action;
    int number;

    protected NetworkACLTO() {
    }


    public NetworkACLTO(long id,String vlanTag, String protocol, Integer portStart, Integer portEnd, boolean revoked,
            boolean alreadyAdded, List<String> cidrList, Integer icmpType,Integer icmpCode,TrafficType trafficType, boolean allow, int number) {
        this.vlanTag = vlanTag;
        this.protocol = protocol;

        if (portStart != null) {
            List<Integer> range = new ArrayList<Integer>();
            range.add(portStart);
            if (portEnd != null) {
                range.add(portEnd);
            }

            portRange = new int[range.size()];
            int i = 0;
            for (Integer port : range) {
                portRange[i] = port.intValue();
                i ++;
            }
        }

        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.cidrList = cidrList;
        this.icmpType = icmpType;
        this.icmpCode = icmpCode;
        this.trafficType = trafficType;

        if(!allow){
            this.action = "DROP";
        } else {
            this.action = "ACCEPT";
        }

        this.number = number;
    }

    public NetworkACLTO(NetworkACLItem rule, String vlanTag, NetworkACLItem.TrafficType trafficType ) {
        this(rule.getId(), vlanTag, rule.getProtocol(), rule.getSourcePortStart(), rule.getSourcePortEnd(),
                rule.getState() == NetworkACLItem.State.Revoke, rule.getState() == NetworkACLItem.State.Active,
                rule.getSourceCidrList() ,rule.getIcmpType(), rule.getIcmpCode(),trafficType, rule.getAction() == NetworkACLItem.Action.Allow, rule.getNumber());
    }

    public long getId() {
        return id;
    }

    public String getSrcVlanTag() {
        return vlanTag;
    }

    public String getProtocol() {
        return protocol;
    }

    public int[] getSrcPortRange() {
        return portRange;
    }

    public Integer getIcmpType(){
        return icmpType;
    }

    public Integer getIcmpCode(){
        return icmpCode;
    }

    public String getStringPortRange() {
        if (portRange == null || portRange.length < 2)
            return "0:0";
        else
            return NetUtils.portRangeToString(portRange);
    }

    public boolean revoked() {
        return revoked;
    }

    public List<String> getSourceCidrList() {
        return cidrList;
    }

    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public String getAction() {
        return action;
    }

    public int getNumber(){
        return number;
    }
}
