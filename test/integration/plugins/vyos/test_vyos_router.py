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

""" P1 tests for Vyos Router Plugin
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
                             Network,
                             PublicIPAddress,
                             FireWallRule,
                             EgressFireWallRule,
                             NATRule,
                             StaticNATRule,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.sshClient import SshClient
from marvin.codes import PASS
import subprocess
import time
import urllib

class Services:
    """Test Vyos Router plugin
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
                         
                         "vyos_router_device": {
                                "ip": '192.168.2.91',
                                "username": 'vyos',
                                "password": 'password',
                                "timeout": '300',
                                "publicinterface": 'eth0',
                                "privateinterface": 'eth0',
                                "networkdevicetype": 'VyosRouter'
                         },
                         #services supported by Vyos Router for isolated networks.
                         "network_offering": {
                                    "name": 'Vyos Router network offering (marvin)',
                                    "displaytext": 'Vyos (marvin)',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Lb,SourceNat,StaticNat,PortForwarding,Firewall,Dns,UserData,Vpn',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "conservemode": 'true',
                                    "egress_policy": 'false',
                                    "serviceProviderList": {
                                            "PortForwarding": 'VyosRouter',
                                            "Dhcp" : 'VirtualRouter',
                                            "StaticNat" : 'VyosRouter',
                                            "SourceNat" : 'VyosRouter',
                                            "Firewall" : 'VyosRouter',
                                             "Lb": 'VirtualRouter',
                                             "Dns": 'VirtualRouter', 
                                             "UserData": 'VirtualRouter', 
                                             "Vpn": 'VirtualRouter'
                                    },                                              
                         },
                         "network": {
                                  "name": "Vyos network",
                                  "displaytext": "Vyos network",
                         },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         "sleep": 60,
                         "timeout": 10
                    }
class TestVyosRouter(cloudstackTestCase):


    @classmethod
    def encodeURL(cls):
        cls.url = "https://"+cls.vyos_services["ip"]+"?"+"publicinterface="+cls.vyos_services["publicinterface"]+"&privateinterface="+cls.vyos_services["privateinterface"]+"&timeout="+cls.vyos_services["timeout"]+"&fwdevicededicated=false"
        #cls.url=urllib.quote_plus(url)
        cls.debug("********************* Building encoded url variable:\n"+cls.url)
        return cls.url
        #resp_add_device.url = cls.vyos_services["url"]
               #resp_add_device.timeout = cls.vyos_services["timeout"]
               #resp_add_device.publicinterface = cls.vyos_services["publicinterface"]
               #resp_add_device.privateinterface = cls.vyos_services["privateinterface"]
        #url=https%3A%2F%2F192.168.2.91%3Fpublicinterface%3Deth0%26privateinterface%3Deth1%26timeout%3D300%26fwdevicededicated%3Dfalse
        
    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestVyosRouter, cls).getClsTestClient()
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
        cls.vyos_services = cls.services["vyos_router_device"]
        try:
           # Get physical network
           resp = listPhysicalNetworks.listPhysicalNetworksCmd()
           resp.zoneid = cls.zone.id
           physical_networks = cls.api_client.listPhysicalNetworks(resp)
           if isinstance(physical_networks, list):
               cls.debug("phy net %s" % physical_networks)
               physical_network = physical_networks[1]

           # Get vyos router network service provider
           resp = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
           resp.name = 'VyosRouter'
           resp.physicalnetworkid = physical_network.id
           nw_service_providers = cls.api_client.listNetworkServiceProviders(resp)
           if isinstance(nw_service_providers, list):
               vyos_provider = nw_service_providers[0]
               cls.debug("net serv prov %s" % nw_service_providers)
           else:
               # Create vyos provider if not already existent
               resp_add_nsp = addNetworkServiceProvider.addNetworkServiceProviderCmd()
               resp_add_nsp.name = 'VyosRouter'
               resp_add_nsp.physicalnetworkid = physical_network.id
               vyos_provider = cls.api_client.addNetworkServiceProvider(resp_add_nsp)
              
               resp_add_device = addVyosRouterFirewall.addVyosRouterFirewallCmd()
               resp_add_device.physicalnetworkid = physical_network.id
               resp_add_device.username = cls.vyos_services["username"]
               resp_add_device.password = cls.vyos_services["password"]
               
               resp_add_device.url=cls.encodeURL();
               
               # The remaining variables must be encoded in their own URI string.
               #resp_add_device.url = cls.vyos_services["url"]
               #resp_add_device.timeout = cls.vyos_services["timeout"]
               #resp_add_device.publicinterface = cls.vyos_services["publicinterface"]
               #resp_add_device.privateinterface = cls.vyos_services["privateinterface"]
               
               resp_add_device.networkdevicetype = cls.vyos_services["networkdevicetype"]
               
               cls.vyos = cls.api_client.addVyosRouterFirewall(resp_add_device)

           if vyos_provider.state != 'Enabled':
               cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
               cmd.id = vyos_provider.id
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
            cls.debug("Skipping tearDownClass(cls) so we can manually inspect the state of the system")
            #cleanup_resources(cls.api_client, cls._cleanup)
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
            self.debug("Skipping tearDown(self) so we can manually inspect the state of the system")
            #self.debug("Cleaning up the resources")
            #cleanup_resources(self.apiclient, self._cleanup)
            #interval = Configurations.list(
            #                        self.apiclient,
            #                        name='network.gc.interval'
            #                        )
            #wait = Configurations.list(
            #                        self.apiclient,
            #                        name='network.gc.wait'
            #                        )
            #time.sleep(int(interval[0].value) + int(wait[0].value))
            #self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    @attr(tags = ["advancedns"], required_hardware="true")
    def test_network_vyos(self):
        """Test basic workflow with Vyos Router plugin
           1. Create a guest network
           2. Create two VMs on the network
           3. Add firewall rule to allow ssh 
           4. Add Egress rule to allow outgoing ping
           5. Add nat rule to route ssh to vm1           
           6. Add a second public ip to the network
           7. Add static NAT from public ip #2 to vm_2, with firewall rule to allow ssh
           8. Ssh to vm_1, ping vm_2 private address, ping google.com
           9. Ssh to vm_2, ping vm_1 private address, ping google.com
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

        self.debug("STEP 3: Add FW rule to allow ssh")
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

        #Create Firewall rule
        fw_rule_ssh = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=src_nat.id,
                            protocol='TCP',
                            cidrlist=["0.0.0.0/0",],
                            startport='22',
                            endport='22'
                            )
        self.debug("Created firewall rule: %s" % fw_rule_ssh.id)
        
        self.debug("STEP 4: Add Egress rule to allow ping")
        # Skip this if default egress is allow
        
        egress_rule_icmp=EgressFireWallRule.create(self.apiclient,
                                               networkid=self.network.id, 
                                               protocol="ICMP", 
                                               cidrlist=["0.0.0.0/0",],
                                               startport="", 
                                               endport="", 
                                               type="-1", 
                                               code="-1"
                                               )
        self.debug("Created Egress rule: %s" % egress_rule_icmp.id)
        
        self.debug("STEP 5: Creating PF rule routing ssh from %s to vm1" % src_nat.ipaddress)
        nat_rule_ssh = NATRule.create(
                                  self.apiclient,
                                  vm_1,
                                  self.services["natrule"],
                                  ipaddressid=src_nat.id,
                                  openfirewall=False,
                                  networkid=self.network.id
                                  )
        self.debug("Created PF rule: %s" % nat_rule_ssh.id)

        self.debug("STEP 6: Add additional public ip address")
        floating_ip_1 = PublicIPAddress.create(
                                               self.apiclient,
                                               accountid=self.account.name,
                                               zoneid=self.zone.id,
                                               domainid=self.account.domainid,
                                               networkid=self.network.id
                                              )
        self.debug("Associated %s with network %s" % (
                                                      floating_ip_1.ipaddress.ipaddress,
                                                      self.network.id
                                                     )
                  )
        
        self.debug("STEP 7: create static nat rule associating new ip with vm2 and open firewall for ssh to vm2")
        static_rule = StaticNATRule.enable(
                                        self.apiclient,
                                        floating_ip_1.ipaddress.id, 
                                        vm_2.id, 
                                        self.network.id
                                        )
        
        self.debug("open firewall for ssh to vm2")
        #Create Firewall rule
        fw_rule_ssh = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=floating_ip_1.ipaddress.id,
                            protocol='TCP',
                            cidrlist=["0.0.0.0/0",],
                            startport='22',
                            endport='22'
                            )
        
        
        # Should be able to SSH vm_1 via static nat, then ping vm_2 & Internet
        try:
            self.debug("STEP 8: SSH into vm_1: %s" % src_nat)

            ssh = vm_1.get_ssh_client(
                                  ipaddress=src_nat.ipaddress
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
        
        # Should be able to SSH vm_1 via static nat, then ping vm_2 & Internet
        try:
            self.debug("STEP 9: SSH into vm_2: %s" % floating_ip_1)

            ssh = vm_2.get_ssh_client(
                                  ipaddress=floating_ip_1.ipaddress.ipaddress
                                  )
            self.debug("Ping vm_1 at %s" % vm_1.ipaddress)
            # Ping vm_1
            res_1 = ssh.execute("ping -c 1 %s" % vm_1.ipaddress)

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


        self.debug("Skipping delete of vms and network so we can manually inspect system state")
        # Deleting two test VMs
        
        #VirtualMachine.delete(vm_1, self.apiclient, expunge=True)
        #VirtualMachine.delete(vm_2, self.apiclient, expunge=True)

        # Delete Network
        
        #Network.delete(self.network, self.apiclient)

        return
    