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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;

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
				//String[] params = new String[] {"--url", serviceUrl, "--username", vCenterUserName, "--password", vCenterPassword };
				VmwareClient vimClient = new VmwareClient(vCenterAddress + "-" + s_seq++);
				vimClient.connect(serviceUrl, vCenterUserName, vCenterPassword);
				context = new VmwareContext(vimClient, vCenterAddress);
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
