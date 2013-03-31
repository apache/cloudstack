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
from marvin.cloudstackTestClient import cloudstackTestClient

from marvin.integration.lib.factory.AccountFactory import *
from marvin.integration.lib.base.Account import Account

from marvin.integration.lib.factory.ServiceOfferingFactory import *
from marvin.integration.lib.base.ServiceOffering import ServiceOffering

from marvin.integration.lib.factory.NetworkOfferingFactory import *
from marvin.integration.lib.base.NetworkOffering import NetworkOffering

class AccountFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost').getApiClient()

    def test_userAccountFactory(self):
        af = AccountFactory()
        accnt = Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(accnt is not None, msg="no account created by factory")
        self.assertEqual(accnt.name, af.username, msg="account names are not same")

    def test_adminAccountFactory(self):
        af = AdminAccountFactory()
        accnt = Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(accnt is not None, msg="no account created by factory")
        self.assertEqual(accnt.name, af.username, msg="account names are not same")

    def test_userAccountFactoryCustomArgs(self):
        af = AccountFactory(firstname='test', lastname='test')
        accnt = Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(accnt is not None, msg="no account created by factory")
        self.assertEqual(accnt.name, af.username, msg="account names are not same")

    @unittest.skip("Job Queue gets stuck on this")
    def test_disableAccountPostFactoryGeneration(self):
        af = DomainAdminFactory()
        domadmin = Account.create(apiclient=self.apiClient, AccountFactory=af)
        self.assertTrue(domadmin is not None, msg="no account was created")
        self.assertEqual(domadmin.name, af.username, msg = "account names don't match")
        domadmin.disable(self.apiClient, lock=True)

    def tearDown(self):
        pass


class ServiceOfferingFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost').getApiClient()

    def test_serviceOfferingFactory(self):
        sf = ServiceOfferingFactory()
        soffering = ServiceOffering.create(apiclient=self.apiClient, ServiceOfferingFactory=sf)
        self.assertTrue(soffering is not None, msg="no service offering was created")
        self.assertEqual(soffering.name, sf.name, msg="error in service offering factory creation")


    def tearDown(self):
        pass


class NetworkOfferingFactoryTest(unittest.TestCase):
    def setUp(self):
        self.apiClient = cloudstackTestClient(mgtSvr='localhost').getApiClient()

    def test_defaultSourceNatOfferingFactory(self):
        snatOfferingFactory = DefaultIsolatedNetworkOfferingWithSourceNatServiceFactory()
        snatOffering = NetworkOffering.create(apiclient=self.apiClient, NetworkOfferingFactory=snatOfferingFactory)
        self.assertTrue(snatOffering is not None, msg = "no network offering was created")
        self.assertEqual(snatOffering.name, snatOfferingFactory.name, msg="error in network offering factory creation")

    def test_defaultSGOfferingEnable(self):
        sgOfferingFactory = DefaultSharedNetworkOfferingWithSGServiceFactory()
        sgOffering = NetworkOffering.create(apiclient=self.apiClient, NetworkOfferingFactory=sgOfferingFactory)
        sgOffering.update(self.apiClient, state='Enabled')

    def tearDown(self):
        pass