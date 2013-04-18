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


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createVPC.createVPCCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpc = apiclient.createVPC(cmd)
        return VPC(vpc.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVPCs.listVPCsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpc = apiclient.listVPCs(cmd)
        return map(lambda e: VPC(e.__dict__), vpc)


    def update(self, apiclient, **kwargs):
        cmd = updateVPC.updateVPCCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpc = apiclient.updateVPC(cmd)
        return vpc


    def restart(self, apiclient, **kwargs):
        cmd = restartVPC.restartVPCCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpc = apiclient.restartVPC(cmd)
        return vpc


    def delete(self, apiclient, **kwargs):
        cmd = deleteVPC.deleteVPCCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpc = apiclient.deleteVPC(cmd)
        return vpc
