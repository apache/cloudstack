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

""" Tests for Dynamic Compute Offering Feature

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK
    /Dynamic+ComputeOffering

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-6147

    Feature Specifications: https://cwiki.apache.org/confluence/display/
    CLOUDSTACK/Dynamic+Compute+Offering+FS
"""
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              random_gen,
                              get_hypervisor_type)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Resources,
                             AffinityGroup,
                             Host)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               verifyComputeOfferingCreation)

from nose.plugins.attrib import attr
from marvin.codes import PASS, ADMIN_ACCOUNT, USER_ACCOUNT, FAILED
from ddt import ddt, data


@ddt
class TestDynamicServiceOffering(cloudstackTestCase):

    """Test Dynamic Service Offerings
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDynamicServiceOffering, cls).getClsTestClient()
        cls.api_client = cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("dynamic scaling feature is not supported on %s" % cls.hypervisor.lower())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup_co = []
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup)

            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup_co)

            self.cleanup_co[:] = []
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced"])
    def test_create_normal_compute_offering(self):
        """ Create normal compute offering with non zero values for cpu,
            cpu number and memory"""

        # Steps:
        # 1. Create normal compute offering with non zero values
        # for cpu number, cpu speed, memory

        # Validations:
        # 1. Compute offering should be created

        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = 128

        serviceOffering = ServiceOffering.create(
            self.api_client,
            self.services["service_offering"])
        self.assertEqual(
            verifyComputeOfferingCreation(
                self.apiclient,
                serviceOffering.id),
            PASS,
            "Compute Offering verification failed")

        self.cleanup_co.append(serviceOffering)
        return

    @attr(tags=["basic", "advanced"])
    def test_create_dynamic_compute_offering(self):
        """ Create dynamic compute offering with cpunumber, cpuspeed and memory
            not specified"""

        # Steps:
        # 1. Create dynamic compute offering with values for cpu number,
        #    cpu speed, memory not specified

        # Validations:
        # 1. Compute offering should be created

        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering = ServiceOffering.create(
            self.api_client,
            self.services["service_offering"])
        self.assertEqual(
            verifyComputeOfferingCreation(
                self.apiclient,
                serviceOffering.id),
            PASS,
            "Compute Offering verification failed")

        self.cleanup_co.append(serviceOffering)
        return

    @attr(tags=["basic", "advanced"])
    def test_create_dynamic_compute_offering_no_cpunumber(self):
        """ Create dynamic compute offering with only cpunumber unspecified"""

        # Validations:
        # 1. Compute Offering creation should fail

        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = 128

        try:
            serviceOffering = ServiceOffering.create(
                self.api_client,
                self.services["service_offering"])
            self.cleanup_co.append(serviceOffering)
            self.fail(
                "Compute Offering creation succeded, it should have failed")
        except Exception:
            self.debug("Compute Offering Creation failed as expected")
        return

    @attr(tags=["basic", "advanced"])
    def test_create_dynamic_compute_offering_no_cpuspeed(self):
        """ Create dynamic compute offering with only cpuspeed unspecified"""

        # Validations:
        # 1. Compute offering creation should fail

        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = 128

        try:
            serviceOffering = ServiceOffering.create(
                self.api_client,
                self.services["service_offering"])
            self.cleanup_co.append(serviceOffering)
            self.fail(
                "Compute Offering creation succeded, it should have failed")
        except Exception:
            self.debug("Compute Offering Creation failed as expected")
        return

    @attr(tags=["basic", "advanced"])
    def test_create_dynamic_compute_offering_no_memory(self):
        """ Create dynamic compute offering with only memory unspecified"""

        # Validations:
        # 1. Compute offering creation should fail

        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = ""

        try:
            serviceOffering = ServiceOffering.create(
                self.api_client,
                self.services["service_offering"])
            self.cleanup_co.append(serviceOffering)
            self.fail(
                "Compute Offering creation succeded, it should have failed")
        except Exception:
            self.debug("Compute Offering Creation failed as expected")
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_deploy_virtual_machines_static_offering(self, value):
        """Test deploy VM with static offering"""

        # Steps:
        # 1. Create admin/user account and create its user api client
        # 2. Create a static compute offering
        # 3. Deploy a VM with account api client and static service offering
        # 4. Repeat step 3 but also pass custom values for cpu number,
        #    cpu speed and memory while deploying VM

        # Validations:
        # 1. Step 3 should succeed
        # 2. Step 4 should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create Account
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create service offering
        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = 128

        serviceOffering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering)

        # Deploy VM with static service offering
        try:
            VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                accountid=self.account.name,
                domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Deploy VM with static service offering, also with custom values
        try:
            VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=256,
                accountid=self.account.name,
                domainid=self.account.domainid)
            self.fail("VM creation should have failed, it succeeded")
        except Exception as e:
            self.debug("vm creation failed as expected: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_deploy_virtual_machines_dynamic_offering(self, value):
        """Test deploy VM with dynamic compute offering"""

        # Steps:
        # 1. Create admin/user account and create its user api client
        # 2. Create a dynamic service offering
        # 3. Deploy a VM with account api client and dynamic service offering
        #    without providing custom values for cpu number, cpu speed and
        #    memory
        # 4. Deploy a VM with account api client and dynamic service offering
        #    providing custom values for cpu number, cpu speed and memory
        # 5. Deploy a VM with account api client and dynamic service offering
        #    providing custom values only for cpu number

        # Validations:
        # 1. Step 3 should fail
        # 2. Step 4 should succeed
        # 3. Step 5 should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create Account and its api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering)

        # Deploy VM with dynamic compute offering without providing
        # custom values for
        # cpu number, cpu speed and memory
        try:
            VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                accountid=self.account.name,
                domainid=self.account.domainid)
            self.fail("VM creation succeded, it should have failed")
        except Exception as e:
            self.debug("vm creation failed as expected with error: %s" % e)

        # Deploy VM with dynamic compute offering providing custom values for
        # cpu number, cpu speed and memory
        try:
            VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128,
                accountid=self.account.name,
                domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Deploy VM with dynamic compute offering providing custom values
        # for only cpu number
        try:
            VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=2,
                accountid=self.account.name,
                domainid=self.account.domainid)
            self.fail("VM deployment should have failed, it succeded")
        except Exception as e:
            self.debug("vm creation failed as expected: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_check_vm_stats(self, value):
        """Deploy VM with dynamic service offering and check VM stats"""

        # Steps:
        # 1. Create admin/user account and create its user api client
        # 2. Create a dynamic service offering
        # 3. Deploy a VM with account api client and dynamic service offering
        #    providing custom values for cpu number, cpu speed and memory
        # 4. List the VM and verify the dynamic parameters are same as passed

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create Account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create dynamic compute offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering)

        # Custom values
        customcpunumber = 2
        customcpuspeed = 256
        custommemory = 128

        # Deploy VM with dynamic service offering and the custom values
        try:
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=customcpunumber,
                customcpuspeed=customcpuspeed,
                custommemory=custommemory,
                accountid=self.account.name,
                domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        vmlist = VirtualMachine.list(self.apiclient, id=virtualMachine.id)
        self.assertEqual(
            validateList(vmlist)[0],
            PASS,
            "vm list validation failed")
        vm = vmlist[0]

        # Verify the custom values
        self.assertEqual(str(vm.cpunumber), str(customcpunumber), "vm cpu number %s\
                 not matching with provided custom cpu number %s" %
                         (vm.cpunumber, customcpunumber))

        self.assertEqual(str(vm.cpuspeed), str(customcpuspeed), "vm cpu speed %s\
                 not matching with provided custom cpu speed %s" %
                         (vm.cpuspeed, customcpuspeed))

        self.assertEqual(str(vm.memory), str(custommemory), "vm memory %s\
                 not matching with provided custom memory %s" %
                         (vm.memory, custommemory))
        return


@ddt
class TestScaleVmDynamicServiceOffering(cloudstackTestCase):

    """Test scaling VMs with dynamic Service Offerings
    """

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(
            TestScaleVmDynamicServiceOffering,
            cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())
        cls.mode = str(cls.zone.networktype).lower()
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls._cleanup = []
        cls.serviceOffering_static_1 = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"])
        cls._cleanup.append(cls.serviceOffering_static_1)

        if cls.hypervisor.lower() == "vmware":
            virtual_machine = VirtualMachine.create(
                cls.api_client,
                cls.services["virtual_machine"],
                serviceofferingid=cls.serviceOffering_static_1.id,
                mode=cls.zone.networktype)
            cls._cleanup.append(virtual_machine)
            sshClient = virtual_machine.get_ssh_client()
            result = str(
                sshClient.execute("service vmware-tools status")).lower()
            if "running" not in result:
                cls.tearDownClass()
                raise unittest.SkipTest("Skipping scale VM operation because\
                    VMware tools are not installed on the VM")
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup_co = []
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup)

            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup_co)

            self.cleanup_co[:] = []
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_change_so_stopped_vm_static_to_static(self, value):
        """Test scale stopped VM from static offering to static offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make two static service offerings
        # 3. Deploy VM with one static offering
        # 4. Stop the VM
        # 5. Scale VM with 2nd static service offering

        # Validations:
        # 1. Scaling operation should be successful

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        try:
            # Create Account
            self.account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=self.domain.id,
                admin=isadmin)
            self.cleanup.append(self.account)
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)

            # Create static service offerings (Second offering should have
            # one of the custom values greater than 1st one, scaling down is
            # not allowed
            self.services["service_offering"]["cpunumber"] = "2"
            self.services["service_offering"]["cpuspeed"] = "256"
            self.services["service_offering"]["memory"] = "128"

            serviceOffering_static_1 = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.services["service_offering"]["cpunumber"] = "4"

            serviceOffering_static_2 = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.cleanup_co.append(serviceOffering_static_1)
            self.cleanup_co.append(serviceOffering_static_2)

            # Deploy VM
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_static_1.id,
                accountid=self.account.name,
                domainid=self.account.domainid)

            # Stop VM
            virtualMachine.stop(apiclient)

            # Scale VM to new static service offering
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_static_2.id)
        except Exception as e:
            self.fail("Exception occured: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_change_so_stopped_vm_static_to_dynamic(self, value):
        """Test scale stopped VM from static offering to dynamic offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make static and dynamic service offerings
        # 3. Deploy VM with static offering
        # 4. Stop the VM
        # 5. Scale VM with dynamic service offering providing all required
        #    custom values
        # 6. Deploy another VM with static offring and stop the VM
        # 7. Scale VM with dynamic service offering providing only custom cpu
        # number

        # Validations:
        # 1. Scale operation in step 5 should be successful
        # 2. Scale operation in step 7 should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        try:
            # Create Account and api client
            self.account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=self.domain.id,
                admin=isadmin)
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)
            self.cleanup.append(self.account)

            # Create static and dynamic service offerings
            self.services["service_offering"]["cpunumber"] = "2"
            self.services["service_offering"]["cpuspeed"] = "256"
            self.services["service_offering"]["memory"] = "128"

            serviceOffering_static = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.services["service_offering"]["cpunumber"] = ""
            self.services["service_offering"]["cpuspeed"] = ""
            self.services["service_offering"]["memory"] = ""

            serviceOffering_dynamic = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.cleanup_co.append(serviceOffering_static)
            self.cleanup_co.append(serviceOffering_dynamic)

            # Deploy VM with static service offering
            virtualMachine_1 = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_static.id,
                accountid=self.account.name,
                domainid=self.account.domainid)

            # Stop VM
            virtualMachine_1.stop(apiclient)

            # Scale VM to dynamic service offering proving all custom values
            virtualMachine_1.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic.id,
                customcpunumber=4,
                customcpuspeed=256,
                custommemory=128)

            # Deploy VM with static service offering
            virtualMachine_2 = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_static.id,
                accountid=self.account.name,
                domainid=self.account.domainid)

            # Stop VM
            virtualMachine_2.stop(apiclient)
        except Exception as e:
            self.fail("Exception occuered: %s" % e)

            # Scale VM to dynamic service offering proving only custom cpu
            # number
        with self.assertRaises(Exception):
            virtualMachine_2.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic.id,
                customcpunumber=4)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_change_so_stopped_vm_dynamic_to_static(self, value):
        """Test scale stopped VM from dynamic offering to static offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make static and dynamic service offerings
        # 3. Deploy VM with dynamic service offering
        # 4. Stop the VM
        # 5. Scale VM with static service offering

        # Validations:
        # 1. Scale operation in step 5 should be successful

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        try:
            # Create account and api client
            self.account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=self.domain.id,
                admin=isadmin)
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)
            self.cleanup.append(self.account)

            # Create dynamic and static service offering
            self.services["service_offering"]["cpunumber"] = ""
            self.services["service_offering"]["cpuspeed"] = ""
            self.services["service_offering"]["memory"] = ""

            serviceOffering_dynamic = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.services["service_offering"]["cpunumber"] = "4"
            self.services["service_offering"]["cpuspeed"] = "256"
            self.services["service_offering"]["memory"] = "128"

            serviceOffering_static = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.cleanup_co.append(serviceOffering_static)
            self.cleanup_co.append(serviceOffering_dynamic)

            # Deploy VM with dynamic service offering
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128)

            # Stop VM and verify that it is in stopped state
            virtualMachine.stop(apiclient)

            # Scale VM to static service offering
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_static.id)
        except Exception as e:
            self.fail("Exception occured: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_change_so_stopped_vm_dynamic_to_dynamic(self, value):
        """Test scale stopped VM from dynamic offering to dynamic offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make 2 dynamic service offerings
        # 3. Deploy VM with dynamic service offering
        # 4. Stop the VM
        # 5. Scale VM with same dynamic service offering
        # 6. Scale VM with other dynamic service offering
        # 7. Scale VM with same/other dynamic offering but providing custom
        #    value for only cpu number

        # Validations:
        # 1. Scale operation in step 5 should be successful
        # 2. Scale operation in step 6 should be successful
        # 3. Scale operation in step 7 should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        try:
            # Create Account
            self.account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=self.domain.id,
                admin=isadmin)
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)
            self.cleanup.append(self.account)

            # Create dynamic service offerings
            self.services["service_offering"]["cpunumber"] = ""
            self.services["service_offering"]["cpuspeed"] = ""
            self.services["service_offering"]["memory"] = ""

            serviceOffering_dynamic_1 = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            serviceOffering_dynamic_2 = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"])

            self.cleanup_co.append(serviceOffering_dynamic_1)
            self.cleanup_co.append(serviceOffering_dynamic_2)

            # Deploy VM with dynamic service offering
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic_1.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128)

            # Stop VM
            virtualMachine.stop(apiclient)

            # Scale VM with same dynamic service offering
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic_1.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=256)

            # Scale VM with other dynamic service offering
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic_2.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=256)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        # Scale VM with dynamic service offering proving custom value
        # only for cpu number
        with self.assertRaises(Exception):
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic_1.id,
                customcpunumber=4)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"],required_hardware="true")
    def test_change_so_running_vm_static_to_static(self, value):
        """Test scale running VM from static offering to static offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make two static service offerings
        # 3. Deploy VM with one static offering
        # 4. Scale VM with 2nd static service offering

        # Validations:
        # 1. Scaling operation should be successful

        hypervisor = get_hypervisor_type(self.apiclient)
        if hypervisor.lower() in ["kvm", "hyperv", 'lxc']:
            self.skipTest(
                "Scaling VM in running state is not supported on %s" % hypervisor)

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create Account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create static service offerings
        self.services["service_offering"]["cpunumber"] = "2"
        self.services["service_offering"]["cpuspeed"] = "256"
        self.services["service_offering"]["memory"] = "128"

        serviceOffering_static_1 = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.services["service_offering"]["cpunumber"] = "4"

        serviceOffering_static_2 = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_static_1)
        self.cleanup_co.append(serviceOffering_static_2)

        # Deploy VM with static service offering
        try:
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_static_1.id,
                accountid=self.account.name,
                domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Scale VM to other static service offering
        try:
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_static_2.id)
        except Exception as e:
            self.fail("Failure while changing service offering: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"],required_hardware="true")
    def test_change_so_running_vm_static_to_dynamic(self, value):
        """Test scale running VM from static offering to dynamic offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make static and dynamic service offerings
        # 3. Deploy VM with static offering
        # 4. Scale VM with dynamic service offering providing all required
        #    custom values
        # 5. Deploy another VM with static offring
        # 6. Scale VM with dynamic service offering providing only custom cpu
        # number

        # Validations:
        # 1. Scale operation in step 4 should be successful
        # 2. Scale operation in step 6 should fail

        hypervisor = get_hypervisor_type(self.apiclient)
        if hypervisor.lower() in ["kvm", "hyperv", 'lxc']:
            self.skipTest(
                "Scaling VM in running state is not supported on %s" % hypervisor)

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Crate account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create static and dynamic service offerings
        self.services["service_offering"]["cpunumber"] = "2"
        self.services["service_offering"]["cpuspeed"] = "256"
        self.services["service_offering"]["memory"] = "128"

        serviceOffering_static = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_static)
        self.cleanup_co.append(serviceOffering_dynamic)

        # Deploy VM with static service offering
        try:
            virtualMachine_1 = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_static.id,
                accountid=self.account.name,
                domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Scale VM to dynamic service offering
        try:
            virtualMachine_1.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic.id,
                customcpunumber=4,
                customcpuspeed=256,
                custommemory=128)
        except Exception as e:
            self.fail("Failure while changing service offering: %s" % e)

        try:
            virtualMachine_2 = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_static.id,
                accountid=self.account.name,
                domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        try:
            virtualMachine_2.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic.id,
                customcpunumber=4)
            self.fail(
                "Changing service offering with incomplete data should\
                 have failed, it succeded")
        except Exception as e:
            self.debug(
                "Failure while changing service offering as expected: %s" %
                e)

        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"],required_hardware="true")
    def test_change_so_running_vm_dynamic_to_static(self, value):
        """Test scale running VM from dynamic offering to static offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make static and dynamic service offerings
        # 3. Deploy VM with dynamic service offering
        # 4. Scale VM with static service offering

        # Validations:
        # 1. Scale operation in step 4 should be successful
        hypervisor = get_hypervisor_type(self.apiclient)
        if hypervisor.lower() in ["kvm", "hyperv", 'lxc']:
            self.skipTest(
                "Scaling VM in running state is not supported on %s" % hypervisor)

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create dynamic and static service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.services["service_offering"]["cpunumber"] = "4"
        self.services["service_offering"]["cpuspeed"] = "256"
        self.services["service_offering"]["memory"] = "128"

        serviceOffering_static = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_static)
        self.cleanup_co.append(serviceOffering_dynamic)

        # deploy VM with dynamic service offering
        try:
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Scale VM to static service offering
        try:
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_static.id)
        except Exception as e:
            self.fail("Failure while changing service offering: %s" % e)

        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"],required_hardware="true")
    def test_change_so_running_vm_dynamic_to_dynamic(self, value):
        """Test scale running VM from dynamic offering to dynamic offering"""

        # Steps:
        # 1. Create Account (admin/user) and its api client
        # 2. Make 2 dynamic service offerings
        # 3. Deploy VM with dynamic service offering
        # 4. Scale VM with same dynamic service offering
        # 5. Scale VM with other dynamic service offering
        # 6. Scale VM with same/other dynamic offering but providing custom
        #    value for only cpu number

        # Validations:
        # 1. Scale operation in step 4 should be successful
        # 2. Scale operation in step 5 should be successful
        # 3. Scale operation in step 6 should fail

        hypervisor = get_hypervisor_type(self.apiclient)
        if hypervisor.lower() in ["kvm", "hyperv", "lxc"]:
            self.skipTest(
                "Scaling VM in running state is not supported on %s" % hypervisor)

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create dynamic service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic_1 = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        serviceOffering_dynamic_2 = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_dynamic_1)
        self.cleanup_co.append(serviceOffering_dynamic_2)

        # Deploy VM with dynamic service offering
        try:
            virtualMachine = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic_1.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Scale VM with same dynamic offering
        try:
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic_1.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=256)
        except Exception as e:
            self.fail("Failure while changing service offering: %s" % e)

        # Scale VM with other dynamic service offering
        try:
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic_2.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=512)
        except Exception as e:
            self.fail("Failure while changing service offering: %s" % e)

        # Scale VM with dynamic offering proving custom value only for cpu
        # number
        try:
            virtualMachine.scale(
                apiclient,
                serviceOfferingId=serviceOffering_dynamic_1.id,
                customcpunumber=4)
            self.fail(
                "Changing service offering should have failed, it succeded")
        except Exception as e:
            self.debug("Failure while changing service offering: %s" % e)

        return


@ddt
class TestAccountLimits(cloudstackTestCase):

    """Test max limit of account (cpunumber and memory) with dynamic
       compute offering
    """

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestAccountLimits, cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("dynamic scaling feature is not supported on %s" % cls.hypervisor.lower())

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())
        cls.mode = str(cls.zone.networktype).lower()
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup_co = []
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup)

            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup_co)

            self.cleanup_co[:] = []
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_max_account_cpus_deploy_VM(self, value):
        """Test cpu limits of account while deploying VM with dynamic
           compute offering"""

        # Steps:
        # 1. Create Account (admin/user)
        # 2. Update max cpu limit of account to 2
        # 3. Create dynamic service offering
        # 4. Deploy VM with dynamic service offering and cpu number 3

        # Validations:
        # 1. VM creation should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        self.cleanup.append(self.account)

        Resources.updateLimit(self.apiclient,
                              resourcetype=8,
                              max=2,
                              account=self.account.name,
                              domainid=self.account.domainid)

        # Create dynamic service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_dynamic)

        # Deploy VM with dynamic service offering
        try:
            VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=3,
                customcpuspeed=256,
                custommemory=128)
            self.fail("vm creation should have failed, it succeeded")
        except Exception as e:
            self.debug("vm creation failed as expected with error: %s" % e)

        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_max_account_cpus_scale_VM(self, value):
        """Test cpu limits of account while scaling VM with dynamic
           compute offering"""

        # Steps:
        # 1. Create Account (admin/user)
        # 2. Update max cpu limit of account to 2
        # 3. Create dynamic service offering
        # 4. Deploy VM with dynamic service offering and cpu number 2
        # 5. Try to Scale VM with dynamic service offering and cpu number 3

        # Validations:
        # 1. VM creation should succeed
        # 2. VM scaling operation should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        self.cleanup.append(self.account)

        Resources.updateLimit(self.apiclient,
                              resourcetype=8,
                              max=2,
                              account=self.account.name,
                              domainid=self.account.domainid)

        # Create dynamic service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_dynamic)

        # Deploy VM with dynamic service offering
        try:
            virtualMachine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Scale VM with same dynamic offering
        try:
            virtualMachine.scale(
                self.apiclient,
                serviceOfferingId=serviceOffering_dynamic.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=256)
            self.fail("Scaling virtual machine with cpu number more than \
                    allowed limit (of account) succeded, should have failed")
        except Exception as e:
            self.debug(
                "Failure while changing service offering as expected: %s" %
                e)

        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_max_account_memory_deploy_VM(self, value):
        """Test memory limits of account while deploying VM with dynamic
           compute offering"""

        # Steps:
        # 1. Create Account (admin/user)
        # 2. Update max memory limit of account to 256
        # 3. Create dynamic service offering
        # 4. Deploy VM with dynamic service offering and memory 512

        # Validations:
        # 1. VM creation should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        self.cleanup.append(self.account)

        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=256,
                              account=self.account.name,
                              domainid=self.account.domainid)

        # Create dynamic service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_dynamic)

        # Deploy VM with dynamic service offering
        try:
            VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=3,
                customcpuspeed=256,
                custommemory=512)
            self.fail("vm creation should have failed, it succeeded")
        except Exception as e:
            self.debug("vm creation failed as expected with error: %s" % e)

        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"])
    def test_max_account_memory_scale_VM(self, value):
        """Test memory limits of account while scaling VM with
           dynamic compute offering"""

        # Steps:
        # 1. Create Account (admin/user)
        # 2. Update max memory limit of account to 256
        # 3. Create dynamic service offering
        # 4. Deploy VM with dynamic service offering and memory 256
        # 5. Try to Scale VM with dynamic service offering and memory 512

        # Validations:
        # 1. VM creation should succeed
        # 2. VM scaling operation should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        self.cleanup.append(self.account)

        Resources.updateLimit(self.apiclient,
                              resourcetype=9,
                              max=256,
                              account=self.account.name,
                              domainid=self.account.domainid)

        # Create dynamic service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_dynamic)

        # Deploy VM with dynamic service offering
        try:
            virtualMachine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=256)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Scale VM with same dynamic offering
        try:
            virtualMachine.scale(
                self.apiclient,
                serviceOfferingId=serviceOffering_dynamic.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=512)
            self.fail("Scaling virtual machine with cpu number more than \
                    allowed limit (of account) succeded, should have failed")
        except Exception as e:
            self.debug(
                "Failure while changing service offering as expected: %s" %
                e)

        return


@ddt
class TestAffinityGroup(cloudstackTestCase):

    """Test affinity group working with VMs created with dynamic offering
    """

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestAffinityGroup, cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("dynamic scaling feature is not supported on %s" % cls.hypervisor.lower())

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())
        cls.mode = str(cls.zone.networktype).lower()
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup_co = []
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup)

            # Clean up compute offerings
            cleanup_resources(self.apiclient, self.cleanup_co)

            self.cleanup_co[:] = []
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic", "advanced"], BugId="7180", required_hardware="true")
    def test_deploy_VM_with_affinity_group(self, value):
        """Test deploy VMs with affinity group and dynamic compute offering"""

        # Steps:
        # 1. Create Account (admin/user)
        # 2. Update max cpu limit of account to 2
        # 3. Create dynamic service offering
        # 4. Deploy VM with dynamic service offering and cpu number 3

        # Validations:
        # 1. VM creation should fail

        isadmin = True
        if value == USER_ACCOUNT:
            isadmin = False

        # Create account and api client
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id,
            admin=isadmin)
        self.cleanup.append(self.account)

        # Create dynamic service offerings
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"])

        self.cleanup_co.append(serviceOffering_dynamic)

        self.services["host_anti_affinity"][
            "name"] = "aff_grp_" + random_gen(size=6)
        affinityGroup = AffinityGroup.create(
            self.apiclient,
            self.services["host_anti_affinity"],
            self.account.name,
            self.domain.id)

        # Deploy VM with dynamic service offering
        try:
            virtualMachine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                serviceofferingid=serviceOffering_dynamic.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128,
                affinitygroupnames=[
                    affinityGroup.name])
        except Exception as e:
            self.fail("vm creation failed with error: %s" % e)

        otherHostsInCluster = Host.list(
            self.apiclient,
            virtualmachineid=virtualMachine.id)
        if validateList(otherHostsInCluster)[0] == PASS:
            try:
                VirtualMachine.create(
                    self.apiclient,
                    self.services["virtual_machine"],
                    serviceofferingid=serviceOffering_dynamic.id,
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    customcpunumber=2,
                    customcpuspeed=256,
                    custommemory=128,
                    affinitygroupnames=[
                        affinityGroup.name])
            except Exception as e:
                self.fail("vm creation failed with error: %s" % e)

        else:
            try:
                VirtualMachine.create(
                    self.apiclient,
                    self.services["virtual_machine"],
                    serviceofferingid=serviceOffering_dynamic.id,
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    customcpunumber=2,
                    customcpuspeed=256,
                    custommemory=128,
                    affinitygroupnames=[
                        affinityGroup.name])
                self.fail("vm creation should have failed, it succeded")
            except Exception as e:
                self.debug("vm creation failed as expected with error: %s" % e)

        return
