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


# this script will cover VMdeployment  with Userdata tests for MultiNic

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone,
                               list_virtual_machines)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
import base64
import random
import string

_multiprocess_shared_ = True


class TestDeployVmWithUserDataMultiNic(cloudstackTestCase):
    """Tests for UserData
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeployVmWithUserDataMultiNic, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.test_data = cls.testClient.getParsedTestDataConfig()

        # Get Domain, Zone, Template
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.test_data["ostype"]
        )
        if cls.zone.localstorageenabled:
            cls.storagetype = 'local'
            cls.test_data["service_offerings"][
                "tiny"]["storagetype"] = 'local'
        else:
            cls.storagetype = 'shared'
            cls.test_data["service_offerings"][
                "tiny"]["storagetype"] = 'shared'

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.test_data["service_offerings"]["tiny"]
        )

        # Create Network offering without userdata
        cls.network_offering_nouserdata = NetworkOffering.create(
            cls.api_client,
            cls.test_data["network_offering"]
        )
        # Enable Network offering
        cls.network_offering_nouserdata.update(cls.api_client, state='Enabled')

        # Create Network Offering with all the serices
        cls.network_offering_all = NetworkOffering.create(
            cls.api_client,
            cls.test_data["isolated_network_offering"]
        )
        # Enable Network offering
        cls.network_offering_all.update(cls.api_client, state='Enabled')

        cls._cleanup = [
            cls.service_offering,
            cls.network_offering_nouserdata,
            cls.network_offering_all
        ]

        # Generate userdata of 2500 bytes. This is larger than the 2048 bytes limit.
        # CS however allows for upto 4K bytes in the code. So this must succeed.
        # Overall, the query length must not exceed 4K, for then the json decoder
        # will fail this operation at the marvin client side itcls.

        cls.userdata = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2500))

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.test_data["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["simulator", "devcloud", "basic", "advanced"], required_hardware="false")
    def test_deployvm_multinic(self):
        """Test userdata update when non default nic is without userdata for deploy and update
        """

        self.userdata = base64.encodestring(self.userdata)

        network1 = Network.create(
            self.apiclient,
            self.test_data["isolated_network"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering_all.id,
            zoneid=self.zone.id
        )

        self.test_data["network_without_acl"]["netmask"] = "255.255.255.128"

        network2 = Network.create(
            self.apiclient,
            self.test_data["network_without_acl"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkofferingid=self.network_offering_nouserdata.id,
            gateway="10.2.1.1",
            zoneid=self.zone.id
        )

        deployVmResponse = VirtualMachine.create(
            self.apiclient,
            services=self.test_data["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network1.id), str(network2.id)],
            templateid=self.template.id,
            zoneid=self.zone.id
        )

        vms = list_virtual_machines(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id), "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")

        try:
            updateresponse = deployVmResponse.update(self.apiclient, userdata=self.userdata)
        except Exception as e:
            self.fail("Failed to update userdata: %s" % e)

        self.debug("virtual machine update response is: %s" % updateresponse)

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
