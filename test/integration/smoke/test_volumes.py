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
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import os
import urllib
import time
import tempfile

_multiprocess_shared_ = True

class Services:
    """Test Volume Services
    """

    def __init__(self):
        self.services = {
                         "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
                                    "storagetype": "local"
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "storagetype": "local",
                                    "disksize": 1
                        },
                        'resized_disk_offering': {
                                    "displaytext": "Resized",
                                    "name": "Resized",
                                    "storagetype": "local",
                                    "disksize": 3
                        },
                        "volume_offerings": {
                            0: {
                                "diskname": "TestDiskServ",
                            },
                        },
                        "customdisksize": 1,    # GBs
                        "username": "root",     # Creds for SSH to VM
                        "password": "password",
                        "ssh_port": 22,
                        "diskname": "TestDiskServ",
                        "hypervisor": 'KVM',
                        "privateport": 22,
                        "publicport": 22,
                        "protocol": 'TCP',
                        "diskdevice": "/dev/xvdb",
                        "ostype": 'CentOS 5.5 (64-bit)',
                        "sleep": 10,
                        "timeout": 600,
                    }


class TestCreateVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestCreateVolume, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls.custom_disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"],
                                    custom=True
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["customdiskofferingid"] = cls.custom_disk_offering.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.name
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
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

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_01_create_volume(self):
        """Test Volume creation for all Disk Offerings (incl. custom)
        """

        # Validate the following
        # 1. Create volumes from the different sizes
        # 2. Verify the size of volume with actual size allocated

        self.volumes = []
        for k, v in self.services["volume_offerings"].items():
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
            list_volume_response = list_volumes(
                                                self.apiClient,
                                                id=volume.id
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
                ssh.execute("reboot")

            except Exception as e:
                self.fail("SSH access failed for VM %s - %s" %
                                (self.virtual_machine.ipaddress, e))

            # Poll listVM to ensure VM is started properly
            timeout = self.services["timeout"]
            while True:
                time.sleep(self.services["sleep"])

                # Ensure that VM is in running state
                list_vm_response = list_virtual_machines(
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

            try:
                ssh = self.virtual_machine.get_ssh_client(
                                                      reconnect=True
                                                      )
                c = "/sbin/fdisk -l"
                res = ssh.execute(c)

            except Exception as e:
                self.fail("SSH access failed for VM: %s - %s" %
                                (self.virtual_machine.ipaddress, e))

            # Disk /dev/sda doesn't contain a valid partition table
            # Disk /dev/sda: 21.5 GB, 21474836480 bytes
            result = str(res)
            self.debug("fdisk result: %s" % result)

            self.assertEqual(
                             str(list_volume_response[0].size) in result,
                             True,
                             "Check if promised disk size actually available"
                             )
            self.virtual_machine.detach_volume(self.apiClient, volume)

    def tearDown(self):
        #Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestCreateVolume, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestVolumes, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls.resized_disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["resized_disk_offering"]
                                    )
        cls.custom_resized_disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["resized_disk_offering"],
                                    custom=True
                                    )

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["diskofferingid"] = cls.disk_offering.id
        cls.services['resizeddiskofferingid'] = cls.resized_disk_offering.id
        cls.services['customresizeddiskofferingid'] = cls.custom_resized_disk_offering.id

        # Create VMs, VMs etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.name
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                        )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                )

        cls.volume = Volume.create(
                                   cls.api_client,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup = [
                        cls.resized_disk_offering,
                        cls.custom_resized_disk_offering,
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created volumes
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @attr(tags = ["advanced", "advancedns", "smoke"])
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

        list_volume_response = list_volumes(
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

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_03_download_attached_volume(self):
        """Download a Volume attached to a VM
        """
        # Validate the following
        # 1. download volume will fail with proper error message
        #    "Failed - Invalid state of the volume with ID:
        #    It should be either detached or the VM should be in stopped state

        self.debug("Extract attached Volume ID: %s" % self.volume.id)

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.services["zoneid"]
        # A proper exception should be raised;
        # downloading attach VM is not allowed
        with self.assertRaises(Exception):
            self.apiClient.extractVolume(cmd)

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_04_delete_attached_volume(self):
        """Delete a Volume attached to a VM
        """

        # Validate the following
        # 1. delete volume will fail with proper error message
        #    "Failed - Invalid state of the volume with ID:
        #    It should be either detached or the VM should be in stopped state

        self.debug("Trying to delete attached Volume ID: %s" %
                                                        self.volume.id)

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        #Proper exception should be raised; deleting attach VM is not allowed
        #with self.assertRaises(Exception):
        result = self.apiClient.deleteVolume(cmd)
        self.assertEqual(
                         result,
                         None,
                         "Check for delete download error while volume is attached"
                         )
        
    @attr(tags = ["advanced", "advancedns", "smoke"])    
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

        self.virtual_machine.detach_volume(self.apiClient, self.volume)
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])
        list_volume_response = list_volumes(
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

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_06_download_detached_volume(self):
        """Download a Volume unattached to an VM
        """
        # Validate the following
        # 1. able to download the volume when its not attached to instance

        self.debug("Extract detached Volume ID: %s" % self.volume.id)

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.services["zoneid"]
        extract_vol = self.apiClient.extractVolume(cmd)

        #Attempt to download the volume and save contents locally
        try:
            formatted_url = urllib.unquote_plus(extract_vol.url)
            response = urllib.urlopen(formatted_url)
            fd, path = tempfile.mkstemp()
            os.close(fd)
            fd = open(path, 'wb')
            fd.write(response.read())
            fd.close()

        except Exception:
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)" \
                % (extract_vol.url, self.volume.id)
            )

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_07_resize_fail(self):
        """Verify invalid options fail to Resize a volume"""
        # Verify the size is the new size is what we wanted it to be.
        self.debug("Fail Resize Volume ID: %s" % self.volume.id)

        # first, an invalid id
        cmd                = resizeVolume.resizeVolumeCmd()
        cmd.id             = "invalid id"
        cmd.diskofferingid = self.services['resizeddiskofferingid']
        success            = False
        try:
            response = self.apiClient.resizeVolume(cmd)
        except Exception as ex:
            #print str(ex)
            if "HTTP Error 431:" in str(ex):
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
            response = self.apiClient.resizeVolume(cmd)
        except Exception as ex:
            if "HTTP Error 431:" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "ResizeVolume - verify disk offering is handled appropriately")
        # Ok, now let's try and resize a volume that is not custom.
        cmd.id             = self.volume.id
        cmd.diskofferingid = self.services['diskofferingid']
        cmd.size           = 4
        currentSize        = self.volume.size

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume.id,
                                                    self.virtual_machine.id)
                 )
        #attach the volume
        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        #stop the vm if it is on xenserver
        if self.services['hypervisor'].lower() == "xenserver":
            self.virtual_machine.stop(self.apiClient)

        self.apiClient.resizeVolume(cmd)
        count = 0
        success = True
        while count < 10:
            list_volume_response = list_volumes(
                                                self.apiClient,
                                                id=self.volume.id,
                                                type='DATADISK'
                                                )
            for vol in list_volume_response:
                if vol.id == self.volume.id and vol.size != currentSize and vol.state != "Resizing":
                    success = False
            if success:
                break
            else:
                time.sleep(1)
                count += 1

        self.assertEqual(
                         success,
                         True,
                         "Verify the volume did not resize"
                         )
        self.virtual_machine.detach_volume(self.apiClient, self.volume)
        self.cleanup.append(self.volume)

    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_08_resize_volume(self):
        """Resize a volume"""
        # Verify the size is the new size is what we wanted it to be.
        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume.id,
                                                    self.virtual_machine.id
                                                    ))
        self.virtual_machine.attach_volume(self.apiClient, self.volume)
        if self.services['hypervisor'].lower() == "xenserver":
            self.virtual_machine.stop(self.apiClient)
        self.debug("Resize Volume ID: %s" % self.volume.id)

        cmd                = resizeVolume.resizeVolumeCmd()
        cmd.id             = self.volume.id
        cmd.diskofferingid = self.services['resizeddiskofferingid']

        self.apiClient.resizeVolume(cmd)

        count = 0
        success = False
        while count < 3:
            list_volume_response = list_volumes(
                                                self.apiClient,
                                                id=self.volume.id,
                                                type='DATADISK'
                                                )
            for vol in list_volume_response:
                if vol.id == self.volume.id and vol.size == 3221225472L:
                    success = True
            if success:
                break
            else:
                time.sleep(10)
                count += 1

        self.assertEqual(
                         success,
                         True,
                         "Check if the volume resized appropriately"
                         )

        self.virtual_machine.detach_volume(self.apiClient, self.volume)
        self.cleanup.append(self.volume)

    @attr(tags = ["advanced", "advancedns", "smoke"])
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

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        self.apiClient.deleteVolume(cmd)

        list_volume_response = list_volumes(
                                            self.apiClient,
                                            id=self.volume.id,
                                            type='DATADISK'
                                            )
        self.assertEqual(
                        list_volume_response,
                        None,
                        "Check if volume exists in ListVolumes"
                    )
        return
