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

""" Component tests for Nuage VSP SDN plugin's Domain Template feature
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account,
                             Configurations,
                             Domain,
                             Network,
                             User,
                             VirtualMachine)
from marvin.cloudstackAPI import (associateNuageVspDomainTemplate,
                                  listNuageVspDomainTemplates,
                                  listNuageVspGlobalDomainTemplate)
# Import System Modules
from nose.plugins.attrib import attr


class TestNuageDomainTemplate(nuageTestCase):
    """Test Nuage VSP SDN plugin's Domain Template feature
    """

    @classmethod
    def setUpClass(cls):
        """
        Create the following domain tree and accounts that are required for
        executing Nuage VSP SDN plugin's Domain Template feature test cases:
            Under ROOT - Create a domain D1
            Under domain D1 - Create a subdomain D11
            Under each of the domains - create an admin user and a regular
            user account.
        Create Nuage VSP VPC and network (tier) offerings
        Create a VPC with a VPC network (tier) under each of the admin accounts
        of the above domains
        Create three pre-configured Nuage VSP domain templates per enterprise
        in VSD corresponding to each of the above domains
        """

        super(TestNuageDomainTemplate, cls).setUpClass()
        cls.domains_accounts_data = cls.test_data["acl"]

        try:
            # Backup default (ROOT admin user) apikey and secretkey
            cls.default_apikey = cls.api_client.connection.apiKey
            cls.default_secretkey = cls.api_client.connection.securityKey

            # Create domains
            cls.domain_1 = Domain.create(
                cls.api_client,
                cls.domains_accounts_data["domain1"]
            )
            cls._cleanup.append(cls.domain_1)

            cls.domain_11 = Domain.create(
                cls.api_client,
                cls.domains_accounts_data["domain11"],
                parentdomainid=cls.domain_1.id
            )
            cls._cleanup.append(cls.domain_11)

            # Create an admin and an user account under ROOT domain
            cls.account_root = Account.create(
                cls.api_client,
                cls.domains_accounts_data["accountROOT"],
                admin=True,
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_root)
            cls.user_root_apikey = user.apikey
            cls.user_root_secretkey = user.secretkey
            cls._cleanup.append(cls.account_root)

            cls.account_roota = Account.create(
                cls.api_client,
                cls.domains_accounts_data["accountROOTA"],
                admin=False,
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_roota)
            cls.user_roota_apikey = user.apikey
            cls.user_roota_secretkey = user.secretkey
            cls._cleanup.append(cls.account_roota)

            # Create an admin and an user account under domain D1
            cls.account_d1 = Account.create(
                cls.api_client,
                cls.domains_accounts_data["accountD1"],
                admin=True,
                domainid=cls.domain_1.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d1)
            cls.user_d1_apikey = user.apikey
            cls.user_d1_secretkey = user.secretkey
            cls._cleanup.append(cls.account_d1)

            cls.account_d1a = Account.create(
                cls.api_client,
                cls.domains_accounts_data["accountD1A"],
                admin=False,
                domainid=cls.domain_1.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d1a)
            cls.user_d1a_apikey = user.apikey
            cls.user_d1a_secretkey = user.secretkey
            cls._cleanup.append(cls.account_d1a)

            # Create an admin and an user account under subdomain D11
            cls.account_d11 = Account.create(
                cls.api_client,
                cls.domains_accounts_data["accountD11"],
                admin=True,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d11)
            cls.user_d11_apikey = user.apikey
            cls.user_d11_secretkey = user.secretkey
            cls._cleanup.append(cls.account_d11)

            cls.account_d11a = Account.create(
                cls.api_client,
                cls.domains_accounts_data["accountD11A"],
                admin=False,
                domainid=cls.domain_11.id
            )
            user = cls.generateKeysForUser(cls.api_client, cls.account_d11a)
            cls.user_d11a_apikey = user.apikey
            cls.user_d11a_secretkey = user.secretkey
            cls._cleanup.append(cls.account_d11a)

            # Create VPC offering
            cls.vpc_offering = cls.create_VpcOffering(
                cls.test_data["nuagevsp"]["vpc_offering"])

            # Create VPC network (tier) offering
            cls.network_offering = cls.create_NetworkOffering(
                cls.test_data["nuagevsp"]["vpc_network_offering"])

            # Create a VPC with a VPC network (tier) under each of the admin
            # accounts of ROOT domain, domain D1, and subdomain D11
            # Create 500 pre-configured Nuage VSP domain templates per
            # enterprise in VSD corresponding to each of the above domains
            cls.cleanup_domain_templates = []
            cls.domain_template_list = []
            for i in range(0, 3):
                cls.domain_template_list.append("domain_template_" + str(i))
            for account in [cls.account_root, cls.account_d1, cls.account_d11]:
                vpc = cls.create_Vpc(
                    cls.vpc_offering, cidr='10.1.0.0/16', account=account)
                cls.create_Network(
                    cls.network_offering,
                    vpc=vpc,
                    account=account)
                for domain_template in cls.domain_template_list:
                    new_domain_template = cls.vsdk.NUDomainTemplate(
                        name=domain_template,
                        description=domain_template)
                    enterprise = cls._session.user.enterprises.get_first(
                        filter="externalID BEGINSWITH '%s'" % account.domainid)
                    enterprise.create_child(new_domain_template)
                    cls.cleanup_domain_templates.append(
                        enterprise.domain_templates.get_first(
                            filter="name is '%s'" % domain_template))
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Failed to create the setup required to execute "
                            "the test cases: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        # Restore back default (ROOT admin user) apikey and secretkey
        cls.api_client.connection.apiKey = cls.default_apikey
        cls.api_client.connection.securityKey = cls.default_secretkey
        # Cleanup resources used
        cls.debug("Cleaning up the resources")
        for domain_template in cls.cleanup_domain_templates:
            try:
                domain_template.delete()
            except Exception as e:
                cls.error("Failed to cleanup domain template %s in VSD, got "
                          "%s" % (domain_template, e))
        cls.cleanup_domain_templates = []
        for obj in reversed(cls._cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(cls.api_client, expunge=True)
                else:
                    obj.delete(cls.api_client)
            except Exception as e:
                cls.error("Failed to cleanup %s, got %s" % (obj, e))
        try:
            cls.vpc_offering.delete(cls.api_client)
            cls.network_offering.delete(cls.api_client)
            cls.service_offering.delete(cls.api_client)
        except Exception as e:
            cls.error("Failed to cleanup offerings - %s" % e)
        # cleanup_resources(cls.api_client, cls._cleanup)
        cls._cleanup = []
        cls.debug("Cleanup complete!")
        return

    def setUp(self):
        self.account = self.account_root
        self.cleanup = []
        return

    def tearDown(self):
        # Restore back default (ROOT admin user) apikey and secretkey
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        # Cleanup resources used
        self.debug("Cleaning up the resources")
        for obj in reversed(self.cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(self.api_client, expunge=True)
                else:
                    obj.delete(self.api_client)
            except Exception as e:
                self.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(self.api_client, self.cleanup)
        self.cleanup = []
        self.debug("Cleanup complete!")
        return

    @staticmethod
    def generateKeysForUser(api_client, account):
        user = User.list(
            api_client,
            account=account.name,
            domainid=account.domainid
        )[0]

        return (User.registerUserKeys(
            api_client,
            user.id
        ))

    # list_NuageVspDomainTemplates - Lists pre-configured Nuage VSP domain
    # template(s) for the given domain/account user
    def list_NuageVspDomainTemplates(self, account=None, name=None):
        if not account:
            account = self.account
        cmd = listNuageVspDomainTemplates.listNuageVspDomainTemplatesCmd()
        cmd.domainid = account.domainid
        cmd.zoneid = self.zone.id
        domain_templates = self.api_client.listNuageVspDomainTemplates(cmd)
        if name:
            return [domain_template for domain_template in domain_templates
                    if str(domain_template.name) == name]
        else:
            return domain_templates

    # validate_NuageVspDomainTemplate - Validates the given pre-configured
    # Nuage VSP domain template for the given domain/account user
    def validate_NuageVspDomainTemplate(self, name, account=None):
        """Validates the pre-configured Nuage VSP domain template"""
        if not account:
            account = self.account
        self.debug("Validating the availability of pre-configured Nuage VSP "
                   "domain template - %s for domain/account user - %s "
                   % (name, account))
        domain_templates = self.list_NuageVspDomainTemplates(
            name=name, account=account)
        self.assertEqual(isinstance(domain_templates, list), True,
                         "List Nuage VSP Domain Templates should return a "
                         "valid list"
                         )
        self.assertEqual(domain_templates[0].name, name,
                         "Name of the Nuage VSP Domain Template should "
                         "be in the returned list"
                         )
        self.debug("Successfully validated the availability of pre-configured "
                   "Nuage VSP domain template - %s for domain/account user - "
                   "%s" % (name, account))

    # associate_NuageVspDomainTemplate - Associates the given pre-configured
    # Nuage VSP domain template to the given VPC
    def associate_NuageVspDomainTemplate(self, domain_template_name, vpc):
        cmd = associateNuageVspDomainTemplate.\
            associateNuageVspDomainTemplateCmd()
        cmd.domaintemplate = domain_template_name
        cmd.vpcid = vpc.id
        cmd.zoneid = self.zone.id
        return self.api_client.associateNuageVspDomainTemplate(cmd)

    # update_NuageVspGlobalDomainTemplate - Updates the global setting
    # nuagevsp.vpc.domaintemplate.name with the given value
    def update_NuageVspGlobalDomainTemplate(self, value):
        self.debug("Updating global setting nuagevsp.vpc.domaintemplate.name "
                   "with value - %s" % value)
        self.user_apikey = self.api_client.connection.apiKey
        self.user_secretkey = self.api_client.connection.securityKey
        self.api_client.connection.apiKey = self.default_apikey
        self.api_client.connection.securityKey = self.default_secretkey
        Configurations.update(self.api_client,
                              name="nuagevsp.vpc.domaintemplate.name",
                              value=value)
        self.api_client.connection.apiKey = self.user_apikey
        self.api_client.connection.securityKey = self.user_secretkey
        self.debug("Successfully updated global setting "
                   "nuagevsp.vpc.domaintemplate.name with value - %s" % value)

    # list_NuageVspGlobalDomainTemplate - Lists the name of the global/default
    # pre-configured Nuage VSP domain template as mentioned in the global
    # setting "nuagevsp.vpc.domaintemplate.name"
    def list_NuageVspGlobalDomainTemplate(self):
        cmd = listNuageVspGlobalDomainTemplate.\
            listNuageVspGlobalDomainTemplateCmd()
        return self.api_client.listNuageVspGlobalDomainTemplate(cmd)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_nuage_Domain_Template_selection_per_VPC(self):
        """Test Nuage VSP Domain Template selection per VPC
        """

        # 1. Associate an invalid/non-existing Nuage VSP domain template to a
        #    VPC; verify that the association fails.
        # 2. Associate a valid/existing pre-configured Nuage VSP domain
        #    template to a VPC; verify that the association is successful, VPC
        #    networks (domains) are instantiated from the associated domain
        #    template in VSD.
        # 3. Verify that the state of such VPC networks (domains) in VSD is not
        #    affected with their restarts in CloudStack with and without
        #    cleanup.
        # 4. Verify that multiple associations (update) of domain templates to
        #    a VPC goes through till the creation of its first VPC network
        #    (tier).
        # 5. Verify that the VPC networks (domains) creation fails in VSD when
        #    the associated domain templates to their corresponding VPCs have
        #    been deleted in VSD.
        # 6. Verify that the VPC networks (domains) creation fails in VSD when
        #    an acl list is associated with them after their corresponding VPCs
        #    have been associated with a pre-configured Nuage VSP domain
        #    template.
        # 7. Delete all the created objects (cleanup).

        # Creating VPC
        vpc_1 = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')

        # Associating pre-configured Nuage VSP Domain Template to VPC
        with self.assertRaises(Exception):
            self.validate_NuageVspDomainTemplate("invalid_domain_template")
        self.debug("There is no domain template with name "
                   "invalid_domain_template in VSD")
        with self.assertRaises(Exception):
            self.associate_NuageVspDomainTemplate(
                "invalid_domain_template", vpc_1)
        self.debug("Association fails as there is no domain template with "
                   "name invalid_domain_template in VSD")
        self.associate_NuageVspDomainTemplate(
            self.domain_template_list[0], vpc_1)

        # Creating VPC networks (tiers)
        vpc_1_tier_1 = self.create_Network(
            self.network_offering, gateway='10.1.3.1', vpc=vpc_1)
        vpc_1_tier_2 = self.create_Network(
            self.network_offering, gateway='10.1.4.1', vpc=vpc_1)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_1, vpc_1,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_2, vpc_1,
            domain_template_name=self.domain_template_list[0])

        # Restart VPC networks (tiers) without cleanup
        Network.restart(vpc_1_tier_1, self.api_client, cleanup=False)
        Network.restart(vpc_1_tier_2, self.api_client, cleanup=False)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_1, vpc_1,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_2, vpc_1,
            domain_template_name=self.domain_template_list[0])

        # Restart VPC networks (tiers) with cleanup
        Network.restart(vpc_1_tier_1, self.api_client, cleanup=True)
        Network.restart(vpc_1_tier_2, self.api_client, cleanup=True)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_1, vpc_1,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_2, vpc_1,
            domain_template_name=self.domain_template_list[0])

        # Restart VPC without cleanup
        self.restart_Vpc(vpc_1, cleanup=False)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_1, vpc_1,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_2, vpc_1,
            domain_template_name=self.domain_template_list[0])

        # Restart VPC with cleanup
        self.restart_Vpc(vpc_1, cleanup=True)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_1, vpc_1,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_2, vpc_1,
            domain_template_name=self.domain_template_list[0])

        # Creating VPC
        vpc_2 = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')

        # Associating pre-configured Nuage VSP Domain Template to VPC
        self.validate_NuageVspDomainTemplate(self.domain_template_list[0])
        self.associate_NuageVspDomainTemplate(
            self.domain_template_list[0], vpc_2)
        self.validate_NuageVspDomainTemplate(self.domain_template_list[1])
        self.associate_NuageVspDomainTemplate(
            self.domain_template_list[1], vpc_2)

        # Deleting the associated pre-configured Nuage VSP domain template
        enterprise = self._session.user.enterprises.get_first(
            filter="externalID BEGINSWITH '%s'" % self.account.domainid)
        domain_template = enterprise.domain_templates.get_first(
            filter="name is '%s'" % self.domain_template_list[1])
        domain_template.delete()

        # Creating VPC networks (tiers)
        with self.assertRaises(Exception):
            self.create_Network(
                self.network_offering,
                gateway='10.1.1.1',
                vpc=vpc_2)
        self.debug("Corresponding domain creation in VSD fails, but VPC "
                   "(tier) network gets created on CloudStack as the "
                   "associated pre-configured Nuage VSP domain template is no "
                   "longer existing in VSD")

        # Re-creating the associated pre-configured Nuage VSP domain template
        new_domain_template = self.vsdk.NUDomainTemplate(
            name=self.domain_template_list[1],
            description=self.domain_template_list[1])
        enterprise = self._session.user.enterprises.get_first(
            filter="externalID BEGINSWITH '%s'" % self.account.domainid)
        enterprise.create_child(new_domain_template)
        self.cleanup_domain_templates.append(
            enterprise.domain_templates.get_first(
                filter="name is '%s'" % self.domain_template_list[1]))

        vpc_2_tier_1 = self.create_Network(
            self.network_offering, gateway='10.1.2.1', vpc=vpc_2)
        vpc_2_tier_2 = self.create_Network(
            self.network_offering, gateway='10.1.3.1', vpc=vpc_2)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_2_tier_1, vpc_2,
            domain_template_name=self.domain_template_list[1])
        self.verify_vsd_network(
            self.account.domainid, vpc_2_tier_2, vpc_2,
            domain_template_name=self.domain_template_list[1])

        # Creating VPC
        vpc_3 = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')

        # Associating pre-configured Nuage VSP Domain Template to VPC
        self.validate_NuageVspDomainTemplate(self.domain_template_list[0])
        self.associate_NuageVspDomainTemplate(
            self.domain_template_list[0], vpc_3)

        # Creating an ACL list and an ACL item
        acl_list = self.create_NetworkAclList(
            name="acl", description="acl", vpc=vpc_3)
        self.create_NetworkAclRule(
            self.test_data["ingress_rule"], acl_list=acl_list)

        # Creating VPC networks (tiers)
        with self.assertRaises(Exception):
            self.create_Network(
                self.network_offering,
                gateway='10.1.1.1',
                vpc=vpc_3,
                acl_list=acl_list)
        self.debug("Corresponding domain creation in VSD fails, but VPC "
                   "(tier) network gets created on CloudStack as creation of "
                   "Network ACLs from CloudStack is not supported when the "
                   "VPC is associated with a Nuage VSP pre-configured domain "
                   "template")

        vpc_3_tier_1 = self.create_Network(
            self.network_offering, gateway='10.1.2.1', vpc=vpc_3)
        vpc_3_tier_2 = self.create_Network(
            self.network_offering, gateway='10.1.3.1', vpc=vpc_3)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_3_tier_1, vpc_3,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_3_tier_2, vpc_3,
            domain_template_name=self.domain_template_list[0])

        # Creating VPC and VPC network (tier)
        vpc = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')
        vpc_tier = self.create_Network(self.network_offering, vpc=vpc)

        # VSD verification
        self.verify_vsd_network(self.account.domainid, vpc_tier, vpc)

        # Associating pre-configured Nuage VSP Domain Template to VPC
        self.validate_NuageVspDomainTemplate(self.domain_template_list[0])
        with self.assertRaises(Exception):
            self.associate_NuageVspDomainTemplate(
                self.domain_template_list[0], vpc)
        self.debug("Association fails as the corresponding domain and domain "
                   "templates are already created in VSD for the VPC vpc")

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_02_nuage_Domain_Template_selection_per_VPC_as_ROOT_user(self):
        """Test Nuage VSP Domain Template selection per VPC as ROOT domain
        regular user
        """

        # Repeat the tests in the testcase
        # "test_01_nuage_Domain_Template_selection_per_VPC" as ROOT domain
        # regular user

        # Setting ROOT domain user account information
        self.account = self.account_roota

        # Setting ROOT domain user keys in api_client
        self.api_client.connection.apiKey = self.user_roota_apikey
        self.api_client.connection.securityKey = self.user_roota_secretkey

        # Calling testcase "test_01_nuage_Domain_Template_selection_per_VPC"
        self.test_01_nuage_Domain_Template_selection_per_VPC()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_03_nuage_Domain_Template_selection_per_VPC_as_domain_admin(self):
        """Test Nuage VSP Domain Template selection per VPC as domain admin
        user
        """

        # Repeat the tests in the testcase
        # "test_01_nuage_Domain_Template_selection_per_VPC" as domain admin
        # user

        # Setting domain D1 admin account information
        self.account = self.account_d1

        # Setting domain D1 admin keys in api_client
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey

        # Calling testcase "test_01_nuage_Domain_Template_selection_per_VPC"
        self.test_01_nuage_Domain_Template_selection_per_VPC()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_nuage_Domain_Template_selection_per_VPC_as_domain_user(self):
        """Test Nuage VSP Domain Template selection per VPC as domain
        regular user
        """

        # Repeat the tests in the testcase
        # "test_01_nuage_Domain_Template_selection_per_VPC" as domain regular
        # user

        # Setting domain D1 user account information
        self.account = self.account_d1a

        # Setting domain D1 user keys in api_client
        self.api_client.connection.apiKey = self.user_d1a_apikey
        self.api_client.connection.securityKey = self.user_d1a_secretkey

        # Calling testcase "test_01_nuage_Domain_Template_selection_per_VPC"
        self.test_01_nuage_Domain_Template_selection_per_VPC()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_05_nuage_Domain_Template_selection_per_VPC_as_subdom_admin(self):
        """Test Nuage VSP Domain Template selection per VPC as subdomain admin
        user
        """

        # Repeat the tests in the testcase
        # "test_01_nuage_Domain_Template_selection_per_VPC" as subdomain admin
        # user

        # Setting subdomain D11 admin account information
        self.account = self.account_d11

        # Setting subdomain D1 admin keys in api_client
        self.api_client.connection.apiKey = self.user_d11_apikey
        self.api_client.connection.securityKey = self.user_d11_secretkey

        # Calling testcase "test_01_nuage_Domain_Template_selection_per_VPC"
        self.test_01_nuage_Domain_Template_selection_per_VPC()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_06_nuage_Domain_Template_selection_per_VPC_as_subdom_user(self):
        """Test Nuage VSP Domain Template selection per VPC as subdomain
        regular user
        """

        # Repeat the tests in the testcase
        # "test_01_nuage_Domain_Template_selection_per_VPC" as subdomain
        # regular user

        # Setting subdomain D11 user account information
        self.account = self.account_d11a

        # Setting subdomain D11 user keys in api_client
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey

        # Calling testcase "test_01_nuage_Domain_Template_selection_per_VPC"
        self.test_01_nuage_Domain_Template_selection_per_VPC()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_07_nuage_Global_Domain_Template(self):
        """Test Nuage VSP Global Domain Template
        """

        # 1. Update the global setting "nuagevsp.vpc.domaintemplate.name" with
        #    an invalid/non-existing Nuage VSP domain template name; verify
        #    that a new VPC creation fails, and gets cleaned up.
        # 2. Update the global setting "nuagevsp.vpc.domaintemplate.name" with
        #    a valid/existing pre-configured Nuage VSP domain template name;
        #    verify that all VPC networks (domains) get instantiated from that
        #    pre-configured Nuage VSP domain template.
        # 3. Verify that multiple associations (update) of domain templates to
        #    such VPCs goes through till the creation of their first VPC
        #    networks (tiers).
        # 4. Delete all the created objects (cleanup).

        # Updating global setting "nuagevsp.vpc.domaintemplate.name"
        self.update_NuageVspGlobalDomainTemplate(
            value="invalid_domain_template")
        domain_template = self.list_NuageVspGlobalDomainTemplate()[0].name
        self.assertEqual(domain_template, "invalid_domain_template",
                         "Global setting nuagevsp.vpc.domaintemplate.name was "
                         "not updated successfully"
                         )
        with self.assertRaises(Exception):
            self.validate_NuageVspDomainTemplate("invalid_domain_template")
        self.debug("There is no domain template with name "
                   "invalid_domain_template in VSD")

        # Creating VPC
        with self.assertRaises(Exception):
            self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')
        self.debug("VPC creation fails as there is no domain template with "
                   "name invalid_domain_template in VSD as mentioned in "
                   "global setting nuagevsp.vpc.domaintemplate.name")

        # Updating global setting "nuagevsp.vpc.domaintemplate.name"
        self.update_NuageVspGlobalDomainTemplate(
            value=self.domain_template_list[0])
        domain_template = self.list_NuageVspGlobalDomainTemplate()[0].name
        self.assertEqual(domain_template, self.domain_template_list[0],
                         "Global setting nuagevsp.vpc.domaintemplate.name was "
                         "not updated successfully"
                         )
        self.validate_NuageVspDomainTemplate(self.domain_template_list[0])

        # Creating VPC and VPC networks (tiers)
        vpc_1 = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')
        vpc_1_tier_1 = self.create_Network(
            self.network_offering, gateway='10.1.1.1', vpc=vpc_1)
        vpc_1_tier_2 = self.create_Network(
            self.network_offering, gateway='10.1.2.1', vpc=vpc_1)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_1, vpc_1,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_1_tier_2, vpc_1,
            domain_template_name=self.domain_template_list[0])

        # Creating VPC and VPC networks (tiers)
        vpc_2 = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')
        vpc_2_tier_1 = self.create_Network(
            self.network_offering, gateway='10.1.1.1', vpc=vpc_2)
        vpc_2_tier_2 = self.create_Network(
            self.network_offering, gateway='10.1.2.1', vpc=vpc_2)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_2_tier_1, vpc_2,
            domain_template_name=self.domain_template_list[0])
        self.verify_vsd_network(
            self.account.domainid, vpc_2_tier_2, vpc_2,
            domain_template_name=self.domain_template_list[0])

        # Creating VPC
        vpc_3 = self.create_Vpc(self.vpc_offering, cidr='10.1.0.0/16')

        # Associating pre-configured Nuage VSP Domain Template to VPC
        self.validate_NuageVspDomainTemplate(self.domain_template_list[1])
        self.associate_NuageVspDomainTemplate(
            self.domain_template_list[1], vpc_3)

        # Creating VPC networks (tiers)
        vpc_3_tier_1 = self.create_Network(
            self.network_offering, gateway='10.1.1.1', vpc=vpc_3)
        vpc_3_tier_2 = self.create_Network(
            self.network_offering, gateway='10.1.2.1', vpc=vpc_3)

        # VSD verification
        self.verify_vsd_network(
            self.account.domainid, vpc_3_tier_1, vpc_3,
            domain_template_name=self.domain_template_list[1])
        self.verify_vsd_network(
            self.account.domainid, vpc_3_tier_2, vpc_3,
            domain_template_name=self.domain_template_list[1])

        # Updating global setting "nuagevsp.vpc.domaintemplate.name"
        self.update_NuageVspGlobalDomainTemplate(value="")
        domain_template = self.list_NuageVspGlobalDomainTemplate()[0].name
        self.assertEqual(domain_template, "",
                         "Global setting nuagevsp.vpc.domaintemplate.name was "
                         "not updated successfully"
                         )

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_08_nuage_Global_Domain_Template_as_ROOT_user(self):
        """Test Nuage VSP Global Domain Template as ROOT domain regular user
        """

        # Repeat the tests in the testcase
        # "test_07_nuage_Global_Domain_Template" as ROOT domain regular user

        # Setting ROOT domain user account information
        self.account = self.account_roota

        # Setting ROOT domain user keys in api_client
        self.api_client.connection.apiKey = self.user_roota_apikey
        self.api_client.connection.securityKey = self.user_roota_secretkey

        # Calling testcase "test_07_nuage_Global_Domain_Template"
        self.test_07_nuage_Global_Domain_Template()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_09_nuage_Global_Domain_Template_as_domain_admin(self):
        """Test Nuage VSP Global Domain Template as domain admin user
        """

        # Repeat the tests in the testcase
        # "test_07_nuage_Global_Domain_Template" as domain admin user

        # Setting domain D1 admin account information
        self.account = self.account_d1

        # Setting domain D1 admin keys in api_client
        self.api_client.connection.apiKey = self.user_d1_apikey
        self.api_client.connection.securityKey = self.user_d1_secretkey

        # Calling testcase "test_07_nuage_Global_Domain_Template"
        self.test_07_nuage_Global_Domain_Template()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_10_nuage_Global_Domain_Template_as_domain_user(self):
        """Test Nuage VSP Global Domain Template as domain regular user
        """

        # Repeat the tests in the testcase
        # "test_07_nuage_Global_Domain_Template" as domain regular user

        # Setting domain D1 user account information
        self.account = self.account_d1a

        # Setting domain D1 user keys in api_client
        self.api_client.connection.apiKey = self.user_d1a_apikey
        self.api_client.connection.securityKey = self.user_d1a_secretkey

        # Calling testcase "test_07_nuage_Global_Domain_Template"
        self.test_07_nuage_Global_Domain_Template()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_11_nuage_Global_Domain_Template_as_subdomain_admin(self):
        """Test Nuage VSP Global Domain Template as subdomain admin user
        """

        # Repeat the tests in the testcase
        # "test_07_nuage_Global_Domain_Template" as subdomain admin user

        # Setting subdomain D11 admin account information
        self.account = self.account_d11

        # Setting subdomain D1 admin keys in api_client
        self.api_client.connection.apiKey = self.user_d11_apikey
        self.api_client.connection.securityKey = self.user_d11_secretkey

        # Calling testcase "test_07_nuage_Global_Domain_Template"
        self.test_07_nuage_Global_Domain_Template()

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_12_nuage_Global_Domain_Template_as_subdomain_user(self):
        """Test Nuage VSP Global Domain Template as subdomain regular user
        """

        # Repeat the tests in the testcase
        # "test_07_nuage_Global_Domain_Template" as subdomain regular user

        # Setting subdomain D11 user account information
        self.account = self.account_d11a

        # Setting subdomain D11 user keys in api_client
        self.api_client.connection.apiKey = self.user_d11a_apikey
        self.api_client.connection.securityKey = self.user_d11a_secretkey

        # Calling testcase "test_07_nuage_Global_Domain_Template"
        self.test_07_nuage_Global_Domain_Template()
