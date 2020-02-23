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
                                  listKubernetesSupportedVersions,
                                  addKubernetesSupportedVersion,
                                  deleteKubernetesSupportedVersion,
                                  createKubernetesCluster,
                                  stopKubernetesCluster,
                                  deleteKubernetesCluster,
                                  upgradeKubernetesCluster,
                                  scaleKubernetesCluster)
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import FAILED
from marvin.lib.base import (Template,
                             ServiceOffering,
                             Configurations)
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.common import (get_zone)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

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

        cls.setup_failed = False

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

        cls.kubernetes_version_ids = []
        if cls.setup_failed == False:
            try:
                cls.kuberetes_version_1 = cls.addKubernetesSupportedVersion('1.14.9', 'http://staging.yadav.xyz/cks/binaries-iso/setup-1.14.9.iso')
                cls.kubernetes_version_ids.append(cls.kuberetes_version_1.id)
            except Exception as e:
                cls.setup_failed = True
                cls.debug("Failed to get Kubernetes version ISO in ready state, http://staging.yadav.xyz/cks/binaries-iso/setup-1.14.9.iso, %s" % e)
        if cls.setup_failed == False:
            try:
                cls.kuberetes_version_2 = cls.addKubernetesSupportedVersion('1.15.0', 'http://staging.yadav.xyz/cks/binaries-iso/setup-1.15.0.iso')
                cls.kubernetes_version_ids.append(cls.kuberetes_version_2.id)
            except Exception as e:
                cls.setup_failed = True
                cls.debug("Failed to get Kubernetes version ISO in ready state, http://staging.yadav.xyz/cks/binaries-iso/setup-1.15.0.iso, %s" % e)
        if cls.setup_failed == False:
            try:
                cls.kuberetes_version_3 = cls.addKubernetesSupportedVersion('1.16.0', 'http://staging.yadav.xyz/cks/binaries-iso/setup-1.16.0.iso')
                cls.kubernetes_version_ids.append(cls.kuberetes_version_3.id)
            except Exception as e:
                cls.setup_failed = True
                cls.debug("Failed to get Kubernetes version ISO in ready state, http://staging.yadav.xyz/cks/binaries-iso/setup-1.16.0.is, %s" % e)
        if cls.setup_failed == False:
            try:
                cls.kuberetes_version_4 = cls.addKubernetesSupportedVersion('1.16.3', 'http://staging.yadav.xyz/cks/binaries-iso/setup-1.16.3.iso')
                cls.kubernetes_version_ids.append(cls.kuberetes_version_4.id)
            except Exception as e:
                cls.setup_failed = True
                cls.debug("Failed to get Kubernetes version ISO in ready state, http://staging.yadav.xyz/cks/binaries-iso/setup-1.16.3.is, %s" % e)

        cks_template_data = {
            "name": "Kubernetes-Service-Template",
            "displaytext": "Kubernetes-Service-Template",
            "format": "qcow2",
            "hypervisor": "kvm",
            "ostype": "CoreOS",
            "url": "http://staging.yadav.xyz/cks/templates/coreos_production_cloudstack_image-kvm.qcow2.bz2",
            "ispublic": "True",
            "isextractable": "True"
        }
        # "http://dl.openvm.eu/cloudstack/coreos/x86_64/coreos_production_cloudstack_image-kvm.qcow2.bz2"
        cks_template_data_details = []
        if cls.hypervisor.lower() == "vmware":
            cks_template_data["url"] = "http://staging.yadav.xyz/cks/templates/coreos_production_cloudstack_image-vmware.ova" # "http://dl.openvm.eu/cloudstack/coreos/x86_64/coreos_production_cloudstack_image-vmware.ova"
            cks_template_data["format"] = "OVA"
            cks_template_data_details = [{"keyboard":"us","nicAdapter":"Vmxnet3","rootDiskController":"pvscsi"}]
        elif cls.hypervisor.lower() == "xenserver":
            cks_template_data["url"] = "http://staging.yadav.xyz/cks/templates/coreos_production_cloudstack_image-xen.vhd.bz2" # "http://dl.openvm.eu/cloudstack/coreos/x86_64/coreos_production_cloudstack_image-xen.vhd.bz2"
            cks_template_data["format"] = "VHD"
        elif cls.hypervisor.lower() == "kvm":
            cks_template_data["requireshvm"] = "True"
        if cls.setup_failed == False:
            cls.cks_template = Template.register(
                                             cls.apiclient,
                                             cks_template_data,
                                             zoneid=cls.zone.id,
                                             hypervisor=cls.hypervisor,
                                             details=cks_template_data_details
                                            )
            cls.debug("Waiting for CKS template with ID %s to be ready" % cls.cks_template.id)
            try:
                cls.waitForTemplateReadyState(cls.cks_template.id)
            except Exception as e:
                cls.setup_failed = True
                cls.debug("Failed to get CKS template in ready state, {}, {}".format(cks_template_data["url"], e))

            cls.initial_configuration_cks_template_name = Configurations.list(cls.apiclient,
                                                                              name=cls.cks_template_name_key)[0].value
            Configurations.update(cls.apiclient,
                                  cls.cks_template_name_key,
                                  cls.cks_template.name)

        cks_offering_data = {
            "name": "CKS-Instance",
            "displaytext": "CKS Instance",
            "cpunumber": 2,
            "cpuspeed": 1000,
            "memory": 2048,
        }
        cks_offering_data["name"] = cks_offering_data["name"] + '-' + random_gen()
        if cls.setup_failed == False:
            cls.cks_service_offering = ServiceOffering.create(
                                                              cls.apiclient,
                                                              cks_offering_data
                                                             )

        cls._cleanup = []
        if cls.cks_template != None:
            cls._cleanup.append(cls.cks_template)
        if cls.cks_service_offering != None:
            cls._cleanup.append(cls.cks_service_offering)
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
            if cls.initial_configuration_cks_template_name != None:
                Configurations.update(cls.apiclient,
                                      cls.cks_template_name_key,
                                      cls.initial_configuration_cks_template_name)
            # Delete created CKS template
            if cls.setup_failed == False and cls.cks_template != None:
                cls.cks_template.delete(cls.apiclient,
                                        cls.zone.id)
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
    def waitForTemplateReadyState(cls, template_id, retries=30, interval=30):
        """Check if template download will finish"""
        while retries > -1:
            time.sleep(interval)
            template_response = Template.list(
                cls.apiclient,
                id=template_id,
                zoneid=cls.zone.id,
                templatefilter='self'
            )

            if isinstance(template_response, list):
                template = template_response[0]
                if not hasattr(template, 'status') or not template or not template.status:
                    retries = retries - 1
                    continue
                if 'Failed' == template.status:
                    raise Exception("Failed to download template: status - %s" % template.status)
                elif template.status == 'Download Complete' and template.isready:
                    return
            retries = retries - 1
        raise Exception("Template download timed out")

    @classmethod
    def waitForKubernetesSupportedVersionIsoReadyState(cls, version_id, retries=20, interval=30):
        """Check if Kubernetes supported version ISO is in Ready state"""

        while retries > -1:
            time.sleep(interval)
            list_versions_response = cls.listKubernetesSupportedVersion(version_id)
            if not hasattr(list_versions_response, 'isostate') or not list_versions_response or not list_versions_response.isostate:
                retries = retries - 1
                continue
            if 'Creating' == list_versions_response.isostate:
                retries = retries - 1
            elif 'Ready' == list_versions_response.isostate:
                return
            elif 'Failed' == list_versions_response.isostate:
                raise Exception( "Failed to download template: status - %s" % template.status)
            else:
                raise Exception(
                    "Failed to download Kubernetes supported version ISO: status - %s" %
                    list_versions_response.isostate)
        raise Exception("Kubernetes supported version Ready state timed out")

    @classmethod
    def listKubernetesSupportedVersion(cls, version_id):
        listKubernetesSupportedVersionsCmd = listKubernetesSupportedVersions.listKubernetesSupportedVersionsCmd()
        listKubernetesSupportedVersionsCmd.id = version_id
        versionResponse = cls.apiclient.listKubernetesSupportedVersions(listKubernetesSupportedVersionsCmd)
        return versionResponse[0]

    @classmethod 
    def addKubernetesSupportedVersion(cls, semantic_version, iso_url):
        addKubernetesSupportedVersionCmd = addKubernetesSupportedVersion.addKubernetesSupportedVersionCmd()
        addKubernetesSupportedVersionCmd.semanticversion = semantic_version
        addKubernetesSupportedVersionCmd.name = 'v' + semantic_version + '-' + random_gen()
        addKubernetesSupportedVersionCmd.url = iso_url
        addKubernetesSupportedVersionCmd.mincpunumber = 2
        addKubernetesSupportedVersionCmd.minmemory = 2048
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
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_01_deploy_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. stopKubernetesCluster should stop the cluster
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kuberetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now stopping it" % cluster_response.id)

        self.stopAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully stopped, now deleting it" % cluster_response.id)

        self.deleteAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_02_deploy_kubernetes_ha_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_3.id, 1, 2)

        self.verifyKubernetesCluster(cluster_response, name, self.kuberetes_version_3.id, 1, 2)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now deleting it" % cluster_response.id)

        self.deleteAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_03_deploy_invalid_kubernetes_ha_cluster(self):
        """Test to deploy a new Kubernetes cluster

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        try:
            cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_2.id, 1, 2)
            self.debug("Invslid CKS Kubernetes HA cluster deployed with ID: %s. Deleting it and failing test." % cluster_response.id)
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("HA Kubernetes cluster deployed with Kubernetes supported version below version 1.16.0. Must be an error.")
        except CloudstackAPIException as e:
            self.debug("HA Kubernetes cluster with invalid Kubernetes supported version check successful, API failure: %s" % e)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_04_deploy_and_upgrade_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and upgrade it to newer version

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. upgradeKubernetesCluster should return valid info for the cluster
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kuberetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now upgrading it" % cluster_response.id)

        try:
            cluster_response = self.upgradeKubernetesCluster(cluster_response.id, self.kuberetes_version_3.id)
        except Exception as e:
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Failed to upgrade Kubernetes cluster due to: %s" % e)

        self.verifyKubernetesClusterUpgrade(cluster_response, self.kuberetes_version_3.id)

        self.debug("Kubernetes cluster with ID: %s successfully upgraded, now deleting it" % cluster_response.id)

        self.deleteAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return


    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_05_deploy_and_upgrade_kubernetes_ha_cluster(self):
        """Test to deploy a new HA Kubernetes cluster and upgrade it to newer version

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. upgradeKubernetesCluster should return valid info for the cluster
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_3.id, 1, 2)

        self.verifyKubernetesCluster(cluster_response, name, self.kuberetes_version_3.id, 1, 2)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now upgrading it" % cluster_response.id)

        try:
            cluster_response = self.upgradeKubernetesCluster(cluster_response.id, self.kuberetes_version_4.id)
        except Exception as e:
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Failed to upgrade Kubernetes HA cluster due to: %s" % e)

        self.verifyKubernetesClusterUpgrade(cluster_response, self.kuberetes_version_4.id)

        self.debug("Kubernetes cluster with ID: %s successfully upgraded, now deleting it" % cluster_response.id)

        self.deleteAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_06_deploy_and_invalid_upgrade_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and check for failure while tying to upgrade it to a lower version

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. upgradeKubernetesCluster should fail
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kuberetes_version_2.id)

        self.debug("Kubernetes cluster with ID: %s successfully deployed, now scaling it" % cluster_response.id)

        try:
            cluster_response = self.upgradeKubernetesCluster(cluster_response.id, self.kuberetes_version_1.id)
            self.debug("Invalid CKS Kubernetes HA cluster deployed with ID: %s. Deleting it and failing test." % kuberetes_version_1.id)
            self.deleteKubernetesCluster(cluster_response.id)
            self.fail("Kubernetes cluster upgraded to a lower Kubernetes supported version. Must be an error.")
        except Exception as e:
            self.debug("Upgrading Kubernetes cluster with invalid Kubernetes supported version check successful, API failure: %s" % e)

        self.debug("Deleting Kubernetes cluster with ID: %s" % cluster_response.id)

        self.deleteAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_07_deploy_and_scale_kubernetes_cluster(self):
        """Test to deploy a new Kubernetes cluster and check for failure while tying to scale it

        # Validate the following:
        # 1. createKubernetesCluster should return valid info for new cluster
        # 2. The Cloud Database contains the valid information
        # 3. scaleKubernetesCluster should return valid info for the cluster when it is scaled up
        # 4. scaleKubernetesCluster should return valid info for the cluster when it is scaled down
        """
        if self.hypervisor.lower() not in ["kvm", "vmware", "xenserver"]:
            self.skipTest("CKS not supported for hypervisor: %s" % self.hypervisor.lower())
        if self.setup_failed == True:
            self.skipTest("Setup incomplete")
        name = 'testcluster-' + random_gen()
        self.debug("Creating for Kubernetes cluster with name %s" % name)

        cluster_response = self.createKubernetesCluster(name, self.kuberetes_version_2.id)

        self.verifyKubernetesCluster(cluster_response, name, self.kuberetes_version_2.id)

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

        self.deleteAndVerifyKubernetesCluster(cluster_response.id)

        self.debug("Kubernetes cluster with ID: %s successfully deleted" % cluster_response.id)

        return

    def listKubernetesCluster(self, cluster_id):
        listKubernetesClustersCmd = listKubernetesClusters.listKubernetesClustersCmd()
        listKubernetesClustersCmd.id = cluster_id
        clusterResponse = self.apiclient.listKubernetesClusters(listKubernetesClustersCmd)
        return clusterResponse[0]

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

    def deleteAndVerifyKubernetesCluster(self, cluster_id):
        """Delete Kubernetes cluster and check if it is really deleted"""

        delete_response = self.deleteKubernetesCluster(cluster_id)

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
