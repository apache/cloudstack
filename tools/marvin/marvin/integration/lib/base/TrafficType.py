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
from marvin.cloudstackAPI import addTrafficType
from marvin.cloudstackAPI import listTrafficTypes
from marvin.cloudstackAPI import updateTrafficType
from marvin.cloudstackAPI import deleteTrafficType

class TrafficType(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, traffictype, physicalnetworkid, **kwargs):
        cmd = addTrafficType.addTrafficTypeCmd()
        cmd.physicalnetworkid = physicalnetworkid
        cmd.traffictype = traffictype
        [setattr(cmd, key, value) for key,value in kwargs.items]
        traffictype = apiclient.addTrafficType(cmd)


    @classmethod
    def list(self, apiclient, physicalnetworkid, **kwargs):
        cmd = listTrafficTypes.listTrafficTypesCmd()
        cmd.physicalnetworkid = physicalnetworkid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        traffictype = apiclient.listTrafficTypes(cmd)
        return map(lambda e: TrafficType(e.__dict__), traffictype)


    def update(self, apiclient, id, **kwargs):
        cmd = updateTrafficType.updateTrafficTypeCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        traffictype = apiclient.updateTrafficType(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteTrafficType.deleteTrafficTypeCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        traffictype = apiclient.deleteTrafficType(cmd)
