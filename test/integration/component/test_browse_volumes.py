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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Volume,
                             Host,
                             Iso,
                             Configurations,
                             DiskOffering,
                             Domain,
                             StoragePool)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_pod,list_hosts)
from marvin.codes import PASS,FAILED

from marvin.sshClient import SshClient

import requests

import time

import wget

import random

import string


class TestBrowseUploadVolume(cloudstackTestCase):

    """
    Testing Browse Upload Volume Feature
    """
    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestBrowseUploadVolume, cls).getClsTestClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.apiclient = cls.testClient.getApiClient()
        cls._cleanup = []
        cls.cleanup = []
        cls.uploadvolumeformat="VHD"

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

        cls.uploadurl=cls.testdata["browser_upload_volume"][cls.uploadvolumeformat]["url"]
        cls.volname=cls.testdata["browser_upload_volume"][cls.uploadvolumeformat]["diskname"]
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.pod = get_pod(cls.apiclient, cls.zone.id)

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
            cls.testdata["browser_upload_volume"]["browser_resized_disk_offering"],
            custom=True
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering,
            cls.disk_offering
        ]



    def validate_uploaded_volume(self,up_volid,volumestate):

        list_volume_response = Volume.list(
                    self.apiclient,
                    id=up_volid
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

        print results.status_code
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
        print list_volume_response[0]
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
        print list_volume_response[0]
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


    def download_volume(self,volumeid):

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = volumeid
        cmd.mode = "HTTP_DOWNLOAD"
        cmd.zoneid = self.zone.id
        extract_vol = self.apiclient.extractVolume(cmd)

        try:
            formatted_url = urllib.unquote_plus(extract_vol.url)
            self.debug("Attempting to download volume at url %s" % formatted_url)
            response = urllib.urlopen(formatted_url)
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

        self.testdata["browser_upload_volume"]["browser_resized_disk_offering"]["disksize"] = 20

        disk_offering_20_GB = DiskOffering.create(
                                    self.apiclient,
                                    self.testdata["browser_upload_volume"]["browser_resized_disk_offering"]
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

        vmdetails.delete(self.apiclient, expunge=False)

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
                            "Destroyed",
                            "Check virtual machine is in destroyed state"
                        )
        return


    def recover_destroyed_vm(self,vmdetails):

        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = vmdetails.id
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
                            "Stopped",
                            "Check virtual machine is in Stopped state"
                        )

        return

    def volume_snapshot(self):
        """
        @summary: Test to verify creation of snapshot from volume
        and creation of template, volume from snapshot
        """

        list_volumes = Volume.list(
            self.apiclient,
            id=volumedetails.id
        )
        # Creating Snapshot from volume
        snapshot_created = Snapshot.create(
            self.userapiclient,
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


        # Creating expected and actual values dictionaries
        expected_dict = {
            "snapshotid": snapshot_created.id,
            "volumetype": snapshot_created.volumetype,
            "size": self.disk_offering.disksize,
            "accounr": self.account.name,
            "domain": self.domain.id,
            "storagetype": self.storagetype,
            "zone": self.zone.id
        }
        actual_dict = {
            "snapshotid": volume_from_snapshot.snapshotid,
            "volumetype": volume_from_snapshot.type,
            "size": volume_from_snapshot.size / (1024 * 1024 * 1024),
            "accounr": volume_from_snapshot.account,
            "domain": volume_from_snapshot.domainid,
            "storagetype": volume_from_snapshot.storagetype,
            "zone": volume_from_snapshot.zoneid,
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Volume created from Snapshot details are not as expected"
        )
        return

    def volume_snapshost_template(self,snapshot_created):
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
            "name": self.services["ostype"],
            "ostypeid": self.template.ostypeid,
            "type": "USER",
            "zone": self.zone.id,
            "domain": self.domain.id,
            "account": self.account.name,
            "passwordenabled": False,
            "ispublic": False,
            "size": self.disk_offering.disksize
        }
        actual_dict = {
            "name": template_from_snapshot.name,
            "ostypeid": template_from_snapshot.ostypeid,
            "type": template_from_snapshot.templatetype,
            "zone": template_from_snapshot.zoneid,
            "domain": template_from_snapshot.domainid,
            "account": template_from_snapshot.account,
            "passwordenabled": template_from_snapshot.passwordenabled,
            "ispublic": template_from_snapshot.ispublic,
            "size": template_from_snapshot.size / (1024 * 1024 * 1024)
        }
        status = self.__verify_values(
            expected_dict,
            actual_dict
        )
        self.assertEqual(
            True,
            status,
            "Template created from Snapshot details are not as expected"
        )

        list_templates_after = Template.list(
            self.userapiclient,
            templatefilter='self')

        self.assertEquals(
            templates_before_size + 1,
            len(list_templates_after),
            "Template creation failed from snapshot"
        )
        return

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

            self.debug("========================= Test 5: Deploy New VM,Attach the detached Uploaded volume and validate VM operations after attach========================= ")

            vm2details=self.deploy_vm()

            self.attach_volume(vm2details,browseup_vol.id)

            self.vmoperations(vm2details)

            self.debug("========================= Test 6: Detach Uploaded volume and resize detached uploaded volume========================= ")

            self.detach_volume(vm2details,browseup_vol.id)

            self.resize_volume(browseup_vol.id)

            self.debug("========================= Test 7: Attach resized uploaded volume and validate VM operations========================= ")

            self.attach_volume(vm2details,browseup_vol.id)

            self.vmoperations(vm2details)

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

            self.deletevolume(browseup_vol2.id)

            self.debug("========================= Test 11:  Detach and download uploaded volume========================= ")

            self.detach_volume(vm2details,browseup_vol3.id)

            self.download_volume(browseup_vol3.id)

            self.debug("========================= Test 12:  Delete detached uploaded volume========================= ")

            self.detach_volume(vm2details,browseup_vol3.id)

            self.deletevolume(browseup_vol3.id)

 
            self.debug("========================= Test 13:  Delete Uploaded State volume========================= ")

            browseup_vol4=self.browse_upload_volume()

            self.deletevolume(browseup_vol4.id)

            self.debug("========================= Test 14:  Destroy VM which has Uploaded volumes attached========================= ")

            vm4details=self.deploy_vm()
            self.attach_volume(vm4details,browseup_vol4.id)

            self.destroy_vm(vm4details)

            self.debug("========================= Test 15:  Recover destroyed VM which has Uploaded volumes attached========================= ")

            self.recover_destroyed_vm(vm4details)

            self.debug("========================= Test 16:  Delete attached Uploaded volume which is in ready state========================= ")

            browseup_vol5=self.browse_upload_volume()
            self.attach_volume(vm4details,browseup_vol5.id)
            self.deletevolume(browseup_vol5.id)

            self.debug("========================= Test 17:  Create Volume Backup Snapshot uploaded volume attached to the VM========================= ")

            browseup_vol6=self.browse_upload_volume()

            self.attach_volume(vm2details,browseup_vol6.id)

            snapshotdetails=self.volume_snapshot(browseup_vol6.id)

            self.debug("========================= Test 18:  Create Volume from Backup Snapshot of attached uploaded volume========================= ")

            self.volume_snapshot_volume(snapshotdetails)

            self.debug("========================= Test 19:  Create template from Backup Snapshot of attached uploaded volume========================= ")
            self.volume_snapshot_template(snapshotdetails)

        except Exception as e:
            self.fail("Exception occurred  : %s" % e)
        return

    @classmethod
    def tearDownClass(self):
        try:
            self.apiclient = super(TestBrowseUploadVolume,self).getClsTestClient().getApiClient()
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
