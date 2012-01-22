# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Templates ISO
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from utils import *
from base import *
import urllib
from random import random
#Import System modules
import time

class Services:
    """Test ISO Services
    """

    def __init__(self):
        self.services = {
                      "iso_1":
                                {
                                 "displaytext": "Test ISO type 1",
                                 "name": "testISOType_1",
                                 "url": "http://iso.linuxquestions.org/download/504/1819/http/gd4.tuwien.ac.at/dsl-4.4.10.iso",
                                 # Source URL where ISO is located
                                 "zoneid": 1,
                                 "isextractable": True,
                                 "isfeatured": True,
                                 "ispublic": True,
                                 "ostypeid": 12,
                                 },
                        "iso_2":
                                {
                                 "displaytext": "Test ISO type 2",
                                 "name": "testISOType_2",
                                 "url": "http://iso.linuxquestions.org/download/504/1819/http/gd4.tuwien.ac.at/dsl-4.4.10.iso",
                                 # Source URL where ISO is located
                                 "zoneid": 1,
                                 "isextractable": True,
                                 "isfeatured": True,
                                 "ispublic": True,
                                 "ostypeid": 12,
                                 "mode": 'HTTP_DOWNLOAD',
                                 # Used in Extract template, value must be HTTP_DOWNLOAD
                                 "ostypeid": 12,
                                 },
                            "destzoneid": 2, # Copy ISO from one zone to another (Destination Zone)
                            "sourcezoneid": 1, # Copy ISO from one zone to another (Source Zone)
                            "isfeatured": True,
                            "ispublic": True,
                            "isextractable": True,
                            "bootable": True, # For edit template
                            "passwordenabled": True,
                            "ostypeid": 15,
                            "account": 'bhavin333', # Normal user, no admin rights
                            "domainid": 1,
                     }

class TestCreateIso(cloudstackTestCase):

    def setUp(self):
        self.services = Services().services
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:

            self.dbclient.close()
            #Clean up, terminate the created ISOs
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_01_create_iso(self):
        """Test create public & private ISO
        """

        # Validate the following:
        # 1. database (vm_template table) should be updated with newly created ISO
        # 2. UI should show the newly added ISO
        # 3. listIsos API should show the newly added ISO

        iso = Iso.create(self.apiclient, self.services["iso_2"])
        iso.download(self.apiclient)
        self.cleanup.append(iso)

        cmd = listIsos.listIsosCmd()
        cmd.id = iso.id
        list_iso_response = self.apiclient.listIsos(cmd)

        iso_response = list_iso_response[0]

        self.assertNotEqual(
                            len(list_iso_response),
                            0,
                            "Check template available in List ISOs"
                        )

        self.assertEqual(
                            iso_response.displaytext,
                            self.services["iso_2"]["displaytext"],
                            "Check display text of newly created ISO"
                        )
        self.assertEqual(
                            iso_response.name,
                            self.services["iso_2"]["name"],
                            "Check name of newly created ISO"
                        )
        self.assertEqual(
                            iso_response.zoneid,
                            self.services["iso_2"]["zoneid"],
                            "Check zone ID of newly created ISO"
                        )

        #Verify the database entry for ISO
        self.debug(
                   "select name, display_text from vm_template where id = %s and format='ISO';"
                   % iso.id
                   )
        qresultset = self.dbclient.execute(
                                           "select name, display_text from vm_template where id = %s and format='ISO';"
                                           % iso.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            self.services["iso_2"]["name"],
                            "Compare ISO name with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            self.services["iso_2"]["displaytext"],
                            "Compare ISO display text with database record"
                        )
        return

class TestISO(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = fetch_api_client()
        cls.iso_1 = Iso.create(cls.api_client, cls.services["iso_1"])
        cls.iso_1.download(cls.api_client)
        cls.iso_2 = Iso.create(cls.api_client, cls.services["iso_2"])
        cls.iso_2.download(cls.api_client)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            cls.iso_2.delete(cls.api_client)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created ISOs, VMs
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_02_edit_iso(self):
        """Test Edit ISO
        """

        # Validate the following:
        # 1. UI should show the edited values for ISO
        # 2. database (vm_template table) should have updated values

        #Generate random values for updating ISO name and Display text
        new_displayText = random_gen()
        new_name = random_gen()

        cmd = updateIso.updateIsoCmd()
        #Assign new values to attributes
        cmd.id = self.iso_1.id
        cmd.displaytext = new_displayText
        cmd.name = new_name
        cmd.bootable = self.services["bootable"]
        cmd.passwordenabled = self.services["passwordenabled"]
        cmd.ostypeid = self.services["ostypeid"]

        self.apiclient.updateIso(cmd)

        #Check whether attributes are updated in ISO using listIsos
        cmd = listIsos.listIsosCmd()
        cmd.id = self.iso_1.id
        list_iso_response = self.apiclient.listIsos(cmd)

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
                            iso_response.name,
                            new_name,
                            "Check name of updated ISO"
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

        #Verify database entry for updateIso
        self.debug(
                   "select name, display_text, bootable, guest_os_id from vm_template where id = %s and format='ISO';"
                   % self.iso_1.id
                   )
        qresultset = self.dbclient.execute(
                                           "select name, display_text, bootable, guest_os_id from vm_template where id = %s and format='ISO';"
                                           % self.iso_1.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            new_name,
                            "Compare ISO name with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            new_displayText,
                            "Compare ISO display text with database record"
                        )
        self.assertEqual(
                            qresult[2],
                            int(self.services["bootable"]),
                            "Compare template enable_password field with database record"
                        )

        self.assertEqual(
                            qresult[3],
                            self.services["ostypeid"],
                            "Compare template guest OS ID with database record"
                        )
        return

    def test_03_delete_iso(self):
        """Test delete ISO
        """

        # Validate the following:
        # 1. UI should not show the deleted ISP
        # 2. database (vm_template table) should not contain deleted ISO

        self.iso_1.delete(cls.api_client)

        #ListIsos to verify deleted ISO is properly deleted
        cmd = listIsos.listIsosCmd()
        cmd.id = self.iso_1.id
        list_iso_response = self.apiclient.listIsos(cmd)

        self.assertEqual(list_iso_response, None, "Check if ISO exists in ListIsos")

        #Verify whether database entry is deleted or not
        self.debug(
                   "select name, display_text from vm_template where id = %s and format='ISO';"
                   % self.iso_1.id
                   )
        qresultset = self.dbclient.execute(
                                           "select name, display_text from vm_template where id = %s and format='ISO';"
                                           % self.iso_1.id
                                           )

        self.assertEqual(
                                len(qresultset),
                                1,
                                "Check DB Query result set"
                            )
        return

    def test_04_extract_Iso(self):
        "Test for extract ISO"

        # Validate the following
        # 1. Admin should able  extract and download the ISO
        # 2. ListIsos should display all the public templates for all kind of users
        # 3 .ListIsos should not display the system templates

        cmd = extractIso.extractIsoCmd()
        cmd.id = self.iso_2.id
        cmd.mode = self.services["iso_2"]["mode"]
        cmd.zoneid = self.services["iso_2"]["zoneid"]
        list_extract_response = self.apiclient.extractIso(cmd)

        #Format URL to ASCII to retrieve response code
        formatted_url = urllib.unquote_plus(list_extract_response.url)
        url_response = urllib.urlopen(formatted_url)
        response_code = url_response.getcode()

        self.assertEqual(
                            list_extract_response.id,
                            self.iso_2.id,
                            "Check ID of the downloaded ISO"
                        )
        self.assertEqual(
                            list_extract_response.extractMode,
                            self.services["iso_2"]["mode"],
                            "Check mode of extraction"
                        )
        self.assertEqual(
                            list_extract_response.zoneid,
                            self.services["iso_2"]["zoneid"],
                            "Check zone ID of extraction"
                        )
        self.assertEqual(
                         response_code,
                         200,
                         "Check for a valid response of download URL"
                         )
        return

    def test_05_iso_permissions(self):
        """Update & Test for ISO permissions"""

        # validate the following
        # 1. listIsos returns valid permissions set for ISO
        # 2. permission changes should be reflected in vm_template table in database

        cmd = updateIsoPermissions.updateIsoPermissionsCmd()
        cmd.id = self.iso_2.id
        #Update ISO permissions
        cmd.isfeatured = self.services["isfeatured"]
        cmd.ispublic = self.services["ispublic"]
        cmd.isextractable = self.services["isextractable"]
        self.apiclient.updateIsoPermissions(cmd)

        #Verify ListIsos have updated permissions for the ISO for normal user
        cmd = listIsos.listIsosCmd()
        cmd.id = self.iso_2.id
        cmd.account = self.services["account"]
        cmd.domainid = self.services["domainid"]
        list_iso_response = self.apiclient.listIsos(cmd)

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

        #Verify database entry for updated ISO permissions
        self.debug(
                   "select public, featured, extractable from vm_template where id = %s and format='ISO';"
                   % self.iso_2.id
                   )
        qresultset = self.dbclient.execute(
                                           "select public, featured, extractable from vm_template where id = %s and format='ISO';"
                                           % self.iso_2.id
                                           )

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            int(self.services["ispublic"]),
                            "Compare ispublic permission with database record"
                        )

        self.assertEqual(
                            qresult[1],
                            int(self.services["isfeatured"]),
                            "Compare isfeatured permission with database record"
                        )
        self.assertEqual(
                            qresult[2],
                            int(self.services["isextractable"]),
                            "Compare extractable permission with database record"
                        )
        return

    def test_06_copy_iso(self):
        """Test for copy ISO from one zone to another"""

        #Validate the following
        #1. copy ISO should be successful and secondary storage should contain new copied ISO.

        cmd = copyIso.copyIsoCmd()
        cmd.id = self.iso_2.id
        cmd.destzoneid = self.services["destzoneid"]
        cmd.sourcezoneid = self.services["sourcezoneid"]
        self.apiclient.copyIso(cmd)

        #Verify ISO is copied to another zone using ListIsos
        cmd = listIsos.listIsosCmd()
        cmd.id = self.iso_2.id
        list_iso_response = self.apiclient.listIsos(cmd)

        iso_response = list_iso_response[0]

        self.assertNotEqual(
                            len(list_iso_response),
                            0,
                            "Check template extracted in List ISO"
                        )
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
        return
