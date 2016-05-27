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
package com.cloud.host;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceState;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "host")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 32)
public class HostVO implements Host {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "disconnected")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date disconnectedOn;

    @Column(name = "name", nullable = false)
    private String name = null;

    /**
     * Note: There is no setter for status because it has to be set in the dao code.
     */
    @Column(name = "status", nullable = false)
    private Status status = null;

    @Column(name = "type", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @Column(name = "private_ip_address", nullable = false)
    private String privateIpAddress;

    @Column(name = "private_mac_address", nullable = false)
    private String privateMacAddress;

    @Column(name = "private_netmask", nullable = false)
    private String privateNetmask;

    @Column(name = "public_netmask")
    private String publicNetmask;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name = "public_mac_address")
    private String publicMacAddress;

    @Column(name = "storage_ip_address")
    private String storageIpAddress;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "storage_netmask")
    private String storageNetmask;

    @Column(name = "storage_mac_address")
    private String storageMacAddress;

    @Column(name = "storage_ip_address_2")
    private String storageIpAddressDeux;

    @Column(name = "storage_netmask_2")
    private String storageNetmaskDeux;

    @Column(name = "storage_mac_address_2")
    private String storageMacAddressDeux;

    @Column(name = "hypervisor_type", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name = "proxy_port")
    private Integer proxyPort;

    @Column(name = "resource")
    private String resource;

    @Column(name = "fs_type")
    private StoragePoolType fsType;

    @Column(name = "available")
    private boolean available = true;

    @Column(name = "setup")
    private boolean setup = false;

    @Column(name = "resource_state", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ResourceState resourceState;

    @Column(name = "hypervisor_version")
    private String hypervisorVersion;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updated;    // This field should be updated everytime the state is updated.  There's no set method in the vo object because it is done with in the dao code.

    @Column(name = "uuid")
    private String uuid;

    // This is a delayed load value.  If the value is null,
    // then this field has not been loaded yet.
    // Call host dao to load it.
    @Transient
    Map<String, String> details;

    // This is a delayed load value.  If the value is null,
    // then this field has not been loaded yet.
    // Call host dao to load it.
    @Transient
    List<String> hostTags;

    // This value is only for saving and current cannot be loaded.
    @Transient
    HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = new HashMap<String, HashMap<String, VgpuTypesInfo>>();

    @Override
    public String getStorageIpAddressDeux() {
        return storageIpAddressDeux;
    }

    public void setStorageIpAddressDeux(String deuxStorageIpAddress) {
        this.storageIpAddressDeux = deuxStorageIpAddress;
    }

    @Override
    public String getStorageNetmaskDeux() {
        return storageNetmaskDeux;
    }

    @Override
    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public void setStorageNetmaskDeux(String deuxStorageNetmask) {
        this.storageNetmaskDeux = deuxStorageNetmask;
    }

    @Override
    public String getStorageMacAddressDeux() {
        return storageMacAddressDeux;
    }

    public void setStorageMacAddressDeux(String duexStorageMacAddress) {
        this.storageMacAddressDeux = duexStorageMacAddress;
    }

    @Override
    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getPrivateNetmask() {
        return privateNetmask;
    }

    public void setPrivateNetmask(String privateNetmask) {
        this.privateNetmask = privateNetmask;
    }

    @Override
    public String getPublicNetmask() {
        return publicNetmask;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    @Override
    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    @Override
    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    @Override
    public String getStorageIpAddress() {
        return storageIpAddress;
    }

    public void setStorageIpAddress(String storageIpAddress) {
        this.storageIpAddress = storageIpAddress;
    }

    @Override
    public String getStorageNetmask() {
        return storageNetmask;
    }

    public void setStorageNetmask(String storageNetmask) {
        this.storageNetmask = storageNetmask;
    }

    @Override
    public String getStorageMacAddress() {
        return storageMacAddress;
    }

    public boolean isSetup() {
        return setup;
    }

    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    public void setStorageMacAddress(String storageMacAddress) {
        this.storageMacAddress = storageMacAddress;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public String getDetail(String name) {
        return details != null ? details.get(name) : null;
    }

    public void setDetail(String name, String value) {
        assert (details != null) : "Did you forget to load the details?";

        details.put(name, value);
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public List<String> getHostTags() {
        return hostTags;
    }

    public void setHostTags(List<String> hostTags) {
        this.hostTags = hostTags;
    }

    public  HashMap<String, HashMap<String, VgpuTypesInfo>> getGpuGroupDetails() {
        return groupDetails;
    }

    public void setGpuGroups(HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails) {
        this.groupDetails = groupDetails;
    }

    @Column(name = "data_center_id", nullable = false)
    private long dataCenterId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "cpu_sockets")
    private Integer cpuSockets;

    @Column(name = "cpus")
    private Integer cpus;

    @Column(name = "url")
    private String storageUrl;

    @Column(name = "speed")
    private Long speed;

    @Column(name = "ram")
    private long totalMemory;

    @Column(name = "parent", nullable = false)
    private String parent;

    @Column(name = "guid", updatable = true, nullable = false)
    private String guid;

    @Column(name = "capabilities")
    private String caps;

    @Column(name = "total_size")
    private Long totalSize;

    @Column(name = "last_ping")
    private long lastPinged;

    @Column(name = "mgmt_server_id")
    private Long managementServerId;

    @Column(name = "dom0_memory")
    private long dom0MinMemory;

    @Column(name = "version")
    private String version;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public HostVO(String guid) {
        this.guid = guid;
        this.status = Status.Creating;
        this.totalMemory = 0;
        this.dom0MinMemory = 0;
        this.resourceState = ResourceState.Creating;
        this.uuid = UUID.randomUUID().toString();
    }

    protected HostVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public HostVO(long id, String name, Type type, String privateIpAddress, String privateNetmask, String privateMacAddress, String publicIpAddress,
            String publicNetmask, String publicMacAddress, String storageIpAddress, String storageNetmask, String storageMacAddress, String deuxStorageIpAddress,
            String duxStorageNetmask, String deuxStorageMacAddress, String guid, Status status, String version, String iqn, Date disconnectedOn, long dcId, Long podId,
            long serverId, long ping, String parent, long totalSize, StoragePoolType fsType) {
        this(id,
            name,
            type,
            privateIpAddress,
            privateNetmask,
            privateMacAddress,
            publicIpAddress,
            publicNetmask,
            publicMacAddress,
            storageIpAddress,
            storageNetmask,
            storageMacAddress,
            guid,
            status,
            version,
            iqn,
            disconnectedOn,
            dcId,
            podId,
            serverId,
            ping,
            null,
            null,
            null,
            0,
            null);
        this.parent = parent;
        this.totalSize = totalSize;
        this.fsType = fsType;
        this.uuid = UUID.randomUUID().toString();
    }

    public HostVO(long id, String name, Type type, String privateIpAddress, String privateNetmask, String privateMacAddress, String publicIpAddress,
            String publicNetmask, String publicMacAddress, String storageIpAddress, String storageNetmask, String storageMacAddress, String guid, Status status,
            String version, String url, Date disconnectedOn, long dcId, Long podId, long serverId, long ping, Integer cpus, Long speed, Long totalMemory,
            long dom0MinMemory, String caps) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.type = type;
        this.privateIpAddress = privateIpAddress;
        this.privateNetmask = privateNetmask;
        this.privateMacAddress = privateMacAddress;
        this.publicIpAddress = publicIpAddress;
        this.publicNetmask = publicNetmask;
        this.publicMacAddress = publicMacAddress;
        this.storageIpAddress = storageIpAddress;
        this.storageNetmask = storageNetmask;
        this.storageMacAddress = storageMacAddress;
        this.dataCenterId = dcId;
        this.podId = podId;
        this.cpus = cpus;
        this.version = version;
        this.speed = speed;
        this.totalMemory = totalMemory != null ? totalMemory : 0;
        this.guid = guid;
        this.parent = null;
        this.totalSize = null;
        this.fsType = null;
        this.managementServerId = serverId;
        this.lastPinged = ping;
        this.caps = caps;
        this.disconnectedOn = disconnectedOn;
        this.dom0MinMemory = dom0MinMemory;
        this.storageUrl = url;
        this.uuid = UUID.randomUUID().toString();
    }

    public void setPodId(Long podId) {

        this.podId = podId;
    }

    public void setDataCenterId(long dcId) {
        this.dataCenterId = dcId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setStorageUrl(String url) {
        this.storageUrl = url;
    }

    public void setDisconnectedOn(Date disconnectedOn) {
        this.disconnectedOn = disconnectedOn;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivateIpAddress(String ipAddress) {
        this.privateIpAddress = ipAddress;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public void setCpus(Integer cpus) {
        this.cpus = cpus;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setCaps(String caps) {
        this.caps = caps;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public void setLastPinged(long lastPinged) {
        this.lastPinged = lastPinged;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }

    @Override
    public long getLastPinged() {
        return lastPinged;
    }

    @Override
    public String getParent() {
        return parent;
    }

    @Override
    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public String getCapabilities() {
        return caps;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public Long getPodId() {
        return podId;
    }

    @Override
    public Long getManagementServerId() {
        return managementServerId;
    }

    @Override
    public Date getDisconnectedOn() {
        return disconnectedOn;
    }

    @Override
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public Integer getCpuSockets() {
        return cpuSockets;
    }

    @Override
    public Integer getCpus() {
        return cpus;
    }

    @Override
    public Long getSpeed() {
        return speed;
    }

    @Override
    public Long getTotalMemory() {
        return totalMemory;
    }

    @Override
    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer port) {
        proxyPort = port;
    }

    public StoragePoolType getFsType() {
        return fsType;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HostVO) {
            return ((HostVO)obj).getId() == this.getId();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("Host[").append("-").append(id).append("-").append(type).append("]").toString();
    }

    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    @Override
    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    @Override
    @Transient
    public Status getState() {
        return status;
    }

    @Override
    public ResourceState getResourceState() {
        return resourceState;
    }

    public void setResourceState(ResourceState state) {
        resourceState = state;
    }

    @Override
    public boolean isInMaintenanceStates() {
        return (getResourceState() == ResourceState.Maintenance || getResourceState() == ResourceState.ErrorInMaintenance || getResourceState() == ResourceState.PrepareForMaintenance);
    }

    public long getUpdated() {
        return updated;
    }

    public long incrUpdated() {
        updated++;
        return updated;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
