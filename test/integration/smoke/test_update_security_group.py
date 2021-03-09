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
Tests for updating security group name
"""

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import updateSecurityGroup, createSecurityGroup
from marvin.sshClient import SshClient
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (PhysicalNetwork,
                             Account,
                             Host,
                             TrafficType,
                             Domain,
                             Network,
                             NetworkOffering,
                             VirtualMachine,
                             ServiceOffering,
                             Zone,
                             SecurityGroup)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines,
                               list_routers,
                               list_hosts,
                               get_free_vlan)
from marvin.codes import (PASS, FAIL)
import logging
import random
import time

class TestUpdateSecurityGroup(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestUpdateSecurityGroup,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        if str(cls.zone.securitygroupsenabled) != "True":
            sys.exit(1)

        cls.logger = logging.getLogger("TestUpdateSecurityGroup")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        testClient = super(TestUpdateSecurityGroup, cls).getClsTestClient()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        # Create new domain, account, network and VM
        cls.user_domain = Domain.create(
            cls.apiclient,
            services=cls.testdata["acl"]["domain2"],
            parentdomainid=cls.domain.id)

        # Create account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["acl"]["accountD2"],
            admin=True,
            domainid=cls.user_domain.id
        )

        cls._cleanup.append(cls.account)
        cls._cleanup.append(cls.user_domain)

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_01_create_security_group(self):
        # Validate the following:
        #
        # 1. Create a new security group
        # 2. Update the security group with new name
        # 3. List the security group with new name as the keyword
        # 4. Make sure that the response is not empty

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)

        initial_secgroup_name = security_group.name
        new_secgroup_name = "testing-update-security-group"

        cmd = updateSecurityGroup.updateSecurityGroupCmd()
        cmd.id = security_group.id
        cmd.name = new_secgroup_name
        self.apiclient.updateSecurityGroup(cmd)

        new_security_group = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            keyword=new_secgroup_name
        )
        self.assertNotEqual(
            len(new_security_group),
            0,
            "Update security group failed"
        )

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_02_duplicate_security_group_name(self):
        # Validate the following
        #
        # 1. Create a security groups with name "test"
        # 2. Try to create another security group with name "test"
        # 3. Creation of second security group should fail

        security_group_name = "test"
        security_group = SecurityGroup.create(
            self.apiclient,
            {"name": security_group_name},
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with name: %s" % security_group.name)

        security_group_list = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            keyword=security_group.name
        )
        self.assertNotEqual(
            len(security_group_list),
            0,
            "Creating security group failed"
        )

        # Need to use createSecurituGroupCmd since SecurityGroup.create
        # adds random string to security group name
        with self.assertRaises(Exception):
            cmd = createSecurityGroup.createSecurityGroupCmd()
            cmd.name = security_group.name
            cmd.account = self.account.name
            cmd.domainid = self.account.domainid
            self.apiclient.createSecurityGroup(cmd)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_03_update_security_group_with_existing_name(self):
        # Validate the following
        #
        # 1. Create a security groups with name "test"
        # 2. Create another security group
        # 3. Try to update the second security group to update its name to "test"
        # 4. Update security group should fail

        # Create security group
        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)
        security_group_name = security_group.name

        # Make sure its created
        security_group_list = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            keyword=security_group_name
        )
        self.assertNotEqual(
            len(security_group_list),
            0,
            "Creating security group failed"
        )

        # Create another security group
        second_security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % second_security_group.id)

        # Make sure its created
        security_group_list = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            keyword=second_security_group.name
        )
        self.assertNotEqual(
            len(security_group_list),
            0,
            "Creating security group failed"
        )

        # Update the security group
        with self.assertRaises(Exception):
            cmd = updateSecurityGroup.updateSecurityGroupCmd()
            cmd.id = second_security_group.id
            cmd.name = security_group_name
            self.apiclient.updateSecurityGroup(cmd)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_04_update_security_group_with_empty_name(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Update the security group to an empty name
        # 3. Update security group should fail

        # Create security group
        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Make sure its created
        security_group_list = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            keyword=security_group.name
        )
        self.assertNotEqual(
            len(security_group_list),
            0,
            "Creating security group failed"
        )

        # Update the security group
        with self.assertRaises(Exception):
            cmd = updateSecurityGroup.updateSecurityGroupCmd()
            cmd.id = security_group.id
            cmd.name = ""
            self.apiclient.updateSecurityGroup(cmd)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_05_rename_security_group(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Update the security group and change its name to "default"
        # 3. Exception should be thrown as "default" name cant be used

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)

        with self.assertRaises(Exception):
            cmd = updateSecurityGroup.updateSecurityGroupCmd()
            cmd.id = security_group.id
            cmd.name = "default"
            self.apiclient.updateSecurityGroup(cmd)
