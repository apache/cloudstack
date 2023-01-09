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

from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
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
