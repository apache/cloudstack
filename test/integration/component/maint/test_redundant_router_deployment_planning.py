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

class TestRvRDeploymentPlanning(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRvRDeploymentPlanning, cls).getClsTestClient()
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
            self.warn("Warning: Exception during cleanup : %s" % e)
            #raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns"])
    def test_RvR_multipods(self):
        """Test RvR with multi pods
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
            raise self.skipTest("The env don't have 2 pods req for test")

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

    @attr(tags=["advanced", "advancedns"])
    def test_RvR_multicluster(self):
        """Test RvR with multi clusters
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
            raise self.skipTest(
                            "The env don't have 2 clusters req for test")

        self.debug("disable all pods except one!")
        if len(pods) > 1:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updatePod(cmd)

            self.warn("Warning: Disabled all pods in zone")

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

        if pods is not None:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Enabled'
                self.apiclient.updatePod(cmd)
        return

    @attr(tags=["advanced", "advancedns"])
    def test_RvR_multiprimarystorage(self):
        """Test RvR with multi primary storage
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
            raise self.skipTest(
                            "The env don't have 2 storage pools req for test")

        self.debug("disable all pods except one!")
        if len(pods) > 1:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updatePod(cmd)

            self.warn("Warning: Disabled all pods in zone")

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

            self.warn("Warning: Disabled all pods in zone")

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

    @attr(tags=["advanced", "advancedns", "ssh"])
    def test_RvR_multihosts(self):
        """Test RvR with multi hosts
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
            raise self.skipTest(
                            "The env don't have 2 hosts req for test")

        self.debug("disable all pods except one!")
        if len(pods) > 1:
            for pod in pods:
                cmd = updatePod.updatePodCmd()
                cmd.id = pod.id
                cmd.allocationstate = 'Disabled'
                self.apiclient.updatePod(cmd)

            self.warn("Warning: Disabled all pods in zone")

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

            self.warn("Warning: Disabled all pods in zone")

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
