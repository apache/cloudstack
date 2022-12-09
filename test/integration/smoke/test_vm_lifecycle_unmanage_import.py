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
from marvin.lib.vcenter import Vcenter

_multiprocess_shared_ = True

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
        super(TestUnmanageVM, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services["network"]["networkoffering"] = self.network_offering.id
        self.cleanup = []
        self.created_networks = []
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
        self.unmanaged_instance = None
        self.imported_vm = None

    '''
    Fetch vmware datacenter login details
    '''
    def get_vmware_dc_config(self, zone_id):
        zid = self.dbclient.execute("select id from data_center where uuid='%s';" %
                                    zone_id)
        vmware_dc_id = self.dbclient.execute(
            "select vmware_data_center_id from vmware_data_center_zone_map where zone_id='%s';" %
            zid[0])
        vmware_dc_config = self.dbclient.execute(
            "select vcenter_host, username, password from vmware_data_center where id = '%s';" % vmware_dc_id[0])

        return vmware_dc_config

    def delete_vcenter_vm(self, vm_name):
        config = self.get_vmware_dc_config(self.zone.id)
        vc_object = Vcenter(config[0][0], config[0][1], 'P@ssword123')
        vc_object.delete_vm(vm_name)

    def tearDown(self):
        if self.unmanaged_instance is not None:
            try:
                self.delete_vcenter_vm(self.unmanaged_instance)
            except Exception as e:
                print("Warning: Exception during cleaning up vCenter VM: %s : %s" % (self.unmanaged_instance, e))
        else:
            if self.virtual_machine is not None and self.imported_vm is None:
                self.cleanup.append(self.virtual_machine)
        super(TestUnmanageVM, self).tearDown()

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
        6. Verify details of imported VM
        7. Destroy VM
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
        self.unmanaged_instance = vm_instance_name
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
        # 5 - Import VM
        unmanaged_vm_nic = unmanaged_vm.nic[0]
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
        # 6 - Verify details of the imported VM
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
        self.assertEqual(
            len(nicnetworklist),
            len(self.imported_vm.nic),
            "Imported VM number of NICs is different, expected: %d, actual: %d" % (len(nicnetworklist), len(self.imported_vm.nic))
        )
        for nic in self.imported_vm.nic:
            index = int(nic.deviceid) # device id of imported nics will be in order of their import
            self.assertEqual(
                nicnetworklist[index]["network"],
                nic.networkid,
                "Imported VM NIC with id: %s has wrong network, expected: %s, actual: %s" % (nic.id, nicnetworklist[index]["network"], nic.networkid)
            )
        self.check_vm_state(self.imported_vm.id)
        # 7 - Destroy VM. This will be done during cleanup
