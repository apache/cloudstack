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

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.google.gson.Gson;

public class VmWorkTest extends TestCase {
	Gson gson = new Gson();
	
	@Test
	public void testDeployPlanSerialization() {
		DeploymentPlan plan = new DataCenterDeployment(1L);
		ExcludeList excludeList = new ExcludeList();
		
		excludeList.addCluster(1);
		plan.setAvoids(excludeList);
		
		String json = gson.toJson(plan);
		DeploymentPlan planClone = gson.fromJson(json, DataCenterDeployment.class);
		Assert.assertTrue(planClone.getDataCenterId() == plan.getDataCenterId());
	}
	
	@Test
	public void testVmWorkStart() {
		VmWorkStart work = new VmWorkStart();
		Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>();
		params.put(VirtualMachineProfile.Param.HaTag, "HA");
		params.put(VirtualMachineProfile.Param.ControlNic, new Long(100));
		work.setParams(params);
		
		VmWorkStart workClone = gson.fromJson(gson.toJson(work), VmWorkStart.class);
		Assert.assertTrue(work.getParams().size() == workClone.getParams().size());
		Assert.assertTrue(work.getParams().get(VirtualMachineProfile.Param.HaTag).equals(workClone.getParams().get(VirtualMachineProfile.Param.HaTag)));
		
	}
}
