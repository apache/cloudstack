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
from marvin.cloudstackAPI import createVMSnapshot
from marvin.cloudstackAPI import listVMSnapshot
from marvin.cloudstackAPI import deleteVMSnapshot

class VMSnapshot(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, VMSnapshotFactory, **kwargs):
        cmd = createVMSnapshot.createVMSnapshotCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VMSnapshotFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vmsnapshot = apiclient.createVMSnapshot(cmd)
        return VMSnapshot(vmsnapshot.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVMSnapshot.listVMSnapshotCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vmsnapshot = apiclient.listVMSnapshot(cmd)
        return map(lambda e: VMSnapshot(e.__dict__), vmsnapshot)


    def delete(self, apiclient, vmsnapshotid, **kwargs):
        cmd = deleteVMSnapshot.deleteVMSnapshotCmd()
        cmd.vmsnapshotid = vmsnapshotid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        vmsnapshot = apiclient.deleteVMSnapshot(cmd)
