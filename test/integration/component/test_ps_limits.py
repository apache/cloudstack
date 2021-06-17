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

""" P1 tests for primary storage limits

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domain+or+accounts

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-1466

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Limit+Resources+to+domains+and+accounts
"""
import unittest

from ddt import ddt, data
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import (
    PASS,
    FAIL,
    RESOURCE_PRIMARY_STORAGE,
    CHILD_DOMAIN_ADMIN,
    ROOT_DOMAIN_ADMIN)
from marvin.lib.base import (
    Account,
    ServiceOffering,
    VirtualMachine,
    Domain,
    Volume,
    DiskOffering)
from marvin.lib.common import (
    get_domain,
    get_zone,
    get_template,
    matchResourceCount,
    createSnapshotFromVirtualMachineVolume,
    isVmExpunged,
    find_storage_pool_type)
from marvin.lib.utils import (cleanup_resources,
                              validateList)
# Import Local Modules
from nose.plugins.attrib import attr


@ddt
class TestVolumeLimits(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestVolumeLimits,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
        # Fill services from the external config file
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cloudstackTestClient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id

        try:
            cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
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
        if self.unsupportedStorageType:
            self.skipTest(
                "unsupported storage type")
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        try:
            self.services["disk_offering"]["disksize"] = 2
            self.disk_offering = DiskOffering.create(self.apiclient, self.services["disk_offering"])
            self.assertNotEqual(self.disk_offering, None, \
                                "Disk offering is None")
            self.cleanup.append(self.disk_offering)
        except Exception as e:
            self.tearDown()
            self.skipTest("Failure in setup: %s" % e)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setupAccount(self, accountType):
        """Setup the account required for the test"""

        try:
            if accountType == CHILD_DOMAIN_ADMIN:
                self.domain = Domain.create(self.apiclient,
                                            services=self.services["domain"],
                                            parentdomainid=self.domain.id)

            self.account = Account.create(self.apiclient, self.services["account"],
                                          domainid=self.domain.id, admin=True)
            self.cleanup.append(self.account)
            if accountType == CHILD_DOMAIN_ADMIN:
                self.cleanup.append(self.domain)

            self.virtualMachine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                                        accountid=self.account.name, domainid=self.account.domainid,
                                                        diskofferingid=self.disk_offering.id,
                                                        serviceofferingid=self.service_offering.id)

            accounts = Account.list(self.apiclient, id=self.account.id)

            self.assertEqual(validateList(accounts)[0], PASS,
                             "accounts list validation failed")

            self.initialResourceCount = int(accounts[0].primarystoragetotal)
        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_stop_start_vm(self, value):
        """Test Deploy VM with 5 GB volume & verify the usage

        # Validate the following
        # 1. Create a VM with custom disk offering and check the primary storage count
        # 2. Stop VM and verify the resource count remains same
        # 3. Start VM and verify resource count remains same"""

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        expectedCount = self.initialResourceCount
        # Stopping instance
        try:
            self.virtualMachine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        # Starting instance
        try:
            self.virtualMachine.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start instance: %s" % e)

        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @unittest.skip("skip")
    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_destroy_recover_vm(self, value):
        """Test delete and recover instance

        # Validate the following
        # 1. Create a VM with custom disk offering and check the primary storage count
        # 2. Destroy VM and verify the resource count remains same
        # 3. Recover VM and verify resource count remains same"""

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        expectedCount = self.initialResourceCount
        # Stopping instance
        try:
            self.virtualMachine.delete(self.apiclient, expunge=False)
        except Exception as e:
            self.fail("Failed to destroy instance: %s" % e)
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        # Recovering instance
        try:
            self.virtualMachine.recover(self.apiclient)
        except Exception as e:
            self.fail("Failed to start instance: %s" % e)

        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_attach_detach_volume(self, value):
        """Stop attach and detach volume from VM

        # Validate the following
        # 1. Create a VM with custom disk offering and check the primary storage count
        #    of account
        # 2. Create custom volume in account
        # 3. Verify that primary storage count increases by same amount
        # 4. Attach volume to VM and verify resource count remains the same
        # 5. Detach volume and verify resource count remains the same"""

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        apiclient = self.apiclient
        if value == CHILD_DOMAIN_ADMIN:
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain
            )
            self.assertNotEqual(apiclient, FAIL, "Failure while getting\
                    api client of account: %s" % self.account.name)

        try:
            self.services["disk_offering"]["disksize"] = 4
            expectedCount = self.initialResourceCount + int(self.services["disk_offering"]["disksize"])
            disk_offering = DiskOffering.create(self.apiclient,
                                                services=self.services["disk_offering"])

            self.cleanup.append(disk_offering)

            volume = Volume.create(
                apiclient, self.services["volume"], zoneid=self.zone.id,
                account=self.account.name, domainid=self.account.domainid,
                diskofferingid=disk_offering.id)
        except Exception as e:
            self.fail("Failure: %s" % e)

        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            self.virtualMachine.attach_volume(apiclient, volume=volume)
        except Exception as e:
            self.fail("Failed while attaching volume to VM: %s" % e)

        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            self.virtualMachine.detach_volume(apiclient, volume=volume)
        except Exception as e:
            self.fail("Failure while detaching volume: %s" % e)

        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_create_multiple_volumes(self, value):
        """Test create multiple volumes

        # Validate the following
        # 1. Create a VM with custom disk offering and check the primary storage count
        #    of account
        # 2. Create multiple volumes in account
        # 3. Verify that primary storage count increases by same amount
        # 4. Attach volumes to VM and verify resource count remains the same
        # 5. Detach and delete both volumes one by one and verify resource count decreases
        #    proportionately"""

        # Creating service offering with 10 GB volume

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        apiclient = self.apiclient
        if value == CHILD_DOMAIN_ADMIN:
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain
            )
            self.assertNotEqual(apiclient, FAIL, "Failure while getting\
                                api client of account %s" % self.account.name)

        try:
            self.services["disk_offering"]["disksize"] = 5
            disk_offering_5_GB = DiskOffering.create(self.apiclient,
                                                     services=self.services["disk_offering"])
            self.cleanup.append(disk_offering_5_GB)

            self.services["disk_offering"]["disksize"] = 10
            disk_offering_10_GB = DiskOffering.create(self.apiclient,
                                                      services=self.services["disk_offering"])

            self.cleanup.append(disk_offering_10_GB)

            volume_1 = Volume.create(
                apiclient, self.services["volume"], zoneid=self.zone.id,
                account=self.account.name, domainid=self.account.domainid,
                diskofferingid=disk_offering_5_GB.id)

            volume_2 = Volume.create(
                apiclient, self.services["volume"], zoneid=self.zone.id,
                account=self.account.name, domainid=self.account.domainid,
                diskofferingid=disk_offering_10_GB.id)

            self.debug("Attaching volume %s to vm %s" % (volume_1.name, self.virtualMachine.name))
            self.virtualMachine.attach_volume(apiclient, volume=volume_1)

            self.debug("Attaching volume %s to vm %s" % (volume_2.name, self.virtualMachine.name))
            self.virtualMachine.attach_volume(apiclient, volume=volume_2)
        except Exception as e:
            self.fail("Failure: %s" % e)

        expectedCount = self.initialResourceCount + 15  # (5 + 10)
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            # Detaching and deleting volume 1
            self.virtualMachine.detach_volume(apiclient, volume=volume_1)
            volume_1.delete(apiclient)
        except Exception as e:
            self.fail("Failure while volume operation: %s" % e)

        expectedCount -= 5  # After deleting first volume
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            # Detaching and deleting volume 2
            self.virtualMachine.detach_volume(apiclient, volume=volume_2)
            volume_2.delete(apiclient)
        except Exception as e:
            self.fail("Failure while volume operation: %s" % e)

        expectedCount -= 10
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_deploy_multiple_vm(self, value):
        """Test Deploy multiple VMs with & verify the usage
        # Validate the following
        # 1. Deploy multiple VMs with this service offering
        # 2. Update Resource count for the root admin Primary Storage usage
        # 3. Primary Storage usage should list properly
        # 4. Destroy one VM among multiple VM's and verify that primary storage count
        #  decreases by equivalent amount
        """

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        self.virtualMachine_2 = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                                      accountid=self.account.name, domainid=self.account.domainid,
                                                      diskofferingid=self.disk_offering.id,
                                                      serviceofferingid=self.service_offering.id)

        expectedCount = (self.initialResourceCount * 2)  # Total 2 vms
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        self.virtualMachine_3 = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                                      accountid=self.account.name, domainid=self.account.domainid,
                                                      diskofferingid=self.disk_offering.id,
                                                      serviceofferingid=self.service_offering.id)

        expectedCount = (self.initialResourceCount * 3)  # Total 3 vms
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        self.debug("Destroying instance: %s" % self.virtualMachine_2.name)
        try:
            self.virtualMachine_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete instance: %s" % e)

        self.assertTrue(isVmExpunged(self.apiclient, self.virtualMachine_2.id), "VM not expunged \
                in allotted time")

        expectedCount -= (self.template.size / (1024 ** 3))
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic", "selfservice"])
    def test_assign_vm_different_account(self, value):
        """Test assign Vm to different account
        # Validate the following
        # 1. Deploy VM in account and check the primary storage resource count
        # 2. Assign VM to another account
        # 3. Resource count for first account should now equal to 0
        # 4. Resource count for the account to which VM is assigned should
        #    increase to that of initial resource count of first account
        """

        response = self.setupAccount(value)
        self.assertEqual(response[0], PASS, response[1])

        try:
            account_2 = Account.create(self.apiclient, self.services["account"],
                                       domainid=self.domain.id, admin=True)
            self.cleanup.insert(0, account_2)
        except Exception as e:
            self.fail("Failed to create account: %s" % e)

        expectedCount = self.initialResourceCount
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            self.virtualMachine.stop(self.apiclient)
            self.virtualMachine.assign_virtual_machine(self.apiclient,
                                                       account_2.name, account_2.domainid)
        except Exception as e:
            self.fail("Failed to assign virtual machine to account %s: %s" %
                      (account_2.name, e))

        # Checking resource count for account 2
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=account_2.id)
        self.assertEqual(response[0], PASS, response[1])

        expectedCount = 0
        # Checking resource count for original account
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return

    @data(ROOT_DOMAIN_ADMIN, CHILD_DOMAIN_ADMIN)
    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_create_template_snapshot(self, value):
        """Test create snapshot and templates from volume

        # Validate the following
        # 1. Deploy VM with custoom disk offering and check the
        #    primary storage resource count
        # 2. Stop the VM and create Snapshot from VM's volume
        # 3. Create volume againt from this snapshto and attach to VM
        # 4. Verify that primary storage count increases by the volume size
        # 5. Detach and delete volume, verify primary storage count decreaes by volume size"""
        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")
        response = self.setupAccount(value)
        self.debug(response[0])
        self.debug(response[1])
        self.assertEqual(response[0], PASS, response[1])

        apiclient = self.apiclient
        if value == CHILD_DOMAIN_ADMIN:
            apiclient = self.testClient.getUserApiClient(
                UserName=self.account.name,
                DomainName=self.account.domain
            )
            self.assertNotEqual(apiclient, FAIL, "Failure while getting api\
                    client of account: %s" % self.account.name)

        try:
            self.virtualMachine.stop(apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)
        expectedCount = self.initialResourceCount
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        self.debug("Creating snapshot from ROOT volume: %s" % self.virtualMachine.name)
        snapshot = None
        response = createSnapshotFromVirtualMachineVolume(apiclient, self.account, self.virtualMachine.id)
        self.assertEqual(response[0], PASS, response[1])
        snapshot = response[1]
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            self.services["volume"]["size"] = self.services["disk_offering"]["disksize"]
            volume = Volume.create_from_snapshot(apiclient,
                                                 snapshot_id=snapshot.id,
                                                 services=self.services["volume"],
                                                 account=self.account.name,
                                                 domainid=self.account.domainid)

            self.debug("Attaching the volume to vm: %s" % self.virtualMachine.name)
            self.virtualMachine.attach_volume(apiclient, volume)
        except Exception as e:
            self.fail("Failure in volume operation: %s" % e)

        expectedCount += int(self.services["volume"]["size"])
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        try:
            self.virtualMachine.detach_volume(apiclient, volume)
        except Exception as e:
            self.fail("Failure in detach volume operation: %s" % e)

        try:
            self.debug("deleting the volume: %s" % volume.name)
            volume.delete(apiclient)
        except Exception as e:
            self.fail("Failure while deleting volume: %s" % e)

        expectedCount -= int(self.services["volume"]["size"])
        response = matchResourceCount(
            self.apiclient, expectedCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])
        return
