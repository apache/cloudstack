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
""" BVT tests for Virtual Machine Life Cycle
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase

from marvin.lib.utils import *

from marvin.lib.base import (Account,
                             Role,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             StoragePool,
                             Volume,
                             DiskOffering,
                             Snapshot,
                             Template)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_hosts,
                               list_volumes,
                               list_storage_pools)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr

import unittest

class TestDeployVMFromSnapshotOrVolume(cloudstackTestCase):

    def rbd_snapshot_cannot_outlive_source_volume(self):
        '''
            On RBD, a snapshot lives on primary storage with its parent
            volume, so with snapshot.backup.to.secondary=false, deleting the
            parent volume destroys the snapshot too - it can no longer be
            used to deploy a new volume/VM afterward.
        '''
        return self.storage_pool.type == 'RBD'

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVMFromSnapshotOrVolume, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()

        if cls.hypervisor.lower() != "kvm":
            raise unittest.SkipTest("Only KVM hypervisor is supported for deployment of a VM with volume/snapshot")

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            account="system"
        )
        if cls.template == FAILED:
            assert False, "get_template failed to return template with description [system]"

        cls.services["small"]["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.debug(cls.account.id)

        # Prefer a zone-wide pool, but also accept a cluster-scope NFS/RBD pool.
        storage_pools_response = list_storage_pools(cls.apiclient,
                                                    zoneid=cls.zone.id,
                                                    scope="ZONE")

        if not storage_pools_response:
            cluster_storage_pools = list_storage_pools(cls.apiclient,
                                                        zoneid=cls.zone.id,
                                                        scope="CLUSTER") or []
            storage_pools_response = [
                pool for pool in cluster_storage_pools
                if getattr(pool, "type", None) in ("NetworkFilesystem", "RBD")
            ]

        if storage_pools_response:
            cls.storage_pool = storage_pools_response[0]

            cls.debug(
                "storage pool id is %s, scope is %s" %
                (cls.storage_pool.id, cls.storage_pool.scope))
            update1 = StoragePool.update(cls.apiclient,
                                         id=cls.storage_pool.id,
                                         tags="test-vm"
                                         )
            cls.debug(
                "Storage %s pool tag%s" %
                (cls.storage_pool.id, update1.tags))
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["small"],
                tags="test-vm"
            )
            cls._cleanup.append(cls.service_offering)

            do = {
                "name": "do-tags",
                "displaytext": "Disk offering with tags",
                "disksize":8,
                "tags": "test-vm"
            }
            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                do,
            )
            cls._cleanup.append(cls.disk_offering)
        else:
            raise unittest.SkipTest("No zone-wide or cluster-scope NFS/RBD storage pool found. Skipping tests")


        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            templateid=cls.template.id,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services['mode']
        )
        volume = list_volumes(
            cls.apiclient,
            virtualmachineid=cls.virtual_machine.id,
            type='ROOT',
            listall=True
        )[0]
        cls.snapshot = Snapshot.create(
            cls.apiclient,
            volume.id,
        )

    @classmethod
    def tearDownClass(cls):
        super(TestDeployVMFromSnapshotOrVolume, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestDeployVMFromSnapshotOrVolume, self).tearDown()

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_deploy_vm_with_existing_volume(self):
        '''
            Deploy a Virtual machine with existing volume
        '''
        self.create_volume_from_snapshot_deploy_vm(self.snapshot.id)

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_deploy_vm_with_existing_snapshot(self):
        '''
            Deploy a Virtual machine with existing snapshot
        '''

        self.deploy_vm_from_snapshot(self.snapshot)

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_deploy_vm_with_existing_volume_deleted_template(self):
        '''
            Deploy a Virtual machine with existing ROOT volume created from a templated which was deleted
        '''
        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.template.ostypeid,
                    "ispublic": "true"}

        template = Template.create_from_snapshot(self.apiclient, self.snapshot, services)
        self._cleanup.append(template)
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                self.services['small'],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=self.zone.id,
                                                serviceofferingid=self.service_offering.id,
                                                templateid=template.id,
                                                mode=self.services['mode'],
                                                )
        try:
            ssh_client = virtual_machine.get_ssh_client(reconnect=True)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )[0]
        snapshot = Snapshot.create(
            self.apiclient,
            root_volume.id,
        )
        VirtualMachine.delete(virtual_machine, self.apiclient, expunge=True)
        if not self.rbd_snapshot_cannot_outlive_source_volume():
            self.create_volume_from_snapshot_deploy_vm(snapshot.id)

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_deploy_vm_with_existing_snapshot_deleted_template(self):
        '''
            Deploy a Virtual machine with existing snapshot of a ROOT volume created from a templated which was deleted
        '''
        if self.rbd_snapshot_cannot_outlive_source_volume():
            raise unittest.SkipTest(
                "Covered by test_03 on RBD: deploying from a snapshot after "
                "its source ROOT volume was deleted is not supported here."
            )
        services = {"displaytext": "Template-1", "name": "Template-1-name", "ostypeid": self.template.ostypeid,
                    "ispublic": "true"}

        template = Template.create_from_snapshot(self.apiclient, self.snapshot, services)
        self._cleanup.append(template)
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                self.services['small'],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=self.zone.id,
                                                serviceofferingid=self.service_offering.id,
                                                templateid=template.id,
                                                mode=self.services['mode'],
                                                )
        try:
            ssh_client = virtual_machine.get_ssh_client(reconnect=True)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )[0]
        snapshot = Snapshot.create(
            self.apiclient,
            root_volume.id,
        )
        VirtualMachine.delete(virtual_machine, self.apiclient, expunge=True)
        self.deploy_vm_from_snapshot(snapshot)

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_deploy_vm_with_existing_snapshot_deleted_volume(self):
        '''
            Deploy a Virtual machine with existing snapshot of a ROOT volume which was deleted
        '''
        if self.rbd_snapshot_cannot_outlive_source_volume():
            raise unittest.SkipTest(
                "Covered by test_03 on RBD: deploying from a snapshot after "
                "its source ROOT volume was deleted is not supported here."
            )
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                self.services['small'],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=self.zone.id,
                                                serviceofferingid=self.service_offering.id,
                                                templateid=self.template.id,
                                                mode=self.services['mode'],
                                                )
        try:
            ssh_client = virtual_machine.get_ssh_client(reconnect=True)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )[0]
        snapshot = Snapshot.create(
            self.apiclient,
            root_volume.id,
        )
        VirtualMachine.delete(virtual_machine, self.apiclient, expunge=True)
        self.deploy_vm_from_snapshot(snapshot)

    def deploy_vm_from_snapshot(self, snapshot):
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                self.services['small'],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=self.zone.id,
                                                serviceofferingid=self.service_offering.id,
                                                snapshotid=snapshot.id,
                                                mode=self.services['mode'],
                                                )
        try:
            ssh_client = virtual_machine.get_ssh_client(reconnect=True)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))

    def create_volume_from_snapshot_deploy_vm(self, snapshotid):
        volume = Volume.create_from_snapshot(
            self.apiclient,
            snapshot_id=snapshotid,
            services=self.services,
            disk_offering=self.disk_offering.id,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
        )
        virtual_machine = VirtualMachine.create(self.apiclient,
                                                self.services['small'],
                                                accountid=self.account.name,
                                                domainid=self.account.domainid,
                                                zoneid=self.zone.id,
                                                serviceofferingid=self.service_offering.id,
                                                volumeid=volume.id,
                                                mode=self.services['mode'],
                                                )
        try:
            ssh_client = virtual_machine.get_ssh_client(reconnect=True)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" %
                      (virtual_machine.ipaddress, e))
