# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Snapshots
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from utils import *
from base import *
import remoteSSHClient

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
                        "server_with_disk":
                                    {
                                        "displayname": "testserver",
                                        "username": "root",
                                        "password": "password",
                                        "ssh_port": 22,
                                        "hypervisor": 'XenServer',
                                        "domainid": 1,
                                        "privateport": 22,
                                        "publicport": 22,
                                        "protocol": 'TCP',
                                },

                        "server_without_disk":
                                    {
                                        "displayname": "testserver",
                                        "username": "root",
                                        "password": "password",
                                        "ssh_port": 22,
                                        "hypervisor": 'XenServer',
                                        "domainid": 1,
                                        "privateport": 22,
                                        # For NAT rule creation
                                        "publicport": 22,
                                        "protocol": 'TCP',
                                },

                            "recurring_snapshot":
                                    {
                                     "intervaltype": 'HOURLY',
                                     # Frequency of snapshots
                                     "maxsnaps": 2, # Should be min 2
                                     "schedule": 1,
                                     "timezone": 'US/Arizona',
                                     # Timezone Formats - http://cloud.mindtouch.us/CloudStack_Documentation/Developer's_Guide%3A_CloudStack 
                                },

                            "templates":
                                {
                                    "displaytext": 'Template from snapshot',
                                    "name": 'Template from snapshot',
                                    "ostypeid": 12,
                                    "templatefilter": 'self',
                                },
                            "ostypeid": 12,
                            # Cent OS 5.3 (64 bit)
                            "diskdevice": "/dev/xvda",
                            "diskname": "TestDiskServ",
                            "size": 1, # GBs
                            "domainid": 1,
                            "mount_dir": "/mnt/tmp",
                            "sub_dir": "test",
                            "sub_lvl_dir1": "test1",
                            "sub_lvl_dir2": "test2",
                            "random_data": "random.data",
                            "exportpath": 'SecStorage',
                            "sec_storage": '192.168.100.131',
                            # IP address of Sec storage where snapshots are stored
                            "mgmt_server_ip": '192.168.100.154',
                            # Required for verifying snapshots on secondary storage 
                            "username": "root",
                            "password": "password",
                            "ssh_port": 22,
                         }

class TestSnapshots(cloudstackTestCase):

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
        cls.services["server_with_disk"]["zoneid"] = cls.zone.id
        cls.services["server_with_disk"]["diskoffering"] = cls.disk_offering.id

        cls.services["server_without_disk"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskoffering"] = cls.disk_offering.id

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
        cls.virtual_machine = cls.virtual_machine_with_disk = \
                    VirtualMachine.create(
                                cls.api_client,
                                cls.services["server_with_disk"],
                                templateid=template.id,
                                accountid=cls.account.account.name,
                                serviceofferingid=cls.service_offering.id
                                )
        cls.virtual_machine_without_disk = \
                    VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server_without_disk"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    serviceofferingid=cls.service_offering.id
                                    )

        cls.public_ip = PublicIPAddress.create(
                                           cls.api_client,
                                           cls.virtual_machine.account,
                                           cls.virtual_machine.zoneid,
                                           cls.virtual_machine.domainid,
                                           cls.services["server_with_disk"]
                                           )
        cls.nat_rule = NATRule.create(
                                    cls.api_client,
                                    cls.virtual_machine,
                                    cls.services["server_with_disk"],
                                    ipaddressid=cls.public_ip.ipaddress.id
                                    )
        cls._cleanup = [
                        cls.virtual_machine,
                        cls.nat_rule,
                        cls.public_ip,
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
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
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def test_01_snapshot_root_disk(self):
        """Test Snapshot Root Disk
        """

        # Validate the following
        # 1. listSnapshots should list the snapshot that was created.
        # 2. verify that secondary storage NFS share contains
        #    the reqd volume under
        #    /secondary/snapshots/$volumeid/$snapshot_uuid
        # 3. verify backup_snap_id was non null in the `snapshots` table

        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = self.virtual_machine_with_disk.id
        cmd.type = 'ROOT'
        volumes = self.apiclient.listVolumes(cmd)
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)
        self.cleanup.append(snapshot)

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.id = snapshot.id
        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertNotEqual(
                            list_snapshots,
                            None,
                            "Check if result exists in list item call"
                            )
        self.assertEqual(
                            list_snapshots[0].id,
                            snapshot.id,
                            "Check resource id in list resources call"
                        )
        self.debug(
            "select backup_snap_id, account_id, volume_id, path from snapshots where id = %s;" \
            % snapshot.id
            )
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id, path from snapshots where id = %s;" \
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

        # Login to management server to check snapshot present on sec disk
        ssh_client = self.virtual_machine_without_disk.get_ssh_client(
                                                        self.nat_rule.ipaddress
                                                        )
        cmds = [    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s:/%s %s" % (
                                         self.services["sec_storage"],
                                         self.services["exportpath"],
                                         self.services["mount_dir"]
                                         ),
                    "ls %s/snapshots/%s/%s" % (
                                               self.services["mount_dir"],
                                               qresult[1],
                                               qresult[2]
                                               ),
                ]

        for c in cmds:
            result = ssh_client.execute(c)

        self.assertEqual(
                        result[0],
                        str(qresult[3]) + ".vhd",
                        "Check snapshot UUID in secondary storage and database"
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

        self.assertNotEqual(
                            list_snapshots,
                            None,
                            "Check if result exists in list item call"
                            )
        self.assertEqual(
                            list_snapshots[0].id,
                            snapshot.id,
                            "Check resource id in list resources call"
                        )
        self.debug(
            "select backup_snap_id, account_id, volume_id, path from snapshots where id = %s;" \
            % snapshot.id
            )
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id, backup_snap_id from snapshots where id = %s;" \
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

        # Login to management server to check snapshot present on sec disk
        ssh_client = self.virtual_machine_with_disk.get_ssh_client(
                                                        self.nat_rule.ipaddress
                                                        )
        cmds = [    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s:/%s %s" % (
                                         self.services["sec_storage"],
                                         self.services["exportpath"],
                                         self.services["mount_dir"]
                                         ),
                    "ls %s/snapshots/%s/%s" % (
                                               self.services["mount_dir"],
                                               qresult[1],
                                               qresult[2]
                                               ),
                ]
        for c in cmds:
            result = ssh_client.execute(c)

        self.assertEqual(
                            result[0],
                            str(qresult[3]) + ".vhd",
                            "Check snapshot UUID in sec storage and database"
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

        ssh_client = self.virtual_machine.get_ssh_client(
                                                self.nat_rule.ipaddress
                                                )
        #Format partition using ext3
        format_volume_to_ext3(
                              ssh_client,
                              self.services["diskdevice"]
                              )
        cmds = [    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["diskdevice"],
                                      self.services["mount_dir"]
                                      ),
                    "pushd %s" % self.services["mount_dir"],
                    "mkdir -p %s/{%s,%s} " % (
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["sub_lvl_dir2"]
                                            ),
                    "echo %s > %s/%s/%s" % (
                                               random_data_0,
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                            ),
                    "echo %s > %s/%s/%s" % (
                                                random_data_1,
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
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
        volume = Volume.create_from_snapshot(
                                             self.apiclient,
                                             snapshot.id,
                                             self.services
                                             )
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
        ssh = new_virtual_machine.get_ssh_client(self.nat_rule.ipaddress)
        cmds = [
                    "mkdir %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["diskdevice"],
                                      self.services["mount_dir"]
                                      )
               ]

        for c in cmds:
            self.debug(ssh.execute(c))

        returned_data_0 = ssh.execute("cat %s/%s/%s" % (
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                    ))

        returned_data_1 = ssh.execute("cat %s/%s/%s" % (
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                    ))
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

        snapshot = Snapshot.create(self.apiclient, list_volumes[0].id)
        snapshot.delete(self.apiclient)
        #Sleep to ensure all database records are updated
        time.sleep(60)
        cmd = listSnapshots.listSnapshotsCmd()
        cmd.id = snapshot.id
        list_snapshots = self.apiclient.listSnapshots(cmd)
        self.assertEqual(
                         list_snapshots,
                         None,
                         "Check if result exists in list item call"
                         )
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
        recurring_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           volume[0].id,
                                           self.services["recurring_snapshot"]
                                        )
        self.cleanup.append(recurring_snapshot)
        #ListSnapshotPolicy should return newly created policy
        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
        cmd.id = recurring_snapshot.id
        cmd.volumeid = volume[0].id
        list_snapshots_policy = self.apiclient.listSnapshotPolicies(cmd)

        self.assertNotEqual(
                            list_snapshots_policy,
                            None,
                            "Check if result exists in list item call"
                            )
        snapshots_policy = list_snapshots_policy[0]
        self.assertEqual(
                        snapshots_policy.id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                        snapshots_policy.maxsnaps,
                        self.services["recurring_snapshot"]["maxsnaps"],
                        "Check interval type in list resources call"
                        )
        # Sleep for (maxsnaps+1) hours to verify
        # only maxsnaps snapshots are retained
        time.sleep(
            ((self.services["recurring_snapshot"]["maxsnaps"]) + 1) * 3600
            )
        cmd = listSnapshots.listSnapshotsCmd()
        cmd.volumeid = volume.id
        cmd.intervaltype = self.services["recurring_snapshot"]["intervaltype"]
        cmd.snapshottype = 'RECURRING'
        list_snapshots = self.apiclient.listSnapshots(cmd)
        self.assertEqual(
                         len(list_snapshots),
                         self.services["recurring_snapshot"]["maxsnaps"],
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
        recurring_snapshot = SnapshotPolicy.create(
                                    self.apiclient,
                                    volume[0].id,
                                    self.services["recurring_snapshot"]
                                )
        self.cleanup.append(recurring_snapshot)
        #ListSnapshotPolicy should return newly created policy
        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
        cmd.id = recurring_snapshot.id
        cmd.volumeid = volume[0].id
        list_snapshots_policy = self.apiclient.listSnapshotPolicies(cmd)

        self.assertNotEqual(
                            list_snapshots_policy,
                            None,
                            "Check if result exists in list item call"
                            )
        snapshots_policy = list_snapshots_policy[0]
        self.assertEqual(
                        snapshots_policy.id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                            snapshots_policy.maxsnaps,
                            self.services["recurring_snapshot"]["maxsnaps"],
                            "Check interval type in list resources call"
                        )

        # Sleep for (maxsnaps+1) hours to
        # verify only maxsnaps snapshots are retained
        time.sleep(
            ((self.services["recurring_snapshot"]["maxsnaps"]) + 1) * 3600
            )

        cmd = listSnapshots.listSnapshotsCmd()
        cmd.volumeid = volume.id
        cmd.intervaltype = self.services["recurring_snapshot"]["intervaltype"]
        cmd.snapshottype = 'RECURRING'

        list_snapshots = self.apiclient.listSnapshots(cmd)

        self.assertEqual(
                         len(list_snapshots),
                         self.services["recurring_snapshot"]["maxsnaps"],
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
        ssh_client = self.virtual_machine.get_ssh_client(
                                                        self.nat_rule.ipaddress
                                                        )

        cmds = [    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["diskdevice"],
                                      self.services["mount_dir"]
                                      ),
                    "pushd %s" % self.services["mount_dir"],
                    "mkdir -p %s/{%s,%s} " % (
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["sub_lvl_dir2"]
                                            ),
                    "echo %s > %s/%s/%s" % (
                                                random_data_0,
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                            ),
                    "echo %s > %s/%s/%s" % (
                                                random_data_1,
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
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
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["templates"]
                                    )

        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = self.services["templates"]["templatefilter"]
        cmd.id = template.id
        list_templates = self.apiclient.listTemplates(cmd)

        self.assertNotEqual(
                            list_templates,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                            list_templates[0].id,
                            template.id,
                            "Check new template id in list resources call"
                        )

        # Deploy new virtual machine using template
        new_virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server_without_disk"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    serviceofferingid=self.service_offering.id
                                    )
        self.cleanup.append(new_virtual_machine)

        #Login to VM & mount directory
        ssh = new_virtual_machine.get_ssh_client(self.nat_rule.ipaddress)
        cmds = [
                    "mkdir %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["diskdevice"],
                                      self.services["mount_dir"]
                                      )
               ]

        for c in cmds:
            ssh.execute(c)

        returned_data_0 = ssh.execute("cat %s/%s/%s" % (
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                    ))
        returned_data_1 = ssh.execute("cat %s/%s/%s" % (
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                    ))
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
        return
