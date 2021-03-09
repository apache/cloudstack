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
""" Test for storage.overprovisioning.factor update
"""
#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
#Import System modules

class TestUpdateOverProvision(cloudstackTestCase):
    """
    Test to update a storage.overprovisioning.factor
    """
    def setUp(self):
        self.apiClient = self.testClient.getApiClient()

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
    def test_UpdateStorageOverProvisioningFactor(self):
        """
        test update configuration setting at storage scope
        @return:
        """

        """ 1. list storagepools for id """
        """ 2. list overprovisioning factor for storage pool """
        """ 3. update setting for the pool"""
        """ 4. list overprovisioning factor for storage pool and assert"""

        """ list storagepools """
        storage_pools = StoragePool.list(
                                self.apiClient,
                                listall=True
                                )
        self.assertEqual(
                         isinstance(storage_pools, list),
                         True,
                         "List storage pools should not return empty response"
                         )

        if len(storage_pools) < 1:
            raise self.skipTest(
                            "The environment don't have storage pools required for test")

        for pool in storage_pools:
            if pool.type == "NetworkFilesystem" or pool.type == "VMFS":
                break
        if pool.type != "NetworkFilesystem" and pool.type != "VMFS":
            raise self.skipTest("Storage overprovisioning currently not supported on " + pool.type + " pools")

        self.poolId = pool.id
        """ list overprovisioning factor for storage pool """
        failed = 0
        if pool.overprovisionfactor is None:
            failed = 1
        self.assertNotEqual(failed,1,"pool.overprovisionfactor is none")
        factorOld = float(str(pool.overprovisionfactor))
        factorNew = str(factorOld + 1.0)

        """ update setting for the pool"""
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "storage.overprovisioning.factor"
        updateConfigurationCmd.value = factorNew
        updateConfigurationCmd.storageid = pool.id

        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)

        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))

        storage_pools = StoragePool.list(
                                self.apiClient,
                                id = self.poolId
                                )
        pool = storage_pools[0]
        failed = 0
        if pool.overprovisionfactor is None:
            failed = 1
        self.assertNotEqual(failed,1,"pool.overprovisionfactor is none")
        factorNew = float(str(pool.overprovisionfactor))
        self.assertNotEqual(int(factorNew), int(factorOld)," Check if overprovision factor of storage pool has changed")
        self.assertEqual(int(factorNew), int(factorOld + 1.0)," Check if overprovision factor of storage pool has increased by 1")

    def tearDown(self):
        """Reset the storage.overprovisioning.factor back to its original value
        @return:
        """
        storage_pools = StoragePool.list(
                                self.apiClient,
                                id = self.poolId
                                )
        pool = storage_pools[0]
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "storage.overprovisioning.factor"
        factorOld = 0
        if pool.overprovisionfactor is not None:
            factorOld = float(str(pool.overprovisionfactor))
        factorNew = (factorOld - 1.0)
        if factorNew > 0:
            updateConfigurationCmd.value = str(factorNew)
            updateConfigurationCmd.storageid = pool.id
            updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
