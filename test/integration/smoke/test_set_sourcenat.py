# Licensed to the Apache Software Foundation (ASF) under one
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
""" BVT tests for Network Life Cycle
"""
# Import Local Modules
from marvin.codes import (FAILED, STATIC_NAT_RULE, LB_RULE,
                          NAT_RULE, PASS)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             VPC,
                             VpcOffering,
                             ServiceOffering,
                             PublicIPAddress,
                             Network,
                             NetworkOffering)
from marvin.lib.common import (get_domain,
                               get_free_vlan,
                               get_zone,
                               get_template,
                               get_test_template,
                               list_publicIP)

from nose.plugins.attrib import attr
# Import System modules
import time
import logging

_multiprocess_shared_ = True

logger = logging.getLogger('TestSetSourceNatIp')
stream_handler = logging.StreamHandler()
logger.setLevel(logging.DEBUG)
logger.addHandler(stream_handler)


class TestSetSourceNatIp(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSetSourceNatIp, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        # Create Accounts & networks
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["network"]["zoneid"] = cls.zone.id

        cls.vpc_offering = VpcOffering.create(
            cls.apiclient,
            cls.services["vpc_offering"],
        )
        cls._cleanup.append(cls.vpc_offering)
        cls.vpc_offering.update(cls.apiclient, state='Enabled')
        cls.services["vpc"]["vpcoffering"] = cls.vpc_offering.id

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
        )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.services["network"]["networkoffering"] = cls.network_offering.id

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"],
        )
        cls._cleanup.append(cls.service_offering)

        cls.hypervisor = testClient.getHypervisorInfo()
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        network = Network.create(
            cls.apiclient,
            cls.services["network"],
            cls.account.name,
            cls.account.domainid
        )
        cls._cleanup.append(network)
        ip_address = PublicIPAddress.create(
            cls.apiclient,
            cls.account.name,
            cls.zone.id,
            cls.account.domainid
        )
        cls._cleanup.append(ip_address)
        cls.ip_to_want = ip_address.ipaddress.ipaddress
        cls.debug(f'==== my local ip: {cls.ip_to_want}')
        ip_address.delete(cls.apiclient)
        cls._cleanup.remove(ip_address)
        network.delete(cls.apiclient)
        cls._cleanup.remove(network)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestSetSourceNatIp, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestSetSourceNatIp, self).tearDown()

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_01_create_network_with_specified_source_nat_ip_address(self):
        """
        For creation of network witjh a specified address
        """


        self.services["network"]["networkoffering"] = self.network_offering.id
        network = Network.create(
            self.apiclient,
            self.services["network"],
            self.account.name,
            self.account.domainid,
            sourcenatipaddress = self.ip_to_want
        )
        self.cleanup.append(network)

        self.validate_source_nat(network=network, ip=self.ip_to_want)


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_02_change_source_nat_ip_address_for_network(self):
        """
        Test changing a networks source NAT IP address
        """
        network = Network.create(
            self.apiclient,
            self.services["network"],
            self.account.name,
            self.account.domainid,
            sourcenatipaddress = self.ip_to_want
        )
        self.cleanup.append(network)
        second_ip = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            networkid=network.id
        )
        self.cleanup.append(second_ip)
        self.debug(f'==== second ip: {second_ip.ipaddress.ipaddress}')

        self.validate_source_nat(network=network, ip=self.ip_to_want)

        network.update(self.apiclient, sourcenatipaddress=second_ip.ipaddress.ipaddress)

        self.validate_source_nat(network=network, ip=second_ip.ipaddress.ipaddress)

        return


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_03_create_vpc_with_specified_source_nat_ip_address(self):
        """
        Test for creation of a VPC with a specified address
        """

        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            self.vpc_offering.id,
            self.zone.id,
            sourcenatipaddress = self.ip_to_want
        )
        self.cleanup.append(vpc)

        self.validate_source_nat(vpc=vpc, ip=self.ip_to_want)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="false")
    def test_04_change_source_nat_ip_address_for_vpc(self):
        """
        Test changing a networks source NAT IP address
        """
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            self.vpc_offering.id,
            self.zone.id,
            account=self.account.name,
            domainid = self.account.domainid,
            sourcenatipaddress = self.ip_to_want
        )
        self.cleanup.append(vpc)
        second_ip = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            vpcid=vpc.id
        )
        self.debug(f'==== second ip: {second_ip.ipaddress.ipaddress}')

        self.validate_source_nat(vpc=vpc, ip=self.ip_to_want)

        vpc.update(self.apiclient, sourcenatipaddress=second_ip.ipaddress.ipaddress)

        self.validate_source_nat(vpc=vpc, ip=second_ip.ipaddress.ipaddress)

        return


    def validate_source_nat(self, network=None, vpc=None, ip=None):
        list_pub_ip_addr_resp = None
        if network:
            list_pub_ip_addr_resp = list_publicIP(
                self.apiclient,
                associatednetworkid=network.id,
                listall=True,
                issourcenat=True
            )
        elif vpc:
            list_pub_ip_addr_resp = list_publicIP(
                self.apiclient,
                vpcid=vpc.id,
                listall=True,
                issourcenat=True
            )
        self.assertEqual(
            isinstance(list_pub_ip_addr_resp, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_pub_ip_addr_resp),
            0,
            "Check if new IP Address is associated"
        )
        self.debug(f'==== my result {list_pub_ip_addr_resp[0]}')
        self.assertEqual(
            list_pub_ip_addr_resp[0].ipaddress,
            ip,
            f"Check Correct IP Address is returned in the List, expected {ip} but got {list_pub_ip_addr_resp[0].ipaddress}"
        )
