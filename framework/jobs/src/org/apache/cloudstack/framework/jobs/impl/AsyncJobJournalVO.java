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
package org.apache.cloudstack.framework.jobs.impl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJob.JournalType;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="async_job_journal")
public class AsyncJobJournalVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;

    @Column(name="job_id")
	private long jobId;
    
    @Column(name="journal_type", updatable=false, nullable=false, length=32)
    @Enumerated(value=EnumType.STRING)
    private AsyncJob.JournalType journalType;
    
    @Column(name="journal_text", length=1024)
    private String journalText;

    @Column(name="journal_obj", length=1024)
    private String journalObjJsonString;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    protected Date created;

    public AsyncJobJournalVO() {
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

	public AsyncJob.JournalType getJournalType() {
		return journalType;
	}

	public void setJournalType(AsyncJob.JournalType journalType) {
		this.journalType = journalType;
	}

	public String getJournalText() {
		return journalText;
	}

	public void setJournalText(String journalText) {
		this.journalText = journalText;
	}

	public String getJournalObjJsonString() {
		return journalObjJsonString;
	}

	public void setJournalObjJsonString(String journalObjJsonString) {
		this.journalObjJsonString = journalObjJsonString;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}
}
