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

from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr


class TestMigrationMaintainedPool(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestMigrationMaintainedPool, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        cls.pod = get_pod(cls.apiclient, cls.zone.id)

        template = get_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=domain.id
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.apiclient,
                                    cls.services["service_offerings"]["small"]
                                    )

        #create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                        cls.apiclient,
                                        cls.services["small"],
                                        accountid=cls.account.name,
                                        domainid=cls.account.domainid,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls._cleanup = [
                        cls.small_offering,
                        cls.virtual_machine,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestMigrationMaintainedPool, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.apiclient, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return


    @attr(tags=["advanced", "basic", "multipool", "storagemotion", "xenserver"], required_hardware="false")
    def test_02_migrate_volume_to_maintenance_pool(self):
            """
             Trying to migrate a volume to a pool in maintenance mode should fail
            """
            #List Available Storage pools
            storage_pools_response = list_storage_pools(
                                                        self.apiclient,
                                                        account=self.account.name,
                                                        domainid=self.account.domainid
                                                        )
            self.assertEqual(
                            isinstance(storage_pools_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                            len(storage_pools_response),
                            0,
                            "Check list Storage pools response"
                        )
            # Check if there are atleast two storage pools , else skip the test
            if len(storage_pools_response) < 2 :
                self.skipTest("Atleast two storage pools are need to test Storage migration")

            if self.hypervisor.lower() in ['kvm']:
                self.virtual_machine.stop(self.apiclient)

            list_volumes_response = list_volumes(
                                    self.apiclient,
                                    virtualmachineid=self.virtual_machine.id,
                                    listall=True
                                    )
            self.assertEqual(
                         isinstance(list_volumes_response, list),
                         True,
                         "Check list volumes response for valid list"
                        )
            self.assertNotEqual(
                        list_volumes_response,
                        None,
                        "Check if volume exists in ListVolumes"
                        )
            volume = list_volumes_response[0]

            # Ge the list of pools suitable for migration for the volume
            pools = StoragePool.listForMigration(
                                            self.apiclient,
                                            id=volume.id
                                    )
            self.assertEqual(
                         isinstance(pools, list),
                         True,
                         "Check eligible pools for migration returns a valid list"
                        )
            self.assertNotEqual(
                        len(pools),
                        0,
                        "Check if atleast one pool is suitable for migration"
                        )
            pool = pools[0]
            self.debug("Migrating Volume-ID: %s to Pool: %s which is in Maintenance mode" % (volume.id, pool.id))

            # Enable maintenance mode for one of the suitable pools
            StoragePool.enableMaintenance(self.apiclient,id=pool.id)

            # Trying to migrate volume should fail , which is caught here
            with self.assertRaises(Exception):
                    Volume.migrate(
                           self.apiclient,
                           volumeid=volume.id,
                           storageid=pool.id,
                           livemigrate='true'
                           )
            # Cancel the maintenance mode , so that the pool can be cleaned up in teardown
            StoragePool.cancelMaintenance(self.apiclient,id=pool.id)

            return
