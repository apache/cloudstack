/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.sharedfs;


import java.util.Date;
import java.util.UUID;

import com.cloud.utils.db.GenericDao;

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
@Table(name = "shared_filesystem")
public class SharedFSVO implements SharedFS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "description")
    private String description;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "fs_provider_name")
    private String fsProviderName;

    @Column(name = "protocol")
    @Enumerated(value = EnumType.STRING)
    private Protocol protocol;

    @Column(name = "volume_id")
    private Long volumeId;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "fs_type")
    @Enumerated(value = EnumType.STRING)
    private FileSystemType fsType;

    @Column(name = "service_offering_id")
    private Long serviceOfferingId;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updatedCount; // This field should be updated everytime the
    // state is updated. There's no set method in
    // the vo object because it is done with in the
    // dao code.

    @Column(name = GenericDao.CREATED_COLUMN)
    protected Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    protected Date removed;

    public SharedFSVO() {
    }

    public SharedFSVO(String name, String description, long domainId, long accountId, long dataCenterId,
                      String fsProviderName, Protocol protocol, FileSystemType fsType, Long serviceOfferingId) {
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.accountId = accountId;
        this.dataCenterId = dataCenterId;
        this.fsProviderName = fsProviderName;
        this.protocol = protocol;
        this.state = State.Allocated;
        this.fsType = fsType;
        this.serviceOfferingId = serviceOfferingId;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public Class<?> getEntityType() {
        return SharedFS.class;
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
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
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
    public Long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public String getFsProviderName() {
        return fsProviderName;
    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public Long getVolumeId() {
        return volumeId;
    }

    @Override
    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    @Override
    public Long getVmId() {
        return vmId;
    }

    @Override
    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    @Override
    public FileSystemType getFsType() {
        return fsType;
    }

    @Override
    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    @Override
    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    @Override
    public long getUpdatedCount() {
        return updatedCount;
    }

    @Override
    public void incrUpdatedCount() {
        updatedCount++;
    }

}
