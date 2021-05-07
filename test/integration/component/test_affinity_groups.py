#!/usr/bin/env python
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
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import deleteAffinityGroup
from marvin.lib.utils import (cleanup_resources,
                              random_gen)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             AffinityGroup,
                             Domain)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_virtual_machines,
                               wait_for_cleanup)
from nose.plugins.attrib import attr

class Services:
    """Test Account Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "account": {
                "email": "newtest@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                # in MHz
                "memory": 64,
                # In MBs
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "host_anti_affinity": {
                    "name": "",
                    "type": "host anti-affinity",
                },
            "virtual_machine" : {
                
            },
            "new_domain": {
                "name": "New Domain",
            },
            "new_account": {
                "email": "domain@test.com",
                "firstname": "Domain",
                "lastname": "Admin",
                "username": "do_admin",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "new_account1": {
                "email": "user@test.com",
                "firstname": "User",
                "lastname": "User",
                "username": "user",
                # Random characters are appended for unique
                # username
                "password": "password",
            },

        }

class TestCreateAffinityGroup(cloudstackTestCase):
    """
    Test various scenarios for Create Affinity Group API
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreateAffinityGroup, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name
        cls.services["domainid"] = cls.domain.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestCreateAffinityGroup, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None, aff_grp_name=None):

        if not api_client:
            api_client = self.api_client
        if not aff_grp:
            aff_grp = self.services["host_anti_affinity"]
        if not acc:
            acc = self.account.name
        if not domainid:
            domainid = self.domain.id

        if aff_grp_name is None:
            aff_grp["name"] = "aff_grp_" + random_gen(size=6)
        else:
            aff_grp["name"] = aff_grp_name

        try:
            return AffinityGroup.create(api_client, aff_grp, acc, domainid)
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_01_admin_create_aff_grp(self):
        """
        Test create affinity group as admin
        @return:
        """

        aff_grp = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
            acc=self.account.name, domainid=self.account.domainid)
        self.debug("Created Affinity Group: %s" % aff_grp.name)
        list_aff_grps = AffinityGroup.list(self.api_client, id=aff_grp.id)
        self.assertTrue(isinstance(list_aff_grps, list) and len(list_aff_grps) > 0)
        self.assertTrue(list_aff_grps[0].id == aff_grp.id)
        self.cleanup.append(aff_grp)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_02_doadmin_create_aff_grp(self):
        """
        Test create affinity group as domain admin
        @return:
        """

        self.new_domain = Domain.create(self.api_client, self.services["new_domain"])
        self.do_admin = Account.create(self.api_client, self.services["new_account"],
                                      admin=True, domainid=self.new_domain.id)
        self.cleanup.append(self.do_admin)
        self.cleanup.append(self.new_domain)

        domainapiclient = self.testClient.getUserApiClient(self.do_admin.name, self.new_domain.name, 2)

        aff_grp = self.create_aff_grp(api_client=domainapiclient, aff_grp=self.services["host_anti_affinity"],
                                            acc=self.do_admin.name, domainid=self.new_domain.id)
        aff_grp.delete(domainapiclient)

    #@attr(tags=["simulator", "basic", "advanced"])
    @attr(tags=["vogxn", "simulator", "basic", "advanced"], required_hardware="false")
    def test_03_user_create_aff_grp(self):
        """
        Test create affinity group as user
        @return:
        """

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)
        self.cleanup.append(self.user)

        userapiclient = self.testClient.getUserApiClient(self.user.name, self.domain.name)
        aff_grp = self.create_aff_grp(api_client=userapiclient, aff_grp=self.services["host_anti_affinity"],
                            acc=self.user.name, domainid=self.domain.id)
        aff_grp.delete(userapiclient)


    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_04_user_create_aff_grp_existing_name(self):
        """
        Test create affinity group that exists (same name)
        @return:
        """

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        aff_grp = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user.name, domainid=self.domain.id)
        with self.assertRaises(Exception):
            self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user.name, domainid=self.domain.id,
                            aff_grp_name = aff_grp.name)

        self.debug("Deleted Affinity Group: %s" %aff_grp.name)
        aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_05_create_aff_grp_same_name_diff_acc(self):
        """
        Test create affinity group with existing name but within different account
        @return:
        """

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        aff_grp = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user.name, domainid=self.domain.id)

        try:
            self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        except Exception:
            self.debug("Error: Creating affinity group with same name from different account failed.")

        self.debug("Deleted Affinity Group: %s" %aff_grp.name)
        aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_06_create_aff_grp_nonexisting_type(self):
        """
        Test create affinity group of non-existing type
        @return:
        """

        self.non_existing_aff_grp = {
                    "name": "TestAffGrp_HA",
                    "type": "Incorrect type",
                }
        with self.assertRaises(Exception):
            self.create_aff_grp(aff_grp=self.non_existing_aff_grp)

class TestListAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestListAffinityGroups, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name
        cls.services["domainid"] = cls.domain.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        # Create multiple Affinity Groups
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.aff_grp = []
        self.cleanup = []

    def tearDown(self):
        try:
            self.api_client = super(TestListAffinityGroups, self).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestListAffinityGroups, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            aff_grp = self.services["host_anti_affinity"]

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            aff_grp = AffinityGroup.create(api_client,
                                           aff_grp, acc, domainid)
            self.aff_grp.append(aff_grp)
            return aff_grp
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, ag_list, account_name=None, domain_id=None):
        if account_name == None:
            account_name = "admin"
        if domain_id == None:
            domain_id = self.domain.id

        self.debug('Creating VM in AffinityGroup=%s' % ag_list[0])
        vm = VirtualMachine.create(
                   self.api_client,
                   self.services["virtual_machine"],
                   accountid=account_name,
                   domainid=domain_id,
                   templateid=self.template.id,
                   serviceofferingid=self.service_offering.id,
                   affinitygroupnames=ag_list
                )
        self.debug('Created VM=%s in Affinity Group=%s' %
               (vm.id, ag_list[0]))

        list_vm = list_virtual_machines(self.api_client, id=vm.id)
        self.assertEqual(isinstance(list_vm, list), True,
                         "Check list response returns a valid list")
        self.assertNotEqual(len(list_vm),0,
                            "Check VM available in List Virtual Machines")
        vm_response = list_vm[0]
        self.assertEqual(vm_response.state, 'Running',
                         msg="VM is not in Running state")
        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_01_list_aff_grps_for_vm(self):
        """
           List affinity group for a vm
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        list_aff_grps = AffinityGroup.list(self.api_client)

        vm, hostid = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm.id)

        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by VM id failed")

        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_02_list_multiple_aff_grps_for_vm(self):
        """
           List multiple affinity groups associated with a vm
        """

        aff_grp_01 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        aff_grp_02 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        vm, hostid = self.create_vm_in_aff_grps(aff_grps_names, account_name=self.account.name, domain_id=self.domain.id)
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm.id)

        list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

        aff_grps_names.sort()
        list_aff_grps_names.sort()
        self.assertEqual(aff_grps_names, list_aff_grps_names,
                         "One of the Affinity Groups is missing %s"
                         %list_aff_grps_names)

        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        aff_grp_01.delete(self.api_client)
        aff_grp_02.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_03_list_aff_grps_by_id(self):
        """
           List affinity groups by id
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        print(self.aff_grp[0].__dict__)
        list_aff_grps = AffinityGroup.list(self.api_client)
        list_aff_grps = AffinityGroup.list(self.api_client, id=list_aff_grps[0].id)
        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by VM id failed")

        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_04_list_aff_grps_by_name(self):
        """
            List Affinity Groups by name
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)
        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by name failed")

        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_05_list_aff_grps_by_non_existing_id(self):
        """
            List Affinity Groups by non-existing id
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           id=1234)
        self.assertEqual(list_aff_grps, None,
                         "Listing Affinity Group by non-existing id succeeded.")

        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_06_list_aff_grps_by_non_existing_name(self):
        """
            List Affinity Groups by non-existing name
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name="NonexistingName")
        self.assertEqual(list_aff_grps, None,
                         "Listing Affinity Group by non-existing name succeeded.")

        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_07_list_all_vms_in_aff_grp(self):
        """
           List affinity group should list all for a vms associated with that group
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm, hostid = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)
        list_aff_grps = AffinityGroup.list(self.api_client, id=self.aff_grp[0].id)

        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by id failed")

        self.assertEqual(list_aff_grps[0].virtualmachineIds[0], vm.id,
        "List affinity group response.virtualmachineIds for group: %s doesn't contain hostid : %s associated with the group"
            %(self.aff_grp[0].name, vm.id)
            )


        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        self.aff_grp[0].delete(self.api_client)

class TestDeleteAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeleteAffinityGroups, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name
        cls.services["domainid"] = cls.domain.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        # Create multiple Affinity Groups
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.aff_grp = []
        self.cleanup = []

    def tearDown(self):
        try:
            self.api_client = super(TestDeleteAffinityGroups,self).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def tearDownClass(cls):

        try:
            cls.api_client = super(TestDeleteAffinityGroups, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            aff_grp = self.services["host_anti_affinity"]

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            return AffinityGroup.create(api_client, aff_grp, acc, domainid)
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, ag_list, account_name=None, domain_id=None):
        if account_name == None:
            account_name = "admin"
        if domain_id == None:
            domain_id = self.domain.id
        self.debug('Creating VM in AffinityGroup=%s' % ag_list[0])
        vm = VirtualMachine.create(
                   self.api_client,
                   self.services["virtual_machine"],
                   accountid=account_name,
                   domainid=domain_id,
                   templateid=self.template.id,
                   serviceofferingid=self.service_offering.id,
                   affinitygroupnames=ag_list
                )
        self.debug('Created VM=%s in Affinity Group=%s' %
               (vm.id, ag_list[0]))

        list_vm = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(isinstance(list_vm, list), True,
                         "Check list response returns a valid list")
        self.assertNotEqual(len(list_vm),0,
                            "Check VM available in Delete Virtual Machines")

        vm_response = list_vm[0]
        self.assertEqual(vm_response.state, 'Running',
                         msg="VM is not in Running state")

        return vm, vm_response.hostid

    def delete_aff_group(self, apiclient, **kwargs):
        cmd = deleteAffinityGroup.deleteAffinityGroupCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return apiclient.deleteAffinityGroup(cmd)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_01_delete_aff_grp_by_name(self):
        """
            Delete Affinity Group by name
        """

        aff_0 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        AffinityGroup.list(self.api_client, name=aff_0.name)
        self.delete_aff_group(self.api_client, name=aff_0.name)
        self.assertTrue(AffinityGroup.list(self.api_client, name=aff_0.name) is None)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_02_delete_aff_grp_for_acc(self):
        """
            Delete Affinity Group as admin for an account
        """

        aff_0 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.account.name, domainid=self.domain.id)
        aff_1 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.account.name, domainid=self.domain.id)

        aff_0.delete(self.api_client)
        with self.assertRaises(Exception):
            self.create_vm_in_aff_grps([aff_0.name], account_name=self.account.name, domain_id=self.domain.id)
        aff_1.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_03_delete_aff_grp_with_vms(self):
        """
            Delete Affinity Group which has vms in it
        """

        aff_0 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        aff_1 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm, hostid = self.create_vm_in_aff_grps([aff_0.name, aff_1.name], account_name=self.account.name, domain_id=self.domain.id)
        aff_0.delete(self.api_client)
        vm_list = list_virtual_machines(self.apiclient, id=vm.id)
        self.assertTrue(vm_list is not None)
        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        aff_1.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_05_delete_aff_grp_id(self):
        """
            Delete Affinity Group with id which does not belong to this user
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        aff_0 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        type=0)

        aff_1 = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=aff_0.name)

        # Delete Affinity group belonging to different user by id
        with self.assertRaises(Exception):
            self.delete_aff_group(userapiclient, name=list_aff_grps.id)

        #Cleanup
        aff_0.delete(self.api_client)
        aff_1.delete(userapiclient)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_06_delete_aff_grp_name(self):
        """
            Delete Affinity Group by name which does not belong to this user
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        aff_0 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        type=0)

        aff_1 = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=aff_0.name)

        # Delete Affinity group belonging to different user by name
        with self.assertRaises(Exception):
            self.delete_aff_group(userapiclient, name=list_aff_grps.name)

        #Cleanup
        aff_0.delete(self.api_client)
        aff_1.delete(userapiclient)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_08_delete_aff_grp_by_id(self):
        """
            Delete Affinity Group by id.
        """

        aff_grp_1 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        aff_grp_2 = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])

        aff_grp_1.delete(self.api_client)
        aff_grp_2.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_09_delete_aff_grp_root_admin(self):
        """
            Root admin should be able to delete affinity group of other users
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        user1apiclient = self.testClient.getUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        type=0)

        aff_grp = self.create_aff_grp(api_client=user1apiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client)
        self.assertNotEqual(list_aff_grps, [], "Admin not able to list Affinity "
                         "Groups of users")

        aff_grp.delete(self.api_client)

class TestUpdateVMAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestUpdateVMAffinityGroups, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name
        cls.services["domainid"] = cls.domain.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        # Create multiple Affinity Groups
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.aff_grp = []
        self.cleanup = []

    def tearDown(self):
        try:
            self.api_client = super(TestUpdateVMAffinityGroups,self).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def tearDownClass(cls):

        try:
            cls.api_client = super(TestUpdateVMAffinityGroups, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            aff_grp = self.services["host_anti_affinity"]

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, ag_list, account_name=None, domain_id=None):
        if account_name == None:
            account_name = "admin"
        if domain_id == None:
            domain_id = self.domain.id
        self.debug('Creating VM in AffinityGroup=%s' % ag_list)

        vm = VirtualMachine.create(
               self.api_client,
               self.services["virtual_machine"],
               accountid=account_name,
               domainid=domain_id,
               templateid=self.template.id,
               serviceofferingid=self.service_offering.id,
               affinitygroupnames=ag_list
            )
        self.debug('Created VM=%s in Affinity Group=%s' %
                   (vm.id, ag_list))

        list_vm = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(isinstance(list_vm, list), True,
                         "Check list response returns a valid list")
        self.assertNotEqual(len(list_vm),0,
                            "Check VM available in Delete Virtual Machines")

        vm_response = list_vm[0]
        self.assertEqual(vm_response.state, 'Running',
                         msg="VM is not in Running state")

        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_01_update_aff_grp_by_ids(self):
        """
            Update the list of affinityGroups by using affinity groupids

        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        vm1.stop(self.api_client)

        list_aff_grps = AffinityGroup.list(self.api_client, account=self.account.name, domainid=self.domain.id)

        self.assertEqual(len(list_aff_grps), 2 , "2 affinity groups should be present")

        vm1.update_affinity_group(self.api_client,
                                  affinitygroupids=[list_aff_grps[0].id,
                                                    list_aff_grps[1].id])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm1.id)

        list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        aff_grps_names.sort()
        list_aff_grps_names.sort()
        self.assertEqual(aff_grps_names, list_aff_grps_names,
                         "One of the Affinity Groups is missing %s"
                         %list_aff_grps_names)

        vm1.start(self.api_client)

        vm_status = VirtualMachine.list(self.api_client, id=vm1.id)
        self.assertNotEqual(vm_status[0].hostid, hostid2, "The virtual machine "
                         "started on host %s violating the host anti-affinity"
                         "rule" %vm_status[0].hostid)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        for aff_grp in self.aff_grp:
            aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_02_update_aff_grp_by_names(self):
        """
            Update the list of affinityGroups by using affinity groupnames

        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        vm1.stop(self.api_client)

        vm1.update_affinity_group(self.api_client,
                                  affinitygroupnames=[self.aff_grp[0].name,
                                                    self.aff_grp[1].name])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm1.id)

        list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        aff_grps_names.sort()
        list_aff_grps_names.sort()
        self.assertEqual(aff_grps_names, list_aff_grps_names,
                         "One of the Affinity Groups is missing %s"
                         %list_aff_grps_names)

        vm1.start(self.api_client)

        vm_status = VirtualMachine.list(self.api_client, id=vm1.id)
        self.assertNotEqual(vm_status[0].hostid, hostid2, "The virtual machine "
                         "started on host %s violating the host anti-affinity"
                         "rule" %vm_status[0].hostid)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        for aff_grp in self.aff_grp:
            aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_03_update_aff_grp_for_vm_with_no_aff_grp(self):
        """
            Update the list of affinityGroups for vm which is not associated
            with any affinity groups.

        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        vm1, hostid1 = self.create_vm_in_aff_grps([], account_name=self.account.name, domain_id=self.domain.id)
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        vm1.stop(self.api_client)

        vm1.update_affinity_group(self.api_client,
                                  affinitygroupnames=[self.aff_grp[0].name])

        vm1.start(self.api_client)

        vm_status = VirtualMachine.list(self.api_client, id=vm1.id)
        self.assertNotEqual(vm_status[0].hostid, hostid2, "The virtual machine "
                         "started on host %s violating the host anti-affinity"
                         "rule" %vm_status[0].hostid)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        aff_grps = [self.aff_grp[0], self.aff_grp[1]]
        for aff_grp in aff_grps:
            aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost", "NotRun"])
    def test_04_update_aff_grp_remove_all(self):
        """
            Update the list of Affinity Groups to empty list
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        aff_grps = [self.aff_grp[0], self.aff_grp[1]]
        vm1.stop(self.api_client)

        vm1.update_affinity_group(self.api_client, affinitygroupids = [])

        vm1.start(self.api_client)
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm1.id)
        self.assertEqual(list_aff_grps, [], "The affinity groups list is not empyty")

        vm1.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        for aff_grp in aff_grps:
            aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_05_update_aff_grp_on_running_vm(self):
        """
            Update the list of Affinity Groups on running vm
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        aff_grps = [self.aff_grp[0], self.aff_grp[1]]
        with self.assertRaises(Exception):
            vm1.update_affinity_group(self.api_client, affinitygroupnames=[])

        vm1.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        for aff_grp in aff_grps:
            aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost", "NotRun"])
    def test_06_update_aff_grp_invalid_args(self):
        """
            Update the list of Affinity Groups with either both args or none
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"])
        vm1, hostid1 = self.create_vm_in_aff_grps([], account_name=self.account.name, domain_id=self.domain.id)

        aff_grps = [self.aff_grp[0], self.aff_grp[1]]
        vm1.stop(self.api_client)

        with self.assertRaises(Exception):
            vm1.update_affinity_group(self.api_client)

        with self.assertRaises(Exception):
            vm1.update_affinity_group(self.api_client, affinitygroupids=[self.aff_grp[0].id], affinitygroupnames=[self.aff_grp[1].name])

        vm1.update_affinity_group(self.api_client, affinitygroupids=[])

        vm1.delete(self.api_client)
        # Can cleanup affinity groups since none are set on the VM
        for aff_grp in aff_grps:
            aff_grp.delete(self.api_client)

class TestDeployVMAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVMAffinityGroups, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name
        cls.services["domainid"] = cls.domain.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.aff_grp = []
        self.cleanup = []

    def tearDown(self):
        try:
            self.api_client = super(TestDeployVMAffinityGroups,self).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def tearDownClass(cls):

        try:
            cls.api_client = super(TestDeployVMAffinityGroups, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            aff_grp = self.services["host_anti_affinity"]

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=None, ag_ids=None, account_name=None, domain_id=None):
        if account_name == None:
            account_name = "admin"
        if domain_id == None:
            domain_id = self.domain.id
        if api_client == None:
            api_client = self.api_client
        self.debug('Creating VM in AffinityGroup=%s' % ag_list)
        vm = VirtualMachine.create(
               api_client,
               self.services["virtual_machine"],
               accountid=account_name,
               domainid=domain_id,
               templateid=self.template.id,
               serviceofferingid=self.service_offering.id,
               affinitygroupnames=ag_list,
               affinitygroupids=ag_ids
            )
        self.debug('Created VM=%s in Affinity Group=%s' %
                    (vm.id, ag_list))

        list_vm = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(isinstance(list_vm, list), True,
                         "Check list response returns a valid list")
        self.assertNotEqual(len(list_vm),0,
                            "Check VM available in Delete Virtual Machines")

        vm_response = list_vm[0]
        self.assertEqual(vm_response.state, 'Running',
                         msg="VM is not in Running state")

        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_01_deploy_vm_without_aff_grp(self):
        """
            Deploy VM without affinity group
        """
        vm1, hostid1 = self.create_vm_in_aff_grps(account_name=self.account.name, domain_id=self.domain.id)

        vm1.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_02_deploy_vm_by_aff_grp_name(self):
        """
            Deploy VM by aff grp name
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        vm1.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_03_deploy_vm_by_aff_grp_id(self):
        """
            Deploy VM by aff grp id
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name, account=self.account.name, domainid=self.domain.id)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_ids=[list_aff_grps[0].id], account_name=self.account.name, domain_id=self.domain.id)

        vm1.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_04_deploy_vm_anti_affinity_group(self):
        """
        test DeployVM in anti-affinity groups

        deploy VM1 and VM2 in the same host-anti-affinity groups
        Verify that the vms are deployed on separate hosts
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)

        self.assertNotEqual(hostid1, hostid2,
            msg="Both VMs of affinity group %s are on the same host"
            % self.aff_grp[0].name)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_05_deploy_vm_by_id(self):
        """
            Deploy vms by affinity group id
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name, acc=self.account.name, domainid=self.domain.id)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_ids=[list_aff_grps[0].id], account_name=self.account.name, domain_id=self.domain.id)
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_ids=[list_aff_grps[0].id], account_name=self.account.name, domain_id=self.domain.id)

        self.assertNotEqual(hostid1, hostid2,
            msg="Both VMs of affinity group %s are on the same host"
            % self.aff_grp[0].name)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.aff_grp[0].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_06_deploy_vm_aff_grp_of_other_user_by_name(self):
        """
            Deploy vm in affinity group of another user by name
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        type=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(api_client=userapiclient,
                                                  ag_list=[self.aff_grp[0].name], account_name=self.account.name, domain_id=self.domain.id)


        self.aff_grp[0].delete(self.api_client)
        self.aff_grp[1].delete(userapiclient)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_07_deploy_vm_aff_grp_of_other_user_by_id(self):
        """
            Deploy vm in affinity group of another user by id
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        type=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        # Deploy VM in Affinity group belonging to different user by id
        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(api_client=userapiclient,
                                                  ag_ids=[list_aff_grps[0].id], account_name=self.account.name, domain_id=self.domain.id)

        self.aff_grp[0].delete(self.api_client)
        self.aff_grp[1].delete(userapiclient)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_08_deploy_vm_multiple_aff_grps(self):
        """
            Deploy vm in multiple affinity groups
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name,
                                                   self.aff_grp[1].name], account_name=self.account.name, domain_id=self.domain.id)

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm1.id)

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

        aff_grps_names.sort()
        list_aff_grps_names.sort()
        self.assertEqual(aff_grps_names, list_aff_grps_names,
                         "One of the Affinity Groups is missing %s"
                         %list_aff_grps_names)

        vm1.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.aff_grp[0].delete(self.api_client)
        self.aff_grp[1].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_09_deploy_vm_multiple_aff_grps(self):
        """
            Deploy multiple vms in multiple affinity groups
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name,
                                                   self.aff_grp[1].name], account_name=self.account.name, domain_id=self.domain.id)
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name,
                                                   self.aff_grp[1].name], account_name=self.account.name, domain_id=self.domain.id)

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        aff_grps_names.sort()

        for vm in [vm1, vm2]:
            list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm.id)

            list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

            list_aff_grps_names.sort()
            self.assertEqual(aff_grps_names, list_aff_grps_names,
                         "One of the Affinity Groups is missing %s"
                         %list_aff_grps_names)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        self.aff_grp[0].delete(self.api_client)
        self.aff_grp[1].delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_10_deploy_vm_by_aff_grp_name_and_id(self):
        """
            Deploy VM by aff grp name and id
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name],
                                                  ag_ids=[list_aff_grps[0].id], account_name=self.account.name, domain_id=self.domain.id)

        self.aff_grp[0].delete(self.api_client)

class TestAffinityGroupsAdminUser(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestAffinityGroupsAdminUser, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name
        cls.services["domainid"] = cls.domain.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.aff_grp = []
        self.cleanup = []

    def tearDown(self):
        try:
            self.api_client = super(TestAffinityGroupsAdminUser,self).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def tearDownClass(cls):

        try:
            cls.api_client = super(TestAffinityGroupsAdminUser, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            aff_grp = self.services["host_anti_affinity"]

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            return AffinityGroup.create(api_client, aff_grp, acc, domainid)
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=None, ag_ids=None, account_name=None, domain_id=None):
        if account_name == None:
            account_name = "admin"
        if domain_id == None:
            domain_id = self.domain.id
        if api_client == None:
            api_client = self.api_client
        self.debug('Creating VM in AffinityGroup=%s' % ag_list)
        vm = VirtualMachine.create(
               api_client,
               self.services["virtual_machine"],
               templateid=self.template.id,
               serviceofferingid=self.service_offering.id,
               affinitygroupnames=ag_list,
               affinitygroupids=ag_ids
            )
        self.debug('Created VM=%s in Affinity Group=%s' %
                   (vm.id, ag_list))

        list_vm = list_virtual_machines(self.api_client, id=vm.id)

        self.assertEqual(isinstance(list_vm, list), True,
                         "Check list response returns a valid list")
        self.assertNotEqual(len(list_vm),0,
                            "Check VM available in Delete Virtual Machines")

        vm_response = list_vm[0]
        self.assertEqual(vm_response.state, 'Running',
                         msg="VM is not in Running state")

        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_01_deploy_vm_another_user(self):
        """
            Deploy vm as Admin in Affinity Group belonging to regular user (should fail)
        """
        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        type=0)

        aff_grp = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        with self.assertRaises(Exception):
            self.create_vm_in_aff_grps(api_client=self.apiclient, ag_list=[self.aff_grp[0].name])

        aff_grp.delete(userapiclient)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])

    def test_02_create_aff_grp_user(self):
        """
            Create Affinity Group as admin for regular user
        """

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        aff_grp = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"],
                            acc=self.user.name, domainid=self.domain.id)
        aff_grp.delete(self.apiclient)


    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_03_list_aff_grp_all_users(self):
        """
            List Affinity Groups as admin for all the users
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        type=0)

        aff_grp = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client)
        self.assertNotEqual(list_aff_grps, [], "Admin not able to list Affinity "
                         "Groups of users")
        aff_grp.delete(userapiclient)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_04_list_all_admin_aff_grp(self):
        """
            List Affinity Groups belonging to admin user
        """

        aff_grp1 = self.create_aff_grp(api_client=self.api_client,
                            aff_grp=self.services["host_anti_affinity"])
        aff_grp2 = self.create_aff_grp(api_client=self.api_client,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client)

        self.assertNotEqual(list_aff_grps, [], "Admin not able to list Affinity "
                         "Groups belonging to him")
        grp_names = [aff_grp1.name, aff_grp2.name]
        list_names = []
        for grp in list_aff_grps:
            list_names.append(grp.name)

        for name in grp_names:
            self.assertTrue(name in list_names,
                        "Listing affinity groups belonging to Admin didn't return group %s" %(name))

        aff_grp1.delete(self.api_client)
        aff_grp2.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_05_list_all_users_aff_grp(self):
        """
            List Affinity Groups belonging to regular user passing account id and domain id
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        type=0)

        aff_grp1 = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])
        aff_grp2 = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(self.api_client, accountId=self.user1.id, domainId=self.user1.domainid)

        self.assertNotEqual(list_aff_grps, [], "Admin not able to list Affinity "
                         "Groups of users")
        grp_names = [aff_grp1.name, aff_grp2.name]
        list_names = []
        for grp in list_aff_grps:
            list_names.append(grp.name)

        for name in grp_names:
            self.assertTrue(name in list_names,
                        "Missing Group %s from listing" %(name))

        aff_grp1.delete(self.api_client)
        aff_grp2.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_06_list_all_users_aff_grp_by_id(self):
        """
            List Affinity Groups belonging to regular user passing group id
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        type=0)

        aff_grp = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(userapiclient)
        aff_grp_by_id = AffinityGroup.list(self.api_client, id=list_aff_grps[0].id)

        self.assertNotEqual(aff_grp_by_id, [], "Admin not able to list Affinity "
                         "Groups of users")
        self.assertEqual(len(aff_grp_by_id), 1, "%s affinity groups listed by admin with id %s. Expected 1"
                                                %(len(aff_grp_by_id), list_aff_grps[0].id))
        self.assertEqual(aff_grp_by_id[0].name, aff_grp.name,
                "Incorrect name returned when listing user affinity groups as admin by id Expected : %s Got: %s"
                %(aff_grp.name, aff_grp_by_id[0].name )
            )

        aff_grp.delete(self.api_client)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_07_delete_aff_grp_of_other_user(self):
        """
            Delete Affinity Group belonging to regular user
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.getUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        type=0)

        aff_grp = self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity"])

        list_aff_grps = AffinityGroup.list(userapiclient)
        aff_grp_by_id = AffinityGroup.list(self.api_client, id=list_aff_grps[0].id)

        self.assertNotEqual(aff_grp_by_id, [], "Admin not able to list Affinity "
                         "Groups of users")
        self.assertEqual(len(aff_grp_by_id), 1, "%s affinity groups listed by admin with id %s. Expected 1"
                                                %(len(aff_grp_by_id), list_aff_grps[0].id))
        self.assertEqual(aff_grp_by_id[0].name, aff_grp.name,
                "Incorrect name returned when listing user affinity groups as admin by id Expected : %s Got: %s"
                %(aff_grp.name, aff_grp_by_id[0].name )
            )

        aff_grp.delete(self.api_client)
