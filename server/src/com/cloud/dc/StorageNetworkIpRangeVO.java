package com.cloud.dc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="dc_storage_network_ip_range")
public class StorageNetworkIpRangeVO implements StorageNetworkIpRange {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;
	
	@Column(name = "vlan")
	private int vlan;
	
	@Column(name = "data_center_id")
	private long dataCenterId;
	
	@Column(name = "pod_id")
	private long podId;
	
	@Column(name = "start_ip")
	private String startIp;
	
	@Column(name = "end_ip")
	private String endIp;
	
	@Column(name = "network_id")
	private long networkId;
	
	public StorageNetworkIpRangeVO(long dcId, long podId, long networkId, String startIp, String endIp, int vlan) {
		this.dataCenterId = dcId;
		this.podId = podId;
		this.networkId = networkId;
		this.startIp = startIp;
		this.endIp = endIp;
		this.vlan = vlan;
	}
		
	protected StorageNetworkIpRangeVO() {
		
	}
	
	public long getId() {
		return id;
	}
	
	public long getDataCenterId() {
		return dataCenterId;
	}
	
	public void setDataCenterId(long dcId) {
		this.dataCenterId = dcId;
	}
	
	public long getPodId() {
		return podId;
	}
	
	public void setPodId(long podId) {
		this.podId = podId;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void setNetworkId(long nwId) {
		this.networkId = nwId;
	}
		
	public int getVlan() {
		return vlan;
	}
	
	public void setVlan(int vlan) {
		this.vlan = vlan;
	}
	
	public void setStartIp(String start) {
		this.startIp = start;
	}
	
	public String getStartIp() {
		return startIp;
	}
	
	public void setEndIp(String end) {
		this.endIp = end;
	}
	
	public String getEndIp() {
		return endIp;
	}
}
