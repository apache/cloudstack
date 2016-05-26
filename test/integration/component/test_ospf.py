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
""" BVT tests for Service offerings"""

# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient


_multiprocess_shared_ = True


class TestCreateDynmaicServiceOffering(cloudstackTestCase):

    def setUp(self):
        
        self.logger = logging.getLogger('TestOSPF')
        self.stream_handler = logging.StreamHandler()
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.stream_handler)

        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        print str(self)
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.cleanup = []
        self.services = { 
                         "batman_vpc": {
                                 "name": "Batman VPC",
                                 "displaytext": "Marvin Batman VPC",
                                 "netmask": "255.255.252.0"
                                 },
                         "superman_vpc": {
                                 "name": "Superman VPC",
                                 "displaytext": "Marvin Superman VPC",
                                 "netmask": "255.255.252.0"
                                 },
                          "vpc_offering": {
                                    "name": 'Cosmic VPC off',
                                    "displaytext": 'Cosmic VPC off',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,VPCDynamicRouting',
                                },
                          "network_offering": {
                                    "name": 'Milkyway VPC Network offering',
                                    "displaytext": 'Milkyway VPC Network off',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL,VPCDynamicRouting',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Vpn": 'VpcVirtualRouter',
                                            "Dhcp": 'VpcVirtualRouter',
                                            "Dns": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "Lb": 'VpcVirtualRouter',
                                            "UserData": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "VPCDynamicRouting": 'VpcVirtualRouter',
                                            "NetworkACL": 'VpcVirtualRouter'
                                        },
                                },
                           "batman_network_tier1": {
                                "name": "Batman Network Tier 1",
                                "displaytext": "Batman Network Tier 1",
                                "netmask": '255.255.255.0'
                            },
                           "superman_network_tier1": {
                                "name": "Superman Network Tier 1",
                                "displaytext": "Superman Network Tier 1",
                                "netmask": '255.255.255.0'
                            },
                            "service_offering": {
                                "name": "Ultra Tiny Instance",
                                "displaytext": "Ultra Tiny Instance",
                                "cpunumber": 1,
                                "cpuspeed": 100,
                                "memory": 128,
                            },
                            "batman_vm_1": {
                                "displayname": "Batman-VPC-VM-001",
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
                            "superman_vm_1": {
                                "displayname": "Superman-VPC-VM-001",
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
                            "natrule": {
                                "privateport": 22,
                                "publicport": 22,
                                "startport": 22,
                                "endport": 22,
                                "protocol": "TCP",
                                "cidrlist": '0.0.0.0/0',
                            },
                         "template_name" : "macchinina-xen",
                         "ostype": 'Other Linux (64-bit)',
                         "sleep": 60,
                         "timeout": 10,
                         }
        

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    
    
    def validate_vpc_offering(self, vpc_offering):
        """Validates the VPC offering"""

        self.logger.debug("Check if the VPC offering is created successfully?")
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
        self.logger.debug(
                "VPC offering is created successfully - %s" %
                                                        vpc_offering.name)
    
    
    def validate_network(self, network, vpcid):
        """Validates the VPC offering"""
    
        self.logger.debug(
            "Verifying list network response to check if network created?")
        networks = Network.list(
            self.apiclient,
            id=network.id,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response"
        )
        nw = networks[0]

        self.assertEqual(
            nw.networkofferingid,
            self.network_offering.id,
            "Network should be created from network offering - %s" %
            self.network_offering.id
        )
        self.assertEqual(
            nw.vpcid,
            vpcid,
            "Network should be created in VPC: %s" % vpcid
        )
        

    def validate_vpc_network(self, network):
        """Validates the VPC network"""

        self.logger.debug("Check if the VPC network is created successfully?")
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
        self.logger.debug("VPC network created successfully - %s" % network.name)
  

    def set_ospf_zone(self):
        cmd = vpcOSPFConfigUpdate.vpcOSPFConfigUpdateCmd()
        cmd.zoneid = self.zone.id
        cmd.superCIDR = "200.100.0.0/16"
        cmd.enabled = "true"
        response = self.apiclient.vpcOSPFConfigUpdate(cmd)
        self.logger.debug("Response ospfEnabled: %s" % response.enabled)
        self.logger.debug("Response ospfSuperCIDR: %s" % response.superCIDR)
         
        cmd = vpcOSPFConfig.vpcOSPFConfigCmd()
        cmd.zoneid = self.zone.id
        response = self.apiclient.vpcOSPFConfig(cmd)
        self.logger.debug("Response ospf Enabled: %s" % response.enabled)
        self.logger.debug("Response ospfSuperCIDR: %s" % response.superCIDR)
        
        cmd.zoneid = self.zone.id
        
        
        self.assertEqual(
                         response.enabled, True
                         )
        
        
        self.assertEqual(
                         response.superCIDR, "200.100.0.0/16"
                         )
    
    def create_tier(self, vpc_id, srvc, gtwy):
           # 2) Create ACL
        aclgroup = NetworkACLList.create(apiclient=self.apiclient, services={}, name="acl", description="acl", vpcid=vpc_id)
        self.assertIsNotNone(aclgroup, "Failed to create NetworkACL list")
        self.logger.debug("Created a network ACL list %s" % aclgroup.name)

        # 3) Create ACL Items
        aclitem1 = NetworkACL.create(apiclient=self.apiclient, services={},
            protocol="ICMP", action="Allow", aclid=aclgroup.id, cidrlist=["0.0.0.0/0"])
        self.assertIsNotNone(aclitem1, "Network failed to aclItem")
        self.logger.debug("Added a network ACL %s to ACL list %s" % (aclitem1.id, aclgroup.name))
        
        aclitem2 =  NetworkACL.create( self.apiclient, aclid=aclgroup.id, services=self.services["natrule"], 
                                                   traffictype='Ingress' )
        self.assertIsNotNone(aclitem2, "Network failed to aclItem")
        self.logger.debug("Added a network ACL %s to ACL list %s" % (aclitem2.id, aclgroup.name))

        network_tier1 = Network.create(
            self.apiclient,
            srvc,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            aclid=aclgroup.id,
            gateway=gtwy,
            vpcid=vpc_id
        )
        self.validate_network(network_tier1, vpc_id)
        self.logger.debug("Created network_tier1 with ID: %s" % network_tier1.id)
        
        return network_tier1
    
    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_01_dynamic_routing_using_ospf(self):
        self.set_ospf_zone()
        
        self.vpc_off = VpcOffering.create(
                                     self.apiclient,
                                     self.services["vpc_offering"]
                                     )
        self.vpc_off.update(self.apiclient, state='Enabled')
        
        self.validate_vpc_offering(self.vpc_off)
        self.logger.debug("VPC Offering id = %s" % self.vpc_off.id)
        
        """  vpc_super_netmask=255.255.252.0 """
        self.batman_vpc = VPC.create(
            self.apiclient,
            self.services["batman_vpc"],
            netmask="255.255.252.0",
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id
        )
        
        self.validate_vpc_network(self.batman_vpc)
        self.logger.debug("VPC batman_vpc id = %s" % self.batman_vpc.id)
        
        self.superman_vpc = VPC.create(
            self.apiclient,
            self.services["superman_vpc"],
            netmask="255.255.252.0",
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id
        )
        
        self.validate_vpc_network(self.superman_vpc)
        self.logger.debug("VPC superman_vpc id = %s" % self.superman_vpc.id)
        
        """ -----------------------------------------------"""
        
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
            conservemode=False
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.logger.debug("Dynamic Network offering id = %s" % self.network_offering.id)
        
        """ --------------------BATMAN---------------------------"""
        """ create tiers for batman vpc """
        """
         vpc_tier1_name=batman-tier-1
         vpc_tier1_display_name='"Batman Tier 1"'
         vpc_tier1_gateway=200.100.1.1
         vpc_tier1_netmask=255.255.255.0
         """
        self.batman_network_tier1 = self.create_tier(self.batman_vpc.id, self.services["batman_network_tier1"], '200.100.1.1')
        
        """ ----------------------SUPERMAN-------------------------"""
        """
         vpc2_tier1_name=robin-tier-1
         vpc2_tier1_display_name='"Robin Tier 1"'
         vpc2_tier1_gateway=200.100.5.1
         vpc2_tier1_netmask=255.255.255.0 
         """
        self.superman_network_tier1 = self.create_tier(self.superman_vpc.id, self.services["superman_network_tier1"], '200.100.5.1')
        
        """---------------------------------------------------------"""
        
        #create VMs
        self.template = get_template(
            self.apiclient,
            self.zone.id,
            template_name = self.services["template_name"]
        )
        
        self.logger.debug("Using template %s " % self.template.id)
        
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"]
        )
        self.logger.debug("Using service offering %s " % self.service_offering.id)

        self.services["batman_vm_1"]["zoneid"] = self.zone.id
        self.services["batman_vm_1"]["template"] = self.template.id
        self.batman_vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["batman_vm_1"],
            serviceofferingid=self.service_offering.id,
            ipaddress="200.100.1.101",
            networkids=self.batman_network_tier1.id
        )
        self.logger.debug("Deployed VM %s in network: %s" % (self.batman_vm_1.id, self.batman_network_tier1.id))
        
        self.services["superman_vm_1"]["zoneid"] = self.zone.id
        self.services["superman_vm_1"]["template"] = self.template.id
        self.superman_vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["superman_vm_1"],
            serviceofferingid=self.service_offering.id,
            ipaddress="200.100.5.101",
            networkids=self.superman_network_tier1.id
        )
        self.logger.debug("Deployed VM %s in network: %s" % (self.superman_vm_1.id, self.superman_network_tier1.id))
        
        """ check ping from VR of batman to vm of superman""" 
        self.batman_vm_pip = PublicIPAddress.create(
            self.apiclient,
            zoneid=self.zone.id,
            networkid=self.batman_network_tier1.id,
            vpcid=self.batman_vpc.id
        )
        self.logger.debug("Acquired public ip for batman vm %s" % self.batman_vm_pip.ipaddress.ipaddress )
    
        StaticNATRule.enable(
            self.apiclient,
            ipaddressid= self.batman_vm_pip.ipaddress.id,
            virtualmachineid=self.batman_vm_1.id,
            networkid=self.batman_network_tier1.id
        )
        
        self.logger.debug("Static Nat enabled on ip %s" % self.batman_vm_pip.ipaddress.ipaddress )
        
        ssh = SshClient(self.batman_vm_pip.ipaddress.ipaddress, 22, "root", "password") 
        res = ssh.execute("ping -c 1 200.100.5.101")
        print res
        result = str(res)
        self.assertEqual(
            result.count("0% packet loss"),
            1,
            "Ping to other VM @ 200.100.5.101 succeeded"
        )
        res = ssh.execute("ping -c 1 200.100.1.101")
        print res
        result = str(res)
        self.assertEqual(
            result.count("0% packet loss"),
            1,
            "Ping to other VM @ 200.100.5.101 succeeded"
        )
        
        self.logger.debug("Test successful clearing VMs")
        self.cleanup.append(self.batman_vm_1)
        self.cleanup.append(self.superman_vm_1)
        self.cleanup.append(self.service_offering)
        self.logger.debug("Test successful clearing VPC Tiers")
        self.cleanup.append(self.superman_network_tier1)
        self.cleanup.append(self.batman_network_tier1)
        self.cleanup.append(self.network_offering)
        self.logger.debug("Test successful clearing VPCs")
        self.cleanup.append(self.superman_vpc)
        self.cleanup.append(self.batman_vpc)
        self.cleanup.append(self.vpc_off)
        
        return

   