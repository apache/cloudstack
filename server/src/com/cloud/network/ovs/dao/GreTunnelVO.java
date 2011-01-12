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
