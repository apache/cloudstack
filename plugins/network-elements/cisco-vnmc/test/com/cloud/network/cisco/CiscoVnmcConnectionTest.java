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
package com.cloud.network.cisco;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.cloud.utils.exception.ExecutionException;

@Ignore("Requires actual VNMC to connect to")
public class CiscoVnmcConnectionTest {
    static CiscoVnmcConnectionImpl connection;
    static String tenantName = "TenantE";
    static Map<String, String> fwDns = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        connection = new CiscoVnmcConnectionImpl("10.223.56.5", "admin", "C1sco123");
        try {
            boolean response = connection.login();
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
            boolean response = connection.login();
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
            boolean response = connection.createTenant(tenantName);
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
            boolean response = connection.createTenantVDC(tenantName);
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
            boolean response = connection.createTenantVDCEdgeDeviceProfile(tenantName);
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCEdgeDeviceRoutePolicy() {
        try {
            boolean response = connection.createTenantVDCEdgeStaticRoutePolicy(tenantName);
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCEdgeDeviceRoute() {
        try {
            boolean response = connection.createTenantVDCEdgeStaticRoute(tenantName, "10.223.136.1", "0.0.0.0", "0.0.0.0");
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testAssociateRoutePolicyWithEdgeProfile() {
        try {
            boolean response = connection.associateTenantVDCEdgeStaticRoutePolicy(tenantName);
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testAssociateTenantVDCEdgeDhcpPolicy() {
        try {
            boolean response = connection.associateTenantVDCEdgeDhcpPolicy(tenantName, "Edge_Inside");
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCEdgeDhcpPolicy() {
        try {
            boolean response = connection.createTenantVDCEdgeDhcpPolicy(tenantName, "10.1.1.2", "10.1.1.254", "255.255.255.0", "4.4.4.4", tenantName + ".net");
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCEdgeSecurityProfile() {
        try {
            boolean response = connection.createTenantVDCEdgeSecurityProfile(tenantName);
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCSourceNatIpPool() {
        try {
            boolean response = connection.createTenantVDCSourceNatIpPool(tenantName, "1", "10.223.136.10");
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCSourceNatPolicy() {
        try {
            boolean response = connection.createTenantVDCSourceNatPolicy(tenantName, "1");
            assertTrue(response);
            response = connection.createTenantVDCSourceNatPolicyRef(tenantName, "1");
            assertTrue(response);
            response = connection.createTenantVDCSourceNatRule(tenantName, "1", "10.1.1.2", "10.1.1.254");
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateTenantVDCNatPolicySet() {
        try {
            boolean response = connection.createTenantVDCNatPolicySet(tenantName);
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testAssociateNatPolicySet() {
        try {
            boolean response = connection.associateNatPolicySet(tenantName);
            assertTrue(response);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateEdgeFirewall() {
        try {
            boolean response = connection.createEdgeFirewall(tenantName, "44.44.44.44", "192.168.1.1", "255.255.255.0", "255.255.255.192");
            assertTrue(response);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListUnassocAsa1000v() {
        try {
            Map<String, String> response = connection.listUnAssocAsa1000v();
            assertTrue(response.size() >= 0);
            fwDns = response;
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void assocAsa1000v() {
        try {
            boolean result = connection.assignAsa1000v(tenantName, fwDns.get(0));
            assertTrue(result);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
