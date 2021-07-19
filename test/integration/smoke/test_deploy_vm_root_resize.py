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

#Test from the Marvin - Testing in Python wiki

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
#Import Integration Libraries
#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, VirtualMachine, ServiceOffering,\
    Configurations, StoragePool, Template
#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.common import get_zone, get_domain, get_template,\
    list_volumes, list_storage_pools, list_configurations,\
    matchResourceCount
from marvin.codes import FAILED, INVALID_INPUT, PASS,\
    RESOURCE_PRIMARY_STORAGE
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient
import time
import re
from marvin.cloudstackAPI import updateTemplate,registerTemplate

class TestDeployVmRootSize(cloudstackTestCase):
    """Test deploy a VM into a user account
    """
    @classmethod
    def setUpClass(cls):
        cls.cloudstacktestclient = super(TestDeployVmRootSize,
                                     cls).getClsTestClient()
        cls.api_client = cls.cloudstacktestclient.getApiClient()
        cls.hypervisor = cls.cloudstacktestclient.getHypervisorInfo().lower()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        # Get Zone, Domain and Default Built-in template
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client,
                            cls.cloudstacktestclient.getZoneForTests())
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.services["mode"] = cls.zone.networktype
        cls._cleanup = []
        cls.storageID = None
        cls.updateclone = False
        cls.defaultdiskcontroller = "ide"
        cls.template = get_template(cls.api_client, cls.zone.id)
        if cls.template == FAILED:
            assert False, "get_template() failed to return template "

        #create a user account
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id,admin=True
        )
        cls._cleanup.append(cls.account)
        list_pool_resp = list_storage_pools(cls.api_client,
                                            account=cls.account.name,
                                            domainid=cls.domain.id)

        # Identify the storage pool type  and set vmware fullclone to
        # true if storage is VMFS
        if cls.hypervisor == 'vmware':
             # please make sure url of templateregister dictionary in
             # test_data.config pointing to .ova file

             list_config_storage_response = list_configurations(
                        cls.api_client
                        , name=
                        "vmware.root.disk.controller")
             cls.defaultdiskcontroller = list_config_storage_response[0].value
             if list_config_storage_response[0].value == "ide" or \
                             list_config_storage_response[0].value == \
                             "osdefault":
                        Configurations.update(cls.api_client,
                                              "vmware.root.disk.controller",
                                              value="scsi")

                        cls.updateclone = True

             for strpool in list_pool_resp:
                if strpool.type.lower() == "vmfs" or strpool.type.lower()== "networkfilesystem":
                    list_config_storage_response = list_configurations(
                        cls.api_client, name="vmware.create.full.clone",
                        storageid=strpool.id)

                    res = validateList(list_config_storage_response)
                    if res[2]== INVALID_INPUT:
                        raise Exception("Failed to  list configurations ")

                    if list_config_storage_response[0].value == "false":
                        Configurations.update(cls.api_client,
                                              "vmware.create.full.clone",
                                              value="true",
                                              storageid=strpool.id)
                        cls.updateclone = True
                        StoragePool.update(cls.api_client,id=strpool.id,
                                           tags="scsi")
                        cls.storageID = strpool.id
                        break

             list_config_fullclone_global_response = list_configurations(
                        cls.api_client, name="vmware.create.full.clone")

             if list_config_fullclone_global_response[0].value=="false":
                        Configurations.update(cls.api_client,
                                              "vmware.create.full.clone",
                                              value="true")
                        cls.updateclone = True


        #create a service offering
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        #build cleanup list
        cls.services_offering_vmware=ServiceOffering.create(
                cls.api_client,cls.services["service_offering"],tags="scsi")
        cls._cleanup.extend([cls.service_offering,cls.services_offering_vmware])

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used

            if cls.updateclone:
                Configurations.update(cls.api_client,
                                              "vmware.root.disk.controller",
                                              value=cls.defaultdiskcontroller)
                Configurations.update(cls.api_client,
                                              "vmware.create.full.clone",
                                              value="false")
                Configurations.update(cls.api_client,
                                      "vmware.create.full.clone",
                                      value="false", storageid=cls.storageID)
                if cls.storageID:
                    StoragePool.update(cls.api_client, id=cls.storageID,
                                    tags="")

            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):

        self.apiclient = self.cloudstacktestclient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestDeployVmRootSize,self).tearDown()

    @classmethod
    def restartServer(cls):
        """Restart management server"""

        sshClient = SshClient(
                    cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management stop"
        sshClient.execute(command)

        command = "service cloudstack-management start"
        sshClient.execute(command)

        #Waits for management to come up in 5 mins, when it's up it will continue
        timeout = time.time() + 300
        while time.time() < timeout:
            if cls.isManagementUp() is True: return
            time.sleep(5)
        return cls.fail("Management server did not come up, failing")

    @classmethod
    def isManagementUp(cls):
        try:
            cls.api_client.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

    @attr(tags = ['advanced', 'basic', 'sg'], required_hardware="false")
    def test_00_deploy_vm_root_resize(self):
        """Test deploy virtual machine with root resize

        # Validate the following:
        # 1. listVirtualMachines returns accurate information
        # 2. root disk has new size per listVolumes
        # 3. Rejects non-supported hypervisor types
        """

        newrootsize = (self.template.size >> 30) + 2
        if (self.hypervisor.lower() == 'kvm' or self.hypervisor.lower() == 'xenserver'
                or self.hypervisor.lower() == 'vmware' or self.hypervisor.lower() == 'simulator'):

            accounts = Account.list(self.apiclient, id=self.account.id)
            self.assertEqual(validateList(accounts)[0], PASS,
                            "accounts list validation failed")
            initialResourceCount = int(accounts[0].primarystoragetotal)

            if self.hypervisor=="vmware":
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    zoneid=self.zone.id,
                    accountid=self.account.name,
                    domainid=self.domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    zoneid=self.zone.id,
                    accountid=self.account.name,
                    domainid=self.domain.id,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )

            list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )

            res=validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list VM "
                                                        "response")
            self.cleanup.append(self.virtual_machine)

            vm = list_vms[0]
            self.assertEqual(
                vm.id,
                self.virtual_machine.id,
                "Virtual Machine ids do not match"
            )
            self.assertEqual(
                vm.name,
                self.virtual_machine.name,
                "Virtual Machine names do not match"
            )
            self.assertEqual(
                vm.state,
                "Running",
                msg="VM is not in Running state"
            )

            # get root vol from created vm, verify it is correct size
            list_volume_response = list_volumes(
                self.apiclient,
                virtualmachineid=self.virtual_machine.id,
                type='ROOT',
                listall=True
            )
            res=validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list VM "
                                                        "response")
            rootvolume = list_volume_response[0]
            success = False
            if rootvolume is not None and rootvolume.size  == (newrootsize << 30):
                success = True

            self.assertEqual(
                success,
                True,
                "Check if the root volume resized appropriately"
            )

            response = matchResourceCount(
                self.apiclient, (initialResourceCount + newrootsize),
                RESOURCE_PRIMARY_STORAGE,
                accountid=self.account.id)
            self.assertEqual(response[0], PASS, response[1])
        else:
            self.debug("hypervisor %s unsupported for test 00, verifying it errors properly" % self.hypervisor)
            newrootsize = (self.template.size >> 30) + 2
            success = False
            try:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.testdata["virtual_machine"],
                    accountid=self.account.name,
                    zoneid=self.zone.id,
                    domainid=self.account.domainid,
                    serviceofferingid=self.service_offering.id,
                    templateid=self.template.id,
                    rootdisksize=newrootsize
                )
            except Exception as ex:
                if re.search("Hypervisor \S+ does not support rootdisksize override", str(ex)):
                    success = True
                else:
                    self.debug("Virtual machine create did not fail appropriately. Error was actually : " + str(ex));

            self.assertEqual(success, True, "Check if unsupported hypervisor %s fails appropriately" % self.hypervisor)

    @attr(tags = ['advanced', 'basic', 'sg'], required_hardware="false")
    def test_01_deploy_vm_root_resize(self):
        """Test proper failure to deploy virtual machine with rootdisksize of 0
        """
        newrootsize=0
        success=False

        if (self.hypervisor.lower() == 'kvm' or self.hypervisor.lower() == 'xenserver'
                or self.hypervisor.lower() == 'vmware' or self.hypervisor.lower() == 'simulator'):
            try:
                if self.hypervisor=="vmware":
                    self.virtual_machine = VirtualMachine.create(
                        self.apiclient, self.services["virtual_machine"],
                        zoneid=self.zone.id,
                        accountid=self.account.name,
                        domainid=self.domain.id,
                        serviceofferingid=self.services_offering_vmware.id,
                         templateid=self.template.id,
                        rootdisksize=newrootsize
                    )
                else:
                    self.virtual_machine = VirtualMachine.create(
                        self.apiclient, self.services["virtual_machine"],
                        zoneid=self.zone.id,
                        accountid=self.account.name,
                        domainid=self.domain.id,
                        serviceofferingid=self.service_offering.id,
                        templateid=self.template.id,
                        rootdisksize=newrootsize
                    )
            except Exception as ex:
                if "Root disk size should be a positive number" in str(ex):
                    success = True
                else:
                    self.debug("Virtual machine create did not fail appropriately. Error was actually : " + str(ex));

            self.assertEqual(success, True, "Check if passing 0 as rootdisksize fails appropriately")
        else:
            self.debug("test 01 does not support hypervisor type " + self.hypervisor)

    @attr(tags = ['advanced', 'basic', 'sg'], required_hardware="false", BugId="CLOUDSTACK-6984")
    def test_02_deploy_vm_root_resize(self):
        """Test proper failure to deploy virtual machine with rootdisksize less than template size
        """
        newrootsize = (self.template.size >> 30) - 1
        success=False
        self.assertEqual(newrootsize > 0, True, "Provided template is less than 1G in size, cannot run test")

        if (self.hypervisor.lower() == 'kvm' or self.hypervisor.lower() == 'xenserver'
                or self.hypervisor.lower() == 'vmware' or self.hypervisor.lower() == 'simulator'):
            try:
                if self.hypervisor=="vmware":
                    self.virtual_machine = VirtualMachine.create(
                        self.apiclient, self.services["virtual_machine"],
                        zoneid=self.zone.id,
                        accountid=self.account.name,
                        domainid=self.domain.id,
                        serviceofferingid=self.services_offering_vmware.id,
                        templateid=self.template.id,
                        rootdisksize=newrootsize
                    )
                else:
                    self.virtual_machine = VirtualMachine.create(
                        self.apiclient, self.services["virtual_machine"],
                        zoneid=self.zone.id,
                        accountid=self.account.name,
                        domainid=self.domain.id,
                        serviceofferingid=self.service_offering.id,
                        templateid=self.template.id,
                        rootdisksize=newrootsize
                )
            except Exception as ex:
                    if "rootdisksize override is smaller than template size" in str(ex):
                        success = True
                    else:
                        self.debug("Virtual machine create did not fail appropriately. Error was actually : " + str(ex));

            self.assertEqual(success, True, "Check if passing rootdisksize < templatesize fails appropriately")
        else:
            self.debug("test 02 does not support hypervisor type " + self.hypervisor)



