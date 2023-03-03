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

"""
Test case for router resources
"""

# Import local modules

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (scaleSystemVm,
                                  stopRouter,
                                  startRouter,
                                  restartNetwork,
                                  updateConfiguration)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (NetworkOffering,
                             ServiceOffering,
                             VirtualMachine,
                             Account,
                             Domain,
                             Network,
                             Router,
                             destroyRouter,
                             Zone,
                             updateResourceCount)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               list_virtual_machines,
                               list_networks,
                               list_configurations,
                               list_routers,
                               list_service_offering)

import logging

class TestRouterResources(cloudstackTestCase):

    @classmethod
    def setupClass(cls):
        cls.testClient = super(
            TestRouterResources, cls
        ).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestRouterResources")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["big"]
        )

        cls._cleanup.append(cls.service_offering)

        # Create new domain1
        cls.domain1 = Domain.create(
            cls.apiclient,
            services=cls.services["acl"]["domain1"],
            parentdomainid=cls.domain.id
        )

        cls._cleanup.append(cls.domain1)

        # Create account1
        cls.account1 = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD1"],
            domainid=cls.domain1.id
        )

        cls._cleanup.append(cls.account1)

        # Create Network Offering with all the services
        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"]
        )

        cls._cleanup.append(cls.network_offering)

        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.network = Network.create(
            cls.apiclient,
            cls.services["isolated_network"],
            accountid=cls.account1.name,
            domainid=cls.account1.domainid,
            networkofferingid=cls.network_offering.id,
            zoneid=cls.zone.id
        )

        cls._cleanup.append(cls.network)

        virtualmachine = VirtualMachine.create(
            cls.apiclient,
            services=cls.services["virtual_machine_userdata"],
            accountid=cls.account1.name,
            domainid=cls.account1.domainid,
            serviceofferingid=cls.service_offering.id,
            networkids=cls.network.id,
            templateid=cls.template.id,
            zoneid=cls.zone.id
        )

        cls._cleanup.append(virtualmachine)

        vms = list_virtual_machines(
            cls.apiclient,
            account=cls.account1.name,
            domainid=cls.account1.domainid,
            id=virtualmachine.id
        )
        vm = vms[0]

        # get vm cpu and memory values
        cls.vm_cpu_count = vm.cpunumber
        cls.vm_mem_count = vm.memory

        routers = list_routers(
            cls.apiclient,
            account=cls.account1.name,
            domainid=cls.account1.domainid
        )

        router = routers[0]

        router_so_id = router.serviceofferingid

        list_service_response = list_service_offering(
            cls.apiclient,
            id=router_so_id,
            issystem="true",
            systemvmtype="domainrouter",
            listall="true"
        )

        # Get default router service offering cpu and memory values
        cls.default_vr_cpu = list_service_response[0].cpunumber
        cls.default_vr_ram = list_service_response[0].memory

        # Disable Network offering
        cls.network_offering.update(cls.apiclient, state='Disabled')

    @classmethod
    def tearDownClass(cls):
        super(TestRouterResources, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestRouterResources, self).tearDown()

    def get_resource_amount(self, resource_type):
        """
        Function to update resource count of a domain
        for the corresponding resource_type passed as parameter
        :param resource_type:
        :return: resource count
        """
        cmd = updateResourceCount.updateResourceCountCmd()
        cmd.account = self.account1.name
        cmd.domainid = self.domain1.id
        cmd.resourcetype = resource_type
        response = self.apiclient.updateResourceCount(cmd)
        amount = response[0].resourcecount
        return amount

    def update_configuration(self, name, value, domainid):
        """
        Function to update the global setting value for the domain
        :param name:
        :param value:
        :param domainid:
        :return:
        """
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = name
        updateConfigurationCmd.value = value
        updateConfigurationCmd.domainid = domainid
        return self.apiclient.updateConfiguration(updateConfigurationCmd)

    def get_vr_service_offering(self):
        """
        Function to get Virtual Router service offering
        :return:
        """
        routers = list_routers(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid
        )

        router = routers[0]

        router_so_id = router.serviceofferingid

        list_service_response = list_service_offering(
            self.apiclient,
            id=router_so_id,
            issystem="true",
            listall="true"
        )

        return list_service_response

    def stop_router(self, routerid):
        cmd = stopRouter.stopRouterCmd()
        cmd.id = routerid
        cmd.forced = "true"
        self.apiclient.stopRouter(cmd)

    def destroy_router(self, routerid):
        self.stop_router(routerid)
        cmd = destroyRouter.destroyRouterCmd()
        cmd.id = routerid
        self.apiclient.destroyRouter(cmd)

    def restart_network(self):
        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = self.network.id
        cmd.cleanup = True
        self.apiclient.restartNetwork(cmd)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_01_count_vm_resources(self):
        """
        Test case to just count running vm resources with global setting set to false

        # Steps
        1. Vm is already created
        2. Get the resource count of the running vm
        3. Update the resource count for the domain/account
        4. Make sure that these two values matches
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))

        self.info("Initial resource count of domain/account are")
        self.info("cpu is %s and ram is %s" % (cores, ram))

        self.assertEqual(
            self.vm_cpu_count,
            cores,
            "VM CPU count doesnt not match"
        )

        self.assertEqual(
            self.vm_mem_count,
            ram,
            "VM memory count does not match"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_02_count_vm_resources_with_all_vr_resource(self):
        """
        Test vm resource count along with vr resource count when global
        setting value is set to "all"

        # Steps
        1. Get the vm resource count from test case 1
        2. Get the default service offering of VR
        3. Extract cpu and ram size from it
        4. Set the global setting "resource.count.routers" to true
        5. Set the value of "resource.count.routers.type" to "all"
        6. Update the resource count of domain/account
        7. Make sure that the cpu and ram count is equal to (vm + vr)
        """

        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 2
        list_service_offering_response = self.get_vr_service_offering()

        # Step 3
        vr_cpu = list_service_offering_response[0].cpunumber
        vr_ram = list_service_offering_response[0].memory

        # Step 4
        self.update_configuration("resource.count.routers",
                                  "true", self.domain1.id)

        # Step 5
        self.update_configuration("resource.count.routers.type",
                                  "all", self.domain1.id)

        # Step 6
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count of domain/account are")
        self.info("cpu is %s and ram is %s" % (cores, ram))

        # Step 7
        self.assertEqual(
            cores,
            self.vm_cpu_count + vr_cpu,
            "Total resource count for cpu does not match VM + VR cpu count"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + vr_ram,
            "Total resource count for memory does not match VM + VR memory"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_03_count_vm_resources_with_delta_vr_resource(self):
        """
        Test vm resource count along with vr resource count when global setting
        value is set to "delta"

        # Steps
        1. Get the current service offering of VR
        2. Extract cpu and ram size from it
        3. Set the global setting "resource.count.routers" to true
        4. Set the value of "resource.count.routers.type" to "delta"
        5. Update the resource count of domain/account
        6. Make sure that the cpu and ram count is equal to
           VM + (current router offering - default router offering)
        """

        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1 and 2
        list_service_offering_response = self.get_vr_service_offering()
        new_vr_cpu = list_service_offering_response[0].cpunumber
        new_vr_ram = list_service_offering_response[0].memory

        # Step 3
        self.update_configuration("resource.count.routers",
                                  "true", self.domain1.id)

        # Step 4
        self.update_configuration("resource.count.routers.type",
                                  "delta", self.domain1.id)

        # Step 5
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count for domain/account are")
        self.info("cpu is %s and ram is %s" % (cores, ram))

        # Step 6
        self.assertEqual(
            cores,
            self.vm_cpu_count + (new_vr_cpu - self.default_vr_cpu),
            "Total resource count of cpu does not match delta for vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + (new_vr_ram - self.default_vr_ram),
            "Total resource count of memory does not match delta for vr memory"
        )

    def test_04_count_vm_resource_with_new_vr_offering_all(self):
        """
        Test to count vm resources along with new vr service offering with
        global setting set to "all"

        Steps
        1. Create a new router service offering with 2 cores and 2Gb Ram
        2. Stop the router
        3. Update the service offering of the router with new offering
        4. Start the router
        5. Set the global setting value to "all"
        6. Update the resource count of domain/account
        7. Get the new cpu/ram count of VR
        8. Make sure that the resource count is equal to VM + new VR service offering
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1
        offering_data = {
            'displaytext': 'TestOffering',
            'cpuspeed': 1000,
            'cpunumber': 2,
            'name': 'TestOffering',
            'memory': 2048,
            'issystem': 'true',
            'systemvmtype': 'domainrouter'
        }
        network_offering = self.new_network_offering = ServiceOffering.create(
            self.apiclient,
            offering_data,
            domainid=self.domain1.id
        )
        self.cleanup.append(network_offering)

        routers = list_routers(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid
        )

        router = routers[0]

        # Step 2
        # Stop the router
        self.stop_router(router.id)

        # Step 3
        scale_systemvm_cmd = scaleSystemVm.scaleSystemVmCmd()
        scale_systemvm_cmd.id=router.id
        scale_systemvm_cmd.serviceofferingid = self.new_network_offering.id
        self.apiclient.scaleSystemVm(scale_systemvm_cmd)

        # Step 4
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        # Step 5
        self.update_configuration("resource.count.routers.type",
                                  "all", self.domain1.id)

        # Step 6
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count for domain/account are")
        self.info("cpu is %s and ram is %s" % (cores,ram))

        # Step 7
        list_service_offering_response = self.get_vr_service_offering()
        updated_vr_cpu = list_service_offering_response[0].cpunumber
        updated_vr_ram = list_service_offering_response[0].memory

        # Step 8
        self.assertEqual(
            cores,
            self.vm_cpu_count + updated_vr_cpu,
            "Total resource count of cpu does not match delta for vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + updated_vr_ram,
            "Total resource count of memory does not match delta for vr memory"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_05_count_vm_resource_with_delta_vr_count(self):
        """
        Test to count vm resources along with new vr service offering with
        global setting set to "delta"

        Steps
        1. Set the global setting "resource.count.routers.type" value to "delta"
        2. Update the resource count of domain/account
        3. Get the new cpu/ram count of VR
        4. Make sure that the resource count is equal to
         VM + (new VR service offering - default router offering)
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1
        self.update_configuration("resource.count.routers.type",
                                  "delta", self.domain1.id)

        # Step 2
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count for domain/account are")
        self.info("cpu is %s and ram is %s" % (cores, ram))

        # Step 3
        list_service_offering_response = self.get_vr_service_offering()
        updated_vr_cpu = list_service_offering_response[0].cpunumber
        updated_vr_ram = list_service_offering_response[0].memory

        # Step 4
        self.assertEqual(
            cores,
            self.vm_cpu_count + (updated_vr_cpu - self.default_vr_cpu),
            "Total resource count of cpu does not match delta for vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + (updated_vr_ram - self.default_vr_ram),
            "Total resource count of memory does not match delta for vr memory"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_06_count_just_vm_resource(self):
        """
        Test to count vm resources when global setting "resource.count.routers"
        is set to false

        Steps
        1. Set the global setting "resource.count.routers" value to "false"
        2. Update the resource count of domain/account
        3. Make sure that the resource count is equal to VM resource count
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1
        # set resource.count.routers to false
        self.update_configuration("resource.count.routers",
                                  "false", self.domain1.id)

        # Step 2
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count for domain/account are")
        self.info("cpu is %s and ram is %s" % (cores,ram))

        # Step 3
        self.assertEqual(
            cores,
            self.vm_cpu_count,
            "Total resource count of cpu does not match delta for vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count,
            "Total resource count of memory does not match delta for vr memory"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_07_count_vm_resources_with_stopped_router(self):
        """
        Test to count vm resources when global setting "resource.count.routers"
        is set to true and VR is stopped

        Steps
        1. Set the global setting "resource.count.routers" value to "true"
        2. Stop the VR
        3. Update the resource count of domain/account
        4. Get the value of global setting "resource.count.running.vms.only
        5. If the above value is true then resource count should be equal to
           resource count of VM else its resource count of VM + VR
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1
        # set resource.count.routers to true
        self.update_configuration("resource.count.routers",
                                  "true", self.domain1.id)
        self.update_configuration("resource.count.routers.type",
                                  "all", self.domain1.id)

        # Step 2
        routers = list_routers(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid
        )

        router = routers[0]

        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        cmd.forced = True
        self.apiclient.stopRouter(cmd)

        # Step 3
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count for domain/account are")
        self.info("cpu is %s and ram is %s" % (cores,ram))

        # Step 4
        resource_count_running_vms = list_configurations(
            self.apiclient,
            name='resource.count.running.vms.only'
        )

        running_vms_only = resource_count_running_vms[0].value

        new_cpu_count = 0
        new_ram_size = 0

        if running_vms_only == 'true':
            new_cpu_count = self.vm_cpu_count
            new_ram_size = self.vm_mem_count
        else:
            list_service_offering_response = self.get_vr_service_offering()
            updated_vr_cpu = list_service_offering_response[0].cpunumber
            updated_vr_ram = list_service_offering_response[0].memory
            new_cpu_count = self.vm_cpu_count + updated_vr_cpu
            new_ram_size = self.vm_mem_count + updated_vr_ram

        # Step 5
        self.assertEqual(
            cores,
            new_cpu_count,
            "Total resource count of cpu does not match delta for vr cpu"
        )

        self.assertEqual(
            ram,
            new_ram_size,
            "Total resource count of memory does not match delta for vr memory"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_08_count_resources_restarting_network(self):
        """
        Test to count vm resources when global setting "resource.count.routers"
        is set to true and network is restarted with cleanup

        Steps
        1. Restart the network with cleanup option so that VR uses default offering
        2. Update the resource count of domain/account
        3. Make sure that the resource count is equal to VM + default VR service offering
        4. Create a new router service offering with 2 cores and 2Gb Ram
        5. Stop the router
        6. Update the service offering of the router with new offering
        7. Start the router
        8. Update the resource count of domain/account
        9. Make sure that the resource count is equal to VM + new VR service offering
        10. Set the global setting "router.service.offering" with new VR service offering
        11. Restart network with cleanup option
        12. Resource count should be equal to VM + (new VR offering)
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1
        self.restart_network()

        # Step 2
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))

        list_service_offering_response = self.get_vr_service_offering()
        default_vr_cpu = list_service_offering_response[0].cpunumber
        default_vr_ram = list_service_offering_response[0].memory

        # Step 3
        self.assertEqual(
            cores,
            self.vm_cpu_count + default_vr_cpu,
            "Total resource count of cpu does not match vm + default vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + default_vr_ram,
            "Total resource count of memory does not match vm + default vr ram"
        )

        # Step 4
        offering_data = {
            'displaytext': 'TestOffering2',
            'cpuspeed': 1000,
            'cpunumber': 2,
            'name': 'TestOffering2',
            'memory': 2048,
            'issystem': 'true',
            'systemvmtype': 'domainrouter'
        }

        network_offering = ServiceOffering.create(
            self.apiclient,
            offering_data,
            domainid=self.domain1.id
        )
        self.cleanup.append(network_offering)

        # Step 5
        routers = list_routers(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid
        )

        router = routers[0]

        self.stop_router(router.id)

        # Step 6
        scale_systemvm_cmd = scaleSystemVm.scaleSystemVmCmd()
        scale_systemvm_cmd.id=router.id
        scale_systemvm_cmd.serviceofferingid = network_offering.id
        self.apiclient.scaleSystemVm(scale_systemvm_cmd)

        # Step 7
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.apiclient.startRouter(cmd)

        # Step 8
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))

        list_service_offering_response = self.get_vr_service_offering()
        updated_vr_cpu = list_service_offering_response[0].cpunumber
        updated_vr_ram = list_service_offering_response[0].memory

        # Step 9
        self.assertEqual(
            cores,
            self.vm_cpu_count + updated_vr_cpu,
            "Total resource count of cpu does not match vm + new vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + updated_vr_ram,
            "Total resource count of memory does not match vm + new vr ram"
        )

        # Step 10
        self.update_configuration("router.service.offering", list_service_offering_response[0].id, None)

        # Step 11
        self.restart_network()

        # Step 12
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))

        self.assertEqual(
            cores,
            self.vm_cpu_count + updated_vr_cpu,
            "Total resource count of cpu does not match vm + new vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count + updated_vr_ram,
            "Total resource count of memory does not match vm + new vr ram"
        )

        self.update_configuration("router.service.offering", "", None)

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_09_count_vm_resources_with_destroyed_router(self):
        """
        Test to count vm resources when global setting "resource.count.routers"
        is set to true and VR is destroyed

        Steps
        1. Set the global setting "resource.count.routers" value to "true"
        2. Destroy the VR
        3. Update the resource count of domain/account
        4.The new resource count should be equal to VM resource count
        """
        CPU_RESOURCE_ID = 8
        RAM_RESOURCE_ID = 9

        # Step 1
        # set resource.count.routers to true
        self.update_configuration("resource.count.routers",
                                  "true", self.domain1.id)
        self.update_configuration("resource.count.routers.type",
                                  "all", self.domain1.id)

        # Step 2
        routers = list_routers(
            self.apiclient,
            account=self.account1.name,
            domainid=self.account1.domainid
        )

        router = routers[0]

        self.destroy_router(router.id)

        # Step 3
        cores = int(self.get_resource_amount(CPU_RESOURCE_ID))
        ram = int(self.get_resource_amount(RAM_RESOURCE_ID))
        self.info("New resource count for domain/account are")
        self.info("cpu is %s and ram is %s" % (cores,ram))

        # Step 4
        self.assertEqual(
            cores,
            self.vm_cpu_count,
            "Total resource count of cpu does not match delta for vr cpu"
        )

        self.assertEqual(
            ram,
            self.vm_mem_count,
            "Total resource count of memory does not match delta for vr memory"
        )
