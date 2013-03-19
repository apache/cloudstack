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
from marvin.cloudstackAPI import createSecurityGroup
from marvin.cloudstackAPI import listSecurityGroups
from marvin.cloudstackAPI import deleteSecurityGroup

class SecurityGroup(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, SecurityGroupFactory, **kwargs):
        cmd = createSecurityGroup.createSecurityGroupCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in SecurityGroupFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        securitygroup = apiclient.createSecurityGroup(cmd)
        return SecurityGroup(securitygroup.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listSecurityGroups.listSecurityGroupsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        securitygroup = apiclient.listSecurityGroups(cmd)
        return map(lambda e: SecurityGroup(e.__dict__), securitygroup)


    def delete(self, apiclient, **kwargs):
        cmd = deleteSecurityGroup.deleteSecurityGroupCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        securitygroup = apiclient.deleteSecurityGroup(cmd)
