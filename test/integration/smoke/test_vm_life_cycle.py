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
                                  listNics,
                                  listVolumes)
from marvin.lib.utils import *

from marvin.lib.base import (Account,
                             Role,
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
                               get_vm_vapp_configs)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf
# Import System modules
import time
import json
from operator import itemgetter

_multiprocess_shared_ = True


class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVMLifeCycle, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
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
        cls._cleanup = []

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.small_offering)

        cls.medium_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["medium"]
        )
        cls._cleanup.append(cls.medium_offering)

        # create small and large virtual machines
        cls.small_virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        # we don't `cls._cleanup.append(cls.small_virtual_machine)`, as this will be destroyed either during the tests or as a part of the account.

        cls.medium_virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.medium_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup.append(cls.medium_virtual_machine)
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup.append(cls.virtual_machine)

    @classmethod
    def tearDownClass(cls):
        super(TestVMLifeCycle, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestVMLifeCycle, self).tearDown()

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
            "Check VM available in List Virtual Machines"
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

        self.debug("Starting VM - ID: %s" % self.small_virtual_machine.id)
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
            "Check VM available in List Virtual Machines"
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

        self.debug("Rebooting VM - ID: %s" % self.small_virtual_machine.id)
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
    def test_04_reboot_vm_forced(self):
        """Test Force Reboot Virtual Machine
        """

        try:
            self.debug("Force rebooting VM - ID: %s" % self.virtual_machine.id)
            self.small_virtual_machine.reboot(self.apiclient, forced=True)
        except Exception as e:
            self.fail("Failed to force reboot VM: %s" % e)

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
            "Check VM available in List Virtual Machines"
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
            "Check VM available in List Virtual Machines"
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

        if self.zone.localstorageenabled :
            self.skipTest("Migration is not supported on zones with local storage")

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
            # find hosts with same clusterid
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
                log_msg = "Test template does not supports iso9660 filesystem. Proceeding with test without mounting."
                self.debug(log_msg)
                print(log_msg)
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

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_12_start_vm_multiple_volumes_allocated(self):
        """Test attaching multiple datadisks and start VM
        """

        # Validate the following
        # 1. Deploys a VM without starting it and attaches multiple datadisks to it
        # 2. Start VM successfully
        # 3. Destroys the VM with DataDisks option

        custom_disk_offering = DiskOffering.list(self.apiclient, name='Custom')[0]

        # Create VM without starting it
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            startvm=False
        )
        self.cleanup.append(vm)

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            hypervisor=self.hypervisor,
            state='Up')

        if self.hypervisor.lower() in ["simulator"] or not hosts[0].hypervisorversion:
            hypervisor_version = "default"
        else:
            hypervisor_version = hosts[0].hypervisorversion

        res = self.dbclient.execute("select max_data_volumes_limit from hypervisor_capabilities where "
                                    "hypervisor_type='%s' and hypervisor_version='%s';" %
                                    (self.hypervisor.lower(), hypervisor_version))
        if isinstance(res, list) and len(res) > 0:
            max_volumes = res[0][0]
            if max_volumes > 14:
                max_volumes = 14
        else:
            max_volumes = 6

        # Create and attach volumes
        self.services["custom_volume"]["customdisksize"] = 1
        self.services["custom_volume"]["zoneid"] = self.zone.id
        for i in range(max_volumes):
            volume = Volume.create_custom_disk(
                self.apiclient,
                self.services["custom_volume"],
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=custom_disk_offering.id
            )
            self.cleanup.append(volume)    # Needs adjusting when changing tearDown to a super call, since it will try to delete an attached volume.
            VirtualMachine.attach_volume(vm, self.apiclient, volume)
            self.cleanup.remove(volume)

        # Start the VM
        self.debug("Starting VM - ID: %s" % vm.id)
        vm.start(self.apiclient)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=vm.id
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

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_13_destroy_and_expunge_vm(self):
        """Test destroy virtual machine with expunge parameter depending on whether the caller's role has expunge permission.
        """
        # Setup steps:
        # 1. Create role with DENY expunge permission.
        # 2. Create account with said role.
        # 3. Create a VM of said account.
        # 4. Create a VM of cls.account
        # Validation steps:
        # 1. Destroy the VM with the created account and verify it was not destroyed.
        # 1. Destroy the other VM with cls.account and verify it was expunged.

        role = Role.importRole(
            self.apiclient,
            {
                "name": "MarvinFake Import Role ",
                "type": "DomainAdmin",
                "description": "Fake Import Domain Admin Role created by Marvin test",
                "rules" : [{"rule":"list*", "permission":"allow","description":"Listing apis"},
                           {"rule":"get*", "permission":"allow","description":"Get apis"},
                           {"rule":"update*", "permission":"allow","description":"Update apis"},
                           {"rule":"queryAsyncJobResult", "permission":"allow","description":"Query async job result"},
                           {"rule":"deployVirtualMachine", "permission":"allow","description":"Deploy virtual machine"},
                           {"rule":"destroyVirtualMachine", "permission":"allow","description":"Destroy virtual machine"},
                           {"rule":"expungeVirtualMachine", "permission":"deny","description":"Expunge virtual machine"}]
            },
        )
        self.cleanup.append(role)

        domadm = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            roleid=role.id,
            domainid=self.domain.id
        )
        self.cleanup[-1]=domadm    # Hacky way to reverse cleanup order to avoid deleting the role before account. Remove this line when tearDown is changed to call super().
        self.cleanup.append(role)    # Should be self.cleanup.append(domadm) when tearDown is changed to call super().

        domadm_apiclient = self.testClient.getUserApiClient(UserName=domadm.name, DomainName=self.domain.name, type=1)

        vm1 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
        )

        vm2 = VirtualMachine.create(
            domadm_apiclient,
            self.services["small"],
            accountid=domadm.name,
            domainid=domadm.domainid,
            serviceofferingid=self.small_offering.id,
        )

        self.debug("Expunge VM-ID: %s" % vm1.id)

        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = vm1.id
        cmd.expunge = True
        response = self.apiclient.destroyVirtualMachine(cmd)

        self.debug("response: %s" % response)
        self.debug("response: %s" % response.id)
        self.assertEqual(
            response.id,
            None,
            "Check if VM was expunged.",
        )

        self.debug("Expunge VM-ID: %s" % vm2.id)

        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = vm2.id
        cmd.expunge = True
        try:
            domadm_apiclient.destroyVirtualMachine(cmd)
            self.failed("Destroy VM with expunge should have raised an exception.")
        except:
            self.debug("Expected exception! Keep going.")

        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_14_destroy_vm_delete_protection(self):
        """Test destroy Virtual Machine with delete protection
        """

        # Validate the following
        # 1. Should not be able to delete the VM when delete protection is enabled
        # 2. Should be able to delete the VM after disabling delete protection

        vm = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"],
            startvm=False
        )

        vm.update(self.apiclient, deleteprotection=True)
        try:
            vm.delete(self.apiclient)
            self.fail("VM shouldn't get deleted with delete protection enabled")
        except Exception as e:
            self.debug("Expected exception: %s" % e)

        vm.update(self.apiclient, deleteprotection=False)
        vm.delete(self.apiclient)

        return
