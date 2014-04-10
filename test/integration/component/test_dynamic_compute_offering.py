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

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Dynamic+ComputeOffering

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-6147

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Dynamic+Compute+Offering+FS
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.codes import PASS, ADMIN_ACCOUNT, USER_ACCOUNT
from ddt import ddt, data

@ddt
class TestDynamicServiceOffering(cloudstackTestCase):
    """Test Dynamic Service Offerings
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDynamicServiceOffering, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]
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

    @attr(tags=["basic","advanced"])
    def test_create_normal_compute_offering(self):
        """ Create normal compute offering with non zero values for cpu,
            cpu number and memory"""

        # Steps:
        # 1. Create normal compute offering with non zero values for cpu number,
        #    cpu speed, memory

        # Validations:
        # 1. Compute offering should be created

        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = 128

        serviceOffering = ServiceOffering.create(self.api_client,
                                                 self.services["service_offering"]
                                                 )
        self.assertEqual(verifyComputeOfferingCreation(self.apiclient, serviceOffering.id),
                         PASS, "Compute Offering verification failed")

        self.cleanup_co.append(serviceOffering)
        return

    @attr(tags=["basic","advanced"])
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

        serviceOffering = ServiceOffering.create(self.api_client,
                                                 self.services["service_offering"]
                                                 )
        self.assertEqual(verifyComputeOfferingCreation(self.apiclient, serviceOffering.id),
                         PASS, "Compute Offering verification failed")

        self.cleanup_co.append(serviceOffering)
        return

    @attr(tags=["basic","advanced"])
    def test_create_dynamic_compute_offering_no_cpunumber(self):
        """ Create dynamic compute offering with only cpunumber unspecified"""

        # Validations:
        # 1. Compute Offering creation should fail

        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = 128

        try:
            serviceOffering = ServiceOffering.create(self.api_client,
                                                 self.services["service_offering"]
                                                 )
            self.cleanup_co.append(serviceOffering)
            self.fail("Compute Offering creation succeded, it should have failed")
        except Exception:
            self.debug("Compute Offering Creation failed as expected")
        return

    @attr(tags=["basic","advanced"])
    def test_create_dynamic_compute_offering_no_cpuspeed(self):
        """ Create dynamic compute offering with only cpuspeed unspecified"""

        # Validations:
        # 1. Compute offering creation should fail

        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = 128

        try:
            serviceOffering = ServiceOffering.create(self.api_client,
                                                 self.services["service_offering"]
                                                 )
            self.cleanup_co.append(serviceOffering)
            self.fail("Compute Offering creation succeded, it should have failed")
        except Exception:
            self.debug("Compute Offering Creation failed as expected")
        return

    @attr(tags=["basic","advanced"])
    def test_create_dynamic_compute_offering_no_memory(self):
        """ Create dynamic compute offering with only memory unspecified"""

        # Validations:
        # 1. Compute offering creation should fail

        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = ""

        try:
            serviceOffering = ServiceOffering.create(self.api_client,
                                                 self.services["service_offering"]
                                                 )
            self.cleanup_co.append(serviceOffering)
            self.fail("Compute Offering creation succeded, it should have failed")
        except Exception:
            self.debug("Compute Offering Creation failed as expected")
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic","advanced"])
    def test_deploy_virtual_machines_static_offering(self, value):
        """Test deploy VM with static offering"""

        # Steps:
        # 1. Create admin/user account and create its user api client
        # 2. Create a static compute offering
        # 3. Deploy a VM with account api client and static service offering
        # 4. Repeat step 3 but also pass custom values for cpu number, cpu speed and memory
        #    while deploying VM

        # Validations:
        # 1. Step 3 should succeed
        # 2. Step 4 should fail

        isadmin=True
        if value == USER_ACCOUNT:
            isadmin=False

        # Create Account
        self.account = Account.create(self.apiclient,self.services["account"],domainid=self.domain.id, admin=isadmin)
        apiclient = self.testClient.createUserApiClient(
                                    UserName=self.account.name,
                                    DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create service offering
        self.services["service_offering"]["cpunumber"] = 2
        self.services["service_offering"]["cpuspeed"] = 256
        self.services["service_offering"]["memory"] = 128

        serviceOffering = ServiceOffering.create(self.apiclient,
                                                 self.services["service_offering"])

        self.cleanup_co.append(serviceOffering)

        # Deploy VM with static service offering
        try:
            VirtualMachine.create(apiclient,self.services["virtual_machine"],
                                                    serviceofferingid=serviceOffering.id,
                                                    accountid=self.account.name,domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Deploy VM with static service offering, also with custom values
        try:
            VirtualMachine.create(apiclient,self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=4,
                customcpuspeed=512,
                custommemory=256,
                accountid=self.account.name,domainid=self.account.domainid)
            self.fail("VM creation should have failed, it succeeded")
        except Exception as e:
            self.debug("vm creation failed as expected: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic","advanced"])
    def test_deploy_virtual_machines_dynamic_offering(self, value):
        """Test deploy VM with dynamic compute offering"""

        # Steps:
        # 1. Create admin/user account and create its user api client
        # 2. Create a dynamic service offering
        # 3. Deploy a VM with account api client and dynamic service offering
        #    without providing custom values for cpu number, cpu speed and memory
        # 4. Deploy a VM with account api client and dynamic service offering providing
        #    custom values for cpu number, cpu speed and memory
        # 5. Deploy a VM with account api client and dynamic service offering providing
        #    custom values only for cpu number

        # Validations:
        # 1. Step 3 should fail
        # 2. Step 4 should succeed
        # 3. Step 5 should fail

        isadmin=True
        if value == USER_ACCOUNT:
            isadmin=False

        # Create Account and its api client
        self.account = Account.create(self.apiclient,self.services["account"],domainid=self.domain.id, admin=isadmin)
        apiclient = self.testClient.createUserApiClient(
                                    UserName=self.account.name,
                                    DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create dynamic service offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering = ServiceOffering.create(self.apiclient,
                                                 self.services["service_offering"])

        self.cleanup_co.append(serviceOffering)

        # Deploy VM with dynamic compute offering without providing custom values for
        # cpu number, cpu speed and memory
        try:
            VirtualMachine.create(apiclient,self.services["virtual_machine"],
                                                    serviceofferingid=serviceOffering.id,
                                                    accountid=self.account.name,domainid=self.account.domainid)
            self.fail("VM creation succeded, it should have failed")
        except Exception as e:
            self.debug("vm creation failed as expected with error: %s" % e)

        # Deploy VM with dynamic compute offering providing custom values for
        # cpu number, cpu speed and memory
        try:
            VirtualMachine.create(apiclient,self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=2,
                customcpuspeed=256,
                custommemory=128,
                accountid=self.account.name,domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Deploy VM with dynamic compute offering providing custom values for only
        # cpu number
        try:
            VirtualMachine.create(apiclient,self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=2,
                accountid=self.account.name,domainid=self.account.domainid)
            self.fail("VM deployment should have failed, it succeded")
        except Exception as e:
            self.debug("vm creation failed as expected: %s" % e)
        return

    @data(ADMIN_ACCOUNT, USER_ACCOUNT)
    @attr(tags=["basic","advanced"])
    def test_check_vm_stats(self, value):
        """Deploy VM with dynamic service offering and check VM stats"""

        # Steps:
        # 1. Create admin/user account and create its user api client
        # 2. Create a dynamic service offering
        # 3. Deploy a VM with account api client and dynamic service offering
        #    providing custom values for cpu number, cpu speed and memory
        # 4. List the VM and verify the dynamic parameters are same as passed

        isadmin=True
        if value == USER_ACCOUNT:
            isadmin=False

        # Create Account and api client
        self.account = Account.create(self.apiclient,self.services["account"],domainid=self.domain.id, admin=isadmin)
        apiclient = self.testClient.createUserApiClient(
                                    UserName=self.account.name,
                                    DomainName=self.account.domain)
        self.cleanup.append(self.account)

        # Create dynamic compute offering
        self.services["service_offering"]["cpunumber"] = ""
        self.services["service_offering"]["cpuspeed"] = ""
        self.services["service_offering"]["memory"] = ""

        serviceOffering = ServiceOffering.create(self.apiclient,
                                                 self.services["service_offering"])

        self.cleanup_co.append(serviceOffering)

        # Custom values
        customcpunumber = 2
        customcpuspeed = 256
        custommemory = 128

        # Deploy VM with dynamic service offering and the custom values
        try:
            virtualMachine = VirtualMachine.create(apiclient,self.services["virtual_machine"],
                serviceofferingid=serviceOffering.id,
                customcpunumber=customcpunumber,
                customcpuspeed=customcpuspeed,
                custommemory=custommemory,
                accountid=self.account.name,domainid=self.account.domainid)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        vmlist = VirtualMachine.list(self.apiclient, id=virtualMachine.id)
        self.assertEqual(validateList(vmlist)[0], PASS, "vm list validation failed")
        vm = vmlist[0]

        # Verify the custom values
        self.assertEqual(str(vm.cpunumber), str(customcpunumber), "vm cpu number %s\
                 not matching with provided custom cpu number %s" % \
                 (vm.cpunumber, customcpunumber))

        self.assertEqual(str(vm.cpuspeed), str(customcpuspeed), "vm cpu speed %s\
                 not matching with provided custom cpu speed %s" % \
                 (vm.cpuspeed, customcpuspeed))

        self.assertEqual(str(vm.memory), str(custommemory), "vm memory %s\
                 not matching with provided custom memory %s" % \
                 (vm.memory, custommemory))
        return
