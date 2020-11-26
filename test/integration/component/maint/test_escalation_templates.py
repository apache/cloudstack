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
from marvin.lib.base import (Account,
                             Domain, Template, Configurations,VirtualMachine,Snapshot,ServiceOffering
                             )
from marvin.lib.utils import (cleanup_resources, validateList)
from marvin.lib.common import (get_zone, get_template, get_builtin_template_info,update_resource_limit,list_volumes )
from nose.plugins.attrib import attr
from marvin.codes import PASS
from marvin.sshClient import SshClient
from marvin.cloudstackException import CloudstackAPIException
import time


class TestlistTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(
            TestlistTemplates, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.testdata["ostype"]
            )
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        builtin_info = get_builtin_template_info(cls.apiclient, cls.zone.id)
        cls.testdata["templates"]["url"] = builtin_info[0]
        cls.testdata["templates"]["hypervisor"] = builtin_info[1]
        cls.testdata["templates"]["format"] = builtin_info[2]
        if cls.zone.localstorageenabled:
            cls.storagetype = 'local'
            cls.testdata["service_offerings"]["tiny"]["storagetype"] = 'local'
            cls.testdata["disk_offering"]["storagetype"] = 'local'
        else:
            cls.storagetype = 'shared'
            cls.testdata["service_offerings"]["tiny"]["storagetype"] = 'shared'
            cls.testdata["disk_offering"]["storagetype"] = 'shared'
        cls.testdata["virtual_machine"]["hypervisor"] = cls.hypervisor
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = cls.template.id
        cls.testdata["custom_volume"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offerings"]["tiny"]
            )
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.cleanup = []

        # Create 1 domain admin account

        cls.domain = Domain.create(
            cls.apiclient,
            cls.testdata["domain"])

        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            admin=True,
            domainid=cls.domain.id)

        cls.debug("Created account %s in domain %s" %
                  (cls.account.name, cls.domain.id))

        cls.cleanup.append(cls.account)
        cls.cleanup.append(cls.domain)

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
    def RestartServers(self):
        """ Restart management server and usage server """

        sshClient = SshClient(
            self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)
        return

    def updateConfigurAndRestart(self,name, value):
        Configurations.update(self.apiclient,
                              name,value )
        self.RestartServers()
        time.sleep(self.testdata["sleep"])
      
      
    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_CS40139_listtemplate_with_different_pagesize(self):
        """
        @Desc verify list template gives same result with pagesize=500&page=(1,2) and pagesize=1000 when
        there are around 1000 templates
        @steps:
        1: register around 850 templates
        2. call list template api with pagesize=500&page=1 and then page=2
        3.call list template api with pagesize=1000 & page=1
        4. Verify list template returns same list of template in both step 2 and 3
        """
        if self.hypervisor.lower() not in ['xenserver']:
            raise unittest.SkipTest("hypervisor in not xenserver")
            return
        self.updateConfigurAndRestart("default.page.size", "1000")
        self.debug("Updating template resource limit for account: %s" %
                                                self.account.name)
        # Set usage_template=1000 for Account 1
        update_resource_limit(
                              self.apiclient,
                              4, # Template
                              account=self.account.name,
                              domainid=self.domain.id,
                              max=1000
                              )
        
        for i in range(0, 850):
            template_created = Template.register(
                self.apiclient,
                self.testdata["templateregister"],
                zoneid=self.zone.id,
                hypervisor=self.hypervisor,
                account=self.account.name,
                domainid=self.domain.id
            )
            self.assertIsNotNone(
                template_created,
                "Template creation failed"
            )
        listfirst500template = Template.list(
            self.apiclient,
            templatefilter="executable",
            pagesize=500,
            page=1,
            account=self.account.name,
            domainid=self.account.domainid)
        status = validateList(listfirst500template)
        self.assertEqual(
                PASS,
                status[0],
                "First 500 template list is empty")
        listremainingtemplate = Template.list(
            self.apiclient,
            templatefilter="executable",
            pagesize=500,
            page=2,
            account=self.account.name,
            domainid=self.account.domainid)
        status = validateList(listremainingtemplate)
        self.assertEqual(
                PASS,
                status[0],
                "Next 500 template list is empty")
        listalltemplate = Template.list(
            self.apiclient,
            templatefilter="executable",
            pagesize=1000,
            page=1,
            account=self.account.name,
            domainid=self.account.domainid)
        status = validateList(listalltemplate)
        self.assertEqual(
                PASS,
                status[0],
                "entire template list is empty")
        listfirst500template.extend(listremainingtemplate)
        for i, j in zip(listalltemplate,listfirst500template):
            self.assertNotEqual(
            i,
            j,
            "Check template listed are not same"
        )
        return


    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_02_template_permissions(self):
        """
        @Desc: Test to create Public Template by registering or by snapshot and volume when
        Global parameter 'allow.public.user.template' is set to  False
        @steps:
        1.Set Global parameter 'allow.public.user.template' as False. Restart Management server
        2. Create a domain
        3. Create a domain admin and a domain user
        4. Create a vm as domain user
        5. take snapshot of root disk as user vm
        6. try to create public template from snapshot . It should fail
        7. stop the VM
        8. take the public template from volume. it should fail
        9. register a public template as a domain user . it should fail
        10. create a VM  as domain admin
        11. create a snapshot of root disk as domain admin
        12 create a public template of the snapshot .it should fail
        13. Register a public template as domain admin. it should fail
        14 Stop the vm as domain admin
        15. Create a template from volume as domain admin . it should fail

        """
        self.updateConfigurAndRestart("allow.public.user.templates", "false")
        
        user_account = Account.create(
            self.apiclient,
            self.testdata["account2"],
            admin=False,
            domainid=self.domain.id
        )
        admin_user = self.account.user[0]
        self.admin_api_client = self.testClient.getUserApiClient(
            admin_user.username,
            self.domain.name)
        user = user_account.user[0]
        self.user_api_client = self.testClient.getUserApiClient(
            user.username,
            self.domain.name)

        self.testdata["templates"]["ispublic"] = True
        # Register new public template as domain user
        # Exception should be raised for registering public template
        try:
            template = Template.register(
                self.user_api_client,
                self.testdata["templates"],
                zoneid=self.zone.id,
                account=user_account.name,
                domainid=user_account.domainid,
                hypervisor=self.hypervisor
            )
            self.updateConfigurAndRestart("allow.public.user.templates", "true")
            self.fail("Template creation passed for user")
        except CloudstackAPIException as e:
            self.assertRaises("Exception Raised : %s" % e)
        # Register new public template as domain admin
        # Exception should be raised for registering public template
        try:
            template = Template.register(
                self.admin_api_client,
                self.testdata["templates"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                hypervisor=self.hypervisor
            )
            self.updateConfigurAndRestart("allow.public.user.templates", "true")
            self.fail("Template creation passed for domain admin")
        except CloudstackAPIException as e:
            self.assertRaises("Exception Raised : %s" % e)

        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            self.updateConfigurAndRestart("allow.public.user.templates", "true")
            return
        else:
            user_vm_created = VirtualMachine.create(
                self.user_api_client,
                self.testdata["virtual_machine"],
                accountid=user_account.name,
                domainid=user_account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(user_vm_created,
                                 "VM creation failed"
            )
            # Get the Root disk of VM
            volume = list_volumes(
                self.user_api_client,
                virtualmachineid=user_vm_created.id,
                type='ROOT',
                listall=True
            )
            snapshot_created = Snapshot.create(
                self.user_api_client,
                volume[0].id,
                account=user_account.name,
                domainid=user_account.domainid
            )
            self.assertIsNotNone(
                snapshot_created,
                "Snapshot creation failed"
            )
            self.debug("Creating a template from snapshot: %s" % snapshot_created.id)
            #
            # Generate public template from the snapshot
            self.testdata["template"]["ispublic"] = True
            try:
                user_template = Template.create_from_snapshot(
                    self.user_api_client,
                    snapshot_created,
                    self.testdata["template"]
                )
                self.updateConfigurAndRestart("allow.public.user.templates", "true")
                self.fail("Template creation passed from snapshot for domain user")
            except CloudstackAPIException as e:
                self.assertRaises("Exception Raised : %s" % e)

            VirtualMachine.stop(user_vm_created, self.user_api_client)
            list_stopped_vms_after = VirtualMachine.list(
                self.user_api_client,
                listall=self.testdata["listall"],
                domainid=user_account.domainid,
                state="Stopped")
            status = validateList(list_stopped_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "Stopped VM is not in Stopped state"
            )
            try:
                user_template = Template.create(
                    self.user_api_client, self.testdata["template"],
                    volume[0].id
                )
                self.updateConfigurAndRestart("allow.public.user.templates", "true")
                self.fail("Template creation passed from volume for domain user")
            except CloudstackAPIException as e:
                self.assertRaises("Exception Raised : %s" % e)

            admin_vm_created = VirtualMachine.create(
                self.admin_api_client,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
            )
            self.assertIsNotNone(
                admin_vm_created,
                "VM creation failed"
            )
            # Get the Root disk of VM
            volume = list_volumes(
                self.admin_api_client,
                virtualmachineid=admin_vm_created.id,
                type='ROOT',
                listall=True
            )
            snapshot_created = Snapshot.create(
                self.admin_api_client,
                volume[0].id,
                account=self.account.name,
                domainid=self.account.domainid
            )
            self.assertIsNotNone(
                snapshot_created,
                "Snapshot creation failed"
            )
            self.debug("Creating a template from snapshot: %s" % snapshot_created.id)
            #
            #    Generate public template from the snapshot
            try:
                admin_template = Template.create_from_snapshot(
                    self.admin_api_client,
                    snapshot_created,
                    self.testdata["template"]
                )
                self.updateConfigurAndRestart("allow.public.user.templates", "true")
                self.fail("Template creation passed from snapshot for domain admin")
            except CloudstackAPIException as e:
                self.assertRaises("Exception Raised : %s" % e)

            VirtualMachine.stop(admin_vm_created, self.admin_api_client)
            list_stopped_vms_after = VirtualMachine.list(
                self.admin_api_client,
                listall=self.testdata["listall"],
                domainid=self.account.domainid,
                state="Stopped")
            status = validateList(list_stopped_vms_after)
            self.assertEqual(
                PASS,
                status[0],
                "Stopped VM is not in Stopped state"
            )
            try:
                admin_template = Template.create(
                    self.admin_api_client, self.testdata["template"],
                    volume[0].id
                )
                self.updateConfigurAndRestart("allow.public.user.templates", "true")
                self.fail("Template creation passed from volume for domain admin")
            except CloudstackAPIException as e:
                self.assertRaises("Exception Raised : %s" % e)

            self.updateConfigurAndRestart("allow.public.user.templates", "true")
        return
