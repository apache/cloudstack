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

from marvin.cloudstackTestClient import cloudstackTestClient

from marvin.factory.data.account import UserAccountFactory, AdminAccountFactory, DomainAdminFactory
from marvin.factory.data.serviceoffering import *
from marvin.factory.data.template import *
from marvin.factory.data.user import *
from marvin.factory.data.networkoffering import *

from marvin.factory.VirtualMachineFactory import *

from marvin.entity.serviceoffering import ServiceOffering
from marvin.entity.zone import Zone
from marvin.entity.account import Account
from marvin.entity.template import Template
from marvin.entity.user import User
from marvin.entity.network import Network

from marvin.entity.ipaddress import IpAddress
class BuildVsCreateStrategyTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        pass

    def test_buildUserAccountFactory(self):
        af = UserAccountFactory()
        self.assert_(af is not None, msg="Account factory didn't initialize")

    def test_createAccountFactory(self):
        af = UserAccountFactory.create(apiclient=self.apiClient)
        self.assert_(isinstance(af, Account))
        self.assert_(af.id is not None, msg="Account creation failed")
        self.assert_(af.domain is not None, msg="Account belongs to no domain")


class AccountFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_adminAccountFactory(self):
        accnt = AdminAccountFactory.create(apiclient=self.apiClient)
        self.assert_(accnt is not None, msg="no account created by factory")
        self.assert_(accnt.name is not None)

    def test_userAccountFactoryCustomArgs(self):
        accnt = UserAccountFactory.create(apiclient=self.apiClient, firstname='test', lastname='test')
        a = accnt.list(apiclient=self.apiClient, account=accnt.name, domainid=accnt.domainid)
        self.assert_(accnt is not None, msg="no account created by factory")
        self.assert_(accnt.name is not None)

    def test_disableAccountPostFactoryGeneration(self):
        domadmin = DomainAdminFactory.create(apiclient=self.apiClient)
        self.assert_(domadmin is not None, msg="no account was created")
        domadmin.disable(self.apiClient, lock=True, account=domadmin.name, domainid=domadmin.domainid)

    def tearDown(self):
        pass


class ServiceOfferingFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_serviceOfferingFactory(self):
        soffering = SmallServiceOfferingFactory.create(apiclient=self.apiClient)
        self.assert_(soffering is not None, msg="no service offering was created")
        self.assert_(soffering.name is not None, msg="error in service offering factory creation")


    def tearDown(self):
        pass


class NetworkOfferingFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def test_defaultSourceNatOfferingFactory(self):
        snatOffering = DefaultIsolatedNetworkOfferingWithSourceNatServiceFactory.create(apiclient=self.apiClient)
        self.assert_(snatOffering is not None, msg = "no network offering was created")
        self.assert_(snatOffering.name is not None, msg="error in network offering creation")

    def test_defaultSGOfferingEnable(self):
        sgOffering = DefaultSharedNetworkOfferingWithSGServiceFactory.create(apiclient=self.apiClient)
        sgOffering.update(self.apiClient, state='Enabled', name=sgOffering.name, id=sgOffering.id)

    def tearDown(self):
        pass


class VirtualMachineFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        pass

    def test_virtualMachineDeploy(self):
        accnt = UserAccountFactory.create(apiclient=self.apiClient)
        service = SmallServiceOfferingFactory.create(apiclient=self.apiClient)
        tf = DefaultBuiltInTemplateFactory.build() #FIXME: Using build() strategy is confusing
        zones = Zone.list(apiclient=self.apiClient)
        template = Template.list(apiclient=self.apiClient,
                                 templatefilter="featured",
                                 ostype = tf.ostype,
                                 zoneid = zones[0].id)
        vm = VirtualMachineFactory.create(apiclient=self.apiClient,
                                          serviceofferingid = service.id,
                                          templateid = template[0].id,
                                          zoneid = zones[0].id,
                                          account = accnt.name,
                                          domainid = accnt.domainid)
        vm.destroy(apiclient=self.apiClient)

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
        uf = UserFactory.create(apiclient=self.apiClient)
        user = User.list(apiclient=self.apiClient, username=uf.username)
        self.assert_(uf.username == user[0].username, msg="Usernames don't match")


class IpAddressFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost', logging=logging.getLogger('factory.cloudstack')).getApiClient()

    def tearDown(self):
        self.vm.destroy(apiclient=self.apiClient)

    def test_associateIpAddressToNetwork(self):
        accnt = UserAccountFactory.create(apiclient=self.apiClient)
        self.assert_(accnt is not None)
        self.assert_(isinstance(accnt, Account))
        service = ServiceOffering.list(apiclient=self.apiClient, displaytext='Small')
        self.assert_(len(service) > 0)
        template = Template.list(apiclient=self.apiClient, templatefilter="featured")
        self.assert_(len(template) > 0)
        zones = Zone.list(apiclient=self.apiClient)
        self.vm = VirtualMachineFactory.create(
            apiclient=self.apiClient,
            serviceofferingid = service[0].id,
            templateid = template[0].id,
            zoneid = zones[0].id,
            account=accnt.name,
            domainid=accnt.domainid)
        all_ips = IpAddress.listPublic(apiclient=self.apiClient)
        firstip = all_ips[0]
        networks = Network.list(apiclient=self.apiClient, account = accnt.name, domainid = accnt.domainid)
        firstip.associate(apiclient=self.apiClient, networkid = networks[0].id)

