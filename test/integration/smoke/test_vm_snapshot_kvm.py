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
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from marvin.lib.base import (Account,
                             Configurations,
                             ServiceOffering,
                             StoragePool,
                             Template,
                             VirtualMachine,
                             VmSnapshot,
                             Host)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_snapshots,
                               list_virtual_machines,
                               list_configurations)
from marvin.cloudstackAPI import (listTemplates)
import time
import unittest

class TestVmSnapshot(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVmSnapshot, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() != "kvm":
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        hosts = Host.list(
            cls.apiclient,
            zoneid=cls.zone.id,
            type='Routing',
            hypervisor='KVM')

        pools = StoragePool.list(
            cls.apiclient,
            zoneid=cls.zone.id)

        for pool in pools:
            if pool.type == "NetworkFilesystem" or pool.type == "Filesystem":
                raise unittest.SkipTest("Storage-based snapshots functionality is not supported for NFS/Local primary storage")

        for host in hosts:
            if host.details['Host.OS'] in ['CentOS']:
                raise unittest.SkipTest("The standard `qemu-kvm` which is the default for CentOS does not support the new functionality. It has to be installed `qemu-kvm-ev`")

        Configurations.update(cls.apiclient,
            name = "kvm.vmstoragesnapshot.enabled",
            value = "true")
        #The version of CentOS has to be supported
        templ = {
            "name": "CentOS8",
            "displaytext": "CentOS 8",
            "format": "QCOW2",
            "url": "http://download.cloudstack.org/releases/4.14/default-tmpl-centos8.0.qcow2.bz2",
            "ispublic": "True",
            "isextractable": "True",
            "hypervisor": cls.hypervisor,
            "zoneid": cls.zone.id,
            "ostype": "CentOS 8",
            "directdownload": True,
        }

        template = Template.register(cls.apiclient, templ, zoneid=cls.zone.id, hypervisor=cls.hypervisor)
        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        service_offerings_nfs = {
            "name": "nfs",
                "displaytext": "nfs",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
            }

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            service_offerings_nfs,
        )

        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services,
            zoneid=cls.zone.id,
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.zone.networktype,
            hypervisor=cls.hypervisor,
            rootdisksize=20,
        )
        cls.random_data_0 = random_gen(size=100)
        cls.test_dir = "/tmp"
        cls.random_data = "random.data"
        return

    @classmethod
    def tearDownClass(cls):
        try:
            Configurations.update(cls.apiclient,
            name = "kvm.vmstoragesnapshot.enabled",
            value = "false")
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
        try:
            # Login to VM and write data to file system
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "echo %s > %s/%s" %
                (self.random_data_0, self.test_dir, self.random_data),
                "cat %s/%s" %
                (self.test_dir, self.random_data)]

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
            ssh_client = self.virtual_machine.get_ssh_client()

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

        self.virtual_machine.stop(self.apiclient)

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

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
            virtualmachineid=self.virtual_machine.id,
            listall=False)
        self.debug('list_snapshot_response -------------------- %s' % list_snapshot_response)

        self.assertIsNone(list_snapshot_response, "snapshot is already deleted")
