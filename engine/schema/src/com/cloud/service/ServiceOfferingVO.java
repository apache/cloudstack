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

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.offering.ServiceOffering;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.vm.VirtualMachine;

@Entity
@Table(name = "service_offering")
@DiscriminatorValue(value = "Service")
@PrimaryKeyJoinColumn(name = "id")
public class ServiceOfferingVO extends DiskOfferingVO implements ServiceOffering {
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
    private boolean default_use;

    @Column(name = "vm_type")
    private String vm_type;

    @Column(name = "sort_key")
    int sortKey;

    @Column(name = "deployment_planner")
    private String deploymentPlanner = null;

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
            boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse, VirtualMachine.Type vm_type, boolean defaultUse) {
        super(name, displayText, false, tags, recreatable, useLocalStorage, systemUse, true);
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.limitCpuUse = false;
        this.volatileVm = false;
        this.default_use = defaultUse;
        this.vm_type = vm_type == null ? null : vm_type.toString().toLowerCase();
    }

    public ServiceOfferingVO(String name, Integer cpu, Integer ramSize, Integer speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA, boolean limitCpuUse,
            boolean volatileVm, String displayText, boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse, VirtualMachine.Type vm_type,
            Long domainId) {
        super(name, displayText, false, tags, recreatable, useLocalStorage, systemUse, true, domainId);
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.limitCpuUse = limitCpuUse;
        this.volatileVm = volatileVm;
        this.vm_type = vm_type == null ? null : vm_type.toString().toLowerCase();
    }

    public ServiceOfferingVO(String name, Integer cpu, Integer ramSize, Integer speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA,
            boolean limitResourceUse, boolean volatileVm, String displayText, boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse,
            VirtualMachine.Type vm_type, Long domainId, String hostTag) {
        this(name,
            cpu,
            ramSize,
            speed,
            rateMbps,
            multicastRateMbps,
            offerHA,
            limitResourceUse,
            volatileVm,
            displayText,
            useLocalStorage,
            recreatable,
            tags,
            systemUse,
            vm_type,
            domainId);
        this.hostTag = hostTag;
    }

    public ServiceOfferingVO(String name, Integer cpu, Integer ramSize, Integer speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA,
            boolean limitResourceUse, boolean volatileVm, String displayText, boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse,
            VirtualMachine.Type vm_type, Long domainId, String hostTag, String deploymentPlanner) {
        this(name,
            cpu,
            ramSize,
            speed,
            rateMbps,
            multicastRateMbps,
            offerHA,
            limitResourceUse,
            volatileVm,
            displayText,
            useLocalStorage,
            recreatable,
            tags,
            systemUse,
            vm_type,
            domainId,
            hostTag);
        this.deploymentPlanner = deploymentPlanner;
    }

    public ServiceOfferingVO(ServiceOfferingVO offering) {
        super(offering.getId(), offering.getName(), offering.getDisplayText(), false, offering.getTags(), offering.isRecreatable(), offering.getUseLocalStorage(), offering.getSystemUse(), true, offering.getDomainId());
        this.cpu = offering.getCpu();
        this.ramSize = offering.getRamSize();
        this.speed = offering.getSpeed();
        this.rateMbps = offering.getRateMbps();
        this.multicastRateMbps = offering.getMulticastRateMbps();
        this.offerHA = offering.getOfferHA();
        this.limitCpuUse = offering.getLimitCpuUse();
        this.volatileVm = offering.getVolatileVm();
        this.hostTag = offering.getHostTag();
        this.vm_type = offering.getSystemVmType();
    }

    @Override
    public boolean getOfferHA() {
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
        return default_use;
    }

    @Override
    @Transient
    public String[] getTagsArray() {
        String tags = getTags();
        if (tags == null || tags.length() == 0) {
            return new String[0];
        }

        return tags.split(",");
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
        return vm_type;
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
    public boolean getVolatileVm() {
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
        assert (details != null) : "Did you forget to load the details?";

        return details != null ? details.get(name) : null;
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
        this.isDynamic = isdynamic;
    }
}
