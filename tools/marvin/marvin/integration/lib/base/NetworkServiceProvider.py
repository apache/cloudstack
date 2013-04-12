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
from marvin.cloudstackAPI import addNetworkServiceProvider
from marvin.cloudstackAPI import listNetworkServiceProviders
from marvin.cloudstackAPI import updateNetworkServiceProvider
from marvin.cloudstackAPI import deleteNetworkServiceProvider

class NetworkServiceProvider(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, physicalnetworkid, name, **kwargs):
        cmd = addNetworkServiceProvider.addNetworkServiceProviderCmd()
        cmd.id = self.id
        cmd.name = name
        cmd.physicalnetworkid = physicalnetworkid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        networkserviceprovider = apiclient.addNetworkServiceProvider(cmd)
        return networkserviceprovider


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        networkserviceprovider = apiclient.listNetworkServiceProviders(cmd)
        return map(lambda e: NetworkServiceProvider(e.__dict__), networkserviceprovider)


    def update(self, apiclient, **kwargs):
        cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        networkserviceprovider = apiclient.updateNetworkServiceProvider(cmd)
        return networkserviceprovider


    def delete(self, apiclient, **kwargs):
        cmd = deleteNetworkServiceProvider.deleteNetworkServiceProviderCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        networkserviceprovider = apiclient.deleteNetworkServiceProvider(cmd)
        return networkserviceprovider
