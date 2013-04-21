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
from marvin.cloudstackAPI import addHost
from marvin.cloudstackAPI import listHosts
from marvin.cloudstackAPI import updateHost
from marvin.cloudstackAPI import reconnectHost
from marvin.cloudstackAPI import deleteHost
from marvin.cloudstackAPI import prepareHostForMaintenance
from marvin.cloudstackAPI import cancelHostMaintenance
from marvin.cloudstackAPI import updateHostPassword

class Host(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, username, podid, url, hypervisor, zoneid, password, **kwargs):
        cmd = addHost.addHostCmd()
        cmd.id = self.id
        cmd.hypervisor = hypervisor
        cmd.password = password
        cmd.podid = podid
        cmd.url = url
        cmd.username = username
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        host = apiclient.addHost(cmd)
        return host


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listHosts.listHostsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        host = apiclient.listHosts(cmd)
        return map(lambda e: Host(e.__dict__), host)


    def update(self, apiclient, **kwargs):
        cmd = updateHost.updateHostCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        host = apiclient.updateHost(cmd)
        return host


    def reconnect(self, apiclient, **kwargs):
        cmd = reconnectHost.reconnectHostCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        host = apiclient.reconnectHost(cmd)
        return host


    def delete(self, apiclient, **kwargs):
        cmd = deleteHost.deleteHostCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        host = apiclient.deleteHost(cmd)
        return host


    def prepareMaintenance(self, apiclient, **kwargs):
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        hostformaintenance = apiclient.prepareHostForMaintenance(cmd)
        return hostformaintenance


    def cancelMaintenance(self, apiclient, **kwargs):
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        hostmaintenance = apiclient.cancelHostMaintenance(cmd)
        return hostmaintenance


    def updatePassword(self, apiclient, username, password, **kwargs):
        cmd = updateHostPassword.updateHostPasswordCmd()
        cmd.id = self.id
        cmd.password = password
        cmd.username = username
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        hostpassword = apiclient.updateHostPassword(cmd)
        return hostpassword
