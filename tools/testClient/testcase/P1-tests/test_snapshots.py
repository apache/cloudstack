# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" P1 tests for Snapshots
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
                                    "cpuspeed": 200, # in MHz
                                    "memory": 256, # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small Disk",
                                    "name": "Small Disk",
                                    "disksize": 1
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
                                    "ipaddress": '192.168.100.154',
                                    "username": "root",
                                    "password": "fr3sca",
                                    "port": 22,
                                },
                        "recurring_snapshot": {
                                    "intervaltype": 'HOURLY',
                                    # Frequency of snapshots
                                    "maxsnaps": 1, # Should be min 2
                                    "schedule": 1,
                                    "timezone": 'US/Arizona',
                                    # Timezone Formats - http://cloud.mindtouch.us/CloudStack_Documentation/Developer's_Guide%3A_CloudStack 
                                },
                        "templates": {
                                    "displaytext": 'Template',
                                    "name": 'Template',
                                    "ostypeid": 12,
                                    "templatefilter": 'self',
                                },
                        "diskdevice": "/dev/xvda",
                        "diskname": "TestDiskServ",
                        "size": 1, # GBs

                        "mount_dir": "/mnt/tmp",
                        "sub_dir": "test",
                        "sub_lvl_dir1": "test1",
                        "sub_lvl_dir2": "test2",
                        "random_data": "random.data",

                        "ostypeid": 12,
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                        "mode" : 'advanced', # Networking mode: Advanced, Basic
                    }


class TestCreateVMsnapshotTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id

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
                                accountid=self.account.account.name,
                                domainid=self.account.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.debug("Created VM with ID: %s" % self.virtual_machine.id)
        # Get the Root disk of VM 
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=self.virtual_machine.id,
                            type='ROOT'
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
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
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
                                account=self.account.account.name,
                                domainid=self.account.account.domainid
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

        # hosts[0].name = "nfs://192.168.100.21/export/test"
        parse_url = (hosts[0].name).split('/')
        # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

        # Split IP address and export path from name
        sec_storage_ip = parse_url[2]
        # Sec Storage IP: 192.168.100.21

        export_path = '/'.join(parse_url[3:])
        # Export path: export/test
        
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])
        try:
            # Login to VM to check snapshot present on sec disk
            ssh_client = remoteSSHClient.remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

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
                self.debug("command: %s" % c)
                result = ssh_client.execute(c)
                self.debug("Result: %s" % result)
                
        except Exception as e:
            self.fail("SSH failed for Management server: %s" %
                                self.services["mgmt_server"]["ipaddress"])

        res = str(result)
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
                self.debug("command: %s" % c)
                result = ssh_client.execute(c)
                self.debug("Result: %s" % result)

        except Exception as e:
            self.fail("SSH failed for Management server: %s" %
                                self.services["mgmt_server"]["ipaddress"])        
        return


class TestAccountSnapshotClean(cloudstackTestCase):

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
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

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
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.account.name,
                                domainid=cls.account.account.domainid,
                                serviceofferingid=cls.service_offering.id
                                )
        # Get the Root disk of VM 
        volumes = list_volumes(
                            cls.api_client,
                            virtualmachineid=cls.virtual_machine.id,
                            type='ROOT'
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
                                 id=self.account.account.id
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
                        "select backup_snap_id, account_id, volume_id from snapshots where id = %s;" \
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

        # hosts[0].name = "nfs://192.168.100.21/export/test"
        parse_url = (hosts[0].name).split('/')
        # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

        # Split IP address and export path from name
        sec_storage_ip = parse_url[2]
        # Sec Storage IP: 192.168.100.21

        export_path = '/'.join(parse_url[3:])
        # Export path: export/test
        
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])
        try:
            # Login to Secondary storage VM to check snapshot present on sec disk
            ssh_client = remoteSSHClient.remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

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
                self.debug("command: %s" % c)
                result = ssh_client.execute(c)
                self.debug("Result: %s" % result)
                
            res = str(result)
            self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )

            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
            for c in cmds:
                result = ssh_client.execute(c)
        except Exception:
            self.fail("SSH failed for management server: %s" %
                                self.services["mgmt_server"]["ipaddress"])
        
        self.debug("Deleting account: %s" % self.account.account.name)
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

        with self.assertRaises(Exception):
            accounts = list_accounts(
                                 self.apiclient,
                                 id=self.account.account.id
                                 )
        try:
            cmds = [    
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
                self.debug("command: %s" % c)
                result = ssh_client.execute(c)
                self.debug("Result: %s" % result)
                
            res = str(result)
            
            self.assertNotEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
            for c in cmds:
                self.debug("command: %s" % c)
                result = ssh_client.execute(c)
                self.debug("Result: %s" % result)

        except Exception:
            self.fail("SSH failed for management server: %s" %
                                self.services["mgmt_server"]["ipaddress"])
        return


class TestSnapshotDetachedDisk(cloudstackTestCase):

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
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.services["server"]["diskoffering"] = cls.disk_offering.id

        cls.services["template"] = template.id

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
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
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
                               type='DATADISK'
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
                                            )
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
                               type='DATADISK'
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
            self.fail("SSH failed for VM with IP: %s" %
                                self.virtual_machine.ipaddress)
            
        # Fetch values from database
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

        # hosts[0].name = "nfs://192.168.100.21/export/test"
        parse_url = (hosts[0].name).split('/')
        # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']

        # Split IP address and export path from name
        sec_storage_ip = parse_url[2]
        # Sec Storage IP: 192.168.100.21

        export_path = '/'.join(parse_url[3:])
        # Export path: export/test
        
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])
        try:
            # Login to Management server to check snapshot present on sec disk
            ssh_client = remoteSSHClient.remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

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
            res = str(result)
            self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )
            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                    ]
            for c in cmds:
                result = ssh_client.execute(c)
        except Exception as e:
            self.fail("SSH failed for management server: %s" %
                                self.services["mgmt_server"]["ipaddress"])

        return


class TestSnapshotLimit(cloudstackTestCase):

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
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

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
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.account.name,
                                domainid=cls.account.account.domainid,
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
                            type='ROOT'
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
                        snapshottype='RECURRING'
                        )
        
        self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                         len(snapshots),
                         self.services["recurring_snapshot"]["maxsnaps"],
                         "Check maximum number of recurring snapshots retained"
                        )
        # Sleep to ensure that snapshot is reflected in sec storage
        time.sleep(self.services["sleep"])

        # Fetch values from database
        qresultset = self.dbclient.execute(
                        "select backup_snap_id, account_id, volume_id from snapshots where id = %s;" \
                        % self.snapshot.id
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
            ssh_client = remoteSSHClient.remoteSSHClient(
                                    self.services["mgmt_server"]["ipaddress"],
                                    self.services["mgmt_server"]["port"],
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["password"],
                                    )

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

            res = str(result)
            self.assertEqual(
                        res.count(snapshot_uuid),
                        1,
                        "Check snapshot UUID in secondary storage and database"
                        )

            # Unmount the Sec Storage
            cmds = [
                    "umount %s" % (self.services["mount_dir"]),
                ]
            for c in cmds:
                result = ssh_client.execute(c)
        except Exception as e:
            raise Exception("SSH access failed for management server: %s" %
                                    self.services["mgmt_server"]["ipaddress"])
        return


class TestSnapshotEvents(cloudstackTestCase):

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
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

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
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.account.name,
                                domainid=cls.account.account.domainid,
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
                            type='ROOT'
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
                             account=self.account.account.name,
                             domainid=self.account.account.domainid,
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
