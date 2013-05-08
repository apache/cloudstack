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
import java.util.TimeZone;

import javax.management.StandardMBean;

import com.cloud.utils.DateUtil;

public class AsyncJobMBeanImpl extends StandardMBean implements AsyncJobMBean {
	private AsyncJob _job;
	
	public AsyncJobMBeanImpl(AsyncJob job) {
		super(AsyncJobMBean.class, false);
		
		_job = job;
	}
	
	public long getAccountId() {
		return _job.getAccountId();
	}
	
	public long getUserId() {
		return _job.getUserId();
	}
	
	public String getCmd() {
		return _job.getCmd();
	}
	
	public String getCmdInfo() {
		return _job.getCmdInfo();
	}
	
	public String getStatus() {
		int jobStatus = _job.getStatus();
		switch(jobStatus) {
		case AsyncJobConstants.STATUS_SUCCEEDED :
			return "Completed";
		
		case AsyncJobConstants.STATUS_IN_PROGRESS:
			return "In preogress";
			
		case AsyncJobConstants.STATUS_FAILED:
			return "failed";
		}
		
		return "Unknow";
	}
	
	public int getProcessStatus() {
		return _job.getProcessStatus();
	}
	
	public int getResultCode() {
		return _job.getResultCode();
	}
	
	public String getResult() {
		return _job.getResult();
	}
	
	public String getInstanceType() {
		if(_job.getInstanceType() != null)
			return _job.getInstanceType().toString();
		return "N/A";
	}
	
	public String getInstanceId() {
		if(_job.getInstanceId() != null)
			return String.valueOf(_job.getInstanceId());
		return "N/A";
	}
	
	public String getInitMsid() {
		if(_job.getInitMsid() != null) {
			return String.valueOf(_job.getInitMsid());
		}
		return "N/A";
	}
	
	public String getCreateTime() {
		Date time = _job.getCreated();
		if(time != null)
			return DateUtil.getDateDisplayString(TimeZone.getDefault(), time);
		return "N/A";
	}
	
	public String getLastUpdateTime() {
		Date time = _job.getLastUpdated();
		if(time != null)
			return DateUtil.getDateDisplayString(TimeZone.getDefault(), time);
		return "N/A";
	}
	
	public String getLastPollTime() {
		Date time = _job.getLastPolled();
	
		if(time != null)
			return DateUtil.getDateDisplayString(TimeZone.getDefault(), time);
		return "N/A";
	}
	
	public String getSyncQueueId() {
		SyncQueueItem item = _job.getSyncSource();
		if(item != null && item.getQueueId() != null) {
			return String.valueOf(item.getQueueId());
		}
		return "N/A";
	}
	
	public String getSyncQueueContentType() {
		SyncQueueItem item = _job.getSyncSource();
		if(item != null) {
			return item.getContentType();
		}
		return "N/A";
	}
	
	public String getSyncQueueContentId() {
		SyncQueueItem item = _job.getSyncSource();
		if(item != null && item.getContentId() != null) {
			return String.valueOf(item.getContentId());
		}
		return "N/A";
	}
}
