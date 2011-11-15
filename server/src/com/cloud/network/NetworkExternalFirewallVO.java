/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

package com.cloud.network;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

/**
 * NetworkExternalFirewallVO contains information on the networks that are using external firewall
  */

@Entity
@Table(name="network_external_firewall_device_map")
public class NetworkExternalFirewallVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "external_firewall_device_id")
    private long externalFirewallDeviceId;

    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;

    public NetworkExternalFirewallVO(long networkId, long externalFirewallDeviceId) {
        this.networkId = networkId;
        this.externalFirewallDeviceId = externalFirewallDeviceId;
        this.uuid = UUID.randomUUID().toString();
    }

    public NetworkExternalFirewallVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }

    public long getNetworkId() {
        return networkId;
    }

    public long getExternalFirewallDeviceId() {
        return externalFirewallDeviceId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
