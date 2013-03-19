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
        cmd.storageid = storageid
        cmd.volumeid = volumeid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.migrateVolume(cmd)


    @classmethod
    def create(cls, apiclient, VolumeFactory, **kwargs):
        cmd = createVolume.createVolumeCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VolumeFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        volume = apiclient.createVolume(cmd)
        return Volume(volume.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVolumes.listVolumesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.listVolumes(cmd)
        return map(lambda e: Volume(e.__dict__), volume)


    def upload(self, apiclient, url, zoneid, name, format, **kwargs):
        cmd = uploadVolume.uploadVolumeCmd()
        cmd.format = format
        cmd.name = name
        cmd.url = url
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.uploadVolume(cmd)


    def attach(self, apiclient, id, virtualmachineid, **kwargs):
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = id
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.attachVolume(cmd)


    def detach(self, apiclient, **kwargs):
        cmd = detachVolume.detachVolumeCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.detachVolume(cmd)


    def extract(self, apiclient, zoneid, id, mode, **kwargs):
        cmd = extractVolume.extractVolumeCmd()
        cmd.id = id
        cmd.mode = mode
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.extractVolume(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        volume = apiclient.deleteVolume(cmd)
