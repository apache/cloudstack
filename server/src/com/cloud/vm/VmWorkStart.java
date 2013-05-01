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

package com.cloud.vm;

import java.util.HashMap;
import java.util.Map;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.deploy.DeploymentPlan;

public class VmWorkStart extends VmWork {
	private DeploymentPlan plan;
	
	// use serialization friendly map
	private Map<String, String> rawParams;

	public VmWorkStart() {
	}

	public DeploymentPlan getPlan() {
		return plan;
	}

	public void setPlan(DeploymentPlan plan) {
		this.plan = plan;
	}

	public Map<String, String> getRawParams() {
		return rawParams;
	}

	public void setRawParams(Map<String, String> params) {
		this.rawParams = params;
	}
	
	public Map<VirtualMachineProfile.Param, Object> getParams() {
		Map<VirtualMachineProfile.Param, Object> map = new HashMap<VirtualMachineProfile.Param, Object>();
		
		if(rawParams != null) {
			// Strong-typing for VirtualMachineProfile.Param is really over-kill, have to deal with it anyway
			for(Map.Entry<String, String> entry : rawParams.entrySet()) {
				VirtualMachineProfile.Param key = new VirtualMachineProfile.Param(entry.getKey());
				Object val = ApiSerializerHelper.fromSerializedString(entry.getValue());
				map.put(key, val);
			}
		}
		
		return map;
	}
	
	public void setParams( Map<VirtualMachineProfile.Param, Object> params) {
		if(params != null) {
			rawParams = new HashMap<String, String>();
			for(Map.Entry<VirtualMachineProfile.Param, Object> entry : params.entrySet()) {
				rawParams.put(entry.getKey().getName(), ApiSerializerHelper.toSerializedString(entry.getValue()));
			}
		}
	}
}
