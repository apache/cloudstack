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
package org.apache.cloudstack.storage.sharedfs.query.vo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.SharedFS.State;

import com.cloud.api.query.vo.BaseViewVO;
import com.cloud.storage.Storage;
import com.cloud.vm.VirtualMachine;

@Entity
@Table(name = "shared_filesystem_view")
public class SharedFSJoinVO extends BaseViewVO implements InternalIdentity, Identity {

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

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "provider")
    private String provider;

    @Column(name = "fs_type")
    @Enumerated(EnumType.STRING)
    SharedFS.FileSystemType fsType;

    @Column(name = "size")
    private Long size;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "zone_uuid")
    private String zoneUuid;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "instance_id")
    private long instanceId;

    @Column(name = "instance_uuid")
    private String instanceUuid;

    @Column(name = "instance_name")
    private String instanceName;

    @Column(name = "instance_state")
    @Enumerated(value = EnumType.STRING)
    private VirtualMachine.State instanceState;

    @Column(name = "volume_id")
    private long volumeId;

    @Column(name = "volume_uuid")
    private String volumeUuid;

    @Column(name = "volume_name")
    private String volumeName;

    @Column(name = "provisioning_type")
    @Enumerated(EnumType.STRING)
    Storage.ProvisioningType provisioningType;

    @Column(name = "volume_format")
    @Enumerated(EnumType.STRING)
    private Storage.ImageFormat volumeFormat;

    @Column(name = "volume_path")
    private String volumePath;

    @Column(name = "volume_chain_info")
    private String volumeChainInfo;

    @Column(name = "pool_uuid")
    private String poolUuid;

    @Column(name = "pool_name")
    private String poolName;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = "service_offering_uuid")
    private String serviceOfferingUuid;

    @Column(name = "service_offering_name")
    private String serviceOfferingName;

    @Column(name = "disk_offering_uuid")
    private String diskOfferingUuid;

    @Column(name = "disk_offering_name")
    private String diskOfferingName;

    @Column(name = "disk_offering_display_text")
    private String diskOfferingDisplayText;

    @Column(name = "disk_offering_size")
    private long diskOfferingSize;

    @Column(name = "disk_offering_custom")
    private boolean diskOfferingCustom;

    public SharedFSJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return state;
    }

    public String getProvider() {
        return provider;
    }

    public SharedFS.FileSystemType getFsType() {
        return fsType;
    }

    public Long getSize() {
        return size;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public VirtualMachine.State getInstanceState() {
        return instanceState;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Storage.ProvisioningType getProvisioningType() {
        return provisioningType;
    }

    public Storage.ImageFormat getVolumeFormat() {
        return volumeFormat;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public String getVolumeChainInfo() {
        return volumeChainInfo;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public String getPoolName() {
        return poolName;
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

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public String getServiceOfferingUuid() {
        return serviceOfferingUuid;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getDiskOfferingDisplayText() {
        return diskOfferingDisplayText;
    }

    public long getDiskOfferingSize() {
        return diskOfferingSize;
    }

    public boolean isDiskOfferingCustom() {
        return diskOfferingCustom;
    }
}
