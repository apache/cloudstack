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
""" Tests for NuageNetwork VPC
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (cleanup_resources)
from marvin.cloudstackAPI import (listPhysicalNetworks,
                                  listNetworkServiceProviders,
                                  addNetworkServiceProvider,
                                  updateNetworkServiceProvider,
                                  addNuageVspDevice,
                                  destroyVirtualMachine)
from marvin.lib.base import (VirtualMachine,
                             ServiceOffering,
                             Account,
                             NetworkOffering,
                             Network,
                             VPC,
                             VpcOffering,
                             NetworkACL,
                             NetworkACLList)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               wait_for_cleanup,
                               list_networks)

from nose.plugins.attrib import attr

class Services:

    """Test NuageVsp plugin
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "cloudstack@cloudmonkey.com",
                "firstname": "cloudstack",
                "lastname": "bob",
                "username": "admin",
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
            "nuage_vsp_device": {
                #"hostname": '192.168.0.7',
                #"hostname": '10.31.43.226',
                "hostname": '172.31.222.162',
                "username": 'cloudstackuser1',
                "password": 'cloudstackuser1',
                "port": '8443',
                "apiversion": 'v3_2',
                "retrycount": '4',
                "retryinterval": '60'
            },
            # services supported by Nuage for VPC networks.
            "vpc_network_offering": {
                "name": 'nuage_vpc_marvin',
                "displaytext": 'nuage_vpc_marvin',
                "guestiptype": 'Isolated',
                "supportedservices": 'UserData,Dhcp,SourceNat,StaticNat,NetworkACL,Connectivity',
                "traffictype": 'GUEST',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Dhcp": "NuageVsp",
                    "SourceNat": "NuageVsp",
                    "StaticNat": "NuageVsp",
                    "NetworkACL": "NuageVsp",
                    "UserData": "VpcVirtualRouter",
                    "Connectivity": "NuageVsp"
                },
                "serviceCapabilityList": {
                    "SourceNat": {"SupportedSourceNatTypes": "perzone"}
                }
            },
            "vpc": {
                "name": "vpc-networkacl-nuage",
                "displaytext": "vpc-networkacl-nuage",
                "cidr": '10.1.0.0/16'
            },
            "vpcnetwork": {
                "name": "nuagevpcnetwork",
                "displaytext": "nuagevpcnetwork",
                "netmask": '255.255.255.128'
            },
            "ostype": 'CentOS 5.5 (64-bit)',
            "sleep": 60,
            "timeout": 10
        }


class TestVpcNetworkNuage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestVpcNetworkNuage, cls).getClsTestClient()
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
        # nuage vsp device brings the Nuage virtual service platform into play
        cls.nuage_services = cls.services["nuage_vsp_device"]
        try:

            resp = listPhysicalNetworks.listPhysicalNetworksCmd()
            print "in cls.setupClass- resp: %s" % resp
            resp.zoneid = cls.zone.id
            physical_networks = cls.api_client.listPhysicalNetworks(resp)
            for pn in physical_networks:
                if pn.isolationmethods=='VSP':
                    physical_network = pn
            #if isinstance(physical_networks, list):
            #    physical_network = physical_networks[1]
            resp = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
            resp.name = 'NuageVsp'
            resp.physicalnetworkid = physical_network.id
            nw_service_providers = cls.api_client.listNetworkServiceProviders(
                resp)
            if not isinstance(nw_service_providers, list):
                # create network service provider and add nuage vsp device
                resp_add_nsp = \
                    addNetworkServiceProvider.addNetworkServiceProviderCmd()
                resp_add_nsp.name = 'NuageVsp'
                resp_add_nsp.physicalnetworkid = physical_network.id
                cls.api_client.addNetworkServiceProvider(resp_add_nsp)
                #Get NSP ID
                nw_service_providers = cls.api_client.listNetworkServiceProviders(
                    resp)
                cls.debug("NuageVsp NSP ID: %s" % nw_service_providers[0].id)

                resp_add_device = addNuageVspDevice.addNuageVspDeviceCmd()
                resp_add_device.physicalnetworkid = physical_network.id
                resp_add_device.username = cls.nuage_services["username"]
                resp_add_device.password = cls.nuage_services["password"]
                resp_add_device.hostname = cls.nuage_services["hostname"]
                resp_add_device.port = cls.nuage_services["port"]
                resp_add_device.apiversion = cls.nuage_services[
                    "apiversion"]
                resp_add_device.retrycount = cls.nuage_services[
                    "retrycount"]
                resp_add_device.retryinterval = cls.nuage_services[
                    "retryinterval"]
                cls.nuage = cls.api_client.addNuageVspDevice(
                    resp_add_device)
                #Enable NuageVsp NSP
                cls.debug("NuageVsp NSP ID : %s" % nw_service_providers[0].id)
                resp_up_nsp = \
                    updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
                resp_up_nsp.id = nw_service_providers[0].id
                resp_up_nsp.state = 'Enabled'
                cls.api_client.updateNetworkServiceProvider(resp_up_nsp)

            cls.network_offering = NetworkOffering.create(
                cls.api_client,
                cls.services["vpc_network_offering"],
                conservemode=False
            )
            cls._cleanup.append(cls.network_offering)

            cls.network_offering.update(cls.api_client, state='Enabled')
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Unable to add VSP device")
        return

    @attr(tags=["advanced"])
    def test_vpcnetwork_nuage(self):
        """Test network VPC for Nuage"""

        # 1) Create VPC with Nuage VPC offering
        vpcOffering = VpcOffering.list(self.apiclient,name="Nuage VSP VPC offering")
        self.assert_(vpcOffering is not None and len(vpcOffering)>0, "Nuage VPC offering not found")
        vpc = VPC.create(
                apiclient=self.apiclient,
                services=self.services["vpc"],
                networkDomain="vpc.networkacl",
                vpcofferingid=vpcOffering[0].id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
        )
        self.assert_(vpc is not None, "VPC creation failed")

        # 2) Create ACL
        aclgroup = NetworkACLList.create(apiclient=self.apiclient, services={}, name="acl", description="acl", vpcid=vpc.id)
        self.assertIsNotNone(aclgroup, "Failed to create NetworkACL list")
        self.debug("Created a network ACL list %s" % aclgroup.name)

        # 3) Create ACL Item
        aclitem = NetworkACL.create(apiclient=self.apiclient, services={},
            protocol="TCP", number="10", action="Deny", aclid=aclgroup.id, cidrlist=["0.0.0.0/0"])
        self.assertIsNotNone(aclitem, "Network failed to aclItem")
        self.debug("Added a network ACL %s to ACL list %s" % (aclitem.id, aclgroup.name))

        # 4) Create network with ACL
        nwNuage = Network.create(
            self.apiclient,
            self.services["vpcnetwork"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            aclid=aclgroup.id,
            gateway='10.1.0.1'
        )
        self.debug("Network %s created in VPC %s" %(nwNuage.id, vpc.id))

        # 5) Deploy a vm
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(nwNuage.id)]
        )
        self.assert_(vm is not None, "VM failed to deploy")
        self.assert_(vm.state == 'Running', "VM is not running")
        self.debug("VM %s deployed in VPC %s" %(vm.id, vpc.id))

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
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
