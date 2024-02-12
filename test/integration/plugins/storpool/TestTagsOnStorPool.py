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
import pprint
import random
import subprocess
import time
import json

from marvin.cloudstackAPI import (listOsTypes,
                                  listTemplates,
                                  listHosts,
                                  createTemplate,
                                  createVolume,
                                  resizeVolume,
                                  revertSnapshot,
                                  startVirtualMachine)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from marvin.configGenerator import configuration, cluster
from marvin.lib.base import (Account,
                             Configurations,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Template,
                             Tag,
                             VirtualMachine,
                             VmSnapshot,
                             Volume,
                             SecurityGroup,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_disk_offering,
                               list_snapshots,
                               list_storage_pools,
                               list_volumes,
                               list_virtual_machines,
                               list_configurations,
                               list_service_offering,
                               list_clusters,
                               list_zones)
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from nose.plugins.attrib import attr

from storpool import spapi
import uuid
from sp_util import (TestData, StorPoolHelper)

class TestStoragePool(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestStoragePool, cls).setUpClass()
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

        cls.spapi = spapi.Api(host=zone.spEndpoint, port=zone.spEndpointPort, auth=zone.spAuthToken, multiCluster=True)
        testClient = super(TestStoragePool, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ("hyperv", "lxc"):
            cls.unsupportedHypervisor = True
            return

        cls._cleanup = []

        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = list_zones(cls.apiclient, name=zone.name)[0]
        cls.debug(cls.zone)
        cls.debug(list_zones(cls.apiclient, name=zone.name))
        assert cls.zone is not None

        assert cls.zone is not None

        td = TestData()
        cls.testdata = td.testdata
        cls.helper = StorPoolHelper()

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

        storpool_primary_storage = cls.testdata[TestData.primaryStorage]

        storpool_service_offerings = cls.testdata[TestData.serviceOffering]

        cls.template_name = storpool_primary_storage.get("name")

        storage_pool = list_storage_pools(
            cls.apiclient,
            name=cls.template_name
            )

        service_offerings = list_service_offering(
            cls.apiclient,
            name=cls.template_name
            )

        disk_offerings = list_disk_offering(
            cls.apiclient,
            name="ssd"
            )

        cls.disk_offerings = disk_offerings[0]
        if storage_pool is None:
            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.storage_pool = storage_pool
        cls.debug(pprint.pformat(storage_pool))
        if service_offerings is None:
            service_offerings = ServiceOffering.create(cls.apiclient, storpool_service_offerings)
        else:
            service_offerings = service_offerings[0]
        #The version of CentOS has to be supported
        template = get_template(
             cls.apiclient,
            cls.zone.id,
            account = "system"
        )

        cls.debug(pprint.pformat(template))
        cls.debug(pprint.pformat(cls.hypervisor))

        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskofferingid"] = cls.disk_offerings.id

        cls.service_offering = service_offerings
        cls.debug(pprint.pformat(cls.service_offering))

        cls.volume_1 = Volume.create(
            cls.apiclient,
           cls.services,
           account=cls.account.name,
           domainid=cls.account.domainid,
           size=5
        )
        cls.volume_2 = Volume.create(
            cls.apiclient,
           cls.services,
           account=cls.account.name,
           domainid=cls.account.domainid,
           size =5
        )
        cls.volume = Volume.create(
            cls.apiclient,
           cls.services,
           account=cls.account.name,
           domainid=cls.account.domainid,
           size=5
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )
        cls.virtual_machine2= VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )
        cls.virtual_machine3 = VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            diskofferingid=cls.disk_offerings.id,
            size=2,
            rootdisksize=10
        )
        cls.template = template
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
    def test_01_set_vcpolicy_tag_to_vm_with_attached_disks(self):
        ''' Test set vc-policy tag to VM with one attached disk
        '''
        volume_attached = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume_1
            )
        tag = Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine.id,
            resourceType='UserVm',
            tags={'vc-policy': 'testing_vc-policy'}
        )
        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine.id, listall=True)
        vm_tags = vm[0].tags
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id, listall=True
            )

        self.vc_policy_tags(volumes, vm_tags, vm, True)


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_set_vcpolicy_tag_to_attached_disk(self):
        """ Test set vc-policy tag to new disk attached to VM"""
        volume_attached = self.virtual_machine.attach_volume(
                self.apiclient,
                self.volume_2
                )
        volume = list_volumes(self.apiclient, id = volume_attached.id, listall=True)
        name = volume[0].path.split("/")[3]
        sp_volume = self.spapi.volumeList(volumeName="~" + name)

        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine.id, listall=True)
        vm_tags = vm[0].tags
        for vm_tag in vm_tags:
            for sp_tag in sp_volume[0].tags:
                if sp_tag == vm_tag.key:
                    self.assertEqual(sp_tag, vm_tag.key, "StorPool tag is not the same as the Virtual Machine tag")
                    self.assertEqual(sp_volume[0].tags[sp_tag], vm_tag.value, "StorPool tag value is not the same as the Virtual Machine tag value")
                if sp_tag == 'cvm':
                    self.assertEqual(sp_volume[0].tags[sp_tag], vm[0].id, "cvm tag is not the expected value")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_create_vm_snapshot_vc_policy_tag(self):
        """Test to create VM snapshots with VC policy tags
        """
        volume_attached = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume
            )

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            listall=True)
        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine.id, listall=True)
        vm_tags =  vm[0].tags

        self.vc_policy_tags(volumes, vm_tags, vm, True)


        self.assertEqual(volume_attached.id, self.volume.id, "Is not the same volume ")
        try:
            # Login to VM and write data to file system
            ssh_client = self.virtual_machine.get_ssh_client()

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
    def test_04_revert_vm_snapshots_vc_policy_tag(self):
        """Test to revert VM snapshots with VC policy tag
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

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id, listall=True
            )
        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine.id, listall=True)
        vm_tags =  vm[0].tags

        self.vc_policy_tags(volumes, vm_tags, vm, True)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_delete_vm_snapshots(self):
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


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_remove_vcpolicy_tag_when_disk_detached(self):
        """ Test remove vc-policy tag to disk detached from VM"""
        time.sleep(60)
        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine.id, listall=True)
        vm_tags = vm[0].tags
        volumes = list_volumes(
            self.apiclient,
            id= self.volume_2.id, listall=True,
            )
        volume_detached = self.virtual_machine.detach_volume(
                self.apiclient,
                self.volume_2
                )
        self.vc_policy_tags( volumes, vm_tags, vm, False)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_07_delete_vcpolicy_tag(self):
        """ Test delete vc-policy tag of VM"""
        Tag.delete(self.apiclient,
            resourceIds=self.virtual_machine.id,
            resourceType='UserVm',
            tags={'vc-policy': 'testing_vc-policy'})

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id, listall=True
            )
        for v in volumes:
            name = v.path.split("/")[3]
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            tags = spvolume[0].tags
            for t in tags:
                self.assertFalse(t.lower() == 'vc-policy'.lower(), "There is VC Policy tag")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_vcpolicy_tag_to_reverted_disk(self):
        tag = Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine2.id,
            resourceType='UserVm',
            tags={'vc-policy': 'testing_vc-policy'}
        )
        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine2.id, listall=True)
        vm_tags = vm[0].tags

        volume = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine2.id, listall=True,
            type = "ROOT"
            )
        self.vc_policy_tags(volume, vm_tags, vm, True)

        snapshot = Snapshot.create(
            self.apiclient,
            volume[0].id,
            account=self.account.name,
           domainid=self.account.domainid
            )

        virtual_machine = self.virtual_machine2.stop(
            self.apiclient,
            forced=True
            )

        cmd = revertSnapshot.revertSnapshotCmd()
        cmd.id = snapshot.id
        revertedn = self.apiclient.revertSnapshot(cmd)

        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine2.id)
        vm_tags = vm[0].tags

        vol = list_volumes(self.apiclient, id = snapshot.volumeid, listall=True)
        self.vc_policy_tags(vol, vm_tags, vm, True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_09_remove_vm_tags_on_datadisks_attached_to_destroyed_vm(self):
        tag = Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine3.id,
            resourceType='UserVm',
            tags={'vc-policy': 'testing_vc-policy'}
        )
        vm = list_virtual_machines(self.apiclient,id = self.virtual_machine3.id, listall=True)
        vm_tags = vm[0].tags
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine3.id, listall=True
            )

        self.vc_policy_tags(volumes, vm_tags, vm, True)

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine3.id, listall=True, type="DATADISK"
            )
        self.virtual_machine3.delete(self.apiclient, expunge=True)

        self.vc_policy_tags(volumes, vm_tags, vm, False)

    def vc_policy_tags(self, volumes, vm_tags, vm, should_tags_exists=None):
        vcPolicyTag = False
        cvmTag = False
        for v in volumes:
            name = v.path.split("/")[3]
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            tags = spvolume[0].tags
            for t in tags:
                for vm_tag in vm_tags:
                    if t == vm_tag.key:
                        vcPolicyTag = True
                        self.assertEqual(tags[t], vm_tag.value, "Tags are not equal")
                    if t == 'cvm':
                        cvmTag = True
                        self.assertEqual(tags[t], vm[0].id, "CVM tag is not the same as vm UUID")
            #self.assertEqual(tag.tags., second, msg)
        if should_tags_exists:
            self.assertTrue(vcPolicyTag, "There aren't volumes with vm tags")
            self.assertTrue(cvmTag, "There aren't volumes with vm tags")
        else:
            self.assertFalse(vcPolicyTag, "The tags should be removed")
            self.assertFalse(cvmTag, "The tags should be removed")
