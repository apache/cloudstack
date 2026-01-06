//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.vmware.mo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareContextPool;
import com.cloud.utils.StringUtils;

public class TestVmwareContextFactory {

    protected static Logger LOGGER = LogManager.getLogger(TestVmwareContextFactory.class);

    private static volatile int s_seq = 1;
    private static VmwareContextPool s_pool;

    static {
        // skip certificate check
        System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
        s_pool = new VmwareContextPool();
    }

    public static VmwareContext create(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
        assert (vCenterAddress != null);
        assert (vCenterUserName != null);
        assert (vCenterPassword != null);

        String serviceUrl = "https://" + vCenterAddress + "/sdk/vimService";

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("initialize VmwareContext. url: " + serviceUrl + ", username: " + vCenterUserName + ", password: " +
                StringUtils.getMaskedPasswordForDisplay(vCenterPassword));

        VmwareClient vimClient = new VmwareClient(vCenterAddress + "-" + s_seq++);
        vimClient.setVcenterSessionTimeout(1200000);
        vimClient.connect(serviceUrl, vCenterUserName, vCenterPassword);

        VmwareContext context = new VmwareContext(vimClient, vCenterAddress);
        return context;
    }

    public static VmwareContext getContext(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
        VmwareContext context = s_pool.getContext(vCenterAddress, vCenterUserName);
        if (context == null)
            context = create(vCenterAddress, vCenterUserName, vCenterPassword);

        if (context != null) {
            context.setPoolInfo(s_pool, VmwareContextPool.composePoolKey(vCenterAddress, vCenterUserName));
        }

        return context;
    }
}
