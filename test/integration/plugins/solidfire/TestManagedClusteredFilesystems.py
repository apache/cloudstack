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
import XenAPI

from solidfire.factory import ElementFactory

from util import sf_util

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, Cluster, ServiceOffering, Snapshot, StoragePool, User, VirtualMachine, Volume

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_hosts, list_volumes

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

# Prerequisites:
#  Only one zone
#  Only one pod
#  Two clusters
#
# Running the tests:
#  If using XenServer, verify the "xen_server_hostname" variable is correct.
#  Set the Global Setting "max.number.managed.clustered.file.systems" equal to 2.
#
# Note:
#  Verify that TestData.clusterId and TestData.clusterId2 are set properly.


class TestData():
    # constants
    account = "account"
    allocationstate = "allocationstate"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    clusterId2 = "clusterId2"
    computeOffering = "computeoffering"
    domainId = "domainId"
    email = "email"
    firstname = "firstname"
    hypervisor = "hypervisor"
    lastname = "lastname"
    mvip = "mvip"
    name = "name"
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
    xenServer = "xenserver"
    zoneId = "zoneId"

    hypervisor_type = xenServer
    xen_server_hostname = "XenServer-6.5-1"

    def __init__(self):
        self.testdata = {
            TestData.solidFire: {
                TestData.mvip: "10.117.78.225",
                TestData.username: "admin",
                TestData.password: "admin",
                TestData.port: 443,
                TestData.url: "https://10.117.78.225:443"
            },
            TestData.xenServer: {
                TestData.username: "root",
                TestData.password: "solidfire"
            },
            TestData.account: {
                TestData.email: "test@test.com",
                TestData.firstname: "John",
                TestData.lastname: "Doe",
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                TestData.email: "user@test.com",
                TestData.firstname: "Jane",
                TestData.lastname: "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.primaryStorage: {
                TestData.name: "SolidFire-%d" % random.randint(0, 100),
                TestData.scope: "ZONE",
                TestData.url: "MVIP=10.117.78.225;SVIP=10.117.94.225;" +
                       "clusterAdminUsername=admin;clusterAdminPassword=admin;" +
                       "clusterDefaultMinIops=10000;clusterDefaultMaxIops=15000;" +
                       "clusterDefaultBurstIopsPercentOfMaxIops=1.5;",
                TestData.provider: "SolidFire",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 4500000,
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "Any"
            },
            TestData.computeOffering: {
                TestData.name: "SF_CO_1",
                "displaytext": "SF_CO_1 (Min IOPS = 300; Max IOPS = 600)",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "300",
                "maxiops": "600",
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag
            },
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.clusterId2: 6,
            TestData.domainId: 1,
            TestData.url: "10.117.40.114"
        }


class TestManagedClusteredFilesystems(cloudstackTestCase):
    _should_only_be_one_volume_in_list_err_msg = "There should only be one volume in this list."
    _volume_should_have_failed_to_attach_to_vm = "The volume should have failed to attach to the VM."

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestManagedClusteredFilesystems, cls).getClsTestClient()

        cls.apiClient = testclient.getApiClient()
        cls.configData = testclient.getParsedTestDataConfig()
        cls.dbConnection = testclient.getDbConnection()

        cls.testdata = TestData().testdata

        sf_util.set_supports_resign(True, cls.dbConnection)

        cls._connect_to_hypervisor()

        # Set up SolidFire connection
        solidfire = cls.testdata[TestData.solidFire]

        cls.sfe = ElementFactory.create(solidfire[TestData.mvip], solidfire[TestData.username], solidfire[TestData.password])

        # Get Resources from Cloud Infrastructure
        cls.zone = get_zone(cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        cls.template = get_template(cls.apiClient, cls.zone.id, hypervisor=TestData.hypervisor_type)
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])

        # Create test account
        cls.account = Account.create(
            cls.apiClient,
            cls.testdata["account"],
            admin=1
        )

        # Set up connection to make customized API calls
        cls.user = User.create(
            cls.apiClient,
            cls.testdata["user"],
            account=cls.account.name,
            domainid=cls.domain.id
        )

        url = cls.testdata[TestData.url]

        api_url = "http://" + url + ":8080/client/api"
        userkeys = User.registerUserKeys(cls.apiClient, cls.user.id)

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

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        # Resources that are to be destroyed
        cls._cleanup = [
            cls.compute_offering,
            cls.user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)

            cls.primary_storage.delete(cls.apiClient)

            sf_util.purge_solidfire_volumes(cls.sfe)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)

#   Only two 'permanent' SRs per cluster
#
#   Disable the second cluster
#
#   Create VM
#   Create VM
#   Create VM (should fail)
#   Take snapshot of first root disk
#   Create a volume from this snapshot
#   Attach new volume to second VM (should fail)
#
#   Enable the second cluster
#
#   Attach new volume to second VM (should fail)
#   Create VM (should end up in new cluster)
#   Delete first VM (this should free up one SR in the first cluster)
#   Attach new volume to second VM
#   Detach new volume from second VM
#   Attach new volume to second VM
#   Create a volume from the snapshot
#   Attach this new volume to the second VM (should fail)
#   Attach this new volume to the first VM in the new cluster
    def test_managed_clustered_filesystems_limit(self):
        args = { "id": self.testdata[TestData.clusterId2], TestData.allocationstate: "Disabled" }

        Cluster.update(self.apiClient, **args)

        virtual_machine_names = {
            "name": "TestVM1",
            "displayname": "Test VM 1"
        }

        virtual_machine_1 = self._create_vm(virtual_machine_names)

        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine_1.id,
            listall=True
        )

        sf_util.check_list(list_volumes_response, 1, self, TestManagedClusteredFilesystems._should_only_be_one_volume_in_list_err_msg)

        vm_1_root_volume = list_volumes_response[0]

        virtual_machine_names = {
            "name": "TestVM2",
            "displayname": "Test VM 2"
        }

        virtual_machine_2 = self._create_vm(virtual_machine_names)

        virtual_machine_names = {
            "name": "TestVM3",
            "displayname": "Test VM 3"
        }

        class VMStartedException(Exception):
            def __init__(self, *args, **kwargs):
                Exception.__init__(self, *args, **kwargs)

        try:
            # The VM should fail to be created as there should be an insufficient number of clustered filesystems
            # remaining in the compute cluster.
            self._create_vm(virtual_machine_names)

            raise VMStartedException("The VM should have failed to start.")
        except VMStartedException:
            raise
        except Exception:
            pass

        vol_snap = Snapshot.create(
            self.apiClient,
            volume_id=vm_1_root_volume.id
        )

        services = {"diskname": "Vol-1", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot_1 = Volume.create_from_snapshot(self.apiClient, vol_snap.id, services, account=self.account.name, domainid=self.domain.id)

        class VolumeAttachedException(Exception):
            def __init__(self, *args, **kwargs):
                Exception.__init__(self, *args, **kwargs)

        try:
            # The volume should fail to be attached as there should be an insufficient number of clustered filesystems
            # remaining in the compute cluster.
            virtual_machine_2.attach_volume(
                self.apiClient,
                volume_created_from_snapshot_1
            )

            raise VolumeAttachedException(TestManagedClusteredFilesystems._volume_should_have_failed_to_attach_to_vm)
        except VolumeAttachedException:
            raise
        except Exception:
            pass

        args = { "id": self.testdata[TestData.clusterId2], TestData.allocationstate: "Enabled" }

        Cluster.update(self.apiClient, **args)

        try:
            # The volume should fail to be attached as there should be an insufficient number of clustered filesystems
            # remaining in the compute cluster.
            virtual_machine_2.attach_volume(
                self.apiClient,
                volume_created_from_snapshot_1
            )

            raise VolumeAttachedException(TestManagedClusteredFilesystems._volume_should_have_failed_to_attach_to_vm)
        except VolumeAttachedException:
            raise
        except Exception:
            pass

        virtual_machine_names = {
            "name": "TestVMA",
            "displayname": "Test VM A"
        }

        virtual_machine_a = self._create_vm(virtual_machine_names)

        host_for_vm_1 = list_hosts(self.apiClient, id=virtual_machine_1.hostid)[0]
        host_for_vm_a = list_hosts(self.apiClient, id=virtual_machine_a.hostid)[0]

        self.assertTrue(
            host_for_vm_1.clusterid != host_for_vm_a.clusterid,
            "VMs 1 and VM a should be in different clusters."
        )

        virtual_machine_1.delete(self.apiClient, True)

        volume_created_from_snapshot_1 = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_1
        )

        virtual_machine_2.detach_volume(self.apiClient, volume_created_from_snapshot_1)

        volume_created_from_snapshot_1 = virtual_machine_2.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_1
        )

        services = {"diskname": "Vol-2", "zoneid": self.testdata[TestData.zoneId], "ispublic": True}

        volume_created_from_snapshot_2 = Volume.create_from_snapshot(self.apiClient, vol_snap.id, services, account=self.account.name, domainid=self.domain.id)

        try:
            # The volume should fail to be attached as there should be an insufficient number of clustered filesystems
            # remaining in the compute cluster.
            virtual_machine_2.attach_volume(
                self.apiClient,
                volume_created_from_snapshot_2
            )

            raise VolumeAttachedException(TestManagedClusteredFilesystems._volume_should_have_failed_to_attach_to_vm)
        except VolumeAttachedException:
            raise
        except Exception:
            pass

        virtual_machine_a.attach_volume(
            self.apiClient,
            volume_created_from_snapshot_2
        )

    def _create_vm(self, virtual_machine_names):
        return VirtualMachine.create(
            self.apiClient,
            virtual_machine_names,
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

    @classmethod
    def _connect_to_hypervisor(cls):
        host_ip = "https://" + \
              list_hosts(cls.apiClient, clusterid=cls.testdata[TestData.clusterId], name=TestData.xen_server_hostname)[0].ipaddress

        cls.xen_session = XenAPI.Session(host_ip)

        xen_server = cls.testdata[TestData.xenServer]

        cls.xen_session.xenapi.login_with_password(xen_server[TestData.username], xen_server[TestData.password])
