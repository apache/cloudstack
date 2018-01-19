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
    public ConsoleProxyVO(long id, long serviceOfferingId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long dataCenterId, long domainId,
                          long accountId, long userId, int activeSession, boolean haEnabled) {
        super(id, serviceOfferingId, name, name, Type.ConsoleProxy, templateId, hypervisorType, guestOSId, domainId, accountId, userId, haEnabled);
        this.activeSession = activeSession;
        this.dataCenterId = dataCenterId;
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
        lastUpdateTime = time;
    }

    public void setSessionDetails(byte[] details) {
        sessionDetails = details;
    }

    @Override
    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    @Override
    public String getPublicNetmask() {
        return publicNetmask;
    }

    @Override
    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    @Override
    public int getActiveSession() {
        return activeSession;
    }

    @Override
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public byte[] getSessionDetails() {
        return sessionDetails;
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
