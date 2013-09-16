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
package com.cloud.vm;

import java.net.URI;
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

import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.Mode;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "nics")
public class NicVO implements Nic {
    protected NicVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "instance_id")
    Long instanceId;

    @Column(name = "ip4_address")
    String ip4Address;

    @Column(name = "ip6_address")
    String ip6Address;

    @Column(name = "netmask")
    String netmask;

    @Column(name = "isolation_uri")
    URI isolationUri;

    @Column(name = "ip_type")
    AddressFormat addressFormat;

    @Column(name = "broadcast_uri")
    URI broadcastUri;

    @Column(name = "gateway")
    String gateway;

    @Column(name = "mac_address")
    String macAddress;

    @Column(name = "mode")
    @Enumerated(value = EnumType.STRING)
    Mode mode;

    @Column(name = "network_id")
    long networkId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    State state;

    @Column(name = "reserver_name")
    String reserver;

    @Column(name = "reservation_id")
    String reservationId;

    @Column(name = "device_id")
    int deviceId;

    @Column(name = "update_time")
    Date updateTime;

    @Column(name = "default_nic")
    boolean defaultNic;

    @Column(name = "ip6_gateway")
    String ip6Gateway;

    @Column(name = "ip6_cidr")
    String ip6Cidr;

    @Column(name = "strategy")
    @Enumerated(value = EnumType.STRING)
    ReservationStrategy reservationStrategy;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "vm_type")
    VirtualMachine.Type vmType;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;
    
    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "secondary_ip")
    boolean secondaryIp;

    public NicVO(String reserver, Long instanceId, long configurationId, VirtualMachine.Type vmType) {
        this.reserver = reserver;
        this.instanceId = instanceId;
        this.networkId = configurationId;
        this.state = State.Allocated;
        this.vmType = vmType;
    }

    @Override
    public String getIp4Address() {
        return ip4Address;
    }

    public void setIp4Address(String address) {
        ip4Address = address;
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isDefaultNic() {
        return defaultNic;
    }

    public void setDefaultNic(boolean defaultNic) {
        this.defaultNic = defaultNic;
    }

    @Override
    public String getIp6Address() {
        return ip6Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @Override
    public AddressFormat getAddressFormat() {
        return addressFormat;
    }

    public void setAddressFormat(AddressFormat format) {
        this.addressFormat = format;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public URI getIsolationUri() {
        return isolationUri;
    }

    public void setIsolationUri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }

    @Override
    public URI getBroadcastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public int getDeviceId() {
        return deviceId;
    }

    @Override
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String id) {
        this.reservationId = id;
    }

    public void setReservationStrategy(ReservationStrategy strategy) {
        this.reservationStrategy = strategy;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String getReserver() {
        return reserver;
    }

    public void setReserver(String reserver) {
        this.reserver = reserver;
    }

    @Override
    public ReservationStrategy getReservationStrategy() {
        return reservationStrategy;
    }

    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return new StringBuilder("Nic[").append(id).append("-").append(instanceId).append("-").append(reservationId).append("-").append(ip4Address).append("]").toString();
    }

    @Override
    public VirtualMachine.Type getVmType() {
        return vmType;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }

    @Override
	public String getIp6Gateway() {
		return ip6Gateway;
	}

	public void setIp6Gateway(String ip6Gateway) {
		this.ip6Gateway = ip6Gateway;
	}

    @Override
	public String getIp6Cidr() {
		return ip6Cidr;
	}

	public void setIp6Cidr(String ip6Cidr) {
		this.ip6Cidr = ip6Cidr;
	}

    public boolean getSecondaryIp() {
        return secondaryIp;
    }

    public void setSecondaryIp(boolean secondaryIp) {
        this.secondaryIp = secondaryIp;
    }

    public void setVmType(VirtualMachine.Type vmType) {
        this.vmType = vmType;
    }
}
