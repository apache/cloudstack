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
                             Cluster,
                             Configurations,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Template,
                             VirtualMachine,
                             VmSnapshot,
                             Volume,
                             SecurityGroup,
                             Role,
                             DiskOffering,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_disk_offering,
                               list_hosts,
                               list_snapshots,
                               list_storage_pools,
                               list_volumes,
                               list_virtual_machines,
                               list_configurations,
                               list_service_offering,
                               list_clusters,
                               list_zones)
from marvin.cloudstackAPI import (listOsTypes,
                                  listTemplates,
                                  listHosts,
                                  createTemplate,
                                  createVolume,
                                  getVolumeSnapshotDetails,
                                  resizeVolume,
                                  listZones,
                                  migrateVirtualMachine,
                                  findHostsForMigration,
                                  revertSnapshot,
                                  deleteSnapshot)
from marvin.sshClient import SshClient

import time
import pprint
import random
import subprocess
from storpool import spapi
from storpool import sptypes
import unittest

import uuid
from sp_util import (TestData, StorPoolHelper)

class TestEncryptedVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestEncryptedVolumes, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            cls.cleanUpCloudStack()
            raise

    @classmethod
    def setUpCloudStack(cls):
        testClient = super(TestEncryptedVolumes, cls).getClsTestClient()

        cls._cleanup = []

        config = cls.getClsConfig()
        StorPoolHelper.logger = cls
        cls.logger = StorPoolHelper.logger

        cls.apiclient = testClient.getApiClient()

        zone = config.zones[0]
        assert zone is not None
        cls.zone = list_zones(cls.apiclient, name=zone.name)[0]

        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        cls.spapi = spapi.Api(host=zone.spEndpoint, port=zone.spEndpointPort, auth=zone.spAuthToken, multiCluster=True)
        cls.helper = StorPoolHelper()

        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ("hyperv", "lxc"):
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)

        td = TestData()
        cls.testdata = td.testdata

        cls.sp_template_1 = "ssd"
        storpool_primary_storage = {
            "name": cls.sp_template_1,
            "zoneid": cls.zone.id,
            "url": "SP_API_HTTP=%s:%s;SP_AUTH_TOKEN=%s;SP_TEMPLATE=%s" % (zone.spEndpoint, zone.spEndpointPort, zone.spAuthToken, cls.sp_template_1),
            "scope": "zone",
            "capacitybytes": 564325555333,
            "capacityiops": 155466,
            "hypervisor": "kvm",
            "provider": "StorPool",
            "tags": cls.sp_template_1
        }

        cls.storpool_primary_storage = storpool_primary_storage

        storage_pool = list_storage_pools(
            cls.apiclient,
            name=storpool_primary_storage["name"]
        )

        if storage_pool is None:
            newTemplate = sptypes.VolumeTemplateCreateDesc(name=storpool_primary_storage["name"], placeAll="virtual",
                                                           placeTail="virtual", placeHead="virtual", replication=1)
            template_on_local = cls.spapi.volumeTemplateCreate(newTemplate)

            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.primary_storage = storage_pool

        storpool_service_offerings_ssd = {
            "name": "ssd-encrypted",
            "displaytext": "SP_CO_2 (Min IOPS = 10,000; Max IOPS = 15,000)",
            "cpunumber": 1,
            "cpuspeed": 500,
            "memory": 512,
            "storagetype": "shared",
            "customizediops": False,
            "hypervisorsnapshotreserve": 200,
            "encryptroot": True,
            "tags": cls.sp_template_1
        }

        service_offerings_ssd = list_service_offering(
            cls.apiclient,
            name=storpool_service_offerings_ssd["name"]
        )

        if service_offerings_ssd is None:
            service_offerings_ssd = ServiceOffering.create(cls.apiclient, storpool_service_offerings_ssd, encryptroot=True)
        else:
            service_offerings_ssd = service_offerings_ssd[0]

        cls.service_offering = service_offerings_ssd
        cls.debug(pprint.pformat(cls.service_offering))

        cls.sp_template_2 = "ssd2"

        storpool_primary_storage2 = {
            "name": cls.sp_template_2,
            "zoneid": cls.zone.id,
            "url": "SP_API_HTTP=%s:%s;SP_AUTH_TOKEN=%s;SP_TEMPLATE=%s" % (zone.spEndpoint, zone.spEndpointPort, zone.spAuthToken, cls.sp_template_2),
            "scope": "zone",
            "capacitybytes": 564325555333,
            "capacityiops": 1554,
            "hypervisor": "kvm",
            "provider": "StorPool",
            "tags": cls.sp_template_2
        }

        cls.storpool_primary_storage2 = storpool_primary_storage2
        storage_pool = list_storage_pools(
            cls.apiclient,
            name=storpool_primary_storage2["name"]
        )

        if storage_pool is None:
            newTemplate = sptypes.VolumeTemplateCreateDesc(name=storpool_primary_storage2["name"], placeAll="virtual",
                                                           placeTail="virtual", placeHead="virtual", replication=1)
            template_on_local = cls.spapi.volumeTemplateCreate(newTemplate)
            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage2)

        else:
            storage_pool = storage_pool[0]
        cls.primary_storage2 = storage_pool

        storpool_service_offerings_ssd2 = {
            "name": "ssd2-encrypted",
            "displaytext": "SP_CO_2",
            "cpunumber": 1,
            "cpuspeed": 500,
            "memory": 512,
            "storagetype": "shared",
            "customizediops": False,
            "encryptroot": True,
            "tags": cls.sp_template_2
        }

        service_offerings_ssd2 = list_service_offering(
            cls.apiclient,
            name=storpool_service_offerings_ssd2["name"]
        )

        if service_offerings_ssd2 is None:
            service_offerings_ssd2 = ServiceOffering.create(cls.apiclient, storpool_service_offerings_ssd2, encryptroot=True)
        else:
            service_offerings_ssd2 = service_offerings_ssd2[0]

        cls.service_offering2 = service_offerings_ssd2

        cls.disk_offerings_ssd2_encrypted = list_disk_offering(
            cls.apiclient,
            name=cls.testdata[TestData.diskOfferingEncrypted2]["name"]
        )
        if cls.disk_offerings_ssd2_encrypted is None:
            cls.disk_offerings_ssd2_encrypted = DiskOffering.create(cls.apiclient, cls.testdata[TestData.diskOfferingEncrypted2], encrypt=True)
        else:
            cls.disk_offerings_ssd2_encrypted = cls.disk_offerings_ssd2_encrypted[0]

        cls.disk_offering_ssd_encrypted = list_disk_offering(
            cls.apiclient,
            name=cls.testdata[TestData.diskOfferingEncrypted]["name"]
        )

        if cls.disk_offering_ssd_encrypted is None:
            cls.disk_offering_ssd_encrypted = DiskOffering.create(cls.apiclient, cls.testdata[TestData.diskOfferingEncrypted], encrypt=True)
        else:
            cls.disk_offering_ssd_encrypted = cls.disk_offering_ssd_encrypted[0]

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            account="system"
        )

        if template == FAILED:
            assert False, "get_template() failed to return template\
                     with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        role = Role.list(cls.apiclient, name='Root Admin')

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id,
            roleid= 1
        )

        securitygroup = SecurityGroup.list(cls.apiclient, account=cls.account.name, domainid=cls.account.domainid)[0]
        cls.helper.set_securityGroups(cls.apiclient, account=cls.account.name, domainid=cls.account.domainid,
                                      id=securitygroup.id)
        cls._cleanup.append(cls.account)

        cls.volume_1 = Volume.create(
            cls.apiclient,
            {"diskname": "StorPoolEncryptedDiskLiveMigrate"},
            zoneid=cls.zone.id,
            diskofferingid=cls.disk_offering_ssd_encrypted.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )

        cls.volume_2 = Volume.create(
            cls.apiclient,
            {"diskname": "StorPoolEncryptedDiskVMSnapshot"},
            zoneid=cls.zone.id,
            diskofferingid=cls.disk_offering_ssd_encrypted.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            {"name": "StorPool-LiveMigrate-VM%s" % uuid.uuid4()},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.virtual_machine2 = VirtualMachine.create(
            cls.apiclient,
            {"name": "StorPool-VMSnapshots%s" % uuid.uuid4()},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.virtual_machine3 = VirtualMachine.create(
            cls.apiclient,
            {"name": "StorPool-VolumeSnapshots%s" % uuid.uuid4()},
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.template = template
        cls.hostid = cls.virtual_machine.hostid
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


# live migrate VM with encrypted volumes to another host
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_live_migrate_vm(self):
        '''
        Live Migrate VM to another host with encrypted volumes
        '''
        self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume_1
            )

        volumes = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        )

        vm_host = list_hosts(self.apiclient, id=self.virtual_machine.hostid)[0]
        self.logger.debug(vm_host)
        # sshc = SshClient(
        #     host=vm_host.name,
        #     port=22,
        #     user=None,
        #     passwd=None)
        #
        # for volume in volumes:
        #     cmd = 'blkid %s' % volume.path
        #     result = sshc.execute(cmd)
        #     if "LUKS" not in result:
        #         self.fail("The volume isn't encrypted %s" % volume)


        dest_host_cmd = findHostsForMigration.findHostsForMigrationCmd()
        dest_host_cmd.virtualmachineid = self.virtual_machine.id
        host = self.apiclient.findHostsForMigration(dest_host_cmd)[0]

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.hostid = host.id
        self.apiclient.migrateVirtualMachine(cmd)

# VM snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_vm_snapshot(self):
        self.virtual_machine2.attach_volume(
            self.apiclient,
            self.volume_2
            )

        try:
            ssh_client = self.virtual_machine2.get_ssh_client(reconnect=True)

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
                      self.virtual_machine2.ipaddress)
        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data has be write into temp file!"
        )

        time.sleep(30)
        MemorySnapshot = False
        vm_snapshot = VmSnapshot.create(
            self.apiclient,
            self.virtual_machine2.id,
            MemorySnapshot,
            "TestSnapshot",
            "Display Text"
        )
        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

# Revert VM snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_revert_vm_snapshots(self):
        """Test to revert VM snapshots
        """

        try:
            ssh_client = self.virtual_machine2.get_ssh_client(reconnect=True)

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
                      self.virtual_machine2.ipaddress)

        if str(result[0]).index("No such file or directory") == -1:
            self.fail("Check the random data has be delete from temp file!")

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine2.id,
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

        self.virtual_machine2.stop(self.apiclient, forced=True)

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id
            )

        self.virtual_machine2.start(self.apiclient)

        try:
            ssh_client = self.virtual_machine2.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine2.ipaddress)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )

    # Delete VM snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_delete_vm_snapshots(self):
        """Test to delete vm snapshots
        """

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine2.id,
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
            virtualmachineid=self.virtual_machine2.id,
            listall=False)
        self.debug('list_snapshot_response -------------------- %s' % list_snapshot_response)

        self.assertIsNone(list_snapshot_response, "snapshot is already deleted")

# Take volume snapshot
    @unittest.expectedFailure
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_snapshot_volume_with_secondary(self):
        '''
            Test Create snapshot and backup to secondary
        '''
        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "false")
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )

        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_snapshot_volume_on_primary(self):
        '''
            Test Create snapshot and backup to secondary
        '''
        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )
        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot_details")
        except spapi.ApiError as err:
            raise Exception(err)
        self.assertIsNotNone(snapshot, "Could not create snapshot")
# Rever Volume snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_07_revert_volume_on_primary(self):
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )[0]
        snapshot = list_snapshots(
            self.apiclient,
            volumeid = volume.id,
            listall=True
        )[0]
        self.virtual_machine3.stop(self.apiclient, forced=True)

        cmd = revertSnapshot.revertSnapshotCmd()
        cmd.id = snapshot.id
        revertcmd = self.apiclient.revertSnapshot(cmd)

# Delete volume snapshot
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_delete_volume_on_primary(self):
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine3.id,
                        type = "ROOT",
                        listall = True,
                        )[0]
        snapshot = list_snapshots(
            self.apiclient,
            volumeid = volume.id,
            listall=True
        )[0]
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = snapshot.id
        self.apiclient.deleteSnapshot(cmd)

# Live migrate encrypted volume
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_09_live_migrate_volume(self):
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )[0]

        Volume.migrate(self.apiclient, volumeid=volume.id, storageid=self.primary_storage2.id, livemigrate=True)
