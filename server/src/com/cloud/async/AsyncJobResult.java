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

import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.api.ApiSerializerHelper;

public class AsyncJobResult {
	
	private long jobId;
    private JobInfo.Status jobStatus;
	private int processStatus;
	private int resultCode;
	private String result;
	private String uuid;

	public AsyncJobResult(long jobId) {
		this.jobId = jobId;
		jobStatus = JobInfo.Status.IN_PROGRESS;
		processStatus = 0;
		resultCode = 0;
		result = "";
	}
	
	public long getJobId() {
		return jobId;
	}
	
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
    public JobInfo.Status getJobStatus() {
		return jobStatus;
	}
	
    public void setJobStatus(JobInfo.Status jobStatus) {
		this.jobStatus = jobStatus;
	}
	
	public int getProcessStatus() {
		return processStatus;
	}
	
	public void setProcessStatus(int processStatus) {
		this.processStatus = processStatus;
	}
	
	public int getResultCode() {
		return resultCode;
	}
	
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	
	public String getResult() {
		return result;
	}
	
	public void setResult(String result) {
		this.result = result;
	}
	
	public Object getResultObject() {
		return ApiSerializerHelper.fromSerializedString(result);
	}
	
	public void setResultObject(Object result) {
		this.result = ApiSerializerHelper.toSerializedString(result);
	}
	
	@Override
    public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("AsyncJobResult {jobId:").append(getJobId());
        sb.append(", jobStatus: ").append(getJobStatus().ordinal());
		sb.append(", processStatus: ").append(getProcessStatus());
		sb.append(", resultCode: ").append(getResultCode());
		sb.append(", result: ").append(result);
		sb.append("}");
		return sb.toString();
	}
}
