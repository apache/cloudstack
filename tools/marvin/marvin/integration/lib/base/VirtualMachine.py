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
from marvin.cloudstackAPI import restoreVirtualMachine
from marvin.cloudstackAPI import deployVirtualMachine
from marvin.cloudstackAPI import migrateVirtualMachine
from marvin.cloudstackAPI import listVirtualMachines
from marvin.cloudstackAPI import stopVirtualMachine
from marvin.cloudstackAPI import rebootVirtualMachine
from marvin.cloudstackAPI import updateVirtualMachine
from marvin.cloudstackAPI import startVirtualMachine
from marvin.cloudstackAPI import destroyVirtualMachine
from marvin.cloudstackAPI import assignVirtualMachine

class VirtualMachine(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def restore(self, apiclient, virtualmachineid, **kwargs):
        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.restoreVirtualMachine(cmd)


    @classmethod
    def deploy(cls, apiclient, VirtualMachineFactory, **kwargs):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VirtualMachineFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.deployVirtualMachine(cmd)
        return VirtualMachine(virtualmachine.__dict__)


    def migrate(self, apiclient, virtualmachineid, **kwargs):
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.migrateVirtualMachine(cmd)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.listVirtualMachines(cmd)
        return map(lambda e: VirtualMachine(e.__dict__), virtualmachine)


    def stop(self, apiclient, id, **kwargs):
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.stopVirtualMachine(cmd)


    def reboot(self, apiclient, id, **kwargs):
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.rebootVirtualMachine(cmd)


    def update(self, apiclient, id, **kwargs):
        cmd = updateVirtualMachine.updateVirtualMachineCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.updateVirtualMachine(cmd)


    def start(self, apiclient, id, **kwargs):
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.startVirtualMachine(cmd)


    def destroy(self, apiclient, id, **kwargs):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.destroyVirtualMachine(cmd)


    def assign(self, apiclient, account, domainid, virtualmachineid, **kwargs):
        cmd = assignVirtualMachine.assignVirtualMachineCmd()
        cmd.account = account
        cmd.domainid = domainid
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        virtualmachine = apiclient.assignVirtualMachine(cmd)
