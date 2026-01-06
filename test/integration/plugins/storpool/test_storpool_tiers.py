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

import pprint
import uuid

from marvin.cloudstackAPI import (listResourceDetails, addResourceDetail, changeOfferingForVolume)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (DiskOffering,
                             ServiceOffering,
                             StoragePool,
                             VirtualMachine,
                             SecurityGroup,
                             ResourceDetails
                             )
from marvin.lib.common import (get_domain,
                               get_template,
                               list_disk_offering,
                               list_storage_pools,
                               list_volumes,
                               list_service_offering,
                               list_zones)
from marvin.lib.utils import random_gen, cleanup_resources
from nose.plugins.attrib import attr
from storpool import spapi

from sp_util import (TestData, StorPoolHelper)


class TestStorPoolTiers(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        super(TestStorPoolTiers, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            raise

    @classmethod
    def setUpCloudStack(cls):
        config = cls.getClsConfig()
        StorPoolHelper.logger = cls

        zone = config.zones[0]
        assert zone is not None

        cls.spapi = spapi.Api(host=zone.spEndpoint, port=zone.spEndpointPort, auth=zone.spAuthToken, multiCluster=True)
        testClient = super(TestStorPoolTiers, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ("hyperv", "lxc"):
            cls.unsupportedHypervisor = True
            return

        cls._cleanup = []

        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = list_zones(cls.apiclient, name=zone.name)[0]

        td = TestData()
        cls.testdata = td.testdata
        cls.helper = StorPoolHelper()

        disk_offerings_tier1_tags = cls.testdata[TestData.diskOfferingTier1Tag]
        disk_offerings_tier2_tags = cls.testdata[TestData.diskOfferingTier2Tag]
        disk_offerings_tier1_template = cls.testdata[TestData.diskOfferingTier1Template]
        disk_offerings_tier2_template = cls.testdata[TestData.diskOfferingTier2Template]
        disk_offerings_tier2_tags_template = cls.testdata[TestData.diskOfferingWithTagsAndTempl]

        cls.qos = "SP_QOSCLASS"
        cls.spTemplate = "SP_TEMPLATE"

        cls.disk_offerings_tier1_tags = cls.getDiskOffering(disk_offerings_tier1_tags, cls.qos, "ssd")

        cls.disk_offerings_tier2_tags = cls.getDiskOffering(disk_offerings_tier2_tags, cls.qos, "virtual")

        cls.disk_offerings_tier1_template = cls.getDiskOffering(disk_offerings_tier1_template, cls.spTemplate, "ssd")

        cls.disk_offerings_tier2_template = cls.getDiskOffering(disk_offerings_tier2_template, cls.spTemplate,
                                                                "virtual")
        cls.disk_offerings_tier2_tags_template = cls.getDiskOffering(disk_offerings_tier2_tags_template, cls.spTemplate,
                                                                     "virtual")
        cls.resourceDetails(cls.qos, cls.disk_offerings_tier2_tags_template.id, "virtual")

        cls.account = cls.helper.create_account(
            cls.apiclient,
            cls.services["account"],
            accounttype=1,
            domainid=cls.domain.id,
            roleid=1
        )
        cls._cleanup.append(cls.account)

        securitygroup = SecurityGroup.list(cls.apiclient, account=cls.account.name, domainid=cls.account.domainid)[0]
        cls.helper.set_securityGroups(cls.apiclient, account=cls.account.name, domainid=cls.account.domainid,
                                      id=securitygroup.id)

        storpool_primary_storage = cls.testdata[TestData.primaryStorage]

        storpool_service_offerings = cls.testdata[TestData.serviceOffering]

        cls.template_name = storpool_primary_storage.get("name")

        storage_pool = list_storage_pools(
            cls.apiclient,
            name=cls.template_name
        )

        service_offerings = list_service_offering(
            cls.apiclient,
            name=cls.template_name
        )

        disk_offerings = list_disk_offering(
            cls.apiclient,
            name="ssd"
        )

        if storage_pool is None:
            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.storage_pool = storage_pool
        cls.debug(pprint.pformat(storage_pool))
        if service_offerings is None:
            service_offerings = ServiceOffering.create(cls.apiclient, storpool_service_offerings)
        else:
            service_offerings = service_offerings[0]
        # The version of CentOS has to be supported
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            account="system"
        )

        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        cls.service_offering = service_offerings
        cls.debug(pprint.pformat(cls.service_offering))

        cls.template = template
        cls.random_data_0 = random_gen(size=100)
        cls.test_dir = "/tmp"
        cls.random_data = "random.data"
        return

    @classmethod
    def getDiskOffering(cls, dataDiskOffering, qos, resValue):
        disk_offerings = list_disk_offering(cls.apiclient, name=dataDiskOffering.get("name"))
        if disk_offerings is None:
            disk_offerings = DiskOffering.create(cls.apiclient, services=dataDiskOffering, custom=True)
            cls.resourceDetails(qos, disk_offerings.id, resValue)
        else:
            disk_offerings = disk_offerings[0]
            cls.resourceDetails(qos, disk_offerings.id, )
        return disk_offerings

    @classmethod
    def tearDownClass(cls):
        super(TestStorPoolTiers, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        super(TestStorPoolTiers, self).tearDown()

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_check_tags_on_deployed_vm_and_datadisk(self):
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_change_offering_on_attached_root_disk(self):
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()

        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="ROOT",
                                   listall=True)
        self.changeOfferingForVolume(root_volume[0].id, self.disk_offerings_tier2_tags.id, root_volume[0].size)
        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="ROOT",
                                   listall=True)
        self.vc_policy_tags(volumes=root_volume, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier2_tags.id, attached=True)
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)

    def test_03_change_offering_on_attached_data_disk(self):
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()

        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="DATADISK",
                                   listall=True)
        self.changeOfferingForVolume(root_volume[0].id, self.disk_offerings_tier2_tags.id, root_volume[0].size)
        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="DATADISK",
                                   listall=True)
        self.vc_policy_tags(volumes=root_volume, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier2_tags.id, attached=True)
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_check_templates_on_deployed_vm_and_datadisk(self):
        virtual_machine_template_tier1 = VirtualMachine.create(
            self.apiclient,
            {"name": "StorPool-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            overridediskofferingid=self.disk_offerings_tier1_template.id,
            diskofferingid=self.disk_offerings_tier1_template.id,
            size=2,
            hypervisor=self.hypervisor,
            rootdisksize=10
        )
        volumes = list_volumes(self.apiclient, virtualmachineid=virtual_machine_template_tier1.id, listall=True)
        for v in volumes:
            self.check_storpool_template(v, self.disk_offerings_tier1_template.id, self.spTemplate)
        virtual_machine_template_tier1.stop(self.apiclient, forced=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_check_templates_on_deployed_vm_and_datadisk_tier2(self):
        virtual_machine_template_tier2 = VirtualMachine.create(
            self.apiclient,
            {"name": "StorPool-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            overridediskofferingid=self.disk_offerings_tier2_template.id,
            diskofferingid=self.disk_offerings_tier2_template.id,
            size=2,
            hypervisor=self.hypervisor,
            rootdisksize=10
        )
        volumes = list_volumes(self.apiclient, virtualmachineid=virtual_machine_template_tier2.id, listall=True)
        for v in volumes:
            self.check_storpool_template(v, self.disk_offerings_tier2_template.id, self.spTemplate)
        virtual_machine_template_tier2.stop(self.apiclient, forced=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_change_offerings_with_tags_detached_volume(self):
        disk_off_id = self.disk_offerings_tier2_tags.id
        virtual_machine_tier2_tag = VirtualMachine.create(
            self.apiclient,
            {"name": "StorPool-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            overridediskofferingid=disk_off_id,
            diskofferingid=disk_off_id,
            size=2,
            hypervisor=self.hypervisor,
            rootdisksize=10
        )
        virtual_machine_tier2_tag.stop(self.apiclient, forced=True)
        volumes = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier2_tag.id, type="DATADISK",
                               listall=True)

        virtual_machine_tier2_tag.detach_volume(
            self.apiclient,
            volumes[0]
        )

        self.vc_policy_tags(volumes=volumes, vm=virtual_machine_tier2_tag, qos_or_template=self.qos,
                            disk_offering_id=disk_off_id, attached=True)

        self.changeOfferingForVolume(volumes[0].id, self.disk_offerings_tier1_tags.id, volumes[0].size)
        self.vc_policy_tags(volumes=volumes, vm=virtual_machine_tier2_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier1_tags.id, attached=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_07_change_offerings_with_template_detached_volume(self):
        disk_off_id = self.disk_offerings_tier2_template.id
        virtual_machine_tier2_template = VirtualMachine.create(
            self.apiclient,
            {"name": "StorPool-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            overridediskofferingid=disk_off_id,
            diskofferingid=disk_off_id,
            size=2,
            hypervisor=self.hypervisor,
            rootdisksize=10
        )
        virtual_machine_tier2_template.stop(self.apiclient, forced=True)
        volumes = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier2_template.id, type="DATADISK",
                               listall=True)

        virtual_machine_tier2_template.detach_volume(
            self.apiclient,
            volumes[0]
        )

        self.check_storpool_template(volume=volumes[0], disk_offering_id=disk_off_id, qos_or_template=self.spTemplate)

        self.changeOfferingForVolume(volumes[0].id, self.disk_offerings_tier1_template.id, volumes[0].size)
        self.check_storpool_template(volume=volumes[0], disk_offering_id=self.disk_offerings_tier1_template.id,
                                     qos_or_template=self.spTemplate)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_deploy_vm_with_tags_and_template_in_offerings(self):
        """
            Deploy virtual machine with disk offering on which resource details is set tier2 template and tier2 qos tags
        """
        disk_off_id = self.disk_offerings_tier2_tags_template.id
        virtual_machine_tier2_template = VirtualMachine.create(
            self.apiclient,
            {"name": "StorPool-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            overridediskofferingid=disk_off_id,
            diskofferingid=disk_off_id,
            size=2,
            hypervisor=self.hypervisor,
            rootdisksize=10
        )
        virtual_machine_tier2_template.stop(self.apiclient, forced=True)
        volumes = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier2_template.id, type="DATADISK",
                               listall=True)

        virtual_machine_tier2_template.detach_volume(
            self.apiclient,
            volumes[0]
        )

        self.check_storpool_template(volume=volumes[0], disk_offering_id=disk_off_id, qos_or_template=self.spTemplate,
                                     diff_template=True)
        self.vc_policy_tags(volumes=volumes, vm=virtual_machine_tier2_template, qos_or_template=self.qos,
                            disk_offering_id=disk_off_id, attached=True)

        self.changeOfferingForVolume(volumes[0].id, self.disk_offerings_tier1_tags.id, volumes[0].size)
        self.vc_policy_tags(volumes=volumes, vm=virtual_machine_tier2_template, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier1_tags.id, attached=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_09_resize_root_volume(self):
        '''
        Resize Root volume with changeOfferingForVolume
        '''
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()

        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="ROOT",
                                   listall=True)
        self.changeOfferingForVolume(root_volume[0].id, self.disk_offerings_tier2_tags.id, (root_volume[0].size + 1024))
        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="ROOT",
                                   listall=True)
        self.vc_policy_tags(volumes=root_volume, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier2_tags.id, attached=True)
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_10_shrink_root_volume(self):
        '''
        Shrink Root volume with changeOfferingForVolume
        '''
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()

        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="ROOT",
                                   listall=True)
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)
        self.changeOfferingForVolume(root_volume[0].id, self.disk_offerings_tier2_tags.id, (root_volume[0].size - 1024),
                                     True)
        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="ROOT",
                                   listall=True)
        self.vc_policy_tags(volumes=root_volume, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier2_tags.id, attached=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_11_resize_data_volume(self):
        '''
        Resize DATADISK volume with changeOfferingForVolume
        '''
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()

        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="DATADISK",
                                   listall=True)
        self.changeOfferingForVolume(root_volume[0].id, self.disk_offerings_tier2_tags.id, (root_volume[0].size + 1024))
        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="DATADISK",
                                   listall=True)
        self.vc_policy_tags(volumes=root_volume, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier2_tags.id, attached=True)
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_12_shrink_data_volume(self):
        '''
        Shrink DATADISK volume with changeOfferingForVolume
        '''
        virtual_machine_tier1_tag = self.deploy_vm_and_check_tier_tag()

        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="DATADISK",
                                   listall=True)
        self.changeOfferingForVolume(root_volume[0].id, self.disk_offerings_tier2_tags.id, (root_volume[0].size - 1024),
                                     True)
        root_volume = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, type="DATADISK",
                                   listall=True)
        self.vc_policy_tags(volumes=root_volume, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier2_tags.id, attached=True)
        virtual_machine_tier1_tag.stop(self.apiclient, forced=True)

    def deploy_vm_and_check_tier_tag(self):
        virtual_machine_tier1_tag = VirtualMachine.create(
            self.apiclient,
            {"name": "StorPool-%s" % uuid.uuid4()},
            zoneid=self.zone.id,
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            overridediskofferingid=self.disk_offerings_tier1_tags.id,
            diskofferingid=self.disk_offerings_tier1_tags.id,
            size=2,
            hypervisor=self.hypervisor,
            rootdisksize=10
        )
        volumes = list_volumes(self.apiclient, virtualmachineid=virtual_machine_tier1_tag.id, listall=True)
        self.vc_policy_tags(volumes=volumes, vm=virtual_machine_tier1_tag, qos_or_template=self.qos,
                            disk_offering_id=self.disk_offerings_tier1_tags.id, attached=True)
        return virtual_machine_tier1_tag

    @classmethod
    def resourceDetails(cls, qos, id, resValue=None):
        listResourceDetailCmd = listResourceDetails.listResourceDetailsCmd()
        listResourceDetailCmd.resourceid = id
        listResourceDetailCmd.resourcetype = "DiskOffering"
        listResourceDetailCmd.key = qos
        details = cls.apiclient.listResourceDetails(listResourceDetailCmd)

        if details is None:
            resource = addResourceDetail.addResourceDetailCmd()
            resource.resourceid = id
            resource.resourcetype = "DiskOffering"
            resDet = {'key': qos, 'value': resValue}
            resource.details = [resDet]

            resource.fordisplay = True
            details = cls.apiclient.addResourceDetail(resource)

    @classmethod
    def getZone(cls):
        zones = list_zones(cls.apiclient)
        for z in zones:
            if z.name == cls.getClsConfig().mgtSvr[0].zone:
                cls.zone = z
        assert cls.zone is not None

    def vc_policy_tags(self, volumes, vm, qos_or_template, disk_offering_id, should_tags_exists=None, vm_tags=None,
                       attached=None):
        vc_policy_tag = False
        cvm_tag = False
        qs_tag = False
        id = vm.id
        for v in volumes:
            name = v.path.split("/")[3]
            volume = self.spapi.volumeList(volumeName="~" + name)
            tags = volume[0].tags
            resource_details_value = ResourceDetails.list(self.apiclient, resourcetype="DiskOffering",
                                                          resourceid=disk_offering_id, key=qos_or_template)
            for t in tags:
                self.debug("TAGS are %s" % t)
                if vm_tags:
                    for vm_tag in vm_tags:
                        if t == vm_tag.key:
                            vc_policy_tag = True
                            self.assertEqual(tags[t], vm_tag.value, "Tags are not equal")
                if t == 'cvm':
                    self.debug("CVM tag %s is not the same as vm UUID %s" % (tags[t], id))
                    self.debug(type(tags[t]))
                    self.debug(len(tags[t]))
                    self.debug(type(id))
                    self.debug(len(id))
                    cvm_tag = True
                    self.assertEqual(tags[t], id, "CVM tag is not the same as vm UUID ")
                if t == 'qc':
                    qs_tag = True
                    self.assertEqual(tags[t], resource_details_value[0].value, "QOS tags should be the same")
        if should_tags_exists:
            self.assertTrue(vc_policy_tag, "There aren't volumes with vm tags")
            self.assertTrue(cvm_tag, "There aren't volumes with vm tags")
        if attached:
            self.assertTrue(qs_tag, "The QOS tag isn't set")
        else:
            self.assertFalse(vc_policy_tag, "The tags should be removed")
            self.assertFalse(cvm_tag, "The tags should be removed")

    def check_storpool_template(self, volume, disk_offering_id, qos_or_template, diff_template=None):
        name = volume.path.split("/")[3]
        sp_volume = self.spapi.volumeList(volumeName="~" + name)
        template = sp_volume[0].templateName
        resource_details_value = ResourceDetails.list(self.apiclient, resourcetype="DiskOffering",
                                                      resourceid=disk_offering_id, key=qos_or_template)
        if diff_template:
            self.assertNotEqual(template, resource_details_value[0].value, "The templates should not be the same")
        else:
            self.assertEqual(template, resource_details_value[0].value)

    def changeOfferingForVolume(self, volume_id, disk_offering_id, size, shrinkok=None):
        size = int(size / 1024 / 1024 / 1024)
        change_offering_for_volume_cmd = changeOfferingForVolume.changeOfferingForVolumeCmd()
        change_offering_for_volume_cmd.id = volume_id
        change_offering_for_volume_cmd.diskofferingid = disk_offering_id
        change_offering_for_volume_cmd.size = size
        change_offering_for_volume_cmd.shrinkok = shrinkok

        return self.apiclient.changeOfferingForVolume(change_offering_for_volume_cmd)
