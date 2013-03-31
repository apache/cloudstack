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
from marvin.cloudstackAPI import createZone
from marvin.cloudstackAPI import listZones
from marvin.cloudstackAPI import updateZone
from marvin.cloudstackAPI import deleteZone

class Zone(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, ZoneFactory, **kwargs):
        cmd = createZone.createZoneCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in ZoneFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        zone = apiclient.createZone(cmd)
        return Zone(zone.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listZones.listZonesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        zone = apiclient.listZones(cmd)
        return map(lambda e: Zone(e.__dict__), zone)


    def update(self, apiclient, id, **kwargs):
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        zone = apiclient.updateZone(cmd)
        return zone


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteZone.deleteZoneCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        zone = apiclient.deleteZone(cmd)
        return zone
