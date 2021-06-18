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

""" P1 tests for elastic load balancing and elastic IP
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (authorizeSecurityGroupIngress,
                                  disassociateIpAddress,
                                  deleteLoadBalancerRule)
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             PublicIPAddress,
                             VirtualMachine,
                             Network,
                             LoadBalancerRule,
                             SecurityGroup,
                             ServiceOffering,
                             StaticNATRule,
                             PublicIpRange)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template)
from marvin.sshClient import SshClient
import time


class Services:

    """Test elastic load balancing and elastic IP
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,  # in MHz
                "memory": 128,  # In MBs
            },
            "lbrule": {
                "name": "SSH",
                "alg": "roundrobin",
                # Algorithm used for load balancing
                "privateport": 22,
                "publicport": 22,
                "openfirewall": False,
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP"
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
        }


class TestEIP(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestEIP, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        try:
            cls.services["netscaler"] = cls.config.__dict__[
                "netscalerDevice"].__dict__
        except KeyError:
            raise unittest.SkipTest("Please make sure you have included netscalerDevice\
                    dict in your config file (keys - ipaddress, username,\
                    password")
        except Exception as e:
            raise unittest.SkipTest(e)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        # Spawn an instance
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        networks = Network.list(
            cls.api_client,
            zoneid=cls.zone.id,
            listall=True
        )
        if isinstance(networks, list):
            # Basic zone has only one network i.e Basic network
            cls.guest_network = networks[0]
        else:
            raise Exception(
                "List networks returned empty response for zone: %s" %
                cls.zone.id)

        ip_addrs = PublicIPAddress.list(
            cls.api_client,
            associatednetworkid=cls.guest_network.id,
            isstaticnat=True,
            account=cls.account.name,
            domainid=cls.account.domainid,
            listall=True
        )
        if isinstance(ip_addrs, list):
            cls.source_nat = ip_addrs[0]
            print("source_nat ipaddress : ", cls.source_nat.ipaddress)
        else:
            raise Exception(
                "No Source NAT IP found for guest network: %s" %
                cls.guest_network.id)
        cls._cleanup = [
            cls.account,
            cls.service_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["eip"])
    def test_01_eip_by_deploying_instance(self):
        """Test EIP by deploying an instance
        """

        # Validate the following
        # 1. Instance gets an IP from GUEST IP range.
        # 2. One IP from EIP pool is taken and configured on NS
        #    commands to verify on NS:
        #    show ip, show inat- make sure that output says USIP : ON
        # 3. After allowing ingress rule based on source CIDR to the
        #    respective port, verify that you are able to reach guest with EIP
        # 4. user_ip_address.is_system=1, user_ip_address.one_to_one_nat=1

        self.debug("Fetching public network IP range for public network")
        ip_ranges = PublicIpRange.list(
            self.apiclient,
            zoneid=self.zone.id,
            forvirtualnetwork=True
        )
        self.assertEqual(
            isinstance(ip_ranges, list),
            True,
            "Public IP range should return a valid range"
        )
        # Guest network can have multiple IP ranges. In that case, split IP
        # address and then compare the values
        for ip_range in ip_ranges:
            self.debug("IP range: %s - %s" % (
                ip_range.startip,
                ip_range.endip
            ))

            start_ip_list = ip_range.startip.split(".")
            end_ip_list = ip_range.endip.split(".")
            source_nat_list = self.source_nat.ipaddress.split(".")

            self.assertGreaterEqual(
                int(source_nat_list[3]),
                int(start_ip_list[3]),
                "The NAT should be greater/equal to start IP of guest network"
            )

            self.assertLessEqual(
                int(source_nat_list[3]),
                int(end_ip_list[3]),
                "The NAT should be less/equal to start IP of guest network"
            )

        # Verify listSecurity groups response
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(security_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertEqual(
            len(security_groups),
            1,
            "Check List Security groups response"
        )
        self.debug("List Security groups response: %s" %
                   str(security_groups))

        security_group = security_groups[0]

        self.debug(
            "Creating Ingress rule to allow SSH on default security group")

        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = '0.0.0.0/0'
        self.apiclient.authorizeSecurityGroupIngress(cmd)
# COMMENTED:
#        try:
#            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
#            ssh = self.virtual_machine.get_ssh_client(
#                                        ipaddress=self.source_nat.ipaddress)
#        except Exception as e:
#            self.fail("SSH Access failed for %s: %s" % \
#                      (self.virtual_machine.ipaddress, e)
#                      )
        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            self.source_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            self.source_nat.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            1,
            "user_ip_address.is_system value should be 1 for static NAT"
        )

        self.assertEqual(
            qresult[1],
            1,
            "user_ip_address.one_to_one_nat value should be 1 for static NAT"
        )
        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])

        ssh_client = SshClient(
            self.services["netscaler"]["ipaddress"],
            22,
            self.services["netscaler"]["username"],
            self.services["netscaler"]["password"],
        )
        self.debug("command: show ip")
        res = ssh_client.execute("show ip")
        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(self.source_nat.ipaddress),
            1,
            "One IP from EIP pool should be taken and configured on NS"
        )

        self.debug("Command:show inat")
        res = ssh_client.execute("show inat")

        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(
                "NAME: Cloud-Inat-%s" %
                self.source_nat.ipaddress),
            1,
            "User source IP should be enabled for INAT service")
        return

    @attr(tags=["eip"])
    def test_02_acquire_ip_enable_static_nat(self):
        """Test associate new IP and enable static NAT for new IP and the VM
        """

        # Validate the following
        # 1. user_ip_address.is_system = 0 & user_ip_address.one_to_one_nat=1
        # 2. releases default EIP whose user_ip_address.is_system=1
        # 3. After allowing ingress rule based on source CIDR to the
        #    respective port, verify that you are able to reach guest with EIP
        # 4. check configuration changes for EIP reflects on NS
        #    commands to verify on NS :
        #    * "show ip"
        #    * "show inat" - make sure that output says USIP : ON

        self.debug("Acquiring new IP for network: %s" % self.guest_network.id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            services=self.services["virtual_machine"]
        )
        self.debug("IP address: %s is acquired by network: %s" % (
            public_ip.ipaddress.ipaddress,
            self.guest_network.id))
        self.debug("Enabling static NAT for IP Address: %s" %
                   public_ip.ipaddress.ipaddress)

        StaticNATRule.enable(
            self.apiclient,
            ipaddressid=public_ip.ipaddress.id,
            virtualmachineid=self.virtual_machine.id
        )

        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            public_ip.ipaddress.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            public_ip.ipaddress.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            0,
            "user_ip_address.is_system value should be 0 for new IP"
        )

        self.assertEqual(
            qresult[1],
            1,
            "user_ip_address.one_to_one_nat value should be 1 for static NAT"
        )

        self.debug(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            self.source_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            self.source_nat.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            0,
            "user_ip_address.is_system value should be 0 old source NAT"
        )

#        try:
#            self.debug("SSH into VM: %s" % public_ip.ipaddress)
#            ssh = self.virtual_machine.get_ssh_client(
#                                    ipaddress=public_ip.ipaddress)
#        except Exception as e:
#            self.fail("SSH Access failed for %s: %s" % \
#                      (public_ip.ipaddress, e)
#                      )

        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.services["netscaler"]["ipaddress"],
                22,
                self.services["netscaler"]["username"],
                self.services["netscaler"]["password"],
            )
            self.debug("command: show ip")
            res = ssh_client.execute("show ip")
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(public_ip.ipaddress.ipaddress),
                1,
                "One IP from EIP pool should be taken and configured on NS"
            )

            self.debug("Command:show inat")
            res = ssh_client.execute("show inat")

            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(
                    "NAME: Cloud-Inat-%s" %
                    public_ip.ipaddress.ipaddress),
                1,
                "User source IP should be enabled for INAT service")

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["eip"])
    def test_03_disable_static_nat(self):
        """Test disable static NAT and release EIP acquired
        """

        # Validate the following
        # 1. Disable static NAT. Disables one-to-one NAT and releases EIP
        #    whose user_ip_address.is_system=0
        # 2. Gets a new ip from EIP pool whose user_ip_address.is_system=1
        #    and user_ip_address.one_to_one_nat=1
        # 3. DisassicateIP should mark this EIP whose is_system=0 as free.
        #    commands to verify on NS :
        #    * "show ip"
        #    * "show inat"-make sure that output says USIP : ON

        self.debug(
            "Fetching static NAT for VM: %s" % self.virtual_machine.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            associatednetworkid=self.guest_network.id,
            isstaticnat=True,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )
        static_nat = ip_addrs[0]
        self.debug("Static NAT for VM: %s is: %s" % (
            self.virtual_machine.name,
            static_nat.ipaddress
        ))

        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            static_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            static_nat.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            0,
            "user_ip_address.is_system value should be 0"
        )

        self.debug(
            "Disassociate Static NAT: %s" %
            static_nat.ipaddress)
        cmd = disassociateIpAddress.disassociateIpAddressCmd()
        cmd.id = static_nat.id
        self.apiclient.disassociateIpAddress(cmd)

        self.debug("Sleeping - after disassociating static NAT")
        time.sleep(self.services["sleep"])

        # Fetch details from user_ip_address table in database
        self.debug(
            "select state from user_ip_address where public_ip_address='%s';"
            % static_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select state from user_ip_address where public_ip_address='%s';"
            % static_nat.ipaddress
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            "Free",
            "Ip should be marked as Free after disassociate IP"
        )

        self.debug(
            "Fetching static NAT for VM: %s" % self.virtual_machine.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            associatednetworkid=self.guest_network.id,
            isstaticnat=True,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )
        static_nat = ip_addrs[0]
        self.debug("Static NAT for VM: %s is: %s" % (
            self.virtual_machine.name,
            static_nat.ipaddress
        ))

        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            static_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system, one_to_one_nat from user_ip_address\
                    where public_ip_address='%s';" %
            static_nat.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            1,
            "is_system value should be 1 for automatically assigned IP"
        )
        self.assertEqual(
            qresult[1],
            1,
            "one_to_one_nat value should be 1 for automatically assigned IP"
        )
#        try:
#            self.debug("SSH into VM: %s" % static_nat.ipaddress)
#            ssh = self.virtual_machine.get_ssh_client(
#                                    ipaddress=static_nat.ipaddress)
#        except Exception as e:
#            self.fail("SSH Access failed for %s: %s" % \
#                                    (static_nat.ipaddress, e))

        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])

        ssh_client = SshClient(
            self.services["netscaler"]["ipaddress"],
            22,
            self.services["netscaler"]["username"],
            self.services["netscaler"]["password"],
        )
        self.debug("command: show ip")
        res = ssh_client.execute("show ip")
        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(static_nat.ipaddress),
            1,
            "One IP from EIP pool should be taken and configured on NS"
        )

        self.debug("Command:show inat")
        res = ssh_client.execute("show inat")

        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count("USIP: ON"),
            2,
            "User source IP should be enabled for INAT service"
        )
        return

    @attr(tags=["eip"])
    def test_04_disable_static_nat_system(self):
        """Test disable static NAT with system = True
        """

        # Validate the following
        # 1. Try to disassociate/disable static NAT on EIP where is_system=1
        # 2. This operation should fail with proper error message.

        self.debug(
            "Fetching static NAT for VM: %s" % self.virtual_machine.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            associatednetworkid=self.guest_network.id,
            isstaticnat=True,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )
        static_nat = ip_addrs[0]
        self.debug("Static NAT for VM: %s is: %s" % (
            self.virtual_machine.name,
            static_nat.ipaddress
        ))

        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            static_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            static_nat.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            1,
            "user_ip_address.is_system value should be 1"
        )

        self.debug(
            "Disassociate Static NAT: %s" %
            static_nat.ipaddress)

        with self.assertRaises(Exception):
            cmd = disassociateIpAddress.disassociateIpAddressCmd()
            cmd.id = static_nat.id
            self.api_client.disassociateIpAddress(cmd)

        self.debug("Disassociate system IP failed")
        return

    @attr(tags=["eip"])
    def test_05_destroy_instance(self):
        """Test EIO after destroying instance
        """

        # Validate the following
        # 1. Destroy instance. Destroy should result in is_system=0 for EIP
        #    and EIP should also be marked as free.
        # 2. Commands to verify on NS :
        #    * "show ip"
        #    * "show inat" - make sure that output says USIP: ON

        self.debug(
            "Fetching static NAT for VM: %s" % self.virtual_machine.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            associatednetworkid=self.guest_network.id,
            isstaticnat=True,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )
        static_nat = ip_addrs[0]
        self.debug("Static NAT for VM: %s is: %s" % (
            self.virtual_machine.name,
            static_nat.ipaddress
        ))

        self.debug("Destroying an instance: %s" % self.virtual_machine.name)
        self.virtual_machine.delete(self.apiclient, expunge=True)
        self.debug("Destroy instance complete!")

        vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            vms,
            None,
            "list VM should not return anything after destroy"
        )
        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system, state from user_ip_address where\
                    public_ip_address='%s';" %
            static_nat.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system, state from user_ip_address where\
                    public_ip_address='%s';" %
            static_nat.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            0,
            "user_ip_address.is_system value should be 0"
        )

        self.assertEqual(
            qresult[1],
            "Free",
            "IP should be marked as Free after destroying VM"
        )
        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])

        ssh_client = SshClient(
            self.services["netscaler"]["ipaddress"],
            22,
            self.services["netscaler"]["username"],
            self.services["netscaler"]["password"],
        )
        self.debug("command: show ip")
        res = ssh_client.execute("show ip")
        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(static_nat.ipaddress),
            0,
            "show ip should return nothing after VM destroy"
        )

        self.debug("Command:show inat")
        res = ssh_client.execute("show inat")

        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(static_nat.ipaddress),
            0,
            "show inat should return nothing after VM destroy"
        )
        return


class TestELB(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestELB, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        try:
            cls.services["netscaler"] = cls.config.__dict__[
                "netscalerDevice"].__dict__
        except KeyError:
            raise unittest.SkipTest("Please make sure you have included netscalerDevice\
                    dict in your config file (keys - ipaddress, username,\
                    password")
        except Exception as e:
            raise unittest.SkipTest(e)

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        # Spawn an instance
        cls.vm_1 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls.vm_2 = VirtualMachine.create(
            cls.api_client,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        networks = Network.list(
            cls.api_client,
            zoneid=cls.zone.id,
            listall=True
        )
        if isinstance(networks, list):
            # Basic zone has only one network i.e Basic network
            cls.guest_network = networks[0]
        else:
            raise Exception(
                "List networks returned empty response for zone: %s" %
                cls.zone.id)
        cls.lb_rule = LoadBalancerRule.create(
            cls.api_client,
            cls.services["lbrule"],
            accountid=cls.account.name,
            networkid=cls.guest_network.id,
            domainid=cls.account.domainid
        )
        cls.lb_rule.assign(cls.api_client, [cls.vm_1, cls.vm_2])

        cls._cleanup = [
            cls.account,
            cls.service_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["eip"])
    def test_01_elb_create(self):
        """Test ELB by creating a LB rule
        """

        # Validate the following
        # 1. Deploy 2 instances
        # 2. Create LB rule to port 22 for the VMs and try to access VMs with
        #    EIP:port. Make sure that ingress rule is created to allow access
        #    with universal CIDR (0.0.0.0/0)
        # 3. For LB rule IP user_ip_address.is_system=1
        # 4. check configuration changes for EIP reflects on NS
        #    commands to verify on NS :
        #    * "show ip"
        #    * "show lb vserer"-make sure that output says they are all up
        #    and running and USNIP : ON

        # Verify listSecurity groups response
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(security_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertEqual(
            len(security_groups),
            1,
            "Check List Security groups response"
        )
        self.debug("List Security groups response: %s" %
                   str(security_groups))

        security_group = security_groups[0]

        self.debug(
            "Creating Ingress rule to allow SSH on default security group")

        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = '0.0.0.0/0'
        self.apiclient.authorizeSecurityGroupIngress(cmd)

        self.debug(
            "Fetching LB IP for account: %s" % self.account.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            associatednetworkid=self.guest_network.id,
            account=self.account.name,
            domainid=self.account.domainid,
            forloadbalancing=True,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )

        lb_ip = ip_addrs[0]
        self.debug("LB IP generated for account: %s is: %s" % (
            self.account.name,
            lb_ip.ipaddress
        ))
# TODO: uncomment this after ssh issue is resolved
#        self.debug("SSHing into VMs using ELB IP: %s" % lb_ip.ipaddress)
#        try:
#            ssh_1 = self.vm_1.get_ssh_client(ipaddress=lb_ip.ipaddress)
#            self.debug("Command: hostname")
#            result = ssh_1.execute("hostname")
#            self.debug("Result: %s" % result)
#
#            if isinstance(result, list):
#                res = result[0]
#            else:
#                self.fail("hostname retrieval failed!")
#
#            self.assertIn(
#                          res,
#                          [self.vm_1.name, self.vm_2.name],
#                          "SSH should return hostname of one of the VM"
#                          )
#
#            ssh_2 = self.vm_2.get_ssh_client(ipaddress=lb_ip.ipaddress)
#            self.debug("Command: hostname")
#            result = ssh_2.execute("hostname")
#            self.debug("Result: %s" % result)
#
#            if isinstance(result, list):
#                res = result[0]
#            else:
#                self.fail("hostname retrieval failed!")
#            self.assertIn(
#                          res,
#                          [self.vm_1.name, self.vm_2.name],
#                          "SSH should return hostname of one of the VM"
#                          )
#        except Exception as e:
#            self.fail(
#                "SSH Access failed for %s: %s" % (self.vm_1.ipaddress, e))

        # Fetch details from user_ip_address table in database
        self.debug(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            lb_ip.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            lb_ip.ipaddress)
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            1,
            "is_system value should be 1 for system generated LB rule"
        )
        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.services["netscaler"]["ipaddress"],
                22,
                self.services["netscaler"]["username"],
                self.services["netscaler"]["password"],
            )
            self.debug("command: show ip")
            res = ssh_client.execute("show ip")
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(lb_ip.ipaddress),
                1,
                "One IP from EIP pool should be taken and configured on NS"
            )

            self.debug("Command:show lb vserver")
            res = ssh_client.execute("show lb vserver")

            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(
                    "Cloud-VirtualServer-%s-22 (%s:22) - TCP" %
                    (lb_ip.ipaddress,
                     lb_ip.ipaddress)),
                1,
                "User subnet IP should be enabled for LB service")

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["eip"])
    def test_02_elb_acquire_and_create(self):
        """Test ELB by acquiring IP and then creating a LB rule
        """

        # Validate the following
        # 1. Deploy 2 instances
        # 2. Create LB rule to port 22 for the VMs and try to access VMs with
        #    EIP:port. Make sure that ingress rule is created to allow access
        #    with universal CIDR (0.0.0.0/0)
        # 3. For LB rule IP user_ip_address.is_system=0
        # 4. check configuration changes for EIP reflects on NS
        #    commands to verify on NS :
        #    * "show ip"
        #    * "show lb vserer" - make sure that output says they are all up
        #    and running and USNIP : ON

        self.debug("Acquiring new IP for network: %s" % self.guest_network.id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            services=self.services["virtual_machine"]
        )
        self.debug("IP address: %s is acquired by network: %s" % (
            public_ip.ipaddress.ipaddress,
            self.guest_network.id))

        self.debug("Creating LB rule for public IP: %s" %
                   public_ip.ipaddress.ipaddress)
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            accountid=self.account.name,
            ipaddressid=public_ip.ipaddress.id,
            networkid=self.guest_network.id,
            domainid=self.account.domainid
        )
        self.debug("Assigning VMs (%s, %s) to LB rule: %s" % (self.vm_1.name,
                                                              self.vm_2.name,
                                                              lb_rule.name))
        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
# TODO: workaround : add route in the guest VM for SNIP
#
#        self.debug("SSHing into VMs using ELB IP: %s" %
#                                                public_ip.ipaddress)
#        try:
#            ssh_1 = self.vm_1.get_ssh_client(
#                                    ipaddress=public_ip.ipaddress)
#            self.debug("Command: hostname")
#            result = ssh_1.execute("hostname")
#            self.debug("Result: %s" % result)
#
#            if isinstance(result, list):
#                res = result[0]
#            else:
#                self.fail("hostname retrieval failed!")
#            self.assertIn(
#                          res,
#                          [self.vm_1.name, self.vm_2.name],
#                          "SSH should return hostname of one of the VM"
#                          )
#
#            ssh_2 = self.vm_2.get_ssh_client(
#                                    ipaddress=public_ip.ipaddress)
#            self.debug("Command: hostname")
#            result = ssh_2.execute("hostname")
#            self.debug("Result: %s" % result)
#
#            if isinstance(result, list):
#                res = result[0]
#            else:
#                self.fail("hostname retrieval failed!")
#            self.assertIn(
#                          res,
#                          [self.vm_1.name, self.vm_2.name],
#                          "SSH should return hostname of one of the VM"
#                          )
#        except Exception as e:
#            self.fail(
#                "SSH Access failed for %s: %s" % (self.vm_1.ipaddress, e))
#
# Fetch details from user_ip_address table in database
        self.debug(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            public_ip.ipaddress.ipaddress)

        qresultset = self.dbclient.execute(
            "select is_system from user_ip_address where\
                    public_ip_address='%s';" %
            public_ip.ipaddress.ipaddress)

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        self.assertEqual(
            qresult[0],
            0,
            "is_system value should be 0 for non-system generated LB rule"
        )
        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.services["netscaler"]["ipaddress"],
                22,
                self.services["netscaler"]["username"],
                self.services["netscaler"]["password"],
            )
            self.debug("command: show ip")
            res = ssh_client.execute("show ip")
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(public_ip.ipaddress.ipaddress),
                1,
                "One IP from EIP pool should be taken and configured on NS"
            )

            self.debug("Command:show lb vserver")
            res = ssh_client.execute("show lb vserver")

            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count(
                    "Cloud-VirtualServer-%s-22 (%s:22) - TCP" %
                    (public_ip.ipaddress.ipaddress,
                     public_ip.ipaddress.ipaddress)),
                1,
                "User subnet IP should be enabled for LB service")

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["eip"])
    def test_03_elb_delete_lb_system(self):
        """Test delete LB rule generated with public IP with is_system = 1
        """

        # Validate the following
        # 1. Deleting LB rule should release EIP where is_system=1
        # 2. check configuration changes for EIP reflects on NS
        #    commands to verify on NS:
        #    * "show ip"
        #    * "show lb vserer"-make sure that output says they are all up and
        #    running and USNIP : ON

        self.debug(
            "Fetching LB IP for account: %s" % self.account.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            associatednetworkid=self.guest_network.id,
            account=self.account.name,
            domainid=self.account.domainid,
            forloadbalancing=True,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )

        lb_ip = ip_addrs[0]
        self.debug("LB IP generated for account: %s is: %s" % (
            self.account.name,
            lb_ip.ipaddress
        ))

        self.debug("Deleting LB rule: %s" % self.lb_rule.id)
        self.lb_rule.delete(self.apiclient)

        time.sleep(60)
        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])

        ssh_client = SshClient(
            self.services["netscaler"]["ipaddress"],
            22,
            self.services["netscaler"]["username"],
            self.services["netscaler"]["password"],
        )
        self.debug("command: show ip")
        res = ssh_client.execute("show ip")
        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(lb_ip.ipaddress),
            1,
            "One IP from EIP pool should be taken and configured on NS"
        )

        self.debug("Command:show lb vserver")
        res = ssh_client.execute("show lb vserver")

        result = str(res)
        self.debug("Output: %s" % result)

        self.assertEqual(
            result.count(
                "Cloud-VirtualServer-%s-22 (%s:22) - TCP" %
                (lb_ip.ipaddress,
                 lb_ip.ipaddress)),
            1,
            "User subnet IP should be enabled for LB service")
        return

    @attr(tags=["eip"])
    def test_04_delete_lb_on_eip(self):
        """Test delete LB rule generated on EIP
        """

        # Validate the following
        # 1. Deleting LB rule won't release EIP where is_system=0
        # 2. disassociateIP must release the above IP
        # 3. check configuration changes for EIP reflects on NS
        #    commands to verify on NS :
        #    * "show ip"
        #    * "show lb vserer"-make sure that output says they are all up and
        #    running and USNIP : ON

        # Fetch details from account_id table in database
        self.debug(
            "select id from account where account_name='%s';"
            % self.account.name)

        qresultset = self.dbclient.execute(
            "select id from account where account_name='%s';"
            % self.account.name)

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "DB query should return a valid public IP address"
        )
        qresult = qresultset[0]
        account_id = qresult[0]
        # Fetch details from user_ip_address table in database
        self.debug(
            "select public_ip_address from user_ip_address where\
                    is_system=0 and account_id=%s;" %
            account_id)

        qresultset = self.dbclient.execute(
            "select public_ip_address from user_ip_address where\
                    is_system=0 and account_id=%s;" %
            account_id)

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "DB query should return a valid public IP address"
        )
        qresult = qresultset[0]
        public_ip = qresult[0]

        self.debug(
            "Fetching public IP for account: %s" % self.account.name)
        ip_addrs = PublicIPAddress.list(
            self.api_client,
            ipaddress=public_ip,
            listall=True
        )
        self.debug("ip address list: %s" % ip_addrs)
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "List Public IP address should return valid IP address for network"
        )

        lb_ip = ip_addrs[0]

        lb_rules = LoadBalancerRule.list(
            self.apiclient,
            publicipid=lb_ip.id,
            listall=True
        )
        self.assertEqual(
            isinstance(lb_rules, list),
            True,
            "Atleast one LB rule must be present for public IP address"
        )
        lb_rule = lb_rules[0]
        self.debug("Deleting LB rule associated with IP: %s" % public_ip)

        try:
            cmd = deleteLoadBalancerRule.deleteLoadBalancerRuleCmd()
            cmd.id = lb_rule.id
            self.apiclient.deleteLoadBalancerRule(cmd)
        except Exception as e:
            self.fail("Deleting LB rule failed for IP: %s-%s" % (public_ip, e))

# TODO:check the lb rule list and then confirm that lb rule is deleted
        self.debug("LB rule deleted!")

        ip_addrs = PublicIPAddress.list(
            self.api_client,
            ipaddress=public_ip,
            listall=True
        )
        self.assertEqual(
            isinstance(ip_addrs, list),
            True,
            "Deleting LB rule should not delete public IP"
        )
        self.debug("SSH into netscaler: %s" %
                   self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.services["netscaler"]["ipaddress"],
                22,
                self.services["netscaler"]["username"],
                self.services["netscaler"]["password"],
            )
            self.debug("command: show ip")
            res = ssh_client.execute("show ip")
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertNotEqual(
                result.count(public_ip),
                1,
                "One IP from EIP pool should be taken and configured on NS"
            )

            self.debug("Command:show lb vserver")
            res = ssh_client.execute("show lb vserver")

            result = str(res)
            self.debug("Output: %s" % result)

            self.assertNotEqual(
                result.count(
                    "Cloud-VirtualServer-%s-22 (%s:22) - TCP" %
                    (lb_ip.ipaddress,
                     lb_ip.ipaddress)),
                1,
                "User subnet IP should be enabled for LB service")

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.services["netscaler"]["ipaddress"], e))
        return
