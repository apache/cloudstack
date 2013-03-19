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
from marvin.cloudstackAPI import createPod
from marvin.cloudstackAPI import listPods
from marvin.cloudstackAPI import updatePod
from marvin.cloudstackAPI import deletePod

class Pod(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, PodFactory, **kwargs):
        cmd = createPod.createPodCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in PodFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        pod = apiclient.createPod(cmd)
        return Pod(pod.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listPods.listPodsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        pod = apiclient.listPods(cmd)
        return map(lambda e: Pod(e.__dict__), pod)


    def update(self, apiclient, id, **kwargs):
        cmd = updatePod.updatePodCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        pod = apiclient.updatePod(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deletePod.deletePodCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        pod = apiclient.deletePod(cmd)
