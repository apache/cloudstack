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
""" P1 tests for tags
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Tag,
                             Account,
                             VirtualMachine,
                             Iso,
                             Volume,
                             Network,
                             Host,
                             DiskOffering,
                             NATRule,
                             PublicIPAddress,
                             FireWallRule,
                             LoadBalancerRule,
                             Vpn,
                             Template,
                             Snapshot,
                             ServiceOffering,
                             Project)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template)
from marvin.codes import FAILED
import time

class Services:
    """Test tags Services
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                        },
                        "project": {
                                    "name": "Project",
                                    "displaytext": "Test project",
                        },
                        "account": {
                                    "email": "administrator@clogeny.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "user": {
                                    "email": "user@clogeny.com",
                                    "firstname": "User",
                                    "lastname": "User",
                                    "username": "User",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                        "other_user": {
                                    "email": "otheruser@clogeny.com",
                                    "firstname": "Other",
                                    "lastname": "User",
                                    "username": "User",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    # in MHz
                                    "memory": 128,
                                    # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Tiny Disk Offering",
                                    "name": "Tiny Disk Offering",
                                    "disksize": 1
                        },
                        "volume": {
                                   "diskname": "Test Volume",
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
                        "template": {
                                    "displaytext": "Cent OS Template",
                                    "name": "Cent OS Template",
                                    "ostype": 'CentOS 5.3 (64-bit)',
                                    "templatefilter": 'self',
                        },
                        "iso":
                        {
                            "displaytext": "Dummy ISO",
                            "name": "Dummy ISO",
                            "url": "http://people.apache.org/~tsp/dummy.iso",
                            # Source URL where ISO is located
                            "isextractable": True,
                            "isfeatured": True,
                            "ispublic": False,
                            "ostype": 'CentOS 5.3 (64-bit)',
                            "mode": 'HTTP_DOWNLOAD',
                            # Used in Extract template, value must be HTTP_DOWNLOAD
                        },
                        "network_offering": {
                                    "name": 'Network offering-VR services',
                                    "displaytext": 'Network offering-VR services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                                    },
                         "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                },
                         "lbrule": {
                                    "name": "SSH",
                                    "alg": "leastconn",
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
                         "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '55.55.0.0/11',
                                    # Any network (For creating FW rule)
                                },
                         "security_group": {
                                    "name": 'SSH',
                                    "protocol": 'TCP',
                                    "startport": 22,
                                    "endport": 22,
                                    "cidrlist": '0.0.0.0/0',
                        },
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "ostype": 'CentOS 5.3 (64-bit)',
                        "timeout": 10,
                        "mode": 'advanced',
                    }


class TestResourceTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestResourceTags, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

        # Get Zone, Domain and templates

        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            )
        
        cls.user_api_client = cls.testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
        
        # Create service offerings, disk offerings etc
        cls.service_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offering"]
                                    )

        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        
        cls.services["iso"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.vm_1 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.zone.networktype
                                )
        cls.vm_2 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.zone.networktype
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.disk_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            print("Cleanup resources used")
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.rm_tags = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
         
        for tag in self.rm_tags:
            tag['tag_obj'].delete(self.apiclient, tag['resid'],
                  tag['restype'],
                  {tag['key']: tag['value']})

        return
    
    @attr(tags=["advanced"], required_hardware="false")
    def test_01_lbrule_tag(self):
        """ Test Create tag on LB rule and remove the LB rule
        """
        # Validate the following
        # 1. Configured LB rule by assigning 2vms
        # 2. Create Tag on LB rule using CreateTag API
        # 3. Delete the LB rule

        self.debug("Fetching the network details for account: %s" %
                                                self.account.name)
        networks = Network.list(
                                self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should not return an empty response"
                         )
        network = networks[0]
        self.debug("Network for the account: %s is %s" %
                                    (self.account.name, network.name))

        self.debug("Associating public IP for network: %s" % network.id)
        public_ip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=network.id
                                    )
        self.cleanup.append(public_ip)

        self.debug("Trying to create LB rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)

        # Create Load Balancer rule on the public ip
        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    accountid=self.account.name
                                    )

        # Check if the LB rule created successfully
        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         id=lb_rule.id
                                         )

        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return valid list"
                         )

        self.debug("Assigning the virtual machines (%s, %s) to lb rule: %s" %
                                                        (self.vm_1.name,
                                                         self.vm_2.name,
                                                         lb_rule.name))

        lb_rule.assign(self.apiclient, [self.vm_1, self.vm_2])
        self.debug("Creating a tag for load balancer rule")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=lb_rule.id,
                         resourceType='LoadBalancer',
                         tags={'LB': 40}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='LoadBalancer',
                        key='LB',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        value=40
                        )

        self.debug("Tag created: %s" % str(tags))
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         int(tags[0].value),
                         40,
                         "The tag value should match with the original value"
                         )
    
        lb_rules = LoadBalancerRule.list(
                                         self.apiclient,
                                         listall=True,
                                         key='FW',
                                         value=40
                                         )

        self.assertEqual(
                         isinstance(lb_rules, list),
                         True,
                         "List LB rules should return valid list"
                         )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=lb_rule.id,
                       resourceType='LoadBalancer',
                       tags={'LB': 40}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='LoadBalancer',
                        key='LB',
                        account=self.account.name,
                        domainid=self.account.domainid
                        )

        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )

        self.debug("Deleting the Load balancer rule")
        try:
            lb_rule.delete(self.apiclient)
        except Exception as e:
            self.fail("failed to delete load balancer rule! - %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_natrule_tag(self):
        """ Test Create tag on nat rule and remove the nat rule
        """
        # Validate the following
        # 1. Configured PF rule
        # 2. create Tag on PF rule  using CreateTag API
        # 3. Delete the PF rule

        self.debug("Fetching the network details for account: %s" %
                                                self.account.name)
        networks = Network.list(
                                self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should not return an empty response"
                         )
        network = networks[0]
        self.debug("Network for the account: %s is %s" %
                                    (self.account.name, network.name))

        self.debug("Associating public IP for network: %s" % network.id)
        public_ip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=network.id
                                    )
        self.cleanup.append(public_ip)

        self.debug("Trying to create LB rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)

        self.debug("Creating PF rule for vm: %s on Ip: %s" %
                            (self.vm_1.name, public_ip.ipaddress.ipaddress))

        nat_rule = NATRule.create(
                         self.apiclient,
                         self.vm_1,
                         self.services["natrule"],
                         ipaddressid=public_ip.ipaddress.id
                      )

        # Check if NAT rule created successfully
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=nat_rule.id
                                 )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )

        self.debug("Creating a tag for port forwarding rule")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=nat_rule.id,
                         resourceType='portForwardingRule',
                         tags={'PF': 40}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='portForwardingRule',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='PF',
                        value=40
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         int(tags[0].value),
                         40,
                         "The tag value should match with the original value"
                         )
        
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 listall=True,
                                 key='FW',
                                 value=40
                                )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )
    
        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=nat_rule.id,
                       resourceType='portForwardingRule',
                       tags={'PF': 40}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='portForwardingRule',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='PF',
                        value=40
                        )

        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        self.debug("Deleting the port forwarding rule")
        try:
            nat_rule.delete(self.apiclient)
        except Exception as e:
            self.fail("failed to delete port forwarding rule! - %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_firewallrule_tag(self):
        """ Test Create tag on firewall rule and remove the firewall rule
        """
        # Validate the following
        # 1. Configured firewall rule
        # 2. create Tag on firewall rule  using CreateTag API
        # 3. Delete the firewall rule

        self.debug("Fetching the network details for account: %s" %
                                                self.account.name)
        networks = Network.list(
                                self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should not return an empty response"
                         )
        network = networks[0]
        self.debug("Network for the account: %s is %s" %
                                    (self.account.name, network.name))

        self.debug("Associating public IP for network: %s" % network.id)
        public_ip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=network.id
                                    )
        self.cleanup.append(public_ip)

        self.debug("Creating firewall rule on public IP: %s" %
                                                public_ip.ipaddress.ipaddress)
        # Create Firewall rule on public IP
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )

        self.debug("Created firewall rule: %s" % fw_rule.id)

        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     id=fw_rule.id
                                    )
        self.assertEqual(
                         isinstance(fw_rules, list),
                         True,
                         "List fw rules should return a valid firewall rules"
                         )

        self.assertNotEqual(
                            len(fw_rules),
                            0,
                            "Length of fw rules response should not be zero"
                            )

        self.debug("Creating a tag for firewall rule")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=fw_rule.id,
                         resourceType='FirewallRule',
                         tags={'FW': '40'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='FirewallRule',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='FW',
                        value='40'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        
        self.assertEqual(
                         tags[0].value,
                         '40',
                         "The tag value should match with the original value"
                         )

        fw_rules = FireWallRule.list(
                                     self.apiclient,
                                     listall=True,
                                     key='FW',
                                     value='40'
                                    )
        self.assertEqual(
                         isinstance(fw_rules, list),
                         True,
                         "List fw rules should return a valid firewall rules"
                         )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=fw_rule.id,
                       resourceType='FirewallRule',
                       tags={'FW': '40'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='FirewallRule',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='FW',
                        value='40'
                        )

        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )

        self.debug("Deleting the firewall rule")
        try:
            fw_rule.delete(self.apiclient)
        except Exception as e:
            self.fail("failed to delete firewall rule! - %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_vpn_tag(self):
        """ Test Create tag on vpn and remove the vpn
        """
        # Validate the following
        # 1. Enable the VPN
        # 2. create Tag on VPN rule using CreateTag API
        # 3. Delete the VPN rule
        self.skipTest("VPN resource tags are unsupported in 4.0")

        self.debug("Fetching the network details for account: %s" %
                                                self.account.name)
        networks = Network.list(
                                self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should not return an empty response"
                         )
        network = networks[0]
        self.debug("Network for the account: %s is %s" %
                                    (self.account.name, network.name))

        self.debug("Associating public IP for network: %s" % network.id)
        public_ip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=network.id
                                    )
        self.cleanup.append(public_ip)

        nat_rule = NATRule.create(
                         self.apiclient,
                         self.vm_1,
                         self.services["natrule"],
                         ipaddressid=public_ip.ipaddress.id
                      )

        # Check if NAT rule created successfully
        nat_rules = NATRule.list(
                                 self.apiclient,
                                 id=nat_rule.id
                                 )

        self.assertEqual(
                         isinstance(nat_rules, list),
                         True,
                         "List NAT rules should return valid list"
                         )

        # User should be able to enable VPN on source NAT
        self.debug("Creating VPN with public NAT IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        # Assign VPN to source NAT
        try:
            vpn = Vpn.create(
                        self.apiclient,
                        public_ip.ipaddress.id,
                        account=self.account.name,
                        domainid=self.account.domainid
                        )

        except Exception as e:
            print e

        vpns = Vpn.list(
                        self.apiclient,
                        publicipid=public_ip.ipaddress.id,
                        listall=True,
                        )

        self.assertEqual(
                         isinstance(vpns, list),
                         True,
                         "List VPNs should return a valid VPN list"
                         )

        self.assertNotEqual(
                            len(vpns),
                            0,
                            "Length of list VPN response should not be zero"
                            )

        self.debug("Creating a tag for VPN rule")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=nat_rule.id,
                         resourceType='VPN',
                         tags={'protocol': 'L2TP'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='VPN',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='protocol',
                        value='L2TP'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'L2TP',
                         "The tag value should match with the original value"
                         )
        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=vpn.id,
                       resourceType='VPN',
                       tags={'protocol': 'L2TP'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='VPN',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='protocol',
                        value='L2TP'
                        )

        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )

        self.debug("Disabling the VPN")
        try:
            vpn.delete(self.apiclient)
        except Exception as e:
            self.fail("failed to disable VPN! - %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_05_vm_tag(self):
        """ Test creation, listing and deletion tags on UserVM
        """
        # Validate the following
        # 1. Create  a tag on VM using createTags API
        # 2. Delete above created tag using deleteTags API

        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         "The tag value should match with the original value"
                         )
        
        vms = VirtualMachine.list(
                                    self.apiclient,
                                    listall=True,
                                    key='region',
                                    value='India'
                                  )
    
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "Tag based VMs listing failed")
    
        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_06_template_tag(self):
        """ Test creation, listing and deletion tag on templates
        """
        # Validate the following
        # 1. Create a tag on template/ISO using createTags API
        # 2. Delete above created tag using deleteTags API

        try:
            self.debug("Stopping the virtual machine: %s" % self.vm_1.name)
            #Stop virtual machine
            self.vm_1.stop(self.user_api_client)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        timeout = self.services["timeout"]
        while True:
            list_volume = Volume.list(
                                   self.user_api_client,
                                   virtualmachineid=self.vm_1.id,
                                   type='ROOT',
                                   listall=True
                                   )
            if isinstance(list_volume, list):
                break
            elif timeout == 0:
                raise Exception("List volumes failed.")

            time.sleep(5)
            timeout = timeout - 1

        self.volume = list_volume[0]

        self.debug("Creating template from ROOT disk of virtual machine: %s" %
                                                            self.vm_1.name)
        #Create template from volume
        template = Template.create(
                                    self.user_api_client,
                                    self.services["template"],
                                    self.volume.id
                                )
        self.cleanup.append(template)
        self.debug("Created the template(%s). Now restarting the userVm: %s" %
                                            (template.name, self.vm_1.name))
        self.vm_1.start(self.user_api_client)

        self.debug("Creating a tag for the template")
        tag = Tag.create(
                         self.user_api_client,
                         resourceIds=template.id,
                         resourceType='Template',
                         tags={'OS': 'CentOS'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.user_api_client,
                        listall=True,
                        resourceType='Template',
                        key='OS',
                        value='CentOS'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'CentOS',
                         'The tag should have original value'
                         )

        Template.list(
                  self.user_api_client,
                  templatefilter=\
                  self.services["template"]["templatefilter"],
                  listall=True,
                  key='OS',
                  value='CentOS'
                  )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.user_api_client,
                       resourceIds=template.id,
                       resourceType='Template',
                       tags={'OS': 'CentOS'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.user_api_client,
                        listall=True,
                        resourceType='Template',
                        key='OS',
                        value='CentOS'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_07_iso_tag(self):
        """ Test creation, listing and deletion tags on ISO
        """
        # Validate the following
        # 1. Create  a tag on ISO using createTags API
        # 2. Delete above created tag using deleteTags API

        iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.debug("ISO created with ID: %s" % iso.id)

        list_iso_response = Iso.list(self.apiclient,
                                     id=iso.id)
        self.assertEqual(
                         isinstance(list_iso_response, list),
                         True,
                         "Check list response returns a valid list"
                         )

        self.debug("Creating a tag for the ISO")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=iso.id,
                         resourceType='ISO',
                         tags={'OS': 'CentOS'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='ISO',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='OS',
                        value='CentOS'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'CentOS',
                         'The tag should have original value'
                         )
        isos = Iso.list(
                        self.apiclient,
                        key='OS',
                        value='CentOS',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        isofilter='all'
                    )

        self.assertEqual(
                         isinstance(isos, list),
                         True,
                         "List isos should not return an empty response"
                         )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=iso.id,
                       resourceType='ISO',
                       tags={'OS': 'CentOS'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='ISO',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='OS',
                        value='CentOS'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_08_volume_tag(self):
        """ Test creation, listing and deletion tagson volume
        """
        # Validate the following
        # 1. Create a tag on volume using createTags API
        # 2. Delete above created tag using deleteTags API

        self.debug("Creating volume for account: %s " %
                                                self.account.name)
        volume = Volume.create(
                               self.apiclient,
                               self.services["volume"],
                               zoneid=self.zone.id,
                               account=self.account.name,
                               domainid=self.account.domainid,
                               diskofferingid=self.disk_offering.id
                               )
        self.cleanup.append(volume)

        self.debug("Volume created in account: %s" % volume.name)

        self.debug("Creating a tag for the volume")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=volume.id,
                         resourceType='volume',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='volume',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )
        
        vols = Volume.list(self.apiclient,
                  listall=True,
                  key='region',
                  value='India'
                 )
        self.assertEqual(
                         isinstance(vols, list),
                         True,
                         "List volumes should not return empty response"
                        )
    
        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=volume.id,
                       resourceType='volume',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='volume',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_09_snapshot_tag(self):
        """ Test creation, listing and deletion tag son snapshot
        """
        # Validate the following
        # 1. Create a tag on snapshot using createTags API
        # 2. Delete above created tag using deleteTags API

        self.debug("Creating snapshot on ROOT volume for VM: %s " %
                                                            self.vm_1.name)
        # Get the Root disk of VM
        volumes = Volume.list(self.apiclient,
                              virtualmachineid=self.vm_1.id,
                              type='ROOT',
                              listall=True)
        volume = volumes[0]

        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(self.apiclient, volume.id)
        self.debug("Snapshot created: ID - %s" % snapshot.id)
        self.cleanup.append(snapshot)

        snapshots = Snapshot.list(self.apiclient,
                                  id=snapshot.id)
        self.assertEqual(
                         isinstance(snapshots, list),
                         True,
                         "Tag based snapshot listing failed")

        self.debug("Creating a tag for the snapshot")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=snapshot.id,
                         resourceType='snapshot',
                         tags={'type': 'manual'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='snapshot',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='type',
                        value='manual'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'manual',
                         'The tag should have original value'
                         )
        snapshots = Snapshot.list(self.apiclient,
                                  listall=True,
                                  key='type',
                                  value='manual')
        self.assertEqual(
                         isinstance(snapshots, list),
                         True,
                         "Check list response returns a valid list"
                         )
        self.assertNotEqual(
                            snapshots,
                            None,
                            "Check if result exists in list snapshots call"
                            )
        self.debug("Listing snapshots by tag was successful")

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=snapshot.id,
                       resourceType='snapshot',
                       tags={'type': 'manual'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='snapshot',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='type',
                        value='manual'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )

        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_10_network_tag(self):
        """ Testcreation, listing and deletion tags on guest network
        """
        # Validate the following
        # 1. Create  a tag on Network using createTags API
        # 2. Delete above created tag using deleteTags API

        self.debug("Fetching the network details for account: %s" %
                                                self.account.name)
        networks = Network.list(
                                self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should not return an empty response"
                         )
        network = networks[0]
        self.debug("Network for the account: %s is %s" %
                                    (self.account.name, network.name))

        self.debug("Creating a tag for load balancer rule")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=network.id,
                         resourceType='Network',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='Network',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )
        
        networks = Network.list(
                                self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True,
                                key='region',
                                value='India'       
                                )
        self.assertEqual(
                         isinstance(networks, list),
                         True,
                         "List networks should not return an empty response"
                         )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=network.id,
                       resourceType='Network',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='Network',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["basic", "sg"])
    def test_11_migrate_tagged_vm_del(self):
        """ Test migration of a tagged vm and delete the tag
        """
        # Validate the following
        # 1. Create a tag on VM using createTags API
        # 2. Delete above created tag using deleteTags API

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_1.id,
                                  listall=True
                                  )

        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List vms should not return empty response"
                         )
        source_host = vms[0].hostid
    
        hosts = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          resourcestate='Enabled',
                          type='Routing'
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should return valid host response"
                         )

        self.debug("Available hosts: ")
        for host in hosts:
            self.debug("Host: %s" % host.id)

            # Filtering out the source host from list host response
            temp_hosts = [host for host in hosts if host.id != source_host]
            dest_host = temp_hosts[0]

            self.debug("Destination host is: %s" % dest_host.id)
            self.debug("Source host is: %s" % source_host)

        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )

        self.debug("Migrating the instance from: %s to %s" %
                                                (source_host, dest_host.id))
        self.vm_1.migrate(self.apiclient, hostid=dest_host.id)

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_13_tag_case_insensitive(self):
        """ Test to verify that tags are not case sensitive
        """
        # Validate the following
        # 1. Create a tag on VM using createTags API
        # 2. Add same tag in upper case.
        # 3. Verify that tag creation failed.

        self.debug("Creating a tag for user VM")
        tag_1 = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag_1.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )
        try:
            Tag.create(self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'REGION': 'INDIA'})
        except Exception as e:
            pass
        else:
            assert("Creating same tag in upper case succeeded")

        try:
            tag_1.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_14_special_char_mutiple_tags(self):
        """ Test multiple tags and with special characters on same machine
        """
        # Validate the following
        # 1. Create more than 10 tags to VM using createTags API
        # 2. Create a tag with special characters on VM using createTags API

        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={
                                    'region': 'India',
                                    'offering': 'high',
                                    'type': 'webserver',
                                    'priority': 'critical',
                                    'networking': 'advanced',
                                    'os': 'centos',
                                    'backup': 'no$required',
                                    'rootvolume': 'NFS',
                                    'iso': 'na',
                                    'ha': 'yes',
                                    'test': 'test'
                               }
                         )
        self.debug("Tags created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )
        # Cleanup
        tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={
                             'region': 'India',
                             'offering': 'high',
                             'type': 'webserver',
                             'priority': 'critical',
                             'networking': 'advanced',
                             'os': 'centos',
                             'backup': 'no$required',
                             'rootvolume': 'NFS',
                             'iso': 'na',
                             'ha': 'yes',
                             'test': 'test'
                             }
                       )
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_15_project_tag(self):
        """ Test creation, listing and deletion tags on projects
        """
        # Validate the following
        # 1. Create a new project 
        # 2. Create a tag on projects using createTags API
        # 3. Delete the tag.

        # Create project as a domain admin
        project = Project.create(
                                 self.apiclient,
                                 self.services["project"],
                                 account=self.account.name,
                                 domainid=self.account.domainid
                                 )
        # Cleanup created project at end of test
        self.cleanup.append(project)
        self.debug("Created project with domain admin with ID: %s" %
                                                                project.id)

        self.debug("Creating a tag for the project")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=project.id,
                         resourceType='project',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='project',
                        resourceIds=project.id,
                        key='region',
                        )
        self.debug("tags = %s" % tags)
 
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )
    
        projects = Project.list(
                             self.apiclient,
                             listall=True,
                             key='region',
                             value='India'
                            )

        self.assertEqual(
                         isinstance(projects, list),
                         True,
                         "List Project should return valid list"
                         )


        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=project.id,
                       resourceType='project',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='project',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_16_query_tags_other_account(self):
        """ Test Query the tags from other account
        """
        # Validate the following
        # 1. Login with an account(account A)
        # 2. Create a tags on resource(eg:VM)
        # 3. Login with other account and query the tags using
        #    listTags API

        self.debug("Creating user accounts..")

        user_account = Account.create(
                            self.apiclient,
                            self.services["user"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(user_account)

        other_user_account = Account.create(
                            self.apiclient,
                            self.services["other_user"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(other_user_account)

        iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=user_account.name,
                         domainid=user_account.domainid
                         )
        self.debug("ISO created with ID: %s" % iso.id)

        list_iso_response = Iso.list(self.apiclient,
                                     id=iso.id)
        self.assertEqual(
                            isinstance(list_iso_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.debug("Creating a tag for the ISO")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=iso.id,
                         resourceType='ISO',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='ISO',
                        account=user_account.name,
                        domainid=user_account.domainid,
                        key='region',
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         "The tag value should match with the original value"
                         )
 
        self.debug("Verify listTag API using other account")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='ISO',
                        account=other_user_account.name,
                        domainid=other_user_account.domainid,
                        key='region',
                        )
        
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                        )
        
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_17_query_tags_admin_account(self):
        """ Test Query the tags from admin account
        """
        # Validate the following
        # 1. Login with an account(account A)
        # 2. Create a tags on resource(eg:VM)
        # 3. Login with admin account and query the tags using
        #    listTags API

        self.debug("Creating user accounts..")

        user_account = Account.create(
                            self.apiclient,
                            self.services["user"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(user_account)

        iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=user_account.name,
                         domainid=user_account.domainid
                         )

        list_iso_response = Iso.list(self.apiclient,
                                     id=iso.id)
        self.assertEqual(
                         isinstance(list_iso_response, list),
                         True,
                         "Check list response returns a valid list"
                         )
        Tag.create(self.apiclient,
                   resourceIds=iso.id,
                   resourceType='ISO',
                   tags={'region': 'India'})

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='ISO',
                        account=user_account.name,
                        domainid=user_account.domainid,
                        key='region',
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         "The tag value should match with the original value"
                         )
 
        self.debug("Verify listTag API using admin account")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='ISO',
                        key='region',
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                        )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                        )
        
        return

    @attr(tags=["advanced", "basic", "simulator"], required_hardware="false")
    def test_18_invalid_list_parameters(self):
        """ Test listAPI with invalid tags parameter
        """
        # Validate the following
        # 1. Create a tag on  supported resource type(ex:vms)
        # 2. Run the list API commands  with passing invalid key parameter

        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        # Add tag for removal during teardown. vm_1 is shared resource if it is tagged
        # and the test fails with exception then the tag is not deleted. And
        # subsequent tests fail to tag the vm_1 with same key-pair
        # breaking the tests.   
        self.rm_tags.append({'tag_obj': tag,'restype': 'userVM', 'resid': self.vm_1.id,
                 'key': 'region', 'value': 'India'})

        self.debug("Passing invalid key parameter to the listAPI for vms")

        vms = VirtualMachine.list(self.apiclient,
            **{'tags[0].key': 'region111',
             'tags[0].value': 'India',
             'listall' : 'True'}
        )
        self.assertEqual(
                         vms,
                         None,
                         "List vms should return empty response"
                        )

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_19_delete_add_same_tag(self):
        """ Test deletion and addition of same tag on a resource.
        """

        # Validate the following
        # 1. Deletion of a tag without any errors.
        # 2. Add same tag.

        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

        self.assertEqual(
                         tags[0].value,
                         "India",
                         "Tag created with incorrect value"
                         )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )
        self.debug("Recreating the tag with same name")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                            )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

        self.assertEqual(tags[0].value,
                         "India",
                         "Tag created with incorrect value"
                         )
    
        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_20_create_tags_multiple_resources(self):
        "Test creation of same tag on multiple resources"

        self.debug("Creating volume for account: %s " %
                                                self.account.name)
        volume = Volume.create(
                               self.apiclient,
                               self.services["volume"],
                               zoneid=self.zone.id,
                               account=self.account.name,
                               domainid=self.account.domainid,
                               diskofferingid=self.disk_offering.id
                               )
        self.cleanup.append(volume)

        self.debug("Volume created in account: %s" % volume.name)

        self.debug("Creating a tag for the volume")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=volume.id,
                         resourceType='volume',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='volume',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )
        self.assertEqual(
                         tags[0].value,
                         'India',
                         'The tag should have original value'
                         )
 
        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

        self.assertEqual(
                         tags[0].value,
                         "India",
                         "Tag created with incorrect value"
                         )
    
        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         tags,
                         None,
                         "List tags should return empty response"
                         )

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_21_create_tag_stopped_vm(self):
        "Test creation of tag on stopped vm."

        try:
            self.debug("Stopping the virtual machine: %s" % self.vm_1.name)
            #Stop virtual machine
            self.vm_1.stop(self.apiclient)

            self.debug("Creating a tag for user VM")
            tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
            self.debug("Tag created: %s" % tag.__dict__)

            tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
            self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

            self.assertEqual(
                         tags[0].value,
                         "India",
                         "Tag created with incorrect value"
                         )

            self.debug("Deleting the created tag..")
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Exception occured - %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_22_create_tag_destroyed_vm(self):
        "Test creation of tag on stopped vm."

        self.debug("Destroying instance: %s" % self.vm_1.name)
        self.vm_1.delete(self.apiclient, expunge=False)

        self.debug("Creating a tag for user VM")
        tag = Tag.create(
                         self.apiclient,
                         resourceIds=self.vm_1.id,
                         resourceType='userVM',
                         tags={'region': 'India'}
                         )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
                        self.apiclient,
                        listall=True,
                        resourceType='userVM',
                        account=self.account.name,
                        domainid=self.account.domainid,
                        key='region',
                        value='India'
                        )
        self.assertEqual(
                         isinstance(tags, list),
                         True,
                         "List tags should not return empty response"
                         )

        self.assertEqual(
                         tags[0].value,
                         "India",
                         "Tag created with incorrect value"
                         )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                       self.apiclient,
                       resourceIds=self.vm_1.id,
                       resourceType='userVM',
                       tags={'region': 'India'}
                       )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        return
