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
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.codes import FAILED
import time


_multiprocess_shared_ = True

class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {

            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended in create account to
                # ensure unique username generated each time
                "password": "password",
                },
            "small":
            # Create a small virtual machine instance with disk offering
                {
                    "displayname": "testserver",
                    "username": "root", # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                    },
            "service_offerings":
                {
                    "small":
                        {
                            # Small service offering ID to for change VM
                            # service offering from medium to small
                            "name": "SmallInstance",
                            "displaytext": "SmallInstance",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 256,
                            },
                    "big":
                        {
                            # Big service offering ID to for change VM
                            "name": "BigInstance",
                            "displaytext": "BigInstance",
                            "cpunumber": 1,
                            "cpuspeed": 100,
                            "memory": 512,
                            }
                },
            #Change this
            "template": {
                "displaytext": "xs",
                "name": "xs",
                "passwordenabled": False,
                },
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }


class TestVRServiceFailureAlerting(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVRServiceFailureAlerting, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services

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
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]
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

        #create a virtual machine
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
        cls.api_client = super(TestVRServiceFailureAlerting, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(hypervisor="xenserver")
    @attr(tags=["advanced", "basic"])
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
                        "service dnsmasq status",
                        hypervisor=self.hypervisor
                        )
        else:
            try:
                host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                result = get_process_status(
                            host.ipaddress,
                            22,
                            host.user,
                            host.passwd,
                            router.linklocalip,
                            "service apache2 stop"
                            )
            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check router services")

        res = str(result)
        self.debug("apache process status: %s" % res)

        time.sleep(2400) #wait for 40 minutes meanwhile monitor service on VR starts the apache service (router.alerts.check.interval default value is 30minutes)

        qresultset = self.dbclient.execute(
            "select id from alert where subject = '%s' ORDER BY id DESC LIMIT 1;" \
            % str(alertSubject)
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        return
