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
@Table(name=("ovs_tunnel_alloc"))
public class GreTunnelVO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "from")
	private long from;

	@Column(name = "to")
	private long to;

	@Column(name = "in_port")
	private int inPort;
	
	public GreTunnelVO() {
		
	}
	
	public GreTunnelVO(long from, long to) {
		this.from = from;
		this.to = to;
		this.inPort = 0;
	}
	
	public GreTunnelVO(long id, long from, long to) {
		this.from = from;
		this.to = to;
		this.inPort = 0;
		this.id = id;
	}
	
	public void setInPort(int port) {
		inPort = port;
	}
	
	public long getFrom() {
		return from;
	}
	
	public long getTo() {
		return to;
	}
	
	public int getInPort() {
		return inPort;
	}
	
	public long getId() {
		return id;
	}
	
}
