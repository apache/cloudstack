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

import com.cloud.hypervisor.Hypervisor.HypervisorType;

/**
 * ConsoleProxyVO domain object
 */

@Entity
@Table(name = "console_proxy")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(value = "ConsoleProxy")
public class ConsoleProxyVO extends VMInstanceVO implements ConsoleProxy {

    @Column(name = "public_ip_address", nullable = false)
    private String publicIpAddress;

    @Column(name = "public_mac_address", nullable = false)
    private String publicMacAddress;

    @Column(name = "public_netmask", nullable = false)
    private String publicNetmask;

    @Column(name = "active_session", updatable = true, nullable = false)
    private int activeSession;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", updatable = true, nullable = true)
    private Date lastUpdateTime;

    @Column(name = "session_details", updatable = true, nullable = true)
    private byte[] sessionDetails;

    @Transient
    private boolean sslEnabled = false;

    @Transient
    private int port;

    /**
     * Correct constructor to use.
     * 
     */
    public ConsoleProxyVO(long id, long serviceOfferingId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long dataCenterId, long domainId, long accountId,
            int activeSession, boolean haEnabled) {
        super(id, serviceOfferingId, null, name, Type.ConsoleProxy, templateId, hypervisorType, guestOSId, domainId, accountId, haEnabled);
        this.activeSession = activeSession;
    }

    protected ConsoleProxyVO() {
        super();
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
