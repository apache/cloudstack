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

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             DiskOffering,
                             Configurations)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template,
                               is_config_suitable)


class TestDeployVMLease(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVMLease, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_test_template(
            cls.api_client,
            cls.zone.id,
            cls.hypervisor
        )

        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"


        # enable instance lease feature
        Configurations.update(cls.api_client,
                              name="instance.lease.enabled",
                              value="true"
                              )

        # Create service, disk offerings  etc
        cls.non_lease_svc_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"],
            name="non-lease-svc-offering"
        )

        # Create service, disk offerings  etc
        cls.lease_svc_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"],
            name="lease-svc-offering",
            leaseduration=20,
            leaseexpiryaction="DESTROY"
        )

        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )

        cls._cleanup = [
            cls.lease_svc_offering,
            cls.non_lease_svc_offering,
            cls.disk_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # disable instance lease feature
            Configurations.update(cls.api_client,
                                name="instance.lease.enabled",
                                value="false"
                                )
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
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
            "basic"],
        required_hardware="true")
    def test_01_deploy_vm_no_lease_svc_offering(self):
        """Test Deploy Virtual Machine from non-lease-svc-offering

        Validate the following:
        1. deploy VM using non-lease-svc-offering
        2. confirm vm has no lease configured
        """

        non_lease_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.non_lease_svc_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )
        self.verify_no_lease_configured_for_vm(non_lease_vm.id)
        return

    @attr(
        tags=[
            "advanced",
            "basic"],
        required_hardware="true")
    def test_02_deploy_vm_no_lease_svc_offering_with_lease_params(self):
        """Test Deploy Virtual Machine from non-lease-svc-offering and lease parameters are used to enabled lease for vm

        Validate the following:
        1. deploy VM using non-lease-svc-offering and passing leaseduration and leaseexpiryaction
        2. confirm vm has lease configured
        """
        lease_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.non_lease_svc_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor,
            leaseduration=10,
            leaseexpiryaction="STOP"
        )
        self.verify_lease_configured_for_vm(lease_vm.id, lease_duration=10, lease_expiry_action="STOP")
        return

    @attr(
        tags=[
            "advanced",
            "basic"],
        required_hardware="true")
    def test_03_deploy_vm_lease_svc_offering_with_no_param(self):
        """Test Deploy Virtual Machine from lease-svc-offering without lease params
            expect vm to inherit svc_offering lease properties

        Validate the following:
        1. deploy VM using lease-svc-offering without passing leaseduration and leaseexpiryaction
        2. confirm vm has lease configured
        """
        lease_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.lease_svc_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )
        self.verify_lease_configured_for_vm(lease_vm.id, lease_duration=20, lease_expiry_action="DESTROY")
        return

    @attr(
        tags=[
            "advanced",
            "basic"],
        required_hardware="true")
    def test_04_deploy_vm_lease_svc_offering_with_param(self):
        """Test Deploy Virtual Machine from lease-svc-offering with overridden lease properties

        Validate the following:
        1. confirm svc_offering has lease properties
        2. deploy VM using lease-svc-offering and leaseduration and leaseexpiryaction passed
        3. confirm vm has lease configured
        """
        self.verify_svc_offering()

        lease_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.lease_svc_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor,
            leaseduration=30,
            leaseexpiryaction="STOP"
        )
        self.verify_lease_configured_for_vm(lease_vm.id, lease_duration=30, lease_expiry_action="STOP")
        return


    @attr(
        tags=[
            "advanced",
            "basic"],
        required_hardware="true")
    def test_05_deploy_vm_lease_svc_offering_with_lease_param_disabled(self):
        """Test Deploy Virtual Machine from lease-svc-offering and passing -1 leaseduration to set no-expiry

        Validate the following:
        1. deploy VM using lease-svc-offering
        2. leaseduration is set as -1 in the deploy vm request to disable lease
        3. confirm vm has no lease configured
        """

        lease_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.non_lease_svc_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor,
            leaseduration=-1
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=lease_vm.id
        )
        vm = vms[0]
        self.verify_no_lease_configured_for_vm(vm.id)
        return

    @attr(
        tags=[
            "advanced",
            "basic"],
        required_hardware="true")
    def test_06_deploy_vm_lease_svc_offering_with_disabled_lease(self):
        """Test Deploy Virtual Machine from lease-svc-offering with lease feature disabled

        Validate the following:
        1. Disable lease feature
        2. deploy VM using lease-svc-offering
        3. confirm vm has no lease configured
        """

        Configurations.update(self.api_client,
                              name="instance.lease.enabled",
                              value="false"
                              )

        lease_vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.template.id,
            serviceofferingid=self.lease_svc_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=lease_vm.id
        )
        vm = vms[0]
        self.verify_no_lease_configured_for_vm(vm.id)
        return


    def verify_svc_offering(self):
        svc_offering_list = ServiceOffering.list(
            self.api_client,
            id=self.lease_svc_offering.id
        )

        svc_offering = svc_offering_list[0]

        self.assertIsNotNone(
            svc_offering.leaseduration,
            "svc_offering has lease configured"
        )

        self.assertEqual(
            20,
            svc_offering.leaseduration,
            "svc_offering has 20 days for lease"
        )

    def verify_lease_configured_for_vm(self, vm_id=None, lease_duration=None, lease_expiry_action=None):
        vms = VirtualMachine.list(
            self.apiclient,
            id=vm_id
        )
        vm = vms[0]
        self.assertEqual(
            lease_duration,
            vm.leaseduration,
            "check to confirm leaseduration is configured"
            )

        self.assertEqual(
            lease_expiry_action,
            vm.leaseexpiryaction,
            "check to confirm leaseexpiryaction is configured"
            )

        self.assertIsNotNone(vm.leaseexpirydate, "confirm leaseexpirydate is available")


    def verify_no_lease_configured_for_vm(self, vm_id=None):
        if vm_id == None:
            return
        vms = VirtualMachine.list(
            self.apiclient,
            id=vm_id
        )
        vm = vms[0]
        self.assertIsNone(vm.leaseduration)
        self.assertIsNone(vm.leaseexpiryaction)
