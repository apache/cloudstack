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
package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.host.Status;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceState;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * Host DB view.
 * @author minc
 *
 */
@Entity
@Table(name="host_view")
public class HostJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="name")
    private String name;


    @Column(name="status")
    private Status status = null;

    @Column(name="type")
    @Enumerated(value=EnumType.STRING)
    private Type type;

    @Column(name="private_ip_address")
    private String privateIpAddress;

    @Column(name="disconnected")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date disconnectedOn;

    @Column(name="version")
    private String version;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name="hypervisor_version")
    private String hypervisorVersion;

    @Column(name="capabilities")
    private String caps;

    @Column(name="last_ping")
    private long lastPinged;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="resource_state")
    @Enumerated(value=EnumType.STRING)
    private ResourceState resourceState;

    @Column(name="mgmt_server_id")
    private Long managementServerId;

    @Column(name="cpus")
    private Integer cpus;

    @Column(name="speed")
    private Long speed;

    @Column(name="ram")
    private long totalMemory;

    @Column(name="cluster_id")
    private long clusterId;

    @Column(name="cluster_uuid")
    private String clusterUuid;

    @Column(name="cluster_name")
    private String clusterName;

    @Column(name="cluster_type")
    @Enumerated(value=EnumType.STRING)
    Cluster.ClusterType clusterType;

    @Column(name="data_center_id")
    private long zoneId;

    @Column(name="data_center_uuid")
    private String zoneUuid;

    @Column(name="data_center_name")
    private String zoneName;

    @Column(name="pod_id")
    private long podId;

    @Column(name="pod_uuid")
    private String podUuid;

    @Column(name="pod_name")
    private String podName;


    @Column(name="guest_os_category_id")
    private long osCategoryId;

    @Column(name="guest_os_category_uuid")
    private String osCategoryUuid;

    @Column(name="guest_os_category_name")
    private String osCategoryName;

    @Column(name="tag")
    private String tag;

    @Column(name="memory_used_capacity")
    private long memUsedCapacity;

    @Column(name="memory_reserved_capacity")
    private long memReservedCapacity;

    @Column(name="cpu_used_capacity")
    private long cpuUsedCapacity;

    @Column(name="cpu_reserved_capacity")
    private long cpuReservedCapacity;

    @Column(name="job_id")
    private long jobId;

    @Column(name="job_uuid")
    private String jobUuid;

    @Column(name="job_status")
    private int jobStatus;


    /* (non-Javadoc)
     * @see com.cloud.api.query.vo.BaseViewVO#getId()
     */
    @Override
    public long getId() {
        return this.id;
    }

    /* (non-Javadoc)
     * @see com.cloud.api.query.vo.BaseViewVO#setId(long)
     */
    @Override
    public void setId(long id) {
        this.id = id;
    }


    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public Date getDisconnectedOn() {
        return disconnectedOn;
    }

    public void setDisconnectedOn(Date disconnectedOn) {
        this.disconnectedOn = disconnectedOn;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    public String getCapabilities() {
        return caps;
    }

    public void setCapabilities(String caps) {
        this.caps = caps;
    }

    public long getLastPinged() {
        return lastPinged;
    }

    public void setLastPinged(long lastPinged) {
        this.lastPinged = lastPinged;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public ResourceState getResourceState() {
        return resourceState;
    }

    public void setResourceState(ResourceState resourceState) {
        this.resourceState = resourceState;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }

    public Integer getCpus() {
        return cpus;
    }

    public void setCpus(Integer cpus) {
        this.cpus = cpus;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Cluster.ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(Cluster.ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public long getOsCategoryId() {
        return osCategoryId;
    }

    public void setOsCategoryId(long osCategoryId) {
        this.osCategoryId = osCategoryId;
    }

    public String getOsCategoryUuid() {
        return osCategoryUuid;
    }

    public void setOsCategoryUuid(String osCategoryUuid) {
        this.osCategoryUuid = osCategoryUuid;
    }

    public String getOsCategoryName() {
        return osCategoryName;
    }

    public void setOsCategoryName(String osCategoryName) {
        this.osCategoryName = osCategoryName;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(int jobStatus) {
        this.jobStatus = jobStatus;
    }

    public long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public void setPodUuid(String podUuid) {
        this.podUuid = podUuid;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public long getMemUsedCapacity() {
        return memUsedCapacity;
    }

    public void setMemUsedCapacity(long memUsedCapacity) {
        this.memUsedCapacity = memUsedCapacity;
    }

    public long getMemReservedCapacity() {
        return memReservedCapacity;
    }

    public void setMemReservedCapacity(long memReservedCapacity) {
        this.memReservedCapacity = memReservedCapacity;
    }

    public long getCpuUsedCapacity() {
        return cpuUsedCapacity;
    }

    public void setCpuUsedCapacity(long cpuUsedCapacity) {
        this.cpuUsedCapacity = cpuUsedCapacity;
    }

    public long getCpuReservedCapacity() {
        return cpuReservedCapacity;
    }

    public void setCpuReservedCapacity(long cpuReservedCapacity) {
        this.cpuReservedCapacity = cpuReservedCapacity;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }


}
