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

# Import Local Modules
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import seedTemplateFromVmSnapshot
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Account,
                             DiskOffering,
                             ServiceOffering,
                             VirtualMachine,
                             VmSnapshot,
                             Volume)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template)
import urllib
import tempfile
import os
import time
from functools import reduce


class TestDeployVm(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVm, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if not cls.hypervisor.lower() in "vmware":
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )

        cls.service_offering1 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls.disk_offering = DiskOffering.create(cls.apiclient,
                                                cls.services["disk_offering"])
        cls._cleanup.append(cls.service_offering)
        cls._cleanup.append(cls.service_offering1)
        cls._cleanup.append(cls.disk_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.zone.networktype
        )

        cls.vm_snp = VmSnapshot.create(cls.apiclient,
                                       vmid=cls.virtual_machine.id,
                                       )

        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced", "advancedns", "smoke"],
          required_hardware="true")
    def test_01_create_vm_from_vmsnapshots(self):
        """Test to create VM from vm snapshots without data disk
        1-deploy a vm from snapshot
        2-os type of new vm should be same as parent vm
        3-destroy vm
        4-check  volume usage
        5-check vm usage
        """
        vm1_from_snp = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            vmsnapshotid=self.vm_snp.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering1.id,
            mode=self.zone.networktype
        )

        self.assertEqual(vm1_from_snp.ostypeid,
                         self.virtual_machine.ostypeid,
                         "check the os type for vm deployed from vm snapshot")
        self.assertEqual(vm1_from_snp.serviceofferingid,
                         self.service_offering1.id,
                         "check the service offering id for "
                         "vm deployed from vm snapshot")
        vm1_from_snp.delete(self.apiclient)

        vm_id = self.dbclient.execute(
            "select * from vm_instance where uuid = '%s';"
            % vm1_from_snp.id
        )
        vol_id = self.dbclient.execute(
            "select * from volumes where instance_id = '%s';"
            % vm_id[0][0]
        )
        vol_usage = self.dbclient.execute(
            "select * from usage_event where resource_id= '%s' "
            "and resource_name= '%s';"
            % (vol_id[0][0], vol_id[0][7])
        )
        self.assertEqual(len(vol_usage),
                         3,
                         "check number of  volume usage")
        self.assertEqual(vol_usage[0][1],
                         "VOLUME.CREATE",
                         "Check for VOLUME.CREATE usage event"
                         )
        self.assertEqual(vol_usage[1][1],
                         "VOLUME.DELETE",
                         "Check for VOLUME.DELETE usage event"
                         )
        self.assertEqual(vol_usage[2][1],
                         "VOLUME.DELETE",
                         "check for VOLUME.DELETE usage event")

    @attr(tags=["basic", "advanced", "advancedns", "smoke"],
          required_hardware="true")
    def test_02_create_vm_from_vmsnapshots_and_verify_data(self):
        """
        deploy vm from snapshot , ssh and verify data
        1-ssh to vm and create file
        2-take vm snapshot
        3-repeat step 1-2 for three time
        4-deploy vm from vm snapshot created in step 1-2
        5-ssh to vm and add some data
        """
        result = {}
        vm_snp = {}
        for i in xrange(3):
            try:
                ssh = self.virtual_machine.get_ssh_client()
                ssh.execute('touch f%s' % i)
                ssh.execute('echo "this is file f%s">>f%s' % (i, i))
                result[i] = ssh.execute('cat f%s' % i)

            except Exception as e:
                self.fail("SSH access failed for VM %s - %s" %
                          (self.virtual_machine.ipaddress, e))
            time.sleep(60)
            vm_snp[i] = VmSnapshot.create(self.apiclient,
                                          vmid=self.virtual_machine.id
                                          )

            ssh.execute('rm -rf f%s' % i)

        for i in xrange(3):

            vm_from_snp = VirtualMachine.create(
                self.apiclient,
                self.services["small"],
                vmsnapshotid=vm_snp[i].id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering1.id,
                mode=self.zone.networktype
            )
            ssh = vm_from_snp.get_ssh_client()

            vm_result = ssh.execute('cat f%s' % i)
            self.assertEqual(result[i],
                             vm_result,
                             "check vm%s data " % i)

    @attr(tags=["basic", "advanced", "advancedns", "smoke"],
          required_hardware="true")
    def test_03_seed_template_from_vmsnapshot(self):
        """
         Seed template and then deploy vm and check if time
         is same for vm deployment
         1-deploy vm and record time
         2-seed template
         3-deploy multiple vm from seeded template and record avg time
         4-compare avg time and time recorded in step 2
        """

        vm_snp = VmSnapshot.create(self.apiclient,
                                   vmid=self.virtual_machine.id)

        tx1 = time.time()
        VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            vmsnapshotid=vm_snp.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering1.id,
            mode=self.zone.networktype
        )
        k = int(time.time() - tx1)
        list_root_volume = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(validateList(list_root_volume)[0],
                         PASS,
                         "check list volume responce")
        cmd = seedTemplateFromVmSnapshot.seedTemplateFromVmSnapshotCmd()
        cmd.zoneid = self.services["zoneid"]
        cmd.vmsnapshotid = vm_snp.id
        cmd.storageid = list_root_volume[0].storageid
        seed_temp = self.apiclient.seedTemplateFromVmSnapshot(cmd)

        self.assertEqual(seed_temp.storageid,
                         list_root_volume[0].storageid,
                         "check storage id of seeded template")

        record_time = []
        for i in xrange(4):
            t1 = time.time()
            VirtualMachine.create(
                self.apiclient,
                self.services["small"],
                vmsnapshotid=vm_snp.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering1.id,
                mode=self.zone.networktype
            )
            record_time.append(int(time.time() - t1))

        avg_time = reduce(lambda x, y: x + y, record_time) / len(record_time)
        self.assertGreater(
            k,
            avg_time,
            "check time taken to deploy vm after template seeding")


class ExtractVolumeFromVmSnapshot(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(ExtractVolumeFromVmSnapshot, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if not cls.hypervisor.lower() in "vmware":
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = cls.template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )

        cls.service_offering1 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls.disk_offering = DiskOffering.create(cls.apiclient,
                                                cls.services["disk_offering"])
        cls._cleanup.append(cls.service_offering)
        cls._cleanup.append(cls.service_offering1)
        cls._cleanup.append(cls.disk_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            diskofferingid=cls.disk_offering.id,
            mode=cls.zone.networktype
        )

        cls.vm_snp = VmSnapshot.create(cls.apiclient,
                                       vmid=cls.virtual_machine.id,
                                       )
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["basic", "advanced", "advancedns", "smoke"],
          required_hardware="true")
    def test_01_create_volume_from_vmsnapshot(self):
        """
        create volume from vmshnapshot and verify usage
        1-create volume from vmsnapshot
        2-delete volume
        3-verify usage
        """

        list_attached_volume = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid,
            type='DATA')

        self.assertEqual(validateList(list_attached_volume)[0],
                         PASS,
                         "check list volume responce")

        createVolume = Volume.create_from_vmsnapshot(
            self.apiclient,
            self.services,
            vmsnapshotid=self.vm_snp.id,
            volumeid=list_attached_volume[0].id,
            account=self.account.name)
        list_attached_volume2 = Volume.list(self.apiclient,
                                            id=createVolume.id,
                                            account=self.account.name,
                                            domainid=self.account.domainid
                                            )
        self.assertEqual(validateList(list_attached_volume2)[0],
                         PASS,
                         "check list volume response")
        self.assertEqual(list_attached_volume[0].storageid,
                         list_attached_volume[0].storageid,
                         "storage id of created volume")

        self.assertEqual(createVolume.diskofferingid,
                         self.disk_offering.id,
                         "check the disk offering id"
                         )
        self.assertEqual(createVolume.domainid,
                         self.account.domainid,
                         "check the domain id")
        self.assertEqual(createVolume.state,
                         "Ready",
                         "check the created volume state")
        self.assertEqual(createVolume.size,
                         list_attached_volume[0].size,
                         "check the created volume size"
                         )

        self.assertEqual(list_attached_volume2[0].storageid,
                         list_attached_volume[0].storageid,
                         "check the storage id of volume"
                         )
        createVolume.delete(self.apiclient)

        vol_id = self.dbclient.execute(
            "select * from volumes where uuid= '%s';"
            % createVolume.id)
        vol_usage = self.dbclient.execute(
            "select * from usage_event where resource_id= '%s' and "
            "resource_name= '%s';"
            % (vol_id[0][0], createVolume.name)
        )
        self.assertEqual(len(vol_usage),
                         2,
                         "check number of usage for volume")
        self.assertEqual(vol_usage[0][1],
                         "VOLUME.CREATE",
                         "check VOLUME.CREATE usage events")
        self.assertEqual(vol_usage[0][9],
                         createVolume.size,
                         "check the volume size in usage")
        self.assertEqual(vol_usage[1][1],
                         "VOLUME.DELETE",
                         "check VOLUME.DELETE usage events")

    @attr(tags=["basic", "advanced", "advancedns", "smoke"],
          required_hardware="true")
    def test_02_create_volume_from_vmsnapshot(self):
        """
        create volume from vm snapshot and download
        1-create volume from vm snapshot
        2-download volume
        """

        list_attached_volume = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid,
            type='DATA')

        self.assertEqual(validateList(list_attached_volume)[0],
                         PASS,
                         "check list volume responce")

        createVolume = Volume.create_from_vmsnapshot(
            self.apiclient,
            self.services,
            vmsnapshotid=self.vm_snp.id,
            volumeid=list_attached_volume[0].id,
            account=self.account.name
        )
        list_attached_volume2 = Volume.list(self.apiclient,
                                            id=createVolume.id,
                                            account=self.account.name,
                                            domainid=self.account.domainid
                                            )
        self.assertEqual(validateList(list_attached_volume2)[0],
                         PASS,
                         "check list volume response")
        extract_volume = Volume.extract(self.apiclient,
                                        volume_id=createVolume.id,
                                        zoneid=self.zone.id,
                                        mode="HTTP_DOWNLOAD")

        try:
            formatted_url = urllib.unquote_plus(extract_volume.url)
            self.debug(
                "Attempting to download volume at url %s" %
                formatted_url)
            response = urllib.urlopen(formatted_url)
            self.debug("response from volume url %s" % response.getcode())
            fd, path = tempfile.mkstemp()
            self.debug("Saving volume %s to path %s" % (createVolume.id, path))
            os.close(fd)
            with open(path, 'wb') as fd:
                fd.write(response.read())
            self.debug("Saved volume successfully")
        except Exception:
            self.fail(
                "Extract Volume Failed with invalid URL %s (vol id: %s)"
                % (extract_volume.url, createVolume.id)
            )

    @attr(tags=["basic", "advanced", "advancedns", "smoke"],
          required_hardware="true")
    def test_03_create_volume_from_vmsnapshot(self):
        """
        create volume from snapshot and attach to vm
        1-create volume from vm snapshot
        2-attach-detach volume
        3-extract volume
        4-attach-detach volume
        """
        list_attached_volume = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            account=self.account.name,
            domainid=self.account.domainid,
            type='DATA')

        self.assertEqual(validateList(list_attached_volume)[0],
                         PASS,
                         "check list volume responce")

        createVolume = Volume.create_from_vmsnapshot(
            self.apiclient,
            self.services,
            vmsnapshotid=self.vm_snp.id,
            volumeid=list_attached_volume[0].id,
            account=self.account.name
        )
        list_attached_volume2 = Volume.list(self.apiclient,
                                            id=createVolume.id,
                                            account=self.account.name,
                                            domainid=self.account.domainid
                                            )
        self.assertEqual(validateList(list_attached_volume2)[0],
                         PASS,
                         "check list volume response")
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype
        )
        vol_attached = self.virtual_machine.attach_volume(self.apiclient,
                                                          volume=createVolume)

        self.virtual_machine.detach_volume(self.apiclient,
                                           volume=createVolume)

        self.assertEqual(vol_attached.deviceid,
                         1,
                         "check device id of attached volume")
        self.assertEqual(vol_attached.storageid,
                         list_attached_volume2[0].storageid,
                         "Check storageid of attached volume")
        self.assertEqual(vol_attached.virtualmachineid,
                         self.virtual_machine.id,
                         "check vm id to which volume is attached")

        Volume.extract(self.apiclient,
                       volume_id=createVolume.id,
                       zoneid=self.zone.id,
                       mode="HTTP_DOWNLOAD")

        self.virtual_machine.attach_volume(self.apiclient,
                                           volume=createVolume)

        self.virtual_machine.detach_volume(self.apiclient,
                                           volume=createVolume)
