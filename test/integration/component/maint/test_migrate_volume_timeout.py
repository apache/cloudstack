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
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Cluster,
                             StoragePool,
                             Volume,
                             DiskOffering,
                             Configurations,
                             Zone)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_hosts,
                               find_storage_pool_type
                               )
from marvin.codes import PASS
from marvin.sshClient import SshClient
from requests.exceptions import ConnectionError
import time


def restart_ms(self):
    """Restart MS
    #1-ssh into m/c running MS
    #2-restart ms
    #3-verify the response
    #4-loop until you get list_zone api answer """
    sshClient = SshClient(
        self.mgtSvrDetails["mgtSvrIp"],
        22,
        self.mgtSvrDetails["user"],
        self.mgtSvrDetails["passwd"]
    )
    command = "service cloudstack-management restart"
    ms_restart_response = sshClient.execute(command)
    self.assertEqual(
        validateList(ms_restart_response)[0],
        PASS,
        "Check the MS restart response")
    self.assertEqual(
        ms_restart_response[0],
        'Stopping cloudstack-management:[  OK  ]',
        "MS i not stopped"
    )
    self.assertEqual(
        ms_restart_response[1],
        'Starting cloudstack-management: [  OK  ]',
        "MS not started"
    )
    timeout = self.services["timeout"]
    while True:
        time.sleep(self.services["sleep"])
        try:
            list_response = Zone.list(
                self.api_client
            )
            if validateList(list_response)[0] == PASS:
                break
        except ConnectionError as e:
            self.debug("list zone response is not available due to  %s" % e)

        if timeout == 0:
            raise Exception("Ms is not comming up !")
        timeout = timeout - 1


class testMigrateVolumeTimeout(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(
                testMigrateVolumeTimeout,
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
            cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.find_storage_pool = True
            cls.clusterWithSufficientPool = None
            if cls.hypervisor.lower() == 'lxc':
                if not find_storage_pool_type(
                        cls.api_client, storagetype='rbd'):
                    cls.find_storage_pool = False
                    return
            clusters = Cluster.list(cls.api_client, zoneid=cls.zone.id)

            if not validateList(clusters)[0]:

                cls.debug(
                    "check list cluster response for zone id %s" %
                    cls.zone.id)

            for cluster in clusters:
                cls.pool = StoragePool.list(cls.api_client,
                                            clusterid=cluster.id,
                                            keyword="NetworkFilesystem"
                                            )

                if not validateList(cls.pool)[0]:

                    cls.debug(
                        "check list cluster response for zone id %s" %
                        cls.zone.id)

                if len(cls.pool) >= 2:
                    cls.clusterWithSufficientPool = cluster
                    break
            if not cls.clusterWithSufficientPool:
                return

            cls.services_off = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offerings"]["tiny"])
            cls.disk_off = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering_shared_5GB"]
            )
            cls._cleanup.append(cls.services_off)
            cls._cleanup.append(cls.disk_off)

        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if not self.clusterWithSufficientPool or not self.find_storage_pool:
            self.skipTest(
                "Suitable storage pool not found in any cluster for zone %s" %
                self.zone.id)
        self.account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)

    def tearDown(self):
        # Clean up, terminate the created resources
        Configurations.update(self.api_client,
                              name="migratewait",
                              value="3600")
        restart_ms(self)
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "advancedns",
                "basic", "eip", "simulator"])
    def test_migrate_volume_timeout(self):
        """Live migrate volume and check is migration is getting timed out """

        #steps:
        # 1: Update global settings migratewait to 10 sec
        # 2: Deploy vm
        # 3: Create volume and attach
        # 4: Live migrate volume to another storage

        Configurations.update(self.api_client,
                              name="migratewait",
                              value="10")
        restart_ms(self)

        host = list_hosts(
            self.api_client,
            clusterid=self.clusterWithSufficientPool.id)
        self.assertEqual(validateList(host)[0],
                         PASS,
                         "check list host response for cluster id %s"
                         % self.clusterWithSufficientPool.id)

        self.virtual_machine = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.services_off.id,
            hostid=host[0].id
        )

        vms = VirtualMachine.list(
            self.api_client,
            id=self.virtual_machine.id,
            listall=True,
        )

        self.assertEqual(
            validateList(vms)[0],
            PASS,
            "List VMs should return valid response for deployed VM"
        )

        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)

        # create data disk

        self.data_disk = Volume.create(
            self.api_client,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_off.id
        )

        self.virtual_machine.attach_volume(self.api_client,
                                           self.data_disk)
        self.list_data_volume = Volume.list(self.api_client,
                                            virtualmachineid=vm.id,
                                            type='DATA',
                                            account=self.account.name,
                                            domainid=self.account.domainid)

        self.assertEqual(validateList(self.list_data_volume)[0],
                         PASS,
                         "check list voume_response for vm id %s" % vm.id)

        self.pool_id = self.dbclient.execute(
            "select pool_id from volumes where uuid = '%s';"
            % self.list_data_volume[0].id)
        self.storageid = self.dbclient.execute(
            "select uuid from storage_pool where id = '%s';"
            % self.pool_id[0][0])
        pool_to_migrate = [
            pool.id for pool.id in [
                pool.id for pool in self.pool] if str(
                pool.id) != str(
                self.storageid[0][0])]

        with self.assertRaises(Exception):
            Volume.migrate(self.api_client,
                           storageid=pool_to_migrate[0],
                           volumeid=self.data_disk.id,
                           livemigrate='True')
