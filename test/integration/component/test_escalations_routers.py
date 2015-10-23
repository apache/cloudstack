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
from marvin.lib.utils import (get_process_status, validateList,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (list_hosts,
                               list_routers,
                               get_zone,
                               get_domain,
                               get_template)
from nose.plugins.attrib import attr
from marvin.codes import FAILED
from marvin.codes import PASS

_multiprocess_shared_ = True


class TestVR(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(
            TestVR,
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
        cls._cleanup.append(cls.small_offering)
        cls._cleanup.append(cls.account)
        return

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(
            TestVR,
            cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        # Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_FTPModulesInVR(self):
        """
        @desc: Verify FTP modules are loaded in VR of advance zone
        step1 : create a VR in advance zone
        step2: Verify FTP modules are there in created VR
        """
        if self.zone.networktype == "Basic":
            self.skipTest("This test can be run only in advance zone")

        # create a virtual machine
        vm = VirtualMachine.create(
            self.api_client,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"]
        )
        self.assertIsNotNone(vm, "Failed to deploy virtual machine")
        self.cleanup.append(vm)
        response = VirtualMachine.list(self.api_client, id=vm.id)
        status = validateList(response)
        self.assertEqual(
            status[0],
            PASS,
            "list vm response returned invalid list")
        list_router_response = list_routers(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        status = validateList(list_router_response)
        self.assertEqual(
            status[0], PASS, "Check list response returns a valid list")
        router = list_router_response[0]

        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "lsmod | grep ftp",
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
                    "lsmod | grep ftp"
                )

            except Exception as e:
                raise Exception("Exception raised in getting host\
                        credentials: %s " % e)

        res = str(result)
        self.debug("lsmod | grep ftp: %s" % res)
        if "nf_nat_ftp" in res and "nf_conntrack_ftp" in res:
            ismoduleinstalled = True
        else:
            ismoduleinstalled = False
        self.assertEqual(
            ismoduleinstalled,
            True,
            "nf_conntrack_ftp and nf_nat_ftp modules not installed on routers")
        return
