#!/usr/bin/env python
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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (enableStorageMaintenance,
                                  cancelStorageMaintenance
                                  )
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Cluster,
                             StoragePool,
                             Volume)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_hosts
                               )
from marvin.codes import PASS


class testHaPoolMaintenance(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(
                testHaPoolMaintenance,
                cls).getClsTestClient()
            cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                cls.api_client,
                cls.testClient.getZoneForTests())
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            cls.services['mode'] = cls.zone.networktype
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.clusterWithSufficientPool = None
            cls.listResponse = None
            clusters = Cluster.list(cls.api_client, zoneid=cls.zone.id)

            if not validateList(clusters)[0]:

                cls.debug(
                    "check list cluster response for zone id %s" %
                    cls.zone.id)
                cls.listResponse = True
                return

            for cluster in clusters:
                cls.pool = StoragePool.list(cls.api_client,
                                            clusterid=cluster.id,
                                            keyword="NetworkFilesystem"
                                            )

                if not validateList(cls.pool)[0]:

                    cls.debug(
                        "check list cluster response for zone id %s" %
                        cls.zone.id)
                    cls.listResponse = True
                    return

                if len(cls.pool) >= 2:
                    cls.clusterWithSufficientPool = cluster
                    break
            if not cls.clusterWithSufficientPool:
                return

            cls.services["service_offerings"][
                "tiny"]["offerha"] = "True"

            cls.services_off = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"])
            cls._cleanup.append(cls.services_off)

        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        if self.listResponse:
            self.fail(
                "Check list response")
        if not self.clusterWithSufficientPool:
            self.skipTest(
                "sufficient storage not available in any cluster for zone %s" %
                self.zone.id)
        self.account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

    def tearDown(self):
        # Clean up, terminate the created resources
        StoragePool.cancelMaintenance(self.api_client,
                                      id=self.storageid[0][0])
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "cl", "advancedns", "sg",
                "basic", "eip", "simulator", "multihost"])
    def test_ha_with_storage_maintenance(self):
        """put storage in maintenance mode and start ha vm and check usage"""
        # Steps
        # 1. Create a Compute service offering with the 'Offer HA' option
        # selected.
        # 2. Create a Guest VM with the compute service offering created above.
        # 3. put PS into maintenance  mode
        # 4. vm should go in stop state
        # 5. start vm ,vm should come up on another storage
        # 6. check usage events are getting generated for root disk

        host = list_hosts(
            self.api_client,
            clusterid=self.clusterWithSufficientPool.id)
        self.assertEqual(validateList(host)[0],
                         PASS,
                         "check list host response for cluster id %s"
                         % self.clusterWithSufficientPool.id)

        self.virtual_machine_with_ha = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.services_off.id,
            hostid=host[0].id
        )

        vms = VirtualMachine.list(
            self.api_client,
            id=self.virtual_machine_with_ha.id,
            listall=True,
        )

        self.assertEqual(
            validateList(vms)[0],
            PASS,
            "List VMs should return valid response for deployed VM"
        )

        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)

        # Put storage in maintenance  mode

        self.list_root_volume = Volume.list(self.api_client,
                                            virtualmachineid=vm.id,
                                            type='ROOT',
                                            account=self.account.name,
                                            domainid=self.account.domainid)

        self.assertEqual(validateList(self.list_root_volume)[0],
                         PASS,
                         "check list voume_response for vm id %s" % vm.id)

        self.pool_id = self.dbclient.execute(
            "select pool_id from volumes where uuid = '%s';"
            % self.list_root_volume[0].id)
        self.storageid = self.dbclient.execute(
            "select uuid from storage_pool where id = '%s';"
            % self.pool_id[0][0])

        StoragePool.enableMaintenance(self.api_client,
                                      id=self.storageid[0][0])

        self.virtual_machine_with_ha.start(self.api_client)
        self.events = self.dbclient.execute(
            "select type from usage_event where resource_name='%s';"
            % self.list_root_volume[0].name
        )
        self.assertEqual(len(self.events),
                         3,
                         "check the usage event table for root disk %s"
                         % self.list_root_volume[0].name
                         )

        self.assertEqual(str(self.events[0][0]),
                         "VOLUME.CREATE",
                         "check volume create events for volume %s"
                         % self.list_root_volume[0].name)
        self.assertEqual(str(self.events[1][0]),
                         "VOLUME.DELETE",
                         "check fvolume delete events for volume%s"
                         % self.list_root_volume[0].name)
        self.assertEqual(str(self.events[2][0]),
                         "VOLUME.CREATE",
                         "check volume create events for volume %s"
                         % self.list_root_volume[0].name)
