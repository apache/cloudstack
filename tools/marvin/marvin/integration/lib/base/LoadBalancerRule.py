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
from marvin.cloudstackAPI import createLoadBalancerRule
from marvin.cloudstackAPI import listLoadBalancerRules
from marvin.cloudstackAPI import updateLoadBalancerRule
from marvin.cloudstackAPI import deleteLoadBalancerRule

class LoadBalancerRule(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, LoadBalancerRuleFactory, **kwargs):
        cmd = createLoadBalancerRule.createLoadBalancerRuleCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in LoadBalancerRuleFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        loadbalancerrule = apiclient.createLoadBalancerRule(cmd)
        return LoadBalancerRule(loadbalancerrule.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        loadbalancerrule = apiclient.listLoadBalancerRules(cmd)
        return map(lambda e: LoadBalancerRule(e.__dict__), loadbalancerrule)


    def update(self, apiclient, id, **kwargs):
        cmd = updateLoadBalancerRule.updateLoadBalancerRuleCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        loadbalancerrule = apiclient.updateLoadBalancerRule(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteLoadBalancerRule.deleteLoadBalancerRuleCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        loadbalancerrule = apiclient.deleteLoadBalancerRule(cmd)
