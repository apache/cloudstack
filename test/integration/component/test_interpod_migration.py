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
from marvin.cloudstackAPI import (attachVolume,
                                  detachVolume,
                                  deleteVolume,
                                  attachIso,
                                  detachIso,
                                  deleteIso,
                                  startVirtualMachine,
                                  stopVirtualMachine,
                                  migrateVirtualMachineWithVolume)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Host,
                             Pod,
                             StoragePool,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             Iso,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
# Import System modules
import time

_multiprocess_shared_ = True


class TestVMMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVMMigration, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.cleanup = []
        cls.hypervisorNotSupported = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() not in ['vmware']:
            cls.hypervisorNotSupported = True

        if cls.hypervisorNotSupported == False:
            cls.pods = Pod.list(cls.apiclient, zoneid=cls.zone.id, listall=True)
            if len(cls.pods) < 2:
                assert False, "Not enough pods found: %d" % len(cls.pods)
            cls.computeOfferingStorageTags = None
            cls.diskOfferingStorageTags = None

            for pod in cls.pods:
                podStoragePools = StoragePool.list(
                    cls.apiclient,
                    scope='CLUSTER',
                    podid=pod.id)
                if len(podStoragePools) < 1:
                    assert False, "Not enough CLUSTER scope storage pools found for pod: %s" % pod.id
                taggedPool = []
                for pool in podStoragePools:
                    if pool.tags != None and len(pool.tags) > 0:
                        taggedPool.append(pool)
                if len(taggedPool) < 2:
                    assert False, "No CLUSTER scope, tagged storage pools found for pod: %s" % pod.id
                if cls.computeOfferingStorageTags == None:
                    cls.computeOfferingStorageTags = taggedPool[0].tags
                if cls.diskOfferingStorageTags == None:
                    cls.diskOfferingStorageTags = taggedPool[1].tags

            template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.services["ostype"])
            if template == FAILED:
                assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

            # Set Zones and disk offerings
            cls.services["small"]["zoneid"] = cls.zone.id
            cls.services["small"]["template"] = template.id

            cls.services["iso"]["zoneid"] = cls.zone.id

            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id)
            cls.debug(cls.account.id)

            compute_offering_service = cls.services["service_offerings"]["tiny"].copy()
            compute_offering_service["tags"] = cls.computeOfferingStorageTags
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                compute_offering_service)
            disk_offering_service = cls.services["disk_offering"].copy()
            disk_offering_service["disksize"] = 1
            cls.untagged_disk_offering = DiskOffering.create(
                cls.apiclient,
                disk_offering_service)
            disk_offering_service["tags"] = cls.diskOfferingStorageTags
            cls.tagged_disk_offering = DiskOffering.create(
                cls.apiclient,
                disk_offering_service)
            cls.hostId = None
            host = cls.getOldestHost(cls.pods[0].id, cls.pods[1].id)
            if host != None:
                cls.hostId = host.id

            cls.cleanup = [
                cls.service_offering,
                cls.untagged_disk_offering,
                cls.tagged_disk_offering,
                cls.account
            ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.virtual_machine = None
        if self.hypervisorNotSupported == False:
            self.virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["small"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.services['mode'],
                hostid=self.hostId
            )

        self.cleanup = []

    def tearDown(self):
        try:
            if self.virtual_machine != None:
                self.virtual_machine.delete(self.apiclient, expunge=True)
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @classmethod
    def getOldestHost(cls, pod1_id, pod2_id):
        selectedHost = None
        hosts = Host.list(cls.apiclient, type='Routing', podid=pod1_id)
        morehosts = Host.list(cls.apiclient, type='Routing', podid=pod2_id)
        if isinstance(morehosts, list) and len(morehosts)>0:
            if isinstance(hosts, list) and len(hosts)>0:
                hosts.extend(morehosts)
        if isinstance(hosts, list) and len(hosts)>0:
            selectedHost = hosts[0]
            # Very basic way to get lowest version host
            for host in hosts:
                if int(host.hypervisorversion.replace(".",  "")) < int(selectedHost.hypervisorversion.replace(".",  "")):
                    selectedHost = host
        return selectedHost

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_migrate_running_vm(self):
        """Test Running Virtual Machine Migration Without DATA disk or ISO
        """
        # Validate the following:
        # 1. Start VM if not running
        # 2. Migrate VM to a different pod multiple times
        vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id)
        if vmResponse.state != 'Running':
            self.startVm(vmResponse.id)
        migrationCount = 1
        while migrationCount > 0:
            vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id, 'Running')
            hostId = self.getDifferentPodHost(vmResponse.id, vmResponse.hostid).id
            self.debug("#%d migration, current host ID: %s, new host ID: %s" % ((2-migrationCount), vmResponse.hostid, hostId))
            self.migrateVmWithVolume(vmResponse.id, hostId)
            migrationCount = migrationCount - 1
            if migrationCount > 0:
                time.sleep(self.services["sleep"])
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_migrate_running_vm_with_disk_and_iso(self):
        """Test Running Virtual Machine Migration With DATA disks or ISO
        """
        # Validate the following:
        # 1. Start VM if not running
        # 2. Add disks and ISO to the VM
        # 3. Migrate VM to a different pod multiple times
        # 4. Remove disks and ISO from the VM
        vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id)
        if vmResponse.state != 'Running':
            self.startVm(vmResponse.id)
        vol1 = self.addVolumeToVm(vmResponse.id, self.tagged_disk_offering)
        vol2 = self.addVolumeToVm(vmResponse.id, self.untagged_disk_offering)
        # self.addIsoToVm(vmResponse.id)
        migrationCount = 1
        while migrationCount > 0:
            vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id, 'Running')
            hostId = self.getDifferentPodHost(vmResponse.id, vmResponse.hostid).id
            self.debug("#%d migration, current host ID: %s, new host ID: %s" % ((2-migrationCount), vmResponse.hostid, hostId))
            self.migrateVmWithVolume(vmResponse.id, hostId)
            migrationCount = migrationCount - 1
            if migrationCount > 0:
                time.sleep(self.services["sleep"])
        self.removeVolumeFromVm(vol1.id)
        self.removeVolumeFromVm(vol2.id)
        # self.removeIsoFromVm(vmResponse.id, vmResponse.isoid)
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_migrate_stopped_vm(self):
        """Test Stopped Virtual Machine Migration Without DATA disk or ISO
        """
        # Validate the following:
        # 1. Stop VM if not already stopped
        # 2. Migrate VM to a different pod multiple times with volume to pool mapping
        vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id)
        if vmResponse.state != 'Stopped':
            self.stopVm(vmResponse.id)
        migrationCount = 3
        while migrationCount > 0:
            vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id, 'Stopped')
            migrateTo = self.getDifferentPodVolumeStoragePoolMapping(vmResponse.id)
            self.debug("#%d migration, mapping: %s" % ((4-migrationCount), migrateTo))
            self.migrateVmWithVolume(vmResponse.id, None, migrateTo)
            migrationCount = migrationCount - 1
            if migrationCount > 0:
                time.sleep(self.services["sleep"])
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_migrate_stopped_vm_with_disk_and_iso(self):
        """Test Stopped Virtual Machine Migration With DATA disk or ISO
        """
        # Validate the following:
        # 1. Start VM if not running
        # 2. Add disks and ISO to the VM
        # 3. Stop the VM
        # 4. Migrate VM to a different pod multiple times with volume to pool mapping
        # 5. Start VM and remove disks and ISO from the VM
        vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id)
        if vmResponse.state != 'Running':
            self.startVm(vmResponse.id)
        vol1 = self.addVolumeToVm(vmResponse.id, self.tagged_disk_offering)
        vol2 = self.addVolumeToVm(vmResponse.id, self.untagged_disk_offering)
        # self.addIsoToVm(vmResponse.id)
        self.stopVm(vmResponse.id)
        migrationCount = 3
        while migrationCount > 0:
            vmResponse = self.getVmVerifiedResponse(self.virtual_machine.id, 'Stopped')
            migrateTo = self.getDifferentPodVolumeStoragePoolMapping(vmResponse.id)
            self.debug("#%d migration, mapping: %s" % ((4-migrationCount), migrateTo))
            self.migrateVmWithVolume(vmResponse.id, None, migrateTo)
            migrationCount = migrationCount - 1
            if migrationCount > 0:
                time.sleep(self.services["sleep"])
        self.removeVolumeFromVm(vol1.id)
        self.removeVolumeFromVm(vol2.id)
        # self.removeIsoFromVm(vmResponse.id, vmResponse.isoid)
        return

    def startVm(self, vm_id):
        startVirtualMachineCmd = startVirtualMachine.startVirtualMachineCmd()
        startVirtualMachineCmd.id = vm_id
        self.apiclient.startVirtualMachine(startVirtualMachineCmd)

    def stopVm(self, vm_id):
        stopVirtualMachineCmd = stopVirtualMachine.stopVirtualMachineCmd()
        stopVirtualMachineCmd.id = vm_id
        self.apiclient.stopVirtualMachine(stopVirtualMachineCmd)

    def addVolumeToVm(self, vm_id, disk_offering):
        volume = Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=disk_offering.id,
            account=self.account.name,
            domainid=self.account.domainid)
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume.id
        cmd.virtualmachineid = vm_id
        attachedVolume = self.apiclient.attachVolume(cmd)
        return attachedVolume

    def removeVolumeFromVm(self, volume_id):
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = volume_id
        detachedVolume = self.apiclient.detachVolume(cmd)
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = volume_id
        self.apiclient.deleteVolume(cmd)
        return

    def addIsoToVm(self, vm_id):
        iso = Iso.create(
           self.apiclient,
           self.services["iso"],
           account=self.account.name,
           domainid=self.account.domainid)
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = vm_id
        attachedIso = self.apiclient.attachIso(cmd)
        return

    def removeIsoFromVm(self, vm_id, iso_id):
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = vm_id
        self.apiclient.detachIso(cmd)
        cmd = deleteIso.deleteIsoCmd()
        cmd.id = iso_id
        self.apiclient.deleteIso(cmd)
        return

    def getVmVerifiedResponse(self, vm_id, state=None):
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id)
        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s" \
            % self.virtual_machine.id)
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list")
        self.assertNotEqual(
            len(list_vm_response),
            0,
            "Check VM available in List Virtual Machines")
        vmResponse = list_vm_response[0]
        if state != None:
            self.assertEqual(
                vmResponse.state,
                state,
                "VM not in state: %s" % state)
        return vmResponse

    def getDifferentPodHost(self, vm_id, host_id):
        host = None
        currentHost = Host.list(self.apiclient, id=host_id)
        self.assertEqual(
            isinstance(currentHost, list),
            True,
            "Check host list response returns a valid list")
        self.assertNotEqual(
            len(currentHost),
            0,
            "Check current host for VM  ID: %s available in List Hosts" % vm_id)
        currentHost = currentHost[0]
        hosts = Host.listForMigration(self.apiclient, virtualmachineid=vm_id)
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check host list response returns a valid list")
        self.assertNotEqual(
            len(hosts),
            0,
            "Hosts suitable for migration for VM ID: %s not found" % vm_id)
        for hostForMigration in hosts:
            if hostForMigration.podid != currentHost.podid:
                host = hostForMigration
                break
        self.assertNotEqual(
            host,
            None,
            "Host suitable for migration for VM ID: %s in a different pod not found" % vm_id)
        return host

    def getPodStoragePoolWithTags(self, pod_id, tags=None):
        pool = None
        storage_pools = StoragePool.list(
            self.apiclient,
            podid=pod_id,
            listall=True)
        if isinstance(storage_pools, list) and len(storage_pools) > 0:
            if tags != None:
                for storage_pool in storage_pools:
                    if storage_pool.tags == tags:
                        pool = storage_pool
                        break
            else:
                pool = storage_pool[0]
        return pool

    def getDifferentPodVolumeStoragePoolMapping(self, vm_id):
        rootVolume = Volume.list(self.apiclient, virtualmachineid=vm_id, listall=True, type='ROOT')
        self.assertEqual(
            isinstance(rootVolume, list),
            True,
            "Check VM volumes list response returns a valid list")
        self.assertNotEqual(
            len(rootVolume),
            0,
            "Check VM ROOT volume available in List Volumes")
        rootVolume = rootVolume[0]
        volumeStoragePool = StoragePool.list(
            self.apiclient,
            id=rootVolume.storageid)
        self.assertEqual(
            isinstance(volumeStoragePool, list),
            True,
            "Check VM ROOT Volume storage list response returns a valid list")
        self.assertNotEqual(
            len(volumeStoragePool),
            0,
            "Check VM ROOT Volume storage available in List Storage Pools")
        volumeStoragePool = volumeStoragePool[0]
        podId = self.pods[0].id
        if volumeStoragePool.podid == podId:
            podId = self.pods[1].id
        pool = self.getPodStoragePoolWithTags(podId, self.computeOfferingStorageTags)
        self.assertNotEqual(
            pool,
            None,
            "Target storage pool mapping for VM ID: %s failed" % vm_id)
        migrateTo = { "volume": rootVolume.id, "pool": pool.id}
        return [migrateTo]

    def migrateVmWithVolume(self, vm_id, host_id, migrate_to=None):
        migrateVirtualMachineWithVolumeCmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()
        migrateVirtualMachineWithVolumeCmd.virtualmachineid = vm_id
        if host_id != None:
            migrateVirtualMachineWithVolumeCmd.hostid = host_id
        if migrate_to != None:
            migrateVirtualMachineWithVolumeCmd.migrateto = migrate_to
        response = self.apiclient.migrateVirtualMachineWithVolume(migrateVirtualMachineWithVolumeCmd)
        return response
