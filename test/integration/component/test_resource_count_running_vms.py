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
""" tests for Resource count of only running vms in cloudstack 4.14.0.0

"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (validateList,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             Domain,
                             Configurations,
                             Network,
                             NetworkOffering,
                             VirtualMachine,
                             Resources,
                             ServiceOffering,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               isAccountResourceCountEqualToExpectedCount)
from marvin.codes import (PASS, FAILED, RESOURCE_CPU, RESOURCE_MEMORY)
import logging
import random
import time

class TestResourceCountRunningVMs(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestResourceCountRunningVMs,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestResourceCountRunningVMs")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Domain and templates
        cls.domain = get_domain(cls.apiclient)

        cls.template = get_template(cls.apiclient, cls.zone.id, hypervisor="KVM")
        if cls.template == FAILED:
            sys.exit(1)
        cls.templatesize = (cls.template.size / (1024 ** 3))

        cls.services['mode'] = cls.zone.networktype
        # Create Account
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        accounts = Account.list(cls.apiclient, id=cls.account.id)
        cls.expectedCpu = int(accounts[0].cputotal)
        cls.expectedMemory = int(accounts[0].memorytotal)

        if cls.zone.securitygroupsenabled:
            cls.services["shared_network_offering"]["specifyVlan"] = 'True'
            cls.services["shared_network_offering"]["specifyIpRanges"] = 'True'

            cls.network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["shared_network_offering"]
            )
            cls.network_offering.update(cls.apiclient, state='Enabled')

            cls.account_network = Network.create(
                cls.apiclient,
                cls.services["network2"],
                networkofferingid=cls.network_offering.id,
                zoneid=cls.zone.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid
            )
        else:
            cls.network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["isolated_network_offering"],
            )
            # Enable Network offering
            cls.network_offering.update(cls.apiclient, state='Enabled')

            # Create account network
            cls.services["network"]["zoneid"] = cls.zone.id
            cls.services["network"]["networkoffering"] = cls.network_offering.id
            cls.account_network = Network.create(
                cls.apiclient,
                cls.services["network"],
                cls.account.name,
                cls.account.domainid
            )

        cls._cleanup.append(cls.account);
        cls._cleanup.append(cls.network_offering)

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def verify_resource_count_cpu_memory(self, expectedCpu, expectedMemory, account=None):
        if account is None:
            account = self.account
        response = matchResourceCount(
                        self.apiclient, expectedCpu,
                        RESOURCE_CPU,
                        accountid=account.id)
        self.assertEqual(response[0], PASS, response[1])

        response = matchResourceCount(
                        self.apiclient, expectedMemory,
                        RESOURCE_MEMORY,
                        accountid=account.id)
        self.assertEqual(response[0], PASS, response[1])

        result = isAccountResourceCountEqualToExpectedCount(
            self.apiclient, account.domainid, account.name,
            expectedCpu, RESOURCE_CPU)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count of cpu does not match")

        result = isAccountResourceCountEqualToExpectedCount(
            self.apiclient, account.domainid, account.name,
            expectedMemory, RESOURCE_MEMORY)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count of memory does not match")

    def update_account_resource_limitation(self, maxCpu, maxMemory):
        Resources.updateLimit(self.apiclient,
                        resourcetype=RESOURCE_CPU,
                        max=maxCpu,
                        domainid=self.account.domainid,
                        account=self.account.name)

        Resources.updateLimit(self.apiclient,
                        resourcetype=RESOURCE_MEMORY,
                        max=maxMemory,
                        domainid=self.account.domainid,
                        account=self.account.name)

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_01_resource_count_vm_with_normal_offering_in_all_states(self):
        """Create VM with normal offering. Take resources of vm in all states into calculation of resource count.

            Steps:
            # 1. update resource.count.running.vms.only to false
            # 2. create normal service offering
            # 3. deploy vm, resource count of cpu/ram increases
            # 4. stop vm, resource count of cpu/ram is not changed
            # 5. update vm with displayvm=false, resource count decreases
            # 6. update vm with displayvm=true, resource count increases
            # 7. start vm, resource count of cpu/ram is not changed
            # 8. reboot vm, resource count of cpu/ram is not changed
            # 9. destroy vm, resource count of cpu/ram decreases
            # 10. expunge vm, resource count of cpu/ram is not changed
        """

        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="false" )

        # Create small service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["small"]
        )
        self.cleanup.append(self.service_offering)

        # deploy vm
        try:
            virtual_machine_1 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCpu = self.expectedCpu + virtual_machine_1.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_1.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # stop vm
        virtual_machine_1.stop(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # update vm with displayvm=false
        virtual_machine_1.update(self.apiclient, displayvm=False)
        self.expectedCpu = self.expectedCpu - virtual_machine_1.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_1.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # update vm with displayvm=false
        virtual_machine_1.update(self.apiclient, displayvm=True)
        self.expectedCpu = self.expectedCpu + virtual_machine_1.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_1.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # start vm
        virtual_machine_1.start(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # reboot vm
        virtual_machine_1.reboot(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # destroy vm
        virtual_machine_1.delete(self.apiclient, expunge=False)
        self.expectedCpu = self.expectedCpu - virtual_machine_1.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_1.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # expunge vm
        virtual_machine_1.expunge(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_02_resource_count_vm_with_dynamic_offering_in_all_states(self):
        """Create VM with dynamic service offering. Take resources of vm in all states into calculation of resource count.

            Steps:
            # 1. update resource.count.running.vms.only to false
            # 2. create dynamic service offering
            # 3. deploy vm, resource count of cpu/ram increases
            # 4. stop vm, resource count of cpu/ram is not changed
            # 5. update vm with displayvm=false, resource count decreases
            # 6. update vm with displayvm=true, resource count increases
            # 7. start vm, resource count of cpu/ram is not changed
            # 8. reboot vm, resource count of cpu/ram is not changed
            # 9. destroy vm, resource count of cpu/ram decreases
            # 10. expunge vm, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="false" )

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])
        self.cleanup.append(self.service_offering)

        # deploy vm
        try:
            virtual_machine_2 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCpu = self.expectedCpu + virtual_machine_2.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_2.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # stop vm
        virtual_machine_2.stop(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # update vm with displayvm=false
        virtual_machine_2.update(self.apiclient, displayvm=False)
        self.expectedCpu = self.expectedCpu - virtual_machine_2.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_2.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # update vm with displayvm=false
        virtual_machine_2.update(self.apiclient, displayvm=True)
        self.expectedCpu = self.expectedCpu + virtual_machine_2.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_2.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # start vm
        virtual_machine_2.start(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # reboot vm
        virtual_machine_2.reboot(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # destroy vm
        virtual_machine_2.delete(self.apiclient, expunge=False)
        self.expectedCpu = self.expectedCpu - virtual_machine_2.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_2.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # expunge vm
        virtual_machine_2.expunge(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_03_resource_count_vm_with_normal_offering_in_running_state(self):
        """Create VM with normal service offering. Take resources of vm in running state into calculation of resource count.

            Steps:
            # 1. update resource.count.running.vms.only to true
            # 2. create normal service offering
            # 3. deploy vm, resource count of cpu/ram increases
            # 4. stop vm, resource count of cpu/ram decreases
            # 5. start vm, resource count of cpu/ram increases
            # 6. reboot vm, resource count of cpu/ram is not changed
            # 7. destroy vm, resource count of cpu/ram decreases
            # 8. recover vm, resource count of cpu/ram is not changed
            # 9. update vm with displayvm=false, resource count of cpu/ram is not changed
            # 10. destroy vm with expunge = true, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="true" )

        # Create service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["small"]
        )
        self.cleanup.append(self.service_offering)

        # deploy vm
        try:
            virtual_machine_3 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCpu = self.expectedCpu + virtual_machine_3.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_3.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # stop vm
        virtual_machine_3.stop(self.apiclient)
        self.expectedCpu = self.expectedCpu - virtual_machine_3.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_3.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # start vm
        virtual_machine_3.start(self.apiclient)
        self.expectedCpu = self.expectedCpu + virtual_machine_3.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_3.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # reboot vm
        virtual_machine_3.reboot(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # destroy vm
        virtual_machine_3.delete(self.apiclient, expunge=False)
        self.expectedCpu = self.expectedCpu - virtual_machine_3.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_3.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # recover vm
        virtual_machine_3.recover(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # update vm with displayvm=false
        virtual_machine_3.update(self.apiclient, displayvm=False)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # expunge vm
        virtual_machine_3.delete(self.apiclient, expunge=True)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_04_resource_count_vm_with_dynamic_offering_in_running_state(self):
        """Create VM with dynamic service offering. Take resources of vm in running state into calculation of resource count.

            Steps:
            # 1. update resource.count.running.vms.only to true
            # 2. create dynamic service offering
            # 3. deploy vm, resource count of cpu/ram increases
            # 4. stop vm, resource count of cpu/ram decreases
            # 5. start vm, resource count of cpu/ram increases
            # 6. reboot vm, resource count of cpu/ram is not changed
            # 7. destroy vm, resource count of cpu/ram decreases
            # 8. recover vm, resource count of cpu/ram is not changed
            # 9. update vm with displayvm=false, resource count of cpu/ram is not changed
            # 10. destroy vm with expunge = true, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="true" )

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])
        self.cleanup.append(self.service_offering)

        # deploy vm
        try:
            virtual_machine_4 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id,
                mode=self.zone.networktype
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCpu = self.expectedCpu + virtual_machine_4.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_4.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # stop vm
        virtual_machine_4.stop(self.apiclient)
        self.expectedCpu = self.expectedCpu - virtual_machine_4.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_4.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # start vm
        virtual_machine_4.start(self.apiclient)
        self.expectedCpu = self.expectedCpu + virtual_machine_4.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_4.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # reboot vm
        virtual_machine_4.reboot(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # destroy vm
        virtual_machine_4.delete(self.apiclient, expunge=False)
        self.expectedCpu = self.expectedCpu - virtual_machine_4.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_4.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # recover vm
        virtual_machine_4.recover(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # update vm with displayvm=false
        virtual_machine_4.update(self.apiclient, displayvm=False)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # expunge vm
        virtual_machine_4.delete(self.apiclient, expunge=True)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_05_resource_count_vm_with_dynamic_offering_in_running_state_failed_cases(self):
        """Create VM with dynamic service offering. Take resources of vm in running state into calculation of resource count. Test failed cases

            Steps:
            # 1. update resource.count.running.vms.only to true
            # 2. create dynamic service offering
            # 3. update account cpu/ram limitation to current value
            # 4. deploy vm (startvm=false), resource count of cpu/ram is not changed
            # 5. start vm, it should fail
            # 6. increase cpu limitation, start vm, it should fail
            # 7. increase memory limitation, start vm, it should succeed. resource count of cpu/ram increases
            # 8. restore vm, it should succeed. resource count of cpu/ram is not changed
            # 9. destroy vm, resource count of cpu/ram decreases
            # 10. expunge vm, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="true" )

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])
        self.cleanup.append(self.service_offering)

        # update resource limitation
        self.update_account_resource_limitation(self.expectedCpu, self.expectedMemory)

        # deploy vm (startvm=false)
        try:
            virtual_machine_5 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id,
                startvm=False,
                mode=self.zone.networktype
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # start vm
        try:
            virtual_machine_5.start(self.apiclient)
            self.fail("Start VM should fail as there is not enough cpu")
        except Exception:
            self.debug("Start VM failed as expected")

        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # increase cpu limitation, and start vm
        self.update_account_resource_limitation(self.expectedCpu + virtual_machine_5.cpunumber, self.expectedMemory)
        try:
            virtual_machine_5.start(self.apiclient)
            self.fail("Start VM should fail as there is not enough memory")
        except Exception:
            self.debug("Start VM failed as expected")

        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # increase memory limitation, and start vm
        self.update_account_resource_limitation(self.expectedCpu + virtual_machine_5.cpunumber, self.expectedMemory + virtual_machine_5.memory)
        try:
            virtual_machine_5.start(self.apiclient)
            self.debug("Start VM succeed as expected")
        except Exception:
            self.fail("Start VM should succeed as there is enough cpu and memory")

        self.expectedCpu = self.expectedCpu + virtual_machine_5.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_5.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # restore running vm
        virtual_machine_5.restore(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # expunge vm
        virtual_machine_5.delete(self.apiclient, expunge=True)
        self.expectedCpu = self.expectedCpu - virtual_machine_5.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_5.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_06_resource_count_vm_with_dynamic_offering_in_all_states_failed_cases(self):
        """Create VM with dynamic service offering. Take resources of vm in all states into calculation of resource count. Test failed cases

            Steps:
            # 1. update resource.count.running.vms.only to false
            # 2. create dynamic service offering
            # 3. update account cpu/ram limitation to current value
            # 4. deploy vm (startvm=false), it should fail
            # 5. increase cpu limitation, deploy vm, it should fail
            # 6. increase memory limitation, deploy vm, it should succeed. resource count of cpu/ram increases
            # 7. start vm, resource count of cpu/ram is not changed
            # 8. restore vm, it should succeed. resource count of cpu/ram is not changed
            # 9. destroy vm, resource count of cpu/ram decreases
            # 10. expunge vm, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="false" )

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])
        self.cleanup.append(self.service_offering)

        # update resource limitation
        self.update_account_resource_limitation(self.expectedCpu, self.expectedMemory)

        # deploy vm (startvm=false)
        try:
            virtual_machine_6 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id,
                startvm=False,
                mode=self.zone.networktype
            )
            self.fail("Deploy VM should fail as there is not enough cpu")
        except Exception as e:
            self.debug("Deploy VM failed as expected")

        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # increase cpu limitation, and deploy vm
        self.update_account_resource_limitation(self.expectedCpu + 1, self.expectedMemory)
        try:
            virtual_machine_6 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id,
                startvm=False,
                mode=self.zone.networktype
            )
            self.fail("Deploy VM should fail as there is not enough memory")
        except Exception as e:
            self.debug("Deploy VM failed as expected")

        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # increase memory limitation, and deploy vm
        self.update_account_resource_limitation(self.expectedCpu + 1, self.expectedMemory + 256)
        try:
            virtual_machine_6 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id,
                startvm=False,
                mode=self.zone.networktype
            )
            self.debug("Deploy VM succeed as expected")
        except Exception:
            self.fail("Deploy VM should succeed as there is enough cpu and memory")

        self.expectedCpu = self.expectedCpu + virtual_machine_6.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_6.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # start vm
        virtual_machine_6.start(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # restore running vm
        virtual_machine_6.restore(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # expunge vm
        virtual_machine_6.delete(self.apiclient, expunge=True)
        self.expectedCpu = self.expectedCpu - virtual_machine_6.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_6.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_07_resource_count_vm_in_running_state_and_move_and_upgrade(self):
        """Create VM with dynamic service offering. Take resources of vm in running state into calculation. Move vm to another account and upgrade it.

            Steps:
            # 1. update resource.count.running.vms.only to true
            # 2. create dynamic service offering
            # 3. deploy vm, resource count of cpu/ram increases
            # 4. stop vm, resource count of cpu/ram decreases
            # 5. create another account
            # 6. move vm to new account. resource count of cpu/ram of current account is not changed. resource count of cpu/ram of new account is not changed.
            # 7. create another service offering.
            # 8. upgrade vm. resource count of cpu/ram of new account is not changed.
            # 9. start vm, resource count of cpu/ram of new account increases with cpu/ram of new service offering.
            # 10. destroy vm, resource count of cpu/ram decreases
            # 11. expunge vm, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="true" )

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])
        self.cleanup.append(self.service_offering)

        # deploy vm
        try:
            virtual_machine_7 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCpu = self.expectedCpu + virtual_machine_7.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_7.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # stop vm
        virtual_machine_7.stop(self.apiclient)
        self.expectedCpu = self.expectedCpu - virtual_machine_7.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_7.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # create another account
        self.account2 = Account.create(
            self.apiclient,
            self.services["account2"],
            admin=True,
            domainid=self.domain.id
        )
        accounts = Account.list(self.apiclient, id=self.account2.id)
        self.account2Cpu = int(accounts[0].cputotal)
        self.account2Memory = int(accounts[0].memorytotal)

        # move vm to new account. resource count of cpu/ram of current account is not changed. resource count of cpu/ram of new account is not changed.
        oldcpunumber = virtual_machine_7.cpunumber
        oldmemory = virtual_machine_7.memory
        virtual_machine_7.assign_virtual_machine(self.apiclient, self.account2.name, self.account2.domainid)

        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

        # create another service offering
        self.service_offering_big = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"]
        )
        self.cleanup.append(self.service_offering_big)

        # upgrade vm
        virtual_machine_7.change_service_offering(self.apiclient, self.service_offering_big.id)
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

        # start vm, resource count of cpu/ram of new account increases with cpu/ram of new service offering.
        virtual_machine_7.start(self.apiclient)
        self.account2Cpu = self.account2Cpu + self.service_offering_big.cpunumber
        self.account2Memory = self.account2Memory + self.service_offering_big.memory
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

        # expunge vm
        virtual_machine_7.delete(self.apiclient, expunge=True)
        self.account2Cpu = self.account2Cpu - self.service_offering_big.cpunumber
        self.account2Memory = self.account2Memory - self.service_offering_big.memory
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_08_resource_count_vm_in_all_states_and_move_and_upgrade(self):
        """Create VM with dynamic service offering. Take resources of vm in all states into calculation. Move vm to another account and upgrade it.

            Steps:
            # 1. update resource.count.running.vms.only to true
            # 2. create dynamic service offering
            # 3. deploy vm, resource count of cpu/ram increases
            # 4. stop vm, resource count of cpu/ram is not changed
            # 5. create another account
            # 6. move vm to new account. resource count of cpu/ram of current account decreases. resource count of cpu/ram of new account increases.
            # 7. create another service offering.
            # 8. upgrade vm. resource count of cpu/ram of new account is changed.
            # 9. start vm, resource count of cpu/ram is not changed
            # 10. destroy vm, resource count of cpu/ram decreases
            # 11. expunge vm, resource count of cpu/ram is not changed
        """
        Configurations.update(self.apiclient,
                name="resource.count.running.vms.only",
                value="false" )

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])
        self.cleanup.append(self.service_offering)

        # deploy vm
        try:
            virtual_machine_8 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                customcpunumber=1,
                customcpuspeed=100,
                custommemory=256,
                templateid=self.template.id,
                zoneid=self.zone.id
            )
        except Exception as e:
            self.fail("Exception while deploying virtual machine: %s" % e)

        self.expectedCpu = self.expectedCpu + virtual_machine_8.cpunumber
        self.expectedMemory = self.expectedMemory + virtual_machine_8.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # stop vm
        virtual_machine_8.stop(self.apiclient)
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        # create another account
        self.account2 = Account.create(
            self.apiclient,
            self.services["account2"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(self.account2)
        accounts = Account.list(self.apiclient, id=self.account2.id)
        self.account2Cpu = int(accounts[0].cputotal)
        self.account2Memory = int(accounts[0].memorytotal)

        # move vm to new account. resource count of cpu/ram of current account decreases. resource count of cpu/ram of new account increases.
        oldcpunumber = virtual_machine_8.cpunumber
        oldmemory = virtual_machine_8.memory
        virtual_machine_8.assign_virtual_machine(self.apiclient, self.account2.name, self.account2.domainid)

        self.expectedCpu = self.expectedCpu - virtual_machine_8.cpunumber
        self.expectedMemory = self.expectedMemory - virtual_machine_8.memory
        self.verify_resource_count_cpu_memory(self.expectedCpu, self.expectedMemory);

        self.account2Cpu = self.account2Cpu + virtual_machine_8.cpunumber
        self.account2Memory = self.account2Memory + virtual_machine_8.memory
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

        # create another service offering
        self.service_offering_big = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["big"]
        )
        self.cleanup.append(self.service_offering_big)

        # upgrade vm
        virtual_machine_8.change_service_offering(self.apiclient, self.service_offering_big.id)
        self.account2Cpu = self.account2Cpu + self.service_offering_big.cpunumber - oldcpunumber
        self.account2Memory = self.account2Memory + self.service_offering_big.memory - oldmemory
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

        # start vm
        virtual_machine_8.start(self.apiclient)
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);

        # expunge vm
        virtual_machine_8.delete(self.apiclient, expunge=True)
        self.account2Cpu = self.account2Cpu - self.service_offering_big.cpunumber
        self.account2Memory = self.account2Memory - self.service_offering_big.memory
        self.verify_resource_count_cpu_memory(self.account2Cpu, self.account2Memory, account=self.account2);
