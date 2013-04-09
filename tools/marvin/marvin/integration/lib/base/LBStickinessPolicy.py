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
from marvin.cloudstackAPI import createLBStickinessPolicy
from marvin.cloudstackAPI import deleteLBStickinessPolicy
from marvin.cloudstackAPI import listLBStickinessPolicies

class LBStickinessPolicy(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, LBStickinessPolicyFactory, **kwargs):
        cmd = createLBStickinessPolicy.createLBStickinessPolicyCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in LBStickinessPolicyFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        lbstickinesspolicy = apiclient.createLBStickinessPolicy(cmd)
        return LBStickinessPolicy(lbstickinesspolicy.__dict__)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteLBStickinessPolicy.deleteLBStickinessPolicyCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        lbstickinesspolicy = apiclient.deleteLBStickinessPolicy(cmd)
        return lbstickinesspolicy


    @classmethod
    def list(self, apiclient, lbruleid, **kwargs):
        cmd = listLBStickinessPolicies.listLBStickinessPoliciesCmd()
        cmd.lbruleid = lbruleid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        lbstickinesspolicies = apiclient.listLBStickinessPolicies(cmd)
        return map(lambda e: LBStickinessPolicy(e.__dict__), lbstickinesspolicies)
