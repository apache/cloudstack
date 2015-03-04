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
""" BVT tests for Templates ISO
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.cloudstackAPI import listZones, updateIso, extractIso, updateIsoPermissions, copyIso, deleteIso
from marvin.lib.utils import cleanup_resources, random_gen
from marvin.lib.base import Account, Iso
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_isos,
                               list_os_types)
from nose.plugins.attrib import attr
import urllib
# Import System modules
import time

_multiprocess_shared_ = True


class TestCreateIso(cloudstackTestCase):
    # TODO: SIMENH: check the existence of registered of ISO in secondary deploy a VM with registered ISO. can be added \
    # as another test

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.services['mode'] = self.zone.networktype
        self.services["domainid"] = self.domain.id
        self.services["iso2"]["zoneid"] = self.zone.id

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        # Finding the OsTypeId from Ostype
        ostypes = list_os_types(
            self.apiclient,
            description=self.services["ostype"]
        )
        if not isinstance(ostypes, list):
            raise unittest.SkipTest("OSTypeId for given description not found")

        self.services["iso1"]["ostypeid"] = ostypes[0].id
        self.services["iso2"]["ostypeid"] = ostypes[0].id
        self.services["ostypeid"] = ostypes[0].id

        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created ISOs
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns"],
        required_hardware="false")
    def test_01_create_iso(self):
        """Test create public & private ISO
        """

        # Validate the following:
        # 1. database (vm_template table) should be
        #    updated with newly created ISO
        # 2. UI should show the newly added ISO
        # 3. listIsos API should show the newly added ISO

        iso = Iso.create(
            self.apiclient,
            self.services["iso2"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("ISO created with ID: %s" % iso.id)

        try:
            iso.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s"
                      % (iso.id, e))

        list_iso_response = list_isos(
            self.apiclient,
            id=iso.id
        )
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_iso_response),
            0,
            "Check template available in List ISOs"
        )
        iso_response = list_iso_response[0]

        self.assertEqual(
            iso_response.displaytext,
            self.services["iso2"]["displaytext"],
            "Check display text of newly created ISO"
        )
        self.assertEqual(
            iso_response.zoneid,
            self.services["iso2"]["zoneid"],
            "Check zone ID of newly created ISO"
        )
        return


class TestISO(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestISO, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

        cls.services["domainid"] = cls.domain.id
        cls.services["iso1"]["zoneid"] = cls.zone.id
        cls.services["iso2"]["zoneid"] = cls.zone.id
        cls.services["sourcezoneid"] = cls.zone.id
        # populate second zone id for iso copy
        cmd = listZones.listZonesCmd()
        cls.zones = cls.apiclient.listZones(cmd)
        if not isinstance(cls.zones, list):
            raise Exception("Failed to find zones.")

        # Create an account, ISOs etc.
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        # Finding the OsTypeId from Ostype
        ostypes = list_os_types(
            cls.apiclient,
            description=cls.services["ostype"]
        )
        if not isinstance(ostypes, list):
            raise unittest.SkipTest("OSTypeId for given description not found")

        cls.services["iso1"]["ostypeid"] = ostypes[0].id
        cls.services["iso2"]["ostypeid"] = ostypes[0].id
        cls.services["ostypeid"] = ostypes[0].id

        cls.iso_1 = Iso.create(
            cls.apiclient,
            cls.services["iso1"],
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        try:
            cls.iso_1.download(cls.apiclient)
        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (cls.iso_1.id, e))

        cls.iso_2 = Iso.create(
            cls.apiclient,
            cls.services["iso2"],
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        try:
            cls.iso_2.download(cls.apiclient)
        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (cls.iso_2.id, e))

        cls._cleanup = [cls.account]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestISO,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created ISOs, VMs
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def get_iso_details(self, isoname):

        # ListIsos to list default ISOS (VM and xen tools)
        list_default_iso_response = list_isos(
            self.apiclient,
            name=isoname,
            isready="true"
        )
        self.assertEqual(
            list_default_iso_response,
            None,
            "Check if ISO exists in ListIsos"
        )

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke"],
        required_hardware="false")
    def test_02_edit_iso(self):
        """Test Edit ISO
        """

        # Validate the following:
        # 1. UI should show the edited values for ISO
        # 2. database (vm_template table) should have updated values

        # Generate random values for updating ISO name and Display text
        new_displayText = random_gen()
        new_name = random_gen()

        self.debug("Updating ISO permissions for ISO: %s" % self.iso_1.id)

        cmd = updateIso.updateIsoCmd()
        # Assign new values to attributes
        cmd.id = self.iso_1.id
        cmd.displaytext = new_displayText
        cmd.name = new_name
        cmd.bootable = self.services["bootable"]
        cmd.passwordenabled = self.services["passwordenabled"]

        self.apiclient.updateIso(cmd)

        # Check whether attributes are updated in ISO using listIsos
        list_iso_response = list_isos(
            self.apiclient,
            id=self.iso_1.id
        )
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_iso_response),
            0,
            "Check template available in List ISOs"
        )

        iso_response = list_iso_response[0]
        self.assertEqual(
            iso_response.displaytext,
            new_displayText,
            "Check display text of updated ISO"
        )
        self.assertEqual(
            iso_response.bootable,
            self.services["bootable"],
            "Check if image is bootable of updated ISO"
        )

        self.assertEqual(
            iso_response.ostypeid,
            self.services["ostypeid"],
            "Check OSTypeID of updated ISO"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns"],
        required_hardware="false")
    def test_03_delete_iso(self):
        """Test delete ISO
        """

        # Validate the following:
        # 1. UI should not show the deleted ISP
        # 2. database (vm_template table) should not contain deleted ISO

        self.debug("Deleting ISO with ID: %s" % self.iso_1.id)
        self.iso_1.delete(self.apiclient)

        # Sleep to ensure that ISO state is reflected in other calls
        time.sleep(self.services["sleep"])

        # ListIsos to verify deleted ISO is properly deleted
        list_iso_response = list_isos(
            self.apiclient,
            id=self.iso_1.id
        )

        self.assertEqual(
            list_iso_response,
            None,
            "Check if ISO exists in ListIsos"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns"],
        required_hardware="true")
    def test_04_extract_Iso(self):
        "Test for extract ISO"

        # Validate the following
        # 1. Admin should able  extract and download the ISO
        # 2. ListIsos should display all the public templates
        #    for all kind of users
        # 3 .ListIsos should not display the system templates

        self.debug("Extracting ISO with ID: %s" % self.iso_2.id)

        cmd = extractIso.extractIsoCmd()
        cmd.id = self.iso_2.id
        cmd.mode = self.services["iso2"]["mode"]
        cmd.zoneid = self.services["iso2"]["zoneid"]
        list_extract_response = self.apiclient.extractIso(cmd)

        try:
            # Format URL to ASCII to retrieve response code
            formatted_url = urllib.unquote_plus(list_extract_response.url)
            url_response = urllib.urlopen(formatted_url)
            response_code = url_response.getcode()
        except Exception:
            self.fail(
                "Extract ISO Failed with invalid URL %s (ISO id: %s)"
                % (formatted_url, self.iso_2.id)
            )

        self.assertEqual(
            list_extract_response.id,
            self.iso_2.id,
            "Check ID of the downloaded ISO"
        )
        self.assertEqual(
            list_extract_response.extractMode,
            self.services["iso2"]["mode"],
            "Check mode of extraction"
        )
        self.assertEqual(
            list_extract_response.zoneid,
            self.services["iso2"]["zoneid"],
            "Check zone ID of extraction"
        )
        self.assertEqual(
            response_code,
            200,
            "Check for a valid response of download URL"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke",
            "selfservice"])
    def test_05_iso_permissions(self):
        """Update & Test for ISO permissions"""

        # validate the following
        # 1. listIsos returns valid permissions set for ISO
        # 2. permission changes should be reflected in vm_template
        #    table in database

        self.debug("Updating permissions for ISO: %s" % self.iso_2.id)

        cmd = updateIsoPermissions.updateIsoPermissionsCmd()
        cmd.id = self.iso_2.id
        # Update ISO permissions
        cmd.isfeatured = self.services["isfeatured"]
        cmd.ispublic = self.services["ispublic"]
        cmd.isextractable = self.services["isextractable"]
        self.apiclient.updateIsoPermissions(cmd)

        # Verify ListIsos have updated permissions for the ISO for normal user
        list_iso_response = list_isos(
            self.apiclient,
            id=self.iso_2.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )

        iso_response = list_iso_response[0]

        self.assertEqual(
            iso_response.id,
            self.iso_2.id,
            "Check ISO ID"
        )
        self.assertEqual(
            iso_response.ispublic,
            self.services["ispublic"],
            "Check ispublic permission of ISO"
        )

        self.assertEqual(
            iso_response.isfeatured,
            self.services["isfeatured"],
            "Check isfeatured permission of ISO"
        )
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns",
            "smoke",
            "multizone",
            "provisioning"])
    def test_06_copy_iso(self):
        """Test for copy ISO from one zone to another"""

        # Validate the following
        # 1. copy ISO should be successful and secondary storage
        #   should contain new copied ISO.
        if len(self.zones) <= 1:
            self.skipTest(
                "Not enough zones available to perform copy template")

        self.services["destzoneid"] = filter(
            lambda z: z.id != self.zone.id,
            self.zones)[0].id

        self.debug("Copy ISO from %s to %s" % (
            self.zone.id,
            self.services["destzoneid"]
        ))

        cmd = copyIso.copyIsoCmd()
        cmd.id = self.iso_2.id
        cmd.destzoneid = self.services["destzoneid"]
        cmd.sourcezoneid = self.zone.id
        self.apiclient.copyIso(cmd)

        # Verify ISO is copied to another zone using ListIsos
        list_iso_response = list_isos(
            self.apiclient,
            id=self.iso_2.id,
            zoneid=self.services["destzoneid"]
        )
        self.assertEqual(
            isinstance(list_iso_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_iso_response),
            0,
            "Check template extracted in List ISO"
        )
        iso_response = list_iso_response[0]

        self.assertEqual(
            iso_response.id,
            self.iso_2.id,
            "Check ID of the downloaded ISO"
        )
        self.assertEqual(
            iso_response.zoneid,
            self.services["destzoneid"],
            "Check zone ID of the copied ISO"
        )

        self.debug("Cleanup copied ISO: %s" % iso_response.id)
        # Cleanup- Delete the copied ISO
        timeout = self.services["timeout"]
        while True:
            time.sleep(self.services["sleep"])
            list_iso_response = list_isos(
                self.apiclient,
                id=self.iso_2.id,
                zoneid=self.services["destzoneid"]
            )
            self.assertEqual(
                isinstance(list_iso_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_iso_response),
                0,
                "Check template extracted in List ISO"
            )

            iso_response = list_iso_response[0]
            if iso_response.isready:
                break

            if timeout == 0:
                raise Exception(
                    "Failed to download copied iso(ID: %s)" % iso_response.id)

            timeout = timeout - 1
        cmd = deleteIso.deleteIsoCmd()
        cmd.id = iso_response.id
        cmd.zoneid = self.services["destzoneid"]
        self.apiclient.deleteIso(cmd)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "eip",
            "sg",
            "advancedns"],
        required_hardware="false")
    def test_07_list_default_iso(self):
        """Test delete ISO
        """

        # Validate the following:
        # list ISO should list default ISOS (VM and xen tools)

        # ListIsos to list default ISOS (VM and xen tools)
        self.get_iso_details("xs-tools.iso")
        self.get_iso_details("vmware-tools.iso")
        return
