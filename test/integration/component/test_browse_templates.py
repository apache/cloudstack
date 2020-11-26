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
""" P1 tests for Browser Based Upload Volumes
"""
# Import Local Modules

import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.codes import FAILED

import requests
import random
import string
import time
_multiprocess_shared_ = True

class TestBrowseUploadVolume(cloudstackTestCase):

    """
    Testing Browse Upload Volume Feature
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestBrowseUploadVolume,cls).getClsTestClient()
        #print cls.testClient.getParsedTestDataConfig()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.apiclient = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls._cleanup = []
        cls.cleanup = []
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.unsupportedHypervisor = False
        hosts = list_hosts(
            cls.apiclient,
            type="Routing"
        )

        if cls.hypervisor.lower() in ['lxc']:
            # Template creation from root volume is not supported in LXC
            cls.unsupportedHypervisor = True
            return

        if hosts is None:
            raise unittest.SkipTest(
                "There are no hypervisor's available.Check listhosts response")

        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id)

        if cls.template == FAILED:
                raise unittest.SkipTest(
                    "Check for default cent OS template readiness ")

        cls.service_offering = ServiceOffering.create(
            cls.apiclient, 
            cls.testdata["service_offering"]
        )
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.testdata["resized_disk_offering"],
            custom=True
        )
        cls.project = Project.create(
                                 cls.apiclient,
                                 cls.testdata["project"],
                                 account=cls.account.name,
                                 domainid=cls.account.domainid
                                 )
        cls._cleanup = [
            cls.project,
            cls.account,
            cls.service_offering,
            cls.disk_offering
        ]

    def setUp(self):

        if "kvm" in self.hypervisor.lower():
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template.checksum = "{SHA-1}" + "bf580a13f791d86acf3449a7b457a91a14389264"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-1'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-kvm.qcow2.bz2"
            self.test_template.format = "QCOW2"
            self.test_template.ostypeid = self.getOsType("Other Linux (64-bit)")
            self.md5 = "ada77653dcf1e59495a9e1ac670ad95f"
            self.sha256 = "0efc03633f2b8f5db08acbcc5dc1be9028572dfd8f1c6c8ea663f0ef94b458c5"

        if "vmware" in self.hypervisor.lower():
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template.checksum = "{SHA-1}" + "b25d404de8335b4348ff01e49a95b403c90df466"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-2333'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-vmware.ova"
            self.test_template.format = "OVA"
            self.test_template.ostypeid = self.getOsType(self, "Other Linux (64-bit)")
            self.md5 = "d6d97389b129c7d898710195510bf4fb"
            self.sha256 = "f57b59f118ab59284a70d6c63229d1de8f2d69bffc5a82b773d6c47e769c12d9"

        if "xen" in self.hypervisor.lower():
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template = registerTemplate.registerTemplateCmd()
            self.test_template.checksum = "{SHA-1}" + "427fad501d0d8a1d63b8600a9a469fbf91191314"
            self.test_template.hypervisor = self.hypervisor
            self.test_template.zoneid = self.zone.id
            self.test_template.name = 'test sha-2333'
            self.test_template.displaytext = 'test sha-1'
            self.test_template.url = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-xen.vhd.bz2"
            self.test_template.format = "VHD"
            self.test_template.ostypeid = self.getOsType("Other Linux (64-bit)")
            self.md5 = "54ebc933e6e07ae58c0dc97dfd37c824"
            self.sha256 = "bddd9876021d33df9792b71ae4b776598680ac68ecf55e9d9af33c80904cc1f3"

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                            %s" % self.hypervisor)

    def getOsType(self, param):
        cmd = listOsTypes.listOsTypesCmd()
        cmd.description = param
        return self.apiclient.listOsTypes(cmd)[0].id

    def __verify_values(self, expected_vals, actual_vals):

        return_flag = True

        if len(expected_vals) != len(actual_vals):
            return False

        keys = list(expected_vals.keys())
        for i in range(0, len(expected_vals)):
            exp_val = expected_vals[keys[i]]
            act_val = actual_vals[keys[i]]
            if exp_val == act_val:
                return_flag = return_flag and True
            else:
                return_flag = return_flag and False
                self.debug(
                    "expected Value: %s, is not matching with actual value:\
                    %s" %
                    (exp_val, act_val))
        return return_flag

    def validate_uploaded_template(self, apiclient, template_id, retries=70, interval=5):
        """Check if template download will finish in 1 minute"""
        while retries > -1:
            time.sleep(interval)
            template_response = Template.list(
                apiclient,
                id=template_id,
                zoneid=self.zone.id,
                templatefilter='self'
            )

            if isinstance(template_response, list):
                template = template_response[0]
                if not hasattr(template, 'status') or not template or not template.status:
                    retries = retries - 1
                    continue

                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # if Failed
                # Error - Any other string
                if 'Failed' in template.status:
                    raise Exception(
                        "Failed to download template: status - %s" %
                        template.status)

                elif template.status == 'Download Complete' and template.isready:
                    return

                elif 'Downloaded' in template.status:
                    retries = retries - 1
                    continue

                elif 'Installing' not in template.status:
                    if retries >= 0:
                        retries = retries - 1
                        continue
                    raise Exception(
                        "Error in downloading template: status - %s" %
                        template.status)

            else:
                retries = retries - 1
        raise Exception("Template download failed exception.")

    def gettemplatelimts(self):

        totalresoucelist=Account.list(
                                      self.apiclient,
                                      id=self.account.id
                                      )
        totaltemplates=totalresoucelist[0].templatetotal

        return(totaltemplates)

    def getstoragelimits(self, rtype):

        cmd=updateResourceCount.updateResourceCountCmd()
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.resourcetype=rtype

        response=self.apiclient.updateResourceCount(cmd)

        totalstorage=response[0].resourcecount

        return(totalstorage)

    def browse_upload_template(self):
        cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
        cmd.zoneid = self.test_template.zoneid
        cmd.format = self.test_template.format
        cmd.name=self.test_template.name + self.account.name + (random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.displaytext=self.test_template.name + self.account.name + (random.choice(string.ascii_uppercase))
        cmd.hypervisor=self.test_template.hypervisor
        cmd.ostypeid=self.test_template.ostypeid
        #cmd.isdynamicallyscalable="false"
        #cmd.type="template"
        getuploadparamsresponce=self.apiclient.getUploadParamsForTemplate(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.test_template.url

        uploadfile = url.split('/')[-1]
        r = requests.get(url, stream=True)
        with open(uploadfile, 'wb') as f:
            for chunk in r.iter_content(chunk_size=1024): 
                if chunk: # filter out keep-alive new chunks
                    f.write(chunk)
                    f.flush()

        #uploadfile='rajani-thin-volume.vhd'

        #files={'file':('rajani-thin-volume.vhd',open(uploadfile,'rb'),'application/octet-stream')}

        #headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        files={'file':(uploadfile,open(uploadfile,'rb'),'application/octet-stream')}

        headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        results = requests.post(posturl,files=files,headers=headers,verify=False)

        print(results.status_code)
        if results.status_code !=200: 
            self.fail("Upload is not fine")

        self.validate_uploaded_template(self.apiclient, getuploadparamsresponce.id)

        return(getuploadparamsresponce)

    def browse_upload_template_with_out_zoneid(self):

        cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
        cmd.format = self.test_template.format
        cmd.name=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.displaytext=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.hypervisor=self.test_template.hypervisor
        cmd.ostypeid=self.test_template.ostypeid

        success= False
        try:
            getuploadparamsresponce=self.apiclient.getUploadParamsForTemplate(cmd)
        except Exception as ex:
            if "Invalid Parameter" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Upload Template - verify upload Template API request is handled without mandatory params - zoneid ")

        return

    def browse_upload_template_with_out_ostypeid(self):


        cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.test_template.format
        cmd.name=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.displaytext=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.hypervisor=self.test_template.hypervisor

        success= False
        try:
            getuploadparamsresponce=self.apiclient.getUploadParamsForTemplate(cmd)
        except Exception as ex:
            if "Invalid Parameter" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Upload Template - verify upload template API request is handled without mandatory params - ostypeid")

        return

    def browse_upload_template_with_projectid(self,projectid):
        cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.test_template.format
        cmd.name=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.displaytext=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.hypervisor=self.test_template.hypervisor
        cmd.ostypeid=self.test_template.ostypeid
        cmd.projectid=projectid
        #cmd.isdynamicallyscalable="false"
        #cmd.type="template"
        getuploadparamsresponce=self.apiclient.getUploadParamsForTemplate(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.test_template.url

        uploadfile = url.split('/')[-1]
        r = requests.get(url, stream=True)
        with open(uploadfile, 'wb') as f:
            for chunk in r.iter_content(chunk_size=1024): 
                if chunk: # filter out keep-alive new chunks
                    f.write(chunk)
                    f.flush()

        files={'file':(uploadfile,open(uploadfile,'rb'),'application/octet-stream')}

        headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        results = requests.post(posturl,files=files,headers=headers,verify=False)

        print(results.status_code)
        if results.status_code !=200: 
            self.fail("Upload is not fine")

        self.validate_uploaded_template(self.apiclient, getuploadparamsresponce.id)

        return(getuploadparamsresponce)

    def browse_upload_template_multiplezones(self,lzones):

        cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
        cmd.zoneid ="-1"
        cmd.format = self.test_template.format
        cmd.name=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.displaytext=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.hypervisor=self.test_template.hypervisor
        cmd.ostypeid=self.test_template.ostypeid
        #cmd.isdynamicallyscalable="false"
        #cmd.type="template"
        getuploadparamsresponce=self.apiclient.getUploadParamsForTemplate(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.test_template.url

        uploadfile = url.split('/')[-1]
        r = requests.get(url, stream=True)
        with open(uploadfile, 'wb') as f:
            for chunk in r.iter_content(chunk_size=1024): 
                if chunk: # filter out keep-alive new chunks
                    f.write(chunk)
                    f.flush()

        #uploadfile='rajani-thin-volume.vhd'

        #files={'file':('rajani-thin-volume.vhd',open(uploadfile,'rb'),'application/octet-stream')}

        #headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        files={'file':(uploadfile,open(uploadfile,'rb'),'application/octet-stream')}

        headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        results = requests.post(posturl,files=files,headers=headers,verify=False)

        print(results.status_code)
        if results.status_code !=200: 
            self.fail("Upload is not fine")

        for z1 in lzones:
            self.validate_uploaded_template(self.apiclient, getuploadparamsresponce.id)

        return(getuploadparamsresponce)

    def uploadtemplate(self):
        cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.test_template.format
        cmd.name=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.displaytext=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.hypervisor=self.test_template.hypervisor
        cmd.ostypeid=self.test_template.ostypeid
        #cmd.type="template"
        getuploadparamsresponce=self.apiclient.getUploadParamsForTemplate(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.test_template.url

        uploadfile = url.split('/')[-1]
        r = requests.get(url, stream=True)
        with open(uploadfile, 'wb') as f:
            for chunk in r.iter_content(chunk_size=1024): 
                if chunk: # filter out keep-alive new chunks
                    f.write(chunk)
                    f.flush()

        #uploadfile='rajani-thin-volume.vhd'

        #files={'file':('rajani-thin-volume.vhd',open(uploadfile,'rb'),'application/octet-stream')}

        #headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        files={'file':(uploadfile,open(uploadfile,'rb'),'application/octet-stream')}

        headers={'X-signature':signt,'X-metadata':metadata,'X-expires':expiredata}

        results = requests.post(posturl,files=files,headers=headers,verify=False)
        time.sleep(60)

        print(results.status_code)
        if results.status_code !=200: 
            self.fail("Upload is not fine")

        return(getuploadparamsresponce)

    def multiple_browse_upload_template(self):

        templ1=self.uploadtemplate()
        templ2=self.uploadtemplate()
        templ3=self.uploadtemplate()

        self.validate_uploaded_template(self.apiclient, templ1.id)
        self.validate_uploaded_template(self.apiclient, templ2.id)
        self.validate_uploaded_template(self.apiclient, templ3.id)

        self.delete_template(templ1)
        self.delete_template(templ2)
        self.delete_template(templ3)
        return

    def validate_vm(self, apiclient, vm_id, state, retries=72, interval=5):

        """Check if vm  will be running in 6 minute"""
        while retries > -1:
            time.sleep(interval)
            vm_response = list_virtual_machines(
                apiclient,
                id=vm_id
            )

            if isinstance(vm_response, list):
                vm = vm_response[0]
                if not hasattr(vm, 'state'):
                    retries = retries - 1
                    continue

                # If vm is Running for x number of retries
                if vm.state == state:
                    return

                else:
                    retries = retries - 1
                    continue

            else:
                retries = retries - 1
        raise Exception("VM failed exception.")

    def deploy_vm(self,template):
            virtual_machine = VirtualMachine.create(
                                                    self.apiclient,
                                                    self.testdata["virtual_machine"],
                                                    templateid=template.id,
                                                    zoneid=self.zone.id,
                                                    accountid=self.account.name,
                                                    domainid=self.account.domainid,
                                                    serviceofferingid=self.service_offering.id,
                                                )
            self.validate_vm(self.apiclient, vm_id = virtual_machine.id, state="Running")
            return(virtual_machine)

    def attach_volume(self,vmlist,volid):

        list_volume_response = Volume.list(
                    self.apiclient,
                    id=volid
                )
        print(list_volume_response[0])
        vmlist.attach_volume(
                    self.apiclient,
                    list_volume_response[0]
                )
        list_volume_response = Volume.list(
                self.apiclient,
                virtualmachineid=vmlist.id,
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
        self.validate_uploaded_volume(volid,'Ready')

    def validate_uploaded_volume(self, volid, status):
        list_volume_response = Volume.list(
                    self.apiclient,
                    id=volid,
                    listall=True
                    )

        self.assertNotEqual(
                    list_volume_response,
                    None,
                    "Check if volume exists in ListTemplates"
                )

        self.assertEqual(
                    list_volume_response[0].status,
                    status,
                    "Check volume status in List Volumes"
                )
        return

    def reboot_vm(self,vmdetails):
        vmdetails.reboot(self.apiclient)
        self.validate_vm(self.apiclient, vm_id=vmdetails.id, state="Running")

    def stop_vm(self,vmdetails):
        vmdetails.stop(self.apiclient)
        self.validate_vm(self.apiclient, vm_id=vmdetails.id, state="Stopped")

    def start_vm(self,vmdetails):
        vmdetails.start(self.apiclient)
        self.validate_vm(self.apiclient, vm_id=vmdetails.id, state="Running")

    def vmoperations(self,vmdetails):
        self.reboot_vm(vmdetails)

        self.stop_vm(vmdetails)

        self.start_vm(vmdetails)

    def detach_volume(self,vmdetails,volid):
        """Detach a Volume attached to a VM
        """
        list_volume_response = Volume.list(
                    self.apiclient,
                    id=volid
                )
        print(list_volume_response[0])
        vmdetails.detach_volume(self.apiclient,list_volume_response[0])

        # Sleep to ensure the current state will reflected in other calls
        time.sleep(self.testdata["sleep"])

        list_volume_response = Volume.list(
            self.apiclient,
            id=volid
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
        volume = list_volume_response[0]
        self.assertEqual(
            volume.virtualmachineid,
            None,
            "Check if volume state (detached) is reflected"
        )

        self.assertEqual(
            volume.vmname,
            None,
            "Check if volume state (detached) is reflected"
        )
        return

    def restore_vm(self,vmdetails):
        """Test recover Virtual Machine
        """
        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = vmdetails.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=vmdetails.id
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
                            "Check virtual machine is in Running state"
                        )

        return

    def restore_vm_fail(self,vmdetails):

        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = vmdetails.id
        success= False
        try:
            self.apiclient.recoverVirtualMachine(cmd)
        except Exception as ex:
            if "isn't available in the zone" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Restore VM with Deleted Template - Verify Restore VM is handled when template with which VM created id deleted")

        return

    def deletevolume(self,volumeid):
        """Delete a Volume attached to a VM
        """

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = volumeid

        self.apiclient.deleteVolume(cmd)

        list_volume_response = Volume.list(
                                            self.apiclient,
                                            id=volumeid,
                                            type='DATADISK'
                                            )
        self.assertEqual(
                        list_volume_response,
                        None,
                        "Check if volume exists in ListVolumes"
                    )
        return

    def destroy_vm(self,vmdetails):

        vmdetails.delete(self.apiclient, expunge=False)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=vmdetails.id
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

    def recover_destroyed_vm(self,vmdetails):

        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = vmdetails.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=vmdetails.id
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

        return

    def expunge_vm(self,vmdetails):

        self.debug("Expunge VM-ID: %s" % vmdetails.id)

        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = vmdetails.id
        self.apiclient.destroyVirtualMachine(cmd)

        config = Configurations.list(
                                     self.apiclient,
                                     name='expunge.delay'
                                     )

        expunge_delay = int(config[0].value)
        time.sleep(expunge_delay * 2)

        #VM should be destroyed unless expunge thread hasn't run
        #Wait for two cycles of the expunge thread
        config = Configurations.list(
                                     self.apiclient,
                                     name='expunge.interval'
                                     )
        expunge_cycle = int(config[0].value)
        wait_time = expunge_cycle * 4
        while wait_time >= 0:
            list_vm_response = list_virtual_machines(
                                                self.apiclient,
                                                id=vmdetails.id
                                                )
            if not list_vm_response:
                break
            self.debug("Waiting for VM to expunge")
            time.sleep(expunge_cycle)
            wait_time = wait_time - expunge_cycle

            self.debug("listVirtualMachines response: %s" % list_vm_response)

            self.assertEqual(list_vm_response,None,"Check Expunged virtual machine is in listVirtualMachines response")
            return

        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vmdetails.id
        )
        if isinstance(list_vm_response, list):
            self.fail("VM has not been expunged")

    def waitForSystemVMAgent(self, vmname):
        timeout = self.testdata["timeout"]

        while True:
            list_host_response = list_hosts(
                                                 self.apiclient,
                                                 name=vmname
                                                )

            if list_host_response and list_host_response[0].state == 'Up':
                break

            if timeout == 0:
                raise Exception("Timed out waiting for SSVM agent to be Up")

            time.sleep(self.testdata["sleep"])
            timeout = timeout - 1

    def ssvm_internals(self):

        list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        systemvmtype='secondarystoragevm',
                                        state='Running',
                                        zoneid=self.zone.id
                                        )
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        ssvm = list_ssvm_response[0]

        hosts = list_hosts(
                           self.apiclient,
                           id=ssvm.hostid
                           )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list response returns a valid list"
                        )
        host = hosts[0]

        self.debug("Running SSVM check script")

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            #SSH into SSVMs is done via management server for Vmware and Hyper-V
            result = get_process_status(
                                self.apiclient.connection.mgtSvr,
                                22,
                                self.apiclient.connection.user,
                                self.apiclient.connection.passwd,
                                ssvm.privateip,
                                "/usr/local/cloud/systemvm/ssvm-check.sh |grep -e ERROR -e WARNING -e FAIL",
                                hypervisor=self.hypervisor
                                )
        else:
            try:
                host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                result = get_process_status(
                                    host.ipaddress,
                                    22,
                                    host.user,
                                    host.passwd,
                                    ssvm.linklocalip,
                                    "/usr/local/cloud/systemvm/ssvm-check.sh |grep -e ERROR -e WARNING -e FAIL"
                                )
            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check router services")
        res = str(result)
        self.debug("SSVM script output: %s" % res)

        self.assertEqual(
                            res.count("ERROR"),
                            1,
                            "Check for Errors in tests"
                        )

        self.assertEqual(
                            res.count("WARNING"),
                            1,
                            "Check for warnings in tests"
                        )

        #Check status of cloud service
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            #SSH into SSVMs is done via management server for Vmware and Hyper-V
            result = get_process_status(
                                self.apiclient.connection.mgtSvr,
                                22,
                                self.apiclient.connection.user,
                                self.apiclient.connection.passwd,
                                ssvm.privateip,
                                "systemctl is-active cloud",
                                hypervisor=self.hypervisor
                                )
        else:
            try:
                host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                result = get_process_status(
                                    host.ipaddress,
                                    22,
                                    host.user,
                                    host.passwd,
                                    ssvm.linklocalip,
                                    "systemctl is-active cloud"
                                    )
            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check router services")
        res = str(result)
        self.debug("Cloud Process status: %s" % res)
        # Apache CloudStack service (type=secstorage) is running: process id: 2346
        self.assertEqual(
                            res.count("active"),
                            1,
                            "Check cloud service is running or not"
                        )
        return

    def list_sec_storage_vm(self):

        list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        systemvmtype='secondarystoragevm',
                                        state='Running',
                                        )
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #Verify SSVM response
        self.assertNotEqual(
                            len(list_ssvm_response),
                            0,
                            "Check list System VMs response"
                        )

        list_zones_response = list_zones(self.apiclient)
        
        self.assertEqual(
                            isinstance(list_zones_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.debug("Number of zones: %s" % len(list_zones_response))
        self.debug("Number of SSVMs: %s" % len(list_ssvm_response))
        # Number of Sec storage VMs = No of Zones
        self.assertEqual(
                            len(list_ssvm_response),
                            len(list_zones_response),
                            "Check number of SSVMs with number of zones"
                        )
        #For each secondary storage VM check private IP,
        #public IP, link local IP and DNS
        for ssvm in list_ssvm_response:

            self.debug("SSVM state: %s" % ssvm.state)
            self.assertEqual(
                            ssvm.state,
                            'Running',
                            "Check whether state of SSVM is running"
                        )

            self.assertEqual(
                            hasattr(ssvm, 'privateip'),
                            True,
                            "Check whether SSVM has private IP field"
                            )

            self.assertEqual(
                            hasattr(ssvm, 'linklocalip'),
                            True,
                            "Check whether SSVM has link local IP field"
                            )

            self.assertEqual(
                            hasattr(ssvm, 'publicip'),
                            True,
                            "Check whether SSVM has public IP field"
                            )

            #Fetch corresponding ip ranges information from listVlanIpRanges
            ipranges_response = list_vlan_ipranges(
                                                   self.apiclient,
                                                   zoneid=ssvm.zoneid
                                                   )
            self.assertEqual(
                            isinstance(ipranges_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            iprange = ipranges_response[0]
            
            #Fetch corresponding Physical Network of SSVM's Zone
            listphyntwk = PhysicalNetwork.list(
                            self.apiclient,
                            zoneid=ssvm.zoneid
                            )
            
            # Execute the following assertion in all zones except EIP-ELB Zones
            if not (self.zone.networktype.lower() == 'basic' and isinstance(NetScaler.list(self.apiclient,physicalnetworkid=listphyntwk[0].id), list) is True):
                self.assertEqual(
                            ssvm.gateway,
                            iprange.gateway,
                            "Check gateway with that of corresponding ip range"
                            )

            #Fetch corresponding zone information from listZones
            zone_response = list_zones(
                                       self.apiclient,
                                       id=ssvm.zoneid
                                       )
            self.assertEqual(
                            isinstance(zone_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertEqual(
                            ssvm.dns1,
                            zone_response[0].dns1,
                            "Check DNS1 with that of corresponding zone"
                            )

            self.assertEqual(
                            ssvm.dns2,
                            zone_response[0].dns2,
                            "Check DNS2 with that of corresponding zone"
                            )
        return

    def stop_ssvm(self):

        list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        systemvmtype='secondarystoragevm',
                                        state='Running',
                                        zoneid=self.zone.id
                                        )
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        ssvm = list_ssvm_response[0]

        hosts = list_hosts(
                           self.apiclient,
                           id=ssvm.hostid
                           )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list response returns a valid list"
                        )
        host = hosts[0]

        self.debug("Stopping SSVM: %s" % ssvm.id)
        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = ssvm.id
        self.apiclient.stopSystemVm(cmd)
        
        timeout = self.testdata["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        id=ssvm.id
                                        )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")
            
            time.sleep(self.testdata["sleep"])
            timeout = timeout - 1
        
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        ssvm_response = list_ssvm_response[0]
        self.debug("SSVM state after debug: %s" % ssvm_response.state)
        self.assertEqual(
                        ssvm_response.state,
                        'Running',
                        "Check whether SSVM is running or not"
                        )
        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        # Call above tests to ensure SSVM is properly running
        self.list_sec_storage_vm()

    def reboot_ssvm(self):

        list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        systemvmtype='secondarystoragevm',
                                        state='Running',
                                        zoneid=self.zone.id
                                        )
    
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
        ssvm_response = list_ssvm_response[0]

        hosts = list_hosts(
                           self.apiclient,
                           id=ssvm_response.hostid
                           )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list response returns a valid list"
                        )
        host = hosts[0]

        #Store the public & private IP values before reboot
        old_public_ip = ssvm_response.publicip
        old_private_ip = ssvm_response.privateip

        self.debug("Rebooting SSVM: %s" % ssvm_response.id)
        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = ssvm_response.id
        self.apiclient.rebootSystemVm(cmd)

        timeout = self.testdata["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        id=ssvm_response.id
                                        )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")
            
            time.sleep(self.testdata["sleep"])
            timeout = timeout - 1

        ssvm_response = list_ssvm_response[0]
        self.debug("SSVM State: %s" % ssvm_response.state)
        self.assertEqual(
                        'Running',
                        str(ssvm_response.state),
                        "Check whether CPVM is running or not"
                        )

        self.assertEqual(
                    ssvm_response.publicip,
                    old_public_ip,
                    "Check Public IP after reboot with that of before reboot"
                    )

        self.assertEqual(
                    ssvm_response.privateip,
                    old_private_ip,
                    "Check Private IP after reboot with that of before reboot"
                    )

        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        return

    def destroy_ssvm(self):

        list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        systemvmtype='secondarystoragevm',
                                        state='Running',
                                        zoneid=self.zone.id
                                        )
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        ssvm_response = list_ssvm_response[0]

        old_name = ssvm_response.name

        self.debug("Destroying SSVM: %s" % ssvm_response.id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = ssvm_response.id
        self.apiclient.destroySystemVm(cmd)

        timeout = self.testdata["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        zoneid=self.zone.id,
                                        systemvmtype='secondarystoragevm'
                                        )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")
            
            time.sleep(self.testdata["sleep"])
            timeout = timeout - 1

        ssvm_response = list_ssvm_response[0]

        # Verify Name, Public IP, Private IP and Link local IP
        # for newly created SSVM
        self.assertNotEqual(
                        ssvm_response.name,
                        old_name,
                        "Check SSVM new name with name of destroyed SSVM"
                        )
        self.assertEqual(
                        hasattr(ssvm_response, 'privateip'),
                        True,
                        "Check whether SSVM has private IP field"
                        )

        self.assertEqual(
                        hasattr(ssvm_response, 'linklocalip'),
                        True,
                        "Check whether SSVM has link local IP field"
                        )

        self.assertEqual(
                        hasattr(ssvm_response, 'publicip'),
                        True,
                        "Check whether SSVM has public IP field"
                        )
        
        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        return

    def create_data_volume(self):

        diskoffering = DiskOffering.list(self.apiclient)
        self.assertTrue(
            isinstance(
                diskoffering,
                list),
            msg="DiskOffering list is not a list?")
        self.assertTrue(
            len(diskoffering) > 0,
            "no disk offerings in the deployment")

        vol = Volume.create(
            self.apiclient,
            services=self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            diskofferingid=diskoffering[0].id
        )
        self.assertTrue(
            vol is not None, "volume creation fails in domain %s as user %s" %
            (self.domain.name, self.account.name))

        listed_vol = Volume.list(self.apiclient, id=vol.id)
        self.assertTrue(
            listed_vol is not None and isinstance(
                listed_vol,
                list),
            "invalid response from listVolumes for volume %s" %
            vol.id)
        self.assertTrue(
            listed_vol[0].id == vol.id,
            "Volume returned by list volumes %s not matching with queried\
                    volume %s in domain %s" %
            (listed_vol[0].id,
                vol.id,
                self.account.name))
        return(listed_vol[0])

    def attach_data_volume(self,volume,vmdetails):

        list_volume_response = Volume.list(
            self.apiclient,
            id=volume.id
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
        volume = list_volume_response[0]

        self.assertEqual(
            volume.type,
            'DATADISK',
            "Check volume type from list volume response"
        )

        self.assertEqual(
            hasattr(volume, 'vmname'),
            True,
            "Check whether volume has vmname field"
        )
        self.assertEqual(
            hasattr(volume, 'virtualmachineid'),
            True,
            "Check whether volume has virtualmachineid field"
        )

        # Attach volume to VM
        self.debug("Attach volume: %s to VM: %s" % (
            volume.id,
            vmdetails.id
        ))
        vmdetails.attach_volume(self.apiclient, volume)

        # Check all volumes attached to same VM
        list_volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=vmdetails.id,
            type='DATADISK',
            listall=True
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
        volume = list_volume_response[0]
        self.assertEqual(
            volume.vmname,
            vmdetails.name,
            "Check virtual machine name in list volumes response"
        )
        self.assertEqual(
            volume.virtualmachineid,
            vmdetails.id,
            "Check VM ID in list Volume response"
        )
        return

    def delete_template(self,templatedetails):

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter="all",
                                    id=templatedetails.id,
                                    zoneid=self.zone.id)

        if list_template_response is None:
            self.debug("Template is not available")
            return

        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = templatedetails.id
        self.apiclient.deleteTemplate(cmd)

        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter="all",
                                    id=templatedetails.id,
                                    zoneid=self.zone.id
                                    )
        self.assertEqual(
                            list_template_response,
                            None,
                            "Check template available in List Templates"
                        )
        return

    def detach_data_volume(self,volume,vmdetails):

        self.debug("Detach volume: %s to VM: %s" % (
            volume.id,
            vmdetails.id
        ))
        vmdetails.detach_volume(self.apiclient, volume)

        # Sleep to ensure the current state will reflected in other calls
        time.sleep(self.testdata["sleep"])

        list_volume_response = Volume.list(
            self.apiclient,
            id=volume.id
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
        volumelist = list_volume_response[0]
        self.assertEqual(
            volumelist.virtualmachineid,
            None,
            "Check if volume state (detached) is reflected"
        )

        self.assertEqual(
            volumelist.vmname,
            None,
            "Check if volume state (detached) is reflected"
        )
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_01_Browser_template_Life_cycle_tpath(self):
        """
        Test Browser_template_Life_cycle
        """
        #try:

        self.debug("========================= Test 1: Upload Browser based template and validate ========================= ")
        browseup_template=self.browse_upload_template()

        self.debug("========================= Test 2: Deploy a VM with uploaded template and validate VM Operations========================= ")

        vm1details=self.deploy_vm(browseup_template)

        self.vmoperations(vm1details)

        self.debug("========================= Test 3: Attach DATA DISK to the VM ")

        cvolume=self.create_data_volume()
        self.attach_data_volume(cvolume, vm1details)
        self.vmoperations(vm1details)

        self.debug("========================= Test 4: Restore VM created with Uploaded template========================= ")

        self.restore_vm(vm1details)

        self.debug("========================= Test 5: Detach DATA DISK to the VM ")

        self.detach_data_volume(cvolume,vm1details)
        self.vmoperations(vm1details)

        self.deletevolume(cvolume.id)


        self.debug("========================= Test 6: Expunge VM created with Uploaded template========================= ")

        self.expunge_vm(vm1details)

        self.debug("========================= Test 7:  Destroy VM ========================= ")

        vm2details=self.deploy_vm(self.template)

        vm2details=self.deploy_vm(browseup_template)
        self.destroy_vm(vm2details)

        self.debug("========================= Test 8:  Recover destroyed VM which has Uploaded volumes attached========================= ")

        self.recover_destroyed_vm(vm2details)
        self.expunge_vm(vm2details)

        self.debug("========================= Test 9:  Delete the Uploaded Template========================= ")
        self.debug(browseup_template)
        self.delete_template(browseup_template)

        self.debug("========================= Test 10:  Upload Multiple templates========================= ")

        self.multiple_browse_upload_template()

#        except Exception as e:
#            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_02_SSVM_Life_Cycle_With_Browser_Template_TPath(self):
        """
        Test SSVM_Life_Cycle_With_Browser_template_TPath 
        """
        try:
            
            self.debug("========================= Test 11: Stop and Start SSVM and Perform Browser based volume validations ========================= ")

            self.stop_ssvm()
            ssvm1browseup_template=self.browse_upload_template()

            ssvm1vm1details=self.deploy_vm(ssvm1browseup_template)
            #ssvm1vm1details=self.deploy_vm(self.template)

            self.vmoperations(ssvm1vm1details)

            self.expunge_vm(ssvm1vm1details)

            self.debug("========================= Test 12: Reboot SSVM and Perform Browser based volume validations ========================= ")

            self.reboot_ssvm()
            ssvm2browseup_template=self.browse_upload_template()

            ssvm2vm1details=self.deploy_vm(ssvm2browseup_template)

            #ssvm2vm1details=self.deploy_vm(self.template)
            self.vmoperations(ssvm2vm1details)

            self.expunge_vm(ssvm2vm1details)

            self.debug("========================= Test 13: Destroy SSVM and Perform Browser based volume validations ========================= ")

            self.destroy_ssvm()
            ssvm3browseup_template=self.browse_upload_template()

            ssvm3vm1details=self.deploy_vm(ssvm3browseup_template)

            #ssvm2vm1details=self.deploy_vm(self.template)
            self.vmoperations(ssvm3vm1details)

            self.expunge_vm(ssvm3vm1details)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_03_Browser_template_upload_multiple_zones(self):
        """
        Test Browser_template_upload_multiple_zones
        """

        cmd = listZones.listZonesCmd()
        zonelist=self.apiclient.listZones(cmd)
        if len(zonelist) <2:
            raise self.skipTest("Only one zone available hence skipping")
        try:

            self.debug("========================= Test 14: Upload Browser based template into multiple zones ========================= ")
            browseup_template=self.browse_upload_template_multiplezones(zonelist)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_04_Browser_template_ResetVM_With_Deleted_Template(self):
        """
        Test Browser_template_upload_ResetVM_With_Deleted_Template
        """
        try:

            self.debug("========================= Test 15: Test Browser_template_upload_ResetVM_With_Deleted_Template ========================= ")

            browseup_template=self.browse_upload_template()

            vm1details=self.deploy_vm(browseup_template)

            self.delete_template(browseup_template)

            self.restore_vm_fail(vm1details)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_05_Browser_Upload_Template_with_all_API_parameters(self):
        """
        Test Browser_Upload_Template with all API parameters
        """
        try:

            self.debug("========================= Test 16 & 17 Upload template with account name and domainid========================")

            browseup_template1=self.browse_upload_template()

            self.debug("========================= Test 18 Upload template with project id========================")
            browseup_template2=self.browse_upload_template_with_projectid(self.project.id)

            self.debug("========================= Test 19 Upload template with out mandatory param zone id ========================")

            browseup_vol2=self.browse_upload_template_with_out_zoneid()

            self.debug("========================= Test 20 Upload template with out mandatory param ostypeid ========================")

            browseup_vol3=self.browse_upload_template_with_out_ostypeid()

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return



    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_06_Browser_Upload_template_resource_limits(self):
        """
        Test Browser Upload Template Resource limits
        """
        try:

            self.debug("========================= Test 21 Upload Template and verify Template limits========================")
            initialtemplatelimit=self.gettemplatelimts()
            browseup_template1=self.browse_upload_template()
            # Adding time for limit to sync in background
            time.sleep(120)
            afteruploadtemplatelimit=self.gettemplatelimts()

            if int(afteruploadtemplatelimit)!=(int(initialtemplatelimit)+1):
                self.fail("Volume Resouce Count is not updated")

            self.delete_template(browseup_template1)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_07_Browser_Upload_template_secondary_storage_resource_limits(self):
        """
        Test Browser_Upload_Template Secondary Storage Resource limits
        """
        try:

            self.debug("========================= Test 22 Upload template and verify secondary storage limits========================")

            initialsecondarystoragelimit=self.getstoragelimits(11)
            browseup_template1=self.browse_upload_template()

            tmpldetails=Template.list(
                                      self.apiclient,
                                      id=browseup_template1.id,
                                     templatefilter="all",
                                     zoneid=self.zone.id)


            afteruploadsecondarystoragelimit=self.getstoragelimits(11)

            if afteruploadsecondarystoragelimit!=(initialsecondarystoragelimit+tmpldetails[0].size):
                self.fail("Secondary Storage Resouce Count is not updated")

            self.delete_template(browseup_template1)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_08_Browser_Upload_template_resource_limits_after_deletion(self):
        """
        Test Browser_Upload_Template Resource limits after template deletion
        """
        try:
            self.debug("========================= Test 23 Delete Upload template and verify template limits========================")
            browseup_template1=self.browse_upload_template()
            initialtemplatelimit=self.gettemplatelimts()

            self.delete_template(browseup_template1)
            aftertemplatelimit=self.gettemplatelimts()

            if aftertemplatelimit!=(initialtemplatelimit-1):
                self.fail("Template Resource Count is not updated after deletion")

        except Exception as e:
            self.fail("Exceptione occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_09_Browser_Upload_Volume_secondary_storage_resource_limits_after_deletion(self):
        """
        Test Browser_Upload_Template Secondary Storage Resource limits after template deletion
        """
        try:
            self.debug("========================= Test 24 Delete Upload template and verify secondary storage limits========================")

            browseup_template1=self.browse_upload_template()

            tmpldetails=Template.list(
                                      self.apiclient,
                                      id=browseup_template1.id,
                                     templatefilter="all",
                                     zoneid=self.zone.id)

            initialuploadprimarystoragelimit=self.getstoragelimits(11)
            self.delete_template(browseup_template1)

            afteruploadprimarystoragelimit=self.getstoragelimits(11)

            if afteruploadprimarystoragelimit!=(initialuploadprimarystoragelimit-tmpldetails[0].size):
                self.fail("Secondary Storage Resource Count is not updated after deletion")

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_browser_upload_template_incomplete(self):
        """
        Test browser based incomplete template upload, followed by SSVM destroy. Template should go to UploadAbandoned state and get cleaned up.
        """
        try:
            self.debug("========================= Test browser based incomplete template upload ========================")

            #Only register template, without uploading
            cmd = getUploadParamsForTemplate.getUploadParamsForTemplateCmd()

            if 'kvm' in self.hypervisor.lower():
                cmd.url = 'http://dl.openvm.eu/cloudstack/centos/x86_64/centos-7-kvm.qcow2.bz2'
            if 'vmware' in self.hypervisor.lower():
                cmd.url = 'http://dl.openvm.eu/cloudstack/centos/x86_64/centos-7-vmware.ova'
            if 'xenserver' in self.hypervisor.lower():
                cmd.url = 'http://dl.openvm.eu/cloudstack/centos/x86_64/centos-7-xen.vhd.bz2'

            cmd.zoneid = self.zone.id
            cmd.format = self.test_template.format
            cmd.name=self.test_template.name+self.account.name+(random.choice(string.ascii_uppercase))
            cmd.account=self.account.name
            cmd.domainid=self.domain.id
            cmd.displaytext=cmd.name
            cmd.hypervisor=self.test_template.hypervisor
            cmd.ostypeid=self.test_template.ostypeid
            template_response=self.apiclient.getUploadParamsForTemplate(cmd)

            #Destroy SSVM, and wait for new one to start
            self.destroy_ssvm()

            #Verify that the template is cleaned up as part of sync-up during new SSVM start
            list_template_response=Template.list(
                                        self.apiclient,
                                        id=template_response.id,
                                        templatefilter="all",
                                        zoneid=self.zone.id)
            self.assertEqual(list_template_response, None, "Template is not cleaned up, some issue with template sync-up")

        except Exception as e:
            self.fail("Exceptione occurred  : %s" % e)
        return

    @classmethod
    def tearDownClass(self):
        try:
            self.apiclient = super(TestBrowseUploadVolume,self).getClsTestClient().getApiClient()
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
