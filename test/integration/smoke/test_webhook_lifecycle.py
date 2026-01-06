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
""" BVT tests for webhooks lifecycle functionalities
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listEvents)
from marvin.lib.base import (Account,
                             Domain,
                             Webhook,
                             SSHKeyPair)
from marvin.lib.common import (get_domain,
                               get_zone)
from marvin.lib.utils import (random_gen)
from marvin.cloudstackException import CloudstackAPIException
from nose.plugins.attrib import attr
import logging
# Import System modules
import time
from datetime import datetime


_multiprocess_shared_ = True
HTTP_PAYLOAD_URL = "http://smee.io/C9LPa7Ei3iB6Qj2"
HTTPS_PAYLOAD_URL = "https://smee.io/C9LPa7Ei3iB6Qj2"

class TestWebhooks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestWebhooks, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls._cleanup = []
        cls.logger = logging.getLogger('TestWebhooks')
        cls.logger.setLevel(logging.DEBUG)

    @classmethod
    def tearDownClass(cls):
        super(TestWebhooks, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []
        self.domain1 = Domain.create(
            self.apiclient,
            self.services["domain"])
        self.cleanup.append(self.domain1)

    def tearDown(self):
        super(TestWebhooks, self).tearDown()

    def popItemFromCleanup(self, item_id):
        for idx, x in enumerate(self.cleanup):
            if x.id == item_id:
                self.cleanup.pop(idx)
                break

    def createDomainAccount(self, isDomainAdmin=False):
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=isDomainAdmin,
            domainid=self.domain1.id)
        self.cleanup.append(self.account)
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )

    def runWebhookLifecycleTest(self, apiclient, scope=None, domainid=None, account=None, normaluser=None, payloadurl=None, description=None, sslverification=None, secretkey=None, state=None, isdelete=True):
        name = "Test-" + random_gen()
        if payloadurl is None:
            payloadurl = HTTP_PAYLOAD_URL
        self.webhook = Webhook.create(
            apiclient,
            name=name,
            payloadurl=payloadurl,
            description=description,
            scope=scope,
            sslverification=sslverification,
            secretkey=secretkey,
            state=state,
            domainid=domainid,
            account=account
        )
        self.cleanup.append(self.webhook)
        self.assertNotEqual(
            self.webhook,
            None,
            "Check webhook created"
        )
        webhook_id = self.webhook.id
        self.logger.debug("Created webhook: %s" % str(self.webhook.__dict__))
        self.assertEqual(
            name,
            self.webhook.name,
            "Check webhook name"
        )
        self.assertEqual(
            payloadurl,
            self.webhook.payloadurl,
            "Check webhook payloadurl"
        )
        if state is None:
            state = 'Enabled'
        self.assertEqual(
            state,
            self.webhook.state,
            "Check webhook state"
        )
        if scope is None or normaluser is not None:
            scope = 'Local'
        self.assertEqual(
            scope,
            self.webhook.scope,
            "Check webhook scope"
        )
        if sslverification is None:
            sslverification = False
        self.assertEqual(
            sslverification,
            self.webhook.sslverification,
            "Check webhook sslverification"
        )
        if domainid is not None:
            if normaluser is not None:
                domainid = normaluser.domainid
            self.assertEqual(
                domainid,
                self.webhook.domainid,
                "Check webhook domainid"
            )
        if account is not None:
            self.assertEqual(
                account,
                self.webhook.account,
                "Check webhook account"
            )
        if description is not None:
            self.assertEqual(
                description,
                self.webhook.description,
                "Check webhook description"
            )
        if secretkey is not None:
            self.assertEqual(
                secretkey,
                self.webhook.secretkey,
                "Check webhook secretkey"
            )
        list_webhook = Webhook.list(
            apiclient,
            id=webhook_id
        )
        self.assertNotEqual(
            list_webhook,
            None,
            "Check webhook list"
        )
        self.assertEqual(
            len(list_webhook),
            1,
            "Check webhook list length"
        )
        self.assertEqual(
            list_webhook[0].id,
            webhook_id,
            "Check webhook list item"
        )
        if isdelete == False:
            return
        self.webhook.delete(apiclient)
        self.popItemFromCleanup(webhook_id)
        list_webhook = Webhook.list(
            apiclient,
            id=webhook_id
        )
        self.assertTrue(
            list_webhook is None or len(list_webhook) == 0,
            "Check webhook list after delete"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_create_webhook_admin_local(self):
        self.runWebhookLifecycleTest(self.apiclient)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_create_webhook_admin_domain(self):
        self.runWebhookLifecycleTest(self.apiclient, 'Domain', self.domain1.id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_create_webhook_admin_global(self):
        self.runWebhookLifecycleTest(self.apiclient, 'Global')

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_create_webhook_domainadmin_local(self):
        self.createDomainAccount(True)
        self.runWebhookLifecycleTest(self.userapiclient)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_create_webhook_domainadmin_subdomain(self):
        self.createDomainAccount(True)
        self.domain11 = Domain.create(
            self.apiclient,
            self.services["domain"],
            parentdomainid=self.domain1.id)
        self.cleanup.append(self.domain11)
        self.runWebhookLifecycleTest(self.userapiclient, 'Domain', self.domain11.id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_create_webhook_domainadmin_global_negative(self):
        self.createDomainAccount(True)
        try:
            self.runWebhookLifecycleTest(self.userapiclient, 'Global')
        except CloudstackAPIException as e:
            self.assertTrue(
                "errorText:Scope Global can not be specified for owner" in str(e),
                "Check Global scope error check"
            )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_create_webhook_user_local(self):
        self.createDomainAccount()
        self.runWebhookLifecycleTest(self.userapiclient)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_08_create_webhook_user_domain(self):
        """For normal user scope will always be Local irrespective of the passed value
        """
        self.createDomainAccount()
        self.runWebhookLifecycleTest(self.userapiclient, 'Domain', self.domain1.id, normaluser=self.account)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_09_create_webhook_user_gloabl(self):
        """For normal user scope will always be Local irrespective of the passed value
        """
        self.createDomainAccount()
        self.runWebhookLifecycleTest(self.userapiclient, 'Global', normaluser=self.account)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_10_create_webhook_admin_advanced(self):
        self.createDomainAccount()
        self.runWebhookLifecycleTest(
            self.apiclient,
            payloadurl=HTTPS_PAYLOAD_URL,
            scope="Local",
            description="Webhook",
            sslverification=True,
            secretkey="webhook",
            state="Disabled",
            domainid=self.domain1.id,
            account=self.account.name
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_11_update_webhook(self):
        self.createDomainAccount()
        self.runWebhookLifecycleTest(self.userapiclient, isdelete=False)
        description = "Desc-" + random_gen()
        secretkey = random_gen()
        state = 'Disabled'
        updated_webhook = self.webhook.update(
            self.userapiclient,
            description=description,
            secretkey=secretkey,
            state=state
        )['webhook']
        self.assertNotEqual(
            updated_webhook,
            None,
            "Check updated webhook"
        )
        self.assertEqual(
            description,
            updated_webhook.description,
            "Check webhook description"
        )
        self.assertEqual(
            secretkey,
            updated_webhook.secretkey,
            "Check webhook secretkey"
        )
        self.assertEqual(
            state,
            updated_webhook.state,
            "Check webhook state"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_12_list_user_webhook_deliveries(self):
        self.createDomainAccount()
        self.runWebhookLifecycleTest(self.userapiclient, isdelete=False)
        now = datetime.now() # current date and time
        start_time = now.strftime("%Y-%m-%d %H:%M:%S")
        self.keypair = SSHKeyPair.register(
            self.userapiclient,
            name="Test-" + random_gen(),
            publickey="ssh-rsa: e6:9a:1e:b5:98:75:88:5d:56:bc:92:7b:43:48:05:b2"
        )
        self.logger.debug("Registered sshkeypair: %s" % str(self.keypair.__dict__))
        cmd = listEvents.listEventsCmd()
        cmd.startdate = start_time
        cmd.listall = True
        events = self.apiclient.listEvents(cmd)
        register_sshkeypair_event_count = 0
        if events is not None:
            for event in events:
                if event.type == "REGISTER.SSH.KEYPAIR":
                    register_sshkeypair_event_count = register_sshkeypair_event_count + 1
        time.sleep(5)
        list_deliveries = self.webhook.list_deliveries(
            self.userapiclient,
            page=1,
            pagesize=20
        )
        self.assertNotEqual(
            list_deliveries,
            None,
            "Check webhook deliveries list"
        )
        self.assertTrue(
            len(list_deliveries) > 0,
            "Check webhook deliveries list length"
        )
        register_sshkeypair_delivery_count = 0
        for delivery in list_deliveries:
            if delivery.eventtype == "REGISTER.SSH.KEYPAIR":
                register_sshkeypair_delivery_count = register_sshkeypair_delivery_count + 1
        self.assertEqual(
            register_sshkeypair_event_count,
            register_sshkeypair_delivery_count,
            "Check sshkeypair webhook deliveries count"
        )
        self.webhook.delete_deliveries(
            self.userapiclient
        )
        list_deliveries = self.webhook.list_deliveries(
            self.userapiclient
        )
        self.assertTrue(
            list_deliveries is None or len(list_deliveries) == 0,
            "Check webhook deliveries list after delete"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_13_webhook_execute_delivery(self):
        self.createDomainAccount()
        self.runWebhookLifecycleTest(self.userapiclient, isdelete=False)
        payload = "{ \"CloudStack\": \"Integration Test\" }"
        delivery = self.webhook.execute_delivery(
            self.userapiclient,
            payload=payload
        )
        self.assertNotEqual(
            delivery,
            None,
            "Check test webhook delivery"
        )
        self.assertEqual(
            self.webhook.id,
            delivery.webhookid,
            "Check test webhook delivery webhook"
        )
        self.assertEqual(
            payload,
            delivery.payload,
            "Check test webhook delivery payload"
        )
        self.assertEqual(
            self.webhook.id,
            delivery.webhookid,
            "Check test webhook delivery webhook"
        )
