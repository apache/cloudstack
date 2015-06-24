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
""" P1 tests for alert receiving from VR on service failure in VR
"""
# Import Local Modules
# import marvin
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (get_process_status,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Configurations)
from marvin.lib.common import (list_hosts,
                               list_routers,
                               get_zone,
                               get_domain,
                               get_template)
from nose.plugins.attrib import attr
from marvin.codes import FAILED
import time


_multiprocess_shared_ = True


class TestVRServiceFailureAlerting(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestVRServiceFailureAlerting,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        if template == FAILED:
            assert False, "get_template() failed to return template with \
                           description %s" % cls.services["ostype"]
        # Set Zones and disk offerings ??
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        # Create account, service offerings, vm.
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offerings"]["small"]
        )

        # create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(
            TestVRServiceFailureAlerting,
            cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        # Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_VRServiceFailureAlerting(self):

        if self.zone.networktype == "Basic":
            list_router_response = list_routers(
                self.apiclient,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )

        alertSubject = "Monitoring Service on VR " + router.name

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "service dnsmasq stop",
                hypervisor=self.hypervisor
            )
        else:
            try:
                hosts = list_hosts(
                    self.apiclient,
                    zoneid=router.zoneid,
                    type='Routing',
                    state='Up',
                    id=router.hostid
                )

                self.assertEqual(
                    isinstance(hosts, list),
                    True,
                    "Check list host returns a valid list"
                )

                host = hosts[0]
                result = get_process_status(
                    host.ipaddress,
                    22,
                    self.services["configurableData"]["host"]["username"],
                    self.services["configurableData"]["host"]["password"],
                    router.linklocalip,
                    "service apache2 stop"
                )

            except Exception as e:
                raise Exception("Exception raised in getting host\
                        credentials: %s " % e)

        res = str(result)
        self.debug("apache process status: %s" % res)

        configs = Configurations.list(
            self.apiclient,
            name='router.alerts.check.interval'
        )

        # Set the value for one more minute than
        # actual range to be on safer side
        waitingPeriod = (
            int(configs[0].value) + 60)  # in seconds

        time.sleep(waitingPeriod)
        # wait for (router.alerts.check.interval + 10) minutes meanwhile
        # monitor service on VR starts the apache service (
        # router.alerts.check.interval default value is
        # 30minutes)

        qresultset = self.dbclient.execute(
            "select id from alert where subject like\
                    '%{0}%' ORDER BY id DESC LIMIT 1;".format(
            str(alertSubject)))
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        return
