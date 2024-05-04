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
from marvin.cloudstackAPI import (expungeVirtualMachine, updateConfiguration)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account, ServiceOffering, Template, Host, VirtualMachine)
from marvin.lib.common import (get_domain, get_zone)
from nose.plugins.attrib import attr


class TestVMDeploymentPlannerStrictTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestVMDeploymentPlannerStrictTags, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls.service_offering_h1 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h1"])
        cls.service_offering_h2 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h2"])

        cls.template_t1 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t1")
        cls.template_t1.download(cls.apiclient)

        cls.template_t2 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t2")
        cls.template_t2.download(cls.apiclient)

        hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type='Routing')
        cls.host_h1 = hosts[0] if len(hosts) >= 1 else None

        cls._cleanup = [cls.account, cls.service_offering_h1, cls.service_offering_h2, cls.template_t1, cls.template_t2]

    @classmethod
    def tearDownClass(cls):
        if cls.host_h1:
            Host.update(cls.apiclient, id=cls.host_h1.id, hosttags="")
        cls.updateConfiguration("vm.strict.host.tags", "")
        cls.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        cls.updateConfiguration("resource.limit.host.tags", "")
        super(TestVMDeploymentPlannerStrictTags, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        if self.host_h1:
            Host.update(self.apiclient, id=self.host_h1.id, hosttags="h1,t1,v1")
        self.updateConfiguration("vm.strict.host.tags", "")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "")
        self.cleanup = []

    def tearDown(self):
        self.cleanup_vm_for_template(self.template_t1.id)
        self.cleanup_vm_for_template(self.template_t2.id)
        super(TestVMDeploymentPlannerStrictTags, self).tearDown()

    def cleanup_vm_for_template(self, templateid):
        vm_list = VirtualMachine.list(self.apiclient, listall=True, templateid=templateid)
        if type(vm_list) is list:
            for vm in vm_list:
                self.expunge_vm(vm)

    def expunge_vm(self, vm):
        try:
            cmd = expungeVirtualMachine.expungeVirtualMachineCmd()
            cmd.id = vm.id
            self.apiclient.expungeVirtualMachine(cmd)
        except Exception as e:
            self.debug("Failed to expunge VM: %s" % e)

    @classmethod
    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        self.apiclient.updateConfiguration(cmd)

    def deploy_vm(self, destination_id, template_id, service_offering_id):
        return VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                     templateid=template_id, serviceofferingid=service_offering_id,
                                     hostid=destination_id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_01_deploy_vm_on_specific_host_without_strict_tags(self):
        self.updateConfiguration("vm.strict.host.tags", "")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_02_deploy_vm_on_any_host_without_strict_tags(self):
        self.updateConfiguration("vm.strict.host.tags", "")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "")

        vm = self.deploy_vm(None, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertIsNotNone(vm, "VM instance was not deployed")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_03_deploy_vm_on_specific_host_with_strict_tags_success(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "false")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")

        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_04_deploy_vm_on_any_host_with_strict_tags_success(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "false")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(None, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertIsNotNone(vm, "VM instance was not deployed")

        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")

        vm = self.deploy_vm(None, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_05_deploy_vm_on_specific_host_with_strict_tags_failure(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        try:
            vm = self.deploy_vm(self.host_h1.id, self.template_t2.id, self.service_offering_h1.id)
            self.cleanup.append(vm)
            self.fail("VM should not be deployed")
        except Exception as e:
            self.assertTrue("Cannot deploy VM, destination host" in str(e))

        try:
            vm = self.deploy_vm(self.host_h1.id, self.template_t2.id, self.service_offering_h2.id)
            self.cleanup.append(vm)
            self.fail("VM should not be deployed")
        except Exception as e:
            self.assertTrue("Cannot deploy VM, destination host" in str(e))

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_06_deploy_vm_on_any_host_with_strict_tags_failure(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        try:
            vm = self.deploy_vm(None, self.template_t2.id, self.service_offering_h1.id)
            self.cleanup.append(vm)
            self.fail("VM should not be deployed")
        except Exception as e:
            self.assertTrue("No suitable host found for vm " in str(e))


class TestScaleVMStrictTags(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestScaleVMStrictTags, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls.service_offering_h1 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h1"])
        cls.service_offering_h2 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h2"])

        cls.template_t1 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t1")
        cls.template_t1.download(cls.apiclient)

        cls.template_t2 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t2")
        cls.template_t2.download(cls.apiclient)

        hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type='Routing')
        cls.host_h1 = hosts[0] if len(hosts) >= 1 else None

        cls._cleanup = [cls.account, cls.service_offering_h1, cls.service_offering_h2, cls.template_t1, cls.template_t2]

    @classmethod
    def tearDownClass(cls):
        if cls.host_h1:
            Host.update(cls.apiclient, id=cls.host_h1.id, hosttags="")
        cls.updateConfiguration("vm.strict.host.tags", "")
        cls.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        cls.updateConfiguration("resource.limit.host.tags", "")
        super(TestScaleVMStrictTags, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        if self.host_h1:
            Host.update(self.apiclient, id=self.host_h1.id, hosttags="h1,t1,v1,h2,t2,v2")
        self.updateConfiguration("vm.strict.host.tags", "")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "")
        self.cleanup = []

    def tearDown(self):
        self.cleanup_vm_for_template(self.template_t1.id)
        self.cleanup_vm_for_template(self.template_t2.id)
        super(TestScaleVMStrictTags, self).tearDown()

    def cleanup_vm_for_template(self, templateid):
        vm_list = VirtualMachine.list(self.apiclient, listall=True, templateid=templateid)
        if type(vm_list) is list:
            for vm in vm_list:
                self.expunge_vm(vm)

    def expunge_vm(self, vm):
        try:
            cmd = expungeVirtualMachine.expungeVirtualMachineCmd()
            cmd.id = vm.id
            self.apiclient.expungeVirtualMachine(cmd)
        except Exception as e:
            self.debug("Failed to expunge VM: %s" % e)

    @classmethod
    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        self.apiclient.updateConfiguration(cmd)

    def deploy_vm(self, destination_id, template_id, service_offering_id):
        return VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                     templateid=template_id, serviceofferingid=service_offering_id,
                                     hostid=destination_id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_01_scale_vm_strict_tags_success(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")
        vm.stop(self.apiclient)
        vm.scale(self.apiclient, serviceOfferingId=self.service_offering_h2.id)
        vm.start(self.apiclient)
        scaled_vm = VirtualMachine.list(self.apiclient, id=vm.id, listall=True)[0]
        self.assertEqual(scaled_vm.serviceofferingid, self.service_offering_h2.id, "VM was not scaled")
        self.assertEqual(self.host_h1.id, scaled_vm.hostid, "VM was not scaled")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_02_scale_vm_strict_tags_failure(self):
        if self.host_h1:
            Host.update(self.apiclient, id=self.host_h1.id, hosttags="h1,t1,v1")

        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)

        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")
        try:
            vm.stop(self.apiclient)
            vm.scale(self.apiclient, serviceOfferingId=self.service_offering_h2.id)
            vm.start(self.apiclient)
            self.fail("VM should not be be able scale and start")
        except Exception as e:
            self.assertTrue("No suitable host found for vm " in str(e))


class TestRestoreVMStrictTags(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestRestoreVMStrictTags, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls.service_offering_h1 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h1"])
        cls.service_offering_h2 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h2"])

        cls.template_t1 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t1")
        cls.template_t1.download(cls.apiclient)

        cls.template_t2 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t2")
        cls.template_t2.download(cls.apiclient)

        hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type='Routing')
        cls.host_h1 = hosts[0] if len(hosts) >= 1 else None

        cls._cleanup = [cls.account, cls.service_offering_h1, cls.service_offering_h2, cls.template_t1, cls.template_t2]

    @classmethod
    def tearDownClass(cls):
        if cls.host_h1:
            Host.update(cls.apiclient, id=cls.host_h1.id, hosttags="")
        cls.updateConfiguration("vm.strict.host.tags", "")
        cls.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        cls.updateConfiguration("resource.limit.host.tags", "")
        super(TestRestoreVMStrictTags, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        if self.host_h1:
            Host.update(self.apiclient, id=self.host_h1.id, hosttags="h1,t1,v1")
        self.updateConfiguration("vm.strict.host.tags", "")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "")
        self.cleanup = []

    def tearDown(self):
        self.cleanup_vm_for_template(self.template_t1.id)
        self.cleanup_vm_for_template(self.template_t2.id)
        super(TestRestoreVMStrictTags, self).tearDown()

    def cleanup_vm_for_template(self, templateid):
        vm_list = VirtualMachine.list(self.apiclient, listall=True, templateid=templateid)
        if type(vm_list) is list:
            for vm in vm_list:
                self.expunge_vm(vm)

    def expunge_vm(self, vm):
        try:
            cmd = expungeVirtualMachine.expungeVirtualMachineCmd()
            cmd.id = vm.id
            self.apiclient.expungeVirtualMachine(cmd)
        except Exception as e:
            self.debug("Failed to expunge VM: %s" % e)

    @classmethod
    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        self.apiclient.updateConfiguration(cmd)

    def deploy_vm(self, destination_id, template_id, service_offering_id):
        return VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                     templateid=template_id, serviceofferingid=service_offering_id,
                                     hostid=destination_id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_01_restore_vm_strict_tags_success(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")

        vm.restore(self.apiclient, templateid=self.template_t2.id, expunge=True)
        restored_vm = VirtualMachine.list(self.apiclient, id=vm.id, listall=True)[0]
        self.assertEqual(restored_vm.templateid, self.template_t2.id, "VM was not restored")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_02_restore_vm_strict_tags_failure(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)

        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")
        try:
            vm.restore(self.apiclient, templateid=self.template_t2.id, expunge=True)
            self.fail("VM should not be restored")
        except Exception as e:
            self.assertTrue("No suitable host found for vm " in str(e))


class TestMigrateVMStrictTags(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestMigrateVMStrictTags, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type='Routing')
        cls.host_h1 = hosts[0] if len(hosts) >= 1 else None
        cls.host_h2 = None
        if len(hosts) >= 2:
            for host in hosts[1:]:
                if host.clusterid == cls.host_h1.clusterid:
                    cls.host_h2 = host
                    break

        if not cls.host_h2:
            cls.skipTest("There are not enough hosts to run this test")

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls.service_offering_h1 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h1"])
        cls.service_offering_h2 = ServiceOffering.create(cls.apiclient, cls.services["service_offering_h2"])

        cls.template_t1 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t1")
        cls.template_t1.download(cls.apiclient)

        cls.template_t2 = Template.register(cls.apiclient, cls.services["test_templates"][cls.hypervisor.lower()],
                                            zoneid=cls.zone.id, hypervisor=cls.hypervisor.lower(), templatetag="t2")
        cls.template_t2.download(cls.apiclient)

        cls._cleanup = [cls.account, cls.service_offering_h1, cls.service_offering_h2, cls.template_t1, cls.template_t2]

    @classmethod
    def tearDownClass(cls):
        if cls.host_h1:
            Host.update(cls.apiclient, id=cls.host_h1.id, hosttags="")
        if cls.host_h2:
            Host.update(cls.apiclient, id=cls.host_h2.id, hosttags="")
        cls.updateConfiguration("vm.strict.host.tags", "")
        cls.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        cls.updateConfiguration("resource.limit.host.tags", "")
        super(TestMigrateVMStrictTags, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        if self.host_h1:
            Host.update(self.apiclient, id=self.host_h1.id, hosttags="h1,t1,v1")
        self.updateConfiguration("vm.strict.host.tags", "")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "")
        self.cleanup = []

    def tearDown(self):
        self.cleanup_vm_for_template(self.template_t1.id)
        self.cleanup_vm_for_template(self.template_t2.id)
        super(TestMigrateVMStrictTags, self).tearDown()

    def cleanup_vm_for_template(self, templateid):
        vm_list = VirtualMachine.list(self.apiclient, listall=True, templateid=templateid)
        if type(vm_list) is list:
            for vm in vm_list:
                self.expunge_vm(vm)

    def expunge_vm(self, vm):
        try:
            cmd = expungeVirtualMachine.expungeVirtualMachineCmd()
            cmd.id = vm.id
            self.apiclient.expungeVirtualMachine(cmd)
        except Exception as e:
            self.debug("Failed to expunge VM: %s" % e)

    @classmethod
    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        self.apiclient.updateConfiguration(cmd)

    def deploy_vm(self, destination_id, template_id, service_offering_id):
        return VirtualMachine.create(self.apiclient, self.services["virtual_machine"], zoneid=self.zone.id,
                                     templateid=template_id, serviceofferingid=service_offering_id,
                                     hostid=destination_id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_01_migrate_vm_strict_tags_success(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)
        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")
        Host.update(self.apiclient, id=self.host_h2.id, hosttags="h1,t1,v1")
        vm.migrate(self.apiclient, self.host_h2.id)
        migrated_vm = VirtualMachine.list(self.apiclient, id=vm.id, listall=True)[0]
        self.assertEqual(migrated_vm.hostid, self.host_h2.id, "VM was not migratd")

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_02_migrate_vm_strict_tags_failure(self):
        self.updateConfiguration("vm.strict.host.tags", "v1,v2")
        self.updateConfiguration("vm.strict.resource.limit.host.tag.check", "true")
        self.updateConfiguration("resource.limit.host.tags", "h1,h2,t1,t2")

        vm = self.deploy_vm(self.host_h1.id, self.template_t1.id, self.service_offering_h1.id)
        self.cleanup.append(vm)

        self.assertEqual(self.host_h1.id, vm.hostid, "VM instance was not deployed on target host ID")
        Host.update(self.apiclient, id=self.host_h2.id, hosttags="h2,t2,v2")
        try:
            vm.migrate(self.apiclient, self.host_h2.id)
            VirtualMachine.list(self.apiclient, id=vm.id, listall=True)[0]
            self.fail("VM should not be migrated")
        except Exception as e:
            self.assertTrue("Cannot deploy VM, destination host:" in str(e))
