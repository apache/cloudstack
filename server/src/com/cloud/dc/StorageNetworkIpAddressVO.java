package com.cloud.dc;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.network.IpAddress.State;

@Entity
@Table(name="op_dc_storage_network_ip_address")
public class StorageNetworkIpAddressVO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	long id;

	@Column(name = "range_id")
	long rangeId;
		
	@Column(name = "cidr_size")
	int cidrSize;
	
	@Column(name = "ip_address", updatable = false, nullable = false)
	String ipAddress;

	@Column(name = "taken")
	@Temporal(value = TemporalType.TIMESTAMP)
	private Date takenAt;
	
	@Column(name = "mac_address")
	long mac;

	protected StorageNetworkIpAddressVO() {
	}

	public Long getId() {
		return id;
	}

	public void setTakenAt(Date takenDate) {
		this.takenAt = takenDate;
	}

	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ip) {
		this.ipAddress = ip;
	}

	public Date getTakenAt() {
		return takenAt;
	}
	
	public long getRangeId() {
		return rangeId;
	}
	
	public void setRangeId(long id) {
		this.rangeId = id;
	}
	
	public long getMac() {
		return mac;
	}
	
	public void setMac(long mac) {
		this.mac = mac;
	}
	
	public int getCidrSize() {
		return cidrSize;
	}
	
	public void setCidrSize(int cidr) {
		this.cidrSize =cidr;
	}
}
