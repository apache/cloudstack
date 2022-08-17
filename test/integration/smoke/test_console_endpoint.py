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

from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import (createConsoleEndpoint, updateConfiguration, listConfigurations, destroySystemVm)
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_ssvms)
from nose.plugins.attrib import attr

class TestConsoleEndpoint(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestConsoleEndpoint, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.domain = get_domain(cls.apiclient)
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        if cls.template == FAILED:
            assert False, "get_template() failed to return template"

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.vm1 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )

        cls._cleanup = [
            cls.service_offering,
            cls.vm1,
            cls.account
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_console_endpoint_permissions(self):
        cmd = createConsoleEndpoint.createConsoleEndpointCmd()
        cmd.virtualmachineid=self.vm1.id
        endpoint = self.apiclient.createConsoleEndpoint(cmd)

        if not endpoint:
            self.fail("Failed to get generate VM console endpoint")

        self.assertTrue(endpoint.success)
        self.assertNotEqual(len(endpoint.url), 0, "VM console endpoint url was empty")

        account2 = Account.create(
            self.apiclient,
            self.services["account2"],
            domainid=self.domain.id
        )
        self.cleanup.append(account2)
        account2_user = account2.user[0]
        account2ApiClient = self.testClient.getUserApiClient(account2_user.username, self.domain.name)

        endpoint = account2ApiClient.createConsoleEndpoint(cmd)
        self.assertFalse(endpoint.success)
        self.assertTrue(endpoint.url is None)
        return

    def checkForRunningSystemVM(self, ssvm, ssvm_type=None):
        if not ssvm:
            return None

        def checkRunningState():
            if not ssvm_type:
                response = list_ssvms(
                    self.apiclient,
                    id=ssvm.id
                )
            else:
                response = list_ssvms(
                    self.apiclient,
                    zoneid=self.zone.id,
                    systemvmtype=ssvm_type
                )

            if isinstance(response, list):
                ssvm_response = response[0]
                return ssvm_response.state == 'Running', ssvm_response
            return False, None

        res, ssvm_response = wait_until(3, 300, checkRunningState)
        if not res:
            self.fail("Failed to reach systemvm state to Running")
        return ssvm_response

    def destroy_cpvm(self):
        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            zoneid=self.zone.id
        )
        self.assertEqual(isinstance(list_cpvm_response, list), True, "Check list response returns a valid list")
        cpvm_response = list_cpvm_response[0]
        self.debug("Destroying CPVM: %s" % cpvm_response.id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = cpvm_response.id
        self.apiclient.destroySystemVm(cmd)
        self.checkForRunningSystemVM(cpvm_response, 'consoleproxy')

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_console_endpoint_change_websocket_traffic(self):
        cmd = listConfigurations.listConfigurationsCmd()
        cmd.name = "novnc.console.port"
        vncport = self.apiclient.listConfigurations(cmd)[0].value

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "novnc.console.port"
        updateConfigurationCmd.value = "8099"
        self.apiclient.updateConfiguration(updateConfigurationCmd)

        self.destroy_cpvm()

        cmd = createConsoleEndpoint.createConsoleEndpointCmd()
        cmd.virtualmachineid=self.vm1.id
        endpoint = self.apiclient.createConsoleEndpoint(cmd)

        if not endpoint:
            self.fail("Failed to get generate VM console endpoint")

        self.assertTrue(endpoint.success)
        self.assertEqual(endpoint.websocket.port, "8099")

        updateConfigurationCmd.value = vncport
        self.apiclient.updateConfiguration(updateConfigurationCmd)

        self.destroy_cpvm()

        endpoint = self.apiclient.createConsoleEndpoint(cmd)

        if not endpoint:
            self.fail("Failed to get generate VM console endpoint")

        self.assertTrue(endpoint.success)
        self.assertEqual(endpoint.websocket.port, vncport)