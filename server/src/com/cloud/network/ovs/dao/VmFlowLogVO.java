package com.cloud.network.ovs.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="ovs_vm_flow_log")
public class VmFlowLogVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    
    @Column(name="instance_id", updatable=false, nullable=false)
    private Long instanceId;    // vm_instance id
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name="logsequence")
    long logsequence;
    
    protected VmFlowLogVO() {
    	
    }

	public VmFlowLogVO(Long instanceId) {
		super();
		this.instanceId = instanceId;
	}

	public Long getId() {
		return id;
	}

	public Long getInstanceId() {
		return instanceId;
	}

	public Date getCreated() {
		return created;
	}

	public long getLogsequence() {
		return logsequence;
	}
    
	public void incrLogsequence() {
		logsequence++;
	}
}
