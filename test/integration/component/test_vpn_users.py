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

""" P1 tests for VPN users
"""
from nose.plugins.attrib import attr

# Import Local Modules
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import PASS
from marvin.lib.base import (
    Account,
    ServiceOffering,
    VirtualMachine,
    PublicIPAddress,
    Vpn,
    VpnUser,
    Configurations,
    NATRule
)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template
                               )
from marvin.lib.utils import validateList


class Services:
    """Test VPN users Services
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
            "disk_offering": {
                "displaytext": "Small Disk Offering",
                "name": "Small Disk Offering",
                "disksize": 1
            },
            "virtual_machine": {
                "displayname": "TestVM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'KVM',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "vpn_user": {
                "username": "test",
                "password": "p@as=s.w_o-r+d",
            },
            "natrule": {
                "privateport": 1701,
                "publicport": 1701,
                "protocol": "UDP"
            },
            "ostype": 'CentOS 5.5 (64-bit)',
            "sleep": 60,
            "timeout": 10,
            # Networking mode: Advanced, Basic
        }


class TestVPNUsers(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPNUsers, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls._cleanup = []

        cls.services["mode"] = cls.zone.networktype

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestVPNUsers, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []
        try:
            self.apiclient = self.testClient.getApiClient()
            self.dbclient = self.testClient.getDbConnection()
            self.account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=self.domain.id
            )
            self.cleanup.append(self.account)

            self.virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id
            )
            self.cleanup.append(self.virtual_machine)
            self.public_ip = PublicIPAddress.create(
                self.apiclient,
                accountid=self.virtual_machine.account,
                zoneid=self.virtual_machine.zoneid,
                domainid=self.virtual_machine.domainid,
                services=self.services["virtual_machine"]
            )
            self.cleanup.append(self.public_ip)
            return
        except CloudstackAPIException as e:
            self.tearDown()
            raise e

    def tearDown(self):
        super(TestVPNUsers, self).tearDown()

    def create_VPN(self, public_ip):
        """Creates VPN for the network"""

        self.debug("Creating VPN with public IP: %s" % public_ip.ipaddress.id)
        try:
            # Assign VPN to Public IP
            vpn = Vpn.create(self.apiclient,
                             self.public_ip.ipaddress.id,
                             account=self.account.name,
                             domainid=self.account.domainid)
            self.cleanup.append(vpn)

            self.debug("Verifying the remote VPN access")
            vpns = Vpn.list(self.apiclient,
                            publicipid=public_ip.ipaddress.id,
                            listall=True)
            self.assertEqual(
                isinstance(vpns, list),
                True,
                "List VPNs shall return a valid response"
            )
            return vpn
        except Exception as e:
            self.fail("Failed to create remote VPN access: %s" % e)

    def create_VPN_Users(self, rand_name=True, api_client=None):
        """Creates VPN users for the network"""

        self.debug("Creating VPN users for account: %s" %
                   self.account.name)
        if api_client is None:
            api_client = self.apiclient
        try:
            vpnuser = VpnUser.create(
                api_client,
                self.services["vpn_user"]["username"],
                self.services["vpn_user"]["password"],
                account=self.account.name,
                domainid=self.account.domainid,
                rand_name=rand_name
            )
            self.cleanup.append(vpnuser)

            self.debug("Verifying the remote VPN access")
            vpn_users = VpnUser.list(self.apiclient,
                                     id=vpnuser.id,
                                     listall=True)
            self.assertEqual(
                isinstance(vpn_users, list),
                True,
                "List VPNs shall return a valid response"
            )
            return vpnuser
        except Exception as e:
            self.fail("Failed to create remote VPN users: %s" % e)

    @attr(tags=["advanced", "advancedns"])
    @attr(configuration='remote.access.vpn.user.limit')
    def test_01_VPN_user_limit(self):
        """VPN remote access user limit tests"""

        # Validate the following
        # prerequisite: change management configuration setting of
        #    remote.access.vpn.user.limit
        # 1. provision more users than is set in the limit
        #    Provisioning of users after the limit should failProvisioning of
        #    users after the limit should fail

        self.debug("Fetching the limit for remote access VPN users")
        configs = Configurations.list(
            self.apiclient,
            name='remote.access.vpn.user.limit',
            listall=True)
        self.assertEqual(isinstance(configs, list),
                         True,
                         "List configs should return a valid response")

        limit = int(configs[0].value)

        self.debug("Enabling the VPN access for IP: %s" %
                   self.public_ip.ipaddress)

        self.create_VPN(self.public_ip)
        self.debug("Creating %s VPN users" % limit)
        for x in range(limit):
            self.create_VPN_Users()

        self.debug("Adding another user exceeding limit for remote VPN users")
        with self.assertRaises(Exception):
            self.create_VPN_Users()
        self.debug("Limit exceeded exception raised!")
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_02_use_vpn_port(self):
        """Test create VPN when L2TP port in use"""

        # Validate the following
        # 1. set a port forward for UDP: 1701 and enable VPN
        # 2. set port forward rule for the udp port 1701 over which L2TP works
        # 3. port forward should prevent VPN from being enabled

        self.debug("Creating a port forwarding rule on port 1701")
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            self.public_ip.ipaddress.id)
        self.cleanup.append(nat_rule)
        self.debug("Verifying the NAT rule created")
        nat_rules = NATRule.list(self.apiclient, id=nat_rule.id, listall=True)

        self.assertEqual(isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return a valid response")

        self.debug("Enabling the VPN connection for IP: %s" %
                   self.public_ip.ipaddress)
        with self.assertRaises(Exception):
            self.create_VPN(self.public_ip)
        self.debug("Create VPN connection failed! Test successful!")
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_03_enable_vpn_use_port(self):
        """Test create NAT rule when VPN when L2TP enabled"""

        # Validate the following
        # 1. Enable a VPN connection on source NAT
        # 2. Add a VPN user
        # 3. add a port forward rule for UDP port 1701.  Should result in error
        #    saying that VPN is enabled over port 1701

        self.debug("Enabling the VPN connection for IP: %s" %
                   self.public_ip.ipaddress)
        self.create_VPN(self.public_ip)

        self.debug("Creating a port forwarding rule on port 1701")
        # Create NAT rule
        with self.assertRaises(Exception):
            NATRule.create(
                self.apiclient,
                self.virtual_machine,
                self.services["natrule"],
                self.public_ip.ipaddress.id)

        self.debug("Create NAT rule failed! Test successful!")
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_04_add_new_users(self):
        """Test add new users to existing VPN"""

        # Validate the following
        # 1. Enable a VPN connection on source NAT
        # 2. Add new user to VPN when there are already existing users.
        # 3. We should be able to successfully establish a VPN connection using
        #    the newly added user credential.

        self.debug("Enabling the VPN connection for IP: %s" %
                   self.public_ip.ipaddress)
        self.create_VPN(self.public_ip)

        try:
            self.debug("Adding new VPN user to account: %s" %
                       self.account.name)
            self.create_VPN_Users()

            # TODO: Verify the VPN connection
            self.debug("Adding another user to account")
            self.create_VPN_Users()

            # TODO: Verify the VPN connection with new user
        except Exception as e:
            self.fail("Failed to create new VPN user: %s" % e)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_05_add_duplicate_user(self):
        """Test add duplicate user to existing VPN"""

        # Validate the following
        # 1. Enable a VPN connection on source NAT
        # 2. Add a VPN user say "abc"  that already an added user to the VPN.
        # 3. Adding this VPN user should fail.

        self.debug("Enabling the VPN connection for IP: %s" %
                   self.public_ip.ipaddress)
        self.create_VPN(self.public_ip)

        self.debug("Adding new VPN user to account: %s" %
                   self.account.name)
        self.create_VPN_Users(rand_name=False)

        # TODO: Verify the VPN connection
        self.debug("Adding another user to account with same username")
        with self.assertRaises(Exception):
            self.create_VPN_Users(rand_name=False)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_06_add_VPN_user_global_admin(self):
        """Test as global admin, add a new VPN user to an existing VPN entry
            that was created by another account."""

        # Steps for verification
        # 1. Create a new user and deploy few Vms.
        # 2. Enable VPN access. Add few VPN users.
        # 3. Make sure that VPN access works as expected.
        # 4. As global Admin , add VPN user to this user's existing VPN entry.
        #  Validate the following
        # 1. The newly added VPN user should get configured to the router of
        #    user account.
        # 2. We should be able to use this newly created user credential to
        #   establish VPN connection that will give access all VMs of this user

        self.debug("Enabling VPN connection to account: %s" %
                   self.account.name)
        self.create_VPN(self.public_ip)
        self.debug("Creating VPN user for the account: %s" %
                   self.account.name)
        self.create_VPN_Users()

        self.debug("Creating a global admin account")
        admin = Account.create(self.apiclient,
                               self.services["account"],
                               admin=True,
                               domainid=self.account.domainid)
        self.cleanup.append(admin)
        self.debug("Creating API client for newly created user")
        api_client = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)

        self.debug("Adding new user to VPN as a global admin: %s" %
                   admin.name)
        try:
            self.create_VPN_Users(api_client=api_client)
        except Exception as e:
            self.fail("Global admin should be allowed to create VPN user: %s" %
                      e)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_07_add_VPN_user_domain_admin(self):
        """Test as domain admin, add a new VPN user to an existing VPN entry
            that was created by another account."""

        # Steps for verification
        # 1. Create a new user and deploy few Vms.
        # 2. Enable VPN access. Add few VPN users.
        # 3. Make sure that VPN access works as expected.
        # 4. As domain Admin , add VPN user to this user's existing VPN entry.
        #  Validate the following
        # 1. The newly added VPN user should get configured to the router of
        #    user account.
        # 2. We should be able to use this newly created user credential to
        #   establish VPN connection that will give access all VMs of this user

        self.debug("Enabling VPN connection to account: %s" %
                   self.account.name)
        self.create_VPN(self.public_ip)
        self.debug("Creating VPN user for the account: %s" %
                   self.account.name)
        self.create_VPN_Users()

        self.debug("Creating a domain admin account")
        admin = Account.create(self.apiclient,
                               self.services["account"],
                               domainid=self.account.domainid)
        self.cleanup.append(admin)
        self.debug("Creating API client for newly created user")
        api_client = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)

        self.debug("Adding new user to VPN as a domain admin: %s" %
                   admin.name)
        try:
            self.create_VPN_Users(api_client=api_client)
        except Exception as e:
            self.fail("Domain admin should be allowed to create VPN user: %s" %
                      e)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_08_add_TCP_PF_Rule_In_VPN(self):
        """
        Test to add TCP Port Forwarding rule for specific ports(500,1701 and 4500) in VPN
        """
        # Steps for verification
        # 1. Enable vpn on SourceNAT IP address
        # 2. Configure PF with TCP ports 500,1701 and 4500. It should be allowed
        # Should not conflict with UPD ports used for VPN

        vm_res = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            validateList(vm_res)[0],
            PASS,
            "Failed to list virtual machine"
        )
        network_id = vm_res[0].nic[0].networkid
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True,
            issourcenat=True,
            associatednetworkid=network_id
        )
        self.assertEqual(
            validateList(src_nat_list)[0],
            PASS,
            "Failed to list source nat ip address"
        )
        ip = src_nat_list[0]
        try:
            vpn = Vpn.create(
                self.apiclient,
                publicipid=ip.id,
                account=self.account.name,
                domainid=self.account.domainid,
            )
            self.assertIsNotNone(
                vpn,
                "Failed to create remote access vpn"
            )
            self.cleanup.append(vpn)
        except Exception as e:
            self.fail("Failed to enable vpn on SourceNAT IP with error: %s" % e)

        # Create PF rule with TCP ports 500,4500 and 1701
        self.services['natrule']['protocol'] = "TCP"
        for port in [500, 4500, 1701]:
            self.services['natrule']['privateport'] = port
            self.services['natrule']['publicport'] = port
            try:
                nat = NATRule.create(
                    self.apiclient,
                    self.virtual_machine,
                    self.services["natrule"],
                    ip.id
                )
                self.assertIsNotNone(
                    nat,
                    "Failed to add PF rule with tcp parts matching vpn"
                )
                self.cleanup.append(nat)
            except Exception as e:
                self.fail("Creating PF rule for TCP port %s in VPN failed : %s" % (port, e))
        return
