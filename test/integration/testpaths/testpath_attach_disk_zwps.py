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
""" Test case for Data Disk Attach to VM on ZWPS Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             Volume,
                             VirtualMachine,
                             StoragePool
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template
                               )

from marvin.codes import (PASS,
                          ZONETAG1)


class TestAttachDataDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestAttachDataDisk, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls._cleanup = []
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])
        cls.skiptest = False

        try:
            cls.pools = StoragePool.list(cls.apiclient, zoneid=cls.zone.id)
        except Exception as e:
            cls.skiptest = True
            return
        try:

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
            cls.service_offering_zone1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                tags=ZONETAG1
            )
            cls._cleanup.append(cls.service_offering_zone1)

            # Create Disk offering
            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"]
            )

            cls._cleanup.append(cls.disk_offering)

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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            for storagePool in self.pools:
                StoragePool.update(self.apiclient, id=storagePool.id, tags="")

            if hasattr(self, "data_volume_created"):
                data_volumes_list = Volume.list(
                    self.userapiclient,
                    id=self.data_volume_created.id,
                    virtualmachineid=self.vm.id
                )
                if data_volumes_list:
                    self.vm.detach_volume(
                        self.userapiclient,
                        data_volumes_list[0]
                    )

                status = validateList(data_volumes_list)
                self.assertEqual(
                    status[0],
                    PASS,
                    "DATA Volume List Validation Failed")

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced"], required_hardware="true")
    def test_01_attach_datadisk_to_vm_on_zwps(self):
        """ Attach Data Disk To VM on ZWPS
            1.  Check if zwps storage pool exists.
            2.  Adding tag to zone wide primary storage
            3.  Launch a VM on ZWPS
            4.  Attach data disk to vm which is on zwps.
            5.  Verify disk is attached.
        """

        # Step 1
        if len(list(storagePool for storagePool in self.pools
                    if storagePool.scope == "ZONE")) < 1:
            self.skipTest("There must be at least one zone wide \
                storage pools available in the setup")

        # Adding tags to Storage Pools
        zone_no = 1
        for storagePool in self.pools:
            if storagePool.scope == "ZONE":
                StoragePool.update(
                    self.apiclient,
                    id=storagePool.id,
                    tags=[ZONETAG1[:-1] + repr(zone_no)])
                zone_no += 1

        self.vm = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_zone1.id,
            zoneid=self.zone.id
        )

        self.data_volume_created = Volume.create(
            self.userapiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )

        self.cleanup.append(self.data_volume_created)

        # Step 2
        self.vm.attach_volume(
            self.userapiclient,
            self.data_volume_created
        )

        data_volumes_list = Volume.list(
            self.userapiclient,
            id=self.data_volume_created.id,
            virtualmachineid=self.vm.id
        )

        data_volume = data_volumes_list[0]

        status = validateList(data_volume)

        # Step 3
        self.assertEqual(
            status[0],
            PASS,
            "Check: Data if Disk is attached to VM")

        return
