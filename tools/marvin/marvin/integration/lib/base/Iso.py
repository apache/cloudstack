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
from marvin.cloudstackAPI import copyIso
from marvin.cloudstackAPI import registerIso
from marvin.cloudstackAPI import listIsos
from marvin.cloudstackAPI import updateIso
from marvin.cloudstackAPI import attachIso
from marvin.cloudstackAPI import detachIso
from marvin.cloudstackAPI import extractIso
from marvin.cloudstackAPI import deleteIso

class Iso(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    def copy(self, apiclient, sourcezoneid, id, destzoneid, **kwargs):
        cmd = copyIso.copyIsoCmd()
        cmd.id = id
        cmd.destzoneid = destzoneid
        cmd.sourcezoneid = sourcezoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.copyIso(cmd)


    def register(self, apiclient, url, displaytext, name, zoneid, **kwargs):
        cmd = registerIso.registerIsoCmd()
        cmd.displaytext = displaytext
        cmd.name = name
        cmd.url = url
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.registerIso(cmd)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listIsos.listIsosCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.listIsos(cmd)
        return map(lambda e: Iso(e.__dict__), iso)


    def update(self, apiclient, id, **kwargs):
        cmd = updateIso.updateIsoCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.updateIso(cmd)


    def attach(self, apiclient, id, virtualmachineid, **kwargs):
        cmd = attachIso.attachIsoCmd()
        cmd.id = id
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.attachIso(cmd)


    def detach(self, apiclient, virtualmachineid, **kwargs):
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.detachIso(cmd)


    def extract(self, apiclient, id, mode, **kwargs):
        cmd = extractIso.extractIsoCmd()
        cmd.id = id
        cmd.mode = mode
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.extractIso(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteIso.deleteIsoCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        iso = apiclient.deleteIso(cmd)
