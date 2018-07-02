//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import org.apache.commons.lang.StringUtils;

@Entity
@Table(name = "vm_backup")
public class VMBackupVO implements VMBackup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "policy_id")
    private long policyId;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "volumes")
    private String volumes;

    @Column(name = "size")
    private Long size;

    @Column(name = "protected_size")
    private Long protectedSize;

    @Column(name = "status")
    private Status status;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    @Transient
    private List<Long> volumeIds;

    public VMBackupVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
        volumeIds = new ArrayList<>();
    }

    public VMBackupVO(final String name, final String description, final long policyId, final Long vmId,
                      final Status status, final long accountId, final Long zoneId) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.policyId = policyId;
        this.vmId = vmId;
        this.status = status;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.created = new Date();
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
    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Long getVmId() {
        return vmId;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public Long getSize() {
        return size;
    }

    public Long getProtectedSize() {
        return protectedSize;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setProtectedSize(Long protectedSize) {
        this.protectedSize = protectedSize;
    }

    public void setCreated(Date start) {
        this.created = start;
    }

    @Override
    public List<VolumeInfo> getBackedUpVolumes() {
        List<VolumeInfo> info = new ArrayList<>();
        if (StringUtils.isNotBlank(this.volumes)) {
            String[] volumes = StringUtils.substringBetween(this.volumes,"[", "]").split(",");
            for (String vol : volumes) {
                String[] volParts = vol.split(":");
                VMBackup.VolumeInfo volumeInfo = new VolumeInfo(volParts[0], volParts[1],
                        Volume.Type.valueOf(volParts[2]), Long.valueOf(volParts[3]));
                info.add(volumeInfo);
            }
        }
        return info;
    }

    public void setBackedUpVolumes(List<VolumeVO> volumes) {
        StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
        for (VolumeVO volume : volumes) {
            String volTag = volume.getUuid() + ":" + volume.getPath() + ":" +
                    volume.getVolumeType().toString() + ":" + volume.getSize();
            stringJoiner.add(volTag);
        }
        this.volumes = stringJoiner.toString();
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    protected String getVolumes() {
        return volumes;
    }

    protected void setVolumes(String volumes) {
        this.volumes = volumes;
    }

    public long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(long policyId) {
        this.policyId = policyId;
    }
}
