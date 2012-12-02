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
package com.cloud.network.resource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.host.Host;
import com.cloud.network.nicira.NiciraNvpApi;

public class NiciraNvpResourceTest {
	NiciraNvpApi _nvpApi = mock(NiciraNvpApi.class);
	NiciraNvpResource _resource;
	
	@Before
	public void setUp() {
		_resource = new NiciraNvpResource() {
			protected NiciraNvpApi createNiciraNvpApi() {
				return _nvpApi;
			}
		};
	}
	
	@Test (expected=ConfigurationException.class)
	public void resourceConfigureFailure() throws ConfigurationException {
		_resource.configure("NiciraNvpResource", Collections.<String,Object>emptyMap());
	}
	
	@Test 
	public void resourceConfigure() throws ConfigurationException {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("name","nvptestdevice");
		parameters.put("ip","127.0.0.1");
		parameters.put("adminuser","adminuser");
		parameters.put("guid", "aaaaa-bbbbb-ccccc");
		parameters.put("zoneId", "blublub");
		parameters.put("adminpass","adminpass");
		_resource.configure("NiciraNvpResource", parameters);
		
		verify(_nvpApi).setAdminCredentials("adminuser", "adminpass");
		verify(_nvpApi).setControllerAddress("127.0.0.1");
		
		assertTrue("nvptestdevice".equals(_resource.getName()));
		
		/* Pretty lame test, but here to assure this plugin fails 
		 * if the type name ever changes from L2Networking
		 */ 
		assertTrue(_resource.getType() == Host.Type.L2Networking);
	}
	
}
