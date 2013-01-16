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

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.cloud.network.resource.CiscoVnmcResource;
import com.cloud.utils.exception.ExecutionException;



public class CiscoVnmcResourceTest {
	static CiscoVnmcResource resource;
	static String tenantName = "TenantE";
	static List<String> fwDns = null;
	
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

	@Ignore
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

	@Ignore
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
	
	@Ignore
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
	
	@Ignore
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
	
	@Ignore
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
	
	@Ignore
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
	
	@Ignore
	@Test
	public void testAssociateRoutePolicyWithEdgeProfile() {
		try {
			boolean response = resource.associateTenantVDCEdgeStaticRoutePolicy(tenantName); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testAssociateTenantVDCEdgeDhcpPolicy() {
		try {
			boolean response = resource.associateTenantVDCEdgeDhcpPolicy(tenantName, "Edge_Inside"); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testCreateTenantVDCEdgeDhcpPolicy() {
		try {
			boolean response = resource.createTenantVDCEdgeDhcpPolicy(tenantName, 
					"10.1.1.2", "10.1.1.254", "255.255.255.0","4.4.4.4", tenantName+ ".net"); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testCreateTenantVDCEdgeSecurityProfile() {
		try {
			boolean response = resource.createTenantVDCEdgeSecurityProfile(tenantName); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testCreateTenantVDCSourceNATPool() {
		try {
			boolean response = resource.createTenantVDCSourceNATPool(tenantName, "10.223.136.10"); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testCreateTenantVDCSourceNATPolicy() {
		try {
			boolean response = resource.createTenantVDCSourceNATPolicy(tenantName, "10.1.1.2", "10.1.1.254"); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testCreateTenantVDCNatPolicySet() {
		try {
			boolean response = resource.createTenantVDCNatPolicySet(tenantName); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testAssociateNatPolicySet() {
		try {
			boolean response = resource.associateNatPolicySet(tenantName); 
			assertTrue(response);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	@Test
	public void testCreateEdgeFirewall() {
		try {
			boolean response = resource.createEdgeFirewall(tenantName, 
					"44.44.44.44", "192.168.1.1", "255.255.255.0", "255.255.255.192"); 
			assertTrue(response);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testListUnassocAsa1000v() {
		try {
			List<String> response = resource.listUnAssocAsa1000v(); 
			assertTrue(response.size() >=0);
			fwDns = response;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void assocAsa1000v() {
		try {
			boolean result = resource.assocAsa1000v(tenantName, fwDns.get(0)); 
			assertTrue(result);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
