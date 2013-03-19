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
from marvin.cloudstackAPI import resetVpnConnection
from marvin.cloudstackAPI import createVpnConnection
from marvin.cloudstackAPI import listVpnConnections
from marvin.cloudstackAPI import deleteVpnConnection

class VpnConnection(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def reset(self, apiclient, id, **kwargs):
        cmd = resetVpnConnection.resetVpnConnectionCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpnconnection = apiclient.resetVpnConnection(cmd)


    @classmethod
    def create(cls, apiclient, VpnConnectionFactory, **kwargs):
        cmd = createVpnConnection.createVpnConnectionCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VpnConnectionFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpnconnection = apiclient.createVpnConnection(cmd)
        return VpnConnection(vpnconnection.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVpnConnections.listVpnConnectionsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpnconnection = apiclient.listVpnConnections(cmd)
        return map(lambda e: VpnConnection(e.__dict__), vpnconnection)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteVpnConnection.deleteVpnConnectionCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpnconnection = apiclient.deleteVpnConnection(cmd)
