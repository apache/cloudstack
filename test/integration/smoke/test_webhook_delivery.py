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
""" BVT tests for webhooks delivery with a basic server
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain,
                             Webhook,
                             SSHKeyPair)
from marvin.lib.common import (get_domain,
                               get_zone)
from marvin.lib.utils import (random_gen)
from marvin.cloudstackException import CloudstackAPIException
from nose.plugins.attrib import attr
from http.server import BaseHTTPRequestHandler, HTTPServer
import logging
# Import System modules
import time
import json
import socket
import _thread


_multiprocess_shared_ = True
deliveries_received = []

class WebhookReceiver(BaseHTTPRequestHandler):
    """
        WebhookReceiver class to receive webhook events
    """
    def _set_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        post_data = post_data.decode('utf-8')
        event_id = self.headers.get('X-CS-Event-ID')
        print("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n" %
                (str(self.path), str(self.headers), post_data))
        self._set_response()
        global deliveries_received
        if deliveries_received is None:
            deliveries_received = []
        deliveries_received.append({'event': event_id, 'payload': post_data})
        if event_id != None:
            self.wfile.write("Event with ID: {} successfully processed!".format(str(event_id)).encode('utf-8'))
        else:
            self.wfile.write("POST request for {}".format(self.path).encode('utf-8'))

class TestWebhookDelivery(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestWebhookDelivery, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.logger = logging.getLogger('TestWebhookDelivery')
        cls.logger.setLevel(logging.DEBUG)

        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect((cls.mgtSvrDetails["mgtSvrIp"], cls.mgtSvrDetails["port"]))
        cls.server_ip = s.getsockname()[0]
        s.close()
        if cls.server_ip == "127.0.0.1":
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            cls.server_ip = s.getsockname()[0]
            s.close()
        # use random port for webhookreceiver server
        s = socket.socket()
        s.bind(('', 0))
        cls.server_port = s.getsockname()[1]
        s.close()
        cls.webhook_receiver_url = "http://" + cls.server_ip + ":" + str(cls.server_port)
        cls.logger.debug("Running Webhook receiver @ %s" % cls.webhook_receiver_url)
        def startMgmtServer(tname, server):
            cls.logger.debug("Starting WebhookReceiver")
            try:
                server.serve_forever()
            except Exception: pass
        cls.server = HTTPServer(('0.0.0.0', cls.server_port), WebhookReceiver)
        _thread.start_new_thread(startMgmtServer, ("webhook-receiver", cls.server,))

        cls._cleanup = []

    @classmethod
    def tearDownClass(cls):
        if cls.server:
            cls.server.socket.close()
        global deliveries_received
        deliveries_received = []
        super(TestWebhookDelivery, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []
        self.domain1 = Domain.create(
            self.apiclient,
            self.services["domain"])
        self.cleanup.append(self.domain1)

    def tearDown(self):
        super(TestWebhookDelivery, self).tearDown()

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

    def createWebhook(self, apiclient, scope=None, domainid=None, account=None, payloadurl=None, description=None, sslverification=None, secretkey=None, state=None):
        name = "Test-" + random_gen()
        if payloadurl is None:
            payloadurl = self.webhook_receiver_url
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

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_webhook_deliveries(self):
        global deliveries_received
        self.createDomainAccount()
        self.createWebhook(self.userapiclient)
        self.keypair = SSHKeyPair.register(
            self.userapiclient,
            name="Test-" + random_gen(),
            publickey="ssh-rsa: e6:9a:1e:b5:98:75:88:5d:56:bc:92:7b:43:48:05:b2"
        )
        self.logger.debug("Registered sshkeypair: %s" % str(self.keypair.__dict__))
        time.sleep(2)
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
        for delivery in list_deliveries:
            self.assertEqual(
                delivery.success,
                True,
                "Check webhook delivery success"
            )
            self.assertEqual(
                delivery.response,
                ("Event with ID: %s successfully processed!" % delivery.eventid),
                "Check webhook delivery response"
            )
            delivery_matched = False
            for received in deliveries_received:
                if received['event'] == delivery.eventid:
                    self.assertEqual(
                        delivery.payload,
                        received['payload'],
                        "Check webhook delivery payload"
                    )
                    delivery_matched = True
            self.assertTrue(
                delivery_matched,
                "Delivery for %s did not match with server" % delivery.id
            )
