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
package com.cloud.hypervisor;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.NumbersUtil;

@Entity
@Table(name="hypervisor_capabilities")
public class HypervisorCapabilitiesVO implements HypervisorCapabilities, Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name="hypervisor_version")
    private String hypervisorVersion;

    @Column(name="max_guests_limit")
    private Long maxGuestsLimit;

    @Column(name="security_group_enabled")
    private boolean securityGroupEnabled;

    @Column(name="uuid")
    private String uuid;

    protected HypervisorCapabilitiesVO() {
    	this.uuid = UUID.randomUUID().toString();
    }

    public HypervisorCapabilitiesVO(HypervisorType hypervisorType, String hypervisorVersion, Long maxGuestsLimit, boolean securityGroupEnabled) {
        this.hypervisorType = hypervisorType;
        this.hypervisorVersion = hypervisorVersion;
        this.maxGuestsLimit = maxGuestsLimit;
        this.securityGroupEnabled = securityGroupEnabled;
    	this.uuid = UUID.randomUUID().toString();
    }

    /**
     * @param hypervisorType the hypervisorType to set
     */
    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }


    /**
     * @return the hypervisorType
     */
    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    /**
     * @param hypervisorVersion the hypervisorVersion to set
     */
    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    /**
     * @return the hypervisorVersion
     */
    @Override
    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public void setSecurityGroupEnabled(Boolean securityGroupEnabled) {
        this.securityGroupEnabled = securityGroupEnabled;
    }

    /**
     * @return the securityGroupSupport
     */
    @Override
    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    /**
     * @param maxGuests the maxGuests to set
     */
    public void setMaxGuestsLimit(Long maxGuestsLimit) {
        this.maxGuestsLimit = maxGuestsLimit;
    }

    /**
     * @return the maxGuests
     */
    @Override
    public Long getMaxGuestsLimit() {
        return maxGuestsLimit;
    }


    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HypervisorCapabilitiesVO) {
            return ((HypervisorCapabilitiesVO)obj).getId() == this.getId();
        } else {
            return false;
        }
    }

}
