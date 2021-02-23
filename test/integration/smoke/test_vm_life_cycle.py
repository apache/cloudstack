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
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (recoverVirtualMachine,
                                  destroyVirtualMachine,
                                  attachIso,
                                  detachIso,
                                  provisionCertificate,
                                  updateConfiguration,
                                  migrateVirtualMachine,
                                  migrateVirtualMachineWithVolume,
                                  unmanageVirtualMachine,
                                  listUnmanagedInstances,
                                  listNics,
                                  listVolumes)
from marvin.lib.utils import *

from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Iso,
                             Router,
                             Configurations,
                             StoragePool,
                             Volume,
                             DiskOffering,
                             NetworkOffering,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template,
                               get_test_ovf_templates,
                               list_hosts,
                               list_virtual_machines,
                               get_vm_vapp_configs)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf
# Import System modules
import time
import json
from operator import itemgetter

_multiprocess_shared_ = True


class TestDeployVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()

        # If local storage is enabled, alter the offerings to use localstorage
        # this step is needed for devcloud
        if cls.zone.localstorageenabled == True:
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.services["service_offerings"]["small"]["storagetype"] = 'local'
            cls.services["service_offerings"]["medium"]["storagetype"] = 'local'

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
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
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_deploy_vm(self):
        """Test Deploy Virtual Machine
        """
        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. listVirtualMachines returns accurate information
        list_vm_response = VirtualMachine.list(
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

    @attr(tags=["advanced"], required_hardware="false")
    def test_advZoneVirtualRouter(self):
        # TODO: SIMENH: duplicate test, remove it
        """
        Test advanced zone virtual router
        1. Is Running
        2. is in the account the VM was deployed in
        3. Has a linklocalip, publicip and a guestip
        @return:
        """
        routers = Router.list(self.apiclient, account=self.account.name)
        self.assertTrue(len(routers) > 0, msg="No virtual router found")
        router = routers[0]

        self.assertEqual(router.state, 'Running', msg="Router is not in running state")
        self.assertEqual(router.account, self.account.name, msg="Router does not belong to the account")

        # Has linklocal, public and guest ips
        self.assertIsNotNone(router.linklocalip, msg="Router has no linklocal ip")
        self.assertIsNotNone(router.publicip, msg="Router has no public ip")
        self.assertIsNotNone(router.guestipaddress, msg="Router has no guest ip")

    @attr(mode=["basic"], required_hardware="false")
    def test_basicZoneVirtualRouter(self):
        # TODO: SIMENH: duplicate test, remove it
        """
        Tests for basic zone virtual router
        1. Is Running
        2. is in the account the VM was deployed in
        @return:
        """
        routers = Router.list(self.apiclient, account=self.account.name)
        self.assertTrue(len(routers) > 0, msg="No virtual router found")
        router = routers[0]

        self.assertEqual(router.state, 'Running', msg="Router is not in running state")
        self.assertEqual(router.account, self.account.name, msg="Router does not belong to the account")

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware="false")
    def test_deploy_vm_multiple(self):
        """Test Multiple Deploy Virtual Machine

        # Validate the following:
        # 1. deploy 2 virtual machines
        # 2. listVirtualMachines using 'ids' parameter returns accurate information
        """
        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        virtual_machine1 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id
        )
        virtual_machine2 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id
        )

        list_vms = VirtualMachine.list(self.apiclient, ids=[virtual_machine1.id, virtual_machine2.id], listAll=True)
        self.debug(
            "Verify listVirtualMachines response for virtual machines: %s, %s" % (
                virtual_machine1.id, virtual_machine2.id)
        )
        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertEqual(
            len(list_vms),
            2,
            "List VM response was empty, expected 2 VMs"
        )

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVMLifeCycle, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        # if local storage is enabled, alter the offerings to use localstorage
        # this step is needed for devcloud
        if cls.zone.localstorageenabled == True:
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.services["service_offerings"]["small"]["storagetype"] = 'local'
            cls.services["service_offerings"]["medium"]["storagetype"] = 'local'

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls.medium_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["medium"]
        )
        # create small and large virtual machines
        cls.small_virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        cls.medium_virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.medium_offering.id,
            mode=cls.services["mode"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
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
        cls.apiclient = super(TestVMLifeCycle, cls).getClsTestClient().getApiClient()
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created ISOs
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_stop_vm(self):
        """Test Stop Virtual Machine
        """

        # Validate the following
        # 1. Should Not be able to login to the VM.
        # 2. listVM command should return
        #    this VM.State of this VM should be ""Stopped"".
        try:
            self.small_virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_stop_vm_forced(self):
        """Test Force Stop Virtual Machine
        """
        try:
            self.small_virtual_machine.stop(self.apiclient, forced=True)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        list_vm_response = VirtualMachine.list(
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
            "Check virtual machine is in stopped state"
        )
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_start_vm(self):
        """Test Start Virtual Machine
        """
        # Validate the following
        # 1. listVM command should return this VM.State
        #    of this VM should be Running".

        self.debug("Starting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.start(self.apiclient)

        list_vm_response = VirtualMachine.list(
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

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_reboot_vm(self):
        """Test Reboot Virtual Machine
        """

        # Validate the following
        # 1. Should be able to login to the VM.
        # 2. listVM command should return the deployed VM.
        #    State of this VM should be "Running"

        self.debug("Rebooting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.reboot(self.apiclient)

        list_vm_response = VirtualMachine.list(
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
            "Running",
            "Check virtual machine is in running state"
        )
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_destroy_vm(self):
        """Test destroy Virtual Machine
        """

        # Validate the following
        # 1. Should not be able to login to the VM.
        # 2. listVM command should return this VM.State
        #    of this VM should be "Destroyed".

        self.debug("Destroy VM - ID: %s" % self.small_virtual_machine.id)
        self.small_virtual_machine.delete(self.apiclient, expunge=False)

        list_vm_response = VirtualMachine.list(
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

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_restore_vm(self):
        # TODO: SIMENH: add another test the data on the restored VM.
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

        list_vm_response = VirtualMachine.list(
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

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg", "multihost"], required_hardware="false")
    def test_08_migrate_vm(self):
        """Test migrate VM
        """
        # Validate the following
        # 1. Environment has enough hosts for migration
        # 2. DeployVM on suitable host (with another host in the cluster)
        # 3. Migrate the VM and assert migration successful

        suitable_hosts = None

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing'
        )
        self.assertEqual(validateList(hosts)[0], PASS, "hosts list validation failed")

        if len(hosts) < 2:
            self.skipTest("At least two hosts should be present in the zone for migration")

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("Migration is not supported on LXC")

        # For KVM, two hosts used for migration should  be present in same cluster
        # For XenServer and VMware, migration is possible between hosts belonging to different clusters
        # with the help of XenMotion and Vmotion respectively.

        if self.hypervisor.lower() in ["kvm", "simulator"]:
            # identify suitable host
            clusters = [h.clusterid for h in hosts]
            # find hosts withe same clusterid
            clusters = [cluster for index, cluster in enumerate(clusters) if clusters.count(cluster) > 1]

            if len(clusters) <= 1:
                self.skipTest("In " + self.hypervisor.lower() + " Live Migration needs two hosts within same cluster")

            suitable_hosts = [host for host in hosts if host.clusterid == clusters[0]]
        else:
            suitable_hosts = hosts

        target_host = suitable_hosts[0]
        migrate_host = suitable_hosts[1]

        # deploy VM on target host
        vm_to_migrate = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"],
            hostid=target_host.id
        )
        self.debug("Migrating VM-ID: %s to Host: %s" % (
            vm_to_migrate.id,
            migrate_host.id
        ))

        vm_to_migrate.migrate(self.apiclient, migrate_host.id)

        retries_cnt = 3
        while retries_cnt >= 0:
            list_vm_response = VirtualMachine.list(self.apiclient,
                                                   id=vm_to_migrate.id)
            self.assertNotEqual(
                list_vm_response,
                None,
                "Check virtual machine is listed"
            )
            vm_response = list_vm_response[0]
            self.assertEqual(vm_response.id, vm_to_migrate.id, "Check virtual machine ID of migrated VM")
            self.assertEqual(vm_response.hostid, migrate_host.id, "Check destination hostID of migrated VM")
            retries_cnt = retries_cnt - 1
        return

    @attr(configuration="expunge.interval")
    @attr(configuration="expunge.delay")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_09_expunge_vm(self):
        """Test destroy(expunge) Virtual Machine
        """
        # Validate the following
        # 1. listVM command should NOT  return this VM any more.

        self.debug("Expunge VM-ID: %s" % self.small_virtual_machine.id)

        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.destroyVirtualMachine(cmd)

        config = Configurations.list(
            self.apiclient,
            name='expunge.delay'
        )

        expunge_delay = int(config[0].value)
        time.sleep(expunge_delay * 2)

        # VM should be destroyed unless expunge thread hasn't run
        # Wait for two cycles of the expunge thread
        config = Configurations.list(
            self.apiclient,
            name='expunge.interval'
        )
        expunge_cycle = int(config[0].value)
        wait_time = expunge_cycle * 4
        while wait_time >= 0:
            list_vm_response = VirtualMachine.list(
                self.apiclient,
                id=self.small_virtual_machine.id
            )
            if not list_vm_response:
                break
            self.debug("Waiting for VM to expunge")
            time.sleep(expunge_cycle)
            wait_time = wait_time - expunge_cycle

        self.debug("listVirtualMachines response: %s" % list_vm_response)

        self.assertEqual(list_vm_response, None, "Check Expunged virtual machine is in listVirtualMachines response")
        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_10_attachAndDetach_iso(self):
        """Test for attach and detach ISO to virtual machine"""

        # Validate the following
        # 1. Create ISO
        # 2. Attach ISO to VM
        # 3. Log in to the VM.
        # 4. The device should be available for use
        # 5. Detach ISO
        # 6. Check the device is properly detached by logging into VM

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("ISOs are not supported on LXC")

        iso = Iso.create(
            self.apiclient,
            self.services["iso1"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.debug("Successfully created ISO with ID: %s" % iso.id)
        try:
            iso.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s" \
                      % (iso.id, e))

        self.debug("Attach ISO with ID: %s to VM ID: %s" % (
            iso.id,
            self.virtual_machine.id
        ))
        # Attach ISO to virtual machine
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.attachIso(cmd)

        try:
            ssh_client = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (self.virtual_machine.ipaddress, e))
        mount_dir = "/mnt/tmp"
        cmds = "mkdir -p %s" % mount_dir
        self.assertTrue(ssh_client.execute(cmds) == [], "mkdir failed within guest")

        iso_unsupported = False
        for diskdevice in self.services["diskdevice"]:
            res = ssh_client.execute("mount -rt iso9660 {} {}".format(diskdevice, mount_dir))
            if res == []:
                self.services["mount"] = diskdevice
                break
            if str(res).find("mount: unknown filesystem type 'iso9660'") != -1:
                iso_unsupported = True
                self.debug("Test template does not supports iso9660 filesystem. Proceeding with test without mounting.")
                print("Test template does not supports iso9660 filesystem. Proceeding with test without mounting.")
                break
        else:
            self.fail("No mount points matched. Mount was unsuccessful")

        if iso_unsupported == False:
            c = "mount |grep %s|head -1" % self.services["mount"]
            res = ssh_client.execute(c)
            size = ssh_client.execute("du %s | tail -1" % self.services["mount"])
            self.debug("Found a mount point at %s with size %s" % (res, size))

            # Get ISO size
            iso_response = Iso.list(
                self.apiclient,
                id=iso.id
            )
            self.assertEqual(
                isinstance(iso_response, list),
                True,
                "Check list response returns a valid list"
            )

            try:
                # Unmount ISO
                command = "umount %s" % mount_dir
                ssh_client.execute(command)
            except Exception as e:
                self.fail("SSH failed for virtual machine: %s - %s" %
                          (self.virtual_machine.ipaddress, e))

        # Detach from VM
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.detachIso(cmd)

        if iso_unsupported == False:
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

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_11_destroy_vm_and_volumes(self):
        """Test destroy Virtual Machine and it's volumes
        """

        # Validate the following
        # 1. Deploys a VM and attaches disks to it
        # 2. Destroys the VM with DataDisks option

        small_disk_offering = DiskOffering.list(self.apiclient, name='Small')[0]

        small_virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
        )
        vol1 = Volume.create(
            self.apiclient,
            self.services,
            account=self.account.name,
            diskofferingid=small_disk_offering.id,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

        small_virtual_machine.attach_volume(self.apiclient, vol1)

        self.debug("Destroy VM - ID: %s" % small_virtual_machine.id)
        small_virtual_machine.delete(self.apiclient, volumeIds=vol1.id)

        self.assertEqual(VirtualMachine.list(self.apiclient, id=small_virtual_machine.id), None, "List response contains records when it should not")

        self.assertEqual(Volume.list(self.apiclient, id=vol1.id), None, "List response contains records when it should not")


class TestSecuredVmMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSecuredVmMigration, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        if cls.hypervisor.lower() not in ["kvm"]:
            return

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        cls.management_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestSecuredVmMigration, cls).getClsTestClient().getApiClient()
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.hypervisor.lower() not in ["kvm"]:
            self.skipTest("Secured migration is not supported on other than KVM")

        self.hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            hypervisor='KVM')

        if len(self.hosts) < 2:
            self.skipTest("Requires at least two hosts for performing migration related tests")

        self.secure_all_hosts()
        self.updateConfiguration("ca.plugin.root.auth.strictness", "false")

    def tearDown(self):
        self.secure_all_hosts()
        self.updateConfiguration("ca.plugin.root.auth.strictness", "true")
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def get_target_host(self, secured, virtualmachineid):
        target_hosts = Host.listForMigration(self.apiclient,
                                             virtualmachineid=virtualmachineid)
        for host in target_hosts:
            h = list_hosts(self.apiclient, type='Routing', id=host.id)[0]
            if h.details.secured == secured:
                return h

        cloudstackTestCase.skipTest(self, "No target hosts available, skipping test.")

    def check_migration_protocol(self, protocol, host):
        resp = SshClient(host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]) \
            .execute("grep -a listen_%s=1 /etc/libvirt/libvirtd.conf | tail -1" % protocol)

        if protocol not in resp[0]:
            cloudstackTestCase.fail(self, "Libvirt listen protocol expected: '" + protocol + "\n"
                                                                                             "does not match actual: " +
                                    resp[0])

    def migrate_and_check(self, vm, src_host, dest_host, proto='tls'):
        """
            Migrates a VM from source host to destination host and checks status
        """
        self.check_migration_protocol(protocol=proto, host=src_host)
        vm.migrate(self.apiclient, hostid=dest_host.id)
        vm_response = VirtualMachine.list(self.apiclient, id=vm.id)[0]
        self.assertEqual(vm_response.hostid, dest_host.id, "Check destination host ID of migrated VM")

    def waitUntilHostInState(self, hostId, state="Up", interval=5, retries=20):
        while retries > -1:
            print("Waiting for host: %s to be %s. %s retries left." % (hostId, state, retries))
            time.sleep(interval)
            host = Host.list(
                self.apiclient,
                hostid=hostId,
                type='Routing'
            )[0]
            if host.state != state:
                if retries >= 0:
                    retries = retries - 1
                    continue
            else:
                print("Host %s now showing as %s" % (hostId, state))
                return

    def unsecure_host(self, host):
        SshClient(host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]) \
            .execute("rm -f /etc/cloudstack/agent/cloud* && \
                      sed -i 's/listen_tls.*/listen_tls=0/g' /etc/libvirt/libvirtd.conf && \
                      sed -i 's/listen_tcp.*/listen_tcp=1/g' /etc/libvirt/libvirtd.conf && \
                      sed -i '/.*_file=.*/d' /etc/libvirt/libvirtd.conf && \
                      service libvirtd restart && \
                      sleep 30 && \
                      service cloudstack-agent restart")
        print("Unsecuring Host: %s" % (host.name))
        self.waitUntilHostInState(hostId=host.id, state="Up")
        self.check_connection(host=host, secured='false')
        return host

    def secure_all_hosts(self):
        for host in self.hosts:
            cmd = provisionCertificate.provisionCertificateCmd()
            cmd.hostid = host.id
            cmd.reconnect = True
            self.apiclient.provisionCertificate(cmd)

        for host in self.hosts:
            print("Securing Host %s" % host.name)
            self.waitUntilHostInState(hostId=host.id, state="Up")
            self.check_connection(secured='true', host=host)

    def deploy_vm(self, origin_host):
        return VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"],
            hostid=origin_host.id)

    def check_connection(self, secured, host, retries=20, interval=6):

        while retries > -1:
            time.sleep(interval)
            host = Host.list(
                self.apiclient,
                zoneid=self.zone.id,
                hostid=host.id,
                type='Routing'
            )[0]
            if host.details.secured != secured:
                if retries >= 0:
                    retries = retries - 1
                    continue
            else:
                return

        raise Exception("Host detail 'secured' was expected: " + secured +
                        ", actual is: " + host.details.secured)

    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        self.apiclient.updateConfiguration(cmd)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_01_secure_vm_migration(self):
        """Test secure VM migration"""
        # Validate the following
        # 1. Environment has enough hosts for migration
        # 2. DeployVM on suitable host (with another host in the cluster)
        # 3. Migrate the VM and assert migration successful

        src_host = self.hosts[0]
        vm = self.deploy_vm(src_host)
        self.cleanup.append(vm)

        self.debug("Securing Host(s)")
        dest_host = self.get_target_host(secured='true', virtualmachineid=vm.id)
        self.migrate_and_check(vm, src_host, dest_host)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_02_unsecure_vm_migration(self):
        """Test Non-secured VM Migration
        """
        # Validate the following
        # 1. Prepare 2 hosts to run in non-secured more
        # 2. DeployVM on suitable host (with another host in the cluster)
        # 3. Migrate the VM and assert migration successful

        for host in self.hosts:
            self.unsecure_host(host)

        src_host = self.hosts[0]
        vm = self.deploy_vm(src_host)
        self.cleanup.append(vm)

        dest_host = self.get_target_host(secured='false', virtualmachineid=vm.id)
        self.migrate_and_check(vm, src_host, dest_host, proto='tcp')

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_03_secured_to_nonsecured_vm_migration(self):
        """Test destroy Virtual Machine
        """
        # Validate the following
        # 1. Makes one of the hosts non-secured
        # 2. Deploys a VM to a Secured host
        # 3. Migrates the VM to the non-secured host via TLS, and ensure exception

        unsecure_host = self.unsecure_host(self.hosts[0])
        secure_host = self.hosts[1]

        vm = self.deploy_vm(secure_host)
        self.cleanup.append(vm)

        try:
            self.migrate_and_check(vm, secure_host, unsecure_host, proto='tls')
        except Exception:
            pass
        else:
            self.fail("Migration succeeded, instead it should fail")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_04_nonsecured_to_secured_vm_migration(self):
        """Test Non-secured VM Migration
        """
        # Validate the following
        # 1. Makes one of the hosts non-secured
        # 2. Deploys a VM to the non-secured host
        # 3. Migrates the VM to the non-secured host via TCP, and ensure exception

        unsecure_host = self.unsecure_host(self.hosts[0])
        secure_host = self.hosts[1]

        vm = self.deploy_vm(unsecure_host)
        self.cleanup.append(vm)

        try:
            self.migrate_and_check(vm, unsecure_host, secure_host, proto='tcp')
        except Exception:
            pass
        else:
            self.fail("Migration succeeded, instead it should fail")


class TestMigrateVMwithVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestMigrateVMwithVolume, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        cls.management_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        super(TestMigrateVMwithVolume,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.hypervisor.lower() not in ["vmware"]:
            self.skipTest("VM Migration with Volumes is not supported on other than VMware")

    def tearDown(self):
        super(TestMigrateVMwithVolume,self).tearDown()

    def get_target_host(self, virtualmachineid):
        target_hosts = Host.listForMigration(self.apiclient,
                                             virtualmachineid=virtualmachineid)[0]
        if len(target_hosts) < 1:
            self.skipTest("No target hosts found")

        return target_hosts[0]

    def get_target_pool(self, volid):
        target_pools = StoragePool.listForMigration(self.apiclient, id=volid)

        if len(target_pools) < 1:
            self.skipTest("Not enough storage pools found")

        return target_pools[0]

    def get_vm_volumes(self, id):
        return Volume.list(self.apiclient, virtualmachineid=id, listall=True)

    def deploy_vm(self):
        return VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"])

    def migrate_vm_with_pools(self, target_pool, id):
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()

        cmd.storageid = target_pool.id
        cmd.virtualmachineid = id

        return self.apiclient.migrateVirtualMachine(cmd)

    def create_volume(self):
        small_disk_offering = DiskOffering.list(self.apiclient, name='Small')[0]

        return Volume.create(
            self.apiclient,
            self.services,
            account=self.account.name,
            diskofferingid=small_disk_offering.id,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

    """
    BVT for Vmware Offline VM and Volume Migration
    """

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_01_migrate_VM_and_root_volume(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM
        # 2. Finds suitable host for migration
        # 3. Finds suitable storage pool for root volume
        # 4. Migrate the VM to new host and storage pool and assert migration successful

        vm = self.deploy_vm()

        root_volume = self.get_vm_volumes(vm.id)[0]

        target_pool = self.get_target_pool(root_volume.id)

        vm.stop(self.apiclient)

        self.migrate_vm_with_pools(target_pool, vm.id)

        root_volume = self.get_vm_volumes(vm.id)[0]
        self.assertEqual(root_volume.storageid, target_pool.id, "Pool ID was not as expected")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_02_migrate_VM_with_two_data_disks(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM and attaches 2 data disks
        # 2. Finds suitable host for migration
        # 3. Finds suitable storage pool for volumes
        # 4. Migrate the VM to new host and storage pool and assert migration successful

        vm = self.deploy_vm()

        volume1 = self.create_volume()
        volume2 = self.create_volume()

        vm.attach_volume(self.apiclient, volume1)
        vm.attach_volume(self.apiclient, volume2)

        root_volume = self.get_vm_volumes(vm.id)[0]

        target_pool = self.get_target_pool(root_volume.id)

        vm.stop(self.apiclient)

        self.migrate_vm_with_pools(target_pool, vm.id)

        volume1 = Volume.list(self.apiclient, id=volume1.id)[0]
        volume2 = Volume.list(self.apiclient, id=volume2.id)[0]
        root_volume = self.get_vm_volumes(vm.id)[0]

        self.assertEqual(root_volume.storageid, target_pool.id, "Pool ID was not as expected")
        self.assertEqual(volume1.storageid, target_pool.id, "Pool ID was not as expected")
        self.assertEqual(volume2.storageid, target_pool.id, "Pool ID was not as expected")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_03_migrate_detached_volume(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM and attaches 1 data disk
        # 2. Detaches the Disk
        # 3. Finds suitable storage pool for the Disk
        # 4. Migrate the storage pool and assert migration successful

        vm = self.deploy_vm()

        volume1 = self.create_volume()

        vm.attach_volume(self.apiclient, volume1)
        vm.detach_volume(self.apiclient, volume1)

        target_pool = self.get_target_pool(volume1.id)

        Volume.migrate(self.apiclient, storageid=target_pool.id, volumeid=volume1.id)

        vol = Volume.list(self.apiclient, volume=volume1.id)[0]

        self.assertEqual(vol.storageid, target_pool.id, "Storage pool was not the same as expected")


class TestKVMLiveMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestKVMLiveMigration, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        cls.management_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        super(TestKVMLiveMigration,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.hypervisor.lower() not in ["kvm"]:
            self.skipTest("VM Live Migration with Volumes is not supported on other than KVM")

        self.hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            hypervisor='KVM')

        if len(self.hosts) < 2:
            self.skipTest("Requires at least two hosts for performing migration related tests")

    def tearDown(self):
        super(TestKVMLiveMigration,self).tearDown()

    def get_target_host(self, virtualmachineid):
        target_hosts = Host.listForMigration(self.apiclient,
                                             virtualmachineid=virtualmachineid)
        if len(target_hosts) < 1:
            self.skipTest("No target hosts found")

        return target_hosts[0]

    def get_target_pool(self, volid):
        target_pools = StoragePool.listForMigration(self.apiclient, id=volid)

        if len(target_pools) < 1:
            self.skipTest("Not enough storage pools found")

        return target_pools[0]

    def get_vm_volumes(self, id):
        return Volume.list(self.apiclient, virtualmachineid=id, listall=True)

    def deploy_vm(self):
        return VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"])

    def create_volume(self):
        small_disk_offering = DiskOffering.list(self.apiclient, name='Small')[0]

        return Volume.create(
            self.apiclient,
            self.services,
            account=self.account.name,
            diskofferingid=small_disk_offering.id,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_01_migrate_VM_and_root_volume(self):
        """Test VM will be migrated with it's root volume"""
        # Validates the following:
        # 1. Deploys a VM
        # 2. Migrates the VM and the root volume to another host and storage pool
        # 3. Asserts migration success and checks for location

        vm = self.deploy_vm()

        root_volume = self.get_vm_volumes(vm.id)[0]

        target_pool = self.get_target_pool(root_volume.id)

        target_host = self.get_target_host(vm.id)

        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()

        cmd.migrateto = [{"volume": str(root_volume.id), "pool": str(target_pool.id)}]

        cmd.virtualmachineid = vm.id
        cmd.hostid = target_host.id

        response = self.apiclient.migrateVirtualMachineWithVolume(cmd)

        self.assertEqual(response.hostid, target_host.id)

        self.assertEqual(Volume.list(self.apiclient, id=root_volume.id)[0].storageid,
                         target_pool.id,
                         "Pool ID was not as expected")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_02_migrate_VM_with_two_data_disks(self):
        """Test VM will be migrated with it's root volume"""
        # Validate the following
        # 1. Deploys a VM and attaches 2 data disks
        # 2. Finds suitable host for migration
        # 3. Finds suitable storage pool for volumes
        # 4. Migrate the VM to new host and storage pool and assert migration successful

        vm = self.deploy_vm()

        volume1 = self.create_volume()
        volume2 = self.create_volume()

        vm.attach_volume(self.apiclient, volume1)
        vm.attach_volume(self.apiclient, volume2)

        root_volume = self.get_vm_volumes(vm.id)[0]

        target_pool = self.get_target_pool(root_volume.id)
        volume1.target_pool = self.get_target_pool(volume1.id)
        volume2.target_pool = self.get_target_pool(volume2.id)

        target_host = self.get_target_host(vm.id)

        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()

        cmd.migrateto = [{"volume": str(root_volume.id), "pool": str(target_pool.id)},
                         {"volume": str(volume1.id), "pool": str(volume1.target_pool.id)},
                         {"volume": str(volume2.id), "pool": str(volume2.target_pool.id)}]
        cmd.virtualmachineid = vm.id
        cmd.hostid = target_host.id

        response = self.apiclient.migrateVirtualMachineWithVolume(cmd)

        self.assertEqual(Volume.list(self.apiclient, id=root_volume.id)[0].storageid,
                         target_pool.id,
                         "Pool ID not as expected")

        self.assertEqual(Volume.list(self.apiclient, id=volume1.id)[0].storageid,
                         volume1.target_pool.id,
                         "Pool ID not as expected")

        self.assertEqual(Volume.list(self.apiclient, id=volume2.id)[0].storageid,
                         volume2.target_pool.id,
                         "Pool ID not as expected")

        self.assertEqual(response.hostid,
                         target_host.id,
                         "HostID not as expected")


class TestUnmanageVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestUnmanageVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if cls.template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        cls.hypervisorNotSupported = cls.hypervisor.lower() != "vmware"

        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-network_offering"],
        )
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls._cleanup = [
            cls.small_offering,
            cls.network_offering,
            cls.account
        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services["network"]["networkoffering"] = self.network_offering.id

        self.network = Network.create(
            self.apiclient,
            self.services["l2-network"],
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )

        self.cleanup = [
            self.network
        ]

    @attr(tags=["advanced", "advancedns", "smoke", "sg"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_01_unmanage_vm_cycle(self):
        """
        Test the following:
        1. Deploy VM
        2. Unmanage VM
        3. Verify VM is not listed in CloudStack
        4. Verify VM is listed as part of the unmanaged instances
        5. Import VM
        6. Destroy VM
        """

        # 1 - Deploy VM
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.small_offering.id,
            networkids=self.network.id,
            zoneid=self.zone.id
        )
        vm_id = self.virtual_machine.id
        vm_instance_name = self.virtual_machine.instancename
        hostid = self.virtual_machine.hostid
        hosts = Host.list(
            self.apiclient,
            id=hostid
        )
        host = hosts[0]
        clusterid = host.clusterid

        list_vm = list_virtual_machines(
            self.apiclient,
            id=vm_id
        )
        self.assertEqual(
            isinstance(list_vm, list),
            True,
            "Check if virtual machine is present"
        )
        vm_response = list_vm[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM state should be running after deployment"
        )

        # 2 - Unmanage VM from CloudStack
        self.virtual_machine.unmanage(self.apiclient)

        list_vm = list_virtual_machines(
            self.apiclient,
            id=vm_id
        )

        self.assertEqual(
            list_vm,
            None,
            "VM should not be listed"
        )

        unmanaged_vms = VirtualMachine.listUnmanagedInstances(
            self.apiclient,
            clusterid=clusterid,
            name=vm_instance_name
        )

        self.assertEqual(
            len(unmanaged_vms),
            1,
            "Unmanaged VMs matching instance name list size is 1"
        )

        unmanaged_vm = unmanaged_vms[0]
        self.assertEqual(
            unmanaged_vm.powerstate,
            "PowerOn",
            "Unmanaged VM is still running"
        )


class TestVAppsVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVAppsVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.hypervisorNotSupported = cls.hypervisor.lower() != "vmware"

        if cls.hypervisorNotSupported == False:

            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )

            cls.templates = get_test_ovf_templates(
                cls.apiclient,
                cls.zone.id,
                cls.services['test_ovf_templates'],
                cls.hypervisor
            )
            if len(cls.templates) == 0:
                assert False, "get_test_ovf_templates() failed to return templates"

            cls.custom_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["custom_service_offering"]
            )

            cls.isolated_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["isolated_network_offering"],
            )
            cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

            cls.l2_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["l2-network_offering"],
            )
            cls.l2_network_offering.update(cls.apiclient, state='Enabled')

            cls._cleanup = [
                cls.account,
                cls.custom_offering,
                cls.isolated_network_offering,
                cls.l2_network_offering
            ]

            # Uncomment when tests are finished, to cleanup the test templates
            for template in cls.templates:
                cls._cleanup.append(template)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during class cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def get_ova_parsed_information_from_template(self, template):
        if not template:
            return None
        details = template.details.__dict__
        configurations = []
        disks = []
        isos = []
        networks = []
        for propKey in details:
            if propKey.startswith('ACS-configuration'):
                configurations.append(json.loads(details[propKey]))
            elif propKey.startswith('ACS-disk'):
                detail = json.loads(details[propKey])
                if detail['isIso'] == True:
                    isos.append(detail)
                else:
                    disks.append(detail)
            elif propKey.startswith('ACS-network'):
                networks.append(json.loads(details[propKey]))

        return configurations, disks, isos, networks

    def verify_nics(self, nic_networks, vm_id):
        cmd = listNics.listNicsCmd()
        cmd.virtualmachineid = vm_id
        vm_nics =  self.apiclient.listNics(cmd)
        self.assertEqual(
            isinstance(vm_nics, list),
            True,
            "Check listNics response returns a valid list"
        )
        self.assertEqual(
            len(nic_networks),
            len(vm_nics),
            msg="VM NIC count is different, expected = {}, result = {}".format(len(nic_networks), len(vm_nics))
        )
        nic_networks.sort(key=itemgetter('nic')) # CS will create NIC in order of InstanceID. Check network order
        vm_nics.sort(key=itemgetter('deviceid'))
        for i in range(len(vm_nics)):
            nic = vm_nics[i]
            nic_network = nic_networks[i]
            self.assertEqual(
                nic.networkid,
                nic_network["network"],
                msg="VM NIC(InstanceID: {}) network mismatch, expected = {}, result = {}".format(nic_network["nic"], nic_network["network"], nic.networkid)
            )

    def verify_disks(self, template_disks, vm_id):
        cmd = listVolumes.listVolumesCmd()
        cmd.virtualmachineid = vm_id
        cmd.listall = True
        vm_volumes =  self.apiclient.listVolumes(cmd)
        self.assertEqual(
            isinstance(vm_volumes, list),
            True,
            "Check listVolumes response returns a valid list"
        )
        self.assertEqual(
            len(template_disks),
            len(vm_volumes),
            msg="VM volumes count is different, expected = {}, result = {}".format(len(template_disks), len(vm_volumes))
        )
        template_disks.sort(key=itemgetter('diskNumber'))
        vm_volumes.sort(key=itemgetter('deviceid'))
        for j in range(len(vm_volumes)):
            volume = vm_volumes[j]
            disk = template_disks[j]
            self.assertEqual(
                volume.size,
                disk["virtualSize"],
                msg="VM Volume(diskNumber: {}) network mismatch, expected = {}, result = {}".format(disk["diskNumber"], disk["virtualSize"], volume.size)
            )

    @attr(tags=["advanced", "advancedns", "smoke", "sg", "dev"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_01_vapps_vm_cycle(self):
        """
        Test the following for all found ovf templates:
        1. Deploy VM
        2. Verify VM has correct properties
        3. Verify VM has correct disks
        4. Verify VM has correct nics
        5. Destroy VM
        """

        for template in self.templates:
            configurations, disks, isos, network = self.get_ova_parsed_information_from_template(template)

            if configurations:
                conf = configurations[0]
                items = conf['hardwareItems']
                cpu_speed = 1000
                cpu_number = 0
                memory = 0
                for item in items:
                    if item['resourceType'] == 'Memory':
                        memory = item['virtualQuantity']
                    elif item['resourceType'] == 'Processor':
                        cpu_number = item['virtualQuantity']

            nicnetworklist = []
            networks = []
            vm_service = self.services["virtual_machine_vapps"][template.name]
            network_mappings = vm_service["nicnetworklist"]
            for network_mapping in network_mappings:
                network_service = self.services["isolated_network"]
                network_offering_id = self.isolated_network_offering.id
                if network_mapping["network"] == 'l2':
                    network_service = self.services["l2-network"]
                    network_offering_id = self.l2_network_offering.id
                network = Network.create(
                    self.apiclient,
                    network_service,
                    networkofferingid=network_offering_id,
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    zoneid=self.zone.id)
                networks.append(network)
                for interface in network_mapping["nic"]:
                    nicnetworklist.append({"nic": interface, "network": network.id})

            vm = VirtualMachine.create(
                self.apiclient,
                vm_service,
                accountid=self.account.name,
                domainid=self.account.domainid,
                templateid=template.id,
                serviceofferingid=self.custom_offering.id,
                zoneid=self.zone.id,
                customcpunumber=cpu_number,
                customcpuspeed=cpu_speed,
                custommemory=memory,
                properties=vm_service['properties'],
                nicnetworklist=nicnetworklist
            )

            list_vm_response = VirtualMachine.list(
                self.apiclient,
                id=vm.id
            )
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % vm.id
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
                vm.id,
                "Check virtual machine id in listVirtualMachines"
            )
            self.assertEqual(
                vm_response.name,
                vm.name,
                "Check virtual machine name in listVirtualMachines"
            )
            self.assertEqual(
                vm_response.state,
                'Running',
                msg="VM is not in Running state"
            )

            # Verify nics
            self.verify_nics(nicnetworklist, vm.id)
            # Verify disks
            self.verify_disks(disks, vm.id)
            # Verify properties
            original_properties = vm_service['properties']
            vm_properties = get_vm_vapp_configs(self.apiclient, self.config, self.zone, vm.instancename)
            for property in original_properties:
                if property["key"] in vm_properties:
                    self.assertEqual(
                        vm_properties[property["key"]],
                        property["value"],
                        "Check VM property %s with original value" % property["key"]
                    )

            cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
            cmd.id = vm.id
            self.apiclient.destroyVirtualMachine(cmd)
