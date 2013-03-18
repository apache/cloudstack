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
from marvin.cloudstackAPI import createSnapshot
from marvin.cloudstackAPI import listSnapshots
from marvin.cloudstackAPI import deleteSnapshot

class Snapshot(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    @classmethod
    def create(cls, apiclient, SnapshotFactory, **kwargs):
        cmd = createSnapshot.createSnapshotCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in SnapshotFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        snapshot = apiclient.createSnapshot(cmd)
        return Snapshot(snapshot.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listSnapshots.listSnapshotsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        snapshot = apiclient.listSnapshots(cmd)
        return map(lambda e: Snapshot(e.__dict__), snapshot)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        snapshot = apiclient.deleteSnapshot(cmd)
