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
""" BVT tests for resource alert rules and alert delivery
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Configurations,
                             Domain,
                             ResourceAlertRule,
                             ServiceOffering,
                             Tag,
                             VirtualMachine,
                             Webhook)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template)
from marvin.lib.utils import random_gen
from marvin.codes import FAILED
from nose.plugins.attrib import attr
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import logging
import socket
import time
import _thread

_multiprocess_shared_ = True
deliveries_received = []

# Rules are checked every resource.alert.evaluation.interval (60s by default) and VM stats every
# vm.stats.interval, so the first alert can take a few minutes after a rule is created.
ALERT_WAIT_SECONDS = 420
POLL_SECONDS = 15


class AlertReceiver(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers['Content-Length'])
        body = self.rfile.read(length).decode('utf-8')
        deliveries_received.append({'event': self.headers.get('X-CS-Event'), 'payload': body})
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'ok')

    def log_message(self, *args):
        pass


class TestResourceAlerts(cloudstackTestCase):

    original_config_values = {}

    @classmethod
    def setUpClass(cls):
        testClient = super(TestResourceAlerts, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.logger = logging.getLogger('TestResourceAlerts')
        cls.logger.setLevel(logging.DEBUG)
        cls._cleanup = []

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.root_domain = get_domain(cls.apiclient)
        cls.template = get_suitable_test_template(cls.apiclient, cls.zone.id, cls.services["ostype"], cls.hypervisor)
        if cls.template == FAILED:
            assert False, "get_suitable_test_template() failed to return template"
        cls.services["small"]["zoneid"] = cls.zone.id

        cls.start_receiver()
        cls.manage_configurations()

        cls.domain = Domain.create(cls.apiclient, cls.services["domain"], parentdomainid=cls.root_domain.id)
        cls._cleanup.append(cls.domain)
        cls.user1 = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.user1)
        cls.user2 = Account.create(cls.apiclient, cls.services["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.user2)
        cls.domain_admin = Account.create(cls.apiclient, cls.services["account"], admin=True, domainid=cls.domain.id)
        cls._cleanup.append(cls.domain_admin)
        cls.user1_api = testClient.getUserApiClient(UserName=cls.user1.name, DomainName=cls.domain.name)
        cls.user2_api = testClient.getUserApiClient(UserName=cls.user2.name, DomainName=cls.domain.name)
        cls.domain_admin_api = testClient.getUserApiClient(UserName=cls.domain_admin.name, DomainName=cls.domain.name,
                                                           type=2)

        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.services["service_offerings"]["tiny"])
        cls._cleanup.append(cls.service_offering)
        cls.vm1 = cls.deploy_vm(cls.user1)
        cls.vm2 = cls.deploy_vm(cls.user2)

    @classmethod
    def tearDownClass(cls):
        if cls.server:
            cls.server.socket.close()
        cls.manage_configurations(restore=True)
        super(TestResourceAlerts, cls).tearDownClass()

    @classmethod
    def deploy_vm(cls, account):
        vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            templateid=cls.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.zone.networktype
        )
        cls._cleanup.append(vm)
        return vm

    @classmethod
    def start_receiver(cls):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect((cls.mgtSvrDetails["mgtSvrIp"], cls.mgtSvrDetails["port"]))
        server_ip = s.getsockname()[0]
        s.close()
        s = socket.socket()
        s.bind(('', 0))
        port = s.getsockname()[1]
        s.close()
        cls.receiver_url = "http://%s:%d" % (server_ip, port)
        cls.server = HTTPServer(('0.0.0.0', port), AlertReceiver)
        _thread.start_new_thread(lambda server: server.serve_forever(), (cls.server,))
        cls.logger.debug("Alert receiver running at %s" % cls.receiver_url)

    @classmethod
    def manage_configurations(cls, restore=False):
        updates = {
            "webhook.delivery.allow.http": "true",
            "webhook.delivery.blocklist": "1.2.3.4/32"
        }
        if restore:
            for name, value in cls.original_config_values.items():
                if value is not None:
                    Configurations.update(cls.apiclient, name=name, value=value)
            cls.original_config_values.clear()
            return
        for name, value in updates.items():
            configs = Configurations.list(cls.apiclient, name=name)
            cls.original_config_values[name] = configs[0].value if configs else None
            Configurations.update(cls.apiclient, name=name, value=value)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestResourceAlerts, self).tearDown()

    def create_rule(self, apiclient, **kwargs):
        rule = ResourceAlertRule.create(apiclient, name="Test-" + random_gen(), **kwargs)
        self.cleanup.append(rule)
        return rule

    def forget(self, item):
        self.cleanup = [x for x in self.cleanup if x.id != item.id]

    def wait_for(self, check, what):
        waited = 0
        while waited < ALERT_WAIT_SECONDS:
            result = check()
            if result:
                return result
            time.sleep(POLL_SECONDS)
            waited += POLL_SECONDS
        self.fail("Timed out after %ds waiting for %s" % (ALERT_WAIT_SECONDS, what))

    def alerts_of(self, rule, apiclient=None):
        return rule.list_alerts(apiclient or self.apiclient, listall=True) or []

    def assertApiFails(self, fn, message):
        with self.assertRaises(Exception) as ctx:
            fn()
        self.assertIn(message, str(ctx.exception))

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="false")
    def test_01_rule_lifecycle(self):
        """Create, list, update, disable, enable and delete a rule on one VM"""
        rule = self.create_rule(self.apiclient, resourcetype="VirtualMachine", resourceid=self.vm1.id,
                                metric="CPU_UTILIZATION", condition="GT", threshold=80, severity="HIGH",
                                resetinterval=300)
        self.assertEqual(rule.resourceid, self.vm1.id)
        self.assertEqual(rule.resourcename, self.vm1.displayname)
        self.assertEqual(rule.resetinterval, 300)

        rules = ResourceAlertRule.list(self.apiclient, id=rule.id)
        self.assertEqual(len(rules), 1, "Rule should be listed")

        updated = rule.update(self.apiclient, threshold=90, severity="CRITICAL")
        self.assertEqual(updated.threshold, 90)
        self.assertEqual(updated.severity, "CRITICAL")
        self.assertEqual(updated.state, "Enabled")

        paused = rule.update(self.apiclient, state="Disabled")
        self.assertEqual(paused.state, "Disabled")
        self.assertEqual(rule.update(self.apiclient, state="Enabled").state, "Enabled")

        rule.delete(self.apiclient)
        self.forget(rule)
        self.assertIsNone(ResourceAlertRule.list(self.apiclient, id=rule.id), "Deleted rule should not be listed")

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="false")
    def test_02_invalid_input(self):
        """Bad thresholds are refused"""
        self.assertApiFails(lambda: self.create_rule(self.apiclient, resourcetype="VirtualMachine",
                                                     metric="CPU_UTILIZATION", condition="GT", threshold=150,
                                                     severity="LOW"),
                            "percentage")
        self.assertApiFails(lambda: self.create_rule(self.apiclient, resourcetype="VirtualMachine",
                                                     metric="NETWORK_READ_KBPS", condition="GT", threshold=-1,
                                                     severity="LOW"),
                            "zero or more")
        self.assertApiFails(lambda: self.create_rule(self.apiclient, resourcetype="Volume",
                                                     metric="CPU_UTILIZATION", condition="GT", threshold=50,
                                                     severity="LOW"),
                            "CPU_UTILIZATION")

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="false")
    def test_03_user_limits(self):
        """Users can only use their own VMs and Volumes, no hosts, pools or email"""
        self.assertApiFails(lambda: self.create_rule(self.user1_api, resourcetype="Host", metric="CPU_UTILIZATION",
                                                     condition="GT", threshold=90, severity="LOW"),
                            "Only root admins")
        self.assertApiFails(lambda: self.create_rule(self.user1_api, resourcetype="VirtualMachine",
                                                     metric="CPU_UTILIZATION", condition="GT", threshold=90,
                                                     severity="LOW", email=True),
                            "Only root admins")
        self.assertApiFails(lambda: self.create_rule(self.user1_api, resourcetype="VirtualMachine",
                                                     resourceid=self.vm2.id, metric="CPU_UTILIZATION",
                                                     condition="GT", threshold=90, severity="LOW"),
                            "permission")

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="false")
    def test_04_users_cannot_see_or_change_other_rules(self):
        """A user cannot list, update or delete another user's rule"""
        rule = self.create_rule(self.user1_api, resourcetype="VirtualMachine", resourceid=self.vm1.id,
                                metric="CPU_UTILIZATION", condition="GT", threshold=90, severity="LOW")
        self.assertIsNone(ResourceAlertRule.list(self.user2_api, id=rule.id), "Other user's rule must not be listed")
        self.assertApiFails(lambda: rule.update(self.user2_api, threshold=50), "permission")
        self.assertApiFails(lambda: rule.delete(self.user2_api), "permission")
        self.assertEqual(ResourceAlertRule.list(self.apiclient, id=rule.id)[0].threshold, 90)

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="false")
    def test_05_domain_admin_scope(self):
        """A domain admin sees rules in their domain but not root admin rules"""
        user_rule = self.create_rule(self.user1_api, resourcetype="VirtualMachine", resourceid=self.vm1.id,
                                     metric="CPU_UTILIZATION", condition="GT", threshold=90, severity="LOW")
        admin_rule = self.create_rule(self.apiclient, resourcetype="VirtualMachine", resourceid=self.vm1.id,
                                      metric="CPU_UTILIZATION", condition="GT", threshold=90, severity="LOW")
        ids = [r.id for r in (ResourceAlertRule.list(self.domain_admin_api, listall=True) or [])]
        self.assertIn(user_rule.id, ids)
        self.assertNotIn(admin_rule.id, ids)
        self.assertApiFails(lambda: self.create_rule(self.domain_admin_api, resourcetype="StoragePool",
                                                     metric="STORAGE_UTILIZATION", condition="GT", threshold=90,
                                                     severity="LOW"),
                            "Only root admins")

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="true")
    def test_06_alert_fires_and_is_delivered_to_webhook(self):
        """A firing rule saves an alert and sends it to its webhook"""
        webhook = Webhook.create(self.user1_api, name="Test-" + random_gen(), payloadurl=self.receiver_url)
        self.cleanup.append(webhook)
        rule = self.create_rule(self.user1_api, resourcetype="VirtualMachine", resourceid=self.vm1.id,
                                metric="CPU_UTILIZATION", condition="GTE", threshold=0, severity="LOW",
                                webhookids=webhook.id)
        self.assertEqual(rule.webhookids, [webhook.id])

        alerts = self.wait_for(lambda: self.alerts_of(rule, self.user1_api), "the rule to fire")
        self.assertEqual(alerts[0].resourceid, self.vm1.id)
        self.assertEqual(alerts[0].metrictype, "CPU_UTILIZATION")

        def received():
            for d in deliveries_received:
                if d['event'] == 'RESOURCE.ALERT' and json.loads(d['payload']).get('ruleid') == rule.id:
                    return json.loads(d['payload'])
            return None
        payload = self.wait_for(received, "the alert to reach the webhook")
        self.assertEqual(payload['resourceid'], self.vm1.id)
        self.assertEqual(payload["resourcename"], self.vm1.displayname)
        self.assertEqual(payload['severity'], "LOW")

        deliveries = webhook.list_deliveries(self.user1_api, eventtype="RESOURCE.ALERT") or []
        self.assertTrue(len(deliveries) > 0, "Alert delivery should be recorded on the webhook")
        self.assertTrue(deliveries[0].success, "Alert delivery should be successful")

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="true")
    def test_07_all_resources_rule_scope_and_opt_out(self):
        """An all-resources rule covers only what its owner can see, and skips opted out VMs"""
        user_rule = self.create_rule(self.user1_api, resourcetype="VirtualMachine", metric="NETWORK_WRITE_KBPS",
                                     condition="GTE", threshold=0, severity="LOW")
        Tag.create(self.apiclient, resourceIds=self.vm2.id, resourceType="UserVm",
                   tags={"resource.alert.opt.out": "true"})
        try:
            admin_rule = self.create_rule(self.apiclient, resourcetype="VirtualMachine",
                                          metric="NETWORK_WRITE_KBPS", condition="GTE", threshold=0, severity="LOW")
            self.wait_for(lambda: self.alerts_of(user_rule, self.user1_api), "the user rule to fire")
            self.wait_for(lambda: [a for a in self.alerts_of(admin_rule) if a.resourceid == self.vm1.id],
                          "the admin rule to fire on the user VM")
            self.assertEqual({a.resourceid for a in self.alerts_of(user_rule, self.user1_api)}, {self.vm1.id},
                             "A user rule must only cover the user's own VMs")
            self.assertNotIn(self.vm2.id, {a.resourceid for a in self.alerts_of(admin_rule)},
                             "An opted out VM must be skipped by all-resources rules")
        finally:
            Tag.delete(self.apiclient, resourceIds=self.vm2.id, resourceType="UserVm",
                       tags={"resource.alert.opt.out": "true"})

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="true")
    def test_08_deleting_rule_removes_its_alerts(self):
        """Deleting a rule also removes its alert history"""
        rule = self.create_rule(self.apiclient, resourcetype="VirtualMachine", resourceid=self.vm1.id,
                                metric="CPU_UTILIZATION", condition="GTE", threshold=0, severity="LOW")
        self.wait_for(lambda: self.alerts_of(rule), "the rule to fire")
        rule.delete(self.apiclient)
        self.forget(rule)
        left = ResourceAlertRule.list_alerts_for_resource(self.apiclient, "VirtualMachine", self.vm1.id,
                                                          listall=True) or []
        self.assertEqual([a for a in left if a.alertruleid == rule.id], [],
                         "Alerts of a deleted rule should be removed")

    @attr(tags=["advanced", "basic", "smoke"], required_hardware="true")
    def test_09_rule_removed_when_vm_expunged(self):
        """A rule on a VM goes away when the VM is expunged"""
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            templateid=self.template.id,
            accountid=self.user1.name,
            domainid=self.user1.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype
        )
        rule = self.create_rule(self.user1_api, resourcetype="VirtualMachine", resourceid=vm.id,
                                metric="CPU_UTILIZATION", condition="GT", threshold=99, severity="LOW")
        vm.delete(self.apiclient, expunge=True)
        self.wait_for(lambda: ResourceAlertRule.list(self.apiclient, id=rule.id) is None,
                      "the rule to be removed after the VM was expunged")
        self.forget(rule)
