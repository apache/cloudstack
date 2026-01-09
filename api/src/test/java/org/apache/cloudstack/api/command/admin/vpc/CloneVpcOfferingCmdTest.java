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

package org.apache.cloudstack.api.command.admin.vpc;

import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcProvisioningService;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloneVpcOfferingCmdTest {

    private CloneVPCOfferingCmd cloneVpcOfferingCmd;

    @Mock
    private VpcProvisioningService vpcService;

    @Mock
    private ResponseGenerator responseGenerator;

    @Mock
    private VpcOffering mockVpcOffering;

    @Mock
    private VpcOfferingResponse mockVpcOfferingResponse;

    @Before
    public void setUp() {
        cloneVpcOfferingCmd = new CloneVPCOfferingCmd();
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "_vpcProvSvc", vpcService);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "_responseGenerator", responseGenerator);
    }

    @Test
    public void testGetSourceOfferingId() {
        Long sourceOfferingId = 789L;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "sourceOfferingId", sourceOfferingId);
        assertEquals(sourceOfferingId, cloneVpcOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testGetName() {
        String name = "ClonedVpcOffering";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "vpcOfferingName", name);
        assertEquals(name, cloneVpcOfferingCmd.getVpcOfferingName());
    }

    @Test
    public void testGetDisplayText() {
        String displayText = "Cloned VPC Offering Display Text";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "displayText", displayText);
        assertEquals(displayText, cloneVpcOfferingCmd.getDisplayText());
    }

    @Test
    public void testGetDisplayTextDefaultsToName() {
        String name = "ClonedVpcOffering";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "vpcOfferingName", name);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "displayText", null);
        assertEquals(name, cloneVpcOfferingCmd.getDisplayText());
    }

    @Test
    public void testGetServiceOfferingId() {
        Long serviceOfferingId = 456L;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "serviceOfferingId", serviceOfferingId);
        assertEquals(serviceOfferingId, cloneVpcOfferingCmd.getServiceOfferingId());
    }

    @Test
    public void testGetInternetProtocol() {
        String internetProtocol = "dualstack";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "internetProtocol", internetProtocol);
        assertEquals(internetProtocol, cloneVpcOfferingCmd.getInternetProtocol());
    }

    @Test
    public void testGetProvider() {
        String provider = "NSX";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "provider", provider);
        assertEquals(provider, cloneVpcOfferingCmd.getProvider());
    }

    @Test
    public void testGetNetworkMode() {
        String networkMode = "ROUTED";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "networkMode", networkMode);
        assertEquals(networkMode, cloneVpcOfferingCmd.getNetworkMode());
    }

    @Test
    public void testGetRoutingMode() {
        String routingMode = "dynamic";
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "routingMode", routingMode);
        assertEquals(routingMode, cloneVpcOfferingCmd.getRoutingMode());
    }

    @Test
    public void testGetNsxSupportLb() {
        Boolean nsxSupportLb = true;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "nsxSupportsLbService", nsxSupportLb);
        assertEquals(nsxSupportLb, cloneVpcOfferingCmd.getNsxSupportsLbService());
    }

    @Test
    public void testGetSpecifyAsnumber() {
        Boolean specifyAsnumber = false;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "specifyAsNumber", specifyAsnumber);
        assertEquals(specifyAsnumber, cloneVpcOfferingCmd.getSpecifyAsNumber());
    }

    @Test
    public void testExecuteSuccess() {
        Long sourceOfferingId = 789L;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(vpcService.cloneVPCOffering(any(CloneVPCOfferingCmd.class))).thenReturn(mockVpcOffering);
        when(responseGenerator.createVpcOfferingResponse(mockVpcOffering)).thenReturn(mockVpcOfferingResponse);

        cloneVpcOfferingCmd.execute();

        assertNotNull(cloneVpcOfferingCmd.getResponseObject());
        assertEquals(mockVpcOfferingResponse, cloneVpcOfferingCmd.getResponseObject());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteFailure() {
        Long sourceOfferingId = 789L;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(vpcService.cloneVPCOffering(any(CloneVPCOfferingCmd.class))).thenReturn(null);

        try {
            cloneVpcOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to clone VPC offering", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetSupportedServices() {
        List<String> supportedServices = Arrays.asList("Dhcp", "Dns", "SourceNat", "NetworkACL");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "supportedServices", supportedServices);
        assertEquals(supportedServices, cloneVpcOfferingCmd.getSupportedServices());
    }

    @Test
    public void testGetServiceProviders() {
        Map<String, HashMap<String, String>> serviceProviderList = new HashMap<>();

        HashMap<String, String> dhcpProvider = new HashMap<>();
        dhcpProvider.put("service", "Dhcp");
        dhcpProvider.put("provider", "VpcVirtualRouter");

        HashMap<String, String> dnsProvider = new HashMap<>();
        dnsProvider.put("service", "Dns");
        dnsProvider.put("provider", "VpcVirtualRouter");

        HashMap<String, String> aclProvider = new HashMap<>();
        aclProvider.put("service", "NetworkACL");
        aclProvider.put("provider", "VpcVirtualRouter");

        serviceProviderList.put("0", dhcpProvider);
        serviceProviderList.put("1", dnsProvider);
        serviceProviderList.put("2", aclProvider);

        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "serviceProviderList", serviceProviderList);

        Map<String, List<String>> result = cloneVpcOfferingCmd.getServiceProviders();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertNotNull(result.get("Dhcp"));
        assertNotNull(result.get("Dns"));
        assertNotNull(result.get("NetworkACL"));
        assertEquals("VpcVirtualRouter", result.get("Dhcp").get(0));
        assertEquals("VpcVirtualRouter", result.get("Dns").get(0));
        assertEquals("VpcVirtualRouter", result.get("NetworkACL").get(0));
    }

    @Test
    public void testGetEnable() {
        Boolean enable = true;
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "enable", enable);
        assertEquals(enable, cloneVpcOfferingCmd.getEnable());
    }

    @Test
    public void testCloneWithAllParameters() {
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "sourceOfferingId", 789L);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "vpcOfferingName", "ClonedVpcOffering");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "displayText", "Cloned VPC Offering");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "serviceOfferingId", 456L);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "internetProtocol", "ipv4");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "provider", "NSX");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "networkMode", "NATTED");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "routingMode", "static");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "nsxSupportsLbService", true);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "specifyAsNumber", false);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "enable", true);

        assertEquals(Long.valueOf(789L), cloneVpcOfferingCmd.getSourceOfferingId());
        assertEquals("ClonedVpcOffering", cloneVpcOfferingCmd.getVpcOfferingName());
        assertEquals("Cloned VPC Offering", cloneVpcOfferingCmd.getDisplayText());
        assertEquals(Long.valueOf(456L), cloneVpcOfferingCmd.getServiceOfferingId());
        assertEquals("ipv4", cloneVpcOfferingCmd.getInternetProtocol());
        assertEquals("NSX", cloneVpcOfferingCmd.getProvider());
        assertEquals("NATTED", cloneVpcOfferingCmd.getNetworkMode());
        assertEquals("static", cloneVpcOfferingCmd.getRoutingMode());
        assertEquals(Boolean.TRUE, cloneVpcOfferingCmd.getNsxSupportsLbService());
        assertEquals(Boolean.FALSE, cloneVpcOfferingCmd.getSpecifyAsNumber());
        assertEquals(Boolean.TRUE, cloneVpcOfferingCmd.getEnable());
    }

    @Test
    public void testSourceOfferingIdNullByDefault() {
        assertNull(cloneVpcOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testProviderNullByDefault() {
        assertNull(cloneVpcOfferingCmd.getProvider());
    }

    @Test
    public void testServiceCapabilityList() {
        Map<String, List<String>> serviceCapabilityList = new HashMap<>();
        serviceCapabilityList.put("Connectivity", Arrays.asList("RegionLevelVpc:true", "DistributedRouter:true"));
        serviceCapabilityList.put("SourceNat", Arrays.asList("RedundantRouter:true"));
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "serviceCapabilityList", serviceCapabilityList);

        Map<String, List<String>> result = cloneVpcOfferingCmd.getServiceCapabilityList();
        assertNotNull(result);
        assertEquals(serviceCapabilityList, result);
    }

    @Test
    public void testCloneVpcOfferingWithNsxProvider() {
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "sourceOfferingId", 789L);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "provider", "NSX");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "nsxSupportsLbService", true);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "networkMode", "ROUTED");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "routingMode", "dynamic");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "specifyAsNumber", true);

        assertEquals("NSX", cloneVpcOfferingCmd.getProvider());
        assertEquals(Boolean.TRUE, cloneVpcOfferingCmd.getNsxSupportsLbService());
        assertEquals("ROUTED", cloneVpcOfferingCmd.getNetworkMode());
        assertEquals("dynamic", cloneVpcOfferingCmd.getRoutingMode());
        assertEquals(Boolean.TRUE, cloneVpcOfferingCmd.getSpecifyAsNumber());
    }

    @Test
    public void testCloneVpcOfferingWithNetrisProvider() {
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "sourceOfferingId", 789L);
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "provider", "Netris");
        ReflectionTestUtils.setField(cloneVpcOfferingCmd, "networkMode", "NATTED");

        assertEquals("Netris", cloneVpcOfferingCmd.getProvider());
        assertEquals("NATTED", cloneVpcOfferingCmd.getNetworkMode());
    }
}

