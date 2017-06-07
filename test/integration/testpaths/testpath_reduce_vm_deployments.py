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

from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              random_gen,
                              is_snapshot_on_nfs,
                              isAlmostEqual)
from marvin.lib.base import (Account,
                             DiskOffering,
                             AffinityGroup,
                             Snapshot,
                             Volume,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Iso,
                             Router,
                             Configurations)
from marvin.lib.common import (get_domain,
                               list_volumes,
                               list_hosts,
                               list_service_offering,
                               list_virtual_machines,
                               list_snapshots,
                               get_zone,
                               get_template)
from marvin.codes import FAILED, PASS
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

# Import System modules
import time
import re
import random
import string

_multiprocess_shared_ = True


class TestDeployVMandVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVMandVMLifeCycle, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.domain = get_domain(cls.apiclient)

        # if local storage is enabled, alter the offerings to use localstorage
        # this step is needed for devcloud
        if cls.zone.localstorageenabled:
            cls.services["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.services["service_offerings"]["small"]["storagetype"] = 'local'
            cls.services["service_offerings"][
                "medium"]["storagetype"] = 'local'

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, \
                "get_template() failed to return template with description" \
                " %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        cls._cleanup = []

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls.medium_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["medium"]
        )

        cls.big_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["big"]
        )

        cls.service_offering_1 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )

        cls.service_offering_2 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
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
        cls._cleanup.extend([cls.account,
                             cls.small_offering,
                             cls.medium_offering,
                             cls.big_offering,
                             cls.service_offering_1,
                             cls.service_offering_2])

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created ISOs
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    @attr(
        tags=[
            "devcloud",
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="false",
        BugId="CLOUDSTACK-6984")
    def test_01_vm_operations(self):
        """Test Stop Virtual Machine
        """

        # Validate the following
        # 1. Should Not be able to login to the VM.
        # 2. listVM command should return
        # Validate the following
        # 3. listVM command should return this VM.State
        #    of this VM should be Running".
        #    this VM.State of this VM should be ""Stopped"".
        # 4. Should be able to login to the VM.
        # 5. listVM command should return the deployed VM.
        #    State of this VM should be "Running"
        # 6. Should not be able to login to the VM.
        # 7. listVM command should return this VM.State
        #    of this VM should be "Destroyed".
        # 8. listVM command should return this VM.
        #    State of this VM should be "Stopped".
        # 9. We should be able to Start this VM successfully.
        # 10. Environment has enough hosts for migration
        # 11. DeployVM on suitable host (with another host in the cluster)
        # 12. Migrate the VM and assert migration successful

        try:
            self.small_virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

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
            "Check VM avaliable in List Virtual Machines"
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.small_virtual_machine.id
        )
        self.assertEqual(
            list_vm_response[0].state,
            "Running",
            "Check virtual machine is in running state"
        )
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
            "Check VM avaliable in List Virtual Machines"
        )

        self.assertEqual(
            list_vm_response[0].state,
            "Running",
            "Check virtual machine is in running state"
        )
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
        suitable_hosts = None

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing'
        )
        self.assertEqual(
            validateList(hosts)[0],
            PASS,
            "hosts list validation failed")

        if len(hosts) < 2:
            self.skipTest(
                "At least two hosts should be present in"
                " the zone for migration")

        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("Migration is not supported on LXC")

        # For KVM, two hosts used for migration should  be present
        #  in same cluster
        # For XenServer and VMware, migration is possible between
        #  hosts belonging to different clusters
        # with the help of XenMotion and Vmotion respectively.

        if self.hypervisor.lower() in ["kvm", "simulator"]:
            # identify suitable host
            clusters = [h.clusterid for h in hosts]
            # find hosts withe same clusterid
            clusters = [
                cluster for index,
                cluster in enumerate(clusters) if clusters.count(cluster) > 1]

            if len(clusters) <= 1:
                self.skipTest(
                    "In " +
                    self.hypervisor.lower() +
                    " Live Migration needs two hosts within same cluster")

            suitable_hosts = [
                host for host in hosts if host.clusterid == clusters[0]]
        else:
            suitable_hosts = hosts

        target_host = suitable_hosts[0]
        migrate_host = suitable_hosts[1]

        # deploy VM on target host
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
        while retries_cnt >= 0:
            list_vm_response = VirtualMachine.list(self.apiclient,
                                                   id=self.vm_to_migrate.id)
            self.assertNotEqual(
                list_vm_response,
                None,
                "Check virtual machine is listed"
            )
            vm_response = list_vm_response[0]
            self.assertEqual(
                vm_response.id,
                self.vm_to_migrate.id,
                "Check virtual machine ID of migrated VM")
            self.assertEqual(
                vm_response.hostid,
                migrate_host.id,
                "Check destination hostID of migrated VM")
            retries_cnt = retries_cnt - 1
        return

    @attr(configuration="expunge.interval")
    @attr(configuration="expunge.delay")
    @attr(
        tags=[
            "devcloud",
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="false")
    def test_02_expunge_vm(self):
        """Test destroy(expunge) Virtual Machine
        """
        # Validate the following
        # 1. listVM command should NOT  return this VM any more.

        self.debug("Expunge VM-ID: %s" % self.small_virtual_machine.id)

        self.small_virtual_machine.delete(self.apiclient)

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
            # time.sleep(expunge_cycle)
            wait_time = wait_time - expunge_cycle

        self.debug("listVirtualMachines response: %s" % list_vm_response)

        self.assertEqual(
            list_vm_response,
            None,
            "Check Expunged virtual machine is in"
            " listVirtualMachines response")
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_03_attachAndDetach_iso(self):
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
            self.fail("Exception while downloading ISO %s: %s"
                      % (iso.id, e))

        self.debug("Attach ISO with ID: %s to VM ID: %s" % (
            iso.id,
            self.virtual_machine.id
        ))
        # Attach ISO to virtual machine
        self.virtual_machine.attach_iso(self.apiclient, iso)

        try:
            ssh_client = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (self.virtual_machine.ipaddress, e))

        mount_dir = "/mnt/tmp"
        cmds = "mkdir -p %s" % mount_dir
        self.assert_(
            ssh_client.execute(cmds) == [],
            "mkdir failed within guest")

        for diskdevice in self.services["diskdevice"]:
            res = ssh_client.execute(
                "mount -rt iso9660 {} {}".format(diskdevice, mount_dir))
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
            # Unmount ISO
            command = "umount %s" % mount_dir
            ssh_client.execute(command)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (self.virtual_machine.ipaddress, e))

        # Detach from VM
        self.virtual_machine.detach_iso(self.apiclient)

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

    @attr(
        tags=[
            "devcloud",
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="false")
    def test_04_deploy_vm(self):
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
            "Verify listVirtualMachines response for virtual machine: %s"
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

        self.assertEqual(
            router.state,
            'Running',
            msg="Router is not in running state")
        self.assertEqual(
            router.account,
            self.account.name,
            msg="Router does not belong to the account")

        # Has linklocal, public and guest ips
        self.assertIsNotNone(
            router.linklocalip,
            msg="Router has no linklocal ip")
        self.assertIsNotNone(router.publicip, msg="Router has no public ip")
        self.assertIsNotNone(
            router.guestipaddress,
            msg="Router has no guest ip")

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

        self.assertEqual(
            router.state,
            'Running',
            msg="Router is not in running state")
        self.assertEqual(
            router.account,
            self.account.name,
            msg="Router does not belong to the account")

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware="false")
    def test_deploy_vm_multiple(self):
        """Test Multiple Deploy Virtual Machine

        # Validate the following:
        # 1. listVirtualMachines using 'ids' parameter returns
        # accurate information
        """
        ids = ','.join(map(str, [self.small_virtual_machine.id,
                                 self.medium_virtual_machine.id]))

        list_vms = VirtualMachine.list(
            self.apiclient,
            ids=ids,
            listAll=True)

        self.debug(
            "Verify listVirtualMachines response for virtual"
            " machines: %s, %s" %
            (self.small_virtual_machine.id, self.medium_virtual_machine.id))
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

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_scale_vm(self):
        """Test scale virtual machine
        """
        # Validate the following
        # Scale up the vm and see if it scales to the new svc offering and is
        # finally in running state

        #        VirtualMachine should be updated to tell cloudstack
        #        it has PV tools
        #        available and successfully scaled. We will only mock
        #        that behaviour
        #        here but it is not expected in production since the VM
        #        scaling is not
        #        guaranteed until tools are installed, vm rebooted

        # If hypervisor is Vmware, then check if
        # the vmware tools are installed and the process is running
        # Vmware tools are necessary for scale VM operation
        if self.hypervisor.lower() in ('kvm', 'hyperv', 'lxc'):
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)

        if self.hypervisor.lower() == "vmware":
            sshClient = self.virtual_machine.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" not in result:
                self.skipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")

        # Set enable.dynamic.scale.vm to true
        Configurations.update(self.apiclient,
                              name="enable.dynamic.scale.vm",
                              value="true"
                              )

        self.debug("Scaling VM-ID: %s to service offering: %s and state %s" % (
            self.virtual_machine.id,
            self.big_offering.id,
            self.virtual_machine.state
        ))

        self.virtual_machine.scale_virtualmachine(
            self.apiclient, self.big_offering.id)

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            list_vm_response,
            None,
            "Check virtual machine is listVirtualMachines"
        )

        vm_response = list_vm_response[0]
        self.assertEqual(
            vm_response.id,
            self.virtual_machine.id,
            "Check virtual machine ID of scaled VM"
        )

        self.debug(
            "Scaling VM-ID: %s from service offering: %s to new service\
                    offering %s and the response says %s" %
            (self.virtual_machine.id,
             self.virtual_machine.serviceofferingid,
             self.big_offering.id,
             vm_response.serviceofferingid))

        self.assertEqual(
            vm_response.serviceofferingid,
            self.big_offering.id,
            "Check service offering of the VM"
        )

        self.assertEqual(
            vm_response.state,
            'Running',
            "Check the state of VM"
        )
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_snapshot_root_disk(self):
        """Test Snapshot Root Disk
        """

        # Validate the following
        # 1. listSnapshots should list the snapshot that was created.
        # 2. verify that secondary storage NFS share contains
        #    the reqd volume under
        #    /secondary/snapshots//$account_id/$volumeid/$snapshot_uuid
        # 3. verify backup_snap_id was non null in the `snapshots` table
        # 4. Verify that zoneid is returned in listSnapshots API response

        if self.hypervisor.lower() in ['hyperv', 'lxc', 'kvm']:
            if self.hypervisor.lower() == 'kvm':
                configs = Configurations.list(
                    self.apiclient,
                    name='kvm.snapshot.enabled'
                )
                if configs[0].value == "false":
                    self.skipTest(
                        "Snapshot creation not supported on %s hypervisor " %
                        self.hypervisor.lower)
            else:
                self.skipTest(
                    "Snapshot creation not supported on %s hypervisor " %
                    self.hypervisor.lower)

        self.virtual_machine_with_disk = self.virtual_machine

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

        self.assertIsNotNone(snapshots[0].zoneid,
                             "Zone id is not none in listSnapshots")
        self.assertEqual(
            snapshots[0].zoneid,
            self.zone.id,
            "Check zone id in the list snapshots"
        )

        self.debug(
            "select backup_snap_id, account_id, volume_id from "
            "snapshots where uuid = '%s';" % str(snapshot.id))
        qresultset = self.dbclient.execute(
            "select backup_snap_id, account_id, volume_id"
            " from snapshots where uuid = '%s';" % str(snapshot.id))
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = qresultset[0]

        snapshot_uuid = qresult[0]      # backup_snap_id = snapshot UUID

        self.assertNotEqual(
            str(snapshot_uuid),
            'NULL',
            "Check if backup_snap_id is not null"
        )

        self.assertTrue(
            is_snapshot_on_nfs(
                self.apiclient,
                self.dbclient,
                self.config,
                self.zone.id,
                snapshot.id))
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_02_edit_service_offering(self):
        """Test to update existing service offering"""

        # Validate the following:
        # 1. updateServiceOffering should return
        #    a valid information for newly created offering

        # Generate new name & displaytext from random data
        random_displaytext = random_gen()
        random_name = random_gen()

        self.debug("Updating service offering with ID: %s" %
                   self.service_offering_1.id)

        self.service_offering_1.update(self.apiclient,
                                       displaytext=random_displaytext,
                                       name=random_name)
        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_1.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is updated"
        )

        self.assertEqual(
            list_service_response[0].displaytext,
            random_displaytext,
            "Check server displaytext in updateServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            random_name,
            "Check server name in updateServiceOffering"
        )

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_03_delete_service_offering(self):
        """Test to delete service offering"""

        # Validate the following:
        # 1. deleteServiceOffering should return
        #    a valid information for newly created offering

        self.debug("Deleting service offering with ID: %s" %
                   self.service_offering_2.id)

        self.service_offering_2.delete(self.apiclient)

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_2.id
        )

        self.assertEqual(
            list_service_response,
            None,
            "Check if service offering exists in listDiskOfferings"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_change_offering_small(self):
        """Test to change service to a small capacity
        """
        # Validate the following
        # 1. Log in to the Vm .We should see that the CPU and memory Info of
        #    this Vm matches the one specified for "Small" service offering.
        # 2. Using  listVM command verify that this Vm
        #    has Small service offering Id.

        if self.hypervisor.lower() == "lxc":
            self.skipTest(
                "Skipping this test for {} due to"
                " bug CS-38153".format(self.hypervisor))
        try:
            self.medium_virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        self.medium_virtual_machine.change_service_offering(
            self.apiclient,
            self.small_offering.id
        )

        self.debug("Starting VM - ID: %s" % self.medium_virtual_machine.id)
        self.medium_virtual_machine.start(self.apiclient)
        # Ensure that VM is in running state
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=self.medium_virtual_machine.id
        )

        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state == 'Running':
                self.debug("VM state: %s" % vm.state)
            else:
                raise Exception(
                    "Failed to start VM (ID: %s) after changing\
                            service offering" % vm.id)

        try:
            ssh = self.medium_virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.medium_virtual_machine.ipaddress, e)
            )

        cpuinfo = ssh.execute("cat /proc/cpuinfo")
        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        # 'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i][0].split()[3]
        meminfo = ssh.execute("cat /proc/meminfo")
        # MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]

        self.debug(
            "CPU count: %s, CPU Speed: %s, Mem Info: %s" % (
                cpu_cnt,
                cpu_speed,
                total_mem
            ))
        self.assertAlmostEqual(
            int(cpu_cnt),
            self.small_offering.cpunumber,
            "Check CPU Count for small offering"
        )
        self.assertAlmostEqual(
            list_vm_response[0].cpuspeed,
            self.small_offering.cpuspeed,
            "Check CPU Speed for small offering"
        )

        range = 20
        if self.hypervisor.lower() == "hyperv":
            range = 200
        # TODO: Find the memory allocated to VM on hyperv hypervisor using
        # powershell commands and use that value to equate instead of
        # manipulating range, currently we get the memory count much less
        # because of the UI component
        self.assertTrue(
            isAlmostEqual(int(int(total_mem) / 1024),
                          int(self.small_offering.memory),
                          range=range
                          ),
            "Check Memory(kb) for small offering"
        )
        return

    @attr(
        tags=[
            "devcloud",
            "basic",
            "advanced",
            "post"],
        required_hardware="true")
    def test_deployvm_userdata_post(self):
        """Test userdata as POST, size > 2k
        """
        user_data = ''.join(
            random.choice(
                string.ascii_uppercase +
                string.digits) for x in range(2500))
        self.services["virtual_machine"]["userdata"] = user_data

        deployVmResponse = VirtualMachine.create(
            self.apiclient,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            method='POST'
        )
        vms = list_virtual_machines(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assert_(
            len(vms) > 0,
            "There are no Vms deployed in the account %s" %
            self.account.name)
        vm = vms[0]
        self.assert_(
            vm.id == str(
                deployVmResponse.id),
            "Vm deployed is different from the test")
        self.assert_(vm.state == "Running", "VM is not in Running state")


class TestCreateServiceOffering(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.services = self.testClient.getParsedTestDataConfig()

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_01_create_service_offering(self):
        """Test to create service offering"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information

        service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )
        self.cleanup.append(service_offering)

        self.debug(
            "Created service offering with ID: %s" %
            service_offering.id)

        list_service_response = list_service_offering(
            self.apiclient,
            id=service_offering.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is created"
        )

        self.assertEqual(
            list_service_response[0].cpunumber,
            self.services["service_offerings"]["tiny"]["cpunumber"],
            "Check server id in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].cpuspeed,
            self.services["service_offerings"]["tiny"]["cpuspeed"],
            "Check cpuspeed in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].displaytext,
            self.services["service_offerings"]["tiny"]["displaytext"],
            "Check server displaytext in createServiceOfferings"
        )
        self.assertEqual(
            list_service_response[0].memory,
            self.services["service_offerings"]["tiny"]["memory"],
            "Check memory in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            self.services["service_offerings"]["tiny"]["name"],
            "Check name in createServiceOffering"
        )
        return


class TestDeployVmWithAffinityGroup(cloudstackTestCase):

    """
    This test deploys a virtual machine into a user account
    using the small service offering and builtin template
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDeployVmWithAffinityGroup,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.domain = get_domain(cls.apiclient)
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )

        if cls.template == FAILED:
            assert False,\
                "get_template() failed to return " \
                "template with description %s" % cls.services["ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )

        cls.ag = AffinityGroup.create(
            cls.apiclient,
            cls.services["virtual_machine"]["affinity"],
            account=cls.account.name,
            domainid=cls.domain.id)

        cls._cleanup = [
            cls.service_offering,
            cls.ag,
            cls.account,
        ]
        return

    @attr(tags=["basic", "advanced", "multihost"], required_hardware="false")
    def test_DeployVmAntiAffinityGroup(self):
        """
        test DeployVM in anti-affinity groups

        deploy VM1 and VM2 in the same host-anti-affinity groups
        Verify that the vms are deployed on separate hosts
        """
        # deploy VM1 in affinity group created in setUp

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing'
        )
        self.assertEqual(
            validateList(hosts)[0],
            PASS,
            "hosts list validation failed")

        if len(hosts) < 2:
            self.skipTest(
                "At least two hosts should be present in"
                " the zone to verify affinity group of type"
                " host-antiaffinity")

        vm1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            affinitygroupnames=self.ag.name
        )

        list_vm1 = list_virtual_machines(
            self.apiclient,
            id=vm1.id
        )
        self.assertEqual(
            isinstance(list_vm1, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vm1),
            0,
            "Check VM available in List Virtual Machines"
        )
        vm1_response = list_vm1[0]
        self.assertEqual(
            vm1_response.state,
            'Running',
            msg="VM is not in Running state"
        )
        host_of_vm1 = vm1_response.hostid

        # deploy VM2 in affinity group created in setUp
        vm2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            affinitygroupnames=self.ag.name
        )
        list_vm2 = list_virtual_machines(
            self.apiclient,
            id=vm2.id
        )
        self.assertEqual(
            isinstance(list_vm2, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vm2),
            0,
            "Check VM available in List Virtual Machines"
        )
        vm2_response = list_vm2[0]
        self.assertEqual(
            vm2_response.state,
            'Running',
            msg="VM is not in Running state"
        )
        host_of_vm2 = vm2_response.hostid

        self.assertNotEqual(
            host_of_vm1,
            host_of_vm2,
            msg="Both VMs of affinity group %s are on the same host" %
            self.ag.name)

    @classmethod
    def tearDownClass(cls):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


class TestDeployvGPUenabledVM(cloudstackTestCase):

    """
    Test deploy a vGPU enabled VM into a user account
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeployvGPUenabledVM, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.noSuitableHost = False
        # Need to add check whether zone containing the xen hypervisor or not
        # as well
        hosts = list_hosts(
            cls.apiclient,
            hypervisor="XenServer"
        )
        if hosts is None:
            # GPU feature is supported only on XenServer.Check listhosts
            # response
            cls.unsupportedHypervisor = True
            return
        else:
            gpuhosts = 0
            for ghost in hosts:
                if ghost.hypervisorversion >= "6.2.0":
                    sshClient = SshClient(
                        host=ghost.ipaddress,
                        port=cls.testdata['configurableData']['host']["publicport"],
                        user=cls.testdata['configurableData']['host']["username"],
                        passwd=cls.testdata['configurableData']['host']["password"])
                    if ghost.hypervisorversion == "6.2.0":
                        res = sshClient.execute(
                            "xe patch-list uuid=0850b186-4d47-11e3-a720-001b2151a503")
                        if len(res) == 0:
                            continue
                    res = sshClient.execute(
                        "xe vgpu-type-list model-name=\"GRID K120Q\"")
                    if len(res) != 0:
                        gpuhosts = gpuhosts + 1
                    else:
                        continue
        if gpuhosts == 0:
            # No XenServer available with GPU Drivers installed
            cls.noSuitableHost = True
            return

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()["vgpu"]
        if self.noSuitableHost or self.unsupportedHypervisor:
            self.skipTest("Skipping test because suitable hypervisor/host not\
                    present")

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata["mode"] = self.zone.networktype
        # Before running this test, register a windows template with ostype as
        # 'Windows 7 (32-bit)'
        self.template = get_template(
            self.apiclient,
            self.zone.id,
            self.testdata["ostype"])

        if self.template == FAILED:
            assert False,\
                "get_template() failed to return template with description" \
                " %s" % self.testdata["ostype"]
        # create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        self.testdata["small"]["zoneid"] = self.zone.id
        self.testdata["small"]["template"] = self.template.id

        self.testdata["service_offerings"]["vgpu260qwin"]["serviceofferingdetails"] = [
            {
                'pciDevice': 'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs'}, {
                'vgpuType': 'GRID K120Q'}]
        # create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["vgpu260qwin"],
        )
        # build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    @attr(tags=['advanced', 'basic', 'vgpu'], required_hardware="true")
    def test_deploy_vgpu_enabled_vm(self):
        """Test Deploy Virtual Machine

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. Virtual Machine is vGPU enabled (via SSH)
        # 3. listVirtualMachines returns accurate information
        """
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.testdata['mode']
        )

        list_vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id)

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertNotEqual(
            len(list_vms),
            0,
            "List VM response was empty"
        )

        vm = list_vms[0]
        self.assertEqual(
            vm.id,
            self.virtual_machine.id,
            "Virtual Machine ids do not match"
        )
        self.assertEqual(
            vm.name,
            self.virtual_machine.name,
            "Virtual Machine names do not match"
        )
        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )
        hosts = list_hosts(
            self.apiclient,
            id=vm.hostid
        )
        hostip = hosts[0].ipaddress
        try:
            sshClient = SshClient(
                host=hostip,
                port=self.testdata['configurableData']['host']["publicport"],
                user=self.testdata['configurableData']['host']["username"],
                passwd=self.testdata['configurableData']['host']["password"])
            res = sshClient.execute(
                "xe vgpu-list vm-name-label=%s params=type-uuid %s" %
                (vm.instancename))
            self.debug("SSH result: %s" % res)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (hostip, e)
                      )
        result = str(res)
        self.assertEqual(
            result.count("type-uuid"),
            1,
            "VM is vGPU enabled."
        )

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


class TestDeployVMFromISO(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVMFromISO, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )

        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )

        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="true")
    def test_deploy_vm_from_iso(self):
        """Test Deploy Virtual Machine from ISO
        """

        # Validate the following:
        # 1. deploy VM using ISO
        # 2. listVM command should return the deployed VM. State of this VM
        #    should be "Running".
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "vm deploy from ISO feature is not supported on %s" %
                self.hypervisor.lower())

        self.iso = Iso.create(
            self.apiclient,
            self.testdata["configurableData"]["bootableIso"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        try:
            # Download the ISO
            self.iso.download(self.apiclient)

        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (self.iso.id, e))

        self.debug("Registered ISO: %s" % self.iso.name)
        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.iso.id,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        return


class TestDeployVM(cloudstackTestCase):

    """Test deploy a VM into a user account
    """

    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)

        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(
            self.apiclient,
            self.zone.id,
            self.testdata["ostype"])

        if self.template == FAILED:
            assert False, "get_template() failed to return template "

        # create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        # create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["small"]
        )
        # build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware="true")
    def test_00_deploy_vm_root_resize(self):
        """Test deploy virtual machine with root resize

        # Validate the following:
        # 1. listVirtualMachines returns accurate information
        # 2. root disk has new size per listVolumes
        # 3. Rejects non-supported hypervisor types
        """
        if(self.hypervisor.lower() == 'kvm', 'xenserver', 'vmware'):
            newrootsize = (self.template.size >> 30) + 2
            self.virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                rootdisksize=newrootsize
            )

            list_vms = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine.id)

            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s"
                % self.virtual_machine.id
            )

            self.assertEqual(
                isinstance(list_vms, list),
                True,
                "List VM response was not a valid list"
            )
            self.assertNotEqual(
                len(list_vms),
                0,
                "List VM response was empty"
            )

            vm = list_vms[0]
            self.assertEqual(
                vm.id,
                self.virtual_machine.id,
                "Virtual Machine ids do not match"
            )
            self.assertEqual(
                vm.name,
                self.virtual_machine.name,
                "Virtual Machine names do not match"
            )
            self.assertEqual(
                vm.state,
                "Running",
                msg="VM is not in Running state"
            )

            # get root vol from created vm, verify it is correct size
            list_volume_response = list_volumes(
                self.apiclient,
                virtualmachineid=self.virtual_machine.id,
                type='ROOT',
                listall=True
            )

            rootvolume = list_volume_response[0]
            success = False
            if rootvolume is not None and rootvolume.size == (
                    newrootsize << 30):
                success = True

            self.assertEqual(
                success,
                True,
                "Check if the root volume resized appropriately"
            )
        else:
            self.debug(
                "hypervisor %s unsupported for test 00, "
                "verifying it errors properly" % self.hypervisor)

            newrootsize = (self.template.size >> 30) + 2
            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if re.search(
                        "Hypervisor \S+ does not support rootdisksize "
                        "override", str(ex)):
                    success = True
                else:
                    self.debug(
                        "virtual machine create did not fail appropriately."
                        " Error was actually : " + str(ex))

            self.assertEqual(
                success,
                True,
                "Check if unsupported hypervisor %s fails appropriately" %
                self.hypervisor)

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware="true")
    def test_01_deploy_vm_root_resize(self):
        """Test proper failure to deploy virtual machine with
           rootdisksize of 0
        """
        if (self.hypervisor.lower() == 'kvm'):
            newrootsize = 0
            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if "rootdisk size should be a non zero number" in str(ex):
                    success = True
                else:
                    self.debug(
                        "virtual machine create did not fail appropriately."
                        " Error was actually : " + str(ex))

            self.assertEqual(
                success,
                True,
                "Check if passing 0 as rootdisksize fails appropriately")
        else:
            self.debug(
                "test 01 does not support hypervisor type " +
                self.hypervisor)

    @attr(
        tags=[
            'advanced',
            'basic',
            'sg'],
        required_hardware="true",
        BugId="CLOUDSTACK-6984")
    def test_02_deploy_vm_root_resize(self):
        """Test proper failure to deploy virtual machine with
           rootdisksize less than template size
        """
        if (self.hypervisor.lower() == 'kvm'):
            newrootsize = (self.template.size >> 30) - 1

            self.assertEqual(
                newrootsize > 0,
                True,
                "Provided template is less than 1G in size, cannot run test")

            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if "rootdisksize override is smaller than template size" in \
                        str(ex):
                    success = True
                else:
                    self.debug(
                        "virtual machine create did not fail appropriately."
                        " Error was actually : " + str(ex))

            self.assertEqual(
                success,
                True,
                "Check if passing rootdisksize < "
                "templatesize fails appropriately")
        else:
            self.debug(
                "test 01 does not support hypervisor type " +
                self.hypervisor)

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


class TestDeployVmWithUserData(cloudstackTestCase):

    """Tests for UserData
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVmWithUserData, cls).getClsTestClient()
        cls.apiClient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        cls.zone = get_zone(cls.apiClient, testClient.getZoneForTests())
        if cls.zone.localstorageenabled:
            # For devcloud since localstroage is enabled
            cls.services["service_offerings"]["tiny"]["storagetype"] = "local"
        cls.service_offering = ServiceOffering.create(
            cls.apiClient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.account = Account.create(
            cls.apiClient,
            services=cls.services["account"])
        cls.cleanup = [cls.account]
        cls.template = get_template(
            cls.apiClient,
            cls.zone.id,
            cls.services["ostype"]
        )

        if cls.template == FAILED:
            assert False,\
                "get_template() failed to return template with description" \
                " %s" % cls.services["ostype"]

        cls.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,
                          cls.account.id))

        # Generate userdata of 2500 bytes. This is larger than
        # the 2048 bytes limit.
        # CS however allows for upto 4K bytes in the code.
        # So this must succeed.
        # Overall, the query length must not exceed 4K, for then
        # the json decoder will fail this operation at
        # the marvin client side itcls.
        user_data = ''.join(
            random.choice(
                string.ascii_uppercase +
                string.digits) for x in range(2500))
        cls.services["virtual_machine"]["userdata"] = user_data

    def setup(self):
        self.hypervisor = self.testClient.getHypervisorInfo()

    @attr(
        tags=[
            "devcloud",
            "basic",
            "advanced",
            "post"],
        required_hardware="true")
    def test_deployvm_userdata_post(self):
        """Test userdata as POST, size > 2k
        """
        deployVmResponse = VirtualMachine.create(
            self.apiClient,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            method='POST'
        )
        vms = list_virtual_machines(
            self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assert_(
            len(vms) > 0,
            "There are no Vms deployed in the account %s" %
            self.account.name)
        vm = vms[0]
        self.assert_(
            vm.id == str(
                deployVmResponse.id),
            "Vm deployed is different from the test")
        self.assert_(vm.state == "Running", "VM is not in Running state")

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiClient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
