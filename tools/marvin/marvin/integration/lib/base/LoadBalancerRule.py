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
from marvin.cloudstackAPI import removeFromLoadBalancerRule
from marvin.cloudstackAPI import listLoadBalancerRuleInstances
from marvin.cloudstackAPI import assignToLoadBalancerRule

class LoadBalancerRule(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createLoadBalancerRule.createLoadBalancerRuleCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        loadbalancerrule = apiclient.createLoadBalancerRule(cmd)
        return LoadBalancerRule(loadbalancerrule.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        loadbalancerrule = apiclient.listLoadBalancerRules(cmd)
        return map(lambda e: LoadBalancerRule(e.__dict__), loadbalancerrule)


    def update(self, apiclient, **kwargs):
        cmd = updateLoadBalancerRule.updateLoadBalancerRuleCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        loadbalancerrule = apiclient.updateLoadBalancerRule(cmd)
        return loadbalancerrule


    def delete(self, apiclient, **kwargs):
        cmd = deleteLoadBalancerRule.deleteLoadBalancerRuleCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        loadbalancerrule = apiclient.deleteLoadBalancerRule(cmd)
        return loadbalancerrule


    def remove(self, apiclient, virtualmachineids, **kwargs):
        cmd = removeFromLoadBalancerRule.removeFromLoadBalancerRuleCmd()
        cmd.id = self.id
        cmd.virtualmachineids = virtualmachineids
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        fromloadbalancerrule = apiclient.removeFromLoadBalancerRule(cmd)
        return fromloadbalancerrule


    def assign(self, apiclient, virtualmachineids, **kwargs):
        cmd = assignToLoadBalancerRule.assignToLoadBalancerRuleCmd()
        cmd.id = self.id
        cmd.virtualmachineids = virtualmachineids
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        toloadbalancerrule = apiclient.assignToLoadBalancerRule(cmd)
        return toloadbalancerrule


    @classmethod
    def listInstances(self, apiclient, **kwargs):
        cmd = listLoadBalancerRuleInstances.listLoadBalancerRuleInstancesCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        loadbalancerruleinstances = apiclient.listLoadBalancerRuleInstances(cmd)
        return map(lambda e: LoadBalancerRule(e.__dict__), loadbalancerruleinstances)
