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
from marvin.cloudstackAPI import createPhysicalNetwork
from marvin.cloudstackAPI import listPhysicalNetworks
from marvin.cloudstackAPI import updatePhysicalNetwork
from marvin.cloudstackAPI import deletePhysicalNetwork

class PhysicalNetwork(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createPhysicalNetwork.createPhysicalNetworkCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        physicalnetwork = apiclient.createPhysicalNetwork(cmd)
        return PhysicalNetwork(physicalnetwork.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listPhysicalNetworks.listPhysicalNetworksCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        physicalnetwork = apiclient.listPhysicalNetworks(cmd)
        return map(lambda e: PhysicalNetwork(e.__dict__), physicalnetwork)


    def update(self, apiclient, **kwargs):
        cmd = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        physicalnetwork = apiclient.updatePhysicalNetwork(cmd)
        return physicalnetwork


    def delete(self, apiclient, **kwargs):
        cmd = deletePhysicalNetwork.deletePhysicalNetworkCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        physicalnetwork = apiclient.deletePhysicalNetwork(cmd)
        return physicalnetwork
