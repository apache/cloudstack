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
""" BVT tests for Volumes
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
#from marvin.cloudstackException import *
from marvin.cloudstackAPI import (deleteVolume,
                                  extractVolume,
                                  resizeVolume)
#from marvin.sshClient import SshClient
from marvin.lib.utils import (cleanup_resources,
                              format_volume_to_ext3,
                              wait_until)
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             Volume,
                             Host,
                             DiskOffering,
                             StoragePool,)
from marvin.lib.common import (get_domain,
                                get_suitable_test_template,
                                get_zone,
                                find_storage_pool_type,
                                get_pod,
                                list_disk_offering)
from marvin.lib.utils import checkVolumeSize
from marvin.codes import SUCCESS, FAILED, XEN_SERVER
from nose.plugins.attrib import attr
#Import System modules
import os
import urllib.request, urllib.parse, urllib.error
import time
import tempfile

_multiprocess_shared_ = True

class TestCreateVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCreateVolume, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls._cleanup = []
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.invalidStoragePoolType = False
        #for LXC if the storage pool of type 'rbd' ex: ceph is not available, skip the test
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                # RBD storage type is required for data volumes for LXC
                cls.invalidStoragePoolType = True
                return
        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )
        cls.sparse_disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["sparse_disk_offering"]
                                    )
        cls.custom_disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"],
                                    custom=True
                                    )

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["customdiskofferingid"] = cls.custom_disk_offering.id
        cls.services["diskname"] = cls.services["volume"]["diskname"]
        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offerings"]["tiny"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                )
        cls._cleanup = [
                        cls.service_offering,
                        cls.disk_offering,
                        cls.custom_disk_offering,
                        cls.account
                        ]

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.invalidStoragePoolType:
            self.skipTest("Skipping test because of valid storage\
                    pool not available")

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_01_create_volume(self):
        """Test Volume creation for all Disk Offerings (incl. custom)
        """

        # Validate the following
        # 1. Create volumes from the different sizes
        # 2. Verify the size of volume with actual size allocated

        self.volumes = []
        for k, v in list(self.services["volume_offerings"].items()):
            volume = Volume.create(
                                   self.apiClient,
                                   v,
                                   zoneid=self.zone.id,
                                   account=self.account.name,
                                   domainid=self.account.domainid,
                                   diskofferingid=self.disk_offering.id
                                   )
            self.debug("Created a volume with ID: %s" % volume.id)
            self.volumes.append(volume)

        if self.virtual_machine.hypervisor == "KVM":
            sparse_volume = Volume.create(
                                        self.apiClient,
                                        self.services,
                                        zoneid=self.zone.id,
                                        account=self.account.name,
                                        domainid=self.account.domainid,
                                        diskofferingid=self.sparse_disk_offering.id
                                        )
            self.debug("Created a sparse volume: %s" % sparse_volume.id)
            self.volumes.append(sparse_volume)

        volume = Volume.create_custom_disk(
                                    self.apiClient,
                                    self.services,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    )
        self.debug("Created a volume with custom offering: %s" % volume.id)
        self.volumes.append(volume)

        #Attach a volume with different disk offerings
        #and check the memory allocated to each of them
        for volume in self.volumes:
            list_volume_response = Volume.list(
                                               self.apiClient,
                                               id=volume.id)
            self.assertEqual(
                            isinstance(list_volume_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                                list_volume_response,
                                None,
                                "Check if volume exists in ListVolumes"
                                )
            self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    volume.id,
                                                    self.virtual_machine.id
                                                    ))
            self.virtual_machine.attach_volume(
                                                self.apiClient,
                                                volume
                                               )
            try:
                ssh = self.virtual_machine.get_ssh_client()
                self.debug("Rebooting VM %s" % self.virtual_machine.id)
                ssh.execute("reboot")
            except Exception as e:
                self.fail("SSH access failed for VM %s - %s" %
                                (self.virtual_machine.ipaddress, e))

            # Poll listVM to ensure VM is started properly
            timeout = self.services["timeout"]
            while True:
                time.sleep(self.services["sleep"])

                # Ensure that VM is in running state
                list_vm_response = VirtualMachine.list(
                                            self.apiClient,
                                            id=self.virtual_machine.id
                                            )

                if isinstance(list_vm_response, list):
                    vm = list_vm_response[0]
                    if vm.state == 'Running':
                        self.debug("VM state: %s" % vm.state)
                        break

                if timeout == 0:
                    raise Exception(
                        "Failed to start VM (ID: %s) " % vm.id)
                timeout = timeout - 1

            vol_sz = str(list_volume_response[0].size)
            ssh = self.virtual_machine.get_ssh_client(
                                                      reconnect=True
                                                      )
            # Get the updated volume information
            list_volume_response = Volume.list(
                                               self.apiClient,
                                               id=volume.id)
            if list_volume_response[0].hypervisor.lower() == XEN_SERVER.lower():
                volume_name = "/dev/xvd" + chr(ord('a') + int(list_volume_response[0].deviceid))
                self.debug(" Using XenServer volume_name: %s" % (volume_name))
                ret = checkVolumeSize(ssh_handle=ssh,volume_name=volume_name,size_to_verify=vol_sz)
            elif list_volume_response[0].hypervisor.lower() == "kvm":
                volume_name = "/dev/vd" + chr(ord('a') + int(list_volume_response[0].deviceid))
                self.debug(" Using KVM volume_name: %s" % (volume_name))
                ret = checkVolumeSize(ssh_handle=ssh,volume_name=volume_name,size_to_verify=vol_sz)
            elif list_volume_response[0].hypervisor.lower() == "hyperv":
                ret = checkVolumeSize(ssh_handle=ssh,volume_name="/dev/sdb",size_to_verify=vol_sz)
            else:
                ret = checkVolumeSize(ssh_handle=ssh,size_to_verify=vol_sz)
            self.debug(" Volume Size Expected %s  Actual :%s" %(vol_sz,ret[1]))
            self.virtual_machine.detach_volume(self.apiClient, volume)
            self.assertEqual(ret[0],SUCCESS,"Check if promised disk size actually available")
            time.sleep(self.services["sleep"])

    def tearDown(self):
        #Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestCreateVolume, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVolumes, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls._cleanup = []
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.invalidStoragePoolType = False
        #for LXC if the storage pool of type 'rbd' ex: ceph is not available, skip the test
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                # RBD storage type is required for data volumes for LXC
                cls.invalidStoragePoolType = True
                return
        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )
        cls.resized_disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["resized_disk_offering"]
                                    )
        cls.custom_resized_disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["resized_disk_offering"],
                                    custom=True
                                    )

        cls.template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if cls.template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = cls.template.id
        cls.services["diskofferingid"] = cls.disk_offering.id
        cls.services['resizeddiskofferingid'] = cls.resized_disk_offering.id
        cls.services['customresizeddiskofferingid'] = cls.custom_resized_disk_offering.id

        # Create VMs, VMs etc
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offerings"]["tiny"]
                                        )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                )
        pools = StoragePool.list(cls.apiclient)
        # cls.assertEqual(
        #         validateList(pools)[0],
        #         PASS,
        #         "storage pool list validation failed")



        if cls.hypervisor.lower() == 'lxc' and cls.storage_pools.type.lower() != 'rbd':
            raise unittest.SkipTest("Snapshots not supported on Hyper-V or LXC")
        cls.volume = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup = [
                        cls.resized_disk_offering,
                        cls.custom_resized_disk_offering,
                        cls.service_offering,
                        cls.disk_offering,
                        cls.volume,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.attached = False
        self.cleanup = []

        if self.invalidStoragePoolType:
            self.skipTest("Skipping test because valid storage pool not\
                    available")

    def tearDown(self):
        #Clean up, terminate the created volumes
        if self.attached:
            self.virtual_machine.detach_volume(self.apiClient, self.volume)

        if self.virtual_machine.hypervisor == "KVM":
            self.virtual_machine.stop(self.apiClient)
            self.virtual_machine.start(self.apiClient)

        cleanup_resources(self.apiClient, self.cleanup)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_02_attach_volume(self):
        """Attach a created Volume to a Running VM
        """
        # Validate the following
        # 1. shows list of volumes
        # 2. "Attach Disk" pop-up box will display with list of  instances
        # 3. disk should be  attached to instance successfully

        self.debug(
            "Attaching volume (ID: %s) to VM (ID: %s)" % (
                self.volume.id,
                self.virtual_machine.id
            ))
        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        self.attached = True
        list_volume_response = Volume.list(
            self.apiClient,
            id=self.volume.id
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        volume = list_volume_response[0]
        self.assertNotEqual(
            volume.virtualmachineid,
            None,
            "Check if volume state (attached) is reflected"
        )
        try:
            #Format the attached volume to a known fs
            format_volume_to_ext3(self.virtual_machine.get_ssh_client())

        except Exception as e:

            self.fail("SSH failed for VM: %s - %s" %
                      (self.virtual_machine.ipaddress, e))
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_download_attached_volume(self):
        """Download a Volume attached to a VM
        """
        # Validate the following
        # 1. download volume will fail with proper error message
        #    "Failed - Invalid state of the volume with ID:
        #    It should be either detached or the VM should be in stopped state

        self.debug("Extract attached Volume ID: %s" % self.volume.id)

        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        self.attached = True
        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.services["zoneid"]
        # A proper exception should be raised;
        # downloading attach VM is not allowed
        with self.assertRaises(Exception):
            self.apiClient.extractVolume(cmd)

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_delete_attached_volume(self):
        """Delete a Volume attached to a VM
        """

        # Validate the following
        # 1. delete volume will fail with proper error message
        #    "Failed - Invalid state of the volume with ID:
        #    It should be either detached or the VM should be in stopped state

        self.debug("Trying to delete attached Volume ID: %s" %
                                                        self.volume.id)
        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        self.attached = True
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        #Proper exception should be raised; deleting attach VM is not allowed
        #with self.assertRaises(Exception):
        with self.assertRaises(Exception):
            self.apiClient.deleteVolume(cmd)

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_detach_volume(self):
        """Detach a Volume attached to a VM
        """

        # Validate the following
        # Data disk should be detached from instance and detached data disk
        # details should be updated properly

        self.debug(
                "Detaching volume (ID: %s) from VM (ID: %s)" % (
                                                    self.volume.id,
                                                    self.virtual_machine.id
                                                    ))
        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        self.virtual_machine.detach_volume(self.apiClient, self.volume)
        self.attached = False
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])
        list_volume_response = Volume.list(
                                                self.apiClient,
                                                id=self.volume.id
                                                )
        self.assertEqual(
                            isinstance(list_volume_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            list_volume_response,
                            None,
                            "Check if volume exists in ListVolumes"
                            )
        volume = list_volume_response[0]
        self.assertEqual(
                         volume.virtualmachineid,
                         None,
                         "Check if volume state (detached) is reflected"
                         )
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_06_download_detached_volume(self):
        """Download a Volume unattached to an VM
        """
        # Validate the following
        # 1. able to download the volume when its not attached to instance

        self.debug("Extract detached Volume ID: %s" % self.volume.id)

        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])
        self.virtual_machine.detach_volume(self.apiClient, self.volume)
        self.attached = False
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.services["zoneid"]
        extract_vol = self.apiClient.extractVolume(cmd)

        #Attempt to download the volume and save contents locally
        try:
            formatted_url = urllib.parse.unquote_plus(extract_vol.url)
            self.debug("Attempting to download volume at url %s" % formatted_url)
            response = urllib.request.urlopen(formatted_url)
            self.debug("response from volume url %s" % response.getcode())
            fd, path = tempfile.mkstemp()
            self.debug("Saving volume %s to path %s" %(self.volume.id, path))
            os.close(fd)
            with open(path, 'wb') as fd:
                fd.write(response.read())
            self.debug("Saved volume successfully")
        except Exception:
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)" \
                % (extract_vol.url, self.volume.id)
            )

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_07_resize_fail(self):
        """Test resize (negative) non-existent volume"""
        # Verify the size is the new size is what we wanted it to be.
        self.debug("Fail Resize Volume ID: %s" % self.volume.id)

        # first, an invalid id
        cmd                = resizeVolume.resizeVolumeCmd()
        cmd.id             = "invalid id"
        cmd.diskofferingid = self.services['customresizeddiskofferingid']
        success            = False
        try:
            self.apiClient.resizeVolume(cmd)
        except Exception as ex:
            #print str(ex)
            if "invalid" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "ResizeVolume - verify invalid id is handled appropriately")

        # Next, we'll try an invalid disk offering id
        cmd.id             = self.volume.id
        cmd.diskofferingid = "invalid id"
        success            = False
        try:
            self.apiClient.resizeVolume(cmd)
        except Exception as ex:
            if "invalid" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "ResizeVolume - verify disk offering is handled appropriately")

        # try to resize a root disk with a disk offering, root can only be resized by size=
        # get root vol from created vm
        list_volume_response = Volume.list(
                                            self.apiClient,
                                            virtualmachineid=self.virtual_machine.id,
                                            type='ROOT',
                                            listall=True
                                            )

        rootvolume = list_volume_response[0]

        cmd.id             = rootvolume.id
        cmd.diskofferingid = self.services['diskofferingid']
        with self.assertRaises(Exception):
            self.apiClient.resizeVolume(cmd)

        # Ok, now let's try and resize a volume that is not custom.
        cmd.id             = self.volume.id
        cmd.diskofferingid = self.services['diskofferingid']
        cmd.size           = 4

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume.id,
                                                    self.virtual_machine.id)
                 )
        #attach the volume
        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        self.attached = True
        #stop the vm if it is on xenserver
        hosts = Host.list(self.apiClient, id=self.virtual_machine.hostid)
        self.assertTrue(isinstance(hosts, list))
        self.assertTrue(len(hosts) > 0)
        self.debug("Found %s host" % hosts[0].hypervisor)

        if hosts[0].hypervisor == "XenServer":
            self.virtual_machine.stop(self.apiClient)
        elif hosts[0].hypervisor.lower() in ("vmware", "hyperv"):
            self.skipTest("Resize Volume is unsupported on VmWare and Hyper-V")

        # Attempting to resize it should throw an exception, as we're using a non
        # customisable disk offering, therefore our size parameter should be ignored
        with self.assertRaises(Exception):
            self.apiClient.resizeVolume(cmd)

        if hosts[0].hypervisor == "XenServer":
            self.virtual_machine.start(self.apiClient)
            time.sleep(30)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_08_resize_volume(self):
        """Test resize a volume"""
        # Verify the size is the new size is what we wanted it to be.
        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume.id,
                                                    self.virtual_machine.id
                                                    ))

        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        self.attached = True
        hosts = Host.list(self.apiClient, id=self.virtual_machine.hostid)
        self.assertTrue(isinstance(hosts, list))
        self.assertTrue(len(hosts) > 0)
        self.debug("Found %s host" % hosts[0].hypervisor)

        if hosts[0].hypervisor == "XenServer":
            self.virtual_machine.stop(self.apiClient)
        elif hosts[0].hypervisor.lower() in ("vmware", "hyperv"):
            self.skipTest("Resize Volume is unsupported on VmWare and Hyper-V")

        # resize the data disk
        self.debug("Resize Volume ID: %s" % self.volume.id)

        self.services["disk_offering"]["disksize"] = 20
        disk_offering_20_GB = DiskOffering.create(
                                    self.apiclient,
                                    self.services["disk_offering"]
                                    )
        self.cleanup.append(disk_offering_20_GB)

        cmd                = resizeVolume.resizeVolumeCmd()
        cmd.id             = self.volume.id
        cmd.diskofferingid = disk_offering_20_GB.id

        self.apiClient.resizeVolume(cmd)

        count = 0
        success = False
        while count < 3:
            list_volume_response = Volume.list(
                                                self.apiClient,
                                                id=self.volume.id,
                                                type='DATADISK'
                                                )
            for vol in list_volume_response:
                if vol.id == self.volume.id and int(vol.size) == (int(disk_offering_20_GB.disksize) * (1024** 3)) and vol.state == 'Ready':
                    success = True
            if success:
                break
            else:
                time.sleep(10)
                count += 1

        self.assertEqual(
                         success,
                         True,
                         "Check if the data volume resized appropriately"
                         )

        can_shrink = False

        list_volume_response = Volume.list(
                                            self.apiClient,
                                            id=self.volume.id,
                                            type='DATADISK'
                                            )
        storage_pool_id = [x.storageid for x in list_volume_response if x.id == self.volume.id][0]
        storage = StoragePool.list(self.apiclient, id=storage_pool_id)[0]
        # At present only CLVM supports shrinking volumes
        if storage.type.lower() == "clvm":
            can_shrink = True

        if can_shrink:
            self.services["disk_offering"]["disksize"] = 10
            disk_offering_10_GB = DiskOffering.create(
                                        self.apiclient,
                                        self.services["disk_offering"]
                                        )
            self.cleanup.append(disk_offering_10_GB)

            cmd                = resizeVolume.resizeVolumeCmd()
            cmd.id             = self.volume.id
            cmd.diskofferingid = disk_offering_10_GB.id
            cmd.shrinkok       = "true"

            self.apiClient.resizeVolume(cmd)

            count = 0
            success = False
            while count < 3:
                list_volume_response = Volume.list(
                                                    self.apiClient,
                                                    id=self.volume.id
                                                    )
                for vol in list_volume_response:
                    if vol.id == self.volume.id and int(vol.size) == (int(disk_offering_10_GB.disksize) * (1024 ** 3)) and vol.state == 'Ready':
                        success = True
                if success:
                    break
                else:
                    time.sleep(10)
                    count += 1

            self.assertEqual(
                             success,
                             True,
                             "Check if the root volume resized appropriately"
                             )

        #start the vm if it is on xenserver

        if hosts[0].hypervisor == "XenServer":
            self.virtual_machine.start(self.apiClient)
            time.sleep(30)
        return

    @attr(tags = ["advanced", "advancedns", "smoke","basic"], required_hardware="false")
    def test_09_delete_detached_volume(self):
        """Delete a Volume unattached to an VM
        """
        # Validate the following
        # 1. volume should be deleted successfully and listVolume should not
        #    contain the deleted volume details.
        # 2. "Delete Volume" menu item not shown under "Actions" menu.
        #    (UI should not allow  to delete the volume when it is attached
        #    to instance by hiding the menu Item)

        self.debug("Delete Volume ID: %s" % self.volume.id)

        self.volume_1 = Volume.create(
                                   self.apiclient,
                                   self.services,
                                   account=self.account.name,
                                   domainid=self.account.domainid
        )

        self.virtual_machine.attach_volume(self.apiClient, self.volume_1)
        self.virtual_machine.detach_volume(self.apiClient, self.volume_1)

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume_1.id
        self.apiClient.deleteVolume(cmd)

        list_volume_response = Volume.list(
                                            self.apiClient,
                                            id=self.volume_1.id,
                                            type='DATADISK'
                                            )
        self.assertEqual(
                        list_volume_response,
                        None,
                        "Check if volume exists in ListVolumes"
                    )
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_10_list_volumes(self):

        # Validate the following
        #
        # 1. List Root Volume and waits until it has the newly introduced attributes
        #
        # 2. Verifies return attributes has values different from none, when instance is running
        #

        list_vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)[0]

        host = Host.list(
            self.apiclient,
            type='Routing',
            id=list_vm.hostid
        )[0]
        list_pods = get_pod(self.apiclient, self.zone.id, host.podid)

        root_volume = self.wait_for_attributes_and_return_root_vol()

        self.assertTrue(hasattr(root_volume, "utilization"))
        self.assertTrue(root_volume.utilization is not None)

        self.assertTrue(hasattr(root_volume, "virtualsize"))
        self.assertTrue(root_volume.virtualsize is not None)

        self.assertTrue(hasattr(root_volume, "physicalsize"))
        self.assertTrue(root_volume.physicalsize is not None)

        self.assertTrue(hasattr(root_volume, "vmname"))
        self.assertEqual(root_volume.vmname, list_vm.name)

        self.assertTrue(hasattr(root_volume, "clustername"))
        self.assertTrue(root_volume.clustername is not None)

        self.assertTrue(hasattr(root_volume, "clusterid"))
        self.assertTrue(root_volume.clusterid is not None)

        self.assertTrue(hasattr(root_volume, "storageid"))
        self.assertTrue(root_volume.storageid is not None)

        self.assertTrue(hasattr(root_volume, "storage"))
        self.assertTrue(root_volume.storage is not None)

        self.assertTrue(hasattr(root_volume, "zoneid"))
        self.assertEqual(root_volume.zoneid, self.zone.id)

        self.assertTrue(hasattr(root_volume, "zonename"))
        self.assertEqual(root_volume.zonename, self.zone.name)

        self.assertTrue(hasattr(root_volume, "podid"))
        self.assertEqual(root_volume.podid, list_pods.id)

        self.assertTrue(hasattr(root_volume, "podname"))
        self.assertEqual(root_volume.podname, list_pods.name)

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_11_attach_volume_with_unstarted_vm(self):
        """Attach a created Volume to a unstarted VM
        """
        # Validate the following
        # 1. Attach to a vm in startvm=false state works and vm can be started afterwards.
        # 2. shows list of volumes
        # 3. "Attach Disk" pop-up box will display with list of  instances
        # 4. disk should be  attached to instance successfully

        test_vm = VirtualMachine.create(
            self.apiclient,
            self.services,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"],
            startvm=False
        )

        self.debug(
            "Attaching volume (ID: %s) to VM (ID: %s)" % (
                self.volume.id,
                test_vm.id
            ))
        test_vm.attach_volume(self.apiClient, self.volume)
        test_vm.start(self.apiClient)

        list_volume_response = Volume.list(
            self.apiClient,
            id=self.volume.id
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        volume = list_volume_response[0]
        self.assertNotEqual(
            volume.virtualmachineid,
            None,
            "Check if volume state (attached) is reflected"
        )
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])

        test_vm.detach_volume(self.apiClient, self.volume)
        self.cleanup.append(test_vm)

        return

    def wait_for_attributes_and_return_root_vol(self):
        def checkVolumeResponse():
            list_volume_response = Volume.list(
                self.apiClient,
                virtualmachineid=self.virtual_machine.id,
                type='ROOT',
                listall=True
            )

            if isinstance(list_volume_response, list) and list_volume_response[0].virtualsize is not None:
                return True, list_volume_response[0]
            return False, None

        # sleep interval is 1s, retries is 360, this will sleep atmost 360 seconds, or 6 mins
        res, response = wait_until(1, 360, checkVolumeResponse)
        if not res:
            self.fail("Failed to return root volume response")
        return response


    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_11_migrate_volume_and_change_offering(self):

    # Validates the following
    #
    # 1. Creates a new Volume with a small disk offering
    #
    # 2. Migrates the Volume to another primary storage and changes the offering
    #
    # 3. Verifies the Volume has new offering when migrated to the new storage.

        small_offering = list_disk_offering(
            self.apiclient,
            name = "Small"
        )[0]

        large_offering = list_disk_offering(
            self.apiclient,
            name = "Large"
        )[0]
        volume = Volume.create(
            self.apiClient,
            self.services,
            zoneid = self.zone.id,
            account = self.account.name,
            domainid = self.account.domainid,
            diskofferingid = small_offering.id
        )
        self.debug("Created a small volume: %s" % volume.id)

        self.virtual_machine.attach_volume(self.apiclient, volume=volume)

        if self.virtual_machine.hypervisor == "KVM":
            self.virtual_machine.stop(self.apiclient)

        pools = StoragePool.listForMigration(
            self.apiclient,
            id=volume.id
            )

        pool = None

        if pools and len(pools) > 0:
            pool = pools[0]
        else:
            raise self.skipTest("Not enough storage pools found, skipping test")

        if hasattr(pool, 'tags'):
            StoragePool.update(self.apiclient, id=pool.id, tags="")

        self.debug("Migrating Volume-ID: %s to Pool: %s" % (volume.id, pool.id))
        livemigrate = False
        if self.virtual_machine.hypervisor.lower() == "vmware" or self.virtual_machine.hypervisor.lower() == 'xenserver':
            livemigrate = True

        Volume.migrate(
            self.apiclient,
            volumeid = volume.id,
            storageid = pool.id,
            newdiskofferingid = large_offering.id,
            livemigrate = livemigrate
        )
        if self.virtual_machine.hypervisor == "KVM":
            self.virtual_machine.start(self.apiclient
        )
        migrated_vol = Volume.list(
            self.apiclient,
            id = volume.id
        )[0]
        self.assertEqual(
            migrated_vol.diskofferingname,
            large_offering.name,
            "Offering name did not match with the new one "
        )
        return
