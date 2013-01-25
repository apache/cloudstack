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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="service_offering_view")
public class ServiceOfferingJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id", updatable=false, nullable = false)
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="name")
    private String name;

    @Column(name="display_text")
    private String displayText;

    @Column(name="tags", length=4096)
    String tags;

    @Column(name="use_local_storage")
    private boolean useLocalStorage;

    @Column(name="system_use")
    private boolean systemUse;

    @Column(name="cpu")
    private int cpu;

    @Column(name="speed")
    private int speed;

    @Column(name="ram_size")
    private int ramSize;

    @Column(name="nw_rate")
    private Integer rateMbps;

    @Column(name="mc_rate")
    private Integer multicastRateMbps;

    @Column(name="ha_enabled")
    private boolean offerHA;

    @Column(name="limit_cpu_use")
    private boolean limitCpuUse;

    @Column(name="host_tag")
    private String hostTag;

    @Column(name="default_use")
    private boolean default_use;

    @Column(name="vm_type")
    private String vm_type;

    @Column(name="sort_key")
    int sortKey;


    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName = null;

    @Column(name="domain_path")
    private String domainPath = null;


    public ServiceOfferingJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }


    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isUseLocalStorage() {
        return useLocalStorage;
    }

    public void setUseLocalStorage(boolean useLocalStorage) {
        this.useLocalStorage = useLocalStorage;
    }

    public boolean isSystemUse() {
        return systemUse;
    }

    public void setSystemUse(boolean systemUse) {
        this.systemUse = systemUse;
    }


    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getRamSize() {
        return ramSize;
    }

    public void setRamSize(int ramSize) {
        this.ramSize = ramSize;
    }

    public Integer getRateMbps() {
        return rateMbps;
    }

    public void setRateMbps(Integer rateMbps) {
        this.rateMbps = rateMbps;
    }

    public Integer getMulticastRateMbps() {
        return multicastRateMbps;
    }

    public void setMulticastRateMbps(Integer multicastRateMbps) {
        this.multicastRateMbps = multicastRateMbps;
    }

    public boolean isOfferHA() {
        return offerHA;
    }

    public void setOfferHA(boolean offerHA) {
        this.offerHA = offerHA;
    }

    public boolean isLimitCpuUse() {
        return limitCpuUse;
    }

    public void setLimitCpuUse(boolean limitCpuUse) {
        this.limitCpuUse = limitCpuUse;
    }

    public String getHostTag() {
        return hostTag;
    }

    public void setHostTag(String hostTag) {
        this.hostTag = hostTag;
    }

    public boolean isDefaultUse() {
        return default_use;
    }

    public void setDefaultUse(boolean default_use) {
        this.default_use = default_use;
    }

    public String getSystemVmType() {
        return vm_type;
    }

    public void setSystemVmType(String vm_type) {
        this.vm_type = vm_type;
    }


}
