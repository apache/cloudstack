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
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
    Long ACLId;

    @Column(name="icmp_code")
    Integer icmpCode;

    @Column(name="icmp_type")
    Integer icmpType;

    @Column(name="type")
    @Enumerated(value=EnumType.STRING)
    NetworkACLType type;

    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;


    // This is a delayed load value.  If the value is null,
    // then this field has not been loaded yet.
    // Call firewallrules dao to load it.
    @Transient
    List<String> sourceCidrs;

    @Column(name="uuid")
    String uuid;

    public void setSourceCidrList(List<String> sourceCidrs) {
        this.sourceCidrs=sourceCidrs;
    }

    @Override
    public List<String> getSourceCidrList() {
        return sourceCidrs;
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
    public long getACLId() {
        return ACLId;
    }

    @Override
    public NetworkACLType getType() {
        return type;
    }
    public Date getCreated() {
        return created;
    }

    protected NetworkACLItemVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public NetworkACLItemVO(Integer portStart, Integer portEnd, String protocol,
                            long aclId, List<String> sourceCidrs, Integer icmpCode,
                            Integer icmpType, TrafficType trafficType) {
        this.sourcePortStart = portStart;
        this.sourcePortEnd = portEnd;
        this.protocol = protocol;
        this.ACLId = aclId;
        this.state = State.Staged;
        this.icmpCode = icmpCode;
        this.icmpType = icmpType;
        this.sourceCidrs = sourceCidrs;
        this.uuid = UUID.randomUUID().toString();
        this.type = NetworkACLType.User;
        this.trafficType = trafficType;
    }


    public NetworkACLItemVO(int port, String protocol, long aclId, List<String> sourceCidrs, Integer icmpCode, Integer icmpType) {
        this(port, port, protocol, aclId, sourceCidrs, icmpCode, icmpType, null);
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNumber() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setType(NetworkACLType type) {
        this.type = type;
    }

    @Override
    public TrafficType getTrafficType() {
        return trafficType;
    }
}
