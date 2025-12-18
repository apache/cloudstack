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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareContextPool;

public class VmwareSecondaryStorageContextFactory {
    protected static Logger LOGGER = LogManager.getLogger(VmwareSecondaryStorageContextFactory.class);

    private static volatile int s_seq = 1;

    private static VmwareContextPool s_pool;

    public static int s_vCenterSessionTimeout = 1200000; // Timeout in milliseconds

    public static void initFactoryEnvironment() {
        System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
        s_pool = new VmwareContextPool();
    }

    public static VmwareContext create(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
        assert (vCenterAddress != null);
        assert (vCenterUserName != null);
        assert (vCenterPassword != null);

        String serviceUrl = "https://" + vCenterAddress + "/sdk/vimService";
        VmwareClient vimClient = new VmwareClient(vCenterAddress + "-" + s_seq++);
        vimClient.setVcenterSessionTimeout(s_vCenterSessionTimeout);
        vimClient.connect(serviceUrl, vCenterUserName, vCenterPassword);
        VmwareContext context = new VmwareContext(vimClient, vCenterAddress);
        assert (context != null);

        context.setPoolInfo(s_pool, VmwareContextPool.composePoolKey(vCenterAddress, vCenterUserName));

        return context;
    }

    public static VmwareContext getContext(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
        VmwareContext context = s_pool.getContext(vCenterAddress, vCenterUserName);
        if (context == null) {
            context = create(vCenterAddress, vCenterUserName, vCenterPassword);
        } else {
            // Validate current context and verify if vCenter session timeout value of the context matches the timeout value set by Admin
            if (!context.validate() || (context.getVimClient().getVcenterSessionTimeout() != s_vCenterSessionTimeout)) {
                LOGGER.info("Validation of the context faild. dispose and create a new one");
                context.close();
                context = create(vCenterAddress, vCenterUserName, vCenterPassword);
            }
        }

        if (context != null) {
            context.registerStockObject("username", vCenterUserName);
            context.registerStockObject("password", vCenterPassword);
        }

        return context;
    }

    public static void invalidate(VmwareContext context) {
        context.close();
    }

    public static void setVcenterSessionTimeout(int timeout) {
        s_vCenterSessionTimeout = timeout;
    }

}
