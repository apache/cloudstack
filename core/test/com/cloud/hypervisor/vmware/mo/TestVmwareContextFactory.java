/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.apputils.version.ExtendedAppUtil;

public class TestVmwareContextFactory {
	private static volatile int s_seq = 1;
	
	static {
		// skip certificate check
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
	}

	public static VmwareContext create(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
		assert(vCenterAddress != null);
		assert(vCenterUserName != null);
		assert(vCenterPassword != null);

		String serviceUrl = "https://" + vCenterAddress + "/sdk/vimService";
		String[] params = new String[] {"--url", serviceUrl, "--username", vCenterUserName, "--password", vCenterPassword };
		ExtendedAppUtil appUtil = ExtendedAppUtil.initialize(vCenterAddress + "-" + s_seq++, params);
		
		appUtil.connect();
		VmwareContext context = new VmwareContext(appUtil, vCenterAddress);
		return context;
	}
}
