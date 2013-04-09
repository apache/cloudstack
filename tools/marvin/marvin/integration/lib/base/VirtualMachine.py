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
from marvin.cloudstackAPI import scaleVirtualMachine
from marvin.cloudstackAPI import deployVirtualMachine
from marvin.cloudstackAPI import migrateVirtualMachine
from marvin.cloudstackAPI import listVirtualMachines
from marvin.cloudstackAPI import stopVirtualMachine
from marvin.cloudstackAPI import rebootVirtualMachine
from marvin.cloudstackAPI import updateVirtualMachine
from marvin.cloudstackAPI import startVirtualMachine
from marvin.cloudstackAPI import destroyVirtualMachine
from marvin.cloudstackAPI import assignVirtualMachine
from marvin.cloudstackAPI import addNicToVirtualMachine
from marvin.cloudstackAPI import removeNicFromVirtualMachine
from marvin.cloudstackAPI import resetPasswordForVirtualMachine
from marvin.cloudstackAPI import resetSSHKeyForVirtualMachine
from marvin.cloudstackAPI import updateDefaultNicForVirtualMachine

class VirtualMachine(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def restore(self, apiclient, virtualmachineid, **kwargs):
        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.id = self.id
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.restoreVirtualMachine(cmd)
        return virtualmachine


    def scale(self, apiclient, serviceofferingid, **kwargs):
        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.id = self.id
        cmd.serviceofferingid = serviceofferingid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.scaleVirtualMachine(cmd)
        return virtualmachine


    @classmethod
    def deploy(cls, apiclient, VirtualMachineFactory, **kwargs):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VirtualMachineFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.deployVirtualMachine(cmd)
        return VirtualMachine(virtualmachine.__dict__)


    def migrate(self, apiclient, virtualmachineid, **kwargs):
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.id = self.id
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.migrateVirtualMachine(cmd)
        return virtualmachine


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.listVirtualMachines(cmd)
        return map(lambda e: VirtualMachine(e.__dict__), virtualmachine)


    def stop(self, apiclient, **kwargs):
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.stopVirtualMachine(cmd)
        return virtualmachine


    def reboot(self, apiclient, **kwargs):
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.rebootVirtualMachine(cmd)
        return virtualmachine


    def update(self, apiclient, **kwargs):
        cmd = updateVirtualMachine.updateVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.updateVirtualMachine(cmd)
        return virtualmachine


    def start(self, apiclient, **kwargs):
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.startVirtualMachine(cmd)
        return virtualmachine


    def destroy(self, apiclient, **kwargs):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.destroyVirtualMachine(cmd)
        return virtualmachine


    def assign(self, apiclient, account, domainid, virtualmachineid, **kwargs):
        cmd = assignVirtualMachine.assignVirtualMachineCmd()
        cmd.id = self.id
        cmd.account = account
        cmd.domainid = domainid
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        virtualmachine = apiclient.assignVirtualMachine(cmd)
        return virtualmachine

    def remove_nic(self, apiclient, nicid, **kwargs):
        cmd = removeNicFromVirtualMachine.removeNicFromVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        cmd.nicid = nicid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        nicfromvirtualmachine = apiclient.removeNicFromVirtualMachine(cmd)
        return nicfromvirtualmachine


    def add_nic(self, apiclient, networkid, **kwargs):
        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        cmd.networkid = networkid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        nictovirtualmachine = apiclient.addNicToVirtualMachine(cmd)
        return nictovirtualmachine


    def update_default_nic(self, apiclient, nicid, **kwargs):
        cmd = updateDefaultNicForVirtualMachine.updateDefaultNicForVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        cmd.nicid = nicid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        defaultnicforvirtualmachine = apiclient.updateDefaultNicForVirtualMachine(cmd)
        return defaultnicforvirtualmachine


    def reset_password(self, apiclient, **kwargs):
        cmd = resetPasswordForVirtualMachine.resetPasswordForVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        passwordforvirtualmachine = apiclient.resetPasswordForVirtualMachine(cmd)
        return passwordforvirtualmachine


    def reset_sshkey(self, apiclient, keypair, **kwargs):
        cmd = resetSSHKeyForVirtualMachine.resetSSHKeyForVirtualMachineCmd()
        cmd.id = self.id
        cmd.keypair = keypair
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        sshkeyforvirtualmachine = apiclient.resetSSHKeyForVirtualMachine(cmd)
        return sshkeyforvirtualmachine
