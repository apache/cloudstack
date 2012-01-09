# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#
""" BVT tests for Snapshots
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
from utils import *
from base import *

services = TEST_SNAPSHOT_SERVICES

class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.virtual_machine = cls.virtual_machine_with_disk =\
                    VirtualMachine.create(cls.api_client, services["server_with_disk"])
        cls.virtual_machine_without_disk =\
                    VirtualMachine.create(cls.api_client, services["server_without_disk"])
        cls.nat_rule = NATRule.create(cls.api_client, cls.virtual_machine, services["server_with_disk"])
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.virtual_machine.delete(cls.api_client)
            cls.virtual_machine_without_disk.delete(cls.api_client)
            cls.nat_rule.delete(cls.api_client)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)

        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def test_01_snapshot_root_disk(self):
        """Test Snapshot Root Disk
        """
        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = self.virtual_machine_with_disk.id
        cmd.type = 'ROOT'

        volumes = self.apiclient.listVolumes(cmd)
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.id = snapshot.id
        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertNotEqual(list_snapshots, None, "Check if result exists in list item call")

        self.assertEqual(
                            list_snapshots[0].id,
                            snapshot.id,
                            "Check resource id in list resources call"
                        )
        self.debug("select backup_snap_id from snapshots where id = %s;" % snapshot.id)
        qresultset = self.dbclient.execute("select backup_snap_id from snapshots where id = %s;" % snapshot.id)
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


    def test_02_snapshot_data_disk(self):
        """Test Snapshot Data Disk
        """

        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = self.virtual_machine_with_disk.id
        cmd.type = 'DATADISK'

        volume = self.apiclient.listVolumes(cmd)
        snapshot = Snapshot.create(self.apiclient, volume[0].id)

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.id = snapshot.id
        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertNotEqual(list_snapshots, None, "Check if result exists in list item call")

        self.assertEqual(
                            list_snapshots[0].id,
                            snapshot.id,
                            "Check resource id in list resources call"
                        )

        self.debug("select backup_snap_id from snapshots where id = %s;" % snapshot.id)
        qresultset = self.dbclient.execute("select backup_snap_id from snapshots where id = %s;" % snapshot.id)
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

    def test_03_volume_from_snapshot(self):
        """Create volumes from snapshots
        """

        #1. Login to machine; create temp/test directories on data volume
        #2. Snapshot the Volume
        #3. Create another Volume from snapshot
        #4. Mount/Attach volume to another server
        #5. Compare data
        random_data_0 = random_gen(100)
        random_data_1 = random_gen(100)

        ssh_client = self.virtual_machine.get_ssh_client(services["server_with_disk"]["ipaddress"])

        #Format partition using ext3
        format_volume_to_ext3(ssh_client, services["diskdevice"])
        cmds = [    "mkdir -p %s" % services["mount_dir"],
                    "mount %s1 %s" %(services["diskdevice"], services["mount_dir"]),
                    "pushd %s" % services["mount_dir"],
                    "mkdir -p %s/{%s,%s} " %(
                                                services["sub_dir"],
                                                services["sub_lvl_dir1"],
                                                services["sub_lvl_dir2"]
                                            ),
                    "echo %s > %s/%s/%s" %(
                                                random_data_0,
                                                services["sub_dir"],
                                                services["sub_lvl_dir1"],
                                                services["random_data"]
                                            ),
                    "echo %s > %s/%s/%s" %(
                                                random_data_1,
                                                services["sub_dir"],
                                                services["sub_lvl_dir2"],
                                                services["random_data"]
                                            )
                ]
        for c in cmds:
            self.debug(ssh_client.execute(c))

        cmd = listVolumes.listVolumesCmd()
        cmd.hostid = self.virtual_machine.id
        cmd.type = 'DATADISK'

        list_volume_response = self.apiclient.listVolumes(cmd)
        volume = list_volume_response[0]

        #Create snapshot from attached volume
        snapshot = Snapshot.create(self.apiclient, volume.id)
        self.cleanup.append(snapshot)
        #Create volume from snapshot
        volume = Volume.create_from_snapshot(self.apiclient, snapshot.id, services)
        self.cleanup.append(volume)

        cmd = listVolumes.listVolumesCmd()
        cmd.id = volume.id
        list_volumes = self.apiclient.listVolumes(cmd)

        self.assertNotEqual(
                            len(list_volumes),
                            None,
                            "Check Volume list Length"
                      )

        self.assertEqual (
                        list_volumes[0].id,
                        volume.id,
                        "Check Volume in the List Volumes"
                    )

        #Attaching volume to new VM
        new_virtual_machine = self.virtual_machine_without_disk
        self.cleanup.append(new_virtual_machine)
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume.id
        cmd.virtualmachineid = new_virtual_machine.id

        volume = self.apiclient.attachVolume(cmd)

        #Login to VM to verify test directories and files
        ssh = new_virtual_machine.get_ssh_client(services["server_without_disk"]["ipaddress"])
        cmds = [
                    "mkdir %s"  %services["mount_dir"],
                    "mount %s1 %s" %(services["diskdevice"], services["mount_dir"])
               ]

        for c in cmds:
            self.debug(ssh.execute(c))

        returned_data_0 = ssh.execute("cat %s/%s/%s" %(
                                                        services["sub_dir"], 
                                                        services["sub_lvl_dir1"], 
                                                        services["random_data"]
                                                        )
                                    )

        returned_data_1 = ssh.execute("cat %s/%s/%s" %(
                                                        services["sub_dir"], 
                                                        services["sub_lvl_dir2"], 
                                                        services["random_data"]
                                    )               )
        #Verify returned data
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

        #detach volume for cleanup
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = volume.id
        self.apiclient.detachVolume(cmd)
        return

    def test_04_delete_snapshot(self):
        """Test Delete Snapshot
        """

        cmd = listVolumes.listVolumesCmd()
        cmd.hostid = self.virtual_machine.id
        cmd.type = 'DATADISK'
        list_volumes = self.apiclient.listVolumes(cmd)

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.id = list_volumes[0].id
        list_snapshots = self.apiclient.listSnapshots(cmd)

        snapshot = Snapshot.create(self.apiclient,list_volumes[0].id)
        snapshot.delete(self.apiclient)
        #Sleep to ensure all database records are updated
        time.sleep(60)
        cmd = listSnapshots.listSnapshotsCmd()
        cmd.id = snapshot.id
        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertEqual(list_snapshots, None, "Check if result exists in list item call")
        return

    def test_05_recurring_snapshot_root_disk(self):
        """Test Recurring Snapshot Root Disk
        """
        #1. Create snapshot policy for root disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = self.virtual_machine_with_disk.id
        cmd.type = 'ROOT'

        volume = self.apiclient.listVolumes(cmd)

        recurring_snapshot = SnapshotPolicy.create(self.apiclient, volume[0].id, services["recurring_snapshot"])
        self.cleanup.append(recurring_snapshot)

        #ListSnapshotPolicy should return newly created policy
        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
        cmd.id = recurring_snapshot.id
        cmd.volumeid=volume[0].id
        list_snapshots_policy = self.apiclient.listSnapshotPolicies(cmd)

        self.assertNotEqual(list_snapshots_policy, None, "Check if result exists in list item call")

        snapshots_policy = list_snapshots_policy[0]
        self.assertEqual(
                            snapshots_policy.id,
                            recurring_snapshot.id,
                            "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                            snapshots_policy.maxsnaps,
                            services["recurring_snapshot"]["maxsnaps"],
                            "Check interval type in list resources call"
                        )

        #Sleep for (maxsnaps+1) hours to verify only maxsnaps snapshots are retained
        time.sleep(((services["recurring_snapshot"]["maxsnaps"])+1)*3600)

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.volumeid=volume.id
        cmd.intervaltype = services["recurring_snapshot"]["intervaltype"]
        cmd.snapshottype = 'RECURRING'

        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertEqual(
                         len(list_snapshots),
                         services["recurring_snapshot"]["maxsnaps"],
                         "Check maximum number of recurring snapshots retained"
                        )
        return

    def test_06_recurring_snapshot_data_disk(self):
        """Test Recurring Snapshot data Disk
        """

        #1. Create snapshot policy for data disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = self.virtual_machine_with_disk.id
        cmd.type = 'DATADISK'

        volume = self.apiclient.listVolumes(cmd)

        recurring_snapshot = SnapshotPolicy.create(self.apiclient, volume[0].id, services["recurring_snapshot"])
        self.cleanup.append(recurring_snapshot)

        #ListSnapshotPolicy should return newly created policy
        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
        cmd.id = recurring_snapshot.id
        cmd.volumeid=volume[0].id
        list_snapshots_policy = self.apiclient.listSnapshotPolicies(cmd)

        self.assertNotEqual(list_snapshots_policy, None, "Check if result exists in list item call")

        snapshots_policy = list_snapshots_policy[0]
        self.assertEqual(
                            snapshots_policy.id,
                            recurring_snapshot.id,
                            "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                            snapshots_policy.maxsnaps,
                            services["recurring_snapshot"]["maxsnaps"],
                            "Check interval type in list resources call"
                        )

        #Sleep for (maxsnaps+1) hours to verify only maxsnaps snapshots are retained
        time.sleep(((services["recurring_snapshot"]["maxsnaps"])+1)*3600)

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.volumeid=volume.id
        cmd.intervaltype = services["recurring_snapshot"]["intervaltype"]
        cmd.snapshottype = 'RECURRING'

        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertEqual(
                         len(list_snapshots),
                         services["recurring_snapshot"]["maxsnaps"],
                         "Check maximum number of recurring snapshots retained"
                         )
        return

    def test_07_template_from_snapshot(self):
        """Create Template from snapshot
        """

        #1. Login to machine; create temp/test directories on data volume
        #2. Snapshot the Volume
        #3. Create Template from snapshot
        #4. Deploy Virtual machine using this template
        #5. Login to newly created virtual machine
        #6. Compare data

        random_data_0 = random_gen(100)
        random_data_1 = random_gen(100)

        #Login to virtual machine
        ssh_client = self.virtual_machine.get_ssh_client(services["server_with_disk"]["ipaddress"])

        cmds = [    "mkdir -p %s" % services["mount_dir"],
                    "mount %s1 %s" %(services["diskdevice"], services["mount_dir"]),
                    "pushd %s" % services["mount_dir"],
                    "mkdir -p %s/{%s,%s} " %(
                                                services["sub_dir"], 
                                                services["sub_lvl_dir1"], 
                                                services["sub_lvl_dir2"]
                                            ),
                    "echo %s > %s/%s/%s" %(
                                                random_data_0, 
                                                services["sub_dir"], 
                                                services["sub_lvl_dir1"], 
                                                services["random_data"]
                                            ),
                    "echo %s > %s/%s/%s" %(
                                                random_data_1, 
                                                services["sub_dir"], 
                                                services["sub_lvl_dir2"], 
                                                services["random_data"]
                                        )
                ]
        for c in cmds:
            ssh_client.execute(c)

        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.type = 'ROOT'

        volume = self.apiclient.listVolumes(cmd)[0]
        #Create a snapshot of volume
        snapshot = Snapshot.create(self.apiclient, volume.id)
        self.cleanup.append(snapshot)

        # Generate template from the snapshot
        template = Template.create_from_snapshot(self.apiclient, snapshot, services["templates"])

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = services["templates"]["templatefilter"]
        cmd.id= template.id
        list_templates = self.apiclient.listTemplates(cmd)

        self.assertNotEqual(list_templates, None, "Check if result exists in list item call")

        self.assertEqual(
                            list_templates[0].id,
                            template.id,
                            "Check new template id in list resources call"
                        )

        # Deploy new virtual machine using template
        new_virtual_machine =  VirtualMachine.create(
                                                        self.apiclient, 
                                                        services["server_without_disk"], 
                                                        template.id
                                                    )
        self.cleanup.append(new_virtual_machine)
        #Login to VM & mount directory
        ssh = new_virtual_machine.get_ssh_client(services["server_without_disk"]["ipaddress"])
        cmds = [
                    "mkdir %s"  %services["mount_dir"],
                    "mount %s1 %s" %(services["diskdevice"], services["mount_dir"])
               ]

        for c in cmds:
            ssh.execute(c)

        returned_data_0 = ssh.execute("cat %s/%s/%s" %(
                                                        services["sub_dir"], 
                                                        services["sub_lvl_dir1"], 
                                                        services["random_data"]
                                    )               )
        returned_data_1 = ssh.execute("cat %s/%s/%s" %(
                                                        services["sub_dir"], 
                                                        services["sub_lvl_dir2"], 
                                                        services["random_data"]
                                    )               )
        #Verify returned data
        self.assertEqual(random_data_0, returned_data_0[0], "Verify newly attached volume contents with existing one")
        self.assertEqual(random_data_1, returned_data_1[0], "Verify newly attached volume contents with existing one")

        return
