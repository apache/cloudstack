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
""" BVT tests for Instance Boot Group and readiness rules lifecycle
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             InstanceGroup,
                             InstanceBootGroup,
                             InstanceBootGroupReadinessRule)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import (random_gen)
from marvin.cloudstackException import CloudstackAPIException
from marvin.codes import FAILED
from nose.plugins.attrib import attr
import logging
import time

_multiprocess_shared_ = True


class TestInstanceBootGroup(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestInstanceBootGroup, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.logger = logging.getLogger('TestInstanceBootGroup')
        cls.logger.setLevel(logging.DEBUG)

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_template(
            cls.apiclient,
            zone_id=cls.zone.id,
            hypervisor=cls.hypervisor
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template"

        cls._cleanup = []
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)

    @classmethod
    def tearDownClass(cls):
        super(TestInstanceBootGroup, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestInstanceBootGroup, self).tearDown()

    def deployTestVm(self, name_hint, group=None):
        """Deploys a small VM under the shared test account, tracked for cleanup"""
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            group=group
        )
        self.cleanup.append(vm)
        return vm

    def createTestBootGroup(self, name_hint):
        boot_group = InstanceBootGroup.create(
            self.apiclient,
            name="-".join([name_hint, random_gen()]),
            description="smoke test boot group",
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(boot_group)
        return boot_group

    def waitForMemberReadinessStatus(self, boot_group, expected_status, timeout=180, interval=5, ignoreinstancestate=None):
        """Polls listInstanceBootGroupMembers (with readiness details requested) until every
        member reaches expected_status, or timeout"""
        members = []
        waited = 0
        while waited <= timeout:
            kwargs = {"details": ["readiness"]}
            if ignoreinstancestate is not None:
                kwargs["ignoreinstancestate"] = ignoreinstancestate
            members = InstanceBootGroup.listMembers(self.apiclient, bootgroupid=boot_group.id, **kwargs)
            if members and all(m.readinessstatus == expected_status for m in members):
                return members
            time.sleep(interval)
            waited += interval
        return members

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_boot_group_and_member_lifecycle(self):
        """Create a boot group, add a VM member and an InstanceGroup member with
        distinct boot order tiers, verify listing, update a member's order, remove a
        member, then delete the boot group and confirm its members are cleaned up"""

        vm1 = self.deployTestVm("smoke-boot-db")

        group = InstanceGroup.create(
            self.apiclient,
            name="smoke-boot-web-grp",
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(group)
        vm2 = self.deployTestVm("smoke-boot-web", group=group.name)

        boot_group = self.createTestBootGroup("smoke-boot-group")
        self.assertEqual(boot_group.name.startswith("smoke-boot-group"), True, "Check boot group name")

        member_vm = boot_group.addMember(self.apiclient, order=0, virtualmachineid=vm1.id)
        self.assertEqual(member_vm.membertype, "VirtualMachine", "Check VM member type")
        self.assertEqual(member_vm.memberid, vm1.id, "Check VM member id")
        self.assertEqual(member_vm.order, 0, "Check VM member boot order")

        member_group = boot_group.addMember(self.apiclient, order=1, instancegroupid=group.id)
        self.assertEqual(member_group.membertype, "InstanceGroup", "Check instance group member type")
        self.assertEqual(member_group.memberid, group.id, "Check instance group member id")
        self.assertEqual(member_group.order, 1, "Check instance group member boot order")

        members = InstanceBootGroup.listMembers(self.apiclient, bootgroupid=boot_group.id)
        self.assertEqual(len(members), 2, "Check both members are listed")

        member_vm.update(self.apiclient, order=5)
        members = InstanceBootGroup.listMembers(self.apiclient, bootgroupid=boot_group.id)
        updated_member_vm = next(m for m in members if m.id == member_vm.id)
        self.assertEqual(updated_member_vm.order, 5, "Check member boot order was updated")

        member_group.delete(self.apiclient)
        members = InstanceBootGroup.listMembers(self.apiclient, bootgroupid=boot_group.id)
        self.assertEqual(len(members), 1, "Check member was removed")
        self.assertEqual(members[0].id, member_vm.id, "Check remaining member is the VM member")

        boot_group_id = boot_group.id
        boot_group.delete(self.apiclient)
        self.cleanup.remove(boot_group)
        boot_groups = InstanceBootGroup.list(self.apiclient, id=boot_group_id)
        self.assertTrue(
            boot_groups is None or len(boot_groups) == 0,
            "Check boot group no longer listed after delete"
        )
        # The parent boot group is gone, so listing its members is itself an invalid request
        # (bootgroupid must reference an existing group), not an empty-result query.
        with self.assertRaises(CloudstackAPIException):
            InstanceBootGroup.listMembers(self.apiclient, bootgroupid=boot_group_id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_readiness_rule_lifecycle(self):
        """Create, list, update and delete a readiness rule on a boot group member"""

        vm = self.deployTestVm("smoke-boot-rule-vm")
        boot_group = self.createTestBootGroup("smoke-boot-rule-group")
        boot_group.addMember(self.apiclient, order=0, virtualmachineid=vm.id)

        rule = InstanceBootGroupReadinessRule.create(
            self.apiclient,
            bootgroupid=boot_group.id,
            ruletype="Ping",
            virtualmachineid=vm.id
        )
        self.assertEqual(rule.ruletype, "Ping", "Check readiness rule type")
        self.assertEqual(rule.enabled, True, "Check readiness rule is enabled by default")

        rules = InstanceBootGroupReadinessRule.list(self.apiclient, bootgroupid=boot_group.id)
        self.assertEqual(len(rules), 1, "Check readiness rule is listed")

        rule.update(self.apiclient, enabled=False)
        rules = InstanceBootGroupReadinessRule.list(self.apiclient, bootgroupid=boot_group.id, id=rule.id)
        self.assertEqual(rules[0].enabled, False, "Check readiness rule was disabled")

        rule.delete(self.apiclient)
        rules = InstanceBootGroupReadinessRule.list(self.apiclient, bootgroupid=boot_group.id)
        self.assertTrue(
            rules is None or len(rules) == 0,
            "Check readiness rule no longer listed after delete"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_start_stop_reboot_orchestration(self):
        """Deploy a two-tier boot group with no readiness rules attached (pure boot-order
        semantics), then exercise startInstanceBootGroup / rebootInstanceBootGroup /
        stopInstanceBootGroup and confirm all members end each phase in the right state"""

        vm1 = self.deployTestVm("smoke-boot-tier0")
        vm2 = self.deployTestVm("smoke-boot-tier1")
        boot_group = self.createTestBootGroup("smoke-boot-orch-group")
        boot_group.addMember(self.apiclient, order=0, virtualmachineid=vm1.id)
        boot_group.addMember(self.apiclient, order=1, virtualmachineid=vm2.id)

        vm1.stop(self.apiclient, forced=True)
        vm2.stop(self.apiclient, forced=True)

        boot_group.start(self.apiclient)
        vm1 = VirtualMachine.list(self.apiclient, id=vm1.id)[0]
        vm2 = VirtualMachine.list(self.apiclient, id=vm2.id)[0]
        self.assertEqual(vm1.state, 'Running', "Check tier0 VM is Running after boot group start")
        self.assertEqual(vm2.state, 'Running', "Check tier1 VM is Running after boot group start")

        boot_group.reboot(self.apiclient, forced=True)
        vm1 = VirtualMachine.list(self.apiclient, id=vm1.id)[0]
        vm2 = VirtualMachine.list(self.apiclient, id=vm2.id)[0]
        self.assertEqual(vm1.state, 'Running', "Check tier0 VM is Running after boot group reboot")
        self.assertEqual(vm2.state, 'Running', "Check tier1 VM is Running after boot group reboot")

        boot_group.stop(self.apiclient, forced=True)
        vm1 = VirtualMachine.list(self.apiclient, id=vm1.id)[0]
        vm2 = VirtualMachine.list(self.apiclient, id=vm2.id)[0]
        self.assertEqual(vm1.state, 'Stopped', "Check tier0 VM is Stopped after boot group stop")
        self.assertEqual(vm2.state, 'Stopped', "Check tier1 VM is Stopped after boot group stop")

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_04_readiness_cache_invalidated_on_out_of_band_restart(self):
        """Regression test for InstanceBootGroupVmStateListener: once a member is Ready,
        restarting its VM directly (outside boot group orchestration) must invalidate the
        cached rule result (internally Unknown, surfaced as a NotReady aggregate) rather
        than leaving a stale Ready behind"""

        if self.zone.networktype.lower() != 'advanced':
            self.skipTest("Ping readiness rule requires an advanced zone with a virtual router")

        vm = self.deployTestVm("smoke-boot-cache-vm")
        boot_group = self.createTestBootGroup("smoke-boot-cache-group")
        boot_group.addMember(self.apiclient, order=0, virtualmachineid=vm.id)
        InstanceBootGroupReadinessRule.create(
            self.apiclient,
            bootgroupid=boot_group.id,
            ruletype="Ping",
            virtualmachineid=vm.id
        )

        vm.stop(self.apiclient, forced=True)
        boot_group.start(self.apiclient)

        members = self.waitForMemberReadinessStatus(boot_group, "Ready")
        self.assertTrue(len(members) > 0, "Check member list is not empty")
        self.assertEqual(members[0].readinessstatus, "Ready", "Check member is Ready after boot group start")

        # Restart the VM directly, bypassing boot group orchestration entirely.
        vm.reboot(self.apiclient, forced=True)

        members = InstanceBootGroup.listMembers(
            self.apiclient,
            bootgroupid=boot_group.id,
            details=["readiness"],
            ignoreinstancestate=True
        )
        # The per-rule cache is invalidated to Unknown, but the member-level aggregate treats
        # any non-Ready rule status (Unknown included) as NotReady - Unknown never surfaces as
        # the aggregate readinessstatus itself. The listener's message is what distinguishes
        # this from a genuine failed check, so assert on that instead.
        self.assertEqual(
            members[0].readinessstatus,
            "NotReady",
            "Cached readiness must be invalidated (no longer a stale Ready) after an out-of-band VM restart"
        )
        self.assertTrue(
            "not yet re-verified" in members[0].readinessmessage,
            "Check the NotReady status is specifically due to cache invalidation on restart, not some other failure"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_membership_guard_rejects_duplicate_membership(self):
        """A VM already a member of one boot group cannot be added as a member of another"""

        vm = self.deployTestVm("smoke-boot-guard-vm")
        boot_group1 = self.createTestBootGroup("smoke-boot-guard-group1")
        boot_group2 = self.createTestBootGroup("smoke-boot-guard-group2")

        boot_group1.addMember(self.apiclient, order=0, virtualmachineid=vm.id)

        with self.assertRaises(CloudstackAPIException):
            boot_group2.addMember(self.apiclient, order=0, virtualmachineid=vm.id)
