#!/usr/bin/env python
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
Tests for host control state
"""

from marvin.cloudstackAPI import updateHost
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_routers,
                               list_ssvms)
from marvin.lib.base import (Account,
                             Domain,
                             Host,
                             ServiceOffering,
                             VirtualMachine)


class TestHostControlState(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHostControlState, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.domain = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )

        cls._cleanup = [
            cls.domain,
            cls.account,
            cls.vm,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        super(TestHostControlState, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestHostControlState, self).tearDown()

    def disable_host(self, id):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Disable"
        response = self.apiclient.updateHost(cmd)
        self.assertEqual(response.resourcestate, "Disabled")

    def enable_host(self, id):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Enable"
        response = self.apiclient.updateHost(cmd)
        self.assertEqual(response.resourcestate, "Enabled")

    def verify_uservm_host_control_state(self, vm_id, state):
        list_vms = VirtualMachine.list(
            self.apiclient,
            id=vm_id
        )
        vm = list_vms[0]
        self.assertEqual(vm.hostcontrolstate,
                         state,
                         msg="host control state should be %s, but it is %s" % (state, vm.hostcontrolstate))

    def verify_ssvm_host_control_state(self, vm_id, state):
        list_ssvm_response = list_ssvms(
            self.apiclient,
            id=vm_id
        )
        vm = list_ssvm_response[0]
        self.assertEqual(vm.hostcontrolstate,
                         state,
                         msg="host control state should be %s, but it is %s" % (state, vm.hostcontrolstate))

    def verify_router_host_control_state(self, vm_id, state):
        list_router_response = list_routers(
            self.apiclient,
            id=vm_id
        )
        vm = list_router_response[0]
        self.assertEqual(vm.hostcontrolstate,
                         state,
                         msg="host control state should be %s, but it is %s" % (state, vm.hostcontrolstate))

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_uservm_host_control_state(self):
        """ Verify host control state for user vm """
        # 1. verify hostcontrolstate = Enabled
        # 2. Disable the host, verify hostcontrolstate = Disabled

        list_vms = VirtualMachine.list(
            self.apiclient,
            id=self.vm.id
        )
        host_id = list_vms[0].hostid

        self.verify_uservm_host_control_state(self.vm.id, "Enabled")

        self.disable_host(host_id)
        self.verify_uservm_host_control_state(self.vm.id, "Disabled")

        self.enable_host(host_id)
        self.verify_uservm_host_control_state(self.vm.id, "Enabled")

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_ssvm_host_control_state(self):
        """ Verify host control state for systemvm """
        # 1. verify hostcontrolstate = Enabled
        # 2. Disable the host, verify hostcontrolstate = Disabled

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        ssvm = list_ssvm_response[0]
        host_id = ssvm.hostid

        self.verify_ssvm_host_control_state(ssvm.id, "Enabled")

        self.disable_host(host_id)
        self.verify_ssvm_host_control_state(ssvm.id, "Disabled")

        self.enable_host(host_id)
        self.verify_ssvm_host_control_state(ssvm.id, "Enabled")

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_router_host_control_state(self):
        """ Verify host control state for router """
        # 1. verify hostcontrolstate = Enabled
        # 2. Disable the host, verify hostcontrolstate = Disabled

        list_router_response = list_routers(
            self.apiclient,
            state='Running',
            listall=True,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]
        host_id = router.hostid

        self.verify_router_host_control_state(router.id, "Enabled")

        self.disable_host(host_id)
        self.verify_router_host_control_state(router.id, "Disabled")

        self.enable_host(host_id)
        self.verify_router_host_control_state(router.id, "Enabled")
