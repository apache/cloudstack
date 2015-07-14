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

""" P1 for Security groups
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, validateList
from marvin.cloudstackAPI import authorizeSecurityGroupIngress, revokeSecurityGroupIngress
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             SecurityGroup,
                             Router,
                             Host,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_process_status)
from marvin.sshClient import SshClient
from marvin.codes import PASS

# Import System modules
import time
import subprocess
import socket
import platform


class TestDefaultSecurityGroup(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDefaultSecurityGroup,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine_userdata"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine_userdata"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            admin=True,
            domainid=cls.domain.id
        )

        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestDefaultSecurityGroup,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_01_deployVM_InDefaultSecurityGroup(self):
        """Test deploy VM in default security group
        """

        # Validate the following:
        # 1. deploy Virtual machine using admin user
        # 2. listVM should show a VM in Running state
        # 3. listRouters should show one router running

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.debug("Deployed VM with ID: %s" % self.virtual_machine.id)
        self.cleanup.append(self.virtual_machine)

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check for list VM response"
        )
        vm_response = list_vm_response[0]
        self.assertNotEqual(
            len(list_vm_response),
            0,
            "Check VM available in List Virtual Machines"
        )

        self.assertEqual(

            vm_response.id,
            self.virtual_machine.id,
            "Check virtual machine id in listVirtualMachines"
        )

        self.assertEqual(
            vm_response.displayname,
            self.virtual_machine.displayname,
            "Check virtual machine displayname in listVirtualMachines"
        )

        # Verify List Routers response for account
        self.debug(
            "Verify list routers response for account: %s"
            % self.account.name
        )
        routers = Router.list(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "Check for list Routers response"
        )

        self.debug("Router Response: %s" % routers)
        self.assertEqual(
            len(routers),
            1,
            "Check virtual router is created for account or not"
        )
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_02_listSecurityGroups(self):
        """Test list security groups for admin account
        """

        # Validate the following:
        # 1. listSecurityGroups in admin account
        # 2. There should be one security group (default) listed
        #    for the admin account
        # 3. No Ingress Rules should be part of the default security group

        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertNotEqual(
            len(sercurity_groups),
            0,
            "Check List Security groups response"
        )
        self.debug("List Security groups response: %s" %
                   str(sercurity_groups))
        self.assertEqual(
            hasattr(sercurity_groups, 'ingressrule'),
            False,
            "Check ingress rule attribute for default security group"
        )
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_03_accessInDefaultSecurityGroup(self):
        """Test access in default security group
        """

        # Validate the following:
        # 1. deploy Virtual machine using admin user
        # 2. listVM should show a VM in Running state
        # 3. listRouters should show one router running

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.debug("Deployed VM with ID: %s" % self.virtual_machine.id)
        self.cleanup.append(self.virtual_machine)

        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check for list VM response"
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine.id
        )

        vm_response = list_vm_response[0]
        self.assertNotEqual(
            len(list_vm_response),
            0,
            "Check VM available in List Virtual Machines"
        )

        self.assertEqual(

            vm_response.id,
            self.virtual_machine.id,
            "Check virtual machine id in listVirtualMachines"
        )

        self.assertEqual(
            vm_response.displayname,
            self.virtual_machine.displayname,
            "Check virtual machine displayname in listVirtualMachines"
        )
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )

        self.debug("List Security groups response: %s" %
                   str(sercurity_groups))
        self.assertNotEqual(
            len(sercurity_groups),
            0,
            "Check List Security groups response"
        )
        self.assertEqual(
            hasattr(sercurity_groups, 'ingressrule'),
            False,
            "Check ingress rule attribute for default security group"
        )

        # SSH Attempt to VM should fail
        with self.assertRaises(Exception):
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            SshClient(
                self.virtual_machine.ssh_ip,
                self.virtual_machine.ssh_port,
                self.virtual_machine.username,
                self.virtual_machine.password
            )
        return


class TestAuthorizeIngressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestAuthorizeIngressRule,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine_userdata"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine_userdata"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestAuthorizeIngressRule,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_01_authorizeIngressRule(self):
        """Test authorize ingress rule
        """

        # Validate the following:
        # 1. Create Security group for the account.
        # 2. Createsecuritygroup (ssh-incoming) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. deployVirtualMachine into this security group (ssh-incoming)

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )

        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )

        self.debug(
            "Authorizing ingress rule for sec group ID: %s for ssh access" %
            security_group.id)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.debug("Deploying VM in account: %s" % self.account.name)
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.virtual_machine.ipaddress, e)
                      )
        return


class TestRevokeIngressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRevokeIngressRule, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine_userdata"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine_userdata"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestRevokeIngressRule,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def revokeSGRule(self, sgid):
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = sgid
        self.apiclient.revokeSecurityGroupIngress(cmd)
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_01_revokeIngressRule(self):
        """Test revoke ingress rule
        """

        # Validate the following:
        # 1. Create Security group for the account.
        # 2. Createsecuritygroup (ssh-incoming) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. deployVirtualMachine into this security group (ssh-incoming)
        # 5. Revoke the ingress rule, SSH access should fail

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )

        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )
        # Authorize Security group to SSH to VM
        self.debug(
            "Authorizing ingress rule for sec group ID: %s for ssh access" %
            security_group.id)
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.debug("Deploying VM in account: %s" % self.account.name)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.virtual_machine.ipaddress, e)
                      )

        self.debug("Revoking ingress rule for sec group ID: %s for ssh access"
                   % security_group.id)
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            validateList(security_groups)[0],
            PASS,
            "Security groups list validation failed"
        )
        for sg in security_groups:
            if not sg.ingressrule:
                continue
            self.revokeSGRule(sg.ingressrule[0].ruleid)

        # SSH Attempt to VM should fail
        with self.assertRaises(Exception):
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            SshClient(self.virtual_machine.ssh_ip,
                      self.virtual_machine.ssh_port,
                      self.virtual_machine.username,
                      self.virtual_machine.password,
                      retries=5
                      )
        return


class TestDhcpOnlyRouter(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDhcpOnlyRouter, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine_userdata"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine_userdata"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.testdata["virtual_machine_userdata"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.testdata['mode']
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestDhcpOnlyRouter,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["sg", "eip", "basic"], required_hardware="true")
    def test_01_dhcpOnlyRouter(self):
        """Test router services for user account
        """

        # Validate the following
        # 1. List routers for any user account
        # 2. The only service supported by this router should be dhcp

        # Find router associated with user account
        list_router_response = Router.list(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        hosts = Host.list(
            self.apiclient,
            zoneid=router.zoneid,
            type='Routing',
            state='Up',
            id=router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list host returns a valid list"
        )
        host = hosts[0]

        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )

        result = get_process_status(
            host.ipaddress,
            self.testdata['configurableData']['host']["publicport"],
            self.testdata['configurableData']['host']["username"],
            self.testdata['configurableData']['host']["password"],
            router.linklocalip,
            "service dnsmasq status"
        )
        res = str(result)
        self.debug("Dnsmasq process status: %s" % res)

        self.assertEqual(
            res.count("running"),
            1,
            "Check dnsmasq service is running or not"
        )
        return


class TestdeployVMWithUserData(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestdeployVMWithUserData,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine_userdata"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine_userdata"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestdeployVMWithUserData,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_01_deployVMWithUserData(self):
        """Test Deploy VM with User data"""

        # Validate the following
        # 1. CreateAccount of type user
        # 2. CreateSecurityGroup ssh-incoming
        # 3. authorizeIngressRule to allow ssh-access
        # 4. deployVirtualMachine into this group with some
        #    base64 encoded user-data
        # 5. wget http://10.1.1.1/latest/user-data to get the
        #    latest userdata from the router for this VM

        # Find router associated with user account
        list_router_response = Router.list(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )

        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))

        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.debug("Deploying VM in account: %s" % self.account.name)
        # Should be able to SSH VM
        try:
            self.debug(
                "SSH to VM with IP Address: %s"
                % self.virtual_machine.ssh_ip
            )

            ssh = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (self.virtual_machine.ipaddress, e)
                      )

        cmds = [
            "wget http://%s/latest/user-data" % router.guestipaddress,
            "cat user-data",
        ]
        for c in cmds:
            result = ssh.execute(c)
            self.debug("%s: %s" % (c, result))

        res = str(result)
        self.assertEqual(
            res.count(self.testdata["virtual_machine_userdata"]["userdata"]),
            1,
            "Verify user data"
        )
        return


class TestDeleteSecurityGroup(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata['mode'] = self.zone.networktype

        template = get_template(
            self.apiclient,
            self.zone.id,
            self.testdata["ostype"]
        )

        self.testdata["domainid"] = self.domain.id
        self.testdata["virtual_machine_userdata"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine_userdata"]["template"] = template.id

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [
            self.account,
            self.service_offering
        ]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeleteSecurityGroup, cls).getClsTestClient()
        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestDeleteSecurityGroup,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_01_delete_security_grp_running_vm(self):
        """Test delete security group with running VM"""

        # Validate the following
        # 1. createsecuritygroup (ssh-incoming) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. deleteSecurityGroup created in step 1. Deletion should fail
        #    complaining there are running VMs in this group

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )

        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))

        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id]
        )
        self.debug("Deploying VM in account: %s" % self.account.name)

        # Deleting Security group should raise exception
        with self.assertRaises(Exception):
            security_group.delete(self.apiclient)

        # sleep to ensure that Security group is deleted properly
        time.sleep(self.testdata["sleep"])

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            id=security_group.id
        )
        self.assertNotEqual(
            sercurity_groups,
            None,
            "Check List Security groups response"
        )
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_02_delete_security_grp_withoout_running_vm(self):
        """Test delete security group without running VM"""

        # Validate the following
        # 1. createsecuritygroup (ssh-incoming) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. deleteSecurityGroup created in step 1. Deletion should fail
        #    complaining there are running VMs in this group

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )

        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id]
        )
        self.debug("Deploying VM in account: %s" % self.account.name)

        # Destroy the VM
        self.virtual_machine.delete(self.apiclient, expunge=True)

        try:
            self.debug("Deleting Security Group: %s" % security_group.id)
            security_group.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete security group - ID: %s: %s"
                      % (security_group.id, e)
                      )
        return


class TestIngressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata['mode'] = self.zone.networktype

        template = get_template(
            self.apiclient,
            self.zone.id,
            self.testdata["ostype"]
        )

        self.testdata["domainid"] = self.domain.id
        self.testdata["virtual_machine_userdata"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine_userdata"]["template"] = template.id

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [
            self.account,
            self.service_offering
        ]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestIngressRule, cls).getClsTestClient()
        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.api_client = cls.testClient.getApiClient()
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestIngressRule,
                cls).getClsTestClient().getApiClient()
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_01_authorizeIngressRule_AfterDeployVM(self):
        """Test delete security group with running VM"""

        # Validate the following
        # 1. createsecuritygroup (ssh-incoming, 22via22) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. authorizeSecurityGroupIngress to allow ssh access (startport:222
        #    to endport:22) to the VM

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))

        # Authorize Security group to SSH to VM
        ingress_rule_1 = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule_1, dict),
            True,
            "Check ingress rule created properly"
        )
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.debug("Deploying VM in account: %s" % self.account.name)

        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))
        # Authorize Security group to SSH to VM
        ingress_rule_2 = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule_ICMP"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule_2, dict),
            True,
            "Check ingress rule created properly"
        )
        # SSH should be allowed on 22 & 2222 ports
        try:
            self.debug("Trying to SSH into VM %s on port %s" % (
                self.virtual_machine.ssh_ip,
                self.testdata["ingress_rule"]["endport"]
            ))
            self.virtual_machine.get_ssh_client()

        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s, %s"
                      % (ingress_rule_1["id"], e))

        # User should be able to ping VM
        try:
            self.debug("Trying to ping VM %s" % self.virtual_machine.ssh_ip)
            platform_type = platform.system().lower()
            if platform_type == 'windows':
                result = subprocess.call(
                    ['ping', '-n', '1', self.virtual_machine.ssh_ip])
            else:
                result = subprocess.call(
                    ['ping', '-c 1', self.virtual_machine.ssh_ip])
            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertEqual(
                result,
                0,
                "Check if ping is successful or not"
            )

        except Exception as e:
            self.fail("Ping failed for ingress rule ID: %s, %s"
                      % (ingress_rule_2["id"], e))
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_02_revokeIngressRule_AfterDeployVM(self):
        """Test Revoke ingress rule after deploy VM"""

        # Validate the following
        # 1. createsecuritygroup (ssh-incoming, 22via22) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. authorizeSecurityGroupIngress to allow ssh access (startport:222
        #    to endport:22) to the VM
        # 5. check ssh access via port 222
        # 6. revokeSecurityGroupIngress to revoke rule added in step 5. verify
        #    that ssh-access into the VM is now NOT allowed through ports 222
        #    but allowed through port 22

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )
        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )

        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))

        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.debug("Deploying VM in account: %s" % self.account.name)

        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))

        # Authorize Security group to SSH to VM
        ingress_rule_2 = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule_ICMP"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule_2, dict),
            True,
            "Check ingress rule created properly"
        )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__
        icmp_rule = (ingress_rule_2["ingressrule"][0]).__dict__

        # SSH should be allowed on 22
        try:
            self.debug("Trying to SSH into VM %s on port %s" % (
                self.virtual_machine.ssh_ip,
                self.testdata["ingress_rule"]["endport"]
            ))
            self.virtual_machine.get_ssh_client()

        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s, %s"
                      % (ssh_rule["ruleid"], e))

        # User should be able to ping VM
        try:
            self.debug("Trying to ping VM %s" % self.virtual_machine.ssh_ip)
            platform_type = platform.system().lower()
            if platform_type == 'windows':
                result = subprocess.call(
                    ['ping', '-n', '1', self.virtual_machine.ssh_ip])
            else:
                result = subprocess.call(
                    ['ping', '-c 1', self.virtual_machine.ssh_ip])
            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertEqual(
                result,
                0,
                "Check if ping is successful or not"
            )
        except Exception as e:
            self.fail("Ping failed for ingress rule ID: %s, %s"
                      % (icmp_rule["ruleid"], e))
        self.debug(
            "Revoke Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))
        #skip revoke and ping tests in case of EIP/ELB zone
        vm_res = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )
        self.assertEqual(validateList(vm_res)[0], PASS, "invalid vm response")
        vm_nw = Network.list(
            self.apiclient,
            id=vm_res[0].nic[0].networkid
        )
        self.assertEqual(validateList(vm_nw)[0], PASS, "invalid network response")
        vm_nw_off = vm_nw[0].networkofferingname
        if vm_nw_off != "DefaultSharedNetscalerEIPandELBNetworkOffering":
            result = security_group.revoke(
                self.apiclient,
                id=icmp_rule["ruleid"]
            )
            self.debug("Revoke ingress rule result: %s" % result)
            time.sleep(self.testdata["sleep"])
            # User should not be able to ping VM
            try:
                self.debug("Trying to ping VM %s" % self.virtual_machine.ssh_ip)
                if platform_type == 'windows':
                    result = subprocess.call(
                        ['ping', '-n', '1', self.virtual_machine.ssh_ip])
                else:
                    result = subprocess.call(
                        ['ping', '-c 1', self.virtual_machine.ssh_ip])

                self.debug("Ping result: %s" % result)
                # if ping successful, then result should be 0
                self.assertNotEqual(
                    result,
                    0,
                    "Check if ping is successful or not"
                )
            except Exception as e:
                self.fail("Ping failed for ingress rule ID: %s, %s"
                          % (icmp_rule["ruleid"], e))
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_03_stopStartVM_verifyIngressAccess(self):
        """Test Start/Stop VM and Verify ingress rule"""

        # Validate the following
        # 1. createsecuritygroup (ssh-incoming, 22via22) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. once the VM is running and ssh-access is available,
        #    stopVirtualMachine
        # 5. startVirtualMachine. After stop start of the VM is complete
        #    verify that ssh-access to the VM is allowed

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(sercurity_groups, list),
            True,
            "Check for list security groups response"
        )

        self.assertEqual(
            len(sercurity_groups),
            2,
            "Check List Security groups response"
        )

        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s"
            % (
                security_group.id,
                self.account.name
            ))

        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(ingress_rule, dict),
            True,
            "Check ingress rule created properly"
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.debug("Deploying VM in account: %s" % self.account.name)

        # SSH should be allowed on 22 port
        try:
            self.debug(
                "Trying to SSH into VM %s" %
                self.virtual_machine.ssh_ip)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s"
                      % ingress_rule["id"]
                      )

        try:
            self.virtual_machine.stop(self.apiclient)
            self.virtual_machine.start(self.apiclient)
            # Sleep to ensure that VM is in running state
            time.sleep(self.testdata["sleep"])
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        # SSH should be allowed on 22 port after restart
        try:
            self.debug(
                "Trying to SSH into VM %s" %
                self.virtual_machine.ssh_ip)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s"
                      % ingress_rule["id"]
                      )
        return

class TestIngressRuleSpecificIpSet(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestIngressRuleSpecificIpSet,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            admin=True,
            domainid=cls.domain.id
        )

        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def getLocalMachineIpAddress(self):
        """ Get IP address of the machine on which test case is running """
        socket_ = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        socket_.connect(('8.8.8.8', 0))
        return socket_.getsockname()[0]

    def setHostConfiguration(self):
        """ Set necessary configuration on hosts for correct functioning of
        ingress rules """

        hosts = Host.list(self.apiclient,
                type="Routing",
                listall=True)

        for host in hosts:
            sshClient = SshClient(
                host.ipaddress,
                self.testdata["configurableData"]["host"]["publicport"],
                self.testdata["configurableData"]["host"]["username"],
                self.testdata["configurableData"]["host"]["password"]
            )

            qresultset = self.dbclient.execute(
                        "select guid from host where uuid = '%s';" %
                            host.id, db="cloud")
            self.assertNotEqual(
                        len(qresultset),
                            0,
                                "Check DB Query result set"
                                )
            hostguid = qresultset[-1][0]

            commands = ["echo 1 > /proc/sys/net/bridge/bridge-nf-call-iptables",
                        "echo 1 > /proc/sys/net/bridge/bridge-nf-call-arptables",
                        "sysctl -w net.bridge.bridge-nf-call-iptables=1",
                        "sysctl -w net.bridge.bridge-nf-call-arptables=1",
                        "xe host-param-clear param-name=tags uuid=%s" % hostguid
                        ]

            for command in commands:
                response = sshClient.execute(command)
                self.debug(response)

            Host.reconnect(self.apiclient, id=host.id)

            retriesCount = 10
            while retriesCount >= 0:
                hostsList = Host.list(self.apiclient,
                                      id=host.id)
                if hostsList[0].state.lower() == "up":
                    break
                time.sleep(60)
                retriesCount -= 1
                if retriesCount == 0:
                    raise Exception("Host failed to come in up state")

            sshClient = SshClient(
                host.ipaddress,
                self.testdata["configurableData"]["host"]["publicport"],
                self.testdata["configurableData"]["host"]["username"],
                self.testdata["configurableData"]["host"]["password"]
            )

            response = sshClient.execute("service xapi restart")
            self.debug(response)
        return

    def revokeSGRule(self, sgid):
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = sgid
        self.apiclient.revokeSecurityGroupIngress(cmd)
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_ingress_rules_specific_IP_set(self):
        """Test ingress rules for specific IP set

        # Validate the following:
        # 1. Create an account and add ingress rule
             (CIDR 0.0.0.0/0) in default security group
        # 2. Deploy 2 VMs in the default sec group
        # 3. Check if SSH works for the VMs from test machine, should work
        # 4. Check if SSH works for the VM from different machine (
             for instance, management server), should work
        # 5. Revoke the ingress rule and add ingress rule for specific IP
             set (including test machine)
        # 6. Add new Vm to default sec group
        # 7. Verify that SSH works to VM from test machine
        # 8. Verify that SSH does not work to VM from different machine which
             is outside specified IP set
        """

        # Default Security group should not have any ingress rule
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            validateList(security_groups)[0],
            PASS,
            "Security groups list validation failed"
        )

        defaultSecurityGroup = security_groups[0]

        # Authorize Security group to SSH to VM
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = defaultSecurityGroup.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = '0.0.0.0/0'
        ingress_rule = self.apiclient.authorizeSecurityGroupIngress(cmd)

        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[defaultSecurityGroup.id],
            mode=self.testdata['mode']
        )
        self.cleanup.append(virtual_machine_1)
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[defaultSecurityGroup.id],
            mode=self.testdata['mode']
        )
        self.cleanup.append(virtual_machine_2)
        try:
            SshClient(
                virtual_machine_1.ssh_ip,
                virtual_machine_1.ssh_port,
                virtual_machine_1.username,
                virtual_machine_1.password
            )
        except Exception as e:
            self.revokeSGRule(ingress_rule.ingressrule[0].ruleid)
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine_1.ipaddress, e)
            )

        try:
            SshClient(
                virtual_machine_2.ssh_ip,
                virtual_machine_2.ssh_port,
                virtual_machine_2.username,
                virtual_machine_2.password
            )
        except Exception as e:
            self.revokeSGRule(ingress_rule.ingressrule[0].ruleid)
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine_2.ipaddress, e)
            )
        try:
            sshClient = SshClient(
                self.mgtSvrDetails["mgtSvrIp"],
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"]
            )
        except Exception as e:
            self.revokeSGRule(ingress_rule.ingressrule[0].ruleid)
            self.fail("SSH Access failed for %s: %s" %
                      (self.mgtSvrDetails["mgtSvrIp"], e)
            )
        response = sshClient.execute("ssh %s@%s -v" %
                                     (virtual_machine_1.username,
                                      virtual_machine_1.ssh_ip))
        self.debug("Response is :%s" % response)

        self.assertTrue("connection established" in str(response).lower(),
                        "SSH to VM at %s failed from external machine ip %s other than test machine" %
                        (virtual_machine_1.ssh_ip,
                         self.mgtSvrDetails["mgtSvrIp"]))

        response = sshClient.execute("ssh %s@%s -v" %
                                     (virtual_machine_2.username,
                                      virtual_machine_2.ssh_ip))
        self.debug("Response is :%s" % response)

        self.assertTrue("connection established" in str(response).lower(),
                        "SSH to VM at %s failed from external machine ip %s other than test machine" %
                        (virtual_machine_2.ssh_ip,
                         self.mgtSvrDetails["mgtSvrIp"]))
        virtual_machine_3 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[defaultSecurityGroup.id],
            mode=self.testdata['mode']
        )
        self.cleanup.append(virtual_machine_3)
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            validateList(security_groups)[0],
            PASS,
            "Security groups list validation failed"
        )
        for sg in security_groups:
            if not sg.ingressrule:
                continue
            self.revokeSGRule(sg.ingressrule[0].ruleid)
        localMachineIpAddress = self.getLocalMachineIpAddress()
        cidr = localMachineIpAddress + "/32"

        # Authorize Security group to SSH to VM
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = defaultSecurityGroup.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ingress_rule = self.apiclient.authorizeSecurityGroupIngress(cmd)
        if self.testdata["configurableData"]["setHostConfigurationForIngressRule"]:
            self.setHostConfiguration()
            time.sleep(180)

        virtual_machine_3.stop(self.apiclient)
        virtual_machine_3.start(self.apiclient)

        try:
            sshClient = SshClient(
                virtual_machine_3.ssh_ip,
                virtual_machine_3.ssh_port,
                virtual_machine_3.username,
                virtual_machine_3.password
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine_3.ssh_ip, e)
            )

        sshClient = SshClient(
            self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )

        response = sshClient.execute("ssh %s@%s -v" %
                                     (virtual_machine_3.username,
                                      virtual_machine_3.ssh_ip))
        self.debug("Response is :%s" % response)

        self.assertFalse("connection established" in str(response).lower(),
                         "SSH to VM at %s succeeded from external machine ip %s other than test machine" %
                         (virtual_machine_3.ssh_ip,
                          self.mgtSvrDetails["mgtSvrIp"]))
        return

    @attr(tags=["sg", "eip", "advancedsg"])
    def test_ingress_rules_specific_IP_set_non_def_sec_group(self):
        """Test ingress rules for specific IP set and non default security group

        # Validate the following:
        # 1. Create an account and add ingress rule
             (CIDR 0.0.0.0/0) in default security group
        # 2. Deploy 2 VMs in the default sec group
        # 3. Check if SSH works for the VMs from test machine, should work
        # 4. Check if SSH works for the VM from different machine (
             for instance, management server), should work
        # 5. Add new security group to the account and add ingress rule for
             specific IP set (including test machine)
        # 6. Add new Vm to new sec group
        # 7. Verify that SSH works to VM from test machine
        # 8. Verify that SSH does not work to VM from different machine which
             is outside specified IP set
        """

        # Default Security group should not have any ingress rule
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            validateList(security_groups)[0],
            PASS,
            "Security groups list validation failed"
        )

        defaultSecurityGroup = security_groups[0]

        # Authorize Security group to SSH to VM
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = defaultSecurityGroup.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = '0.0.0.0/0'
        self.apiclient.authorizeSecurityGroupIngress(cmd)

        virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[defaultSecurityGroup.id],
            mode=self.testdata['mode']
        )
        self.cleanup.append(virtual_machine_1)
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[defaultSecurityGroup.id],
            mode=self.testdata['mode']
        )
        self.cleanup.append(virtual_machine_2)
        try:
            SshClient(
                virtual_machine_1.ssh_ip,
                virtual_machine_1.ssh_port,
                virtual_machine_1.username,
                virtual_machine_1.password
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine_1.ipaddress, e)
                      )

        try:
            SshClient(
                virtual_machine_2.ssh_ip,
                virtual_machine_2.ssh_port,
                virtual_machine_2.username,
                virtual_machine_2.password
            )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine_2.ipaddress, e)
                      )

        sshClient = SshClient(
               self.mgtSvrDetails["mgtSvrIp"],
               22,
               self.mgtSvrDetails["user"],
               self.mgtSvrDetails["passwd"]
        )

        response = sshClient.execute("ssh %s@%s -v" %
                    (virtual_machine_1.username,
                        virtual_machine_1.ssh_ip))
        self.debug("Response is :%s" % response)

        self.assertTrue("connection established" in str(response).lower(),
                    "SSH to VM at %s failed from external machine ip %s other than test machine" %
                    (virtual_machine_1.ssh_ip,
                        self.mgtSvrDetails["mgtSvrIp"]))

        response = sshClient.execute("ssh %s@%s -v" %
                    (virtual_machine_2.username,
                        virtual_machine_2.ssh_ip))
        self.debug("Response is :%s" % response)

        self.assertTrue("connection established" in str(response).lower(),
                    "SSH to VM at %s failed from external machine ip %s other than test machine" %
                    (virtual_machine_2.ssh_ip,
                        self.mgtSvrDetails["mgtSvrIp"]))

        localMachineIpAddress = self.getLocalMachineIpAddress()
        cidr = localMachineIpAddress + "/32"

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        # Authorize Security group to SSH to VM
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        self.apiclient.authorizeSecurityGroupIngress(cmd)

        virtual_machine_3 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[security_group.id],
            mode=self.testdata['mode']
        )
        self.cleanup.append(virtual_machine_3)
        if self.testdata["configurableData"]["setHostConfigurationForIngressRule"]:
            self.setHostConfiguration()
            time.sleep(180)

        virtual_machine_3.stop(self.apiclient)
        virtual_machine_3.start(self.apiclient)

        try:
            sshClient = SshClient(
                virtual_machine_3.ssh_ip,
                virtual_machine_3.ssh_port,
                virtual_machine_3.username,
                virtual_machine_3.password
        )
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine_3.ssh_ip, e)
                      )
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            validateList(security_groups)[0],
            PASS,
            "Security groups list validation failed"
        )
        for sg in security_groups:
            if sg.id == security_group.id or not sg.ingressrule:
                continue
            self.revokeSGRule(sg.ingressrule[0].ruleid)
        sshClient = SshClient(
               self.mgtSvrDetails["mgtSvrIp"],
               22,
               self.mgtSvrDetails["user"],
               self.mgtSvrDetails["passwd"]
        )

        response = sshClient.execute("ssh %s@%s -v" %
                    (virtual_machine_3.username,
                        virtual_machine_3.ssh_ip))
        self.debug("Response is :%s" % response)

        self.assertFalse("connection established" in str(response).lower(),
                    "SSH to VM at %s succeeded from external machine ip %s other than test machine" %
                    (virtual_machine_3.ssh_ip,
                        self.mgtSvrDetails["mgtSvrIp"]))
        return
