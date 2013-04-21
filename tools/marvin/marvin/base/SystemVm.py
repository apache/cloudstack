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
from marvin.cloudstackAPI import migrateSystemVm
from marvin.cloudstackAPI import stopSystemVm
from marvin.cloudstackAPI import listSystemVms
from marvin.cloudstackAPI import rebootSystemVm
from marvin.cloudstackAPI import startSystemVm
from marvin.cloudstackAPI import destroySystemVm

class SystemVm(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def migrate(self, apiclient, hostid, virtualmachineid, **kwargs):
        cmd = migrateSystemVm.migrateSystemVmCmd()
        cmd.id = self.id
        cmd.hostid = hostid
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        systemvm = apiclient.migrateSystemVm(cmd)
        return systemvm


    def stop(self, apiclient, **kwargs):
        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        systemvm = apiclient.stopSystemVm(cmd)
        return systemvm


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listSystemVms.listSystemVmsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        systemvm = apiclient.listSystemVms(cmd)
        return map(lambda e: SystemVm(e.__dict__), systemvm)


    def reboot(self, apiclient, **kwargs):
        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        systemvm = apiclient.rebootSystemVm(cmd)
        return systemvm


    def start(self, apiclient, **kwargs):
        cmd = startSystemVm.startSystemVmCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        systemvm = apiclient.startSystemVm(cmd)
        return systemvm


    def destroy(self, apiclient, **kwargs):
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        systemvm = apiclient.destroySystemVm(cmd)
        return systemvm
