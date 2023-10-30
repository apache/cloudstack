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
import unittest

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import listHosts
from marvin.cloudstackException import CloudstackAPIException
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

        cls.hypervisor = cls.get_hypervisor_type()

    @classmethod
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

        #build cleanup list
        self.cleanup = []

    @classmethod
    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @classmethod
    def get_hypervisor_type(cls):

        """Return the hypervisor available in setup"""

        cmd = listHosts.listHostsCmd()
        cmd.type = 'Routing'
        cmd.listall = True
        hosts = cls.api_client.listHosts(cmd)
        hosts_list_validation_result = validateList(hosts)
        assert hosts_list_validation_result[0] == PASS, "host list validation failed"
        return hosts_list_validation_result[1]


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_CRUD_operations_guest_OS(self):
        """Test add, list, update operations on guest OS
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

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_CRUD_operations_guest_OS_mapping(self):
        """Test add, list, update operations on guest OS mapping
            1. Add a guest OS
            2. Add a guest OS mapping
            3. Delete the added guest OS and mappings
        """
        list_os_categories = GuestOSCategory.list(self.apiclient, name="CentOS", listall=True)
        os_category = list_os_categories[0]
        self.guestos1 = GuestOS.add(
            self.apiclient,
            osdisplayname="testCentOS",
            oscategoryid=os_category.id
        )

        if self.hypervisor.hypervisor.lower() not in ["xenserver", "vmware"]:
            raise unittest.SkipTest("OS name check with hypervisor is supported only on XenServer and VMware")

        self.guestosmapping1 = GuestOsMapping.add(
            self.apiclient,
            ostypeid=self.guestos1.id,
            hypervisor=self.hypervisor.hypervisor,
            hypervisorversion=self.hypervisor.hypervisorversion,
            osnameforhypervisor="testOSMappingName"
        )

        list_guestos_mapping = GuestOsMapping.list(self.apiclient, id=self.guestosmapping1.id, listall=True)
        self.assertNotEqual(
            len(list_guestos_mapping),
            0,
            "List guest OS mapping was empty"
        )
        guestosmapping = list_guestos_mapping[0]
        self.assertEqual(
            guestosmapping.id,
            self.guestosmapping1.id,
            "Guest os mapping ids do not match"
        )

        GuestOsMapping.remove(
            self.apiclient,
            id=self.guestosmapping1.id
        )

        GuestOS.remove(
            self.apiclient,
            id=self.guestos1.id
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_guest_OS_mapping_check_with_hypervisor(self):
        """Test add, list, update operations on guest OS mapping
            1. Add a guest OS
            2. Add a guest OS mapping with osmappingcheckenabled true
            3. Delete the added guest OS and mappings
        """
        list_os_categories = GuestOSCategory.list(self.apiclient, name="CentOS", listall=True)
        os_category = list_os_categories[0]
        self.guestos1 = GuestOS.add(
            self.apiclient,
            osdisplayname="testOSname1",
            oscategoryid=os_category.id
        )

        if self.hypervisor.hypervisor.lower() not in ["xenserver", "vmware"]:
            raise unittest.SkipTest("OS name check with hypervisor is supported only on XenServer and VMware")

        if self.hypervisor.hypervisor.lower() == "xenserver":
            testosname="Debian Squeeze 6.0 (32-bit)"
        else:
            testosname="debian4_64Guest"

        self.guestosmapping1 = GuestOsMapping.add(
            self.apiclient,
            ostypeid=self.guestos1.id,
            hypervisor=self.hypervisor.hypervisor,
            hypervisorversion=self.hypervisor.hypervisorversion,
            osnameforhypervisor=testosname,
            osmappingcheckenabled=True
        )

        list_guestos_mapping = GuestOsMapping.list(self.apiclient, id=self.guestosmapping1.id, listall=True)
        self.assertNotEqual(
            len(list_guestos_mapping),
            0,
            "List guest OS mapping was empty"
        )
        guestosmapping = list_guestos_mapping[0]
        self.assertEqual(
            guestosmapping.id,
            self.guestosmapping1.id,
            "Guest os mapping ids do not match"
        )

        GuestOsMapping.remove(
            self.apiclient,
            id=self.guestosmapping1.id
        )

        GuestOS.remove(
            self.apiclient,
            id=self.guestos1.id
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_guest_OS_mapping_check_with_hypervisor_failure(self):
        """Test add, list, update operations on guest OS mapping
            1. Add a guest OS
            2. Add a guest OS mapping with osmappingcheckenabled true
            3. Delete the added guest OS and mappings
        """
        list_os_categories = GuestOSCategory.list(self.apiclient, name="CentOS", listall=True)
        os_category = list_os_categories[0]
        self.guestos1 = GuestOS.add(
            self.apiclient,
            osdisplayname="testOSname2",
            oscategoryid=os_category.id
        )

        if self.hypervisor.hypervisor.lower() not in ["xenserver", "vmware"]:
            raise unittest.SkipTest("OS name check with hypervisor is supported only on XenServer and VMware")

        testosname = "incorrectOSname"

        try:
            self.guestosmapping1 = GuestOsMapping.add(
                self.apiclient,
                ostypeid=self.guestos1.id,
                hypervisor=self.hypervisor.hypervisor,
                hypervisorversion=self.hypervisor.hypervisorversion,
                osnameforhypervisor=testosname,
                osmappingcheckenabled=True
            )
            GuestOsMapping.remove(
                self.apiclient,
                id=self.guestosmapping1.id
            )
            self.fail("Since os mapping name is wrong, this API should fail")
        except CloudstackAPIException as e:
            self.debug("Addition guest OS mapping failed as expected %s " % e)
        GuestOS.remove(
            self.apiclient,
            id=self.guestos1.id
        )
        return
