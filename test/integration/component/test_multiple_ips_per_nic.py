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
""" Tests for Multiple IPs per NIC feature

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Multiple+IPs+per+NIC+Test+Plan

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-4840

    Design Document: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Multiple+IP+address+per+NIC
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              random_gen,
                              validateList)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             PublicIPAddress,
                             NATRule,
                             StaticNATRule,
                             FireWallRule,
                             NIC,
                             Network,
                             VPC,
                             ServiceOffering,
                             VpcOffering,
                             Domain,
                             Router)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone,
                               setSharedNetworkParams,
                               get_free_vlan,
                               createEnabledNetworkOffering)
from nose.plugins.attrib import attr
from marvin.codes import PASS, ISOLATED_NETWORK, VPC_NETWORK, SHARED_NETWORK, FAIL, FAILED
from ddt import ddt, data
import time


def createNetwork(self, networkType):
    """Create a network of given type (isolated/shared/isolated in VPC)"""

    network = None

    if networkType == ISOLATED_NETWORK:
        try:
            network = Network.create(
                self.apiclient,
                self.services["isolated_network"],
                networkofferingid=self.isolated_network_offering.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                zoneid=self.zone.id)
            self.cleanup.append(network)
        except Exception as e:
            self.fail("Isolated network creation failed because: %s" % e)

    elif networkType == SHARED_NETWORK:
        physical_network, vlan = get_free_vlan(self.api_client, self.zone.id)

        # create network using the shared network offering created
        self.services["shared_network"]["acltype"] = "domain"
        self.services["shared_network"]["vlan"] = vlan
        self.services["shared_network"][
            "networkofferingid"] = self.shared_network_offering.id
        self.services["shared_network"][
            "physicalnetworkid"] = physical_network.id

        self.services["shared_network"] = setSharedNetworkParams(
            self.services["shared_network"])

        try:
            network = Network.create(
                self.api_client,
                self.services["shared_network"],
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id)
            self.cleanup.append(network)
        except Exception as e:
            self.fail("Shared Network creation failed because: %s" % e)

    elif networkType == VPC_NETWORK:
        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(
            self.apiclient,
            self.services["vpc"],
            vpcofferingid=self.vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(vpc)
        vpcs = VPC.list(self.apiclient, id=vpc.id)
        self.assertEqual(
            validateList(vpcs)[0],
            PASS,
            "VPC list validation failed, vpc list is %s" %
            vpcs)

        network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_network_offering_vpc.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0")
        self.cleanup.append(network)
    return network


def CreateEnabledNetworkOffering(apiclient, networkServices):
    """Create network offering of given services and enable it"""

    result = createEnabledNetworkOffering(apiclient, networkServices)
    assert result[
        0] == PASS, "Network offering creation/enabling failed due to %s" % result[2]
    return result[1]


def createNetworkRules(
        self,
        virtual_machine,
        network,
        vmguestip,
        networktype,
        ruletype):
    """ Acquire public ip in the given network, open firewall if required and
        create NAT rule for the public ip to the given guest vm ip address"""

    try:
        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if networktype == VPC_NETWORK else None)
        self.cleanup.append(public_ip)

        if networktype != VPC_NETWORK:
            fwr = FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])
            self.cleanup.append(fwr)

        if ruletype == "nat":
            nat_rule = NATRule.create(
                self.api_client,
                virtual_machine,
                self.services["natrule"],
                ipaddressid=public_ip.ipaddress.id,
                networkid=network.id,
                vmguestip=vmguestip)
            self.cleanup.append(nat_rule)
        elif ruletype == "staticnat":
            StaticNATRule.enable(
                self.apiclient,
                public_ip.ipaddress.id,
                virtual_machine.id,
                network.id,
                vmguestip=vmguestip)
    except Exception as e:
        self.debug("Exception occurred while creating network rules: %s" % e)
        return FAIL
    return PASS


@ddt
class TestBasicOperations(cloudstackTestCase):

    """Test Basic operations (add/remove/list) IP to/from NIC
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestBasicOperations, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo().lower()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup = [cls.service_offering]
        cls.services["shared_network_offering_all_services"][
            "specifyVlan"] = "True"
        cls.services["shared_network_offering_all_services"][
            "specifyIpRanges"] = "True"

        cls.shared_network_offering = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["shared_network_offering_all_services"])
        cls._cleanup.append(cls.shared_network_offering)
        cls.mode = cls.zone.networktype
        cls.isolated_network_offering = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)
        cls.isolated_network_offering_vpc = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["nw_offering_isolated_vpc"])
        cls._cleanup.append(cls.isolated_network_offering_vpc)
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"])
        cls.vpc_off.update(cls.api_client, state='Enabled')
        cls._cleanup.append(cls.vpc_off)
        return

    @classmethod
    def tearDownClass(cls):
        super(TestBasicOperations, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestBasicOperations, self).tearDown()

    def VerifyStaticNatForPublicIp(self, ipaddressid, natrulestatus):
        """ List public IP and verify that NAT rule status for the IP is as desired """

        publiciplist = PublicIPAddress.list(
            self.apiclient,
            id=ipaddressid,
            listall=True)
        self.assertEqual(
            validateList(publiciplist)[0],
            PASS,
            "Public IP list validation failed")
        self.assertEqual(
            publiciplist[0].isstaticnat,
            natrulestatus,
            "isstaticnat should be %s, it is %s" %
            (natrulestatus,
             publiciplist[0].isstaticnat))

        return

    @data(SHARED_NETWORK)
    @attr(tags=["advanced"])
    def test_add_ip_to_nic(self, value):
        """ Add secondary IP to NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add secondary IP to the default nic of VM
        # 4. Try to add the same IP again
        # 5. Try to add secondary IP providing wrong virtual machine id
        # 6. Try to add secondary IP with correct virtual machine id but wrong
        # IP address

        # Validations:
        # 1. Step 3 should succeed
        # 2. Step 4 should fail
        # 3. Step 5 should should fail
        # 4. Step 6 should fail

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(virtual_machine)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        try:
            NIC.addIp(
                self.apiclient,
                id=virtual_machine.nic[0].id,
                ipaddress=ipaddress_1.ipaddress)
            self.debug(
                "Adding already added secondary IP %s to NIC of vm %s succeeded, should have failed" %
                (ipaddress_1.ipaddress, virtual_machine.id))
        except Exception as e:
            self.debug(
                "Failed while adding already added secondary IP to NIC of vm %s" %
                virtual_machine.id)

        try:
            NIC.addIp(
                self.apiclient, id=(
                    virtual_machine.nic[0].id + random_gen()))
            self.fail(
                "Adding secondary IP with wrong NIC id succeeded, it shoud have failed")
        except Exception as e:
            self.debug("Failed while adding secondary IP to wrong NIC")

        try:
            NIC.addIp(
                self.apiclient,
                id=virtual_machine.nic[0].id,
                ipaddress="255.255.255.300")
            self.fail(
                "Adding secondary IP with wrong ipaddress succeeded, it should have failed")
        except Exception as e:
            self.debug(
                "Failed while adding wrong secondary IP to NIC of VM %s: %s" %
                (virtual_machine.id, e))
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_remove_ip_from_nic(self, value):
        """ Remove secondary IP from NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add secondary IP to the default nic of VM
        # 4. Remove the secondary IP
        # 5. Try to remove secondary ip by giving incorrect ipaddress id

        # Validations:
        # 1. Step 4 should succeed
        # 2. Step 5 should fail

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(virtual_machine)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        NIC.removeIp(self.apiclient, ipaddressid=ipaddress_1.id)
        #Following block is to verify
        #1.Removing nic in shared network should mark allocated state to NULL in DB
        #2.To make sure that re-add the same ip address to the same nic
        #3.Remove the IP from the NIC
        #All the above steps should succeed
        if value == SHARED_NETWORK:
            qresultset = self.dbclient.execute(
                "select allocated from user_ip_address where public_ip_address = '%s';"
                % str(ipaddress_1.ipaddress)
            )
            self.assertEqual(
                qresultset[0][0],
                None,
                "Removing IP from nic didn't release the ip address from user_ip_address table"
            )
        else:
            qresultset = self.dbclient.execute(
                "select id from nic_secondary_ips where ip4_address = '%s';"
                % str(ipaddress_1.ipaddress))
            if len(qresultset):
                self.fail("Failed to release the secondary ip from the nic")
        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id,
            ipaddress=ipaddress_1.ipaddress
        )
        NIC.removeIp(self.apiclient, ipaddressid=ipaddress_2.id)
        try:
            NIC.removeIp(
                self.apiclient,
                ipaddressid=(
                    ipaddress_1.id +
                    random_gen()))
            self.fail("Removing invalid IP address, it should have failed")
        except Exception as e:
            self.debug(
                "Removing invalid IP failed as expected with Exception %s" %
                e)
        return

    @attr(tags=["advanced"])
    def test_remove_invalid_ip(self):
        """ Remove invalid ip"""

        # Steps:
        # 1. Try to remove secondary ip without passing ip address id

        # Validations:
        # 1. Step 1 should fail

        try:
            NIC.removeIp(self.apiclient, ipaddressid="")
            self.fail(
                "Removing IP address without passing IP succeeded, it should have failed")
        except Exception as e:
            self.debug(
                "Removing IP from NIC without passing ipaddressid failed as expected with Exception %s" %
                e)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_list_nics(self, value):
        """Test listing nics associated with the ip address"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add secondary IP to the default nic of VM
        # 4. Try to list the secondary ips without passing vm id
        # 5. Try to list secondary IPs by passing correct vm id
        # 6. Try to list secondary IPs by passing correct vm id and its nic id
        # 7. Try to list secondary IPs by passing incorrect vm id and correct nic id
        # 8. Try to list secondary IPs by passing correct vm id and incorrect nic id
        # 9. Try to list secondary IPs by passing incorrect vm id and incorrect
        # nic id

        # Validations:
        # 1. Step 4 should fail
        # 2. Step 5 should succeed
        # 3. Step 6 should succeed
        # 4. Step 7 should fail
        # 5. Step 8 should fail
        # 6. Step 9 should fail

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(virtual_machine)

        NIC.addIp(self.apiclient, id=virtual_machine.nic[0].id)

        try:
            nics = NIC.list(self.apiclient)
            self.fail(
                "Listing NICs without passign VM id succeeded, it should have failed, list is %s" %
                nics)
        except Exception as e:
            self.debug(
                "Listing NICs without passing virtual machine id failed as expected")

        try:
            NIC.list(self.apiclient, virtualmachineid=virtual_machine.id)
        except Exception as e:
            self.fail(
                "Listing NICs for virtual machine %s failed with Exception %s" %
                (virtual_machine.id, e))

        NIC.list(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            nicid=virtual_machine.nic[0].id)

        try:
            nics = NIC.list(
                self.apiclient,
                virtualmachineid=(
                    virtual_machine.id +
                    random_gen()),
                nicid=virtual_machine.nic[0].id)
            self.fail(
                "Listing NICs with wrong virtual machine id and right nic id succeeded, should have failed")
        except Exception as e:
            self.debug(
                "Listing NICs with wrong virtual machine id and right nic failed as expected with Exception %s" %
                e)

        try:
            nics = NIC.list(
                self.apiclient,
                virtualmachineid=virtual_machine.id,
                nicid=(
                    virtual_machine.nic[0].id +
                    random_gen()))
            self.fail(
                "Listing NICs with correct virtual machine id but wrong nic id succeeded, should have failed")
        except Exception as e:
            self.debug(
                "Listing NICs with correct virtual machine id but wrong nic id failed as expected with Exception %s" %
                e)

        try:
            nics = NIC.list(
                self.apiclient,
                virtualmachineid=(
                    virtual_machine.id +
                    random_gen()),
                nicid=(
                    virtual_machine.nic[0].id +
                    random_gen()))
            self.fail(
                "Listing NICs with wrong virtual machine id and wrong nic id succeeded, should have failed")
        except Exception as e:
            self.debug(
                "Listing NICs with wrong virtual machine id and wrong nic id failed as expected with Exception %s" %
                e)

        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_operations_non_root_admin_api_client(self, value):
        """Test basic operations using non root admin apii client"""

        # Steps:
        # 1. Create Domain and Account in it
        # 2. Create network in it (isoalted/ shared/ vpc)
        # 3. Create User API client of this account
        # 4. Deploy a VM in this network and account
        # 5. Add secondary IP to the default nic of VM using non root admin api client
        # 6. List secondary IPs using non root admin api client
        # 7. Remove secondary IP using non root admin api client

        # Validations:
        # 1. All the operations should be successful

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        child_domain = Domain.create(
            self.apiclient,
            services=self.services["domain"],
            parentdomainid=self.domain.id)
        self.cleanup.append(child_domain)

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=child_domain.id)
        self.cleanup.append(self.account)

        apiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(virtual_machine)

        ipaddress_1 = NIC.addIp(apiclient, id=virtual_machine.nic[0].id)

        try:
            NIC.list(apiclient, virtualmachineid=virtual_machine.id)
        except Exception as e:
            self.fail(
                "Listing NICs for virtual machine %s failed with Exception %s" %
                (virtual_machine.id, e))

        try:
            NIC.list(
                apiclient,
                virtualmachineid=virtual_machine.id,
                nicid=virtual_machine.nic[0].id)
        except Exception as e:
            self.fail(
                "Listing NICs for virtual machine %s and nic id %s failed with Exception %s" %
                (virtual_machine.id, virtual_machine.nic[0].id, e))

        try:
            NIC.removeIp(apiclient, ipaddressid=ipaddress_1.id)
        except Exception as e:
            self.fail(
                "Removing seondary IP %s from NIC failed as expected with Exception %s" %
                (ipaddress_1.id, e))

        return


@ddt
class TestNetworkRules(cloudstackTestCase):

    """Test PF/NAT/static nat rules with the secondary IPs
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetworkRules, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo().lower()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup = [cls.service_offering]
        cls.services["shared_network_offering_all_services"][
            "specifyVlan"] = "True"
        cls.services["shared_network_offering_all_services"][
            "specifyIpRanges"] = "True"

        cls.shared_network_offering = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["shared_network_offering_all_services"])
        cls._cleanup.append(cls.shared_network_offering)
        cls.mode = cls.zone.networktype
        cls.isolated_network_offering = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)
        cls.isolated_network_offering_vpc = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["nw_offering_isolated_vpc"])
        cls._cleanup.append(cls.isolated_network_offering_vpc)
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"])
        cls._cleanup.append(cls.vpc_off)
        cls.vpc_off.update(cls.api_client, state='Enabled')
        return

    @classmethod
    def tearDownClass(cls):
        super(TestNetworkRules, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestNetworkRules, self).tearDown()

    def VerifyStaticNatForPublicIp(self, ipaddressid, natrulestatus):
        """ List public IP and verify that NAT rule status for the IP is as desired """

        publiciplist = PublicIPAddress.list(
            self.apiclient,
            id=ipaddressid,
            listall=True)
        self.assertEqual(
            validateList(publiciplist)[0],
            PASS,
            "Public IP list validation failed")
        self.assertEqual(
            publiciplist[0].isstaticnat,
            natrulestatus,
            "isstaticnat should be %s, it is %s" %
            (natrulestatus,
             publiciplist[0].isstaticnat))

        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced", "dvs"])
    def test_add_PF_rule(self, value):
        """ Add secondary IP to NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire public IP, open firewall for it, and
        #    create NAT rule for this public IP to the 1st secondary IP
        # 5. Repeat step 4 for another public IP
        # 6. Repeat step 4 for 2nd secondary IP
        # 7. Repeat step 4 for invalid secondary IP

        # Validations:
        # 1. Step 4 should succeed
        # 2. Step 5 should succeed
        # 3. Step 6 should succeed
        # 4. Step 7 should fail

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(virtual_machine)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                ipaddress_1.ipaddress,
                value,
                ruletype="nat"),
            PASS,
            "Failure in creating NAT rule")
        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                ipaddress_1.ipaddress,
                value,
                ruletype="nat"),
            PASS,
            "Failure in creating NAT rule")
        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                ipaddress_2.ipaddress,
                value,
                ruletype="nat"),
            PASS,
            "Failure in creating NAT rule")
        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                "255.255.255.300",
                value,
                ruletype="nat"),
            FAIL,
            "Failure in NAT rule creation")
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced", "dvs"])
    def test_delete_PF_nat_rule(self, value):
        """ Add secondary IP to NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add secondary IP to the default nic of VM
        # 4. Acquire public IP, open firewall for it, and
        #    create NAT rule for this public IP to the 1st secondary IP
        # 5. Try to delete secondary IP when NAT rule exists for it
        # 6. Delete firewall rule and NAT rule

        # Validations:
        # 1. Step 5 should fail
        # 2. Step 6 should succeed

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)
        firewallrule = None

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)
        self.cleanup.append(virtual_machine)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)
        self.cleanup.append(public_ip)

        if value != VPC_NETWORK:
            firewallrule = FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])
            self.cleanup.append(firewallrule)

        # Create NAT rule
        natrule = NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)
        self.cleanup.append(natrule)
        try:
            NIC.removeIp(self.apiclient, ipaddressid=ipaddress_1.id)
            self.fail(
                "Removing secondary IP succeeded while it had active NAT rule on it, should have failed")
        except Exception as e:
            self.debug(
                "Removing secondary IP with active NAT rule failed as expected")

        if firewallrule:
            try:
                firewallrule.delete(self.apiclient)
                self.cleanup.remove(firewallrule)
            except Exception as e:
                self.fail(
                    "Exception while deleting firewall rule %s: %s" %
                    (firewallrule.id, e))

        natrule.delete(self.apiclient)
        self.cleanup.remove(natrule)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced", "dvs"])
    def test_disassociate_ip_mapped_to_secondary_ip_through_PF_rule(
            self,
            value):
        """ Add secondary IP to NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add secondary IP to the default nic of VM
        # 4. Acquire public IP, open firewall for it, and
        #    create NAT rule for this public IP to the 1st secondary IP
        # 5. Try to delete the public IP used for NAT rule

        # Validations:
        # 1. Step 5 should succeed

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        # Create NAT rule
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)

        public_ip.delete(self.apiclient)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced", "dvs"])
    def test_add_static_nat_rule(self, value):
        """ Add secondary IP to NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire public IP, open firewall for it, and
        #    create static NAT rule for this public IP to the 1st secondary IP
        # 5. Repeat step 4 for another public IP
        # 6. Repeat step 4 for 2nd secondary IP
        # 7. Repeat step 4 for invalid secondary IP
        # 8. Try to remove 1st secondary IP (with active static nat rule)

        # Validations:
        # 1. Step 4 should succeed
        # 2. Step 5 should succeed
        # 3. Step 6 should succeed
        # 4. Step 7 should fail
        # 5. Step 8 should succeed

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                ipaddress_1.ipaddress,
                value,
                ruletype="staticnat"),
            PASS,
            "Failure in creating NAT rule")
        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                ipaddress_1.ipaddress,
                value,
                ruletype="staticnat"),
            FAIL,
            "Failure in creating NAT rule")
        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                ipaddress_2.ipaddress,
                value,
                ruletype="staticnat"),
            PASS,
            "Failure in creating NAT rule")
        self.assertEqual(
            createNetworkRules(
                self,
                virtual_machine,
                network,
                "255.255.255.300",
                value,
                ruletype="staticnat"),
            FAIL,
            "Failure in NAT rule creation")

        try:
            NIC.removeIp(self.apiclient, ipaddress_1.id)
            self.fail(
                "Ip address should not get removed when active static NAT rule is defined for it")
        except Exception as e:
            self.debug(
                "Exception while removing secondary ip address as expected\
                        because static nat rule is present for it: %s" % e)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced", "dvs"])
    def test_disable_static_nat(self, value):
        """ Add secondary IP to NIC of a VM"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire public IP, open firewall for it, and
        #    enable static NAT rule for this public IP to the 1st secondary IP
        # 5. Disable the static nat rule and enable it again

        # Validations:
        # 1. Verify step 5 by listing seconday IP and checking the appropriate
        # flag

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        StaticNATRule.enable(
            self.apiclient,
            public_ip.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_1.ipaddress)

        self.VerifyStaticNatForPublicIp(public_ip.ipaddress.id, True)

        # Disabling static NAT
        StaticNATRule.disable(self.apiclient, public_ip.ipaddress.id)

        self.VerifyStaticNatForPublicIp(public_ip.ipaddress.id, False)

        StaticNATRule.enable(
            self.apiclient,
            public_ip.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_1.ipaddress)

        self.VerifyStaticNatForPublicIp(public_ip.ipaddress.id, True)

        public_ip.delete(self.apiclient)
        return


@ddt
class TestVmNetworkOperations(cloudstackTestCase):

    """Test VM and Network operations with network rules created on secondary IP
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVmNetworkOperations, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo().lower()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services[
                "ostype"]
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup = [cls.service_offering]
        cls.services["shared_network_offering_all_services"][
            "specifyVlan"] = "True"
        cls.services["shared_network_offering_all_services"][
            "specifyIpRanges"] = "True"

        cls.shared_network_offering = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["shared_network_offering_all_services"])
        cls._cleanup.append(cls.shared_network_offering)
        cls.mode = cls.zone.networktype
        cls.isolated_network_offering = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["isolated_network_offering"])
        cls._cleanup.append(cls.isolated_network_offering)
        cls.isolated_network_offering_vpc = CreateEnabledNetworkOffering(
            cls.api_client,
            cls.services["nw_offering_isolated_vpc"])
        cls._cleanup.append(cls.isolated_network_offering_vpc)
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"])
        cls.vpc_off.update(cls.api_client, state='Enabled')
        cls._cleanup.append(cls.vpc_off)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
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
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def VerifyStaticNatForPublicIp(self, ipaddressid, natrulestatus):
        """ List public IP and verify that NAT rule status for the IP is as desired """

        publiciplist = PublicIPAddress.list(
            self.apiclient,
            id=ipaddressid,
            listall=True)
        self.assertEqual(
            validateList(publiciplist)[0],
            PASS,
            "Public IP list validation failed")
        self.assertEqual(
            publiciplist[0].isstaticnat,
            natrulestatus,
            "isstaticnat should be %s, it is %s" %
            (natrulestatus,
             publiciplist[0].isstaticnat))

        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_delete_vm(self, value):
        """ Test delete VM and verify network rules are cleaned up"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire 2 public IPs in the network
        # 5. For 1st public IP create nat rule to 1st private IP, for 2nd public IP, create
        #    static nat rule to 2nd private IP
        # 6. Destroy the virtual machine
        # 7. Verify that nat rule does not exist and static nat is not enabled for
        #    secondary IP

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        # Add secondary IPs to default NIC of VM
        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)
        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        # Acquire public IP addresses in the network
        public_ip_1 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        public_ip_2 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        # Create Firewall and natrule for 1st IP and static nat rule for 2nd IP
        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip_1.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        natrule = NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)

        StaticNATRule.enable(
            self.apiclient,
            public_ip_2.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_2.ipaddress)

        # Delete VM
        virtual_machine.delete(self.apiclient, expunge=True)

        # Make sure the VM is expunged
        retriesCount = 20
        while True:
            vms = VirtualMachine.list(self.apiclient, id=virtual_machine.id)
            if vms is None:
                break
            elif retriesCount == 0:
                self.fail("Failed to expunge vm even after 20 minutes")
            time.sleep(60)
            retriesCount -= 1

        # Try to list nat rule
        with self.assertRaises(Exception):
            NATRule.list(self.apiclient, id=natrule.id, listall=True)

        # Verify static nat rule is no longer enabled
        self.VerifyStaticNatForPublicIp(public_ip_2.ipaddress.id, False)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_recover_vm(self, value):
        """ Test recover VM operation with VM having secondary IPs"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire 2 public IPs in the network
        # 5. For 1st public IP create nat rule to 1st private IP, for 2nd public IP, create
        #    static nat rule to 2nd private IP
        # 6. Destroy the virtual machine and recover it
        # 7. Verify that nat and static nat rules exist

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)
        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip_1 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        public_ip_2 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip_1.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        natrule = NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)

        StaticNATRule.enable(
            self.apiclient,
            public_ip_2.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_2.ipaddress)

        virtual_machine.delete(self.apiclient, expunge=False)
        virtual_machine.recover(self.apiclient)

        retriesCount = 10
        while True:
            vms = VirtualMachine.list(self.apiclient, id=virtual_machine.id)
            self.assertEqual(
                validateList(vms)[0],
                PASS,
                "vms list validation failed")
            if str(vms[0].state).lower() == "stopped":
                break
            elif retriesCount == 0:
                self.fail("Failed to recover vm even after 10 mins")
            time.sleep(60)
            retriesCount -= 1

        natrules = NATRule.list(self.apiclient, id=natrule.id, listall=True)
        self.assertEqual(
            validateList(natrules)[0],
            PASS,
            "nat rules validation failed")

        self.VerifyStaticNatForPublicIp(public_ip_2.ipaddress.id, True)

        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_network_restart_cleanup_true(self, value):
        """Test network restart (cleanup True) with VM having secondary IPs and related network rules"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire 2 public IPs in the network
        # 5. For 1st public IP create nat rule to 1st private IP, for 2nd public IP, create
        #    static nat rule to 2nd private IP
        # 6. Restart the network with cleanup option True
        # 7. Verify that nat and static nat rules exist after network restart

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)
        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip_1 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        public_ip_2 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip_1.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        natrule = NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)

        StaticNATRule.enable(
            self.apiclient,
            public_ip_2.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_2.ipaddress)

        network.restart(self.apiclient, cleanup=True)

        natrulelist = NATRule.list(self.apiclient, id=natrule.id, listall=True)
        self.assertEqual(
            validateList(natrulelist)[0],
            PASS,
            "nat rules list validation failed")

        self.VerifyStaticNatForPublicIp(public_ip_2.ipaddress.id, True)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_network_restart_cleanup_false(self, value):
        """Test network restart (cleanup True) with VM having secondary IPs and related network rules"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire 2 public IPs in the network
        # 5. For 1st public IP create nat rule to 1st private IP, for 2nd public IP, create
        #    static nat rule to 2nd private IP
        # 6. Restart the network with cleanup option False
        # 7. Verify that nat and static nat rules exist after network restart

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)
        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip_1 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        public_ip_2 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip_1.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        natrule = NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)

        StaticNATRule.enable(
            self.apiclient,
            public_ip_2.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_2.ipaddress)

        network.restart(self.apiclient, cleanup=False)

        natrulelist = NATRule.list(self.apiclient, id=natrule.id, listall=True)
        self.assertEqual(
            validateList(natrulelist)[0],
            PASS,
            "nat rules list validation failed")

        self.VerifyStaticNatForPublicIp(public_ip_2.ipaddress.id, True)
        return

    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    @attr(tags=["advanced"])
    def test_reboot_router_VM(self, value):
        """ Test reboot router and persistence of network rules"""

        # Steps:
        # 1. Create Account and create network in it (isoalted/ shared/ vpc)
        # 2. Deploy a VM in this network and account
        # 3. Add 2 secondary IPs to the default nic of VM
        # 4. Acquire 2 public IPs in the network
        # 5. For 1st public IP create nat rule to 1st private IP, for 2nd public IP, create
        #    static nat rule to 2nd private IP
        # 6. Reboot router VM
        # 7. Verify that nat and static nat rules exist after router restart

        if value == VPC_NETWORK and self.hypervisor == 'hyperv':
            self.skipTest("VPC is not supported on Hyper-V")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(self.account)

        network = createNetwork(self, value)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            networkids=[
                network.id],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid)

        ipaddress_1 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)
        ipaddress_2 = NIC.addIp(
            self.apiclient,
            id=virtual_machine.nic[0].id)

        public_ip_1 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        public_ip_2 = PublicIPAddress.create(
            self.api_client,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id,
            vpcid=network.vpcid if value == VPC_NETWORK else None)

        if value != VPC_NETWORK:
            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip_1.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"])

        natrule = NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            networkid=network.id,
            vmguestip=ipaddress_1.ipaddress)

        StaticNATRule.enable(
            self.apiclient,
            public_ip_2.ipaddress.id,
            virtual_machine.id,
            network.id,
            vmguestip=ipaddress_2.ipaddress)

        routers = Router.list(
            self.apiclient,
            networkid=network.id,
            listall=True)
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "routers list validation failed")

        Router.reboot(self.apiclient, id=routers[0].id)

        natrulelist = NATRule.list(self.apiclient, id=natrule.id, listall=True)
        self.assertEqual(
            validateList(natrulelist)[0],
            PASS,
            "nat rules list validation failed")

        self.VerifyStaticNatForPublicIp(public_ip_2.ipaddress.id, True)
        return
