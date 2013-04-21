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
from marvin.cloudstackAPI import addVpnUser
from marvin.cloudstackAPI import listVpnUsers
from marvin.cloudstackAPI import removeVpnUser

class VpnUser(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, username, password, **kwargs):
        cmd = addVpnUser.addVpnUserCmd()
        cmd.id = self.id
        cmd.password = password
        cmd.username = username
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpnuser = apiclient.addVpnUser(cmd)
        return vpnuser


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVpnUsers.listVpnUsersCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpnuser = apiclient.listVpnUsers(cmd)
        return map(lambda e: VpnUser(e.__dict__), vpnuser)


    def remove(self, apiclient, username, **kwargs):
        cmd = removeVpnUser.removeVpnUserCmd()
        cmd.id = self.id
        cmd.username = username
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpnuser = apiclient.removeVpnUser(cmd)
        return vpnuser
