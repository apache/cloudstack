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

import unittest
import logging
from nose.plugins.attrib import attr
from should_dsl import should, should_not

from marvin.cloudstackTestClient import cloudstackTestClient

from marvin.factory.data.account import UserAccount, AdminAccount, DomainAdmin
from marvin.factory.data.serviceoffering import *
from marvin.factory.data.diskoffering import *
from marvin.factory.data.template import *
from marvin.factory.data.user import *
from marvin.factory.data.networkoffering import *
from marvin.factory.data.network import *
from marvin.factory.data.vm import *
from marvin.factory.data.firewallrule import SshFirewallRule
from marvin.factory.data.vpc import DefaultVpc

from marvin.factory.virtualmachine import *

from marvin.entity.firewall import Firewall
from marvin.entity.serviceoffering import ServiceOffering
from marvin.entity.diskoffering import DiskOffering
from marvin.entity.networkoffering import NetworkOffering
from marvin.entity.zone import Zone
from marvin.entity.account import Account
from marvin.entity.template import Template
from marvin.entity.user import User
from marvin.entity.network import Network
from marvin.entity.ipaddress import IpAddress

from marvin.util import *


class BuildVsCreateStrategyTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        pass

    def test_buildUserAccountFactory(self):
        af = UserAccount.build()
        self.assert_(af is not None, msg="Account factory didn't initialize")

    def test_createAccountFactory(self):
        af = UserAccount(apiclient=self.apiClient)
        self.assert_(isinstance(af, Account))
        self.assert_(af.id is not None, msg="Account creation failed")
        self.assert_(af.domain is not None, msg="Account belongs to no domain")


class AccountFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_adminAccountFactory(self):
        accnt = AdminAccount(apiclient=self.apiClient)
        self.assert_(accnt is not None, msg="no account created by factory")
        self.assert_(accnt.name is not None)

    def test_userAccountFactoryCustomArgs(self):
        accnt = UserAccount(apiclient=self.apiClient, firstname='test', lastname='test')
        a = accnt.list(apiclient=self.apiClient, account=accnt.name, domainid=accnt.domainid)
        self.assert_(accnt is not None, msg="no account created by factory")
        self.assert_(accnt.name is not None)

    def test_disableAccountPostFactoryGeneration(self):
        domadmin = DomainAdmin(apiclient=self.apiClient)
        a = Account.list(apiclient=self.apiClient, id=domadmin.id)
        self.assert_(domadmin is not None, msg="no account was created")
        domadmin.disable(lock=True, account=domadmin.name, domainid=domadmin.domainid)

    def tearDown(self):
        pass


class ServiceOfferingFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_serviceOfferingFactory(self):
        soffering = SmallServiceOffering(apiclient=self.apiClient)
        self.assert_(soffering is not None, msg="no service offering was created")
        self.assert_(soffering.name is not None, msg="error in service offering factory creation")


    def tearDown(self):
        pass


class NetworkOfferingFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_defaultSourceNatOfferingFactory(self):
        snatOffering = DefaultIsolatedNetworkOfferingWithSourceNatService(apiclient=self.apiClient)
        self.assert_(snatOffering is not None, msg = "no network offering was created")
        self.assert_(snatOffering.name is not None, msg="error in network offering creation")

    @attr(tags='offering')
    def test_defaultSGOfferingEnable(self):
        DefaultSharedNetworkOfferingWithSGService(apiclient=self.apiClient)

    def tearDown(self):
        pass


class UserFactorySubFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        pass

    @unittest.skip("This is a chicken and egg problem")
    def test_userSubFactory(self):
        """
        Skip because users are contained in accounts but
        cannot be created until accounts exist

        A subfactory is unsuitable as the semantics of the
        caller is not to create the user before creating the account
        @return:
        """
        uf = User(apiclient=self.apiClient)
        user = User.list(apiclient=self.apiClient, username=uf.username)
        self.assert_(uf.username == user[0].username, msg="Usernames don't match")


class NetworkFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        self.accnt.delete()

    @attr(tags='network')
    def test_isolatedGuestNetwork(self):
        self.accnt = UserAccount(apiclient=self.apiClient)
        zones = Zone.list(apiclient=self.apiClient)
        network = GuestIsolatedNetwork(
            apiclient=self.apiClient,
            zoneid=zones[0].id
            )
        logging.getLogger('factory.cloudstack').debug("network created with id %s, name %s" %(network.id, network.name))


class NetworkOfferingFactoryWithMultiplePostHooksTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    @attr(tags='post')
    def test_multiplePostHooksNetworkOffering(self):
        sharedOffering = DefaultSharedNetworkOffering(apiclient=self.apiClient)
        sharedOffering |should| be_instance_of(NetworkOffering)
        sharedOffering |should_not| equal_to(None)
        sharedOffering.state |should| equal_to('Enabled')
        logging.getLogger('factory.cloudstack').debug("networkoffering created with id %s, name %s, state %s"
                                                      %(sharedOffering.id, sharedOffering.name, sharedOffering.state))


class VirtualMachineTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        self.vm.destroy()

    def test_virtualMachineDeploy(self):
        accnt = UserAccount(apiclient=self.apiClient)
        service = SmallServiceOffering(apiclient=self.apiClient)
        tf = DefaultBuiltInTemplate.build()
        zones = Zone.list(apiclient=self.apiClient)
        template = Template.list(apiclient=self.apiClient,
                                 templatefilter="featured",
                                 ostype = tf.ostype,
                                 zoneid = zones[0].id)
        self.vm = VirtualMachineFactory(apiclient=self.apiClient,
                                          serviceofferingid = service.id,
                                          templateid = template[0].id,
                                          zoneid = zones[0].id,
                                          account = accnt.name,
                                          domainid = accnt.domainid)


class IpAddressFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        self.vm.destroy()

    def test_associateIpAddressToNetwork(self):
        # user account where we run test
        accnt = UserAccount(apiclient=self.apiClient)
        self.assert_(isinstance(accnt, Account))

        # get required arguments - templates, service offerings, zone
        service = ServiceOffering.list(apiclient=self.apiClient, displaytext='Small')
        template = Template.list(apiclient=self.apiClient, templatefilter="featured")
        zones = Zone.list(apiclient=self.apiClient)

        self.vm = VirtualMachineFactory(
            apiclient=self.apiClient,
            serviceofferingid=service[0].id,
            templateid=template[0].id,
            zoneid=zones[0].id,
            account=accnt.name,
            domainid=accnt.domainid)

        networks = Network.list(apiclient=self.apiClient,
            account = accnt.name, domainid = accnt.domainid)
        IpAddress(apiclient=self.apiClient, networkid = networks[0].id)


class FirewallRuleFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        self.account.delete()

    @attr(tags='firewall')
    def test_firewallRuleFactoryTest(self):
        self.account = UserAccount(apiclient=self.apiClient)
        domainid = get_domain(self.apiClient).id
        self.account |should| be_instance_of(Account)
        vm = VirtualMachineFactory(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=domainid,
            templateid=get_template(self.apiClient).id,
            serviceofferingid=get_service_offering(self.apiClient).id,
            zoneid=get_zone(self.apiClient).id
        )
        vm |should| be_instance_of(VirtualMachine)
        vm.state |should| equal_to('Running')
        vm.nic |should_not| equal_to(None)
        vm.nic |should| be_instance_of(list)

        ipaddress = IpAddress(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=domainid,
            zoneid=get_zone(self.apiClient).id
        )
        ipaddress |should_not| be(None)
        ipaddress |should| be_instance_of(IpAddress)

        fwrule = SshFirewallRule(
            apiclient=self.apiClient,
            ipaddressid=ipaddress.id
        )
        fwrule |should_not| be(None)
        fwrule |should| be_instance_of(Firewall)


@attr(tags='disk')
class DiskOfferingFactoryTest(unittest.TestCase):

    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_createSharedDiskOffering(self):
        shared_disk_offering = SharedDiskOffering(apiclient=self.apiClient)
        shared_disk_offering |should_not| be(None)
        shared_disk_offering.name |should_not| equal_to(None)
        shared_disk_offering.delete()

    def test_createLocalDiskOffering(self):
        local_disk_offering = LocalDiskOffering(apiclient=self.apiClient)
        local_disk_offering | should_not | be(None)
        local_disk_offering.name | should_not | equal_to(None)
        local_disk_offering.delete()

    def test_listDeletedDiskOfferings(self):
        local_disk_offering = LocalDiskOffering(apiclient=self.apiClient)
        local_disk_offering | should_not | be(None)
        local_disk_offering.name | should_not | equal_to(None)
        local_disk_offering.delete()

        listed_offering = DiskOffering.list(apiclient=self.apiClient, name=local_disk_offering.name)
        listed_offering | should | be(None)


@attr(tags='gc')
class DeleteAllNonAdminAccounts(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_deleteAllNonAdminAccounts(self):
        accounts = Account.list(apiclient=self.apiClient, listall=True)
        for account in accounts:
            if account.accounttype == 0 or account.accounttype == 2 or account.accounttype == 1:
                if account.name == 'admin':
                    continue
                account.delete(apiclient=self.apiClient)


@attr(tags='post')
class StaticNatPostVirtualMachineDeployHook(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_staticNatPostVmCreation(self):
        self.account = UserAccount(apiclient=self.apiClient)
        domainid = get_domain(self.apiClient).id
        self.account |should| be_instance_of(Account)
        vm = VirtualMachineWithStaticNat(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=domainid,
            templateid=get_template(self.apiClient).id,
            serviceofferingid=get_service_offering(self.apiClient).id,
            zoneid=get_zone(self.apiClient).id
        )
        vm |should| be_instance_of(VirtualMachine)
        vm.state |should| equal_to('Running')
        vm.nic |should_not| equal_to(None)
        vm.nic |should| be_instance_of(list)

    def tearDown(self):
        self.account.delete()

class VpcCreationTest(unittest.TestCase):

    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_vpcCreation(self):
        self.account = UserAccount(apiclient=self.apiClient)
        self.vpc = DefaultVpc(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=get_domain(apiclient=self.apiClient).id,
            zoneid=get_zone(apiclient=self.apiClient).id
        )
        self.vpc |should_not| be(None)
        ntwk = DefaultVpcNetwork(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=get_domain(apiclient=self.apiClient).id,
            vpcid=self.vpc.id,
            zoneid=get_zone(apiclient=self.apiClient).id
        )
        ntwk | should_not | be(None)
        ntwk.state | should | equal_to('Allocated')
        ntwk.delete()

    def tearDown(self):
        self.vpc.delete()
        self.account.delete()

@attr(tags='vpc')
class VpcVmDeployTest(unittest.TestCase):

    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_vpcVmDeploy(self):
        self.account = UserAccount(apiclient=self.apiClient)
        self.vpc = DefaultVpc(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=get_zone(apiclient=self.apiClient).id
        )
        self.vpc |should_not| be(None)
        ntwk = DefaultVpcNetwork(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            vpcid=self.vpc.id,
            zoneid=get_zone(apiclient=self.apiClient).id
        )
        ntwk | should_not | be(None)
        ntwk.state | should | equal_to('Allocated')
        vm = VirtualMachineFactory(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            templateid=get_template(self.apiClient).id,
            serviceofferingid=get_service_offering(self.apiClient).id,
            networkid=ntwk.id,
            zoneid=get_zone(self.apiClient).id
        )
        vm | should_not | be(None)
        vm.state | should | equal_to("Running")

    def tearDown(self):
        self.account.delete()


@attr(tags='single')
class VpcVmFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost',
            logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_vpcVmDeployShort(self):
        self.account = UserAccount(apiclient=self.apiClient)
        vm = VpcVirtualMachine(
            apiclient=self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            templateid=get_template(self.apiClient).id,
            serviceofferingid=get_service_offering(self.apiClient).id,
            zoneid=get_zone(self.apiClient).id
        )
        vm | should_not | be(None)
        vm.state | should | equal_to("Running")

    def tearDown(self):
        self.account.delete()
