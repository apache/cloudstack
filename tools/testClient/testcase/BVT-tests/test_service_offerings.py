# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#

""" BVT tests for Service offerings"""

#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *

services = TEST_SERVICE_OFFERING

class TestServiceOfferings(cloudstackTestCase):
    
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
    
    @classmethod
    def setUpClass(cls):
        
        cls.api_client = fetch_api_client()
        
        cmd = createServiceOffering.createServiceOfferingCmd()
        cmd.cpunumber = services["off_1"]["cpunumber"]
        cmd.cpuspeed  = services["off_1"]["cpuspeed"]
        cmd.displaytext = services["off_1"]["displaytext"]
        cmd.memory  = services["off_1"]["memory"]
        cmd.name = services["off_1"]["name"]
        
        cls.small_service_offering = cls.api_client.createServiceOffering(cmd)
        
        cmd = createServiceOffering.createServiceOfferingCmd()
        cmd.cpunumber = services["off_2"]["cpunumber"]
        cmd.cpuspeed  = services["off_2"]["cpuspeed"]
        cmd.displaytext = services["off_2"]["displaytext"]
        cmd.memory  = services["off_2"]["memory"]
        cmd.name = services["off_2"]["name"]
        
        cls.medium_service_offering = cls.api_client.createServiceOffering(cmd)
        return 
    
    @classmethod
    def tearDownClass(cls):
        
        cls.api_client = fetch_api_client()
        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        cmd.id = cls.small_service_offering.id
        cls.api_client.deleteServiceOffering(cmd)
        return 
    
    def test_01_create_service_offering(self):
        """Test to create service offering"""
               
        # Validate the following:
        # 1. createServiceOfferings should return a valid information for newly created offering
        # 2. The Cloud Database contains the valid information
            
        cmd = createServiceOffering.createServiceOfferingCmd()
        #Add the required parameters for creating new service offering
        cmd.cpunumber = services["off_1"]["cpunumber"]
        cmd.cpuspeed  = services["off_1"]["cpuspeed"]
        cmd.displaytext = services["off_1"]["displaytext"]
        cmd.memory  = services["off_1"]["memory"]
        cmd.name = services["off_1"]["name"]
        
        self.tmp_service_offering = self.apiclient.createServiceOffering(cmd)
        
        cmd = listServiceOfferings.listServiceOfferingsCmd()
        cmd.name = services["off_1"]["name"]
        list_service_response = self.apiclient.listServiceOfferings(cmd)
        
        self.assertNotEqual(
                            len(list_service_response),
                            0,
                            "Check Service offering is created"
                        )
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
        
        qresultset = self.dbclient.execute(
                                           "select cpu, speed, ram_size from service_offering where id = %s;" 
                                           % self.tmp_service_offering.id
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
        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        cmd.id = self.tmp_service_offering.id
        self.api_client.deleteServiceOffering(cmd)
        return
    
    def test_02_edit_service_offering(self):
        """Test to update existing service offering"""
        
        # Validate the following:
        # 1. updateServiceOffering should return a valid information for newly created offering
        random_displaytext = random_gen()
        random_name = random_gen()        
        cmd = updateServiceOffering.updateServiceOfferingCmd()
        cmd.id= self.small_service_offering.id
        cmd.displaytext = random_displaytext
        cmd.name = random_name
        self.apiclient.updateServiceOffering(cmd)
        
        cmd = listServiceOfferings.listServiceOfferingsCmd()
        cmd.id = self.small_service_offering.id
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
        
        qresultset = self.dbclient.execute(
                                           "select id, display_text, name from disk_offering where type='Service' and id = %s;" 
                                           % self.small_service_offering.id
                                           )
        
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]
       
        self.assertEqual(
                            qresult[0],
                            self.small_service_offering.id,
                            "Check service offering ID in the database"
                        )
        self.assertEqual(
                            qresult[1],
                            services["off_1"]["displaytext"],
                            "Check service offering ID in the database"
                        )
        self.assertEqual(
                            qresult[2],
                            services["off_1"]["name"],
                            "Check service offering ID in the database"
                        )
        
        return

    def test_03_delete_service_offering(self):
        """Test to delete service offering"""
            
        # Validate the following:
        # 1. deleteServiceOffering should return a valid information for newly created offering
        
        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        #Add the required parameters required to call for API
        cmd.id = self.medium_service_offering.id
        self.apiclient.deleteServiceOffering(cmd)
        
        cmd = listServiceOfferings.listServiceOfferingsCmd()
        cmd.id = self.medium_service_offering.id
        list_service_response = self.apiclient.listServiceOfferings(cmd) 

        self.assertEqual(
                            list_service_response, 
                            None, 
                            "Check if service offering exists in listDiskOfferings"
                        )

            
        qresultset = self.dbclient.execute(
                                           "select id from service_offering where id = %s;" 
                                           % self.medium_service_offering.id
                                           )       
        self.assertEqual(
                            len(qresultset),
                            1,
                            "Check DB Query result set"
                            )              
        return
       
    def tearDown(self):
        self.dbclient.close()
        return
