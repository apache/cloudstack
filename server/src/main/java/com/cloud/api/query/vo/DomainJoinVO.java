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

import java.util.Comparator;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "domain_view")
public class DomainJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="parent")
    private Long parent = null;

    @Column(name="name")
    private String name = null;

    @Column(name="owner")
    private long accountId;

    @Column(name="path")
    private String path = null;

    @Column(name="level")
    private int level;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="child_count")
    private int childCount = 0;

    @Column(name="next_child_seq")
    private long nextChildSeq = 1L;

    @Column(name="state")
    private Domain.State state;

    @Column(name="network_domain")
    private String networkDomain;

    @Column(name="uuid")
    private String uuid;


    @Column(name="vmLimit")
    private Long vmLimit;

    @Column(name="vmTotal")
    private Long vmTotal;


    @Column(name="ipLimit")
    private Long ipLimit;

    @Column(name="ipTotal")
    private Long ipTotal;

    @Column(name="volumeLimit")
    private Long volumeLimit;

    @Column(name="volumeTotal")
    private Long volumeTotal;

    @Column(name="snapshotLimit")
    private Long snapshotLimit;

    @Column(name="snapshotTotal")
    private Long snapshotTotal;

    @Column(name="templateLimit")
    private Long templateLimit;

    @Column(name="templateTotal")
    private Long templateTotal;

    @Column(name="projectLimit")
    private Long projectLimit;

    @Column(name="projectTotal")
    private Long projectTotal;


    @Column(name="networkLimit")
    private Long networkLimit;

    @Column(name="networkTotal")
    private Long networkTotal;


    @Column(name="vpcLimit")
    private Long vpcLimit;

    @Column(name="vpcTotal")
    private Long vpcTotal;


    @Column(name="cpuLimit")
    private Long cpuLimit;

    @Column(name="cpuTotal")
    private Long cpuTotal;


    @Column(name="memoryLimit")
    private Long memoryLimit;

    @Column(name="memoryTotal")
    private Long memoryTotal;


    @Column(name="primaryStorageLimit")
    private Long primaryStorageLimit;

    @Column(name="primaryStorageTotal")
    private Long primaryStorageTotal;


    @Column(name="secondaryStorageLimit")
    private Long secondaryStorageLimit;

    @Column(name="secondaryStorageTotal")
    private Long secondaryStorageTotal;

    @Transient
    private String parentName;

    @Transient
    private String parentUuid;

    public DomainJoinVO() {
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


    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        if(parent == null) {
            this.parent = DomainVO.ROOT_DOMAIN;
        } else {
            if(parent.longValue() <= DomainVO.ROOT_DOMAIN)
                this.parent = DomainVO.ROOT_DOMAIN;
            else
                this.parent = parent;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAccountId() {
        return accountId;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int count) {
        childCount = count;
    }

    public long getNextChildSeq() {
        return nextChildSeq;
    }

    public void setNextChildSeq(long seq) {
        nextChildSeq = seq;
    }

    public Domain.State getState() {
        return state;
    }

    public void setState(Domain.State state) {
        this.state = state;
    }

    public String toString() {
        return new StringBuilder("Domain:").append(id).append(path).toString();
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String domainSuffix) {
        this.networkDomain = domainSuffix;
    }


    public Long getVmTotal() {
        return vmTotal;
    }


    public void setVmTotal(Long vmTotal) {
        this.vmTotal = vmTotal;
    }


    public Long getIpTotal() {
        return ipTotal;
    }


    public void setIpTotal(Long ipTotal) {
        this.ipTotal = ipTotal;
    }

    public Long getVolumeTotal() {
        return volumeTotal;
    }


    public void setVolumeTotal(Long volumeTotal) {
        this.volumeTotal = volumeTotal;
    }



    public Long getSnapshotTotal() {
        return snapshotTotal;
    }


    public void setSnapshotTotal(Long snapshotTotal) {
        this.snapshotTotal = snapshotTotal;
    }




    public Long getTemplateTotal() {
        return templateTotal;
    }


    public void setTemplateTotal(Long templateTotal) {
        this.templateTotal = templateTotal;
    }


    public Long getProjectTotal() {
        return projectTotal;
    }


    public void setProjectTotal(Long projectTotal) {
        this.projectTotal = projectTotal;
    }



    public Long getNetworkTotal() {
        return networkTotal;
    }


    public void setNetworkTotal(Long networkTotal) {
        this.networkTotal = networkTotal;
    }


    public Long getVpcTotal() {
        return vpcTotal;
    }


    public void setVpcTotal(Long vpcTotal) {
        this.vpcTotal = vpcTotal;
    }


    public Long getCpuTotal() {
        return cpuTotal;
    }


    public void setCpuTotal(Long cpuTotal) {
        this.cpuTotal = cpuTotal;
    }


    public Long getMemoryTotal() {
        return memoryTotal;
    }


    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }


    public Long getPrimaryStorageTotal() {
        return primaryStorageTotal;
    }


    public void setPrimaryStorageTotal(Long primaryStorageTotal) {
        this.primaryStorageTotal = primaryStorageTotal;
    }

    public Long getSecondaryStorageTotal() {
        return secondaryStorageTotal;
    }


    public void setSecondaryStorageTotal(Long secondaryStorageTotal) {
        this.secondaryStorageTotal = secondaryStorageTotal;
    }


    public Long getVmLimit() {
        return vmLimit;
    }


    public void setVmLimit(Long vmLimit) {
        this.vmLimit = vmLimit;
    }


    public Long getIpLimit() {
        return ipLimit;
    }


    public void setIpLimit(Long ipLimit) {
        this.ipLimit = ipLimit;
    }


    public Long getVolumeLimit() {
        return volumeLimit;
    }


    public void setVolumeLimit(Long volumeLimit) {
        this.volumeLimit = volumeLimit;
    }


    public Long getSnapshotLimit() {
        return snapshotLimit;
    }


    public void setSnapshotLimit(Long snapshotLimit) {
        this.snapshotLimit = snapshotLimit;
    }


    public Long getTemplateLimit() {
        return templateLimit;
    }


    public void setTemplateLimit(Long templateLimit) {
        this.templateLimit = templateLimit;
    }


    public Long getProjectLimit() {
        return projectLimit;
    }


    public void setProjectLimit(Long projectLimit) {
        this.projectLimit = projectLimit;
    }


    public Long getNetworkLimit() {
        return networkLimit;
    }


    public void setNetworkLimit(Long networkLimit) {
        this.networkLimit = networkLimit;
    }


    public Long getVpcLimit() {
        return vpcLimit;
    }


    public void setVpcLimit(Long vpcLimit) {
        this.vpcLimit = vpcLimit;
    }


    public Long getCpuLimit() {
        return cpuLimit;
    }


    public void setCpuLimit(Long cpuLimit) {
        this.cpuLimit = cpuLimit;
    }


    public Long getMemoryLimit() {
        return memoryLimit;
    }


    public void setMemoryLimit(Long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }


    public Long getPrimaryStorageLimit() {
        return primaryStorageLimit;
    }


    public void setPrimaryStorageLimit(Long primaryStorageLimit) {
        this.primaryStorageLimit = primaryStorageLimit;
    }


    public Long getSecondaryStorageLimit() {
        return secondaryStorageLimit;
    }


    public void setSecondaryStorageLimit(Long secondaryStorageLimit) {
        this.secondaryStorageLimit = secondaryStorageLimit;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public static Comparator<DomainJoinVO> domainIdComparator
            = new Comparator<DomainJoinVO>() {

        public int compare(DomainJoinVO domainJoinVO1, DomainJoinVO domainJoinVO2) {

            Long domainId1 = domainJoinVO1.getId();
            Long domainId2 = domainJoinVO2.getId();

            //-- ascending order
            return domainId1.compareTo(domainId2);
        }

    };
}
