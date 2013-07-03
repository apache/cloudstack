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

import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *


class Services:
    """Test network offering Services
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
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
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
                                  "vlan" : 3111,
                                  "startip": "172.16.15.2",
                                  "endip" : "172.16.15.10",
                                  "gateway" : "172.16.15.1",
                                  "netmask" : "255.255.255.0",
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

class TestSharedNetworkWithoutIp(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestSharedNetworkWithoutIp,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
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

        cls._cleanup = [
                        cls.service_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator", "network", "api"])
    def test_deployVmSharedNetworkWithoutIpRange(self):
        """Test deployVM in shared network without startIp/endIp
        """

        # Steps for validation
        # 1. create a shared network using shared network offering but do not
        #    specify startIp/endIp arguments
        # 2. create an account
        # 3. deploy a VM in this account using the above network
        # Validate the following
        # 1. listNetworks should return the created network
        # 2. listAccounts to return the created account
        # 3. VM deployment should succeed and NIC is in networks address space
        # 4. delete the account

        self.debug(
                "Fetching default shared network offering from nw offerings")
        network_offerings = NetworkOffering.list(
                                    self.apiclient,
                                    listall=True,
                                    guestiptype="Shared",
                                    name="DefaultSharedNetworkOffering",
                                    displaytext="Offering for Shared networks"
                                    )
        self.assertEqual(
                    isinstance(network_offerings, list),
                    True,
                    "Nw offerings should have atleast a shared nw offering"
                    )
        shared_nw_off = network_offerings[0]
        self.debug("Shared netwrk offering: %s" % shared_nw_off.name)

        self.debug("Creating a network from shared network offering")
        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=shared_nw_off.id,
                                    zoneid=self.zone.id
                                    )
        self.debug("Created network with ID: %s" % self.network.id)

        self.debug("Deploying VM in account: %s" % self.account.name)
        try:
            # Spawn an instance in that network
            VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
            self.debug("Deployed VM in network: %s" % self.network.id)
        except Exception as e:
            self.fail("Deply Vm in shared network failed! - %s" % e)
        return