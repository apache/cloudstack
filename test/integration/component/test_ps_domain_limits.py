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

""" P1 tests for primary storage domain limits

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domains+and+accounts
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Resources,
                             Domain,
                             Volume,
                             DiskOffering,
                             Snapshot)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               createSnapshotFromVirtualMachineVolume,
                               isVmExpunged,
                               isDomainResourceCountEqualToExpectedCount,
                               find_storage_pool_type)
from marvin.lib.utils import (cleanup_resources)
from marvin.codes import (PASS,
                          FAIL,
                          FAILED,
                          RESOURCE_PRIMARY_STORAGE)


class TestMultipleChildDomain(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestMultipleChildDomain,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id

        cls._cleanup = []
        try:
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.services["disk_offering"]["disksize"] = 5
        try:
            self.disk_offering = DiskOffering.create(
                self.apiclient,
                self.services["disk_offering"]
            )
            self.assertNotEqual(self.disk_offering, None,
                                "Disk offering is None")
            self.cleanup.append(self.disk_offering)
        except Exception as e:
            self.tearDown()
            self.skipTest("Failure while creating disk offering: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def updateDomainResourceLimits(self, parentdomainlimit, subdomainlimit):
        """Update primary storage limits of the parent domain and its
        child domains"""

        try:
            # Update resource limit for domain
            Resources.updateLimit(self.apiclient, resourcetype=10,
                                  max=parentdomainlimit,
                                  domainid=self.parent_domain.id)

            # Update Resource limit for sub-domains
            Resources.updateLimit(self.apiclient, resourcetype=10,
                                  max=subdomainlimit,
                                  domainid=self.cadmin_1.domainid)

            Resources.updateLimit(self.apiclient, resourcetype=10,
                                  max=subdomainlimit,
                                  domainid=self.cadmin_2.domainid)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    def setupAccounts(self):
        try:
            self.parent_domain = Domain.create(
                self.apiclient,
                services=self.services["domain"],
                parentdomainid=self.domain.id)
            self.parentd_admin = Account.create(
                self.apiclient,
                self.services["account"],
                admin=True,
                domainid=self.parent_domain.id)

            # Create sub-domains and their admin accounts
            self.cdomain_1 = Domain.create(
                self.apiclient,
                services=self.services["domain"],
                parentdomainid=self.parent_domain.id)
            self.cdomain_2 = Domain.create(
                self.apiclient,
                services=self.services["domain"],
                parentdomainid=self.parent_domain.id)

            self.cadmin_1 = Account.create(
                self.apiclient,
                self.services["account"],
                admin=True,
                domainid=self.cdomain_1.id)

            self.cadmin_2 = Account.create(
                self.apiclient,
                self.services["account"],
                admin=True,
                domainid=self.cdomain_2.id)

            # Cleanup the resources created at end of test
            self.cleanup.append(self.cadmin_1)
            self.cleanup.append(self.cadmin_2)
            self.cleanup.append(self.cdomain_1)
            self.cleanup.append(self.cdomain_2)
            self.cleanup.append(self.parentd_admin)
            self.cleanup.append(self.parent_domain)

            users = {
                self.cdomain_1: self.cadmin_1,
                self.cdomain_2: self.cadmin_2
            }
        except Exception as e:
            return [FAIL, e, None]
        return [PASS, None, users]

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_01_multiple_domains_primary_storage_limits(self):
        """Test primary storage limit of domain and its sub-domains

        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        2. Update primary storage limits of the parent domain and child domains
        3. Deploy VM in child domain 1 so that total primary storage
           is less than the limit of child domain
        4. Repeat step 3 for child domain 2
        5. Try to deploy VM in parent domain now so that the total primary storage in
           parent domain (including that in sub-domains is more than the primary
           storage limit of the parent domain
        6. Delete the admin account of child domain 1 and check resource count
           of the parent domain
        7.  Delete VM deployed in account 2 and check primary storage count
           of parent domain

        # Validations:
        1. Step 3 and 4 should succeed
        2. Step 5 should fail as the resource limit exceeds in parent domain
        3. After step 6, resource count in parent domain should decrease by equivalent
           quantity
        4. After step 7, resource count in parent domain should be 0"""

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(
            result[0],
            PASS,
            "Failure while setting up accounts and domains: %s" %
            result[1])

        templatesize = (self.template.size / (1024 ** 3))
        disksize = 10
        subdomainlimit = (templatesize + disksize)

        result = self.updateDomainResourceLimits(
            ((subdomainlimit * 3) - 1),
            subdomainlimit)
        self.assertEqual(
            result[0],
            PASS,
            "Failure while updating resource limits: %s" %
            result[1])

        try:
            self.services["disk_offering"]["disksize"] = disksize
            disk_offering_custom = DiskOffering.create(
                self.apiclient,
                services=self.services["disk_offering"])
            self.cleanup.append(disk_offering_custom)
        except Exception as e:
            self.fail("Failed to create disk offering")

        # Get API clients of parent and child domain admin accounts
        api_client_admin = self.testClient.getUserApiClient(
            UserName=self.parentd_admin.name,
            DomainName=self.parentd_admin.domain)
        self.assertNotEqual(
            api_client_admin,
            FAILED,
            "Failed to create api client for account: %s" %
            self.parentd_admin.name)

        api_client_cadmin_1 = self.testClient.getUserApiClient(
            UserName=self.cadmin_1.name,
            DomainName=self.cadmin_1.domain)
        self.assertNotEqual(
            api_client_cadmin_1,
            FAILED,
            "Failed to create api client for account: %s" %
            self.cadmin_1.name)

        api_client_cadmin_2 = self.testClient.getUserApiClient(
            UserName=self.cadmin_2.name,
            DomainName=self.cadmin_2.domain)
        self.assertNotEqual(
            api_client_cadmin_2,
            FAILED,
            "Failed to create api client for account: %s" %
            self.cadmin_2.name)

        VirtualMachine.create(
            api_client_cadmin_1,
            self.services["virtual_machine"],
            accountid=self.cadmin_1.name,
            domainid=self.cadmin_1.domainid,
            diskofferingid=disk_offering_custom.id,
            serviceofferingid=self.service_offering.id)

        self.initialResourceCount = (templatesize + disksize)
        result = isDomainResourceCountEqualToExpectedCount(
            self.apiclient, self.parent_domain.id,
            self.initialResourceCount, RESOURCE_PRIMARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        # Create VM in second child domain
        vm_2 = VirtualMachine.create(
            api_client_cadmin_2,
            self.services["virtual_machine"],
            accountid=self.cadmin_2.name,
            domainid=self.cadmin_2.domainid,
            diskofferingid=disk_offering_custom.id,
            serviceofferingid=self.service_offering.id)

        # Now the VMs in two child domains have exhausted the primary storage limit
        # of parent domain, hence VM creation in parent domain with custom disk offering
        # should fail
        with self.assertRaises(Exception):
            VirtualMachine.create(
                api_client_admin,
                self.services["virtual_machine"],
                accountid=self.parentd_admin.name,
                domainid=self.parentd_admin.domainid,
                diskofferingid=disk_offering_custom.id,
                serviceofferingid=self.service_offering.id)

        # Deleting user account
        self.cadmin_1.delete(self.apiclient)
        self.cleanup.remove(self.cadmin_1)

        expectedCount = self.initialResourceCount
        result = isDomainResourceCountEqualToExpectedCount(
            self.apiclient, self.parent_domain.id,
            expectedCount, RESOURCE_PRIMARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")

        try:
            vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        self.assertTrue(isVmExpunged(self.apiclient, vm_2.id), "VM not expunged \
                in allotted time")

        expectedCount -= templatesize
        result = isDomainResourceCountEqualToExpectedCount(
            self.apiclient, self.parent_domain.id,
            expectedCount, RESOURCE_PRIMARY_STORAGE)
        self.assertFalse(result[0], result[1])
        self.assertTrue(result[2], "Resource count does not match")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_multiple_domains_primary_storage_limits(self):
        """Test primary storage counts in multiple child domains
        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        Repeat following steps for both the child domains
        2. Deploy VM in child domain
        3. Check if the resource count for domain is updated correctly
        4. Create a volume and attach it to the VM
        5. Check if the primary storage resource count is updated correctly

        """

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(
            result[0],
            PASS,
            "Failure while setting up accounts and domains: %s" %
            result[1])
        users = result[2]

        templatesize = (self.template.size / (1024 ** 3))

        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)
            self.assertNotEqual(
                apiclient,
                FAILED,
                "Failed to create api client for account: %s" %
                self.account.name)
            try:
                vm = VirtualMachine.create(
                    apiclient,
                    self.services["virtual_machine"],
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id,
                    serviceofferingid=self.service_offering.id)

                expectedCount = templatesize + self.disk_offering.disksize
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                # Creating service offering with 10 GB volume
                self.services["disk_offering"]["disksize"] = 10
                disk_offering_10_GB = DiskOffering.create(
                    self.apiclient,
                    services=self.services["disk_offering"])

                self.cleanup.append(disk_offering_10_GB)

                volume = Volume.create(
                    apiclient,
                    self.services["volume"],
                    zoneid=self.zone.id,
                    account=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=disk_offering_10_GB.id)

                volumeSize = (volume.size / (1024 ** 3))
                expectedCount += volumeSize

                vm.attach_volume(apiclient, volume=volume)
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")
            except Exception as e:
                self.fail("Failure: %s" % e)
            return

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_multiple_domains_multiple_volumes(self):
        """Test primary storage counts in multiple child domains
        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        Repeat following steps for both the child domains
        2. Deploy VM in child domain
        3. Check if the resource count for domain is updated correctly
        4. Create multiple volumes and attach it to the VM
        5. Check if the primary storage resource count is updated correctly
        6. Delete one of the volumes and check if the primary storage resource count
           reduced by equivalent number
        7. Detach other volume and check primary storage resource count remains the same

        """
        # Setting up account and domain hierarchy

        if self.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(self.apiclient, storagetype='rbd'):
                self.skipTest("RBD storage type is required for data volumes for LXC")
        result = self.setupAccounts()
        if result[0] == FAIL:
            self.fail(
                "Failure while setting up accounts and domains: %s" %
                result[1])
        else:
            users = result[2]

        templatesize = (self.template.size / (1024 ** 3))

        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain)
            self.assertNotEqual(
                apiclient,
                FAILED,
                "Failed to create api client for account: %s" %
                self.account.name)

            try:
                vm = VirtualMachine.create(
                    apiclient,
                    self.services["virtual_machine"],
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id,
                    serviceofferingid=self.service_offering.id)

                expectedCount = templatesize + self.disk_offering.disksize
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                volume1size = self.services["disk_offering"]["disksize"] = 15
                disk_offering_15_GB = DiskOffering.create(
                    self.apiclient,
                    services=self.services["disk_offering"])

                self.cleanup.append(disk_offering_15_GB)

                volume2size = self.services["disk_offering"]["disksize"] = 20
                disk_offering_20_GB = DiskOffering.create(
                    self.apiclient,
                    services=self.services["disk_offering"])

                self.cleanup.append(disk_offering_20_GB)

                volume_1 = Volume.create(
                    apiclient,
                    self.services["volume"],
                    zoneid=self.zone.id,
                    account=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=disk_offering_15_GB.id)

                volume_2 = Volume.create(
                    apiclient,
                    self.services["volume"],
                    zoneid=self.zone.id,
                    account=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=disk_offering_20_GB.id)

                vm.attach_volume(apiclient, volume=volume_1)
                vm.attach_volume(apiclient, volume=volume_2)

                expectedCount += volume1size + volume2size
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                vm.detach_volume(apiclient, volume=volume_1)
                volume_1.delete(apiclient)

                expectedCount -= volume1size
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")
            except Exception as e:
                self.fail("Failure: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_create_template_snapshot(self):
        """Test create snapshot and templates from volume

        # Validate the following
        1. Create parent domain with two child sub-domains (and their admin accounts)
        Follow these steps for both the domains
        # 1. Create template from snapshot and verify secondary storage resource count
        # 2. Create Volume from Snapshot and verify primary storage resource count
        # 3. Attach volume to instance which was created from snapshot and
        #    verify primary storage resource count
        # 4. Detach volume from instance which was created from snapshot and
        #    verify the primary storage resource count
        # 5. Delete volume which was created from snapshot and verify primary storage
             resource count"""

        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            self.skipTest("Snapshots feature is not supported on %s" % self.hypervisor.lower())

        result = self.setupAccounts()
        if result[0] == FAIL:
            self.fail(
                "Failure while setting up accounts and domains: %s" %
                result[1])
        users = result[2]

        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain

            try:
                apiclient = self.testClient.getUserApiClient(
                    UserName=self.account.name,
                    DomainName=self.account.domain)
                self.assertNotEqual(
                    apiclient,
                    FAILED,
                    "Failed to create api client for account: %s" %
                    self.account.name)

                vm = VirtualMachine.create(
                    apiclient,
                    self.services["virtual_machine"],
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id,
                    serviceofferingid=self.service_offering.id)

                templatesize = (self.template.size / (1024 ** 3))

                initialResourceCount = expectedCount = templatesize + \
                    self.disk_offering.disksize
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    initialResourceCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                vm.stop(self.apiclient)

                response = createSnapshotFromVirtualMachineVolume(
                    apiclient,
                    self.account,
                    vm.id)
                self.assertEqual(response[0], PASS, response[1])
                snapshot = response[1]

                response = snapshot.validateState(
                    apiclient,
                    Snapshot.BACKED_UP)
                self.assertEqual(response[0], PASS, response[1])

                self.services["volume"]["size"] = self.services[
                    "disk_offering"]["disksize"]
                volume = Volume.create_from_snapshot(
                    apiclient,
                    snapshot_id=snapshot.id,
                    services=self.services["volume"],
                    account=self.account.name,
                    domainid=self.account.domainid)
                volumeSize = (volume.size / (1024 ** 3))
                vm.attach_volume(apiclient, volume)
                expectedCount = initialResourceCount + (volumeSize)
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                expectedCount -= volumeSize
                vm.detach_volume(apiclient, volume)
                volume.delete(apiclient)
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.domain.id,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")
            except Exception as e:
                self.fail("Failed with exception : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_assign_virtual_machine_different_domain(self):
        """Test assign virtual machine to account belonging to different domain

        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        2. Deploy VM in child domain 1
        3. Check if the resource count for domain 1 is updated correctly
        4. Assign this virtual machine to account 2 in domain 2
        5. Verify that primaru storage resource count of domain 1 is now 0 and
           primary storage resource count of domain 2 is increased by equivalent number
        """

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])

        apiclient = self.testClient.getUserApiClient(
            UserName=self.cadmin_1.name,
            DomainName=self.cadmin_1.domain)
        self.assertNotEqual(
            apiclient,
            FAILED,
            "Failed to create api client for account: %s" %
            self.cadmin_1.name)

        try:
            vm_1 = VirtualMachine.create(
                apiclient,
                self.services["virtual_machine"],
                accountid=self.cadmin_1.name,
                domainid=self.cadmin_1.domainid,
                diskofferingid=self.disk_offering.id,
                serviceofferingid=self.service_offering.id)

            templatesize = (self.template.size / (1024 ** 3))

            expectedCount = templatesize + self.disk_offering.disksize
            result = isDomainResourceCountEqualToExpectedCount(
                self.apiclient, self.cadmin_1.domainid,
                expectedCount, RESOURCE_PRIMARY_STORAGE)
            self.assertFalse(result[0], result[1])
            self.assertTrue(result[2], "Resource count does not match")

            vm_1.stop(apiclient)
            vm_1.assign_virtual_machine(
                self.apiclient,
                account=self.cadmin_2.name,
                domainid=self.cadmin_2.domainid)

            result = isDomainResourceCountEqualToExpectedCount(
                self.apiclient, self.cadmin_2.domainid,
                expectedCount, RESOURCE_PRIMARY_STORAGE)
            self.assertFalse(result[0], result[1])
            self.assertTrue(result[2], "Resource count does not match")

            expectedCount = 0
            result = isDomainResourceCountEqualToExpectedCount(
                self.apiclient, self.cadmin_1.domainid,
                expectedCount, RESOURCE_PRIMARY_STORAGE)
            self.assertFalse(result[0], result[1])
            self.assertTrue(result[2], "Resource count does not match")
        except Exception as e:
            self.fail("Failed with exception: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_06_destroy_recover_vm(self):
        """Test primary storage counts while destroying and recovering VM
        # Steps
        1. Create a parent domain and two sub-domains in it (also admin accounts
           of each domain)
        Repeat following steps for both the child domains
        2. Deploy VM in child domain
        3. Check if the resource count for domain is updated correctly
        4. Destroy the VM
        5. Verify that the primary storage resource count remains the same
        6. Recover the VM
        7. Verify that the primary storage resource count remains the same
        """

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])
        users = result[2]

        for domain, admin in list(users.items()):
            self.account = admin
            self.domain = domain
            try:
                vm_1 = VirtualMachine.create(
                    self.apiclient,
                    self.services["virtual_machine"],
                    accountid=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id,
                    serviceofferingid=self.service_offering.id)

                templatesize = (self.template.size / (1024 ** 3))

                expectedCount = templatesize + self.disk_offering.disksize
                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.account.domainid,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                vm_1.delete(self.apiclient, expunge=False)

                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.account.domainid,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")

                vm_1.recover(self.apiclient)

                result = isDomainResourceCountEqualToExpectedCount(
                    self.apiclient, self.account.domainid,
                    expectedCount, RESOURCE_PRIMARY_STORAGE)
                self.assertFalse(result[0], result[1])
                self.assertTrue(result[2], "Resource count does not match")
            except Exception as e:
                self.fail("Failed with exception: %s" % e)
        return
