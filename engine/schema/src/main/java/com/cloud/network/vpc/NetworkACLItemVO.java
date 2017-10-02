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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.NetUtils;

@Entity
@Table(name = "network_acl_item")
public class NetworkACLItemVO implements NetworkACLItem {

    /**
     *
     */
    private static final long serialVersionUID = 2790623532888742060L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "start_port", updatable = false)
    Integer sourcePortStart;

    @Column(name = "end_port", updatable = false)
    Integer sourcePortEnd;

    @Column(name = "protocol", updatable = false)
    String protocol = NetUtils.TCP_PROTO;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "state")
    State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = "acl_id")
    long aclId;

    @Column(name = "icmp_code")
    Integer icmpCode;

    @Column(name = "icmp_type")
    Integer icmpType;

    @Column(name = "traffic_type")
    @Enumerated(value = EnumType.STRING)
    TrafficType trafficType;

    // This is a delayed load value.  If the value is null,
    // then this field has not been loaded yet.
    // Call the NetworkACLItem dao to load it.
    @Transient
    List<String> sourceCidrs;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "number")
    int number;

    @Column(name = "action")
    @Enumerated(value = EnumType.STRING)
    Action action;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    public NetworkACLItemVO() {
        uuid = UUID.randomUUID().toString();
    }

    public NetworkACLItemVO(Integer portStart, Integer portEnd, String protocol, long aclId, List<String> sourceCidrs, Integer icmpCode, Integer icmpType,
            TrafficType trafficType, Action action, int number) {
        sourcePortStart = portStart;
        sourcePortEnd = portEnd;
        this.protocol = protocol;
        this.aclId = aclId;
        state = State.Staged;
        this.icmpCode = icmpCode;
        this.icmpType = icmpType;
        setSourceCidrList(sourceCidrs);
        uuid = UUID.randomUUID().toString();
        this.trafficType = trafficType;
        this.action = action;
        this.number = number;
    }

    public void setSourceCidrList(List<String> sourceCidrs) {
        this.sourceCidrs = sourceCidrs;
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
        return uuid;
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
        List<String> srcCidrs = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(sourceCidrs,",;");
        while(st.hasMoreTokens()) {
            srcCidrs.add(st.nextToken());
        }
        this.sourceCidrs = srcCidrs;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }
}
