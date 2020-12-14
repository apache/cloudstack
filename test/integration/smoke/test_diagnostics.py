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
""" BVT tests for remote diagnostics of system VMs
"""
import urllib.request, urllib.parse, urllib.error

from marvin.cloudstackAPI import (runDiagnostics, getDiagnosticsData)
from marvin.cloudstackTestCase import cloudstackTestCase
# Import Local Modules
from marvin.codes import FAILED
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_test_template,
                               list_ssvms,
                               list_routers)
from nose.plugins.attrib import attr


class TestRemoteDiagnostics(cloudstackTestCase):
    """
    Test remote diagnostics with system VMs and VR as root admin
    """

    @classmethod
    def setUpClass(cls):

        testClient = super(TestRemoteDiagnostics, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            cls.fail("get_test_template() failed to return template")

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls._cleanup = []

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.vm_1 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.vm_1)

    @classmethod
    def tearDownClass(cls):
        super(TestRemoteDiagnostics,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []

    def tearDown(self):
        super(TestRemoteDiagnostics,self).tearDown()

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_01_ping_in_vr_success(self):
        '''
        Test Ping command execution in VR
        '''

        # Validate the following:
        # 1. Ping command is executed remotely on VR

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
        self.debug('Starting the router with ID: %s' % router.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = router.id
        cmd.ipaddress = '8.8.8.8'
        cmd.type = 'ping'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Ping in VR')

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_02_ping_in_vr_failure(self):
        '''
        Test Ping command execution in VR
        '''

        # Validate the following:
        # 1. Ping command is executed remotely on VR
        # 2. Validate Ping command execution with a non-existent/pingable IP address

        if self.hypervisor.lower() == 'simulator':
            raise self.skipTest("Skipping negative test case for Simulator hypervisor")

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
        self.debug('Starting the router with ID: %s' % router.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = router.id
        cmd.ipaddress = '192.0.2.2'
        cmd.type = 'ping'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertNotEqual(
            '0',
            cmd_response.exitcode,
            'Check diagnostics command returns a non-zero exit code')

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_03_ping_in_ssvm_success(self):
        '''
        Test Ping command execution in SSVM
        '''

        # Validate the following:
        # 1. Ping command is executed remotely on SSVM

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        ssvm = list_ssvm_response[0]

        self.debug('Setting up SSVM with ID %s' % ssvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = ssvm.id
        cmd.ipaddress = '8.8.8.8'
        cmd.type = 'ping'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Ping in SSVM'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_04_ping_in_ssvm_failure(self):
        '''
        Test Ping command execution in SSVM
        '''

        # Validate the following:
        # 1. Ping command is executed remotely on SSVM
        # 2. Validate Ping command execution with a non-existent/pingable IP address

        if self.hypervisor.lower() == 'simulator':
            raise self.skipTest("Skipping negative test case for Simulator hypervisor")

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        ssvm = list_ssvm_response[0]

        self.debug('Setting up SSVM with ID %s' % ssvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = ssvm.id
        cmd.ipaddress = '192.0.2.2'
        cmd.type = 'ping'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertNotEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Ping in SSVM'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_05_ping_in_cpvm_success(self):
        '''
        Test Ping command execution in CPVM
        '''

        # Validate the following:
        # 1. Ping command is executed remotely on CPVM

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        cpvm = list_ssvm_response[0]

        self.debug('Setting up CPVM with ID %s' % cpvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = cpvm.id
        cmd.ipaddress = '8.8.8.8'
        cmd.type = 'ping'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Ping in CPVM'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_06_ping_in_cpvm_failure(self):
        '''
        Test Ping command execution in CPVM
        '''

        # Validate the following:
        # 1. Ping command is executed remotely on CPVM
        # 2. Validate Ping command execution with a non-existent/pingable IP address

        if self.hypervisor.lower() == 'simulator':
            raise self.skipTest("Skipping negative test case for Simulator hypervisor")

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        cpvm = list_ssvm_response[0]

        self.debug('Setting up CPVM with ID %s' % cpvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = cpvm.id
        cmd.ipaddress = '192.0.2.2'
        cmd.type = 'ping'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertNotEqual(
            '0',
            cmd_response.exitcode,
            'Check diagnostics command returns a non-zero exit code'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_07_arping_in_vr(self):
        '''
        Test Arping command execution in VR
        '''

        # Validate the following:
        # 1. Arping command is executed remotely on VR

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
        self.debug('Starting the router with ID: %s' % router.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = router.id
        cmd.ipaddress = router.gateway
        cmd.type = 'arping'
        cmd.params = "-I eth2"
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Arping in VR')

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_08_arping_in_ssvm(self):
        '''
        Test Arping command execution in SSVM
        '''

        # Validate the following:
        # 1. Arping command is executed remotely on SSVM

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        ssvm = list_ssvm_response[0]

        self.debug('Setting up SSVM with ID %s' % ssvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = ssvm.id
        cmd.ipaddress = ssvm.gateway
        cmd.type = 'arping'
        cmd.params = '-I eth2'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Arping in SSVM'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_09_arping_in_cpvm(self):
        '''
        Test Arping command execution in CPVM
        '''

        # Validate the following:
        # 1. Arping command is executed remotely on CPVM

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        cpvm = list_cpvm_response[0]

        self.debug('Setting up CPVM with ID %s' % cpvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = cpvm.id
        cmd.ipaddress = cpvm.gateway
        cmd.type = 'arping'
        cmd.params = '-I eth2'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Arping in CPVM'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_10_traceroute_in_vr(self):
        '''
        Test Arping command execution in VR
        '''

        # Validate the following:
        # 1. Arping command is executed remotely on VR

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
        self.debug('Starting the router with ID: %s' % router.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = router.id
        cmd.ipaddress = '8.8.4.4'
        cmd.type = 'traceroute'
        cmd.params = "-m 10"
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Arping in VR')

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_11_traceroute_in_ssvm(self):
        '''
        Test Traceroute command execution in SSVM
        '''

        # Validate the following:
        # 1. Traceroute command is executed remotely on SSVM

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        ssvm = list_ssvm_response[0]

        self.debug('Setting up SSVM with ID %s' % ssvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = ssvm.id
        cmd.ipaddress = '8.8.4.4'
        cmd.type = 'traceroute'
        cmd.params = '-m 10'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Traceroute in SSVM'
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_12_traceroute_in_cpvm(self):
        '''
        Test Traceroute command execution in CPVMM
        '''

        # Validate the following:
        # 1. Traceroute command is executed remotely on CPVM

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        cpvm = list_cpvm_response[0]

        self.debug('Setting up CPVMM with ID %s' % cpvm.id)

        cmd = runDiagnostics.runDiagnosticsCmd()
        cmd.targetid = cpvm.id
        cmd.ipaddress = '8.8.4.4'
        cmd.type = 'traceroute'
        cmd.params = '-m 10'
        cmd_response = self.apiclient.runDiagnostics(cmd)

        self.assertEqual(
            '0',
            cmd_response.exitcode,
            'Failed to run remote Traceroute in CPVM'
        )

    '''
    Add Get Diagnostics data BVT
    '''

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_13_retrieve_vr_default_files(self):
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
        self.debug('Setting up VR with ID %s' % router.id)
        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = router.id

        response = self.apiclient.getDiagnosticsData(cmd)
        is_valid_url = self.check_url(response.url)

        self.assertEqual(
            True,
            is_valid_url,
            msg="Failed to create valid download url response"
        )

    def check_url(self, url):
        import urllib.request, urllib.error, urllib.parse
        try:
            r = urllib.request.urlopen(url)
            if r.code == 200:
                return True
        except urllib.error.HTTPError:
            return False
        except urllib.error.URLError:
            return False
        return True

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_14_retrieve_vr_one_file(self):
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
        self.debug('Setting up VR with ID %s' % router.id)
        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = router.id
        cmd.type = "/var/log/cloud.log"

        response = self.apiclient.getDiagnosticsData(cmd)

        is_valid_url = self.check_url(response.url)

        self.assertEqual(
            True,
            is_valid_url,
            msg="Failed to create valid download url response"
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_15_retrieve_ssvm_default_files(self):
        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        ssvm = list_ssvm_response[0]

        self.debug('Setting up SSVM with ID %s' % ssvm.id)

        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = ssvm.id

        response = self.apiclient.getDiagnosticsData(cmd)

        is_valid_url = self.check_url(response.url)

        self.assertEqual(
            True,
            is_valid_url,
            msg="Failed to create valid download url response"
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_16_retrieve_ssvm_single_file(self):
        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        ssvm = list_ssvm_response[0]

        self.debug('Setting up SSVM with ID %s' % ssvm.id)

        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = ssvm.id
        cmd.type = "/var/log/cloud.log"

        response = self.apiclient.getDiagnosticsData(cmd)

        is_valid_url = self.check_url(response.url)

        self.assertEqual(
            True,
            is_valid_url,
            msg="Failed to create valid download url response"
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_17_retrieve_cpvm_default_files(self):
        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        cpvm = list_cpvm_response[0]

        self.debug('Setting up CPVM with ID %s' % cpvm.id)

        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = cpvm.id

        response = self.apiclient.getDiagnosticsData(cmd)

        is_valid_url = self.check_url(response.url)

        self.assertEqual(
            True,
            is_valid_url,
            msg="Failed to create valid download url response"
        )

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="true")
    def test_18_retrieve_cpvm_single_file(self):
        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
        )

        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            'Check list response returns a valid list'
        )
        cpvm = list_cpvm_response[0]

        self.debug('Setting up CPVM with ID %s' % cpvm.id)

        cmd = getDiagnosticsData.getDiagnosticsDataCmd()
        cmd.targetid = cpvm.id
        cmd.type = "/var/log/cloud.log"

        response = self.apiclient.getDiagnosticsData(cmd)

        is_valid_url = self.check_url(response.url)

        self.assertEqual(
            True,
            is_valid_url,
            msg="Failed to create valid download url response"
        )
