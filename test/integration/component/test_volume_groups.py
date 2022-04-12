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
""" P1 tests for Volumes
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Volume,
                             Cluster,
                             DiskOffering)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_pod)

import re



class TestAttachVolumeWithGroup(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.test_client = super(TestAttachVolumeWithGroup, cls).getClsTestClient()
        cls.api_client = cls.test_client.getApiClient()
        cls.test_data = cls.test_client.getParsedTestDataConfig()
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.test_client.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.check_volume_group_tbl = "SELECT volume_group.id FROM volume_group,vm_instance WHERE volume_group.vm_id = vm_instance.id AND vm_instance.uuid='{}'"
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.test_data["ostype"]
        )
        cls._cleanup = []

        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.test_data["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        cls.account = Account.create(
            cls.api_client,
            cls.test_data["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.test_data["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)



    @classmethod
    def tearDownClass(cls):
        super(TestAttachVolumeWithGroup, cls).tearDownClass()

    def setUp(self):
        self.api_client = self.test_client.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestAttachVolumeWithGroup, self).tearDown()

    @attr(tags=["advanced", "advancedns", "needle"])
    def test_attach_mixed_volumes(self):
        virtual_machine = VirtualMachine.create(
            self.api_client,
            self.test_data["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(virtual_machine)
        volume_list = []
        volume_count = 6
        for i in range(0, volume_count):
            volume_list.append([Volume.create(
                self.api_client,
                self.test_data["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            ), 0, 0])
            self.cleanup.append(volume_list[i])

        for i in range(0, volume_count):
            if i > 3:
                virtual_machine.attach_volume(
                    self.api_client,
                    volume_list[i][0],
                    volumegroup=2
                )
                volume_list[i][1] = 2
                volume_list[i][2] = i % 2
                continue
            if i > 1:
                virtual_machine.attach_volume(
                    self.api_client,
                    volume_list[i][0],
                    volumegroup=1
                )
                volume_list[i][1] = 1
                volume_list[i][2] = i % 2
                continue

            virtual_machine.attach_volume(
                self.api_client,
                volume_list[i][0]
            )
            volume_list[i][1] = 0
            volume_list[i][2] = (i % 2) + 1

        for i in range(0, volume_count):
            vol_res = Volume.list(
                self.api_client,
                id=volume_list[i][0].id
            )
            controller = "scsi{}:{}".format(volume_list[i][1], volume_list[i][2])
            if vol_res is not None:
                m = re.search(controller, vol_res[0]['chaininfo'])
                if m is not None:
                    self.assertTrue(True)
                else:
                    self.assertTrue(False, "Volume is not on the right controller")
            else:
                self.assertTrue(False, "Volume not found with id: {}".format(volume_list[i][0].id))

    @attr(tags=["advanced", "advancedns", "needle"])
    def test_unmanage_ingest_vm_with_vol_groups(self):
        self.helper_unmanageInstance()

    @attr(tags=["advanced", "advancedns", "needle"])
    def test_unmanage_ingest_vm_with_controller_conf_param(self):
        self.helper_unmanageInstance(False, True)

    def helper_unmanageInstance(self, withVolGroups=True, withUseControllerConf=False):
        virtual_machine = VirtualMachine.create(
            self.api_client,
            self.test_data['virtual_machine'],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )
        volume_count = 3
        volume_list = []

        for i in range(0, volume_count):
            volume_list.append(Volume.create(
                    self.api_client,
                    self.test_data["volume"],
                    zoneid=self.zone.id,
                    account=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id))

        for i in range(0, volume_count):
            virtual_machine.attach_volume(
                             self.api_client,
                             volume_list[i],
                             volumegroup=i)

        virtual_machine.unmanage(self.api_client)
        result = self.dbclient.execute(self.check_volume_group_tbl.format(virtual_machine.id))
        self.assertEqual(len(result), 0, msg="After unmanage vm, at least one volume group was not removed")

        cluster_list = Cluster.list(self.api_client,
                                    hypervisor=virtual_machine.hypervisor,
                                    zonename=virtual_machine.zonename)

        for cluster in cluster_list:
            unmanage_vm_list = VirtualMachine.listUnmanagedInstances(self.api_client,
                                                                     clusterid=cluster.id,
                                                                     name=virtual_machine.instancename)

            if unmanage_vm_list and len(unmanage_vm_list) == 1:
                data_disk_offering_list = []
                controller_units = [0, 1, 2]
                for volume in unmanage_vm_list[0].disk:
                    if not (volume.controllerunit == 0 and volume.position == 0):
                        if(withVolGroups):
                            data_disk_offering_list.append({
                                'disk': volume.id,
                                'diskOffering': self.disk_offering.id,
                                'volumeGroup': volume.controllerunit
                            })
                        else:
                            data_disk_offering_list.append({
                                'disk': volume.id,
                                'diskOffering': self.disk_offering.id
                            })
                        self.assertIn(volume.controllerunit, controller_units,
                                      msg="After unmanage vm, at least one volume is connected on wrong controller")
                        controller_units.remove(volume.controllerunit)

                details = {'dataDiskController': virtual_machine.details['dataDiskController'],
                           'rootDiskController': virtual_machine.details['rootDiskController']}
                if(withUseControllerConf):
                    imported_vm = VirtualMachine.importUnmanagedVM(apiclient=self.api_client,
                                                                   clusterid=unmanage_vm_list[0].clusterid,
                                                                   name=unmanage_vm_list[0].name,
                                                                   details=[details],
                                                                   datadiskofferinglist=data_disk_offering_list,
                                                                   serviceofferingid=virtual_machine.serviceofferingid,
                                                                   useControllerConfiguration='true')
                else:
                    imported_vm = VirtualMachine.importUnmanagedVM(apiclient=self.api_client,
                                                                   clusterid=unmanage_vm_list[0].clusterid,
                                                                   name=unmanage_vm_list[0].name,
                                                                   details=[details],
                                                                   datadiskofferinglist=data_disk_offering_list,
                                                                   serviceofferingid=virtual_machine.serviceofferingid)
                vm = VirtualMachine(imported_vm.__dict__, {})
                vm.stop(apiclient=self.api_client)
                vm.start(apiclient=self.api_client)

                vols = Volume.list(apiclient=self.api_client, virtualmachineid=vm.id)
                allowed_controller_cfg = ["scsi0:0", "scsi0:1", "scsi1:0", "scsi2:0"]
                for vol in vols:
                    for cfg in allowed_controller_cfg:
                        m = re.search(cfg, vol['chaininfo'])
                        if m is not None:
                            allowed_controller_cfg.remove(cfg)
                            break
                    self.cleanup.append(vol)
                self.assertEqual(len(allowed_controller_cfg), 0,
                                 msg="After import vm, at least one volume is connected on the wrong controller.")

                vm.delete(apiclient=self.api_client, expunge=True)
                result = self.dbclient.execute(self.check_volume_group_tbl.format(vm.id))
                self.assertEqual(len(result), 0, msg="After delete vm, at least one volume group was not removed")
                break

    @attr(tags=["advanced", "advancedns", "needle"])
    def test_controller_pos_seven_with_groups(self):
        virtual_machine = VirtualMachine.create(
            self.api_client,
            self.test_data['virtual_machine'],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(virtual_machine)
        volume_count = 8
        volume_list = []

        for i in range(0, volume_count):
            volume_list.append(Volume.create(
                    self.api_client,
                    self.test_data["volume"],
                    zoneid=self.zone.id,
                    account=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id))
            self.cleanup.append(volume_list[i])

        for i in range(0, 2):
            for j in range(0, volume_count):
                virtual_machine.attach_volume(
                        self.api_client,
                        volume_list[j],
                        volumegroup=i)

                virtual_machine.stop(self.api_client)
                virtual_machine.start(self.api_client)

                virtual_machine.detach_volume(
                    self.api_client,
                    volume_list[j])
