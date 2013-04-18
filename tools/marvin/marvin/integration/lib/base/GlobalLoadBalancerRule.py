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
from marvin.cloudstackAPI import createGlobalLoadBalancerRule
from marvin.cloudstackAPI import listGlobalLoadBalancerRules
from marvin.cloudstackAPI import updateGlobalLoadBalancerRule
from marvin.cloudstackAPI import deleteGlobalLoadBalancerRule
from marvin.cloudstackAPI import removeFromGlobalLoadBalancerRule
from marvin.cloudstackAPI import assignToGlobalLoadBalancerRule

class GlobalLoadBalancerRule(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createGlobalLoadBalancerRule.createGlobalLoadBalancerRuleCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        globalloadbalancerrule = apiclient.createGlobalLoadBalancerRule(cmd)
        return GlobalLoadBalancerRule(globalloadbalancerrule.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listGlobalLoadBalancerRules.listGlobalLoadBalancerRulesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        globalloadbalancerrule = apiclient.listGlobalLoadBalancerRules(cmd)
        return map(lambda e: GlobalLoadBalancerRule(e.__dict__), globalloadbalancerrule)


    def update(self, apiclient, **kwargs):
        cmd = updateGlobalLoadBalancerRule.updateGlobalLoadBalancerRuleCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        globalloadbalancerrule = apiclient.updateGlobalLoadBalancerRule(cmd)
        return globalloadbalancerrule


    def delete(self, apiclient, **kwargs):
        cmd = deleteGlobalLoadBalancerRule.deleteGlobalLoadBalancerRuleCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        globalloadbalancerrule = apiclient.deleteGlobalLoadBalancerRule(cmd)
        return globalloadbalancerrule


    def remove(self, apiclient, loadbalancerrulelist, **kwargs):
        cmd = removeFromGlobalLoadBalancerRule.removeFromGlobalLoadBalancerRuleCmd()
        cmd.id = self.id
        cmd.loadbalancerrulelist = loadbalancerrulelist
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        fromgloballoadbalancerrule = apiclient.removeFromGlobalLoadBalancerRule(cmd)
        return fromgloballoadbalancerrule


    def assign(self, apiclient, loadbalancerrulelist, **kwargs):
        cmd = assignToGlobalLoadBalancerRule.assignToGlobalLoadBalancerRuleCmd()
        cmd.id = self.id
        cmd.loadbalancerrulelist = loadbalancerrulelist
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        togloballoadbalancerrule = apiclient.assignToGlobalLoadBalancerRule(cmd)
        return togloballoadbalancerrule
