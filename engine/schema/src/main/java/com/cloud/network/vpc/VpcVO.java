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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vpc")
public class VpcVO implements Vpc {

    @Id
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "display_text")
    String displayText;

    @Column(name = "zone_id")
    long zoneId;

    @Column(name = "cidr")
    private String cidr = null;

    @Column(name = "domain_id")
    Long domainId = null;

    @Column(name = "account_id")
    Long accountId = null;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    State state;

    @Column(name = "redundant")
    boolean redundant;

    @Column(name = "vpc_offering_id")
    long vpcOfferingId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = "network_domain")
    String networkDomain;

    @Column(name = "restart_required")
    boolean restartRequired = false;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    @Column(name="uses_distributed_router")
    boolean usesDistributedRouter = false;

    @Column(name = "region_level_vpc")
    boolean regionLevelVpc = false;

    public VpcVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VpcVO(final long zoneId, final String name, final String displayText, final long accountId, final long domainId,
            final long vpcOffId, final String cidr, final String networkDomain, final boolean useDistributedRouter,
            final boolean regionLevelVpc, final boolean isRedundant) {
        this.zoneId = zoneId;
        this.name = name;
        this.displayText = displayText;
        this.accountId = accountId;
        this.domainId = domainId;
        this.cidr = cidr;
        uuid = UUID.randomUUID().toString();
        state = State.Enabled;
        this.networkDomain = networkDomain;
        vpcOfferingId = vpcOffId;
        usesDistributedRouter = useDistributedRouter;
        this.regionLevelVpc = regionLevelVpc;
        redundant = isRedundant;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    @Override
    public String getCidr() {
        return cidr;
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
    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public long getVpcOfferingId() {
        return vpcOfferingId;
    }

    public void setVpcOfferingId(final long vpcOfferingId) {
        this.vpcOfferingId = vpcOfferingId;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDisplayText(final String displayText) {
        this.displayText = displayText;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("[VPC [");
        return buf.append(id).append("-").append(name).append("]").toString();
    }

    @Override
    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setRestartRequired(final boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

    @Override
    public boolean isRestartRequired() {
        return restartRequired;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean isRegionLevelVpc() {
        return regionLevelVpc;
    }


    public void setDisplay(final boolean display) {
        this.display = display;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    @Override
    public boolean isRedundant() {
        return redundant;
    }

    public void setRedundant(final boolean isRedundant) {
        redundant = isRedundant;
    }

    @Override
    public Class<?> getEntityType() {
        return Vpc.class;
    }

    @Override
    public boolean usesDistributedRouter() {
        return usesDistributedRouter;
    }
}
