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
from marvin.cloudstackAPI import addNiciraNvpDevice
from marvin.cloudstackAPI import listNiciraNvpDevices
from marvin.cloudstackAPI import deleteNiciraNvpDevice
from marvin.cloudstackAPI import listNiciraNvpDeviceNetworks

class NiciraNvpDevice(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, username, physicalnetworkid, password, hostname, transportzoneuuid, **kwargs):
        cmd = addNiciraNvpDevice.addNiciraNvpDeviceCmd()
        cmd.id = self.id
        cmd.hostname = hostname
        cmd.password = password
        cmd.physicalnetworkid = physicalnetworkid
        cmd.transportzoneuuid = transportzoneuuid
        cmd.username = username
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        niciranvpdevice = apiclient.addNiciraNvpDevice(cmd)
        return niciranvpdevice


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listNiciraNvpDevices.listNiciraNvpDevicesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        niciranvpdevice = apiclient.listNiciraNvpDevices(cmd)
        return map(lambda e: NiciraNvpDevice(e.__dict__), niciranvpdevice)


    def delete(self, apiclient, nvpdeviceid, **kwargs):
        cmd = deleteNiciraNvpDevice.deleteNiciraNvpDeviceCmd()
        cmd.id = self.id
        cmd.nvpdeviceid = nvpdeviceid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        niciranvpdevice = apiclient.deleteNiciraNvpDevice(cmd)
        return niciranvpdevice

    @classmethod
    def listNetworks(self, apiclient, nvpdeviceid, **kwargs):
        cmd = listNiciraNvpDeviceNetworks.listNiciraNvpDeviceNetworksCmd()
        cmd.nvpdeviceid = nvpdeviceid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        niciranvpdevicenetworks = apiclient.listNiciraNvpDeviceNetworks(cmd)
        return map(lambda e: NiciraNvpDevice(e.__dict__), niciranvpdevicenetworks)
