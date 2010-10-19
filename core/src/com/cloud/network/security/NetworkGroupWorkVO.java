/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.security;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="op_nwgrp_work")
public class NetworkGroupWorkVO {
	public enum Step {
        Scheduled,
        Processing,
        Done,
        Error
    }
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    
    @Column(name="instance_id", updatable=false, nullable=false)
    private Long instanceId;    // vm_instance id
    
    
    @Column(name="mgmt_server_id", nullable=true)
    private Long serverId;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    
    @Column(name="step", nullable = false)
    @Enumerated(value=EnumType.STRING)
    private Step step;
    
    @Column(name="taken", nullable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date dateTaken;
    
    @Column(name="seq_no", nullable=true)
    private Long logsequenceNumber = null;

    
    protected NetworkGroupWorkVO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public Long getInstanceId() {
        return instanceId;
    }
    
   
    public Long getServerId() {
        return serverId;
    }
  

    public void setServerId(final Long serverId) {
        this.serverId = serverId;
    }

    public Date getCreated() {
        return created;
    }



	public NetworkGroupWorkVO(Long instanceId, Long serverId, Date created,
			Step step, Date dateTaken) {
		super();
		this.instanceId = instanceId;
		this.serverId = serverId;
		this.created = created;
		this.step = step;
		this.dateTaken = dateTaken;
	}

	@Override
	public String toString() {
    	return new StringBuilder("[NWGrp-Work:id=").append(id).append(":vm=").append(instanceId).append("]").toString();
    }

	public Date getDateTaken() {
		return dateTaken;
	}

	public void setStep(Step step) {
		this.step = step;
	}

	public Step getStep() {
		return step;
	}

	public void setDateTaken(Date date) {
		dateTaken = date;
	}

	public Long getLogsequenceNumber() {
		return logsequenceNumber;
	}

	public void setLogsequenceNumber(Long logsequenceNumber) {
		this.logsequenceNumber = logsequenceNumber;
	}

}
