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
""" BVT tests for extension custom actions lifecycle functionalities
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import listManagementServers
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import (Extension,
                             ExtensionCustomAction)
from marvin.lib.common import (get_zone)
from marvin.lib.utils import (random_gen)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
# Import System modules
import logging
import random
import string
import time

_multiprocess_shared_ = True

class TestExtensions(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestExtensions, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        cls._cleanup = []
        cls.logger = logging.getLogger('TestExtensions')
        cls.logger.setLevel(logging.DEBUG)

        cls.resource_name_suffix = random_gen()
        cls.extension = Extension.create(
            cls.apiclient,
            name=f"ext-{cls.resource_name_suffix}",
            type='Orchestrator'
        )
        cls._cleanup.append(cls.extension)

    @classmethod
    def tearDownClass(cls):
        super(TestExtensions, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestExtensions, self).tearDown()

    def popItemFromCleanup(self, item_id):
        for idx, x in enumerate(self.cleanup):
            if x.id == item_id:
                self.cleanup.pop(idx)
                break

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_extension_create_custom_action(self):
        name = random_gen()
        details = [{}]
        details[0]['abc'] = 'xyz'
        parameters = [{}]
        parameter0 = {
            'name': 'Name',
            'type': 'STRING',
            'validationformat': 'NONE',
            'required': True
        }
        parameters[0] = parameter0
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=name,
            details=details,
            parameters=parameters
        )
        self.cleanup.append(self.custom_action)
        self.assertEqual(
            name,
            self.custom_action.name,
            "Check custom action name failed"
        )
        self.assertEqual(
            self.extension.id,
            self.custom_action.extensionid,
            "Check custom action extension ID failed"
        )
        self.assertEqual(
            'VirtualMachine',
            self.custom_action.resourcetype,
            "Check custom action resorucetype failed"
        )
        self.assertFalse(
            self.custom_action.enabled,
            "Check custom action enabled failed"
        )
        self.assertTrue(
            self.custom_action.allowedroletypes is not None and len(self.custom_action.allowedroletypes) == 1 and self.custom_action.allowedroletypes[0] == 'Admin',
            "Check custom action allowedroletypes failed"
        )
        self.assertEqual(
            3,
            self.custom_action.timeout,
            "Check custom action timeout failed"
        )
        action_details = self.custom_action.details.__dict__
        for k, v in details[0].items():
            self.assertIn(k, action_details, f"Key '{k}' should be present in details")
            self.assertEqual(v, action_details[k], f"Value for key '{k}' should be '{v}'")
        self.assertTrue(
            self.custom_action.parameters is not None and len(self.custom_action.parameters) == 1,
            "Check custom action parameters count failed"
        )
        parameter_result = self.custom_action.parameters[0].__dict__
        for k, v in parameter0.items():
            self.assertIn(k, parameter_result, f"Key '{k}' should be present in the parameter")
            self.assertEqual(v, parameter_result[k], f"Parameter property for key '{k}' should be '{v}'")

        actions = self.extension.list_custom_actions(self.apiclient)
        self.assertTrue(
            actions is not None and len(actions) == 1 and actions[0].id == self.custom_action.id,
            "Check extension custom actions failed"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_extension_create_custom_action_name_fail(self):
        name = random_gen()
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=name
        )
        try:
            self.custom_action1 = ExtensionCustomAction.create(
                self.apiclient,
                extensionid=self.extension.id,
                name=name
            )
            self.cleanup.append(self.custom_action1)
            self.fail(f"Same name: {name} custom action created twice")
        except CloudstackAPIException: pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_extension_create_custom_action_resourcetype_fail(self):
        try:
            self.custom_action = ExtensionCustomAction.create(
                self.apiclient,
                extensionid=self.extension.id,
                name=random_gen(),
                resourcetype='Host'
            )
            self.cleanup.append(self.custom_action)
            self.fail(f"Unknown resourcetype: {type} custom action created")
        except CloudstackAPIException: pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_extension_update_custom_action(self):
        details = [{}]
        details[0]['abc'] = 'xyz'
        parameters = [{}]
        parameter0 = {
            'name': 'Name',
            'type': 'STRING',
            'validationformat': 'NONE',
            'required': True
        }
        parameters[0] = parameter0
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=random_gen(),
            details=details,
            parameters=parameters
        )
        self.cleanup.append(self.custom_action)

        details[0]['bca'] = 'yzx'
        description = 'Updated test description'
        parameter1 = {
            'name': 'COUNT',
            'type': 'NUMBER',
            'validationformat': 'NONE',
            'required': False,
            'valueoptions': '10,100,1000'
        }
        parameters.append(parameter1)
        updated_action = self.custom_action.update(
            self.apiclient,
            description=description,
            enabled=True,
            details=details,
            parameters=parameters)['extensioncustomaction']
        self.assertTrue(
            updated_action.enabled,
            "Check custom action enabled failed"
        )
        self.assertEqual(
            description,
            updated_action.description,
            "Check custom action description failed"
        )
        action_details = updated_action.details.__dict__
        for k, v in details[0].items():
            self.assertIn(k, action_details, f"Key '{k}' should be present in details")
            self.assertEqual(v, action_details[k], f"Value for key '{k}' should be '{v}'")
        self.assertTrue(
            updated_action.parameters is not None and len(updated_action.parameters) == 2,
            "Check custom action parameters count failed"
        )
        for idx, param in enumerate(parameters):
            parameter_result = updated_action.parameters[idx].__dict__
            for k, v in param.items():
                self.assertIn(k, parameter_result, f"Key '{k}' should be present in the parameter")
                actual = ','.join(str(x) for x in parameter_result[k]) if k == 'valueoptions' else parameter_result[k]
                self.assertEqual(v, actual, f"Parameter property for key '{k}' should be '{v}'")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_extension_run_custom_action_invalid_resource_type_fail(self):
        name = random_gen()
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=name,
            enabled=True
        )
        self.cleanup.append(self.custom_action)
        try:
            self.custom_action.run(
                self.apiclient,
                resourcetype='Host',
                resourceid='abcd'
            )
            self.fail(f"Invalid resource custom action: {name} ran successfully")
        except Exception as e:
            msg = str(e)
            if msg.startswith("Job failed") == False or "Internal error running action" not in msg == False:
                self.fail(f"Unknown exception occurred: {e}")
            pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_extension_run_custom_action_fail(self):
        name = random_gen()
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=name,
            enabled=True
        )
        self.cleanup.append(self.custom_action)
        try:
            self.custom_action.run(
                self.apiclient,
                resourceid='abcd'
            )
            self.fail(f"Invalid resource custom action: {name} ran successfully")
        except Exception as e:
            msg = str(e)
            if msg.startswith("Job failed") == False or "Invalid action" not in msg == False:
                self.fail(f"Unknown exception occurred: {e}")
            pass
