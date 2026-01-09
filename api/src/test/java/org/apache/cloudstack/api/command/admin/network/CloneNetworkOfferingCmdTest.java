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

package org.apache.cloudstack.api.command.admin.network;

import com.cloud.offering.NetworkOffering;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
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
public class CloneNetworkOfferingCmdTest {

    private CloneNetworkOfferingCmd cloneNetworkOfferingCmd;

    @Mock
    private com.cloud.configuration.ConfigurationService configService;

    @Mock
    private ResponseGenerator responseGenerator;

    @Mock
    private NetworkOffering mockNetworkOffering;

    @Mock
    private NetworkOfferingResponse mockNetworkOfferingResponse;

    @Before
    public void setUp() {
        cloneNetworkOfferingCmd = new CloneNetworkOfferingCmd();
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "_configService", configService);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "_responseGenerator", responseGenerator);
    }

    @Test
    public void testGetSourceOfferingId() {
        Long sourceOfferingId = 123L;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "sourceOfferingId", sourceOfferingId);
        assertEquals(sourceOfferingId, cloneNetworkOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testGetAddServices() {
        List<String> addServices = Arrays.asList("Dhcp", "Dns");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "addServices", addServices);
        assertEquals(addServices, cloneNetworkOfferingCmd.getAddServices());
    }

    @Test
    public void testGetDropServices() {
        List<String> dropServices = Arrays.asList("Firewall", "Vpn");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "dropServices", dropServices);
        assertEquals(dropServices, cloneNetworkOfferingCmd.getDropServices());
    }

    @Test
    public void testGetGuestIpType() {
        String guestIpType = "Isolated";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "guestIptype", guestIpType);
        assertEquals(guestIpType, cloneNetworkOfferingCmd.getGuestIpType());
    }

    @Test
    public void testGetTraffictype() {
        String trafficType = "GUEST";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "traffictype", trafficType);
        assertEquals(trafficType, cloneNetworkOfferingCmd.getTraffictype());
    }

    @Test
    public void testGetName() {
        String name = "ClonedNetworkOffering";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "networkOfferingName", name);
        assertEquals(name, cloneNetworkOfferingCmd.getNetworkOfferingName());
    }

    @Test
    public void testGetDisplayText() {
        String displayText = "Cloned Network Offering Display Text";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "displayText", displayText);
        assertEquals(displayText, cloneNetworkOfferingCmd.getDisplayText());
    }

    @Test
    public void testGetDisplayTextDefaultsToName() {
        String name = "ClonedNetworkOffering";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "networkOfferingName", name);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "displayText", null);
        assertEquals(name, cloneNetworkOfferingCmd.getDisplayText());
    }

    @Test
    public void testGetAvailability() {
        String availability = "Required";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "availability", availability);
        assertEquals(availability, cloneNetworkOfferingCmd.getAvailability());
    }

    @Test
    public void testGetTags() {
        String tags = "tag1,tag2,tag3";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "tags", tags);
        assertEquals(tags, cloneNetworkOfferingCmd.getTags());
    }

    @Test
    public void testExecuteSuccess() {
        Long sourceOfferingId = 123L;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneNetworkOffering(any(CloneNetworkOfferingCmd.class))).thenReturn(mockNetworkOffering);
        when(responseGenerator.createNetworkOfferingResponse(mockNetworkOffering)).thenReturn(mockNetworkOfferingResponse);

        cloneNetworkOfferingCmd.execute();

        assertNotNull(cloneNetworkOfferingCmd.getResponseObject());
        assertEquals(mockNetworkOfferingResponse, cloneNetworkOfferingCmd.getResponseObject());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteFailure() {
        Long sourceOfferingId = 123L;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneNetworkOffering(any(CloneNetworkOfferingCmd.class))).thenReturn(null);

        try {
            cloneNetworkOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to clone network offering", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetConserveMode() {
        Boolean conserveMode = true;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "conserveMode", conserveMode);
        assertEquals(conserveMode, cloneNetworkOfferingCmd.getConserveMode());
    }

    @Test
    public void testGetSpecifyVlan() {
        Boolean specifyVlan = false;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "specifyVlan", specifyVlan);
        assertEquals(specifyVlan, cloneNetworkOfferingCmd.getSpecifyVlan());
    }

    @Test
    public void testGetSpecifyIpRanges() {
        Boolean specifyIpRanges = true;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "specifyIpRanges", specifyIpRanges);
        assertEquals(specifyIpRanges, cloneNetworkOfferingCmd.getSpecifyIpRanges());
    }

    @Test
    public void testGetIsPersistent() {
        Boolean isPersistent = true;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "isPersistent", isPersistent);
        assertEquals(isPersistent, cloneNetworkOfferingCmd.getIsPersistent());
    }

    @Test
    public void testGetEgressDefaultPolicy() {
        Boolean egressDefaultPolicy = false;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "egressDefaultPolicy", egressDefaultPolicy);
        assertEquals(egressDefaultPolicy, cloneNetworkOfferingCmd.getEgressDefaultPolicy());
    }

    @Test
    public void testGetServiceOfferingId() {
        Long serviceOfferingId = 456L;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "serviceOfferingId", serviceOfferingId);
        assertEquals(serviceOfferingId, cloneNetworkOfferingCmd.getServiceOfferingId());
    }

    @Test
    public void testGetForVpc() {
        Boolean forVpc = true;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "forVpc", forVpc);
        assertEquals(forVpc, cloneNetworkOfferingCmd.getForVpc());
    }

    @Test
    public void testGetMaxConnections() {
        Integer maxConnections = 1000;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "maxConnections", maxConnections);
        assertEquals(maxConnections, cloneNetworkOfferingCmd.getMaxconnections());
    }

    @Test
    public void testGetNetworkRate() {
        Integer networkRate = 200;
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "networkRate", networkRate);
        assertEquals(networkRate, cloneNetworkOfferingCmd.getNetworkRate());
    }

    @Test
    public void testGetInternetProtocol() {
        String internetProtocol = "ipv4";
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "internetProtocol", internetProtocol);
        assertEquals(internetProtocol, cloneNetworkOfferingCmd.getInternetProtocol());
    }

    @Test
    public void testAddServicesNullByDefault() {
        assertNull(cloneNetworkOfferingCmd.getAddServices());
    }

    @Test
    public void testDropServicesNullByDefault() {
        assertNull(cloneNetworkOfferingCmd.getDropServices());
    }

    @Test
    public void testSupportedServicesParameter() {
        List<String> supportedServices = Arrays.asList("Dhcp", "Dns", "SourceNat");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "supportedServices", supportedServices);
        assertEquals(supportedServices, cloneNetworkOfferingCmd.getSupportedServices());
    }

    @Test
    public void testServiceProviderListParameter() {
        Map<String, HashMap<String, String>> serviceProviderList = new HashMap<>();

        HashMap<String, String> dhcpProvider = new HashMap<>();
        dhcpProvider.put("service", "Dhcp");
        dhcpProvider.put("provider", "VirtualRouter");

        HashMap<String, String> dnsProvider = new HashMap<>();
        dnsProvider.put("service", "Dns");
        dnsProvider.put("provider", "VirtualRouter");

        serviceProviderList.put("0", dhcpProvider);
        serviceProviderList.put("1", dnsProvider);

        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "serviceProviderList", serviceProviderList);

        Map<String, List<String>> result = cloneNetworkOfferingCmd.getServiceProviders();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get("Dhcp"));
        assertNotNull(result.get("Dns"));
        assertEquals("VirtualRouter", result.get("Dhcp").get(0));
        assertEquals("VirtualRouter", result.get("Dns").get(0));
    }

    @Test
    public void testCloneWithAllParameters() {
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "sourceOfferingId", 123L);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "networkOfferingName", "ClonedOffering");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "displayText", "Cloned Offering Display");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "availability", "Optional");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "guestIptype", "Isolated");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "traffictype", "GUEST");
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "conserveMode", true);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "specifyVlan", false);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "isPersistent", true);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "egressDefaultPolicy", false);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "networkRate", 200);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "serviceOfferingId", 456L);

        assertEquals(Long.valueOf(123L), cloneNetworkOfferingCmd.getSourceOfferingId());
        assertEquals("ClonedOffering", cloneNetworkOfferingCmd.getNetworkOfferingName());
        assertEquals("Cloned Offering Display", cloneNetworkOfferingCmd.getDisplayText());
        assertEquals("Optional", cloneNetworkOfferingCmd.getAvailability());
        assertEquals("Isolated", cloneNetworkOfferingCmd.getGuestIpType());
        assertEquals("GUEST", cloneNetworkOfferingCmd.getTraffictype());
        assertEquals(Boolean.TRUE, cloneNetworkOfferingCmd.getConserveMode());
        assertEquals(Boolean.FALSE, cloneNetworkOfferingCmd.getSpecifyVlan());
        assertEquals(Boolean.TRUE, cloneNetworkOfferingCmd.getIsPersistent());
        assertEquals(Boolean.FALSE, cloneNetworkOfferingCmd.getEgressDefaultPolicy());
        assertEquals(Integer.valueOf(200), cloneNetworkOfferingCmd.getNetworkRate());
        assertEquals(Long.valueOf(456L), cloneNetworkOfferingCmd.getServiceOfferingId());
    }

    @Test
    public void testCloneWithAddAndDropServices() {
        List<String> addServices = Arrays.asList("StaticNat", "PortForwarding");
        List<String> dropServices = Arrays.asList("Vpn");

        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "sourceOfferingId", 123L);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "addServices", addServices);
        ReflectionTestUtils.setField(cloneNetworkOfferingCmd, "dropServices", dropServices);

        assertEquals(addServices, cloneNetworkOfferingCmd.getAddServices());
        assertEquals(dropServices, cloneNetworkOfferingCmd.getDropServices());
    }
}

