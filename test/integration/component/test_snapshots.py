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
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin.remoteSSHClient import remoteSSHClient


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
                         "mgmt_server": {
                                    "ipaddress": '192.168.100.21',
                                    "username": "root",
                                    "password": "password",
                                    "port": 22,
                                },
                        "recurring_snapshot": {
                                    "intervaltype": 'HOURLY',
                                    # Frequency of snapshots
                                    "maxsnaps": 1,  # Should be min 2
                                    "schedule": 1,
                                    "timezone": 'US/Arizona',
                                    # Timezone Formats - http://cloud.mindtouch.us/CloudStack_Documentation/Developer's_Guide%3A_CloudStack
                                },
                        "templates": {
                                    "displaytext": 'Template',
                                    "name": 'Template',
                                    "ostype": "CentOS 5.3 (64-bit)",
                                    "templatefilter": 'self',
                                },
                        "diskdevice": "/dev/xvda",
                        "diskname": "TestDiskServ",
                        "size": 1,  # GBs

                        "mount_dir": "/mnt/tmp",
                        "sub_dir": "test",
                        "sub_lvl_dir1": "test1",
                        "sub_lvl_dir2": "test2",
                        "random_data": "random.data",
                        "ostype": "CentOS 5.3 (64-bit)",
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                    }


class TestSnapshotRootDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestSnapshotRootDisk, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["server_without_disk"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

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
        cls.virtual_machine = cls.virtual_machine_with_disk = \
                    VirtualMachine.create(
                                cls.api_client,
                                cls.services["server_without_disk"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke"])
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
                            type='ROOT',
                            listall=True
                            )

        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volumes[0].id,
                                   account=self.account.name,
                                   domainid=self.account.domainid
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
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
            % str(snapshot.id)
            )
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
                        % str(snapshot.id)
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
        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export/test"
            parse_url = (host.name).split('/')
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
                    "mount %s/%s %s" % (
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

            except Exception:
                self.fail("SSH failed for Virtual machine: %s" %
                                self.virtual_machine_with_disk.ipaddress)

            uuids.append(result)
            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
            try:
                for c in cmds:
                    self.debug(c)
                    result = ssh_client.execute(c)
                    self.debug(result)

            except Exception as e:
                self.fail("SSH failed for Virtual machine: %s" %
                                self.virtual_machine_with_disk.ipaddress)
        
        res = str(uuids)
        # Check snapshot UUID in secondary storage and database
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        return


class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestSnapshots, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        
        cls.services["domainid"] = cls.domain.id
        cls.services["server_with_disk"]["zoneid"] = cls.zone.id
        cls.services["server_with_disk"]["diskoffering"] = cls.disk_offering.id

        cls.services["server_without_disk"]["zoneid"] = cls.zone.id

        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskoffering"] = cls.disk_offering.id

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
        cls.virtual_machine = cls.virtual_machine_with_disk = \
                    VirtualMachine.create(
                                cls.api_client,
                                cls.services["server_with_disk"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls.virtual_machine_without_disk = \
                    VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["server_without_disk"],
                                    templateid=template.id,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_02_snapshot_data_disk(self):
        """Test Snapshot Data Disk
        """

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
        self.debug(
            "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
            % str(snapshot.id)
            )
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
                        % str(snapshot.id)
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
        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export"
            parse_url = (host.name).split('/')
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
                    "mount %s/%s %s" % (
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

            uuids.append(result)
            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
            try:
                for c in cmds:
                    self.debug(c)
                    ssh_client.execute(c)

            except Exception as e:
                self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine_with_disk.ipaddress)

        res = str(uuids)
        # Check snapshot UUID in secondary storage and database
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        return

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke", "xen"])
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
                self.debug(c)
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
                self.debug(c)
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine.ipaddress)

        list_volume_response = list_volumes(
                                    self.apiclient,
                                    virtualmachineid=self.virtual_machine.id,
                                    type='DATADISK',
                                    listall=True
                                    )

        volume_response = list_volume_response[0]
        #Create snapshot from attached volume
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volume_response.id,
                                   account=self.account.name,
                                   domainid=self.account.domainid
                                   )
        self.debug("Created Snapshot from volume: %s" % volume_response.id)

        #Create volume from snapshot
        self.debug("Creating volume from snapshot: %s" % snapshot.id)
        volume = Volume.create_from_snapshot(
                                        self.apiclient,
                                        snapshot.id,
                                        self.services,
                                        account=self.account.name,
                                        domainid=self.account.domainid
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
                self.debug(c)
                result = ssh.execute(c)
                self.debug(result)

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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_04_delete_snapshot(self):
        """Test Delete Snapshot
        """

        #1. Snapshot the Volume
        #2. Delete the snapshot
        #3. Verify snapshot is removed by calling List Snapshots API

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
        return

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_05_recurring_snapshot_root_disk(self):
        """Test Recurring Snapshot Root Disk
        """
        #1. Create snapshot policy for root disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid=self.virtual_machine_with_disk.id,
                        type='ROOT',
                        listall=True
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
                        snapshottype='RECURRING',
                        listall=True
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke"])
    def test_06_recurring_snapshot_data_disk(self):
        """Test Recurring Snapshot data Disk
        """
        #1. Create snapshot policy for data disk
        #2. ListSnapshot policy should return newly created policy
        #3. Verify only most recent number (maxsnaps) snapshots retailed

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
                            snapshottype='RECURRING',
                            listall=True
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "smoke", "xen"])
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
                    "umount %s" % (self.services["mount_dir"]),
                ]
        for c in cmds:
            self.debug(c)
            ssh_client.execute(c)

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

        #Create a snapshot of volume
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volume.id,
                                   account=self.account.name,
                                   domainid=self.account.domainid
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
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
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
            self.debug(returned_data_0)
            returned_data_1 = ssh.execute("cat %s/%s/%s/%s" % (
                                                self.services["mount_dir"],
                                                self.services["sub_dir"],
                                                self.services["sub_lvl_dir2"],
                                                self.services["random_data"]
                                    ))
            self.debug(returned_data_1)
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
                self.debug(c)
                ssh_client.execute(c)

        except Exception as e:
            self.fail("SSH failed for VM with IP address: %s" %
                                    new_virtual_machine.ipaddress)
        return

class TestCreateVMsnapshotTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestCreateVMsnapshotTemplate, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

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

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns"])
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
        #  volume under /secondary/snapshots/$accountid/$volumeid/$snapshot_uuid
        # 7. verify backup_snap_id was non null in the `snapshots` table
        # 8. listTemplates() should return the newly created Template,
        #    and check for template state as READY"
        # 9. listVirtualMachines() command should return the deployed VM.
        #    State of this VM should be Running.

        #Create Virtual Machine
        self.virtual_machine = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.debug("Created VM with ID: %s" % self.virtual_machine.id)
        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=self.virtual_machine.id,
                            type='ROOT',
                            listall=True
                            )
        volume = volumes[0]

        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)
        self.debug("Snapshot created: ID - %s" % snapshot.id)
        self.cleanup.append(snapshot)

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
        self.debug("select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
                        % snapshot.id)
        # Verify backup_snap_id is not NULL
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
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

        # Generate template from the snapshot
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["templates"]
                                    )
        self.debug("Created template from snapshot: %s" % template.id)
        self.cleanup.append(template)

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
                            templates[0].isready,
                            True,
                            "Check new template state in list templates call"
                        )

        # Deploy new virtual machine using template
        new_virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["server"],
                                    templateid=template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                    )
        self.debug("Created VM with ID: %s from template: %s" % (
                                                        new_virtual_machine.id,
                                                        template.id
                                                        ))
        self.cleanup.append(new_virtual_machine)

        # Newly deployed VM should be 'Running'
        virtual_machines = list_virtual_machines(
                                self.apiclient,
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
        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export/test"
            parse_url = (host.name).split('/')
            # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

            # Stripping end ':' from storage type
            storage_type = parse_url[0][:-1]
            # Split IP address and export path from name
            sec_storage_ip = parse_url[2]
            # Sec Storage IP: 192.168.100.21
            if sec_storage_ip[-1] != ":":
                sec_storage_ip = sec_storage_ip + ":"

            export_path = '/'.join(parse_url[3:])
            # Export path: export/test

            # Sleep to ensure that snapshot is reflected in sec storage
            time.sleep(self.services["sleep"])
            try:
                # Login to VM to check snapshot present on sec disk
                ssh_client = remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

                cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount -t %s %s/%s %s" % (
                                         storage_type,
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
                    self.debug("command: %s" % c)
                    result = ssh_client.execute(c)
                    self.debug("Result: %s" % result)

            except Exception as e:
                self.fail("SSH failed for Management server: %s - %s" %
                                (self.services["mgmt_server"]["ipaddress"], e))
            uuids.append(result)
            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
            try:
                for c in cmds:
                    self.debug("command: %s" % c)
                    result = ssh_client.execute(c)
                    self.debug("Result: %s" % result)

            except Exception as e:
                self.fail("SSH failed for Management server: %s - %s" %
                                (self.services["mgmt_server"]["ipaddress"], e))

        res = str(uuids)
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )

        return


class TestAccountSnapshotClean(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestAccountSnapshotClean, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

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

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id
                                )
        # Get the Root disk of VM
        volumes = list_volumes(
                            cls.api_client,
                            virtualmachineid=cls.virtual_machine.id,
                            type='ROOT',
                            listall=True
                            )
        volume = volumes[0]

        # Create a snapshot from the ROOTDISK
        cls.snapshot = Snapshot.create(cls.api_client, volumes[0].id)

        cls._cleanup = [
                        cls.service_offering,
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns"])
    def test_02_accountSnapshotClean(self):
        """Test snapshot cleanup after account deletion
        """
        # Validate the following
        # 1. listAccounts API should list out the newly created account
        # 2. listVirtualMachines() command should return the deployed VM.
        #    State of this VM should be "Running"
        # 3. a)listSnapshots should list the snapshot that was created.
        #    b)verify that secondary storage NFS share contains the reqd volume
        #      under /secondary/snapshots/$accountid/$volumeid/$snapshot_uuid
        # 4. a)listAccounts should not list account that is deleted
        #    b) snapshot image($snapshot_uuid) should be deleted from the
        #       /secondary/snapshots/$accountid/$volumeid/

        accounts = list_accounts(
                                 self.apiclient,
                                 id=self.account.id
                                 )
        self.assertEqual(
                            isinstance(accounts, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                             len(accounts),
                             0,
                             "Check list Accounts response"
                             )

        # VM should be in 'Running' state
        virtual_machines = list_virtual_machines(
                                self.apiclient,
                                id=self.virtual_machine.id
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
            self.debug("VM ID: %s, VM state: %s" % (
                                            virtual_machine.id,
                                            virtual_machine.state
                                            ))
            self.assertEqual(
                        virtual_machine.state,
                        'Running',
                        "Check list VM response for Running state"
                    )

        # Verify the snapshot was created or not
        snapshots = list_snapshots(
                                   self.apiclient,
                                   id=self.snapshot.id
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
                            self.snapshot.id,
                            "Check snapshot id in list resources call"
                        )

        # Fetch values from database
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
                        % self.snapshot.id
                        )
        self.assertEqual(
                            isinstance(qresultset, list),
                            True,
                            "Check DB response returns a valid list"
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
        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export/test"
            parse_url = (host.name).split('/')
            # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

            # Stripping end ':' from storage type
            storage_type = parse_url[0][:-1]
            # Split IP address and export path from name
            sec_storage_ip = parse_url[2]
            # Sec Storage IP: 192.168.100.21

            if sec_storage_ip[-1] != ":":
                sec_storage_ip = sec_storage_ip + ":"

            export_path = '/'.join(parse_url[3:])
            # Export path: export/test

            # Sleep to ensure that snapshot is reflected in sec storage
            time.sleep(self.services["sleep"])
            try:
                # Login to Secondary storage VM to check snapshot present on sec disk
                ssh_client = remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

                cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount -t %s %s/%s %s" % (
                                         storage_type,
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
                    self.debug("command: %s" % c)
                    result = ssh_client.execute(c)
                    self.debug("Result: %s" % result)

                uuids.append(result)

                # Unmount the Sec Storage
                cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
                for c in cmds:
                    result = ssh_client.execute(c)
            except Exception as e:
                self.fail("SSH failed for management server: %s - %s" %
                                (self.services["mgmt_server"]["ipaddress"], e))

        res = str(uuids)
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )

        self.debug("Deleting account: %s" % self.account.name)
        # Delete account
        self.account.delete(self.apiclient)

        interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
        self.assertEqual(
                            isinstance(interval, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.debug("account.cleanup.interval: %s" % interval[0].value)

        # Wait for account cleanup interval
        time.sleep(int(interval[0].value) * 2)

        accounts = list_accounts(
                                 self.apiclient,
                                 id=self.account.id
                                 )

        self.assertEqual(
            accounts,
            None,
            "List accounts should return empty list after account deletion"
            )

        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export/test"
            parse_url = (host.name).split('/')
            # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

            # Stripping end ':' from storage type
            storage_type = parse_url[0][:-1]
            # Split IP address and export path from name
            sec_storage_ip = parse_url[2]
            # Sec Storage IP: 192.168.100.21

            if sec_storage_ip[-1] != ":":
                sec_storage_ip = sec_storage_ip + ":"

            export_path = '/'.join(parse_url[3:])
            # Export path: export/test

            try:
                cmds = [
                        "mount -t %s %s/%s %s" % (
                                         storage_type,
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
                    self.debug("command: %s" % c)
                    result = ssh_client.execute(c)
                    self.debug("Result: %s" % result)

                uuids.append(result)
                # Unmount the Sec Storage
                cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
                for c in cmds:
                    self.debug("command: %s" % c)
                    result = ssh_client.execute(c)
                    self.debug("Result: %s" % result)

            except Exception as e:
                self.fail("SSH failed for management server: %s - %s" %
                                (self.services["mgmt_server"]["ipaddress"], e))

        res = str(uuids)
        self.assertNotEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        return


class TestSnapshotDetachedDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestSnapshotDetachedDisk, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.services["server"]["diskoffering"] = cls.disk_offering.id

        cls.services["template"] = template.id

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
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "xen"])
    def test_03_snapshot_detachedDisk(self):
        """Test snapshot from detached disk
        """
        # Validate the following
        # 1. login in VM  and write some data on data disk(use fdisk to
        #    partition datadisk,fdisk /dev/sdb, and make filesystem using
        #    mkfs.ext3)
        # 2. Detach the data disk and write some data on data disk
        # 3. perform the snapshot on the detached volume
        # 4. listvolumes with VM id shouldn't show the detached volume
        # 5. listSnapshots should list the snapshot that was created
        # 6. verify that secondary storage NFS share contains the reqd volume
        #    under /secondary/snapshots/$accountid/$volumeid/$snapshot_uuid
        # 7. verify backup_snap_id was non null in the `snapshots` table

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
                                            ),
                    "sync",
                ]
            for c in cmds:
                self.debug(ssh_client.execute(c))

            #detach volume from VM
            cmd = detachVolume.detachVolumeCmd()
            cmd.id = volume.id
            self.apiclient.detachVolume(cmd)

            #Create snapshot from detached volume
            snapshot = Snapshot.create(self.apiclient, volume.id)
            self.cleanup.append(snapshot)

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
                                (self.virtual_machine.ipaddress, e))

        # Fetch values from database
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
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
                            str(qresult[0]),
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
        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export/test"
            parse_url = (host.name).split('/')
            # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

            # Stripping end ':' from storage type
            storage_type = parse_url[0][:-1]

            # Split IP address and export path from name
            sec_storage_ip = parse_url[2]
            # Sec Storage IP: 192.168.100.21

            if sec_storage_ip[-1] != ":":
                sec_storage_ip = sec_storage_ip + ":"

            export_path = '/'.join(parse_url[3:])
            # Export path: export/test

            # Sleep to ensure that snapshot is reflected in sec storage
            time.sleep(self.services["sleep"])
            try:
                # Login to Management server to check snapshot present on
                # sec disk
                ssh_client = remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

                cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount -t %s %s/%s %s" % (
                                         storage_type,
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

                uuids.append(result)
                # Unmount the Sec Storage
                cmds = [
                        "umount %s" % (self.services["mount_dir"]),
                        ]
                for c in cmds:
                    result = ssh_client.execute(c)
            except Exception as e:
                self.fail("SSH failed for management server: %s - %s" %
                                (self.services["mgmt_server"]["ipaddress"], e))

        res = str(uuids)
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        return


class TestSnapshotLimit(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestSnapshotLimit, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

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

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id
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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns"])
    def test_04_snapshot_limit(self):
        """Test snapshot limit in snapshot policies
        """
        # Validate the following
        # 1. Perform hourly recurring snapshot on the root disk of VM and keep
        #    the maxsnapshots as 1
        # 2. listSnapshots should list the snapshot that was created
        #    snapshot folder in secondary storage should contain only one
        #    snapshot image(/secondary/snapshots/$accountid/$volumeid/)

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

        # Create a snapshot policy
        recurring_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           volume.id,
                                           self.services["recurring_snapshot"]
                                        )
        self.cleanup.append(recurring_snapshot)

        snapshot_policy = list_snapshot_policy(
                                        self.apiclient,
                                        id=recurring_snapshot.id,
                                        volumeid=volume.id
                                        )
        self.assertEqual(
                            isinstance(snapshot_policy, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            snapshot_policy,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                        snapshot_policy[0].id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                        snapshot_policy[0].maxsnaps,
                        self.services["recurring_snapshot"]["maxsnaps"],
                        "Check interval type in list resources call"
                        )
        # Sleep for (maxsnaps+1) hours to verify
        # only maxsnaps snapshots are retained
        time.sleep(
            (self.services["recurring_snapshot"]["maxsnaps"]) * 3600
            )

        # Verify the snapshot was created or not
        snapshots = list_snapshots(
                        self.apiclient,
                        volumeid=volume.id,
                        intervaltype=\
                        self.services["recurring_snapshot"]["intervaltype"],
                        snapshottype='RECURRING',
                        listall=True
                        )

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
        snapshot = snapshots[0]
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])

        # Fetch values from database
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where uuid = '%s';" \
                        % snapshot.id
                        )
        self.assertEqual(
                            isinstance(qresultset, list),
                            True,
                            "Check DBQuery returns a valid list"
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
        uuids = []
        for host in hosts:
            # hosts[0].name = "nfs://192.168.100.21/export/test"
            parse_url = (host.name).split('/')
            # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

            # Stripping end ':' from storage type
            storage_type = parse_url[0][:-1]
            # Split IP address and export path from name
            sec_storage_ip = parse_url[2]
            # Sec Storage IP: 192.168.100.21

            if sec_storage_ip[-1] != ":":
                sec_storage_ip = sec_storage_ip + ":"

            export_path = '/'.join(parse_url[3:])
            # Export path: export/test
            try:
                # Login to VM to check snapshot present on sec disk
                ssh_client = remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

                cmds = [
                    "mkdir -p %s" % self.services["mount_dir"],
                    "mount -t %s %s/%s %s" % (
                                         storage_type,
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

                uuids.append(result)

                # Unmount the Sec Storage
                cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
                for c in cmds:
                    result = ssh_client.execute(c)
            except Exception as e:
                raise Exception(
                        "SSH access failed for management server: %s - %s" %
                                    (self.services["mgmt_server"]["ipaddress"], e))

        res = str(uuids)
        self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
        return


class TestSnapshotEvents(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestSnapshotEvents, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

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

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id
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
    
    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns"])
    def test_05_snapshot_events(self):
        """Test snapshot events
        """
        # Validate the following
        # 1. Perform snapshot on the root disk of this VM and check the events/alerts.
        # 2. delete the snapshots and check the events/alerts
        # 3. listEvents() shows created/deleted snapshot events

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
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)
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
