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
""" Tests for Portable public IP Ranges feature

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Portable+IP+Test+Execution

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/portable+public+IP
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (VirtualMachine,
                             PublicIPAddress,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             NATRule,
                             Account,
                             PortablePublicIpRange,
                             StaticNATRule,
                             FireWallRule)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               get_region,
                               get_pod,
                               isIpInDesiredState)
from netaddr import IPAddress
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

class TestCreatePortablePublicIpRanges(cloudstackTestCase):
    """Test Create Portable IP Ranges
    """


    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestCreatePortablePublicIpRanges, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_create_portable_ip_range(self):
        """Test create new portable ip range
        """
        # 1. Create new portable ip range with root admin api
        # 2. Portable ip range should be created successfully

        self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id

        try:
            #create new portable ip range
            new_portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.testdata["configurableData"]["portableIpRange"])

            self.cleanup.append(new_portable_ip_range)
        except Exception as e:
            self.fail("Failed to create portable IP range: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_create_portable_ip_range_non_root_admin(self):
        """Test create new portable ip range with non admin root account
        """
        # 1. Create new portable ip range with non root admin api client
        # 2. Portable ip range should not be created

        try:
            self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
            self.cleanup.append(self.account)

            self.api_client_user = self.testClient.getUserApiClient(
                                            UserName=self.account.name,
                                            DomainName=self.account.domain
                                            )

            self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id

            self.debug("Trying to create portable ip range with non root-admin api client, should raise exception")
            with self.assertRaises(Exception):
                portable_ip_range = PortablePublicIpRange.create(self.api_client_user,
                                         self.testdata["configurableData"]["portableIpRange"])
                self.cleanup.append(portable_ip_range)
        except Exception as e:
            self.fail(e)

        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_create_portable_ip_range_invalid_region(self):
        """Test create portable ip range with invalid region id"""

        # 1. Try to create new portable ip range with invalid region id
        # 2. Portable ip range creation should fail
        self.testdata["configurableData"]["portableIpRange"]["regionid"] = -1

        #create new portable ip range
        self.debug("Trying to create portable ip range with wrong region id")

        with self.assertRaises(Exception):
            portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                         self.testdata["configurableData"]["portableIpRange"])
            self.cleanup.append(portable_ip_range)

        return

class TestDeletePortablePublicIpRanges(cloudstackTestCase):
    """Test delete Portable IP Ranges
    """


    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeletePortablePublicIpRanges, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.testdata["configurableData"]["portableIpRange"])

        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_delete_portable_ip_range(self):
        """Test delete ip range
        """
        # 1. Try to delete the created range with root admin api client
        # 2. Portable range should be deleted successfully

        self.portable_ip_range.delete(self.apiclient)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_delete_portable_ip_range_non_root_admin(self):
        """Test delete ip range - non admin root
        """
        # 1. Try to delete the created range with non root admin api client
        # 2. Portable range deletion should fail

        try:
            self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )

            self.cleanup.append(self.account)

            self.api_client_user = self.testClient.getUserApiClient(
                                            UserName=self.account.name,
                                            DomainName=self.account.domain
                                            )
        except Exception as e:
            self.fail(e)

        try:
            with self.assertRaises(Exception):
                self.portable_ip_range.delete(self.api_client_user)
        except Exception as e:
            self.fail(e)
        finally:
            self.portable_ip_range.delete(self.apiclient)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_delete_portable_ip_range_in_use(self):
        """Test delete ip range
        """
        # 1. Associate a portable ip
        # 2. Try to delete the portable ip range with root admin api client
        # 3. Portable ip range should not be deleted unless currently used ip is disassociated

        try:
            self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )

            self.cleanup.append(self.account)
            self.network_offering = NetworkOffering.create(
                                            self.apiclient,
                                            self.testdata["isolated_network_offering"],
                                            conservemode=False
                                            )
            # Enable Network offering
            self.network_offering.update(self.apiclient, state='Enabled')

            self.network = Network.create(
                                    self.apiclient,
                                    self.testdata["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
            portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )

        except Exception as e:
            self.fail(e)

        try:
            with self.assertRaises(Exception):
                self.debug("Trying to Delete portable ip range with root-admin api, this should fail")
                self.portable_ip_range.delete(self.apiclient)
        except Exception as e:
            self.fail(e)
        finally:
            self.debug("Disassociating portable ip")
            portableip.delete(self.apiclient)
            self.debug("Deleting portable ip range")
            self.portable_ip_range.delete(self.apiclient)
        return

class TestListPortablePublicIpRanges(cloudstackTestCase):
    """Test List Portable IP Ranges
    """


    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestListPortablePublicIpRanges, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(self.testdata["configurableData"]["portableIpRange"]["startip"]),
                     str(self.testdata["configurableData"]["portableIpRange"]["endip"])))

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                              self.testdata["configurableData"]["portableIpRange"])

        self.debug("Created new portable IP range with startip:%s and endip:%s and id:%s" %
                    (self.portable_ip_range.startip,
                     self.portable_ip_range.endip,
                     self.portable_ip_range.id))

        self.cleanup = [self.portable_ip_range, ]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_list_portable_ip_range(self):
        """Test list portable ip ranges
        """
        # 1. Create new portable ip range
        # 2. Try to list ip ranges with root admin api client
        # 3. Portable ip ranges should list properly

        list_portable_ip_range = PortablePublicIpRange.list(self.apiclient,
                                                            id=self.portable_ip_range.id)

        self.assertEqual(
                 isinstance(list_portable_ip_range, list),
                 True,
                 "List portable IP ranges should not return an empty response"
                 )

        portable_ip_range = list_portable_ip_range[0]

        self.assertEqual(str(portable_ip_range.startip), str(self.testdata["configurableData"]["portableIpRange"]["startip"]),
                         "Listed startip not matching with the startip of created public ip range")

        self.assertEqual(str(portable_ip_range.endip), str(self.testdata["configurableData"]["portableIpRange"]["endip"]),
                         "Listed endip not matching with the endip of created public ip range")

        self.assertEqual(str(portable_ip_range.gateway), str(self.testdata["configurableData"]["portableIpRange"]["gateway"]),
                         "Listed gateway not matching with the gateway of created public ip range")

        self.assertEqual(str(portable_ip_range.netmask), str(self.testdata["configurableData"]["portableIpRange"]["netmask"]),
                         "Listed netmask not matching with the netmask of created public ip range")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_list_portable_ip_range_non_root_admin(self):
        """Test list portable ip ranges with non admin root account
        """
        # 1. Create new portable ip range
        # 2. Try to list ip ranges with root non admin api client
        # 3. Portable ip ranges listing should fail

        self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )

        self.cleanup.append(self.account)

        self.api_client_user = self.testClient.getUserApiClient(
                                            UserName=self.account.name,
                                            DomainName=self.account.domain
                                            )

        self.debug("Trying to list portable ip ranges with non root-admin api, should raise exception")
        with self.assertRaises(Exception):
            PortablePublicIpRange.list(self.api_client_user,
                                       id=self.portable_ip_range.id)
        return

class TestAssociatePublicIp(cloudstackTestCase):
    """Test associate Portable IP/ non portable public ip
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAssociatePublicIp, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        # Set Zones and disk offerings
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = template.id

        cls.account = Account.create(
                            cls.api_client,
                            cls.testdata["account"],
                            domainid=cls.domain.id,
                            admin=True
                            )
        cls._cleanup = [cls.account, ]

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.testdata["isolated_network_offering"],
                                            conservemode=False
                                            )

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.network = Network.create(
                                    cls.api_client,
                                    cls.testdata["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network offering
            cls.network_offering.update(cls.api_client, state='Disabled')
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.cleanup = []
        self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id
        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.testdata["configurableData"]["portableIpRange"])
        self.cleanup.append(self.portable_ip_range)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            self.network_offering.update(self.apiclient, state='Disabled')
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_associate_ip_address(self):
        """ Test assocoate public ip address
        """

        # 1. Create new portable ip range
        # 2. Create a network and associate public ip without mentioning (isportable)
        # 3. Create a network and associate public ip with isportable=False
        # 4. Create a network and associate public ip with isPortable=True
        # 5. All three public ip associations should succeed

        self.debug("Associating default public ip address with network: %s" % self.network.id)
        publicipaddress = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id
                                    )

        self.debug("Associated default public ip address: %s" % publicipaddress.ipaddress.ipaddress)

        self.debug("Associating public ip address with network: %s with isportable=False" % self.network.id)
        publicipaddressnotportable = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=False
                                    )

        self.debug("Associated public ip address (not portable): %s" % publicipaddressnotportable.ipaddress.ipaddress)
        publicipaddressnotportable.delete(self.apiclient)

        self.debug("Associating public ip address with network: %s with isportable=True" % self.network.id)
        publicipaddressportable = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.debug("Associated public ip address (portable): %s" % publicipaddressportable.ipaddress.ipaddress)
        publicipaddressportable.delete(self.apiclient)

        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_associate_ip_address_invalid_zone(self):
        """ Test Associate IP with invalid zone id
        """
        # 1. Create new portable ip range
        # 2. try to associate a portable ip with invalid region id
        # 3. IP association should fail

        self.debug("Trying to associate portable public ip with invalid zone id, this should fail")

        with self.assertRaises(Exception):
            publicipaddress = PublicIPAddress.create(
                                   self.apiclient,
                                   accountid=self.account.name,
                                   zoneid = -1,
                                   domainid=self.account.domainid,
                                   regionid = self.region.id,
                                   isportable=True
                                    )
            publicipaddress.delete(self.apiclient)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_associate_ip_address_services_enable_disable(self):
        """ Test enabling and disabling NAT, Firewall services on portable ip
        """
        # 1. Create new portable ip range
        # 2. Associate a portable ip
        # 3. Enable NAT and Firewall rules on this portable ip
        # 4. Disable NAT and Firewall rules created
        # 5. Enabling and disabling ofthe rules should be successful

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )

        self.cleanup.append(self.service_offering)

        try:

            self.debug("DeployingVirtual Machine")
            self.virtual_machine = VirtualMachine.create(
                                            self.apiclient,
                                            self.testdata["small"],
                                            accountid=self.account.name,
                                            domainid=self.account.domainid,
                                            serviceofferingid=self.service_offering.id,
                                            networkids = [self.network.id],
                                            mode=self.testdata['mode']
                                            )
            self.debug("Created virtual machine instance: %s with ssh_ip: %s" %
                        (self.virtual_machine.id, self.virtual_machine.ssh_ip))

        except Exception as e:
            self.fail("Exception while deploying vm : %s" % e)

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        response = isIpInDesiredState(self.apiclient, portableip.ipaddress.id, state="allocated")
        exceptionOccured = response[0]
        ipInDesiredState = response[1]
        exceptionMessage = response[2]
        if (exceptionOccured or (not ipInDesiredState)):
            portableip.delete(self.apiclient)
            self.fail(exceptionMessage)

        try:
            # Open up firewall port for SSH
            self.debug("Opening firewall on the portable public ip")
            fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.testdata["natrule"]["protocol"],
                            cidrlist=["0.0.0.0/0"],
                            startport=self.testdata["natrule"]["publicport"],
                            endport=self.testdata["natrule"]["publicport"]
                            )

            #Create NAT rule
            self.debug("Creating NAT rule on the portable public ip")
            nat_rule = NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.testdata["natrule"],
                        portableip.ipaddress.id
                        )
        except Exception as e:
            portableip.delete(self.apiclient)
            self.fail("Error: %s" % e)

        try:

            self.debug("Trying to SSH to ip: %s" % portableip.ipaddress.ipaddress)
            SshClient(portableip.ipaddress.ipaddress,
                      self.testdata['natrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception while SSHing : %s" % e)

        finally:
            self.debug("Deleting firewall rule")
            fw_rule.delete(self.apiclient)

            self.debug("Deleting NAT rule")
            nat_rule.delete(self.apiclient)

            self.debug("disassocoating portable ip: %s" % portableip.ipaddress.ipaddress)
            portableip.delete(self.apiclient)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_associate_ip_address_no_free_ip(self):
        """ Test assocoate public ip address
        """

        # 1. Create new portable ip range
        # 2. Create a network and associate all available portbale public ips
        # 5. Try to associate portable ip, it should fail

        associatedipaddresses = []

        startip_int = int(IPAddress(self.portable_ip_range.startip))
        endip_int = int(IPAddress(self.portable_ip_range.endip))
        totalportableips = ((endip_int - startip_int) + 1)

        self.debug(totalportableips)

        for x in range(0, totalportableips):

            self.debug("Associating public ip address with network: %s with isportable=True" % self.network.id)
            portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
            associatedipaddresses.append(portableip)
            self.debug("Associated public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        self.debug("Trying to associate portable public ip when no free ips available, this should fail")
        with self.assertRaises(Exception):
            portableipaddress = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                   )
            portableipaddress.delete(self.apiclient)

        self.debug("Associating portable ip address failed")

        self.debug("Disassociating previously associated ip addresses")

        for x in range(0, totalportableips):
            associatedipaddresses[x].delete(self.apiclient)

        return

class TestDisassociatePublicIp(cloudstackTestCase):
    """Test Disassociate Portable IP/ non portable IP
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDisassociatePublicIp, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        # Set Zones and disk offerings
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = template.id
        cls._cleanup = []

        cls.account = Account.create(
                            cls.api_client,
                            cls.testdata["account"],
                            domainid=cls.domain.id,
                            admin=True
                            )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.testdata["isolated_network_offering"],
                                            conservemode=False
                                            )

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls._cleanup.append(cls.network_offering)

        cls.network = Network.create(
                                    cls.api_client,
                                    cls.testdata["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

        cls.virtual_machine = VirtualMachine.create(
                                                    cls.api_client,
                                                    cls.testdata["small"],
                                                    accountid=cls.account.name,
                                                    domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id,
                                                    networkids = [cls.network.id],
                                                    mode=cls.testdata['mode']
                                                    )
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Disable Network offering
            cls.network_offering.update(cls.api_client, state='Disabled')
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id

        #create new portable ip range
        new_portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.testdata["configurableData"]["portableIpRange"])
        self.cleanup.append(new_portable_ip_range)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            self.network_offering.update(self.apiclient, state='Disabled')
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_disassociate_ip_address_no_services(self):
        """ Test disassociating portable ip
        """
        # 1. Create new portable ip range
        # 2. Associate a portable ip
        # 3. Disassociate the portable ip with root admin api client
        # 4. Disassociating should be successful

        try:
            portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
            portableip.delete(self.apiclient)
        except Exception as e:
            raise Exception("Exception occured: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_disassociate_ip_address_services_enabled(self):
        """ Test disassociating portable ip
        """
        # 1. Create new portable ip range
        # 2. Associate a portable ip
        # 3. Enable NAT and Firewall services on this portable IP
        # 4. Disassociate the portable ip with root admin api client
        # 5. Disassociating should be successful

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )

        response = isIpInDesiredState(self.apiclient, portableip.ipaddress.id, state="allocated")
        exceptionOccured = response[0]
        ipInDesiredState = response[1]
        exceptionMessage = response[2]
        if (exceptionOccured or (not ipInDesiredState)):
            portableip.delete(self.apiclient)
            self.fail(exceptionMessage)

        try:
            # Open up firewall port for SSH
            self.debug("Opening firewall on the portable public ip")
            FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.testdata["natrule"]["protocol"],
                            cidrlist=["0.0.0.0/0"],
                            startport=self.testdata["natrule"]["publicport"],
                            endport=self.testdata["natrule"]["publicport"]
                            )

            #Create NAT rule
            self.debug("Creating NAT rule on the portable public ip")
            NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.testdata["natrule"],
                        portableip.ipaddress.id
                        )
        except Exception as e:
            portableip.delete(self.apiclient)
            self.fail("Error: %s" % e)

        try:
            portableip.delete(self.apiclient)
        except Exception as e:
            raise Exception("Exception while disassociating portable ip: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_disassociate_ip_address_other_account(self):
        """ Test disassociating portable IP with non-owner account
        """

        # 1. Create new portable ip range
        # 2. Associate a portable ip
        # 3. Try to Disassociate the portable ip with an account which is not owner of portable ip
        # 4. Disassociating should fail

        try:
            portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        except Exception as e:
            self.fail("Failed to create portable ip: %s" % e)

        try:
            self.otherAccount = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id
                            )
            self.cleanup.append(self.otherAccount)

            self.apiclientOtherAccount = self.testClient.getUserApiClient(
                                            UserName=self.otherAccount.name,
                                            DomainName=self.otherAccount.domain
                                            )

            # Trying to disassociate portable ip using
            # api client of other account than the one
            # used to create portable ip
            with self.assertRaises(Exception):
                portableip.delete(self.apiclientOtherAccount)

            # Disassociate IP using api client of account used to create it
            portableip.delete(self.apiclient)
        except Exception as e:
            self.fail("Exception while disassociating portable ip: %s" % e)
        return

class TestDeleteAccount(cloudstackTestCase):
    """ Test Delete Account having portable ip
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeleteAccount, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        # Set Zones and disk offerings
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = template.id

        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.cleanup = []
        try:
            self.account = Account.create(
                            self.apiclient,
                            self.testdata["account"],
                            domainid=self.domain.id,
                            admin=True
                            )
            self.cleanup.append(self.account)
            self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id
            #create new portable ip range
            new_portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.testdata["configurableData"]["portableIpRange"])
            self.cleanup.append(new_portable_ip_range)
            self.network_offering = NetworkOffering.create(
                                            self.apiclient,
                                            self.testdata["isolated_network_offering"],
                                            conservemode=False
                                            )
            # Enable Network offering
            self.network_offering.update(self.apiclient, state='Enabled')

            self.network = Network.create(
                                    self.apiclient,
                                    self.testdata["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
            self.cleanup.append(self.network_offering)
        except Exception as e:
            self.fail("Exception in setupClass: %s" % e)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_delete_account_services_disabled(self):
        """ test delete account with portable ip with no services enabled
        """
        # 1. Associate a portable ip to an account
        # 2. Delete account
        # 3. Account should get deleted successfully

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.account.delete(self.apiclient)
        list_publicips = PublicIPAddress.list(self.apiclient,
                                       id=portableip.ipaddress.id)
        self.assertEqual(list_publicips, None, "List of ip addresses should be empty")
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_delete_account_services_enabled(self):
        """ test delete account with portable ip with PF and firewall services enabled
        """
        # 1. Associate a portable ip to an account
        # 2. Enabled PF and Firewall rules on this IP
        # 3. Delete account
        # 4. Account should get deleted successfully

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )

        self.cleanup.append(self.service_offering)

        self.debug("Deploying Virtual Machine")
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.testdata['mode']
        )
        self.debug("Created virtual machine instance: %s" % self.virtual_machine.id)

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        response = isIpInDesiredState(self.apiclient, portableip.ipaddress.id, state="allocated")
        exceptionOccured = response[0]
        ipInDesiredState = response[1]
        exceptionMessage = response[2]
        if (exceptionOccured or (not ipInDesiredState)):
            portableip.delete(self.apiclient)
            self.account.delete(self.apiclient)
            self.fail(exceptionMessage)

        try:
            # Open up firewall port for SSH
            self.debug("Opening firewall on the portable public ip")
            FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.testdata["natrule"]["protocol"],
                            cidrlist=["0.0.0.0/0"],
                            startport=self.testdata["natrule"]["publicport"],
                            endport=self.testdata["natrule"]["publicport"]
                            )

            #Create NAT rule
            self.debug("Creating NAT rule on the portable public ip")
            NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.testdata["natrule"],
                        portableip.ipaddress.id
                        )
        except Exception as e:
            portableip.delete(self.apiclient)
            self.account.delete(self.apiclient)
            self.fail("Error %s" % e)

        self.debug("Deleting account: %s :" % self.account.name)

        self.account.delete(self.apiclient)

        self.debug("Trying to list the ip address associated with deleted account, \
                should throw exception")

        list_publicips = PublicIPAddress.list(self.apiclient,
                                              id=portableip.ipaddress.id)
        self.assertEqual(list_publicips, None, "List of ip addresses should be empty")
        return

class TestPortableIpTransferAcrossNetworks(cloudstackTestCase):
    """Test Transfer Portable IP Across Networks
    """


    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestPortableIpTransferAcrossNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill testdata from the external config file
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls.testdata["regionid"] = cls.region.id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        # Set Zones and disk offerings
        cls.testdata["small"]["zoneid"] = cls.zone.id
        cls.testdata["small"]["template"] = template.id

        cls._cleanup = []

        # Set Zones and Network offerings
        cls.account = Account.create(
                            cls.api_client,
                            cls.testdata["account"],
                            domainid=cls.domain.id,
                            admin=True
                            )
        cls._cleanup.append(cls.account)

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.testdata["isolated_network_offering"],
                                            conservemode=False
                                            )
        cls._cleanup.append(cls.network_offering)

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )

        cls.network1 = Network.create(
                                    cls.api_client,
                                    cls.testdata["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

        cls.virtual_machine1 = VirtualMachine.create(
                                            cls.api_client,
                                            cls.testdata["small"],
                                            accountid=cls.account.name,
                                            domainid=cls.account.domainid,
                                            serviceofferingid=cls.service_offering.id,
                                            networkids = [cls.network1.id],
                                          )
        cls.network2 = Network.create(
                                    cls.api_client,
                                    cls.testdata["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.virtual_machine2 = VirtualMachine.create(
                                            cls.api_client,
                                            cls.testdata["small"],
                                            accountid=cls.account.name,
                                            domainid=cls.account.domainid,
                                            serviceofferingid=cls.service_offering.id,
                                            networkids = [cls.network2.id],
                                            )
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

        self.testdata["configurableData"]["portableIpRange"]["regionid"] = self.region.id

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                              self.testdata["configurableData"]["portableIpRange"])

        self.cleanup = [self.portable_ip_range, ]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_list_portable_ip_range_non_root_admin(self):
        """Test list portable ip ranges with non admin root account
        """
        # 1. Create new network 1 and associate portable IP 1
        # 2. Have at least 1 VM in network1
        # 3. Create a new network 2 and at least 1 VM in network 2
        # 2. enable static NAT on portable IP 1 with a VM in network 2
        # 3. SSH to the VM in network 2

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network1.id,
                                    isportable=True
                                    )

        response = isIpInDesiredState(self.apiclient, portableip.ipaddress.id, state="allocated")
        exceptionOccured = response[0]
        ipInDesiredState = response[1]
        exceptionMessage = response[2]
        if (exceptionOccured or (not ipInDesiredState)):
            portableip.delete(self.apiclient)
            self.fail(exceptionMessage)

        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)
        #Create NAT rule
        self.debug("Creating NAT rule on the portable public ip")

        try:
            # Enable Static NAT for VM
            StaticNATRule.enable(
                             self.apiclient,
                             portableip.ipaddress.id,
                             self.virtual_machine2.id,
                             networkid=self.network2.id
                            )

            # Open up firewall port for SSH
            self.debug("Opening firewall on the portable public ip")
            FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.testdata["natrule"]["protocol"],
                            cidrlist=["0.0.0.0/0"],
                            startport=self.testdata["natrule"]["publicport"],
                            endport=self.testdata["natrule"]["publicport"]
                            )
        except Exception as e:
            portableip.delete(self.apiclient)
            self.fail("Error: %s" % e)

        static_nat_list = PublicIPAddress.list(
                                    self.apiclient,
                                    associatednetworkid=self.network2.id,
                                    listall=True,
                                    isstaticnat=True,
                                    ipaddress=portableip.ipaddress.ipaddress,
                                    )
        self.assertEqual(
                         isinstance(static_nat_list, list),
                         True,
                         "List Public IP should return a valid static NAT info that was created on portable ip"
                         )
        self.assertTrue(
                        static_nat_list[0].ipaddress == portableip.ipaddress.ipaddress and static_nat_list[0].virtualmachineid==self.virtual_machine2.id,
                        "There is some issue in transferring portable ip {} across networks".format(portableip.ipaddress.ipaddress)
                        )
        try:

            self.debug("Trying to SSH to ip: %s" % portableip.ipaddress.ipaddress)
            SshClient(portableip.ipaddress.ipaddress,
                      self.testdata['natrule']["publicport"],
                      self.virtual_machine2.username,
                      self.virtual_machine2.password
                      )
        except Exception as e:
            self.fail("Exception while SSHing : %s" % e)

        finally:
            self.debug("disassociating portable ip: %s" % portableip.ipaddress.ipaddress)
            portableip.delete(self.apiclient)
