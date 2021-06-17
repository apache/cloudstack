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
                                  listKubernetesSupportedVersions,
                                  addKubernetesSupportedVersion,
                                  deleteKubernetesSupportedVersion)
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import FAILED
from marvin.lib.base import Configurations
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.common import get_zone
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

import time

_multiprocess_shared_ = True

class TestKubernetesSupportedVersion(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestKubernetesSupportedVersion, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.kubernetes_version_iso_url = 'http://download.cloudstack.org/cks/setup-1.16.3.iso'

        cls.initial_configuration_cks_enabled = Configurations.list(cls.apiclient,
                                                                    name="cloud.kubernetes.service.enabled")[0].value
        if cls.initial_configuration_cks_enabled not in ["true", True]:
            cls.debug("Enabling CloudStack Kubernetes Service plugin and restarting management server")
            Configurations.update(cls.apiclient,
                                  "cloud.kubernetes.service.enabled",
                                  "true")
            cls.restartServer()

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
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
        return cls.fail("Management server did not come up, failing")

    @classmethod
    def isManagementUp(cls):
        try:
            cls.apiclient.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

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
    def test_01_add_delete_kubernetes_supported_version(self):
        """Test to add a new Kubernetes supported version

        # Validate the following:
        # 1. addKubernetesSupportedVersion should return valid info for new version
        # 2. The Cloud Database contains the valid information when listKubernetesSupportedVersions is called
        """

        version = self.services["cks_kubernetes_versions"]["1.16.3"]
        name = 'v' + version["semanticversion"] + '-' + random_gen()

        self.debug("Adding Kubernetes supported version with name: %s" % name)

        version_response = self.addKubernetesSupportedVersion(version["semanticversion"], name, self.zone.id, version["url"], version["mincpunumber"], version["minmemory"])

        list_versions_response = self.listKubernetesSupportedVersion(version_response.id)

        self.assertEqual(
            list_versions_response.name,
            name,
            "Check KubernetesSupportedVersion name {}, {}".format(list_versions_response.name, name)
        )

        self.assertEqual(
            list_versions_response.semanticversion,
            version["semanticversion"],
            "Check KubernetesSupportedVersion version {}, {}".format(list_versions_response.semanticversion, version["semanticversion"])
        )
        self.assertEqual(
            list_versions_response.zoneid,
            self.zone.id,
            "Check KubernetesSupportedVersion zone {}, {}".format(list_versions_response.zoneid, self.zone.id)
        )

        db_version_name = self.dbclient.execute("select name from kubernetes_supported_version where uuid = '%s';" % version_response.id)[0][0]

        self.assertEqual(
            str(db_version_name),
            name,
            "Check KubernetesSupportedVersion name in DB {}, {}".format(db_version_name, name)
        )

        self.debug("Added Kubernetes supported version with ID: %s. Waiting for its ISO to be Ready" % version_response.id)

        self.waitForKubernetesSupportedVersionIsoReadyState(version_response.id)

        self.debug("Deleting Kubernetes supported version with ID: %s" % version_response.id)

        delete_response = self.deleteKubernetesSupportedVersion(version_response.id, True)

        self.assertEqual(
            delete_response.success,
            True,
            "Check KubernetesSupportedVersion deletion in DB {}, {}".format(delete_response.success, True)
        )

        db_version_removed = self.dbclient.execute("select removed from kubernetes_supported_version where uuid = '%s';" % version_response.id)[0][0]

        self.assertNotEqual(
            db_version_removed,
            None,
            "KubernetesSupportedVersion not removed in DB"
        )

        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_02_add_unsupported_kubernetes_supported_version(self):
        """Test to trying to add a new unsupported Kubernetes supported version

        # Validate the following:
        # 1. API should return an error
        """

        version = '1.1.1'
        name = 'v' + version + '-' + random_gen()
        try:
            version_response = self.addKubernetesSupportedVersion(version, name, self.zone.id, self.kubernetes_version_iso_url)
            self.debug("Unsupported CKS Kubernetes supported added with ID: %s. Deleting it and failing test." % version_response.id)
            self.waitForKubernetesSupportedVersionIsoReadyState(version_response.id)
            self.deleteKubernetesSupportedVersion(version_response.id, True)
            self.fail("Kubernetes supported version below version 1.11.0 been added. Must be an error.")
        except CloudstackAPIException as e:
            self.debug("Unsupported version error check successful, API failure: %s" % e)
        return

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_03_add_invalid_kubernetes_supported_version(self):
        """Test to trying to add a new unsupported Kubernetes supported version

        # Validate the following:
        # 1. API should return an error
        """

        version = 'invalid'
        name = 'v' + version + '-' + random_gen()
        try:
            version_response = self.addKubernetesSupportedVersion(version, name, self.zone.id, self.kubernetes_version_iso_url)
            self.debug("Invalid Kubernetes supported added with ID: %s. Deleting it and failing test." % version_response.id)
            self.waitForKubernetesSupportedVersionIsoReadyState(version_response.id)
            self.deleteKubernetesSupportedVersion(version_response.id, True)
            self.fail("Invalid Kubernetes supported version has been added. Must be an error.")
        except CloudstackAPIException as e:
            self.debug("Unsupported version error check successful, API failure: %s" % e)
        return

    def addKubernetesSupportedVersion(self, version, name, zoneId, isoUrl, mincpunumber=2, minmemory=2048):
        addKubernetesSupportedVersionCmd = addKubernetesSupportedVersion.addKubernetesSupportedVersionCmd()
        addKubernetesSupportedVersionCmd.semanticversion = version
        addKubernetesSupportedVersionCmd.name = name
        addKubernetesSupportedVersionCmd.zoneid = zoneId
        addKubernetesSupportedVersionCmd.url = isoUrl
        addKubernetesSupportedVersionCmd.mincpunumber = mincpunumber
        addKubernetesSupportedVersionCmd.minmemory = minmemory
        versionResponse = self.apiclient.addKubernetesSupportedVersion(addKubernetesSupportedVersionCmd)
        if not versionResponse:
            self.cleanup.append(versionResponse)
        return versionResponse

    def listKubernetesSupportedVersion(self, versionId):
        listKubernetesSupportedVersionsCmd = listKubernetesSupportedVersions.listKubernetesSupportedVersionsCmd()
        listKubernetesSupportedVersionsCmd.id = versionId
        versionResponse = self.apiclient.listKubernetesSupportedVersions(listKubernetesSupportedVersionsCmd)
        return versionResponse[0]

    def deleteKubernetesSupportedVersion(self, cmd):
        response = self.apiclient.deleteKubernetesSupportedVersion(cmd)
        return response

    def deleteKubernetesSupportedVersion(self, versionId, deleteIso):
        deleteKubernetesSupportedVersionCmd = deleteKubernetesSupportedVersion.deleteKubernetesSupportedVersionCmd()
        deleteKubernetesSupportedVersionCmd.id = versionId
        deleteKubernetesSupportedVersionCmd.deleteiso = deleteIso
        response = self.apiclient.deleteKubernetesSupportedVersion(deleteKubernetesSupportedVersionCmd)
        return response

    def waitForKubernetesSupportedVersionIsoReadyState(self, version_id, retries=30, interval=60):
        """Check if Kubernetes supported version ISO is in Ready state"""

        while retries > 0:
            time.sleep(interval)
            list_versions_response = self.listKubernetesSupportedVersion(version_id)
            if not hasattr(list_versions_response, 'isostate') or not list_versions_response or not list_versions_response.isostate:
                retries = retries - 1
                continue
            if 'Ready' == list_versions_response.isostate:
                return
            elif 'Failed' == list_versions_response.isostate:
                raise Exception( "Failed to download template: status - %s" % template.status)
            retries = retries - 1
        raise Exception("Kubernetes supported version Ready state timed out")
