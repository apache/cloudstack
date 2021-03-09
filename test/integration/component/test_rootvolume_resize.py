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

""" P1 tests for testing resize of  root volume functionality

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/
    Root+Resize+Support

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-9829
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Resources,
                             Domain,
                             Volume,
                             Snapshot,
                             Template,
                             VmSnapshot,
                             Host,
                             Configurations,
                             StoragePool)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               list_snapshots,
                               list_hosts,
                               list_configurations,
                               list_storage_pools)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.codes import (PASS,
                          FAIL,
                          FAILED,
                          RESOURCE_PRIMARY_STORAGE,
                          INVALID_INPUT)
from marvin.lib.utils import checkVolumeSize
import time
from marvin.sshClient import SshClient


class TestResizeVolume(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestResizeVolume, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = (cls.testClient.getHypervisorInfo()).lower()
        cls.storageID = None
        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cls.testClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.unsupportedHypervisorType = False
        cls.updateclone = False
        if cls.hypervisor not in ['xenserver',"kvm","vmware"]:
            cls.unsupportedHypervisorType=True
            return
        cls.template = get_template(
            cls.api_client,
            cls.zone.id
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        try:
            cls.parent_domain = Domain.create(cls.api_client,
                                              services=cls.services[
                                                  "domain"],
                                              parentdomainid=cls.domain.id)
            cls.parentd_admin = Account.create(cls.api_client,
                                               cls.services["account"],
                                               admin=True,
                                               domainid=cls.parent_domain.id)
            cls._cleanup.append(cls.parentd_admin)
            cls._cleanup.append(cls.parent_domain)
            list_pool_resp = list_storage_pools(cls.api_client,
                                               account=cls.parentd_admin.name,domainid=cls.parent_domain.id)
            res = validateList(list_pool_resp)
            if res[2]== INVALID_INPUT:
                raise Exception("Failed to  list storage pool-no storagepools found ")
            #Identify the storage pool type  and set vmware fullclone to true if storage is VMFS
            if cls.hypervisor == 'vmware':
                for strpool in list_pool_resp:
                    if strpool.type.lower() == "vmfs" or strpool.type.lower()== "networkfilesystem":
                        list_config_storage_response = list_configurations(
                            cls.api_client
                            , name=
                            "vmware.create.full.clone",storageid=strpool.id)
                        res = validateList(list_config_storage_response)
                        if res[2]== INVALID_INPUT:
                         raise Exception("Failed to  list configurations ")
                        if list_config_storage_response[0].value == "false":
                            Configurations.update(cls.api_client,
                                                  "vmware.create.full.clone",
                                                  value="true",storageid=strpool.id)
                            cls.updateclone = True
                            StoragePool.update(cls.api_client,id=strpool.id,tags="scsi")
                            cls.storageID = strpool.id
                            cls.unsupportedStorageType = False
                            break
                    else:
                        cls.unsupportedStorageType = True
            # Creating service offering with normal config
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"])
            cls.services_offering_vmware=ServiceOffering.create(
                cls.api_client,cls.services["service_offering"],tags="scsi")
            cls._cleanup.extend([cls.service_offering,cls.services_offering_vmware])

        except Exception as e:
            cls.tearDownClass()
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used

            if cls.updateclone:
                Configurations.update(cls.api_client,
                                      "vmware.create.full.clone",
                                      value="false",storageid=cls.storageID)

            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        if self.unsupportedStorageType:
            self.skipTest("Tests are Skipped - unsupported Storage type used ")
        elif self.unsupportedHypervisorType:
            self.skipTest("Tests are Skipped - unsupported Hypervisor type used")

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots

            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def updateResourceLimits(self, accountLimit=None, domainLimit=None):
        """Update primary storage limits of the parent domain and its
        child domains"""
        try:
            if domainLimit:
                # Update resource limit for domain
                Resources.updateLimit(self.apiclient, resourcetype=10,
                                      max=domainLimit,
                                      domainid=self.parent_domain.id)
            if accountLimit:
                # Update resource limit for domain
                Resources.updateLimit(self.apiclient,
                                      resourcetype=10,
                                      max=accountLimit,
                                      account=self.parentd_admin.name,
                                      domainid=self.parent_domain.id)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]


    def setupAccounts(self):
        try:
            self.parent_domain = Domain.create(self.apiclient,
                                              services=self.services[
                                                  "domain"],
                                              parentdomainid=self.domain.id)
            self.parentd_admin = Account.create(self.apiclient,
                                               self.services["account"],
                                               admin=True,
                                               domainid=self.parent_domain.id)
            # Cleanup the resources created at end of test
            self.cleanup.append(self.parent_domain)
            self.cleanup.append(self.parentd_admin)


        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    def chk_volume_resize(self, apiclient, vm):

        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )
        # get root vol from created vm, verify its size
        list_volume_response = Volume.list(
            apiclient,
            virtualmachineid=vm.id,
            type='ROOT',
            listall='True'
        )
        rootvolume = list_volume_response[0]
        if vm.state == "Running" and \
                (vm.hypervisor.lower() == "xenserver" or \
                             vm.hypervisor.lower() == "vmware"):
            self.virtual_machine.stop(apiclient)
            time.sleep(self.services["sleep"])
            if vm.hypervisor.lower() == "vmware":
                rootdiskcontroller = self.getDiskController(vm)
                if rootdiskcontroller!="scsi":
                    raise Exception("root volume resize only supported on scsi disk ,"
                                    "please check rootdiskcontroller type")

        rootvolobj = Volume(rootvolume.__dict__)
        newsize = (rootvolume.size >> 30) + 2
        success = False
        if rootvolume is not None:
            try:
                rootvolobj.resize(apiclient, size=newsize)
                if vm.hypervisor.lower() == "xenserver" or \
                                vm.hypervisor.lower() == "vmware":
                    self.virtual_machine.start(apiclient)
                    time.sleep(self.services["sleep"])
                ssh = SshClient(self.virtual_machine.ssh_ip, 22,
                                "root", "password")
                newsizeinbytes = newsize * 1024 * 1024 * 1024

                if vm.hypervisor.lower() == "xenserver":
                    volume_name = "/dev/xvd" + \
                                  chr(ord('a') +
                                      int(
                                          list_volume_response[0].deviceid))
                    self.debug(" Using XenServer"
                               " volume_name: %s" % volume_name)
                    ret = checkVolumeSize(ssh_handle=ssh,
                                          volume_name=volume_name,
                                          size_to_verify=newsizeinbytes)
                    success = True
                elif vm.hypervisor.lower() == "kvm":
                    volume_name = "/dev/vd" + chr(ord('a') + int
                    (list_volume_response[0]
                     .deviceid))
                    self.debug(" Using KVM volume_name:"
                               " %s" % volume_name)
                    ret = checkVolumeSize(ssh_handle=ssh,
                                          volume_name=volume_name,
                                          size_to_verify=newsizeinbytes)
                    success = True
                elif vm.hypervisor.lower() == "vmware":
                    ret = checkVolumeSize(ssh_handle=ssh,
                                          volume_name="/dev/sdb",
                                          size_to_verify=newsizeinbytes)
                    success = True
                self.debug(" Volume Size Expected %s "
                           " Actual :%s" % (newsizeinbytes, ret[1]))
            except Exception as e:
                # need to write the rootdisk controller  code.
                if vm.hypervisor == "vmware" and rootdiskcontroller == "ide":
                    assert "Found unsupported root disk " \
                           "controller :ide" in e.message, \
                        "able to resize ide root volume Testcase failed"
                else:
                    raise Exception("fail to resize the volume: %s" % e)
        else:
            self.debug("hypervisor %s unsupported for test "
                       ", verifying it errors properly" % self.hypervisor)
            success = False
        return success

    def getDiskController(self, vm, diskcontroller="ide"):

        if vm.hypervisor.lower() == "vmware":
            try:
                qresultvmuuid = self.dbclient.execute(
                    "select id from vm_instance where uuid = '%s' ;" %
                    vm.id
                )
                self.assertNotEqual(
                    len(qresultvmuuid),
                    0,
                    "Check DB Query result set"
                )
                vmid = int(qresultvmuuid[0][0])
                qresult = self.dbclient.execute(
                    "select rootDiskController from"
                    " user_vm_details where id = '%s';" % vmid
                )
                self.debug("Query result: %s" % qresult)
                diskcontroller = qresult[0][0]
            except Exception as e:
                raise Exception("Warning: Exception while"
                                " checking usage event for the "
                                "root_volume_resize : %s" % e)
        return diskcontroller

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_create__snapshot_new_resized_rootvolume_size(self):
        """Test create snapshot on resized root volume

        # Validate the following

        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Perform(resize)  of the root  volume
        # 3. Perform snapshot on resized volume
        """

        # deploy a vm

        try:
            if self.updateclone:

                    self.virtual_machine = VirtualMachine.create(
                        self.apiclient, self.services["virtual_machine"],
                        accountid=self.parentd_admin.name,
                        domainid=self.parent_domain.id,
                        serviceofferingid=self.services_offering_vmware.id,
                        mode=self.zone.networktype
                    )
            else:
                    self.virtual_machine = VirtualMachine.create(
                        self.apiclient, self.services["virtual_machine"],
                        accountid=self.parentd_admin.name,
                        domainid=self.parent_domain.id,
                        serviceofferingid=self.service_offering.id,
                        mode=self.zone.networktype
                    )

            # listVirtual machine
            list_vms = VirtualMachine.list(self.apiclient,
                                           id=self.virtual_machine.id)

            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" %
                self.virtual_machine.id
            )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")

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
            result = self.chk_volume_resize(self.apiclient, vm)
            if result:
                # get root vol from created vm, verify it is correct size
                list_volume_response = Volume.list(
                    self.apiclient,
                    virtualmachineid=
                    self.virtual_machine.id,
                    type='ROOT',
                    listall='True'
                )
                res = validateList(list_volume_response)
                self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
                rootvolume = list_volume_response[0]
                self.debug("Creating a Snapshot from root  volume: "
                           "%s" % rootvolume.id)
                snapshot = Snapshot.create(
                    self.apiclient,
                    rootvolume.id,
                    account=self.parentd_admin.name,
                    domainid=self.parent_domain.id
                )
                snapshots = list_snapshots(
                    self.apiclient,
                    id=snapshot.id
                )
                res = validateList(snapshots)
                self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
                self.assertEqual(
                    snapshots[0].id,
                    snapshot.id,
                    "Check resource id in list resources call"
                )
            else:
                self.debug("Volume resize is failed")

        except Exception as e:
            raise Exception("Exception while performing"
                            "  the snapshot on resized root volume"
                            " test case: %s" % e)

        self.cleanup.append(self.virtual_machine)
        self.cleanup.append(snapshot)

        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_create__template_new_resized_rootvolume_size(self):
        """Test create Template resized root volume

        # Validate the following

        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Perform(resize)  of the root  volume
        # 3. Stop the vm
        # 4. Create a template from  resized  root volume
        """

        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])
        apiclient = self.testClient.getUserApiClient(
            UserName=self.parentd_admin.name,
            DomainName=self.parentd_admin.domain)
        self.assertNotEqual(apiclient, FAILED, "Failed to get api client\
                            of account: %s" % self.parentd_admin.name)

        # deploy a vm
        try:
            if self.updateclone:

                self.virtual_machine = VirtualMachine.create(
                    apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    mode=self.zone.networktype
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id,
                    mode=self.zone.networktype
                )

            # listVirtual macine
            list_vms = VirtualMachine.list(apiclient,
                                           id=self.virtual_machine.id)
            self.debug("Verify listVirtualMachines response"
                       " for virtual machine: %s" % self.virtual_machine.id
                       )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
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
            list_volume_response = Volume.list(
                apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            rootvolume = list_volume_response[0]
            newsize = (rootvolume.size >> 30) + 2
            result = self.chk_volume_resize(apiclient, vm)
            if result:
                try:
                    # create a template from stopped VM instances root volume
                    if vm.state == "Running":
                        self.virtual_machine.stop(apiclient)
                    template_from_root = Template.create(
                        apiclient,
                        self.services["template"],
                        volumeid=rootvolume.id,
                        account=self.parentd_admin.name,
                        domainid=self.parent_domain.id)
                    list_template_response = Template.list(
                        apiclient,
                        id=template_from_root.id,
                        templatefilter="all")
                    res = validateList(list_template_response)
                    self.assertNotEqual(res[2], INVALID_INPUT, "Check if template exists in ListTemplates")
                    # Deploy new virtual machine using template
                    self.virtual_machine2 = VirtualMachine.create(
                        apiclient,
                        self.services["virtual_machine"],
                        templateid=template_from_root.id,
                        accountid=self.parentd_admin.name,
                        domainid=self.parent_domain.id,
                        serviceofferingid=self.service_offering.id,
                    )

                    vm_response = VirtualMachine.list(
                        apiclient,
                        id=self.virtual_machine2.id,
                        account=self.parentd_admin.name,
                        domainid=self.parent_domain.id
                    )
                    res = validateList(vm_response)
                    self.assertNotEqual(res[2], INVALID_INPUT, "Check for list VM response return valid list")
                    self.cleanup.append(self.virtual_machine2)
                    self.cleanup.reverse()
                    vm2 = vm_response[0]
                    self.assertEqual(
                        vm2.state,
                        'Running',
                        "Check the state of VM created from Template"
                    )
                    list_volume_response = Volume.list(
                        apiclient,
                        virtualmachineid=vm2.id,
                        type='ROOT',
                        listall='True'
                    )
                    self.assertEqual(
                        list_volume_response[0].size,
                        (newsize * 1024 * 1024 * 1024),

                        "Check for root volume size  not matched with template size"
                    )
                except Exception as e:
                    raise Exception("Exception while resizing the "
                                    "root volume: %s" % e)

            else:
                self.debug(" volume resize failed for root volume")
        except Exception as e:
            raise Exception("Exception while performing"
                            " template creation from "
                            "resized_root_volume : %s" % e)

        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_vmsnapshot__on_resized_rootvolume_vm(self):
        """Test vmsnapshot on resized root volume

        # Validate the following

        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Perform(resize)  of the root  volume
        # 3. Perform VM snapshot on VM
        """

        # deploy a vm

        try:
            if self.updateclone:

                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    mode=self.zone.networktype
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id,
                    mode=self.zone.networktype
                )

            # listVirtual macine
            list_vms = VirtualMachine.list(self.apiclient,
                                           id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
            self.cleanup.append(self.virtual_machine)
            vm = list_vms[0]
            self.assertEqual(
                vm.id,
                self.virtual_machine.id,
                "Virtual Machine ids do not match"
            )
            # get root vol from created vm, verify it is correct size
            list_volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            rootvolume = list_volume_response[0]
            newsize = (rootvolume.size >> 30) + 2
            result = self.chk_volume_resize(self.apiclient, vm)
            if result:
                try:
                    if 'kvm' in self.hypervisor.lower():
                        self.virtual_machine.stop(self.apiclient)
                    virtualmachine_snapshot = VmSnapshot.create \
                        (self.apiclient, self.virtual_machine.id)
                    virtulmachine_snapshot_list = VmSnapshot. \
                        list(self.apiclient,
                             vmsnapshotid=virtualmachine_snapshot.id)
                    status = validateList(virtulmachine_snapshot_list)
                    self.assertEqual(
                        PASS,
                        status[0],
                        "Listing of configuration failed")
                    self.assertEqual(virtualmachine_snapshot.id,
                                     virtulmachine_snapshot_list[0].id,
                                     "Virtual Machine Snapshot id do not match")
                except Exception as  e:
                    raise Exception("Issue CLOUDSTACK-10080: Exception while performing"
                                    " vmsnapshot: %s" % e)
            else:
                self.debug("volume resize failed for root volume")
        except Exception as e:
            raise Exception("Exception while performing"
                            " vmsnapshot on resized volume Test: %s" % e)


    @attr(tags=["advanced"], required_hardware="true")
    def test_04_vmreset_after_migrate_vm__rootvolume_resized(self):
        """Test migrate vm after  root volume resize

        # Validate the following

        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Perform(resize)  of the root  volume
        # 3. migrate vm from host to another
        # 4. perform vm reset after vm migration

        """

        try:
            if self.updateclone:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    mode=self.zone.networktype
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id,
                    mode=self.zone.networktype
                )

            # listVirtual macine
            list_vms = VirtualMachine.list(self.apiclient,
                                           id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
            self.cleanup.append(self.virtual_machine)
            vm = list_vms[0]
            self.assertEqual(
                vm.id,
                self.virtual_machine.id,
                "Virtual Machine ids do not match"
            )
            # get root vol from created vm, verify it is correct size
            list_volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            rootvolume = list_volume_response[0]
            result = self.chk_volume_resize(self.apiclient, vm)
            if result:
                try:
                    list_host_response = list_hosts(
                        self.apiclient,
                        id=self.virtual_machine.hostid
                    )
                    res = validateList(list_host_response)
                    self.assertNotEqual(res[2], INVALID_INPUT, "listHosts returned invalid object in response")

                    sourcehost = list_host_response[0]
                    try:
                        self.list_hosts_suitable = Host.listForMigration \
                            (self.apiclient,
                             virtualmachineid=self.virtual_machine.id
                             )
                    except Exception as e:
                        self.debug("Not found suitable host")
                        raise Exception("Exception while getting hosts"
                                        " list suitable for migration: %s" % e)
                    self.virtualmachine_migrate_response = \
                        self.virtual_machine.migrate(
                            self.apiclient,
                            self.list_hosts_suitable[0].id)
                    list_vms = VirtualMachine.list(
                        self.apiclient,
                        id=self.virtual_machine.id,
                        hostid=self.list_hosts_suitable[0].id)

                    res = validateList(list_vms)
                    self.assertNotEqual(res[2], INVALID_INPUT, "listVirtualMachines returned "
                                                               "invalid object in response")

                    self.virtual_machine_reset = self.virtual_machine.restore \
                        (self.apiclient,
                         self.services["virtual_machine"]["template"])
                    list_restorevolume_response = Volume.list(
                        self.apiclient,
                        virtualmachineid=
                        self.virtual_machine.id,
                        type='ROOT',
                        listall='True'
                    )
                    restorerootvolume = list_restorevolume_response[0]
                    self.assertEqual(rootvolume.size, restorerootvolume.size,
                                     "root volume  and restore root"
                                     " volume size differs - CLOUDSTACK-10079")
                except Exception as e:
                    raise Exception("Warning: Exception "
                                    "during VM migration: %s" % e)

        except Exception as e:
            raise Exception("Warning: Exception during executing"
                            " the test-migrate_vm_after_rootvolume_resize: %s" % e)

        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_5_vmdeployment_with_size(self):
        """Test vm deployment with new rootdisk size parameter

        # Validate the following
        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Verify the  root disksize after deployment

        """
        templateSize = (self.template.size / (1024 ** 3))
        newsize = templateSize + 2
        # deploy a vm
        try:
            if self.updateclone:

                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    mode=self.zone.networktype
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id,
                    mode=self.zone.networktype
                )

            # listVirtual macine
            list_vms = VirtualMachine.list(self.apiclient,
                                           id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s"
                % self.virtual_machine.id
            )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
            self.cleanup.append(self.virtual_machine)
            vm = list_vms[0]
            ssh = SshClient(self.virtual_machine.ssh_ip, 22, "root",
                            "password")
            newsize = newsize * 1024 * 1024 * 1024
            list_volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            if vm.hypervisor.lower() == "xenserver":
                volume_name = "/dev/xvd" + chr(ord('a') + int(
                    list_volume_response[0].deviceid))
                self.debug(" Using XenServer"
                           " volume_name: %s" % volume_name)
                ret = checkVolumeSize(ssh_handle=ssh,
                                      volume_name=volume_name,
                                      size_to_verify=newsize)
            elif vm.hypervisor.lower() == "kvm":
                volume_name = "/dev/vd" + chr(ord('a')
                                              + int(
                    list_volume_response[0].deviceid))
                self.debug(" Using KVM volume_name: %s" % volume_name)
                ret = checkVolumeSize(ssh_handle=ssh,
                                      volume_name=volume_name,
                                      size_to_verify=newsize)
            elif vm.hypervisor.lower() == "vmware":
                ret = checkVolumeSize(ssh_handle=ssh,
                                      volume_name="/dev/sdb",
                                      size_to_verify=newsize)
            self.debug(" Volume Size Expected %s"
                       "  Actual :%s" % (newsize, ret[1]))
        except Exception as e:
            raise Exception("Warning: Exception during"
                            " VM deployment with new"
                            "  rootdisk paramter : %s" % e)


    @attr(tags=["advanced"], required_hardware="true")
    def test_6_resized_rootvolume_with_lessvalue(self):
        """Test resize root volume with less than original volume size

        # Validate the following

        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Perform(resize)  of the root  volume with less
         than current root volume
        # 3. Check for proper error message

        """

        # deploy a vm
        try:
            if self.updateclone:

                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    mode=self.zone.networktype
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id,
                    mode=self.zone.networktype
                )

            # listVirtual macine
            time.sleep(self.services["sleep"])
            list_vms = VirtualMachine.list(self.apiclient,
                                           id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
            self.cleanup.append(self.virtual_machine)
            vm = list_vms[0]
            self.assertEqual(
                vm.id,
                self.virtual_machine.id,
                "Virtual Machine ids do not match"
            )
            # get root vol from created vm, verify it is correct size
            list_volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            if vm.state == "Running" and (
                            vm.hypervisor.lower() == "xenserver" or
                            vm.hypervisor.lower() == "vmware"):
                self.virtual_machine.stop(self.apiclient)
                time.sleep(self.services["sleep"])

            rootvolume = list_volume_response[0]
            # converting json response to Volume  Object
            rootvol = Volume(rootvolume.__dict__)
            newsize = (rootvolume.size >> 30) - 1
            success = False
            if rootvolume is not None and 'vmware' in vm.hypervisor.lower():
                try:
                    rootvol.resize(self.apiclient, size=newsize)
                except Exception as e:
                    assert "Shrink operation on ROOT volume not supported" \
                           in e.message, \
                        "TestCase Failed,able to resize root volume  or error message is not matched"
        except Exception as e:
            raise Exception("Warning: Exception "
                            "during executing test resize"
                            " volume with less value : %s" % e)
            if rootvol is not None and 'kvm' or 'xenserver' in vm.hypervisor.lower():
                rootvol.resize(self.apiclient, size=newsize)

    @attr(tags=["advanced"], required_hrdware="true")
    def test_7_usage_events_after_rootvolume_resized_(self):
        """Test check usage events after root volume resize

        # Validate the following

        # 1. Deploy a VM without any disk offering (only root disk)
        # 2. Perform(resize)  of the root  volume
        # 3. Check the corresponding usage events
        """

        # deploy a vm
        try:
            if self.updateclone:

                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id,
                    mode=self.zone.networktype
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    self.apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id,
                    mode=self.zone.networktype
                )
            # listVirtual macine
            time.sleep(self.services["sleep"])
            list_vms = VirtualMachine.list(self.apiclient,
                                           id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s"
                % self.virtual_machine.id
            )
            res = validateList(list_vms)
            self.assertNotEqual(res[2], INVALID_INPUT, "Invalid list response")
            self.cleanup.append(self.virtual_machine)
            vm = list_vms[0]
            self.assertEqual(
                vm.id,
                self.virtual_machine.id,
                "Virtual Machine ids do not match"
            )
            # get root vol from created vm, verify it is correct size
            list_volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            if vm.state == "Running" and (
                            vm.hypervisor.lower() == "xenserver" or
                            vm.hypervisor.lower() == "vmware"):
                self.virtual_machine.stop(self.apiclient)
                time.sleep(self.services["sleep"])
            rootvolume = list_volume_response[0]
            # converting json response to Volume  Object
            rootvol = Volume(rootvolume.__dict__)
            newsize = (rootvolume.size >> 30) + 2
            success = False
            if rootvolume is not None:
                try:
                    rootvol.resize(self.apiclient, size=newsize)
                    qresultset = self.dbclient.execute(
                        "select id from account where uuid = '%s';"
                        % self.parentd_admin.id)

                    res = validateList(qresultset)
                    self.assertNotEqual(res[2], INVALID_INPUT, "Check DB Query result set")
                    qresult = qresultset[0]
                    account_id = qresult[0]
                    self.debug("select type,size from usage_event"
                               " where account_id = '%s';"
                               % account_id)
                    qresultsize = self.dbclient.execute(
                        "select size from usage_event where account_id = '%s' "
                        "and type='VOLUME.RESIZE' ORDER BY ID  DESC LIMIT 1;"
                        % account_id
                    )
                    res = validateList(qresultsize)
                    self.assertNotEqual(res[2], INVALID_INPUT, "Check DB Query result set")
                    qresult = int(qresultsize[0][0])
                    self.debug("Query result: %s" % qresult)
                    self.assertEqual(
                        qresult,
                        (newsize * 1024 * 1024 * 1024),
                        "Usage event not logged properly with right volume"
                        " size please check ")
                except Exception as e:
                    raise Exception("Warning: Exception while checking usage "
                                    "event for the root volume resize : %s"
                                    % e)
        except Exception as e:
            raise Exception("Warning: Exception performing "
                            "usage_events_after_rootvolume_resized Test  : %s"
                            % e)

    @attr(tags=["advanced"], required_hardware="true")
    def test_08_increase_volume_size_within_account_limit(self):
        """Test increasing volume size within the account limit and verify
           primary storage usage

        # Validate the following
        # 1. Create a domain and its admin account
        # 2. Set account primary storage limit well beyond (20 GB volume +
        #    template size of VM)
        # 3. Deploy a VM without any disk offering (only root disk)
        #
        # 4. Increase (resize) the volume to 20 GB
        # 6. Resize operation should be successful and primary storage count
        #    for account should be updated successfully"""

        # Setting up account and domain hierarchy

        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])
        apiclient = self.testClient.getUserApiClient(
            UserName=self.parentd_admin.name,
            DomainName=self.parentd_admin.domain)
        self.assertNotEqual(apiclient, FAILED, "Failed to get api client\
                            of account: %s" % self.parentd_admin.name)

        templateSize = (self.template.size / (1024 ** 3))
        accountLimit = (templateSize + 20)
        response = self.updateResourceLimits(accountLimit=accountLimit)
        self.assertEqual(response[0], PASS, response[1])

        try:

            if self.updateclone:

                self.virtual_machine = VirtualMachine.create(
                    apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.services_offering_vmware.id
                )
            else:
                self.virtual_machine = VirtualMachine.create(
                    apiclient, self.services["virtual_machine"],
                    accountid=self.parentd_admin.name,
                    domainid=self.parent_domain.id,
                    serviceofferingid=self.service_offering.id
                )
            list_vms = VirtualMachine.list(apiclient,
                                           id=self.virtual_machine.id)
            self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
            self.assertEqual(
                isinstance(list_vms, list),
                True,
                "List VM response was not a valid list"
            )
            self.cleanup.append(self.virtual_machine)
            self.cleanup.reverse()
            vm = list_vms[0]
            list_volume_response = Volume.list(
                apiclient,
                virtualmachineid=
                self.virtual_machine.id,
                type='ROOT',
                listall='True'
            )
            res = validateList(list_volume_response)
            self.assertNotEqual(res[2], INVALID_INPUT, "listVolumes returned invalid object in response")
            if vm.state == "Running" and \
                    (vm.hypervisor.lower() == "xenserver" or
                             vm.hypervisor.lower() == "vmware"):
                self.virtual_machine.stop(self.apiclient)
                time.sleep(self.services["sleep"])
            rootvolume = list_volume_response[0]
            # converting json response to Volume  Object
            rootvol = Volume(rootvolume.__dict__)
            newsize = (rootvolume.size >> 30) + 20
            if rootvolume is not None:
                try:
                    rootvol.resize(apiclient, size=newsize)
                    response = matchResourceCount(
                        self.apiclient, newsize,
                        RESOURCE_PRIMARY_STORAGE,
                        accountid=self.parentd_admin.id)
                    if response[0] == FAIL:
                        raise Exception(response[1])
                except Exception as e:
                    self.fail("Failed with exception: %s" % e)

        except Exception as e:
            raise Exception("Warning: Exception while checking primary"
                            " storage capacity after root "
                            "volume resize  : %s" % e)

        return
