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
"""
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.cloudstackException import cloudstackAPIException
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from netaddr import *
from marvin.sshClient import SshClient

from nose.plugins.attrib import attr

class Services:
    """Test Multiple IP Ranges
    """
    def __init__(self):
        self.services = {
                        "account": {
                                    "email": "test@test.com",
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
                                    "cpuspeed": 200,    # in MHz
                                    "memory": 256,      # In MBs
                        },
                        "network_offering": {
                                    "name": 'Network offering portable ip',
                                    "displaytext": 'Network offering-VR services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                        },
                        "network": {
                                  "name": "Test Network - Portable IP",
                                  "displaytext": "Test Network - Portable IP",
                        },
                        "network1": {
                                  "name": "Test Network 1 - Portable IP",
                                  "displaytext": "Test Network 1 - Portable IP",
                        },
                        "network2": {
                                  "name": "Test Network 2 - Portable IP",
                                  "displaytext": "Test Network 2 - Portable IP",
                        },
                        "disk_offering": {
                                    "displaytext": "Small Disk",
                                    "name": "Small Disk",
                                    "disksize": 1
                        },
                        "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": "TCP",
                                    "cidr" : '0.0.0.0/0',
                        },
                        "small":
                                # Create a small virtual machine instance with disk offering
                                {
                                 "displayname": "testserver",
                                  "username": "root", # VM creds for SSH
                                  "password": "password",
                                  "ssh_port": 22,
                                  "hypervisor": 'XenServer',
                                  "privateport": 22,
                                  "publicport": 22,
                                  "protocol": 'TCP',
                        },
                        "vm1":
                                # Create a small virtual machine instance with disk offering
                                {
                                 "displayname": "vm1",
                                  "username": "root", # VM creds for SSH
                                  "password": "password",
                                  "ssh_port": 22,
                                  "hypervisor": 'XenServer',
                                  "privateport": 22,
                                  "publicport": 22,
                                  "protocol": 'TCP',
                        },
                        "vm2":
                                # Create a small virtual machine instance with disk offering
                                {
                                 "displayname": "vm2",
                                  "username": "root", # VM creds for SSH
                                  "password": "password",
                                  "ssh_port": 22,
                                  "hypervisor": 'XenServer',
                                  "privateport": 22,
                                  "publicport": 22,
                                  "protocol": 'TCP',
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
          }

class TestCreatePortablePublicIpRanges(cloudstackTestCase):
    """Test Create Portable IP Ranges
    """


    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestCreatePortablePublicIpRanges, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id
 
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

    @attr(tags=["advanced"])
    def test_create_portable_ip_range(self):
        """Test create new portable ip range
        """
        # 1. Create new portable ip range with root admin api
        # 2. Portable ip range should be created successfully

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        self.debug(portable_ip_range_services)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(portable_ip_range_services["startip"]),
                     str(portable_ip_range_services["endip"])))

        #create new portable ip range
        new_portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             portable_ip_range_services)

        self.debug("Created new portable IP range with startip:%s and endip:%s and id:%s" %
                    (new_portable_ip_range.startip,
                     new_portable_ip_range.endip,
                     new_portable_ip_range.id))

        self.cleanup.append(new_portable_ip_range)

        return

    @attr(tags=["advanced"])
    def test_create_portable_ip_range_non_root_admin(self):
        """Test create new portable ip range with non admin root account
        """
        # 1. Create new portable ip range with non root admin api client
        # 2. Portable ip range should not be created

        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )

        self.api_client_user = self.testClient.createUserApiClient(
                                            UserName=self.account.name,
                                            DomainName=self.account.domain
                                            )

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = self.region.id

        self.debug("Trying to create portable ip range with non root-admin api client, should raise exception")
        with self.assertRaises(Exception):
            portable_ip_range = PortablePublicIpRange.create(self.api_client_user,
                                         portable_ip_range_services)
            self.cleanup.append(portable_ip_range)

        return

    @attr(tags=["advanced"])
    def test_create_portable_ip_range_invalid_region(self):
        """Test create portable ip range with invalid region id"""

        # 1. Try to create new portable ip range with invalid region id
        # 2. Portable ip range creation should fail

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = -1

        #create new portable ip range 
        self.debug("Trying to create portable ip range with wrong region id")

        with self.assertRaises(Exception):
            portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                         portable_ip_range_services)
            self.cleanup.append(portable_ip_range)

        return

class TestDeletePortablePublicIpRanges(cloudstackTestCase):
    """Test delete Portable IP Ranges
    """


    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestDeletePortablePublicIpRanges, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id

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

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(portable_ip_range_services["startip"]),
                     str(portable_ip_range_services["endip"])))

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             portable_ip_range_services)

        self.debug("Created new portable IP range with startip:%s and endip:%s and id:%s" %
                    (self.portable_ip_range.startip,
                     self.portable_ip_range.endip,
                     self.portable_ip_range.id))

        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"])
    def test_delete_portable_ip_range(self):
        """Test delete ip range
        """
        # 1. Try to delete the created range with root admin api client
        # 2. Portable range should be deleted successfully

        self.debug("Deleting portable ip range with root-admin api")

        self.portable_ip_range.delete(self.apiclient)

        self.debug("Deleted portable ip range")

        return

    @attr(tags=["advanced"])
    def test_delete_portable_ip_range_non_root_admin(self):
        """Test delete ip range - non admin root
        """
        # 1. Try to delete the created range with non root admin api client
        # 2. Portable range deletion should fail

        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )

        self.cleanup.append(self.account)

        self.api_client_user = self.testClient.createUserApiClient(
                                            UserName=self.account.name,
                                            DomainName=self.account.domain
                                            )

        with self.assertRaises(Exception):
            self.portable_ip_range.delete(self.api_client_user)

        self.portable_ip_range.delete(self.apiclient)
        return

    @attr(tags=["advanced"])
    def test_delete_portable_ip_range_in_use(self):
        """Test delete ip range
        """
        # 1. Associate a portable ip
        # 2. Try to delete the portable ip range with root admin api client
        # 3. Portable ip range should not be deleted unless currently used ip is disassociated

        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )

        self.cleanup.append(self.account)

        self.debug(
            "Creating n/w offering"
            )
        self.network_offering = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )

        self.debug("Created n/w offering with ID: %s" %
                                                    self.network_offering.id)
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.debug("Creating network")

        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )

        self.debug("Created network with id: %s" % self.network.id)
        self.debug("Associating public ip address with network: %s with isportable=True" % self.network.id)
        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.debug("Associated public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        with self.assertRaises(Exception):
            self.debug("Trying to Delete portable ip range with root-admin api, this should fail")
            self.portable_ip_range.delete(self.apiclient)

        self.debug("Deleting portable ip range failed")
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
        cls.api_client = super(TestListPortablePublicIpRanges, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id

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

        #create new portable ip range
        self.portable_ip_range_services = get_portable_ip_range_services(self.config)

        if self.portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        self.portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(self.portable_ip_range_services["startip"]),
                     str(self.portable_ip_range_services["endip"])))

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.portable_ip_range_services)

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

    @attr(tags=["advanced"])
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

        self.assertEqual(str(portable_ip_range.startip), str(self.portable_ip_range_services["startip"]),
                         "Listed startip not matching with the startip of created public ip range")

        self.assertEqual(str(portable_ip_range.endip), str(self.portable_ip_range_services["endip"]),
                         "Listed endip not matching with the endip of created public ip range")

        self.assertEqual(str(portable_ip_range.gateway), str(self.portable_ip_range_services["gateway"]),
                         "Listed gateway not matching with the gateway of created public ip range")

        self.assertEqual(str(portable_ip_range.netmask), str(self.portable_ip_range_services["netmask"]),
                         "Listed netmask not matching with the netmask of created public ip range")
        return

    @attr(tags=["advanced"])
    def test_list_portable_ip_range_non_root_admin(self):
        """Test list portable ip ranges with non admin root account
        """
        # 1. Create new portable ip range
        # 2. Try to list ip ranges with root non admin api client
        # 3. Portable ip ranges listing should fail

        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )

        self.cleanup.append(self.account)

        self.api_client_user = self.testClient.createUserApiClient(
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
        cls.api_client = super(TestAssociatePublicIp, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id,
                            admin=True
                            )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

        cls._cleanup = [cls.account]
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

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(portable_ip_range_services["startip"]),
                     str(portable_ip_range_services["endip"])))

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             portable_ip_range_services)

        self.debug("Created new portable IP range with startip:%s and endip:%s and id:%s" %
                    (self.portable_ip_range.startip,
                     self.portable_ip_range.endip,
                     self.portable_ip_range.id))

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

    @attr(tags=["advanced"])
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
 
    @attr(tags=["advanced"])
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
        self.debug("Associating ip address failed")
        return

    @unittest.skip("SSH failing to portable ip, need to investigate the issue")
    @attr(tags=["advanced"])
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
            self.services["service_offering"]
        )

        self.cleanup.append(self.service_offering)

        try:

            self.debug("DeployingVirtual Machine")
            self.virtual_machine = VirtualMachine.create(
                                            self.apiclient,
                                            self.services["small"],
                                            accountid=self.account.name,
                                            domainid=self.account.domainid,
                                            serviceofferingid=self.service_offering.id,
                                            networkids = [self.network.id],
                                            mode=self.services['mode']
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

        # Open up firewall port for SSH
        self.debug("Opening firewall on the portable public ip")
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=[self.services["natrule"]["cidr"]],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )

        #Create NAT rule
        self.debug("Creating NAT rule on the portable public ip")
        nat_rule = NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.services["natrule"],
                        portableip.ipaddress.id
                        )

        try:

            self.debug("Trying to SSH to ip: %s" % portableip.ipaddress.ipaddress)
            SshClient(portableip.ipaddress.ipaddress,
                      self.services['natrule']["publicport"],
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

    @attr(tags=["advanced"])
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
        cls.api_client = super(TestDisassociatePublicIp, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id,
                            admin=True
                            )

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.network = Network.create(
                                    cls.api_client,
                                    cls.services["network"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

        cls.virtual_machine = VirtualMachine.create(
                                                    cls.api_client,
                                                    cls.services["small"],
                                                    accountid=cls.account.name,
                                                    domainid=cls.account.domainid,
                                                    serviceofferingid=cls.service_offering.id,
                                                    networkids = [cls.network.id],
                                                    mode=cls.services['mode']
                                                    )

        cls._cleanup = [
                        cls.account,
                        cls.service_offering,
                        cls.network_offering
                       ]
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

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(portable_ip_range_services["startip"]),
                     str(portable_ip_range_services["endip"])))

        #create new portable ip range
        new_portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             portable_ip_range_services)

        self.debug("Created new portable IP range with startip:%s and endip:%s and id:%s" %
                    (new_portable_ip_range.startip,
                     new_portable_ip_range.endip,
                     new_portable_ip_range.id))

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
 
    @attr(tags=["advanced"])
    def test_disassociate_ip_address_no_services(self):
        """ Test disassociating portable ip
        """
        # 1. Create new portable ip range
        # 2. Associate a portable ip
        # 3. Disassociate the portable ip with root admin api client
        # 4. Disassociating should be successful

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        try:
            self.debug("Disassociating portable ip: %s with id: %s" %
                        (portableip.ipaddress.ipaddress, portableip.ipaddress.id)
                       )

            portableip.delete(self.apiclient)

        except Exception as e:
            raise Exception("Exception while disassociating portable ip: %s" % e)

        return

    @attr(tags=["advanced"])
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
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        # Open up firewall port for SSH
        self.debug("Opening firewall on the portable public ip")
        FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=[self.services["natrule"]["cidr"]],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )

        #Create NAT rule
        self.debug("Creating NAT rule on the portable public ip")
        NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.services["natrule"],
                        portableip.ipaddress.id
                        )

        try:
            self.debug("Disassociating portable ip: %s with id: %s" %
                        (portableip.ipaddress.ipaddress, portableip.ipaddress.id)
                       )

            portableip.delete(self.apiclient)

        except Exception as e:
            raise Exception("Exception while disassociating portable ip: %s" % e)

        return

    @attr(tags=["advanced"])
    def test_disassociate_ip_address_other_account(self):
        """ Test disassociating portable IP with non-owner account
        """

        # 1. Create new portable ip range
        # 2. Associate a portable ip
        # 3. Try to Disassociate the portable ip with an account which is not owner of portable ip
        # 4. Disassociating should fail

        portableip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id,
                                    isportable=True
                                    )
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        self.user_account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )

        self.api_client_user = self.testClient.createUserApiClient(
                                            UserName=self.user_account.name,
                                            DomainName=self.user_account.domain
                                            )
        try:
            self.debug("Disassociating portable ip: %s with id: %s with other account :%s" %
                        (portableip.ipaddress.ipaddress, portableip.ipaddress.id, self.user_account.name)
                       )

            with self.assertRaises(Exception):
                portableip.delete(self.api_client_user)

        except Exception as e:
            raise Exception("Exception while disassociating portable ip: %s" % e)

        portableip.delete(self.apiclient)
        return

class TestDeleteAccount(cloudstackTestCase):
    """ Test Delete Account having portable ip
    """

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestDeleteAccount, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id
 
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

        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id,
                            admin=True
                            )
        self.cleanup = []

        portable_ip_range_services = get_portable_ip_range_services(self.config)

        if portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(portable_ip_range_services["startip"]),
                     str(portable_ip_range_services["endip"])))

        #create new portable ip range
        new_portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             portable_ip_range_services)

        self.debug("Created new portable IP range with startip:%s and endip:%s and id:%s" %
                    (new_portable_ip_range.startip,
                     new_portable_ip_range.endip,
                     new_portable_ip_range.id))

        self.cleanup.append(new_portable_ip_range)

        self.debug(
            "Creating n/w offering"
            )
        self.network_offering = NetworkOffering.create(
                                            self.apiclient,
                                            self.services["network_offering"],
                                            conservemode=False
                                            )

        self.debug("Created n/w offering with ID: %s" %
                                                    self.network_offering.id)
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')
        self.debug("Creating network")

        self.network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )

        self.cleanup.append(self.network_offering)

        self.debug("Created network with id: %s" % self.network.id)
        return

    def tearDown(self):
        try:
            # Disable Network offering
            self.network_offering.update(self.apiclient, state='Enabled')

            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"])
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
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)

        self.debug("Deleting account: %s :" % self.account.name)

        self.account.delete(self.apiclient)

        self.debug("Account deleted successfully")

        with self.assertRaises(Exception):
            PublicIPAddress.list(self.apiclient,
                                 id=portableip.ipaddress.id)

        return

    @attr(tags=["advanced"])
    def test_delete_account_services_enabled(self):
        """ test delete account with portable ip with PF and firewall services enabled
        """
        # 1. Associate a portable ip to an account
        # 2. Enabled PF and Firewall rules on this IP
        # 3. Delete account
        # 4. Account should get deleted successfully

        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"]
        )

        self.cleanup.append(self.service_offering)

        self.debug("Deploying Virtual Machine")
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services['mode']
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

        # Open up firewall port for SSH
        self.debug("Opening firewall on the portable public ip")
        FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=[self.services["natrule"]["cidr"]],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )

        #Create NAT rule
        self.debug("Creating NAT rule on the portable public ip")
        NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.services["natrule"],
                        portableip.ipaddress.id
                        )

        self.debug("Deleting account: %s :" % self.account.name)

        self.account.delete(self.apiclient)

        self.debug("Trying to list the ip address associated with deleted account, \
                should throw exception")

        with self.assertRaises(Exception):
            PublicIPAddress.list(self.apiclient,
                                 id=portableip.ipaddress.id)

        return

class TestPortableIpTransferAcrossNetworks(cloudstackTestCase):
    """Test Transfer Portable IP Across Networks
    """


    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestPortableIpTransferAcrossNetworks, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.region = get_region(cls.api_client, cls.services)
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["regionid"] = cls.region.id

        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Set Zones and disk offerings
        cls.services["vm1"]["zoneid"] = cls.zone.id
        cls.services["vm1"]["template"] = template.id
        cls.services["vm2"]["zoneid"] = cls.zone.id
        cls.services["vm2"]["template"] = template.id

        # Set Zones and Network offerings
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id,
                            admin=True
                            )

        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )

        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.network1 = Network.create(
                                    cls.api_client,
                                    cls.services["network1"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )

        cls.virtual_machine1 = VirtualMachine.create(
                                            cls.api_client,
                                            cls.services["vm1"],
                                            accountid=cls.account.name,
                                            domainid=cls.account.domainid,
                                            serviceofferingid=cls.service_offering.id,
                                            networkids = [cls.network1.id],
                                          )
        cls.network2 = Network.create(
                                    cls.api_client,
                                    cls.services["network2"],
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    networkofferingid=cls.network_offering.id,
                                    zoneid=cls.zone.id
                                    )
        cls.virtual_machine2 = VirtualMachine.create(
                                            cls.api_client,
                                            cls.services["vm2"],
                                            accountid=cls.account.name,
                                            domainid=cls.account.domainid,
                                            serviceofferingid=cls.service_offering.id,
                                            networkids = [cls.network2.id],
                                            )
        cls._cleanup = [cls.account, cls.network_offering]

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

        #create new portable ip range
        self.portable_ip_range_services = get_portable_ip_range_services(self.config)

        if self.portable_ip_range_services is None:
            self.skipTest('Failed to read config values related to portable ip range')

        self.portable_ip_range_services["regionid"] = self.region.id

        self.debug("Creating new portable IP range with startip:%s and endip:%s" %
                    (str(self.portable_ip_range_services["startip"]),
                     str(self.portable_ip_range_services["endip"])))

        #create new portable ip range
        self.portable_ip_range = PortablePublicIpRange.create(self.apiclient,
                                                             self.portable_ip_range_services)

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

    @attr(tags=["advanced","swamy"])
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
        self.debug("created public ip address (portable): %s" % portableip.ipaddress.ipaddress)
        #Create NAT rule
        self.debug("Creating NAT rule on the portable public ip")
        # Enable Static NAT for VM
        StaticNATRule.enable(
                             self.apiclient,
                             portableip.ipaddress.id,
                             self.virtual_machine2.id,
                             networkid=self.network2.id
                            )
        # Open up firewall port for SSH
        self.debug("Opening firewall on the portable public ip")
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=portableip.ipaddress.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=[self.services["natrule"]["cidr"]],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )
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
                      self.services['natrule']["publicport"],
                      self.virtual_machine2.username,
                      self.virtual_machine2.password
                      )
        except Exception as e:
            self.fail("Exception while SSHing : %s" % e)

        finally:
            self.debug("disassociating portable ip: %s" % portableip.ipaddress.ipaddress)
            portableip.delete(self.apiclient)
