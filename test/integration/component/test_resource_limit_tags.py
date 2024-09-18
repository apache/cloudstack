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
""" BVT tests for resource limit tags functionalities
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listCapacity,
                                  listResourceLimits,
                                  updateResourceLimit,
                                  updateResourceCount)
from marvin.lib.base import (Host,
                             StoragePool,
                             Account,
                             Domain,
                             Zone,
                             ServiceOffering,
                             Template,
                             DiskOffering,
                             VirtualMachine,
                             Volume,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.codes import FAILED
from marvin.cloudstackException import CloudstackAPIException
from nose.plugins.attrib import attr
import logging
# Import System modules
import math
import random


_multiprocess_shared_ = True
MAX_VM_LIMIT = 2
MAX_RAM_VM_LIMIT = 3
MAX_DATA_VOLUME_LIMIT = 3
MAX_PS_DATA_VOLUME_LIMIT = 2

class TestResourceLimitTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestResourceLimitTags, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls._cleanup = []
        cls.logger = logging.getLogger('TestResourceLimitTags')

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"])
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.host_tags = ['htag1', 'htag2', 'htag3']
        cls.host_tags_supporting_types = [0, 8, 9]
        Configurations.update(cls.apiclient,
            "resource.limit.host.tags",
            value=','.join(cls.host_tags)
        )
        cls.storage_tags = ['stag1', 'stag2', 'stag3']
        cls.storage_tags_supporting_types = [2, 10]
        Configurations.update(cls.apiclient,
            "resource.limit.storage.tags",
            value=','.join(cls.storage_tags)
        )

        hosts = Host.list(cls.apiclient, type='Routing')
        cls.original_host_tag_map = {}
        for idx, host in enumerate(hosts):
            cls.original_host_tag_map[host.id] = host.hosttags
            if idx % 2 == 0:
                Host.update(cls.apiclient, id=host.id, hosttags=cls.host_tags[1])
            else:
                Host.update(cls.apiclient, id=host.id, hosttags='')

        pools = StoragePool.list(cls.apiclient)
        cls.original_storage_pool_tag_map = {}
        for idx, pool in enumerate(pools):
            cls.original_storage_pool_tag_map[pool.id] = pool.tags
            if idx % 2 == 0:
                StoragePool.update(cls.apiclient, id=pool.id, tags=cls.storage_tags[1])
            else:
                StoragePool.update(cls.apiclient, id=pool.id, tags='')


        cls.untagged_compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"])
        cls._cleanup.append(cls.untagged_compute_offering)

        host_tagged_compute_offering_service = cls.services["service_offerings"]["tiny"].copy()
        host_tagged_compute_offering_service["hosttags"] = cls.host_tags[1]
        cls.host_tagged_compute_offering = ServiceOffering.create(
            cls.apiclient,
            host_tagged_compute_offering_service)
        cls._cleanup.append(cls.host_tagged_compute_offering)

        host_storage_tagged_compute_offering_service = cls.services["service_offerings"]["tiny"].copy()
        host_storage_tagged_compute_offering_service["hosttags"] = cls.host_tags[1]
        host_storage_tagged_compute_offering_service["tags"] = cls.storage_tags[1]
        cls.host_storage_tagged_compute_offering = ServiceOffering.create(
            cls.apiclient,
            host_storage_tagged_compute_offering_service)
        cls._cleanup.append(cls.host_storage_tagged_compute_offering)

        cls.untagged_disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.untagged_disk_offering)

        tagged_disk_offering_service = cls.services["disk_offering"].copy()
        tagged_disk_offering_service["tags"] = cls.storage_tags[1]
        cls.tagged_disk_offering = DiskOffering.create(
            cls.apiclient,
            tagged_disk_offering_service
        )
        cls._cleanup.append(cls.tagged_disk_offering)

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

    @classmethod
    def tearDownClass(cls):
        for host_id in cls.original_host_tag_map:
            tag = cls.original_host_tag_map[host_id]
            if tag is None:
                tag = ''
            Host.update(cls.apiclient, id=host_id, hosttags=tag)
        for pool_id in cls.original_storage_pool_tag_map:
            tag = cls.original_storage_pool_tag_map[pool_id]
            if tag is None:
                tag = ''
            StoragePool.update(cls.apiclient, id=pool_id, tags=tag)
        super(TestResourceLimitTags, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []
        self.tag_type = random.choice(['host', 'storage'])
        self.domain1 = Domain.create(
            self.apiclient,
            self.services["domain"])
        self.cleanup.append(self.domain1)
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain1.id)
        self.cleanup.append(self.account)
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )

    def tearDown(self):
        super(TestResourceLimitTags, self).tearDown()

    def check_entity_tagged_resource_count(self, taggedresources):
        self.assertNotEqual(
            taggedresources,
            None,
            "Check tagged resources list"
        )
        for type in self.host_tags_supporting_types:
            filtered_limits = list(filter(lambda x: x.resourcetype == type, taggedresources))
            self.assertEqual(
                len(filtered_limits),
                len(self.host_tags)
            )
            for limit in filtered_limits:
                self.assertTrue(limit.tag in self.host_tags)
        for type in self.storage_tags_supporting_types:
            filtered_limits = list(filter(lambda x: x.resourcetype == type, taggedresources))
            self.assertEqual(
                len(filtered_limits),
                len(self.storage_tags)
            )
            for limit in filtered_limits:
                self.assertTrue(limit.tag in self.storage_tags)

    def verify_entity_resource_limits(self, limits, resource_type, tag, max):
        if type(limits) is list:
            if len(limits) == 0:
                self.fail("Empty limits list")
            limits = limits[0]
        self.assertNotEqual(
            limits,
            None,
            "Check tagged limits list"
        )
        self.assertEqual(max,
            limits.max,
            "Max value not equal"
        )
        self.assertEqual(tag,
            limits.tag,
            "Tag value not equal"
        )
        self.assertEqual(str(resource_type),
            limits.resourcetype,
            "Resource type value not equal"
        )

    def update_domain_account_tagged_limit(self, resource_type, tag, max, for_account=False):
        cmd = updateResourceLimit.updateResourceLimitCmd()
        cmd.domainid = self.domain1.id
        if for_account:
            cmd.account = self.account.name
        cmd.resourcetype = resource_type
        cmd.tag = tag
        cmd.max = max
        response = self.apiclient.updateResourceLimit(cmd)
        return response

    def list_domain_account_tagged_limit(self, resource_type, tag, for_account=False):
        cmd = listResourceLimits.listResourceLimitsCmd()
        cmd.domainid = self.domain1.id
        if for_account:
            cmd.account = self.account.name
        cmd.resourcetype = resource_type
        cmd.tag = tag
        response = self.apiclient.listResourceLimits(cmd)
        return response

    def run_test_update_domain_account_tagged_limit(self, for_account=False):
        tags = self.host_tags
        resource_types = self.host_tags_supporting_types
        if self.tag_type == 'storage':
            tags = self.storage_tags
            resource_types = self.storage_tags_supporting_types
        tag = random.choice(tags)
        resource_type = random.choice(resource_types)
        max = random.randrange(5, 10)

        response = self.update_domain_account_tagged_limit(resource_type, tag, max, for_account)
        self.verify_entity_resource_limits(response, resource_type, tag, max)

        response = self.list_domain_account_tagged_limit(resource_type, tag, for_account)
        self.verify_entity_resource_limits(response, resource_type, tag, max)

    def run_test_domain_account_tagged_vm_limit(self, resource_type, max):
        counter = 0
        increment = 1
        if resource_type == 8:
            increment = 100
        elif resource_type == 9:
            increment = 128
        vm_to_delete = None
        while counter < max:
            self.vm = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                serviceofferingid=self.host_tagged_compute_offering.id,
                mode=self.services["mode"]
            )
            self.cleanup.append(self.vm)
            counter = counter + increment
            if vm_to_delete is None:
                vm_to_delete = self.vm
        # Tagged VM shouldn't be deployed
        try:
            self.vm2 = VirtualMachine.create(
                self.userapiclient,
                self.services["virtual_machine"],
                serviceofferingid=self.host_tagged_compute_offering.id,
                mode=self.services["mode"]
            )
            self.cleanup.append(self.vm2)
            self.fail("VM deployed over tagged limit for domain/account")
        except CloudstackAPIException as e:
            self.logger.debug("Over tagged limit VM for domain/account deployment failed with : %s" % e)
        # Untagged VM should be deployed fine
        self.vm3 = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.untagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.vm3)
        # Delete one of the tagged VMs and check if a new tagged VM is deployed
        vm_to_delete.delete(self.apiclient, expunge=True)
        for idx, x in enumerate(self.cleanup):
            if x.id == vm_to_delete.id:
                self.cleanup.pop(idx)
                break
        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.host_tagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.vm)

    def run_test_domain_account_tagged_volume_limit(self, resource_type, max):
        increment = 1
        if resource_type == 10:
            increment = 1*1024*1024*1024
        volume_to_delete = None
        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.host_storage_tagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.vm)
        counter = 1
        if resource_type == 10:
            counter = self.root_volume_size
        while counter < max:
            self.volume = Volume.create(
                self.userapiclient,
                self.services["volume"],
                diskofferingid=self.tagged_disk_offering.id,
                zoneid=self.zone.id
            )
            self.cleanup.append(self.volume)
            counter = counter + increment
            if volume_to_delete is None:
                volume_to_delete = self.volume
        # Tagged VM shouldn't be deployed
        try:
            self.volume2 = Volume.create(
                self.userapiclient,
                self.services["volume"],
                diskofferingid=self.tagged_disk_offering.id,
                zoneid=self.zone.id
            )
            self.cleanup.append(self.volume2)
            self.fail("Volume created over tagged limit for domain/account")
        except CloudstackAPIException as e:
            self.logger.debug("Over tagged limit volume for domain/account deployment failed with : %s" % e)
        # Untagged VM should be deployed fine
        self.volume3 = Volume.create(
            self.userapiclient,
            self.services["volume"],
            diskofferingid=self.untagged_disk_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.volume3)
        # Delete one of the tagged volumes and check if a new tagged volume is deployed
        volume_to_delete.delete(self.apiclient)
        for idx, x in enumerate(self.cleanup):
            if x.id == volume_to_delete.id:
                self.cleanup.pop(idx)
                break
        self.volume = Volume.create(
            self.userapiclient,
            self.services["volume"],
            diskofferingid=self.tagged_disk_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.volume)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_check_list_capacity(self):
        """Test to verify listing capacity with tags
        """
        # Validate the following:
        # 1. List capacity with a valid tag
        # 2. Verify

        cmd = listCapacity.listCapacityCmd()
        cmd.tag = self.host_tags[1]
        response = self.apiclient.listCapacity(cmd)
        self.assertNotEqual(
            response,
            None,
            "Check capacity is listed"
        )
        self.assertTrue(len(response) > 0)
        cmd.tag = "INVALID"
        response = self.apiclient.listCapacity(cmd)
        self.assertEqual(
            response,
            None,
            "Check capacity is listed incorrectly"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_check_list_domain_tagged_limit(self):
        """Test to verify listing domain tagged resource counts
        """

        domain = Domain.list(
            self.apiclient,
            id = self.domain1.id
        )[0]
        self.check_entity_tagged_resource_count(domain.taggedresources)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_check_list_account_tagged_limit(self):
        """Test to verify listing account tagged resource counts
        """

        account = Account.list(
            self.apiclient,
            id = self.account.id
        )[0]
        self.check_entity_tagged_resource_count(account.taggedresources)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_check_update_domain_tagged_limit(self):
        """Test to verify listing domain tagged resource limits
        """
        self.run_test_update_domain_account_tagged_limit()
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_check_update_account_tagged_limit(self):
        """Test to verify listing domain tagged resource limits
        """
        self.run_test_update_domain_account_tagged_limit(True)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_verify_domain_tagged_vm_limit(self):
        """Test to verify domain tagged resource limits working
            1. Check if VM(s) can be deployed within tagged limits for domain
            2. Check if VM(s) can not be deployed over tagged limits for domain
            3. Check if VM(s) can be deployed without tag for domain
        """
        resource_type = 0
        tag = self.host_tags[1]
        max = MAX_VM_LIMIT
        self.update_domain_account_tagged_limit(resource_type, tag, max)
        self.run_test_domain_account_tagged_vm_limit(resource_type, max)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_verify_account_tagged_vm_limit(self):
        """Test to verify account tagged resource limits working
            1. Check if VM(s) can be deployed within tagged limits for account
            2. Check if VM(s) can not be deployed over tagged limits for account
            3. Check if VM(s) can be deployed without tag for account
        """
        resource_type = 9
        tag = self.host_tags[1]
        max = self.host_tagged_compute_offering.memory * MAX_RAM_VM_LIMIT
        self.update_domain_account_tagged_limit(resource_type, tag, max)
        self.run_test_domain_account_tagged_vm_limit(resource_type, max)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_08_verify_domain_tagged_volume_limit(self):
        """Test to verify domain tagged resource limits working
            1. Check if volume(s) can be deployed within tagged limits for domain
            2. Check if volume(s) can not be deployed over tagged limits for domain
            3. Check if volume(s) can be deployed without tag for domain
        """
        resource_type = 2
        tag = self.storage_tags[1]
        max = MAX_DATA_VOLUME_LIMIT
        self.update_domain_account_tagged_limit(resource_type, tag, max)
        self.run_test_domain_account_tagged_volume_limit(resource_type, max)
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_09_verify_account_tagged_volume_limit(self):
        """Test to verify account tagged resource limits working
            1. Check if volume(s) can be deployed within tagged limits for domain
            2. Check if volume(s) can not be deployed over tagged limits for domain
            3. Check if volume(s) can be deployed without tag for domain
        """
        self.test_vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.untagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.test_vm)
        volume = Volume.list(
            self.userapiclient,
            virtualmachineid=self.test_vm.id,
            listall=True,
            type='ROOT'
        )[0]
        self.root_volume_size = volume['size']
        resource_type = 10
        tag = self.storage_tags[1]
        max = math.ceil(self.root_volume_size/(1024*1024*1024)) + MAX_PS_DATA_VOLUME_LIMIT
        self.update_domain_account_tagged_limit(resource_type, tag, max, True)
        self.run_test_domain_account_tagged_volume_limit(resource_type, (max*1024*1024*1024))
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_10_verify_assign_vm_limit(self):
        """Test to verify limits are updated on changing VM owner
        """
        self.donor_account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain1.id)
        self.cleanup.append(self.donor_account)
        # Pass mode=basic to avoid creation of PF rules for the VM
        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.host_storage_tagged_compute_offering.id,
            mode='basic'
        )
        self.cleanup.append(self.vm)
        self.vm.stop(self.userapiclient)
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        tags = [self.host_storage_tagged_compute_offering.hosttags, self.host_storage_tagged_compute_offering.storagetags]
        source_account_usage_before = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        self.vm.assign_virtual_machine(self.apiclient, self.donor_account.name ,self.domain1.id)
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        source_account_usage_after = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        for usage in source_account_usage_after:
            self.assertTrue(usage.total == 0, "Usage for %s with tag %s is not zero for source account" % (usage.resourcetypename, usage.tag))
        acc = Account.list(
            self.apiclient,
            id=self.donor_account.id
        )[0]
        target_account_usage = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        for idx, usage in enumerate(target_account_usage):
            expected_usage = source_account_usage_before[idx]
            self.assertTrue(usage.total == expected_usage.total, "Usage for %s with tag %s is not matching for target account" % (usage.resourcetypename, usage.tag))
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_11_verify_scale_vm_limit(self):
        """Test to verify limits are updated on scaling VM
        """
        scale_compute_offering_service = self.services["service_offerings"]["tiny"].copy()
        scale_compute_offering_service["cpunumber"] = 2 * self.host_tagged_compute_offering.cpunumber
        scale_compute_offering_service["memory"] = 2 * self.host_tagged_compute_offering.memory
        scale_compute_offering_service["hosttags"] = self.host_tagged_compute_offering.hosttags
        self.scale_compute_offering = ServiceOffering.create(
            self.apiclient,
            scale_compute_offering_service)
        self.cleanup.append(self.scale_compute_offering)

        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.host_tagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.vm)
        self.vm.stop(self.userapiclient)
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        tags = [self.host_tagged_compute_offering.hosttags]
        account_usage_before = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        self.vm.scale(self.userapiclient, self.scale_compute_offering.id)
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        account_usage_after = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        for idx, usage in enumerate(account_usage_after):
            expected_usage_total = account_usage_before[idx].total
            if usage.resourcetype in [8, 9]:
                expected_usage_total = 2 * expected_usage_total
            self.assertTrue(usage.total == expected_usage_total, "Usage for %s with tag %s is not matching for target account" % (usage.resourcetypename, usage.tag))
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_12_verify_scale_volume_limit(self):
        """Test to verify limits are updated on scaling volume
        """
        scale_disk_offering_service = self.services["disk_offering"].copy()
        scale_disk_offering_service["tags"] = self.tagged_disk_offering.tags
        scale_disk_offering_service["disksize"] = 2 * self.tagged_disk_offering.disksize
        self.scale_disk_offering = DiskOffering.create(
            self.apiclient,
            scale_disk_offering_service
        )
        self.cleanup.append(self.scale_disk_offering)

        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.host_tagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.vm)
        self.volume = Volume.create(
            self.userapiclient,
            self.services["volume"],
            diskofferingid=self.tagged_disk_offering.id,
            zoneid=self.zone.id
        )
        self.vm.attach_volume(
            self.userapiclient,
            volume=self.volume
        )
        self.vm.detach_volume(
            self.userapiclient,
            volume=self.volume
        )
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        tags = [self.tagged_disk_offering.tags]
        account_usage_before = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        self.volume.resize(
            self.userapiclient,
            diskofferingid=self.scale_disk_offering.id
        )
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        account_usage_after = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        for idx, usage in enumerate(account_usage_after):
            expected_usage_total = account_usage_before[idx].total
            if usage.resourcetype in [10]:
                expected_usage_total = 2 * expected_usage_total
            self.assertTrue(usage.total == expected_usage_total, "Usage for %s with tag %s is not matching for target account" % (usage.resourcetypename, usage.tag))
        return

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_13_verify_restore_vm_limit(self):
        """Test to verify limits are updated on restoring VM
        """
        hypervisor = self.hypervisor.lower()
        restore_template_service = self.services["test_templates"][
            hypervisor if hypervisor != 'simulator' else 'xenserver'].copy()
        restore_template = Template.register(self.apiclient, restore_template_service, zoneid=self.zone.id, hypervisor=hypervisor, templatetag=self.host_tags[1])
        restore_template.download(self.apiclient)
        self.cleanup.append(restore_template)

        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            templateid=restore_template.id,
            serviceofferingid=self.host_storage_tagged_compute_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(self.vm)
        old_root_vol = Volume.list(self.userapiclient, virtualmachineid=self.vm.id)[0]

        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]
        tags = [self.host_storage_tagged_compute_offering.hosttags, self.host_storage_tagged_compute_offering.storagetags]
        account_usage_before = list(filter(lambda x: x.tag in tags, acc['taggedresources']))

        self.vm.restore(self.userapiclient, restore_template.id, rootdisksize=16, expunge=True)
        acc = Account.list(
            self.userapiclient,
            id=self.account.id
        )[0]

        account_usage_after = list(filter(lambda x: x.tag in tags, acc['taggedresources']))
        for idx, usage in enumerate(account_usage_after):
            expected_usage_total = account_usage_before[idx].total
            if usage.resourcetype in [10]:
                expected_usage_total = expected_usage_total - old_root_vol.size + 16 * 1024 * 1024 * 1024
            self.assertTrue(usage.total == expected_usage_total, "Usage for %s with tag %s is not matching for target account" % (usage.resourcetypename, usage.tag))
        return
