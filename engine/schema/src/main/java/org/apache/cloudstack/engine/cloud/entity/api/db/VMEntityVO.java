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
package org.apache.cloudstack.engine.cloud.entity.api.db;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.cloudstack.backup.Backup;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.StateMachine;
import com.cloud.utils.fsm.FiniteStateObject;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.google.gson.Gson;

@Entity
@Table(name = "vm_instance")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 32)
public class VMEntityVO implements VirtualMachine, FiniteStateObject<State, VirtualMachine.Event> {
    @Id
    @TableGenerator(name = "vm_instance_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "vm_instance_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    protected long id;

    @Column(name = "name", updatable = false, nullable = false, length = 255)
    protected String hostName = null;

    @Encrypt
    @Column(name = "vnc_password", updatable = true, nullable = false, length = 255)
    protected String vncPassword;

    @Column(name = "proxy_id", updatable = true, nullable = true)
    protected Long proxyId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "proxy_assign_time", updatable = true, nullable = true)
    protected Date proxyAssignTime;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value = EnumType.STRING)
    @StateMachine(state = State.class, event = Event.class)
    @Column(name = "state", updatable = true, nullable = false, length = 32)
    protected State state = null;

    @Column(name = "private_ip_address", updatable = true)
    protected String privateIpAddress;

    @Column(name = "instance_name", updatable = true, nullable = false)
    protected String instanceName;

    @Column(name = "vm_template_id", updatable = true, nullable = true, length = 17)
    protected Long templateId = new Long(-1);

    @Column(name = "guest_os_id", nullable = false, length = 17)
    protected long guestOSId;

    @Column(name = "host_id", updatable = true, nullable = true)
    protected Long hostId;

    @Column(name = "last_host_id", updatable = true, nullable = true)
    protected Long lastHostId;

    @Column(name = "pod_id", updatable = true, nullable = false)
    protected Long podIdToDeployIn;

    @Column(name = "private_mac_address", updatable = true, nullable = true)
    protected String privateMacAddress;

    @Column(name = "data_center_id", updatable = true, nullable = false)
    protected long dataCenterIdToDeployIn;

    @Column(name = "vm_type", updatable = false, nullable = false, length = 32)
    @Enumerated(value = EnumType.STRING)
    protected Type type;

    @Column(name = "ha_enabled", updatable = true, nullable = true)
    protected boolean haEnabled;

    @Column(name = "limit_cpu_use", updatable = true, nullable = true)
    private boolean limitCpuUse;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updated;    // This field should be updated everytime the state is updated.  There's no set method in the vo object because it is done with in the dao code.

    @Column(name = GenericDao.CREATED_COLUMN)
    protected Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    protected Date removed;

    @Column(name = "update_time", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    protected Date updateTime;

    @Column(name = "domain_id")
    protected long domainId;

    @Column(name = "account_id")
    protected long accountId;

    @Column(name = "service_offering_id")
    protected long serviceOfferingId;

    @Column(name = "reservation_id")
    protected String reservationId;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    protected HypervisorType hypervisorType;

    @Transient
    Map<String, String> details;

    @Column(name = "uuid")
    protected String uuid = UUID.randomUUID().toString();

    //orchestration columns
    @Column(name = "owner")
    private String owner = null;

    @Transient
    List<String> computeTags;

    @Transient
    List<String> rootDiskTags;

    @Column(name = "host_name")
    private String hostname = null;

    @Column(name = "display_name")
    private String displayname = null;

    @Transient
    List<String> networkIds;

    @Column(name = "disk_offering_id")
    protected Long diskOfferingId;

    @Column(name = "display_vm", updatable = true, nullable = false)
    protected boolean display = true;

    @Column(name = "backup_offering_id")
    protected Long backupOfferingId;

    @Column(name = "backup_external_id")
    private String backupExternalId;

    @Column(name = "backup_volumes")
    private String backupVolumes;

    @Column(name = "dynamically_scalable")
    protected boolean dynamicallyScalable;

    @Transient
    private VMReservationVO vmReservation;

    public VMEntityVO(long id, long serviceOfferingId, String name, String instanceName, Type type, Long vmTemplateId, HypervisorType hypervisorType, long guestOSId,
            long domainId, long accountId, boolean haEnabled, Long diskOfferingId) {
        this.id = id;
        hostName = name != null ? name : uuid;
        if (vmTemplateId != null) {
            templateId = vmTemplateId;
        }
        this.instanceName = instanceName;
        this.type = type;
        this.guestOSId = guestOSId;
        this.haEnabled = haEnabled;
        vncPassword = Long.toHexString(new SecureRandom().nextLong());
        state = State.Stopped;
        this.accountId = accountId;
        this.domainId = domainId;
        this.serviceOfferingId = serviceOfferingId;
        this.hypervisorType = hypervisorType;
        limitCpuUse = false;
        this.diskOfferingId = diskOfferingId;
    }

    public VMEntityVO(long id, long serviceOfferingId, String name, String instanceName, Type type, Long vmTemplateId, HypervisorType hypervisorType, long guestOSId,
            long domainId, long accountId, boolean haEnabled, boolean limitResourceUse) {
        this(id, serviceOfferingId, name, instanceName, type, vmTemplateId, hypervisorType, guestOSId, domainId, accountId, haEnabled, null);
        limitCpuUse = limitResourceUse;
    }

    protected VMEntityVO() {
    }

    public Date getRemoved() {
        return removed;
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
    public Type getType() {
        return type;
    }

    @Override
    public long getUpdated() {
        return updated;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterIdToDeployIn;
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public State getState() {
        return state;
    }

    // don't use this directly, use VM state machine instead, this method is added for migration tool only
    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String address) {
        privateIpAddress = address;
    }

    public void setVncPassword(String vncPassword) {
        this.vncPassword = vncPassword;
    }

    @Override
    public String getVncPassword() {
        return vncPassword;
    }

    @Override
    public long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getProxyId() {
        return proxyId;
    }

    public void setProxyId(Long proxyId) {
        this.proxyId = proxyId;
    }

    public Date getProxyAssignTime() {
        return proxyAssignTime;
    }

    public void setProxyAssignTime(Date time) {
        proxyAssignTime = time;
    }

    @Override
    public long getTemplateId() {
        if (templateId == null) {
            return -1;
        } else {
            return templateId;
        }
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    @Override
    public long getGuestOSId() {
        return guestOSId;
    }

    public void setGuestOSId(long guestOSId) {
        this.guestOSId = guestOSId;
    }

    public void incrUpdated() {
        updated++;
    }

    public void decrUpdated() {
        updated--;
    }

    @Override
    public Long getHostId() {
        return hostId;
    }

    @Override
    public Long getLastHostId() {
        return lastHostId;
    }

    public void setLastHostId(Long lastHostId) {
        this.lastHostId = lastHostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    @Override
    public boolean isHaEnabled() {
        return haEnabled;
    }

    @Override
    public boolean limitCpuUse() {
        return limitCpuUse;
    }

    public void setLimitCpuUse(boolean value) {
        limitCpuUse = value;
    }

    @Override
    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    @Override
    public Long getPodIdToDeployIn() {
        return podIdToDeployIn;
    }

    public void setPodId(long podId) {
        podIdToDeployIn = podId;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public void setDataCenterId(long dataCenterId) {
        dataCenterIdToDeployIn = dataCenterId;
    }

    public boolean isRemoved() {
        return removed != null;
    }

    public void setHaEnabled(boolean value) {
        haEnabled = value;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationId() {
        return reservationId;
    }

    @Override
    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetail(String name, String value) {
        assert (details != null) : "Did you forget to load the details?";

        details.put(name, value);
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    transient String toString;

    @Override
    public String toString() {
        if (toString == null) {
            toString = new StringBuilder("VM[").append(type.toString()).append("|").append(hostName).append("]").toString();
        }
        return toString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)(id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VMEntityVO other = (VMEntityVO)obj;
        if (id != other.id)
            return false;
        return true;
    }

    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getComputeTags() {
        return computeTags;
    }

    public void setComputeTags(List<String> computeTags) {
        this.computeTags = computeTags;
    }

    public List<String> getRootDiskTags() {
        return rootDiskTags;
    }

    public void setRootDiskTags(List<String> rootDiskTags) {
        this.rootDiskTags = rootDiskTags;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public List<String> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(List<String> networkIds) {
        this.networkIds = networkIds;
    }

    @Override
    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public VMReservationVO getVmReservation() {
        return vmReservation;
    }

    public void setVmReservation(VMReservationVO vmReservation) {
        this.vmReservation = vmReservation;
    }

    @Override
    public Class<?> getEntityType() {
        return VirtualMachine.class;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    @Override
    public boolean isDynamicallyScalable() {
        return dynamicallyScalable;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public PartitionType partitionType() {
        return PartitionType.VM;
    }

    @Override
    public Long getBackupOfferingId() {
        return backupOfferingId;
    }

    @Override
    public String getBackupExternalId() {
        return backupExternalId;
    }

    @Override
    public List<Backup.VolumeInfo> getBackupVolumeList() {
        return Arrays.asList(new Gson().fromJson(this.backupVolumes, Backup.VolumeInfo[].class));
    }
}
