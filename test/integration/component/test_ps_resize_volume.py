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

""" P1 tests for testing resize volume functionality with primary storage
    imit constraints on account/domain

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/
    Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/
    CLOUDSTACK/Limit+Resources+to+domains+and+accounts
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
                             DiskOffering,
                             Volume)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               matchResourceCount,
                               isDomainResourceCountEqualToExpectedCount,
                               find_storage_pool_type)
from marvin.lib.utils import (cleanup_resources,
                              get_hypervisor_type)
from marvin.codes import (PASS,
                          FAIL,
                          FAILED,
                          RESOURCE_PRIMARY_STORAGE,
                          RESOURCE_SECONDARY_STORAGE,
                          XEN_SERVER)


class TestResizeVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestResizeVolume,
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
        cls._cleanup = []
        cls.unsupportedStorageType = False
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.resourcetypemapping = {RESOURCE_PRIMARY_STORAGE: 10,
                                   RESOURCE_SECONDARY_STORAGE: 11}

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id

        try:
            cls.hypervisor = str(get_hypervisor_type(cls.api_client)).lower()
            if cls.hypervisor.lower() in ['hyperv']:
                raise unittest.SkipTest("Volume resize is not supported on %s" % cls.hypervisor)
            # Creating service offering with normal config
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"])
            cls._cleanup.append(cls.service_offering)

            cls.services["disk_offering"]["disksize"] = 5
            cls.disk_offering_5_GB = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls._cleanup.append(cls.disk_offering_5_GB)

            cls.services["disk_offering"]["disksize"] = 20
            cls.disk_offering_20_GB = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls._cleanup.append(cls.disk_offering_20_GB)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(
                "Failure while creating disk offering: %s" %
                e)
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
        if self.unsupportedStorageType:
            self.skipTest(
                "unsupported storage type")
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def updateResourceLimits(self, accountLimit=None, domainLimit=None):
        """Update primary storage limits of the parent domain and its
        child domains"""

        try:
            if domainLimit:
                # Update resource limit for domain
                Resources.updateLimit(self.apiclient, resourcetype=10,
                                      max=domainLimit,
                                      domainid=self.parent_domain.id)
            if accountLimit:
                # Update resource limit for domain
                Resources.updateLimit(self.apiclient,
                                      resourcetype=10,
                                      max=accountLimit,
                                      account=self.parentd_admin.name,
                                      domainid=self.parent_domain.id)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    def setupAccounts(self):
        try:
            self.parent_domain = Domain.create(self.apiclient,
                                               services=self.services[
                                                   "domain"],
                                               parentdomainid=self.domain.id)
            self.parentd_admin = Account.create(self.apiclient,
                                                self.services["account"],
                                                admin=True,
                                                domainid=self.parent_domain.id)

            # Cleanup the resources created at end of test
            self.cleanup.append(self.parentd_admin)
            self.cleanup.append(self.parent_domain)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_increase_volume_size_within_account_limit(self):
        """Test increasing volume size within the account limit and verify
           primary storage usage

        # Validate the following
        # 1. Create a domain and its admin account
        # 2. Set account primary storage limit well beyond (20 GB volume +
        #    template size of VM)
        # 3. Deploy a VM without any disk offering (only root disk)
        # 4. Create a volume of 5 GB in the account and attach it to the VM
        # 5. Increase (resize) the volume to 20 GB
        # 6. Resize operation should be successful and primary storage count
        #    for account should be updated successfully"""

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])

        apiclient = self.testClient.getUserApiClient(
            UserName=self.parentd_admin.name,
            DomainName=self.parentd_admin.domain)
        self.assertNotEqual(apiclient, FAILED, "Failed to get api client\
                            of account: %s" % self.parentd_admin.name)

        templateSize = (self.template.size / (1024**3))
        accountLimit = (templateSize + self.disk_offering_20_GB.disksize)
        response = self.updateResourceLimits(accountLimit=accountLimit)
        self.assertEqual(response[0], PASS, response[1])
        try:
            virtualMachine = VirtualMachine.create(
                apiclient, self.services["virtual_machine"],
                accountid=self.parentd_admin.name,
                domainid=self.parent_domain.id,
                serviceofferingid=self.service_offering.id
            )

            volume = Volume.create(
                apiclient, self.services["volume"],
                zoneid=self.zone.id,
                account=self.parentd_admin.name,
                domainid=self.parent_domain.id,
                diskofferingid=self.disk_offering_5_GB.id)

            virtualMachine.attach_volume(apiclient, volume=volume)

            expectedCount = (templateSize + self.disk_offering_5_GB.disksize)
            response = matchResourceCount(
                self.apiclient, expectedCount,
                RESOURCE_PRIMARY_STORAGE,
                accountid=self.parentd_admin.id)
            if response[0] == FAIL:
                raise Exception(response[1])

            if self.hypervisor == str(XEN_SERVER).lower():
                virtualMachine.stop(self.apiclient)
            volume.resize(
                apiclient,
                diskofferingid=self.disk_offering_20_GB.id)

            expectedCount = (templateSize + self.disk_offering_20_GB.disksize)
            response = matchResourceCount(
                self.apiclient, expectedCount,
                RESOURCE_PRIMARY_STORAGE,
                accountid=self.parentd_admin.id)
            if response[0] == FAIL:
                raise Exception(response[1])
        except Exception as e:
            self.fail("Failed with exception: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_increase_volume_size_above_account_limit(self):
        """Test increasing volume size above the account limit

        # Validate the following
        # 1. Create a domain and its admin account
        # 2. Set account primary storage limit more than (5 GB volume +
        #    template size of VM) and less than (20 GB volume+ template
        #    size of VM)
        # 3. Deploy a VM without any disk offering (only root disk)
        # 4. Create a volume of 5 GB in the account and attach it to the VM
        # 5. Try to (resize) the volume to 20 GB
        # 6. Resize opearation should fail"""

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])

        templateSize = (self.template.size / (1024**3))
        accountLimit = ((templateSize + self.disk_offering_20_GB.disksize) - 1)
        response = self.updateResourceLimits(accountLimit=accountLimit)
        self.assertEqual(response[0], PASS, response[1])

        apiclient = self.testClient.getUserApiClient(
            UserName=self.parentd_admin.name,
            DomainName=self.parentd_admin.domain)
        self.assertNotEqual(apiclient, FAILED, "Failed to get api client\
                            of account: %s" % self.parentd_admin.name)

        try:
            virtualMachine = VirtualMachine.create(
                apiclient, self.services["virtual_machine"],
                accountid=self.parentd_admin.name,
                domainid=self.parent_domain.id,
                serviceofferingid=self.service_offering.id
            )

            volume = Volume.create(
                apiclient, self.services["volume"],
                zoneid=self.zone.id,
                account=self.parentd_admin.name,
                domainid=self.parent_domain.id,
                diskofferingid=self.disk_offering_5_GB.id)

            virtualMachine.attach_volume(apiclient, volume=volume)

            expectedCount = (templateSize + self.disk_offering_5_GB.disksize)
            response = matchResourceCount(
                self.apiclient, expectedCount,
                RESOURCE_PRIMARY_STORAGE,
                accountid=self.parentd_admin.id)
            if response[0] == FAIL:
                raise Exception(response[1])
        except Exception as e:
            self.fail("Failed with exception: %s" % e)

        if self.hypervisor == str(XEN_SERVER).lower():
            virtualMachine.stop(self.apiclient)
        with self.assertRaises(Exception):
            volume.resize(
                apiclient,
                diskofferingid=self.disk_offering_20_GB.id)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_increase_volume_size_above_domain_limit(self):
        """Test increasing volume size above the domain limit

        # Validate the following
        # 1. Create a domain and its admin account
        # 2. Set domain primary storage limit more than (5 GB volume +
        #    template size of VM) and less than (20 GB volume+
        #    template size of VM)
        # 3. Deploy a VM without any disk offering (only root disk)
        # 4. Create a volume of 5 GB in the account and attach it to the VM
        # 5. Try to (resize) the volume to 20 GB
        # 6. Resize opearation should fail"""

        # Setting up account and domain hierarchy
        result = self.setupAccounts()
        self.assertEqual(result[0], PASS, result[1])

        templateSize = (self.template.size / (1024**3))
        domainLimit = ((templateSize + self.disk_offering_20_GB.disksize) - 1)
        response = self.updateResourceLimits(domainLimit=domainLimit)
        self.assertEqual(response[0], PASS, response[1])

        apiclient = self.testClient.getUserApiClient(
            UserName=self.parentd_admin.name,
            DomainName=self.parentd_admin.domain)
        self.assertNotEqual(apiclient, FAILED, "Failed to get api client\
                            of account: %s" % self.parentd_admin.name)

        try:
            virtualMachine = VirtualMachine.create(
                apiclient, self.services["virtual_machine"],
                accountid=self.parentd_admin.name,
                domainid=self.parent_domain.id,
                serviceofferingid=self.service_offering.id
            )

            volume = Volume.create(
                apiclient, self.services["volume"],
                zoneid=self.zone.id,
                account=self.parentd_admin.name,
                domainid=self.parent_domain.id,
                diskofferingid=self.disk_offering_5_GB.id)

            virtualMachine.attach_volume(apiclient, volume=volume)

            expectedCount = (templateSize + self.disk_offering_5_GB.disksize)
            result = isDomainResourceCountEqualToExpectedCount(
                self.apiclient, self.parent_domain.id,
                expectedCount, RESOURCE_PRIMARY_STORAGE)
            self.assertFalse(result[0], result[1])
            self.assertTrue(result[2], "Resource count does not match")
        except Exception as e:
            self.fail("Failed with exception: %s" % e)

        if self.hypervisor == str(XEN_SERVER).lower():
            virtualMachine.stop(self.apiclient)
        with self.assertRaises(Exception):
            volume.resize(
                apiclient,
                diskofferingid=self.disk_offering_20_GB.id)
        return
