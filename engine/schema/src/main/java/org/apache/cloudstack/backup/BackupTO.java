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

import java.util.Date;
import java.util.List;

public class BackupTO implements Backup {

    private long id;
    private String uuid;
    private Long accountId;
    private String name;
    private String description;
    private Long parentId;
    private Long vmId;
    private List<Long> volumeIds;
    private Status status;
    private Date startTime;
    private Long zoneId;
    private String externalId;
    private String parentExternalId;

    public BackupTO() {
    }

    public BackupTO(final Long zoneId, final Long accountId, final String externalId, final String name, final String description,
                    final String parentExternalId, final Long vmId, final List<Long> volumeIds, final Status status, final Date startTime) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.parentExternalId = parentExternalId;
        this.vmId = vmId;
        this.volumeIds = volumeIds;
        this.status = status;
        this.startTime = startTime;
    }

    @Override
    public long getId() {
        return id;
    }

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

    @Override
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getParentExternalId() {
        return parentExternalId;
    }

    public void setParentExternalId(String parentExternalId) {
        this.parentExternalId = parentExternalId;
    }

    @Override

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    @Override
    public List<Long> getVolumeIds() {
        return volumeIds;
    }

    public void setVolumeIds(List<Long> volumeIds) {
        this.volumeIds = volumeIds;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public Date getRemoved() {
        return null;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
}
