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
from marvin.cloudstackAPI import createPortForwardingRule
from marvin.cloudstackAPI import listPortForwardingRules
from marvin.cloudstackAPI import updatePortForwardingRule
from marvin.cloudstackAPI import deletePortForwardingRule

class PortForwardingRule(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, PortForwardingRuleFactory, **kwargs):
        cmd = createPortForwardingRule.createPortForwardingRuleCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in PortForwardingRuleFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        portforwardingrule = apiclient.createPortForwardingRule(cmd)
        return PortForwardingRule(portforwardingrule.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        portforwardingrule = apiclient.listPortForwardingRules(cmd)
        return map(lambda e: PortForwardingRule(e.__dict__), portforwardingrule)


    def update(self, apiclient, publicport, protocol, ipaddressid, privateport, **kwargs):
        cmd = updatePortForwardingRule.updatePortForwardingRuleCmd()
        cmd.id = self.id
        cmd.ipaddressid = ipaddressid
        cmd.privateport = privateport
        cmd.protocol = protocol
        cmd.publicport = publicport
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        portforwardingrule = apiclient.updatePortForwardingRule(cmd)
        return portforwardingrule


    def delete(self, apiclient, **kwargs):
        cmd = deletePortForwardingRule.deletePortForwardingRuleCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        portforwardingrule = apiclient.deletePortForwardingRule(cmd)
        return portforwardingrule
