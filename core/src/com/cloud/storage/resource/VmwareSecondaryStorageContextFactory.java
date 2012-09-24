/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.storage.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.apputils.version.ExtendedAppUtil;

public class VmwareSecondaryStorageContextFactory {
	private static volatile int s_seq = 1;
	
	private static Map<String, VmwareContext> s_contextMap = new HashMap<String, VmwareContext>();
	
	public static void initFactoryEnvironment() {
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
	}
	
	public static VmwareContext create(String vCenterAddress, String vCenterUserName, String vCenterPassword) throws Exception {
		assert(vCenterAddress != null);
		assert(vCenterUserName != null);
		assert(vCenterPassword != null);
		
		VmwareContext context = null;
		
		synchronized(s_contextMap) {
			context = s_contextMap.get(vCenterAddress);
			if(context == null) {
				String serviceUrl = "https://" + vCenterAddress + "/sdk/vimService";
				String[] params = new String[] {"--url", serviceUrl, "--username", vCenterUserName, "--password", vCenterPassword };
				ExtendedAppUtil appUtil = ExtendedAppUtil.initialize(vCenterAddress + "-" + s_seq++, params);
				
				appUtil.connect();
				context = new VmwareContext(appUtil, vCenterAddress);
				context.registerStockObject("username", vCenterUserName);
				context.registerStockObject("password", vCenterPassword);
				
				s_contextMap.put(vCenterAddress, context);
			}
		}
		
		assert(context != null);
		return context;
	}
	
	public static void invalidate(VmwareContext context) {
		synchronized(s_contextMap) {
            for(Iterator<Map.Entry<String, VmwareContext>> entryIter = s_contextMap.entrySet().iterator(); entryIter.hasNext();) {
                Map.Entry<String, VmwareContext> entry = entryIter.next();
                if(entry.getValue() == context) {
                    entryIter.remove();
                }
			}
		}
		
		context.close();
	}
}
