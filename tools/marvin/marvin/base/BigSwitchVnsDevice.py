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
from marvin.cloudstackAPI import addBigSwitchVnsDevice
from marvin.cloudstackAPI import listBigSwitchVnsDevices
from marvin.cloudstackAPI import deleteBigSwitchVnsDevice

class BigSwitchVnsDevice(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, physicalnetworkid, hostname, **kwargs):
        cmd = addBigSwitchVnsDevice.addBigSwitchVnsDeviceCmd()
        cmd.id = self.id
        cmd.hostname = hostname
        cmd.physicalnetworkid = physicalnetworkid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        bigswitchvnsdevice = apiclient.addBigSwitchVnsDevice(cmd)
        return bigswitchvnsdevice


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listBigSwitchVnsDevices.listBigSwitchVnsDevicesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        bigswitchvnsdevice = apiclient.listBigSwitchVnsDevices(cmd)
        return map(lambda e: BigSwitchVnsDevice(e.__dict__), bigswitchvnsdevice)


    def delete(self, apiclient, vnsdeviceid, **kwargs):
        cmd = deleteBigSwitchVnsDevice.deleteBigSwitchVnsDeviceCmd()
        cmd.id = self.id
        cmd.vnsdeviceid = vnsdeviceid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        bigswitchvnsdevice = apiclient.deleteBigSwitchVnsDevice(cmd)
        return bigswitchvnsdevice
