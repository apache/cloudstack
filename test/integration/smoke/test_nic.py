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
""" NIC tests for VM """
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Configurations,
                             Host,
                             ServiceOffering,
                             Network,
                             VirtualMachine,
                             NetworkOffering)
from marvin.lib.common import (get_zone,
                               get_suitable_test_template,
                               get_domain)
from marvin.lib.utils import validateList, wait_until
from marvin.sshClient import SshClient
from marvin.codes import PASS
from nose.plugins.attrib import attr

import signal
import sys
import logging
import time
import threading
from queue import Queue


class TestNic(cloudstackTestCase):

    def setUp(self):
        self.cleanup = []
        self.logger = logging.getLogger('TestNIC')
        self.stream_handler = logging.StreamHandler()
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.stream_handler)


        def signal_handler(signal, frame):
            self.tearDown()
            sys.exit(0)

        # assign the signal handler immediately
        signal.signal(signal.SIGINT, signal_handler)

        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() == "hyperv":
            self.skipTest("Not supported on Hyper-V")

        try:
            self.apiclient = self.testClient.getApiClient()
            self.dbclient = self.testClient.getDbConnection()
            self.services = self.testClient.getParsedTestDataConfig()

            # Get Zone, Domain and templates
            domain = get_domain(self.apiclient)
            self.zone = get_zone(
                self.apiclient,
                self.testClient.getZoneForTests()
                )

            # if local storage is enabled, alter the offerings to use
            # localstorage
            # this step is needed for devcloud
            if self.zone.localstorageenabled:
                self.services["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
            template = get_suitable_test_template(
                self.apiclient,
                self.zone.id,
                self.services["ostype"],
                self.hypervisor
            )
            if template == FAILED:
                assert False, "get_suitable_test_template() failed to return template with description %s" % self.services["ostype"]
            # Set Zones and disk offerings
            self.services["small"]["zoneid"] = self.zone.id
            self.services["small"]["template"] = template.id

            self.services["iso1"]["zoneid"] = self.zone.id
            self.services["network"]["zoneid"] = self.zone.id

            # Create Account, VMs, NAT Rules etc
            self.account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=domain.id
            )
            self.cleanup.insert(0, self.account)

            self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offerings"]["tiny"]
            )
            self.cleanup.insert(0, self.service_offering)

            ####################
            # Network offering
            self.network_offering = NetworkOffering.create(
                self.apiclient,
                self.services["network_offering"],
            )
            self.cleanup.insert(0, self.network_offering)
            self.network_offering.update(
                self.apiclient,
                state='Enabled')  # Enable Network offering
            self.services["network"][
                "networkoffering"] = self.network_offering.id

            self.network_offering_shared = NetworkOffering.create(
                self.apiclient,
                self.services["network_offering_shared"],
            )
            self.cleanup.insert(0, self.network_offering_shared)
            self.network_offering_shared.update(
                self.apiclient,
                state='Enabled')  # Enable Network offering
            self.services["network2"][
                "networkoffering"] = self.network_offering_shared.id

            ################
            # Test Network
            self.test_network = Network.create(
                self.apiclient,
                self.services["network"],
                self.account.name,
                self.account.domainid,
            )
            self.cleanup.insert(0, self.test_network)
            self.test_network2 = Network.create(
                self.apiclient,
                self.services["network2"],
                self.account.name,
                self.account.domainid,
                zoneid=self.services["network"]["zoneid"]
            )
            self.cleanup.insert(0, self.test_network2)
        except Exception:
            self.tearDown()
            raise

    @attr(
        tags=[
            "devcloud",
            "smoke",
            "advanced",
            "advancedns"],
        required_hardware="true")
    def test_01_nic(self):
        # TODO: SIMENH: add validation
        """Test to add and update added nic to a virtual machine"""

        hypervisorIsVmware = False
        isVmwareToolInstalled = False
        if self.hypervisor.lower() == "vmware":
            hypervisorIsVmware = True

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.test_network.id],
            mode=self.zone.networktype if hypervisorIsVmware else "default"
        )

        self.cleanup.insert(0, self.virtual_machine)
        vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.assertEqual(
                validateList(vms)[0],
                PASS,
                "vms list validation failed")

        vm_response = vms[0]

        self.assertEqual(
            len(vm_response.nic),
            1,
            "Verify we only start with one nic"
        )

        self.assertEqual(
            vm_response.nic[0].isdefault,
            True,
            "Verify initial adapter is set to default"
        )
        existing_nic_ip = vm_response.nic[0].ipaddress
        existing_nic_id = vm_response.nic[0].id

        self.virtual_machine.add_nic(
                    self.apiclient,
                    self.test_network2.id)
        list_vm_response = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine.id
            )

        self.assertEqual(
                len(list_vm_response[0].nic),
                2,
                "Verify we have 2 NIC's now"
            )

        # If hypervisor is Vmware, then check if
        # the vmware tools are installed and the process is running
        # Vmware tools are necessary for remove nic operations (vmware 5.5+)
        if hypervisorIsVmware:
            sshClient = self.virtual_machine.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            self.debug("and result is: %s" % result)
            if "running" in result:
                isVmwareToolInstalled = True

        goForUnplugOperation = True
        # If Vmware tools are not installed in case of vmware hypervisor
        # then don't go further for unplug operation (remove nic) as it won't
        # be supported
        if hypervisorIsVmware and not isVmwareToolInstalled:
            goForUnplugOperation = False


        if goForUnplugOperation:
            new_nic_id = ""
            for nc in list_vm_response[0].nic:
                if nc.ipaddress != existing_nic_ip:
                    new_nic_id = nc.id

            self.virtual_machine.update_default_nic(self.apiclient, new_nic_id)

            time.sleep(5)

            list_vm_response = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine.id
            )

            # iterate as we don't know for sure what order our NIC's will be
            # returned to us.
            for nc in list_vm_response[0].nic:
                if nc.ipaddress == existing_nic_ip:
                    self.assertEqual(
                        nc.isdefault,
                        False,
                        "Verify initial adapter is NOT set to default"
                    )
                else:
                    self.assertEqual(
                        nc.isdefault,
                        True,
                        "Verify second adapter is set to default"
                    )

            with self.assertRaises(Exception):
                self.virtual_machine.remove_nic(self.apiclient, new_nic_id)

            self.virtual_machine.update_default_nic(
                self.apiclient,
                existing_nic_id)
            time.sleep(5)
            self.virtual_machine.remove_nic(self.apiclient, new_nic_id)
            time.sleep(5)

            list_vm_response = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine.id
            )

            self.assertEqual(
                len(list_vm_response[0].nic),
                1,
                "Verify we are back to a signle NIC"
            )

        return

    def test_02_nic_with_mac(self):
        """Test to add and update added nic to a virtual machine with specific mac"""

        hypervisorIsVmware = False
        isVmwareToolInstalled = False
        if self.hypervisor.lower() == "vmware":
            hypervisorIsVmware = True

        self.virtual_machine2 = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.test_network.id],
            macaddress="aa:bb:cc:dd:ee:ff",
            mode=self.zone.networktype if hypervisorIsVmware else "default"
        )
        self.cleanup.insert(0, self.virtual_machine2)
        self.assertEqual(self.virtual_machine2.nic[0].macaddress, "aa:bb:cc:dd:ee:ff", "Mac address not honored")
        vmdata = self.virtual_machine2.add_nic(
            self.apiclient,
            self.test_network2.id,
            macaddress="ee:ee:dd:cc:bb:aa")
        found = False
        for n in vmdata.nic:
            if n.macaddress == "ee:ee:dd:cc:bb:aa":
                found = True
                break

        self.assertTrue(found, "Nic not successfully added with specified mac address")


    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_nic_multiple_vmware(self):
        """Test to adding multiple nics to a VMware VM and restarting VM

           Refer to CLOUDSTACK-10107 for details, in this test we add 8 nics to
           a VM and stop, start it to show that VMware VMs are not limited to
           having up to 7 nics.
        """

        if self.hypervisor.lower() != "vmware":
            self.skipTest("Skipping test applicable for VMware")

        network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["nw_off_isolated_persistent"]
        )
        self.cleanup.insert(0, network_offering)
        network_offering.update(self.apiclient, state='Enabled')

        offering = dict(self.services["network"])
        offering["networkoffering"] = network_offering.id

        networks = []

        def createNetwork(idx):
            offering["name"] = "Test Network%s" % idx
            network = Network.create(
                self.apiclient,
                offering,
                self.account.name,
                self.account.domainid,
                zoneid=self.services["network"]["zoneid"]
            )
            networks.append(network)
            self.cleanup.insert(0, network)


        class NetworkMaker(threading.Thread):
            def __init__(self, queue=None, createNetwork=None):
                threading.Thread.__init__(self)
                self.queue = queue
                self.createNetwork = createNetwork

            def run(self):
                while True:
                    idx = self.queue.get()
                    if idx is not None:
                        self.createNetwork(idx)
                    self.queue.task_done()

        # Start multiple networks
        tsize = 8
        queue = Queue()
        for _ in range(tsize):
            worker = NetworkMaker(queue, createNetwork)
            worker.setDaemon(True)
            worker.start()

        for idx in range(tsize):
            queue.put(idx)
        queue.join()

        # Deploy a VM
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[networks[0].id],
            mode=self.zone.networktype
        )
        self.cleanup.insert(0, vm)

        # Add nics to networks
        for network in networks[1:]:
            response = vm.add_nic(self.apiclient, network.id)
            found = False
            for nic in response.nic:
                if nic.networkid == network.id:
                    found = True
                    break
            self.assertTrue(found, "Nic not successfully added for the specific network")

        # Stop VM
        vm.stop(self.apiclient, forced=True)

        vms = VirtualMachine.list(
            self.apiclient,
            id=vm.id
        )
        self.assertEqual(
                validateList(vms)[0],
                PASS,
                "vms list validation failed")

        vm_response = vms[0]
        self.assertEqual(
            vm_response.state,
            "Stopped",
            "Verify the VM is stopped"
        )

        # Start VM
        vm.start(self.apiclient)

        vms = VirtualMachine.list(
            self.apiclient,
            id=vm.id
        )
        self.assertEqual(
                validateList(vms)[0],
                PASS,
                "vms list validation failed")

        vm_response = vms[0]
        self.assertEqual(
            vm_response.state,
            "Running",
            "Verify the VM is running"
        )

        self.assertTrue(len(vm_response.nic) == len(networks), "Number of nics on VM not 8")

        # Validate nics exist on each of the network
        for network in networks:
            found = False
            for nic in vm_response.nic:
                if nic.networkid == network.id:
                    found = True
                    break
            self.assertTrue(found, "Nic not found for the specific network")


    def get_vm_nic(self, vm_uuid):
        return self.dbclient.execute(
            "SELECT n.state, n.broadcast_uri, n.ip4_address, n.network_id FROM nics n JOIN vm_instance v ON n.instance_id = v.id "
            "WHERE v.uuid = '%s' AND n.removed IS NULL" % vm_uuid)[0]

    def get_nics_count(self, network_id):
        return self.dbclient.execute("SELECT nics_count FROM op_networks WHERE id = %d" % network_id)[0][0]

    def wait_for_vm_and_nic_state(self, vm, vm_state, nic_state, timeout):
        def check_state():
            vms = VirtualMachine.list(self.apiclient, id=vm.id, listall=True)
            nic = self.get_vm_nic(vm.id)
            return vms is not None and vms[0].state == vm_state and nic[0] == nic_state, nic

        result, nic = wait_until(5, timeout // 5, check_state)
        self.assertTrue(result, "VM %s did not reach state %s with nic state %s, nic is %s" % (vm.name, vm_state, nic_state, nic))
        return nic

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_nic_reserved_again_after_out_of_band_power_on(self):
        """Test that a NIC released on a missing VM is reserved again and nics_count restored once the VM runs again

        1. Deploy a VM in an isolated network, record its nic and the network's nics_count
        2. Destroy the domain with virsh, so the VM is reported missing and synced to Stopped
        3. Verify the nic is released (Allocated, no broadcast uri) and nics_count went down by one
        4. Recreate the domain with virsh, so the VM is synced to Running out of band
        5. Verify the nic is Reserved again with the same broadcast uri and ip, and nics_count is restored
        """
        if self.hypervisor.lower() != "kvm":
            self.skipTest("Out of band power on through virsh is only tested on KVM")

        host_config = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        ping_interval = int(Configurations.list(self.apiclient, name="ping.interval")[0].value)

        vm = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[self.test_network.id]
        )
        self.cleanup.insert(0, vm)

        # 1 - record the reserved nic and nics_count
        nic_state, broadcast_uri, ip_address, network_id = self.get_vm_nic(vm.id)
        self.assertEqual(nic_state, "Reserved", "Nic of a running VM should be Reserved")
        self.assertIsNotNone(broadcast_uri, "Nic of a running VM should have a broadcast uri")
        nics_count = self.get_nics_count(network_id)

        vm_response = VirtualMachine.list(self.apiclient, id=vm.id, listall=True)[0]
        host = Host.list(self.apiclient, id=vm_response.hostid)[0]
        ssh_host = SshClient(host.ipaddress, 22, host_config["username"], host_config["password"], 10)
        domain_xml = "/tmp/%s.xml" % vm_response.instancename
        self.cleanup_domain_xml = lambda: ssh_host.execute("rm -f %s" % domain_xml)

        # 2 - make the VM go missing
        ssh_host.execute("virsh dumpxml %s > %s" % (vm_response.instancename, domain_xml))
        ssh_host.execute("virsh destroy %s" % vm_response.instancename)

        # 3 - nic released and nics_count decremented
        # a missing VM is only synced after a graceful period of two ping intervals, noticed on the next ping
        released_nic = self.wait_for_vm_and_nic_state(vm, "Stopped", "Allocated", 3 * ping_interval + 30)
        self.assertIsNone(released_nic[1], "Released nic should not have a broadcast uri")
        self.assertEqual(self.get_nics_count(network_id), nics_count - 1, "nics_count should drop by one when the nic is released")

        # 4 - bring the domain back out of band, a persistent domain is still defined and only needs a start
        ssh_host.execute("virsh create %s || virsh start %s" % (domain_xml, vm_response.instancename))

        # 5 - nic reserved again and nics_count restored
        # the power on is noticed on the next ping
        reconciled_nic = self.wait_for_vm_and_nic_state(vm, "Running", "Reserved", ping_interval + 30)
        self.assertEqual(reconciled_nic[1], broadcast_uri, "Reconciled nic should get the network's broadcast uri back")
        self.assertEqual(reconciled_nic[2], ip_address, "Reconciled nic should keep its ip address")
        self.assertEqual(self.get_nics_count(network_id), nics_count, "nics_count should be restored when the nic is reserved again")

    def tearDown(self):
        if hasattr(self, "cleanup_domain_xml"):
            self.cleanup_domain_xml()
        try:
            for obj in self.cleanup:
                try:
                    obj.delete(self.apiclient)
                    time.sleep(10)
                except Exception as ex:
                    self.debug(
                        "Error deleting: " +
                        str(obj) +
                        ", exception: " +
                        str(ex))
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
