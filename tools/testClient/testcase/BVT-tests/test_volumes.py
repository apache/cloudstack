# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Volumes
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
from utils import *
from base import *
#Import System modules
import os
import urllib2
import time
import tempfile

services = TEST_VOLUME_SERVICES

class TestCreateVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.virtual_machine = VirtualMachine.create(cls.api_client, services)
        cls.nat_rule = NATRule.create(cls.api_client, cls.virtual_machine, services)

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup= []

    def test_01_create_volume(self):
        """Test Volume creation for all Disk Offerings (incl. custom)
        """
        self.volumes = []
        for k,v in services["volume_offerings"].items():
            volume = Volume.create(self.apiClient, v)
            self.volumes.append(volume)
            self.cleanup.append(volume)

        self.volumes.append(Volume.create_custom_disk(self.apiClient, services))
        #Attach a volume with different disk offerings and check the memory allocated to each of them
        for volume in self.volumes:
            cmd = listVolumes.listVolumesCmd()
            cmd.id = volume.id
            list_volume_response = self.apiClient.listVolumes(cmd)

            self.assertNotEqual(list_volume_response, None, "Check if volume exists in ListVolumes")
            qresultset = self.dbclient.execute("select id from volumes where id = %s" %volume.id)
            self.assertNotEqual(len(qresultset), 0, "Check if volume exists in Database")
            attached_volume = self.virtual_machine.attach_volume(self.apiClient,volume)
            ssh = self.virtual_machine.get_ssh_client(services["ipaddress"])

            ssh.execute("shutdown -r now")
            #Sleep to ensure the machine is rebooted properly
            time.sleep(120)

            ssh = self.virtual_machine.get_ssh_client(services["ipaddress"], True)
            c = "fdisk -l|grep %s|head -1" % services["diskdevice"]
            res = ssh.execute(c)
            #Disk /dev/sda: 21.5 GB, 21474836480 bytes

            actual_disk_size = res[0].split()[4]

            self.assertEqual(str(list_volume_response[0].size), actual_disk_size, "Check if promised disk size actually available")
            self.virtual_machine.detach_volume(self.apiClient,volume)

    def tearDown(self):
        #Clean up, terminate the created templates
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.nat_rule.delete(cls.api_client)
            cls.virtual_machine.delete(cls.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)

class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.virtual_machine = VirtualMachine.create(cls.api_client, services)
        cls.volume = Volume.create(cls.api_client, services)

    @classmethod
    def tearDownClass(cls):
        try:
            cls.virtual_machine.delete(cls.api_client)
            cls.volume.delete(cls.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def test_02_attach_volume(self):
        """Attach a created Volume to a Running VM
        """
        self.virtual_machine.attach_volume(self.volume)

        #Sleep to ensure the current state will reflected in other calls
        time.sleep(60)
        cmd = listVolumes.listVolumesCmd()
        cmd.id = self.volume.id
        list_volume_response = self.apiClient.listVolumes(cmd)

        self.assertNotEqual(list_volume_response, None, "Check if volume exists in ListVolumes")
        volume = list_volume_response[0]
        self.assertNotEqual(volume.virtualmachineid, None, "Check if volume state (attached) is reflected")

        qresultset = self.dbclient.execute("select instance_id, device_id from volumes where id = %s" %self.volume.id)
        self.assertNotEqual(len(qresultset), 0, "Check if volume exists in Database")

        qresult = qresultset[0]
        self.assertEqual(qresult[0], self.virtual_machine.id, "Check if volume is assc. with virtual machine in Database")
        #self.assertEqual(qresult[1], 0, "Check if device is valid in the database")

        #Format the attached volume to a known fs
        format_volume_to_ext3(self.virtual_machine.get_ssh_client(services["ipaddress"]))

    def test_03_download_attached_volume(self):
        """Download a Volume attached to a VM
        """

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = services["zoneid"]
        #A proper exception should be raised; downloading attach VM is not allowed
        with self.assertRaises(Exception):
            self.apiClient.deleteVolume(cmd)

    def test_04_delete_attached_volume(self):
        """Delete a Volume attached to a VM
        """

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        #A proper exception should be raised; deleting attach VM is not allowed
        with self.assertRaises(Exception):
            self.apiClient.deleteVolume(cmd)


    def test_05_detach_volume(self):
        """Detach a Volume attached to a VM
        """
        self.virtual_machine.detach_volume(self.virtual_machine)
        #Sleep to ensure the current state will reflected in other calls
        time.sleep(60)
        cmd = listVolumes.listVolumesCmd()
        cmd.id = self.volume.id
        list_volume_response = self.apiClient.listVolumes(cmd)

        self.assertNotEqual(list_volume_response, None, "Check if volume exists in ListVolumes")
        volume = list_volume_response[0]
        self.assertEqual(volume.virtualmachineid, None, "Check if volume state (detached) is reflected")

        qresultset = self.dbclient.execute("select instance_id, device_id from volumes where id = %s" %self.volume.id)
        self.assertNotEqual(len(qresultset), 0, "Check if volume exists in Database")

        qresult = qresultset[0]
        self.assertEqual(qresult[0], None, "Check if volume is unassc. with virtual machine in Database")
        self.assertEqual(qresult[1], None, "Check if no device is valid in the database")


    def test_06_download_detached_volume(self):
        """Download a Volume unattached to an VM
        """

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.volume.id
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = services["zoneid"]
        extract_vol = self.apiClient.extractVolume(cmd)

        #Attempt to download the volume and save contents locally
        try:
            response = urllib2.urlopen(urllib2.unquote(extract_vol.url))
            fd, path = tempfile.mkstemp()
            os.close(fd)
            fd = open(path, 'wb')
            fd.write(response.read())
            fd.close()

        except Exception as e:
            print e
            self.fail("Extract Volume Failed with invalid URL %s (vol id: %s)" %(extract_vol.url, self.volume.id))

    def test_07_delete_detached_volume(self):
        """Delete a Volume unattached to an VM
        """

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        self.apiClient.deleteVolume(cmd)

        time.sleep(60)
        cmd = listVolumes.listVolumesCmd()
        cmd.id = self.volume.id
        cmd.type = 'DATADISK'

        list_volume_response = self.apiClient.listVolumes(cmd)
        self.assertEqual(list_volume_response, None, "Check if volume exists in ListVolumes")
