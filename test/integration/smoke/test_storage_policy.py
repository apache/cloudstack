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

""" Test for VMWare storage policies
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             Volume,
                             DiskOffering)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template)
from marvin.cloudstackAPI import (importVsphereStoragePolicies,
                                  listVsphereStoragePolicies,
                                  listVsphereStoragePolicyCompatiblePools)


class TestVMWareStoragePolicies(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestVMWareStoragePolicies, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls._cleanup = []
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.testdata["l2-network_offering"],
        )
        cls.network_offering.update(cls.apiclient, state='Enabled')
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor,
            deploy_as_is=cls.hypervisor.lower() == "vmware"
        )
        cls._cleanup.append(cls.network_offering)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = []
        self.cleanup.append(self.account)
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, reversed(self.cleanup))
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    def import_vmware_storage_policies(self, apiclient):
        cmd = importVsphereStoragePolicies.importVsphereStoragePoliciesCmd()
        cmd.zoneid = self.zone.id
        return apiclient.importVsphereStoragePolicies(cmd)

    def list_storage_policies(self, apiclient):
        cmd = listVsphereStoragePolicies.listVsphereStoragePoliciesCmd()
        cmd.zoneid = self.zone.id
        return apiclient.listVsphereStoragePolicies(cmd)

    def list_storage_policy_compatible_pools(self, apiclient, policyid):
        cmd = listVsphereStoragePolicyCompatiblePools.listVsphereStoragePolicyCompatiblePoolsCmd()
        cmd.zoneid = self.zone.id
        cmd.policyid = policyid
        return apiclient.listVsphereStoragePolicyCompatiblePools(cmd)

    def create_volume(self, apiclient):
        cmd = create

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="true")
    def test_01_import_storage_policies(self):
        """Test VMWare storage policies
        """

        # Validate the following:
        # 1. Import VMWare storage policies - the command should return non-zero result
        # 2. List current VMWare storage policies - the command should return non-zero result
        # 3. Create service offering with first of the imported policies
        # 4. Create disk offering with first of the imported policies
        # 5. Create VM using the already created service offering
        # 6. Create volume using the already created disk offering
        # 7. Attach this volume to our VM
        # 8. Detach the volume from our VM

        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() != "vmware":
            self.skipTest(
                "VMWare storage policies feature is not supported on %s" %
                self.hypervisor.lower())

        self.debug("Importing VMWare storage policies")
        imported_policies = self.import_vmware_storage_policies(self.apiclient)

        if len(imported_policies) == 0:
            self.skipTest("There are no VMWare storage policies")

        self.debug("Listing VMWare storage policies")
        listed_policies = self.list_storage_policies(self.apiclient)

        self.assertNotEqual(
            len(listed_policies),
            0,
            "Check if list of storage policies is not zero"
        )

        self.assertEqual(
            len(imported_policies),
            len(listed_policies),
            "Check if the number of imported policies is identical to the number of listed policies"
        )

        selected_policy = None
        for imported_policy in imported_policies:
            compatible_pools = self.list_storage_policy_compatible_pools(self.apiclient, imported_policy.id)
            if compatible_pools and len(compatible_pools) > 0:
                selected_policy = imported_policy
                break

        if not selected_policy:
            self.skipTest("There are no compatible storage pools with the imported policies")

        # Create service offering with the first storage policy from the list
        service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"],
            storagepolicy=selected_policy.id
        )
        self.cleanup.append(service_offering)

        # Create disk offering with the first storage policy from the list
        disk_offering = DiskOffering.create(
            self.apiclient,
            self.testdata["disk_offering"],
            storagepolicy=selected_policy.id
        )
        self.cleanup.append(disk_offering)

        l2_network = Network.create(
            self.apiclient,
            self.testdata["l2-network"],
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.cleanup.append(l2_network)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            networkids=l2_network.id,
            serviceofferingid=service_offering.id,
        )
        self.cleanup.append(virtual_machine)

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )
        self.debug("Deployed VM on host: %s" % vms[0].hostid)

        volume = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            diskofferingid=disk_offering.id
           )
        self.cleanup.append(volume)

        list_volume_response = Volume.list(
            self.apiclient,
            id=volume.id
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        return
