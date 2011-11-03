/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.service;

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
@Table(name="service_offering")
@DiscriminatorValue(value="Service")
@PrimaryKeyJoinColumn(name="id")
public class ServiceOfferingVO extends DiskOfferingVO implements ServiceOffering {
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
    
    protected ServiceOfferingVO() {
        super();
    }

    public ServiceOfferingVO(String name, int cpu, int ramSize, int speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA, String displayText, boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse, VirtualMachine.Type vm_type, boolean defaultUse) {
        super(name, displayText, false, tags, recreatable, useLocalStorage, systemUse, true);
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.limitCpuUse = false; 
        this.default_use = defaultUse;
        this.vm_type = vm_type == null ? null : vm_type.toString().toLowerCase();
    }

    public ServiceOfferingVO(String name, int cpu, int ramSize, int speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA, boolean limitCpuUse, String displayText, boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse, VirtualMachine.Type vm_type, Long domainId) {
        super(name, displayText, false, tags, recreatable, useLocalStorage, systemUse, true, domainId);
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.limitCpuUse = limitCpuUse;  
        this.vm_type = vm_type == null ? null : vm_type.toString().toLowerCase();
    }

    public ServiceOfferingVO(String name, int cpu, int ramSize, int speed, Integer rateMbps, Integer multicastRateMbps, boolean offerHA, boolean limitResourceUse, String displayText, boolean useLocalStorage, boolean recreatable, String tags, boolean systemUse, VirtualMachine.Type vm_type, Long domainId, String hostTag) {
        this(name, cpu, ramSize, speed, rateMbps, multicastRateMbps, offerHA, limitResourceUse, displayText, useLocalStorage, recreatable, tags, systemUse, vm_type, domainId);
        this.hostTag = hostTag;
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
	public int getCpu() {
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
	public int getSpeed() {
	    return speed;
	}
	
	@Override
	public int getRamSize() {
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
	
	public String getHostTag() {
		return hostTag;
	}
	
	public String getSystemVmType(){
	    return vm_type;
	}
	
    public void setSortKey(int key) {
    	sortKey = key;
    }
    
    public int getSortKey() {
    	return sortKey;
    }
	
}
