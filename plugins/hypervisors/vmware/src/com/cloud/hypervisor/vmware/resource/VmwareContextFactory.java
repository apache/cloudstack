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
package com.cloud.hypervisor.vmware.resource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.cluster.ClusterManager;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareContextPool;
import com.cloud.utils.StringUtils;

@Component
public class VmwareContextFactory {
    private static final Logger s_logger = Logger.getLogger(VmwareContextFactory.class);

	private static volatile int s_seq = 1;
	private static VmwareManager s_vmwareMgr;
	private static ClusterManager s_clusterMgr;
	private static VmwareContextPool s_pool;

	@Inject VmwareManager _vmwareMgr;
	@Inject ClusterManager _clusterMgr;

	static {
		// skip certificate check
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
		s_pool = new VmwareContextPool();
	}

	@PostConstruct
	void init() {
	    s_vmwareMgr = _vmwareMgr;
	    s_clusterMgr = _clusterMgr;
	}

	public static VmwareContext create(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
		assert(vCenterAddress != null);
		assert(vCenterUserName != null);
		assert(vCenterPassword != null);

		String serviceUrl = "https://" + vCenterAddress + "/sdk/vimService";
		if(s_logger.isDebugEnabled())
			s_logger.debug("initialize VmwareContext. url: " + serviceUrl + ", username: " + vCenterUserName + ", password: " + StringUtils.getMaskedPasswordForDisplay(vCenterPassword));

		VmwareClient vimClient = new VmwareClient(vCenterAddress + "-" + s_seq++);
		vimClient.connect(serviceUrl, vCenterUserName, vCenterPassword);

		VmwareContext context = new VmwareContext(vimClient, vCenterAddress);
		context.registerStockObject(VmwareManager.CONTEXT_STOCK_NAME, s_vmwareMgr);

		context.registerStockObject("serviceconsole", s_vmwareMgr.getServiceConsolePortGroupName());
		context.registerStockObject("manageportgroup", s_vmwareMgr.getManagementPortGroupName());
		context.registerStockObject("noderuninfo", String.format("%d-%d", s_clusterMgr.getManagementNodeId(), s_clusterMgr.getCurrentRunId()));

		context.setPoolInfo(s_pool, VmwareContextPool.composePoolKey(vCenterAddress, vCenterUserName));
		s_pool.registerOutstandingContext(context);
		
		return context;
	}
	
	public static VmwareContext getContext(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
		VmwareContext context = s_pool.getContext(vCenterAddress, vCenterUserName);
		if(context == null) {
			context = create(vCenterAddress, vCenterUserName, vCenterPassword);
		} else {
			if(!context.validate()) {
				s_logger.info("Validation of the context faild. dispose and create a new one");
				context.close();
				context = create(vCenterAddress, vCenterUserName, vCenterPassword);
			}
		}
		
		if(context != null) {
			context.registerStockObject(VmwareManager.CONTEXT_STOCK_NAME, s_vmwareMgr);

			context.registerStockObject("serviceconsole", s_vmwareMgr.getServiceConsolePortGroupName());
			context.registerStockObject("manageportgroup", s_vmwareMgr.getManagementPortGroupName());
			context.registerStockObject("noderuninfo", String.format("%d-%d", s_clusterMgr.getManagementNodeId(), s_clusterMgr.getCurrentRunId()));
		}
		
		return context;
	}
}
