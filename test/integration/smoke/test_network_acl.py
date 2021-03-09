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
""" Tests for Network ACLs in VPC
"""
#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

class TestNetworkACL(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestNetworkACL, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.account = Account.create(cls.apiclient, services=cls.services["account"])
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,\
                          cls.account.id))
        cls.cleanup = [cls.account]

    @attr(tags=["advanced"], required_hardware="true")
    def test_network_acl(self):
        #TODO: SIMENH: add actual verification Logic for rules.
        """Test network ACL lists and items in VPC"""

        # 0) Get the default network offering for VPC
        networkOffering = NetworkOffering.list(self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(networkOffering) > 0, "No VPC based network offering")

        # 1) Create VPC
        vpcOffering = VpcOffering.list(self.apiclient, name="Default VPC offering")
        self.assertTrue(vpcOffering is not None and len(vpcOffering)>0, "No VPC offerings found")
        self.services["vpc"] = {}
        self.services["vpc"]["name"] = "vpc-networkacl"
        self.services["vpc"]["displaytext"] = "vpc-networkacl"
        self.services["vpc"]["cidr"] = "10.1.1.0/24"
        vpc = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc.networkacl",
                vpcofferingid=vpcOffering[0].id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.domain.id
        )
        self.assertTrue(vpc is not None, "VPC creation failed")

        # 2) Create ACL
        aclgroup = NetworkACLList.create(apiclient=self.apiclient, services={}, name="acl", description="acl", vpcid=vpc.id)
        self.assertIsNotNone(aclgroup, "Failed to create NetworkACL list")
        self.debug("Created a network ACL list %s" % aclgroup.name)

        # 3) Create ACL Item
        aclitem = NetworkACL.create(apiclient=self.apiclient, services={},
            protocol="TCP", number="10", action="Deny", aclid=aclgroup.id, cidrlist=["0.0.0.0/0"])
        self.assertIsNotNone(aclitem, "Network failed to aclItem")
        self.debug("Added a network ACL %s to ACL list %s" % (aclitem.id, aclgroup.name))

        # 4) Create network with ACL
        self.services["vpcnetwork"] = {}
        self.services["vpcnetwork"]["name"] = "vpcntwk"
        self.services["vpcnetwork"]["displaytext"] = "vpcntwk"
        ntwk = Network.create(
            apiclient=self.apiclient,
            services=self.services["vpcnetwork"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            aclid=aclgroup.id,
            gateway="10.1.1.1",
            netmask="255.255.255.192"
        )
        self.assertIsNotNone(ntwk, "Network failed to create")
        self.debug("Network %s created in VPC %s" %(ntwk.id, vpc.id))

        # 5) Deploy a vm
        self.services["virtual_machine"]["networkids"] = ntwk.id
        vm = VirtualMachine.create(self.apiclient, services=self.services["virtual_machine"],
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid= self.domain.id,
            serviceofferingid=self.service_offering.id,
        )
        self.assertTrue(vm is not None, "VM failed to deploy")
        self.assertTrue(vm.state == 'Running', "VM is not running")
        self.debug("VM %s deployed in VPC %s" %(vm.id, vpc.id))

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Cleanup failed with %s" % e)
