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
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin.remoteSSHClient import remoteSSHClient
import datetime


class Services:
    """Test netscaler services
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
                                    "cpuspeed": 100, # in MHz
                                    "memory": 128, # In MBs
                         },
                         "virtual_machine": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                         "netscaler": {
                                "ipaddress": '10.147.60.27',
                                "username": 'nsroot',
                                "password": 'nsroot',
                                "networkdevicetype": 'NetscalerVPXLoadBalancer',
                                "publicinterface": '1/1',
                                "privateinterface": '1/1',
                                "numretries": 2,
                                "lbdevicededicated": False,
                                "lbdevicecapacity": 50,
                                "port": 22,
                         },
                         "network_offering": {
                                    "name": 'Netscaler',
                                    "displaytext": 'Netscaler',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList" : {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'Netscaler',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                    },
                         },
                         "network": {
                                  "name": "Netscaler",
                                  "displaytext": "Netscaler",
                         },
                         "lbrule": {
                                    "name": "SSH",
                                    "alg": "roundrobin",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 22,
                                    "openfirewall": False,
                         },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 60,
                         "timeout": 10,
                         "mode":'advanced'
                    }


class TestLbStickyPolicy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestLbStickyPolicy,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
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

        # Creating network using the network offering created
        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

        # Spawn an instance in that network
        cls.virtual_machine = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
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
        cls._cleanup = [
                        cls.service_offering,
                        cls.account
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            interval = list_configurations(
                                    cls.api_client,
                                    name='network.gc.interval'
                                    )
            wait = list_configurations(
                                    cls.api_client,
                                    name='network.gc.wait'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) + int(wait[0].value))
            cls.network_offering.delete(cls.api_client)
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
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advancedns"])
    def test_01_source_based_roundrobin(self):
        """Test Create a "SourceBased" stick policy for a Lb rule with "RoundRobin" algorithm
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
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
    def test_02_source_based_source_algo(self):
        """Test Create a "SourceBased" stick policy for a Lb rule with "Source" algorithm
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

        self.services["lbrule"]["alg"] = 'source'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'leastconn'
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'roundrobin'
        self.services["lbrule"]["publicport"] = 80
        self.services["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'source'
        self.services["lbrule"]["publicport"] = 80
        self.services["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'leastconn'
        self.services["lbrule"]["publicport"] = 80
        self.services["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'roundrobin'
        self.services["lbrule"]["publicport"] = 80
        self.services["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'source'
        self.services["lbrule"]["publicport"] = 80
        self.services["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return

    @attr(tags = ["advancedns"])
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

        self.services["lbrule"]["alg"] = 'leastconn'
        self.services["lbrule"]["publicport"] = 80
        self.services["lbrule"]["privateport"] = 80
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
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

        self.debug("SSH into Netscaler to check whether sticky policy configured properly or not?")
        self.debug("SSH into netscaler: %s" %
                                    self.services["netscaler"]["ipaddress"])
        try:
            ssh_client = remoteSSHClient(
                                    self.services["netscaler"]["ipaddress"],
                                    self.services["netscaler"]["port"],
                                    self.services["netscaler"]["username"],
                                    self.services["netscaler"]["password"],
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
            self.fail("SSH Access failed for %s: %s" % \
                      (self.services["netscaler"]["ipaddress"], e))
        return
