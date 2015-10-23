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

"""Test case for checking creation of two VM's with same name on VMWare"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Configurations
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               )

from marvin.sshClient import SshClient
import time


class TestSameVMName(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSameVMName, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        try:
            cls.skiptest = False

            if cls.hypervisor.lower() not in ['vmware']:
                cls.skiptest = True

            # Create an account
            cls.account_1 = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls.account_2 = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )

            # Create user api client of the account
            cls.userapiclient_1 = testClient.getUserApiClient(
                UserName=cls.account_1.name,
                DomainName=cls.account_1.domain
            )

            cls.userapiclient_2 = testClient.getUserApiClient(
                UserName=cls.account_2.name,
                DomainName=cls.account_2.domain
            )
            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )

            cls._cleanup = [
                cls.account_1,
                cls.account_2,
                cls.service_offering,
            ]
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def RestartServer(cls):
        """Restart management server"""

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        if self.skiptest:
            self.skipTest(
                "This test is to be checked on VMWare only  \
                        Hence, skip for %s" % self.hypervisor)

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"])
    def test_vms_with_same_name(self):
        """ Test vm deployment with same name

        # 1. Deploy a VM on with perticular name from account_1
        # 2. Try to deploy another vm with same name from account_2
        # 3. Verify that second VM deployment fails

        """
        # Step 1
        # Create VM on cluster wide
        configs = Configurations.list(
            self.apiclient,
            name="vm.instancename.flag")
        orig_value = configs[0].value

        if orig_value == "false":
            Configurations.update(self.apiclient,
                                  name="vm.instancename.flag",
                                  value="true"
                                  )

            # Restart management server
            self.RestartServer()
            time.sleep(120)

        self.testdata["small"]["displayname"] = "TestName"
        self.testdata["small"]["name"] = "TestName"
        VirtualMachine.create(
            self.userapiclient_1,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account_1.name,
            domainid=self.account_1.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.userapiclient_2,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=self.account_2.name,
                domainid=self.account_2.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id,
            )
        return
