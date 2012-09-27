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

package com.cloud.network.ovs.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name=("ovs_tunnel_network"))
public class OvsTunnelNetworkVO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "from")
	private long from;

	@Column(name = "to")
	private long to;

	@Column(name = "key")
	private int key;
	
	@Column(name = "network_id")
	private long networkId;
	
	@Column(name = "port_name")
    private String portName;
	
	@Column(name = "state")
	private String state;
	
	public OvsTunnelNetworkVO() {
		
	}
	
	public OvsTunnelNetworkVO(long from, long to, int key, long networkId) {
		this.from = from;
		this.to = to;
		this.key = key;
		this.networkId = networkId;
		this.portName = "[]";
		this.state = "FAILED";
	}
	
	public void setKey(int key) {
		this.key = key;
	}
	
	public long getFrom() {
		return from;
	}
	
	public long getTo() {
		return to;
	}
	
	public int getKey() {
		return key;
	}
	
	public long getId() {
		return id;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public String getState() {
	    return state;
	}
	
	public void setState(String state) {
	    this.state = state;
	}
	
	public void setPortName(String name) {
	    this.portName = name;
	}
	
	public String getPortName() {
	    return portName;
	}
}
