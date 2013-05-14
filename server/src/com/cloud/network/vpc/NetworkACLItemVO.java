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
package com.cloud.network.vpc;

import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.NetUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name="network_acl_item")
public class NetworkACLItemVO implements NetworkACLItem {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;

    @Column(name="start_port", updatable=false)
    Integer sourcePortStart;

    @Column(name="end_port", updatable=false)
    Integer sourcePortEnd;

    @Column(name="protocol", updatable=false)
    String protocol = NetUtils.TCP_PROTO;

    @Enumerated(value=EnumType.STRING)
    @Column(name="state")
    State state;

    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name="acl_id")
    long aclId;

    @Column(name="icmp_code")
    Integer icmpCode;

    @Column(name="icmp_type")
    Integer icmpType;

    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;

    @Column(name="cidr")
    String sourceCidrs;

    @Column(name="uuid")
    String uuid;

    @Column(name="number")
    int number;

    @Column(name="action")
    @Enumerated(value=EnumType.STRING)
    Action action;

    public NetworkACLItemVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public NetworkACLItemVO(Integer portStart, Integer portEnd, String protocol,
                            long aclId, List<String> sourceCidrs, Integer icmpCode,
                            Integer icmpType, TrafficType trafficType, Action action, int number) {
        this.sourcePortStart = portStart;
        this.sourcePortEnd = portEnd;
        this.protocol = protocol;
        this.aclId = aclId;
        this.state = State.Staged;
        this.icmpCode = icmpCode;
        this.icmpType = icmpType;
        setSourceCidrList(sourceCidrs);
        this.uuid = UUID.randomUUID().toString();
        this.trafficType = trafficType;
        this.action = action;
        this.number = number;
    }

    public void setSourceCidrList(List<String> sourceCidrs) {
        if(sourceCidrs == null){
            this.sourceCidrs = null;
        } else {
            StringBuilder sb = new StringBuilder();
            for(String cidr : sourceCidrs){
                if(sb.length() != 0){
                    sb.append(",");
                }
                sb.append(cidr);
            }
            this.sourceCidrs=sb.toString();
        }
    }

    @Override
    public List<String> getSourceCidrList() {
        if(sourceCidrs == null || sourceCidrs.isEmpty()){
            return null;
        } else {
            List<String> cidrList = new ArrayList<String>();
            String[] cidrs = sourceCidrs.split(",");
            for(String cidr : cidrs){
                cidrList.add(cidr);
            }
            return cidrList;
        }
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Integer getSourcePortStart() {
        return sourcePortStart;
    }

    @Override
    public Integer getSourcePortEnd() {
        return sourcePortEnd;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public long getAclId() {
        return aclId;
    }

    public Date getCreated() {
        return created;
    }



    @Override
    public String toString() {
        return new StringBuilder("Rule[").append(id).append("-").append("NetworkACL").append("-").append(state).append("]").toString();
    }

    @Override
    public Integer getIcmpCode() {
        return icmpCode;
    }

    @Override
    public Integer getIcmpType() {
        return icmpType;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public TrafficType getTrafficType() {
        return trafficType;
    }

    public void setSourcePortStart(Integer sourcePortStart) {
        this.sourcePortStart = sourcePortStart;
    }

    public void setSourcePortEnd(Integer sourcePortEnd) {
        this.sourcePortEnd = sourcePortEnd;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setIcmpCode(Integer icmpCode) {
        this.icmpCode = icmpCode;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    public void setSourceCidrs(String sourceCidrs) {
        this.sourceCidrs = sourceCidrs;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
