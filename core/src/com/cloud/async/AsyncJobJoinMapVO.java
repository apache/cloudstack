// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.async;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="async_job_join_map")
public class AsyncJobJoinMapVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;

    @Column(name="job_id")
	private long jobId;
    
    @Column(name="join_job_id")
	private long joinJobId;

    @Column(name="join_status")
    private int joinStatus;
    
    @Column(name="join_result", length=1024)
    private String joinResult;

    @Column(name="join_msid")
    private long joinMsid;

    @Column(name="complete_msid")
    private Long completeMsid;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name="last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    public AsyncJobJoinMapVO() {
    	created = DateUtil.currentGMTTime();
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public long getJoinJobId() {
		return joinJobId;
	}

	public void setJoinJobId(long joinJobId) {
		this.joinJobId = joinJobId;
	}

	public int getJoinStatus() {
		return joinStatus;
	}

	public void setJoinStatus(int joinStatus) {
		this.joinStatus = joinStatus;
	}

	public String getJoinResult() {
		return joinResult;
	}

	public void setJoinResult(String joinResult) {
		this.joinResult = joinResult;
	}

	public long getJoinMsid() {
		return joinMsid;
	}

	public void setJoinMsid(long joinMsid) {
		this.joinMsid = joinMsid;
	}

	public Long getCompleteMsid() {
		return completeMsid;
	}

	public void setCompleteMsid(Long completeMsid) {
		this.completeMsid = completeMsid;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
