/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.network.element;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name=("virtual_router_elements"))
public class VirtualRouterElementsVO implements VirtualRouterElements {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="type")
    @Enumerated(EnumType.STRING)
    private VirtualRouterElementsType type;
    
    @Column(name="ready")
    private boolean isReady;
    
    @Column(name="nsp_id")
    private long nspId;
    
    @Column(name="uuid")
    private String uuid;
    
    @Column(name="dhcp_provided")
    private boolean isDhcpProvided;
    
    @Column(name="dns_provided")
    private boolean isDnsProvided;
    
    @Column(name="gateway_provided")
    private boolean isGatewayProvided;
    
    @Column(name="firewall_provided")
    private boolean isFirewallProvided;
    
    @Column(name="source_nat_provided")
    private boolean isSourceNatProvided;
    
    @Column(name="load_balance_provided")
    private boolean isLoadBalanceProvided;
    
    @Column(name="vpn_provided")
    private boolean isVpnProvided;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;

    public VirtualRouterElementsVO() {
    }
    
    public VirtualRouterElementsVO(long nspId, String uuid, VirtualRouterElementsType type, boolean isDhcpProvided, boolean isDnsProvided,
            boolean isGatewayProvided, boolean isFirewallProvided, boolean isSourceNatProvided, boolean isLoadBalanceProvided, boolean isVpnProvided) {
        this.nspId = nspId;
        this.uuid = uuid;
        this.type = type;
        this.isDhcpProvided = isDhcpProvided;
        this.isDnsProvided = isDnsProvided;
        this.isGatewayProvided = isGatewayProvided;
        this.isFirewallProvided = isFirewallProvided;
        this.isSourceNatProvided = isSourceNatProvided;
        this.isLoadBalanceProvided = isLoadBalanceProvided;
        this.isVpnProvided = isVpnProvided;
    }

    public long getNspId() {
        return nspId;
    }

    public String getUuid() {
        return uuid;
    }

    public long getId() {
        return id;
    }

    public boolean getIsDhcpProvided() {
        return isDhcpProvided;
    }

    public boolean getIsDnsProvided() {
        return isDnsProvided;
    }

    public boolean getIsGatewayProvided() {
        return isGatewayProvided;
    }

    public boolean getIsFirewallProvided() {
        return isFirewallProvided;
    }

    public boolean getIsSourceNatProvided() {
        return isSourceNatProvided;
    }

    public boolean getIsLoadBalanceProvided() {
        return isLoadBalanceProvided;
    }

    public boolean getIsVpnProvided() {
        return isVpnProvided;
    }
    
    public void setIsDhcpProvided(boolean isDhcpProvided) {
        this.isDhcpProvided = isDhcpProvided;
    }

    public void setIsDnsProvided(boolean isDnsProvided) {
        this.isDnsProvided = isDnsProvided;
    }

    public void setIsGatewayProvided(boolean isGatewayProvided) {
        this.isGatewayProvided = isGatewayProvided;
    }

    public void setIsFirewallProvided(boolean isFirewallProvided) {
        this.isFirewallProvided = isFirewallProvided;
    }

    public void setIsSourceNatProvided(boolean isSourceNatProvided) {
        this.isSourceNatProvided = isSourceNatProvided;
    }

    public void setIsLoadBalanceProvided(boolean isLoadBalanceProvided) {
        this.isLoadBalanceProvided = isLoadBalanceProvided;
    }

    public void setIsVpnProvided(boolean isVpnProvided) {
        this.isVpnProvided = isVpnProvided;
    }
    
    @Override
    public VirtualRouterElementsType getType() {
        return this.type;
    }
    
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setIsReady(boolean isReady) {
        this.isReady = isReady;
    }

    public boolean getIsReady() {
        return isReady;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setType(VirtualRouterElementsType type) {
        this.type = type;
    }

    public void setNspId(long nspId) {
        this.nspId = nspId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
