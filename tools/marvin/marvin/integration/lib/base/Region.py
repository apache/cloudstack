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
from marvin.cloudstackAPI import addRegion
from marvin.cloudstackAPI import listRegions
from marvin.cloudstackAPI import updateRegion
from marvin.cloudstackAPI import removeRegion

class Region(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, endpoint, name, **kwargs):
        cmd = addRegion.addRegionCmd()
        cmd.id = self.id
        cmd.endpoint = endpoint
        cmd.name = name
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        region = apiclient.addRegion(cmd)
        return region


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listRegions.listRegionsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        region = apiclient.listRegions(cmd)
        return map(lambda e: Region(e.__dict__), region)


    def update(self, apiclient, **kwargs):
        cmd = updateRegion.updateRegionCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        region = apiclient.updateRegion(cmd)
        return region


    def remove(self, apiclient, **kwargs):
        cmd = removeRegion.removeRegionCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        region = apiclient.removeRegion(cmd)
        return region
