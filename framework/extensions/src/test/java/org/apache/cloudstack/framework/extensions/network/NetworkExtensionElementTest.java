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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Covers the four VpcProvider methods that used to throw UnsupportedOperationException:
 * createPrivateGateway, deletePrivateGateway, applyStaticRoutes and applyACLItemsToPrivateGw.
 *
 * <p>The payload assertions are deliberately literal. The JSON key names are the wire
 * contract with the extension script, so a rename here is a breaking change for every
 * deployed script and should fail a test rather than slip through.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class NetworkExtensionElementTest {

    private static final long VPC_ID = 7L;
    private static final long ZONE_ID = 1L;
    private static final long GATEWAY_ID = 31L;

    @Spy
    @InjectMocks
    NetworkExtensionElement element = new NetworkExtensionElement();

    @Mock
    VpcDao vpcDao;

    private VpcVO vpc;

    @Before
    public void setUp() {
        vpc = mock(VpcVO.class);
        lenient().when(vpc.getId()).thenReturn(VPC_ID);
        lenient().when(vpc.getZoneId()).thenReturn(ZONE_ID);
        lenient().when(vpc.getUuid()).thenReturn("vpc-uuid");
        lenient().when(vpc.getCidr()).thenReturn("10.0.0.0/8");
        lenient().when(vpc.getState()).thenReturn(Vpc.State.Enabled);
    }

    /** A fully populated gateway: every optional field has a value. */
    private PrivateGateway mockPrivateGateway() {
        PrivateGateway gateway = mock(PrivateGateway.class);
        lenient().when(gateway.getVpcId()).thenReturn(VPC_ID);
        lenient().when(gateway.getId()).thenReturn(GATEWAY_ID);
        lenient().when(gateway.getUuid()).thenReturn("gw-uuid");
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

    /** The minimum a gateway can carry: every optional field is blank or null. */
    private PrivateGateway mockBarePrivateGateway() {
        PrivateGateway gateway = mock(PrivateGateway.class);
        lenient().when(gateway.getVpcId()).thenReturn(VPC_ID);
        lenient().when(gateway.getId()).thenReturn(GATEWAY_ID);
        lenient().when(gateway.getUuid()).thenReturn("gw-uuid");
        lenient().when(gateway.getNetworkId()).thenReturn(55L);
        lenient().when(gateway.getZoneId()).thenReturn(ZONE_ID);
        lenient().when(gateway.getPhysicalNetworkId()).thenReturn(200L);
        lenient().when(gateway.getSourceNat()).thenReturn(true);
        lenient().when(gateway.getNetworkACLId()).thenReturn(3L);
        lenient().when(gateway.getIp4Address()).thenReturn("");
        lenient().when(gateway.getGateway()).thenReturn(null);
        lenient().when(gateway.getNetmask()).thenReturn("  ");
        lenient().when(gateway.getBroadcastUri()).thenReturn(null);
        lenient().when(gateway.getState()).thenReturn(null);
        return gateway;
    }

    private void stubScript(boolean outcome) {
        doReturn(outcome).when(element).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    private JsonObject captureVpcPayload(String expectedCommand) {
        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(element).executeVpcScript(any(Vpc.class), eq(expectedCommand), captor.capture());
        return captor.getValue();
    }

    private JsonObject objectAt(JsonArray array, int index) {
        return array.get(index).getAsJsonObject();
    }

    // ---- script command names: the wire contract with the extension script ----

    @Test
    public void testScriptCommandNamesAreStable() {
        assertEquals("create-private-gateway", NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY);
        assertEquals("delete-private-gateway", NetworkExtensionElement.CMD_DELETE_PRIVATE_GATEWAY);
        assertEquals("apply-static-routes", NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        assertEquals("apply-private-gw-acl", NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL);
    }

    // ---- createPrivateGateway / deletePrivateGateway ----

    @Test
    public void testCreatePrivateGatewaySendsGatewayPayload() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.createPrivateGateway(gateway));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY);
        assertEquals(String.valueOf(GATEWAY_ID), payload.get("gateway_id").getAsString());
        assertEquals("gw-uuid", payload.get("gateway_uuid").getAsString());
        assertEquals("172.16.0.5", payload.get("gateway_ip_address").getAsString());
        assertEquals("172.16.0.1", payload.get("gateway_gateway").getAsString());
        assertEquals("255.255.255.0", payload.get("gateway_netmask").getAsString());
        assertEquals("900", payload.get("gateway_vlan").getAsString());
        assertEquals("ready", payload.get("gateway_state").getAsString());
    }

    @Test
    public void testCreatePrivateGatewayCarriesVpcAndRemainingGatewayFields() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.createPrivateGateway(gateway));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY);
        // The script resolves the router from the VPC, so these must travel with the gateway.
        assertEquals(String.valueOf(VPC_ID), payload.get("vpc_id").getAsString());
        assertEquals(String.valueOf(ZONE_ID), payload.get("zone_id").getAsString());
        assertEquals("enabled", payload.get("vpc_state").getAsString());
        assertEquals("10.0.0.0/8", payload.get("vpc_cidr").getAsString());

        assertEquals("55", payload.get("gateway_network_id").getAsString());
        assertEquals(String.valueOf(ZONE_ID), payload.get("gateway_zone_id").getAsString());
        assertEquals("200", payload.get("gateway_physical_network_id").getAsString());
        assertEquals("false", payload.get("gateway_source_nat").getAsString());
        assertEquals("3", payload.get("gateway_network_acl_id").getAsString());
        assertEquals("vlan://900", payload.get("gateway_broadcast_uri").getAsString());
    }

    @Test
    public void testDeletePrivateGatewayUsesDeleteCommand() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.deletePrivateGateway(gateway));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_DELETE_PRIVATE_GATEWAY);
        assertEquals(String.valueOf(GATEWAY_ID), payload.get("gateway_id").getAsString());
    }

    @Test
    public void testPrivateGatewayPayloadOmitsBlankOptionalFields() throws Exception {
        PrivateGateway gateway = mockBarePrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.createPrivateGateway(gateway));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY);
        // Blank is not the same as absent: a script that tests for the key must not see
        // an empty address and configure a port with it.
        assertFalse(payload.has("gateway_ip_address"));
        assertFalse(payload.has("gateway_gateway"));
        assertFalse(payload.has("gateway_netmask"));
        assertFalse(payload.has("gateway_broadcast_uri"));
        assertFalse(payload.has("gateway_vlan"));
        assertFalse(payload.has("gateway_state"));
        // The mandatory fields survive.
        assertEquals(String.valueOf(GATEWAY_ID), payload.get("gateway_id").getAsString());
        assertEquals("true", payload.get("gateway_source_nat").getAsString());
        assertEquals("3", payload.get("gateway_network_acl_id").getAsString());
    }

    @Test
    public void testPrivateGatewayPayloadOmitsVlanWhenBroadcastUriIsMalformed() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(gateway.getBroadcastUri()).thenReturn("vlan://bad host");
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.createPrivateGateway(gateway));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY);
        // An unparsable URI must not abort the call; the raw value still goes out.
        assertEquals("vlan://bad host", payload.get("gateway_broadcast_uri").getAsString());
        assertFalse(payload.has("gateway_vlan"));
    }

    @Test
    public void testPrivateGatewayPayloadOmitsVlanWhenBroadcastUriCarriesNone() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(gateway.getBroadcastUri()).thenReturn("untagged");
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.createPrivateGateway(gateway));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_CREATE_PRIVATE_GATEWAY);
        assertEquals("untagged", payload.get("gateway_broadcast_uri").getAsString());
        assertFalse(payload.has("gateway_vlan"));
    }

    @Test
    public void testPrivateGatewayWithoutVpcIsRejected() throws Exception {
        PrivateGateway gateway = mock(PrivateGateway.class);
        when(gateway.getVpcId()).thenReturn(null);

        assertFalse(element.createPrivateGateway(gateway));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    @Test
    public void testPrivateGatewayWithMissingVpcIsRejected() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(null);

        assertFalse(element.deletePrivateGateway(gateway));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    @Test
    public void testCreatePrivateGatewayReturnsFalseWhenScriptFails() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(false);

        // createPrivateGateway reports failure by return value, unlike the apply* methods.
        assertFalse(element.createPrivateGateway(gateway));
    }

    @Test
    public void testDeletePrivateGatewayReturnsFalseWhenScriptFails() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(false);

        assertFalse(element.deletePrivateGateway(gateway));
    }

    // ---- applyStaticRoutes ----

    private StaticRouteProfile mockRoute(String cidr, String nextHop, StaticRoute.State state) {
        StaticRouteProfile route = mock(StaticRouteProfile.class);
        lenient().when(route.getCidr()).thenReturn(cidr);
        lenient().when(route.getNextHop()).thenReturn(nextHop);
        lenient().when(route.getState()).thenReturn(state);
        return route;
    }

    @Test
    public void testApplyStaticRoutesSkipsRevokedAndSortsByCidr() throws Exception {
        stubScript(true);

        List<StaticRouteProfile> routes = List.of(
                mockRoute("10.2.0.0/16", "172.16.0.9", StaticRoute.State.Active),
                mockRoute("10.1.0.0/16", "172.16.0.8", StaticRoute.State.Add),
                mockRoute("10.3.0.0/16", "172.16.0.7", StaticRoute.State.Revoke));

        assertTrue(element.applyStaticRoutes(vpc, routes));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        JsonArray array = payload.getAsJsonArray("static_routes");
        assertEquals(2, array.size());
        assertEquals("10.1.0.0/16", objectAt(array, 0).get("cidr").getAsString());
        assertEquals("10.2.0.0/16", objectAt(array, 1).get("cidr").getAsString());
    }

    @Test
    public void testApplyStaticRoutesSerialisesEveryRouteField() throws Exception {
        stubScript(true);

        StaticRouteProfile route = mockRoute("10.1.0.0/16", "172.16.0.8", StaticRoute.State.Active);
        when(route.getId()).thenReturn(88L);
        when(route.getVpcGatewayId()).thenReturn(31L);
        when(route.getIp4Address()).thenReturn("172.16.0.5");
        when(route.getGateway()).thenReturn("172.16.0.1");
        when(route.getNetmask()).thenReturn("255.255.255.0");
        when(route.getVlanTag()).thenReturn("900");

        assertTrue(element.applyStaticRoutes(vpc, List.of(route)));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        JsonObject json = objectAt(payload.getAsJsonArray("static_routes"), 0);
        assertEquals(88L, json.get("id").getAsLong());
        assertEquals("10.1.0.0/16", json.get("cidr").getAsString());
        assertEquals("172.16.0.8", json.get("next_hop").getAsString());
        assertEquals("active", json.get("state").getAsString());
        assertEquals("31", json.get("vpc_gateway_id").getAsString());
        assertEquals("172.16.0.5", json.get("gateway_ip_address").getAsString());
        assertEquals("172.16.0.1", json.get("gateway").getAsString());
        assertEquals("255.255.255.0", json.get("netmask").getAsString());
        assertEquals("900", json.get("vlan").getAsString());
    }

    @Test
    public void testApplyStaticRoutesOmitsBlankRouteFields() throws Exception {
        stubScript(true);

        StaticRouteProfile route = mockRoute("10.1.0.0/16", "172.16.0.8", StaticRoute.State.Active);
        when(route.getVpcGatewayId()).thenReturn(null);
        when(route.getIp4Address()).thenReturn("");
        when(route.getGateway()).thenReturn(null);
        when(route.getNetmask()).thenReturn("   ");
        when(route.getVlanTag()).thenReturn(null);

        assertTrue(element.applyStaticRoutes(vpc, List.of(route)));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        JsonObject json = objectAt(payload.getAsJsonArray("static_routes"), 0);
        assertFalse(json.has("vpc_gateway_id"));
        assertFalse(json.has("gateway_ip_address"));
        assertFalse(json.has("gateway"));
        assertFalse(json.has("netmask"));
        assertFalse(json.has("vlan"));
        // A route that is not attached to a private gateway still carries its own fields.
        assertEquals("10.1.0.0/16", json.get("cidr").getAsString());
        assertEquals("172.16.0.8", json.get("next_hop").getAsString());
    }

    @Test
    public void testApplyStaticRoutesToleratesNullCidrWhenSorting() throws Exception {
        stubScript(true);

        List<StaticRouteProfile> routes = List.of(
                mockRoute("10.1.0.0/16", "172.16.0.8", StaticRoute.State.Active),
                mockRoute(null, "172.16.0.9", StaticRoute.State.Active));

        // The comparator must not blow up on a half-built route.
        assertTrue(element.applyStaticRoutes(vpc, routes));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        JsonArray array = payload.getAsJsonArray("static_routes");
        assertEquals(2, array.size());
        assertEquals("", objectAt(array, 0).get("cidr").getAsString());
    }

    @Test
    public void testApplyStaticRoutesCarriesVpcIdentity() throws Exception {
        stubScript(true);

        assertTrue(element.applyStaticRoutes(vpc, List.of()));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        assertEquals(String.valueOf(VPC_ID), payload.get("vpc_id").getAsString());
        assertEquals(String.valueOf(ZONE_ID), payload.get("zone_id").getAsString());
    }

    @Test
    public void testApplyStaticRoutesWithNullListSendsEmptyArray() throws Exception {
        stubScript(true);

        assertTrue(element.applyStaticRoutes(vpc, null));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        // An empty array means "no routes"; omitting the key would mean "unchanged".
        assertTrue(payload.has("static_routes"));
        assertEquals(0, payload.getAsJsonArray("static_routes").size());
    }

    @Test
    public void testApplyStaticRoutesThrowsWhenScriptFails() {
        stubScript(false);

        assertThrows(ResourceUnavailableException.class,
                () -> element.applyStaticRoutes(vpc, List.of()));
    }

    @Test
    public void testApplyStaticRoutesWithoutVpcIsRejected() throws Exception {
        assertFalse(element.applyStaticRoutes(null, List.of()));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    // ---- applyACLItemsToPrivateGw ----

    private NetworkACLItem mockAclItem(int number, NetworkACLItem.State state) {
        NetworkACLItem item = mock(NetworkACLItem.class);
        lenient().when(item.getState()).thenReturn(state);
        lenient().when(item.getNumber()).thenReturn(number);
        lenient().when(item.getAction()).thenReturn(NetworkACLItem.Action.Allow);
        lenient().when(item.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Ingress);
        lenient().when(item.getProtocol()).thenReturn("tcp");
        return item;
    }

    @Test
    public void testApplyAclItemsToPrivateGwSkipsRevoked() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        NetworkACLItem active = mockAclItem(10, NetworkACLItem.State.Active);
        NetworkACLItem revoked = mockAclItem(20, NetworkACLItem.State.Revoke);

        assertTrue(element.applyACLItemsToPrivateGw(gateway, List.of(active, revoked)));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL);
        assertEquals(1, payload.getAsJsonArray("acl_rules").size());
        assertEquals(String.valueOf(GATEWAY_ID), payload.get("gateway_id").getAsString());
    }

    @Test
    public void testApplyAclItemsToPrivateGwSortsByRuleNumber() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        List<NetworkACLItem> rules = List.of(
                mockAclItem(30, NetworkACLItem.State.Active),
                mockAclItem(10, NetworkACLItem.State.Add),
                mockAclItem(20, NetworkACLItem.State.Active));

        assertTrue(element.applyACLItemsToPrivateGw(gateway, rules));

        JsonArray array = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL)
                .getAsJsonArray("acl_rules");
        // Rule order is the evaluation order on the backend, so it has to be deterministic.
        assertEquals(3, array.size());
        assertEquals(10, objectAt(array, 0).get("number").getAsInt());
        assertEquals(20, objectAt(array, 1).get("number").getAsInt());
        assertEquals(30, objectAt(array, 2).get("number").getAsInt());
    }

    @Test
    public void testApplyAclItemsToPrivateGwSerialisesRuleFields() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        NetworkACLItem rule = mockAclItem(10, NetworkACLItem.State.Active);
        when(rule.getAction()).thenReturn(NetworkACLItem.Action.Deny);
        when(rule.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Egress);
        when(rule.getSourcePortStart()).thenReturn(80);
        when(rule.getSourcePortEnd()).thenReturn(443);
        when(rule.getSourceCidrList()).thenReturn(List.of("10.1.0.0/16", "10.2.0.0/16"));
        // Mockito hands back 0 rather than null for a boxed Integer, which would make
        // the omit-when-null branch look taken when it was not.
        when(rule.getIcmpType()).thenReturn(null);
        when(rule.getIcmpCode()).thenReturn(null);

        assertTrue(element.applyACLItemsToPrivateGw(gateway, List.of(rule)));

        JsonObject json = objectAt(captureVpcPayload(NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL)
                .getAsJsonArray("acl_rules"), 0);
        assertEquals("deny", json.get("action").getAsString());
        assertEquals("egress", json.get("trafficType").getAsString());
        assertEquals("tcp", json.get("protocol").getAsString());
        assertEquals(80, json.get("portStart").getAsInt());
        assertEquals(443, json.get("portEnd").getAsInt());
        assertFalse(json.has("icmpType"));
        assertFalse(json.has("icmpCode"));
        JsonArray cidrs = json.getAsJsonArray("sourceCidrs");
        assertEquals(2, cidrs.size());
        assertEquals("10.1.0.0/16", cidrs.get(0).getAsString());
    }

    @Test
    public void testApplyAclItemsToPrivateGwCarriesVpcAndGatewayIdentity() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.applyACLItemsToPrivateGw(gateway, List.of()));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL);
        // The rules belong to a gateway, but the script addresses the router by VPC.
        assertEquals(String.valueOf(VPC_ID), payload.get("vpc_id").getAsString());
        assertEquals("gw-uuid", payload.get("gateway_uuid").getAsString());
        assertEquals("900", payload.get("gateway_vlan").getAsString());
    }

    @Test
    public void testApplyAclItemsToPrivateGwWithNullRulesSendsEmptyArray() throws Exception {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(true);

        assertTrue(element.applyACLItemsToPrivateGw(gateway, null));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_PRIVATE_GW_ACL);
        assertTrue(payload.has("acl_rules"));
        assertEquals(0, payload.getAsJsonArray("acl_rules").size());
    }

    @Test
    public void testApplyAclItemsToPrivateGwWithoutVpcIsRejected() throws Exception {
        PrivateGateway gateway = mock(PrivateGateway.class);
        when(gateway.getVpcId()).thenReturn(null);

        assertFalse(element.applyACLItemsToPrivateGw(gateway, List.of()));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    @Test
    public void testApplyAclItemsToPrivateGwThrowsWhenScriptFails() {
        PrivateGateway gateway = mockPrivateGateway();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        stubScript(false);

        assertThrows(ResourceUnavailableException.class,
                () -> element.applyACLItemsToPrivateGw(gateway, List.of()));
    }

    // ---- null gateway: the first half of getVpcForPrivateGateway's guard ----
    //
    // Every public entry point can be handed a null gateway, so each is checked
    // separately rather than trusting that they share a helper.

    @Test
    public void testCreatePrivateGatewayWithNullGatewayIsRejected() throws Exception {
        assertFalse(element.createPrivateGateway(null));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    @Test
    public void testDeletePrivateGatewayWithNullGatewayIsRejected() throws Exception {
        assertFalse(element.deletePrivateGateway(null));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    @Test
    public void testApplyAclItemsToPrivateGwWithNullGatewayIsRejected() throws Exception {
        assertFalse(element.applyACLItemsToPrivateGw(null, List.of()));

        verify(element, never()).executeVpcScript(any(Vpc.class), anyString(), any(JsonObject.class));
    }

    @Test
    public void testApplyStaticRoutesOmitsStateWhenNull() throws Exception {
        stubScript(true);

        // A route whose state has not been set yet: the key must be absent rather
        // than present and empty, for the same reason as every other blank field.
        StaticRouteProfile route = mockRoute("10.1.0.0/16", "172.16.0.8", null);

        assertTrue(element.applyStaticRoutes(vpc, List.of(route)));

        JsonObject payload = captureVpcPayload(NetworkExtensionElement.CMD_APPLY_STATIC_ROUTES);
        JsonObject json = objectAt(payload.getAsJsonArray("static_routes"), 0);
        assertFalse(json.has("state"));
        assertEquals("10.1.0.0/16", json.get("cidr").getAsString());
    }
}
