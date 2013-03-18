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
from marvin.cloudstackAPI import createVPC
from marvin.cloudstackAPI import listVPCs
from marvin.cloudstackAPI import updateVPC
from marvin.cloudstackAPI import restartVPC
from marvin.cloudstackAPI import deleteVPC

class VPC(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    @classmethod
    def create(cls, apiclient, VPCFactory, **kwargs):
        cmd = createVPC.createVPCCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VPCFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpc = apiclient.createVPC(cmd)
        return VPC(vpc.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVPCs.listVPCsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpc = apiclient.listVPCs(cmd)
        return map(lambda e: VPC(e.__dict__), vpc)


    def update(self, apiclient, **kwargs):
        cmd = updateVPC.updateVPCCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpc = apiclient.updateVPC(cmd)


    def restart(self, apiclient, **kwargs):
        cmd = restartVPC.restartVPCCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpc = apiclient.restartVPC(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteVPC.deleteVPCCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vpc = apiclient.deleteVPC(cmd)
