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

""" P1 tests for Snapshots Improvements
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (random_gen,
                                          is_snapshot_on_nfs,
                                          cleanup_resources)
from marvin.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        Snapshot,
                                        Template,
                                        Volume,
                                        DiskOffering
                                        )
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template,
                                        list_snapshots
                                        )
from marvin.cloudstackAPI import (createSnapshot,
                                  createVolume,
                                  createTemplate,
                                  listOsTypes,
                                  stopVirtualMachine
                                  )

class Services:
    def __init__(self):
        self.services = {
                        "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 200,    # in MHz
                                    "memory": 256,    # In MBs
                        },
                         "service_offering2": {
                                    "name": "Med Instance",
                                    "displaytext": "Med Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 1000,    # In MHz
                                    "memory": 1024,    # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small Disk",
                                    "name": "Small Disk",
                                    "disksize": 1,
                                    "storagetype": "shared",
                        },
                        "disk_offering2": {
                                    "displaytext": "Med Disk",
                                    "name": "Med Disk",
                                    "disksize": 5,
                                    "storagetype": "shared",
                        },
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended in create account to
                                    # ensure unique username generated each time
                                    "password": "password",
                        },
                        "virtual_machine": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "template": {
                                    "displaytext": "Public Template",
                                    "name": "Public template",
                                    "ostype": 'CentOS 5.3 (64-bit)',
                                    "isfeatured": True,
                                    "ispublic": True,
                                    "isextractable": True,
                                    "templatefilter": 'self',
                        },
                        "volume": {
                                   "diskname": "TestDiskServ",
                                   "size": 1,    # GBs
                        },
                        "diskdevice": "/dev/xvda",
                        "rootdisk": "/dev/xvda",

                        "mount_dir": "/mnt/tmp",
                        "sub_dir": "test",
                        "sub_lvl_dir1": "test1",
                        "sub_lvl_dir2": "test2",
                        "random_data": "random.data",

                        "ostype": 'CentOS 5.3 (64-bit)',
                        "NumberOfThreads": 1,
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced',
                        # Networking mode: Advanced, Basic
                }

class TestSnapshotOnRootVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshotOnRootVolume, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.template = get_template(
                                    cls.api_client,
                                    cls.zone.id,
                                    cls.services["ostype"])
        cls.account = Account.create(cls.api_client,
                                     cls.services["account"],
                                     domainid=cls.domain.id)
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"])
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"],
                                    domainid=cls.domain.id)
        cls.service_offering2 = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering2"])
        cls.disk_offering2 = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering2"],
                                    domainid=cls.domain.id)

        cls._cleanup = [cls.account,
                        cls.service_offering,
                        cls.disk_offering,
                        cls.service_offering2,
                        cls.disk_offering2]

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("snapshots are not supported on %s" %
                self.hypervisor)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_snapshot_on_rootVolume(self):
        """Test create VM with default cent os template and create snapshot
            on root disk of the vm
        """
        # Validate the following
        # 1. Deploy a Linux VM using default CentOS template, use small service
        #    offering, disk offering
        # 2. Create snapshot on the root disk of this newly cteated vm
        # 3. listSnapshots should list the snapshot that was created.
        # 4. verify that secondary storage NFS share contains the reqd
        # volume under /secondary/snapshots/$accountid/$volumeid/$snapshot_uuid
        # 5. verify backup_snap_id was non null in the `snapshots` table

        # Create virtual machine with small systerm offering and disk offering
        new_virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    zoneid=self.zone.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    diskofferingid=self.disk_offering.id,
                                )
        self.debug("Virtual machine got created with id: %s" %
                                                    new_virtual_machine.id)
        list_virtual_machine_response = VirtualMachine.list(
                                                    self.apiclient,
                                                    id=new_virtual_machine.id)
        self.assertEqual(isinstance(list_virtual_machine_response, list),
                         True,
                         "Check listVirtualMachines returns a valid list")

        self.assertNotEqual(len(list_virtual_machine_response),
                            0,
                            "Check listVirtualMachines response")
        self.cleanup.append(new_virtual_machine)

        # Getting root volume id of the vm created above
        list_volume_response = Volume.list(
                                self.apiclient,
                                virtualmachineid=list_virtual_machine_response[0].id,
                                type="ROOT",
                                account=self.account.name,
                                domainid=self.account.domainid)

        self.assertEqual(isinstance(list_volume_response, list),
                         True,
                         "Check listVolumes returns a valid list")
        self.assertNotEqual(len(list_volume_response),
                            0,
                            "Check listVolumes response")
        self.debug(
            "Snapshot will be created on the volume with voluem id: %s" %
                                                    list_volume_response[0].id)

        # Perform snapshot on the root volume
        root_volume_snapshot = Snapshot.create(
                                        self.apiclient,
                                       volume_id=list_volume_response[0].id)
        self.debug("Created snapshot: %s for vm: %s" % (
                                        root_volume_snapshot.id,
                                        list_virtual_machine_response[0].id))
        list_snapshot_response = Snapshot.list(
                                        self.apiclient,
                                        id=root_volume_snapshot.id,
                                        account=self.account.name,
                                        domainid=self.account.domainid)
        self.assertEqual(isinstance(list_snapshot_response, list),
                         True,
                         "Check listSnapshots returns a valid list")

        self.assertNotEqual(len(list_snapshot_response),
                            0,
                            "Check listSnapshots response")
        # Verify Snapshot state
        self.assertEqual(
                            list_snapshot_response[0].state in [
                                                                'BackedUp',
                                                                'CreatedOnPrimary'
                                                                ],
                            True,
                            "Snapshot state is not as expected. It is %s" %
                            list_snapshot_response[0].state
                        )

        self.assertEqual(
                list_snapshot_response[0].volumeid,
                list_volume_response[0].id,
                "Snapshot volume id is not matching with the vm's volume id")
        self.cleanup.append(root_volume_snapshot)

        # Below code is to verify snapshots in the backend and in db.
        # Verify backup_snap_id field in the snapshots table for the snapshot created, it should not be null

        self.debug("select id, removed, backup_snap_id from snapshots where uuid = '%s';" % root_volume_snapshot.id)
        qryresult = self.dbclient.execute("select id, removed, backup_snap_id from snapshots where uuid = '%s';" % root_volume_snapshot.id)
        self.assertNotEqual(len(qryresult), 0, "Check sql query to return snapshots list")
        snapshot_qry_response = qryresult[0]
        snapshot_id = snapshot_qry_response[0]
        is_removed = snapshot_qry_response[1]
        backup_snap_id = snapshot_qry_response[2]
        self.assertNotEqual(is_removed, "NULL", "Snapshot is removed from CS, please check the logs")
        msg = "Backup snapshot id is set to null for the backedup snapshot :%s" % snapshot_id
        self.assertNotEqual(backup_snap_id, "NULL", msg )

        # Check if the snapshot is present on the secondary storage
        self.assertTrue(is_snapshot_on_nfs(self.apiclient, self.dbclient, self.config, self.zone.id, root_volume_snapshot.id))

        return

class TestCreateSnapshot(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateSnapshot, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls._cleanup.append(cls.service_offering)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Snapshots are not supported on %s"
                    % self.hypervisor)

        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )
        self.cleanup.append(self.account)

        self.apiclient = self.testClient.getUserApiClient(
                                UserName=self.account.name,
                                DomainName=self.account.domain)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def create_VM(self, host_id=None):
        try:
            self.debug('Creating VM for account=%s' %
                                            self.account.name)
            vm = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    hostid=host_id,
                                    mode=self.services["mode"]
                                    )
            self.debug('Created VM=%s in account=%s' %
                                        (vm.id, self.account.name))
            return vm
        except Exception as e:
            self.fail('Unable to deploy VM in a account=%s - %s' %
                                                (self.account.name, e))

    def stop_VM(self, virtual_machine):
        """ Return Stop Virtual Machine command"""

        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = virtual_machine.id
        return cmd

    def create_Snapshot_On_Root_Disk(self, virtual_machine):
        try:
            volumes = Volume.list(
                                  self.apiclient,
                                  virtualmachineid=virtual_machine.id,
                                  type='ROOT',
                                  listall=True
                                  )
            self.assertEqual(
                            isinstance(volumes, list),
                            True,
                            "Check list response returns a valid list"
                        )
            volume = volumes[0]

            cmd = createSnapshot.createSnapshotCmd()
            cmd.volumeid = volume.id
            cmd.account = self.account.name
            cmd.domainid = self.account.domainid
            return cmd
        except Exception as e:
            self.fail('Unable to create new job for snapshot: %s' % e)

    def create_Template_from_Snapshot(self, snapshot):
        try:
            self.debug("Creating template from snapshot: %s" % snapshot.name)

            cmd = createTemplate.createTemplateCmd()
            cmd.displaytext = self.services["template"]["displaytext"]
            cmd.name = "-".join([self.services["template"]["name"],
                                 random_gen()])

            ncmd = listOsTypes.listOsTypesCmd()
            ncmd.description = self.services["template"]["ostype"]
            ostypes = self.apiclient.listOsTypes(ncmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                                        self.services["template"]["ostype"])
            cmd.ostypeid = ostypes[0].id
            cmd.snapshotid = snapshot.id

            return cmd
        except Exception as e:
            self.fail("Failed to create template from snapshot: %s - %s" %
                                                        (snapshot.name, e))

    def create_Volume_from_Snapshot(self, snapshot):
        try:
            self.debug("Creating volume from snapshot: %s" % snapshot.name)

            cmd = createVolume.createVolumeCmd()
            cmd.name = "-".join([
                                self.services["volume"]["diskname"],
                                random_gen()])
            cmd.snapshotid = snapshot.id
            cmd.zoneid = self.zone.id
            cmd.size = self.services["volume"]["size"]
            cmd.account = self.account.name
            cmd.domainid = self.account.domainid
            return cmd
        except Exception as e:
            self.fail("Failed to create volume from snapshot: %s - %s" %
                                                        (snapshot.name, e))

    def create_Snapshot_VM(self):
        """Creates a virtual machine and take a snapshot on root disk

            1. Create a virtual machine
            2. SSH into virtual machine
            3. Create dummy folders on the ROOT disk of the virtual machine
            4. Take a snapshot of ROOT disk"""

        jobs = []
        self.debug("Deploying VM for account: %s" % self.account.name)
        for i in range(self.services["NumberOfThreads"]):
            vm = self.create_VM()

            self.debug("Create snapshot on ROOT disk")
            jobs.append(self.create_Snapshot_On_Root_Disk(vm))

        # Submit snapshot job at one go
        self.testClient.submitCmdsAndWait(jobs)
        return

    def create_Snapshot_Stop_VM(self):
        """Creates a snapshot on ROOT disk while vm is in stopping state

            1. Create a virtual machine
            2. SSH into virtual machine
            3. Create dummy folders on the ROOT disk of the virtual machine
            4. Create snapshot on ROOT disk
            5. Stop virtual machine while snapshots are taken on ROOT disk"""


        jobs = []
        self.debug("Deploying VM for account: %s" % self.account.name)
        for i in range(self.services["NumberOfThreads"]):
            vm = self.create_VM()

            self.debug("Create thread to stop virtual machine: %s" % vm.name)
            jobs.append(self.stop_VM(vm))

            self.debug("Create snapshot on ROOT disk")
            jobs.append(self.create_Snapshot_On_Root_Disk(vm))

        self.debug("Running concurrent migration jobs in account: %s" %
                                                    self.account.name)
        # Submit snapshot job at one go
        self.testClient.submitCmdsAndWait(jobs)

        return

    def get_Snapshots_For_Account(self, account, domainid):
        try:
            snapshots = list_snapshots(
                                      self.apiclient,
                                      account=account,
                                      domainid=domainid,
                                      listall=True,
                                      key='type',
                                      value='manual'
                                      )
            self.debug("List Snapshots result : %s" % snapshots)
            self.assertEqual(
                             isinstance(snapshots, list),
                             True,
                             "List snapshots shall return a valid list"
                             )
            return snapshots
        except Exception as e:
            self.fail("Failed to fetch snapshots for account: %s - %s" %
                                                                (account, e))

    def verify_Snapshots(self):
        try:
            self.debug("Listing snapshots for accout : %s" % self.account.name)
            snapshots = self.get_Snapshots_For_Account(
                                            self.account.name,
                                            self.account.domainid)
            self.assertEqual(
                    len(snapshots),
                    int(self.services["NumberOfThreads"]),
                    "No of snapshots should equal to no of threads spawned"
                 )
        except Exception as e:
            self.fail("Failed to verify snapshots created: %s" % e)

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"])
    @attr(configuration='concurrent.snapshots.threshold.perhost')
    def test_01_concurrent_snapshots_live_migrate(self):
        """Test perform concurrent snapshots and migrate the vm from one host
            to another

            1.Configure the concurrent.snapshots.threshold.perhost=3
            2.Deploy a Linux VM using default CentOS template, use small
            service offering, disk offering
            3.Perform snapshot on the root disk of this newly created VMs"""

        # Validate the following
        # a. Check all snapshots jobs are running concurrently on backgrounds
        # b. listSnapshots should list this newly created snapshot.
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        self.debug("Create virtual machine and snapshot on ROOT disk volume")
        self.create_Snapshot_VM()

        self.debug("Verify whether snapshots were created properly or not?")
        self.verify_Snapshots()
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"])
    @attr(configuration='concurrent.snapshots.threshold.perhost')
    def test_02_stop_vm_concurrent_snapshots(self):
        """Test stop running VM while performing concurrent snapshot on volume

            1.Configure the concurrent.snapshots.threshold.perhost=3
            2.Deploy a Linux VM using default CentOS template, use small
            service offering, disk offering
            3.Perform snapshot on root disk of this newly created VM
            4.stop the running Vms while snapshot on volume in progress
        """

        # Validate the following
        # a. check all snapshots jobs are running concurrently on back grounds
        # b. listSnapshots should list this newly created snapshot.

        self.debug("Create virtual machine and snapshot on ROOT disk volume")
        self.create_Snapshot_Stop_VM()

        self.debug("Verify whether snapshots were created properly or not?")
        self.verify_Snapshots()
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"])
    @attr(configuration='concurrent.snapshots.threshold.perhost')
    def test_03_concurrent_snapshots_create_template(self):
        """Test while parent concurrent snapshot job in progress,create
            template from completed snapshot

            1.Configure the concurrent.snapshots.threshold.perhost=3
            2.Deploy a Linux VM using default CentOS template, use small
            service offering, disk offering
            3.Perform snapshot on root disk of this newly created VMs(10 vms)
            4.while parent concurrent snapshot job in progress,create template
            from completed snapshot"""

        # Validate the following
        # a.Able to create Template from snapshots
        # b.check all snapshots jobs are running concurrently on back grounds
        # c.listSnapshots should list this newly created snapshot.

        self.debug("Create virtual machine and snapshot on ROOT disk")
        self.create_Snapshot_VM()

        self.debug("Verify whether snapshots were created properly or not?")
        self.verify_Snapshots()

        self.debug("Fetch the list of snapshots belong to account: %s" %
                                                    self.account.name)
        snapshots = self.get_Snapshots_For_Account(
                                                self.account.name,
                                                self.account.domainid)
        jobs = []
        for snapshot in snapshots:
            self.debug("Create a template from snapshot: %s" % snapshot.name)
            jobs.append(self.create_Template_from_Snapshot(snapshot))

        # Verify IO usage by submitting the concurrent jobs
        self.testClient.submitCmdsAndWait(jobs)

        self.debug("Verifying if templates are created properly or not?")
        templates = Template.list(
                            self.apiclient,
                            templatefilter=self.services["template"]["templatefilter"],
                            account=self.account.name,
                            domainid=self.account.domainid,
                            listall=True)
        self.assertNotEqual(templates,
                            None,
                            "Check if result exists in list item call")
        for template in templates:
            self.assertEqual(template.isready,
                         True,
                        "Check new template state in list templates call")

        self.debug("Test completed successfully.")
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns"])
    @attr(configuration='concurrent.snapshots.threshold.perhost')
    def test_04_concurrent_snapshots_create_volume(self):
        """Test while parent concurrent snapshot job in progress,create volume
            from completed snapshot

            1.Configure the concurrent.snapshots.threshold.perhost=3
            2.Deploy a Linux VM using default CentOS template, use small
            service offering, disk offering.
            3.Perform snapshot on root disk of this newly created VM
            4.while parent concurrent snapshot job in progress,create volume
            from completed snapshot"""

        # Validate the following
        # a.Able to create Volume from snapshots
        # b.check all snapshots jobs are running concurrently on back grounds
        # c.listSnapshots should list this newly created snapshot.

        self.debug("Create virtual machine and snapshot on ROOT disk thread")
        self.create_Snapshot_VM()

        self.debug("Verify whether snapshots were created properly or not?")
        self.verify_Snapshots()

        self.debug("Fetch the list of snapshots belong to account: %s" %
                                                    self.account.name)
        snapshots = self.get_Snapshots_For_Account(
                                                self.account.name,
                                                self.account.domainid)
        jobs = []
        for snapshot in snapshots:
            self.debug("Create a volume from snapshot: %s" % snapshot.name)
            jobs.append(self.create_Volume_from_Snapshot(snapshot))

        # Verify IO usage by submitting the concurrent jobs
        self.testClient.submitCmdsAndWait(jobs)

        self.debug("Verifying if volume created properly or not?")
        volumes = Volume.list(self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True,
                              type='ROOT')

        self.assertNotEqual(volumes,
                            None,
                            "Check if result exists in list item call")
        for volume in volumes:
            self.debug("Volume: %s, state: %s" % (volume.name, volume.state))
            self.assertEqual(volume.state,
                         "Ready",
                         "Check new volume state in list volumes call")

        self.debug("Test completed successfully.")
        return
