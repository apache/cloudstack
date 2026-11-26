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
package org.apache.cloudstack.framework.extensions.network;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.network.Network;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Pins the wire contract between this class and the extension script for the four
 * VPC gateway / static route commands, and writes the payloads it actually produces
 * to {@code target/extension-payloads/} so they can be replayed against a real
 * backend.
 *
 * <p>The sibling unit test asserts what this class writes. The script's own test
 * asserts what the script reads. Neither notices if the two drift apart, because
 * each half is checked against a fixture written by the same hand. This test closes
 * that gap from the Java side by asserting the keys the script treats as mandatory,
 * and it produces the input for closing it from the other side: the extension's
 * {@code test-with-ovn.sh --payloads target/extension-payloads} replays these exact
 * files against a live OVN northbound database.</p>
 *
 * <p>The required-key lists below are a transcription of the script's
 * {@code require_arg} calls. If the script starts needing a new field, this test
 * should be updated in the same change and will fail until the payload carries it.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class NetworkExtensionElementPayloadContractTest {

    /** Where the replayable payloads land; see the class javadoc. */
    private static final File OUTPUT_DIR = new File("target/extension-payloads");

    private static final long VPC_ID = 7L;
    private static final long TIER_ID = 100L;
    private static final long ZONE_ID = 1L;
    private static final long GATEWAY_ID = 31L;

    @Spy
    @InjectMocks
    NetworkExtensionElement element = new NetworkExtensionElement();

    @Mock
    VpcDao vpcDao;

    private VpcVO vpc;

    @BeforeClass
    public static void prepareOutputDir() {
        if (!OUTPUT_DIR.isDirectory() && !OUTPUT_DIR.mkdirs()) {
            throw new IllegalStateException("could not create " + OUTPUT_DIR);
        }
    }

    @Before
    public void setUp() {
        vpc = mock(VpcVO.class);
        lenient().when(vpc.getId()).thenReturn(VPC_ID);
        lenient().when(vpc.getUuid()).thenReturn("c0ffee00-0000-4000-8000-0000000000c7");
        lenient().when(vpc.getZoneId()).thenReturn(ZONE_ID);
        lenient().when(vpc.getCidr()).thenReturn("10.2.0.0/16");
        lenient().when(vpc.getState()).thenReturn(Vpc.State.Enabled);
        lenient().when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        doReturn(true).when(element).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    private PrivateGateway privateGateway() {
        PrivateGateway gateway = mock(PrivateGateway.class);
        lenient().when(gateway.getVpcId()).thenReturn(VPC_ID);
        lenient().when(gateway.getId()).thenReturn(GATEWAY_ID);
        lenient().when(gateway.getUuid()).thenReturn("4f1d9f6e-0b7a-4a1e-9c2d-1a2b3c4d5e6f");
        lenient().when(gateway.getNetworkId()).thenReturn(55L);
        lenient().when(gateway.getZoneId()).thenReturn(ZONE_ID);
        lenient().when(gateway.getPhysicalNetworkId()).thenReturn(200L);
        lenient().when(gateway.getIp4Address()).thenReturn("172.16.0.5");
        lenient().when(gateway.getGateway()).thenReturn("172.16.0.1");
        lenient().when(gateway.getNetmask()).thenReturn("255.255.255.0");
        lenient().when(gateway.getBroadcastUri()).thenReturn("vlan://900");
        lenient().when(gateway.getSourceNat()).thenReturn(false);
        lenient().when(gateway.getNetworkACLId()).thenReturn(3L);
        lenient().when(gateway.getState()).thenReturn(VpcGateway.State.Ready);
        return gateway;
    }

    /**
     * Captures the payload the element produced for {@code command}, checks the keys
     * the script cannot run without, and writes the whole thing out in the envelope
     * buildVpcScriptPayload wraps it in, so the file
     * is directly consumable by the script.
     */
    private void captureAndWrite(String command, String... requiredKeys) {
        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(element).executeVpcScript(any(Vpc.class), eq(command), captor.capture());
        writeEnvelope(command, captor.getValue(), requiredKeys);
    }

    /**
     * Tier-level commands go out through {@code executeScript(Network, ...)} rather
     * than the VPC path, so they need their own capture point.
     */
    private void captureNetworkCommandAndWrite(String command, String... requiredKeys) {
        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(element).executeScript(any(Network.class), eq(command), captor.capture());
        writeEnvelope(command, captor.getValue(), requiredKeys);
    }

    private void writeEnvelope(String command, JsonObject payload, String... requiredKeys) {
        for (String key : requiredKeys) {
            assertTrue(command + " must carry '" + key + "': the script's require_arg fails without it",
                    payload.has(key));
        }

        JsonObject envelope = new JsonObject();
        // An empty object here is a placeholder: the replay harness substitutes the
        // details of whatever backend it is pointed at. The KEY names are the contract.
        envelope.add(NetworkExtensionElement.ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS, new JsonObject());
        envelope.add(NetworkExtensionElement.ARG_NETWORK_EXTENSION_DETAILS, new JsonObject());
        envelope.add(NetworkExtensionElement.ARG_PAYLOAD, payload);

        File out = new File(OUTPUT_DIR, command + ".json");
        try {
            Files.write(out.toPath(),
                    new GsonBuilder().setPrettyPrinting().create().toJson(envelope)
                            .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("could not write " + out, e);
        }
    }

    /** A VPC tier: its own logical switch, a leg on the shared VPC router. */
    private Network vpcTier() {
        Network tier = mock(Network.class);
        lenient().when(tier.getId()).thenReturn(TIER_ID);
        lenient().when(tier.getUuid()).thenReturn("9b2c7a10-5d3e-4c8f-a1b2-c3d4e5f60718");
        lenient().when(tier.getVpcId()).thenReturn(VPC_ID);
        lenient().when(tier.getCidr()).thenReturn("10.2.1.0/24");
        lenient().when(tier.getGateway()).thenReturn("10.2.1.1");
        lenient().when(tier.getDataCenterId()).thenReturn(ZONE_ID);
        lenient().when(tier.getGuestType()).thenReturn(Network.GuestType.Isolated);
        lenient().when(tier.getState()).thenReturn(Network.State.Implemented);
        return tier;
    }

    @Test
    public void testImplementVpcPayloadMeetsTheScriptContract() throws Exception {
        doNothing().when(element).ensureExtensionDetails(any(Vpc.class));
        // No source NAT IP allocated yet: the common state when a VPC is first
        // implemented. The address arrives later via update-vpc-source-nat-ip.
        doReturn(null).when(element).getVpcSourceNatIp(VPC_ID);

        assertTrue(element.implementVpc(vpc, null, null));
        // ovn.sh cmd_implement_vpc: require_arg vpc_id; vpc_cidr sizes the VPC-wide
        // source NAT rule once an address exists.
        captureAndWrite(NetworkExtensionElement.CMD_IMPLEMENT_VPC, "vpc_id", "vpc_cidr");
    }

    @Test
    public void testApplyNetworkAclPayloadMeetsTheScriptContract() throws Exception {
        doReturn(true).when(element).canHandle(any(Network.class), any());
        doReturn(true).when(element).executeScript(any(Network.class), anyString(), any(JsonObject.class));

        NetworkACLItem allowHttp = mock(NetworkACLItem.class);
        lenient().when(allowHttp.getState()).thenReturn(NetworkACLItem.State.Active);
        lenient().when(allowHttp.getNumber()).thenReturn(10);
        lenient().when(allowHttp.getAction()).thenReturn(NetworkACLItem.Action.Allow);
        lenient().when(allowHttp.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Ingress);
        lenient().when(allowHttp.getProtocol()).thenReturn("tcp");
        lenient().when(allowHttp.getSourcePortStart()).thenReturn(80);
        lenient().when(allowHttp.getSourcePortEnd()).thenReturn(80);
        lenient().when(allowHttp.getIcmpType()).thenReturn(null);
        lenient().when(allowHttp.getIcmpCode()).thenReturn(null);
        lenient().when(allowHttp.getSourceCidrList()).thenReturn(List.of("0.0.0.0/0"));

        NetworkACLItem denyPeerTier = mock(NetworkACLItem.class);
        lenient().when(denyPeerTier.getState()).thenReturn(NetworkACLItem.State.Active);
        lenient().when(denyPeerTier.getNumber()).thenReturn(20);
        lenient().when(denyPeerTier.getAction()).thenReturn(NetworkACLItem.Action.Deny);
        lenient().when(denyPeerTier.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Egress);
        lenient().when(denyPeerTier.getProtocol()).thenReturn("all");
        lenient().when(denyPeerTier.getSourcePortStart()).thenReturn(null);
        lenient().when(denyPeerTier.getSourcePortEnd()).thenReturn(null);
        lenient().when(denyPeerTier.getIcmpType()).thenReturn(null);
        lenient().when(denyPeerTier.getIcmpCode()).thenReturn(null);
        lenient().when(denyPeerTier.getSourceCidrList()).thenReturn(List.of("10.2.2.0/24"));

        assertTrue(element.applyNetworkACLs(vpcTier(), List.of(allowHttp, denyPeerTier)));
        // ovn.sh cmd_apply_network_acl: require_arg network_id, needs the tier cidr
        // for the default policy, and vpc_id to pick the VPC router scope.
        captureNetworkCommandAndWrite(NetworkExtensionElement.CMD_APPLY_NETWORK_ACL,
                "network_id", "vpc_id", "cidr", "acl_rules");
    }

    @Test
    public void testCreatePrivateGatewayPayloadMeetsTheScriptContract() throws Exception {
        assertTrue(element.createPrivateGateway(privateGateway()));
        // ovn.sh cmd_create_private_gateway: require_arg gateway_id, gateway_ip_address;
        // vpc_id selects the VPC router scope; netmask sizes the subnet.
        captureAndWrite(NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY,
                "vpc_id", "gateway_id", "gateway_ip_address", "gateway_netmask");
    }

    @Test
    public void testDeletePrivateGatewayPayloadMeetsTheScriptContract() throws Exception {
        assertTrue(element.deletePrivateGateway(privateGateway()));
        // ovn.sh cmd_delete_private_gateway: require_arg gateway_id.
        captureAndWrite(NetworkExtensionElement.CMD_DELETE_PRIVATE_GATEWAY,
                "vpc_id", "gateway_id");
    }

    @Test
    public void testApplyStaticRoutesPayloadMeetsTheScriptContract() throws Exception {
        StaticRouteProfile viaGateway = mock(StaticRouteProfile.class);
        lenient().when(viaGateway.getCidr()).thenReturn("10.9.0.0/16");
        lenient().when(viaGateway.getNextHop()).thenReturn("172.16.0.1");
        lenient().when(viaGateway.getState()).thenReturn(StaticRoute.State.Active);
        lenient().when(viaGateway.getId()).thenReturn(88L);
        lenient().when(viaGateway.getVpcGatewayId()).thenReturn(GATEWAY_ID);

        StaticRouteProfile second = mock(StaticRouteProfile.class);
        lenient().when(second.getCidr()).thenReturn("10.8.0.0/16");
        lenient().when(second.getNextHop()).thenReturn("172.16.0.1");
        lenient().when(second.getState()).thenReturn(StaticRoute.State.Add);
        lenient().when(second.getId()).thenReturn(89L);

        assertTrue(element.applyStaticRoutes(vpc, List.of(viaGateway, second)));
        // ovn.sh cmd_apply_static_routes: require_arg vpc_id, then iterates
        // .payload.static_routes[] reading .cidr and .next_hop.
        captureAndWrite(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES,
                "vpc_id", "static_routes");
    }

    @Test
    public void testApplyPrivateGwAclPayloadMeetsTheScriptContract() throws Exception {
        NetworkACLItem rule = mock(NetworkACLItem.class);
        lenient().when(rule.getState()).thenReturn(NetworkACLItem.State.Active);
        lenient().when(rule.getNumber()).thenReturn(10);
        lenient().when(rule.getAction()).thenReturn(NetworkACLItem.Action.Allow);
        lenient().when(rule.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Ingress);
        lenient().when(rule.getProtocol()).thenReturn("tcp");
        lenient().when(rule.getSourcePortStart()).thenReturn(443);
        lenient().when(rule.getSourcePortEnd()).thenReturn(443);
        lenient().when(rule.getIcmpType()).thenReturn(null);
        lenient().when(rule.getIcmpCode()).thenReturn(null);
        lenient().when(rule.getSourceCidrList()).thenReturn(List.of("10.9.0.0/16"));

        assertTrue(element.applyACLItemsToPrivateGw(privateGateway(), List.of(rule)));
        // ovn.sh cmd_apply_private_gw_acl: require_arg gateway_id, gateway_ip_address;
        // gateway_netmask scopes the rules; acl_rules carries the rule set.
        captureAndWrite(NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL,
                "vpc_id", "gateway_id", "gateway_ip_address", "gateway_netmask", "acl_rules");
    }
}
