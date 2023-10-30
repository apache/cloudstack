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
package com.cloud.service;

import java.util.Date;
import java.util.Map;
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
import javax.persistence.Transient;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.offering.ServiceOffering;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;

@Entity
@Table(name = "service_offering")
public class ServiceOfferingVO implements ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name = null;

    @Column(name = "unique_name")
    private String uniqueName;

    @Column(name = "display_text", length = 4096)
    private String displayText = null;

    @Column(name = "customized")
    private boolean customized;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    ServiceOffering.State state = ServiceOffering.State.Active;

    @Column(name = "disk_offering_id")
    private Long diskOfferingId;

    @Column(name = "disk_offering_strictness")
    private boolean diskOfferingStrictness = false;

    @Column(name = "cpu")
    private Integer cpu;

    @Column(name = "speed")
    private Integer speed;

    @Column(name = "ram_size")
    private Integer ramSize;

    @Column(name = "nw_rate")
    private Integer rateMbps;

    @Column(name = "mc_rate")
    private Integer multicastRateMbps;

    @Column(name = "ha_enabled")
    private boolean offerHA;

    @Column(name = "limit_cpu_use")
    private boolean limitCpuUse;

    @Column(name = "is_volatile")
    private boolean volatileVm;

    @Column(name = "host_tag")
    private String hostTag;

    @Column(name = "default_use")
    private boolean defaultUse;

    @Column(name = "vm_type")
    private String vmType;

    @Column(name = "sort_key")
    int sortKey;

    @Column(name = "deployment_planner")
    private String deploymentPlanner = null;

    @Column(name = "system_use")
    private boolean systemUse;

    @Column(name = "dynamic_scaling_enabled")
    private boolean dynamicScalingEnabled = true;

    // This is a delayed load value.  If the value is null,
    // then this field has not been loaded yet.
    // Call service offering dao to load it.
    @Transient
    Map<String, String> details;

    // This flag is required to tell if the offering is dynamic once the cpu, memory and speed are set.
    // In some cases cpu, memory and speed are set to non-null values even if the offering is dynamic.
    @Transient
    boolean isDynamic;

    protected ServiceOfferingVO() {
        super();
    }

    public ServiceOfferingVO(String name, Integer cpu, Integer ramSize, Integer speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA, String displayText,
                             boolean systemUse, VirtualMachine.Type vmType, boolean defaultUse) {
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        limitCpuUse = false;
        volatileVm = false;
        this.defaultUse = defaultUse;
        this.vmType = vmType == null ? null : vmType.toString().toLowerCase();
        uuid = UUID.randomUUID().toString();
        this.systemUse = systemUse;
        this.name = name;
        this.displayText = displayText;
    }

    public ServiceOfferingVO(String name, Integer cpu, Integer ramSize, Integer speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA,
                             boolean limitResourceUse, boolean volatileVm, String displayText, boolean systemUse,
                             VirtualMachine.Type vmType, String hostTag, String deploymentPlanner, boolean dynamicScalingEnabled, boolean isCustomized) {
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.limitCpuUse = limitResourceUse;
        this.volatileVm = volatileVm;
        this.vmType = vmType == null ? null : vmType.toString().toLowerCase();
        this.hostTag = hostTag;
        this.deploymentPlanner = deploymentPlanner;
        uuid = UUID.randomUUID().toString();
        this.systemUse = systemUse;
        this.name = name;
        this.displayText = displayText;
        this.dynamicScalingEnabled = dynamicScalingEnabled;
        this.customized = isCustomized;
    }

    public ServiceOfferingVO(ServiceOfferingVO offering) {
        id = offering.getId();
        diskOfferingId = offering.getDiskOfferingId();
        name = offering.getName();
        displayText = offering.getDisplayText();
        customized = true;
        cpu = offering.getCpu();
        ramSize = offering.getRamSize();
        speed = offering.getSpeed();
        rateMbps = offering.getRateMbps();
        multicastRateMbps = offering.getMulticastRateMbps();
        offerHA = offering.isOfferHA();
        limitCpuUse = offering.getLimitCpuUse();
        volatileVm = offering.isVolatileVm();
        hostTag = offering.getHostTag();
        vmType = offering.getSystemVmType();
        systemUse = offering.isSystemUse();
        dynamicScalingEnabled = offering.isDynamicScalingEnabled();
        diskOfferingStrictness = offering.diskOfferingStrictness;
    }

    @Override
    public boolean isOfferHA() {
        return offerHA;
    }

    public void setOfferHA(boolean offerHA) {
        this.offerHA = offerHA;
    }

    @Override
    public boolean getLimitCpuUse() {
        return limitCpuUse;
    }

    public void setLimitResourceUse(boolean limitCpuUse) {
        this.limitCpuUse = limitCpuUse;
    }

    @Override
    public boolean getDefaultUse() {
        return defaultUse;
    }

    @Override
    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setRamSize(int ramSize) {
        this.ramSize = ramSize;
    }

    @Override
    public Integer getSpeed() {
        return speed;
    }

    @Override
    public Integer getRamSize() {
        return ramSize;
    }

    public void setRateMbps(Integer rateMbps) {
        this.rateMbps = rateMbps;
    }

    @Override
    public Integer getRateMbps() {
        return rateMbps;
    }

    public void setMulticastRateMbps(Integer multicastRateMbps) {
        this.multicastRateMbps = multicastRateMbps;
    }

    @Override
    public Integer getMulticastRateMbps() {
        return multicastRateMbps;
    }

    public void setHostTag(String hostTag) {
        this.hostTag = hostTag;
    }

    @Override
    public String getHostTag() {
        return hostTag;
    }

    @Override
    public String getSystemVmType() {
        return vmType;
    }

    @Override
    public void setSortKey(int key) {
        sortKey = key;
    }

    @Override
    public int getSortKey() {
        return sortKey;
    }

    @Override
    public boolean isVolatileVm() {
        return volatileVm;
    }

    @Override
    public String getDeploymentPlanner() {
        return deploymentPlanner;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public String getDetail(String name) {
        return details != null ? details.get(name) : null ;
    }

    public void setDetail(String name, String value) {
        assert (details != null) : "Did you forget to load the details?";

        details.put(name, value);
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    @Override
    public boolean isDynamic() {
        return cpu == null || speed == null || ramSize == null || isDynamic;
    }

    public void setDynamicFlag(boolean isdynamic) {
        isDynamic = isdynamic;
    }

    public boolean isCustomCpuSpeedSupported() {
        return isCustomized() && speed == null;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSystemUse() {
        return systemUse;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public boolean isCustomized() {
        return customized;
    }

    @Override
    public void setCustomized(boolean customized) {
        this.customized = customized;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public ServiceOffering.State getState() {
        return state;
    }

    @Override
    public void setState(ServiceOffering.State state) {
        this.state = state;
    }

    @Override
    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public void setDiskOfferingId(Long diskOfferingId) {
        this.diskOfferingId = diskOfferingId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return String.format("Service offering %s.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "name", "uuid"));
    }

    public boolean isDynamicScalingEnabled() {
        return dynamicScalingEnabled;
    }

    public void setDynamicScalingEnabled(boolean dynamicScalingEnabled) {
        this.dynamicScalingEnabled = dynamicScalingEnabled;
    }

    public Boolean getDiskOfferingStrictness() {
        return diskOfferingStrictness;
    }

    public void setDiskOfferingStrictness(boolean diskOfferingStrictness) {
        this.diskOfferingStrictness = diskOfferingStrictness;
    }
}
