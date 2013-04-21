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
from marvin.base import CloudStackEntity
from marvin.cloudstackAPI import createLBHealthCheckPolicy
from marvin.cloudstackAPI import deleteLBHealthCheckPolicy
from marvin.cloudstackAPI import listLBHealthCheckPolicies

class LBHealthCheckPolicy(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createLBHealthCheckPolicy.createLBHealthCheckPolicyCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        lbhealthcheckpolicy = apiclient.createLBHealthCheckPolicy(cmd)
        return LBHealthCheckPolicy(lbhealthcheckpolicy.__dict__)


    def delete(self, apiclient, **kwargs):
        cmd = deleteLBHealthCheckPolicy.deleteLBHealthCheckPolicyCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        lbhealthcheckpolicy = apiclient.deleteLBHealthCheckPolicy(cmd)
        return lbhealthcheckpolicy


    @classmethod
    def list(self, apiclient, lbruleid, **kwargs):
        cmd = listLBHealthCheckPolicies.listLBHealthCheckPoliciesCmd()
        cmd.lbruleid = lbruleid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        lbhealthcheckpolicies = apiclient.listLBHealthCheckPolicies(cmd)
        return map(lambda e: LBHealthCheckPolicy(e.__dict__), lbhealthcheckpolicies)
