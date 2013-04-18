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
from marvin.integration.lib.base import CloudStackEntity
from marvin.cloudstackAPI import createDiskOffering
from marvin.cloudstackAPI import listDiskOfferings
from marvin.cloudstackAPI import updateDiskOffering
from marvin.cloudstackAPI import deleteDiskOffering

class DiskOffering(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createDiskOffering.createDiskOfferingCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        diskoffering = apiclient.createDiskOffering(cmd)
        return DiskOffering(diskoffering.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listDiskOfferings.listDiskOfferingsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        diskoffering = apiclient.listDiskOfferings(cmd)
        return map(lambda e: DiskOffering(e.__dict__), diskoffering)


    def update(self, apiclient, **kwargs):
        cmd = updateDiskOffering.updateDiskOfferingCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        diskoffering = apiclient.updateDiskOffering(cmd)
        return diskoffering


    def delete(self, apiclient, **kwargs):
        cmd = deleteDiskOffering.deleteDiskOfferingCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        diskoffering = apiclient.deleteDiskOffering(cmd)
        return diskoffering
