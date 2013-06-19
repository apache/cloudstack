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

from random import random
import marvin
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
                        "mode": 'advanced',
                        # Networking mode, Advanced, Basic
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
            #Clean up, terminate the created network offerings
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
        self._cleanup.insert(0, self.account)
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_createRvRNetwork(self):
        """Test create network with redundant routers
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
                                guestcidr=' 192.168.2.0/23'
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
                         "Guest cidr should be 192.168.2.0/23"
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
        self._cleanup.insert(0, self.account)
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
                                'ifconfig eth2',
                                hypervisor=self.apiclient.hypervisor
                                )
        else:
            result = get_process_status(
                                master_host.ipaddress,
                                self.services['host']["publicport"],
                                self.services['host']["username"],
                                self.services['host']["password"],
                                master_router.linklocalip,
                                'ifconfig eth2'
                                )

        res = str(result)

        self.debug("Command 'ifconfig eth2': %s" % result)
        self.debug("Router's public Ip: %s" % master_router.publicip)
        self.assertEqual(
                         res.count(master_router.publicip),
                         1,
                         "master router should have the public IP configured"
                         )
        self.assertEqual(
                         result.count('Bcast:0.0.0.0'),
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
                                'ifconfig eth2',
                                hypervisor=self.apiclient.hypervisor
                                )
        else:
            result = get_process_status(
                                backup_host.ipaddress,
                                self.services['host']["publicport"],
                                self.services['host']["username"],
                                self.services['host']["password"],
                                backup_router.linklocalip,
                                'ifconfig eth2'
                                )
        res = str(result)

        self.debug("Command 'ifconfig eth2': %s" % result)
        self.assertEqual(
                    res.count('Bcast:0.0.0.0'),
                    1,
                    "backup router should NOT have the public IP configured"
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


class TestRedundancy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRedundancy,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_stopMasterRvR(self):
        """Test stop MASTER RVR
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

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    'UNKNOWN',
                    "Redundant state of the router should be UNKNOWN"
                    )

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be MASTER"
                    )

        self.debug("Starting the old MASTER router")
        try:
            Router.start(self.apiclient, id=master_router.id)
            self.debug("old MASTER router started")
        except Exception as e:
            self.fail("Failed to stop master router: %s" % e)

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be BACKUP"
                    )
        self.assertEqual(
                         master_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_stopBackupRvR(self):
        """Test stop BACKUP RVR
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

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    'UNKNOWN',
                    "Redundant state of the router should be UNKNOWN"
                    )

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be MASTER"
                    )

        self.debug("Starting the old BACKUP router")
        try:
            Router.start(self.apiclient, id=backup_router.id)
            self.debug("old BACKUP router started")
        except Exception as e:
            self.fail("Failed to stop master router: %s" % e)

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be BACKUP"
                    )
        self.assertEqual(
                         backup_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_rebootMasterRvR(self):
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

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be BACKUP"
                    )

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be MASTER"
                    )
        self.assertEqual(
                         master_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_rebootBackupRvR(self):
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

        self.debug("Rebooting the backuo router")
        try:
            Router.reboot(self.apiclient, id=backup_router.id)
        except Exception as e:
            self.fail("Failed to reboot BACKUP router: %s" % e)

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be BACKUP"
                    )

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be MASTER"
                    )
        self.assertEqual(
                         master_router.publicip,
                         routers[0].publicip,
                         "Public IP should be same after reboot"
                         )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_stopBackupRvR_startInstance(self):
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

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    'UNKNOWN',
                    "Redundant state of the router should be UNKNOWN"
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

        self.debug("Listing routers for network: %s" % self.network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network.id,
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
                    "Redundant state of the router should be BACKUP"
                    )
        return


class TestApplyAndDeleteNetworkRulesOnRvR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestApplyAndDeleteNetworkRulesOnRvR,
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

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_apply_and__delete_NetworkRulesOnRvR(self):
        """Test apply and delete network rules on redundant router
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP
        # 6. listRemoteAccessVpns for the network associated should show the
        #    VPN created
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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
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
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          networkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                            ipaddress=public_ip_2.ipaddress.ipaddress,
                            reconnect=True,
                            port=self.services["natrule_221"]["publicport"]
                           )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)
        return


class TestEnableVPNOverRvR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestEnableVPNOverRvR,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_enableVPNOverRvR(self):
        """Test redundant router internals
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
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


class TestNetworkRulesMasterDownDeleteNetworkRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNetworkRulesMasterDownDeleteNetworkRules,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_applyNetworkRules_MasterDown_deleteNetworkRules(self):
        """Test apply network rules when master down and delete network rules
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listStaticNats for the network associated
        # 7. listFirewallRules should show allowed ports open
        # 8. ssh to succeed to the guestVM
        # 9. listPublicIpAddresses for networkid should show acquired IP addr
        # 10. listPortForwardRules to show open ports 221, 222
        # 11. ssh should succeed for both ports
        # 12. listPublicIpAddresses for networkid should show acquired IP addr
        # 13 and 14. listLoadBalancerRules should show associated VMs for
        #    public IP
        # 15. ssh should succeed to the user VMs
        # 16. listRouters should show one Router in MASTER state and Running
        # 17. ssh should work for PF, FW, and LB ips
        # 18. listRouters should show both routers MASTER and BACKUP in
        #    Running state
        # 19. listPortForwardingRules, listFirewallRules, listLoadBalancerRule
        #    should return empty response
        # 20. listPublicIpAddresses should show now more addresses

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

        self.debug("Stopping router ID: %s" % master_router.id)

        try:
            Router.stop(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to stop master router..")

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
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          networkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Starting router ID: %s" % master_router.id)

        try:
            Router.start(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to start master router..")

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        return


class TestApplyDeleteNetworkRulesRebootRouter(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestApplyDeleteNetworkRulesRebootRouter,
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
        self._clean.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_applyNetworkRules_MasterDown_deleteNetworkRules(self):
        """Test apply network rules when master & backup routers rebooted
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listStaticNats for the network associated
        # 7. listFirewallRules should show allowed ports open
        # 8. ssh to succeed to the guestVM
        # 9. listPublicIpAddresses for networkid should show acquired IP addr
        # 10. listPortForwardRules to show open ports 221, 222
        # 11. ssh should succeed for both ports
        # 12. listPublicIpAddresses for networkid should show acquired IP addr
        # 13 and 14. listLoadBalancerRules should show associated VMs for
        #    public IP
        # 15. ssh should succeed to the user VMs
        # 16. listRouters should show one Router in MASTER state and Running
        # 17. ssh should work for PF, FW, and LB ips
        # 18. listRouters should show both routers MASTER and BACKUP in
        #    Running state
        # 19. listPortForwardingRules, listFirewallRules, listLoadBalancerRule
        #    should return empty response
        # 20. listPublicIpAddresses should show now more addresses

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
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          networkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Starting router ID: %s" % master_router.id)

        for router in routers:
            try:
                self.debug("Rebooting router ID: %s" % master_router.id)
                #Stop the router
                cmd = rebootRouter.rebootRouterCmd()
                cmd.id = router.id
                self.apiclient.rebootRouter(cmd)
            except Exception as e:
                self.fail("Failed to reboot router..")

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        return


class TestRestartRvRNetworkWithoutCleanup(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRestartRvRNetworkWithoutCleanup,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_restartRvRNetwork_withoutCleanup(self):
        """Test apply rules after network restart
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. restartNetwork cleanup=false
        # 5. listRouters in the account
        # 6. delete the account

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

        self.debug("restarting network with cleanup=False")
        try:
            network.restart(self.apiclient, cleanup=False)
        except Exception as e:
                self.fail("Failed to cleanup network - %s" % e)

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
            self.assertIn(
                    router.linklocalip,
                    [master_router.linklocalip, backup_router.linklocalip],
                    "Routers should have same linklocal IP after nw restart"
                  )
        return


class TestRestartRvRNetworkWithCleanup(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRestartRvRNetworkWithCleanup,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_restartRvRNetwork_withCleanup(self):
        """Test Restart network with cleanup
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. restartNetwork cleanup=false
        # 5. listRouters in the account
        # 6. delete the account

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

        self.debug("restarting network with cleanup=True")
        try:
            network.restart(self.apiclient, cleanup=True)
        except Exception as e:
                self.fail("Failed to cleanup network - %s" % e)

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
            self.assertIn(
                    router.linklocalip,
                    [master_router.linklocalip, backup_router.linklocalip],
                    "Routers should have same linklocal IP after nw restart"
                  )
        return


class TestDeleteRvRNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestDeleteRvRNetwork,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_restartRvRNetwork_withCleanup(self):
        """Test Restart network with cleanup
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. restartNetwork cleanup=false
        # 5. listRouters in the account
        # 6. delete the account

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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        self.debug("Trying to delete the network with running Vms")
        with self.assertRaises(Exception):
            network.delete(self.apiclient, cleanup=True)

        self.debug("Network delete failed!")
        self.debug("Destroying the user VMs for account: %s" %
                                        self.account.name)

        try:
            virtual_machine.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete guest Vm from account: %s - %s" %
                                            (self.account.name, e))

        interval = list_configurations(
                                    self.apiclient,
                                    name='expunge.delay'
                                    )
        delay = int(interval[0].value)
        interval = list_configurations(
                                    self.apiclient,
                                    name='expunge.interval'
                                    )
        exp = int(interval[0].value)

        self.debug("Sleeping for exp delay + interval time")
        # Sleep to ensure that all resources are deleted
        time.sleep((delay + exp) * 2)

        self.debug("Trying to delete guest network for account: %s" %
                                                self.account.name)
        try:
            network.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete network: %s" % e)
        return


class TestNetworkGCRvR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestNetworkGCRvR,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_networkGC_RvR(self):
        """Test network garbage collection with RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. stop the running user VM
        # 5. wait for network.gc time
        # 6. listRouters
        # 7. start the routers MASTER and BACK
        # 8. wait for network.gc time and listRouters
        # 9. delete the account

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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        self.debug("Stopping the user VM: %s" % virtual_machine.name)

        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop guest Vm: %s - %s" %
                                            (virtual_machine.name, e))

        interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
        delay = int(interval[0].value)
        interval = list_configurations(
                                    self.apiclient,
                                    name='network.gc.wait'
                                    )
        exp = int(interval[0].value)

        self.debug("Sleeping for network gc wait + interval time")
        # Sleep to ensure that all resources are deleted
        time.sleep((delay + exp) * 2)

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Stopped",
                             "Router should be in stopped state"
                             )
            self.debug("Starting the stopped router again")
            cmd = startRouter.startRouterCmd()
            cmd.id = router.id
            self.apiclient.startRouter(cmd)

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router should be in running state"
                             )

        self.debug("Sleeping for network gc wait + interval time")
        # Sleep to ensure that all resources are deleted
        time.sleep((delay + exp) * 2)

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Stopped",
                             "Router should be in stopped state"
                             )
        return


class TestApplyRulesRestartRvRNetwork(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestApplyRulesRestartRvRNetwork,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_applyRules_restartRvRNetwork(self):
        """Test apply rules after network restart
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listStaticNats for the network associated
        # 7. listFirewallRules should show allowed ports open
        # 8. ssh to succeed to the guestVM
        # 9. listPublicIpAddresses for networkid should show acquired IP addr
        # 10. listPortForwardRules to show open ports 221, 222
        # 11. ssh should succeed for both ports
        # 12. listPublicIpAddresses for networkid should show acquired IP addr
        # 13 and 14. listLoadBalancerRules should show associated VMs for
        #    public IP
        # 15. ssh should succeed to the user VMs
        # 16. listRouters should show one Router in MASTER state and Running &
        #    one in BACKUP and Running
        # 17. ssh should work for PF, FW, and LB ips
        # 18. listRouters should show one Router in MASTER state and Running &
        #    one in BACKUP and Running
        # 19. ssh should work for PF, FW, and LB ips
        # 20. listPortForwardingRules, listFirewallRules, listLoadBalancerRule
        #     should return empty response
        # 21. listPublicIpAddresses should show now more addresses

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
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          networkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Restarting network ID: %s with cleanup true" %
                                                                network.id)

        try:
            network.restart(self.apiclient, cleanup=True)
        except Exception as e:
                self.fail("Failed to cleanup network")

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Restarting network ID: %s with cleanup false" %
                                                                network.id)

        try:
            network.restart(self.apiclient, cleanup=False)
        except Exception as e:
                self.fail("Failed to cleanup network")

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)
        return


class TestUpgradeDowngradeRVR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestUpgradeDowngradeRVR,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_upgradeVR_to_redundantVR(self):
        """Test upgrade virtual router to redundant virtual router
        """

        # Steps to validate
        # 1. create a network with DefaultNetworkOfferingWithSourceNATservice
        #    (all VR based services)
        # 2. deploy a VM in the above network and listRouters
        # 3. create a network Offering that has redundant router enabled and
        #    all VR based services
        # 4. updateNetwork created above to the offfering in 3.
        # 5. listRouters in the network
        # 6. delete account in which resources are created
        # Validate the following
        # 1. listNetworks should show the created network in allocated state
        # 2. VM should be deployed and in Running state and there should be
        #    one Router running for this network
        # 3. listNetworkOfferings should show craeted offering for RvR
        # 4. listNetworks shows the network still successfully implemented
        # 5. listRouters shows two routers Up and Running (MASTER and BACKUP)

        network_offerings = NetworkOffering.list(
                            self.apiclient,
                            name='DefaultIsolatedNetworkOfferingWithSourceNatService',
                            listall=True
                         )
        self.assertEqual(
                    isinstance(network_offerings, list),
                    True,
                    "List network offering should not return empty response"
                    )

        network_off_vr = network_offerings[0]
        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                        network_off_vr.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=network_off_vr.id,
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
        self.debug("Deployed VM in the account: %s" %
                                    self.account.name)

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

        self.debug("Listing routers for account: %s" %
                                        self.account.name)
        routers = Router.list(
                              self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return only one router"
                    )
        self.assertEqual(
                    len(routers),
                    1,
                    "Length of the list router should be 1"
                    )

        self.debug("Upgrading the network to RVR network offering..")
        try:
            network.update(
                           self.apiclient,
                           networkofferingid=self.network_offering.id
                           )
        except Exception as e:
            self.fail("Failed to upgrade the network from VR to RVR: %s" % e)

        self.debug("Listing routers for account: %s" %
                                        self.account.name)
        routers = Router.list(
                              self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return two routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (MASTER & BACKUP)"
                    )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_downgradeRvR_to_VR(self):
        """Test downgrade redundant virtual router to virtual router
        """

        # Steps to validate
        # 1. create a network Offering that has redundant router enabled and
        #    all VR based services
        # 2. create a network with above offering
        # 3. deploy a VM in the above network and listRouters
        # 4. create a network Offering that has redundant router disabled and
        #    all VR based services
        # 5. updateNetwork - downgrade - created above to the offfering in 4.
        # 6. listRouters in the network
        # 7. delete account in which resources are created
        # Validate the following
        # 1. listNetworkOfferings should show craeted offering for RvR
        # 2. listNetworks should show the created network in allocated state
        # 3. VM should be deployed and in Running state and there should be
        #    two routers (MASTER and BACKUP) for this network
        # 4. listNetworkOfferings should show craeted offering for VR
        # 5. listNetworks shows the network still successfully implemented
        # 6. listRouters shows only one router for this network in Running

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
        self.debug("Deployed VM in the account: %s" %
                                    self.account.name)

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

        self.debug("Listing routers for account: %s" %
                                        self.account.name)
        routers = Router.list(
                              self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return two routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (MASTER & BACKUP)"
                    )

        network_offerings = NetworkOffering.list(
                            self.apiclient,
                            name='DefaultIsolatedNetworkOfferingWithSourceNatService',
                            listall=True
                         )
        self.assertEqual(
                    isinstance(network_offerings, list),
                    True,
                    "List network offering should not return empty response"
                    )

        network_off_vr = network_offerings[0]

        self.debug("Upgrading the network to RVR network offering..")
        try:
            network.update(
                           self.apiclient,
                           networkofferingid=network_off_vr.id
                           )
        except Exception as e:
            self.fail("Failed to upgrade the network from VR to RVR: %s" % e)

        self.debug("Listing routers for account: %s" %
                                        self.account.name)
        routers = Router.list(
                              self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return only one router"
                    )
        self.assertEqual(
                    len(routers),
                    1,
                    "Length of the list router should be 1"
                    )
        return


class TestRVRWithDiffEnvs(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestRVRWithDiffEnvs,
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
        self._cleanup.insert(0, self.account)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_RvR_multipods(self):
        """Test RvR with muti pods
        """

        # Steps to validate
        # 0. listPods should have at least 2 pods
        # 1. create a network offering for redundant router
        # 2. create a network out of this offering
        # 3. deploy a VM in this network
        # 4. listRouters
        # 5. delete the account
        # Validate the following
        # 1. listNetworkOfferings should show created offering for RvR
        # 2. listNetworks should show the created network in allocated state
        # 3. VM should be deployed and in Running state
        # 4. There should be two routers (MASTER and BACKUP) for this network
        #    ensure both routers should be on different pods

        self.debug("Checking if the current zone has 2 active pods in it..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Enabled"
                        )
        self.assertEqual(
                         isinstance(pods, list),
                         True,
                         "List pods should not return an empty response"
                         )

        if len(pods) < 2:
            raise unittest.SkipTest("The env don't have 2 pods req for test")

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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )
        self.assertNotEqual(
                            routers[0].podid,
                            routers[1].podid,
                            "Both the routers should be in different pods"
                            )
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_RvR_multicluster(self):
        """Test RvR with muti clusters
        """

        # Steps to validate
        # 0. listClusters should have at least two clusters (if there are
        #    multiple pods, disable all except one with two clusters)
        # 1. create a network offering for redundant router
        # 2. create a network out of this offering
        # 3. deploy a VM in this network on a host in either of clusters
        #    found in 0. (specify hostid for deployment)
        # 4. listRouters
        # 5. delete the account
        # 6. enable all disabled pods
        # Validate the following
        # 1. listNetworkOfferings should show created offering for RvR
        # 2. listNetworks should show the created network in allocated state
        # 3. VM should be deployed and in Running state
        # 4. There should be two routers (MASTER and BACKUP) for this network
        #    ensure both routers should be on different pods

        self.debug("Checking if the current zone has 2 active pods in it..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Enabled"
                        )
        self.assertEqual(
                         isinstance(pods, list),
                         True,
                         "List pods should not return an empty response"
                         )
        enabled_pod = pods[0]

        self.debug("Cheking if pod has atleast 2 clusters")
        clusters = Cluster.list(
                                self.apiclient,
                                podid=enabled_pod.id,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(clusters, list),
                         True,
                         "List clusters should not return empty response"
                         )
        if len(clusters) < 2:
            raise unittest.SkipTest(
                            "The env don't have 2 clusters req for test")

        self.debug("disable all pods except one!")
        if len(pods) > 1:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updatePod(cmd)

            self.debug("Warning: Disabled all pods in zone")

            cmd = updatePod.updatePodCmd()
            cmd.id = pods[0].id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updatePod(cmd)
            self.debug("Enabled first pod for testing..")

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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        hosts = Host.list(
                          self.apiclient,
                          id=routers[0].hostid,
                          listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List host should return a valid data"
                         )
        first_host = hosts[0]

        hosts = Host.list(
                          self.apiclient,
                          id=routers[1].hostid,
                          listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List host should return a valid data"
                         )
        second_host = hosts[0]

        # Checking if the cluster IDs of both routers are different?
        self.assertNotEqual(
                            first_host.clusterid,
                            second_host.clusterid,
                            "Both the routers should be in different clusters"
                            )
        self.debug("Enabling remaining pods if any..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Disabled"
                        )

        for pod in pods:
            cmd = updatePod.updatePodCmd()
            cmd.id = pod.id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updatePod(cmd)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_RvR_multiprimarystorage(self):
        """Test RvR with muti primary storage
        """

        # Steps to validate
        # 0. listStoragePools should have atleast two pools in a single
        #    cluster (disable pods/clusters as necessary)
        # 1. create a network offering for redundant router
        # 2. create a network out of this offering
        # 3. deploy a VM in this network on a host in the cluster from 0
        #    (specify hostid for deployment)
        # 4. listRouters
        # 5. delete the account
        # 6. enable the clusters and pods
        # Validate the following
        # 1. listNetworkOfferings should show created offering for RvR
        # 2. listNetworks should show the created network in allocated state
        # 3. VM should be deployed and in Running state and on the specified
        #    host
        # 4. There should be two routers (MASTER and BACKUP) for this network
        #    ensure both routers should be on different storage pools

        self.debug(
            "Checking if the current zone has multiple active pods in it..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Enabled"
                        )
        self.assertEqual(
                         isinstance(pods, list),
                         True,
                         "List pods should not return an empty response"
                         )

        enabled_pod = pods[0]
        self.debug("Cheking if pod has multiple clusters")
        clusters = Cluster.list(
                                self.apiclient,
                                podid=enabled_pod.id,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(clusters, list),
                         True,
                         "List clusters should not return empty response"
                         )

        enabled_cluster = clusters[0]

        self.debug("Cheking if cluster has multiple storage pools")
        storage_pools = StoragePool.list(
                                self.apiclient,
                                clusterid=enabled_cluster.id,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(storage_pools, list),
                         True,
                         "List storage pools should not return empty response"
                         )

        if len(storage_pools) < 2:
            raise unittest.SkipTest(
                            "The env don't have 2 storage pools req for test")

        self.debug("disable all pods except one!")
        if len(pods) > 1:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updatePod(cmd)

            self.debug("Warning: Disabled all pods in zone")

            cmd = updatePod.updatePodCmd()
            cmd.id = pods[0].id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updatePod(cmd)
            self.debug("Enabled first pod for testing..")

        self.debug("disable all clusters except one!")
        if len(pods) > 1:
            for cluster in clusters:
                cmd = updateCluster.updateClusterCmd()
                cmd.id = cluster.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updateCluster(cmd)

            self.debug("Warning: Disabled all pods in zone")

            cmd = updateCluster.updateClusterCmd()
            cmd.id = clusters[0].id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updateCluster(cmd)
            self.debug("Enabled first cluster for testing..")

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

        self.debug("Retrieving the list of hosts in the cluster")
        hosts = Host.list(
                          self.apiclient,
                          clusterid=enabled_cluster.id,
                          listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should not return an empty response"
                         )
        host = hosts[0]

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)],
                                  hostid=host.id
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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )
        self.assertNotEqual(
                    routers[0].hostid,
                    routers[1].hostid,
                    "Both the routers should be in different storage pools"
                            )
        self.debug("Enabling remaining pods if any..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Disabled"
                        )
        self.assertEqual(
                         isinstance(pods, list),
                         True,
                         "List pods should not return an empty response"
                         )

        for pod in pods:
            cmd = updatePod.updatePodCmd()
            cmd.id = pod.id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updatePod(cmd)

        clusters = Cluster.list(
                                self.apiclient,
                                allocationstate="Disabled",
                                podid=enabled_pod.id,
                                listall=True
                                )

        for cluster in clusters:
                cmd = updateCluster.updateClusterCmd()
                cmd.id = cluster.id
                cmd.allocationstate = 'Enabled'
                self.apiclient.updateCluster(cmd)
        return

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_RvR_multihosts(self):
        """Test RvR with muti hosts
        """

        # Steps to validate
        # 0. listHosts should have atleast two hosts in a single cluster
        #    (disable pods/clusters as necessary)
        # 1. create a network offering for redundant router
        # 2. create a network out of this offering
        # 3. deploy a VM in this network on a host in the cluster from 0
        #    (specify hostid for deployment)
        # 4. listRouters
        # 5. delete the account
        # 6. enable the clusters and pods
        # Validate the following
        # 1. listNetworkOfferings should show created offering for RvR
        # 2. listNetworks should show the created network in allocated state
        # 3. VM should be deployed and in Running state and on specified host
        # 4. There should be two routers (MASTER and BACKUP) for this network
        #    ensure both routers should be on different hosts

        self.debug(
            "Checking if the current zone has multiple active pods in it..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Enabled"
                        )
        self.assertEqual(
                         isinstance(pods, list),
                         True,
                         "List pods should not return an empty response"
                         )

        enabled_pod = pods[0]
        self.debug("Cheking if pod has multiple clusters")
        clusters = Cluster.list(
                                self.apiclient,
                                podid=enabled_pod.id,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(clusters, list),
                         True,
                         "List clusters should not return empty response"
                         )

        enabled_cluster = clusters[0]

        self.debug("Cheking if cluster has multiple hosts")
        hosts = Host.list(
                                self.apiclient,
                                clusterid=enabled_cluster.id,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should not return empty response"
                         )

        if len(hosts) < 2:
            raise unittest.SkipTest(
                            "The env don't have 2 hosts req for test")

        self.debug("disable all pods except one!")
        if len(pods) > 1:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updatePod(cmd)

            self.debug("Warning: Disabled all pods in zone")

            cmd = updatePod.updatePodCmd()
            cmd.id = pods[0].id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updatePod(cmd)
            self.debug("Enabled first pod for testing..")

        self.debug("disable all clusters except one!")
        if len(pods) > 1:
            for cluster in clusters:
                cmd = updateCluster.updateClusterCmd()
                cmd.id = cluster.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updateCluster(cmd)

            self.debug("Warning: Disabled all pods in zone")

            cmd = updateCluster.updateClusterCmd()
            cmd.id = clusters[0].id
            cmd.allocationstate = 'Enabled'
            self.apiclient.updateCluster(cmd)
            self.debug("Enabled first cluster for testing..")

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

        self.debug("Retrieving the list of hosts in the cluster")
        hosts = Host.list(
                          self.apiclient,
                          clusterid=enabled_cluster.id,
                          listall=True
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should not return an empty response"
                         )
        host = hosts[0]

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)],
                                  hostid=host.id
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
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                  )
        self.assertNotEqual(
                            routers[0].hostid,
                            routers[1].hostid,
                            "Both the routers should be in different hosts"
                            )
        self.debug("Enabling remaining pods if any..")
        pods = Pod.list(
                        self.apiclient,
                        zoneid=self.zone.id,
                        listall=True,
                        allocationstate="Disabled"
                        )

        if pods is not None:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Enabled'
                self.apiclient.updatePod(cmd)

        clusters = Cluster.list(
                                self.apiclient,
                                allocationstate="Disabled",
                                podid=enabled_pod.id,
                                listall=True
                                )
        if clusters is not None:
            for cluster in clusters:
                cmd = updateCluster.updateClusterCmd()
                cmd.id = cluster.id
                cmd.allocationstate = 'Enabled'
                self.apiclient.updateCluster(cmd)
        return
