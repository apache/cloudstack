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
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (recoverVirtualMachine,
                                  destroyVirtualMachine,
                                  attachIso,
                                  detachIso,
                                  provisionCertificate,
                                  updateConfiguration)
from marvin.lib.utils import *

from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Iso,
                             Router,
                             Configurations)
from marvin.lib.common import (get_domain,
                                get_zone,
                                get_template,
                               list_hosts)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
#Import System modules
import time
import re

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
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

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


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags = ["advanced"], required_hardware="false")
    def test_advZoneVirtualRouter(self):
        #TODO: SIMENH: duplicate test, remove it
        """
        Test advanced zone virtual router
        1. Is Running
        2. is in the account the VM was deployed in
        3. Has a linklocalip, publicip and a guestip
        @return:
        """
        routers = Router.list(self.apiclient, account=self.account.name)
        self.assertTrue(len(routers) > 0, msg = "No virtual router found")
        router = routers[0]

        self.assertEqual(router.state, 'Running', msg="Router is not in running state")
        self.assertEqual(router.account, self.account.name, msg="Router does not belong to the account")

        #Has linklocal, public and guest ips
        self.assertIsNotNone(router.linklocalip, msg="Router has no linklocal ip")
        self.assertIsNotNone(router.publicip, msg="Router has no public ip")
        self.assertIsNotNone(router.guestipaddress, msg="Router has no guest ip")


    @attr(mode = ["basic"], required_hardware="false")
    def test_basicZoneVirtualRouter(self):
        #TODO: SIMENH: duplicate test, remove it
        """
        Tests for basic zone virtual router
        1. Is Running
        2. is in the account the VM was deployed in
        @return:
        """
        routers = Router.list(self.apiclient, account=self.account.name)
        self.assertTrue(len(routers) > 0, msg = "No virtual router found")
        router = routers[0]

        self.assertEqual(router.state, 'Running', msg="Router is not in running state")
        self.assertEqual(router.account, self.account.name, msg="Router does not belong to the account")

    @attr(tags = ['advanced','basic','sg'], required_hardware="false")
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
            "Verify listVirtualMachines response for virtual machines: %s, %s" % (virtual_machine1.id, virtual_machine2.id)
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

        #if local storage is enabled, alter the offerings to use localstorage
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
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

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
        #create small and large virtual machines
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
            #Clean up, terminate the created ISOs
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_restore_vm(self):
        #TODO: SIMENH: add another test the data on the restored VM.
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

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg", "multihost"], required_hardware="false")
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

        if self.hypervisor.lower() in ["kvm","simulator"]:
            #identify suitable host
            clusters = [h.clusterid for h in hosts]
            #find hosts withe same clusterid
            clusters = [cluster for index, cluster in enumerate(clusters) if clusters.count(cluster) > 1]

            if len(clusters) <= 1:
                self.skipTest("In " + self.hypervisor.lower() + " Live Migration needs two hosts within same cluster")

            suitable_hosts = [host for host in hosts if host.clusterid == clusters[0]]
        else:
            suitable_hosts = hosts

        target_host = suitable_hosts[0]
        migrate_host = suitable_hosts[1]

        #deploy VM on target host
        self.vm_to_migrate = VirtualMachine.create(
            self.apiclient,
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

        self.vm_to_migrate.migrate(self.apiclient, migrate_host.id)

        retries_cnt = 3
        while retries_cnt >=0:
            list_vm_response = VirtualMachine.list(self.apiclient,
                                                   id=self.vm_to_migrate.id)
            self.assertNotEqual(
                                list_vm_response,
                                None,
                                "Check virtual machine is listed"
                               )
            vm_response = list_vm_response[0]
            self.assertEqual(vm_response.id,self.vm_to_migrate.id,"Check virtual machine ID of migrated VM")
            self.assertEqual(vm_response.hostid,migrate_host.id,"Check destination hostID of migrated VM")
            retries_cnt = retries_cnt - 1
        return

    @attr(configuration = "expunge.interval")
    @attr(configuration = "expunge.delay")
    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
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

        #VM should be destroyed unless expunge thread hasn't run
        #Wait for two cycles of the expunge thread
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

        self.assertEqual(list_vm_response,None,"Check Expunged virtual machine is in listVirtualMachines response")
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
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

        try:
            ssh_client = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                                (self.virtual_machine.ipaddress, e))

        mount_dir = "/mnt/tmp"
        cmds = "mkdir -p %s" % mount_dir
        self.assert_(ssh_client.execute(cmds) == [], "mkdir failed within guest")

        for diskdevice in self.services["diskdevice"]:
            res = ssh_client.execute("mount -rt iso9660 {} {}".format(diskdevice, mount_dir))
            if res == []:
                self.services["mount"] = diskdevice
                break
        else:
            self.fail("No mount points matched. Mount was unsuccessful")

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
            #Unmount ISO
            command = "umount %s" % mount_dir
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
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.management_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]

        template = get_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

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
            h = list_hosts(self.apiclient,type='Routing', id=host.id)[0]
            if h.details.secured == secured:
                return h

        cloudstackTestCase.skipTest(self, "No target hosts available, skipping test.")

    def check_migration_protocol(self, protocol, host):
        resp = SshClient(host.ipaddress, port=22, user=self.hostConfig["username"],passwd=self.hostConfig["password"])\
            .execute("grep -a listen_%s=1 /etc/libvirt/libvirtd.conf | tail -1" % protocol)

        if protocol not in resp[0]:
            cloudstackTestCase.fail(self, "Libvirt listen protocol expected: '" + protocol + "\n"
                                    "does not match actual: " + resp[0])

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
        SshClient(host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"])\
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
        else: self.fail("Migration succeeded, instead it should fail")

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
        else: self.fail("Migration succeeded, instead it should fail")

