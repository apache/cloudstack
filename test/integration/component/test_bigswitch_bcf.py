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

""" P1 tests for BigSwitchBcf network Plugin
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import *
from marvin.lib.utils import (cleanup_resources,
                              random_gen, validateList)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             NetworkOffering,
                             VpcOffering,
                             Network,
                             PublicIPAddress,
                             FireWallRule,
                             NATRule,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.sshClient import SshClient
from marvin.codes import PASS
import subprocess
import time


class Services:
    """Test BigSwitchBcf plugin
    """

    def __init__(self):
        self.services = {
                         "account": {
                                    "email": "cloudstack@cloudmonkey.com",
                                    "firstname": "cloudstack",
                                    "lastname": "bob",
                                    "username": "bobbuilder",
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
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
                         "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "startport": 22,
                                    "endport": 22,
                                    "protocol": "TCP",
                                    "cidrlist": '0.0.0.0/0',
                                },
                         "net1_net2_aclrule": {
                                    "protocol": "ICMP",
                                    "cidrlist": '0.0.0.0/0',
                                },
                         "big_switch_bcf_device": {
                                "hostname": '10.212.1.104',
                                "username": 'admin',
                                "password": 'bsn',
                                "retrycount": '4',
                                "retryinterval": '60'
                         },
                         #services supported by Big Switch BCF for isolated networks.
                         "network_offering": {
                                    "name": 'BCF network offering (marvin)',
                                    "displaytext": 'BCF (marvin)',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,PortForwarding,Firewall',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "PortForwarding": 'VirtualRouter',
                                            "Dhcp" : 'VirtualRouter',
                                            "Connectivity" : 'BigSwitchBcf',
                                            "StaticNat" : 'VirtualRouter',
                                            "SourceNat" : 'VirtualRouter',
                                            "Firewall" : 'VirtualRouter'
                                    },
                         },
                         "vpc_network_offering": {
                                    "name": 'BCF VPC network offering (marvin)',
                                    "displaytext": 'BCF VPC (marvin)',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,SourceNat,Connectivity,PortForwarding,StaticNat,NetworkACL',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Dhcp": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "NetworkACL": 'VpcVirtualRouter',
                                            "Connectivity" : 'BigSwitchBcf'
                                        },
                                },
                         "vpc_offering": {
                                    "name": 'BCF VPC',
                                    "displaytext": 'BCF VPC',
                                    "supportedservices": 'Dhcp,SourceNat,PortForwarding,StaticNat,NetworkACL,Connectivity',
                                },
                         "vpc": {
                                 "name": "TestVPC",
                                 "displaytext": "TestVPC",
                                 "cidr": '10.0.0.1/16'
                                 },
                         "network": {
                                  "name": "BCF network",
                                  "displaytext": "BCF network",
                         },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         "sleep": 60,
                         "timeout": 10
                    }

class TestBigSwitchBcf(cloudstackTestCase):


    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestBigSwitchBcf, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services

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

        # Get test parameters
        cls.bigswitch_services = cls.services["big_switch_bcf_device"]
        try:
           # Get physical network
           resp = listPhysicalNetworks.listPhysicalNetworksCmd()
           resp.zoneid = cls.zone.id
           physical_networks = cls.api_client.listPhysicalNetworks(resp)
           if isinstance(physical_networks, list):
               cls.debug("phy net %s" % physical_networks)
               physical_network = physical_networks[1]

           # Get bigswitch network service provider
           resp = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
           resp.name = 'BigSwitchBcf'
           resp.physicalnetworkid = physical_network.id
           nw_service_providers = cls.api_client.listNetworkServiceProviders(resp)
           if isinstance(nw_service_providers, list):
               bigswitch_provider = nw_service_providers[0]
               cls.debug("net serv prov %s" % nw_service_providers)
           else:
               # Create bigswitch provider if not already existent
               resp_add_nsp = addNetworkServiceProvider.addNetworkServiceProviderCmd()
               resp_add_nsp.name = 'BigSwitchBcf'
               resp_add_nsp.physicalnetworkid = physical_network.id
               bigswitch_provider = cls.api_client.addNetworkServiceProvider(resp_add_nsp)
               # add BCF controller
               resp_add_device = addBigSwitchBcfDevice.addBigSwitchBcfDeviceCmd()
               resp_add_device.physicalnetworkid = physical_network.id
               resp_add_device.username = cls.bigswitch_services["username"]
               resp_add_device.password = cls.bigswitch_services["password"]
               resp_add_device.hostname = cls.bigswitch_services["hostname"]
               resp_add_device.retrycount = cls.bigswitch_services["retrycount"]
               resp_add_device.retryinterval = cls.bigswitch_services["retryinterval"]
               cls.bigswitch = cls.api_client.addBigSwitchBcfDevice(resp_add_device)

           if bigswitch_provider.state != 'Enabled':
               cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
               cmd.id = bigswitch_provider.id
               cmd.state = 'Enabled'
               cls.api_client.updateNetworkServiceProvider(cmd)

           # Create non-VPC network offering
           cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
           cls._cleanup.append(cls.network_offering)

           cls.network_offering.update(cls.api_client, state='Enabled')

           # Create VPC network offering
           cls.vpc_network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["vpc_network_offering"],
                                            conservemode=False
                                            )
           cls._cleanup.append(cls.vpc_network_offering)

           cls.vpc_network_offering.update(cls.api_client, state='Enabled')

           # Create VPC offering
           cls.vpc_offering = VpcOffering.create(
                                            cls.api_client,
                                            cls.services["vpc_offering"]
                                            )
           cls._cleanup.append(cls.vpc_offering)

           cls.vpc_offering.update(cls.api_client, state='Enabled')

           # Create compute service offering
           cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
           cls._cleanup.append(cls.service_offering)

        except Exception as e:
           cls.tearDownClass()
           raise Exception ("Warning: Exception in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
#        self._cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self._cleanup)
            interval = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
            wait = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
            time.sleep(int(interval[0].value) + int(wait[0].value))
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    @attr(tags = ["advancedns"])
    def test_network_bcf(self):
        """Test basic workflow with BigSwitch BCF plugin
           1. Create a non-VPC guest network
           2. Create two VMs on the network
           3. Add firewall rule to make virtual router pingable
           4. Test ping to virtual router public IP
           5. Add static NAT to vm_1, with firewall rule to allow ssh
           6. Ssh to vm_1, ping vm_2 private address, ping google.com
        """

        self.debug("STEP 1: Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(
                                      self.apiclient,
                                      self.services["network"],
                                      accountid=self.account.name,
                                      domainid=self.account.domainid,
                                      networkofferingid=self.network_offering.id,
                                      zoneid=self.zone.id
                                     )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("STEP 2: Deploying VMs in account: %s" % self.account.name)

        vm_1 = VirtualMachine.create(
                                     self.apiclient,
                                     self.services["virtual_machine"],
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     serviceofferingid=self.service_offering.id,
                                     networkids=[ str(self.network.id), ]
                                    )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id=vm_1.id
                                              )
        self.debug(
                   "Verify listVirtualMachines response for virtual machine: %s" \
                   % vm_1.id
                  )
        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                         vm_response.state,
                         "Running",
                         "VM state should be running after deployment"
                        )

        vm_2 = VirtualMachine.create(
                                     self.apiclient,
                                     self.services["virtual_machine"],
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     serviceofferingid=self.service_offering.id,
                                     networkids=[ str(self.network.id), ]
                                    )
        self.debug("Deployed VM in network: %s" % self.network.id)
        list_vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id=vm_2.id
                                              )

        self.debug(
                   "Verify listVirtualMachines response for virtual machine: %s" \
                   % vm_2.id
                  )

        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        self.debug("STEP 3: Add FW rule to allow source nat ping")
        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        associatednetworkid=self.network.id,
                                        account=self.account.name,
                                        domainid=self.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]

        #Create Firewall rules on source NAT
        fw_rule_icmp = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='ICMP',
                            cidrlist=["0.0.0.0/0",]
                            )
        self.debug("Created firewall rule: %s" % fw_rule_icmp.id)

        self.debug("STEP 4: Trying to ping source NAT %s" % src_nat.ipaddress)
        # User should be able to ping router via source nat ip
        try:
            self.debug("Trying to ping source NAT %s" % src_nat.ipaddress)
            result = subprocess.call(
                ['ping', '-c 1', src_nat.ipaddress])

            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertEqual(
                             result,
                             0,
                             "Check if ping is successful or not"
                            )
        except Exception as e:
            self.fail("Ping failed for source NAT %s (%s)"
                      % (src_nat.ipaddress, e))

        self.debug("STEP 5: Add static NAT to vm_1 with FW rule to allow SSH")
        floating_ip_1 = PublicIPAddress.create(
                                               self.apiclient,
                                               accountid=self.account.name,
                                               zoneid=self.zone.id,
                                               domainid=self.account.domainid,
                                               networkid=self.network.id,
                                              )
        self.debug("Associated %s with network %s" % (
                                                      floating_ip_1.ipaddress,
                                                      self.network.id
                                                     )
                  )
        NATRule.create(
                                  self.apiclient,
                                  vm_1,
                                  self.services["natrule"],
                                  ipaddressid=floating_ip_1.ipaddress.id,
                                  openfirewall=False,
                                  networkid=self.network.id
                                  )

        # Should be able to SSH vm_1 via static nat, then ping vm_2 & Internet
        try:
            self.debug("STEP 6: SSH into vm_1: %s" % floating_ip_1)

            ssh = vm_1.get_ssh_client(
                                  ipaddress=floating_ip_1.ipaddress.ipaddress
                                  )
            self.debug("Ping vm_2 at %s" % vm_2.ipaddress)
            # Ping vm_2
            res_1 = ssh.execute("ping -c 1 %s" % vm_2.ipaddress)

            self.debug("Ping to google.com from VM")
            # Ping Internet
            res_2 = ssh.execute("ping -c 1 www.google.com")

            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("SSH Access failed: %s" % e)

        self.debug("ping result1: %s" % res_1);
        self.debug("ping result2: %s" % res_2);

        result1 = str(res_1)
        self.assertEqual(
                         result1.count("1 received"),
                         1,
                         "Ping vm_2 from vm_1 should be successful"
                         )

        result2 = str(res_2)
        self.assertEqual(
                         result2.count("1 received"),
                         1,
                         "Ping Internet from vm_1 should be successful"
                         )

        # Deleting two test VMs
        VirtualMachine.delete(vm_1, self.apiclient, expunge=True)
        VirtualMachine.delete(vm_2, self.apiclient, expunge=True)

        # Delete Network
        Network.delete(self.network, self.apiclient)

        return

    @attr(tags = ["advancedns"])
    def test_vpc_network_bcf(self):
        """Test VPC workflow with BigSwitch BCF plugin
           1. Create a VPC with three networks
           2. Create one VM on each of the three networks
           3. Add firewall rule to make virtual router pingable
           4. Test ping to virtual router public IP
           5. Add static NAT to vm_1, with firewall rule to allow ssh
           6. Add NAT rule to allow ping between net1 and net2
           7. Ssh to vm_1, ping vm_2 private address, should succeed
           8. continue ... ping vm_3 private address, should fail
           9. continue ... ping Internet, should succeed
        """

        self.debug("STEP 1: Creating VPC with VPC offering: %s" %
                            self.vpc_offering.id)
        vpc = VPC.create(
                         self.apiclient,
                         self.services["vpc"],
                         accountid=self.account.name,
                         domainid=self.account.domainid,
                         vpcofferingid=self.vpc_offering.id,
                         zoneid=self.zone.id
                        )
        self.debug("Created VPC with ID: %s" % self.vpc.id)

        # Creating network using the vpc network offering created
        self.debug("Creating networks with vpc network offering: %s" %
                    self.vpc_network_offering.id)
        net1 = Network.create(
                              self.apiclient,
                              self.services["vpc_network"],
                              accountid=self.account.name,
                              domainid=self.account.domainid,
                              networkofferingid=self.vpc_network_offering.id,
                              zoneid=self.zone.id,
                              gateway="10.0.100.1",
                              vpcid=vpc.id
                             )
        self.debug("Created network with ID: %s" % net1.id)

        net2 = Network.create(
                              self.apiclient,
                              self.services["vpc_network"],
                              accountid=self.account.name,
                              domainid=self.account.domainid,
                              networkofferingid=self.vpc_network_offering.id,
                              zoneid=self.zone.id,
                              gateway="10.0.101.1",
                              vpcid=vpc.id
                             )
        self.debug("Created network with ID: %s" % net2.id)

        net3 = Network.create(
                              self.apiclient,
                              self.services["vpc_network"],
                              accountid=self.account.name,
                              domainid=self.account.domainid,
                              networkofferingid=self.vpc_network_offering.id,
                              zoneid=self.zone.id,
                              gateway="10.0.102.0",
                              vpcid=vpc.id
                             )
        self.debug("Created network with ID: %s" % net3.id)

        self.debug("STEP 2: Deploying VMs in networks")

        vm_1 = VirtualMachine.create(
                                     self.apiclient,
                                     self.services["virtual_machine"],
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     serviceofferingid=self.service_offering.id,
                                     networkids=[ str(net1.id), ]
                                    )
        self.debug("Deployed VM in network: %s" % net1.id)
        list_vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id=vm_1.id
                                              )
        self.debug(
                   "Verify listVirtualMachines response for virtual machine: %s" \
                   % vm_1.id
                  )
        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                         vm_response.state,
                         "Running",
                         "VM state should be running after deployment"
                        )

        vm_2 = VirtualMachine.create(
                                     self.apiclient,
                                     self.services["virtual_machine"],
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     serviceofferingid=self.service_offering.id,
                                     networkids=[ str(net2.id), ]
                                    )
        self.debug("Deployed VM in network: %s" % net2.id)
        list_vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id=vm_2.id
                                              )

        self.debug(
                   "Verify listVirtualMachines response for virtual machine: %s" \
                   % vm_2.id
                  )

        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.state,
                            "Running",
                            "VM state should be running after deployment"
                        )

        vm_3 = VirtualMachine.create(
                                     self.apiclient,
                                     self.services["virtual_machine"],
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     serviceofferingid=self.service_offering.id,
                                     networkids=[ str(net3.id), ]
                                    )
        self.debug("Deployed VM in network: %s" % net3.id)
        list_vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id=vm_3.id
                                              )
        self.debug(
                   "Verify listVirtualMachines response for virtual machine: %s" \
                   % vm_3.id
                  )
        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check list response returns a valid list"
                        )
        vm_response = list_vm_response[0]

        self.assertEqual(
                         vm_response.state,
                         "Running",
                         "VM state should be running after deployment"
                        )

        self.debug("STEP 3: Add FW rule to allow source nat ping")
        src_nat_list = PublicIPAddress.list(
                                        self.apiclient,
                                        account=self.account.name,
                                        domainid=self.account.domainid,
                                        listall=True,
                                        issourcenat=True,
                                        vpcid=vpc.id
                                        )
        self.assertEqual(
                         isinstance(src_nat_list, list),
                         True,
                         "List Public IP should return a valid source NAT"
                         )
        self.assertNotEqual(
                    len(src_nat_list),
                    0,
                    "Length of response from listPublicIp should not be 0"
                    )

        src_nat = src_nat_list[0]

        #Create Firewall rules on source NAT
        fw_rule_icmp = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='ICMP',
                            cidrlist=["0.0.0.0/0",]
                            )
        self.debug("Created firewall rule: %s" % fw_rule_icmp.id)

        self.debug("STEP 4: Trying to ping source NAT %s" % src_nat.ipaddress)
        # User should be able to ping router via source nat ip
        try:
            self.debug("Trying to ping source NAT %s" % src_nat.ipaddress)
            result = subprocess.call(
                ['ping', '-c 1', src_nat.ipaddress])

            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertEqual(
                             result,
                             0,
                             "Check if ping is successful or not"
                            )
        except Exception as e:
            self.fail("Ping failed for source NAT %s (%s)"
                      % (src_nat.ipaddress, e))

        self.debug("STEP 5: Add static NAT to vm_1 with FW rule to allow SSH")
        floating_ip_1 = PublicIPAddress.create(
                                               self.apiclient,
                                               accountid=self.account.name,
                                               zoneid=self.zone.id,
                                               domainid=self.account.domainid,
                                               networkid=net1.id,
                                               vpcid=vpc.id
                                              )
        self.debug("Associated %s with network %s" % (
                                                      floating_ip_1.ipaddress,
                                                      net1.id
                                                     )
                  )
        NATRule.create(
                                  self.apiclient,
                                  vm_1,
                                  self.services["natrule"],
                                  ipaddressid=floating_ip_1.ipaddress.id,
                                  openfirewall=False,
                                  networkid=net1.id,
                                  vpcid=vpc.id
                                  )

        # Should be able to SSH vm_1 via static nat, then ping vm_2 & Internet
        try:
            self.debug("STEP 6: SSH into vm_1: %s" % floating_ip_1)

            ssh = vm_1.get_ssh_client(
                                  ipaddress=floating_ip_1.ipaddress.ipaddress
                                  )
#            self.debug("Ping vm_2 at %s" % vm_2.ipaddress)
            # Ping to outsite world
#            res_1 = ssh.execute("ping -c 1 %s" % vm_2.ipaddress)

#            self.debug("Ping to google.com from VM")
            # Ping to outsite world
#            res_2 = ssh.execute("ping -c 1 www.google.com")

            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("SSH Access failed: %s" % e)

#        self.debug("ping result1: %s" % res_1);
#        self.debug("ping result2: %s" % res_2);

#        result1 = str(res_1)
#        self.assertEqual(
#                         result1.count("1 received"),
#                         1,
#                         "Ping vm_2 from vm_1 should be successful"
#                         )

#        result2 = str(res_2)
#        self.assertEqual(
#                         result2.count("1 received"),
#                         1,
#                         "Ping Internet from vm_1 should be successful"
#                         )

        # Deleting two test VMs
        VirtualMachine.delete(vm_1, self.apiclient, expunge=True)
        VirtualMachine.delete(vm_2, self.apiclient, expunge=True)

        # Delete Network
        Network.delete(self.network, self.apiclient)

        return
