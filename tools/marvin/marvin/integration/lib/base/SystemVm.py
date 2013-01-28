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
from marvin.cloudstackAPI import migrateSystemVm
from marvin.cloudstackAPI import stopSystemVm
from marvin.cloudstackAPI import listSystemVms
from marvin.cloudstackAPI import rebootSystemVm
from marvin.cloudstackAPI import startSystemVm
from marvin.cloudstackAPI import destroySystemVm

class SystemVm(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def migrate(self, apiclient, hostid, virtualmachineid, **kwargs):
        cmd = migrateSystemVm.migrateSystemVmCmd()
        cmd.hostid = hostid
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        systemvm = apiclient.migrateSystemVm(cmd)


    def stop(self, apiclient, id, **kwargs):
        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        systemvm = apiclient.stopSystemVm(cmd)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listSystemVms.listSystemVmsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        systemvm = apiclient.listSystemVms(cmd)
        return map(lambda e: SystemVm(e.__dict__), systemvm)


    def reboot(self, apiclient, id, **kwargs):
        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        systemvm = apiclient.rebootSystemVm(cmd)


    def start(self, apiclient, id, **kwargs):
        cmd = startSystemVm.startSystemVmCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        systemvm = apiclient.startSystemVm(cmd)


    def destroy(self, apiclient, id, **kwargs):
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        systemvm = apiclient.destroySystemVm(cmd)
