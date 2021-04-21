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

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.host.Host.Type;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceState;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;

/**
 * Host DB view.
 *
 */
@Entity
@Table(name = "host_view")
public class HostJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    private Status status = null;

    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @Column(name = "private_ip_address")
    private String privateIpAddress;

    @Column(name = "disconnected")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date disconnectedOn;

    @Column(name = "version")
    private String version;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name = "hypervisor_version")
    private String hypervisorVersion;

    @Column(name = "capabilities")
    private String caps;

    @Column(name = "last_ping")
    private long lastPinged;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "oobm_enabled")
    private boolean outOfBandManagementEnabled = false;

    @Column(name = "oobm_power_state")
    @Enumerated(value = EnumType.STRING)
    private OutOfBandManagement.PowerState outOfBandManagementPowerState;

    @Column(name = "ha_enabled")
    private boolean hostHAEnabled = false;

    @Column(name = "ha_state")
    private HAConfig.HAState hostHAState;

    @Column(name = "ha_provider")
    private String hostHAProvider;

    @Column(name = "resource_state")
    @Enumerated(value = EnumType.STRING)
    private ResourceState resourceState;

    @Column(name = "mgmt_server_id")
    private Long managementServerId;

    @Column(name = "cpu_sockets")
    private Integer cpuSockets;

    @Column(name = "cpus")
    private Integer cpus;

    @Column(name = "speed")
    private Long speed;

    @Column(name = "ram")
    private long totalMemory;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "cluster_uuid")
    private String clusterUuid;

    @Column(name = "cluster_name")
    private String clusterName;

    @Column(name = "cluster_type")
    @Enumerated(value = EnumType.STRING)
    Cluster.ClusterType clusterType;

    @Column(name = "data_center_id")
    private long zoneId;

    @Column(name = "data_center_uuid")
    private String zoneUuid;

    @Column(name = "data_center_name")
    private String zoneName;

    @Column(name = "pod_id")
    private long podId;

    @Column(name = "pod_uuid")
    private String podUuid;

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "guest_os_category_id")
    private long osCategoryId;

    @Column(name = "guest_os_category_uuid")
    private String osCategoryUuid;

    @Column(name = "guest_os_category_name")
    private String osCategoryName;

    @Column(name = "tag")
    private String tag;

    @Column(name = "memory_used_capacity")
    private long memUsedCapacity;

    @Column(name = "memory_reserved_capacity")
    private long memReservedCapacity;

    @Column(name = "cpu_used_capacity")
    private long cpuUsedCapacity;

    @Column(name = "cpu_reserved_capacity")
    private long cpuReservedCapacity;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_uuid")
    private String jobUuid;

    @Column(name = "job_status")
    private int jobStatus;

    @Column(name = "annotation")
    private String annotation;

    @Column(name = "last_annotated")
    private Date lastAnnotated;

    @Column(name = "username")
    private String username;

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getVersion() {
        return version;
    }

    public long getZoneId() {
        return zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public Type getType() {
        return type;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public Date getDisconnectedOn() {
        return disconnectedOn;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public String getCapabilities() {
        return caps;
    }

    public long getLastPinged() {
        return lastPinged;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public boolean isOutOfBandManagementEnabled() {
        return outOfBandManagementEnabled;
    }

    public OutOfBandManagement.PowerState getOutOfBandManagementPowerState() {
        return outOfBandManagementPowerState;
    }

    public boolean isHostHAEnabled() {
        return hostHAEnabled;
    }

    public HAConfig.HAState getHostHAState() {
        return hostHAState;
    }

    public String getHostHAProvider() {
        return hostHAProvider;
    }

    public ResourceState getResourceState() {
        return resourceState;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public Integer getCpus() {
        return cpus;
    }

    public Long getSpeed() {
        return speed;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public long getClusterId() {
        return clusterId;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public String getClusterName() {
        return clusterName;
    }

    public Cluster.ClusterType getClusterType() {
        return clusterType;
    }

    public long getOsCategoryId() {
        return osCategoryId;
    }

    public String getOsCategoryUuid() {
        return osCategoryUuid;
    }

    public String getOsCategoryName() {
        return osCategoryName;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public long getPodId() {
        return podId;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public String getPodName() {
        return podName;
    }

    public long getMemUsedCapacity() {
        return memUsedCapacity;
    }

    public long getMemReservedCapacity() {
        return memReservedCapacity;
    }

    public long getCpuUsedCapacity() {
        return cpuUsedCapacity;
    }

    public long getCpuReservedCapacity() {
        return cpuReservedCapacity;
    }

    public String getTag() {
        return tag;
    }

    public String getAnnotation() {
        return annotation;
    }

    public Date getLastAnnotated() {
        return lastAnnotated;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAnnotated() {
        return StringUtils.isNotBlank(annotation);
    }

    public boolean isInMaintenanceStates() {
        return Arrays.asList(
                    ResourceState.Maintenance, ResourceState.ErrorInMaintenance, ResourceState.PrepareForMaintenance)
                .contains(getResourceState());
    }
}
