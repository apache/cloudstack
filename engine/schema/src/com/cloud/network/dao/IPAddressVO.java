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
package com.cloud.network.dao;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.cloud.network.IpAddress;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.Ip;

/**
 * A bean representing a public IP Address
 *
 */
@Entity
@Table(name = ("user_ip_address"))
public class IPAddressVO implements IpAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "account_id")
    private Long allocatedToAccountId = null;

    @Column(name = "domain_id")
    private Long allocatedInDomainId = null;

    @Column(name = "public_ip_address")
    @Enumerated(value = EnumType.STRING)
    private Ip address = null;

    @Column(name = "data_center_id", updatable = false)
    private long dataCenterId;

    @Column(name = "source_nat")
    private boolean sourceNat;

    @Column(name = "allocated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date allocatedTime;

    @Column(name = "vlan_db_id")
    private long vlanId;

    @Column(name = "one_to_one_nat")
    private boolean oneToOneNat;

    @Column(name = "vm_id")
    private Long associatedWithVmId;

    @Column(name = "state")
    private State state;

    @Column(name = "mac_address")
    private long macAddress;

    @Column(name = "source_network_id")
    private Long sourceNetworkId;

    @Column(name = "network_id")
    private Long associatedWithNetworkId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "physical_network_id")
    private Long physicalNetworkId;

    @Column(name = "is_system")
    private boolean system;

    @Column(name = "account_id")
    @Transient
    private Long accountId = null;

    @Transient
    @Column(name = "domain_id")
    private Long domainId = null;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "dnat_vmip")
    private String vmIp;

    @Column(name = "is_portable")
    private boolean portable = false;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    protected IPAddressVO() {
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public boolean readyToUse() {
        return state == State.Allocated;
    }

    public IPAddressVO(Ip address, long dataCenterId, long macAddress, long vlanDbId, boolean sourceNat) {
        this.address = address;
        this.dataCenterId = dataCenterId;
        vlanId = vlanDbId;
        this.sourceNat = sourceNat;
        allocatedInDomainId = null;
        allocatedToAccountId = null;
        allocatedTime = null;
        state = State.Free;
        this.macAddress = macAddress;
        uuid = UUID.randomUUID().toString();
    }

    public IPAddressVO(Ip address, long dataCenterId, Long networkId, Long vpcId, long physicalNetworkId, long sourceNetworkId, long vlanDbId, boolean portable) {
        this.address = address;
        this.dataCenterId = dataCenterId;
        associatedWithNetworkId = networkId;
        this.vpcId = vpcId;
        this.physicalNetworkId = physicalNetworkId;
        this.sourceNetworkId = sourceNetworkId;
        vlanId = vlanDbId;
        this.portable = portable;
        uuid = UUID.randomUUID().toString();
    }

    public long getMacAddress() {
        return macAddress;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dcId) {
        dataCenterId = dcId;
    }

    @Override
    public Ip getAddress() {
        return address;
    }

    @Override
    public Long getAllocatedToAccountId() {
        return allocatedToAccountId;
    }

    @Override
    public Long getAllocatedInDomainId() {
        return allocatedInDomainId;
    }

    @Override
    public Long getAssociatedWithNetworkId() {
        return associatedWithNetworkId;
    }

    public void setAssociatedWithNetworkId(Long networkId) {
        associatedWithNetworkId = networkId;
    }

    @Override
    public Long getAssociatedWithVmId() {
        return associatedWithVmId;
    }

    public void setAssociatedWithVmId(Long associatedWithVmId) {
        this.associatedWithVmId = associatedWithVmId;
    }

    @Override
    public Date getAllocatedTime() {
        return allocatedTime;
    }

    public void setAllocatedToAccountId(Long accountId) {
        allocatedToAccountId = accountId;
    }

    public void setAllocatedInDomainId(Long domainId) {
        allocatedInDomainId = domainId;
    }

    public void setSourceNat(boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    @Override
    public boolean isSourceNat() {
        return sourceNat;
    }

    public void setAllocatedTime(Date allocated) {
        allocatedTime = allocated;
    }

    @Override
    public long getVlanId() {
        return vlanId;
    }

    public void setVlanId(long vlanDbId) {
        vlanId = vlanDbId;
    }

    @Override
    public boolean isOneToOneNat() {
        return oneToOneNat;
    }

    public void setOneToOneNat(boolean oneToOneNat) {
        this.oneToOneNat = oneToOneNat;
    }

    @Override
    public long getDomainId() {
        return allocatedInDomainId == null ? -1 : allocatedInDomainId;
    }

    @Override
    public long getAccountId() {
        return allocatedToAccountId == null ? -1 : allocatedToAccountId;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return new StringBuilder("Ip[").append(address).append("-").append(dataCenterId).append("]").toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getSourceNetworkId() {
        return sourceNetworkId;
    }

    public void setSourceNetworkId(Long sourceNetworkId) {
        this.sourceNetworkId = sourceNetworkId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    @Override
    public boolean getSystem() {
        return system;
    }

    public void setSystem(boolean isSystem) {
        system = isSystem;
    }

    @Override
    public boolean isPortable() {
        return portable;
    }

    public void setPortable(boolean portable) {
        this.portable = portable;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public String getVmIp() {
        return vmIp;
    }

    public void setVmIp(String vmIp) {
        this.vmIp = vmIp;
    }

    @Override
    public Long getNetworkId() {
        return sourceNetworkId;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public Class<?> getEntityType() {
        return IpAddress.class;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }
}
