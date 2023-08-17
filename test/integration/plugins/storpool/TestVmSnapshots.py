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

# Import Local Modules
import random
import time

from marvin.cloudstackAPI import (listTemplates)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             VmSnapshot,
                             User,
                             Volume,
                             SecurityGroup,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_clusters,
                               list_snapshots,
                               list_virtual_machines,
                               list_configurations,
                               list_disk_offering,
                               list_accounts,
                               list_storage_pools,
                               list_service_offering,
                               list_zones
                               )
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual, get_hypervisor_type
from nose.plugins.attrib import attr
import uuid
from sp_util import (TestData, StorPoolHelper)

class TestVmSnapshot(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        super(TestVmSnapshot, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            cls.cleanUpCloudStack()
            raise

    @classmethod
    def setUpCloudStack(cls):
        config = cls.getClsConfig()
        StorPoolHelper.logger = cls

        zone = config.zones[0]
        assert zone is not None

        testClient = super(TestVmSnapshot, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False

        # Setup test data
        td = TestData()
        cls.testdata = td.testdata
        cls.helper = StorPoolHelper()


        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = list_zones(cls.apiclient, name=zone.name)[0]
        cls.debug(cls.zone)
        cls.debug(list_zones(cls.apiclient, name=zone.name))
        assert cls.zone is not None

        cls.cluster = list_clusters(cls.apiclient)[0]
        cls.hypervisor = get_hypervisor_type(cls.apiclient)

        #The version of CentOS has to be supported
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            account = "system"
        )


        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.template = template

        cls.account = cls.helper.create_account(
                            cls.apiclient,
                            cls.services["account"],
                            accounttype = 1,
                            domainid=cls.domain.id,
                            roleid = 1
                            )
        cls._cleanup.append(cls.account)

        securitygroup = SecurityGroup.list(cls.apiclient, account = cls.account.name, domainid= cls.account.domainid)[0]
        cls.helper.set_securityGroups(cls.apiclient, account = cls.account.name, domainid= cls.account.domainid, id = securitygroup.id)

        primarystorage = cls.testdata[TestData.primaryStorage]

        serviceOffering = cls.testdata[TestData.serviceOffering]
        storage_pool = list_storage_pools(
            cls.apiclient,
            name = primarystorage.get("name")
            )
        cls.primary_storage = storage_pool[0]

        disk_offering = list_disk_offering(
            cls.apiclient,
            name="ssd"
            )

        assert disk_offering is not None


        service_offering_only = list_service_offering(
            cls.apiclient,
            name="ssd"
            )
        if service_offering_only is not None:
            cls.service_offering_only = service_offering_only[0]
        else:
            cls.service_offering_only = ServiceOffering.create(
                cls.apiclient,
                serviceOffering)
        assert cls.service_offering_only is not None

        cls.disk_offering = disk_offering[0]

        # Create 1 data volume_1
        cls.volume = Volume.create(
            cls.apiclient,
            cls.testdata[TestData.volume_1],
            account=cls.account.name,
            domainid=cls.domain.id,
            zoneid=cls.zone.id,
            diskofferingid=cls.disk_offering.id,
            size=10
        )

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering_only.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.random_data_0 = random_gen(size=100)
        cls.test_dir = "/tmp"
        cls.random_data = "random.data"
        return

    @classmethod
    def tearDownClass(cls):
        cls.cleanUpCloudStack()

    @classmethod
    def cleanUpCloudStack(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_create_vm_snapshots(self):
        """Test to create VM snapshots
        """
        volume_attached = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume
            )

        self.assertEqual(volume_attached.id, self.volume.id, "Is not the same volume ")
        try:
            # Login to VM and write data to file system
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            cmds = [
                "echo %s > %s/%s" %
                (self.random_data_0, self.test_dir, self.random_data),
                "sync",
                "sleep 1",
                "sync",
                "sleep 1",
                "cat %s/%s" %
                (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)


        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)
        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data has be write into temp file!"
        )

        time.sleep(30)
        MemorySnapshot = False
        vm_snapshot = VmSnapshot.create(
            self.apiclient,
            self.virtual_machine.id,
            MemorySnapshot,
            "TestSnapshot",
            "Display Text"
        )
        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_revert_vm_snapshots(self):
        """Test to revert VM snapshots
        """

        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            cmds = [
                "rm -rf %s/%s" % (self.test_dir, self.random_data),
                "ls %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)

        if str(result[0]).index("No such file or directory") == -1:
            self.fail("Check the random data has be delete from temp file!")

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )

        self.assertEqual(
            list_snapshot_response[0].state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        self.virtual_machine.stop(self.apiclient, forced=True)

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id
            )

        self.virtual_machine.start(self.apiclient)

        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_delete_vm_snapshots(self):
        """Test to delete vm snapshots
        """

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )
        VmSnapshot.deleteVMSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            #vmid=self.virtual_machine.id,
            virtualmachineid=self.virtual_machine.id,
            listall=False)
        self.debug('list_snapshot_response -------------------- %s' % list_snapshot_response)

        self.assertIsNone(list_snapshot_response, "snapshot is already deleted")
