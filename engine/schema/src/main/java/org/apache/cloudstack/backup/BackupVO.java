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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

@Entity
@Table(name = "backup")
public class BackupVO implements Backup {

    public BackupVO() {
        this.uuid = UUID.randomUUID().toString();
        volumeIds = new ArrayList<>();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "volumes")
    private String volumes;

    @Column(name = "status")
    private Status status;

    @Column(name = "start")
    private Date start;

    private List<Long> volumeIds;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public Long getUserId() {
        return userId;
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
    public Long getParentId() {
        return parentId;
    }

    @Override
    public Long getVMId() {
        return vmId;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Date getStartTime() {
        return start;
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

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    protected void convertVolumeStringToList() {
        volumeIds = new ArrayList<>();
        if (StringUtils.isNotBlank(volumes)) {
            String[] strIds = StringUtils.substringBetween(volumes,"[", "]").split(",");
            for (String strId: strIds) {
                if (StringUtils.isNotBlank(strId)) {
                    volumeIds.add(Long.valueOf(strId));
                }
            }
        }
    }

    @Override
    public List<Long> getVolumeIds() {
        convertVolumeStringToList();
        return volumeIds;
    }

    public void setVolumeIds(List<Long> volumes) {
        if (CollectionUtils.isEmpty(volumes)) {
            volumeIds = new ArrayList<>();
        } else {
            volumeIds = new ArrayList<>(volumes);
        }
        convertVolumeIdsToString();
    }

    private void convertVolumeIdsToString() {
        StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
        if (CollectionUtils.isNotEmpty(volumeIds)) {
            for (Long volId : volumeIds) {
                stringJoiner.add(String.valueOf(volId));
            }
            volumes = stringJoiner.toString();
        } else {
            volumes = null;
        }
    }

    protected String getVolumes() {
        return volumes;
    }

    protected void setVolumes(String volumes) {
        this.volumes = volumes;
    }
}
