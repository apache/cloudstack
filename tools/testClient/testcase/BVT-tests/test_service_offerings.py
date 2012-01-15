# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

""" BVT tests for Service offerings"""

#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
from utils import *
from base import *

services = TEST_SERVICE_OFFERING

class TestCreateServiceOffering(cloudstackTestCase):

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

    def test_01_create_service_offering(self):
        """Test to create service offering"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information for newly created offering
        # 2. The Cloud Database contains the valid information

        service_offering = ServiceOffering.create(self.apiclient, services["off_1"])
        self.cleanup.append(service_offering)

        cmd = listServiceOfferings.listServiceOfferingsCmd()
        cmd.id = service_offering.id
        list_service_response = self.apiclient.listServiceOfferings(cmd)

        self.assertNotEqual(
                            len(list_service_response),
                            0,
                            "Check Service offering is created"
                        )
        service_response = list_service_response[0]

        self.assertEqual(
                            list_service_response[0].cpunumber,
                            services["off_1"]["cpunumber"],
                            "Check server id in createServiceOffering"
                        )
        self.assertEqual(
                            list_service_response[0].cpuspeed,
                            services["off_1"]["cpuspeed"],
                            "Check cpuspeed in createServiceOffering"
                        )
        self.assertEqual(
                            list_service_response[0].displaytext,
                            services["off_1"]["displaytext"],
                            "Check server displaytext in createServiceOfferings"
                        )
        self.assertEqual(
                            list_service_response[0].memory,
                            services["off_1"]["memory"],
                            "Check memory in createServiceOffering"
                        )
        self.assertEqual(
                            list_service_response[0].name,
                            services["off_1"]["name"],
                            "Check name in createServiceOffering"
                        )
        #Verify the service offerings with database records
        qresultset = self.dbclient.execute(
                                           "select cpu, speed, ram_size from service_offering where id = %s;"
                                           % service_offering.id
                                           )

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            services["off_1"]["cpunumber"],
                            "Check number of CPUs allocated to service offering in the database"
                        )
        self.assertEqual(
                            qresult[1],
                            services["off_1"]["cpuspeed"],
                            "Check number of CPUs allocated to service offering in the database"
                        )
        self.assertEqual(
                            qresult[2],
                            services["off_1"]["memory"],
                            "Check number of CPUs allocated to service offering in the database"
                        )
        return


class TestServiceOfferings(cloudstackTestCase):

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
        cls.service_offering_1 = ServiceOffering.create(cls.api_client, services["off_1"])
        cls.service_offering_2 = ServiceOffering.create(cls.api_client, services["off_2"])
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = fetch_api_client()
            cls.service_offering_1.delete(cls.api_client)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def test_02_edit_service_offering(self):
        """Test to update existing service offering"""

        # Validate the following:
        # 1. updateServiceOffering should return a valid information for newly created offering

        #Generate new name & displaytext from random data
        random_displaytext = random_gen()
        random_name = random_gen()


        cmd = updateServiceOffering.updateServiceOfferingCmd()
        #Add parameters for API call
        cmd.id= self.service_offering_1.id
        cmd.displaytext = random_displaytext
        cmd.name = random_name
        self.apiclient.updateServiceOffering(cmd)

        cmd = listServiceOfferings.listServiceOfferingsCmd()
        cmd.id = self.service_offering_1.id
        list_service_response = self.apiclient.listServiceOfferings(cmd)

        self.assertNotEqual(
                            len(list_service_response),
                            0,
                            "Check Service offering is updated"
                        )

        self.assertEqual(
                            list_service_response[0].displaytext,
                            random_displaytext,
                            "Check server displaytext in updateServiceOffering"
                        )
        self.assertEqual(
                            list_service_response[0].name,
                            random_name,
                            "Check server name in updateServiceOffering"
                        )

        #Verify updated values in database
        qresultset = self.dbclient.execute(
                                           "select id, display_text, name from disk_offering where type='Service' and id = %s;"
                                           % self.service_offering_1.id
                                           )

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            self.service_offering_1.id,
                            "Check service offering ID in the database"
                        )
        self.assertEqual(
                            qresult[1],
                            random_displaytext,
                            "Check service offering ID in the database"
                        )
        self.assertEqual(
                            qresult[2],
                            random_name,
                            "Check service offering ID in the database"
                        )

        return

    def test_03_delete_service_offering(self):
        """Test to delete service offering"""

        # Validate the following:
        # 1. deleteServiceOffering should return a valid information for newly created offering

        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        #Add the required parameters required to call for API
        cmd.id = self.service_offering_2.id
        self.apiclient.deleteServiceOffering(cmd)

        cmd = listServiceOfferings.listServiceOfferingsCmd()
        cmd.id = self.service_offering_2.id
        list_service_response = self.apiclient.listServiceOfferings(cmd)

        self.assertEqual(
                            list_service_response,
                            None,
                            "Check if service offering exists in listDiskOfferings"
                        )

        #Verify database records for deleted service offerings
        qresultset = self.dbclient.execute(
                                           "select id from service_offering where id = %s;"
                                           % self.service_offering_2.id
                                           )
        self.assertEqual(
                            len(qresultset),
                            1,
                            "Check DB Query result set"
                            )
        return

