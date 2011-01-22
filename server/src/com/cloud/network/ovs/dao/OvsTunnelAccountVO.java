package com.cloud.network.ovs.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name=("ovs_tunnel_account"))
public class OvsTunnelAccountVO {
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
	
	@Column(name = "account")
	private long account;
	
	@Column(name = "port_name")
    private String portName;
	
	@Column(name = "state")
	private String state;
	
	public OvsTunnelAccountVO() {
		
	}
	
	public OvsTunnelAccountVO(long from, long to, int key, long account) {
		this.from = from;
		this.to = to;
		this.key = key;
		this.account = account;
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
	
	public long getAccount() {
		return account;
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
