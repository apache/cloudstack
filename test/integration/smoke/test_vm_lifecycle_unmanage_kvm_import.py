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
""" BVT tests for Virtual Machine Life Cycle - Unmanage - Import
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import *
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Network,
                             NetworkOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template)
from marvin.codes import FAILED
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf
import unittest
from marvin.sshClient import SshClient

_multiprocess_shared_ = True

class TestUnmanageKvmVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestUnmanageKvmVM, cls).getClsTestClient()
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

        cls.hypervisorNotSupported = cls.hypervisor.lower() != "kvm"
        if cls.hypervisorNotSupported:
            return

        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

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

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-network_offering"],
        )
        cls._cleanup.append(cls.network_offering)
        cls.network_offering.update(cls.apiclient, state='Enabled')
        cls.isolated_network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["nw_off_isolated_persistent"],
        )
        cls._cleanup.append(cls.isolated_network_offering)
        cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

    @classmethod
    def tearDownClass(cls):
        super(TestUnmanageKvmVM, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.created_networks = []
        self.virtual_machine = None
        self.unmanaged_instance = None
        self.imported_vm = None
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        if self.hypervisorNotSupported:
            return
        self.services["network"]["networkoffering"] = self.network_offering.id
        network_data = self.services["l2-network"]
        self.network = Network.create(
            self.apiclient,
            network_data,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.cleanup.append(self.network)
        self.created_networks.append(self.network)
        network_data['name'] = "Test L2 Network1"
        network_data['displaytext'] = "Test L2 Network1"
        self.network1 = Network.create(
            self.apiclient,
            network_data,
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.cleanup.append(self.network1)
        self.created_networks.append(self.network1)
        self.network2 = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            zoneid=self.zone.id,
            networkofferingid=self.isolated_network_offering.id
        )
        self.cleanup.append(self.network2)
        self.created_networks.append(self.network2)


    def tearDown(self):
        super(TestUnmanageKvmVM, self).tearDown()

    def check_vm_state(self, vm_id):
        list_vm = VirtualMachine.list(
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
        return vm_response


    @attr(tags=["advanced", "advancedns", "smoke", "sg"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_01_unmanage_vm_cycle_persistent_domain(self):
        """
        Test the following:
        1. Deploy VM
        2. Unmanage VM
        3. Verify VM is not listed in CloudStack
        4. Verify VM is listed as part of the unmanaged instances
        5. Verify VM domain is persistent for KVM hypervisor
        6. Stop VM using virsh, confirm VM is stopped
        7. Start VM using virsh, confirm VM is running
        8. Import VM
        9. Verify details of imported VM
        10. Destroy VM
        """

        # 1 - Deploy VM
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.small_offering.id,
            networkids=[self.network.id, self.network1.id, self.network2.id],
            zoneid=self.zone.id
        )

        vm_id = self.virtual_machine.id
        vm_instance_name = self.virtual_machine.instancename

        networks = []
        for network in self.created_networks:
            n = Network.list(
                self.apiclient,
                id=network.id
            )[0]
            networks.append(n)
        hostid = self.virtual_machine.hostid
        hosts = Host.list(
            self.apiclient,
            id=hostid
        )
        host = hosts[0]
        clusterid = host.clusterid
        self.check_vm_state(vm_id)

        # 2 - Unmanage VM from CloudStack
        self.virtual_machine.unmanage(self.apiclient)

        # 3 - Verify VM is not listed in CloudStack
        list_vm = VirtualMachine.list(
            self.apiclient,
            id=vm_id
        )
        self.assertEqual(
            list_vm,
            None,
            "VM should not be listed"
        )
        # 4 - Verify VM is listed as part of the unmanaged instances
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

        # 5 - Verify VM domain is persistent for KVM
        ssh_host = self.get_ssh_client(host.ipaddress,
                                    self.hostConfig["username"],
                                    self.hostConfig["password"], 10)

        cmd = f"virsh dominfo {vm_instance_name}"
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception(f"Failed to fetch domain info for VM: {vm_instance_name} on host: {host.name}. Error: {result}")
        persistent_line = next((line for line in result if "Persistent:" in line), None)
        if not persistent_line:
            raise Exception(f"'Persistent' info not found in dominfo output for VM: {vm_instance_name} on host: {host.name}")
        if "yes" not in persistent_line.lower():
            raise Exception(f"VM: {vm_instance_name} is NOT persistent on host: {host.name}")


        # 6 - Stop VM using virsh, confirm VM is stopped
        host = Host.list(
            self.apiclient,
            id=unmanaged_vm.hostid
        )[0]
        cmd = "virsh destroy %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to stop VM: %s on host: %s" % (vm_instance_name, host.name))

        cmd = "virsh list --all | grep %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to fetch VM: %s state on host: %s" % (vm_instance_name, host.name))
        state_line = next((line for line in result if vm_instance_name in line), None)
        if not state_line:
            raise Exception(f"VM: {vm_instance_name} not found in 'virsh list --all' output on host: {host.name}")
        if 'shut off' not in state_line.lower():
            raise Exception(f"VM: {vm_instance_name} is NOT stopped on host: {host.name}, state: {state_line}")

        # 7 - Start VM using virsh, confirm VM is running
        cmd = "virsh start %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to start VM: %s on host: %s" % (vm_instance_name, host.name))
        time.sleep(30)
        cmd = "virsh list | grep %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to fetch VM: %s state on host: %s" % (vm_instance_name, host.name))
        if 'running' not in str(result).lower():
            raise Exception(f"VM: {vm_instance_name} is NOT running on host: {host.name}, state: {state_line}")

        # 8 - Import VM

        nicnetworklist = []
        nicipaddresslist = []
        for nic in unmanaged_vm.nic:
            for network in networks:
                if int(network.vlan) == int(nic.vlanid):
                    nicnetworklist.append({
                        "nic": nic.id,
                        "network": network.id
                    })
                    if network.type == "Isolated":
                        nicipaddresslist.append({
                            "nic": nic.id,
                            "ip4Address": "auto"
                        })
        import_vm_service = {
            "nicnetworklist": nicnetworklist,
            "nicipaddresslist": nicipaddresslist
        }

        self.imported_vm = VirtualMachine.importUnmanagedInstance(
            self.apiclient,
            clusterid=clusterid,
            name=vm_instance_name,
            serviceofferingid=self.small_offering.id,
            services=import_vm_service,
            templateid=self.template.id)
        self.cleanup.append(self.imported_vm)
        self.unmanaged_instance = None
        # 9 - Verify details of the imported VM
        self.assertEqual(
            self.small_offering.id,
            self.imported_vm.serviceofferingid,
            "Imported VM service offering is different, expected: %s, actual: %s" % (self.small_offering.id, self.imported_vm.serviceofferingid)
        )
        self.assertEqual(
            self.template.id,
            self.imported_vm.templateid,
            "Imported VM template is different, expected: %s, actual: %s" % (self.template.id, self.imported_vm.templateid)
        )
        self.check_vm_state(self.imported_vm.id)
        # 10 - Destroy VM. This will be done during cleanup



    @attr(tags=["advanced", "advancedns", "smoke", "sg"], required_hardware="true")
    @skipTestIf("hypervisorNotSupported")
    def test_02_unmanage_stopped_vm_cycle_persistent_domain(self):
        """
        Test the following:
        1. Deploy VM
        2. Stop VM
        3. Unmanage VM
        4. Verify VM is not listed in CloudStack
        5. Verify VM is listed as part of the unmanaged instances
        6. Start VM using virsh, confirm VM is running
        7. Import VM
        8. Verify details of imported VM
        9. Destroy VM
        """
        # 1 - Deploy VM
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.small_offering.id,
            networkids=[self.network.id, self.network1.id, self.network2.id],
            zoneid=self.zone.id
        )

        vm_id = self.virtual_machine.id
        vm_instance_name = self.virtual_machine.instancename

        networks = []
        for network in self.created_networks:
            n = Network.list(
                self.apiclient,
                id=network.id
            )[0]
            networks.append(n)
        hostid = self.virtual_machine.hostid
        hosts = Host.list(
            self.apiclient,
            id=hostid
        )
        host = hosts[0]
        clusterid = host.clusterid
        self.check_vm_state(vm_id)

        #2 - Stop VM
        self.virtual_machine.stop(self.apiclient)
        list_vm = VirtualMachine.list(
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
            "Stopped",
            "VM state should be Stopped after stopping the VM"
        )

        # 3 - Unmanage VM from CloudStack
        self.virtual_machine.unmanage(self.apiclient)

        # 4 - Verify VM is not listed in CloudStack
        list_vm = VirtualMachine.list(
            self.apiclient,
            id=vm_id
        )
        self.assertEqual(
            list_vm,
            None,
            "VM should not be listed"
        )

        # 5 - Verify VM is listed as part of the unmanaged instances
        ssh_host = self.get_ssh_client(host.ipaddress,
                                    self.hostConfig["username"],
                                    self.hostConfig["password"], 10)
        cmd = "virsh list --all | grep %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to fetch VM: %s state on host: %s" % (vm_instance_name, host.name))
        if 'shut off' not in str(result).lower():
            raise Exception(f"VM: {vm_instance_name} is NOT in stopped on host: {host.name}")

        # 6 - Start VM using virsh, confirm VM is running
        cmd = "virsh start %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to start VM: %s on host: %s" % (vm_instance_name, host.name))
        time.sleep(30)
        cmd = "virsh list | grep %s" % vm_instance_name
        result = ssh_host.execute(cmd)
        if result == None or result == "":
            raise Exception("Failed to fetch VM: %s state on host: %s" % (vm_instance_name, host.name))
        if 'running' not in str(result).lower():
            raise Exception(f"VM: {vm_instance_name} is NOT running on host: {host.name}")

        # 8 - Import VM
        self.imported_vm = VirtualMachine.importUnmanagedInstance(
            self.apiclient,
            clusterid=clusterid,
            name=vm_instance_name,
            serviceofferingid=self.small_offering.id,
            services={},
            templateid=self.template.id)
        self.cleanup.append(self.imported_vm)
        self.unmanaged_instance = None

        # 9 - Verify details of the imported VM
        self.assertEqual(
            self.small_offering.id,
            self.imported_vm.serviceofferingid,
            "Imported VM service offering is different, expected: %s, actual: %s" % (self.small_offering.id, self.imported_vm.serviceofferingid)
        )
        self.assertEqual(
            self.template.id,
            self.imported_vm.templateid,
            "Imported VM template is different, expected: %s, actual: %s" % (self.template.id, self.imported_vm.templateid)
        )
        self.check_vm_state(self.imported_vm.id)
        # 10 - Destroy VM. This will be done during cleanup


    def get_ssh_client(self, ip, username, password, retries=10):
        """ Setup ssh client connection and return connection """
        try:
            ssh_client = SshClient(ip, 22, username, password, retries)
        except Exception as e:
            raise unittest.SkipTest("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to ip=%s" % ip)

        return ssh_client
