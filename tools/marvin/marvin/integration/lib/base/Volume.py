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
from marvin.cloudstackAPI import migrateVolume
from marvin.cloudstackAPI import createVolume
from marvin.cloudstackAPI import listVolumes
from marvin.cloudstackAPI import uploadVolume
from marvin.cloudstackAPI import attachVolume
from marvin.cloudstackAPI import detachVolume
from marvin.cloudstackAPI import extractVolume
from marvin.cloudstackAPI import deleteVolume

class Volume(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def migrate(self, apiclient, storageid, volumeid, **kwargs):
        cmd = migrateVolume.migrateVolumeCmd()
        cmd.id = self.id
        cmd.storageid = storageid
        cmd.volumeid = volumeid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.migrateVolume(cmd)
        return volume


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createVolume.createVolumeCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.createVolume(cmd)
        return Volume(volume.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVolumes.listVolumesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.listVolumes(cmd)
        return map(lambda e: Volume(e.__dict__), volume)


    def upload(self, apiclient, url, zoneid, name, format, **kwargs):
        cmd = uploadVolume.uploadVolumeCmd()
        cmd.id = self.id
        cmd.format = format
        cmd.name = name
        cmd.url = url
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.uploadVolume(cmd)
        return volume


    def attach(self, apiclient, virtualmachineid, **kwargs):
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = self.id
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.attachVolume(cmd)
        return volume


    def detach(self, apiclient, **kwargs):
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.detachVolume(cmd)
        return volume


    def extract(self, apiclient, zoneid, mode, **kwargs):
        cmd = extractVolume.extractVolumeCmd()
        cmd.id = self.id
        cmd.mode = mode
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.extractVolume(cmd)
        return volume


    def delete(self, apiclient, **kwargs):
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.deleteVolume(cmd)
        return volume
