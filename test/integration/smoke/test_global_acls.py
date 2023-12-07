# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Network, NetworkACLList, NetworkOffering, VpcOffering, VPC, NetworkACL)
from marvin.lib.common import (get_domain, get_zone)
from nose.plugins.attrib import attr
from marvin.cloudstackException import CloudstackAPIException


class Services:
    """Test Global ACLs
    """

    def __init__(self):
        self.services = {
            "root_domain": {
                "name": "ROOT",
            },
            "domain": {
                "name": "Domain",
            },
            "user": {
                "username": "user",
                "roletype": 0,
            },
            "domain_admin": {
                "username": "Domain admin",
                "roletype": 2,
            },
            "root_admin": {
                "username": "Root admin",
                "roletype": 1,
            },
            "vpc": {
                "name": "vpc-networkacl",
                "displaytext": "vpc-networkacl",
                "cidr": "10.1.1.0/24",
            },
            "vpcnetwork": {
                "name": "vpcnetwork",
                "displaytext": "vpcnetwork",
            },
            "rule": {
                "protocol": "all",
                "traffictype": "ingress",
            }
        }


class TestGlobalACLs(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestGlobalACLs, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        return

    def setUp(self):
        self.user_apiclient = self.testClient.getUserApiClient(self.services["user"]["username"],
                                                               self.services["domain"]["name"],
                                                               self.services["user"]["roletype"])

        self.domain_admin_apiclient = self.testClient.getUserApiClient(self.services["domain_admin"]["username"],
                                                                       self.services["domain"]["name"],
                                                                       self.services["domain_admin"]["roletype"])

        self.admin_apiclient = self.testClient.getUserApiClient(self.services["root_admin"]["username"],
                                                                self.services["root_domain"]["name"],
                                                                self.services["root_admin"]["roletype"])

        self.cleanup = []
        return

    def tearDown(self):
        super(TestGlobalACLs, self).tearDown()

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_create_global_acl(self):
        """ Test create global ACL as a normal user, domain admin and root admin users.
        """

        self.debug("Creating ACL list as a normal user, should raise exception.")
        self.assertRaisesRegex(CloudstackAPIException, "Only Root Admin can create global ACLs.",
                               NetworkACLList.create, apiclient=self.user_apiclient, services={},
                               name="acl", description="acl")

        self.debug("Creating ACL list as a domain admin, should raise exception.")
        self.assertRaisesRegex(CloudstackAPIException, "Only Root Admin can create global ACLs.",
                               NetworkACLList.create, apiclient=self.domain_admin_apiclient, services={},
                               name="acl", description="acl")

        self.debug("Creating ACL list as a root admin, should work.")
        acl = NetworkACLList.create(apiclient=self.admin_apiclient, services={}, name="acl", description="acl")
        self.cleanup.append(acl)
        self.assertIsNotNone(acl, "A root admin user should be able to create a global ACL.")

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_replace_acl_of_network(self):
        """ Test to replace ACL of a VPC as a normal user, domain admin and root admin users.
        """
        # Get network offering
        networkOffering = NetworkOffering.list(self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(networkOffering) > 0, "No VPC network offering")

        # Getting VPC offering
        vpcOffering = VpcOffering.list(self.apiclient, name="Default VPC offering")
        self.assertTrue(vpcOffering is not None and len(vpcOffering) > 0, "No VPC offerings found")

        # Creating VPC
        vpc = VPC.create(
            apiclient=self.apiclient,
            services=self.services["vpc"],
            networkDomain="vpc.networkacl",
            vpcofferingid=vpcOffering[0].id,
            zoneid=self.zone.id,
            domainid=self.domain.id
        )
        self.cleanup.append(vpc)
        self.assertTrue(vpc is not None, "VPC creation failed")

        # Creating ACL list
        acl = NetworkACLList.create(apiclient=self.apiclient, services={}, name="acl", description="acl")

        # Creating tier on VPC with ACL list
        network = Network.create(
            apiclient=self.apiclient,
            services=self.services["vpcnetwork"],
            accountid="Admin",
            domainid=self.domain.id,
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            aclid=acl.id,
            gateway="10.1.1.1",
            netmask="255.255.255.192"
        )
        self.cleanup.append(network)

        # User should be able to replace ACL
        network.replaceACLList(apiclient=self.user_apiclient, aclid=acl.id)
        # Domain Admin should be able to replace ACL
        network.replaceACLList(apiclient=self.domain_admin_apiclient, aclid=acl.id)
        # Admin should be able to replace ACL
        network.replaceACLList(apiclient=self.admin_apiclient, aclid=acl.id)

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_create_acl_rule(self):
        """ Test to create ACL rule as a normal user, domain admin and root admin users.
        """
        # Creating ACL list
        acl = NetworkACLList.create(apiclient=self.admin_apiclient, services={}, name="acl", description="acl")
        self.cleanup.append(acl)

        self.debug("Creating ACL rule as a user, should raise exception.")
        self.assertRaisesRegex(CloudstackAPIException, "Only Root Admins can create rules for a global ACL.",
                               NetworkACL.create, self.user_apiclient, services=self.services["rule"], aclid=acl.id)
        self.debug("Creating ACL rule as a domain admin, should raise exception.")
        self.assertRaisesRegex(CloudstackAPIException, "Only Root Admins can create rules for a global ACL.",
                               NetworkACL.create, self.domain_admin_apiclient, services=self.services["rule"], aclid=acl.id)
        self.debug("Creating ACL rule as a root admin, should work.")
        acl_rule = NetworkACL.create(self.admin_apiclient, services=self.services["rule"], aclid=acl.id)
        self.cleanup.append(acl_rule)

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_delete_acl_rule(self):
        """ Test to delete ACL rule as a normal user, domain admin and root admin users.
        """
        # Creating ACL list
        acl = NetworkACLList.create(apiclient=self.apiclient, services={}, name="acl", description="acl")
        self.cleanup.append(acl)

        # Creating ACL rule
        acl_rule = NetworkACL.create(self.apiclient, services=self.services["rule"], aclid=acl.id)
        self.cleanup.append(acl_rule)

        self.debug("Deleting ACL rule as a user, should raise exception.")
        self.assertRaisesRegex(Exception, "Only Root Admin can delete global ACL rules.",
                               NetworkACL.delete, acl_rule, self.user_apiclient)
        self.debug("Deleting ACL rule as a domain admin, should raise exception.")
        self.assertRaisesRegex(Exception, "Only Root Admin can delete global ACL rules.",
                               NetworkACL.delete, acl_rule, self.domain_admin_apiclient)

        self.debug("Deleting ACL rule as a root admin, should work.")
        NetworkACL.delete(acl_rule, self.admin_apiclient)
        self.cleanup.remove(acl_rule)

        # Verify if the number of ACL rules is equal to four, i.e. the number of rules
        # for the default ACLs `default_allow` (2 rules) and `default_deny` (2 rules) ACLs
        number_of_acl_rules = acl_rule.list(apiclient=self.admin_apiclient)
        self.assertEqual(len(number_of_acl_rules), 4)

        return


    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_delete_global_acl(self):
        """ Test delete global ACL as a normal user, domain admin and root admin users.
        """

        # Creating ACL list. Not adding to cleanup as it will be deleted in this method
        acl = NetworkACLList.create(apiclient=self.apiclient, services={}, name="acl", description="acl")
        self.cleanup.append(acl)

        self.debug("Deleting ACL list as a normal user, should raise exception.")
        self.assertRaisesRegex(Exception, "Only Root Admin can delete global ACLs.",
                               NetworkACLList.delete, acl, apiclient=self.user_apiclient)

        self.debug("Deleting ACL list as a domain admin, should raise exception.")
        self.assertRaisesRegex(Exception, "Only Root Admin can delete global ACLs.",
                               NetworkACLList.delete, acl, apiclient=self.domain_admin_apiclient)

        self.debug("Deleting ACL list as a root admin, should work.")
        acl.delete(apiclient=self.admin_apiclient)
        self.cleanup.remove(acl)

        # Verify if number of ACLs is equal to two, i.e. the number of default ACLs `default_allow` and `default_deny`
        number_of_acls = NetworkACLList.list(apiclient=self.admin_apiclient)
        self.assertEqual(len(number_of_acls), 2)

        return
