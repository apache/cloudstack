package com.cloud.network.ovs.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name=("ovs_tunnel"))
public class OvsTunnelVO {
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
	
	
	public OvsTunnelVO() {
		
	}
	
	public OvsTunnelVO(long from, long to) {
		this.from = from;
		this.to = to;
		this.key = 0;
	}
	
	public OvsTunnelVO(long id, long from, long to) {
		this.from = from;
		this.to = to;
		this.key = 0;
		this.id = id;
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
}
