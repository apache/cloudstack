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
from marvin.cloudstackAPI import createCondition
from marvin.cloudstackAPI import listConditions
from marvin.cloudstackAPI import deleteCondition

class Condition(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, ConditionFactory, **kwargs):
        cmd = createCondition.createConditionCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in ConditionFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        condition = apiclient.createCondition(cmd)
        return Condition(condition.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listConditions.listConditionsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        condition = apiclient.listConditions(cmd)
        return map(lambda e: Condition(e.__dict__), condition)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteCondition.deleteConditionCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        condition = apiclient.deleteCondition(cmd)
