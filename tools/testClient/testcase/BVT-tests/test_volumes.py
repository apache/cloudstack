# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Volumes
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from utils import *
from base import *
import remoteSSHClient
#Import System modules
import os
import urllib
import time
import tempfile


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
                                    "password": "fr3sca",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 200, # in MHz
                                    "memory": 256, # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "disksize": 1
                        },
                        "volume_offerings": {
                            0: {
                                "diskname": "TestDiskServ",
                                "domainid": 1,
                            },
                        },
                            "customdisksize": 1, # GBs
                            "username": "root", # Creds for SSH to VM
                            "password": "password",
                            "ssh_port": 22,
                            "diskname": "TestDiskServ",
                            "hypervisor": 'XenServer',
                            "domainid": 1,
                            "privateport": 22,
                            "publicport": 22,
                            "protocol": 'TCP',
                            "diskdevice": "/dev/sda",
                            "ostypeid": 12,
                        }


class TestCreateVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client)
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
                            cls.services["ostypeid"]
                            )
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["customdiskofferingid"] = cls.custom_disk_offering.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True
                            )

        cls.services["account"] = cls.account.account.name
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services,
                                    accountid=cls.account.account.name,
                                    serviceofferingid=cls.service_offering.id
                                )

        cls.public_ip = PublicIPAddress.create(
                                           cls.api_client,
                                           cls.virtual_machine.account,
                                           cls.virtual_machine.zoneid,
                                           cls.virtual_machine.domainid,
                                           cls.services
                                           )
        cls.nat_rule = NATRule.create(
                                cls.api_client,
                                cls.virtual_machine,
                                cls.services,
                                ipaddressid=cls.public_ip.ipaddress.id
                                )
        cls._cleanup = [
                        cls.nat_rule,
                        cls.virtual_machine,
                        cls.service_offering,
                        cls.public_ip,
                        cls.disk_offering,
                        cls.custom_disk_offering,
                        cls.account
                        ]

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def test_01_create_volume(self):
        """Test Volume creation for all Disk Offerings (incl. custom)
        """
        self.volumes = []
        for k, v in self.services["volume_offerings"].items():
            volume = Volume.create(
                                   self.apiClient,
                                   v,
                                   zoneid=self.zone.id,
                                   account=self.account.account.name,
                                   diskofferingid=self.disk_offering.id
                                   )
            self.volumes.append(volume)
            self.cleanup.append(volume)

        volume = Volume.create_custom_disk(self.apiClient, self.services)
        self.volumes.append(volume)
        self.cleanup.append(volume)

        #Attach a volume with different disk offerings
        #and check the memory allocated to each of them
        for volume in self.volumes:
            cmd = listVolumes.listVolumesCmd()
            cmd.id = volume.id
            list_volume_response = self.apiClient.listVolumes(cmd)

            self.assertNotEqual(
                                list_volume_response,
                                None,
                                "Check if volume exists in ListVolumes"
                                )
            attached_volume = self.virtual_machine.attach_volume(
                                                        self.apiClient,
                                                        volume
                                                        )

            ssh = self.virtual_machine.get_ssh_client(self.nat_rule.ipaddress)

            ssh.execute("reboot")
            #Sleep to ensure the machine is rebooted properly
            time.sleep(120)
            ssh = self.virtual_machine.get_ssh_client(
                                                      self.nat_rule.ipaddress,
                                                      reconnect=True
                                                      )
            c = "fdisk -l|grep %s|head -1" % self.services["diskdevice"]
            res = ssh.execute(c)
            # Disk /dev/sda doesn't contain a valid partition table
            # Disk /dev/sda: 21.5 GB, 21474836480 bytes

            # Res may return more than one lines
            # Split res with space as delimiter to form new list (result)   
            result = []
            for i in res:
                for k in i.split():
                    result.append(k)

            self.assertEqual(
                             str(list_volume_response[0].size) in result,
                             True,
                             "Check if promised disk size actually available"
                             )
            self.virtual_machine.detach_volume(self.apiClient, volume)

    def tearDown(self):
        #Clean up, terminate the created templates
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client)
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["diskofferingid"] = cls.disk_offering.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True
                            )

        cls.services["account"] = cls.account.account.name
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                        )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services,
                                    accountid=cls.account.account.name,
                                    serviceofferingid=cls.service_offering.id
                                )

        cls.public_ip = PublicIPAddress.create(
                                           cls.api_client,
                                           cls.virtual_machine.account,
                                           cls.virtual_machine.zoneid,
                                           cls.virtual_machine.domainid,
                                           cls.services
                                           )
        cls.nat_rule = NATRule.create(
                                      cls.api_client,
                                      cls.virtual_machine,
                                      cls.services,
                                      ipaddressid=cls.public_ip.ipaddress.id
                                    )
        cls.volume = Volume.create(
                                   cls.api_client,
                                   cls.services
                                   )
        cls._cleanup = [
                        cls.nat_rule,
                        cls.virtual_machine,
                        cls.public_ip,
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

    def test_02_attach_volume(self):
        """Attach a created Volume to a Running VM
        """
        # Validate the following
        # 1. shows list of volumes
        # 2. "Attach Disk" pop-up box will display with list of  instances
        # 3. disk should be  attached to instance successfully

        self.virtual_machine.attach_volume(self.apiClient, self.volume)

        #Sleep to ensure the current state will reflected in other calls
        time.sleep(60)
        cmd = listVolumes.listVolumesCmd()
        cmd.id = self.volume.id
        list_volume_response = self.apiClient.listVolumes(cmd)

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

        #Format the attached volume to a known fs
        format_volume_to_ext3(
                        self.virtual_machine.get_ssh_client(
                                                self.nat_rule.ipaddress
                              ))

    def test_03_download_attached_volume(self):
        """Download a Volume attached to a VM
        """
        # Validate the following
        # 1. download volume will fail with proper error message
        #    "Failed - Invalid state of the volume with ID:
        #    It should be either detached or the VM should be in stopped state

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.services["zoneid"]
        # A proper exception should be raised;
        # downloading attach VM is not allowed
        with self.assertRaises(Exception):
            self.apiClient.deleteVolume(cmd)

    def test_04_delete_attached_volume(self):
        """Delete a Volume attached to a VM
        """

        # Validate the following
        # 1. delete volume will fail with proper error message
        #    "Failed - Invalid state of the volume with ID:
        #    It should be either detached or the VM should be in stopped state

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        #Proper exception should be raised; deleting attach VM is not allowed
        with self.assertRaises(Exception):
            self.apiClient.deleteVolume(cmd)


    def test_05_detach_volume(self):
        """Detach a Volume attached to a VM
        """

        # Validate the following
        # Data disk should be detached from instance and detached data disk
        # details should be updated properly

        self.virtual_machine.detach_volume(self.apiClient, self.volume)
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(60)
        cmd = listVolumes.listVolumesCmd()
        cmd.id = self.volume.id
        list_volume_response = self.apiClient.listVolumes(cmd)

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

    def test_06_download_detached_volume(self):
        """Download a Volume unattached to an VM
        """
        # Validate the following
        # 1. able to download the volume when its not attached to instance

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

        except Exception as e:
            print e
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)" \
                % (extract_vol.url, self.volume.id)
            )

    def test_07_delete_detached_volume(self):
        """Delete a Volume unattached to an VM
        """
        # Validate the following
        # 1. volume should be deleted successfully and listVolume should not
        #    contain the deleted volume details.
        # 2. "Delete Volume" menu item not shown under "Actions" menu.
        #    (UI should not allow  to delete the volume when it is attached
        #    to instance by hiding the menu Item)

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        self.apiClient.deleteVolume(cmd)

        time.sleep(60)
        cmd = listVolumes.listVolumesCmd()
        cmd.id = self.volume.id
        cmd.type = 'DATADISK'

        list_volume_response = self.apiClient.listVolumes(cmd)
        self.assertEqual(
                        list_volume_response,
                        None,
                        "Check if volume exists in ListVolumes"
                    )
