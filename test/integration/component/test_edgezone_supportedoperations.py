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
""" BVT tests for supported VM and network operations on an Edge zone
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (attachVolume,
                                  detachVolume,
                                  deleteVolume,
                                  attachIso,
                                  detachIso,
                                  deleteIso,
                                  startVirtualMachine,
                                  stopVirtualMachine,
                                  migrateVirtualMachineWithVolume)
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Host,
                             Pod,
                             StoragePool,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             Iso,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
# Import System modules
import time

_multiprocess_shared_ = True


class TestEdgeZoneSupportedOperations(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestVMMigration, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.cleanup = []
        cls.testsNotSupported = False
        if cls.zone.type != 'Edge':
            cls.testsNotSupported = True
            return
        cls.services["test_templates"]["kvm"]["directdownload"] = "true"
        cls.template = Template.register(cls.apiclient, cls.services["test_templates"]["kvm"],
                          zoneid=cls.zone.id, hypervisor=cls.hypervisor)
        cls.cleanup.append(cls.template)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["virtual_machine"]["hypervisor"] = cls.hypervisor
            cls.services["service_offerings"]["tiny"]["storagetype"] = "local"
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.cleanup.append(cls.service_offering)
        cls.l2_network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-l2_network_offering"],
        )
        cls.cleanup.append(cls.l2_network_offering)
        cls.l2_network_offering.update(cls.apiclient, state='Enabled')
        cls.domain = get_domain(cls.apiclient)
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=False,
            domainid=cls.domain.id
        )
        cls.cleanup.append(cls.account)

    @classmethod
    def tearDownClass(cls):
        super(TestMetrics, cls).tearDownClass()

    def setUp(self):
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )
        self.cleanup = []

    def tearDown(self):
        super(TestMetrics, self).tearDown()

    @skipTestIf("testsNotSupported")
    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_deploy_vm_l2network(self):
        """Test to deploy VM in a L2 network
        """
        network = Network.create(
            self.apiclient,
            self.services["l2-network"],
            zoneid=self.zone.id,
            networkofferingid=self.l2_network_offering.id
        )
        self.cleanup.append(network)
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.service_offering.id
            networkids=network.id
        )
        self.cleanup.append(vm)
        self.assertEqual(
            vm.state,
            "Running",
            "Check VM deployed in edge zone with a L2 network is running"
        )
