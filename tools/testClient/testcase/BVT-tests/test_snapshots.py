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
#Import System modules

services = TEST_SNAPSHOT_SERVICES

MOUNT_DIR = "/mnt/tmp"
SUB_DIR = "test"
SUB_LVL_DIR1 = "test1"
SUB_LVL_DIR2 = "test2"
RANDOM_DATA = "random.data"

class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.virtual_machine = cls.virtual_machine_with_disk = VirtualMachine.create(cls.api_client, services["server_with_disk"])
        cls.virtual_machine_without_disk = VirtualMachine.create(cls.api_client, services["server_without_disk"])

    @classmethod
    def tearDownClass(cls):
#        cls.virtual_machine.delete(cls.api_client)
#        cls.virtual_machine_without_disk.delete(cls.api_client)
        return
 
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = { 'virtual_machine': [],
                         'snapshot' : [],
                         'volume' : []
                        }

    def tearDown(self):
        #Clean up, terminate the created instance, volumes and snapshots
#        cleanup_resources(self.apiclient, self.cleanup)
        return


#    def test_01_snapshot_root_disk(self):
#        """Test Snapshot Root Disk
#        """     
#        cmd = listVolumes.listVolumesCmd()
#        cmd.virtualmachineid = self.virtual_machine_with_disk.id
#        cmd.type = 'ROOT'
#        
#        volumes = self.apiclient.listVolumes(cmd)
#        snapshot = Snapshot.create(self.apiclient, volumes[0].id)
#
#        cmd = listSnapshots.listSnapshotsCmd()
#        cmd.id = snapshot.id
#        list_snapshots = self.apiclient.listSnapshots(cmd)
#
#        self.assertNotEqual(list_snapshots, None, "Check if result exists in list item call")
#        
#        self.assertEqual(
#                            list_snapshots[0].id,
#                            snapshot.id,
#                            "Check resource id in list resources call"
#                        )
#
#    def test_02_snapshot_data_disk(self):
#        """Test Snapshot Data Disk
#        """     
#
#        cmd = listVolumes.listVolumesCmd()
#        cmd.virtualmachineid = self.virtual_machine_with_disk.id
#        cmd.type = 'DATADISK'
#        
#        volume = self.apiclient.listVolumes(cmd)
#        snapshot = Snapshot.create(self.apiclient, volume[0].id)
#
#        cmd = listSnapshots.listSnapshotsCmd()
#        cmd.id = snapshot.id
#        list_snapshots = self.apiclient.listSnapshots(cmd)
#
#        self.assertNotEqual(list_snapshots, None, "Check if result exists in list item call")
#        
#        self.assertEqual(
#                            list_snapshots[0].id,
#                            snapshot.id,
#                            "Check resource id in list resources call"
#                        )
#
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
        
        ssh_client = self.virtual_machine.get_ssh_client()
        format_volume_to_ext3(ssh_client, services["diskdevice"])
        cmds = [    "mkdir -p %s" %MOUNT_DIR,
                    "mount %s1 %s" %(services["diskdevice"], MOUNT_DIR),
                    "pushd %s" %MOUNT_DIR,
                    "mkdir -p %s/{%s,%s} " %(SUB_DIR, SUB_LVL_DIR1, SUB_LVL_DIR2),
                    "echo %s > %s/%s/%s" %(random_data_0, SUB_DIR, SUB_LVL_DIR1, RANDOM_DATA),
                    "echo %s > %s/%s/%s" %(random_data_1, SUB_DIR, SUB_LVL_DIR1, RANDOM_DATA)
                ] 
        for c in cmds:
            self.debug(ssh_client.execute(c))
        
        cmd = listVolumes.listVolumesCmd()
        cmd.hostid = self.virtual_machine.id
        cmd.type = 'DATADISK'

        list_volume_response = self.apiclient.listVolumes(cmd)
        volume = list_volume_response[0]
        
        snapshot = Snapshot.create(self.apiclient, volume.id) 
        self.cleanup['snapshot'].append(snapshot)
        volume = Volume.create_from_snapshot(self.apiclient, snapshot.id, services)
        self.cleanup['volume'].append(volume)
        
        cmd = listVolumes.listVolumesCmd()
        cmd.id = volume.id
        list_volumes = self.apiclient.listVolumes(cmd)
        
        assertNotEqual(
                            len(list_volumes),
                            None,
                            "Check Volume list Length"
                      )
    
        assertEqual (
                        list_volumes[0].id,
                        volume.id,
                        "Check Volume in the List Volumes"
                    )

        new_virtual_machine = self.virtual_machine_without_disk
        self.cleanup['virtual_machine'].append(new_virtual_machine)
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = self.volume.id
        cmd.virtualmachineid = deploy_new_virtual_machine.id

        volume = self.apiclient.attachVolume(cmd)
        ssh = virtual_machine.get_ssh_client()
        cmds = [
                    "mkdir %s"  %MOUNT_DIR,
                    "mount %s1 %s" %(services["diskdevice"], MOUNT_DIR)
               ]

        for c in cmds:
            self.debug(ssh.execute(c))

        returned_data_0 = ssh.execute("cat %s/%s/%s/%s" %(MOUNT_DIR, SUB_DIR, SUB_LVL_DIR1, RANDOM_DATA))
        returned_data_1 = ssh.execute("cat %s/%s/%s/%s" %(MOUNT_DIR, SUB_DIR, SUB_LVL_DIR2, RANDOM_DATA))
        self.assertEqual(random_data_0, returned_data_0, "Verify newly attached volume contents with existing one")
        self.assertEqual(random_data_1, returned_data_1, "Verify newly attached volume contents with existing one")
        return
    
#    def test_04_delete_snapshot(self):
#        """Test Delete Snapshot
#        """
#    
#        cmd = listVolumes.listVolumesCmd()
#        cmd.hostid = self.virtual_machine.id
#        cmd.type = 'DATADISK'
#        list_volumes = self.apiclient.listVolumes(cmd)
#            
#        cmd = listSnapshots.listSnapshotsCmd()
#        cmd.id = list_volumes[0].id
#        list_snapshots = self.apiclient.listSnapshots(cmd)
#
#        snapshot = Snapshot.create(self.apiclient,list_volumes[0].id)
#        snapshot.delete(self.apiclient)
#        #Sleep to ensure all database records are updated
#        time.sleep(60)
#        cmd = listSnapshots.listSnapshotsCmd()
#        cmd.id = snapshot.id
#        list_snapshots = self.apiclient.listSnapshots(cmd)
#        
#        self.assertEqual(list_snapshots, None, "Check if result exists in list item call")
# 
#    def test_05_recurring_snapshot_root_disk(self):
#        """Test Recurring Snapshot Root Disk
#        """     
#        
#        cmd = listVolumes.listVolumesCmd()
#        cmd.virtualmachineid = self.virtual_machine_with_disk.id
#        cmd.type = 'ROOT'
#        
#        volume = self.apiclient.listVolumes(cmd)
#        
#        cmd = createSnapshotPolicy.createSnapshotPolicyCmd()
#        cmd.intervaltype=services["recurring_snapshot"]["intervaltype"]
#        cmd.maxsnaps=services["recurring_snapshot"]["maxsnaps"]
#        cmd.schedule=services["recurring_snapshot"]["schedule"]
#        cmd.timezone=services["recurring_snapshot"]["timezone"]
#        cmd.volumeid=volume[0].id
#        recurring_snapshot = self.apiclient.createSnapshotPolicy(cmd)
#        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
#        cmd.id = recurring_snapshot.id
#        cmd.volumeid=volume[0].id
#        list_snapshots = self.apiclient.listSnapshotPolicies(cmd)
#
#        self.assertNotEqual(list_snapshots, None, "Check if result exists in list item call")
#        
#        self.assertEqual(
#                            list_snapshots[0].id,
#                            recurring_snapshot.id,
#                            "Check recurring snapshot id in list resources call"
#                        )
#        self.assertEqual(
#                            list_snapshots[0].maxsnaps,
#                            services["recurring_snapshot"]["maxsnaps"],
#                            "Check interval type in list resources call"
#                        )
#        
#        #Sleep for 9 hours to check only 8 snapshots are retained
#        time.sleep(32400)
#        cmd = listSnapshots.listSnapshotsCmd()
#        cmd.volumeid=volume.id
#        cmd.intervaltype = services["recurring_snapshot"]["intervaltype"]
#        cmd.snapshottype = 'RECURRING'
#        
#        list_snapshots = self.apiclient.listSnapshots(cmd)
#        
#        self.assertEqual(len(list_snapshots),8, "Check maximum number of recurring snapshots retained")
#        
#    def test_06_recurring_snapshot_data_disk(self):
#        """Test Recurring Snapshot data Disk
#        """     
#        
#        cmd = listVolumes.listVolumesCmd()
#        cmd.virtualmachineid = self.virtual_machine_with_disk.id
#        cmd.type = 'DATADISK'
#        
#        volume = self.apiclient.listVolumes(cmd)
#        
#        cmd = createSnapshotPolicy.createSnapshotPolicyCmd()
#        cmd.intervaltype=services["recurring_snapshot"]["intervaltype"]
#        cmd.maxsnaps=services["recurring_snapshot"]["maxsnaps"]
#        cmd.schedule=services["recurring_snapshot"]["schedule"]
#        cmd.timezone=services["recurring_snapshot"]["timezone"]
#        cmd.volumeid=volume[0].id
#        recurring_snapshot = self.apiclient.createSnapshotPolicy(cmd)
#        
#        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
#        cmd.id = recurring_snapshot.id
#        cmd.volumeid=volume[0].id
#        list_snapshots = self.apiclient.listSnapshotPolicies(cmd)
#
#        self.assertNotEqual(list_snapshots, None, "Check if result exists in list item call")
#        
#        self.assertEqual(
#                            list_snapshots[0].id,
#                            recurring_snapshot.id,
#                            "Check recurring snapshot id in list resources call"
#                        )
#        self.assertEqual(
#                            list_snapshots[0].maxsnaps,
#                            services["recurring_snapshot"]["maxsnaps"],
#                            "Check interval type in list resources call"
#                        )
#        
#        #Sleep for 9 hours to check only 8 snapshots are retained
#        time.sleep(32400)
#        cmd = listSnapshots.listSnapshotsCmd()
#        cmd.volumeid=volume.id
#        cmd.intervaltype = services["recurring_snapshot"]["intervaltype"]
#        cmd.snapshottype = 'RECURRING'
#        
#        list_snapshots = self.apiclient.listSnapshots(cmd)
#        
#        self.assertEqual(len(list_snapshots),8, "Check maximum number of recurring snapshots retained")
