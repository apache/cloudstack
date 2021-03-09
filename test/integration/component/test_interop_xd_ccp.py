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
""" BVT tests for XenDesktop CloudPlatform InterOperability
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (restoreVirtualMachine,
                                  destroyVirtualMachine,
                                  listVirtualMachines,
                                  attachIso,
                                  detachIso,
                                  listHypervisorCapabilities,
                                  deleteVolume,
                                  createVolume,
                                  attachVolume)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (SecurityGroup,
                             NetworkOffering,
                             Account,
                             Template,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Configurations,
                             Volume,
                             DiskOffering,
                             Domain,
                             StoragePool,
                             Region,
                             Zone,
                             Network)
from marvin.lib.common import (get_domain,
                                get_zone,
                                get_template,
                                get_pod,
                                list_hosts,
                                get_windows_template,
                                list_virtual_machines
                                )

from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
#Import System modules
import time
import random
import string
import unittest

_multiprocess_shared_ = True
class TestXDCCPInterop(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestXDCCPInterop, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        hosts = list_hosts(
               cls.apiclient,
               type="Routing"
           )
   
        if hosts is None:
               raise unittest.SkipTest(
                   "There are no hypervisor's available.Check list hosts response")
        for hypervisorhost in hosts :
                    if hypervisorhost.hypervisor == "XenServer":
                        cls.uploadtemplateformat="VHD"
                        break
                    elif hypervisorhost.hypervisor== "VMware":
                        cls.uploadtemplateformat="OVA"
                        break
                    elif hypervisorhost.hypervisor== "KVM":
                       cls.uploadtemplateformat="KVM"
                    break

        if cls.uploadtemplateformat=="KVM":
            raise unittest.SkipTest("Interop is not supported on KVM")

        cls.uploadurl=cls.services["interop"][cls.uploadtemplateformat]["url"]
  
        cls.xtemplate = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.xtemplate == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id,
            admin=False
        )
        cls.debug(cls.account.id)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["large"]
        )

        cls.template = get_windows_template(
            cls.apiclient,
            cls.zone.id,
            ostype_desc="Windows 8 (64-bit)")
        #cls.template = get_windows_template(cls.apiclient, cls.zone.id ,ostype_desc="Windows Server 2012 (64-bit)")

        if cls.template == FAILED:
            if "http://pleaseupdateURL/" in cls.uploadurl:
                raise unittest.SkipTest(
                    "Check Test Data file if it has the valid template URL")
            cls.template = Template.register(
                cls.apiclient,
                cls.services["interop"][cls.uploadtemplateformat],
                zoneid=cls.zone.id,
                domainid=cls.account.domainid,
                account=cls.account.name
            )
            timeout = cls.services["vgpu"]["timeout"]

            while True:
                time.sleep(cls.services["vgpu"]["sleep"]) 
                list_template_response = Template.list(
                    cls.apiclient,
                    templatefilter=cls.services["templatefilter"],
                    id=cls.template.id
                )
                if (isinstance(list_template_response, list)) is not True:
                    raise unittest.SkipTest(
                        "Check list template api response returns a valid list")

                if len(list_template_response) is None:
                    raise unittest.SkipTest(
                        "Check template registered is in List Templates")

                template_response = list_template_response[0]
                if template_response.isready:
                    break

                if timeout == 0:
                    raise unittest.SkipTest(
                        "Failed to download template(ID: %s). " %
                        template_response.id)

                timeout = timeout - 1
        cls.volume=[]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        user_data = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2500))
        cls.services["virtual_machine"]["userdata"] = user_data


#        cls.services["large"]["zoneid"] = cls.zone.id
#        cls.services["large"]["template"] = cls.template.id

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,  
            mode=cls.services['mode'],
            startvm="false"
        )
        cls.user_api_client = cls.testClient.getUserApiClient(
            UserName=cls.account.name,
            DomainName=cls.account.domain
        )


        cls.cleanup = [
            cls.service_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        #self.user_api_client = self.testClient.getApiClient()
        #self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []


    @attr(tags = ["devcloud", "advanced", "advancedns", "basic", "sg"], required_hardware="true")
    def test_01_create_list_delete_security_group(self):
        """
        Test Security Group Creation,List,Deletion on a Basic
        """
        if self.zone.networktype!="Basic":
            self.skipTest("Security Group creation is applicable only with Basic zone setup. skipping")

        sg=SecurityGroup.create(self.user_api_client,
                                self.services("security_group")
                                )

        listsg=SecurityGroup.list(self.user_api_client,id=sg.id)

        if sg.name!=listsg[0].name:
            self.fail("Security Group is not created with specified details")

        sg.delete(self.user_api_client)

        return

    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_01_list_all_regions_with_noparams(self):
        """
        Test List Regions With No Parameters
        """
        regionavailable="no"
        listregions=Region.list(self.user_api_client)

        self.assertEqual(
                            isinstance(listregions, list),
                            True,
                            "Check listRegions response returns a valid list"
                        )

        for reg1 in listregions:
            if reg1.name=="Local":
                regionavailable="yes"
                exit
        if regionavailable=="no":
            self.fail("There is no region created")

    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_02_list_all_diskofferings_with_noparams(self):
        """
        Test List Disk Offerings with No Parameters
        """

        diskofferingvailable=0
        listdiskofferings=DiskOffering.list(self.user_api_client)

        self.assertEqual(
                            isinstance(listdiskofferings, list),
                            True,
                            "Check list Disk Offerings response returns a valid list"
                        )

        for diskoffering1 in listdiskofferings:
            if diskoffering1.name=="Small":
                diskofferingvailable=diskofferingvailable+1
            elif diskoffering1.name=="Medium":
                diskofferingvailable=diskofferingvailable+1
            elif diskoffering1.name=="Large":
                diskofferingvailable=diskofferingvailable+1
            elif diskoffering1.name=="Custom":
                diskofferingvailable=diskofferingvailable+1

        if diskofferingvailable<4:
            self.fail("All the default disk offerings are not listed")


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_03_list_all_Serviceofferings_with_noparams(self):
        """
        Test List Service Offerings with No Parameters
        """

        serviceofferingvailable=0
        listserviceofferings=ServiceOffering.list(self.user_api_client)

        self.assertEqual(
                            isinstance(listserviceofferings, list),
                            True,
                            "Check list Service Offerings response returns a valid list"
                        )

        for serviceoffering1 in listserviceofferings:
            if serviceoffering1.name=="Small Instance":
                serviceofferingvailable=serviceofferingvailable+1
            elif serviceoffering1.name=="Medium Instance":
                serviceofferingvailable=serviceofferingvailable+1

        if serviceofferingvailable<2:
            self.fail("All the default service offerings are not listed")


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="false")
    def test_04_list_zones_with_no_params(self):

        """
        Test list zones
        """
        zonesavailable=0
        listallzones=Zone.list(self.user_api_client)

        self.assertEqual(
                            isinstance(listallzones, list),
                            True,
                            "Check list zones response returns a valid list"
                        )

        for zone1 in listallzones:
            if zone1.allocationstate=="Enabled":
                zonesavailable=zonesavailable+1
 
        if zonesavailable<1:
            self.fail("Check if zones are listed")

        return


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_05_validate_stopped_vm_deployment(self):
        """
        Test Deploy Virtual Machine in Stopped State
        """

        list_vm_response = VirtualMachine.list(
                                                 self.user_api_client,
                                                 id=self.virtual_machine.id
                                                 )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )
        vm_response = list_vm_response[0]
        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )
        self.assertEqual(
                    vm_response.name,
                    self.virtual_machine.name,
                    "Check virtual machine name in listVirtualMachines"
                    )
        self.assertEqual(
            vm_response.state,
            'Stopped',
             msg="VM is not in Stopped state"
        )
        return

    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_06_attachvolume_to_a_stopped_vm(self):
        """
        Test Attach Volume To A Stopped VM
        """

        list_vm_response = VirtualMachine.list(
                                                 self.user_api_client,
                                                 id=self.virtual_machine.id
                                                 )

        self.assertEqual(
            list_vm_response[0].state,
            'Stopped',
             msg="Check if VM is in Stopped state"
        )
        custom_disk_offering=DiskOffering.list(
                                self.user_api_client,
                                 name="custom"
                                 )

        self.__class__.volume = Volume.create(
            self.user_api_client,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=custom_disk_offering[0].id,
            size=1
        )

                # Check List Volume response for newly created volume
        list_volume_response = Volume.list(
                    self.user_api_client,
                    id=self.volume.id
                )
        self.assertNotEqual(
                    list_volume_response,
                    None,
                    "Check if volume exists in ListVolumes"
                )

                # Attach volume to VM
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = self.volume.id
        cmd.virtualmachineid = self.virtual_machine.id
        cmd.deviceid=1
        vol1=self.user_api_client.attachVolume(cmd)

            # Check all volumes attached to same VM
        list_volume_response = Volume.list(
                self.user_api_client,
                virtualmachineid=self.virtual_machine.id,
                type='DATADISK',
                listall=True
            )
        self.assertNotEqual(
                list_volume_response,
                None,
                "Check if volume exists in ListVolumes")
        self.assertEqual(
                isinstance(list_volume_response, list),
                True,
                "Check list volumes response for valid list")

        self.assertEqual(
                list_volume_response[0].deviceid,
                1,
                "Check listed volume device id is 1")

        return

    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_07_start_vm(self):
        """
        Test Start Stopped Virtual Machine with volumes attached 
        """

        list_vm_response = VirtualMachine.list(
                                                 self.user_api_client,
                                                 id=self.virtual_machine.id
                                                 )

        self.assertEqual(
            list_vm_response[0].state,
            'Stopped',
             msg="Check if VM is in Stopped state before starting it"
        )
        self.virtual_machine.start(self.user_api_client)

        time.sleep(600)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=self.virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
                )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_08_list_all_vms_with_zone_id(self):
        """
        Test list all vm's available with the zone id
        """

        vm_available=0
        
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.listall = True
        cmd.zoneid=self.zone.id

        list_vm_response=self.user_api_client.listVirtualMachines(cmd)
        """
        list_vm_response = VirtualMachine.list(
                                                 self.user_api_client,
                                                 zoneid=self.zone.id
                                                 )
        """
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list vm's with zone id response returns a valid list"
                        )
        for vm1 in list_vm_response:
            if vm1.id==self.virtual_machine.id:
                vm_available=vm_available+1

        if vm_available<1:
            self.fail("List VM's with zone id is not listing the expected vm details correctly")

        return


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="false")
    def test_09_reboot_vm(self):
        """Test Reboot Virtual Machine
        """

        self.virtual_machine.reboot(self.user_api_client)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=self.virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_10_detach_volume(self):
        """
        Test Detach Volume 
        """

        list_volume_response1 = Volume.list(
            self.user_api_client,
            id=self.volume.id
        )

        if  list_volume_response1[0].virtualmachineid is None:
            self.skipTest("Check if volume is attached to the VM before detach")

        self.virtual_machine.detach_volume(self.user_api_client, self.volume)

        # Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])

        list_volume_response = Volume.list(
            self.user_api_client,
            id=self.volume.id
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        volume1 = list_volume_response[0]
        self.assertEqual(
            volume1.virtualmachineid,
            None,
            "Check if volume state (detached) is reflected"
        )

        self.assertEqual(
            volume1.vmname,
            None,
            "Check if volume state (detached) is reflected"
        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_11_delete_detached_volume(self):
        """
        Delete a Volume unattached to an VM
        """

        list_volume_response1 = Volume.list(
            self.user_api_client,
            id=self.volume.id
        )

        if  list_volume_response1[0].virtualmachineid is not None:
            self.skipTest("Check if volume is detached before deleting")

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        self.user_api_client.deleteVolume(cmd)

        # Sleep to ensure the current state will reflected in other calls
        time.sleep(self.services["sleep"])

        list_volume_response = Volume.list(
            self.user_api_client,
            id=self.volume.id,
        )
        self.assertEqual(
            list_volume_response,
            None,
            "Volume %s was not deleted" % self.volume.id
        )
        return


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_12_stop_vm_with_force_false(self):
        """
        Test Stop Virtual Machine
        """

        list_vm_response = VirtualMachine.list(
                                                 self.user_api_client,
                                                 id=self.virtual_machine.id
                                                 )

        if list_vm_response[0].state!='Running':
            self.skipTest("Check if VM is in Running state before stopping it")

        try:
            self.virtual_machine.stop(self.user_api_client,forced="false")
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)
        return




    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="true")
    def test_13_destroy_vm(self):
        """
        Test destroy Virtual Machine
        """

        self.virtual_machine.delete(self.user_api_client, expunge=False)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=self.virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Destroyed",
                            "Check virtual machine is in destroyed state"
                        )
        return


    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="false")
    def test_14_restore_stopped_vm(self):

        """
        Test Restoring Stopped Virtual Machine
        """

        noffering=NetworkOffering.list(
                     self.user_api_client,
                     name="DefaultIsolatedNetworkOfferingWithSourceNatService"
                     )
        vmnetwork=Network.create(
                                 self.user_api_client,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=noffering[0].id,
                                zoneid=self.zone.id
                                 )

        list_nw_response = Network.list(
                                            self.user_api_client,
                                            id=vmnetwork.id
                                            )
        self.assertEqual(
                            isinstance(list_nw_response, list),
                            True,
                            "Check list response returns a valid networks list"
                        )
        restorevm = VirtualMachine.create(
            self.user_api_client,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services['mode'],
            networkids=list_nw_response[0].id,
            startvm="false"
        )


        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=restorevm.id
                                            )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in Stopped state"
                        )

        custom_disk_offering=DiskOffering.list(
                                self.apiclient,
                                 name="custom"
                                 )

        newvolume = Volume.create(
            self.user_api_client,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            diskofferingid=custom_disk_offering[0].id,
            size=1
        )

                # Attach volume to VM
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = newvolume.id
        cmd.virtualmachineid = restorevm.id
        cmd.deviceid=1
        vol1=self.user_api_client.attachVolume(cmd)

        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid=restorevm.id
        self.user_api_client.restoreVirtualMachine(cmd)

        time.sleep(600)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=restorevm.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in Stopped state"
                        )

        restorevm.start(self.user_api_client)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=restorevm.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )


        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )

        restorevm.delete(self.apiclient)

        vmnetwork.delete(self.user_api_client)

        return



    @attr(tags = ["devcloud", "advanced", "advancedns",  "basic", "sg"], required_hardware="false")
    def test_15_restore_vm_with_template_id(self):

        """
        Test restoring Virtual Machine with template id
        """

        noffering=NetworkOffering.list(
                     self.user_api_client,
                     name="DefaultIsolatedNetworkOfferingWithSourceNatService"
                     )
        vm1network=Network.create(
                                 self.user_api_client,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=noffering[0].id,
                                zoneid=self.zone.id
                                 )

        list_nw_response = Network.list(
                                            self.user_api_client,
                                            id=vm1network.id
                                            )
        self.assertEqual(
                            isinstance(list_nw_response, list),
                            True,
                            "Check list response returns a valid networks list"
                        )

        restorevm = VirtualMachine.create(
            self.user_api_client,
            self.services["small"],
            networkids=vm1network.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services['mode'],
            startvm="true"
        )


        time.sleep(600)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=restorevm.id
                                            )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )


        custom_disk_offering=DiskOffering.list(
                                self.user_api_client,
                                 name="custom"
                                 )

        newvolume = Volume.create(
            self.user_api_client,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            diskofferingid=custom_disk_offering[0].id,
            size=1
        )

                # Attach volume to VM
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = newvolume.id
        cmd.virtualmachineid = restorevm.id
        cmd.deviceid=1
        vol1=self.user_api_client.attachVolume(cmd)

        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = restorevm.id
        cmd.templateid = self.xtemplate.id
        self.user_api_client.restoreVirtualMachine(cmd)

        time.sleep(600)

        list_vm_response = VirtualMachine.list(
                                            self.user_api_client,
                                            id=restorevm.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in Stopped state"
                        )

        restorevm.delete(self.apiclient)

        vm1network.delete(self.user_api_client)

        return



    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_16_create_template_volume(self):
        """Test Create template from volume
        """

        noffering=NetworkOffering.list(
                     self.user_api_client,
                     name="DefaultIsolatedNetworkOfferingWithSourceNatService"
                     )
        vm2network=Network.create(
                                 self.user_api_client,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=noffering[0].id,
                                zoneid=self.zone.id
                                 )

        list_nw_response = Network.list(
                                            self.user_api_client,
                                            id=vm2network.id
                                            )
        self.assertEqual(
                            isinstance(list_nw_response, list),
                            True,
                            "Check list response returns a valid networks list"
                        )

        templatevm = VirtualMachine.create(
                                    self.user_api_client,
                                    self.services["small"],
                                    templateid=self.template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkids=vm2network.id,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services['mode'],
                                    startvm="true"
                                    )
        time.sleep(600)
        vm_response = VirtualMachine.list(
                            self.user_api_client,
                            id=templatevm.id)

        self.assertNotEqual(
                            len(vm_response),
                            0,
                            "Check VMs available in List VMs response"
                        )
        vm = vm_response[0]
        self.assertEqual(
                            vm.state,
                            'Running',
                            "Check the state of VM created from Template"
                        )

        templatevm.stop(self.user_api_client,forced="false")

        vm_response = VirtualMachine.list(
                            self.user_api_client,
                            id=templatevm.id)

        vm = vm_response[0]
        self.assertEqual(
                            vm.state,
                            'Stopped',
                            "Check the state of VM is in Stopped state before creating the Template"
                        )

        list_volume_response = Volume.list(
            self.user_api_client,
            virtualmachineid=vm.id,
            type="ROOT",
            listall=True
        )

        #Create template from Virtual machine and Volume ID
        roottemplate = Template.create(
                                self.user_api_client,
                                self.services["interop"]["template"],
                                volumeid=list_volume_response[0].id,
                                account=self.account.name,
                                domainid=self.domain.id,
                                )

        time.sleep(600)

        list_template_response = Template.list(
                                    self.user_api_client,
                                    templatefilter=\
                                    self.services["templatefilter"],
                                    id=roottemplate.id
                                    )

        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #Verify template response to check whether template added successfully
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )
        template_response = list_template_response[0]

        self.assertEqual(
                            template_response.displaytext,
                            self.services["interop"]["template"]["displaytext"],
                            "Check display text of newly created template"
                        )
        name = template_response.name
        self.assertEqual(
                            name.count(self.services["interop"]["template"]["name"]),
                            1,
                            "Check name of newly created template"
                        )


        templatevm.delete(self.apiclient)
        vm2network.delete(self.user_api_client)

        vm3network=Network.create(
                                 self.user_api_client,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=noffering[0].id,
                                zoneid=self.zone.id
                                 )

        list_nw_response = Network.list(
                                            self.user_api_client,
                                            id=vm3network.id
                                            )
        self.assertEqual(
                            isinstance(list_nw_response, list),
                            True,
                            "Check list response returns a valid networks list"
                        )


        templatevm = VirtualMachine.create(
                                    self.user_api_client,
                                    self.services["small"],
                                    templateid=roottemplate.id,
                                    networkids=vm3network.id,
                                    serviceofferingid=self.service_offering.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    mode=self.services['mode'],
                                    startvm="true"
                                    )
        time.sleep(600)
        vm_response = VirtualMachine.list(
                            self.user_api_client,
                            id=templatevm.id)

        self.assertNotEqual(
                            len(vm_response),
                            0,
                            "Check VMs available in List VMs response"
                        )
        vm = vm_response[0]
        self.assertEqual(
                            vm.state,
                            'Running',
                            "Check the state of VM created from Template"
                        )

        # Delete the template
        roottemplate.delete(self.user_api_client)

        list_template_response = Template.list(
                                    self.user_api_client,
                                    templatefilter=\
                                    self.services["template"]["templatefilter"],
                                    id=roottemplate.id,
                                    zoneid=self.zone.id
                                    )
        self.assertEqual(
                            list_template_response,
                            None,
                            "Check template available in List Templates"
                        )

        templatevm.delete(self.apiclient)

        vm3network.delete(self.user_api_client)
        return


    @attr(tags=["devcloud", "basic", "advanced", "post"], required_hardware="true")
    def test_17_deployvm_userdata_post(self):
        """Test userdata as POST, size > 2k
        """
        deployVmResponse = VirtualMachine.create(
            self.user_api_client,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            method='POST'
        )
        vms = list_virtual_machines(
            self.user_api_client,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id), "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")
        return

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="true")
    def test_18_deployvm_userdata(self):
        """Test userdata as GET, size > 2k
        """
        deployVmResponse = VirtualMachine.create(
            self.user_api_client,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )
        vms = list_virtual_machines(
            self.user_api_client,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id), "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")

        return




    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_19_template_tag(self):
        """ Test creation, listing and deletion tag on templates
        """

        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("template creation from volume feature is not supported on %s" % self.hypervisor.lower())

        try:
            
            noffering=NetworkOffering.list(
                     self.user_api_client,
                     name="DefaultIsolatedNetworkOfferingWithSourceNatService"
                     )
            vm4network=Network.create(
                                 self.user_api_client,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=noffering[0].id,
                                zoneid=self.zone.id
                                 )

            list_nw_response = Network.list(
                                            self.user_api_client,
                                            id=vm4network.id
                                            )
            self.assertEqual(
                            isinstance(list_nw_response, list),
                            True,
                            "Check list response returns a valid networks list"
                        )

            vm_1 = VirtualMachine.create(
                                    self.user_api_client,
                                    self.services["small"],
                                    templateid=self.template.id,
                                    networkids=vm4network.id,
                                    serviceofferingid=self.service_offering.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    mode=self.services['mode'],
                                    startvm="true"
                                    )
            time.sleep(600)
            self.debug("Stopping the virtual machine: %s" % vm_1.name)
            # Stop virtual machine
            vm_1.stop(self.user_api_client)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        timeout = self.services["timeout"]
        while True:
            list_volume = Volume.list(
                self.user_api_client,
                virtualmachineid=vm_1.id,
                type='ROOT',
                listall=True
            )
            if isinstance(list_volume, list):
                break
            elif timeout == 0:
                raise Exception("List volumes failed.")

            time.sleep(5)
            timeout = timeout - 1

        self.volume = list_volume[0]

        self.debug("Creating template from ROOT disk of virtual machine: %s" %
                   vm_1.name)
        # Create template from volume
        template = Template.create(
            self.user_api_client,
            self.services["template"],
            self.volume.id
        )
        self.cleanup.append(template)
        self.debug("Created the template(%s). Now restarting the userVm: %s" %
                   (template.name, vm_1.name))
        vm_1.start(self.user_api_client)

        self.debug("Creating a tag for the template")
        tag = Tag.create(
            self.user_api_client,
            resourceIds=template.id,
            resourceType='Template',
            tags={'OS': 'windows8'}
        )
        self.debug("Tag created: %s" % tag.__dict__)

        tags = Tag.list(
            self.user_api_client,
            listall=True,
            resourceType='Template',
            key='OS',
            value='windows8'
        )
        self.assertEqual(
            isinstance(tags, list),
            True,
            "List tags should not return empty response"
        )
        self.assertEqual(
            tags[0].value,
            'windows8',
            'The tag should have original value'
        )

        Template.list(
            self.user_api_client,
            templatefilter=self.services["template"]["templatefilter"],
            listall=True,
            key='OS',
            value='windows8'
        )

        self.debug("Deleting the created tag..")
        try:
            tag.delete(
                self.user_api_client,
                resourceIds=template.id,
                resourceType='Template',
                tags={'OS': 'windows8'}
            )
        except Exception as e:
            self.fail("Failed to delete the tag - %s" % e)

        self.debug("Verifying if tag is actually deleted!")
        tags = Tag.list(
            self.user_api_client,
            listall=True,
            resourceType='Template',
            key='OS',
            value='windows8'
        )
        self.assertEqual(
            tags,
            None,
            "List tags should return empty response"
        )
        return
