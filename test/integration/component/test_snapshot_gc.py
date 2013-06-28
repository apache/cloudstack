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
                        "volume": {
                                   "diskname": "APP Data Volume",
                                   "size": 1,   # in GBs
                                   "diskdevice": ['/dev/xvdb', '/dev/sdb', '/dev/hdb', '/dev/vdb' ],   # Data Disk
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
    @attr(tags = ["advanced", "advancedns", "basic", "sg"])
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
                    "mkdir -p %s" % self.services["paths"]["mount_dir"],
                    "mount -t %s %s/%s %s" % (
                                         storage_type,
                                         sec_storage_ip,
                                         export_path,
                                         self.services["paths"]["mount_dir"]
                                         ),
                    "ls %s/snapshots/%s/%s" % (
                                               self.services["paths"]["mount_dir"],
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
                                         self.services["paths"]["mount_dir"]
                                         ),
                        "ls %s/snapshots/%s/%s" % (
                                               self.services["paths"]["mount_dir"],
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
                    "umount %s" % (self.services["paths"]["mount_dir"]),
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