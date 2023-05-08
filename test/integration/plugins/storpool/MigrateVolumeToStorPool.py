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
import pprint
import random
import subprocess
import time
import uuid

from marvin.cloudstackAPI import (listOsTypes,
                                  listTemplates,
                                  listHosts,
                                  createTemplate,
                                  createVolume,
                                  resizeVolume,
                                  startVirtualMachine,
                                  migrateVirtualMachine,
                                  migrateVolume
                                  )
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from marvin.configGenerator import configuration, cluster
from marvin.lib.base import (Account,
                             Configurations,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Template,
                             Tag,
                             VirtualMachine,
                             VmSnapshot,
                             Volume,
                             SecurityGroup,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_disk_offering,
                               list_snapshots,
                               list_storage_pools,
                               list_volumes,
                               list_virtual_machines,
                               list_configurations,
                               list_service_offering,
                               list_clusters,
                               list_zones)
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from nose.plugins.attrib import attr

from storpool import spapi
from sp_util import (TestData, StorPoolHelper)


class TestMigrateVolumeToAnotherPool(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        super(TestMigrateVolumeToAnotherPool, cls).setUpClass()
        try:
            cls.setUpCloudStack()
        except Exception:
            cls.cleanUpCloudStack()
            raise

    @classmethod
    def setUpCloudStack(cls):
        cls.spapi = spapi.Api(host="10.2.23.248", port="81", auth="6549874687", multiCluster=True)
        testClient = super(TestMigrateVolumeToAnotherPool, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()

        cls._cleanup = []

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

        td = TestData()
        cls.testdata = td.testdata
        cls.helper = StorPoolHelper()
        storpool_primary_storage = cls.testdata[TestData.primaryStorage]
        cls.template_name = storpool_primary_storage.get("name")
        storpool_service_offerings = cls.testdata[TestData.serviceOffering]

        nfs_service_offerings = cls.testdata[TestData.serviceOfferingsPrimary]
        ceph_service_offerings = cls.testdata[TestData.serviceOfferingsCeph]


        storage_pool = list_storage_pools(
            cls.apiclient,
            name=cls.template_name
            )

        nfs_storage_pool = list_storage_pools(
            cls.apiclient,
            name='nfs'
            )

        ceph_primary_storage = cls.testdata[TestData.primaryStorage4]

        cls.ceph_storage_pool = list_storage_pools(
            cls.apiclient,
            name=ceph_primary_storage.get("name")
            )[0]

        service_offerings = list_service_offering(
            cls.apiclient,
            name=cls.template_name
            )
        nfs_service_offering = list_service_offering(
            cls.apiclient,
            name='nfs'
            )

        ceph_service_offering = list_service_offering(
            cls.apiclient,
            name=ceph_primary_storage.get("name")
            )

        disk_offerings = list_disk_offering(
            cls.apiclient,
            name="ssd"
            )

        cls.disk_offerings = disk_offerings[0]
        if storage_pool is None:
            storage_pool = StoragePool.create(cls.apiclient, storpool_primary_storage)
        else:
            storage_pool = storage_pool[0]
        cls.storage_pool = storage_pool
        if service_offerings is None:
            service_offerings = ServiceOffering.create(cls.apiclient, storpool_service_offerings)
        else:
            service_offerings = service_offerings[0]
        if nfs_service_offering is None:
            nfs_service_offering = ServiceOffering.create(cls.apiclient, nfs_service_offerings)
        else:
            nfs_service_offering = nfs_service_offering[0]

        if ceph_service_offering is None:
            ceph_service_offering = ServiceOffering.create(cls.apiclient, ceph_service_offerings)
        else:
            ceph_service_offering = ceph_service_offering[0]
        #The version of CentOS has to be supported
        template = get_template(
             cls.apiclient,
            cls.zone.id,
            account = "system"
        )

        cls.nfs_storage_pool = nfs_storage_pool[0]
        if cls.nfs_storage_pool.state == "Maintenance":
            cls.nfs_storage_pool = StoragePool.cancelMaintenance(cls.apiclient, cls.nfs_storage_pool.id)

        if cls.ceph_storage_pool.state == "Maintenance":
            cls.ceph_storage_pool = StoragePool.cancelMaintenance(cls.apiclient, cls.ceph_storage_pool.id)

        cls.account = cls.helper.create_account(
                            cls.apiclient,
                            cls.services["account"],
                            accounttype = 1,
                            domainid=cls.domain.id,
                            roleid = 1
                            )
        cls._cleanup.append(cls.account)

        securitygroup = SecurityGroup.list(cls.apiclient, account = cls.account.name, domainid= cls.account.domainid)[0]
        cls.helper.set_securityGroups(cls.apiclient, account = cls.account.name, domainid= cls.account.domainid, id = securitygroup.id)

        cls.clusters = cls.helper.getClustersWithStorPool(cls.apiclient, cls.zone.id,)

        cls.vm = VirtualMachine.create(cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            clusterid=random.choice(cls.clusters),
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=nfs_service_offering.id,
            hypervisor=cls.hypervisor,
            rootdisksize=10
            )
        cls.vm2 = VirtualMachine.create(cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            clusterid=random.choice(cls.clusters),
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=nfs_service_offering.id,
            hypervisor= cls.hypervisor,
            rootdisksize=10
            )
        cls.vm3 = VirtualMachine.create(cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            clusterid=random.choice(cls.clusters),
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=nfs_service_offering.id,
            hypervisor= cls.hypervisor,
            rootdisksize=10
            )
        cls.vm4 = VirtualMachine.create(cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            clusterid=random.choice(cls.clusters),
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=ceph_service_offering.id,
            hypervisor= cls.hypervisor,
            rootdisksize=10
            )
        cls.vm5 = VirtualMachine.create(cls.apiclient,
            {"name":"StorPool-%s" % uuid.uuid4() },
            zoneid=cls.zone.id,
            clusterid=random.choice(cls.clusters),
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=ceph_service_offering.id,
            hypervisor= cls.hypervisor,
            rootdisksize=10
            )
        cls.storage_pool = StoragePool.update(cls.apiclient,
                                              id=cls.storage_pool.id,
                                              tags = ["ssd, nfs"])


        if template == FAILED:
            assert False, "get_template() failed to return template\
                    with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id


        cls.service_offering = service_offerings
        cls.nfs_service_offering = nfs_service_offering

        cls.template = template
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
            if cls.nfs_storage_pool.state is not "Maintenance":
                cls.nfs_storage_pool = StoragePool.enableMaintenance(cls.apiclient, cls.nfs_storage_pool.id)

            if cls.ceph_storage_pool.state is not "Maintenance":
                cls.ceph_storage_pool = StoragePool.enableMaintenance(cls.apiclient, cls.ceph_storage_pool.id)

            cls.storage_pool = StoragePool.update(cls.apiclient,
                                              id=cls.storage_pool.id,
                                              tags = ["ssd"])
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
    def test_1_migrate_vm_from_nfs_to_storpool(self):
        ''' Test migrate virtual machine from NFS primary storage to StorPool'''

        self.vm.stop(self.apiclient, forced=True)
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = self.vm.id
        cmd.storageid = self.storage_pool.id
        migrated_vm = self.apiclient.migrateVirtualMachine(cmd)
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = migrated_vm.id,
            listall=True
            )
        for v in volumes:
            name = v.path.split("/")[3]
            try:
                sp_volume = self.spapi.volumeList(volumeName="~" + name)
            except spapi.ApiError as err:
                raise Exception(err)

            self.assertEqual(v.storageid, self.storage_pool.id, "Did not migrate virtual machine from NFS to StorPool")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_2_migrate_volume_from_nfs_to_storpool(self):
        ''' Test migrate volume from NFS primary storage to StorPool'''

        self.vm2.stop(self.apiclient, forced=True)
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.vm2.id,
            listall=True
            )
        for v in volumes:
            cmd = migrateVolume.migrateVolumeCmd()
            cmd.storageid = self.storage_pool.id
            cmd.volumeid = v.id
            volume =  self.apiclient.migrateVolume(cmd)
            self.assertEqual(volume.storageid, self.storage_pool.id, "Did not migrate volume from NFS to StorPool")

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.vm2.id,
            listall=True
            )
        for v in volumes:
            name = v.path.split("/")[3]
            try:
                sp_volume = self.spapi.volumeList(volumeName="~" + name)
            except spapi.ApiError as err:
                raise Exception(err)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_3_migrate_volume_from_nfs_to_storpool(self):
        '''Test write on disk before migrating volume from NFS primary storage
         Check that data is on disk after migration'''

        try:
            # Login to VM and write data to file system
            ssh_client = self.vm3.get_ssh_client(reconnect = True)

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
                      self.vm3.ipaddress)
        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data has be write into temp file!"
        )

        self.vm3.stop(self.apiclient, forced=True)
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.vm3.id,
            listall=True
            )
        time.sleep(30)
        for v in volumes:
            cmd = migrateVolume.migrateVolumeCmd()
            cmd.storageid = self.storage_pool.id
            cmd.volumeid = v.id
            volume =  self.apiclient.migrateVolume(cmd)
            self.assertEqual(volume.storageid, self.storage_pool.id, "Did not migrate volume from NFS to StorPool")

        volumes = list_volumes(
            self.apiclient,
            virtualmachineid = self.vm3.id,
            listall=True
            )
        for v in volumes:
            name = v.path.split("/")[3]
            try:
                sp_volume = self.spapi.volumeList(volumeName="~" + name)
            except spapi.ApiError as err:
                raise Exception(err)

        self.vm3.start(self.apiclient)
        try:
            ssh_client = self.vm3.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.test_dir, self.random_data)
            ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" %
                      self.vm3.ipaddress)

        self.assertEqual(
            self.random_data_0,
            result[0],
            "Check the random data is equal with the ramdom file!"
        )
