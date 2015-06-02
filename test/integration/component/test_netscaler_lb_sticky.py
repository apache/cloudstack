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

""" P1 tests for netscaler load balancing sticky policy
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             LoadBalancerRule,
                             PublicIPAddress,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               add_netscaler)
from marvin.sshClient import SshClient


class TestLbStickyPolicy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestLbStickyPolicy, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["configurableData"]["netscaler"]["lbdevicededicated"] = False

        try:
            cls.exception_string = "Connection limit to CFE exceeded"
            cls.skiptest = False
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.testdata["nw_off_isolated_netscaler"],
                conservemode=True
            )
            # Enable Network offering
            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.testdata["small"]["zoneid"] = cls.zone.id
            cls.testdata["small"]["template"] = cls.template.id

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"]
            )
            cls.account = Account.create(
                cls.api_client,
                cls.testdata["account"],
                admin=True,
                domainid=cls.domain.id
            )
            cls._cleanup.insert(0, cls.account)
            # Creating network using the network offering created
            cls.network = Network.create(
                cls.api_client,
                cls.testdata["network"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id
            )

            # Spawn an instance in that network
            cls.virtual_machine = VirtualMachine.create(
                cls.api_client,
                cls.testdata["small"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                networkids=[str(cls.network.id)]
            )
            cls.public_ip = PublicIPAddress.create(
                cls.api_client,
                accountid=cls.account.name,
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                networkid=cls.network.id
            )
        except Exception as e:
            if cls.exception_string.lower() in e.lower():
                cls.skiptest = True
                cls.exception_msg = e
            else:
                cls.tearDownClass()
                raise Exception("Warning: Exception in setUpClass: %s" % e)
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
        if self.skiptest:
            self.skipTest(self.exception_msg)
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            # Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns"])
    def test_01_source_based_roundrobin(self):
        """Test Create a "SourceBased" stick policy for a Lb rule with
           "RoundRobin" algorithm
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "SourceBased" stick policy for a Lb rule with
        #    "RoundRobin" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with round robin algo" %
            self.public_ip.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='SourceBased',
                name='SourceBasedRR',
                param={"holdtime": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy configured\
                    properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: SOURCEIP"),
                1,
                "'SourceBased' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: ROUNDROBIN"),
                1,
                "'ROUNDROBIN' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_02_source_based_source_algo(self):
        """Test Create a "SourceBased" stick policy for a Lb rule
           with "Source" algorithm
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "SourceBased" stick policy for a Lb rule with
        #    "Source" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with source algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'source'
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='SourceBased',
                name='SourceBasedSource',
                param={"holdtime": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: SOURCEIP"),
                1,
                "'SourceBased' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: SOURCEIPHASH"),
                1,
                "'SOURCE' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_03_source_based_leastconn(self):
        """Test Create a "SourceBased" stick policy for a Lb rule with leastconn algo
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "SourceBased" stick policy for a Lb rule with
        #    "leastconn" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with leastconn algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'leastconn'
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='SourceBased',
                name='SourceBasedLeast',
                param={"holdtime": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy configured\
                    properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: SOURCEIP"),
                1,
                "'SourceBased' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: LEASTCONNECTION"),
                1,
                "'leastconn' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_04_lbcookie_based_roundrobin(self):
        """Test Create a "LBCookie" stick policy for a Lb rule with roundrobin algo
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "LBCookie" stick policy for a Lb rule with
        #    "roundrobin" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with roundrobin algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'roundrobin'
        self.testdata["lbrule"]["publicport"] = 80
        self.testdata["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='LbCookie',
                name='LbCookieRR',
                param={"holdtime": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: COOKIEINSERT"),
                1,
                "'LBCookie' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: ROUNDROBIN"),
                1,
                "'ROUNDROBIN' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_05_lbcookie_source_algo(self):
        """Test Create a "LBCookie" stick policy for a Lb rule with "Source" algorithm
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "LBCookie" stick policy for a Lb rule with
        #    "Source" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with source algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'source'
        self.testdata["lbrule"]["publicport"] = 80
        self.testdata["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='LbCookie',
                name='LbCookieSource',
                param={"holdtime": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: COOKIEINSERT"),
                1,
                "'LbCookie' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: SOURCEIPHASH"),
                1,
                "'SOURCE' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_06_lbcookie_leastconn(self):
        """Test Create a "LBCookie" stick policy for a Lb rule with leastconn algo
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "LBCookie" stick policy for a Lb rule with
        #    "leastconn" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with leastconn algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'leastconn'
        self.testdata["lbrule"]["publicport"] = 80
        self.testdata["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='LBCookie',
                name='LbcookieLeastConn',
                param={"holdtime": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: COOKIEINSERT"),
                1,
                "'LbCookie' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: LEASTCONNECTION"),
                1,
                "'leastconn' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_07_appcookie_based_roundrobin(self):
        """Test Create a "AppCookie" stick policy for a Lb rule with roundrobin algo
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "AppCookie" stick policy for a Lb rule with
        #    "roundrobin" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with roundrobin algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'roundrobin'
        self.testdata["lbrule"]["publicport"] = 80
        self.testdata["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='AppCookie',
                name='AppCookieRR',
                param={"name": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: RULE"),
                1,
                "'AppCookie' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: ROUNDROBIN"),
                1,
                "'ROUNDROBIN' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_08_appcookie_source_algo(self):
        """Test Create a "AppCookie" stick policy for a Lb rule with "Source"
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "AppCookie" stick policy for a Lb rule with
        #    "Source" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with source algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'source'
        self.testdata["lbrule"]["publicport"] = 80
        self.testdata["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='AppCookie',
                name='AppCookieSource',
                param={"name": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: RULE"),
                1,
                "'AppCookie' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: SOURCEIPHASH"),
                1,
                "'SOURCE' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return

    @attr(tags=["advancedns"])
    def test_09_appcookie_leastconn(self):
        """Test Create a "AppCookie" stick policy for a Lb rule with leastconn
        """

        # Validate the following
        # 1. Configure Netscaler for load balancing.
        # 2. Create a Network offering with LB services provided by Netscaler
        #    and all other services by VR.
        # 3. Create a new account/user.
        # 4. Deploy few VMs using a network from the above created Network
        #    offering.
        # 5. Create a "AppCookie" stick policy for a Lb rule with
        #    "leastconn" algorithm

        self.debug(
            "Creating LB rule for IP address: %s with leastconn algo" %
            self.public_ip.ipaddress.ipaddress)

        self.testdata["lbrule"]["alg"] = 'leastconn'
        self.testdata["lbrule"]["publicport"] = 80
        self.testdata["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=self.public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.network.id
        )
        self.cleanup.append(lb_rule)
        self.debug("Created the load balancing rule for public IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        self.debug("Assigning VM instance: %s to LB rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        lb_rule.assign(self.apiclient, [self.virtual_machine])
        self.debug("Assigned VM instance: %s to lb rule: %s" % (
            self.virtual_machine.name,
            lb_rule.name
        ))
        self.debug(
            "Configuring 'SourceBased' Sticky policy on lb rule: %s" %
            lb_rule.name)
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname='AppCookie',
                name='AppCookieLeastConn',
                param={"name": 20}
            )
            self.debug("Response: %s" % result)
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

        self.debug(
            "SSH into Netscaler to check whether sticky policy\
                    configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                   self.testdata["configurableData"]["netscaler"]["ipaddress"])
        try:
            ssh_client = SshClient(
                self.testdata["configurableData"]["netscaler"]["ipaddress"],
                self.testdata["configurableData"]["netscaler"]["port"],
                self.testdata["configurableData"]["netscaler"]["username"],
                self.testdata["configurableData"]["netscaler"]["password"],
            )
            cmd = "show lb vserver Cloud-VirtualServer-%s-%s" % (
                self.public_ip.ipaddress.ipaddress,
                lb_rule.publicport)
            self.debug("command: %s" % cmd)
            res = ssh_client.execute(cmd)
            result = str(res)
            self.debug("Output: %s" % result)

            self.assertEqual(
                result.count("Persistence: RULE"),
                1,
                "'AppCookie' sticky policy should be configured on NS"
            )

            self.assertEqual(
                result.count("Configured Method: LEASTCONNECTION"),
                1,
                "'leastconn' algorithm should be configured on NS"
            )

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.testdata["configurableData"]["netscaler"]["ipaddress"], e))
        return
