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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.uservm.UserVm;

@Entity
@Table(name="user_vm")
@DiscriminatorValue(value="User")
@PrimaryKeyJoinColumn(name="id")
public class UserVmVO extends VMInstanceVO implements UserVm {

    @Column(name="iso_id", nullable=true, length=17)
    private Long isoId = null;
    
    @Column(name="user_data", updatable=true, nullable=true, length=2048)
    private String userData;
    
    @Column(name="display_name", updatable=true, nullable=true)
    private String displayName;
    
    transient String password;

    @Override
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Long getIsoId() {
        return isoId;
    }
    
    @Override
    public long getServiceOfferingId() {
        return serviceOfferingId;
    }
    
    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public UserVmVO(long id,
                    String instanceName,
                    String displayName,
                    long templateId,
                    HypervisorType hypervisorType,
                    long guestOsId,
                    boolean haEnabled,
                    boolean limitCpuUse,
                    long domainId,
                    long accountId,
                    long serviceOfferingId,
                    String userData,
                    String name) {
        super(id, serviceOfferingId, name, instanceName, Type.User, templateId, hypervisorType, guestOsId, domainId, accountId, haEnabled, limitCpuUse);
        this.userData = userData;
        this.displayName = displayName != null ? displayName : name != null ? name : instanceName;
    	this.details = new HashMap<String, String>();
    }
    
    protected UserVmVO() {
        super();
    }

	public void setIsoId(Long id) {
	    this.isoId = id;
	}
	
    @Override
	public void setUserData(String userData) {
		this.userData = userData;
	}

    @Override
	public String getUserData() {
		return userData;
	}
	
	@Override
	public String getDisplayName() {
	    return displayName;
	}
	
	public void setDisplayName(String displayName) {
	    this.displayName = displayName;
	}
	
    @Override
    public String getDetail(String name) {
        assert (details != null) : "Did you forget to load the details?";
        
        return details != null ? details.get(name) : null;
    }
    
    public void setAccountId(long accountId){
        this.accountId = accountId;
    }
    
    public void setDomainId(long domainId){
        this.domainId = domainId;
    }
}
