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
from marvin.base import CloudStackEntity
from marvin.cloudstackAPI import createStoragePool
from marvin.cloudstackAPI import listStoragePools
from marvin.cloudstackAPI import updateStoragePool
from marvin.cloudstackAPI import deleteStoragePool
from marvin.cloudstackAPI import cancelStorageMaintenance
from marvin.cloudstackAPI import enableStorageMaintenance

class StoragePool(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createStoragePool.createStoragePoolCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        storagepool = apiclient.createStoragePool(cmd)
        return StoragePool(storagepool.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listStoragePools.listStoragePoolsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        storagepool = apiclient.listStoragePools(cmd)
        return map(lambda e: StoragePool(e.__dict__), storagepool)


    def update(self, apiclient, **kwargs):
        cmd = updateStoragePool.updateStoragePoolCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        storagepool = apiclient.updateStoragePool(cmd)
        return storagepool


    def delete(self, apiclient, **kwargs):
        cmd = deleteStoragePool.deleteStoragePoolCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        storagepool = apiclient.deleteStoragePool(cmd)
        return storagepool


    def cancel_maintenance(self, apiclient, **kwargs):
        cmd = cancelStorageMaintenance.cancelStorageMaintenanceCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        storagemaintenance = apiclient.cancelStorageMaintenance(cmd)
        return storagemaintenance


    def enable_maintenance(self, apiclient, **kwargs):
        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        storagemaintenance = apiclient.enableStorageMaintenance(cmd)
        return storagemaintenance
