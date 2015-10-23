//
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
//

package com.cloud.network.nicira;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.rest.HttpClientHelper;

public class NiciraNvpApiIT {

    protected NiciraNvpApi api;

    protected long timestamp = System.currentTimeMillis();

    @Before
    public void setup() throws Exception {
        PropertiesUtil.loadFromFile(PropertiesUtil.findConfigFile("config.properties"));
        final String host = System.getProperty("nvp.host");
        final String user = System.getProperty("nvp.admin.user");
        final String pass = System.getProperty("nvp.admin.pwd");
        api = NiciraNvpApi.create()
            .host(host)
            .username(user)
            .password(pass)
            .httpClient(HttpClientHelper.createHttpClient(5))
            .build();
    }

    @Test
    public void testCRUDSecurityProfile() {
        SecurityProfile sProfile = new SecurityProfile();
        sProfile.setDisplayName("SecProfile" + timestamp);

        final List<SecurityRule> egressRules = new ArrayList<SecurityRule>();
        sProfile.setLogicalPortEgressRules(egressRules);
        egressRules.add(new SecurityRule(SecurityRule.ETHERTYPE_IPV4, "1.10.10.0", null, 80, 88, 6));
        egressRules.add(new SecurityRule(SecurityRule.ETHERTYPE_IPV6, "2a80:34ac::1", null, 90, 98, 6));

        final List<SecurityRule> ingressRules = new ArrayList<SecurityRule>();
        sProfile.setLogicalPortIngressRules(ingressRules);
        ingressRules.add(new SecurityRule(SecurityRule.ETHERTYPE_IPV4, "1.10.10.0", null, 50, 58, 6));
        ingressRules.add(new SecurityRule(SecurityRule.ETHERTYPE_IPV6, "280a:3ac4::1", null, 60, 68, 6));

        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        sProfile.setTags(tags);
        tags.add(new NiciraNvpTag("nvp", "MyTag1"));
        tags.add(new NiciraNvpTag("nicira", "MyTag2"));
        // In the creation we don't get to specify UUID, href or schema: they don't exist yet

        try {
            sProfile = api.createSecurityProfile(sProfile);

            // We can now update the new entity
            sProfile.setDisplayName("UpdatedSecProfile" + timestamp);
            api.updateSecurityProfile(sProfile, sProfile.getUuid());

            // Read them all
            List<SecurityProfile> profiles = api.findSecurityProfile();
            SecurityProfile scInList = null;
            for (final SecurityProfile iProfile : profiles) {
                if (iProfile.getUuid().equalsIgnoreCase(sProfile.getUuid())) {
                    scInList = iProfile;
                }
            }
            assertEquals("Read a Security Profile different from the one just created and updated", sProfile, scInList);

            // Read them filtered by uuid (get one)
            profiles = api.findSecurityProfile(sProfile.getUuid());
            assertEquals("Read a Security Profile different from the one just created and updated", sProfile, profiles.get(0));
            assertEquals("Read a Security Profile filtered by unique id (UUID) with more than one item", 1, profiles.size());

            // We can now delete the new entity
            api.deleteSecurityProfile(sProfile.getUuid());
        } catch (final NiciraNvpApiException e) {
            e.printStackTrace();
            assertTrue("Errors in Security Profile CRUD", false);
        }
    }

    @Test
    public void testCRUDAcl() {
        Acl acl = new Acl();
        acl.setDisplayName("Acl" + timestamp);

        // Note that if the protocol is 6 (TCP) then you cannot put ICMP code and type
        // Note that if the protocol is 1 (ICMP) then you cannot put ports
        final List<AclRule> egressRules = new ArrayList<AclRule>();
        acl.setLogicalPortEgressRules(egressRules);
        egressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 1, "allow", null, null, "1.10.10.0", "1.10.10.1", null, null, null, null, 0, 0, 5));
        egressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 6, "allow", null, null, "1.10.10.6", "1.10.10.7", 80, 80, 80, 80, 1, null, null));

        final List<AclRule> ingressRules = new ArrayList<AclRule>();
        acl.setLogicalPortIngressRules(ingressRules);
        ingressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 1, "allow", null, null, "1.10.10.0", "1.10.10.1", null, null, null, null, 0, 0, 5));
        ingressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 6, "allow", null, null, "1.10.10.6", "1.10.10.7", 80, 80, 80, 80, 1, null, null));

        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        acl.setTags(tags);
        tags.add(new NiciraNvpTag("nvp", "MyTag1"));
        tags.add(new NiciraNvpTag("nicira", "MyTag2"));
        // In the creation we don't get to specify UUID, href or schema: they don't exist yet

        try {
            acl = api.createAcl(acl);

            // We can now update the new entity
            acl.setDisplayName("UpdatedAcl" + timestamp);
            api.updateAcl(acl, acl.getUuid());

            // Read them all
            List<Acl> acls = api.findAcl();
            Acl scInList = null;
            for (final Acl iAcl : acls) {
                if (iAcl.getUuid().equalsIgnoreCase(acl.getUuid())) {
                    scInList = iAcl;
                }
            }
            assertEquals("Read a ACL different from the one just created and updated", acl, scInList);

            // Read them filtered by uuid (get one)
            acls = api.findAcl(acl.getUuid());
            assertEquals("Read a ACL different from the one just created and updated", acl, acls.get(0));
            assertEquals("Read a ACL filtered by unique id (UUID) with more than one item", 1, acls.size());

            // We can now delete the new entity
            api.deleteAcl(acl.getUuid());
        } catch (final NiciraNvpApiException e) {
            e.printStackTrace();
            assertTrue("Errors in ACL CRUD", false);
        }
    }

    @Test
    public void testCRUDLogicalSwitch() throws Exception {
        LogicalSwitch logicalSwitch = new LogicalSwitch();
        logicalSwitch.setDisplayName("LogicalSwitch" + timestamp);
        logicalSwitch.setPortIsolationEnabled(true);
        logicalSwitch.setReplicationMode("service");
        logicalSwitch.setTags(new ArrayList<NiciraNvpTag>());
        logicalSwitch.getTags().add(new NiciraNvpTag("anto", "hugo"));

        // In the creation we don't get to specify UUID, href or schema: they don't exist yet

        logicalSwitch = api.createLogicalSwitch(logicalSwitch);

        // We can now update the new entity
        logicalSwitch.setDisplayName("UpdatedLogicalSwitch" + timestamp);
        api.updateLogicalSwitch(logicalSwitch, logicalSwitch.getUuid());

        // Read them all
        List<LogicalSwitch> logicalSwitches = api.findLogicalSwitch();
        for (final LogicalSwitch iLogicalSwitch : logicalSwitches) {
            if (iLogicalSwitch.getUuid().equalsIgnoreCase(logicalSwitch.getUuid())) {
                assertEquals("Read a LogicalSwitch different from the one just created and updated", logicalSwitch, iLogicalSwitch);
            }
        }

        // Read them filtered by uuid (get one)
        logicalSwitches = api.findLogicalSwitch(logicalSwitch.getUuid());
        assertEquals("Read a LogicalSwitch different from the one just created and updated", logicalSwitch, logicalSwitches.get(0));
        assertEquals("Read a LogicalSwitch filtered by unique id (UUID) with more than one item", 1, logicalSwitches.size());

        // Before deleting the test LogicalSwitch, test its ports
        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", "OwnerName"));

        LogicalSwitchPort logicalSwitchPort = new LogicalSwitchPort("LSwitchPort" + timestamp, tags, true);
        logicalSwitchPort = api.createLogicalSwitchPort(logicalSwitch.getUuid(), logicalSwitchPort);

        logicalSwitchPort.setDisplayName("UpdatedLSwitchPort" + timestamp);
        api.updateLogicalSwitchPort(logicalSwitch.getUuid(), logicalSwitchPort);

        final List<LogicalSwitchPort> logicalSwitchePorts = api.findLogicalSwitchPortsByUuid(logicalSwitch.getUuid(), logicalSwitchPort.getUuid());
        for (final LogicalSwitchPort iLSwitchPort : logicalSwitchePorts) {
            if (iLSwitchPort.getUuid().equalsIgnoreCase(logicalSwitchPort.getUuid())) {
                assertEquals("Read a LogicalSwitchPort different from the one just created and updated", logicalSwitchPort, iLSwitchPort);
            }
        }

        // And finally test attachments
        final String attachmentUuid = UUID.randomUUID().toString();
        final VifAttachment vifAttachment = new VifAttachment(attachmentUuid);
        api.updateLogicalSwitchPortAttachment(logicalSwitch.getUuid(), logicalSwitchPort.getUuid(), vifAttachment);

        assertEquals("Read a LogicalSwitchPort by vifAttachment different than expected",
                        api.findLogicalSwitchPortUuidByVifAttachmentUuid(logicalSwitch.getUuid(), vifAttachment.getVifUuid()), logicalSwitchPort.getUuid());

        api.deleteLogicalSwitchPort(logicalSwitch.getUuid(), logicalSwitchPort.getUuid());

        // We can now delete the new entity
        api.deleteLogicalSwitch(logicalSwitch.getUuid());
    }

    @Test
    public void testCRUDLogicalRouter() {
        LogicalRouter logicalRouter = new LogicalRouter();
        logicalRouter.setDisplayName("LogicalRouter" + timestamp);
        logicalRouter.setDistributed(true);
        logicalRouter.setNatSynchronizationEnabled(true);
        logicalRouter.setReplicationMode(LogicalRouter.REPLICATION_MODE_SERVICE);
        final RoutingConfig routingConfig = new SingleDefaultRouteImplicitRoutingConfig(
                        new RouterNextHop("192.168.10.20"));
        logicalRouter.setRoutingConfig(routingConfig);

        // In the creation we don't get to specify UUID, href or schema: they don't exist yet

        try {
            logicalRouter = api.createLogicalRouter(logicalRouter);

            // We can now update the new entity
            logicalRouter.setDisplayName("UpdatedLogicalSwitch" + timestamp);
            api.updateLogicalRouter(logicalRouter, logicalRouter.getUuid());

            // Read them all
            List<LogicalRouter> logicalRouters = api.findLogicalRouter();
            LogicalRouter lsInList = null;
            for (final LogicalRouter iLogicalRouter : logicalRouters) {
                if (iLogicalRouter.getUuid().equalsIgnoreCase(logicalRouter.getUuid())) {
                    lsInList = iLogicalRouter;
                }
            }
            assertEquals("Read a LogicalRouter different from the one just created and updated", logicalRouter, lsInList);

            // Read them filtered by uuid (get one)
            logicalRouters = api.findLogicalRouter(logicalRouter.getUuid());
            assertEquals("Read a LogicalRouter different from the one just created and updated", logicalRouter, logicalRouters.get(0));
            assertEquals("Read a LogicalRouter filtered by unique id (UUID) with more than one item", 1, logicalRouters.size());

            assertEquals(logicalRouters.get(0), api.findOneLogicalRouterByUuid(logicalRouter.getUuid()));

            // Before deleting the test LogicalRouter, test its ports
            final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account", "OwnerName"));

            LogicalRouterPort logicalRouterPort = new LogicalRouterPort();
            logicalRouterPort.setDisplayName("LRouterPort" + timestamp);
            logicalRouterPort.setTags(tags);
            logicalRouterPort.setAdminStatusEnabled(true);
            logicalRouterPort.setPortno(1024);
            logicalRouterPort.setMacAddress("00:00:00:00:00:00");

            final List<String> ipAddresses = new ArrayList<String>();
            // Add some ips to this list
            logicalRouterPort.setIpAddresses(ipAddresses);
            logicalRouterPort = api.createLogicalRouterPort(logicalRouter.getUuid(), logicalRouterPort);

            logicalRouterPort.setDisplayName("UpdatedLRouterPort" + timestamp);
            api.updateLogicalRouterPort(logicalRouter.getUuid(), logicalRouterPort);

            final List<LogicalRouterPort> logicalRouterePorts = api.findLogicalRouterPortsByUuid(logicalRouter.getUuid(), logicalRouterPort.getUuid());
            for (final LogicalRouterPort iLRouterPort : logicalRouterePorts) {
                if (iLRouterPort.getUuid().equalsIgnoreCase(logicalRouterPort.getUuid())) {
                    assertEquals("Read a LogicalRouterPort different from the one just created and updated", logicalRouterPort, iLRouterPort);
                }
            }

            UUID.randomUUID().toString();

            // Test CRUD for Nat Rules
            SourceNatRule snr = new SourceNatRule();
            snr.setToSourceIpAddressMin("192.168.10.10");
            snr.setToSourceIpAddressMax("192.168.10.20");
            snr.setOrder(200);
            final Match match = new Match();
            match.setSourceIpAddresses("192.168.150.150");
            snr.setMatch(match);
            snr = (SourceNatRule) api.createLogicalRouterNatRule(logicalRouter.getUuid(), snr);
            snr.setToSourceIpAddressMax("192.168.10.30");
            api.updateLogicalRouterNatRule(logicalRouter.getUuid(), snr);

            api.findNatRulesByLogicalRouterUuid(logicalRouter.getUuid());
            api.deleteLogicalRouterNatRule(logicalRouter.getUuid(), snr.getUuid());

            api.deleteLogicalRouterPort(logicalRouter.getUuid(), logicalRouterPort.getUuid());

            // We can now delete the new entity
            api.deleteLogicalRouter(logicalRouter.getUuid());
        } catch (final NiciraNvpApiException e) {
            e.printStackTrace();
            assertTrue("Errors in LogicalRouter CRUD", false);
        }
    }

    @Test
    public void testGetControlClusterStatus() throws NiciraNvpApiException {
        final ControlClusterStatus controlClusterStatus = api.getControlClusterStatus();
        final String clusterStatus = controlClusterStatus.getClusterStatus();
        final boolean correctStatus = clusterStatus.equalsIgnoreCase("stable") ||
                        clusterStatus.equalsIgnoreCase("joining") || clusterStatus.equalsIgnoreCase("unstable");
        assertTrue("Not recognizable cluster status", correctStatus);
    }

}
