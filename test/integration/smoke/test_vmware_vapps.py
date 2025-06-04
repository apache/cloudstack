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
        cls._cleanup = []

        if cls.hypervisorNotSupported == False:

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
            cls._cleanup.append(cls.custom_offering)

            cls.isolated_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["isolated_network_offering"],
            )
            cls._cleanup.append(cls.isolated_network_offering)
            cls.isolated_network_offering.update(cls.apiclient, state='Enabled')

            cls.l2_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["l2-network_offering"],
            )
            cls._cleanup.append(cls.l2_network_offering)
            cls.l2_network_offering.update(cls.apiclient, state='Enabled')

            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

    @classmethod
    def tearDownClass(cls):
        super(TestVAppsVM, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestVAppsVM, self).tearDown()

    def get_ova_parsed_information_from_template(self, template):
        if not template:
            return None
        details = template.deployasisdetails.__dict__
        configurations = []
        disks = []
        isos = []
        networks = []
        for propKey in details:
            if propKey.startswith('configuration'):
                configurations.append(json.loads(details[propKey]))
            elif propKey.startswith('disk'):
                detail = json.loads(details[propKey])
                if detail['isIso'] == True:
                    isos.append(detail)
                else:
                    disks.append(detail)
            elif propKey.startswith('network'):
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
