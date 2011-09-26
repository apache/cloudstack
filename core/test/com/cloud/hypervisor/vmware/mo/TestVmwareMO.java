/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.testcase.Log4jEnabledTestCase;
import com.google.gson.Gson;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachineConfigSpec;

// This test case needs a particular setup, only used for my own test
public class TestVmwareMO extends Log4jEnabledTestCase {
    private static final Logger s_logger = Logger.getLogger(TestVmwareMO.class);

    public void test() {
		try {
			VmwareContext context = TestVmwareContextFactory.create(
					"10.223.80.29", "Administrator", "Suite219");
			
			HostMO hostMo = new HostMO(context, "HostSystem", "host-9");
			
			System.out.println("host Type " + hostMo.getHostType());
			Gson gson = GsonHelper.getGsonLogger();
			System.out.println(gson.toJson(hostMo.getHostFirewallSystemMO().getFirewallInfo()));
			
			context.close();
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
    }
}

