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
package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackInfoResponse {
	@SerializedName(ApiConstants.SUCCESS)
	private Boolean success;
	 
	@SerializedName(ApiConstants.DISPLAY_TEXT)
	private String displayText;
	
	@SerializedName(ApiConstants.STATUS)
	private Boolean status;
	
    @SerializedName(ApiConstants.JOB_ID)
    private Long jobId;
    
    @SerializedName(ApiConstants.ID)
    private Long id;

	public CloudStackInfoResponse() {
    }

    public Boolean getSuccess() {
		return success;
    }

	public String getDisplayText() {
		return displayText;
	}

	public Boolean getStatus() {
		return status;
	}
	
    public Long getJobId() {
		return jobId;
	}
    
    public Long getId() {
    	return id;
    }
}
