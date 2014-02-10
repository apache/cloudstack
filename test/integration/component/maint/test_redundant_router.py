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
from marvin.integration.lib.base import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.common import *

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


class TestCreateRvRNetworkOffering(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestCreateRvRNetworkOffering,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls._cleanup = []
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_createRvRNetworkOffering(self):
        """Test create RvR supported network offering
        """

        # Steps to validate
        # 1. create a network offering
        # - all services by VirtualRouter
        # - enable RedundantRouter servicecapability
        # 2. enable the network offering
        # Validate the following
        # 1. Redundant Router offering should be created successfully and
        #    listed in listNetworkOfferings response
        # assert if RvR capability is enabled

        self.debug("Creating network offering with redundant VR capability")
        try:
            network_offering = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=True
                                            )
        except Exception as e:
            self.fail("Create network offering failed! - %s" % e)

        self.debug("Enabling network offering - %s" % network_offering.name)
        # Enable Network offering
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        self.debug("Checking if the network offering created successfully?")
        network_offs = NetworkOffering.list(
                                            self.apiclient,
                                            id=network_offering.id,
                                            listall=True
                                            )
        self.assertEqual(
                    isinstance(network_offs, list),
                    True,
                    "List network offering should not return empty response"
                 )
        self.assertEqual(
                     len(network_offs),
                     1,
                    "List network off should have newly created network off"
                     )
        for service in network_offs[0].service:
            if service.name == 'SourceNat':
                self.debug("Verifying SourceNat capabilites")
                for capability in service.capability:
                    if capability.name == 'RedundantRouter':
                        self.assertTrue(capability.value=='true')
                        self.debug("RedundantRouter is enabled")

        return


class TestCreateRvRNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestCreateRvRNetwork,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        cls._cleanup.append(cls.network_offering)

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

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

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_createRvRNetwork(self):
        """Test create network with redundant routers
        """

        # Validate the following:
        # 1. listNetworkOfferings shows created offering
        # 2. listNetworks should show created network in Allocated state
        # 3. returns no Running routers in the network
        # 4. listVirtualmachines shows VM in Running state
        # 5. returns 2 routers
        # - same public IP
        # - same MAC address of public NIC
        # - different guestip address
        # - redundant state (MASTER or BACKUP)
        # - same gateway for the public traffic
        # 6. all routers, networks and user VMs are cleaned up

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

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Redundant states: %s, %s" % (
                                                master_router.redundantstate,
                                                backup_router.redundantstate
                                                ))
        self.assertEqual(
                         master_router.publicip,
                         backup_router.publicip,
                         "Public Ip should be same for both(MASTER & BACKUP)"
                         )
        self.assertEqual(
                      master_router.redundantstate,
                      "MASTER",
                      "Redundant state of router should be MASTER"
                      )
        self.assertEqual(
                      backup_router.redundantstate,
                      "BACKUP",
                      "Redundant state of router should be BACKUP"
                      )
        self.assertNotEqual(
                master_router.guestipaddress,
                backup_router.guestipaddress,
                "Both (MASTER & BACKUP) routers should not have same guest IP"
                )

        self.assertNotEqual(
                master_router.guestmacaddress,
                backup_router.guestmacaddress,
                "Both (MASTER & BACKUP) routers should not have same guestMAC"
                )
        return


class TestCreateRvRNetworkNonDefaultGuestCidr(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestCreateRvRNetworkNonDefaultGuestCidr,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
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

    @attr(tags=["advanced", "advancedns"])
    def test_createRvRNetwork(self):
        """Test create network with non-default guest cidr with redundant routers
        """

        # Validate the following:
        # 1. listNetworkOfferings shows created offering
        # 2. listNetworks should show created network in Allocated state
        # - gw = 192.168.2.1 and cidr = 192.168.2.0/23
        # 3. returns no Running routers in the network
        # 4. listVirtualmachines shows VM in Running state
        # 5. returns 2 routers
        # - same public IP
        # - same MAC address of public NIC
        # - different guestip address
        # - redundant state (MASTER or BACKUP)
        # - same gateway for the public traffic
        # 6. all routers, networks and user VMs are cleaned up

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id,
                                netmask='255.255.254.0',
                                gateway='192.168.2.1'
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
        self.assertEqual(
                         nw_response.gateway,
                         '192.168.2.1',
                         "The gateway should be 192.168.2.1"
                         )
        self.assertEqual(
                         nw_response.cidr,
                         '192.168.2.0/23',
                         "Guest cidr should be 192.168.2.0/23 but is %s" % nw_response.cidr
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

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.assertEqual(
                         master_router.publicip,
                         backup_router.publicip,
                         "Public Ip should be same for both(MASTER & BACKUP)"
                         )
        self.assertEqual(
                      master_router.redundantstate,
                      "MASTER",
                      "Redundant state of router should be MASTER"
                      )
        self.assertEqual(
                      backup_router.redundantstate,
                      "BACKUP",
                      "Redundant state of router should be BACKUP"
                      )
        self.assertNotEqual(
                master_router.guestipaddress,
                backup_router.guestipaddress,
                "Both (MASTER & BACKUP) routers should not have same guest IP"
                )

        self.assertNotEqual(
                master_router.guestmacaddress,
                backup_router.guestmacaddress,
                "Both (MASTER & BACKUP) routers should not have same guestMAC"
                )
        return


class TestRVRInternals(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRVRInternals,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
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

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_redundantVR_internals(self):
        """Test redundant router internals
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. login to both Redundant Routers
        # 5. login to user VM
        # 6. delete user account
        # Validate the following:
        # 1. listNetworks lists network in Allocated state
        # 2. listRouters lists no routers created yet
        # 3. listRouters returns Master and Backup routers
        # 4. ssh in to both routers and verify:
        #  - MASTER router has eth2 with public Ip address
        #  - BACKUP router has only guest eth0 and link local eth1
        #  - Broadcast on MASTER eth2 is non-zero (0.0.0.0)
        #  - execute checkrouter.sh in router home and check if it is status
        #    "MASTER|BACKUP" as returned by the listRouters API
        # 5. DNS of the user VM is set to RedundantRouter Gateway
        #     (/etc/resolv.conf)
        #    Check that the default gateway for the guest is the rvr gateway
        #    and not the guestIp of either of the RvRs

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

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Fetching the host details for double hop into router")

        hosts = Host.list(
                          self.apiclient,
                          id=master_router.hostid,
                          listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should return a valid list"
                         )
        master_host = hosts[0]
        self.debug("Host for master router: %s" % master_host.name)
        self.debug("Host for master router: %s" % master_host.ipaddress)

        hosts = Host.list(
                          self.apiclient,
                          id=backup_router.hostid,
                          listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should return a valid list"
                         )
        backup_host = hosts[0]
        self.debug("Host for backup router: %s" % backup_host.name)
        self.debug("Host for backup router: %s" % backup_host.ipaddress)
        self.debug(master_router.linklocalip)

        # Check eth2 port for master router
        if self.apiclient.hypervisor.lower() == 'vmware':
            result = get_process_status(
                                self.apiclient.connection.mgtSvr,
                                22,
                                self.apiclient.connection.user,
                                self.apiclient.connection.passwd,
                                master_router.linklocalip,
                                'ip addr show eth2',
                                hypervisor=self.apiclient.hypervisor
                                )
        else:
            result = get_process_status(
                                master_host.ipaddress,
                                self.services['host']["publicport"],
                                self.services['host']["username"],
                                self.services['host']["password"],
                                master_router.linklocalip,
                                'ip addr show eth2'
                                )

        res = str(result)

        self.debug("Command 'ip addr show eth2': %s" % result)
        self.debug("Router's public Ip: %s" % master_router.publicip)
        self.assertEqual(
                         res.count("state UP"),
                         1,
                         "MASTER router's public interface should be UP"
                         )
        self.assertEqual(
                         result.count('brd 0.0.0.0'),
                         0,
                         "Broadcast address of eth2 should not be 0.0.0.0"
                         )

        # Check eth2 port for backup router
        if self.apiclient.hypervisor.lower() == 'vmware':
            result = get_process_status(
                                self.apiclient.connection.mgtSvr,
                                22,
                                self.apiclient.connection.user,
                                self.apiclient.connction.passwd,
                                backup_router.linklocalip,
                                'ip addr show eth2',
                                hypervisor=self.apiclient.hypervisor
                                )
        else:
            result = get_process_status(
                                backup_host.ipaddress,
                                self.services['host']["publicport"],
                                self.services['host']["username"],
                                self.services['host']["password"],
                                backup_router.linklocalip,
                                'ip addr show eth2',
                                )
        res = str(result)

        self.debug("Command 'ip addr show eth2': %s" % result)
        self.assertEqual(
                         res.count("state DOWN"),
                         1,
                         "BACKUP router's public interface should be DOWN"
                         )
        self.assertEqual(
                         result.count('brd 0.0.0.0'),
                         0,
                         "Broadcast address of eth2 should not be 0.0.0.0"
                         )
        vms = VirtualMachine.list(
                             self.apiclient,
                             id=virtual_machine.id,
                             listall=True
                             )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should not return empty response"
                         )
        vm = vms[0]

        self.assertNotEqual(
                    vm.nic[0].gateway,
                    master_router.publicip,
                    "The gateway of user VM should be same as master router"
                 )

        self.assertNotEqual(
                    vm.nic[0].gateway,
                    backup_router.publicip,
                    "The gateway of user VM should be same as backup router"
                 )

        return


class TestRvRRedundancy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRvRRedundancy,
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls._cleanup = []
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        cls._cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
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
        self.cleanup = []
        self.account = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup.insert(0, self.account)
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
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
        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        self.virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        # wait for VR to update state
        time.sleep(self.services["sleep"])
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_01_stopMasterRvR(self):
        """Test stop master RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=MASTER. only one router is
        #    returned with redundantstate = MASTER for this network
        # 5. stopRouter that is Master. Router goes to stopped state
        #    successfully
        # 6. listRouters in the account and in the network. Lists old MASTER
        #    router in redundantstate=UNKNOWN, and the old BACKUP router as
        #    new MASTER
        # 7. start the stopped router. Stopped rvr starts up successfully and
        #    is in Running state
        # 8. listRouters in the account and in the network. Router shows up as
        #    BACKUP and NOT MASTER, should have only one BACKUP and one MASTER
        #    at the end, public IP of the SourceNAT should remain same after
        #    reboot
        # 9. delete the account

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping the MASTER router")
        try:
            Router.stop(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to stop master router: %s" % e)

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=master_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertIn(
            routers[0].redundantstate,
            ['UNKNOWN', 'FAULT'],
            "Redundant state of the master router should be UNKNOWN/FAULT but is %s" % routers[0].redundantstate
        )

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return backup router"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'MASTER',
                    "Redundant state of the router should be MASTER but is %s" % routers[0].redundantstate
                    )

        self.debug("Starting the old MASTER router")
        try:
            Router.start(self.apiclient, id=master_router.id)
            self.debug("old MASTER router started")
        except Exception as e:
            self.fail("Failed to start master router: %s" % e)

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Checking state of the master router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=master_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return backup router"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'BACKUP',
                    "Redundant state of the router should be BACKUP but is %s" % routers[0].redundantstate
                    )
        self.assertEqual(
                         master_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_02_stopBackupRvR(self):
        """Test stop backup RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=MASTER. only one router is
        #    returned with redundantstate = MASTER for this network
        # 5. stopRouter that is BACKUP. Router goes to stopped state
        #    successfully
        # 6. listRouters in the account and in the network. Lists old MASTER
        #    router in redundantstate=UNKNOWN
        # 7. start the stopped router. Stopped rvr starts up successfully and
        #    is in Running state
        # 8. listRouters in the account and in the network. Router shows up as
        #    BACKUP and NOT MASTER, should have only one BACKUP and one MASTER
        #    at the end, public IP of the SourceNAT should remain same after
        #    reboot
        # 9. delete the account

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping the BACKUP router")
        try:
            Router.stop(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to stop backup router: %s" % e)

        # wait for VR update state
        time.sleep(self.services["sleep"])

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertIn(
            routers[0].redundantstate,
            ['UNKNOWN', 'FAULT'],
            "Redundant state of the backup router should be UNKNOWN/FAULT but is %s" % routers[0].redundantstate
        )

        self.debug("Checking state of the master router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=master_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'MASTER',
                    "Redundant state of the router should be MASTER but is %s" % routers[0].redundantstate
                    )

        self.debug("Starting the old BACKUP router")
        try:
            Router.start(self.apiclient, id=backup_router.id)
            self.debug("old BACKUP router started")
        except Exception as e:
            self.fail("Failed to stop master router: %s" % e)

        # wait for VR to start and update state
        time.sleep(self.services["sleep"])

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return backup router"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'BACKUP',
                    "Redundant state of the router should be BACKUP but is %s" % routers[0].redundantstate
                    )
        self.assertEqual(
                         backup_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_03_rebootMasterRvR(self):
        """Test reboot master RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=MASTER. only one router is
        #    returned with redundantstate = MASTER for this network
        # 5. reboot router that is MASTER. Router reboots state
        #    successfully
        # 6. lists old MASTER router in redundantstate=BACKUP and the old
        #    BACKUP router as new MASTER + public IP of the SourceNAT should
        #    remain same after the reboot

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Rebooting the master router")
        try:
            Router.reboot(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to reboot MASTER router: %s" % e)

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Checking state of the master router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=master_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'BACKUP',
                    "Redundant state of the router should be BACKUP but is %s" % routers[0].redundantstate
                    )

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'MASTER',
                    "Redundant state of the router should be MASTER but is %s" % routers[0].redundantstate
                    )
        self.assertEqual(
                         master_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_04_rebootBackupRvR(self):
        """Test reboot backup RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=MASTER. only one router is
        #    returned with redundantstate = MASTER for this network
        # 5. reboot router that is BACKUP. Router reboots state
        #    successfully
        # 6. lists old BACKUP router in redundantstate=BACKUP, and the old
        #    MASTER router is still MASTER+ public IP of the SourceNAT should
        #    remain same after the reboot

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Rebooting the backup router")
        try:
            Router.reboot(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to reboot BACKUP router: %s" % e)

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'BACKUP',
                    "Redundant state of the router should be BACKUP but is %s" % routers[0].redundantstate
                    )

        self.debug("Checking state of the master router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=master_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'MASTER',
                    "Redundant state of the router should be MASTER but is %s" % routers[0].redundantstate
                    )
        self.assertEqual(
                         master_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_05_stopBackupRvR_startInstance(self):
        """Test stop backup RVR and start instance
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        #  listNetworks returns the allocated network
        # 2. listRouters in above network. Lists no routers in the created
        #    network
        # 3. deployVM in above user account in the created network. VM is
        #    successfully Running
        # 4. listRouters that has redundantstate=MASTER. only one router is
        #    returned with redundantstate = MASTER for this network
        # 5. stop router that is BACKUP.
        # 6. listRouters in the account and in the network
        # 7. deployVM in the user account in the created network
        # 8. listRouters in the account and in the network
        # 9. delete the account

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping the backup router")
        try:
            Router.stop(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to stop BACKUP router: %s" % e)

        # wait for VR to update state
        time.sleep(self.services["sleep"])

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertIn(
            routers[0].redundantstate,
            'UNKNOWN',
            "Redundant state of the backup router should be UNKNOWN but is %s" % routers[0].redundantstate
        )

        # Spawn an instance in that network
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % self.network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=vm_2.id,
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

        self.debug("Checking state of the backup router in %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              id=backup_router.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    routers[0].redundantstate,
                    'BACKUP',
                    "Redundant state of the router should be BACKUP but is %s" % routers[0].redundantstate
                    )
        return
