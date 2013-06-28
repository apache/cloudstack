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
""" BVT tests for Virtual Machine Life Cycle
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True
class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {
                "disk_offering":{
                    "displaytext": "Small",
                    "name": "Small",
                    "disksize": 1
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
                "small":
                # Create a small virtual machine instance with disk offering 
                {
                    "displayname": "testserver",
                    "username": "root", # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "medium":   # Create a medium virtual machine instance 
                {
                    "displayname": "testserver",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "service_offerings":
                {
                 "tiny":
                   {
                        "name": "Tiny Instance",
                        "displaytext": "Tiny Instance",
                        "cpunumber": 1,
                        "cpuspeed": 100, # in MHz
                        "memory": 128, # In MBs
                    },
                 "small":
                    {
                     # Small service offering ID to for change VM 
                     # service offering from medium to small
                        "name": "Small Instance",
                        "displaytext": "Small Instance",
                        "cpunumber": 1,
                        "cpuspeed": 100,
                        "memory": 256,
                    },
                "medium":
                    {
                    # Medium service offering ID to for
                    # change VM service offering from small to medium
                        "name": "Medium Instance",
                        "displaytext": "Medium Instance",
                        "cpunumber": 1,
                        "cpuspeed": 100,
                        "memory": 256,
                    }
                },
                "iso":  # ISO settings for Attach/Detach ISO tests
                {
                    "displaytext": "Test ISO",
                    "name": "testISO",
                    "url": "http://people.apache.org/~tsp/dummy.iso",
                     # Source URL where ISO is located
                    "ostype": 'CentOS 5.3 (64-bit)',
                    "mode": 'HTTP_DOWNLOAD', # Downloading existing ISO 
                },
                "template": {
                    "displaytext": "Cent OS Template",
                    "name": "Cent OS Template",
                    "passwordenabled": True,
                },
            "diskdevice": ['/dev/xvdd', '/dev/cdrom', '/dev/sr0', '/dev/cdrom1' ],
            # Disk device where ISO is attached to instance
            "mount_dir": "/mnt/tmp",
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }


class TestDeployVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.apiclient = super(TestDeployVM, cls).getClsTestClient().getApiClient()
        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient, cls.services)
        cls.zone = get_zone(cls.apiclient, cls.services)
        cls.services['mode'] = cls.zone.networktype

        #If local storage is enabled, alter the offerings to use localstorage
        #this step is needed for devcloud
        if cls.zone.localstorageenabled == True:
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.services["service_offerings"]["small"]["storagetype"] = 'local'
            cls.services["service_offerings"]["medium"]["storagetype"] = 'local'

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["medium"]["zoneid"] = cls.zone.id
        cls.services["medium"]["template"] = template.id
        cls.services["iso"]["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )
        cls.debug(cls.account.id)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services['mode']
        )

        cls.cleanup = [
            cls.service_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            cls.debug("Warning! Exception in tearDown: %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()


    @attr(tags = ["simulator", "devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_deploy_vm(self):
        """Test Deploy Virtual Machine
        """
        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. listVirtualMachines returns accurate information
        list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.virtual_machine.id
                                                 )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )
        vm_response = list_vm_response[0]
        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )
        self.assertEqual(
                    vm_response.name,
                    self.virtual_machine.name,
                    "Check virtual machine name in listVirtualMachines"
                    )
        self.assertEqual(
            vm_response.state,
            'Running',
             msg="VM is not in Running state"
        )
        return


    @attr(tags = ["simulator", "advanced"])
    def test_advZoneVirtualRouter(self):
        """
        Test advanced zone virtual router
        1. Is Running
        2. is in the account the VM was deployed in
        3. Has a linklocalip, publicip and a guestip
        @return:
        """
        routers = list_routers(self.apiclient, account=self.account.name)
        self.assertTrue(len(routers) > 0, msg = "No virtual router found")
        router = routers[0]

        self.assertEqual(router.state, 'Running', msg="Router is not in running state")
        self.assertEqual(router.account, self.account.name, msg="Router does not belong to the account")

        #Has linklocal, public and guest ips
        self.assertIsNotNone(router.linklocalip, msg="Router has no linklocal ip")
        self.assertIsNotNone(router.publicip, msg="Router has no public ip")
        self.assertIsNotNone(router.guestipaddress, msg="Router has no guest ip")

    @attr(hypervisor = ["simulator"])
    @attr(mode = ["basic"])
    def test_basicZoneVirtualRouter(self):
        """
        Tests for basic zone virtual router
        1. Is Running
        2. is in the account the VM was deployed in
        @return:
        """
        routers = list_routers(self.apiclient, account=self.account.name)
        self.assertTrue(len(routers) > 0, msg = "No virtual router found")
        router = routers[0]

        self.assertEqual(router.state, 'Running', msg="Router is not in running state")
        self.assertEqual(router.account, self.account.name, msg="Router does not belong to the account")

    def tearDown(self):
        pass


class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestVMLifeCycle, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

        #if local storage is enabled, alter the offerings to use localstorage
        #this step is needed for devcloud
        if cls.zone.localstorageenabled == True:
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.services["service_offerings"]["small"]["storagetype"] = 'local'
            cls.services["service_offerings"]["medium"]["storagetype"] = 'local'

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["medium"]["zoneid"] = cls.zone.id
        cls.services["medium"]["template"] = template.id
        cls.services["iso"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=domain.id
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["small"]
                                    )

        cls.medium_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["medium"]
                                    )
        #create small and large virtual machines
        cls.small_virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.name,
                                        domainid=cls.account.domainid,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls.medium_virtual_machine = VirtualMachine.create(
                                       cls.api_client,
                                       cls.services["medium"],
                                       accountid=cls.account.name,
                                       domainid=cls.account.domainid,
                                       serviceofferingid=cls.medium_offering.id,
                                       mode=cls.services["mode"]
                                    )
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.name,
                                        domainid=cls.account.domainid,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls._cleanup = [
                        cls.small_offering,
                        cls.medium_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(TestVMLifeCycle, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_01_stop_vm(self):
        """Test Stop Virtual Machine
        """

        # Validate the following
        # 1. Should Not be able to login to the VM.
        # 2. listVM command should return
        #    this VM.State of this VM should be ""Stopped"".

        self.debug("Stopping VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.stop(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )

        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in stopped state"
                        )
        return

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_02_start_vm(self):
        """Test Start Virtual Machine
        """
        # Validate the following
        # 1. listVM command should return this VM.State
        #    of this VM should be Running".

        self.debug("Starting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.start(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.small_virtual_machine.id
                )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_03_reboot_vm(self):
        """Test Reboot Virtual Machine
        """

        # Validate the following
        # 1. Should be able to login to the VM.
        # 2. listVM command should return the deployed VM.
        #    State of this VM should be "Running"

        self.debug("Rebooting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.reboot(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_06_destroy_vm(self):
        """Test destroy Virtual Machine
        """

        # Validate the following
        # 1. Should not be able to login to the VM.
        # 2. listVM command should return this VM.State
        #    of this VM should be "Destroyed".

        self.debug("Destroy VM - ID: %s" % self.small_virtual_machine.id)
        self.small_virtual_machine.delete(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Destroyed",
                            "Check virtual machine is in destroyed state"
                        )
        return

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_07_restore_vm(self):
        """Test recover Virtual Machine
        """

        # Validate the following
        # 1. listVM command should return this VM.
        #    State of this VM should be "Stopped".
        # 2. We should be able to Start this VM successfully.

        self.debug("Recovering VM - ID: %s" % self.small_virtual_machine.id)

        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in Stopped state"
                        )

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg", "multihost"])
    def test_08_migrate_vm(self):
        """Test migrate VM
        """
        # Validate the following
        # 1. Environment has enough hosts for migration
        # 2. DeployVM on suitable host (with another host in the cluster)
        # 3. Migrate the VM and assert migration successful

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing'
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check the number of hosts in the zone"
        )
        self.assertGreaterEqual(
            len(hosts),
            2,
            "Atleast 2 hosts should be present for VM migration"
        )

        #identify suitable host
        clusters = [h.clusterid for h in hosts]
        #find hosts withe same clusterid
        clusters = [cluster for index, cluster in enumerate(clusters) if clusters.count(cluster) > 1]

        if len(clusters) <= 1:
            self.skipTest("Migration needs a cluster with at least two hosts")

        suitable_hosts = [host for host in hosts if host.clusterid == clusters[0]]
        target_host = suitable_hosts[0]
        migrate_host = suitable_hosts[1]

        #deploy VM on target host
        self.vm_to_migrate = VirtualMachine.create(
            self.api_client,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"],
            hostid=target_host.id
        )
        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                        self.vm_to_migrate.id,
                                        migrate_host.id
                                        ))

        self.vm_to_migrate.migrate(self.api_client, migrate_host.id)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.vm_to_migrate.id
                                            )
        self.assertNotEqual(
                            list_vm_response,
                            None,
                            "Check virtual machine is listed"
                        )

        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.id,
                            self.vm_to_migrate.id,
                            "Check virtual machine ID of migrated VM"
                        )

        self.assertEqual(
                            vm_response.hostid,
                            migrate_host.id,
                            "Check destination hostID of migrated VM"
                        )
        return

    @attr(configuration = "expunge.interval")
    @attr(configuration = "expunge.delay")
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_09_expunge_vm(self):
        """Test destroy(expunge) Virtual Machine
        """
        # Validate the following
        # 1. listVM command should NOT  return this VM any more.

        self.debug("Expunge VM-ID: %s" % self.small_virtual_machine.id)

        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.destroyVirtualMachine(cmd)

        config = list_configurations(
                                     self.apiclient,
                                     name='expunge.delay'
                                     )

        expunge_delay = int(config[0].value)
        time.sleep(expunge_delay * 2)

        #VM should be destroyed unless expunge thread hasn't run
        #Wait for two cycles of the expunge thread
        config = list_configurations(
                                     self.apiclient,
                                     name='expunge.interval'
                                     )
        expunge_cycle = int(config[0].value)
        wait_time = expunge_cycle * 2
        while wait_time >= 0:
            list_vm_response = list_virtual_machines(
                                                self.apiclient,
                                                id=self.small_virtual_machine.id
                                                )
            if list_vm_response:
                time.sleep(expunge_cycle)
                wait_time = wait_time - expunge_cycle
            else:
                break

        self.debug("listVirtualMachines response: %s" % list_vm_response)

        self.assertEqual(
                        list_vm_response,
                        None,
                        "Check Expunged virtual machine is in listVirtualMachines response"
                    )
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg", "needle"])
    def test_10_attachAndDetach_iso(self):
        """Test for attach and detach ISO to virtual machine"""

        # Validate the following
        # 1. Create ISO
        # 2. Attach ISO to VM
        # 3. Log in to the VM.
        # 4. The device should be available for use
        # 5. Detach ISO
        # 6. Check the device is properly detached by logging into VM

        iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=self.account.name,
                         domainid=self.account.domainid
                         )

        self.debug("Successfully created ISO with ID: %s" % iso.id)
        try:
            iso.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s"\
                      % (iso.id, e))

        self.debug("Attach ISO with ID: %s to VM ID: %s" % (
                                                    iso.id,
                                                    self.virtual_machine.id
                                                    ))
        #Attach ISO to virtual machine
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.attachIso(cmd)

        #determine device type from hypervisor
        hosts = Host.list(self.apiclient, id=self.virtual_machine.hostid)
        self.assertTrue(isinstance(hosts, list))
        self.assertTrue(len(hosts) > 0)
        self.debug("Found %s host" % hosts[0].hypervisor)

        if hosts[0].hypervisor.lower() == "kvm":
            self.services["diskdevice"] = "/dev/vdb"

        try:
            ssh_client = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                                (self.virtual_machine.ipaddress, e))

        cmds = "mkdir -p %s" % self.services["mount_dir"]
        self.assert_(ssh_client.execute(cmds) == [], "mkdir failed within guest")

        for diskdevice in self.services["diskdevice"]:
            res = ssh_client.execute("mount -rt iso9660 {} {}".format(diskdevice, self.services["mount_dir"]))
            if res == []:
                self.services["mount"] = diskdevice
                break
        else:
            self.skipTest("No mount points matched. Mount was unsuccessful")

        c = "fdisk -l|grep %s|head -1" % self.services["mount"]
        res = ssh_client.execute(c)
        self.debug("Found a mount point at %s" % res)

        # Res may contain more than one strings depending on environment
        # Split strings to form new list which is used for assertion on ISO size 
        result = []
        for i in res:
            for k in i.split():
                result.append(k)

        # Get ISO size
        iso_response = list_isos(
                                 self.apiclient,
                                 id=iso.id
                                 )
        self.assertEqual(
                            isinstance(iso_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        iso_size = iso_response[0].size

        self.assertEqual(
                         str(iso_size) in result,
                         True,
                         "Check size of the attached ISO"
                         )
        try:
            #Unmount ISO
            command = "umount %s" % self.services["mount_dir"]
            ssh_client.execute(command)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                                (self.virtual_machine.ipaddress, e))

        #Detach from VM
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.detachIso(cmd)

        try:
            res = ssh_client.execute(c)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                                (self.virtual_machine.ipaddress, e))

        # Check if ISO is properly detached from VM (using fdisk)
        result = self.services["mount"] in str(res)

        self.assertEqual(
                         result,
                         False,
                         "Check if ISO is detached from virtual machine"
                         )
        return
