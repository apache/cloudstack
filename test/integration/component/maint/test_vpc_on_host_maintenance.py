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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Host,
                             VPC,
                             VpcOffering)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
import time

class TestVPCHostMaintenance(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPCHostMaintenance, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []
        cls.hosts = []
        cls.vpcSupported = True
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv']:
            cls.vpcSupported = False
            return
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["mode"] = cls.zone.networktype

        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"]
        )
        cls.vpc_off.update(cls.api_client, state='Enabled')
        cls.hosts = Host.list(
            cls.api_client,
            zoneid=cls.zone.id,
            listall=True,
            type='Routing'
        )

        if isinstance(cls.hosts, list):
            for host in cls.hosts:
                Host.enableMaintenance(
                    cls.api_client,
                    id=host.id
                )

                timeout = cls.services["timeout"]
                while True:
                    time.sleep(cls.services["sleep"])
                    hosts_states = Host.list(
                        cls.api_client,
                        id=host.id,
                        listall=True
                    )
                    if hosts_states[
                            0].resourcestate == 'PrepareForMaintenance':
                        # Wait for sometimetill host goes in maintenance state
                        time.sleep(cls.services["sleep"])
                    elif hosts_states[0].resourcestate == 'Maintenance':
                        time.sleep(cls.services["sleep"])
                        break
                    elif timeout == 0:
                        raise unittest.SkipTest(
                            "Failed to enable maintenance mode on %s" %
                            host.name)
                    timeout = timeout - 1

        cls._cleanup.append(cls.vpc_off)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            for host in cls.hosts:
                Host.cancelMaintenance(
                    cls.api_client,
                    id=host.id
                )
                hosts_states = Host.list(
                    cls.api_client,
                    id=host.id,
                    listall=True
                )
                if hosts_states[0].resourcestate != 'Enabled':
                    raise Exception(
                        "Failed to cancel maintenance mode on %s" %
                        (host.name))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if not self.vpcSupported:
            self.skipTest("VPC is not supported on %s" % self.hypervisor)

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def validate_vpc_offering(self, vpc_offering):
        """Validates the VPC offering"""

        self.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
            self.apiclient,
            id=vpc_offering.id
        )
        self.assertEqual(
            isinstance(vpc_offs, list),
            True,
            "List VPC offerings should return a valid list"
        )
        self.assertEqual(
            vpc_offering.name,
            vpc_offs[0].name,
            "Name of the VPC offering should match with listVPCOff data"
        )
        self.debug(
            "VPC offering is created successfully - %s" %
            vpc_offering.name)
        return

    def validate_vpc_network(self, network, state=None):
        """Validates the VPC network"""

        self.debug("Check if the VPC network is created successfully?")
        vpc_networks = VPC.list(
            self.apiclient,
            id=network.id
        )
        self.assertEqual(
            isinstance(vpc_networks, list),
            True,
            "List VPC network should return a valid list"
        )
        self.assertEqual(
            network.name,
            vpc_networks[0].name,
            "Name of the VPC network should match with listVPC data"
        )
        if state:
            self.assertEqual(
                vpc_networks[0].state.lower(),
                state,
                "VPC state should be '%s'" % state
            )
        self.debug("VPC network validated - %s" % network.name)
        return

    @attr(tags=["advanced", "intervlan"])
    def test_01_create_vpc_host_maintenance(self):
        """ Test VPC when host is in maintenance mode
        """

        # Validate the following
        # 1. Put the host in maintenance mode.
        # 2. Attempt to Create a VPC with cidr - 10.1.1.1/16
        # 3. VPC will be created but will be in "Disabled" state

        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            start=False
        )
        self.validate_vpc_network(vpc, state='enabled')
        return
