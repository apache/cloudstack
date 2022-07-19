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
""" Tests for Kubernetes supported version """

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (listInfrastructure,
                                  listTemplates,
                                  listKubernetesSupportedVersions,
                                  addKubernetesSupportedVersion,
                                  deleteKubernetesSupportedVersion,
                                  listKubernetesClusters,
                                  createKubernetesCluster,
                                  stopKubernetesCluster,
                                  startKubernetesCluster,
                                  deleteKubernetesCluster,
                                  upgradeKubernetesCluster,
                                  scaleKubernetesCluster,
                                  getKubernetesClusterConfig,
                                  destroyVirtualMachine,
                                  deleteNetwork)
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import PASS, FAILED
from marvin.lib.base import (Template,
                             Network,
                             ServiceOffering,
                             Account,
                             StoragePool,
                             Configurations)
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              random_gen)
from marvin.lib.common import (get_zone,
                               get_domain)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf

from kubernetes import client, config
import time, io, yaml

_multiprocess_shared_ = True

k8s_cluster = None

class TestKubernetesCluster(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestKubernetesCluster, cls).getClsTestClient()
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

        if cls.hypervisorNotSupported == False:
            cls.endpoint_url = Configurations.list(cls.apiclient, name="endpoint.url")[0].value
            if "localhost" in cls.endpoint_url:
                endpoint_url = "http://%s:%d/client/api " %(cls.mgtSvrDetails["mgtSvrIp"], cls.mgtSvrDetails["port"])
                cls.debug("Setting endpoint.url to %s" %(endpoint_url))
                Configurations.update(cls.apiclient, "endpoint.url", endpoint_url)
            cls.initial_configuration_cks_enabled = Configurations.list(cls.apiclient, name="cloud.kubernetes.service.enabled")[0].value
            if cls.initial_configuration_cks_enabled not in ["true", True]:
                cls.debug("Enabling CloudStack Kubernetes Service plugin and restarting management server")
                Configurations.update(cls.apiclient,
                                      "cloud.kubernetes.service.enabled",
                                      "true")
                cls.restartServer()
            cls.updateVmwareSettings(False)
            cls.cks_service_offering = None

            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version_1_23_3 = cls.addKubernetesSupportedVersion(cls.services["cks_kubernetes_versions"]["1.23.3"])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version_1_23_3.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state, version=%s, url=%s, %s" %
                        (cls.services["cks_kubernetes_versions"]["1.23.3"]["semanticversion"], cls.services["cks_kubernetes_versions"]["1.23.3"]["url"], e))
            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version_1_24_0 = cls.addKubernetesSupportedVersion(cls.services["cks_kubernetes_versions"]["1.24.0"])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version_1_24_0.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state, version=%s, url=%s, %s" %
                        (cls.services["cks_kubernetes_versions"]["1.24.0"]["semanticversion"], cls.services["cks_kubernetes_versions"]["1.24.0"]["url"], e))

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
        if str(cls.zone.securitygroupsenabled) == "True":
            networks = Network.list(
                cls.apiclient,
                listall=True
            )
            cls.default_network = networks[0]

        return

    @classmethod
    def tearDownClass(cls):
        if k8s_cluster != None and k8s_cluster.id != None:
            clsObj = TestKubernetesCluster()
            clsObj.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)

        version_delete_failed = False
        # Delete added Kubernetes supported version
        for version_id in cls.kubernetes_version_ids:
            try:
                cls.deleteKubernetesSupportedVersion(version_id)
            except Exception as e:
                version_delete_failed = True
                cls.debug("Error: Exception during cleanup for added Kubernetes supported versions: %s" % e)
        try:
            # Restore CKS enabled
            if cls.initial_configuration_cks_enabled not in ["true", True]:
                cls.debug("Restoring Kubernetes Service enabled value")
                Configurations.update(cls.apiclient,
                                      "cloud.kubernetes.service.enabled",
                                      "false")
                cls.restartServer()

            cls.updateVmwareSettings(True)

            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        if version_delete_failed == True:
            raise Exception("Warning: Exception during cleanup, unable to delete Kubernetes supported versions")
        return

    @classmethod
    def updateVmwareSettings(cls, tearDown):
        value = "false"
        if not tearDown:
            value = "true"
        if cls.hypervisor.lower() == 'vmware':
            Configurations.update(cls.apiclient,
                                  "vmware.create.full.clone",
                                  value)
            allStoragePools = StoragePool.list(
                cls.apiclient
            )
            for pool in allStoragePools:
                Configurations.update(cls.apiclient,
                                      storageid=pool.id,
                                      name="vmware.create.full.clone",
                                      value=value)

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

        #Waits for management to come up in 5 mins, when it's up it will continue
        timeout = time.time() + 300
        while time.time() < timeout:
            if cls.isManagementUp() is True: return
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
                raise Exception( "Failed to download template: status - %s" % template.status)
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
    def listKubernetesCluster(cls, cluster_id = None):
        listKubernetesClustersCmd = listKubernetesClusters.listKubernetesClustersCmd()
        listKubernetesClustersCmd.listall = True
        if cluster_id != None:
            listKubernetesClustersCmd.id = cluster_id
        clusterResponse = cls.apiclient.listKubernetesClusters(listKubernetesClustersCmd)
        if cluster_id != None and clusterResponse != None:
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



    def deleteKubernetesClusterAndVerify(self, cluster_id, verify = True, forced = False):
        """Delete Kubernetes cluster and check if it is really deleted"""

        delete_response = {}
        forceDeleted = False
        try:
            delete_response = self.deleteKubernetesCluster(cluster_id)
        except Exception as e:
            if forced:
                cluster = self.listKubernetesCluster(cluster_id)
                if cluster != None:
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
                        self.dbclient.execute("update kubernetes_cluster set state='Destroyed', removed=now() where uuid = '%s';" % cluster.id)
            else:
                raise Exception("Error: Exception during delete cluster : %s" % e)

        if verify == True and forceDeleted == False:
            self.assertEqual(
                delete_response.success,
                True,
                "Check KubernetesCluster delete response {}, {}".format(delete_response.success, True)
            )

            db_cluster_removed = self.dbclient.execute("select removed from kubernetes_cluster where uuid = '%s';" % cluster_id)[0][0]

            self.assertNotEqual(
                db_cluster_removed,
                None,
                "KubernetesCluster not removed in DB, {}".format(db_cluster_removed)
            )

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
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

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_01_invalid_upgrade_kubernetes_cluster(self):
        """Test to check for failure while tying to upgrade a Kubernetes cluster to a lower version

        # Validate the following:
        # 1. upgradeKubernetesCluster should fail
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster(version=self.kubernetes_version_1_24_0)

        self.debug("Downgrading Kubernetes cluster with ID: %s to a lower version. This should fail!" % k8s_cluster.id)

        try:
            k8s_cluster = self.upgradeKubernetesCluster(k8s_cluster.id, self.kubernetes_version_1_23_3.id)
            self.debug("Invalid CKS Kubernetes HA cluster deployed with ID: %s. Deleting it and failing test." % self.kubernetes_version_1_23_3.id)
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Kubernetes cluster downgrade to a lower Kubernetes supported version. Must be an error.")
        except Exception as e:
            self.debug("Upgrading Kubernetes cluster with invalid Kubernetes supported version check successful, API failure: %s" % e)
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)

        self.verifyKubernetesClusterUpgrade(k8s_cluster, self.kubernetes_version_1_24_0.id)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_02_upgrade_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and upgrade it to newer version

        # Validate the following:
        # 1. upgradeKubernetesCluster should return valid info for the cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster(version=self.kubernetes_version_1_23_3)

        time.sleep(self.services["sleep"])
        self.debug("Upgrading Kubernetes cluster with ID: %s" % k8s_cluster.id)
        try:
            k8s_cluster = self.upgradeKubernetesCluster(k8s_cluster.id, self.kubernetes_version_1_24_0.id)
        except Exception as e:
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Failed to upgrade Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterUpgrade(k8s_cluster, self.kubernetes_version_1_24_0.id)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_03_deploy_and_scale_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and check for failure while tying to scale it

        # Validate the following:
        # 1. scaleKubernetesCluster should return valid info for the cluster when it is scaled up
        # 2. scaleKubernetesCluster should return valid info for the cluster when it is scaled down
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster()

        self.debug("Upscaling Kubernetes cluster with ID: %s" % k8s_cluster.id)
        try:
            k8s_cluster = self.scaleKubernetesCluster(k8s_cluster.id, 2)
        except Exception as e:
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Failed to upscale Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterScale(k8s_cluster, 2)
        self.debug("Kubernetes cluster with ID: %s successfully upscaled, now downscaling it" % k8s_cluster.id)

        try:
            k8s_cluster = self.scaleKubernetesCluster(k8s_cluster.id, 1)
        except Exception as e:
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Failed to downscale Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterScale(k8s_cluster)
        self.debug("Kubernetes cluster with ID: %s successfully downscaled" % k8s_cluster.id)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_04_autoscale_kubernetes_cluster(self):
        """Test to enable autoscaling a Kubernetes cluster
        # Validate the following:
        # 1. scaleKubernetesCluster should return valid info for the cluster when it is autoscaled
        # 2. cluster-autoscaler pod should be running
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster(version=self.kubernetes_version_1_24_0)

        self.debug("Autoscaling Kubernetes cluster with ID: %s" % k8s_cluster.id)
        try:
            k8s_cluster = self.autoscaleKubernetesCluster(k8s_cluster.id, 1, 2)
            self.verifyKubernetesClusterAutocale(k8s_cluster, 1, 2)

            up = self.waitForAutoscalerPodInRunningState(k8s_cluster.id)
            self.assertTrue(up, "Autoscaler pod failed to run")
            self.debug("Kubernetes cluster with ID: %s has autoscaler running" % k8s_cluster.id)
        except Exception as e:
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Failed to autoscale Kubernetes cluster due to: %s" % e)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_05_basic_lifecycle_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. stopKubernetesCluster should stop the cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster()

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now stopping it" % k8s_cluster.id)

        self.stopAndVerifyKubernetesCluster(k8s_cluster.id)

        self.debug("Kubernetes cluster with ID: %s successfully stopped, now starting it again" % k8s_cluster.id)

        try:
            k8s_cluster = self.startKubernetesCluster(k8s_cluster.id)
        except Exception as e:
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Failed to start Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterState(k8s_cluster, 'Running')
        return


    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_06_delete_kubernetes_cluster(self):
        """Test to delete an existing Kubernetes cluster

        # Validate the following:
        # 1. deleteKubernetesCluster should delete an existing Kubernetes cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster()

        self.debug("Deleting Kubernetes cluster with ID: %s" % k8s_cluster.id)

        self.deleteKubernetesClusterAndVerify(k8s_cluster.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % k8s_cluster.id)

        k8s_cluster = None

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_07_deploy_kubernetes_ha_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        if self.default_network:
            self.skipTest("HA cluster on shared network requires external ip address, skipping it")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster(1, 2)
        self.debug("HA Kubernetes cluster with ID: %s successfully deployed" % k8s_cluster.id)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_08_upgrade_kubernetes_ha_cluster(self):
        """Test to upgrade a Kubernetes cluster to newer version

        # Validate the following:
        # 1. upgradeKubernetesCluster should return valid info for the cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        if self.default_network:
            self.skipTest("HA cluster on shared network requires external ip address, skipping it")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster(1, 2, version=self.kubernetes_version_1_23_3)
        time.sleep(self.services["sleep"])

        self.debug("Upgrading HA Kubernetes cluster with ID: %s" % k8s_cluster.id)
        try:
            k8s_cluster = self.upgradeKubernetesCluster(k8s_cluster.id, self.kubernetes_version_1_24_0.id)
        except Exception as e:
            self.deleteKubernetesClusterAndVerify(k8s_cluster.id, False, True)
            self.fail("Failed to upgrade Kubernetes HA cluster due to: %s" % e)

        self.verifyKubernetesClusterUpgrade(k8s_cluster, self.kubernetes_version_1_24_0.id)
        self.debug("Kubernetes cluster with ID: %s successfully upgraded" % k8s_cluster.id)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_09_delete_kubernetes_ha_cluster(self):
        """Test to delete a HA Kubernetes cluster

        # Validate the following:
        # 1. deleteKubernetesCluster should delete an existing HA Kubernetes cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        if self.default_network:
            self.skipTest("HA cluster on shared network requires external ip address, skipping it")
        global k8s_cluster
        k8s_cluster = self.getValidKubernetesCluster(1, 2)

        self.debug("Deleting Kubernetes cluster with ID: %s" % k8s_cluster.id)
        return

    def createKubernetesCluster(self, name, version_id, size=1, control_nodes=1):
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
        if self.default_network:
            createKubernetesClusterCmd.networkid = self.default_network.id
        clusterResponse = self.apiclient.createKubernetesCluster(createKubernetesClusterCmd)
        if not clusterResponse:
            self.cleanup.append(clusterResponse)
        return clusterResponse

    def startKubernetesCluster(self, cluster_id):
        startKubernetesClusterCmd = startKubernetesCluster.startKubernetesClusterCmd()
        startKubernetesClusterCmd.id = cluster_id
        response = self.apiclient.startKubernetesCluster(startKubernetesClusterCmd)
        return response

    def upgradeKubernetesCluster(self, cluster_id, version_id):
        upgradeKubernetesClusterCmd = upgradeKubernetesCluster.upgradeKubernetesClusterCmd()
        upgradeKubernetesClusterCmd.id = cluster_id
        upgradeKubernetesClusterCmd.kubernetesversionid = version_id
        response = self.apiclient.upgradeKubernetesCluster(upgradeKubernetesClusterCmd)
        return response

    def scaleKubernetesCluster(self, cluster_id, size):
        scaleKubernetesClusterCmd = scaleKubernetesCluster.scaleKubernetesClusterCmd()
        scaleKubernetesClusterCmd.id = cluster_id
        scaleKubernetesClusterCmd.size = size
        response = self.apiclient.scaleKubernetesCluster(scaleKubernetesClusterCmd)
        return response

    def autoscaleKubernetesCluster(self, cluster_id, minsize, maxsize):
        scaleKubernetesClusterCmd = scaleKubernetesCluster.scaleKubernetesClusterCmd()
        scaleKubernetesClusterCmd.id = cluster_id
        scaleKubernetesClusterCmd.autoscalingenabled = True
        scaleKubernetesClusterCmd.minsize = minsize
        scaleKubernetesClusterCmd.maxsize = maxsize
        response = self.apiclient.scaleKubernetesCluster(scaleKubernetesClusterCmd)
        return response

    def fetchKubernetesClusterConfig(self, cluster_id):
        getKubernetesClusterConfigCmd = getKubernetesClusterConfig.getKubernetesClusterConfigCmd()
        getKubernetesClusterConfigCmd.id = cluster_id
        response = self.apiclient.getKubernetesClusterConfig(getKubernetesClusterConfigCmd)
        return response

    def waitForAutoscalerPodInRunningState(self, cluster_id, retries=5, interval=60):
        k8s_config = self.fetchKubernetesClusterConfig(cluster_id)
        cfg = io.StringIO(k8s_config.configdata)
        cfg = yaml.safe_load(cfg)
        # Adding this so we don't get certificate exceptions
        cfg['clusters'][0]['cluster']['insecure-skip-tls-verify']=True
        config.load_kube_config_from_dict(cfg)
        v1 = client.CoreV1Api()

        while retries > 0:
            time.sleep(interval)
            pods = v1.list_pod_for_all_namespaces(watch=False, label_selector="app=cluster-autoscaler").items
            if len(pods) == 0 :
                self.debug("Autoscaler pod still not up")
                continue
            pod = pods[0]
            if pod.status.phase == 'Running' :
                self.debug("Autoscaler pod %s up and running!" % pod.metadata.name)
                return True
            self.debug("Autoscaler pod %s up but not running on retry %d. State is : %s" %(pod.metadata.name, retries, pod.status.phase))
            retries = retries - 1
        return False

    def getValidKubernetesCluster(self, size=1, control_nodes=1, version={}):
        cluster = k8s_cluster

        # Does a cluster already exist ?
        if cluster == None or cluster.id == None:
            if not version:
                version = self.kubernetes_version_1_24_0
            self.debug("No existing cluster available, k8s_cluster: %s" % cluster)
            return self.createNewKubernetesCluster(version, size, control_nodes)

        # Is the existing cluster what is needed ?
        valid = cluster.size == size and cluster.controlnodes == control_nodes
        if version:
            # Check the version only if specified
            valid = valid and cluster.kubernetesversionid == version.id
        else:
            version = self.kubernetes_version_1_24_0

        if valid:
            cluster_id = cluster.id
            cluster = self.listKubernetesCluster(cluster_id)
            if cluster == None:
                # Looks like the cluster disappeared !
                self.debug("Existing cluster, k8s_cluster ID: %s not returned by list API" % cluster_id)
                return self.createNewKubernetesCluster(version, size, control_nodes)

        if valid:
            try:
                self.verifyKubernetesCluster(cluster, cluster.name, None, size, control_nodes)
                self.debug("Existing Kubernetes cluster available with name %s" % cluster.name)
                return cluster
            except  AssertionError as error:
                self.debug("Existing cluster failed verification due to %s, need to deploy a new one" % error)
                self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

        # Can't have too many loose clusters running around
        if cluster.id != None:
            self.deleteKubernetesClusterAndVerify(cluster.id, False, True)

        self.debug("No valid cluster, need to deploy a new one")
        return self.createNewKubernetesCluster(version, size, control_nodes)

    def createNewKubernetesCluster(self, version, size, control_nodes) :
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)
        try:
            cluster = self.createKubernetesCluster(name, version.id, size, control_nodes)
            self.verifyKubernetesCluster(cluster, name, version.id, size, control_nodes)
        except Exception as ex:
            self.fail("Kubernetes cluster deployment failed: %s" % ex)
        except AssertionError as err:
            self.fail("Kubernetes cluster deployment failed during cluster verification: %s" % err)
        return cluster

    def verifyKubernetesCluster(self, cluster_response, name, version_id=None, size=1, control_nodes=1):
        """Check if Kubernetes cluster is valid"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')

        if name != None:
            self.assertEqual(
                cluster_response.name,
                name,
                "Check KubernetesCluster name {}, {}".format(cluster_response.name, name)
            )

        if version_id != None:
            self.verifyKubernetesClusterVersion(cluster_response, version_id)

        self.assertEqual(
            cluster_response.zoneid,
            self.zone.id,
            "Check KubernetesCluster zone {}, {}".format(cluster_response.zoneid, self.zone.id)
        )

        self.verifyKubernetesClusterSize(cluster_response, size, control_nodes)

        db_cluster_name = self.dbclient.execute("select name from kubernetes_cluster where uuid = '%s';" % cluster_response.id)[0][0]

        self.assertEqual(
            str(db_cluster_name),
            name,
            "Check KubernetesCluster name in DB {}, {}".format(db_cluster_name, name)
        )

    def verifyKubernetesClusterState(self, cluster_response, state):
        """Check if Kubernetes cluster state is Running"""

        self.assertEqual(
            cluster_response.state,
            'Running',
            "Check KubernetesCluster state {}, {}".format(cluster_response.state, state)
        )

    def verifyKubernetesClusterVersion(self, cluster_response, version_id):
        """Check if Kubernetes cluster node sizes are valid"""

        self.assertEqual(
            cluster_response.kubernetesversionid,
            version_id,
            "Check KubernetesCluster version {}, {}".format(cluster_response.kubernetesversionid, version_id)
        )

    def verifyKubernetesClusterSize(self, cluster_response, size=1, control_nodes=1):
        """Check if Kubernetes cluster node sizes are valid"""

        self.assertEqual(
            cluster_response.size,
            size,
            "Check KubernetesCluster size {}, {}".format(cluster_response.size, size)
        )

        self.assertEqual(
            cluster_response.controlnodes,
            control_nodes,
            "Check KubernetesCluster control nodes {}, {}".format(cluster_response.controlnodes, control_nodes)
        )

    def verifyKubernetesClusterUpgrade(self, cluster_response, version_id):
        """Check if Kubernetes cluster state and version are valid after upgrade"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')
        self.verifyKubernetesClusterVersion(cluster_response, version_id)

    def verifyKubernetesClusterScale(self, cluster_response, size=1, control_nodes=1):
        """Check if Kubernetes cluster state and node sizes are valid after upgrade"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')
        self.verifyKubernetesClusterSize(cluster_response, size, control_nodes)

    def verifyKubernetesClusterAutocale(self, cluster_response, minsize, maxsize):
        """Check if Kubernetes cluster state and node sizes are valid after upgrade"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')
        self.assertEqual(
            cluster_response.minsize,
            minsize,
            "Check KubernetesCluster minsize {}, {}".format(cluster_response.minsize, minsize)
        )
        self.assertEqual(
            cluster_response.maxsize,
            maxsize,
            "Check KubernetesCluster maxsize {}, {}".format(cluster_response.maxsize, maxsize)
        )

    def stopAndVerifyKubernetesCluster(self, cluster_id):
        """Stop Kubernetes cluster and check if it is really stopped"""

        stop_response = self.stopKubernetesCluster(cluster_id)

        self.assertEqual(
            stop_response.success,
            True,
            "Check KubernetesCluster stop response {}, {}".format(stop_response.success, True)
        )

        db_cluster_state = self.dbclient.execute("select state from kubernetes_cluster where uuid = '%s';" % cluster_id)[0][0]

        self.assertEqual(
            db_cluster_state,
            'Stopped',
            "KubernetesCluster not stopped in DB, {}".format(db_cluster_state)
        )
