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
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.codes import PASS,FAILED,SUCCESS,XEN_SERVER

from marvin.sshClient import SshClient

import requests

import wget

import random

import string

import telnetlib
import os
import urllib.request, urllib.parse, urllib.error
import time
import tempfile
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
        cls.uploadvolumeformat="VHD"
        cls.storagetype = 'shared'
        cls.globalurl="http://url"

        hosts = list_hosts(
            cls.apiclient,
            type="Routing"
        )

        if hosts is None:
            raise unittest.SkipTest(
                "There are no hypervisor's available.Check listhosts response")
        for hypervisorhost in hosts :
                 if hypervisorhost.hypervisor == "XenServer":
                     cls.uploadvolumeformat="VHD"
                     break
                 elif hypervisorhost.hypervisor== "VMware":
                     cls.uploadvolumeformat="OVA"
                     break
                 elif hypervisorhost.hypervisor=="KVM":
                     cls.uploadvolumeformat="QCOW2"
                     break
                 else:
                     break

        cls.uploadurl=cls.testdata["configurableData"]["browser_upload_volume"][cls.uploadvolumeformat]["url"]
        cls.volname=cls.testdata["configurableData"]["browser_upload_volume"][cls.uploadvolumeformat]["diskname"]
        cls.md5sum=cls.testdata["configurableData"]["browser_upload_volume"][cls.uploadvolumeformat]["checksum"]
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.pod = get_pod(cls.apiclient, cls.zone.id)

        if cls.uploadvolumeformat=="QCOW2" or cls.uploadvolumeformat=="VHD": 
                cls.extuploadurl=cls.testdata["configurableData"]["browser_upload_volume_extended"][cls.uploadvolumeformat]["url"]
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
            cls.testdata["configurableData"]["browser_upload_volume"]["browser_resized_disk_offering"],
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

    def validate_uploaded_volume(self,up_volid,volumestate):

        config1 = Configurations.list(
                                     self.apiclient,
                                     name='upload.operation.timeout'
                                     )

        config2 = Configurations.list(
                                     self.apiclient,
                                     name='upload.monitoring.interval'
                                     )

        uploadtimeout = int(config1[0].value)
        monitoringinterval=int(config2[0].value)

        time.sleep((uploadtimeout*60)+monitoringinterval)

        list_volume_response = Volume.list(
                    self.apiclient,
                    id=up_volid
                )
        if list_volume_response is None:
            self.debug("Volume got deleted after timeout")
            return

        self.assertEqual(
                    list_volume_response[0].state,
                    volumestate,
                    "Check volume state in ListVolumes"
                )
        return


    def browse_upload_volume_with_projectid(self,projectid):

        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.projectid=projectid
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires

        url=self.uploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')

        list_volume_response = Volume.list(
                    self.apiclient,
                    projectid=projectid
                )
        if list_volume_response[0].id==getuploadparamsresponce.id:
            return(getuploadparamsresponce)
        else:
            self.fail("Volume is not listed with projectid")


    def browse_upload_volume_with_out_zoneid(self):


        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        success= False
        try:
            getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)
        except Exception as ex:
            if "Invalid Parameter" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Upload Volume - verify upload volume API request is handled without mandatory params - zoneid ")

        return


    def browse_upload_volume_with_out_format(self):


        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        success= False
        try:
            getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)
        except Exception as ex:
            if "Invalid Parameter" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Upload Volume - verify upload volume API request is handled without mandatory params - format")

        return

    def browse_upload_volume(self):
        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        self.globalurl=getuploadparamsresponce.postURL
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.uploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')

        return(getuploadparamsresponce)

    def onlyupload(self):
        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)
        return(getuploadparamsresponce)



    def invalidupload(self):

        success= False
        try:
            cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
            cmd.zoneid = self.zone.id
            cmd.format = "invalidformat"
            cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
            cmd.account=self.account.name
            cmd.domainid=self.domain.id
            getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        except Exception as ex:
            if "No enum constant com.cloud.storage.Storage.ImageFormat" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Verify - Upload volume with invalid format is handled")

        return


    def invalidposturl(self):

        success= False
        try:
            cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
            cmd.zoneid = self.zone.id
            cmd.format = self.uploadvolumeformat
            cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
            cmd.account=self.account.name
            cmd.domainid=self.domain.id
            getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)


            signt=getuploadparamsresponce.signature
            posturl="http://invalidposturl/2999834."+self.uploadvolumeformat
            metadata=getuploadparamsresponce.metadata
            expiredata=getuploadparamsresponce.expires
            #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
            url=self.uploadurl

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

            self.debug(results.status_code) 
            if results.status_code !=200: 
                self.fail("Upload is not fine")

            self.validate_uploaded_volume(getuploadparamsresponce.id,'UploadedAbandoned')

        except Exception as ex:
                if "Max retries exceeded with url" in str(ex):
                    success = True

        self.assertEqual(
                                 success,
                                 True,
                                 "Verify - Tampered Post URL is handled")

        return(getuploadparamsresponce)


    def reuse_url(self):
        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=self.globalurl
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        url=self.uploadurl
        time.sleep(300)

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
        time.sleep(60)

        print(results.status_code)
        if results.status_code == 200: 
                self.fail("Upload URL is allowed to reuse")

        config = Configurations.list(
                                     self.apiclient,
                                     name='upload.operation.timeout'
                                     )

        uploadtimeout = int(config[0].value)
        time.sleep(uploadtimeout*60)
        self.validate_uploaded_volume(getuploadparamsresponce.id,'UploadAbandoned')
        return

    def validate_storage_cleanup(self,invalidpostvolume,cleanup_interval):

        list_volume_response = Volume.list(
                    self.apiclient,
                    id=invalidpostvolume.id
                )
        self.assertNotEqual(
                    list_volume_response,
                    None,
                    "Check if volume exists in ListVolumes"
                )

        config1 = Configurations.list(
                                     self.apiclient,
                                     name='upload.operation.timeout'
                                     )
        config2 = Configurations.list(
                                     self.apiclient,
                                     name='upload.monitoring.interval'
                                     )
        uploadtimeout = int(config1[0].value)
        monitorinterval=int(config2[0].value)

        if cleanup_interval >= ((uploadtimeout*60)+monitorinterval):
            time.sleep(cleanup_interval)
        else:
            time.sleep(((uploadtimeout*60)+monitorinterval))

        list_volume_response = Volume.list(
                    self.apiclient,
                    id=invalidpostvolume.id
                )
        self.assertEqual(
                    list_volume_response,
                    None,
                    "Storage Cleanup - Verify UploadAbandoned volumes are deleted"
                )


    def validate_max_vol_size(self,up_vol,volumestate):

        list_volume_response = Volume.list(
                    self.apiclient,
                    id=up_vol.id
                )
        self.assertNotEqual(
                    list_volume_response,
                    None,
                    "Check if volume exists in ListVolumes"
                )

        self.assertEqual(
                    list_volume_response[0].state,
                    volumestate,
                    "Check volume state in ListVolumes"
                )

        config = Configurations.list(
                                     self.apiclient,
                                     name='storage.max.volume.upload.size'
                                     )

        max_size = int(config[0].value)
        self.debug(max_size)
        self.debug(int(list_volume_response[0].size)/(1024*1024*1024))
        if (int(list_volume_response[0].size)/(1024*1024*1024)) > max_size:
            self.fail("Global Config storage.max.volume.upload.size is not considered with Browser Based Upload volumes")



    def browse_upload_volume_with_md5(self):
        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.checksum=self.md5sum
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.uploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')

        return(getuploadparamsresponce)

    def browse_upload_volume_with_invalid_md5(self):
        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.checksum="xxxxxxxx"
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.uploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')

        return(getuploadparamsresponce)

    def validate_vm(self,vmdetails,vmstate):

        time.sleep(120 )
        vm_response = VirtualMachine.list(
                self.apiclient,
                id=vmdetails.id,
            )
        self.assertEqual(
                isinstance(vm_response, list),
                True,
                "Check list VM response for valid list"
            )

            # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
                len(vm_response),
                0,
                "Check VMs available in List VMs response"
            )

        deployedvm = vm_response[0]
        self.assertEqual(
                deployedvm.state,
                vmstate,
                "Check the state of VM"
            )

    def deploy_vm(self):
            virtual_machine = VirtualMachine.create(
                                                    self.apiclient,
                                                    self.testdata["virtual_machine"],
                                                    templateid=self.template.id,
                                                    zoneid=self.zone.id,
                                                    accountid=self.account.name,
                                                    domainid=self.account.domainid,
                                                    serviceofferingid=self.service_offering.id,
                                                )
            self.validate_vm(virtual_machine,'Running')
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


    def attach_deleted_volume(self,vmlist,volume):

        success= False
        try:
            vmlist.attach_volume(
                    self.apiclient,
                    volume
                )
        except Exception as ex:
            if "Please specify a volume with the valid type: DATADISK" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "Attaching the Deleted Volume is handled appropriately not to get attached the deleted uploaded volume")

        return


    def reboot_vm(self,vmdetails):
        vmdetails.reboot(self.apiclient)
        self.validate_vm(vmdetails,'Running')

    def stop_vm(self,vmdetails):
        vmdetails.stop(self.apiclient)
        self.validate_vm(vmdetails,'Stopped')

    def start_vm(self,vmdetails):
        vmdetails.start(self.apiclient)
        self.validate_vm(vmdetails,'Running')

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
        #TODO: SIMENH: add another test the data on the restored VM.
        """Test recover Virtual Machine
        """

        #cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = vmdetails.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response = VirtualMachine.list(
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

    def deletevolume_fail(self,volumeid):
        """Delete a Volume attached to a VM
        """

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = volumeid
        success= False
        try:
            self.apiclient.deleteVolume(cmd)
        except Exception as ex:
            if "Please specify a volume that is not attached to any VM" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "DeleteVolume - verify Ready State volume (attached to a VM) is handled appropriately not to get deleted ")

        return

    def delete_volume(self,volumeid):
        """Delete a Volume attached to a VM
        """

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id =volumeid

        self.apiclient.deleteVolume(cmd)

    def download_volume(self,volumeid):

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = volumeid
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.zone.id
        extract_vol = self.apiclient.extractVolume(cmd)

        try:
            formatted_url = urllib.parse.unquote_plus(extract_vol.url)
            self.debug("Attempting to download volume at url %s" % formatted_url)
            response = urllib.request.urlopen(formatted_url)
            self.debug("response from volume url %s" % response.getcode())
            fd, path = tempfile.mkstemp()
            self.debug("Saving volume %s to path %s" %(volumeid, path))
            os.close(fd)
            with open(path, 'wb') as fd:
                fd.write(response.read())
            self.debug("Saved volume successfully")
        except Exception:
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)" \
                % (extract_vol.url, volumeid)
            )

    def resize_fail(self,volumeid):

        cmd                = resizeVolume.resizeVolumeCmd()
        cmd.id             = volumeid
        cmd.diskofferingid = self.disk_offering.id
        success            = False
        try:
            self.apiclient.resizeVolume(cmd)
        except Exception as ex:
            if "Volume should be in ready or allocated state before attempting a resize" in str(ex):
                success = True
        self.assertEqual(
                success,
                True,
                "ResizeVolume - verify Uploaded State volume is handled appropriately")


    def resize_volume(self,volumeid):

        """Test resize a volume"""

        self.testdata["configurableData"]["browser_upload_volume"]["browser_resized_disk_offering"]["disksize"] = 20

        disk_offering_20_GB = DiskOffering.create(
                                    self.apiclient,
                                    self.testdata["configurableData"]["browser_upload_volume"]["browser_resized_disk_offering"]
                                    )
        self.cleanup.append(disk_offering_20_GB)

        cmd= resizeVolume.resizeVolumeCmd()
        cmd.id= volumeid
        cmd.diskofferingid = disk_offering_20_GB.id

        self.apiclient.resizeVolume(cmd)

        count = 0
        success = False
        while count < 3:
            list_volume_response = Volume.list(
                                                self.apiclient,
                                                id=volumeid,
                                                type='DATADISK'
                                                )
            for vol in list_volume_response:
                if vol.id == volumeid and int(vol.size) == (int(disk_offering_20_GB.disksize) * (1024** 3)) and vol.state == 'Ready':
                    success = True
            if success:
                break
            else:
                time.sleep(10)
                count += 1

        self.assertEqual(
                         success,
                         True,
                         "Check if the data volume resized appropriately"
                         )

        return


    def destroy_vm(self,vmdetails):

        success            = False
        vmdetails.delete(self.apiclient, expunge=False)

        try:
            list_vm_response1 = VirtualMachine.list(
                                            self.apiclient,
                                            id=vmdetails.id
                                            )
        except Exception as ex:
            if "Unable to find a virtual machine with specified vmId" in str(ex):
                success = True

        if success == "True": 
            self.debug("VM is already expunged")
            return

        list_vm_response1 = VirtualMachine.list(
                                            self.apiclient,
                                            id=vmdetails.id
                                            )

        if list_vm_response1 is None:
            self.debug("VM already expunged")
            return

        if list_vm_response1[0].state=="Expunging":
            self.debug("VM already getting expunged")
            return

        list_vm_response = VirtualMachine.list(
                                            self.apiclient,
                                            id=vmdetails.id
                                            )
        if list_vm_response is None:
            self.debug("VM already expunged")
            return

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

        list_vm_response1 = VirtualMachine.list(
                                            self.apiclient,
                                            id=vmdetails.id
                                            )
        if list_vm_response1 is None:
            self.debug("VM already expunged")
            return

        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = vmdetails.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response1 = VirtualMachine.list(
                                            self.apiclient,
                                            id=vmdetails.id
                                            )
        if list_vm_response1 is None:
            self.debug("VM already expunged")
            return

        list_vm_response1 = VirtualMachine.list(
                                            self.apiclient,
                                            id=vmdetails.id
                                            )
        if list_vm_response1[0].state=="Expunging":
            self.debug("VM already getting expunged")
            return

        list_vm_response = VirtualMachine.list(
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
            list_vm_response = VirtualMachine.list(
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

    def volume_snapshot(self,volumedetails):
        """
        @summary: Test to verify creation of snapshot from volume
        and creation of template, volume from snapshot
        """

        if self.uploadvolumeformat=="QCOW2":
            config = Configurations.list(
                                     self.apiclient,
                                     name='kvm.snapshot.enabled'
                                     )
            kvmsnapshotenabled = config[0].value
            if kvmsnapshotenabled == "false":
                self.fail("Please enable kvm.snapshot.enable global config")
        list_volumes = Volume.list(
            self.apiclient,
            id=volumedetails.id
        )
        # Creating Snapshot from volume
        snapshot_created = Snapshot.create(
            self.apiclient,
            volumedetails.id,
        )

        self.assertIsNotNone(snapshot_created, "Snapshot not created")

        self.cleanup.append(snapshot_created)

        # Creating expected and actual values dictionaries
        expected_dict = {
            "id": volumedetails.id,
            "intervaltype": "MANUAL",
            "snapshottype": "MANUAL",
            "volumetype": list_volumes[0].type,
            "domain": self.domain.id
        }
        actual_dict = {
            "id": snapshot_created.volumeid,
            "intervaltype": snapshot_created.intervaltype,
            "snapshottype": snapshot_created.snapshottype,
            "volumetype": snapshot_created.volumetype,
            "domain": snapshot_created.domainid,
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Snapshot created from Volume details are not as expected"
        )
        return(snapshot_created)

    def volume_snapshot_volume(self,snapshot_created):

        # Creating Volume from snapshot
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([self.testdata["volume"]
                             ["diskname"], random_gen()])
        cmd.snapshotid = snapshot_created.id

        volume_from_snapshot = Volume(
            self.apiclient.createVolume(cmd).__dict__)

        self.assertIsNotNone(
            volume_from_snapshot,
            "Volume creation failed from snapshot"
        )

        return

    def volume_snapshot_template(self,snapshot_created):
        # Creating Template from Snapshot
        list_templates_before = Template.list(
            self.apiclient,
            templatefilter='self')

        if list_templates_before is None:
            templates_before_size = 0
        else:
            templates_before_size = len(list_templates_before)

        cmd = createTemplate.createTemplateCmd()
        cmd.name = self.testdata["ostype"]
        cmd.displaytext = self.testdata["ostype"]
        cmd.ostypeid = self.template.ostypeid
        cmd.snapshotid = snapshot_created.id
        cmd.ispublic = False
        cmd.passwordenabled = False

        template_from_snapshot = Template(
            self.apiclient.createTemplate(cmd).__dict__)

        self.assertIsNotNone(
            template_from_snapshot,
            "Template creation failed from snapshot"
        )

        self.cleanup.append(template_from_snapshot)

        # Creating expected and actual values dictionaries
        expected_dict = {
            "name": self.testdata["ostype"],
            "ostypeid": self.template.ostypeid,
            "type": "USER",
            "zone": self.zone.id,
            "passwordenabled": False,
            "ispublic": False,
            "size": self.disk_offering.disksize
        }
        actual_dict = {
            "name": template_from_snapshot.name,
            "ostypeid": template_from_snapshot.ostypeid,
            "type": template_from_snapshot.templatetype,
            "zone": template_from_snapshot.zoneid,
            "passwordenabled": template_from_snapshot.passwordenabled,
            "ispublic": template_from_snapshot.ispublic,
            "size": template_from_snapshot.size / (1024 * 1024 * 1024)
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        #self.assertEqual(
         #   True,
          #  status,
           # "Template created from Snapshot details are not as expected"
        #)

        list_templates_after = Template.list(
            self.apiclient,
            templatefilter='self')

        self.assertEqual(
            templates_before_size + 1,
            len(list_templates_after),
            "Template creation failed from snapshot"
        )
        return


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



    def uploadvol(self,getuploadparamsresponce):

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        success = False
        url=self.uploadurl

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
        list_volume_response = Volume.list(
                    self.apiclient,
                    id=getuploadparamsresponce.id
                )
        self.debug("======================Before SSVM Reboot==================")

        self.reboot_ssvm()
        self.debug("======================After SSVM Reboot==================")

        config = Configurations.list(
                                     self.apiclient,
                                     name='upload.operation.timeout'
                                     )

        uploadtimeout = int(config[0].value)
        time.sleep(uploadtimeout*60)

        self.validate_uploaded_volume(getuploadparamsresponce.id,'UploadAbandoned')

        return()



    def uploadvolwithssvmreboot(self,getuploadparamsresponce):

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires

        self.debug("======================Before SSVM Reboot==================")
        list_volume_response = Volume.list(
                    self.apiclient,
                    id=getuploadparamsresponce.id
                )

        self.debug(list_volume_response[0])
        self.reboot_ssvm()

        success = False
        url=self.uploadurl

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
        list_volume_response = Volume.list(
                    self.apiclient,
                    id=getuploadparamsresponce.id
                )

        self.debug("======================Upload After SSVM Reboot==================")
        self.debug(list_volume_response[0])

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')

        return()

    def uploadwithcustomoffering(self):

        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.diskofferingid=self.disk_offering.id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        self.globalurl=getuploadparamsresponce.postURL
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.uploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')



    def uploadwithimagestoreid(self):

        sscmd=listImageStores.listImageStoresCmd()
        sscmd.zoneid=self.zone.id
        sscmdresponse=self.apiclient.listImageStores(sscmd)

        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.imagestoreuuid=sscmdresponse[0].id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        self.globalurl=getuploadparamsresponce.postURL
        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.uploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')
    def uploadwithsamedisplaytext(self,voldetails):


        list_volume_response = Volume.list(
                    self.apiclient,
                    id=voldetails.id
                )

        success=True
        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=list_volume_response[0].name
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)
        list_volume_response1 = Volume.list(
                  self.apiclient,
                    id=getuploadparamsresponce.id
                )
        if list_volume_response1[0].name==voldetails.name:
           success=False

        self.assertEqual(
                success,
                False,
                "Verify: Upload Multiple volumes with same name is handled")

        return

    def uploadvolwithmultissvm(self):

        ssvmhosts = list_hosts(
            self.apiclient,
            type="SecondaryStorageVM"
        )
        self.debug("Total SSVMs are:")
        self.debug(len(ssvmhosts))

        if len(ssvmhosts)==1:
            return(1)

        config = Configurations.list(
                                     self.apiclient,
                                     name='secstorage.session.max'
                                     )

        multissvmvalue = int(config[0].value)
        if multissvmvalue !=1:
            return(0)

        browseup_vol=self.browse_upload_volume()

        vm1details=self.deploy_vm()

        self.attach_volume(vm1details,browseup_vol.id)

        self.vmoperations(vm1details)

        self.destroy_vm(vm1details)

        self.detach_volume(vm1details,browseup_vol.id)

        cmd=deleteVolume.deleteVolumeCmd()
        cmd.id=browseup_vol.id
        self.apiclient.deleteVolume(cmd)

        return(2)


    def uploadwithextendedfileextentions(self):

        cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
        cmd.zoneid = self.zone.id
        cmd.format = self.uploadvolumeformat
        cmd.name=self.volname+self.account.name+(random.choice(string.ascii_uppercase))
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.diskofferingid=self.disk_offering.id
        getuploadparamsresponce=self.apiclient.getUploadParamsForVolume(cmd)

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires

        #url = 'http://10.147.28.7/templates/rajani-thin-volume.vhd'
        url=self.extuploadurl

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

        self.validate_uploaded_volume(getuploadparamsresponce.id,'Uploaded')



    def posturlwithdeletedvolume(self,getuploadparamsresponce):

        signt=getuploadparamsresponce.signature
        posturl=getuploadparamsresponce.postURL
        metadata=getuploadparamsresponce.metadata
        expiredata=getuploadparamsresponce.expires
        self.validate_uploaded_volume(getuploadparamsresponce.id,'UploadAbandoned')

        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = getuploadparamsresponce.id

        self.apiclient.delete_volume(cmd)

        success = False

        url=self.extuploadurl

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
        if results.status_code ==200: 
            return("FAIL")
        return("PASS")

    def volume_migration(self,browseup_vol,vm1details):
        
        pools = StoragePool.list(
            self.apiclient,
            zoneid=self.zone.id
        )
        if not pools:
            self.skipTest(
                "No suitable storage pools found for volume migration.\
                        Skipping")
        self.assertEqual(
            validateList(pools)[0],
            PASS,
            "invalid pool response from findStoragePoolsForMigration")
        pool = pools[0]

        try:
            if vm1details is None:
                Volume.migrate(
                               self.apiclient,
                               volumeid=browseup_vol.id,
                               storageid=pool.id,
                               livemigrate='false'
                               )
            else:
                Volume.migrate(
                               self.apiclient,
                               volumeid=browseup_vol.id,
                               storageid=pool.id,
                               livemigrate='true'
            )

        except Exception as e:
            self.fail("Volume migration failed with error %s" % e)

        return


    def getvolumelimts(self):

        totalresoucelist=Account.list(
                                      self.apiclient,
                                      id=self.account.id
                                      )
        totalvolumes=totalresoucelist[0].volumetotal

        return(totalvolumes)


    def getstoragelimts(self,rtype):

        cmd=updateResourceCount.updateResourceCountCmd()
        cmd.account=self.account.name
        cmd.domainid=self.domain.id
        cmd.resourcetype=rtype

        responce=self.apiclient.updateResourceCount(cmd)

        totalstorage=responce[0].resourcecount

        return(totalstorage)


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_01_Browser_volume_Life_cycle_tpath(self):
        """
        Test Browser_volume_Life_cycle - This includes upload volume,attach to a VM, write data ,Stop ,Start, Reboot,Reset  of a VM, detach,attach back to the VM, delete volumes  
        """
        try:

            self.debug("========================= Test 1: Upload Browser based volume and validate ========================= ")
            browseup_vol=self.browse_upload_volume()

            self.debug("========================= Test 2: Deploy a VM , Attach Uploaded Browser based volume and validate VM Operations========================= ")

            vm1details=self.deploy_vm()

            self.attach_volume(vm1details,browseup_vol.id)

            self.vmoperations(vm1details)

            self.debug("========================= Test 3: Restore VM with Uploaded volume attached========================= ")

            self.restore_vm(vm1details)

            self.debug("========================= Test 4: Detach Uploaded volume and validation of VM operations after detach========================= ")

            self.detach_volume(vm1details,browseup_vol.id)

            self.vmoperations(vm1details)

            self.destroy_vm(vm1details)

            self.debug("========================= Test 5: Deploy New VM,Attach the detached Uploaded volume and validate VM operations after attach========================= ")

            vm2details=self.deploy_vm()

            self.attach_volume(vm2details,browseup_vol.id)

            self.vmoperations(vm2details)

            self.debug("========================= Test 6: Detach Uploaded volume and resize detached uploaded volume========================= ")

            self.detach_volume(vm2details,browseup_vol.id)

            if self.hypervisor.lower() != "hyperv":
                self.resize_volume(browseup_vol.id)

            self.debug("========================= Test 7: Attach resized uploaded volume and validate VM operations========================= ")

            self.attach_volume(vm2details,browseup_vol.id)

            self.vmoperations(vm2details)
            self.detach_volume(vm2details,browseup_vol.id)

            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol.id
            self.apiclient.deleteVolume(cmd)

            self.debug("========================= Test 8: Try resizing uploaded state volume and validate the error scenario========================= ")

            browseup_vol2=self.browse_upload_volume()

            self.resize_fail(browseup_vol2.id)

            self.debug("========================= Test 9: Attach multiple uploaded volumes to a VM and validate VM operations========================= ")

            browseup_vol3=self.browse_upload_volume()

            self.attach_volume(vm2details,browseup_vol2.id)

            self.attach_volume(vm2details,browseup_vol3.id)

            self.vmoperations(vm2details)

            self.debug("========================= Test 10:  Detach and delete uploaded volume========================= ")

            self.detach_volume(vm2details,browseup_vol2.id)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol2.id
            self.apiclient.deleteVolume(cmd)


            self.debug("========================= Test 11:  Detach and download uploaded volume========================= ")

            self.detach_volume(vm2details,browseup_vol3.id)

            self.download_volume(browseup_vol3.id)

            self.debug("========================= Test 12:  Delete detached uploaded volume========================= ")


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol3.id
            self.apiclient.deleteVolume(cmd)

            self.debug("========================= Deletion of UnUsed VM's after test is complete========================= ")

            self.expunge_vm(vm2details)

            self.debug("========================= Test 13:  Delete Uploaded State volume========================= ")

            browseup_vol4=self.browse_upload_volume()


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol4.id
            self.apiclient.deleteVolume(cmd)

            self.debug("========================= Test 14:  Destroy VM which has Uploaded volumes attached========================= ")

            vm4details=self.deploy_vm()

            newvolumetodestoy_VM=self.browse_upload_volume()

            self.attach_volume(vm4details,newvolumetodestoy_VM.id)

            self.destroy_vm(vm4details)

            self.debug("========================= Test 15:  Recover destroyed VM which has Uploaded volumes attached========================= ")

            self.recover_destroyed_vm(vm4details)
            self.expunge_vm(vm4details)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=newvolumetodestoy_VM.id
            self.apiclient.deleteVolume(cmd)

            self.debug("========================= Test 16:  Delete attached Uploaded volume which is in ready state and it should not be allowed to delete========================= ")

            vm5details=self.deploy_vm()
            browseup_vol5=self.browse_upload_volume()
            self.attach_volume(vm5details,browseup_vol5.id)
            self.deletevolume_fail(browseup_vol5.id)

            self.debug("========================= Test 17:  Create Volume Backup Snapshot uploaded volume attached to the VM========================= ")

            vm6details=self.deploy_vm()
            browseup_vol6=self.browse_upload_volume()

            self.attach_volume(vm6details,browseup_vol6.id)

            snapshotdetails=self.volume_snapshot(browseup_vol6)

            self.debug("========================= Test 18:  Create Volume from Backup Snapshot of attached uploaded volume========================= ")

            self.volume_snapshot_volume(snapshotdetails)

            self.debug("========================= Test 19:  Create template from Backup Snapshot of attached uploaded volume========================= ")
            self.volume_snapshot_template(snapshotdetails)

            self.detach_volume(vm6details,browseup_vol6.id)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol6.id
            self.apiclient.deleteVolume(cmd)

            self.expunge_vm(vm6details)

            self.debug("========================= Test 20: Upload Browser based volume with checksum and validate ========================= ")
            browseup_vol_withchecksum=self.browse_upload_volume_with_md5()

            self.debug("========================= Test 21: Deploy a VM , Attach Uploaded Browser based volume with checksum and validate VM Operations========================= ")

            vm7details=self.deploy_vm()

            self.attach_volume(vm7details,browseup_vol_withchecksum.id)

            self.debug("========================= Test 22: Detach Uploaded volume with checksum and validation of VM operations after detach========================= ")

            self.detach_volume(vm7details,browseup_vol_withchecksum.id)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol_withchecksum.id

            self.apiclient.deleteVolume(cmd)


            self.vmoperations(vm7details)

            self.expunge_vm(vm7details)


        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_02_SSVM_Life_Cycle_With_Browser_Volume_TPath(self):
        """
        Test SSVM_Life_Cycle_With_Browser_Volume_TPath - This includes SSVM life cycle followed by Browser volume upload operations
        """
        try:
            
            self.debug("========================= Test 23: Stop and Start SSVM and Perform Browser based volume validations ========================= ")

            self.stop_ssvm()
            ssvm1browseup_vol=self.browse_upload_volume()

            ssvm1vm1details=self.deploy_vm()

            self.attach_volume(ssvm1vm1details,ssvm1browseup_vol.id)

            self.vmoperations(ssvm1vm1details)

            self.detach_volume(ssvm1vm1details,ssvm1browseup_vol.id)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=ssvm1browseup_vol.id
            self.apiclient.deleteVolume(cmd)

            self.expunge_vm(ssvm1vm1details)

            self.debug("========================= Test 24: Reboot SSVM and Perform Browser based volume validations ========================= ")

            self.reboot_ssvm()
            ssvm2browseup_vol=self.browse_upload_volume()

            ssvm2vm1details=self.deploy_vm()

            self.attach_volume(ssvm2vm1details,ssvm2browseup_vol.id)

            self.vmoperations(ssvm2vm1details)

            self.detach_volume(ssvm2vm1details,ssvm2browseup_vol.id)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=ssvm2browseup_vol.id
            self.apiclient.deleteVolume(cmd)


            self.expunge_vm(ssvm2vm1details)

            self.debug("========================= Test 25: Reboot SSVM and Perform Browser based volume validations ========================= ")

            self.destroy_ssvm()
            ssvm3browseup_vol=self.browse_upload_volume()

            ssvm3vm1details=self.deploy_vm()

            self.attach_volume(ssvm3vm1details,ssvm3browseup_vol.id)

            self.vmoperations(ssvm3vm1details)

            self.detach_volume(ssvm3vm1details,ssvm3browseup_vol.id)


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=ssvm3browseup_vol.id
            self.apiclient.deleteVolume(cmd)


            self.expunge_vm(ssvm3vm1details)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_03_Browser_Upload_Volume_Global_Config_TPath(self):
        """
        Test Browser_Upload_Volume_Global_Config limits 
        """
        try:
            
            self.debug("========================= Test 26 Validate Storage.max.upload.size ========================= ")
            globalconfig_browse_up_vol=self.browse_upload_volume()
            self.validate_max_vol_size(globalconfig_browse_up_vol,"Uploaded")


        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_04_Browser_Upload_Volume_Negative_Scenarios_TPath(self):
        """
        Test Browser_Upload_Volume_Negative_Scenarios
        """
        try:
            self.debug("========================= Test 27 Reuse the POST URL after expiry time========================= ")
            reuse_browse_up_vol=self.browse_upload_volume()
            self.reuse_url()

            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=reuse_browse_up_vol.id
            self.apiclient.deleteVolume(cmd)


            self.debug("========================= Test 28 Reboot SSVM before upload is completed=========================")
            browse_up_vol=self.onlyupload()
            self.uploadvol(browse_up_vol)

            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browse_up_vol.id
            self.apiclient.deleteVolume(cmd)


            self.debug("========================= Test 29 Reboot SSVM after getting the upload volume params and before initiating the upload=========================")
            browse_up_vol=self.onlyupload()
            self.uploadvolwithssvmreboot(browse_up_vol)

            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=reuse_browse_up_vol.id
            self.apiclient.deleteVolume(cmd)

            self.debug("========================= Test 30 Attach Deleted Volume=========================")
            deleted_browse_up_vol=self.browse_upload_volume()


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=deleted_browse_up_vol.id
            self.apiclient.deleteVolume(cmd)


            deletedvm1details=self.deploy_vm()
            self.attach_deleted_volume(deletedvm1details, deleted_browse_up_vol)

            self.debug("========================= Test 31 Upload Volume with Invalid Format=========================")
            self.invalidupload()

            self.debug("========================= Test 32 Upload Mutliple Volumes with same display text=========================")
            samedisplaytext_browse_up_vol=self.browse_upload_volume()
            self.uploadwithsamedisplaytext(samedisplaytext_browse_up_vol)

            self.debug("========================= Test 33 Upload Volume with custom offering id=========================")
            self.uploadwithcustomoffering()

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_05_Browser_Upload_Volume_MultiSSVM_Scenarios_TPath(self):
        """
        Test Browser_Upload_Volume_MultiSSVM_Scenarios
        """
        try:

            self.debug("========================= Test 34 Upload volume with Multiple SSVM=========================")

            testresult=self.uploadvolwithmultissvm()
            if testresult==0:
                raise unittest.SkipTest("secstorage.session.max global config is not set to 1 which means Multiple SSVM's are not present")
            elif testresult==1:
                raise unittest.SkipTest("only one SSVM is present")

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_06_Browser_Upload_Volume_with_extended_file_extenstions(self):
        """
        Test Browser_Upload_Volume_with_extended_file_extenstions
        """

        try:
            self.debug("========================= Test 35 Upload volume with extended file extenstions=========================")
            if self.uploadvolumeformat=="OVA":
                 raise unittest.SkipTest("This test is need not be executed on VMWARE")
            self.uploadwithextendedfileextentions()

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_07_Browser_Upload_Volume_Storage_Cleanup_Config_Validation(self):
        """
        Test Browser_Upload_Volume_Storage_Cleanup_Config_Validation
        """
        self.debug("========================= Test 36 Validate storage.cleanup.enabled and storage.cleanup.interval ========================= ")
        config1 = Configurations.list(
                                     self.apiclient,
                                     name='storage.cleanup.enabled'
                                     )

        config2 = Configurations.list(
                                     self.apiclient,
                                     name='storage.cleanup.interval'
                                     )

        cleanup_enabled=config1[0].value
        cleanup_interval = int(config2[0].value)

        if cleanup_enabled=="false":
                raise unittest.SkipTest("storage.cleanup.enabled is not set to true")

        if cleanup_interval>600:
                raise unittest.SkipTest("storage.cleanup.interval is set to wait for more than 10 mins before cleanup. Please reduce the interval to less than 10 mins")

        invaliduploadvolume=self.invalidposturl()

        self.validate_storage_cleanup(invaliduploadvolume,cleanup_interval)

        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_08_Browser_Upload_Volume_TamperedPostURL(self):
        """
        Test Browser_Upload_Volume_Negative_Scenarios
        """
        try:
            self.debug("========================= Test 37 Upload Volume with tampered post URL=========================")
            invaliduploadvolume=self.invalidposturl()

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_09_Browser_Upload_Volume_PostURL_with_Deleted_Uploadvolume_Details(self):
        """
        Test Browser_Upload_Volume_PostURL_with_Deleted_Uploadvolume_Details
        """
        self.debug("========================= Test 38 PostURL_with_Deleted_Upload_Abondaned volume details=========================")
        browse_up_vol=self.onlyupload()
        res=self.posturlwithdeletedvolume(browse_up_vol)

        if res=="FAIL":
            self.fail("Verify - PostURL_with_Deleted_Uploadvolume_Details ")

        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_10_Browser_Upload_Volume_API_with_imagepoolid(self):
        """
        Test Browser_Upload_Volume_API_with_imagepoolid
        """
        self.debug("========================= Test 39 Test Browser_Upload_Volume_API_with_imagepoolid=========================")
        self.uploadwithimagestoreid()

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_11_migrate_upload_volume(self):
        """
        Test Browser_Upload_Volume_migrate_upload_volume
        """
        self.debug("========================= Test 40 Test Browser_Upload_Volume_Migration=========================")

        browseup_vol=self.browse_upload_volume()
        vm1details=self.deploy_vm()
        self.attach_volume(vm1details,browseup_vol.id)
        self.volume_migration(browseup_vol, vm1details)
        self.debug("========================= Test 41 Test VM Operations after Browser_Upload_Volume_Migration=========================")
        self.vmoperations(vm1details)

        self.debug("========================= Test 42 Detach Browser_Upload_Volume after Migration and attach to a new VM=========================")
        self.detach_volume(vm1details,browseup_vol.id)
        vm2details=self.deploy_vm()
        self.attach_volume(vm2details,browseup_vol.id)
        self.vmoperations(vm2details)

        self.debug("========================= Test 43 Detach Browser_Upload_Volume and Migrate to another storage=========================")

        self.detach_volume(vm2details,browseup_vol.id)
        self.volume_migration(browseup_vol, "None")

        self.debug("========================= Test 44 Attach detached Browser_Upload_Volume after Migration =========================")

        self.attach_volume(vm2details,browseup_vol.id)
        self.vmoperations(vm2details)

        self.debug("========================= Test 45 Detach  ,Resize,Attach  Browser_Upload_Volume after Migration =========================")

        self.detach_volume(vm2details,browseup_vol.id)

        if self.hypervisor.lower() != "hyperv":
            self.resize_volume(browseup_vol.id)

        self.attach_volume(vm2details,browseup_vol.id)
        self.vmoperations(vm2details)

        self.detach_volume(vm2details,browseup_vol.id)

        self.cleanup.append(browseup_vol)
        self.cleanup.append(vm2details)
        self.cleanup.append(vm1details)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_12_Browser_Upload_Volume_with_all_API_parameters(self):
        """
        Test Browser_Upload_Volumewith all API parameters
        """
        try:

            self.debug("========================= Test 46 & 47 Upload volume with account name and domainid========================")

            browseup_vol1=self.browse_upload_volume()

            self.debug("========================= Test 48 Upload volume with projectid========================")
            browseup_vol2=self.browse_upload_volume_with_projectid(self.project.id)

            self.debug("========================= Test 49 Upload volume with out mandatory param zone id ========================")

            browseup_vol2=self.browse_upload_volume_with_out_zoneid()


            self.debug("========================= Test 50 Upload volume with out mandatory param format ========================")

            browseup_vol3=self.browse_upload_volume_with_out_format()

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return



    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_13_Browser_Upload_Volume_volume_resource_limits(self):
        """
        Test Browser_Upload_Volume Volume Resource limits
        """
        try:

            self.debug("========================= Test 51 Upload volume and verify volume limits========================")
            initialvolumelimit=self.getvolumelimts()
            browseup_vol1=self.browse_upload_volume()
            afteruploadvolumelimit=self.getvolumelimts()

            if int(afteruploadvolumelimit)!=(int(initialvolumelimit)+1):
                self.fail("Volume Resouce Count is not updated")


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=deleted_browse_up_vol1.id
            self.apiclient.deleteVolume(cmd)


        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_14_Browser_Upload_Volume_secondary_storage_resource_limits(self):
        """
        Test Browser_Upload_Volume Secondary Storage Resource limits
        """
        try:

            self.debug("========================= Test 52 Upload volume and verify secondary storage limits========================")

            initialsecondarystoragelimit=self.getstoragelimts(11)
            browseup_vol1=self.browse_upload_volume()
            volumedetails=Volume.list(
                                      self.apiclient,
                                      id=browseup_vol1.id)
            afteruploadsecondarystoragelimit=self.getstoragelimts(11)

            if afteruploadsecondarystoragelimit!=(initialsecondarystoragelimit+volumedetails[0].size):
                self.fail("Secondary Storage Resouce Count is not updated")


            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=deleted_browse_up_vol1.id
            self.apiclient.deleteVolume(cmd)


        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_15_Browser_Upload_Volume_primary_storage_resource_limits(self):
        """
        Test Browser_Upload_Volume Primary Storage Resource limits
        """
        try:

            self.debug("========================= Test 53 Attach Upload volume and verify primary storage limits========================")

            initialprimarystoragelimit=self.getstoragelimts(10)
            browseup_vol1=self.browse_upload_volume()
            volumedetails=Volume.list(
                                      self.apiclient,
                                      id=browseup_vol1.id)
            afteruploadprimarystoragelimit=self.getstoragelimts(10)

            if afteruploadprimarystoragelimit!=(initialprimarystoragelimit+volumedetails[0].size):
                self.fail("Primary Storage Resource Count is not updated")

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_16_Browser_Upload_volume_resource_limits_after_deletion(self):
        """
        Test Browser_Upload_Volume resource_limits_after_deletion
        """
        try:
            self.debug("========================= Test 54 Delete Upload volume and verify volume limits========================")
            browseup_vol1=self.browse_upload_volume()
            initialvolumelimit=self.getvolumelimts()

            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol1.id
            self.apiclient.deleteVolume(cmd)

            aftervolumelimit=self.getvolumelimts()

            if aftervolumelimit!=(initialvolumelimit-1):
                self.fail("Volume Resource Count is not updated after deletion")

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return



    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="true")
    def test_17_Browser_Upload_Volume_secondary_storage_resource_limits_after_deletion(self):
        """
        Test Browser_Upload_Volume secondary_storage_resource_limits_after_deletion
        """
        try:
            self.debug("========================= Test 55 Delete Upload volume and secondary storage limits========================")

            browseup_vol1=self.browse_upload_volume()

            volumedetails=Volume.list(
                                      self.apiclient,
                                      id=browseup_vol1.id)

            initialuploadsecondarystoragelimit=self.getstoragelimts(11)

            cmd=deleteVolume.deleteVolumeCmd()
            cmd.id=browseup_vol1.id
            self.apiclient.deleteVolume(cmd)

            afteruploadsecondarystoragelimit=self.getstoragelimts(11)

            if afteruploadsecondarystoragelimit!=(initialuploadsecondarystoragelimit-volumedetails[0].size):
                self.fail("Secondary Storage Resouce Count is not updated after deletion")

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return


    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_browser_upload_volume_incomplete(self):
        """
        Test browser based incomplete volume upload, followed by SSVM destroy. Volume should go to UploadAbandoned/Error state and get cleaned up.
        """
        try:
            self.debug("========================= Test browser based incomplete volume upload ========================")

            #Only register volume, without uploading
            cmd = getUploadParamsForVolume.getUploadParamsForVolumeCmd()
            cmd.zoneid = self.zone.id
            cmd.format = self.uploadvolumeformat
            cmd.name = self.volname + self.account.name + (random.choice(string.ascii_uppercase))
            cmd.account = self.account.name
            cmd.domainid = self.domain.id
            upload_volume_response = self.apiclient.getUploadParamsForVolume(cmd)

            #Destroy SSVM, and wait for new one to start
            self.destroy_ssvm()

            #Verify that the volume is cleaned up as part of sync-up during new SSVM start
            self.validate_uploaded_volume(upload_volume_response.id, 'UploadAbandoned')

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


