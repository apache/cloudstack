package com.cloud.dc;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

@Entity
@Table(name="dc_storage_network_ip_range")
@SecondaryTables({@SecondaryTable(name="networks", pkJoinColumns={@PrimaryKeyJoinColumn(name="network_id", referencedColumnName="id")}),
	@SecondaryTable(name="host_pod_ref", pkJoinColumns={@PrimaryKeyJoinColumn(name="pod_id", referencedColumnName="id")}),
	@SecondaryTable(name="data_center", pkJoinColumns={@PrimaryKeyJoinColumn(name="data_center_id", referencedColumnName="id")})
})
public class StorageNetworkIpRangeVO implements StorageNetworkIpRange {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;
	
	@Column(name = "uuid")
	String uuid;
	
	@Column(name = "vlan")
	private Integer vlan;
	
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
	
	@Column(name="netmask")
	private String netmask;
	
	@Column(name = "uuid", table = "networks", insertable = false, updatable = false)
	String networkUuid;
	
	@Column(name = "uuid", table = "host_pod_ref", insertable = false, updatable = false)
	String podUuid;
	
	@Column(name = "uuid", table = "data_center", insertable = false, updatable = false)
	String zoneUuid;
	
	public StorageNetworkIpRangeVO(long dcId, long podId, long networkId, String startIp, String endIp, Integer vlan, String netmask) {
		this();
		this.dataCenterId = dcId;
		this.podId = podId;
		this.networkId = networkId;
		this.startIp = startIp;
		this.endIp = endIp;
		this.vlan = vlan;
		this.netmask = netmask;
	}
		
	protected StorageNetworkIpRangeVO() {
		this.uuid = UUID.randomUUID().toString();
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
		
	public Integer getVlan() {
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
	
	public String getNetmask() {
		return netmask;
	}
	
	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public String getPodUuid() {
		return podUuid;
	}

	@Override
	public String getNetworkUuid() {
		return networkUuid;
	}

	@Override
	public String getZoneUuid() {
		return zoneUuid;
	}
}
