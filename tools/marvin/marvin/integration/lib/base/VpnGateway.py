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
from marvin.cloudstackAPI import createVpnGateway
from marvin.cloudstackAPI import listVpnGateways
from marvin.cloudstackAPI import deleteVpnGateway

class VpnGateway(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, VpnGatewayFactory, **kwargs):
        cmd = createVpnGateway.createVpnGatewayCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VpnGatewayFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpngateway = apiclient.createVpnGateway(cmd)
        return VpnGateway(vpngateway.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVpnGateways.listVpnGatewaysCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpngateway = apiclient.listVpnGateways(cmd)
        return map(lambda e: VpnGateway(e.__dict__), vpngateway)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteVpnGateway.deleteVpnGatewayCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpngateway = apiclient.deleteVpnGateway(cmd)
