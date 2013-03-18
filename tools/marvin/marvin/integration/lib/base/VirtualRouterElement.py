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
from marvin.cloudstackAPI import createVirtualRouterElement
from marvin.cloudstackAPI import listVirtualRouterElements
from marvin.cloudstackAPI import configureVirtualRouterElement

class VirtualRouterElement(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    @classmethod
    def create(cls, apiclient, VirtualRouterElementFactory, **kwargs):
        cmd = createVirtualRouterElement.createVirtualRouterElementCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VirtualRouterElementFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualrouterelement = apiclient.createVirtualRouterElement(cmd)
        return VirtualRouterElement(virtualrouterelement.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVirtualRouterElements.listVirtualRouterElementsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualrouterelement = apiclient.listVirtualRouterElements(cmd)
        return map(lambda e: VirtualRouterElement(e.__dict__), virtualrouterelement)


    def configure(self, apiclient, enabled, id, **kwargs):
        cmd = configureVirtualRouterElement.configureVirtualRouterElementCmd()
        cmd.id = id
        cmd.enabled = enabled
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualrouterelement = apiclient.configureVirtualRouterElement(cmd)
