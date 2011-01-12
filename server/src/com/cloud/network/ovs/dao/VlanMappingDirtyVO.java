package com.cloud.network.ovs.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name=("ovs_vlan_mapping_dirty"))
public class VlanMappingDirtyVO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "dirty")
	private boolean dirty;

	@Column(name = "account_id")
	private long accountId;
	
	public VlanMappingDirtyVO() {
		
	}
	
	public VlanMappingDirtyVO(long accountId, boolean dirty) {
		this.accountId = accountId;
		this.dirty = dirty;
	}
	
	public long getId() {
		return id;
	}
	
	public long getAccountId() {
		return accountId;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public void markDirty() {
		setDirty(true);
	}
	public void clean() {
		setDirty(false);
	}
}
