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
from marvin.cloudstackAPI import createSnapshotPolicy
from marvin.cloudstackAPI import listSnapshotPolicies
from marvin.cloudstackAPI import deleteSnapshotPolicies

class SnapshotPolicy(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createSnapshotPolicy.createSnapshotPolicyCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        snapshotpolicy = apiclient.createSnapshotPolicy(cmd)
        return SnapshotPolicy(snapshotpolicy.__dict__)


    @classmethod
    def list(self, apiclient, volumeid, **kwargs):
        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
        cmd.volumeid = volumeid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        snapshotpolicies = apiclient.listSnapshotPolicies(cmd)
        return map(lambda e: SnapshotPolicy(e.__dict__), snapshotpolicies)


    def delete(self, apiclient, **kwargs):
        cmd = deleteSnapshotPolicies.deleteSnapshotPoliciesCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        snapshotpolicies = apiclient.deleteSnapshotPolicies(cmd)
        return snapshotpolicies
