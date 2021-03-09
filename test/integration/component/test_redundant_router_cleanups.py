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
from marvin.lib.base import (Account,
                             Network,
                             ServiceOffering,
                             NetworkOffering,
                             VirtualMachine,
                             Router,
                             Configurations)
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import startRouter
from marvin.codes import PASS
import time

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


class TestRedundantRouterNetworkCleanups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRedundantRouterNetworkCleanups, cls).getClsTestClient()
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
        self.cleanup=[]
        self.cleanup.insert(0, self.account)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            #raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="false")
    def test_restart_ntwk_no_cleanup(self):
        """Test restarting RvR network without cleanup
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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & primary)"
                    )

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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & primary)"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="false")
    def test_restart_ntwk_with_cleanup(self):
        """Test restart RvR network with cleanup
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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & primary)"
                    )

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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & primary)"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="false")
    def test_network_gc(self):
        """Test network garbage collection with RVR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. stop the running user VM
        # 5. wait for network.gc time
        # 6. listRouters
        # 7. start the routers PRIMARY and BACK
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
                    "list router should return Primary and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & primary)"
                    )

        self.debug("Stopping the user VM: %s" % virtual_machine.name)

        try:
            virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop guest Vm: %s - %s" %
                                            (virtual_machine.name, e))

        interval = Configurations.list(
                                    self.apiclient,
                                    name='network.gc.interval'
                                    )
        delay = int(interval[0].value)
        interval = Configurations.list(
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
                    "list router should return Primary and backup routers"
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
                    "list router should return Primary and backup routers"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router should be in running state"
                             )

        self.debug("Sleeping for network gc wait + interval time")
        # Sleep to ensure that all resources are deleted
        time.sleep((delay + exp) * 3)

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
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Stopped",
                             "Router should be in stopped state"
                             )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_restart_network_with_destroyed_primaryVR(self):
        """Test restarting RvR network without cleanup after destroying primary VR
        """

        # Steps to validate
        # 1. createNetwork using network offering for redundant virtual router
        # 2. listRouters in above network
        # 3. deployVM in above user account in the created network
        # 4. Destroy primary VR
        # 5. restartNetwork cleanup=false
        # 6. Verify RVR status after network restart

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
            validateList(networks)[0],
            PASS,
            "List networks should return a valid response for created network"
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
            validateList(vms)[0],
            PASS,
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
            validateList(routers)[0],
            PASS,
            "list router should return Primary and backup routers"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & primary)"
        )
        if routers[0].redundantstate == 'PRIMARY' and\
                routers[1].redundantstate == 'BACKUP':
            primary_router = routers[0]
            backup_router = routers[1]
        elif routers[1].redundantstate == 'PRIMARY' and \
                routers[0].redundantstate == 'BACKUP':
            primary_router = routers[1]
            backup_router = routers[0]
        else:
            self.fail("Both the routers in RVR are in BackupState - CLOUDSTACK-9015")

        Router.stop(
            self.apiclient,
            id=primary_router.id
        )
        Router.destroy(
            self.apiclient,
            id=primary_router.id
        )
        primaryVR = Router.list(
            self.apiclient,
            id=primary_router.id
        )
        self.assertIsNone(primaryVR, "Router is not destroyed")
        new_primary = Router.list(
            self.apiclient,
            id=backup_router.id
        )
        self.assertEqual(validateList(new_primary)[0], PASS, "Invalid response after vr destroy")
        self.assertEqual(
            new_primary[0].redundantstate,
            "PRIMARY",
            "Backup didn't switch to Primary after destroying Primary VR"
        )

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
            validateList(routers)[0],
            PASS,
            "list router should return Primary and backup routers afrer network restart"
        )
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & primary)"
        )
        for router in routers:
            self.assertEqual(
                router.state,
                "Running",
                "Router state should be running"
            )
        if routers[0].redundantstate == 'PRIMARY' and\
                routers[1].redundantstate == 'BACKUP':
            self.debug("Found primary and backup VRs after network restart")
        elif routers[0].redundantstate == 'BACKUP' and \
                routers[1].redundantstate == 'PRIMARY':
            self.debug("Found primary and backup routers")
        else:
            self.fail("RVR is not in proper start after network restart")
        return
