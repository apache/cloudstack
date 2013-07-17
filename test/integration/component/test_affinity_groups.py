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
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin import remoteSSHClient
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
            "host_anti_affinity_0": {
                    "name": "TestAffGrp_HA_0",
                    "type": "host anti-affinity",
                },
            "host_anti_affinity_1": {
                    "name": "TestAffGrp_HA_1",
                    "type": "host anti-affinity",
                },
            "virtual_machine" : {
                "hypervisor" : "KVM",
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

        cls.api_client = super(TestCreateAffinityGroup, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls._cleanup = [
            cls.service_offering,
            cls.account,
        ]
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
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            self.services["host_anti_affinity_0"]
        if acc == None:
            acc = self.account.name
        if domainid == None:
            domainid = self.domain.id

        try:
            self.aff_grp = AffinityGroup.create(api_client, aff_grp, acc, domainid)
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    @attr(tags=["simulator", "basic", "advanced"])
    def test_01_admin_create_aff_grp(self):

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.debug("Created Affinity Group: %s" %self.aff_grp.name)

        list_aff_grps = AffinityGroup.list(self.api_client)
        AffinityGroup.delete(self.api_client, list_aff_grps[0].name)
        self.debug("Deleted Affinity Group: %s" %list_aff_grps[0].name)

    @attr(tags=["simulator", "basic", "advanced"])
    def test_02_doadmin_create_aff_grp(self):

        self.new_domain = Domain.create(self.api_client, self.services["new_domain"])
        self.do_admin = Account.create(self.api_client, self.services["new_account"],
                                      admin=True, domainid=self.new_domain.id)
        self.cleanup.append(self.do_admin)
        self.cleanup.append(self.new_domain)

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.do_admin.name, domainid=self.new_domain.id)

        AffinityGroup.delete(self.api_client, name=self.aff_grp.name,
                             account=self.do_admin.name, domainid=self.new_domain.id)
        self.debug("Deleted Affinity Group: %s" %self.aff_grp.name)


    @attr(tags=["simulator", "basic", "advanced"])
    def test_03_user_create_aff_grp(self):

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user.name, domainid=self.domain.id)

        AffinityGroup.delete(self.api_client, name=self.aff_grp.name,
                             account=self.user.name, domainid=self.domain.id)
        self.debug("Deleted Affinity Group: %s" %self.aff_grp.name)


    @attr(tags=["simulator", "basic", "advanced"])
    def test_04_user_create_aff_grp_existing_name(self):

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user.name, domainid=self.domain.id)
        with self.assertRaises(Exception):
            self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user.name, domainid=self.domain.id)

        AffinityGroup.delete(self.api_client, name=self.aff_grp.name,
                             account=self.user.name, domainid=self.domain.id)
        self.debug("Deleted Affinity Group: %s" %self.aff_grp.name)

    @attr(tags=["simulator", "basic", "advanced"])
    def test_05_create_aff_grp_same_name_diff_acc(self):

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user.name, domainid=self.domain.id)

        try:
            self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        except Exception:
            self.debug("Error: Creating affinity group with same name from different account failed.")

        AffinityGroup.delete(self.api_client, name=self.aff_grp.name,
                             account=self.user.name, domainid=self.domain.id)
        self.debug("Deleted Affinity Group: %s" %self.aff_grp.name)

    @attr(tags=["simulator", "basic", "advanced"])
    def test_06_create_aff_grp_nonexisting_type(self):

        self.non_existing_aff_grp = {
                    "name": "TestAffGrp_HA",
                    "type": "Incorrect type",
                }
        with self.assertRaises(Exception):
            self.create_aff_grp(aff_grp=self.non_existing_aff_grp)

class TestListAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestListAffinityGroups, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.__cleanup = [
            cls.service_offering,
            cls.account,
        ]

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
            cleanup_resources(cls.api_client, cls.__cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            self.services["host_anti_affinity_0"]

        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, ag_list):
        self.debug('Creating VM in AffinityGroup=%s' % ag_list[0])
        vm = VirtualMachine.create(
                   self.api_client,
                   self.services["virtual_machine"],
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

    def test_01_list_aff_grps_for_vm(self):
        """
           List affinity group for a vm
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        list_aff_grps = AffinityGroup.list(self.api_client)

        vm, hostid = self.create_vm_in_aff_grps([self.aff_grp[0].name])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm.id)

        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by VM id failed")

        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    def test_02_list_multiple_aff_grps_for_vm(self):
        """
           List multiple affinity groups associated with a vm
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        vm, hostid = self.create_vm_in_aff_grps(aff_grps_names)
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

        for i in aff_grps_names:
            AffinityGroup.delete(self.api_client, i)

    def test_03_list_aff_grps_by_id(self):
        """
           List affinity groups by id
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        print self.aff_grp[0].__dict__
        list_aff_grps = AffinityGroup.list(self.api_client)
        list_aff_grps = AffinityGroup.list(self.api_client, id=list_aff_grps[0].id)
        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by VM id failed")

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    def test_04_list_aff_grps_by_name(self):
        """
            List Affinity Groups by name
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)
        self.assertEqual(list_aff_grps[0].name, self.aff_grp[0].name,
                         "Listing Affinity Group by name failed")

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    def test_05_list_aff_grps_by_non_existing_id(self):
        """
            List Affinity Groups by non-existing id
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           id=1234)
        self.assertEqual(list_aff_grps, None,
                         "Listing Affinity Group by non-existing id succeeded.")

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    def test_06_list_aff_grps_by_non_existing_name(self):
        """
            List Affinity Groups by non-existing name
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name="NonexistingName")
        self.assertEqual(list_aff_grps, None,
                         "Listing Affinity Group by non-existing name succeeded.")

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

class TestDeleteAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestDeleteAffinityGroups, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.__cleanup = [
            cls.service_offering,
            cls.account,
        ]

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
            cleanup_resources(cls.api_client, cls.__cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            self.services["host_anti_affinity_0"]
        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, ag_list):
        self.debug('Creating VM in AffinityGroup=%s' % ag_list[0])
        vm = VirtualMachine.create(
                   self.api_client,
                   self.services["virtual_machine"],
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

    def test_01_delete_aff_grp_by_id(self):
        """
            Delete Afifnity Group by id.
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        AffinityGroup.delete(self.api_client, id=list_aff_grps[0].id)

        AffinityGroup.delete(self.api_client, name=self.aff_grp[1].name)

    def test_02_delete_aff_grp_for_acc(self):
        """
            Delete Afifnity Group for an account.
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"],
                            acc=self.account.name, domainid=self.domain.id)

        AffinityGroup.delete(self.api_client, account=self.account.name,
                             domainid=self.domain.id, name=self.aff_grp[0].name)

        with self.assertRaises(Exception):
            vm, hostid = self.create_vm_in_aff_grps([self.aff_grp[0].name])

        AffinityGroup.delete(self.api_client, account=self.account.name,
                             domainid=self.domain.id, name=self.aff_grp[1].name)

    def test_03_delete_aff_grp_with_vms(self):
        """
            Delete Afifnity Group which has vms in it.
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.account.name, domainid=self.domain.id)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"],
                            acc=self.account.name, domainid=self.domain.id)

        vm, hostid = self.create_vm_in_aff_grps([self.aff_grp[0].name,
                                                 self.aff_grp[1].name])

        AffinityGroup.delete(self.api_client, account=self.account.name,
                             domainid=self.domain.id, name=self.aff_grp[0].name)

        vm_list = list_virtual_machines(self.apiclient,
                                                 id=self.virtual_machine.id)


        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        AffinityGroup.delete(self.api_client, account=self.account.name,
                             domainid=self.domain.id, name=self.aff_grp[0].name)
        AffinityGroup.delete(self.api_client, account=self.account.name,
                             domainid=self.domain.id, name=self.aff_grp[1].name)

    def test_04_delete_aff_grp_with_vms(self):
        """
            Delete Affinity Group which has after updating affinity group for
            vms in it.
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])

        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name])
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name])

        with self.assertRaises(Exception):
            AffinityGroup.delete(self.api_client, name=self.aff_grp[0].name)

        vm1.update_affinity_group(self.api_client, affinitygroupnames=[])

        with self.assertRaises(Exception):
            AffinityGroup.delete(self.api_client, name=self.aff_grp[0].name)

        vm2.update_affinity_group(self.api_client, affinitygroupnames=[])

        AffinityGroup.delete(self.api_client, name=self.aff_grp[0].name)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        AffinityGroup.delete(self.api_client, name=self.aff_grp[1].name)

    def test_05_delete_aff_grp_id(self):
        """
            Delete Affinity Group with id which does not belong to this user
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.createUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        acctType=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity_1"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        # Delete Affinity group belonging to different user by id
        with self.assertRaises(Exception):
            AffinityGroup.delete(userapiclient, name=list_aff_grps.id)

        #Cleanup
        AffinityGroup.delete(self.api_client, name=self.aff_grp[0].name)
        AffinityGroup.delete(userapiclient, name=self.aff_grp[1].name)

    def test_06_delete_aff_grp_name(self):
        """
            Delete Affinity Group by name which does not belong to this user
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.createUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        acctType=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity_1"])

        # Delete Affinity group belonging to different user by name
        with self.assertRaises(Exception):
            AffinityGroup.delete(userapiclient, name=self.aff_grp[0].name)

        #Cleanup
        AffinityGroup.delete(self.api_client, name=self.aff_grp[0].name)
        AffinityGroup.delete(userapiclient, name=self.aff_grp[1].name)

class TestUpdateVMAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestUpdateVMAffinityGroups, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.__cleanup = [
            cls.service_offering,
            cls.account,
        ]

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
            cleanup_resources(cls.api_client, cls.__cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            self.services["host_anti_affinity_0"]
        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, ag_list):
        self.debug('Creating VM in AffinityGroup=%s' % ag_list[0])
        vm = VirtualMachine.create(
                self.api_client,
               self.services["virtual_machine"],
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

    def test_01_update_aff_grp_by_ids(self):
        """
            Update the list of affinityGroups by using affinity groupids

        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])

        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name])
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name])

        vm1.stop(self.api_client)

        list_aff_grps = AffinityGroup.list(self.api_client)

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
        for i in aff_grps_names:
            AffinityGroup.delete(self.api_client, i)

    def test_02_update_aff_grp_by_names(self):
        """
            Update the list of affinityGroups by using affinity groupnames

        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])
        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name])
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name])

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
        for i in aff_grps_names:
            AffinityGroup.delete(self.api_client, i)

    def test_03_update_aff_grp_for_vm_with_no_aff_grp(self):
        """
            Update the list of affinityGroups for vm which is not associated
            with any affinity groups.

        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])
        vm1, hostid1 = self.create_vm_in_aff_grps([])
        vm2, hostid2 = self.create_vm_in_aff_grps([self.aff_grp[0].name])

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
        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        for i in aff_grps_names:
            AffinityGroup.delete(self.api_client, i)

    def test_04_update_aff_grp_remove_all(self):
        """
            Update the list of Affinity Groups to empty list
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])
        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name])

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        vm1.stop(self.api_client)

        vm1.update_affinity_group(self.api_client,
                                  affinitygroupnames=[])

        vm1.start(self.api_client)
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           virtualmachineid=vm1.id)
        self.assertEqual(list_aff_grps, [], "The affinity groups list is not empyty")

        vm1.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        for i in aff_grps_names:
            AffinityGroup.delete(self.api_client, i)

    def test_05_update_aff_grp_on_running_vm(self):
        """
            Update the list of Affinity Groups on running vm
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])
        vm1, hostid1 = self.create_vm_in_aff_grps([self.aff_grp[0].name])

        aff_grps_names = [self.aff_grp[0].name, self.aff_grp[1].name]
        with self.assertRaises(Exception):
            vm1.update_affinity_group(self.api_client, affinitygroupnames=[])

        vm1.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        for i in aff_grps_names:
            AffinityGroup.delete(self.api_client, i)

class TestDeployVMAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestDeployVMAffinityGroups, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.__cleanup = [
            cls.service_offering,
            cls.account,
        ]

        # Create multiple Affinity Groups
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
            cleanup_resources(cls.api_client, cls.__cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            self.services["host_anti_affinity_0"]

        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=None, ag_ids=None):

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

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_01_deploy_vm_without_aff_grp(self):
        """
            Deploy VM without affinity group
        """
        vm1, hostid1 = self.create_vm_in_aff_grps()

        vm1.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_02_deploy_vm_by_aff_grp_name(self):
        """
            Deploy VM by aff grp name
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name])

        vm1.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_03_deploy_vm_by_aff_grp_id(self):
        """
            Deploy VM by aff grp id
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_ids=[list_aff_grps[0].id])

        vm1.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_04_deploy_vm_anti_affinity_group(self):
        """
        test DeployVM in anti-affinity groups

        deploy VM1 and VM2 in the same host-anti-affinity groups
        Verify that the vms are deployed on separate hosts
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name])
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name])

        self.assertNotEqual(hostid1, hostid2,
            msg="Both VMs of affinity group %s are on the same host"
            % self.aff_grp[0].name)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_05_deploy_vm_by_id(self):
        """
            Deploy vms by affinity group id
        """
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_ids=[list_aff_grps[0].id])
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_ids=[list_aff_grps[0].id])

        self.assertNotEqual(hostid1, hostid2,
            msg="Both VMs of affinity group %s are on the same host"
            % self.aff_grp[0].name)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_06_deploy_vm_aff_grp_of_other_user_by_name(self):
        """
            Deploy vm in affinity group of another user by name
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.createUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        acctType=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity_1"])

        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(api_client=userapiclient,
                                                  ag_list=[self.aff_grp[0].name])


        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)
        AffinityGroup.delete(userapiclient, self.aff_grp[1].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_07_deploy_vm_aff_grp_of_other_user_by_id(self):
        """
            Deploy vm in affinity group of another user by id
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user1.name,
                            domainid=self.domain.id)

        self.user2 = Account.create(self.apiclient, self.services["new_account1"])
        self.cleanup.append(self.user2)

        userapiclient = self.testClient.createUserApiClient(
                                        UserName=self.user2.name,
                                        DomainName=self.user2.domain,
                                        acctType=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity_1"])

        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        # Deploy VM in Affinity group belonging to different user by id
        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(api_client=userapiclient,
                                                  ag_ids=[list_aff_grps[0].id])

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)
        AffinityGroup.delete(userapiclient, self.aff_grp[1].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_08_deploy_vm_multiple_aff_grps(self):
        """
            Deploy vm in multiple affinity groups
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])
        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name,
                                                   self.aff_grp[1].name])

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
        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)
        AffinityGroup.delete(self.api_client, self.aff_grp[1].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_09_deploy_vm_multiple_aff_grps(self):
        """
            Deploy multiple vms in multiple affinity groups
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_1"])
        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name,
                                                   self.aff_grp[1].name])
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name,
                                                   self.aff_grp[1].name])

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

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)
        AffinityGroup.delete(self.api_client, self.aff_grp[1].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_10_deploy_vm_by_aff_grp_name_and_id(self):
        """
            Deploy VM by aff grp name and id
        """

        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"])
        list_aff_grps = AffinityGroup.list(self.api_client,
                                           name=self.aff_grp[0].name)

        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name],
                                                  ag_ids=[list_aff_grps[0].id])

        AffinityGroup.delete(self.api_client, self.aff_grp[0].name)

class TestAffinityGroupsAdminUser(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestAffinityGroupsAdminUser, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.__cleanup = [
            cls.service_offering,
            cls.account,
        ]

        # Create multiple Affinity Groups
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
            cleanup_resources(cls.api_client, cls.__cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None,
                  acc=None, domainid=None):

        if api_client == None:
            api_client = self.api_client
        if aff_grp == None:
            self.services["host_anti_affinity_0"]

        try:
            self.aff_grp.append(AffinityGroup.create(api_client,
                                                     aff_grp, acc, domainid))
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=None, ag_ids=None):

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

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_01_deploy_vm_another_user(self):
        """
            Deploy vm in Affinity Group belonging to regular user
        """
        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.createUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        acctType=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity_0"])

        with self.assertRaises(Exception):
            vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[self.aff_grp[0].name])

        AffinityGroup.delete(userapiclient, self.aff_grp[0].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_02_create_aff_grp_user(self):
        """
            Create Affinity Group forregular user
        """

        self.user = Account.create(self.api_client, self.services["new_account"],
                                  domainid=self.domain.id)

        self.cleanup.append(self.user)
        self.create_aff_grp(aff_grp=self.services["host_anti_affinity_0"],
                            acc=self.user.name, domainid=self.domain.id)

        AffinityGroup.delete(self.api_client, name=self.aff_grp[0].name,
                             account=self.user.name, domainid=self.domain.id)
        self.debug("Deleted Affinity Group: %s" %self.aff_grp[0].name)

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_03_list_aff_grp_all_users(self):
        """
            List Affinity Groups for all the users
        """

        self.user1 = Account.create(self.api_client,
                                       self.services["new_account"])

        self.cleanup.append(self.user1)
        userapiclient = self.testClient.createUserApiClient(
                                        UserName=self.user1.name,
                                        DomainName=self.user1.domain,
                                        acctType=0)

        self.create_aff_grp(api_client=userapiclient,
                            aff_grp=self.services["host_anti_affinity_0"])

        list_aff_grps = AffinityGroup.list(self.api_client)
        print list_aff_grps
        self.assertNotEqual(list_aff_grps, [], "Admin not able to list Affinity "
                         "Groups of users")
        AffinityGroup.delete(userapiclient, self.aff_grp[0].name)
