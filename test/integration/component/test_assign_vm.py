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

"""
"""
#Import Local Modules
from nose.plugins.attrib           import attr
from marvin.cloudstackTestCase     import cloudstackTestCase
from marvin.integration.lib.base   import (Account,
                                           Domain,
                                           User,
                                           Project,
                                           Volume,
                                           Snapshot,
                                           DiskOffering,
                                           ServiceOffering,
                                           VirtualMachine)
from marvin.integration.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           cleanup_resources,
                                           list_volumes,
                                           update_resource_limit,
                                           list_networks,
                                           list_snapshots,
                                           list_virtual_machines)

def log_test_exceptions(func):
    def test_wrap_exception_log(self, *args, **kwargs):
        try:
            func(self, *args, **kwargs)
        except Exception as e:
            self.debug('Test %s Failed due to Exception=%s' % (func, e))
            raise e
    test_wrap_exception_log.__doc__ = func.__doc__
    return test_wrap_exception_log

class Services:
    """Test service data for:Change the ownershop of
       VM/network/datadisk/snapshot/template/ISO from one account to any other account.
    """
    def __init__(self):
        self.services = {"domain"           : {"name": "Domain",},
                         "account"          : {"email"     : "test@test.com",
                                               "firstname" : "Test",
                                               "lastname"  : "User",
                                               "username"  : "test",
                                               # Random characters are appended in create account to
                                               # ensure unique username generated each time
                                               "password"  : "password",},
                         "user"             : {"email"    : "user@test.com",
                                               "firstname": "User",
                                               "lastname" : "User",
                                               "username" : "User",
                                               # Random characters are appended for unique
                                               # username
                                               "password" : "fr3sca",},
                         "project"          : {"name"        : "Project",
                                               "displaytext" : "Test project",},
                         "volume"           : {"diskname" : "TestDiskServ",
                                               "max"      : 6,},
                         "disk_offering"    : {"displaytext" : "Small",
                                               "name"        : "Small",
                                               "disksize"    : 1},
                         "virtual_machine"  : {"displayname" : "testserver",
                                               "username"    : "root",# VM creds for SSH
                                               "password"    : "password",
                                               "ssh_port"    : 22,
                                               "hypervisor"  : 'XenServer',
                                               "privateport" : 22,
                                               "publicport"  : 22,
                                               "protocol"    : 'TCP',},
                         "service_offering" : {"name"        : "Tiny Instance",
                                               "displaytext" : "Tiny Instance",
                                               "cpunumber"   : 1,
                                               "cpuspeed"    : 100,# in MHz
                                               "memory"      : 128},
                                               #"storagetype" : "local"},
                         "sleep"            :  60,
                         "ostype"           :  'CentOS 5.3 (64-bit)',# CentOS 5.3 (64-bit)
                        }

class TestVMOwnership(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.api_client = super(TestVMOwnership,
                               cls).getClsTestClient().getApiClient()
        cls.services  = Services().services
        # Get Zone  Domain and create Domains and sub Domains.
        cls.domain           = get_domain(cls.api_client, cls.services)
        cls.zone             = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        # Get and set template id for VM creation.
        cls.template = get_template(cls.api_client,
                                    cls.zone.id,
                                    cls.services["ostype"])
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        def create_domain_account_user(parentDomain=None):
            domain  =  Domain.create(cls.api_client,
                                     cls.services["domain"],
                                     parentdomainid=parentDomain.id if parentDomain else None)
            cls._cleanup.append(domain)
            # Create an Account associated with domain
            account = Account.create(cls.api_client,
                                     cls.services["account"],
                                     domainid=domain.id)
            cls._cleanup.append(account)
            # Create an User, Project, Volume associated with account
            user    = User.create(cls.api_client,
                                  cls.services["user"],
                                  account=account.name,
                                  domainid=account.domainid)
            cls._cleanup.append(user)
            project = Project.create(cls.api_client,
                                     cls.services["project"],
                                     account=account.name,
                                     domainid=account.domainid)
            cls._cleanup.append(project)
            volume  = Volume.create(cls.api_client,
                                    cls.services["volume"],
                                    zoneid=cls.zone.id,
                                    account=account.name,
                                    domainid=account.domainid,
                                    diskofferingid=cls.disk_offering.id)
            cls._cleanup.append(volume)
            return {'domain':domain, 'account':account, 'user':user, 'project':project, 'volume':volume}

        # Create disk offerings.
        try:
            cls.disk_offering = DiskOffering.create(cls.api_client,
                                                    cls.services["disk_offering"])
            # Create service offerings.
            cls.service_offering = ServiceOffering.create(cls.api_client,
                                                          cls.services["service_offering"])
            # Cleanup
            cls._cleanup = [cls.service_offering]
            # Create domain, account, user, project and volumes.
            cls.domain_account_user1   = create_domain_account_user()
            cls.domain_account_user2   = create_domain_account_user()
            cls.sdomain_account_user1  = create_domain_account_user(cls.domain_account_user1['domain'])
            cls.sdomain_account_user2  = create_domain_account_user(cls.domain_account_user2['domain'])
            cls.ssdomain_account_user2 = create_domain_account_user(cls.sdomain_account_user2['domain'])
        except Exception as e:
          raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.api_client#self.testClient.getApiClient()
        #self.dbclient  = self.testClient.getDbConnection()
        self.cleanup   = []
        self.snapshot  = None
        return

    def create_vm(self,
                  account,
                  domain,
                  isRunning=False,
                  project  =None,
                  limit    =None,
                  pfrule   =False,
                  lbrule   =None,
                  natrule  =None,
                  volume   =None,
                  snapshot =False):
        #TODO: Implemnt pfrule/lbrule/natrule
        self.debug("Deploying instance in the account: %s" % account.name)
        self.virtual_machine = VirtualMachine.create(self.apiclient,
                                                     self.services["virtual_machine"],
                                                     accountid=account.name,
                                                     domainid=domain.id,
                                                     serviceofferingid=self.service_offering.id,
                                                     mode=self.zone.networktype if pfrule else 'basic',
                                                     projectid=project.id if project else None)
        self.debug("Deployed instance in account: %s" % account.name)
        list_virtual_machines(self.apiclient,
                              id=self.virtual_machine.id)
        if snapshot:
           volumes = list_volumes(self.apiclient,
                                  virtualmachineid=self.virtual_machine.id,
                                  type='ROOT',
                                  listall=True)
           self.snapshot = Snapshot.create(self.apiclient,
                                      volumes[0].id,
                                      account=account.name,
                                      domainid=account.domainid)
        if volume:
            self.virtual_machine.attach_volume(self.apiclient,
                                               volume)
        if not isRunning:
            self.virtual_machine.stop(self.apiclient)
        self.cleanup.append(self.virtual_machine)

    def check_vm_is_moved_in_account_domainid(self, account):
        list_vm_response = list_virtual_machines(self.api_client,
                                                 id=self.virtual_machine.id,
                                                 account=account.name,
                                                 domainid=account.domainid)
        self.debug('VM=%s moved to account=%s and domainid=%s' % (list_vm_response, account.name, account.domainid))
        self.assertNotEqual(len(list_vm_response), 0, 'Unable to move VM to account=%s domainid=%s' % (account.name, account.domainid))

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, reversed(self.cleanup))
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_01_move_across_different_domains(self):
        """Test as root, stop a VM from domain1 and attempt to move it to account in domain2
        """
        # Validate the following:
        # 1. deploy VM in domain_1
        # 2. stop VM in domain_1
        # 3. assignVirtualMachine to domain_2
        self.create_vm(self.domain_account_user1['account'], self.domain_account_user1['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.domain_account_user2['account'].name ,self.domain_account_user2['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.domain_account_user2['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_02_move_across_subdomains(self):
        """Test as root, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in subdomain_1
        # 2. stop VM in subdomain_1
        # 3. assignVirtualMachine to subdomain_2
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.sdomain_account_user2['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_03_move_from_domain_to_subdomain(self):
        """Test as root stop a VM from domain1 and attempt to move it to subdomain1
        """
        # Validate the following:
        # 1. deploy VM in domain_1
        # 2. stop VM in domain_1
        # 3. assignVirtualMachine to subdomain_1
        self.create_vm(self.domain_account_user1['account'], self.domain_account_user1['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.sdomain_account_user1['account'].name ,self.sdomain_account_user1['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.sdomain_account_user1['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_04_move_from_domain_to_sub_of_subdomain(self):
        """Test as root, stop a VM from domain1 and attempt to move it to sub-subdomain1
        """
        # Validate the following:
        # 1. deploy VM in domain_2
        # 2. stop VM in domain_2
        # 3. assignVirtualMachine to sub subdomain_2
        self.create_vm(self.domain_account_user2['account'], self.domain_account_user2['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.ssdomain_account_user2['account'].name ,self.ssdomain_account_user2['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.ssdomain_account_user2['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_05_move_to_domain_from_sub_of_subdomain(self):
        """Test as root, stop a VM from sub-subdomain1 and attempt to move it to domain1
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain2
        # 2. stop VM in sub subdomain2
        # 3. assignVirtualMachine to sub domain2
        self.create_vm(self.ssdomain_account_user2['account'], self.ssdomain_account_user2['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.domain_account_user2['account'].name ,self.domain_account_user2['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.domain_account_user2['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_06_move_to_domain_from_subdomain(self):
        """Test as root, stop a Vm from subdomain1 and attempt to move it to domain1
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1
        # 2. stop VM in sub subdomain1
        # 3. assignVirtualMachine to domain1
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.domain_account_user1['account'].name ,self.domain_account_user1['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.domain_account_user1['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_07_move_across_subdomain(self):
        """Test as root, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1
        # 2. stop VM in sub subdomain1
        # 3. assignVirtualMachine to subdomain2
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.sdomain_account_user2['account'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_08_move_across_subdomain_network_create(self):
        """Test as root, stop a VM from subdomain1 and attempt to move it to subdomain2, network should get craeted
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1
        # 2. stop VM in sub subdomain1
        # 3. assignVirtualMachine to subdomain2 network should get created
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'])
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)
        self.check_vm_is_moved_in_account_domainid(self.sdomain_account_user2['account'])
        networks = list_networks(self.apiclient,
                                 account=self.sdomain_account_user2['account'].name,
                                 domainid=self.sdomain_account_user2['domain'].id)
        self.assertEqual(isinstance(networks, list),
                         True,
                         "Check for list networks response return valid data")
        self.assertNotEqual(len(networks),
                            0,
                            "Check list networks response")

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_09_move_across_subdomain(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1
        # 2. stop VM in sub subdomain1
        # 3. assignVirtualMachine to subdomain2
        userapiclient = self.testClient.getUserApiClient(account=self.sdomain_account_user1['account'].name,
                                                         domain=self.sdomain_account_user1['domain'].name,
                                                         type=2)
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'])
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, userapiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_10_move_across_subdomain_vm_running(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1
        # 3. assignVirtualMachine to subdomain2
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'],isRunning=True)
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, self.apiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_11_move_across_subdomain_vm_pfrule(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1 with PF rule set.
        # 3. assignVirtualMachine to subdomain2
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'],pfrule=True)
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, self.apiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_12_move_across_subdomain_vm_volumes(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1 with volumes.
        # 3. assignVirtualMachine to subdomain2
        userapiclient = self.testClient.getUserApiClient(account=self.sdomain_account_user1['account'].name,
                                                         domain=self.sdomain_account_user1['domain'].name,
                                                         type=2)
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'],volume=self.sdomain_account_user1['volume'])
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, userapiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)
         # Check all volumes attached to same VM
        list_volume_response = list_volumes(self.apiclient,
                                            virtualmachineid=self.virtual_machine.id,
                                            type='DATADISK',
                                            listall=True)
        self.assertEqual(isinstance(list_volume_response, list),
                         True,
                         "Check list volumes response for valid list")

        self.assertNotEqual(list_volume_response[0].domainid, self.sdomain_account_user2['domain'].id, "Volume ownership not changed.")
        self.virtual_machine.detach_volume(self.apiclient,
                                           self.sdomain_account_user1['volume'])

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_13_move_across_subdomain_vm_snapshot(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1 with snapshot.
        # 3. assignVirtualMachine to subdomain2
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'], snapshot=True)
        self.virtual_machine.assign_virtual_machine(self.apiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)
        snapshots = list_snapshots(self.apiclient,
                                   id=self.snapshot.id)
        self.assertEqual(snapshots,
                         None,
                         "Snapshots stil present for a vm in domain")

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_14_move_across_subdomain_vm_project(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1 with snapshot.
        # 3. assignVirtualMachine to subdomain2
        userapiclient = self.testClient.getUserApiClient(account=self.sdomain_account_user1['account'].name,
                                                         domain=self.sdomain_account_user1['domain'].name,
                                                         type=2)
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'], project=self.sdomain_account_user1['project'])
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, userapiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_15_move_across_subdomain_account_limit(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2 when limit reached
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1 when account limit is reached.
        # 3. assignVirtualMachine to subdomain2
        update_resource_limit(self.apiclient,
                              0, # VM Instances
                              account=self.sdomain_account_user2['account'].name,
                              domainid=self.sdomain_account_user2['domain'].id,
                              max=0)
        userapiclient = self.testClient.getUserApiClient(account=self.sdomain_account_user1['account'].name,
                                                         domain=self.sdomain_account_user1['domain'].name,
                                                         type=2)
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'], snapshot=True)
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, userapiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)

    @attr(tags = ["advanced"])
    @log_test_exceptions
    def test_16_move_across_subdomain_volume_and_account_limit(self):
        """Test as domain admin, stop a VM from subdomain1 and attempt to move it to subdomain2 volumes are attached and limit reached
        """
        # Validate the following:
        # 1. deploy VM in sub subdomain1 when account limit is reached.
        # 3. assignVirtualMachine to subdomain2
        update_resource_limit(
                              self.apiclient,
                              0, # VM Instances
                              account=self.sdomain_account_user2['account'].name,
                              domainid=self.sdomain_account_user2['domain'].id,
                              max=0)
        userapiclient = self.testClient.getUserApiClient(account=self.sdomain_account_user1['account'].name,
                                                         domain=self.sdomain_account_user1['domain'].name,
                                                         type=2)
        self.create_vm(self.sdomain_account_user1['account'], self.sdomain_account_user1['domain'], snapshot=True, volume=self.sdomain_account_user1['volume'])
        self.assertRaises(Exception, self.virtual_machine.assign_virtual_machine, userapiclient, self.sdomain_account_user2['account'].name ,self.sdomain_account_user2['domain'].id)
        self.virtual_machine.detach_volume(self.apiclient,
                                            self.sdomain_account_user1['volume'])
