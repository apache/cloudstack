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
""" P1 tests for Snapshots
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase

from marvin.lib.base import (Snapshot,
                             Template,
                             VirtualMachine,
                             Account,
                             ServiceOffering,
                             DiskOffering,
                             Volume)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_events,
                               list_volumes,
                               list_snapshots,
                               list_templates,
                               list_virtual_machines,
                               )

from marvin.lib.utils import (cleanup_resources,
                              format_volume_to_ext3,
                              random_gen,
                              is_snapshot_on_nfs,
                              get_hypervisor_type)

from marvin.cloudstackAPI import detachVolume
import time


class Services:

    """Test Snapshots Services
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
                "cpuspeed": 200,    # in MHz
                                    "memory": 256,      # In MBs
            },
            "disk_offering": {
                "displaytext": "Small Disk",
                "name": "Small Disk",
                "disksize": 1
            },
            "server_with_disk":
            {
                "displayname": "Test VM -With Disk",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },

            "server_without_disk":
            {
                "displayname": "Test VM-No Disk",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                # For NAT rule creation
                "publicport": 22,
                "protocol": 'TCP',
            },
            "server": {
                "displayname": "TestVM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "recurring_snapshot": {
                "intervaltype": 'HOURLY',
                # Frequency of snapshots
                "maxsnaps": 1,  # Should be min 2
                "schedule": 1,
                "timezone": 'US/Arizona',
                # Timezone Formats -
                # http://cloud.mindtouch.us/CloudStack_Documentation/Developer's_Guide%3A_CloudStack
            },
            "templates": {
                "displaytext": 'Template',
                "name": 'Template',
                "ostype": "CentOS 5.3 (64-bit)",
                "templatefilter": 'self',
            },
            "volume": {
                "diskname": "APP Data Volume",
                "size": 1,   # in GBs
                "xenserver": {"rootdiskdevice": "/dev/xvda",
                              "datadiskdevice_1": '/dev/xvdb',
                              "datadiskdevice_2": '/dev/xvdc',   # Data Disk
                              },
                "kvm": {"rootdiskdevice": "/dev/vda",
                        "datadiskdevice_1": "/dev/vdb",
                        "datadiskdevice_2": "/dev/vdc"
                        },
                "vmware": {"rootdiskdevice": "/dev/hda",
                           "datadiskdevice_1": "/dev/hdb",
                           "datadiskdevice_2": "/dev/hdc"
                           }
            },
            "paths": {
                "mount_dir": "/mnt/tmp",
                "sub_dir": "test",
                "sub_lvl_dir1": "test1",
                "sub_lvl_dir2": "test2",
                "random_data": "random.data",
            },
            "ostype": "CentOS 5.3 (64-bit)",
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
        }


class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshots, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = str(get_hypervisor_type(cls.api_client)).lower()
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["domainid"] = cls.domain.id
        cls.services["volume"]["zoneid"] = cls.services[
            "server_with_disk"]["zoneid"] = cls.zone.id
        cls.services["server_with_disk"]["diskoffering"] = cls.disk_offering.id

        cls.services["server_without_disk"]["zoneid"] = cls.zone.id

        cls.services["templates"]["ostypeid"] = cls.template.ostypeid
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskoffering"] = cls.disk_offering.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestSnapshots, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor: %s" % self.hypervisor)

        # Create VMs, NAT Rules etc
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

        self.virtual_machine = self.virtual_machine_with_disk = \
            VirtualMachine.create(
                self.api_client,
                self.services["server_with_disk"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.services["mode"]
            )
        return

    def tearDown(self):
        super(TestSnapshots, self).tearDown()

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_snapshot_data_disk(self):
        """Test Snapshot Data Disk
        """
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        volume = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine_with_disk.id,
            type='DATADISK',
            listall=True
        )
        self.assertEqual(
            isinstance(volume, list),
            True,
            "Check list response returns a valid list"
        )

        self.debug("Creating a Snapshot from data volume: %s" % volume[0].id)
        snapshot = Snapshot.create(
            self.apiclient,
            volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list item call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check resource id in list resources call"
        )
        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snapshot.id))
        return

    # @attr(speed="slow")
    # @attr(
    #     tags=[
    #         "advanced",
    #         "advancedns",
    #         "basic",
    #         "sg"],
    #     required_hardware="true")
    @attr(tags=["TODO"], required_hardware="true")
    def test_01_volume_from_snapshot(self):
        """Test Creating snapshot from volume having spaces in name(KVM)
        # Validate the following
        # 1. Create a virtual machine and data volume
        # 2. Attach data volume to VM
        # 3. Login to machine; create temp/test directories on data volume
        #    and write some random data
        # 4. Snapshot the Volume
        # 5. Create another Volume from snapshot
        # 6. Mount/Attach volume to another virtual machine
        # 7. Compare data, data should match
        """

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        random_data_0 = random_gen(size=100)
        random_data_1 = random_gen(size=100)

        self.debug("random_data_0: %s" % random_data_0)
        self.debug("random_data_1: %s" % random_data_1)

        try:
            ssh_client = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for VM: %s" %
                      self.virtual_machine.ipaddress)

        volume = Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Created volume with ID: %s" % volume.id)
        self.virtual_machine.attach_volume(
            self.apiclient,
            volume
        )  # volume should be cleanup with `self.virtual_machine`
        self.debug("Attach volume: %s to VM: %s" %
                   (volume.id, self.virtual_machine.id))

        self.debug("Formatting volume: %s to ext3" % volume.id)
        # Format partition using ext3
        # Note that this is the second data disk partition of virtual machine
        # as it was already containing data disk before attaching the new
        # volume, Hence datadiskdevice_2
        format_volume_to_ext3(
            ssh_client,
            self.services["volume"][self.hypervisor]["datadiskdevice_2"]
        )
        cmds = [
            "fdisk -l",
            "mkdir -p %s" %
            self.services["paths"]["mount_dir"],
            "mount -t ext3 %s1 %s" %
            (self.services["volume"][
                self.hypervisor]["datadiskdevice_2"],
                self.services["paths"]["mount_dir"]),
            "mkdir -p %s/%s/{%s,%s} " %
            (self.services["paths"]["mount_dir"],
                self.services["paths"]["sub_dir"],
                self.services["paths"]["sub_lvl_dir1"],
                self.services["paths"]["sub_lvl_dir2"]),
            "echo %s > %s/%s/%s/%s" %
            (random_data_0,
                self.services["paths"]["mount_dir"],
                self.services["paths"]["sub_dir"],
                self.services["paths"]["sub_lvl_dir1"],
                self.services["paths"]["random_data"]),
            "echo %s > %s/%s/%s/%s" %
            (random_data_1,
                self.services["paths"]["mount_dir"],
                self.services["paths"]["sub_dir"],
                self.services["paths"]["sub_lvl_dir2"],
                self.services["paths"]["random_data"]),
            "cat %s/%s/%s/%s" %
            (self.services["paths"]["mount_dir"],
                self.services["paths"]["sub_dir"],
                self.services["paths"]["sub_lvl_dir1"],
                self.services["paths"]["random_data"])]
        for c in cmds:
            self.debug("Command: %s" % c)
            result = ssh_client.execute(c)
            self.debug(result)

        # Unmount the Sec Storage
        cmds = [
            "umount %s" % (self.services["paths"]["mount_dir"]),
        ]
        for c in cmds:
            self.debug("Command: %s" % c)
            ssh_client.execute(c)

        list_volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            id=volume.id
        )

        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volume response for valid data"
        )
        volume_response = list_volume_response[0]
        # Create snapshot from attached volume
        snapshot = Snapshot.create(
            self.apiclient,
            volume_response.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created snapshot: %s" % snapshot.id)
        # Create volume from snapshot
        volume_from_snapshot = Volume.create_from_snapshot(
            self.apiclient,
            snapshot.id,
            self.services["volume"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        # Detach the volume from virtual machine
        self.virtual_machine.detach_volume(
            self.apiclient,
            volume
        )
        self.debug("Detached volume: %s from VM: %s" %
                   (volume.id, self.virtual_machine.id))

        self.debug("Created Volume: %s from Snapshot: %s" % (
            volume_from_snapshot.id,
            snapshot.id))
        volumes = Volume.list(
            self.apiclient,
            id=volume_from_snapshot.id
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(volumes),
            None,
            "Check Volume list Length"
        )
        self.assertEqual(
            volumes[0].id,
            volume_from_snapshot.id,
            "Check Volume in the List Volumes"
        )
        # Attaching volume to new VM
        new_virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["server_without_disk"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.debug("Deployed new VM for account: %s" % self.account.name)
        self.cleanup.append(new_virtual_machine)

        self.debug("Attaching volume: %s to VM: %s" % (
            volume_from_snapshot.id,
            new_virtual_machine.id
        ))

        new_virtual_machine.attach_volume(
            self.apiclient,
            volume_from_snapshot
        )

        # Rebooting is required so that newly attached disks are detected
        self.debug("Rebooting : %s" % new_virtual_machine.id)

        new_virtual_machine.reboot(self.apiclient)

        try:
            # Login to VM to verify test directories and files
            ssh = new_virtual_machine.get_ssh_client()

            # Mount datadiskdevice_1 because this is the first data disk of the
            # new virtual machine
            cmds = [
                "fdisk -l",
                "mkdir -p %s" %
                self.services["paths"]["mount_dir"],
                "mount -t ext3 %s1 %s" %
                (self.services["volume"][
                    self.hypervisor]["datadiskdevice_1"],
                    self.services["paths"]["mount_dir"]),
            ]

            for c in cmds:
                self.debug("Command: %s" % c)
                result = ssh.execute(c)
                self.debug(result)

            returned_data_0 = ssh.execute(
                "cat %s/%s/%s/%s" % (
                    self.services["paths"]["mount_dir"],
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir1"],
                    self.services["paths"]["random_data"]
                ))
            returned_data_1 = ssh.execute(
                "cat %s/%s/%s/%s" % (
                    self.services["paths"]["mount_dir"],
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir2"],
                    self.services["paths"]["random_data"]
                ))
        except Exception as e:
            self.fail("SSH access failed for VM: %s, Exception: %s" %
                      (new_virtual_machine.ipaddress, e))

        self.debug("returned_data_0: %s" % returned_data_0[0])
        self.debug("returned_data_1: %s" % returned_data_1[0])

        # Verify returned data
        self.assertEqual(
            random_data_0,
            returned_data_0[0],
            "Verify newly attached volume contents with existing one"
        )
        self.assertEqual(
            random_data_1,
            returned_data_1[0],
            "Verify newly attached volume contents with existing one"
        )
        # Unmount the Sec Storage
        cmds = [
            "umount %s" % (self.services["paths"]["mount_dir"]),
        ]
        for c in cmds:
            self.debug("Command: %s" % c)
            ssh_client.execute(c)
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_delete_snapshot(self):
        """Test Delete Snapshot
        """

        # 1. Snapshot the Volume
        # 2. Delete the snapshot
        # 3. Verify snapshot is removed by calling List Snapshots API
        # 4. Verify snapshot was removed from image store

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        self.debug("Creating volume under account: %s" % self.account.name)
        volume = Volume.create(
            self.apiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Created volume: %s" % volume.id)
        self.debug("Attaching volume to vm: %s" % self.virtual_machine.id)

        self.virtual_machine.attach_volume(
            self.apiclient,
            volume
        )
        self.debug("Volume attached to vm")

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            id=volume.id
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )
        snapshot = Snapshot.create(
            self.apiclient,
            volumes[0].id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        snapshot.delete(self.apiclient)
        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id
        )
        self.assertEqual(
            snapshots,
            None,
            "Check if result exists in list item call"
        )
        self.assertFalse(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snapshot.id))
        return

    @attr(speed="slow")
    @attr(
        tags=[
            "advanced",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="true")
    def test_03_snapshot_detachedDisk(self):
        """Test snapshot from detached disk
        """
        # Validate the following
        # 1. login in VM  and write some data on data disk(use fdisk to
        #    partition datadisk,fdisk, and make filesystem using
        #    mkfs.ext3)
        # 2. Detach the data disk and write some data on data disk
        # 3. perform the snapshot on the detached volume
        # 4. listvolumes with VM id shouldn't show the detached volume
        # 5. listSnapshots should list the snapshot that was created
        # 6. verify backup_snap_id was non null in the `snapshots` table

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            listall=True
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )
        volume = volumes[0]
        random_data_0 = random_gen(size=100)
        random_data_1 = random_gen(size=100)
        try:
            ssh_client = self.virtual_machine.get_ssh_client()

            # Format partition using ext3
            format_volume_to_ext3(
                ssh_client,
                self.services["volume"][self.hypervisor]["datadiskdevice_1"]
            )
            cmds = [
                "mkdir -p %s" %
                self.services["paths"]["mount_dir"],
                "mount %s1 %s" %
                (self.services["volume"][
                    self.hypervisor]["datadiskdevice_1"],
                    self.services["paths"]["mount_dir"]),
                "pushd %s" %
                self.services["paths"]["mount_dir"],
                "mkdir -p %s/{%s,%s} " %
                (self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir1"],
                    self.services["paths"]["sub_lvl_dir2"]),
                "echo %s > %s/%s/%s" %
                (random_data_0,
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir1"],
                    self.services["paths"]["random_data"]),
                "echo %s > %s/%s/%s" %
                (random_data_1,
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir2"],
                    self.services["paths"]["random_data"]),
                "sync",
                "umount %s" %
                (self.services["paths"]["mount_dir"]),
            ]
            for c in cmds:
                self.debug(ssh_client.execute(c))

            # detach volume from VM
            cmd = detachVolume.detachVolumeCmd()
            cmd.id = volume.id
            self.apiclient.detachVolume(cmd)

            # Create snapshot from detached volume
            snapshot = Snapshot.create(self.apiclient, volume.id)

            volumes = list_volumes(
                self.apiclient,
                virtualmachineid=self.virtual_machine.id,
                type='DATADISK',
                listall=True
            )

            self.assertEqual(
                volumes,
                None,
                "Check Volume is detached"
            )

            # Verify the snapshot was created or not
            snapshots = list_snapshots(
                self.apiclient,
                id=snapshot.id
            )
            self.assertNotEqual(
                snapshots,
                None,
                "Check if result exists in list snapshots call"
            )
            self.assertEqual(
                snapshots[0].id,
                snapshot.id,
                "Check snapshot id in list resources call"
            )
        except Exception as e:
            self.fail("SSH failed for VM with IP: %s - %s" %
                      (self.virtual_machine.ssh_ip, e))

        qresultset = self.dbclient.execute(
            "select id from snapshots where uuid = '%s';"
            % snapshot.id
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = qresultset[0]
        self.assertNotEqual(
            str(qresult[0]),
            'NULL',
            "Check if backup_snap_id is not null"
        )
        return

    # @attr(speed="slow")
    # @attr(
    #     tags=[
    #         "advanced",
    #         "advancedns",
    #         "smoke",
    #         "xen"],
    #     required_hardware="true")
    @attr(tags=["TODO"], required_hardware="true")
    def test_07_template_from_snapshot(self):
        """Create Template from snapshot
        # 1. Login to machine; create temp/test directories on data volume
        # 2. Snapshot the Volume
        # 3. Create Template from snapshot
        # 4. Deploy Virtual machine using this template
        # 5. Login to newly created virtual machine
        # 6. Compare data in the root disk with the one that was written on the volume, it should match
        """

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)

        random_data_0 = random_gen(size=100)
        random_data_1 = random_gen(size=100)

        try:
            # Login to virtual machine
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "mkdir -p %s" % self.services["paths"]["mount_dir"],
                "mount %s1 %s" % (
                    self.services["volume"][self.hypervisor]["rootdiskdevice"],
                    self.services["paths"]["mount_dir"]
                ),
                "mkdir -p %s/%s/{%s,%s} " % (
                    self.services["paths"]["mount_dir"],
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir1"],
                    self.services["paths"]["sub_lvl_dir2"]
                ),
                "echo %s > %s/%s/%s/%s" % (
                    random_data_0,
                    self.services["paths"]["mount_dir"],
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir1"],
                    self.services["paths"]["random_data"]
                ),
                "echo %s > %s/%s/%s/%s" % (
                    random_data_1,
                    self.services["paths"]["mount_dir"],
                    self.services["paths"]["sub_dir"],
                    self.services["paths"]["sub_lvl_dir2"],
                    self.services["paths"]["random_data"]
                ),
                "sync",
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s" %
                      self.virtual_machine.ipaddress)

        # Unmount the Volume
        cmds = [
            "umount %s" % (self.services["paths"]["mount_dir"]),
        ]
        for c in cmds:
            self.debug(c)
            ssh_client.execute(c)

        volumes = list_volumes(
            userapiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )

        volume = volumes[0]

        # Create a snapshot of volume
        snapshot = Snapshot.create(
            userapiclient,
            volume.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(snapshot)

        self.debug("Snapshot created from volume ID: %s" % volume.id)
        # Generate template from the snapshot
        template = Template.create_from_snapshot(
            userapiclient,
            snapshot,
            self.services["templates"]
        )
        self.debug("Template created from snapshot ID: %s" % snapshot.id)

        # Verify created template
        templates = list_templates(
            userapiclient,
            templatefilter=self.services["templates"]["templatefilter"],
            id=template.id
        )
        self.assertNotEqual(
            templates,
            None,
            "Check if result exists in list item call"
        )

        self.assertEqual(
            templates[0].id,
            template.id,
            "Check new template id in list resources call"
        )
        self.debug("Deploying new VM from template: %s" % template.id)

        # Deploy new virtual machine using template
        new_virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["server_without_disk"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(new_virtual_machine)
        try:
            # Login to VM & mount directory
            ssh = new_virtual_machine.get_ssh_client()

            cmds = [
                "mkdir -p %s" % self.services["paths"]["mount_dir"],
                "mount %s1 %s" % (
                    self.services["volume"][self.hypervisor]["rootdiskdevice"],
                    self.services["paths"]["mount_dir"]
                )
            ]

            for c in cmds:
                ssh.execute(c)

            returned_data_0 = ssh.execute("cat %s/%s/%s/%s" % (
                self.services["paths"]["mount_dir"],
                self.services["paths"]["sub_dir"],
                self.services["paths"]["sub_lvl_dir1"],
                self.services["paths"]["random_data"]
            ))
            self.debug(returned_data_0)
            returned_data_1 = ssh.execute("cat %s/%s/%s/%s" % (
                self.services["paths"]["mount_dir"],
                self.services["paths"]["sub_dir"],
                self.services["paths"]["sub_lvl_dir2"],
                self.services["paths"]["random_data"]
            ))
            self.debug(returned_data_1)
        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s" %
                      new_virtual_machine.ipaddress)
        # Verify returned data
        self.assertEqual(
            random_data_0,
            returned_data_0[0],
            "Verify newly attached volume contents with existing one"
        )
        self.assertEqual(
            random_data_1,
            returned_data_1[0],
            "Verify newly attached volume contents with existing one"
        )
        # Unmount the volume
        cmds = [
            "umount %s" % (self.services["paths"]["mount_dir"]),
        ]
        try:
            for c in cmds:
                self.debug(c)
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s, Exception: %s" %
                      (new_virtual_machine.ipaddress, e))
        return


class TestCreateVMSnapshotTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestCreateVMSnapshotTemplate,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.unsupportedHypervisor = False
        cls.hypervisor = get_hypervisor_type(cls.api_client)
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["domainid"] = cls.domain.id
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestCreateVMSnapshotTemplate, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("snapshots are not supported on %s" % self.hypervisor.lower())
        return

    def tearDown(self):
        super(TestCreateVMSnapshotTemplate, self).tearDown()

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_createVM_snapshotTemplate(self):
        """Test create VM, Snapshot and Template
        """
        # Validate the following
        # 1. Deploy VM using default template, small service offering
        #    and small data disk offering.
        # 2. Perform snapshot on the root disk of this VM.
        # 3. Create a template from snapshot.
        # 4. Create a instance from above created template.
        # 5. listSnapshots should list the snapshot that was created.
        # 6. verify that secondary storage NFS share contains the reqd
        #    volume under /secondary/snapshots/$accountid/
        #    $volumeid/$snapshot_uuid
        # 7. verify backup_snap_id was non null in the `snapshots` table
        # 8. listTemplates() should return the newly created Template,
        #    and check for template state as READY"
        # 9. listVirtualMachines() command should return the deployed VM.
        #    State of this VM should be Running.

        # Create Virtual Machine

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)

        self.virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["server"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.virtual_machine)
        self.debug("Created VM with ID: %s" % self.virtual_machine.id)

        volumes = list_volumes(
            userapiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        volume = volumes[0]

        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(userapiclient, volume.id)
        self.debug("Snapshot created: ID - %s" % snapshot.id)
        self.cleanup.append(snapshot)

        snapshots = list_snapshots(
            userapiclient,
            id=snapshot.id
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list snapshots call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check snapshot id in list resources call"
        )
        self.debug(
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" %
            snapshot.id)
        snapshot_uuid = snapshot.id

        # Generate template from the snapshot
        template = Template.create_from_snapshot(
            userapiclient,
            snapshot,
            self.services["templates"]
        )
        self.cleanup.append(template)
        self.debug("Created template from snapshot: %s" % template.id)

        templates = list_templates(
            userapiclient,
            templatefilter=self.services["templates"]["templatefilter"],
            id=template.id
        )

        self.assertNotEqual(
            templates,
            None,
            "Check if result exists in list item call"
        )

        self.assertEqual(
            templates[0].isready,
            True,
            "Check new template state in list templates call"
        )

        # Deploy new virtual machine using template
        new_virtual_machine = VirtualMachine.create(
            userapiclient,
            self.services["server"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(new_virtual_machine)
        self.debug("Created VM with ID: %s from template: %s" % (
            new_virtual_machine.id,
            template.id
        ))

        # Newly deployed VM should be 'Running'
        virtual_machines = list_virtual_machines(
            userapiclient,
            id=new_virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(virtual_machines, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(virtual_machines),
            0,
            "Check list virtual machines response"
        )
        for virtual_machine in virtual_machines:
            self.assertEqual(
                virtual_machine.state,
                'Running',
                "Check list VM response for Running state"
            )
        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snapshot_uuid))
        return


class TestSnapshotEvents(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshotEvents, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services
        cls._cleanup = []
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.unsupportedHypervisor = False
        cls.hypervisor = get_hypervisor_type(cls.api_client)
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.virtual_machine)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestSnapshotEvents, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("snapshots are not supported on %s" % self.hypervisor)
        return

    def tearDown(self):
        super(TestSnapshotEvents, self).tearDown()

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_05_snapshot_events(self):
        """Test snapshot events
        """
        # Validate the following
        # 1. Perform snapshot on the root disk of this VM and
        #    check the events/alerts.
        # 2. delete the snapshots and check the events/alerts
        # 3. listEvents() shows created/deleted snapshot events

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        # Get the Root disk of VM
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )
        volume = volumes[0]

        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(self.apiclient, volume.id)
        self.debug("Snapshot created with ID: %s" % snapshot.id)

        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list snapshots call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check snapshot id in list resources call"
        )
        snapshot.delete(self.apiclient)

        # Sleep to ensure that snapshot is deleted properly
        time.sleep(self.services["sleep"])
        events = list_events(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            type='SNAPSHOT.DELETE'
        )
        self.assertEqual(
            isinstance(events, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            events,
            None,
            "Check if event exists in list events call"
        )
        self.assertIn(
            events[0].state,
            ['Completed', 'Scheduled'],
            "Check events state in list events call"
        )
        return
