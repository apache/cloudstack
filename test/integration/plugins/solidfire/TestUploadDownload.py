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

import logging
import random
import SignedAPICall
import urllib.request, urllib.error, urllib.parse

from solidfire.factory import ElementFactory

from util import sf_util

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, DiskOffering, ServiceOffering, StoragePool, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_clusters, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources, wait_until

# Prerequisites:
#  Only one zone
#  Only one pod
#  Only one cluster

# Note:
#  If you do have more than one cluster, you might need to change this line: cls.cluster = list_clusters(cls.apiClient)[0]
#  Set extract.url.cleanup.interval to 240.
#  Set extract.url.expiration.interval to 120.


class TestData:
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    diskOffering = "diskoffering"
    domainId = "domainId"
    hypervisor = "hypervisor"
    kvm = "kvm"
    login = "login"
    mvip = "mvip"
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    provider = "provider"
    scope = "scope"
    solidFire = "solidfire"
    storageTag = "SolidFire_SAN_1"
    tags = "tags"
    url = "url"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    volume_1 = "volume_1"
    xenServer = "xenserver"
    zoneId = "zoneId"

    # modify to control which hypervisor type to test
    hypervisor_type = kvm
    volume_url = "http://10.117.40.114/tiny-centos-63.qcow2"
    file_type = "QCOW2"
    properties_file = "volume.properties"
    install_path_index = 14
    secondary_storage_server = "10.117.40.114"
    secondary_storage_server_root = "/export/secondary/"
    secondary_storage_server_username = "cloudstack"
    secondary_storage_server_password = "solidfire"
    # "HTTP_DOWNLOAD" and "FTP_UPLOAD" are valid for download_mode, but they lead to the same behavior
    download_mode = "HTTP_DOWNLOAD"

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "10.117.40.120",
                TestData.username: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://10.117.40.120:443"
            },
            TestData.account: {
                "email": "test@test.com",
                "firstname": "John",
                "lastname": "Doe",
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                "email": "user@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.primaryStorage: {
                "name": "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                "url": "MVIP=10.117.40.120;SVIP=10.117.41.120;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any"
            },
            TestData.virtualMachine: {
                "name": "TestVM",
                "displayname": "Test VM"
            },
            TestData.computeOffering: {
                "name": "SF_CO_1",
                "displaytext": "SF_CO_1 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "10000",
                "maxiops": "15000",
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag
            },
            TestData.diskOffering: {
                "name": "SF_DO_1",
                "displaytext": "SF_DO_1 Custom Size",
                "customizediops": False,
                "miniops": 5000,
                "maxiops": 10000,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.volume_1: {
                "diskname": "testvolume",
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestUploadDownload(cloudstackTestCase):
    errorText = "should be either detached or the VM should be in stopped state"
    assertText = "The length of the response for the 'volume_store_ref' result should be equal to 1."
    assertText2 = "The length of the response for the 'volume_store_ref' result should be equal to 0."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestUploadDownload, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.cluster = list_clusters(cls.apiClient)[0]
        cls.template = get_template(cls.apiClient, cls.zone.id, hypervisor=TestData.hypervisor_type)
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])

        # Create test account
        cls.account = Account.create(
            cls.apiClient,
            cls.testdata[TestData.account],
            admin=1
        )

        # Set up connection to make customized API calls
        user = User.create(
            cls.apiClient,
            cls.testdata[TestData.user],
            account=cls.account.name,
            domainid=cls.domain.id
        )

        url = cls.testdata[TestData.url]

        api_url = "http://" + url + ":8080/client/api"
        userkeys = User.registerUserKeys(cls.apiClient, user.id)

        cls.cs_api = SignedAPICall.CloudStack(api_url, userkeys.apikey, userkeys.secretkey)

        primarystorage = cls.testdata[TestData.primaryStorage]

        cls.primary_storage = StoragePool.create(
            cls.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=cls.zone.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        cls.disk_offering = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering],
            custom=True
        )

        # Create VM and volume for tests
        cls.virtual_machine = VirtualMachine.create(
            cls.apiClient,
            cls.testdata[TestData.virtualMachine],
            accountid=cls.account.name,
            zoneid=cls.zone.id,
            serviceofferingid=compute_offering.id,
            templateid=cls.template.id,
            domainid=cls.domain.id,
            startvm=True
        )

        cls._cleanup = [
            compute_offering,
            cls.disk_offering,
            user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cls.virtual_machine.delete(cls.apiClient, True)

            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiClient, self.cleanup)
        except Exception as e:
            logging.debug("Exception in tearDown(self): %s" % e)

    def test_01_upload_and_download_snapshot(self):
        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=self.virtual_machine.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, "There should only be one volume in this list.")

        vm_root_volume = list_volumes_response[0]

        ### Perform tests related to uploading a QCOW2 file to secondary storage and then moving it to managed storage

        volume_name = "Volume-A"
        services = {"format": TestData.file_type, "diskname": volume_name}

        uploaded_volume = Volume.upload(self.apiClient, services, self.zone.id,
                                        account=self.account.name, domainid=self.account.domainid,
                                        url=TestData.volume_url, diskofferingid=self.disk_offering.id)

        self._wait_for_volume_state(uploaded_volume.id, "Uploaded")

        uploaded_volume_id = sf_util.get_cs_volume_db_id(self.dbConnection, uploaded_volume)

        result = self._get_volume_store_ref_row(uploaded_volume_id)

        self.assertEqual(
            len(result),
            1,
            TestUploadDownload.assertText
        )

        install_path = self._get_install_path(result[0][TestData.install_path_index])

        self._verify_uploaded_volume_present(install_path)

        uploaded_volume = self.virtual_machine.attach_volume(
            self.apiClient,
            uploaded_volume
        )

        uploaded_volume = sf_util.check_and_get_cs_volume(self, uploaded_volume.id, volume_name, self)

        sf_account_id = sf_util.get_sf_account_id(self.cs_api, self.account.id, self.primary_storage.id, self, "The SolidFire account ID should be a non-zero integer.")

        sf_volumes = sf_util.get_active_sf_volumes(self.sfe, sf_account_id)

        self.assertNotEqual(
            len(sf_volumes),
            0,
            "The length of the response for the SolidFire-volume query should not be zero."
        )

        sf_volume = sf_util.check_and_get_sf_volume(sf_volumes, uploaded_volume.name, self)

        sf_volume_size = sf_util.get_volume_size_with_hsr(self.cs_api, uploaded_volume, self)

        sf_util.check_size_and_iops(sf_volume, uploaded_volume, sf_volume_size, self)

        sf_vag_id = sf_util.get_vag_id(self.cs_api, self.cluster.id, self.primary_storage.id, self)

        sf_util.check_vag(sf_volume, sf_vag_id, self)

        result = self._get_volume_store_ref_row(uploaded_volume_id)

        self.assertEqual(
            len(result),
            0,
            TestUploadDownload.assertText2
        )

        self._verify_uploaded_volume_not_present(install_path)

        ### Perform tests related to extracting the contents of a volume on managed storage to a QCOW2 file
        ### and downloading the file

        try:
            # for data disk
            Volume.extract(self.apiClient, uploaded_volume.id, self.zone.id, TestData.download_mode)

            raise Exception("The volume extraction (for the data disk) did not fail (as expected).")
        except Exception as e:
            if TestUploadDownload.errorText in str(e):
                pass
            else:
                raise

        vm_root_volume_id = sf_util.get_cs_volume_db_id(self.dbConnection, vm_root_volume)

        try:
            # for root disk
            Volume.extract(self.apiClient, vm_root_volume.id, self.zone.id, TestData.download_mode)

            raise Exception("The volume extraction (for the root disk) did not fail (as expected).")
        except Exception as e:
            if TestUploadDownload.errorText in str(e):
                pass
            else:
                raise

        self.virtual_machine.stop(self.apiClient)

        self._extract_volume_and_verify(uploaded_volume_id, "Unable to locate the extracted file for the data disk (attached)")

        result = self._get_volume_store_ref_row(vm_root_volume_id)

        self.assertEqual(
            len(result),
            0,
            TestUploadDownload.assertText2
        )

        self._extract_volume_and_verify(vm_root_volume_id, "Unable to locate the extracted file for the root disk")

        uploaded_volume = self.virtual_machine.detach_volume(
            self.apiClient,
            uploaded_volume
        )

        self._extract_volume_and_verify(uploaded_volume_id, "Unable to locate the extracted file for the data disk (detached)")

        uploaded_volume = Volume(uploaded_volume.__dict__)

        uploaded_volume.delete(self.apiClient)

        # self.virtual_machine.start(self.apiClient)

    def _verify_uploaded_volume_present(self, install_path, verify_properties_file=True):
        result, result2 = self._get_results(install_path)

        self.assertFalse(result is None or len(result.strip()) == 0, "Unable to find the QCOW2 file")

        if verify_properties_file:
            self.assertFalse(result2 is None or len(result2.strip()) == 0, "Unable to find the " + TestData.properties_file + " file")

    def _verify_uploaded_volume_not_present(self, install_path):
        result, result2 = self._get_results(install_path)

        self.assertTrue(result is None or len(result.strip()) == 0, "QCOW2 file present, but should not be")
        self.assertTrue(result2 is None or len(result2.strip()) == 0, TestData.properties_file + " file present, but should not be")

    def _get_results(self, install_path):
        ssh_connection = sf_util.get_ssh_connection(TestData.secondary_storage_server,
                                                    TestData.secondary_storage_server_username,
                                                    TestData.secondary_storage_server_password)

        stdout = ssh_connection.exec_command("ls -l " + TestData.secondary_storage_server_root +
                                                            install_path + " | grep qcow2")[1]

        result = stdout.read()

        stdout = ssh_connection.exec_command("ls -l " + TestData.secondary_storage_server_root +
                                                            install_path + " | grep " + TestData.properties_file)[1]

        result2 = stdout.read()

        ssh_connection.close()

        return result, result2

    def _get_install_path(self, install_path):
        index = install_path.rfind('/')

        return install_path[:index]

    def _get_volume_store_ref_row(self, volume_id):
        sql_query = "Select * From volume_store_ref Where volume_id = '" + str(volume_id) + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result

    def _extract_volume_and_verify(self, volume_id, error_msg):
        extract_result = Volume.extract(self.apiClient, volume_id, self.zone.id, TestData.download_mode)

        result = self._get_volume_store_ref_row(volume_id)

        self.assertEqual(
            len(result),
            1,
            TestUploadDownload.assertText
        )

        install_path = self._get_install_path(result[0][TestData.install_path_index])

        self._verify_uploaded_volume_present(install_path, False)

        url_response = urllib.request.urlopen(extract_result.url)

        if url_response.code != 200:
            raise Exception(error_msg)

        self._wait_for_removal_of_extracted_volume(volume_id, extract_result.url)

    def _wait_for_removal_of_extracted_volume(self, volume_id, extract_result_url):
        retry_interval = 60
        num_tries = 10

        wait_result, return_val = wait_until(retry_interval, num_tries, self._check_removal_of_extracted_volume_state, volume_id, extract_result_url)

        if not wait_result:
            raise Exception(return_val)

    def _check_removal_of_extracted_volume_state(self, volume_id, extract_result_url):
        result = self._get_volume_store_ref_row(volume_id)

        if len(result) == 0:
            try:
                urllib.request.urlopen(extract_result_url)
            except Exception as e:
                if "404" in str(e):
                    return True, ""

        return False, "The extracted volume has not been removed."

    def _wait_for_volume_state(self, volume_id, volume_state):
        retry_interval = 30
        num_tries = 10

        wait_result, return_val = wait_until(retry_interval, num_tries, TestUploadDownload._check_volume_state, self.apiClient, volume_id, volume_state)

        if not wait_result:
            raise Exception(return_val)

    @staticmethod
    def _check_volume_state(api_client, volume_id, volume_state):
        volume = list_volumes(
            api_client,
            id=volume_id,
            listall=True
        )[0]

        if str(volume.state).lower() == volume_state.lower():
            return True, ""

        return False, "The volume is not in the '" + volume_state + "' state. State = " + str(volume.state)
