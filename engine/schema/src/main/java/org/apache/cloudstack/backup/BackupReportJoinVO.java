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

package org.apache.cloudstack.backup;

import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "backup_report_view")
public class BackupReportJoinVO {

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name="zone_uuid")
    private String zoneUuid;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "account_id")
    private long accountId;

    @Column(name="account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name="project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name="vm_uuid")
    private String vmUuid;

    @Column(name = "vm_name")
    private String vmName;

    @Column(name = "backup_id")
    private long backupId;

    @Column(name="backup_uuid")
    private String backupUuid;

    @Column(name = "backup_name")
    private String backupName;

    @Column(name = "offering_name")
    private String offeringName;

    @Column(name = "size")
    private long size;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private Backup.Status status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "logid")
    private String logid;

    @Column(name = "date")
    @Temporal(value = TemporalType.DATE)
    private Date date;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public BackupReportJoinVO() {
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

    public long getDomainId() {
        return domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getProjectName() {
        return projectName;
    }

    public long getVmId() {
        return vmId;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public long getBackupId() {
        return backupId;
    }

    public String getBackupUuid() {
        return backupUuid;
    }

    public String getBackupName() {
        return backupName;
    }

    public String getOfferingName() {
        return offeringName;
    }

    public long getSize() {
        return size;
    }

    public Backup.Status getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Date getDate() {
        return date;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getLogid() {
        return logid;
    }
}
