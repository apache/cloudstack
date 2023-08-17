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
""" BVT tests for Events Resource
"""
import json
import os
import tempfile
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request

from datetime import datetime

from marvin.cloudstackAPI import (listEvents)

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import SUCCESS, FAILED
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             NetworkOffering,
                             Network,
                             Domain,
                             Account,
                             Volume,
                             Host,
                             DiskOffering)
from marvin.lib.common import (get_domain,
                               get_suitable_test_template,
                               get_zone)

from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestEventsResource(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestEventsResource, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls._cleanup = []
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_01_events_resource(self):
        """Test Events resources while doing some operation on VM, volume, template, account, etc

        # Validate the following
        # 1. Note start time
        # 2. Deploy VM for a new account
        # 3. Check resource id and resource type of all concerned events during the period
        """
        now = datetime.now() # current date and time
        start_time = now.strftime("%Y-%m-%d %H:%M:%S")

        template = get_suitable_test_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"],
            self.hypervisor
        )
        if template == FAILED:
            self.fail("get_suitable_test_template() failed to return template with description %s" %
                      self.services["ostype"])

        self.disk_offering = DiskOffering.create(
            self.apiclient,
            self.services["disk_offering"]
        )
        self.cleanup.append(self.disk_offering)
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )
        self.cleanup.append(self.service_offering)
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
        )
        self.network_offering.update(self.apiclient, state='Enabled')
        self.services["network"]["networkoffering"] = self.network_offering.id
        self.cleanup.append(self.network_offering)
        self.services["zoneid"] = self.zone.id
        self.services["template"] = template.id
        self.services["network"]["zoneid"] = self.zone.id

        domain1 = Domain.create(self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain.id
        )
        self.services["domainid"] = domain1.id

        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=domain1.id
        )

        account_network = Network.create(
            self.apiclient,
            self.services["network"],
            account.name,
            account.domainid
        )
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services,
            accountid=account.name,
            domainid=account.domainid,
            networkids=account_network.id,
            serviceofferingid=self.service_offering.id
        )
        volume = Volume.create(
            self.apiclient,
            self.services,
            zoneid=self.zone.id,
            account=account.name,
            domainid=account.domainid,
            diskofferingid=self.disk_offering.id
        )
        virtual_machine.attach_volume(
            self.apiclient,
            volume
        )
        virtual_machine.stop(self.apiclient)
        account_network.restart(self.apiclient, cleanup=False)
        time.sleep(self.services["sleep"])
        virtual_machine.restore(self.apiclient)
        time.sleep(self.services["sleep"])
        virtual_machine.detach_volume(self.apiclient, volume)
        volume.delete(self.apiclient)
        ts = str(time.time())
        virtual_machine.update(self.apiclient, displayname=ts)
        virtual_machine.delete(self.apiclient)
        account_network.update(self.apiclient, name=account_network.name + ts)
        account_network.delete(self.apiclient)
        account.update(self.apiclient, newname=account.name + ts)
        account.disable(self.apiclient)
        account.delete(self.apiclient)
        domain1.delete(self.apiclient)

        cmd = listEvents.listEventsCmd()
        cmd.startdate = start_time
        cmd.listall = True
        events = self.apiclient.listEvents(cmd)
        self.assertEqual(
            isinstance(events, list),
            True,
            "List Events response was not a valid list"
        )
        self.assertNotEqual(
            len(events),
            0,
            "List Events returned an empty list"
        )

        for event in events:
            if event.type.startswith("VM.") or (event.type.startswith("NETWORK.") and not event.type.startswith("NETWORK.ELEMENT")) or event.type.startswith("VOLUME.") or event.type.startswith("ACCOUNT.") or event.type.startswith("DOMAIN.") or event.type.startswith("TEMPLATE."):
                if event.resourceid is None or event.resourcetype is None:
                    self.debug("Failed event:: %s" % json.dumps(event, indent=2))
                    self.fail("resourceid or resourcetype for the event not found!")
                else:
                    self.debug("Event %s at %s:: Resource Type: %s, Resource ID: %s" % (event.type, event.created, event.resourcetype, event.resourceid))

    def tearDown(self):
        super(TestEventsResource, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestEventsResource, cls).tearDownClass()
