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
from marvin.cloudstackAPI import createServiceOffering
from marvin.cloudstackAPI import listServiceOfferings
from marvin.cloudstackAPI import updateServiceOffering
from marvin.cloudstackAPI import deleteServiceOffering

class ServiceOffering(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createServiceOffering.createServiceOfferingCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        serviceoffering = apiclient.createServiceOffering(cmd)
        return ServiceOffering(serviceoffering.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listServiceOfferings.listServiceOfferingsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        serviceoffering = apiclient.listServiceOfferings(cmd)
        return map(lambda e: ServiceOffering(e.__dict__), serviceoffering)


    def update(self, apiclient, **kwargs):
        cmd = updateServiceOffering.updateServiceOfferingCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        serviceoffering = apiclient.updateServiceOffering(cmd)
        return serviceoffering


    def delete(self, apiclient, **kwargs):
        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        serviceoffering = apiclient.deleteServiceOffering(cmd)
        return serviceoffering
