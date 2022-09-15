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

"""
Test restore running VM on VMWare with one cluster having 2 Primary Storage
"""


from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             StoragePool
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               list_virtual_machines
                               )

from marvin.codes import CLUSTERTAG1, ROOT, PASS
import time


class TestRestoreVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestRestoreVM, cls).getClsTestClient()
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
            if cls.hypervisor.lower() not in ["vmware"]:
                cls.skiptest = True
                return

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)
            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            # Create Service offering
            cls.service_offering_cwps = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=CLUSTERTAG1
            )
            cls._cleanup.append(cls.service_offering_cwps)
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
            self.skipTest("This test is to be checked on VMWare only \
                    Hence, skip for %s" % self.hypervisor)

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def tearDown(self):
        try:
            if self.pools:
                StoragePool.update(
                    self.apiclient,
                    id=self.pools[0].id,
                    tags="")

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_recover_VM(self):
        """ Test Restore VM on VMWare
            1. Deploy a VM without datadisk
            2. Restore the VM
            3. Verify that VM comes up in Running state
        """
        try:
            self.pools = StoragePool.list(
                self.apiclient,
                zoneid=self.zone.id,
                scope="CLUSTER")

            status = validateList(self.pools)

            # Step 3
            self.assertEqual(
                status[0],
                PASS,
                "Check: Failed to list  cluster wide storage pools")

            if len(self.pools) < 2:
                self.skipTest("There must be at at least two cluster wide\
                storage pools available in the setup")

        except Exception as e:
            self.skipTest(e)

        # Adding tags to Storage Pools
        cluster_no = 1
        StoragePool.update(
            self.apiclient,
            id=self.pools[0].id,
            tags=[CLUSTERTAG1[:-1] + repr(cluster_no)])

        self.vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            templateid=self.template.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_cwps.id,
            zoneid=self.zone.id,
        )
        # Step 2

        volumes_root_list = list_volumes(
            self.apiclient,
            virtualmachineid=self.vm.id,
            type=ROOT,
            listall=True
        )

        root_volume = volumes_root_list[0]

        # Restore VM till its ROOT disk is recreated on onother Primary Storage
        while True:
            self.vm.restore(self.apiclient)
            volumes_root_list = list_volumes(
                self.apiclient,
                virtualmachineid=self.vm.id,
                type=ROOT,
                listall=True
            )

            root_volume = volumes_root_list[0]

            if root_volume.storage != self.pools[0].name:
                break

        # Step 3
        vm_list = list_virtual_machines(
            self.apiclient,
            id=self.vm.id)

        state = vm_list[0].state
        i = 0
        while(state != "Running"):
            vm_list = list_virtual_machines(
                self.apiclient,
                id=self.vm.id)

            time.sleep(10)
            i = i + 1
            state = vm_list[0].state
            if i >= 10:
                self.fail("Restore VM Failed")
                break

        return
