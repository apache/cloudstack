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
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import (listInfrastructure,
                                  listTemplates,
                                  listKubernetesSupportedVersions,
                                  addKubernetesSupportedVersion,
                                  deleteKubernetesSupportedVersion,
                                  listKubernetesClusters,
                                  createKubernetesCluster,
                                  stopKubernetesCluster,
                                  deleteKubernetesCluster,
                                  upgradeKubernetesCluster,
                                  scaleKubernetesCluster,
                                  destroyVirtualMachine,
                                  deleteNetwork)
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import PASS, FAILED
from marvin.lib.base import (Template,
                             ServiceOffering,
                             Account,
                             Configurations)
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              random_gen)
from marvin.lib.common import (get_zone,
                               get_domain)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf

import time

_multiprocess_shared_ = True

class TestKubernetesCluster(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestKubernetesCluster, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.cks_template_name_key = "cloud.kubernetes.cluster.template.name." + cls.hypervisor.lower()

        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            cls.hypervisorNotSupported = True
        cls.setup_failed = False
        cls._cleanup = []
        cls.kubernetes_version_ids = []

        if cls.hypervisorNotSupported == False:
            cls.initial_configuration_cks_enabled = Configurations.list(cls.apiclient,
                                                                        name="cloud.kubernetes.service.enabled")[0].value
            if cls.initial_configuration_cks_enabled not in ["true", True]:
                cls.debug("Enabling CloudStack Kubernetes Service plugin and restarting management server")
                Configurations.update(cls.apiclient,
                                      "cloud.kubernetes.service.enabled",
                                      "true")
                cls.restartServer()

            cls.cks_template = None
            cls.initial_configuration_cks_template_name = None
            cls.cks_service_offering = None

            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version_1 = cls.addKubernetesSupportedVersion(cls.services["cks_kubernetes_versions"]["1.14.9"])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version_1.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state, version=%s, url=%s, %s" %
                        (cls.services["cks_kubernetes_versions"]["1.14.9"]["semanticversion"], cls.services["cks_kubernetes_versions"]["1.14.9"]["url"], e))
            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version_2 = cls.addKubernetesSupportedVersion(cls.services["cks_kubernetes_versions"]["1.15.0"])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version_2.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state, version=%s, url=%s, %s" %
                        (cls.services["cks_kubernetes_versions"]["1.15.0"]["semanticversion"], cls.services["cks_kubernetes_versions"]["1.15.0"]["url"], e))
            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version_3 = cls.addKubernetesSupportedVersion(cls.services["cks_kubernetes_versions"]["1.16.0"])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version_3.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state, version=%s, url=%s, %s" %
                        (cls.services["cks_kubernetes_versions"]["1.16.0"]["semanticversion"], cls.services["cks_kubernetes_versions"]["1.16.0"]["url"], e))
            if cls.setup_failed == False:
                try:
                    cls.kubernetes_version_4 = cls.addKubernetesSupportedVersion(cls.services["cks_kubernetes_versions"]["1.16.3"])
                    cls.kubernetes_version_ids.append(cls.kubernetes_version_4.id)
                except Exception as e:
                    cls.setup_failed = True
                    cls.debug("Failed to get Kubernetes version ISO in ready state, version=%s, url=%s, %s" %
                        (cls.services["cks_kubernetes_versions"]["1.16.3"]["semanticversion"], cls.services["cks_kubernetes_versions"]["1.16.3"]["url"], e))

            if cls.setup_failed == False:
                cls.cks_template = cls.getKubernetesTemplate()
                if cls.cks_template == FAILED:
                    assert False, "getKubernetesTemplate() failed to return template for hypervisor %s" % cls.hypervisor
                    cls.setup_failed = True
                else:
                    cls._cleanup.append(cls.cks_template)

            if cls.setup_failed == False:
                cls.initial_configuration_cks_template_name = Configurations.list(cls.apiclient,
                                                                                  name=cls.cks_template_name_key)[0].value
                Configurations.update(cls.apiclient,
                                      cls.cks_template_name_key,
                                      cls.cks_template.name)

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
        return

    @classmethod
    def tearDownClass(cls):
        version_delete_failed = False
        # Delete added Kubernetes supported version
        for version_id in cls.kubernetes_version_ids:
            try:
                cls.deleteKubernetesSupportedVersion(version_id)
            except Exception as e:
                version_delete_failed = True
                cls.debug("Error: Exception during cleanup for added Kubernetes supported versions: %s" % e)
        try:
            # Restore original CKS template
            if cls.hypervisorNotSupported == False and cls.initial_configuration_cks_template_name != None:
                Configurations.update(cls.apiclient,
                                      cls.cks_template_name_key,
                                      cls.initial_configuration_cks_template_name)
            # Restore CKS enabled
            if cls.initial_configuration_cks_enabled not in ["true", True]:
                cls.debug("Restoring Kubernetes Service enabled value")
                Configurations.update(cls.apiclient,
                                      "cloud.kubernetes.service.enabled",
                                      "false")
                cls.restartServer()

            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        if version_delete_failed == True:            
            raise Exception("Warning: Exception during cleanup, unable to delete Kubernetes supported versions")
        return

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
    def getKubernetesTemplate(cls, cks_templates=None):

        if cks_templates is None:
            cks_templates = cls.services["cks_templates"]

        hypervisor = cls.hypervisor.lower()

        if hypervisor not in cks_templates.keys():
            cls.debug("Provided hypervisor has no CKS template")
            return FAILED

        cks_template = cks_templates[hypervisor]

        cmd = listTemplates.listTemplatesCmd()
        cmd.name = cks_template['name']
        cmd.templatefilter = 'all'
        cmd.zoneid = cls.zone.id
        cmd.hypervisor = hypervisor
        templates = cls.apiclient.listTemplates(cmd)

        if validateList(templates)[0] != PASS:
            details = None
            if hypervisor in ["vmware"] and "details" in cks_template:
                details = cks_template["details"]
            template = Template.register(cls.apiclient, cks_template, zoneid=cls.zone.id, hypervisor=hypervisor.lower(), randomize_name=False, details=details)
            template.download(cls.apiclient)
            return template

        for template in templates:
            if template.isready and template.ispublic:
                return Template(template.__dict__)

        return FAILED

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
        deleteKubernetesSupportedVersionCmd.deleteiso = True
        cls.apiclient.deleteKubernetesSupportedVersion(deleteKubernetesSupportedVersionCmd)

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Delete any existing Kubernetes cluster for account
            clusters = self.listKubernetesCluster()
            if clusters != None:
                for cluster in clusters:
                    self.deleteKubernetesClusterAndVerify(cluster.id, False, True)
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_01_deploy_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. stopKubernetesCluster should stop the cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kubernetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now stopping it" % cluster_response.id)

        self.stopAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully stopped, now deleting it" % cluster_response.id)

        self.deleteKubernetesClusterAndVerify(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_02_deploy_kubernetes_ha_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_3.id, 1, 2)

        self.verifyKubernetesCluster(cluster_response, name, self.kubernetes_version_3.id, 1, 2)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now deleting it" % cluster_response.id)

        self.deleteKubernetesClusterAndVerify(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_03_deploy_invalid_kubernetes_ha_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        try:
            cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_2.id, 1, 2)
            self.debug("Invalid CKS Kubernetes HA cluster deployed with ID: %s. Deleting it and failing test." % cluster_response.id)
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("HA Kubernetes cluster deployed with Kubernetes supported version below version 1.16.0. Must be an error.")
        except CloudstackAPIException as e:
            self.debug("HA Kubernetes cluster with invalid Kubernetes supported version check successful, API failure: %s" % e)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_04_deploy_and_upgrade_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and upgrade it to newer version

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. upgradeKubernetesCluster should return valid info for the cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kubernetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now upgrading it" % cluster_response.id)

        try:
            cluster_response = self.upgradeKubernetesCluster(cluster_response.id, self.kubernetes_version_3.id)
        except Exception as e:
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Failed to upgrade Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterUpgrade(cluster_response, self.kubernetes_version_3.id)

        self.debug("Kubernetes cluster with ID: %s successfully upgraded, now deleting it" % cluster_response.id)

        self.deleteKubernetesClusterAndVerify(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return


    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_05_deploy_and_upgrade_kubernetes_ha_cluster(self):
        """Test to deploy a new HA Kubernetes cluster and upgrade it to newer version

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. upgradeKubernetesCluster should return valid info for the cluster
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_3.id, 1, 2)

        self.verifyKubernetesCluster(cluster_response, name, self.kubernetes_version_3.id, 1, 2)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now upgrading it" % cluster_response.id)

        try:
            cluster_response = self.upgradeKubernetesCluster(cluster_response.id, self.kubernetes_version_4.id)
        except Exception as e:
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Failed to upgrade Kubernetes HA cluster due to: %s" % e)

        self.verifyKubernetesClusterUpgrade(cluster_response, self.kubernetes_version_4.id)

        self.debug("Kubernetes cluster with ID: %s successfully upgraded, now deleting it" % cluster_response.id)

        self.deleteKubernetesClusterAndVerify(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_06_deploy_and_invalid_upgrade_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and check for failure while tying to upgrade it to a lower version

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. upgradeKubernetesCluster should fail
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kubernetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now scaling it" % cluster_response.id)

        try:
            cluster_response = self.upgradeKubernetesCluster(cluster_response.id, self.kubernetes_version_1.id)
            self.debug("Invalid CKS Kubernetes HA cluster deployed with ID: %s. Deleting it and failing test." % kubernetes_version_1.id)
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Kubernetes cluster upgraded to a lower Kubernetes supported version. Must be an error.")
        except Exception as e:
            self.debug("Upgrading Kubernetes cluster with invalid Kubernetes supported version check successful, API failure: %s" % e)

        self.debug("Deleting Kubernetes cluster with ID: %s" % cluster_response.id)

        self.deleteKubernetesClusterAndVerfiy(cluster_response.id, True)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_07_deploy_and_scale_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and check for failure while tying to scale it

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. scaleKubernetesCluster should return valid info for the cluster when it is scaled up
        # 4. scaleKubernetesCluster should return valid info for the cluster when it is scaled down
        """
        if self.setup_failed == True:
            self.fail("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kubernetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kubernetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now upscaling it" % cluster_response.id)

        try:
            cluster_response = self.scaleKubernetesCluster(cluster_response.id, 2)
        except Exception as e:
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Failed to upscale Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterScale(cluster_response, 2)

        self.debug("Kubernetes cluster with ID: %s successfully upscaled, now downscaling it" % cluster_response.id)

        try:
            cluster_response = self.scaleKubernetesCluster(cluster_response.id, 1)
        except Exception as e:
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Failed to downscale Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterScale(cluster_response)

        self.debug("Kubernetes cluster with ID: %s successfully downscaled, now deleting it" % cluster_response.id)

        self.deleteKubernetesClusterAndVerify(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    def listKubernetesCluster(self, cluster_id = None):
        listKubernetesClustersCmd = listKubernetesClusters.listKubernetesClustersCmd()
        if cluster_id != None:
            listKubernetesClustersCmd.id = cluster_id
        clusterResponse = self.apiclient.listKubernetesClusters(listKubernetesClustersCmd)
        if cluster_id != None:
            return clusterResponse[0]
        return clusterResponse

    def createKubernetesCluster(self, name, version_id, size=1, master_nodes=1):
        createKubernetesClusterCmd = createKubernetesCluster.createKubernetesClusterCmd()
        createKubernetesClusterCmd.name = name
        createKubernetesClusterCmd.description = name + "-description"
        createKubernetesClusterCmd.kubernetesversionid = version_id
        createKubernetesClusterCmd.size = size
        createKubernetesClusterCmd.masternodes = master_nodes
        createKubernetesClusterCmd.serviceofferingid = self.cks_service_offering.id
        createKubernetesClusterCmd.zoneid = self.zone.id
        createKubernetesClusterCmd.noderootdisksize = 10
        createKubernetesClusterCmd.account = self.account.name
        createKubernetesClusterCmd.domainid = self.domain.id
        clusterResponse = self.apiclient.createKubernetesCluster(createKubernetesClusterCmd)
        if not clusterResponse:
            self.cleanup.append(clusterResponse)
        return clusterResponse

    def stopKubernetesCluster(self, cluster_id):
        stopKubernetesClusterCmd = stopKubernetesCluster.stopKubernetesClusterCmd()
        stopKubernetesClusterCmd.id = cluster_id
        response = self.apiclient.stopKubernetesCluster(stopKubernetesClusterCmd)
        return response

    def deleteKubernetesCluster(self, cluster_id):
        deleteKubernetesClusterCmd = deleteKubernetesCluster.deleteKubernetesClusterCmd()
        deleteKubernetesClusterCmd.id = cluster_id
        response = self.apiclient.deleteKubernetesCluster(deleteKubernetesClusterCmd)
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

    def verifyKubernetesCluster(self, cluster_response, name, version_id, size=1, master_nodes=1):
        """Check if Kubernetes cluster is valid"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')

        self.assertEqual(
            cluster_response.name,
            name,
            "Check KubernetesCluster name {}, {}".format(cluster_response.name, name)
        )

        self.verifyKubernetesClusterVersion(cluster_response, version_id)

        self.assertEqual(
            cluster_response.zoneid,
            self.zone.id,
            "Check KubernetesCluster zone {}, {}".format(cluster_response.zoneid, self.zone.id)
        )

        self.verifyKubernetesClusterSize(cluster_response, size, master_nodes)

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

    def verifyKubernetesClusterSize(self, cluster_response, size=1, master_nodes=1):
        """Check if Kubernetes cluster node sizes are valid"""

        self.assertEqual(
            cluster_response.size,
            size,
            "Check KubernetesCluster size {}, {}".format(cluster_response.size, size)
        )

        self.assertEqual(
            cluster_response.masternodes,
            master_nodes,
            "Check KubernetesCluster master nodes {}, {}".format(cluster_response.masternodes, master_nodes)
        )

    def verifyKubernetesClusterUpgrade(self, cluster_response, version_id):
        """Check if Kubernetes cluster state and version are valid after upgrade"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')
        self.verifyKubernetesClusterVersion(cluster_response, version_id)

    def verifyKubernetesClusterScale(self, cluster_response, size=1, master_nodes=1):
        """Check if Kubernetes cluster state and node sizes are valid after upgrade"""

        self.verifyKubernetesClusterState(cluster_response, 'Running')
        self.verifyKubernetesClusterSize(cluster_response, size, master_nodes)

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

    def deleteKubernetesClusterAndVerify(self, cluster_id, verify = True, forced = False):
        """Delete Kubernetes cluster and check if it is really deleted"""

        forceDeleted = False
        try:
            delete_response = self.deleteKubernetesCluster(cluster_id)
        except Exception as e:
            if forced:
                cluster = self.listKubernetesCluster(cluster_id)
                if cluster != None:
                    if cluster.state in ['Running', 'Upgrading', 'Scaling']:
                        self.stopKubernetesCluster(cluster_id)
                        self.deleteKubernetesCluster(cluster_id)
                    else:
                        forceDeleted = True
                        for cluster_vm_id in cluster.virtualmachineids:
                            cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
                            cmd.id = cluster_vm_id
                            cmd.expunge = True
                            self.apiclient.destroyVirtualMachine(cmd)
                        cmd = deleteNetwork.deleteNetworkCmd()
                        cmd.id = cluster.networkid
                        cmd.forced = True
                        self.apiclient.deleteNetwork(cmd)



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
