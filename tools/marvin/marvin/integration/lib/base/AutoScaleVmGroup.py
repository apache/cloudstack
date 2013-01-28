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
from marvin.cloudstackAPI import enableAutoScaleVmGroup
from marvin.cloudstackAPI import createAutoScaleVmGroup
from marvin.cloudstackAPI import listAutoScaleVmGroups
from marvin.cloudstackAPI import updateAutoScaleVmGroup
from marvin.cloudstackAPI import disableAutoScaleVmGroup
from marvin.cloudstackAPI import deleteAutoScaleVmGroup

class AutoScaleVmGroup(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def enable(self, apiclient, id, **kwargs):
        cmd = enableAutoScaleVmGroup.enableAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmgroup = apiclient.enableAutoScaleVmGroup(cmd)


    @classmethod
    def create(cls, apiclient, AutoScaleVmGroupFactory, **kwargs):
        cmd = createAutoScaleVmGroup.createAutoScaleVmGroupCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in AutoScaleVmGroupFactory.attributes()]
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmgroup = apiclient.createAutoScaleVmGroup(cmd)
        return AutoScaleVmGroup(autoscalevmgroup.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listAutoScaleVmGroups.listAutoScaleVmGroupsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmgroup = apiclient.listAutoScaleVmGroups(cmd)
        return map(lambda e: AutoScaleVmGroup(e.__dict__), autoscalevmgroup)


    def update(self, apiclient, id, **kwargs):
        cmd = updateAutoScaleVmGroup.updateAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmgroup = apiclient.updateAutoScaleVmGroup(cmd)


    def disable(self, apiclient, id, **kwargs):
        cmd = disableAutoScaleVmGroup.disableAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmgroup = apiclient.disableAutoScaleVmGroup(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteAutoScaleVmGroup.deleteAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmgroup = apiclient.deleteAutoScaleVmGroup(cmd)
