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
"""
BVT tests for NCC integration with cloudstack
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.common import get_domain, get_zone, get_template
from marvin.lib import ncc
from marvin.lib.base import (Account,
                             VirtualMachine,
                             PublicIPAddress,
                             LoadBalancerRule,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             NATRule,
                            PhysicalNetwork,
                            NetworkServiceProvider,
	                        RegisteredServicePackage)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
import logging


class TestNccIntegrationDedicated(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNccIntegrationDedicated, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls._cleanup = []

        cls.logger = logging.getLogger('TestNccIntegrationDedicated')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"] )
        ncc_ip = cls.services["NCC"]["NCCIP"]
        ns_ip = cls.services["NSDedicated"]["NSIP"]
        cls.debug("NS IP - Dedicated: %s" % ns_ip)

        mgmt_srv_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]
        #ncc_ip = "10.102.195.215"
        #ns_ip = "10.102.195.210"
        cls.ns = ncc.NCC(ncc_ip, ns_ip, mgmt_srv_ip, logger=cls.logger)
        cls.ns.registerCCP(cls.api_client)
        cls.ns.registerNS()
        cls.ns.assignNStoCSZone()
        spname = cls.services["servicepackage_dedicated"]["name"]

        # Create Service package and get device group id, tenant group id and service package id
        # These would be needed later for clean up

        (cls.dv_group_id, cls.tnt_group_id, cls.srv_pkg_id) = cls.ns.createServicePackages(
            spname,
            "NetScalerVPX",
            ns_ip,
            isolation_policy="dedicated")
        cls.debug("Created service package in NCC")
        cls.debug("dv_group, tnt_group, srv_pkg_id: %s %s %s" %(cls.dv_group_id,cls.tnt_group_id, cls.srv_pkg_id))

        srv_pkg_list = RegisteredServicePackage.list(cls.api_client)
        # Choose the one created
        cls.srv_pkg_uuid = None
        for sp in srv_pkg_list:
            if sp.name == spname:
                cls.srv_pkg_uuid = sp.id
        #srv_pkg_id = srv_pkg_list[0].id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"]
        )
        cls._cleanup.append(cls.account)

        try:
            cls.services["nw_off_ncc_DedicatedSP"]["servicepackageuuid"] = cls.srv_pkg_uuid
            cls.services["nw_off_ncc_DedicatedSP"]["servicepackagedescription"] = "A NetScalerVPX is dedicated per network."
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.services["nw_off_ncc_DedicatedSP"])
        except Exception as e:
            raise Exception ("Unable to create network offering with Service package % s due to exception % s"
                             % (cls.srv_pkg_uuid, e))

        # Network offering should be removed so that service package may be deleted later
        cls._cleanup.append(cls.network_offering)

        cls.network_offering.update(cls.api_client, state = "Enabled")
        cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"]
            )
        cls.services["small"]["template"] = cls.template.id

        # Enable Netscaler Service Provider

        cls.phy_nws = PhysicalNetwork.list(cls.api_client,zoneid=cls.zone.id)
        if isinstance(cls.phy_nws, list):
            physical_network = cls.phy_nws[0]

        try:
            cls.ns_service_provider = NetworkServiceProvider.list(cls.api_client,name='Netscaler')
            if isinstance(cls.ns_service_provider, list):
                ns_provider = cls.ns_service_provider[0]
        except:
            raise Exception ("Netscaler service provider not found!!")

        try:
            if ns_provider.state != "Enabled":
                NetworkServiceProvider.update(cls.api_client, id=ns_provider.id, physicalnetworkid=physical_network.id, state="Enabled")
        except:
            raise Exception ("Enabling Netscaler Service provider failed. Unable to proceed")

        return

    @classmethod
    def tearDownClass(cls):
         try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
         except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
         cls.ns.cleanup_ncc(cls.dv_group_id, cls.srv_pkg_uuid, cls.srv_pkg_id, cls.tnt_group_id)
         return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        return

    def tearDown(self):
        return


    @attr(tags=["ncc"], required_hardware="true")
    def test_01_dedicated_first_network(self):
        # Create network
        self.debug("Creating network with network offering: %s" % self.network_offering.id)
        self.network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network: %s" % self.network.id)

        self.debug("Trying VM deploy with network created on account: %s" % self.account.name)

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            networkids=self.network.id,
            serviceofferingid=self.service_offering.id
        )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list")
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.debug("Aquiring public IP for network: %s" % self.network.id)

        ip_with_lb_rule = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.network.id)

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
            ip_with_lb_rule.ipaddress.ipaddress)

        self.services["lbrule"]["alg"] = 'roundrobin'
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=ip_with_lb_rule.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            id=lb_rule.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "List LB rules should return a newly created LB rule"
        )
        self.debug("Adding %s to the LB rule %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])

    @attr(tags=["ncc"], required_hardware="true")
    def test_02_dedicated_another_network(self):
        # Create network
        self.network = Network.create(
            self.apiclient,
            self.services["network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id
        )
        self.debug("Created network: %s" % self.network.id)

        self.debug("Trying VM deploy with network created on account: %s" % self.account.name)

        with self.assertRaises(Exception):
            self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=self.network.id,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id
            )
        return
