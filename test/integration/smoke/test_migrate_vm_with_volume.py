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
""" BVT tests for Virtual Machine Life Cycle
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (recoverVirtualMachine,
                                  destroyVirtualMachine,
                                  attachIso,
                                  detachIso,
                                  provisionCertificate,
                                  updateConfiguration,
                                  migrateVirtualMachine,
                                  migrateVirtualMachineWithVolume,
                                  listNics,
                                  listVolumes)
from marvin.lib.utils import *

from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Iso,
                             Router,
                             Configurations,
                             StoragePool,
                             Volume,
                             DiskOffering,
                             NetworkOffering,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template,
                               get_test_ovf_templates,
                               list_hosts,
                               get_vm_vapp_configs)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf
# Import System modules
import time
import json
from operator import itemgetter

_multiprocess_shared_ = True


class TestMigrateVMwithVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestMigrateVMwithVolume, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        cls.management_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        super(TestMigrateVMwithVolume,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.hypervisor.lower() not in ["vmware"]:
            self.skipTest("VM Migration with Volumes is not supported on other than VMware")

        self.hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            hypervisor='VMware')

        if len(self.hosts) < 2:
            self.skipTest("Requires at least two hosts for performing migration related tests")

    def tearDown(self):
        super(TestMigrateVMwithVolume,self).tearDown()

    def get_target_host(self, virtualmachineid):
        target_hosts = Host.listForMigration(self.apiclient,
                                             virtualmachineid=virtualmachineid)
        if len(target_hosts) < 1:
            self.skipTest("No target hosts found")

        return target_hosts[0]

    def get_target_pool(self, volid):
        target_pools = StoragePool.listForMigration(self.apiclient, id=volid)

        if len(target_pools) < 1:
            self.skipTest("Not enough storage pools found")

        return target_pools[0]

    def get_vm_volumes(self, id):
        return Volume.list(self.apiclient, virtualmachineid=id, listall=True)

    def deploy_vm(self):
        return VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"])

    def migrate_vm_to_pool(self, target_pool, id):

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()

        cmd.storageid = target_pool.id
        cmd.virtualmachineid = id

        return self.apiclient.migrateVirtualMachine(cmd)

    def create_volume(self):
        small_disk_offering = DiskOffering.list(self.apiclient, name='Small')[0]

        return Volume.create(
            self.apiclient,
            self.services,
            account=self.account.name,
            diskofferingid=small_disk_offering.id,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

    """
    BVT for Vmware Offline and Live VM and Volume Migration
    """

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_01_offline_migrate_VM_and_root_volume(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM
        # 2. Stops the VM
        # 3. Finds suitable storage pool for root volume
        # 4. Migrate the VM to new storage pool and assert migration successful

        vm = self.deploy_vm()

        root_volume = self.get_vm_volumes(vm.id)[0]

        target_pool = self.get_target_pool(root_volume.id)

        vm.stop(self.apiclient)

        self.migrate_vm_to_pool(target_pool, vm.id)

        root_volume = self.get_vm_volumes(vm.id)[0]
        self.assertEqual(root_volume.storageid, target_pool.id, "Pool ID was not as expected")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_02_offline_migrate_VM_with_two_data_disks(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM and attaches 2 data disks
        # 2. Stops the VM
        # 3. Finds suitable storage pool for volumes
        # 4. Migrate the VM to new storage pool and assert migration successful

        vm = self.deploy_vm()

        volume1 = self.create_volume()
        volume2 = self.create_volume()

        vm.attach_volume(self.apiclient, volume1)
        vm.attach_volume(self.apiclient, volume2)

        root_volume = self.get_vm_volumes(vm.id)[0]

        target_pool = self.get_target_pool(root_volume.id)

        vm.stop(self.apiclient)

        self.migrate_vm_to_pool(target_pool, vm.id)

        volume1 = Volume.list(self.apiclient, id=volume1.id)[0]
        volume2 = Volume.list(self.apiclient, id=volume2.id)[0]
        root_volume = self.get_vm_volumes(vm.id)[0]

        self.assertEqual(root_volume.storageid, target_pool.id, "Pool ID was not as expected")
        self.assertEqual(volume1.storageid, target_pool.id, "Pool ID was not as expected")
        self.assertEqual(volume2.storageid, target_pool.id, "Pool ID was not as expected")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_03_live_migrate_VM_with_two_data_disks(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM and attaches 2 data disks
        # 2. Finds suitable host for migration
        # 3. Finds suitable storage pool for volumes
        # 4. Migrate the VM to new host and storage pool and assert migration successful

        vm = self.deploy_vm()

        root_volume = self.get_vm_volumes(vm.id)[0]
        volume1 = self.create_volume()
        volume2 = self.create_volume()
        vm.attach_volume(self.apiclient, volume1)
        vm.attach_volume(self.apiclient, volume2)

        target_host = self.get_target_host(vm.id)
        target_pool = self.get_target_pool(root_volume.id)
        volume1.target_pool = self.get_target_pool(volume1.id)
        volume2.target_pool = self.get_target_pool(volume2.id)

        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()
        cmd.migrateto = [{"volume": str(root_volume.id), "pool": str(target_pool.id)},
                         {"volume": str(volume1.id), "pool": str(volume1.target_pool.id)},
                         {"volume": str(volume2.id), "pool": str(volume2.target_pool.id)}]
        cmd.virtualmachineid = vm.id
        cmd.hostid = target_host.id

        response = self.apiclient.migrateVirtualMachineWithVolume(cmd)

        self.assertEqual(Volume.list(self.apiclient, id=root_volume.id)[0].storageid,
                         target_pool.id,
                         "Pool ID not as expected")

        self.assertEqual(Volume.list(self.apiclient, id=volume1.id)[0].storageid,
                         volume1.target_pool.id,
                         "Pool ID not as expected")

        self.assertEqual(Volume.list(self.apiclient, id=volume2.id)[0].storageid,
                         volume2.target_pool.id,
                         "Pool ID not as expected")

        self.assertEqual(response.hostid,
                         target_host.id,
                         "HostID not as expected")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_04_migrate_detached_volume(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM and attaches 1 data disk
        # 2. Detaches the Disk
        # 3. Finds suitable storage pool for the Disk
        # 4. Migrate the storage pool and assert migration successful

        vm = self.deploy_vm()

        volume1 = self.create_volume()

        vm.attach_volume(self.apiclient, volume1)
        vm.detach_volume(self.apiclient, volume1)

        target_pool = self.get_target_pool(volume1.id)

        Volume.migrate(self.apiclient, storageid=target_pool.id, volumeid=volume1.id)

        vol = Volume.list(self.apiclient, id=volume1.id)[0]

        self.assertEqual(vol.storageid, target_pool.id, "Storage pool was not the same as expected")
