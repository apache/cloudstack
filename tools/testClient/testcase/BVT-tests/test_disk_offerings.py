# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

""" BVT tests for Disk offerings"""

#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
from utils import *
from base import *

services = TEST_DISK_OFFERING

class TestCreateDiskOffering(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def test_01_create_disk_offering(self):
        """Test to create disk offering"""

        # Validate the following:
        # 1. createDiskOfferings should return a valid information for newly created offering
        # 2. The Cloud Database contains the valid information

        disk_offering = DiskOffering.create(self.apiclient, services["off_1"])
        self.cleanup.append(disk_offering)

        cmd = listDiskOfferings.listDiskOfferingsCmd()
        cmd.id = disk_offering.id
        list_disk_response = self.apiclient.listDiskOfferings(cmd)

        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check Disk offering is created"
                        )
        disk_response = list_disk_response[0]

        self.assertEqual(
                            disk_response.displaytext,
                            services["off_1"]["displaytext"],
                            "Check server id in createServiceOffering"
                        )
        self.assertEqual(
                            disk_response.name,
                            services["off_1"]["name"],
                            "Check name in createServiceOffering"
                        )

        #Verify the database entries for new disk offering
        qresultset = self.dbclient.execute(
                                           "select display_text, id from disk_offering where id = %s;"
                                           %disk_offering.id
                                           )

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            services["off_1"]["displaytext"],
                            "Compare display text with database record"
                        )
        self.assertEqual(
                            qresult[1],
                            disk_offering.id,
                            "Check ID in the database"
                        )
        return


class TestDiskOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):

        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.disk_offering_1 = DiskOffering.create(cls.api_client, services["off_1"])
        cls.disk_offering_2 = DiskOffering.create(cls.api_client, services["off_2"])
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            cls.disk_offering_1.delete(cls.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def test_02_edit_disk_offering(self):
        """Test to update existing disk offering"""

        # Validate the following:
        # 1. updateDiskOffering should return a valid information for newly created offering

        #Generate new name & displaytext from random data
        random_displaytext = random_gen()
        random_name = random_gen()

        cmd = updateDiskOffering.updateDiskOfferingCmd()
        cmd.id= self.disk_offering_1.id
        cmd.displaytext = random_displaytext
        cmd.name = random_name

        self.apiclient.updateDiskOffering(cmd)

        cmd = listDiskOfferings.listDiskOfferingsCmd()
        cmd.id = self.disk_offering_1.id
        list_disk_response = self.apiclient.listDiskOfferings(cmd)

        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check disk offering is updated"
                        )

        disk_response = list_disk_response[0]

        self.assertEqual(
                            disk_response.displaytext,
                            random_displaytext,
                            "Check service displaytext in updateServiceOffering"
                        )
        self.assertEqual(
                            disk_response.name,
                            random_name,
                            "Check service name in updateServiceOffering"
                        )

        #Verify database entries for updated disk offerings
        qresultset = self.dbclient.execute(
                                           "select display_text, id from disk_offering where id = %s;"
                                           %self.disk_offering_1.id
                                           )

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            random_displaytext,
                            "Compare displaytext with database record"
                        )
        self.assertEqual(
                            qresult[1],
                            self.disk_offering_1.id,
                            "Check name in the database"
                        )

        return

    def test_03_delete_disk_offering(self):
        """Test to delete disk offering"""

        # Validate the following:
        # 1. deleteDiskOffering should return a valid information for newly created offering


        cmd = deleteDiskOffering.deleteDiskOfferingCmd()
        cmd.id = self.disk_offering_2.id
        self.apiclient.deleteDiskOffering(cmd)

        cmd = listDiskOfferings.listDiskOfferingsCmd()
        cmd.id = self.disk_offering_2.id
        list_disk_response = self.apiclient.listDiskOfferings(cmd)

        self.assertEqual(
                            list_disk_response,
                            None,
                            "Check if disk offering exists in listDiskOfferings"
                        )

        #Verify database entry for deleted disk offering
        qresultset = self.dbclient.execute(
                                           "select display_text, name from disk_offering where id = %s;"
                                           % str(self.disk_offering_2.id)
                                           )

        self.assertEqual(
                            len(qresultset),
                            1,
                            "Check DB Query result set"
                            )

        return

