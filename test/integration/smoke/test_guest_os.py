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
#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (VirtualMachine,
                             Account,
                             GuestOSCategory,
                             GuestOS,
                             GuestOsMapping,
                             NetworkOffering,
                             Network)
from marvin.lib.common import get_test_template, get_zone, list_virtual_machines
from marvin.lib.utils import (validateList, cleanup_resources)
from nose.plugins.attrib import attr
from marvin.codes import PASS,FAIL

class Services:
    def __init__(self):
        self.services = {
        }

class TestGuestOS(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestGuestOS, cls)
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

        #build cleanup list
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_CRUD_operations_guest_OS(self):
        """Test register, list, update operations on guest OS
            1. Add a guest OS
            2. List the guest OS
            3. Delete the added guest OS
        """
        list_os_categories = GuestOSCategory.list(self.apiclient, name="CentOS", listall=True)
        self.assertNotEqual(
            len(list_os_categories),
            0,
            "List OS categories was empty"
        )
        os_category = list_os_categories[0]

        self.guestos1 = GuestOS.add(
            self.apiclient,
            osdisplayname="testCentOS",
            oscategoryid=os_category.id
        )
        list_guestos = GuestOS.list(self.apiclient, id=self.guestos1.id, listall=True)
        self.assertNotEqual(
            len(list_guestos),
            0,
            "List guest OS was empty"
        )
        guestos = list_guestos[0]
        self.assertEqual(
            guestos.id,
            self.guestos1.id,
            "Guest os ids do not match"
        )

        GuestOS.remove(
            self.apiclient,
            id=self.guestos1.id
        )
