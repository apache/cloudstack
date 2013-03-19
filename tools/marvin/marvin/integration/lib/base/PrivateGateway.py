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
from marvin.cloudstackAPI import createPrivateGateway
from marvin.cloudstackAPI import listPrivateGateways
from marvin.cloudstackAPI import deletePrivateGateway

class PrivateGateway(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, PrivateGatewayFactory, **kwargs):
        cmd = createPrivateGateway.createPrivateGatewayCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in PrivateGatewayFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        privategateway = apiclient.createPrivateGateway(cmd)
        return PrivateGateway(privategateway.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listPrivateGateways.listPrivateGatewaysCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        privategateway = apiclient.listPrivateGateways(cmd)
        return map(lambda e: PrivateGateway(e.__dict__), privategateway)


    def delete(self, apiclient, id, **kwargs):
        cmd = deletePrivateGateway.deletePrivateGatewayCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        privategateway = apiclient.deletePrivateGateway(cmd)
