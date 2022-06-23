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
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from marvin.lib.base import (Account,
                             Cluster,
                             Configurations,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Template,
                             VirtualMachine,
                             VmSnapshot,
                             Volume,
                             SecurityGroup,
                             Role,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_disk_offering,
                               list_hosts,
                               list_snapshots,
                               list_storage_pools,
                               list_volumes,
                               list_virtual_machines,
                               list_configurations,
                               list_service_offering,
                               list_clusters,
                               list_zones)
from marvin.cloudstackAPI import (listOsTypes,
                                  listTemplates,
                                  listHosts,
                                  createTemplate,
                                  createVolume,
                                  getVolumeSnapshotDetails,
                                  resizeVolume,
                                  listZones)
import time
import pprint
import random
import subprocess
from storpool import spapi
from storpool import sptypes
from marvin.configGenerator import configuration
import uuid
from sp_util import (TestData, StorPoolHelper)

class TestStoragePool(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestStoragePool, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            cls.cleanUpCloudStack()
            raise

    @classmethod
    def setUpCloudStack(cls):
        testClient = super(TestStoragePool, cls).getClsTestClient()

        cls._cleanup = []

        cls.apiclient = testClient.getApiClient()
        cls.helper = StorPoolHelper()

        cls.unsupportedHypervisor = False
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ("hyperv", "lxc"):
            cls.unsupportedHypervisor = True
            return

        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = None
        zones = list_zones(cls.apiclient)

        for z in zones:
            if z.name == cls.getClsConfig().mgtSvr[0].zone:
                cls.zone = z

        assert cls.zone is not None

        cls.sp_template_1 = "ssd"
        storpool_primary_storage = {
            "name" : cls.sp_template_1,
            "zoneid": cls.zone.id,
            "url": "SP_API_HTTP=10.2.23.248:81;SP_AUTH_TOKEN=6549874687;SP_TEMPLATE=%s" % cls.sp_template_1,
            "scope": "zone",
            "capacitybytes": 564325555333,
            "capacityiops": 155466,
            "hypervisor": "kvm",
            "provider": "StorPool",
            "tags": cls.sp_template_1
            }

        cls.storpool_primary_storage = storpool_primary_storage
        host, port, auth = cls.getCfgFromUrl(url = storpool_primary_storage["url"])
        cls.spapi = spapi.Api(host=host, port=port, auth=auth, multiCluster=True)

        storage_pool = list_storage_pools(
            cls.apiclient,
            name=storpool_primary_storage["name"]
            )

        if storage_pool is None:
            newTemplate = sptypes.VolumeTemplateCreateDesc(name = storpool_primary_storage["name"],placeAll = "virtual", placeTail = "virtual", placeHead = "virtual", replication=1)
            template_on_local = cls.spapi.volumeTemplateCreate(newTemplate)

            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.primary_storage = storage_pool


        storpool_service_offerings_ssd = {
            "name": cls.sp_template_1,
                "displaytext": "SP_CO_2 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
                "hypervisorsnapshotreserve": 200,
                "tags": cls.sp_template_1
            }

        service_offerings_ssd = list_service_offering(
            cls.apiclient,
            name=storpool_service_offerings_ssd["name"]
            )

        if service_offerings_ssd is None:
            service_offerings_ssd = ServiceOffering.create(cls.apiclient, storpool_service_offerings_ssd)
        else:
            service_offerings_ssd = service_offerings_ssd[0]

        cls.service_offering = service_offerings_ssd
        cls.debug(pprint.pformat(cls.service_offering))


        cls.sp_template_2 = "ssd2"

        storpool_primary_storage2 = {
            "name" : cls.sp_template_2,
            "zoneid": cls.zone.id,
            "url": "SP_API_HTTP=10.2.23.248:81;SP_AUTH_TOKEN=6549874687;SP_TEMPLATE=%s" % cls.sp_template_2,
            "scope": "zone",
            "capacitybytes": 564325555333,
            "capacityiops": 1554,
            "hypervisor": "kvm",
            "provider": "StorPool",
            "tags": cls.sp_template_2
            }

        cls.storpool_primary_storage2 = storpool_primary_storage2
        storage_pool = list_storage_pools(
            cls.apiclient,
            name=storpool_primary_storage2["name"]
            )

        if storage_pool is None:
            newTemplate = sptypes.VolumeTemplateCreateDesc(name = storpool_primary_storage2["name"],placeAll = "virtual", placeTail = "virtual", placeHead = "virtual", replication=1)

            template_on_local = cls.spapi.volumeTemplateCreate(newTemplate)

            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage2)

        else:
            storage_pool = storage_pool[0]
        cls.primary_storage2 = storage_pool

        storpool_service_offerings_ssd2 = {
            "name": cls.sp_template_2,
                "displaytext": "SP_CO_2",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
                "tags": cls.sp_template_2
            }

        service_offerings_ssd2 = list_service_offering(
            cls.apiclient,
            name=storpool_service_offerings_ssd2["name"]
            )

        if service_offerings_ssd2 is None:
            service_offerings_ssd2 = ServiceOffering.create(cls.apiclient, storpool_service_offerings_ssd2)
        else:
            service_offerings_ssd2 = service_offerings_ssd2[0]

        cls.service_offering2 = service_offerings_ssd2

        disk_offerings = list_disk_offering(
            cls.apiclient,
            name="Small"
            )

        disk_offering_20 = list_disk_offering(
            cls.apiclient,
            name="Medium"
            )

        disk_offering_100 = list_disk_offering(
            cls.apiclient,
            name="Large"
            )


        cls.disk_offerings = disk_offerings[0]
        cls.disk_offering_20 = disk_offering_20[0]
        cls.disk_offering_100 = disk_offering_100[0]

        #The version of CentOS has to be supported
        template = get_template(
             cls.apiclient,
            cls.zone.id,
            account = "system"
        )

        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskofferingid"] = cls.disk_offerings.id

        role = Role.list(cls.apiclient, name='Admin')

        # Create VMs, VMs etc
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id,
                            roleid = role[0].id
                            )

        securitygroup = SecurityGroup.list(cls.apiclient, account = cls.account.name, domainid= cls.account.domainid)[0]
        cls.helper.set_securityGroups(cls.apiclient, account = cls.account.name, domainid= cls.account.domainid, id = securitygroup.id)
        cls._cleanup.append(cls.account)

        cls.volume_1 = Volume.create(
            cls.apiclient,
            {"diskname":"StorPoolDisk-1" },
            zoneid=cls.zone.id,
            diskofferingid=disk_offerings[0].id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )

        cls.volume_2 = Volume.create(
            cls.apiclient,
            {"diskname":"StorPoolDisk-2" },
            zoneid=cls.zone.id,
            diskofferingid=disk_offerings[0].id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )

        cls.volume = Volume.create(
            cls.apiclient,
            {"diskname":"StorPoolDisk-3" },
            zoneid=cls.zone.id,
            diskofferingid=disk_offerings[0].id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.virtual_machine2 = VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.vm_migrate = VirtualMachine.create(
            cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            templateid=template.id,
            serviceofferingid=cls.service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
        )

        cls.template = template
        cls.hostid = cls.virtual_machine.hostid
        cls.random_data_0 = random_gen(size=100)
        cls.test_dir = "/tmp"
        cls.random_data = "random.data"
        return

    @classmethod
    def tearDownClass(cls):
        cls.cleanUpCloudStack()

    @classmethod
    def cleanUpCloudStack(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        if self.unsupportedHypervisor:
            self.skipTest("Skipping test because unsupported hypervisor\
                    %s" % self.hypervisor)
        return

    def tearDown(self):
        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_snapshot_to_template(self):
        ''' Create template from snapshot without bypass secondary storage
        '''
        volume = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            type = "ROOT",
            listall = True,
            )

        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "false")
        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            snapshotid = snapshot.id
            )
        virtual_machine = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )
        ssh_client = virtual_machine.get_ssh_client()

        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")
        self._cleanup.append(template)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_snapshot_to_template_bypass_secondary(self):
        ''' Test Create Template from snapshot bypassing secondary storage
        '''
        ##cls.virtual_machine
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )
        try:
            name = volume[0].path.split("/")[3]
            sp_volume = self.spapi.volumeList(volumeName = "~" + name)
        except spapi.ApiError as err:
            raise Exception(err)


        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")

        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snasphot in snapshot_details")
        except spapi.ApiError as err:
               raise Exception(err)

        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            snapshotid = snapshot.id
            )

        flag = False
        sp_snapshots = self.spapi.snapshotsList()
        for snap in sp_snapshots:
            tags = snap.tags
            for t in tags:
                if tags[t] == template.id:
                    flag = True
                    break
            else:
                continue
            break

        if flag is False:
            raise Exception("Template does not exists in Storpool")
        virtual_machine = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )
        ssh_client = virtual_machine.get_ssh_client()
        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")
        self._cleanup.append(template)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_snapshot_volume_with_secondary(self):
        '''
            Test Create snapshot and backup to secondary
        '''
        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "false")
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )
        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot_details")
        except spapi.ApiError as err:
            raise Exception(err)
        self.assertIsNotNone(snapshot, "Could not create snapshot")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_snapshot_volume_bypass_secondary(self):
        '''
            Test snapshot bypassing secondary
        '''
        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )
        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot details")
        except spapi.ApiError as err:
            raise Exception(err)
        self.assertIsNotNone(snapshot, "Could not create snapshot")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_delete_template_bypassed_secondary(self):
        ''' Test delete template from snapshot bypassed secondary storage
        '''
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )
        try:
            name = volume[0].path.split("/")[3]
            sp_volume = self.spapi.volumeList(volumeName = "~" + name)
        except spapi.ApiError as err:
            raise Exception(err)

        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")

        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot details")
        except spapi.ApiError as err:
            raise Exception(err)

        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            snapshotid = snapshot.id
            )

        flag = False
        storpoolGlId = None
        sp_snapshots = self.spapi.snapshotsList()
        for snap in sp_snapshots:
            tags = snap.tags
            for t in tags:
                if tags[t] == template.id:
                    storpoolGlId = "~" + snap.globalId
                    flag = True
                    break
            else:
                continue
            break

        if flag is False:
            raise Exception("Template does not exists in Storpool")

        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")
        temp = Template.delete(template, self.apiclient, self.zone.id)
        self.assertIsNone(temp, "Template was not deleted")

        try:
            sp_snapshot = self.spapi.snapshotList(snapshotName = storpoolGlId)
            if sp_snapshot is not None:
                self.debug("Snapshot exists on StorPool name " + storpoolGlId)
        except spapi.ApiError as err:
                self.debug("Do nothing the template has to be deleted")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_template_from_snapshot(self):
        ''' Test create template bypassing secondary from snapshot which is backed up on secondary storage
        '''
        ##cls.virtual_machine
        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )
        try:
            name = volume[0].path.split("/")[3]
            sp_volume = self.spapi.volumeList(volumeName = "~" + name)
        except spapi.ApiError as err:
            raise Exception(err)

        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "false")

        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapsho details")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            snapshotid = snapshot.id
            )

        flag = False
        globalId = None
        sp_snapshots = self.spapi.snapshotsList()
        for snap in sp_snapshots:
            tags = snap.tags
            for t in tags:
                if tags[t] == template.id:
                    flag = True
                    globalId = snap.globalId
                    break
            else:
                continue
            break

        if flag is False:
            raise Exception("Template does not exists in Storpool")


        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")
        temp = Template.delete(template, self.apiclient, self.zone.id)
        self.assertIsNone(temp, "Template was not deleted")

        if globalId is not None:
            try:
                sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + globalId)
                if sp_snapshot is not None:
                    self.debug("Snapshot exists on Storpool name " + globalId)
            except spapi.ApiError as err:
                self.debug("Do nothing the template has to be deleted")
        else:
            flag = False
            sp_snapshots = self.spapi.snapshotsList()
            for snap in sp_snapshots:
                tags = snap.tags
                for t in tags:
                    if tags[t] == template.id:
                        flag = True
                        break
                else:
                    continue
                break

            if flag is True:
                raise Exception("Template should not exists in Storpool")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_07_delete_snapshot_of_deleted_volume(self):
        ''' Delete snapshot and template if volume is already deleted, not bypassing secondary
        '''

        backup_config = Configurations.update(self.apiclient,
        name = "sp.bypass.secondary.storage",
        value = "false")

        volume = Volume.create(
            self.apiclient,
            {"diskname":"StorPoolDisk-Delete" },
            zoneid = self.zone.id,
            diskofferingid = self.disk_offerings.id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        delete = volume
        self.virtual_machine2.stop(self.apiclient, forced=True)
        self.virtual_machine2.attach_volume(
            self.apiclient,
            volume
            )
        self.virtual_machine2.detach_volume(
            self.apiclient,
            volume
            )

        volume = list_volumes(self.apiclient, id = volume.id)

        name = volume[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
        except spapi.ApiError as err:
           raise Exception(err)

        snapshot = Snapshot.create(
            self.apiclient,
             volume_id = volume[0].id,
             account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    try:
                        sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                        flag = True
                    except spapi.ApiError as err:
                       raise Exception(err)
            if flag == False:
                raise Exception("Could not finad snapshot in snapshot details")
        except Exception as err:
            raise Exception(err)

        template = self.create_template_from_snapshot(self.apiclient, self.services, snapshotid = snapshot.id)

        template_from_volume = self.create_template_from_snapshot(self.apiclient, self.services, volumeid = volume[0].id)

        Volume.delete(delete, self.apiclient, )
        Snapshot.delete(snapshot, self.apiclient)

        flag = False

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            if snapshot_details is not None:
                try:
                    for s in snapshot_details:
                        if s["snapshotDetailsName"] == snapshot.id:
                            name = s["snapshotDetailsValue"].split("/")[3]
                            sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                            flag = True
                except spapi.ApiError as err:
                    flag = False

            if flag is True:
                raise Exception("Snapshot was not deleted")
        except Exception as err:
            self.debug('Snapshot was deleted %s' % err)

        Template.delete(template, self.apiclient, zoneid = self.zone.id)
        Template.delete(template_from_volume, self.apiclient, zoneid = self.zone.id)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_delete_snapshot_of_deleted_volume(self):
        ''' Delete snapshot and template if volume is already deleted, bypassing secondary
        '''

        backup_config = Configurations.update(self.apiclient,
        name = "sp.bypass.secondary.storage",
        value = "true")

        volume = Volume.create(
            self.apiclient,
            {"diskname":"StorPoolDisk-Delete" },
            zoneid = self.zone.id,
            diskofferingid = self.disk_offerings.id,
            account=self.account.name,
            domainid=self.account.domainid,
            )
        delete = volume
        self.virtual_machine2.attach_volume(
            self.apiclient,
            volume
            )
        self.virtual_machine2.detach_volume(
            self.apiclient,
            volume
            )

        volume = list_volumes(self.apiclient, id = volume.id)

        name = volume[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
        except spapi.ApiError as err:
           raise Exception(err)

        snapshot = Snapshot.create(
            self.apiclient,
             volume_id = volume[0].id,
             account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            if snapshot_details is not None:
                flag = False
                for s in snapshot_details:
                    if s["snapshotDetailsName"] == snapshot.id:
                        name = s["snapshotDetailsValue"].split("/")[3]
                        try:
                            sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                            flag = True
                        except spapi.ApiError as err:
                           raise Exception(err)
                if flag == False:
                    raise Exception("Could not find snapshot in snapshot details")
        except Exception as err:
            raise Exception(err)

        template = self.create_template_from_snapshot(self.apiclient, self.services, snapshotid = snapshot.id)

        Volume.delete(delete, self.apiclient, )
        Snapshot.delete(snapshot, self.apiclient)

        flag = False
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            if snapshot_details is not None:
                try:
                    for s in snapshot_details:
                        if s["snapshotDetailsName"] == snapshot.id:
                            name = s["snapshotDetailsValue"].split("/")[3]
                            sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                            flag = True
                except spapi.ApiError as err:
                    flag = False

            if flag is True:
                raise Exception("Snapshot was not deleted")
        except Exception as err:
            self.debug('Snapshot was deleted %s' % err)


        Template.delete(template, self.apiclient, zoneid = self.zone.id)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_09_vm_from_bypassed_template(self):
        '''Create virtual machine with sp.bypass.secondary.storage=false
        from template created on StorPool and Secondary Storage'''

        volume = list_volumes(
                        self.apiclient,
                        virtualmachineid = self.virtual_machine.id,
                        type = "ROOT",
                        listall = True,
                        )

        name = volume[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
        except spapi.ApiError as err:
           raise Exception(err)

        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "true")

        snapshot = Snapshot.create(
           self.apiclient,
            volume_id = volume[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    try:
                        sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                        flag = True
                    except spapi.ApiError as err:
                       raise Exception(err)
            if flag == False:
                raise Exception("Could not find snapshot in snapshot details")
        except Exception as err:
            raise Exception(err)

        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            snapshotid = snapshot.id
            )
        self._cleanup.append(template)

        flag = False
        sp_snapshots = self.spapi.snapshotsList()
        for snap in sp_snapshots:
            tags = snap.tags
            for t in tags:
                if tags[t] == template.id:
                    flag = True
                    break
            else:
                continue
            break

        if flag is False:
            raise Exception("Template does not exists in Storpool")


        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")

        backup_config = Configurations.update(self.apiclient,
            name = "sp.bypass.secondary.storage",
            value = "false")

        vm = VirtualMachine.create(
            self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid = template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10,
            )

        ssh_client = vm.get_ssh_client(reconnect=True)


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_10_create_vm_snapshots(self):
        """Test to create VM snapshots
        """
        volume_attached = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume
            )

        vol = list_volumes(self.apiclient, virtualmachineid=self.virtual_machine.id, id=volume_attached.id)
        name = vol[0].path.split("/")[3]
        sp_volume = self.spapi.volumeList(volumeName = "~" + name)
        self.assertEqual(volume_attached.id, self.volume.id, "Is not the same volume ")
        try:
            # Login to VM and write data to file system
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "echo %s > %s/%s" %
                (self.random_data_0, self.test_dir, self.random_data),
                "sync",
                "sleep 1",
                "sync",
                "sleep 1",
                "cat %s/%s" %
                (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)


        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)
        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data has be write into temp file!"
        )

        time.sleep(30)
        MemorySnapshot = False
        vm_snapshot = VmSnapshot.create(
            self.apiclient,
            self.virtual_machine.id,
            MemorySnapshot,
            "TestSnapshot",
            "Display Text"
        )
        self.assertEqual(
            vm_snapshot.state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_11_revert_vm_snapshots(self):
        """Test to revert VM snapshots
        """

        try:
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "rm -rf %s/%s" % (self.test_dir, self.random_data),
                "ls %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)

        if str(result[0]).index("No such file or directory") == -1:
            self.fail("Check the random data has be delete from temp file!")

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )

        self.assertEqual(
            list_snapshot_response[0].state,
            "Ready",
            "Check the snapshot of vm is ready!"
        )

        self.virtual_machine.stop(self.apiclient, forced=True)

        VmSnapshot.revertToSnapshot(
            self.apiclient,
            list_snapshot_response[0].id
            )

        self.virtual_machine.start(self.apiclient)

        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.virtual_machine.ipaddress)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_12_delete_vm_snapshots(self):
        """Test to delete vm snapshots
        """

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_snapshot_response,
            None,
            "Check if snapshot exists in ListSnapshot"
        )
        VmSnapshot.deleteVMSnapshot(
            self.apiclient,
            list_snapshot_response[0].id)

        time.sleep(30)

        list_snapshot_response = VmSnapshot.list(
            self.apiclient,
            #vmid=self.virtual_machine.id,
            virtualmachineid=self.virtual_machine.id,
            listall=False)
        self.debug('list_snapshot_response -------------------- %s' % list_snapshot_response)

        self.assertIsNone(list_snapshot_response, "snapshot is already deleted")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_13_detach_volume(self):
        '''Attach volume on VM on 2nd zone'''
        self.virtual_machine.stop(self.apiclient)
        self.virtual_machine.detach_volume(
            self.apiclient,
            self.volume
            )
        vol = list_volumes(self.apiclient, id=self.volume.id)
        name = vol[0].path.split("/")[3]
        spvolume = self.spapi.volumeList(volumeName = "~" + name)
        self.assertEqual(vol[0].id, self.volume.id, "Is not the same volume ")
        tags = spvolume[0].tags
        for t in tags:
            self.assertFalse(t.lower() == 'cvm'.lower(), "cvm tag still set on detached volume")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_14_attach_detach_volume_to_running_vm(self):
        ''' Test Attach Volume To Running Virtual Machine
        '''
        time.sleep(60)
        self.assertEqual(VirtualMachine.RUNNING, self.virtual_machine.state, "Running")
        volume = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume_1
            )
        print(volume)
        self.assertIsNotNone(volume, "Volume is not None")

        list_vm_volumes = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            id= volume.id
            )
        print(list_vm_volumes)
        self.assertEqual(volume.id, list_vm_volumes[0].id, "Is true")

        name = list_vm_volumes[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
        except spapi.ApiError as err:
           raise Exception(err)

        volume = self.virtual_machine.detach_volume(
            self.apiclient,
            self.volume_1
            )
        list_vm_volumes = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            id = volume.id
            )

        print(list_vm_volumes)
        self.assertIsNone(list_vm_volumes, "Is None")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_15_resize_root_volume_on_working_vm(self):
        ''' Test Resize Root volume on Running Virtual Machine
        '''
        self.assertEqual(VirtualMachine.RUNNING, self.virtual_machine2.state, "Running")
        volume = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine2.id,
            type = "ROOT",
            listall = True,
            )
        volume = volume[0]

        name = volume.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertEqual(volume.type, 'ROOT', "Volume is not of ROOT type")
        shrinkOk = False
        if volume.size > int((self.disk_offering_20.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volume.id
        cmd.size = 20
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)

        new_size = Volume.list(
            self.apiclient,
            id=volume.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_20.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )
        volume = new_size[0]

        name = volume.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if volume.size > int((self.disk_offering_100.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volume.id
        cmd.size = 100
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=volume.id
            )

        volume = new_size[0]

        name = volume.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_100.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_16_resize_attached_volume_on_working_vm(self):
        ''' Test Resize Volume  Attached To Running Virtual Machine
        '''
        self.assertEqual(VirtualMachine.RUNNING, self.virtual_machine.state, "Running")
        volume = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume_1
            )

        listvol = Volume.list(
            self.apiclient,
            id=volume.id
            )
        name = listvol[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != listvol[0].size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if volume.size > int((self.disk_offering_20.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volume.id
        cmd.diskofferingid = self.disk_offering_20.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)

        new_size = Volume.list(
            self.apiclient,
            id=volume.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_20.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )
        volume = new_size[0]

        name = volume.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if volume.size > int((self.disk_offering_100.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volume.id
        cmd.diskofferingid = self.disk_offering_100.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=volume.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_100.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )

        # return to small disk
        volume = new_size[0]

        name = volume.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if volume.size > int((self.disk_offerings.disksize)* (1024**3)):
            shrinkOk= True

        cmd.diskofferingid = self.disk_offerings.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=volume.id
            )

        volume = new_size[0]

        name = volume.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertTrue(
            (new_size[0].size == int((self.disk_offerings.disksize)*(1024**3))),
            "Could not return to Small disk"
            )


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_17_attach_detach_volume_to_stopped_vm(self):
        ''' Test Attach Volume To Stopped Virtual Machine
        '''
        virtual_machine = self.virtual_machine.stop(
            self.apiclient,
            forced=True
            )

        time.sleep(60)
        volume_2 = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume_2
            )
        list_vm_volumes = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            id= volume_2.id
            )

        name = list_vm_volumes[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
        except spapi.ApiError as err:
           raise Exception(err)

        print(list_vm_volumes)
        self.assertEqual(volume_2.id,list_vm_volumes[0].id, "Is true")

        time.sleep(90)
        volume_2 = self.virtual_machine.detach_volume(
            self.apiclient,
            self.volume_2
            )
        list_vm_volumes = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            id = volume_2.id
            )
        print(list_vm_volumes)
        self.assertIsNone(list_vm_volumes, "Is None")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_18_resize_attached_volume(self):
        ''' Test Resize Volume  Attached To Virtual Machine
        '''

        shrinkOk = False
        if self.volume_1.size > int((self.disk_offering_20.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = self.volume_1.id
        cmd.diskofferingid = self.disk_offering_20.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)

        new_size = Volume.list(
            self.apiclient,
            id=self.volume_1.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_20.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )
        self.volume_1 = new_size[0]

        name = self.volume_1.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != self.volume_1.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if self.volume_1.size > int((self.disk_offering_100.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = self.volume_1.id
        cmd.diskofferingid = self.disk_offering_100.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=self.volume_1.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_100.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )

        # return to small disk
        self.volume_1 = new_size[0]

        name = self.volume_1.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != self.volume_1.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if self.volume_1.size > int((self.disk_offerings.disksize)* (1024**3)):
            shrinkOk= True

        cmd.diskofferingid = self.disk_offerings.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=self.volume_1.id
            )

        name = new_size[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != new_size[0].size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)
        self.assertTrue(
            (new_size[0].size == int((self.disk_offerings.disksize)*(1024**3))),
            "Could not return to Small disk"
            )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_19_resize_detached_volume(self):
        ''' Test Resize Volume Detached To Virtual Machine
        '''
        list_vm_volumes = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            id= self.volume_2.id
            )

        #check that the volume is not attached to VM
        self.assertIsNone(list_vm_volumes, "List volumes is not None")

        shrinkOk = False
        if self.volume_2.size > int((self.disk_offering_20.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = self.volume_2.id
        cmd.diskofferingid = self.disk_offering_20.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)

        new_size = Volume.list(
            self.apiclient,
            id=self.volume_2.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_20.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )
        self.volume_2 = new_size[0]

        name = self.volume_2.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != self.volume_2.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if self.volume_2.size > int((self.disk_offering_100.disksize) * (1024**3)):
            shrinkOk= True

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = self.volume_2.id
        cmd.diskofferingid = self.disk_offering_100.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=self.volume_2.id
            )

        self.assertTrue(
            (new_size[0].size == int((self.disk_offering_100.disksize) * (1024**3))),
            "New size is not int((self.disk_offering_20) * (1024**3)"
            )

        # return to small disk
        self.volume_2 = new_size[0]

        name = self.volume_2.path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != self.volume_2.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        shrinkOk = False
        if self.volume_2.size > int((self.disk_offerings.disksize)* (1024**3)):
            shrinkOk= True

        cmd.diskofferingid = self.disk_offerings.id
        cmd.shrinkok = shrinkOk

        self.apiclient.resizeVolume(cmd)
        new_size = Volume.list(
            self.apiclient,
            id=self.volume_2.id
            )

        name = new_size[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
            if spvolume[0].size != new_size[0].size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertTrue(
            (new_size[0].size == int((self.disk_offerings.disksize)*(1024**3))),
            "Could not return to Small disk"
            )

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_20_snapshot_to_volume(self):
        ''' Create volume from snapshot
        '''
        snapshot = Snapshot.create(
            self.apiclient,
            volume_id = self.volume_2.id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot details")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        volume = self.create_volume(
            self.apiclient,
            zoneid = self.zone.id,
            snapshotid = snapshot.id,
            account=self.account.name,
            domainid=self.account.domainid
            )

        listvol = Volume.list(
            self.apiclient,
            id=volume.id
            )
        name = listvol[0].path.split("/")[3]
        try:
            spvolume = self.spapi.volumeList(volumeName="~" + name)
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertIsNotNone(volume, "Could not create volume from snapshot")
        self.assertIsInstance(volume, Volume, "Volume is not instance of Volume")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_21_snapshot_detached_volume(self):
        ''' Test Snapshot Detached Volume
        '''
        self.virtual_machine.stop(
            self.apiclient,
            forced = True
            )
        self.volume = self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume
            )
        self.assertIsNotNone(self.volume, "Attach: Is none")
        self.volume = self.virtual_machine.detach_volume(
            self.apiclient,
            self.volume
            )

        self.assertIsNotNone(self.volume, "Detach: Is none")

        snapshot = Snapshot.create(
            self.apiclient,
            self.volume.id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot details")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertIsNotNone(snapshot, "Snapshot is None")

        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not Instance of Snappshot")

        snapshot = Snapshot.delete(
            snapshot,
            self.apiclient
            )

        self.assertIsNone(snapshot, "Snapshot was not deleted")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_22_snapshot_root_disk(self):
        ''' Test ROOT Disk Snapshot
        '''
        vm = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid = self.zone.id,
            templateid = self.template.id,
            serviceofferingid = self.service_offering.id,
            hypervisor = self.hypervisor,
            rootdisksize = 10
            )
        list_volumes_of_vm = list_volumes(
            self.apiclient,
            virtualmachineid = vm.id,
            listall = True,
            )
        self.assertIs(len(list_volumes_of_vm), 1, "VM has more disk than 1")

        snapshot = Snapshot.create(
            self.apiclient,
            list_volumes_of_vm[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = self.apiclient.getVolumeSnapshotDetails(cmd)
            flag = False
            for s in snapshot_details:
                if s["snapshotDetailsName"] == snapshot.id:
                    name = s["snapshotDetailsValue"].split("/")[3]
                    sp_snapshot = self.spapi.snapshotList(snapshotName = "~" + name)
                    flag = True
            if flag == False:
                raise Exception("Could not find snapshot in snapshot details")
        except spapi.ApiError as err:
           raise Exception(err)

        self.assertIsNotNone(snapshot, "Snapshot is None")

        self.assertEqual(list_volumes_of_vm[0].id, snapshot.volumeid, "Snapshot is not for the same volume")


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_23_volume_to_template(self):
        ''' Create Template From ROOT Volume
        '''
        volume = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            type = "ROOT",
            listall = True,
            )

        self.virtual_machine.stop(self.apiclient)

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            volumeid = volume[0].id
            )

        virtual_machine = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )
        ssh_client = virtual_machine.get_ssh_client()
        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")
        self._cleanup.append(template)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_24_migrate_vm_to_another_storage(self):
        ''' Migrate VM to another Primary Storage
        '''
        list_volumes_of_vm = list_volumes(
            self.apiclient,
            virtualmachineid = self.vm_migrate.id,
            listall = True,
            )

        self.assertTrue(len(list_volumes_of_vm) == 1, "There are more volumes attached to VM")

        if list_volumes_of_vm[0].storageid is self.primary_storage.id:
            cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
            cmd.virtualmachineid = self.vm_migrate.id
            if hostid:
                cmd.hostid = hostid
            vm =   apiclient.migrateVirtualMachine(cmd)
            volume = list_volumes(
                self.apiclient,
                virtualmachineid = vm.id
                )[0]
            self.assertNotEqual(volume.storageid, self.primary_storage.id, "Could not migrate VM")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_25_migrate_volume_to_another_storage(self):
        ''' Migrate Volume To Another Primary Storage
        '''
        self.assertFalse(hasattr(self.volume, 'virtualmachineid') , "Volume is not detached")

        self.assertFalse(hasattr(self.volume, 'storageid') , "Volume is not detached")
        volume = Volume.migrate(
            self.apiclient,
            volumeid = self.volume.id,
            storageid = self.primary_storage2.id
            )

        self.assertIsNotNone(volume, "Volume is None")

        self.assertEqual(volume.storageid, self.primary_storage2.id, "Storage is the same")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_26_create_vm_on_another_storpool_storage(self):
        """ Create Virtual Machine on another StorPool primary StoragePool"""
        virtual_machine = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=self.template.id,
            serviceofferingid=self.service_offering2.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )
        self.assertIsNotNone(virtual_machine, "Could not create virtual machine on another Storpool primary storage")


    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_27_snapshot_to_volume_of_root_disk(self):
        ''' Create volume from snapshot
        '''
        virtual_machine = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )
        volume1 = list_volumes(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            type = "ROOT",
            listall = True,
            )
        snapshot = Snapshot.create(
            self.apiclient,
            volume_id = volume1[0].id,
            account=self.account.name,
            domainid=self.account.domainid,
            )

        self.assertIsNotNone(snapshot, "Could not create snapshot")
        self.assertIsInstance(snapshot, Snapshot, "Snapshot is not an instance of Snapshot")

        volume = self.create_volume(
            self.apiclient,
            zoneid = self.zone.id,
            snapshotid = snapshot.id,
            account=self.account.name,
            domainid=self.account.domainid
            )

        self.assertIsNotNone(volume, "Could not create volume from snapshot")
        self.assertIsInstance(volume, Volume, "Volume is not instance of Volume")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_28_download_volume(self):
        vol = self.volume.extract(
            self.apiclient,
            volume_id = self.volume.id,
            zoneid = self.zone.id,
            mode = "HTTP_DOWNLOAD"
            )
        self.assertIsNotNone(vol, "Volume is None")
        self.assertIsNotNone(vol.url, "No URL provided")
        Volume.delete(vol, self.apiclient)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_29_create_vm_from_template_not_on_storpool(self):
        ''' Create virtual machine from template which for some reason is deleted from StorPool, but exists in template_spoool_ref DB tables '''

        volume = Volume.list(
            self.apiclient,
            virtualmachineid = self.virtual_machine.id,
            type = "ROOT",
            listall = True,
            )

        self.virtual_machine.stop(self.apiclient)

        template = self.create_template_from_snapshot(
            self.apiclient,
            self.services,
            volumeid = volume[0].id
            )

        virtual_machine = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )
        ssh_client = virtual_machine.get_ssh_client(reconnect= True)
        name = 'ssd-' + template.id
        flag = False
        storpoolGlId = None

        sp_snapshots = self.spapi.snapshotsList()
        for snap in sp_snapshots:
            tags = snap.tags
            for t in tags:
                if tags[t] == template.id:
                    storpoolGlId = snap.globalId
                    flag = True
                    break
            else:
                continue
            break

        if flag is False:
            try:
                sp_snapshot = self.spapi.snapshotList(snapshotName = name)
            except spapi.ApiError as err:
                raise Exception(err)


        self.spapi.snapshotDelete(snapshotName ="~" + storpoolGlId)

        virtual_machine2 = VirtualMachine.create(self.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            templateid=template.id,
            serviceofferingid=self.service_offering.id,
            hypervisor=self.hypervisor,
            rootdisksize=10
            )

        ssh_client = virtual_machine2.get_ssh_client(reconnect= True)
        self.assertIsNotNone(template, "Template is None")
        self.assertIsInstance(template, Template, "Template is instance of template")
        self._cleanup.append(template)

    @classmethod
    def create_volume(self, apiclient, zoneid=None, snapshotid=None, account=None, domainid=None):
        """Create Volume"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = "Test"

        if zoneid:
            cmd.zoneid = zoneid

        if snapshotid:
            cmd.snapshotid = snapshotid

        if account:
            cmd.account=account

        if domainid:
            cmd.domainid=domainid
        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def get_local_cluster(cls):
       storpool_clusterid = subprocess.check_output(['storpool_confshow', 'CLUSTER_ID'])
       clusterid = storpool_clusterid.split("=")
       cls.debug(storpool_clusterid)
       clusters = list_clusters(cls.apiclient)
       for c in clusters:
           configuration = list_configurations(
               cls.apiclient,
               clusterid = c.id
               )
           for conf in configuration:
               if conf.name == 'sp.cluster.id'  and (conf.value in clusterid[1]):
                   return c

    @classmethod
    def get_remote_cluster(cls):
       storpool_clusterid = subprocess.check_output(['storpool_confshow', 'CLUSTER_ID'])
       clusterid = storpool_clusterid.split("=")
       cls.debug(storpool_clusterid)
       clusters = list_clusters(cls.apiclient)
       for c in clusters:
           configuration = list_configurations(
               cls.apiclient,
               clusterid = c.id
               )
           for conf in configuration:
               if conf.name == 'sp.cluster.id'  and (conf.value not in clusterid[1]):
                   return c

    @classmethod
    def list_hosts_by_cluster_id(cls, clusterid):
        """List all Hosts matching criteria"""
        cmd = listHosts.listHostsCmd()
        cmd.clusterid = clusterid
        return(cls.apiclient.listHosts(cmd))


    def start(cls, vmid, hostid):
        """Start the instance"""
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = vmid
        cmd.hostid = hostid
        return (cls.apiclient.startVirtualMachine(cmd))


    @classmethod
    def create_template_from_snapshot(self, apiclient, services, snapshotid=None, volumeid=None):
        """Create template from Volume"""
        # Create template from Virtual machine and Volume ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = "StorPool_Template"
        cmd.name = "-".join(["StorPool-", random_gen()])
        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            raise Exception(
                "Unable to find Ostype is required for creating template")

        cmd.isfeatured = True
        cmd.ispublic = True
        cmd.isextractable =  False

        if snapshotid:
            cmd.snapshotid = snapshotid
        if volumeid:
            cmd.volumeid = volumeid
        return Template(apiclient.createTemplate(cmd).__dict__)

    @classmethod
    def getCfgFromUrl(cls, url):
        cfg = dict([
            option.split('=')
            for option in url.split(';')
        ])
        host, port = cfg['SP_API_HTTP'].split(':')
        auth = cfg['SP_AUTH_TOKEN']
        return host, int(port), auth


