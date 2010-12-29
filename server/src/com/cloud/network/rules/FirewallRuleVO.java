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
package com.cloud.network.rules;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.Ip;

@Entity
@Table(name="firewall_rules")
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="purpose", discriminatorType=DiscriminatorType.STRING, length=32)
public class FirewallRuleVO implements FirewallRule {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name=GenericDao.XID_COLUMN)
    String xId;

    @Column(name="domain_id", updatable=false)
    long domainId;
    
    @Column(name="account_id", updatable=false)
    long accountId;
    
    @Column(name="ip_address", updatable=false)
    @Enumerated(value=EnumType.ORDINAL)
    Ip sourceIpAddress;
    
    @Column(name="start_port", updatable=false)
    int sourcePortStart;

    @Column(name="end_port", updatable=false)
    int sourcePortEnd;
    
    @Column(name="protocol", updatable=false)
    String protocol = "TCP";
    
    @Enumerated(value=EnumType.STRING)
    @Column(name="purpose")
    Purpose purpose;
    
    @Enumerated(value=EnumType.STRING)
    @Column(name="state")
    State state;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;
    
    @Column(name="network_id")
    long networkId;

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getXid() {
        return xId;
    }

    @Override
    public Ip getSourceIpAddress() {
        return sourceIpAddress;
    }

    @Override
    public int getSourcePortStart() {
        return sourcePortStart;
    }

    @Override
    public int getSourcePortEnd() {
        return sourcePortEnd;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }
    
    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public long getNetworkId() {
        return networkId;
    }
    
    public Date getCreated() {
        return created;
    }
    
    protected FirewallRuleVO() {
    }
    
    public FirewallRuleVO(String xId, Ip srcIp, int portStart, int portEnd, String protocol, long networkId, long accountId, long domainId, Purpose purpose) {
        this.xId = xId;
        if (xId == null) {
            this.xId = UUID.randomUUID().toString();
        }
        this.accountId = accountId;
        this.domainId = domainId;
        this.sourceIpAddress = srcIp;
        this.sourcePortStart = portStart;
        this.sourcePortEnd = portEnd;
        this.protocol = protocol;
        this.purpose = purpose;
        this.networkId = networkId;
        this.state = State.Staged;
    }
    
    public FirewallRuleVO(String xId, Ip srcIp, int port, String protocol, long networkId, long accountId, long domainId, Purpose purpose) {
        this(xId, srcIp, port, port, protocol, networkId, accountId, domainId, purpose);
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Rule[").append(id).append("-").append(purpose).append("-").append(state).append("]").toString();
    }
}
