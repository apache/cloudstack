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
package com.cloud.network.nicira;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.utils.PropertiesUtil;

public class NiciraNvpApiIT {

    protected NiciraNvpApi api;

    protected long timestamp = System.currentTimeMillis();

    @Before
    public void setup() throws IOException {
        final Properties properties = new Properties();
        PropertiesUtil.loadFromFile(properties, PropertiesUtil.findConfigFile("config.properties"));
        api = new NiciraNvpApi();
        api.setControllerAddress(properties.getProperty("nvp.host"));
        api.setAdminCredentials(properties.getProperty("nvp.admin.user"),
                properties.getProperty("nvp.admin.pwd"));
    }

    @Test
    public void testCRUDSecurityProfile() throws NiciraNvpApiException {
        SecurityProfile sProfile = new SecurityProfile();
        sProfile.setDisplayName("SecProfile"+timestamp);

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
            sProfile.setDisplayName("UpdatedSecProfile"+timestamp);
            api.updateSecurityProfile(sProfile, sProfile.getUuid());

            // Read them all
            NiciraNvpList<SecurityProfile> profiles = api.findSecurityProfile();
            SecurityProfile scInList = null;
            for(final SecurityProfile iProfile : profiles.getResults()) {
                if (iProfile.getUuid().equalsIgnoreCase(sProfile.getUuid())) {
                    scInList = iProfile;
                }
            }
            Assert.assertEquals("Read a Security Profile different from the one just created and updated",
                    sProfile, scInList);

            // Read them filtered by uuid (get one)
            profiles = api.findSecurityProfile(sProfile.getUuid());
            Assert.assertEquals("Read a Security Profile different from the one just created and updated",
                    sProfile,
                    profiles.getResults().get(0));
            Assert.assertEquals("Read a Security Profile filtered by unique id (UUID) with more than one item",
                    1, profiles.getResults().size());

            // We can now delete the new entity
            api.deleteSecurityProfile(sProfile.getUuid());
        } catch (final NiciraNvpApiException e) {
            e.printStackTrace();
            assertTrue("Errors creating Security Profile", false);
        }
    }

    @Test
    public void testCRUDAcl() throws NiciraNvpApiException {
        Acl acl = new Acl();
        acl.setDisplayName("Acl"+timestamp);

        // Note that if the protocol is 6 (TCP) then you cannot put ICMP code and type
        // Note that if the protocol is 1 (ICMP) then you cannot put ports
        final List<AclRule> egressRules = new ArrayList<AclRule>();
        acl.setLogicalPortEgressRules(egressRules);
        egressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 1, "allow", null, null,
                "1.10.10.0", "1.10.10.1", null, null, null, null, 0, 0, 5));
        egressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 6, "allow", null, null,
                "1.10.10.6", "1.10.10.7", 80, 80, 80, 80, 1, null, null));

        final List<AclRule> ingressRules = new ArrayList<AclRule>();
        acl.setLogicalPortIngressRules(ingressRules);
        ingressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 1, "allow", null, null,
                "1.10.10.0", "1.10.10.1", null, null, null, null, 0, 0, 5));
        ingressRules.add(new AclRule(AclRule.ETHERTYPE_IPV4, 6, "allow", null, null,
                "1.10.10.6", "1.10.10.7", 80, 80, 80, 80, 1, null, null));

        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        acl.setTags(tags);
        tags.add(new NiciraNvpTag("nvp", "MyTag1"));
        tags.add(new NiciraNvpTag("nicira", "MyTag2"));
        // In the creation we don't get to specify UUID, href or schema: they don't exist yet

        try {
            acl = api.createAcl(acl);

            // We can now update the new entity
            acl.setDisplayName("UpdatedAcl"+timestamp);
            api.updateAcl(acl, acl.getUuid());

            // Read them all
            NiciraNvpList<Acl> acls = api.findAcl();
            Acl scInList = null;
            for(final Acl iAcl : acls.getResults()) {
                if (iAcl.getUuid().equalsIgnoreCase(acl.getUuid())) {
                    scInList = iAcl;
                }
            }
            Assert.assertEquals("Read a ACL different from the one just created and updated",
                    acl, scInList);

            // Read them filtered by uuid (get one)
            acls = api.findAcl(acl.getUuid());
            Assert.assertEquals("Read a ACL different from the one just created and updated",
                    acl,
                    acls.getResults().get(0));
            Assert.assertEquals("Read a ACL filtered by unique id (UUID) with more than one item",
                    1, acls.getResults().size());

            // We can now delete the new entity
            api.deleteAcl(acl.getUuid());
        } catch (final NiciraNvpApiException e) {
            e.printStackTrace();
            assertTrue("Errors creating ACL", false);
        }
    }
}
