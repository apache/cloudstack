package com.cloud.vm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="user_vm_details")
public class UserVmDetailVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="vm_id")
    private long vmId;
    
    @Column(name="name")
    private String name;
    
    @Column(name="value")
    private String value;
    
    public UserVmDetailVO() {}
    
    public UserVmDetailVO(long vmId, String name, String value) {
    	this.vmId = vmId;
    	this.name = name;
    	this.value = value;
    }

	public long getId() {
		return id;
	}

	public long getVmId() {
		return vmId;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setVmId(long vmId) {
		this.vmId = vmId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
