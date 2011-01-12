package com.cloud.network.ovs.dao;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;

@Entity
@Table(name=("ovs_host_vlan_alloc"))
public class VlanMappingVO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "host_id")
	private long hostId;

	@Column(name = "account_id")
	private long accountId;

	@Column(name = "vlan")
	private long vlan;
	
	@Column(name = "ref")
	int ref;

	public VlanMappingVO(long accountId, long hostId, long vlan) {
		this.hostId = hostId;
		this.accountId = accountId;
		this.vlan = vlan;
		this.ref = 0;
	}

	public VlanMappingVO() {

	}

	public long getHostId() {
		return hostId;
	}

	public long getAccountId() {
		return accountId;
	}
	
	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public long getVlan() {
		return vlan;
	}

	public long getId() {
		return id;
	}
	
	public int getRef() {
		return ref;
	}
	
	public void setRef(int ref) {
		this.ref = ref;
	}
	
	public void ref() {
		ref++;
		setRef(ref);
	}
	
	public int unref() {
		ref--;
		setRef(ref);
		return getRef();
	}
}
