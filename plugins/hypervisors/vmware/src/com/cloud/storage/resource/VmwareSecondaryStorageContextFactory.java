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
package com.cloud.storage.resource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareContextPool;
import com.cloud.utils.NumbersUtil;

public class VmwareSecondaryStorageContextFactory {
    private static final Logger s_logger = Logger.getLogger(VmwareSecondaryStorageContextFactory.class);
    
	private static volatile int s_seq = 1;

	private static VmwareContextPool s_pool;
    private static ConfigurationDao s_configDao;

    @Inject ConfigurationDao _configDao;

	public static void initFactoryEnvironment() {
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
		s_pool = new VmwareContextPool();
	}

    @PostConstruct
        void init() {
            s_configDao = _configDao;
    }

	public static VmwareContext create(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
		assert(vCenterAddress != null);
		assert(vCenterUserName != null);
		assert(vCenterPassword != null);

		String serviceUrl = "https://" + vCenterAddress + "/sdk/vimService";
        int vCenterSessionTimeout = NumbersUtil.parseInt(s_configDao.getValue(Config.VmwareVcenterSessionTimeout.key()), 600) * 1000;
		VmwareClient vimClient = new VmwareClient(vCenterAddress + "-" + s_seq++);
        vimClient.setVcenterSessionTimeout(vCenterSessionTimeout);
        vimClient.connect(serviceUrl, vCenterUserName, vCenterPassword);
		VmwareContext context = new VmwareContext(vimClient, vCenterAddress);
		assert(context != null);
		
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
			context.registerStockObject("username", vCenterUserName);
			context.registerStockObject("password", vCenterPassword);
		}
		
		return context;
	}
	
	public static void invalidate(VmwareContext context) {
		context.close();
	}
}
