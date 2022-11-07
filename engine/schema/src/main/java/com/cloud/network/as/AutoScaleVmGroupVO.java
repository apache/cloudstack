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
package com.cloud.network.as;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "autoscale_vmgroups")
@Inheritance(strategy = InheritanceType.JOINED)
public class AutoScaleVmGroupVO implements AutoScaleVmGroup, InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "zone_id", updatable = false)
    private long zoneId;

    @Column(name = "domain_id", updatable = false)
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "load_balancer_id")
    private Long loadBalancerId;

    @Column(name = "name")
    String name;

    @Column(name = "min_members", updatable = true)
    private int minMembers;

    @Column(name = "max_members", updatable = true)
    private int maxMembers;

    @Column(name = "member_port")
    private int memberPort;

    @Column(name = "interval")
    private int interval;

    @Column(name = "last_interval", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastInterval;

    @Column(name = "profile_id")
    private long profileId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    protected Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    protected Date created;

    @Column(name = "state")
    private State state;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    @Column(name = "next_vm_seq")
    private long nextVmSeq = 1L;

    public AutoScaleVmGroupVO() {
    }

    public AutoScaleVmGroupVO(long lbRuleId, long zoneId, long domainId,
            long accountId, String name, int minMembers, int maxMembers, int memberPort,
            int interval, Date lastInterval, long profileId, State state) {

        uuid = UUID.randomUUID().toString();
        loadBalancerId = lbRuleId;
        if (StringUtils.isBlank(name)) {
            this.name = uuid;
        } else {
            this.name = name;
        }
        this.minMembers = minMembers;
        this.maxMembers = maxMembers;
        this.memberPort = memberPort;
        this.profileId = profileId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.state = state;
        this.interval = interval;
        this.lastInterval = lastInterval;
    }

    @Override
    public String toString() {
        return new StringBuilder("AutoScaleVmGroupVO[").append("id=").append(id)
                .append("|name=").append(name)
                .append("|loadBalancerId=").append(loadBalancerId)
                .append("|profileId=").append(profileId)
                .append("]").toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public long getZoneId() {
        return zoneId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public Long getLoadBalancerId() {
        return loadBalancerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinMembers() {
        return minMembers;
    }

    @Override
    public int getMaxMembers() {
        return maxMembers;
    }

    @Override
    public int getMemberPort() {
        return memberPort;
    }

    @Override
    public int getInterval() {
        return interval;
    }

    @Override
    public Date getLastInterval() {
        return lastInterval;
    }

    @Override
    public long getProfileId() {
        return profileId;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public void setLastInterval(Date lastInterval) {
        this.lastInterval = lastInterval;
    }

    public void setLoadBalancerId(Long loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    @Override
    public String getUuid() {
        return uuid;
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

    public long getNextVmSeq() {
        return nextVmSeq;
    }

    public void setNextVmSeq(long nextVmSeq) {
        this.nextVmSeq = nextVmSeq;
    }

    @Override
    public Class<?> getEntityType() {
        return AutoScaleVmGroup.class;
    }
}
