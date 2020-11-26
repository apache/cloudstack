#!/usr/bin/env python
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
# 
#    http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import deleteAffinityGroup
from marvin.lib.utils import (cleanup_resources,
                        random_gen)
from marvin.lib.base import (Account,
                        Project,
                        ServiceOffering,
                        VirtualMachine,
                        AffinityGroup,
                        Domain)
from marvin.lib.common import (get_zone,
                         get_domain,
                         get_template,
                         list_hosts,
                         list_virtual_machines,
                         wait_for_cleanup)
from nose.plugins.attrib import attr

class Services:
    """Test Account Services
    """

    def __init__(self):
       self.services = {
          "domain": {
             "name": "NonRootDomain"
          },
          "domain_admin_account": {
             "email": "newtest@test.com",
             "firstname": "Test",
             "lastname": "User",
             "username": "doadmintest",
             "password": "password"
          },
          "account": {
             "email": "newtest@test.com",
             "firstname": "Test",
             "lastname": "User",
             "username": "acc",
             "password": "password"
          },
          "account_not_in_project": {
             "email": "newtest@test.com",
             "firstname": "Test",
             "lastname": "User",
             "username": "account_not_in_project",
             "password": "password"
          },
          "project": {
             "name": "Project",
             "displaytext": "Project"
          },
          "project2": {
             "name": "Project2",
             "displaytext": "Project2"
          },
          "service_offering": {
             "name": "Tiny Instance",
             "displaytext": "Tiny Instance",
             "cpunumber": 1,
             "cpuspeed": 100,
             "memory": 64
          },
          "ostype": 'CentOS 5.3 (64-bit)',
          "host_anti_affinity": {
                "name": "",
                "type": "host anti-affinity"
             },
          "virtual_machine" : {
          }
       }

class TestCreateAffinityGroup(cloudstackTestCase):
    """
    Test various scenarios for Create Affinity Group API for projects
    """

    @classmethod
    def setUpClass(cls):
       cls.testClient = super(TestCreateAffinityGroup, cls).getClsTestClient()
       cls.api_client = cls.testClient.getApiClient()
       cls.services = Services().services

       #Get Zone, Domain and templates
       cls.rootdomain = get_domain(cls.api_client)
       cls.domain = Domain.create(cls.api_client, cls.services["domain"])

       cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
       cls.template = get_template(
          cls.api_client,
          cls.zone.id,
          cls.services["ostype"]
       )
       
       cls.services["virtual_machine"]["zoneid"] = cls.zone.id
       cls.services["template"] = cls.template.id
       cls.services["zoneid"] = cls.zone.id
       
       cls.domain_admin_account = Account.create(
          cls.api_client,
          cls.services["domain_admin_account"],
          domainid=cls.domain.id,
          admin=True
       )

       cls.domain_api_client = cls.testClient.getUserApiClient(cls.domain_admin_account.name, cls.domain.name, 2)

       cls.account = Account.create(
          cls.api_client,
          cls.services["account"],
          domainid=cls.domain.id
       )       

       cls.account_api_client = cls.testClient.getUserApiClient(cls.account.name, cls.domain.name, 0)

       cls.account_not_in_project = Account.create(
          cls.api_client,
          cls.services["account_not_in_project"],
          domainid=cls.domain.id
       )

       cls.account_not_in_project_api_client = cls.testClient.getUserApiClient(cls.account_not_in_project.name, cls.domain.name, 0)

       cls.project = Project.create(
          cls.api_client,
          cls.services["project"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )
       
       cls.project2 = Project.create(
          cls.api_client,
          cls.services["project2"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )

       cls.debug("Created project with ID: %s" % cls.project.id)
       cls.debug("Created project2 with ID: %s" % cls.project2.id)

       # Add user to the project
       cls.project.addAccount(
          cls.api_client,
          cls.account.name
       )

       cls.service_offering = ServiceOffering.create(
          cls.api_client,
          cls.services["service_offering"],
          domainid=cls.account.domainid
       )
       
       cls._cleanup = []
       return

    def setUp(self):
       self.apiclient = self.testClient.getApiClient()
       self.dbclient = self.testClient.getDbConnection()
       self.cleanup = []

    def tearDown(self):
       try:
#            #Clean up, terminate the created instance, volumes and snapshots
          cleanup_resources(self.apiclient, self.cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)
       return

    @classmethod
    def tearDownClass(cls):
       try:
          #Clean up, terminate the created templates
          cls.domain.delete(cls.api_client, cleanup=True)
          cleanup_resources(cls.api_client, cls._cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None, aff_grp_name=None, projectid=None):

       if not api_client:
          api_client = self.api_client
       if aff_grp is None:
          aff_grp = self.services["host_anti_affinity"]
       if aff_grp_name is None:
          aff_grp["name"] = "aff_grp_" + random_gen(size=6)
       else:
          aff_grp["name"] = aff_grp_name
       if projectid is None:
          projectid = self.project.id
       try:
          return AffinityGroup.create(api_client, aff_grp, None, None, projectid)
       except Exception as e:
          raise Exception("Error: Creation of Affinity Group failed : %s" % e)
   
    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_01_admin_create_aff_grp_for_project(self):
       """
       Test create affinity group as admin in project
       @return:
       """
       aff_grp = self.create_aff_grp()
       self.debug("Created Affinity Group: %s" % aff_grp.name)
       list_aff_grps = AffinityGroup.list(self.api_client, id=aff_grp.id)
       self.assertTrue(isinstance(list_aff_grps, list) and len(list_aff_grps) > 0)
       self.assertTrue(list_aff_grps[0].id == aff_grp.id)
       self.assertTrue(list_aff_grps[0].projectid == self.project.id)
       self.cleanup.append(aff_grp)
 
    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_02_doadmin_create_aff_grp_for_project(self):
        """
        Test create affinity group as domain admin for projects
        @return:
        """
        aff_grp = self.create_aff_grp(api_client=self.domain_api_client)
        list_aff_grps = AffinityGroup.list(self.domain_api_client, id=aff_grp.id)
        self.assertTrue(isinstance(list_aff_grps, list) and len(list_aff_grps) > 0)
        self.assertTrue(list_aff_grps[0].id == aff_grp.id)
        self.assertTrue(list_aff_grps[0].projectid == self.project.id)
        self.cleanup.append(aff_grp)
 
    @attr(tags=["vogxn", "simulator", "basic", "advanced"], required_hardware="false")
    def test_03_user_create_aff_grp_for_project(self):
        """
        Test create affinity group as user for projects
        @return:
        """
        aff_grp = self.create_aff_grp(api_client=self.account_api_client)
        list_aff_grps = AffinityGroup.list(self.api_client, id=aff_grp.id)
        self.assertTrue(isinstance(list_aff_grps, list) and len(list_aff_grps) > 0)
        self.assertTrue(list_aff_grps[0].id == aff_grp.id)
        self.assertTrue(list_aff_grps[0].projectid == self.project.id)
        self.cleanup.append(aff_grp)
  
    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_4_user_create_aff_grp_existing_name_for_project(self):
        """
        Test create affinity group that exists (same name) for projects
        @return:
        """
        
        failed_aff_grp = None
        aff_grp = self.create_aff_grp(api_client=self.account_api_client)
        with self.assertRaises(Exception):
           failed_aff_grp = self.create_aff_grp(api_client=self.account_api_client,aff_grp_name = aff_grp.name)
        
        if failed_aff_grp:
            self.cleanup.append(failed_aff_grp)
        self.cleanup.append(aff_grp)

class TestListAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
       cls.testClient = super(TestListAffinityGroups, cls).getClsTestClient()
       cls.api_client = cls.testClient.getApiClient()
       cls.services = Services().services

       #Get Zone, Domain and templates
       cls.rootdomain = get_domain(cls.api_client)
       cls.domain = Domain.create(cls.api_client, cls.services["domain"])

       cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
       cls.template = get_template(
          cls.api_client,
          cls.zone.id,
          cls.services["ostype"]
       )
       
       cls.services["virtual_machine"]["zoneid"] = cls.zone.id
       cls.services["template"] = cls.template.id
       cls.services["zoneid"] = cls.zone.id
       
       cls.domain_admin_account = Account.create(
          cls.api_client,
          cls.services["domain_admin_account"],
          domainid=cls.domain.id,
          admin=True
       )

       cls.domain_api_client = cls.testClient.getUserApiClient(cls.domain_admin_account.name, cls.domain.name, 2)

       cls.account = Account.create(
          cls.api_client,
          cls.services["account"],
          domainid=cls.domain.id
       )       

       cls.account_api_client = cls.testClient.getUserApiClient(cls.account.name, cls.domain.name, 0)

       cls.account_not_in_project = Account.create(
          cls.api_client,
          cls.services["account_not_in_project"],
          domainid=cls.domain.id
       )

       cls.account_not_in_project_api_client = cls.testClient.getUserApiClient(cls.account_not_in_project.name, cls.domain.name, 0)

       cls.project = Project.create(
          cls.api_client,
          cls.services["project"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )
       
       cls.project2 = Project.create(
          cls.api_client,
          cls.services["project2"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )

       cls.debug("Created project with ID: %s" % cls.project.id)
       cls.debug("Created project2 with ID: %s" % cls.project2.id)

       # Add user to the project
       cls.project.addAccount(
          cls.api_client,
          cls.account.name
       )

       cls.service_offering = ServiceOffering.create(
          cls.api_client,
          cls.services["service_offering"],
          domainid=cls.account.domainid
       )
       
       cls._cleanup = []
       return

    def setUp(self):
       self.apiclient = self.testClient.getApiClient()
       self.dbclient = self.testClient.getDbConnection()
       self.cleanup = []

    def tearDown(self):
       try:
#            #Clean up, terminate the created instance, volumes and snapshots
          cleanup_resources(self.api_client, self.cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)
       return

    @classmethod
    def tearDownClass(cls):
       try:
          cls.domain.delete(cls.api_client, cleanup=True)
          cleanup_resources(cls.api_client, cls._cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None, aff_grp_name=None, projectid=None):

       if not api_client:
          api_client = self.api_client
       if aff_grp is None:
          aff_grp = self.services["host_anti_affinity"]
       if aff_grp_name is None:
          aff_grp["name"] = "aff_grp_" + random_gen(size=6)
       else:
          aff_grp["name"] = aff_grp_name
       if projectid is None:
          projectid = self.project.id
       try:
          return AffinityGroup.create(api_client, aff_grp, None, None, projectid)
       except Exception as e:
          raise Exception("Error: Creation of Affinity Group failed : %s" % e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=[], projectid=None):
        self.debug('Creating VM in AffinityGroups=%s' % ag_list)

        if api_client is None:
           api_client = self.api_client
        if projectid is None:
           projectid = self.project.id

        vm = VirtualMachine.create(
                api_client,
                self.services["virtual_machine"],
                projectid=projectid,
                templateid=self.template.id,
                serviceofferingid=self.service_offering.id,
                affinitygroupnames=ag_list
              )
        self.debug('Created VM=%s in Affinity Group=%s' % (vm.id, tuple(ag_list)))

        list_vm = list_virtual_machines(api_client, id=vm.id, projectid=projectid)
        self.assertEqual(isinstance(list_vm, list), True,"Check list response returns a valid list")
        self.assertNotEqual(len(list_vm),0, "Check VM available in List Virtual Machines")
        vm_response = list_vm[0]
        self.assertEqual(vm_response.state, 'Running',msg="VM is not in Running state")
        self.assertEqual(vm_response.projectid, projectid,msg="VM is not in project")
        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_01_list_aff_grps_for_vm(self):
        """
          List affinity group for a vm for projects
        """
        aff_grps = []
        aff_grps.append(self.create_aff_grp(self.domain_api_client, projectid=self.project.id))

        vm, hostid = self.create_vm_in_aff_grps(self.account_api_client,ag_list=[aff_grps[0].name])
        list_aff_grps = AffinityGroup.list(self.api_client,virtualmachineid=vm.id)

        self.assertEqual(list_aff_grps[0].name, aff_grps[0].name,"Listing Affinity Group by VM id failed")
        self.assertEqual(list_aff_grps[0].projectid, self.project.id,"Listing Affinity Group by VM id failed, vm was not in project")

        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.cleanup.append(aff_grps[0])

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_02_list_multiple_aff_grps_for_vm(self):
        """
          List multiple affinity groups associated with a vm for projects
        """

        aff_grp_01 = self.create_aff_grp(self.account_api_client)
        aff_grp_02 = self.create_aff_grp(self.account_api_client)

        aff_grps_names = [aff_grp_01.name, aff_grp_02.name]
        vm, hostid = self.create_vm_in_aff_grps(ag_list=aff_grps_names)
        list_aff_grps = AffinityGroup.list(self.api_client,
                                    virtualmachineid=vm.id)

        list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

        aff_grps_names.sort()
        list_aff_grps_names.sort()
        self.assertEqual(aff_grps_names, list_aff_grps_names,"One of the Affinity Groups is missing %s" % list_aff_grps_names)

        vm.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.cleanup.append(aff_grp_01)
        self.cleanup.append(aff_grp_02)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_03_list_aff_grps_by_id(self):
        """
          List affinity groups by id for projects
        """
        aff_grp = self.create_aff_grp(self.account_api_client)
        list_aff_grps = AffinityGroup.list(self.account_api_client, id=aff_grp.id, projectid=self.project.id)
        self.assertEqual(list_aff_grps[0].id, aff_grp.id,"Listing Affinity Group by id failed")
        with self.assertRaises(Exception):
           AffinityGroup.list(self.account_not_in_project_api_client, id=aff_grp.id, projectid=self.project.id)
        self.cleanup.append(aff_grp)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_04_list_aff_grps_by_name(self):
        """
           List Affinity Groups by name for projects
        """
        aff_grp = self.create_aff_grp(self.account_api_client)
        list_aff_grps = AffinityGroup.list(self.account_api_client, name=aff_grp.name, projectid=self.project.id)
        self.assertEqual(list_aff_grps[0].name, aff_grp.name,"Listing Affinity Group by name failed")
        with self.assertRaises(Exception):
           AffinityGroup.list(self.account_not_in_project_api_client, id=aff_grp.id, projectid=self.project.id)
        self.cleanup.append(aff_grp)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_05_list_aff_grps_by_non_existing_id(self):
        """
           List Affinity Groups by non-existing id for projects
        """
        aff_grp = self.create_aff_grp(self.account_api_client)
        list_aff_grps = AffinityGroup.list(self.account_api_client, id=1234, projectid=self.project.id)
        self.assertEqual(list_aff_grps, None, "Listing Affinity Group by non-existing id succeeded.")
        self.cleanup.append(aff_grp)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_06_list_aff_grps_by_non_existing_name(self):
        """
           List Affinity Groups by non-existing name for projects
        """

        aff_grp = self.create_aff_grp(self.account_api_client)
        list_aff_grps = AffinityGroup.list(self.account_api_client, name="inexistantName", projectid=self.project.id)
        self.assertEqual(list_aff_grps, None, "Listing Affinity Group by non-existing name succeeded.")
        self.cleanup.append(aff_grp)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_07_list_all_vms_in_aff_grp(self):
        """
          List affinity group should list all for a vms associated with that group for projects
        """

        aff_grp = self.create_aff_grp(self.account_api_client)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[aff_grp.name])
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_list=[aff_grp.name])
        list_aff_grps = AffinityGroup.list(self.api_client, id=aff_grp.id, projectid=self.project.id)

        self.assertEqual(list_aff_grps[0].name, aff_grp.name, "Listing Affinity Group by id failed")

        self.assertEqual(list_aff_grps[0].virtualmachineIds[0], vm1.id, "List affinity group response.virtualmachineIds for group: %s doesn't contain vmid : %s" % (aff_grp.name, vm1.id))
        self.assertEqual(list_aff_grps[0].virtualmachineIds[1], vm2.id, "List affinity group response.virtualmachineIds for group: %s doesn't contain vmid : %s" % (aff_grp.name, vm2.id))


        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        self.cleanup.append(aff_grp)
 
class TestDeleteAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
       cls.testClient = super(TestDeleteAffinityGroups, cls).getClsTestClient()
       cls.api_client = cls.testClient.getApiClient()
       cls.services = Services().services

       #Get Zone, Domain and templates
       cls.rootdomain = get_domain(cls.api_client)
       cls.domain = Domain.create(cls.api_client, cls.services["domain"])

       cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
       cls.template = get_template(
          cls.api_client,
          cls.zone.id,
          cls.services["ostype"]
       )
       
       cls.services["virtual_machine"]["zoneid"] = cls.zone.id
       cls.services["template"] = cls.template.id
       cls.services["zoneid"] = cls.zone.id
       
       cls.domain_admin_account = Account.create(
          cls.api_client,
          cls.services["domain_admin_account"],
          domainid=cls.domain.id,
          admin=True
       )

       cls.domain_api_client = cls.testClient.getUserApiClient(cls.domain_admin_account.name, cls.domain.name, 2)

       cls.account = Account.create(
          cls.api_client,
          cls.services["account"],
          domainid=cls.domain.id
       )       

       cls.account_api_client = cls.testClient.getUserApiClient(cls.account.name, cls.domain.name, 0)

       cls.account_not_in_project = Account.create(
          cls.api_client,
          cls.services["account_not_in_project"],
          domainid=cls.domain.id
       )

       cls.account_not_in_project_api_client = cls.testClient.getUserApiClient(cls.account_not_in_project.name, cls.domain.name, 0)

       cls.project = Project.create(
          cls.api_client,
          cls.services["project"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )
       
       cls.project2 = Project.create(
          cls.api_client,
          cls.services["project2"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )

       cls.debug("Created project with ID: %s" % cls.project.id)
       cls.debug("Created project2 with ID: %s" % cls.project2.id)

       # Add user to the project
       cls.project.addAccount(
          cls.api_client,
          cls.account.name
       )

       cls.service_offering = ServiceOffering.create(
          cls.api_client,
          cls.services["service_offering"],
          domainid=cls.account.domainid
       )
       
       cls._cleanup = []
       return

    def setUp(self):
       self.apiclient = self.testClient.getApiClient()
       self.dbclient = self.testClient.getDbConnection()
       self.cleanup = []

    def tearDown(self):
       try:
#            #Clean up, terminate the created instance, volumes and snapshots
          cleanup_resources(self.api_client, self.cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)
       return

    @classmethod
    def tearDownClass(cls):
       try:
          cls.domain.delete(cls.api_client, cleanup=True)
          cleanup_resources(cls.api_client, cls._cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None, aff_grp_name=None, projectid=None):

       if not api_client:
          api_client = self.api_client
       if aff_grp is None:
          aff_grp = self.services["host_anti_affinity"]
       if aff_grp_name is None:
          aff_grp["name"] = "aff_grp_" + random_gen(size=6)
       else:
          aff_grp["name"] = aff_grp_name
       if projectid is None:
          projectid = self.project.id
       try:
          return AffinityGroup.create(api_client, aff_grp, None, None, projectid)
       except Exception as e:
          raise Exception("Error: Creation of Affinity Group failed : %s" % e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=[], projectid=None):
        self.debug('Creating VM in AffinityGroups=%s' % ag_list)

        if api_client is None:
           api_client = self.api_client
        if projectid is None:
           projectid = self.project.id

        vm = VirtualMachine.create(
                api_client,
                self.services["virtual_machine"],
                projectid=projectid,
                templateid=self.template.id,
                serviceofferingid=self.service_offering.id,
                affinitygroupnames=ag_list
              )
        self.debug('Created VM=%s in Affinity Group=%s' % (vm.id, tuple(ag_list)))
        list_vm = list_virtual_machines(self.api_client, id=vm.id, projectid=projectid)
        self.assertEqual(isinstance(list_vm, list), True,"Check list response returns an invalid list %s" % list_vm)
        self.assertNotEqual(len(list_vm),0, "Check VM available in TestDeployVMAffinityGroups")
        self.assertEqual(list_vm[0].id, vm.id,"Listed vm does not have the same ids")
        vm_response = list_vm[0]
        self.assertEqual(vm.state, 'Running',msg="VM is not in Running state")
        self.assertEqual(vm.projectid, projectid,msg="VM is not in project")
        self.assertNotEqual(vm_response.hostid, None, "Host id was null for vm %s" % vm_response)
        return vm, vm_response.hostid

    def delete_aff_group(self, apiclient, **kwargs):
        cmd = deleteAffinityGroup.deleteAffinityGroupCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return apiclient.deleteAffinityGroup(cmd)


    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_01_delete_aff_grp_by_id(self):
        """
           #Delete Affinity Group by id.
        """

        aff_grp1 = self.create_aff_grp(self.account_api_client)
        aff_grp2 = self.create_aff_grp(self.account_api_client)

        aff_grp1.delete(self.account_api_client)
        
        with self.assertRaises(Exception):
           list_aff_grps = AffinityGroup.list(self.api_client, id=aff_grp1.id)
        
        self.cleanup.append(aff_grp2)

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_02_delete_aff_grp_by_id_another_user(self):
        """
           #Delete Affinity Group by id should fail for user not in project
        """

        aff_grp1 = self.create_aff_grp(self.account_api_client)
        aff_grp2 = self.create_aff_grp(self.account_api_client)

        with self.assertRaises(Exception):
           aff_grp1.delete(self.account_not_in_project_api_client)

        self.cleanup.append(aff_grp1)
        self.cleanup.append(aff_grp2)

class TestUpdateVMAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
       cls.testClient = super(TestUpdateVMAffinityGroups, cls).getClsTestClient()
       cls.api_client = cls.testClient.getApiClient()
       cls.services = Services().services

       #Get Zone, Domain and templates
       cls.rootdomain = get_domain(cls.api_client)
       cls.domain = Domain.create(cls.api_client, cls.services["domain"])

       cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
       cls.template = get_template(
          cls.api_client,
          cls.zone.id,
          cls.services["ostype"]
       )
       
       cls.services["virtual_machine"]["zoneid"] = cls.zone.id
       cls.services["template"] = cls.template.id
       cls.services["zoneid"] = cls.zone.id
       
       cls.domain_admin_account = Account.create(
          cls.api_client,
          cls.services["domain_admin_account"],
          domainid=cls.domain.id,
          admin=True
       )

       cls.domain_api_client = cls.testClient.getUserApiClient(cls.domain_admin_account.name, cls.domain.name, 2)

       cls.account = Account.create(
          cls.api_client,
          cls.services["account"],
          domainid=cls.domain.id
       )       

       cls.account_api_client = cls.testClient.getUserApiClient(cls.account.name, cls.domain.name, 0)

       cls.account_not_in_project = Account.create(
          cls.api_client,
          cls.services["account_not_in_project"],
          domainid=cls.domain.id
       )

       cls.account_not_in_project_api_client = cls.testClient.getUserApiClient(cls.account_not_in_project.name, cls.domain.name, 0)

       cls.project = Project.create(
          cls.api_client,
          cls.services["project"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )
       
       cls.project2 = Project.create(
          cls.api_client,
          cls.services["project2"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )

       cls.debug("Created project with ID: %s" % cls.project.id)
       cls.debug("Created project2 with ID: %s" % cls.project2.id)

       # Add user to the project
       cls.project.addAccount(
          cls.api_client,
          cls.account.name
       )

       cls.service_offering = ServiceOffering.create(
          cls.api_client,
          cls.services["service_offering"],
          domainid=cls.account.domainid
       )
       
       cls._cleanup = []
       return

    def setUp(self):
       self.apiclient = self.testClient.getApiClient()
       self.dbclient = self.testClient.getDbConnection()
       self.cleanup = []

    def tearDown(self):
       try:
#            #Clean up, terminate the created instance, volumes and snapshots
          cleanup_resources(self.api_client, self.cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)
       return

    @classmethod
    def tearDownClass(cls):
       try:
          cls.domain.delete(cls.api_client, cleanup=True)
          cleanup_resources(cls.api_client, cls._cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None, aff_grp_name=None, projectid=None):

       if not api_client:
          api_client = self.api_client
       if aff_grp is None:
          aff_grp = self.services["host_anti_affinity"]
       if aff_grp_name is None:
          aff_grp["name"] = "aff_grp_" + random_gen(size=6)
       else:
          aff_grp["name"] = aff_grp_name
       if projectid is None:
          projectid = self.project.id
       try:
          return AffinityGroup.create(api_client, aff_grp, None, None, projectid)
       except Exception as e:
          raise Exception("Error: Creation of Affinity Group failed : %s" % e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=[], projectid=None):
        self.debug('Creating VM in AffinityGroups=%s' % ag_list)

        if api_client is None:
           api_client = self.api_client
        if projectid is None:
           projectid = self.project.id

        vm = VirtualMachine.create(
                api_client,
                self.services["virtual_machine"],
                projectid=projectid,
                templateid=self.template.id,
                serviceofferingid=self.service_offering.id,
                affinitygroupnames=ag_list
              )
        self.debug('Created VM=%s in Affinity Group=%s' % (vm.id, tuple(ag_list)))
        list_vm = list_virtual_machines(self.api_client, id=vm.id, projectid=projectid)
        self.assertEqual(isinstance(list_vm, list), True,"Check list response returns an invalid list %s" % list_vm)
        self.assertNotEqual(len(list_vm),0, "Check VM available in TestDeployVMAffinityGroups")
        self.assertEqual(list_vm[0].id, vm.id,"Listed vm does not have the same ids")
        vm_response = list_vm[0]
        self.assertEqual(vm.state, 'Running',msg="VM is not in Running state")
        self.assertEqual(vm.projectid, projectid,msg="VM is not in project")
        self.assertNotEqual(vm_response.hostid, None, "Host id was null for vm %s" % vm_response)
        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_01_update_aff_grp_by_ids(self):
        """
           Update the list of affinityGroups by using affinity groupids

        """
        aff_grp1 = self.create_aff_grp(self.account_api_client)
        aff_grp2 = self.create_aff_grp(self.account_api_client)

        vm1, hostid1 = self.create_vm_in_aff_grps(ag_list=[aff_grp1.name])
        vm2, hostid2 = self.create_vm_in_aff_grps(ag_list=[aff_grp1.name])

        vm1.stop(self.api_client)

        list_aff_grps = AffinityGroup.list(self.api_client, projectid=self.project.id)

        self.assertEqual(len(list_aff_grps), 2 , "2 affinity groups should be present")

        vm1.update_affinity_group(self.api_client,affinitygroupids=[list_aff_grps[0].id,list_aff_grps[1].id])

        list_aff_grps = AffinityGroup.list(self.api_client,virtualmachineid=vm1.id)

        list_aff_grps_names = [list_aff_grps[0].name, list_aff_grps[1].name]

        aff_grps_names = [aff_grp1.name, aff_grp2.name]
        aff_grps_names.sort()
        list_aff_grps_names.sort()
        self.assertEqual(aff_grps_names, list_aff_grps_names,"One of the Affinity Groups is missing %s" % list_aff_grps_names)

        vm1.start(self.api_client)

        vm_status = VirtualMachine.list(self.api_client, id=vm1.id)
        self.assertNotEqual(vm_status[0].hostid, hostid2, "The virtual machine started on host %s violating the host anti-affinity rule" %vm_status[0].hostid)

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        aff_grp1.delete(self.api_client)
        aff_grp2.delete(self.api_client)
           

class TestDeployVMAffinityGroups(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
       cls.testClient = super(TestDeployVMAffinityGroups, cls).getClsTestClient()
       cls.api_client = cls.testClient.getApiClient()
       cls.services = Services().services

       #Get Zone, Domain and templates
       cls.rootdomain = get_domain(cls.api_client)
       cls.domain = Domain.create(cls.api_client, cls.services["domain"])

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
       cls.domain_admin_account = Account.create(
          cls.api_client,
          cls.services["domain_admin_account"],
          domainid=cls.domain.id,
          admin=True
       )
       cls._cleanup.append(cls.domain_admin_account)
       cls.domain_api_client = cls.testClient.getUserApiClient(cls.domain_admin_account.name, cls.domain.name, 2)

       cls.account = Account.create(
          cls.api_client,
          cls.services["account"],
          domainid=cls.domain.id
       )
       cls._cleanup.append(cls.account)

       cls.account_api_client = cls.testClient.getUserApiClient(cls.account.name, cls.domain.name, 0)

       cls.account_not_in_project = Account.create(
          cls.api_client,
          cls.services["account_not_in_project"],
          domainid=cls.domain.id
       )
       cls._cleanup.append(cls.account_not_in_project)

       cls.account_not_in_project_api_client = cls.testClient.getUserApiClient(cls.account_not_in_project.name, cls.domain.name, 0)
       cls._proj_toclean = []
       cls.project = Project.create(
          cls.api_client,
          cls.services["project"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )
       cls._proj_toclean.append(cls.project)
       cls.project2 = Project.create(
          cls.api_client,
          cls.services["project2"],
          account=cls.domain_admin_account.name,
          domainid=cls.domain_admin_account.domainid
       )
       cls._proj_toclean.append(cls.project2)

       cls.debug("Created project with ID: %s" % cls.project.id)
       cls.debug("Created project2 with ID: %s" % cls.project2.id)

       # Add user to the project
       cls.project.addAccount(
          cls.api_client,
          cls.account.name
       )

       cls.service_offering = ServiceOffering.create(
          cls.api_client,
          cls.services["service_offering"],
          domainid=cls.account.domainid
       )
       
       return

    def setUp(self):
       self.apiclient = self.testClient.getApiClient()
       self.dbclient = self.testClient.getDbConnection()
       self.cleanup = []

    def tearDown(self):
       try:
#            #Clean up, terminate the created instance, volumes and snapshots
          cleanup_resources(self.api_client, self.cleanup)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)
       return

    @classmethod
    def tearDownClass(cls):
       try:
          cleanup_resources(cls.api_client, cls._proj_toclean)
          cleanup_resources(cls.api_client, cls._cleanup)
          cls.domain.delete(cls.api_client, cleanup=True)
       except Exception as e:
          raise Exception("Warning: Exception during cleanup : %s" % e)

    def create_aff_grp(self, api_client=None, aff_grp=None, aff_grp_name=None, projectid=None):

       if not api_client:
          api_client = self.api_client
       if aff_grp is None:
          aff_grp = self.services["host_anti_affinity"]
       if aff_grp_name is None:
          aff_grp["name"] = "aff_grp_" + random_gen(size=6)
       else:
          aff_grp["name"] = aff_grp_name
       if projectid is None:
          projectid = self.project.id
       try:
          return AffinityGroup.create(api_client, aff_grp, None, None, projectid)
       except Exception as e:
          raise Exception("Error: Creation of Affinity Group failed : %s" % e)

    def create_vm_in_aff_grps(self, api_client=None, ag_list=[], projectid=None):
        self.debug('Creating VM in AffinityGroups=%s' % ag_list)

        if api_client is None:
           api_client = self.api_client
        if projectid is None:
           projectid = self.project.id

        vm = VirtualMachine.create(
                api_client,
                self.services["virtual_machine"],
                projectid=projectid,
                templateid=self.template.id,
                serviceofferingid=self.service_offering.id,
                affinitygroupnames=ag_list
              )
        self.debug('Created VM=%s in Affinity Group=%s' % (vm.id, tuple(ag_list)))
        list_vm = list_virtual_machines(self.api_client, id=vm.id, projectid=projectid)
        self.assertEqual(isinstance(list_vm, list), True,"Check list response returns an invalid list %s" % list_vm)
        self.assertNotEqual(len(list_vm),0, "Check VM available in TestDeployVMAffinityGroups")
        self.assertEqual(list_vm[0].id, vm.id,"Listed vm does not have the same ids")
        vm_response = list_vm[0]
        self.assertEqual(vm.state, 'Running',msg="VM is not in Running state")
        self.assertEqual(vm.projectid, projectid,msg="VM is not in project")
        self.assertNotEqual(vm_response.hostid, None, "Host id was null for vm %s" % vm_response)
        return vm, vm_response.hostid

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_01_deploy_vm_anti_affinity_group(self):
        """
        test DeployVM in anti-affinity groups

        deploy VM1 and VM2 in the same host-anti-affinity groups
        Verify that the vms are deployed on separate hosts
        """
        aff_grp = self.create_aff_grp(self.account_api_client)
        vm1, hostid1 = self.create_vm_in_aff_grps(self.account_api_client,ag_list=[aff_grp.name])
        vm2, hostid2 = self.create_vm_in_aff_grps(self.account_api_client, ag_list=[aff_grp.name])

        self.assertNotEqual(hostid1, hostid2, msg="Both VMs of affinity group %s are on the same host: %s , %s, %s, %s" % (aff_grp.name, vm1, hostid1, vm2, hostid2))

        vm1.delete(self.api_client)
        vm2.delete(self.api_client)
        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])
        self.cleanup.append(aff_grp)

    @attr(tags=["simulator", "basic", "advanced", "multihost"], required_hardware="false")
    def test_02_deploy_vm_anti_affinity_group_fail_on_not_enough_hosts(self):
        """
        test DeployVM in anti-affinity groups with more vms than hosts.
        """
        hosts = list_hosts(self.api_client, type="routing")
        aff_grp = self.create_aff_grp(self.account_api_client)
        vms = []
        for host in hosts:
           vms.append(self.create_vm_in_aff_grps(self.account_api_client,ag_list=[aff_grp.name]))

        vm_failed = None
        with self.assertRaises(Exception):
           vm_failed = self.create_vm_in_aff_grps(self.account_api_client,ag_list=[aff_grp.name])
        
        self.assertEqual(len(hosts), len(vms), "Received %s and %s " % (hosts, vms))
        
        if vm_failed:
           vm_failed.expunge(self.api_client)

        wait_for_cleanup(self.api_client, ["expunge.delay", "expunge.interval"])
        self.cleanup.append(aff_grp)



