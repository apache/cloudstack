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
from marvin.cloudstackAPI import createNetworkACL
from marvin.cloudstackAPI import listNetworkACLs
from marvin.cloudstackAPI import deleteNetworkACL

class NetworkACL(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, NetworkACLFactory, **kwargs):
        cmd = createNetworkACL.createNetworkACLCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in NetworkACLFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        networkacl = apiclient.createNetworkACL(cmd)
        return NetworkACL(networkacl.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listNetworkACLs.listNetworkACLsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        networkacl = apiclient.listNetworkACLs(cmd)
        return map(lambda e: NetworkACL(e.__dict__), networkacl)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteNetworkACL.deleteNetworkACLCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        networkacl = apiclient.deleteNetworkACL(cmd)
