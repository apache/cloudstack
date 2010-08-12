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

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * ConsoleProxyVO domain object
 */

@Entity
@Table(name="console_proxy")
@PrimaryKeyJoinColumn(name="id")
@DiscriminatorValue(value="ConsoleProxy")
public class ConsoleProxyVO extends VMInstanceVO implements ConsoleProxy {

    @Column(name="gateway", nullable=false)
    private String gateway;
    
    @Column(name="dns1")
    private String dns1;
    
    @Column(name="dns2")
    private String dns2;

    @Column(name="guest_mac_address")
    private String guestMacAddress;
    
    @Column(name="guest_ip_address")
    private String guestIpAddress;
    
    @Column(name="guest_netmask")
    private String guestNetmask;
    
    @Column(name="public_ip_address", nullable=false)
    private String publicIpAddress;
    
    @Column(name="public_mac_address", nullable=false)
    private String publicMacAddress;
    
    @Column(name="public_netmask", nullable=false)
    private String publicNetmask;
    
    @Column(name="vlan_db_id")
    private Long vlanDbId;
    
    @Column(name="vlan_id")
    private String vlanId;
    
    @Column(name="domain", nullable=false)
    private String domain;
    
    @Column(name="ram_size", updatable=false, nullable=false)
    private int ramSize;
    
    @Column(name="active_session", updatable=true, nullable=false)
    private int activeSession;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_update", updatable=true, nullable=true)
    private Date lastUpdateTime;
    
    @Column(name="session_details", updatable=true, nullable=true)
    private byte[] sessionDetails;

    @Transient
    private boolean sslEnabled = false;
    
	@Transient
    private int port;
    
    public ConsoleProxyVO(
    		long id,
            String name,
            State state,
            String guestMacAddress,
            String guestIpAddress,
            String guestNetMask,
            String privateMacAddress,
            String privateIpAddress,
            String privateNetmask,
            long templateId,
            long guestOSId,
            String publicMacAddress,
            String publicIpAddress,
            String publicNetmask,
            Long vlanDbId,
            String vlanId,
            long podId,
            long dataCenterId,
            String gateway,
            Long hostId,
            String dns1,
            String dns2,
            String domain,
            int ramSize,
            int activeSession) {
    	super(id, name, name, state, Type.ConsoleProxy, templateId, guestOSId,
    			privateMacAddress, privateIpAddress, privateNetmask, dataCenterId, podId, true, hostId, null, null);
    	this.gateway = gateway;
    	this.publicIpAddress = publicIpAddress;
    	this.publicNetmask = publicNetmask;
    	this.publicMacAddress = publicMacAddress;
    	this.guestIpAddress = guestIpAddress;
    	this.guestMacAddress = guestMacAddress;
    	this.guestNetmask = guestNetMask;
    	this.vlanDbId = vlanDbId;
    	this.vlanId = vlanId;
    	this.dns1 = dns1;
    	this.dns2 = dns2;
    	this.domain = domain;
    	this.ramSize = ramSize;
    	this.activeSession = activeSession;
    }
    
    protected ConsoleProxyVO() {
        super();
    }

    public void setGateway(String gateway) {
    	this.gateway = gateway;
    }
    
    public void setDns1(String dns1) {
    	this.dns1 = dns1;
    }
    
    public void setDns2(String dns2) {
    	this.dns2 = dns2;
    }
    
    public void setDomain(String domain) {
    	this.domain = domain;
    }
    
    public void setPublicIpAddress(String publicIpAddress) {
    	this.publicIpAddress = publicIpAddress;
    }
    
    public void setPublicNetmask(String publicNetmask) {
    	this.publicNetmask = publicNetmask;
    }
    
    public void setPublicMacAddress(String publicMacAddress) {
    	this.publicMacAddress = publicMacAddress;
    }
    
    public void setGuestIpAddress(String guestIpAddress) {
    	this.guestIpAddress = guestIpAddress;
    }
    
    public void setGuestNetmask(String guestNetmask) {
    	this.guestNetmask = guestNetmask;
    }
    
    public void setGuestMacAddress(String guestMacAddress) {
    	this.guestMacAddress = guestMacAddress;
    }
    
    public void setRamSize(int ramSize) {
    	this.ramSize = ramSize;
    }
    
    public void setActiveSession(int activeSession) {
    	this.activeSession = activeSession;
    }
    
    public void setLastUpdateTime(Date time) {
    	this.lastUpdateTime = time;
    }
    
    public void setSessionDetails(byte[] details) {
    	this.sessionDetails = details;
    }
	
    @Override
	public String getGateway() {
		return this.gateway;
	}
	
    @Override
	public String getDns1() {
    	return this.dns1;
	}
	
    @Override
	public String getDns2() {
    	return this.dns2;
	}
	
    @Override
	public String getPublicIpAddress() {
    	return this.publicIpAddress;
	}
	
    @Override
	public String getPublicNetmask() {
    	return this.publicNetmask;
	}
	
    @Override
	public String getPublicMacAddress() {
		return this.publicMacAddress;
	}
    

	public String getGuestIpAddress() {
    	return this.guestIpAddress;
	}
	

	public String getGuestNetmask() {
    	return this.guestNetmask;
	}
	

	public String getGuestMacAddress() {
		return this.guestMacAddress;
	}
    
    @Override
	public Long getVlanDbId() {
    	return vlanDbId;
    }
    
    @Override
	public String getVlanId() {
    	return vlanId;
    }
    
    @Override
    public String getDomain() {
    	return this.domain;
    }
	
    @Override
	public int getRamSize() {
    	return this.ramSize;
    }
    
    @Override
	public int getActiveSession() {
    	return this.activeSession;
	}
    
    @Override
    public Date getLastUpdateTime() {
    	return this.lastUpdateTime;
    }
    
    @Override
    public byte[] getSessionDetails() {
    	return this.sessionDetails;
    }

    public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}
    
    public void setPort(int port) {
    	this.port = port;
    }
    
    public int getPort() {
    	return port;
    }

}
