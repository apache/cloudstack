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
"""Utilities functions
"""
# All tests inherit from cloudstack TestCase

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.codes import FAILED, PASS
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             User,
                             DiskOffering,
                             Volume,
                             Template,
                             VmSnapshot,
                             StoragePool,
                             Host,
                             Capacities)
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.common import (get_zone,
                               get_domain,
                               list_clusters,
                               get_template,
                               list_volumes,
                               list_virtual_machines)
from nose.plugins.attrib import attr
from ddt import ddt, data


def verify_vm_state(self, vmid, state):
    list_vm = list_virtual_machines(self.userapiclient,
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    id=vmid)
    self.assertEqual(
        validateList(list_vm)[0],
        PASS,
        'Check List vm response for vmid: %s' %
        vmid)
    self.assertGreater(
        len(list_vm),
        0,
        'Check the list vm response for vm id:  %s' %
        vmid)
    vm = list_vm[0]
    self.assertEqual(
        vm.id,
        str(vmid),
        'Vm deployed is different from the test')
    self.assertEqual(vm.state, state, 'VM is not in %s state' % state)
    self.debug('VM is in is %s state' % state)


def verify_pool_state(self, poolid, state):
    list_storage_pool_response = StoragePool.list(
        self.userapiclient, id=poolid)
    self.assertGreater(len(list_storage_pool_response), 0,
                       'Check list pool response is greater than 0')
    self.assertEqual(
        list_storage_pool_response[0].state,
        state,
        'Storage pool is not in %s state' %
        state)


def verify_vm_storage_pool(self, vmid, storageid):
    root_volume = Volume.list(
        self.userapiclient,
        virtualmachineid=vmid,
        type='ROOT')[0]
    list_volume = Volume.list(self.userapiclient, id=root_volume.id)
    self.assertEqual(
        list_volume[0].storageid,
        storageid,
        'check list volume response for Storage id:  % s ' %
        storageid)


@ddt
class TestPathDisableStorage_Basic(cloudstackTestCase):
    """
        # Tests in this path requires to be run independently
        # ( not to be run in parallel  with any other tests since it involves disabling/enabling storage pools \
         and may cause unexpected failures in other tests
        # The test also requires to have 2 Cluster-wide and 2 zone-wide storage pools available in the setup.
        # For running the tests on local storage, ensure there are 2 local storage pools set up on each host

        """

    @classmethod
    def setUpClass(cls):
        testClient = super(
            TestPathDisableStorage_Basic,
            cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata['ostype'])
        cls.testdata['template']['ostypeid'] = cls.template.ostypeid
        if cls.template == FAILED:
            cls.fail(
                'get_template() failed to return template with description %s'
                % cls.testdata['ostype'])
        cls._cleanup = []
        cls.disabled_list = []
        cls.testdata['template_2']['zoneid'] = cls.zone.id
        cls.testdata['template_2']['ostypeid'] = cls.template.ostypeid
        cls.hypervisor = testClient.getHypervisorInfo()
        try:
            cls.debug('Creating account')
            cls.account = Account.create(cls.apiclient,
                                         cls.testdata['account'],
                                         admin=True
                                         )
            cls._cleanup.append(cls.account)
        except Exception as e:
            cls.tearDownClass()
            raise e

        # Create shared storage offerings
        cls.service_offering_shared = ServiceOffering.create(
            cls.apiclient, cls.testdata['service_offering'])
        cls._cleanup.append(cls.service_offering_shared)
        cls.disk_offering_shared = DiskOffering.create(
            cls.apiclient, cls.testdata['disk_offering'])
        cls.resized_disk_offering = DiskOffering.create(
            cls.apiclient, cls.testdata['resized_disk_offering'])
        cls._cleanup.append(cls.disk_offering_shared)

        # Create offerings for local storage if local storage is enabled
        if cls.zone.localstorageenabled:
            cls.testdata["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.service_offering_local = ServiceOffering.create(
                cls.apiclient, cls.testdata["service_offerings"]["tiny"])
            cls._cleanup.append(cls.service_offering_local)
            cls.testdata["disk_offering"]["storagetype"] = 'local'
            cls.disk_offering_local = DiskOffering.create(
                cls.apiclient, cls.testdata["disk_offering"])
            cls._cleanup.append(cls.disk_offering_local)
            cls.testdata["disk_offering"]["storagetype"] = ' '
            cls.testdata["service_offerings"]["tiny"]["storagetype"] = ' '
        else:
            cls.debug("No local storage found")

        cls.userapiclient = testClient.getUserApiClient(
            UserName=cls.account.name, DomainName=cls.account.domain)
        response = User.login(cls.userapiclient,
                              username=cls.account.name,
                              password=cls.testdata['account']['password']
                              )
        assert response.sessionkey is not None

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception('Warning:Exception during cleanup: %s' % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        if self.disabled_list:
            for poolid in self.disabled_list:
                if StoragePool.list(
                        self.userapiclient,
                        id=poolid)[0].state != 'Up':
                    try:
                        StoragePool.update(
                            self.userapiclient, id=poolid, enabled=True)
                        self.debug('Enabling: % s ' % poolid)
                    except Exception as e:
                        self.fail("Couldn't enable storage % s" % id)

        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.fail('Warning: Exception during cleanup : %s' % e)

    @data('host', 'CLUSTER', 'ZONE')
    @attr(tags=['advanced', 'advancedsg', 'basic'], required_hardware='false')
    def test_01_disable_enable_pool(self, value):
        """

                Test Steps:
                =========
                1. Deploy 2 VMs
                2. Stop VM2
                3. Disable storage pool SP1
                4. Try to deploy a new VM, should fail
                5. Start VM2 which was stopped, should run from same pool
                6. Remove disabled Storage pool SP1, should fail
                7. Enable storage pool SP1
                8. Deploy new VM, VM4 - should succeed
                9. Create and attach new disk to VM4
                10. Disable storage pool SP1 again and enable new pool
                11. Deploy new VM, VM5 - should succeed
                12. Stop VM1 which is running from disabled pool
                13. Migrate ROOT volume of VM1 to another enabled storage pool - should succeed
                14. findStoragePoolsforMigration should not list the disabled pool
                """

        # Choose appropriate service offering depending on the scope the test
        # is being run on
        self.disabled_list = []
        if value == 'CLUSTER':
            other_scope = 'ZONE'
            self.service_offering = self.service_offering_shared
            self.disk_offering = self.disk_offering_shared
        elif value == 'ZONE':
            other_scope = 'CLUSTER'
            self.service_offering = self.service_offering_shared
            self.disk_offering = self.disk_offering_shared
        elif value == 'host':
            # local storage
            other_scope = None
            if self.zone.localstorageenabled:
                self.service_offering = self.service_offering_local
                self.disk_offering = self.disk_offering_local
            else:
                self.skipTest("Local storage not enabled")

        # Keep only one pool active and disable the rest
        try:
            self.list_storage = StoragePool.list(
                self.userapiclient, scope=value)
            if self.list_storage:
                count_st_pools = len(self.list_storage)
            else:
                count_st_pools = 0
            self.disabled_pool_1 = None
            if count_st_pools > 1:
                self.debug(
                    'Found % s storage pools, keeping one and disabling rest' %
                    count_st_pools)
                for pool in self.list_storage[1:]:
                    self.disabled_pool_1 = self.list_storage[1]
                    if pool.state == 'Up':
                        self.debug('Trying to disable storage %s' % pool.id)
                        try:
                            StoragePool.update(
                                self.userapiclient, id=pool.id, enabled=False)
                            self.disabled_list.append(pool.id)
                            self.debug(
                                'Appended to list of disabled pools. List is now: % s ' %
                                self.disabled_list)
                        except Exception as e:
                            raise e
            elif count_st_pools == 1:
                self.debug(
                    'Only one % s wide storage found - will not be able to complete all tests' %
                    value)
            else:
                self.skipTest('No % s  storage pools found' % value)
        except Exception as e:
            raise e

        # Disable the other scope shared storage pools while we are testing on
        # one - applicable for only shared storage
        if value != 'host':
            try:
                self.list_storage = StoragePool.list(
                    self.userapiclient, scope=other_scope)
                if self.list_storage:
                    for pool in self.list_storage:
                        if pool.state == 'Up':
                            self.debug(
                                'Trying to disable storage % s' %
                                pool.id)
                            try:
                                StoragePool.update(
                                    self.userapiclient, id=pool.id, enabled=False)
                                self.disabled_list.append(pool.id)
                                self.debug(
                                    'Appended to list of disabled pools. List is now: % s ' %
                                    self.disabled_list)
                            except Exception as e:
                                self.fail(
                                    "Couldn't disable storage % s" % pool.id)
                else:
                    self.debug('No % s wide storage pools found' % other_scope)
            except Exception as e:
                raise e

        # Step 1: Deploy 2 VMs
        self.virtual_machine_1 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')
        self.virtual_machine_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_2.id, 'Running')

        # Step 2: Keep one VM in stopped state while other keeps running
        try:
            self.debug('Step 2: Stopping one of the VMs')
            self.virtual_machine_2.stop(self.userapiclient)
            verify_vm_state(self, self.virtual_machine_2.id, 'Stopped')
        except Exception as e:
            self.fail('Step 2: Failed to stop VM: %s' % e)

        # Step 3: Disable the Storage Pool, verify VMs are in same state as
        # before
        self.storage_pools_list = StoragePool.list(
            self.userapiclient, scope=value, state='Up')
        self.storage_pool_1 = self.storage_pools_list[0]
        try:
            self.debug(
                'Step 3: Disabling Storage Pool: %s' %
                self.storage_pool_1.id)
            StoragePool.update(
                self.userapiclient,
                id=self.storage_pool_1.id,
                enabled=False)
        except Exception as e:
            self.debug("Step 3: Couldn't disable pool %s" % e)

        verify_pool_state(self, self.storage_pool_1.id, 'Disabled')
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')
        verify_vm_state(self, self.virtual_machine_2.id, 'Stopped')

        # Step 4: Deploying new VM on disabled pool should fail
        self.debug(
            'Step 4: Trying to deploy VM on disabled storage - should fail')
        with self.assertRaises(Exception):
            VirtualMachine.create(self.userapiclient,
                                  self.testdata['small'],
                                  templateid=self.template.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  zoneid=self.zone.id)

        # Step 5: Should be able to start VM on disabled pool
        try:
            self.virtual_machine_2.start(self.userapiclient)
            verify_vm_state(self, self.virtual_machine_2.id, 'Running')
            verify_vm_storage_pool(
                self,
                self.virtual_machine_2.id,
                self.storage_pool_1.id)
        except Exception as e:
            self.fail('Step 5: Failed to start VM: %s' % e)

        # Step 6: Removing disabled pool should fail
        self.debug('Step 6: Trying to remove disabled storage pool')
        with self.assertRaises(Exception):
            StoragePool.delete(self.userapiclient, self.storage_pool_1.id)

        # Step 7: Enable Storage pool
        try:
            self.debug(
                'Step 7: Enabling Storage Pool: %s' %
                self.storage_pool_1.id)
            StoragePool.update(
                self.userapiclient,
                id=self.storage_pool_1.id,
                enabled=True)
        except Exception as e:
            self.debug("Step 7: Couldn't enable pool %s" % e)
        verify_pool_state(self, self.storage_pool_1.id, 'Up')

        # Step 8: Deploy a VM on the pool
        self.virtual_machine_3 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_3.id, 'Running')

        if self.hypervisor.lower() == 'lxc':
            self.skipTest("Not running rest of tests in lxc")

        # Step 9: Create and attach new disk to VM
        self.volume = Volume.create(self.userapiclient,
                                    services=self.testdata['volume'],
                                    diskofferingid=self.disk_offering.id,
                                    zoneid=self.zone.id)
        list_volume = Volume.list(
            self.userapiclient,
            id=self.volume.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.assertEqual(
            validateList(list_volume)[0],
            PASS,
            'Step 9: Check List volume response for volume %s' %
            self.volume.id)
        self.assertEqual(
            list_volume[0].id,
            self.volume.id,
            'Step 9: check list volume response for volume id:  %s' %
            self.volume.id)
        self.debug(
            'Step 9: volume id %s got created successfully' %
            list_volume[0].id)

        self.virtual_machine_3.attach_volume(self.userapiclient, self.volume)
        list_volume = Volume.list(self.userapiclient, id=self.volume.id)
        self.assertEqual(
            list_volume[0].virtualmachineid,
            self.virtual_machine_3.id,
            'Step 9: Check if volume state (attached) is reflected')
        self.debug(
            'Step 9: volume id:%s successfully attached to vm id%s' %
            (self.volume.id, self.virtual_machine_3.id))
        if self.disabled_pool_1:
            newpoolid = self.disabled_pool_1.id
        else:
            self.skipTest(
                'Step 9: Could not find a second storage pool to complete the remaining tests')

        # Step 10: Disable storage pool SP1 again and enable new pool
        try:
            StoragePool.update(self.userapiclient, id=newpoolid, enabled=True)
        except Exception as e:
            self.fail('Step 10: Enable storage pool %s' % e, 'failed')
        verify_pool_state(self, newpoolid, 'Up')
        try:
            self.debug(
                'Step 10: Disabling Storage Pool: %s' %
                self.storage_pool_1.id)
            StoragePool.update(
                self.userapiclient,
                id=self.storage_pool_1.id,
                enabled=False)
            self.disabled_list.append(self.storage_pool_1.id)
            self.debug(
                'Step 10: Appended to list of disabled pools. List is now: % s ' %
                self.disabled_list)
        except Exception as e:
            self.debug("Step 10: Couldn't disable pool %s" % e)
        verify_pool_state(self, self.storage_pool_1.id, 'Disabled')

        # Step 11: Deploy new VM, VM5 - should succeed
        self.virtual_machine_4 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_4.id, 'Running')

        # Step 12: Stop VM1 which is running from disabled pool
        self.virtual_machine_1.stop(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Stopped')

        # Step 13: Migrate ROOT volume of VM1 to another enabled storage pool -
        # should succeed
        if value != 'host':
            root_volume = Volume.list(
                self.userapiclient,
                virtualmachineid=self.virtual_machine_1.id,
                type='ROOT')
            try:
                Volume.migrate(
                    self.userapiclient,
                    volumeid=root_volume[0].id,
                    storageid=newpoolid)
            except Exception as e:
                raise e
            list_volume = list_volumes(
                self.userapiclient, id=root_volume[0].id)
            self.assertEqual(
                isinstance(
                    list_volume,
                    list),
                True,
                'Step 13: Check list volumes response for valid list')

        # Step 14: findStoragePoolsforMigration should not list the disabled
        # pool
        if value != 'host':
            pools_for_migration = StoragePool.listForMigration(
                self.userapiclient, id=root_volume[0].id)
            self.debug(
                'Step 14: List of pools suitable for migration: % s ' %
                pools_for_migration)
            if pools_for_migration:
                if self.storage_pool_1 in pools_for_migration:
                    self.fail(
                        'Step 14: Storage pool % s is supposed to be disabled and not suitable for migration, \
                    but found in the list of pools suitable for migration' %
                        self.storage_pool_1.id)

    @data('host', 'CLUSTER', 'ZONE')
    @attr(tags=['advanced', 'advancedsg', 'basic'], required_hardware='false')
    def test_02_vm_operations_on_disabled_pool(self, value):
        """
        Test Steps:
        =========

        1. Deploy a VM and attach volume
        2. Disable Storage
        3. Create Template from root volume of the VM
        4. Attach a new volume - should fail
        5. Resize DATA disk to a higher value
        6. Take VM Snapshot of the VM (for supported hypervisors)
        7. Destroy the VM and immediately restore the VM
        8. Enable a new storage pool
        9. Re-install the VM with same template
        10. Re-install the VM with the new template created earlier
        11. Repeat tests with enabled pool, Attach new Volume to VM2
        12. Resize disk to a higher value
        13. Reboot the VM
        14. Take VM Snapshot of the VM
        15. Destroy the VM and immediately restore the VM

        """

        # Choose appropriate service offering depending on the scope the test
        # is being run on
        self.disabled_list = []
        if value == 'CLUSTER':
            other_scope = 'ZONE'
            self.service_offering = self.service_offering_shared
            self.disk_offering = self.disk_offering_shared
        elif value == 'ZONE':
            other_scope = 'CLUSTER'
            self.service_offering = self.service_offering_shared
            self.disk_offering = self.disk_offering_shared
        elif value == 'host':
            # local storage
            other_scope = None
            if self.zone.localstorageenabled:
                self.service_offering = self.service_offering_local
                self.disk_offering = self.disk_offering_local
            else:
                self.skipTest("Local storage not enabled")

        if self.hypervisor.lower() == 'lxc':
            self.skipTest("Not running rest of tests in lxc")

        # Keep one storage pool active and disable the rest
        try:
            self.list_storage = StoragePool.list(
                self.userapiclient, scope=value)
            if self.list_storage:
                count_st_pools = len(self.list_storage)
            else:
                count_st_pools = 0
            self.disabled_pool_1 = None
            if count_st_pools > 1:
                self.debug(
                    'Found % s storage pools, keeping one and disabling rest' %
                    count_st_pools)
                for pool in self.list_storage[1:]:
                    self.disabled_pool_1 = self.list_storage[1]
                    if pool.state == 'Up':
                        self.debug('Trying to disable storage %s' % pool.id)
                        try:
                            StoragePool.update(
                                self.userapiclient, id=pool.id, enabled=False)
                            self.disabled_list.append(pool.id)
                            self.debug(
                                'Appended to list of disabled pools. List is now: % s ' %
                                self.disabled_list)
                        except Exception as e:
                            raise e
            elif count_st_pools == 1:
                self.debug(
                    'Only one % s wide storage found - will not be able to complete all tests' %
                    value)
            else:
                self.skipTest('No % s wide storage pools found' % value)
        except Exception as e:
            raise e

        # Disable the other scope storage pools while we are testing on one
        # scope - applicable for only shared storage
        if value != 'host':
            try:
                self.list_storage = StoragePool.list(
                    self.userapiclient, scope=other_scope)
                if self.list_storage:
                    for pool in self.list_storage:
                        if pool.state == 'Up':
                            self.debug(
                                'Trying to disable storage % s' %
                                pool.id)
                            try:
                                StoragePool.update(
                                    self.userapiclient, id=pool.id, enabled=False)
                                self.disabled_list.append(pool.id)
                                self.debug(
                                    'Appended to list of disabled pools. List is now: % s ' %
                                    self.disabled_list)
                            except Exception as e:
                                self.fail(
                                    "Couldn't disable storage % s" % pool.id)
                else:
                    self.debug('No % s wide storage pools found' % other_scope)
            except Exception as e:
                raise e

        # Step 1: Deploy a VM and attach data disk to one VM
        self.virtual_machine_1 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')

        self.volume_1 = Volume.create(self.userapiclient,
                                      services=self.testdata['volume'],
                                      diskofferingid=self.disk_offering.id,
                                      zoneid=self.zone.id)
        self.virtual_machine_1.attach_volume(self.userapiclient, self.volume_1)
        list_volume = Volume.list(self.userapiclient, id=self.volume_1.id)
        self.assertEqual(
            list_volume[0].virtualmachineid,
            self.virtual_machine_1.id,
            ''
            'Check if volume state (attached) is reflected')
        self.debug(
            'Step 1: volume id:%s successfully attached to vm id%s' %
            (self.volume_1.id, self.virtual_machine_1.id))

        # Step 2: Disable the storage pool

        self.storage_pools_list = StoragePool.list(
            self.userapiclient, scope=value, state='Up')
        self.storage_pool_1 = self.storage_pools_list[0]

        try:
            self.debug(
                'Step 2: Disabling Storage Pool: %s' %
                self.storage_pool_1.id)
            StoragePool.update(
                self.userapiclient,
                id=self.storage_pool_1.id,
                enabled=False)
            self.disabled_list.append(self.storage_pool_1.id)
        except Exception as e:
            self.debug("Step 2: Couldn't disable pool %s" % e)
        verify_pool_state(self, self.storage_pool_1.id, 'Disabled')
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')

        # Step 3: Create Template from root volume of the VM
        root_volume_1 = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='ROOT')[0]
        self.virtual_machine_1.stop(self.userapiclient)
        try:
            template_2 = Template.create(self.userapiclient,
                                         self.testdata['template_2'],
                                         volumeid=root_volume_1.id,
                                         account=self.account.name,
                                         domainid=self.account.domainid)
            self.cleanup.append(template_2)
            self.debug('Step 3: Created template with ID: %s' % template_2.id)
            list_template = Template.list(
                self.userapiclient,
                templatefilter='self',
                id=template_2.id)
        except Exception as e:
            self.fail('Step 3: Template from volume failed')

        # Step 4: Attach a new volume - should fail
        self.volume_2 = Volume.create(self.userapiclient,
                                      services=self.testdata['volume'],
                                      diskofferingid=self.disk_offering.id,
                                      zoneid=self.zone.id)
        self.debug(
            'Step 4: Trying to attach new volume to VM on disabled storage - should fail')
        with self.assertRaises(Exception):
            self.virtual_machine_1.attach_volume(
                self.userapiclient, self.volume_2)

        # Step 5: Resize DATA disk to a higher value for attached disk
        try:
            self.volume_1.resize(self.userapiclient,
                                 diskofferingid=self.resized_disk_offering.id)
            list_volume_1 = Volume.list(
                self.userapiclient, id=self.volume_1.id)
            self.assertEqual(
                list_volume_1[0].diskofferingid,
                self.resized_disk_offering.id,
                'check list volume response for volume id:  %s' %
                self.volume_1.id)
            self.debug(
                'Step 5: volume id %s got resized successfully' %
                list_volume_1[0].id)
        except Exception as e:
            self.fail('Step 5: Volume resize on disabled pool failed: % s' % e)

        # Step 6: Take VM Snapshot
        if self.hypervisor.lower() not in ('kvm', 'hyperv', 'lxc'):
            try:
                self.debug(
                    "Step 6: Taking VM Snapshot for vm id % s" %
                    self.virtual_machine_1.id)
                vm_snapshot = VmSnapshot.create(self.userapiclient,
                                                self.virtual_machine_1.id,
                                                'false',
                                                'TestSnapshot',
                                                'Display Text')
                self.assertEqual(
                    vm_snapshot.state,
                    'Ready',
                    'Check VM snapshot is ready')
            except Exception as e:
                self.fail(
                    'Step 6: VM Snapshot on disabled pool failed: % s' %
                    e)

        if vm_snapshot:
            self.debug('Step 6: Deleting Vm Snapshot')
            VmSnapshot.deleteVMSnapshot(self.userapiclient, vm_snapshot.id)

        # Step 7: Destroy VM and immediately restore the VM
        self.debug(
            "Step 7: Deleting and restoring the VM, should continue to run from same storage pool")
        self.virtual_machine_1.delete(self.userapiclient, expunge=False)
        self.virtual_machine_1.recover(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Stopped')
        self.virtual_machine_1.start(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')
        verify_vm_storage_pool(
            self,
            self.virtual_machine_1.id,
            self.storage_pool_1.id)

        # Step 8: Enable new pool
        if self.disabled_pool_1:
            try:
                newpoolid = self.disabled_pool_1.id
                StoragePool.update(
                    self.userapiclient, id=newpoolid, enabled=True)
                self.debug("Step 8: Enabling new pool % s " % newpoolid)
                if newpoolid in self.disabled_list:
                    self.disabled_list.remove(newpoolid)
            except Exception as e:
                self.fail('Step 8: Enable storage pool %s' % e, 'failed')
        else:
            self.debug(
                'Step 8: Could not find a second storage pool, so enabling the first storage pool and running the tests')
            try:
                self.debug(
                    'Step 8: Enabling Storage Pool: %s' %
                    self.storage_pool_1.id)
                StoragePool.update(
                    self.userapiclient,
                    id=self.storage_pool_1.id,
                    enabled=True)
                if self.storage_pool_1.id in self.disabled_list:
                    self.disabled_list.remove(self.storage_pool_1.id)
                newpoolid = self.storage_pool_1.id
            except Exception as e:
                self.fail("Step 8: Couldn't enable pool %s" % e)
        verify_pool_state(self, newpoolid, 'Up')

        # Step 9: Re-install the VM with same template

        if value != 'host':
            self.debug("Step 9: Re-installing VM 1")
            vm_restore = self.virtual_machine_1.restore(
                self.userapiclient, templateid=self.template.id)
            verify_vm_storage_pool(self, self.virtual_machine_1.id, newpoolid)

            # Step 10 : Re-install VM with different template
            self.debug("Step 10: re-installing VM with different template")
            vm_restore = self.virtual_machine_1.restore(
                self.userapiclient, templateid=template_2.id)
            verify_vm_storage_pool(self, self.virtual_machine_1.id, newpoolid)

        # Step 11: Repeat tests with enabled pool. Start with attach VM
        if value != 'host':
            self.debug("Step 11: Attach volume to VM")
            self.virtual_machine_1.attach_volume(
                self.userapiclient, self.volume_2)
            list_volume_2 = Volume.list(
                self.userapiclient, id=self.volume_2.id)
            self.assertEqual(list_volume_2[0].virtualmachineid,
                             self.virtual_machine_1.id,
                             'Check if volume state (attached) is reflected')
            self.debug(
                'Step 11: volume id:% s successfully attached to vm id % s' %
                (self.volume_2.id, self.virtual_machine_1.id))

            # Step 12: Re-size Volume to higher disk offering
            try:
                self.virtual_machine_1.stop(self.userapiclient)
                self.volume_2.resize(
                    self.userapiclient,
                    diskofferingid=self.resized_disk_offering.id)
                list_volume_2 = Volume.list(
                    self.userapiclient, id=self.volume_2.id)
                self.assertEqual(
                    list_volume_2[0].diskofferingid,
                    self.resized_disk_offering.id,
                    'check list volume response for volume id:  %s' %
                    self.volume_2.id)
                self.debug(
                    'Step 12: volume id %s got resized successfully' %
                    list_volume_2[0].id)
            except Exception as e:
                self.fail('Step 12: Failed to resize volume % s ' % e)
            self.virtual_machine_1.start(self.userapiclient)

        # Step 13: Reboot VM
        self.virtual_machine_1.reboot(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')

        # Step 14: Take Snapshot of VM
        if self.hypervisor.lower() not in ('kvm', 'hyperv', 'lxc'):
            try:
                vm_snapshot = VmSnapshot.create(
                    self.userapiclient,
                    self.virtual_machine_1.id,
                    'false',
                    'TestSnapshot2',
                    'Display Text')
                self.assertEqual(
                    vm_snapshot.state,
                    'Ready',
                    'Check the snapshot of vm is ready!')
            except Exception as e:
                self.fail(
                    'Step 14: Snapshot failed post enabling new storage pool')

        # Step 15: Delete and recover VM
        self.debug("Step 15: Deleting and recovering VM")
        self.virtual_machine_1.delete(self.userapiclient, expunge=False)
        self.virtual_machine_1.recover(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Stopped')
        self.virtual_machine_1.start(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')


@ddt
class TestPathDisableStorage_Maint_Tags(cloudstackTestCase):
    """
        # Tests in this path requires to be run independently
        # Not to be run in parallel  with any other tests since it involves disabling/enabling storage pools \
        and may cause unexpected failures in other tests
        # The test also requires to have 2 Cluster-wide and 2 zone-wide storage pools available in the setup.
        # For running the tests on local storage, ensure there are 2 local storage pools set up on each host or different hosts

        """

    @classmethod
    def setUpClass(cls):
        testClient = super(
            TestPathDisableStorage_Maint_Tags,
            cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata['ostype'])
        cls.testdata['template']['ostypeid'] = cls.template.ostypeid
        if cls.template == FAILED:
            cls.fail(
                'get_template() failed to return template with description %s' %
                cls.testdata['ostype'])
        cls._cleanup = []
        cls.disabled_list = []
        cls.maint_list = []
        cls.testdata['template_2']['zoneid'] = cls.zone.id
        cls.testdata['template_2']['ostypeid'] = cls.template.ostypeid
        cls.hypervisor = testClient.getHypervisorInfo()
        try:
            cls.account = Account.create(cls.apiclient,
                                         cls.testdata['account'],
                                         admin=True)
            cls.debug('Creating account')
            cls._cleanup.append(cls.account)

            # Create shared storage offerings
            cls.service_offering_shared = ServiceOffering.create(
                cls.apiclient, cls.testdata['service_offering'])
            cls._cleanup.append(cls.service_offering_shared)
            cls.disk_offering_shared = DiskOffering.create(
                cls.apiclient, cls.testdata['disk_offering'])
            cls.resized_disk_offering = DiskOffering.create(
                cls.apiclient, cls.testdata['resized_disk_offering'])
            cls._cleanup.append(cls.disk_offering_shared)

            # Create offerings for local storage if local storage is enabled
            if cls.zone.localstorageenabled:
                cls.testdata["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
                cls.debug("Creating local storage offering")
                cls.service_offering_local = ServiceOffering.create(
                    cls.apiclient, cls.testdata["service_offerings"]["tiny"])
                cls._cleanup.append(cls.service_offering_local)
                cls.testdata["disk_offering"]["storagetype"] = 'local'
                cls.debug("Creating local storage disk offering")
                cls.disk_offering_local = DiskOffering.create(
                    cls.apiclient, cls.testdata["disk_offering"])
                cls._cleanup.append(cls.disk_offering_local)
                cls.testdata["disk_offering"]["storagetype"] = ' '
                cls.testdata["service_offerings"]["tiny"]["storagetype"] = ' '
            else:
                cls.debug("No local storage found")

            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name, DomainName=cls.account.domain)
            response = User.login(cls.userapiclient,
                                  username=cls.account.name,
                                  password=cls.testdata['account']['password'])
            assert response.sessionkey is not None
        except Exception as e:
            cls.tearDownClass()
            raise e

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception('Warning:Exception during cleanup: %s' % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        if self.disabled_list:
            for poolid in self.disabled_list:
                if StoragePool.list(self.userapiclient, id=poolid)[0].state == 'Disabled':
                    try:
                        StoragePool.update(
                            self.userapiclient, id=poolid, enabled=True)
                        self.debug('Enabling: % s ' % poolid)
                    except Exception as e:
                        self.fail("Couldn't enable storage % s" % id)

        if self.maint_list:
            for poolid in self.maint_list:
                if StoragePool.list(self.userapiclient, id=poolid)[0].state == 'Maintenance':
                    try:
                        StoragePool.cancelMaintenance(
                            self.userapiclient, id=poolid)
                        self.debug(
                            'Cancelled Maintenance mode for % s' %
                            poolid)
                    except Exception as e:
                        self.fail(
                            "Couldn't cancel Maintenance mode for storage % s " %
                            poolid)

        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.fail('Warning: Exception during cleanup : %s' % e)

    @data('host', 'CLUSTER', 'ZONE')
    @attr(tags=['advanced', 'advancedsg', 'basic'], required_hardware='false')
    def test_01_maint_capacity_tags(self, value):
        """

        Test Steps:
        ========

        1. Deploy VM
        2. Add storage to maintenance
        3. Cancel Maintenance
        4. Disable pool and then Start the VM - verify it runs off the same pool
        5. Perform more VM operations - reboot
        6. Add tags to pool
        7. Create tagged offering with same tags
        8. Enable pool
        9. Deploy VM using the tagged offering
        10. Disable storage pool again
        11. Calculate current capacity used so far for the storage pool
        12. Delete VM and check capacity is re-calculated in the disabled pool
        13. Perform VM deploy - should fail since pool is disabled
        14. Re-calculate Capacity, should not be altered


            """

        # Choose appropriate service offering depending on the scope the test
        # is being run on
        self.disabled_list = []
        if value == 'CLUSTER':
            other_scope = 'ZONE'
            self.service_offering = self.service_offering_shared
            self.disk_offering = self.disk_offering_shared
        elif value == 'ZONE':
            other_scope = 'CLUSTER'
            self.service_offering = self.service_offering_shared
            self.disk_offering = self.disk_offering_shared
        elif value == 'host':
            # local storage
            if self.zone.localstorageenabled:
                other_scope = None
                self.service_offering = self.service_offering_local
                self.disk_offering = self.disk_offering_local
            else:
                self.skipTest("Local storage not enabled")

        # Keep 2 storage pools active and disable the rest. If only one storage
        # pool is present, then skip the test
        try:
            self.list_storage = StoragePool.list(
                self.userapiclient, scope=value)
            count_st_pools = len(self.list_storage)
            if count_st_pools <= 1:
                raise unittest.SkipTest(
                    'Found 1 or less storage pools in % s wide scope- cannot proceed' %
                    value)
            elif count_st_pools > 2:
                for pool in self.list_storage[2:]:
                    if pool.state == 'Up':
                        self.debug('Trying to disable storage %s' % pool.id)
                        try:
                            StoragePool.update(
                                self.userapiclient, id=pool.id, enabled=False)
                            self.disabled_list.append(pool.id)
                            self.debug(
                                'Appended to list of disabled pools. List is now: % s ' %
                                self.disabled_list)
                        except Exception as e:
                            raise e
            elif count_st_pools == 2:
                for pool in self.list_storage:
                    if pool.state != 'Up':
                        raise unittest.SkipTest(
                            'Found storage pool % s not in Up State.. cannot proceed' %
                            pool.id)
        except Exception as e:
            raise e

        # Disable the other scope shared storage pools while we are testing on
        # one - applicable for only shared storage
        if value != 'host':
            try:
                self.list_storage = StoragePool.list(
                    self.userapiclient, scope=other_scope)
                if self.list_storage:
                    for pool in self.list_storage:
                        if pool.state == 'Up':
                            self.debug(
                                'Trying to disable storage % s' %
                                pool.id)
                            try:
                                StoragePool.update(
                                    self.userapiclient, id=pool.id, enabled=False)
                                self.disabled_list.append(pool.id)
                                self.debug(
                                    'Appended to list of disabled pools. List is now: % s ' %
                                    self.disabled_list)
                            except Exception as e:
                                self.fail(
                                    "Couldn't disable storage % s" % pool.id)
                else:
                    self.debug('No % s wide storage pools found' % other_scope)
            except Exception as e:
                raise e

        self.debug("Step 1: Deploy VM")
        self.virtual_machine_1 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')

        # Step 2: Add storage to Maintenance mode
        self.debug("Step 2: Adding storage to maintenance mode ")
        root_volume = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='ROOT')[0]
        list_volume = Volume.list(self.userapiclient, id=root_volume.id)
        storage_id = list_volume[0].storageid
        try:
            StoragePool.enableMaintenance(self.userapiclient, id=storage_id)
            self.debug('Step 2: Added % s to Maintenance mode')
            self.maint_list.append(storage_id)
        except Exception as e:
            self.fail(
                'Step 2: Failed to add Storage pool % s to Maintenance mode' %
                storage_id)
        verify_vm_state(self, self.virtual_machine_1.id, 'Stopped')

        # Step 3: Cancel maintenance mode
        try:
            StoragePool.cancelMaintenance(self.userapiclient, id=storage_id)
            self.debug(
                'Step 3: Cancelled Maintenance mode for % s' %
                storage_id)
            self.maint_list.remove(storage_id)
        except Exception as e:
            self.fail(
                "Step 3: Couldn't cancel Maintenance mode for storage % s " %
                storage_id)

        # Step 4: Start the VM after disabling pool and verify it's running
        # from same pool
        try:
            self.debug("Step 4: Starting VM after disabling pool")
            self.list_storage = StoragePool.list(
                self.userapiclient, id=storage_id)
            if self.list_storage[0].state == 'Up':
                StoragePool.update(
                    self.userapiclient,
                    id=storage_id,
                    enabled=False)
                self.debug("Step 4: Disabled pool % s" % storage_id)
                self.disabled_list.append(storage_id)
        except Exception as e:
            raise e

        list_vm = list_virtual_machines(
            self.userapiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=self.virtual_machine_1.id)
        vm = list_vm[0]
        if vm.state != 'Running':
            self.virtual_machine_1.start(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')
        verify_vm_storage_pool(self, self.virtual_machine_1.id, storage_id)

        # Step 5: Perform some VM operations - reboot
        self.debug(
            "Step 5: Performing reboot of VM % s" %
            self.virtual_machine_1.id)
        self.virtual_machine_1.reboot(self.userapiclient)
        verify_vm_storage_pool(self, self.virtual_machine_1.id, storage_id)

        # Step 6: Add tags to the storage pool
        self.debug("Step 6: Adding tags to storage pool")
        StoragePool.update(
            self.userapiclient,
            id=storage_id,
            tags='disable_prov')

        # Step 7: Add tagged service offering
        self.testdata['service_offerings']['tiny']['tags'] = 'disable_prov'
        self.testdata["service_offerings"]["tiny"]["storagetype"] = 'local'
        self.tagged_so = ServiceOffering.create(
            self.userapiclient, self.testdata['service_offerings'])
        self.testdata['service_offerings']['tiny']['tags'] = ' '
        self.testdata["service_offerings"]["tiny"]["storagetype"] = ' '
        self.cleanup.append(self.tagged_so)

        # Step 8: Enable the pool
        try:
            self.debug("Step 8: Enabling pool")
            self.list_storage = StoragePool.list(
                self.userapiclient, id=storage_id)
            if self.list_storage[0].state == 'Disabled':
                StoragePool.update(
                    self.userapiclient,
                    id=storage_id,
                    enabled=True)
                self.disabled_list.remove(storage_id)
        except Exception as e:
            raise e

        # Step 9: Deploy VM using the tagged offering
        self.debug("Step 9: Deploying VM using tagged offering")
        self.virtual_machine_2 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.tagged_so.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_2.id, 'Running')
        verify_vm_storage_pool(self, self.virtual_machine_2.id, storage_id)

        # Step 10: Disable storage Pool
        try:
            self.list_storage = StoragePool.list(
                self.userapiclient, id=storage_id)
            if self.list_storage[0].state == 'Up':
                StoragePool.update(
                    self.userapiclient,
                    id=storage_id,
                    enabled=False)
                if storage_id not in self.disabled_list:
                    self.disabled_list.append(storage_id)
        except Exception as e:
            raise e

        if value != 'host':
            capacity_type = 2
        else:
            capacity_type = 9

        # Step 11: View current capacity of storage pool
        self.debug("Step 11: Getting current capacity...")
        list_capacity_allocated = Capacities.list(
            self.userapiclient, fetchlatest='true', type=capacity_type)
        capacity_1 = list_capacity_allocated[0].capacityused
        self.debug("Capacity 1: % s" % capacity_1)

        # Step 12: Delete VM and check capacity is recalculated in disabled
        # pool
        self.debug("Step 12: Deleting Vm and re-calculating capacity")
        self.virtual_machine_2.delete(self.userapiclient)
        list_capacity_allocated = Capacities.list(
            self.userapiclient, fetchlatest='true', type=capacity_type)
        capacity_2 = list_capacity_allocated[0].capacityused
        self.debug("Capacity 2: % s" % capacity_2)
        self.assertGreater(
            capacity_1,
            capacity_2,
            'Step 12: Capacity Used should be greater after VM delete although Storage is not enabled')

        # Step 13: Deploy new VM with tagged offering again - should fail
        with self.assertRaises(Exception):
            self.virtual_machine_3 = VirtualMachine.create(
                self.userapiclient,
                self.testdata['small'],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.tagged_so.id,
                zoneid=self.zone.id)

        # Step 14: Capacity should not be altered in disabled pool since deploy
        # VM failed
        self.debug(
            "Step 14: Checking capacity is not altered after deploy VM fails")
        list_capacity_allocated = Capacities.list(
            self.userapiclient, fetchlatest='true', type=capacity_type)
        capacity_3 = list_capacity_allocated[0].capacityused
        self.assertEqual(
            capacity_2,
            capacity_3,
            "Step 14: Capacity Used shouldn't be altered since VM deployment failed")


class TestPathDisableStorage_Cross_Cluster(cloudstackTestCase):
    """
        # Tests in this path requires to be run independently (not to be run in parallel  with any other tests \
        since it involves disabling/enabling storage pools and may cause unexpected failures in other tests
        # This test atleast 2 Clusters in the set up wiht suitable hosts for migration.
        # For running the tests on local storage, ensure there are 2 local storage pools set up on each host

        """

    @classmethod
    def setUpClass(cls):
        testClient = super(
            TestPathDisableStorage_Cross_Cluster,
            cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata['mode'] = cls.zone.networktype
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata['ostype'])
        cls.testdata['template']['ostypeid'] = cls.template.ostypeid
        if cls.template == FAILED:
            cls.fail(
                'get_template() failed to return template with description %s' %
                cls.testdata['ostype'])

        cls._cleanup = []
        cls.disabled_list = []
        cls.maint_list = []
        cls.testdata['template_2']['zoneid'] = cls.zone.id
        cls.testdata['template_2']['ostypeid'] = cls.template.ostypeid
        cls.hypervisor = testClient.getHypervisorInfo()

        try:
            cls.account = Account.create(
                cls.apiclient, cls.testdata['account'], admin=True)
            cls.debug('Creating account')
            cls._cleanup.append(cls.account)
            cls.service_offering = ServiceOffering.create(
                cls.apiclient, cls.testdata['service_offering'])
            cls._cleanup.append(cls.service_offering)
            cls.disk_offering = DiskOffering.create(
                cls.apiclient, cls.testdata['disk_offering'])
            cls.resized_disk_offering = DiskOffering.create(
                cls.apiclient, cls.testdata['resized_disk_offering'])
            cls._cleanup.append(cls.disk_offering)

            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name, DomainName=cls.account.domain)
            response = User.login(cls.userapiclient,
                                  username=cls.account.name,
                                  password=cls.testdata['account']['password'])
            assert response.sessionkey is not None
        except Exception as e:
            cls.tearDownClass()
            raise e

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception('Warning:Exception during cleanup: %s' % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        if self.disabled_list:
            for poolid in self.disabled_list:
                if StoragePool.list(self.userapiclient, id=poolid)[
                        0].state == 'Disabled':
                    try:
                        StoragePool.update(
                            self.userapiclient, id=poolid, enabled=True)
                        self.debug('Enabling: % s ' % poolid)
                    except Exception as e:
                        self.fail("Couldn't enable storage % s" % id)
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.fail('Warning: Exception during cleanup : %s' % e)

    @attr(tags=['advanced', 'advancedsg', 'basic'], required_hardware='false')
    def test_01_cross_cluster_attach_disk(self):
        """
            Test Steps:
            ========

            1. Deploy VM in one cluster
            2. Migrate to other cluster
            3. Add data disk, Attach to VM
            4. Disable first storage pool
            5. List for migration should not list the first pool anymore
            6. Stop VM and detach disk
            7. Enable first Pool
            8. Migrate root to first pool
            9. Now disable first pool again
            10. Attach the disk which is running from enabled pool - Should fail
            11.Enable pool again
            12. Attach disk should now pass

            """
        if self.hypervisor.lower() == 'lxc':
            self.skipTest("Not running rest of tests in lxc")

        cluster_id_list = []
        clusters = list_clusters(self.userapiclient, listall='true')
        if len(clusters) == 1:
            raise unittest.SkipTest('Found only one cluster... skipping test')
        for cluster in clusters:
            try:
                self.debug('Processing for cluster % s ' % cluster.id)
                self.list_storage = StoragePool.list(
                    self.userapiclient, clusterid=cluster.id, scope='CLUSTER')
                count_st_pools = len(self.list_storage)
                if count_st_pools > 1:
                    self.debug(
                        'Found % s storage pools in cluster % s, keeping one and disabling rest' %
                        (count_st_pools, cluster.id))
                    for pool in self.list_storage[1:]:
                        self.disabled_pool_1 = self.list_storage[1]
                        if pool.state == 'Up':
                            self.debug(
                                'Trying to disable storage %s' %
                                pool.id)
                            try:
                                StoragePool.update(
                                    self.userapiclient, id=pool.id, enabled=False)
                                self.disabled_list.append(pool.id)
                                self.debug(
                                    'Appended to list of disabled pools. List is now: % s ' %
                                    self.disabled_list)
                            except Exception as e:
                                raise e
                elif count_st_pools == 1:
                    self.debug('Only one cluster wide storage found')
                else:
                    self.fail('No cluster wide storage pools found')
            except Exception as e:
                raise e

        try:
            self.list_storage = StoragePool.list(
                self.userapiclient, scope='ZONE')
            if self.list_storage:
                for pool in self.list_storage:
                    if pool.state == 'Up':
                        self.debug('Trying to disable storage % s' % pool.id)
                        try:
                            StoragePool.update(
                                self.userapiclient, id=pool.id, enabled=False)
                            self.disabled_list.append(pool.id)
                            self.debug(
                                'Appended to list of disabled pools. List is now: % s ' %
                                self.disabled_list)
                        except Exception as e:
                            self.fail("Couldn't disable storage % s" % pool.id)
            else:
                self.debug('No zone wide storage pools found')
        except Exception as e:
            raise e

        # Step 1: Deploy VM in a cluster
        self.virtual_machine_1 = VirtualMachine.create(
            self.userapiclient,
            self.testdata['small'],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')

        root_vol = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='ROOT')[0]
        storage_1 = root_vol.storageid
        host_1 = self.virtual_machine_1.hostid
        self.debug(
            "Step 1: VM1 is running on % s host and % s storage pool" %
            (host_1, storage_1))

        # Step 2: Live Migrate VM to another cluster
        hosts_for_migration = Host.listForMigration(
            self.userapiclient, virtualmachineid=self.virtual_machine_1.id)
        self.debug(
            'Step 2: List of hosts suitable for migration: % s ' %
            hosts_for_migration)
        host_2 = None
        for host in hosts_for_migration:
            self.debug(
                'Step 2: Host Requires storage motion is % s ' %
                host.requiresStorageMotion)
            if host.requiresStorageMotion:
                host_2 = host.id

        if host_2:
            self.debug(
                'Step 2: Migrating VM % s to Host % s' %
                (self.virtual_machine_1.id, host_2))
            self.virtual_machine_1.migrate_vm_with_volume(
                self.userapiclient, hostid=host_2)
        else:
            self.fail('Step 2: No host found suitable for migration')

        # Step 3: Add data disk and attach to VM
        self.volume_1 = Volume.create(self.userapiclient,
                                      services=self.testdata['volume'],
                                      diskofferingid=self.disk_offering.id,
                                      zoneid=self.zone.id)
        self.virtual_machine_1.attach_volume(self.userapiclient, self.volume_1)
        list_volume = Volume.list(self.userapiclient, id=self.volume_1.id)

        self.assertEqual(
            list_volume[0].virtualmachineid,
            self.virtual_machine_1.id,
            'Step 3: Check if volume state (attached) is reflected')
        self.debug(
            'Step 3: volume id:% s successfully attached to vm id % s' %
            (self.volume_1.id, self.virtual_machine_1.id))

        root_vol = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='ROOT')[0]
        storage_2 = root_vol.storageid
        data_vol = Volume.list(
            self.userapiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='DATA')[0]
        self.debug(
            "Step 3: Data Volume is in storage pool: % s" %
            data_vol.storageid)
        self.assertEqual(
            data_vol.storageid,
            root_vol.storageid,
            "Step 3: Root and Data disk should be running from 2nd storage pool where the VM was live migrated")

        # Step 4: Disable first Storage Pool and verify it is not listed in
        # hosts suitable for migration
        try:
            StoragePool.update(self.userapiclient, id=storage_1, enabled=False)
            self.disabled_list.append(storage_1)
            self.debug(
                'Step 4: Appended to list of disabled pools. List is now: % s ' %
                self.disabled_list)
        except Exception as e:
            self.fail("Step 4: Couldn't disable storage % s" % storage_1)

        # Step 5: Disabled pool shouldn't be listed in hostsforMigration since
        # all pools in the cluster are disabled
        hosts_for_migration = Host.listForMigration(
            self.userapiclient, virtualmachineid=self.virtual_machine_1.id)
        self.debug(
            "Step 5: List of Hosts For Migration is % s" %
            hosts_for_migration)
        if hosts_for_migration:
            for host in hosts_for_migration:
                if host_1 == host.id:
                    self.fail(
                        "Step 5: All pools in the cluster are disabled, hence host should not be listed for migration")

        # Step 6: Stop VM and Detach Disk
        self.virtual_machine_1.stop(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Stopped')
        verify_vm_storage_pool(self, self.virtual_machine_1.id, storage_2)
        self.debug("Step 6: Stopping VM and detaching disk")
        self.virtual_machine_1.detach_volume(
            self.userapiclient, volume=self.volume_1)

        # Step 7, 8: Enable Pool for Migrating VM and disable again
        try:
            StoragePool.update(self.userapiclient, id=storage_1, enabled=True)
            if storage_1 in self.disabled_list:
                self.disabled_list.remove(storage_1)
        except Exception as e:
            self.fail("Step 7: Couldn't enable storage % s" % storage_1)

        self.virtual_machine_1.start(self.userapiclient)
        verify_vm_state(self, self.virtual_machine_1.id, 'Running')

        try:
            self.debug(
                'Step 8: Migrating VM % s to Host % s' %
                (self.virtual_machine_1.id, host_1))
            self.virtual_machine_1.migrate_vm_with_volume(
                self.userapiclient, hostid=host_1)
        except Exception as e:
            self.fail(
                "Step 8: Couldn't live migrate VM to host % s due to % s" %
                (host_1, e))

        # Step 9: disable pool again
        try:
            StoragePool.update(self.userapiclient, id=storage_1, enabled=False)
            self.debug("Step 9: Disabling storage pool: % s " % storage_1)
            self.disabled_list.append(storage_1)
        except Exception as e:
            self.fail("Step 9: Couldn't disable storage % s" % storage_1)

        st_list = StoragePool.list(self.userapiclient, id=storage_1)
        self.debug(
            "9.5 Status of storage pool 1 % s  is % s " %
            (st_list[0].name, st_list[0].state))

        # Step 10: Try to attach data disk running from enabled pool with Root
        # running in disabled pool - this should fail
        with self.assertRaises(Exception):
            self.virtual_machine_1.attach_volume(
                self.userapiclient, self.volume_1)
            self.debug(
                "Step 10: Trying to attach volume % s" %
                self.volume_1.id)

        # Step 11: Enable the pool and try to attach again - this should pass
        try:
            StoragePool.update(self.userapiclient, id=storage_1, enabled=True)
            self.debug("Step 11: Enable storage pool: % s " % storage_1)
            self.disabled_list.remove(storage_1)
        except Exception as e:
            self.fail("Step 11: Couldn't enable storage % s" % storage_1)

        # Step 12: Repeat attach volume - should succeed
        self.virtual_machine_1.attach_volume(self.userapiclient, self.volume_1)
        self.debug("Step 12: Trying to attach volume")
        list_volume = Volume.list(self.userapiclient, id=self.volume_1.id)

        self.assertEqual(
            list_volume[0].virtualmachineid,
            self.virtual_machine_1.id,
            'Step 12: Check if volume state (attached) is reflected')
        self.debug(
            'Step 12: volume id:%s successfully attached to vm id%s' %
            (self.volume_1.id, self.virtual_machine_1.id))
