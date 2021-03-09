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


from nose.plugins.attrib import attr
from marvin.lib.base import *
from marvin.lib.utils import *
from marvin.lib.common import *

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import *

class Services:
    """Test Services for customer defects
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
                                    "cpuspeed": 100,
                                    "memory": 128,
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "disksize": 1
                        },
                        "virtual_machine": {
                                    "displayname": "Test VM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "static_nat": {
                                    "startport": 22,
                                    "endport": 22,
                                    "protocol": "TCP"
                        },
                        "network_offering": {
                                    "name": 'Network offering-RVR services',
                                    "displaytext": 'Network off-RVR services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Vpn": 'VirtualRouter',
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                                    "serviceCapabilityList": {
                                        "SourceNat": {
                                            "SupportedSourceNatTypes": "peraccount",
                                            "RedundantRouter": "true",
                                        },
                                        "lb": {
                                               "SupportedLbIsolation": "dedicated"
                                        },
                                    },
                        },
                        "host": {
                                 "username": "root",
                                 "password": "password",
                                 "publicport": 22,
                        },
                        "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                },
                        "lbrule": {
                                    "name": "SSH",
                                    "alg": "roundrobin",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 22,
                                    "openfirewall": True,
                                },
                        "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": "TCP"
                                },
                        "natrule_221": {
                                    "privateport": 22,
                                    "publicport": 221,
                                    "protocol": "TCP"
                                },
                        "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '55.55.0.0/11',
                                    # Any network (For creating FW rule)
                                    "protocol": 'TCP',
                                },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        "sleep": 60,
            }


class TestEnableVPNOverRvR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestEnableVPNOverRvR, cls).getClsTestClient()
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

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering,
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
        self.cleanup.insert(0, self.account)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="false")
    def test_enableVPNOverRvR(self):
        """Test redundant router internals
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list PRIMARY and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listRemoteAccessVpns for the network associated should show VPN
        #    created
        # 7. listRemoteAccessVpns for the network associated should return
        #    empty response

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
                                self.apiclient,
                                id=network.id,
                                listall=True
                                )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
             )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
                    nw_response.state,
                    "Allocated",
                    "The network should be in allocated state after creation"
                    )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
            )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List Vms should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Vm should be in running state after deployment"
                         )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & Primary)"
                    )

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating a remote access VPN for account: %s" %
                                                self.account.name)

        try:
            vpn = Vpn.create(
                         self.apiclient,
                         publicipid=public_ip.ipaddress.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        except Exception as e:
            self.fail("Failed to create VPN for account: %s - %s" % (
                                                 self.account.name, e))

        try:
            vpnuser = VpnUser.create(
                                 self.apiclient,
                                 username="root",
                                 password="password",
                                 account=self.account.name,
                                 domainid=self.account.domainid
                                 )
        except Exception as e:
            self.fail("Failed to create VPN user: %s" % e)

        self.debug("Checking if the remote access VPN is created or not?")
        remote_vpns = Vpn.list(
                               self.apiclient,
                               account=self.account.name,
                               domainid=self.account.domainid,
                               publicipid=public_ip.ipaddress.id,
                               listall=True
                               )
        self.assertEqual(
                         isinstance(remote_vpns, list),
                         True,
                         "List remote VPNs should not return empty response"
                         )
        self.debug("Deleting the remote access VPN for account: %s" %
                                                self.account.name)

        try:
            vpn.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete VPN : %s" % e)

        self.debug("Checking if the remote access VPN is created or not?")
        remote_vpns = Vpn.list(
                               self.apiclient,
                               account=self.account.name,
                               domainid=self.account.domainid,
                               publicipid=public_ip.ipaddress.id,
                               listall=True
                               )
        self.assertEqual(
                         remote_vpns,
                         None,
                         "List remote VPNs should not return empty response"
                         )
        return

