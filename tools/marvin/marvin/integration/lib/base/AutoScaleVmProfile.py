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
from marvin.cloudstackAPI import createAutoScaleVmProfile
from marvin.cloudstackAPI import listAutoScaleVmProfiles
from marvin.cloudstackAPI import updateAutoScaleVmProfile
from marvin.cloudstackAPI import deleteAutoScaleVmProfile

class AutoScaleVmProfile(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    @classmethod
    def create(cls, apiclient, AutoScaleVmProfileFactory, **kwargs):
        cmd = createAutoScaleVmProfile.createAutoScaleVmProfileCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in AutoScaleVmProfileFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        autoscalevmprofile = apiclient.createAutoScaleVmProfile(cmd)
        return AutoScaleVmProfile(autoscalevmprofile.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listAutoScaleVmProfiles.listAutoScaleVmProfilesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmprofile = apiclient.listAutoScaleVmProfiles(cmd)
        return map(lambda e: AutoScaleVmProfile(e.__dict__), autoscalevmprofile)


    def update(self, apiclient, id, **kwargs):
        cmd = updateAutoScaleVmProfile.updateAutoScaleVmProfileCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmprofile = apiclient.updateAutoScaleVmProfile(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteAutoScaleVmProfile.deleteAutoScaleVmProfileCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        autoscalevmprofile = apiclient.deleteAutoScaleVmProfile(cmd)
