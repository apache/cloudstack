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
package com.cloud.network.resource.test;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.cloud.network.resource.CiscoVnmcResource;
import com.cloud.utils.exception.ExecutionException;



public class CiscoVnmcResourceTest {
	static CiscoVnmcResource resource;
	static String tenantName = "TenantB";
	@BeforeClass
	public static void setUpClass() throws Exception {
		resource = new CiscoVnmcResource("10.223.56.5", "admin", "C1sco123");
		try {
			boolean response = resource.login();
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testLogin() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.login();
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testCreateTenant() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.createTenant(tenantName);
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCreateTenantVDC() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.createTenantVDC(tenantName);
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testCreateTenantVDCEdgeDeviceProfile() {
		//fail("Not yet implemented");
		try {
			boolean response = resource.createTenantVDCEdgeDeviceProfile(tenantName);
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCreateTenantVDCEdgeDeviceRoutePolicy() {
		try {
			boolean response = resource.createTenantVDCEdgeStaticRoutePolicy(tenantName);
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCreateTenantVDCEdgeDeviceRoute() {
		try {
			boolean response = resource.createTenantVDCEdgeStaticRoute(tenantName, 
					"10.223.136.1", "Edge_Outside", "0.0.0.0", "0.0.0.0");
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
