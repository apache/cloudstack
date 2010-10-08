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

package com.cloud.vm;

import java.util.Date;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="vm_instance")
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="type", discriminatorType=DiscriminatorType.STRING, length=32)
public class VMInstanceVO implements VirtualMachine {
    @Id
    @TableGenerator(name="vm_instance_sq", table="sequence", pkColumnName="name", valueColumnName="value", pkColumnValue="vm_instance_seq", allocationSize=1)
    @Column(name="id", updatable=false, nullable = false)
	private Long id = null;

    @Column(name="name", updatable=false, nullable=false, length=255)
	private String name = null;

    @Column(name="storage_ip")
    private String storageIp = null;

    @Column(name="display_name", updatable=true, nullable=true)
    private String displayName;
    
    @Column(name="vnc_password", updatable=true, nullable=false, length=255)
    String vncPassword;
    
    @Column(name="proxy_id", updatable=true, nullable=true)
    Long proxyId;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="proxy_assign_time", updatable=true, nullable=true)
    Date proxyAssignTime;

	@Column(name="group", updatable=true, nullable=true)
    private String group;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value=EnumType.STRING)
    @Column(name="state", updatable=true, nullable=false, length=32)
    private State state = null;

    @Column(name="private_ip_address", updatable=true)
	private String privateIpAddress;

    @Column(name="instance_name", updatable=true, nullable=false)
    private String instanceName;

    @Column(name="vm_template_id", updatable=false, nullable=true, length=17)
	private Long templateId = new Long(-1);

    @Column(name="iso_id", nullable=true, length=17)
    private Long isoId = null;
    
    @Column(name="guest_os_id", nullable=false, length=17)
    private long guestOSId;
    
    @Column(name="host_id", updatable=true, nullable=true)
	private Long hostId;
    
    @Column(name="last_host_id", updatable=true, nullable=true)
    private Long lastHostId;

    @Column(name="pod_id", updatable=true, nullable=true)
    private Long podId;

    @Column(name="private_mac_address", updatable=true, nullable=true)
    String privateMacAddress;

    @Column(name="private_netmask")
    private String privateNetmask;

    @Column(name="data_center_id", updatable=true, nullable=false)
    long dataCenterId;

    @Column(name="type", updatable=false, nullable=false, length=32)
    @Enumerated(value=EnumType.STRING)
    Type type;

    @Column(name="ha_enabled", updatable=true, nullable=true)
    boolean haEnabled;
    
    @Column(name="mirrored_vols", updatable=true, nullable=true)
    boolean mirroredVols;

    @Column(name="update_count", updatable = true, nullable=false)
    long updated;	// This field should be updated everytime the state is updated.  There's no set method in the vo object because it is done with in the dao code.
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;
    
    @Column(name="update_time", updatable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    Date updateTime;
	
    public VMInstanceVO(long id,
                        String name,
                        Type type,
                        long templateId,
                        long guestOSId,
                        String privateMacAddress,
                        String privateIpAddress,
                        String privateNetmask,
                        long dataCenterId,
                        Long podId,
                        boolean haEnabled,
                        Long hostId,
                        String displayName,
                        String group) {
        this(id, name, name, State.Creating, type, templateId, guestOSId, privateMacAddress, privateIpAddress, privateNetmask, dataCenterId, podId, haEnabled, hostId, displayName, group);
    }
    
    public VMInstanceVO(Long id,
                        String name,
                        String instanceName,
                        State state,
                        Type type,
                        long vmTemplateId,
                        long guestOSId,
                        String privateMacAddress,
                        String privateIpAddress,
                        String privateNetmask,
                        long dataCenterId,
                        Long podId,
                        boolean haEnabled,
                        Long hostId,
                        String displayName,
                        String group) {
        super();
        this.id = id;
        this.name = name;
        if (vmTemplateId > -1)
        	this.templateId = vmTemplateId;
        else
        	this.templateId = null;
        this.guestOSId = guestOSId;
        this.privateIpAddress = privateIpAddress;
        this.privateMacAddress = privateMacAddress;
        this.privateNetmask = privateNetmask;
        this.hostId = hostId;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.state = state;
        this.type = type;
        this.haEnabled = haEnabled;
        this.instanceName = instanceName;
        this.storageIp = null;
        this.updated = 0;
        this.updateTime = new Date();
        if (displayName != null)
    		this.displayName = displayName;
        else if (type == Type.User)
        	this.displayName = name;
        this.group = group;
        
		this.vncPassword = Long.toHexString(new Random().nextLong());
    }

    protected VMInstanceVO() {
    }
    
    public Date getRemoved() {
    	return removed;
    }
    
    @Override
    public Type getType() {
        return type;
    }
    
    public long getUpdated() {
    	return updated;
    }
    
	public Long getId() {
		return id;
	}
	
	public Date getCreated() {
		return created;
	}
	
	public Date getUpdateTime() {
		return updateTime;
	}
	
	public long getDataCenterId() {
	    return dataCenterId;
	}
	
    public void setPrivateNetmask(String privateNetmask) {
        this.privateNetmask = privateNetmask;
    }

    public String getPrivateNetmask() {
        return privateNetmask;
    }
	

	public void setId(long id) {
		this.id = id;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getInstanceName() {
	    return instanceName;
	}
	
	public void setInstanceName(String instanceName) {
	    this.instanceName = instanceName;
	}
	
	@Override
	public State getState() {
		return state;
	}
	
	// don't use this directly, use VM state machine instead, this method is added for migration tool only
	public void setState(State state) {
		this.state = state;
	}
	
	@Override
	public String getPrivateIpAddress() {
		return privateIpAddress;
	}
	
	public void setPrivateIpAddress(String address) {
		privateIpAddress = address;
	}
    
    public void setVncPassword(String vncPassword) {
        this.vncPassword = vncPassword;
    }
    
    @Override
    public String getVncPassword() {
        return vncPassword;
    }
    
	public Long getProxyId() {
    	return proxyId;
    }
    
    public void setProxyId(Long proxyId) {
    	this.proxyId = proxyId;
    }
    
    public Date getProxyAssignTime() {
    	return this.proxyAssignTime;
    }
    
    public void setProxyAssignTime(Date time) {
    	this.proxyAssignTime = time;
    }
	
	@Override
	public long getTemplateId() {
		if (templateId == null)
			return -1;
		else
			return templateId;
	}
	
	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}

	@Override
	public Long getIsoId() {
	    return isoId;
	}
	
	public long getGuestOSId() {
		return guestOSId;
	}
	
	public void setGuestOSId(long guestOSId) {
		this.guestOSId = guestOSId;
	}

    @Override
	public void setIsoId(Long isoId) {
	    this.isoId = isoId;
	}

	public void incrUpdated() {
		updated++;
	}

	@Override
	public Long getHostId() {
		return hostId;
	}
	
	@Override
	public Long getLastHostId() {
		return lastHostId;
	}
	
	public void setLastHostId(Long lastHostId) {
		this.lastHostId = lastHostId;
	}
	
	public void setHostId(Long hostId) {
		this.hostId = hostId;
	}
	
    @Override
    public boolean isHaEnabled() {
        return haEnabled;
    }

    @Override
    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    @Override
    public Long getPodId() {
        return podId;
    }
    
    public String getStorageIp() {
    	return storageIp;
    }
    
    public void setStorageIp(String storageIp) {
    	this.storageIp = storageIp;
    }
    
    public void setPodId(long podId) {
        this.podId = podId;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }
    
    public boolean isRemoved() {
        return removed != null;
    }
    
    @Override
	public boolean isMirroredVols() {
		return mirroredVols;
	}
    
    public void setHaEnabled(boolean value) {
        haEnabled = value;
    }

	public void setMirroredVols(boolean mirroredVols) {
		this.mirroredVols = mirroredVols;
	}
	
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
	
    @Override
	public String toString() {
    	return new StringBuilder("[").append(type.toString()).append("|").append(instanceName).append("]").toString();
    }
}
