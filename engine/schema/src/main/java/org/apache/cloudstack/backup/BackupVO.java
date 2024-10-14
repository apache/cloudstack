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

import com.cloud.utils.db.GenericDao;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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

@Entity
@Table(name = "backups")
public class BackupVO implements Backup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "type")
    private String backupType;

    @Column(name = "date")
    @Temporal(value = TemporalType.DATE)
    private Date date;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "size")
    private Long size;

    @Column(name = "protected_size")
    private Long protectedSize;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private Backup.Status status;

    @Column(name = "backup_offering_id")
    private long backupOfferingId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "backed_volumes", length = 65535)
    protected String backedUpVolumes;

    public BackupVO() {
        this.uuid = UUID.randomUUID().toString();
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
    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getType() {
        return backupType;
    }

    public void setType(String type) {
        this.backupType = type;
    }

    @Override
    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public Long getProtectedSize() {
        return protectedSize;
    }

    public void setProtectedSize(Long protectedSize) {
        this.protectedSize = protectedSize;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public long getBackupOfferingId() {
        return backupOfferingId;
    }

    public void setBackupOfferingId(long backupOfferingId) {
        this.backupOfferingId = backupOfferingId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public Class<?> getEntityType() {
        return Backup.class;
    }

    @Override
    public String getName() {
        return null;
    }

    public List<VolumeInfo> getBackedUpVolumes() {
        if (StringUtils.isEmpty(this.backedUpVolumes)) {
            return Collections.emptyList();
        }
        return Arrays.asList(new Gson().fromJson(this.backedUpVolumes, Backup.VolumeInfo[].class));
    }

    public void setBackedUpVolumes(String backedUpVolumes) {
        this.backedUpVolumes = backedUpVolumes;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
