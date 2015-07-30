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
""" Test cases for Testing Max Hypervisor Limit
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Host
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines,
                               list_ssvms,
                               list_routers
                               )


from marvin.cloudstackAPI import (updateHypervisorCapabilities,
                                  listHypervisorCapabilities)

from marvin.codes import PASS


class TestMaxHyperviosrLimit(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestMaxHyperviosrLimit, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Get Zone, Domain and templates

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []
        try:
            cls.skiptest = False
            if cls.hypervisor.lower() not in ['xenserver']:
                cls.skiptest = True
                return

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                hosttags="host1"
            )

            cls._cleanup = [
                cls.account,
                cls.service_offering,
            ]
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.cleanup = []
        if self.skiptest:
            self.skipTest("This test is to be checked on xenserver \
                    only  Hence, skip for %s" % self.hypervisor)

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def tearDown(self):
        try:

            cmd = updateHypervisorCapabilities.updateHypervisorCapabilitiesCmd()
            cmd.id = self.hostCapId
            cmd.maxguestslimit = self.originalLimit
            self.apiclient.updateHypervisorCapabilities(cmd)

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_check_hypervisor_max_limit_effect(self):
        """ Test hypervisor max limits effect

        # 1. Read exsiting count of VM's on the host including SSVM and VR
                and modify maxguestcount accordingly
        # 2. Deploy a VM
        # 2. Try to deploy another vm
        # 3. Verify that second VM
                deployment fails (2 SSVMs 1 VR VM and 1 deployed VM)
        """

        hostList = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type="Routing")
        event_validation_result = validateList(hostList)
        self.assertEqual(
            event_validation_result[0],
            PASS,
            "host list validation failed due to %s" %
            event_validation_result[2])

        self.host = Host(hostList[0])
        Host.update(self.apiclient, id=self.host.id, hosttags="host1")

        # Step 1
        # List VM's , SSVM's and VR on selected host
        listVm = list_virtual_machines(self.apiclient,
                                       hostid=self.host.id)

        listssvm = list_ssvms(self.apiclient,
                              hostid=self.host.id)

        listvr = list_routers(self.apiclient,
                              hostid=self.host.id)

        newValue = 1
        if listVm is not None:
            newValue = len(listVm) + newValue

        if listssvm is not None:
            newValue = len(listssvm) + newValue

        if listvr is not None:
            newValue = len(listvr) + newValue

        qresultset = self.dbclient.execute(
            "select hypervisor_version from host where uuid='%s'" %
            self.host.id)

        event_validation_result = validateList(qresultset)
        self.assertEqual(
            event_validation_result[0],
            PASS,
            "event list validation failed due to %s" %
            event_validation_result[2])

        cmdList = listHypervisorCapabilities.listHypervisorCapabilitiesCmd()
        cmdList.hypervisor = self.hypervisor
        config = self.apiclient.listHypervisorCapabilities(cmdList)

        for host in config:
            if host.hypervisorversion == qresultset[0][0]:
                self.hostCapId = host.id
                self.originalLimit = host.maxguestslimit
                break
        else:
            self.skipTest("No hypervisor capabilities found for %s \
                    with version %s" % (self.hypervisor, qresultset[0][0]))

        cmdUpdate = updateHypervisorCapabilities.\
            updateHypervisorCapabilitiesCmd()
        cmdUpdate.id = self.hostCapId
        cmdUpdate.maxguestslimit = newValue
        self.apiclient.updateHypervisorCapabilities(cmdUpdate)

        # Step 2
        vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        self.cleanup.append(vm)
        # Step 3
        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.userapiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
            )
