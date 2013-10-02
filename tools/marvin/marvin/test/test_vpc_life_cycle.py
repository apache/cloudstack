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

from marvin.cloudstackTestCase import *
from marvin.factory.data.account import *
from marvin.factory.data.vm import *
from should_dsl import *
from marvin.util import *
from nose.plugins.attrib import attr


class TestVpcLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(TestVpcLifeCycle, cls).getClsTestClient().getApiClient()
        cls.zoneid = get_zone(cls.apiclient).id
        cls.templateid = get_template(cls.apiclient).id,
        cls.serviceofferingid = get_service_offering(cls.apiclient).id,
        cls.account = UserAccount(
            apiclient=cls.apiclient
        )

    @attr(tags='debug')
    def test_deployvm(self):
        vm = VpcVirtualMachine(
            apiclient=self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zoneid,
            templateid=self.templateid,
            serviceofferingid=self.serviceofferingid
        )
        vm.state | should | equal_to('Running')

    def test_stopvm(self):
        vm = VpcVirtualMachine(
            apiclient=self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zoneid,
            templateid=self.templateid,
            serviceofferingid=self.serviceofferingid
        )
        vm.stop()
        vm.state | should | equal_to('Stopped')

    def test_startvm(self):
        vm = VpcVirtualMachine(
            apiclient=self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zoneid,
            templateid=self.templateid,
            serviceofferingid=self.serviceofferingid
        )
        vm.stop()
        vm.start()
        vm.state | should | equal_to('Running')

    def test_rebootvm(self):
        vm = VpcVirtualMachine(
            apiclient=self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zoneid,
            templateid=self.templateid,
            serviceofferingid=self.serviceofferingid
        )
        vm.reboot()
        vm.state | should | equal_to('Running')

    def test_destroyvm(self):
        vm = VpcVirtualMachine(
            apiclient=self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zoneid,
            templateid=self.templateid,
            serviceofferingid=self.serviceofferingid
        )
        vm.destroy()
        vm.state | should | equal_to('Destroyed')

    @classmethod
    def tearDownClass(cls):
        cls.account.delete()