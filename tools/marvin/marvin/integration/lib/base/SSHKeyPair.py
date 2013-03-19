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
from marvin.cloudstackAPI import createSSHKeyPair
from marvin.cloudstackAPI import registerSSHKeyPair
from marvin.cloudstackAPI import listSSHKeyPairs
from marvin.cloudstackAPI import deleteSSHKeyPair

class SSHKeyPair(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, SSHKeyPairFactory, **kwargs):
        cmd = createSSHKeyPair.createSSHKeyPairCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in SSHKeyPairFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        sshkeypair = apiclient.createSSHKeyPair(cmd)
        return SSHKeyPair(sshkeypair.__dict__)


    def register(self, apiclient, publickey, name, **kwargs):
        cmd = registerSSHKeyPair.registerSSHKeyPairCmd()
        cmd.name = name
        cmd.publickey = publickey
        [setattr(cmd, key, value) for key,value in kwargs.items]
        sshkeypair = apiclient.registerSSHKeyPair(cmd)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listSSHKeyPairs.listSSHKeyPairsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        sshkeypair = apiclient.listSSHKeyPairs(cmd)
        return map(lambda e: SSHKeyPair(e.__dict__), sshkeypair)


    def delete(self, apiclient, name, **kwargs):
        cmd = deleteSSHKeyPair.deleteSSHKeyPairCmd()
        cmd.name = name
        [setattr(cmd, key, value) for key,value in kwargs.items]
        sshkeypair = apiclient.deleteSSHKeyPair(cmd)
