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
import com.cloud.utils.db.StateMachine;
import com.cloud.utils.fsm.FiniteStateObject;

@Entity
@Table(name="vm_instance")
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="type", discriminatorType=DiscriminatorType.STRING, length=32)
public class VMInstanceVO implements VirtualMachine, FiniteStateObject<State, VirtualMachine.Event> {
    @Id
    @TableGenerator(name="vm_instance_sq", table="sequence", pkColumnName="name", valueColumnName="value", pkColumnValue="vm_instance_seq", allocationSize=1)
    @Column(name="id", updatable=false, nullable = false)
	private long id;

    @Column(name="name", updatable=false, nullable=false, length=255)
	private String name = null;

    @Column(name="vnc_password", updatable=true, nullable=false, length=255)
    String vncPassword;
    
    @Column(name="proxy_id", updatable=true, nullable=true)
    Long proxyId;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="proxy_assign_time", updatable=true, nullable=true)
    Date proxyAssignTime;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value=EnumType.STRING)
    @StateMachine(state=State.class, event=Event.class)
    @Column(name="state", updatable=true, nullable=false, length=32)
    private State state = null;

    @Column(name="private_ip_address", updatable=true)
	private String privateIpAddress;

    @Column(name="instance_name", updatable=true, nullable=false)
    private String instanceName;

    @Column(name="vm_template_id", updatable=false, nullable=true, length=17)
	private Long templateId = new Long(-1);

    @Column(name="guest_os_id", nullable=false, length=17)
    private long guestOSId;
    
    @Column(name="host_id", updatable=true, nullable=true)
	private Long hostId;
    
    @Column(name="last_host_id", updatable=true, nullable=true)
    private Long lastHostId;

    @Column(name="pod_id", updatable=true, nullable=false)
    private long podId;

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
    
    @Column(name="domain_id")
    long domainId;
    
    @Column(name="account_id")
    long accountId;
    
    @Column(name="service_offering_id")
    long serviceOfferingId;
    
    public VMInstanceVO(long id,
                        long serviceOfferingId,
                        String name,
                        String instanceName,
                        Type type,
                        Long vmTemplateId,
                        long guestOSId,
                        long domainId,
                        long accountId,
                        boolean haEnabled) {
        this.id = id;
        this.name = name;
        if (vmTemplateId != null) {
            this.templateId = vmTemplateId;
        }
        this.instanceName = instanceName;
        this.type = type;
        this.guestOSId = guestOSId;
        this.haEnabled = haEnabled;
        this.mirroredVols = false;
        this.vncPassword = Long.toHexString(new Random().nextLong());
        this.state = State.Creating;
        this.accountId = accountId;
        this.domainId = domainId;
        this.serviceOfferingId = serviceOfferingId;
    }
                       
	
    public VMInstanceVO(long id,
                        long serviceOfferingId,
                        String name,
                        String instanceName,
                        Type type,
                        long vmTemplateId,
                        long guestOSId,
                        String privateMacAddress,
                        String privateIpAddress,
                        String privateNetmask,
                        long dataCenterId,
                        long podId,
                        long domainId,
                        long accountId,
                        boolean haEnabled,
                        Long hostId) {
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
        this.type = type;
        this.haEnabled = haEnabled;
        this.instanceName = instanceName;
        this.updated = 0;
        this.updateTime = new Date();
		this.vncPassword = Long.toHexString(new Random().nextLong());
		this.state = State.Creating;
		this.serviceOfferingId = serviceOfferingId;
		this.domainId = domainId;
		this.accountId =  accountId;
    }

    protected VMInstanceVO() {
    }
    
    public Date getRemoved() {
    	return removed;
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
    public Type getType() {
        return type;
    }
    
    public long getUpdated() {
    	return updated;
    }
    
	@Override
    public long getId() {
		return id;
	}
	
	@Override
    public Date getCreated() {
		return created;
	}
	
	public Date getUpdateTime() {
		return updateTime;
	}
	
	@Override
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
	@Override
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
    
    public long getServiceOfferingId() {
        return serviceOfferingId;
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
    public long getGuestOSId() {
		return guestOSId;
	}
	
	public void setGuestOSId(long guestOSId) {
		this.guestOSId = guestOSId;
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
    public long getPodId() {
        return podId;
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
	public String toString() {
    	return new StringBuilder("[").append(type.toString()).append("|").append(instanceName).append("]").toString();
    }
}
