# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Snapshots
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *
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
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 64,       # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
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

                            "recurring_snapshot":
                                    {
                                     "intervaltype": 'HOURLY',
                                     # Frequency of snapshots
                                     "maxsnaps": 1,     # Should be min 2
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
                            "diskdevice": "/dev/xvdb",      # Data Disk
                            "rootdisk": "/dev/xvda",        # Root Disk

                            "diskname": "Test Disk",
                            "size": 1,          # GBs

                            "mount_dir": "/mnt/tmp",
                            "sub_dir": "test",
                            "sub_lvl_dir1": "test1",
                            "sub_lvl_dir2": "test2",
                            "random_data": "random.data",

                            "username": "root",
                            "password": "password",
                            "ssh_port": 22,
                            "sleep": 60,
                            "timeout": 10,
                            "mode": 'advanced',
                            # Networking mode, Advanced, Basic
                         }


class TestSnapshotRootDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["server_without_disk"]["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = cls.virtual_machine_with_disk = \
                    VirtualMachine.create(
                                cls.api_client,
                                cls.services["server_without_disk"],
                                templateid=template.id,
                                accountid=cls.account.account.name,
                                domainid=cls.account.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls._cleanup = [
                        cls.service_offering,
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
        #    /secondary/snapshots//$account_id/$volumeid/$snapshot_uuid
        # 3. verify backup_snap_id was non null in the `snapshots` table

        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=self.virtual_machine_with_disk.id,
                            type='ROOT'
                            )

        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volumes[0].id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )
        self.debug("Snapshot created: ID - %s" % snapshot.id)

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
        self.debug(
            "select backup_snap_id, account_id, volume_id from snapshots where id = %s;" \
            % snapshot.id
            )
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where id = %s;" \
                        % snapshot.id
                        )
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]

        snapshot_uuid = qresult[0]      # backup_snap_id = snapshot UUID
        account_id = qresult[1]
        volume_id = qresult[2]

        self.assertNotEqual(
                            str(snapshot_uuid),
                            'NULL',
                            "Check if backup_snap_id is not null"
                        )
        
        # Get the Secondary Storage details from  list Hosts
        hosts = list_hosts(
                                 self.apiclient,
                                 type='SecondaryStorage',
                                 zoneid=self.zone.id
                                 )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list response returns a valid list"
                        )

        # hosts[0].name = "nfs://192.168.100.21/export/test"
        parse_url = (hosts[0].name).split('/')
        # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

        # Split IP address and export path from name
        sec_storage_ip = parse_url[2]
        # Sec Storage IP: 192.168.100.21

        export_path = '/'.join(parse_url[3:])
        # Export path: export/test

        try:
            # Login to VM to check snapshot present on sec disk
            ssh_client = self.virtual_machine_with_disk.get_ssh_client()

            cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s:/%s %s" % (
                                         sec_storage_ip,
                                         export_path,
                                         self.services["mount_dir"]
                                         ),
                    "ls %s/snapshots/%s/%s" % (
                                               self.services["mount_dir"],
                                               account_id,
                                               volume_id
                                               ),
                    ]

            for c in cmds:
                result = ssh_client.execute(c)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                                self.virtual_machine_with_disk.ipaddress)

        res = str(result)
        # Check snapshot UUID in secondary storage and database
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        # Unmount the Sec Storage
        cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
        try:
            for c in cmds:
                result = ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for Virtual machine: %s" %
                                self.virtual_machine_with_disk.ipaddress)

        return


class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        
        cls.services["domainid"] = cls.domain.id
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
                            domainid=cls.domain.id
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
                                domainid=cls.account.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls.virtual_machine_without_disk = \
                    VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server_without_disk"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                    )
        cls._cleanup = [
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

    def test_02_snapshot_data_disk(self):
        """Test Snapshot Data Disk
        """

        volume = list_volumes(
                            self.apiclient,
                            virtualmachineid=self.virtual_machine_with_disk.id,
                            type='DATADISK'
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
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
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
        self.debug(
            "select backup_snap_id, account_id, volume_id from snapshots where id = %s;" \
            % snapshot.id
            )
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where id = %s;" \
                        % snapshot.id
                        )
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]
        snapshot_uuid = qresult[0]      # backup_snap_id = snapshot UUID
        account_id = qresult[1]
        volume_id = qresult[2]

        self.assertNotEqual(
                            str(snapshot_uuid),
                            'NULL',
                            "Check if backup_snap_id is not null"
                        )

        # Get the Secondary Storage details from  list Hosts
        hosts = list_hosts(
                                 self.apiclient,
                                 type='SecondaryStorage',
                                 zoneid=self.zone.id
                                 )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list response returns a valid list"
                        )

        # hosts[0].name = "nfs://192.168.100.21/export"
        parse_url = (hosts[0].name).split('/')
        # parse_url = ['nfs:', '', '192.168.100.21', 'export']

        # Split IP address and export path from name
        sec_storage_ip = parse_url[2]
        # Sec Storage IP: 192.168.100.21

        export_path = '/'.join(parse_url[3:])
        # Export path: export

        try:
            # Login to VM to check snapshot present on sec disk
            ssh_client = self.virtual_machine_with_disk.get_ssh_client()

            cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s:/%s %s" % (
                                         sec_storage_ip,
                                         export_path,
                                         self.services["mount_dir"]
                                         ),
                    "ls %s/snapshots/%s/%s" % (
                                               self.services["mount_dir"],
                                               account_id,
                                               volume_id
                                               ),
                ]
            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine_with_disk.ipaddress)

        res = str(result)
        # Check snapshot UUID in secondary storage and database
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        # Unmount the Sec Storage
        cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
        try:
            for c in cmds:
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine_with_disk.ipaddress)

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

        try:
            ssh_client = self.virtual_machine.get_ssh_client()

            #Format partition using ext3
            format_volume_to_ext3(
                              ssh_client,
                              self.services["diskdevice"]
                              )
            cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["diskdevice"],
                                      self.services["mount_dir"]
                                      ),
                    "mkdir -p %s/%s/{%s,%s} " % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["sub_lvl_dir2"]
                                            ),
                    "echo %s > %s/%s/%s/%s" % (
                                                random_data_0,
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                            ),
                    "echo %s > %s/%s/%s/%s" % (
                                                random_data_1,
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                            ),
                ]
            for c in cmds:
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine.ipaddress)
        # Unmount the Sec Storage
        cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]

        try:
            for c in cmds:
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine.ipaddress)

        list_volume_response = list_volumes(
                                    self.apiclient,
                                    virtualmachineid=self.virtual_machine.id,
                                    type='DATADISK'
                                    )

        volume_response = list_volume_response[0]
        #Create snapshot from attached volume
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volume_response.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )
        self.debug("Created Snapshot from volume: %s" % volume_response.id)

        #Create volume from snapshot
        self.debug("Creating volume from snapshot: %s" % snapshot.id)
        volume = Volume.create_from_snapshot(
                                        self.apiclient,
                                        snapshot.id,
                                        self.services,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        volumes = list_volumes(
                                self.apiclient,
                                id=volume.id
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
                        volume.id,
                        "Check Volume in the List Volumes"
                    )
        #Attaching volume to new VM
        new_virtual_machine = self.virtual_machine_without_disk
        self.cleanup.append(new_virtual_machine)

        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume.id
        cmd.virtualmachineid = new_virtual_machine.id
        self.apiclient.attachVolume(cmd)

        try:
            #Login to VM to verify test directories and files
            ssh = new_virtual_machine.get_ssh_client()

            cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["diskdevice"],
                                      self.services["mount_dir"]
                                      ),
               ]

            for c in cmds:
                ssh.execute(c)

            returned_data_0 = ssh.execute("cat %s/%s/%s/%s" % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                    ))
            returned_data_1 = ssh.execute("cat %s/%s/%s/%s" % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                    ))

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.new_virtual_machine.ipaddress)

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
        # Unmount the Sec Storage
        cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
        try:
            for c in cmds:
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.new_virtual_machine.ipaddress)
        return

    def test_04_delete_snapshot(self):
        """Test Delete Snapshot
        """

        #1. Snapshot the Volume
        #2. Delete the snapshot
        #3. Verify snapshot is removed by calling List Snapshots API

        volumes = list_volumes(
                               self.apiclient,
                               virtualmachineid=self.virtual_machine.id,
                               type='DATADISK'
                               )
        self.assertEqual(
                            isinstance(volumes, list),
                            True,
                            "Check list response returns a valid list"
                        )
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volumes[0].id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
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
        return

    def test_05_recurring_snapshot_root_disk(self):
        """Test Recurring Snapshot Root Disk
        """
        #1. Create snapshot policy for root disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine_with_disk.id,
                        type='ROOT'
                        )
        self.assertEqual(
                            isinstance(volume, list),
                            True,
                            "Check list response returns a valid list"
                        )
        recurring_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           volume[0].id,
                                           self.services["recurring_snapshot"]
                                        )
        self.cleanup.append(recurring_snapshot)

        #ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
                                                     self.apiclient,
                                                     id=recurring_snapshot.id,
                                                     volumeid=volume[0].id
                                                     )
        self.assertEqual(
                            isinstance(list_snapshots_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )
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
            (self.services["recurring_snapshot"]["maxsnaps"]) * 3600
            )

        timeout = self.services["timeout"]
        while True: 
            snapshots = list_snapshots(
                        self.apiclient,
                        volumeid=volume[0].id,
                        intervaltype=\
                        self.services["recurring_snapshot"]["intervaltype"],
                        snapshottype='RECURRING'
                        )
                
            if isinstance(snapshots, list):
                break
                    
            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

            time.sleep(1)
            timeout = timeout - 1

        self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertEqual(
                         len(snapshots),
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

        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine_with_disk.id,
                        type='DATADISK'
                        )

        self.assertEqual(
                            isinstance(volume, list),
                            True,
                            "Check list response returns a valid list"
                        )

        recurring_snapshot = SnapshotPolicy.create(
                                    self.apiclient,
                                    volume[0].id,
                                    self.services["recurring_snapshot"]
                                )
        self.cleanup.append(recurring_snapshot)
        #ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = list_snapshot_policy(
                                                     self.apiclient,
                                                     id=recurring_snapshot.id,
                                                     volumeid=volume[0].id
                                                     )

        self.assertEqual(
                            isinstance(list_snapshots_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )

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

        # Sleep for (maxsnaps) hours to verify only maxsnaps snapshots are
        # retained
        time.sleep(
            (self.services["recurring_snapshot"]["maxsnaps"]) * 3600
            )
        
        timeout = self.services["timeout"]
        while True: 
            snapshots = list_snapshots(
                            self.apiclient,
                            volumeid=volume[0].id,
                            intervaltype=\
                            self.services["recurring_snapshot"]["intervaltype"],
                            snapshottype='RECURRING'
                            )
                
            if isinstance(snapshots, list):
                break
                    
            elif timeout == 0:
                raise Exception("List snapshots API call failed.")

            time.sleep(1)
            timeout = timeout - 1

        self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertEqual(
                         len(snapshots),
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

        try:
            #Login to virtual machine
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["rootdisk"],
                                      self.services["mount_dir"]
                                      ),
                    "mkdir -p %s/%s/{%s,%s} " % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["sub_lvl_dir2"]
                                            ),
                    "echo %s > %s/%s/%s/%s" % (
                                                random_data_0,
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                            ),
                    "echo %s > %s/%s/%s/%s" % (
                                                random_data_1,
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                        )
                ]

            for c in cmds:
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s" %
                                    self.virtual_machine.ipaddress)

        # Unmount the Volume
        cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
        for c in cmds:
            ssh_client.execute(c)

        volumes = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine.id,
                        type='ROOT'
                        )
        self.assertEqual(
                            isinstance(volumes, list),
                            True,
                            "Check list response returns a valid list"
                        )

        volume = volumes[0]

        #Create a snapshot of volume
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volume.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )

        self.debug("Snapshot created from volume ID: %s" % volume.id)
        # Generate template from the snapshot
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["templates"]
                                    )
        self.cleanup.append(template)
        self.debug("Template created from snapshot ID: %s" % snapshot.id)

        # Verify created template
        templates = list_templates(
                                self.apiclient,
                                templatefilter=\
                                self.services["templates"]["templatefilter"],
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
                                    self.apiclient,
                                    self.services["server_without_disk"],
                                    templateid=template.id,
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services["mode"]
                                    )
        self.cleanup.append(new_virtual_machine)

        try:
            #Login to VM & mount directory
            ssh = new_virtual_machine.get_ssh_client()

            cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["rootdisk"],
                                      self.services["mount_dir"]
                                      )
               ]

            for c in cmds:
                ssh.execute(c)

            returned_data_0 = ssh.execute("cat %s/%s/%s/%s" % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir1"],
                                                self.services["random_data"]
                                    ))
            returned_data_1 = ssh.execute("cat %s/%s/%s/%s" % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                    ))

        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s" %
                                    new_virtual_machine.ipaddress)
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
        # Unmount the volume
        cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
        try:
            for c in cmds:
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s" %
                                    new_virtual_machine.ipaddress)
        return
