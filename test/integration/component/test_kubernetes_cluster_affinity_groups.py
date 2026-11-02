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
"""Tests for Kubernetes cluster affinity groups feature"""

import unittest
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listInfrastructure,
                                  listKubernetesSupportedVersions,
                                  addKubernetesSupportedVersion,
                                  deleteKubernetesSupportedVersion,
                                  listKubernetesClusters,
                                  createKubernetesCluster,
                                  stopKubernetesCluster,
                                  startKubernetesCluster,
                                  deleteKubernetesCluster,
                                  scaleKubernetesCluster,
                                  destroyVirtualMachine,
                                  deleteNetwork)
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import (ServiceOffering,
                             Account,
                             AffinityGroup,
                             Configurations)
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.common import (get_zone,
                               get_domain)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf

import time

_multiprocess_shared_ = True

RAND_SUFFIX = random_gen()


class TestKubernetesClusterAffinityGroups(cloudstackTestCase):
    """
    Tests for CKS Affinity Groups feature (since 4.23.0)

    This feature allows specifying different affinity groups for each
    Kubernetes node type (CONTROL, WORKER, ETCD).
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestKubernetesClusterAffinityGroups, cls).getClsTestClient()
        if testClient is None:
            raise unittest.SkipTest("Marvin test client not available - check marvin configuration")
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            cls.hypervisorNotSupported = True

        cls.setup_failed = False
        cls._cleanup = []
        cls.kubernetes_version_ids = []
        cls.initial_configuration_cks_enabled = None

        cls.k8s_version = cls.services.get("cks_kubernetes_version_upgrade_to",
                                           cls.services.get("cks_kubernetes_version_upgrade_from"))

        if cls.hypervisorNotSupported == False:
            cls.endpoint_url = Configurations.list(cls.apiclient, name="endpoint.url")[0].value
            if "localhost" in cls.endpoint_url:
                endpoint_url = "http://%s:%d/client/api" % (cls.mgtSvrDetails["mgtSvrIp"], cls.mgtSvrDetails["port"])
                cls.debug("Setting endpoint.url to %s" % endpoint_url)
                Configurations.update(cls.apiclient, "endpoint.url", endpoint_url)

            cls.initial_configuration_cks_enabled = Configurations.list(
                cls.apiclient, name="cloud.kubernetes.service.enabled")[0].value
            if cls.initial_configuration_cks_enabled not in ["true", True]:
                cls.debug("Enabling CloudStack Kubernetes Service plugin and restarting management server")
                Configurations.update(cls.apiclient, "cloud.kubernetes.service.enabled", "true")
                cls.restartServer()

            cls.cks_service_offering = None

            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version = cls.addKubernetesSupportedVersion(
                        cls.services["cks_kubernetes_versions"][cls.k8s_version])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state: %s" % e)

            if cls.setup_failed == False:
                cks_offering_data = cls.services["cks_service_offering"]
                cks_offering_data["name"] = 'CKS-Instance-' + random_gen()
                cls.cks_service_offering = ServiceOffering.create(
                    cls.apiclient,
                    cks_offering_data
                )
                cls._cleanup.append(cls.cks_service_offering)

                cls.domain = get_domain(cls.apiclient)
                cls.account = Account.create(
                    cls.apiclient,
                    cls.services["account"],
                    domainid=cls.domain.id
                )
                cls._cleanup.append(cls.account)

        cls.default_network = None

        return

    @classmethod
    def tearDownClass(cls):
        # Delete added Kubernetes supported version
        for version_id in cls.kubernetes_version_ids:
            try:
                cls.deleteKubernetesSupportedVersion(version_id)
            except Exception as e:
                cls.debug("Error during cleanup for Kubernetes versions: %s" % e)

        # Restore CKS enabled
        if cls.initial_configuration_cks_enabled not in ["true", True]:
            cls.debug("Restoring Kubernetes Service enabled value")
            Configurations.update(cls.apiclient, "cloud.kubernetes.service.enabled", "false")
            cls.restartServer()

        super(TestKubernetesClusterAffinityGroups, cls).tearDownClass()

    @classmethod
    def restartServer(cls):
        """Restart management server"""
        cls.debug("Restarting management server")
        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management stop"
        sshClient.execute(command)

        command = "service cloudstack-management start"
        sshClient.execute(command)

        # Wait for management to come up in 5 mins
        timeout = time.time() + 300
        while time.time() < timeout:
            if cls.isManagementUp() is True:
                return
            time.sleep(5)
        cls.setup_failed = True
        cls.debug("Management server did not come up, failing")
        return

    @classmethod
    def isManagementUp(cls):
        try:
            cls.apiclient.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

    @classmethod
    def waitForKubernetesSupportedVersionIsoReadyState(cls, version_id, retries=30, interval=60):
        """Check if Kubernetes supported version ISO is in Ready state"""
        while retries > 0:
            time.sleep(interval)
            list_versions_response = cls.listKubernetesSupportedVersion(version_id)
            if not hasattr(list_versions_response, 'isostate') or not list_versions_response or not list_versions_response.isostate:
                retries = retries - 1
                continue
            if 'Ready' == list_versions_response.isostate:
                return
            elif 'Failed' == list_versions_response.isostate:
                raise Exception("Failed to download template: status - %s" % list_versions_response.isostate)
            retries = retries - 1
        raise Exception("Kubernetes supported version Ready state timed out")

    @classmethod
    def listKubernetesSupportedVersion(cls, version_id):
        listKubernetesSupportedVersionsCmd = listKubernetesSupportedVersions.listKubernetesSupportedVersionsCmd()
        listKubernetesSupportedVersionsCmd.id = version_id
        versionResponse = cls.apiclient.listKubernetesSupportedVersions(listKubernetesSupportedVersionsCmd)
        return versionResponse[0]

    @classmethod
    def addKubernetesSupportedVersion(cls, version_service):
        addKubernetesSupportedVersionCmd = addKubernetesSupportedVersion.addKubernetesSupportedVersionCmd()
        addKubernetesSupportedVersionCmd.semanticversion = version_service["semanticversion"]
        addKubernetesSupportedVersionCmd.name = 'v' + version_service["semanticversion"] + '-' + random_gen()
        addKubernetesSupportedVersionCmd.url = version_service["url"]
        addKubernetesSupportedVersionCmd.mincpunumber = version_service["mincpunumber"]
        addKubernetesSupportedVersionCmd.minmemory = version_service["minmemory"]
        kubernetes_version = cls.apiclient.addKubernetesSupportedVersion(addKubernetesSupportedVersionCmd)
        cls.debug("Waiting for Kubernetes version with ID %s to be ready" % kubernetes_version.id)
        cls.waitForKubernetesSupportedVersionIsoReadyState(kubernetes_version.id)
        kubernetes_version = cls.listKubernetesSupportedVersion(kubernetes_version.id)
        return kubernetes_version

    @classmethod
    def deleteKubernetesSupportedVersion(cls, version_id):
        deleteKubernetesSupportedVersionCmd = deleteKubernetesSupportedVersion.deleteKubernetesSupportedVersionCmd()
        deleteKubernetesSupportedVersionCmd.id = version_id
        cls.apiclient.deleteKubernetesSupportedVersion(deleteKubernetesSupportedVersionCmd)

    @classmethod
    def listKubernetesCluster(cls, cluster_id=None, cluster_name=None):
        listKubernetesClustersCmd = listKubernetesClusters.listKubernetesClustersCmd()
        listKubernetesClustersCmd.listall = True
        if cluster_id is not None:
            listKubernetesClustersCmd.id = cluster_id
        if cluster_name is not None:
            listKubernetesClustersCmd.name = cluster_name
        clusterResponse = cls.apiclient.listKubernetesClusters(listKubernetesClustersCmd)
        if (cluster_id is not None or cluster_name is not None) and clusterResponse is not None:
            return clusterResponse[0]
        return clusterResponse

    @classmethod
    def deleteKubernetesCluster(cls, cluster_id):
        deleteKubernetesClusterCmd = deleteKubernetesCluster.deleteKubernetesClusterCmd()
        deleteKubernetesClusterCmd.id = cluster_id
        response = cls.apiclient.deleteKubernetesCluster(deleteKubernetesClusterCmd)
        return response

    @classmethod
    def stopKubernetesCluster(cls, cluster_id):
        stopKubernetesClusterCmd = stopKubernetesCluster.stopKubernetesClusterCmd()
        stopKubernetesClusterCmd.id = cluster_id
        response = cls.apiclient.stopKubernetesCluster(stopKubernetesClusterCmd)
        return response

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.aff_grp = []
        return

    def tearDown(self):
        super(TestKubernetesClusterAffinityGroups, self).tearDown()

    def deleteKubernetesClusterAndVerify(self, cluster_id, verify=True, forced=False):
        """Delete Kubernetes cluster and check if it is really deleted"""
        delete_response = {}
        forceDeleted = False
        try:
            delete_response = self.deleteKubernetesCluster(cluster_id)
        except Exception as e:
            if forced:
                cluster = self.listKubernetesCluster(cluster_id)
                if cluster is not None:
                    if cluster.state in ['Starting', 'Running', 'Upgrading', 'Scaling']:
                        self.stopKubernetesCluster(cluster_id)
                        self.deleteKubernetesCluster(cluster_id)
                    else:
                        forceDeleted = True
                        for cluster_vm in cluster.virtualmachines:
                            cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
                            cmd.id = cluster_vm.id
                            cmd.expunge = True
                            self.apiclient.destroyVirtualMachine(cmd)
                        cmd = deleteNetwork.deleteNetworkCmd()
                        cmd.id = cluster.networkid
                        cmd.forced = True
                        self.apiclient.deleteNetwork(cmd)
                        self.dbclient.execute(
                            "update kubernetes_cluster set state='Destroyed', removed=now() where uuid = '%s';" % cluster.id)
            else:
                raise Exception("Error: Exception during delete cluster : %s" % e)

        if verify and not forceDeleted:
            self.assertEqual(
                delete_response.success,
                True,
                "Check KubernetesCluster delete response {}, {}".format(delete_response.success, True)
            )

            db_cluster_removed = \
            self.dbclient.execute("select removed from kubernetes_cluster where uuid = '%s';" % cluster_id)[0][0]

            self.assertNotEqual(
                db_cluster_removed,
                None,
                "KubernetesCluster not removed in DB, {}".format(db_cluster_removed)
            )

    def create_aff_grp(self, aff_grp_name=None, aff_grp_type="host anti-affinity"):
        """Create an affinity group"""
        if aff_grp_name is None:
            aff_grp_name = "aff_grp_" + random_gen(size=6)

        aff_grp_data = {
            "name": aff_grp_name,
            "type": aff_grp_type
        }
        aff_grp = AffinityGroup.create(
            self.apiclient,
            aff_grp_data,
            self.account.name,
            self.domain.id
        )
        self.aff_grp.append(aff_grp)
        self.cleanup.append(aff_grp)
        return aff_grp

    def createKubernetesCluster(self, name, version_id, size=1, control_nodes=1, etcd_nodes=0,
                                control_aff_grp=None, worker_aff_grp=None, etcd_aff_grp=None):
        """Create a Kubernetes cluster with optional affinity groups for each node type"""
        createKubernetesClusterCmd = createKubernetesCluster.createKubernetesClusterCmd()
        createKubernetesClusterCmd.name = name
        createKubernetesClusterCmd.description = name + "-description"
        createKubernetesClusterCmd.kubernetesversionid = version_id
        createKubernetesClusterCmd.size = size
        createKubernetesClusterCmd.controlnodes = control_nodes
        createKubernetesClusterCmd.serviceofferingid = self.cks_service_offering.id
        createKubernetesClusterCmd.zoneid = self.zone.id
        createKubernetesClusterCmd.noderootdisksize = 10
        createKubernetesClusterCmd.account = self.account.name
        createKubernetesClusterCmd.domainid = self.domain.id

        if etcd_nodes > 0:
            createKubernetesClusterCmd.etcdnodes = etcd_nodes

        # Set affinity groups for node types using the nodeaffinitygroups parameter
        # Format: list of {node: "<NODE_TYPE>", affinitygroup: "<UUID>"}
        if control_aff_grp is not None:
            if not hasattr(createKubernetesClusterCmd, 'nodeaffinitygroups'):
                createKubernetesClusterCmd.nodeaffinitygroups = []
            createKubernetesClusterCmd.nodeaffinitygroups.append({
                "node": "CONTROL",
                "affinitygroup": control_aff_grp.id
            })
        if worker_aff_grp is not None:
            if not hasattr(createKubernetesClusterCmd, 'nodeaffinitygroups'):
                createKubernetesClusterCmd.nodeaffinitygroups = []
            createKubernetesClusterCmd.nodeaffinitygroups.append({
                "node": "WORKER",
                "affinitygroup": worker_aff_grp.id
            })
        if etcd_aff_grp is not None:
            if not hasattr(createKubernetesClusterCmd, 'nodeaffinitygroups'):
                createKubernetesClusterCmd.nodeaffinitygroups = []
            createKubernetesClusterCmd.nodeaffinitygroups.append({
                "node": "ETCD",
                "affinitygroup": etcd_aff_grp.id
            })

        if self.default_network:
            createKubernetesClusterCmd.networkid = self.default_network.id

        clusterResponse = self.apiclient.createKubernetesCluster(createKubernetesClusterCmd)
        return clusterResponse

    def startKubernetesCluster(self, cluster_id):
        startKubernetesClusterCmd = startKubernetesCluster.startKubernetesClusterCmd()
        startKubernetesClusterCmd.id = cluster_id
        response = self.apiclient.startKubernetesCluster(startKubernetesClusterCmd)
        return response

    def scaleKubernetesCluster(self, cluster_id, size):
        scaleKubernetesClusterCmd = scaleKubernetesCluster.scaleKubernetesClusterCmd()
        scaleKubernetesClusterCmd.id = cluster_id
        scaleKubernetesClusterCmd.size = size
        response = self.apiclient.scaleKubernetesCluster(scaleKubernetesClusterCmd)
        return response

    def verifyKubernetesClusterState(self, cluster_response, state):
        """Check if Kubernetes cluster state matches expected state"""
        self.assertEqual(
            cluster_response.state,
            state,
            "Check KubernetesCluster state {}, expected {}".format(cluster_response.state, state)
        )

    def verifyKubernetesClusterAffinityGroups(self, cluster, control_aff_grp=None,
                                               worker_aff_grp=None, etcd_aff_grp=None):
        """Verify affinity groups are correctly assigned to the cluster"""
        if control_aff_grp is not None:
            self.assertEqual(
                cluster.controlnodeaffinitygroupid,
                control_aff_grp.id,
                "Control node affinity group ID mismatch. Expected: {}, Got: {}".format(
                    control_aff_grp.id, cluster.controlnodeaffinitygroupid)
            )
            self.assertEqual(
                cluster.controlnodeaffinitygroupname,
                control_aff_grp.name,
                "Control node affinity group name mismatch. Expected: {}, Got: {}".format(
                    control_aff_grp.name, cluster.controlnodeaffinitygroupname)
            )
        else:
            self.assertTrue(
                not hasattr(cluster, 'controlnodeaffinitygroupid') or cluster.controlnodeaffinitygroupid is None,
                "Control node affinity group should be None"
            )

        if worker_aff_grp is not None:
            self.assertEqual(
                cluster.workernodeaffinitygroupid,
                worker_aff_grp.id,
                "Worker node affinity group ID mismatch. Expected: {}, Got: {}".format(
                    worker_aff_grp.id, cluster.workernodeaffinitygroupid)
            )
            self.assertEqual(
                cluster.workernodeaffinitygroupname,
                worker_aff_grp.name,
                "Worker node affinity group name mismatch. Expected: {}, Got: {}".format(
                    worker_aff_grp.name, cluster.workernodeaffinitygroupname)
            )
        else:
            self.assertTrue(
                not hasattr(cluster, 'workernodeaffinitygroupid') or cluster.workernodeaffinitygroupid is None,
                "Worker node affinity group should be None"
            )

        if etcd_aff_grp is not None:
            self.assertEqual(
                cluster.etcdnodeaffinitygroupid,
                etcd_aff_grp.id,
                "ETCD node affinity group ID mismatch. Expected: {}, Got: {}".format(
                    etcd_aff_grp.id, cluster.etcdnodeaffinitygroupid)
            )
            self.assertEqual(
                cluster.etcdnodeaffinitygroupname,
                etcd_aff_grp.name,
                "ETCD node affinity group name mismatch. Expected: {}, Got: {}".format(
                    etcd_aff_grp.name, cluster.etcdnodeaffinitygroupname)
            )
        else:
            self.assertTrue(
                not hasattr(cluster, 'etcdnodeaffinitygroupid') or cluster.etcdnodeaffinitygroupid is None,
                "ETCD node affinity group should be None"
            )

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_01_create_cluster_with_control_node_affinity_group(self):
        """Test creating a Kubernetes cluster with affinity group for control nodes only

        # Validate the following:
        # 1. Create an affinity group
        # 2. Create a Kubernetes cluster with the affinity group for control nodes
        # 3. Verify cluster is created successfully and affinity group is assigned
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity group for control nodes")
        control_aff_grp = self.create_aff_grp(aff_grp_name="control-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with control node affinity group")
        cluster_name = "cks-aff-grp-control-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                control_aff_grp=control_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=None,
                etcd_aff_grp=None
            )

            self.debug("Kubernetes cluster with control node affinity group created successfully")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_02_create_cluster_with_worker_node_affinity_group(self):
        """Test creating a Kubernetes cluster with affinity group for worker nodes only

        # Validate the following:
        # 1. Create an affinity group
        # 2. Create a Kubernetes cluster with the affinity group for worker nodes
        # 3. Verify cluster is created successfully and affinity group is assigned
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity group for worker nodes")
        worker_aff_grp = self.create_aff_grp(aff_grp_name="worker-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with worker node affinity group")
        cluster_name = "cks-aff-grp-worker-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                worker_aff_grp=worker_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=None,
                worker_aff_grp=worker_aff_grp,
                etcd_aff_grp=None
            )

            self.debug("Kubernetes cluster with worker node affinity group created successfully")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_03_create_cluster_with_all_node_type_affinity_groups(self):
        """Test creating a Kubernetes cluster with different affinity groups for all node types

        # Validate the following:
        # 1. Create separate affinity groups for control, worker, and etcd nodes
        # 2. Create a Kubernetes cluster with affinity groups for all node types
        # 3. Verify cluster is created successfully and all affinity groups are assigned
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity groups for all node types")
        control_aff_grp = self.create_aff_grp(aff_grp_name="control-aff-grp-" + random_gen())
        worker_aff_grp = self.create_aff_grp(aff_grp_name="worker-aff-grp-" + random_gen())
        etcd_aff_grp = self.create_aff_grp(aff_grp_name="etcd-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with affinity groups for all node types")
        cluster_name = "cks-aff-grp-all-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                etcd_nodes=1,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=worker_aff_grp,
                etcd_aff_grp=etcd_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=worker_aff_grp,
                etcd_aff_grp=etcd_aff_grp
            )

            self.debug("Kubernetes cluster with all node type affinity groups created successfully")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_04_create_cluster_with_same_affinity_group_all_node_types(self):
        """Test creating a Kubernetes cluster with the same affinity group for all node types

        # Validate the following:
        # 1. Create a single affinity group
        # 2. Create a Kubernetes cluster using the same affinity group for all node types
        # 3. Verify cluster is created successfully
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating a single affinity group for all node types")
        shared_aff_grp = self.create_aff_grp(aff_grp_name="shared-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with same affinity group for all node types")
        cluster_name = "cks-aff-grp-shared-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                control_aff_grp=shared_aff_grp,
                worker_aff_grp=shared_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=shared_aff_grp,
                worker_aff_grp=shared_aff_grp,
                etcd_aff_grp=None
            )

            self.debug("Kubernetes cluster with shared affinity group created successfully")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_05_scale_cluster_respects_worker_affinity_group(self):
        """Test that scaling a cluster respects the worker node affinity group

        # Validate the following:
        # 1. Create a cluster with worker node affinity group
        # 2. Scale up the cluster
        # 3. Verify affinity group assignments are preserved after scaling
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity group for worker nodes")
        worker_aff_grp = self.create_aff_grp(aff_grp_name="worker-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with worker node affinity group")
        cluster_name = "cks-aff-grp-scale-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                worker_aff_grp=worker_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')

            self.debug("Scaling up Kubernetes cluster from 1 to 2 worker nodes")
            cluster = self.scaleKubernetesCluster(cluster.id, 2)

            self.verifyKubernetesClusterState(cluster, 'Running')
            self.assertEqual(
                cluster.size,
                2,
                "Cluster size should be 2 after scaling up, got {}".format(cluster.size)
            )

            # Verify affinity group is still assigned after scaling
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=None,
                worker_aff_grp=worker_aff_grp,
                etcd_aff_grp=None
            )

            self.debug("Kubernetes cluster scaled successfully with affinity group preserved")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_06_stop_start_cluster_preserves_affinity_groups(self):
        """Test that stopping and starting a cluster preserves affinity groups

        # Validate the following:
        # 1. Create a cluster with affinity groups
        # 2. Stop the cluster
        # 3. Start the cluster
        # 4. Verify affinity groups are preserved
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity groups")
        control_aff_grp = self.create_aff_grp(aff_grp_name="control-aff-grp-" + random_gen())
        worker_aff_grp = self.create_aff_grp(aff_grp_name="worker-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with affinity groups")
        cluster_name = "cks-aff-grp-lifecycle-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=worker_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')

            self.debug("Stopping Kubernetes cluster")
            self.stopKubernetesCluster(cluster.id)

            cluster = self.listKubernetesCluster(cluster.id)
            self.assertEqual(
                cluster.state,
                'Stopped',
                "Cluster should be in Stopped state, got {}".format(cluster.state)
            )

            self.debug("Starting Kubernetes cluster")
            cluster = self.startKubernetesCluster(cluster.id)

            self.verifyKubernetesClusterState(cluster, 'Running')

            # Verify affinity groups are preserved after stop/start
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=worker_aff_grp,
                etcd_aff_grp=None
            )

            self.debug("Kubernetes cluster stop/start completed with affinity groups preserved")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_07_create_cluster_with_invalid_affinity_group_id(self):
        """Test creating a cluster with an invalid affinity group ID fails

        # Validate the following:
        # 1. Attempt to create a cluster with a non-existent affinity group ID
        # 2. Verify the operation fails with appropriate error
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating Kubernetes cluster with invalid affinity group ID")
        cluster_name = "cks-aff-grp-invalid-" + random_gen()

        # Create a fake affinity group object with invalid ID
        class FakeAffinityGroup:
            def __init__(self):
                self.id = "invalid-uuid-12345"
                self.name = "fake-group"

        fake_aff_grp = FakeAffinityGroup()

        with self.assertRaises(Exception) as context:
            self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                control_aff_grp=fake_aff_grp
            )

        self.debug("Expected error when creating cluster with invalid affinity group: %s" % context.exception)

        # Clean up any partially created cluster
        cluster = self.listKubernetesCluster(cluster_name=cluster_name)
        if cluster is not None:
            self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_08_cluster_response_includes_affinity_group_details(self):
        """Test that cluster list response includes affinity group details

        # Validate the following:
        # 1. Create a cluster with affinity groups
        # 2. List the cluster
        # 3. Verify response includes affinity group IDs and names
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity groups")
        control_aff_grp = self.create_aff_grp(aff_grp_name="control-aff-grp-" + random_gen())
        worker_aff_grp = self.create_aff_grp(aff_grp_name="worker-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with affinity groups")
        cluster_name = "cks-aff-grp-response-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=worker_aff_grp
            )

            self.verifyKubernetesClusterState(cluster, 'Running')

            # List the cluster and verify response
            listed_cluster = self.listKubernetesCluster(cluster.id)

            self.assertIsNotNone(listed_cluster, "Cluster should be listed")

            # Verify control node affinity group in response
            self.assertTrue(
                hasattr(listed_cluster, 'controlnodeaffinitygroupid'),
                "Response should include controlnodeaffinitygroupid"
            )
            self.assertTrue(
                hasattr(listed_cluster, 'controlnodeaffinitygroupname'),
                "Response should include controlnodeaffinitygroupname"
            )

            # Verify worker node affinity group in response
            self.assertTrue(
                hasattr(listed_cluster, 'workernodeaffinitygroupid'),
                "Response should include workernodeaffinitygroupid"
            )
            self.assertTrue(
                hasattr(listed_cluster, 'workernodeaffinitygroupname'),
                "Response should include workernodeaffinitygroupname"
            )

            # Verify the values match
            self.verifyKubernetesClusterAffinityGroups(
                listed_cluster,
                control_aff_grp=control_aff_grp,
                worker_aff_grp=worker_aff_grp,
                etcd_aff_grp=None
            )

            self.debug("Cluster response includes correct affinity group details")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_09_create_cluster_without_affinity_groups(self):
        """Test creating a cluster without any affinity groups

        # Validate the following:
        # 1. Create a cluster without specifying any affinity groups
        # 2. Verify cluster is created and affinity group fields are null/empty
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating Kubernetes cluster without affinity groups")
        cluster_name = "cks-no-aff-grp-" + random_gen()
        try:
            cluster = self.createKubernetesCluster(
                cluster_name,
                self.kubernetes_version.id,
                size=1,
                control_nodes=1
            )

            self.verifyKubernetesClusterState(cluster, 'Running')

            # Verify no affinity groups are assigned
            self.verifyKubernetesClusterAffinityGroups(
                cluster,
                control_aff_grp=None,
                worker_aff_grp=None,
                etcd_aff_grp=None
            )

            self.debug("Kubernetes cluster created successfully without affinity groups")
        finally:
            cluster = self.listKubernetesCluster(cluster_name=cluster_name)
            if cluster is not None:
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_10_delete_cluster_with_affinity_groups(self):
        """Test that deleting a cluster with affinity groups works correctly

        # Validate the following:
        # 1. Create a cluster with affinity groups
        # 2. Delete the cluster
        # 3. Verify cluster is deleted and affinity groups still exist
        """
        if self.setup_failed:
            self.fail("Setup incomplete")

        self.debug("Creating affinity groups")
        control_aff_grp = self.create_aff_grp(aff_grp_name="control-aff-grp-" + random_gen())
        worker_aff_grp = self.create_aff_grp(aff_grp_name="worker-aff-grp-" + random_gen())

        self.debug("Creating Kubernetes cluster with affinity groups")
        cluster_name = "cks-aff-grp-delete-" + random_gen()
        cluster = self.createKubernetesCluster(
            cluster_name,
            self.kubernetes_version.id,
            size=1,
            control_nodes=1,
            control_aff_grp=control_aff_grp,
            worker_aff_grp=worker_aff_grp
        )

        self.verifyKubernetesClusterState(cluster, 'Running')
        cluster_id = cluster.id

        self.debug("Deleting Kubernetes cluster")
        self.deleteKubernetesClusterAndVerify(cluster_id)

        # Verify cluster is deleted
        deleted_cluster = self.listKubernetesCluster(cluster_id)
        self.assertIsNone(deleted_cluster, "Cluster should be deleted")

        # Verify affinity groups still exist
        control_aff_grp_list = AffinityGroup.list(self.apiclient, id=control_aff_grp.id)
        self.assertIsNotNone(control_aff_grp_list, "Control affinity group should still exist")

        worker_aff_grp_list = AffinityGroup.list(self.apiclient, id=worker_aff_grp.id)
        self.assertIsNotNone(worker_aff_grp_list, "Worker affinity group should still exist")

        self.debug("Kubernetes cluster deleted successfully, affinity groups preserved")
