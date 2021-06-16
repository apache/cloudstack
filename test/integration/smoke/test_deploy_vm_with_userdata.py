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

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (ServiceOffering,
                                         VirtualMachine,
                                         Account)
from marvin.lib.common import get_test_template, get_zone, list_virtual_machines
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
from marvin.codes import FAILED
import random
import string

class TestDeployVmWithUserData(cloudstackTestCase):
    """Tests for UserData
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVmWithUserData, cls).getClsTestClient()
        cls.apiClient = testClient.getApiClient() 
        cls.services = testClient.getParsedTestDataConfig()

        cls.zone = get_zone(cls.apiClient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.zone.localstorageenabled:
            #For devcloud since localstroage is enabled
            cls.services["service_offerings"]["tiny"]["storagetype"] = "local"
        cls.service_offering = ServiceOffering.create(
            cls.apiClient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.account = Account.create(cls.apiClient, services=cls.services["account"])
        cls.cleanup = [cls.account]
        cls.template = get_test_template(
            cls.apiClient,
            cls.zone.id,
            cls.hypervisor
        )

        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.debug("Successfully created account: %s, id: \
                   %s" % (cls.account.name,\
                          cls.account.id))


        # Generate userdata of 2500 bytes. This is larger than the 2048 bytes limit.
        # CS however allows for upto 4K bytes in the code. So this must succeed.
        # Overall, the query length must not exceed 4K, for then the json decoder
        # will fail this operation at the marvin client side itcls.
        user_data = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2500))
        cls.services["virtual_machine"]["userdata"] = user_data

    def setup(self):
        self.hypervisor = self.testClient.getHypervisorInfo()

    @attr(tags=["devcloud", "basic", "advanced", "post"], required_hardware="true")
    def test_deployvm_userdata_post(self):
        """Test userdata as POST, size > 2k
        """
        deployVmResponse = VirtualMachine.create(
            self.apiClient,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            method='POST'
        )
        vms = list_virtual_machines(
            self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id), "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="true")
    def test_deployvm_userdata(self):
        """Test userdata as GET, size > 2k
        """
        deployVmResponse = VirtualMachine.create(
            self.apiClient,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )
        vms = list_virtual_machines(
            self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id), "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.apiClient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
