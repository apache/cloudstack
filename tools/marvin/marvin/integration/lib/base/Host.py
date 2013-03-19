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
from marvin.cloudstackAPI import addHost
from marvin.cloudstackAPI import listHosts
from marvin.cloudstackAPI import updateHost
from marvin.cloudstackAPI import reconnectHost
from marvin.cloudstackAPI import deleteHost

class Host(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, username, podid, url, hypervisor, zoneid, password, **kwargs):
        cmd = addHost.addHostCmd()
        cmd.hypervisor = hypervisor
        cmd.password = password
        cmd.podid = podid
        cmd.url = url
        cmd.username = username
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        host = apiclient.addHost(cmd)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listHosts.listHostsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        host = apiclient.listHosts(cmd)
        return map(lambda e: Host(e.__dict__), host)


    def update(self, apiclient, id, **kwargs):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        host = apiclient.updateHost(cmd)


    def reconnect(self, apiclient, id, **kwargs):
        cmd = reconnectHost.reconnectHostCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        host = apiclient.reconnectHost(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteHost.deleteHostCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        host = apiclient.deleteHost(cmd)
