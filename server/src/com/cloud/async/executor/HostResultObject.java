package com.cloud.async.executor;

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
import java.util.Date;
import java.util.Set;

import com.cloud.host.Status.Event;
import com.cloud.serializer.Param;

public class HostResultObject {
	@Param(name="id")
	private long id;
	
	@Param(name="averageload")
	private long averageLoad;
	
	@Param(name="name")
	private String name;

    @Param(name="state")
    private String state;

    @Param(name="type")
    private String type;

    @Param(name="ipaddress")
	private String ipAddress;

    @Param(name="hypervisor")
    private String hypervisorType;

    @Param(name="fstype")
    private String fsType;

//    @Param(name="available")
//    private boolean available;
//
//    @Param(name="setup")
//    private boolean setup;

    @Param(name="zoneid")
    private long zoneId;

    @Param(name="zonename")
    private String zoneName;
    
    @Param(name="podid")
	private Long podId;

    @Param(name="podname")
    private String podName;

    @Param(name="cpuallocated")
    private String cpuAllocated;

    @Param(name="cpuused")
    private String cpuUsed;
    
    @Param(name="cpunumber")
    private long cpuNumber;

    @Param(name="url")
    private String storageUrl;

    @Param(name="cpuspeed")
    private Long cpuSpeed;

    @Param(name="memorytotal")
    private long totalMemory;

    @Param(name="memoryallocated")
    private long memoryAllocated;
    
    @Param(name="memoryused")
    private long memoryUsed;

    @Param(name="disksizetotal")
    private long diskSizeTotal;
    
    @Param(name="disksizeallocated")
    private long diskSizeAllocated;
    
    @Param(name="capabilities")
    private String caps;

    @Param(name="totalsize")
    private Long totalSize;

    @Param(name="managementserverid")
    private Long managementServerId;

    @Param(name="version")
    private String version;

    @Param(name="created")
    private Date created;

    @Param(name="removed")
    private Date removed;

    @Param(name="disconnected")
    private Date disconnected;

    @Param(name="events")
    private Set<Event> events;

    @Param(name="oscategoryid")
    private Long osCategoryId;

    @Param(name="oscategoryname")
    private String osCategoryName;
    
    @Param(name="lastpinged")
    private long lastPinged;
    
    @Param(name="networkkbsread")
    private Long networkKbsRead;
    
    @Param(name="networkkbswrite")
    private Long networkKbsWrite;

    public long getId(){
    	return this.id;
    }
    
    public void setId(long id){
    	this.id = id;
    }
    
    public void setOsCategoryId(long osCategoryId){
    	this.osCategoryId = osCategoryId;
    }
    
    public void setOsCategoryName(String osCategoryName){
    	this.osCategoryName = osCategoryName;
    }

    public Long getOsCategoryId(){
    	return this.osCategoryId;
    }
    
    public String getOsCategoryName(){
    	return this.osCategoryName;
    }
    
    public HostResultObject() {
    }

    public Set<Event>getEvents()
    {
    	return this.events;
    }

    public void setEvents(Set<Event> eventSet)
    {
    	this.events = eventSet;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getHypervisorType() {
		return hypervisorType;
	}

	public void setHypervisorType(String hypervisorType) {
		this.hypervisorType = hypervisorType;
	}

	public String getFsType() {
		return fsType;
	}

	public void setFsType(String fsType) {
		this.fsType = fsType;
	}

//	public boolean isAvailable() {
//		return available;
//	}
//
//	public void setAvailable(boolean available) {
//		this.available = available;
//	}
//
//	public boolean isSetup() {
//		return setup;
//	}
//
//	public void setSetup(boolean setup) {
//		this.setup = setup;
//	}

	public long getZoneId() {
		return zoneId;
	}

	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}

	public String getZoneName() {
		return zoneName;
	}

	public String getPodName() {
		return podName;
	}
	
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public void setPodName(String podName) {
		this.podName = podName;
	}

	public Long getPodId() {
		return podId;
	}

	public void setPodId(Long podId) {
		this.podId = podId;
	}

	public String getCpuAllocated() {
		return cpuAllocated;
	}

	public void setCpuAllocated(String cpuAllocated) {
		this.cpuAllocated = cpuAllocated;
	}

	public String getCpuUsed() {
		return cpuUsed;
	}

	public void setCpuUsed(String cpuUsed) {
		this.cpuUsed = cpuUsed;
	}

	public long getCpuNumber() {
		return cpuNumber;
	}

	public void setCpuNumber(long cpuNumber) {
		this.cpuNumber = cpuNumber;
	}
	
	public String getStorageUrl() {
		return storageUrl;
	}

	public void setStorageUrl(String storageUrl) {
		this.storageUrl = storageUrl;
	}

	public Long getCpuSpeed() {
		return cpuSpeed;
	}

	public void setCpuSpeed(Long cpuSpeed) {
		this.cpuSpeed = cpuSpeed;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}

	public long getMemoryAllocated() {
		return memoryAllocated;
	}

	public void setMemoryAllocated(long memoryAllocated) {
		this.memoryAllocated = memoryAllocated;
	}

	public long getMemoryUsed() {
		return memoryUsed;
	}

	public void setMemoryUsed(long memoryUsed) {
		this.memoryUsed = memoryUsed;
	}
	
//	public long getDiskSize() {
//		return diskSizeTotal;
//	}
	
	public long isDiskSizeTotal() {
		return diskSizeTotal;
	}

	public void setDiskSizeTotal(long diskSizeTotal) {
		this.diskSizeTotal = diskSizeTotal;
	}

	public long getDiskSizeAllocated() {
		return diskSizeAllocated;
	}

	public void setDiskSizeAllocated(long diskSizeAllocated) {
		this.diskSizeAllocated = diskSizeAllocated;
	}

	public String getCaps() {
		return caps;
	}

	public void setCaps(String caps) {
		this.caps = caps;
	}

	public Long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(Long totalSize) {
		this.totalSize = totalSize;
	}

	public long getLastPinged() {
		return lastPinged;
	}

	public void setLastPinged(long l) {
		this.lastPinged = l;
	}

	public Long getManagementServerId() {
		return managementServerId;
	}

	public void setManagementServerId(Long managementServerId) {
		this.managementServerId = managementServerId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

	public Date getDisconnected() {
		return disconnected;
	}

	public void setDisconnected(Date disconnected) {
		this.disconnected = disconnected;
	}
	
	public long getAverageLoad(){
		return this.averageLoad;
	}
	
	public void setAverageLoad(long averageLoad){
		this.averageLoad = averageLoad;
	}

	public Long getNetworkKbsRead(){
		return this.networkKbsRead;
	}

	public void setNetworkKbsRead(long networkKbsRead){
		this.networkKbsRead = networkKbsRead;
	}
		
	public Long getNetworkKbsWrite(){
		return this.networkKbsWrite;
	}

	public void setNetworkKbsWrite(long networkKbsWrite){
		this.networkKbsWrite = networkKbsWrite;
	}
}
