# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#

""" BVT tests for Disk offerings"""

#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
from utils import *
from base import *

services = TEST_DISK_OFFERING

class TestDiskOfferings(cloudstackTestCase):
     
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
    
    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        
        cmd =createDiskOffering.createDiskOfferingCmd()
        cmd.displaytext = services["off_1"]["displaytext"]
        cmd.name = services["off_1"]["name"]
        cmd.disksize=services["off_1"]["disksize"]
    
        cls.small_disk_offering = cls.api_client.createDiskOffering(cmd)
        
        cmd = createDiskOffering.createDiskOfferingCmd()
        cmd.displaytext = services["off_2"]["displaytext"]
        cmd.name = services["off_2"]["name"]
        cmd.disksize=services["off_2"]["disksize"]
        
        cls.medium_disk_offering = cls.api_client.createDiskOffering(cmd)
        return 
    
    @classmethod
    def tearDownClass(cls):
        cls.api_client = fetch_api_client()
        
        cmd = deleteDiskOffering.deleteDiskOfferingCmd()
        cmd.id = cls.small_disk_offering.id
        cls.api_client.deleteDiskOffering(cmd)
        
        return 
        
    def test_01_create_disk_offering(self):
        """Test to create disk offering"""
               
        # Validate the following:
        # 1. createDiskOfferings should return a valid information for newly created offering
        # 2. The Cloud Database contains the valid information
        
        cmd = createDiskOffering.createDiskOfferingCmd()
        
        #Add the required parameters for creating new service offering
        cmd.displaytext = services["off_1"]["displaytext"]
        cmd.name = services["off_1"]["name"]
        cmd.disksize=services["off_1"]["disksize"]
        
        self.tmp_disk_offering = self.apiclient.createDiskOffering(cmd)
       
        cmd = listDiskOfferings.listDiskOfferingsCmd()
        cmd.name = services["off_1"]["name"]
        list_disk_response = self.apiclient.listDiskOfferings(cmd) 
        
        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check Disk offering is created"
                        )
        self.assertEqual(
                            list_disk_response[0].displaytext,
                            services["off_1"]["displaytext"],
                            "Check server id in createServiceOffering"
                        )
        self.assertEqual(
                            list_disk_response[0].name,
                            services["off_1"]["name"],
                            "Check name in createServiceOffering"
                        )
        
        qresultset = self.dbclient.execute(
                                           "select display_text, id from disk_offering where id = %s;" 
                                           %list_disk_response[0].id
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
                            list_disk_response[0].id,
                            "Check ID in the database"
                        )
        cmd = deleteDiskOffering.deleteDiskOfferingCmd()
        cmd.id = self.tmp_disk_offering.id
        self.apiclient.deleteDiskOffering(cmd)
        return
    
    def test_02_edit_disk_offering(self):
        """Test to update existing disk offering"""
     
        # Validate the following:
        # 1. updateDiskOffering should return a valid information for newly created offering
                
        cmd = updateDiskOffering.updateDiskOfferingCmd()
        cmd.id= self.small_disk_offering.id
        cmd.displaytext = services["off_1"]["displaytext"]
        cmd.name = services["off_1"]["name"]

        self.apiclient.updateDiskOffering(cmd)
        
        cmd = listDiskOfferings.listDiskOfferingsCmd()
        cmd.id = self.small_disk_offering.id
        list_disk_response = self.apiclient.listDiskOfferings(cmd)
        
        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check disk offering is updated"
                        )
    
        self.assertEqual(
                            list_disk_response[0].displaytext,
                            services["off_1"]["displaytext"],
                            "Check service displaytext in updateServiceOffering"
                        )
        self.assertEqual(
                            list_disk_response[0].name,
                            services["off_1"]["name"],
                            "Check service name in updateServiceOffering"
                        )
    
        qresultset = self.dbclient.execute(
                                           "select display_text, id from disk_offering where id = %s;" 
                                           %self.small_disk_offering.id
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
                            "Compare displaytext with database record"
                        )
        self.assertEqual(
                            qresult[1],
                            self.small_disk_offering.id,
                            "Check name in the database"
                        )

        return

    def test_03_delete_disk_offering(self):
        """Test to delete disk offering"""
            
        # Validate the following:
        # 1. deleteDiskOffering should return a valid information for newly created offering

        
        cmd = deleteDiskOffering.deleteDiskOfferingCmd()
        cmd.id = self.medium_disk_offering.id
        
        self.apiclient.deleteDiskOffering(cmd)
        
        cmd = listDiskOfferings.listDiskOfferingsCmd()
        cmd.id = self.medium_disk_offering.id
        list_disk_response = self.apiclient.listDiskOfferings(cmd) 
        
        self.assertEqual(
                            list_disk_response, 
                            None, 
                            "Check if disk offering exists in listDiskOfferings"
                        )
        qresultset = self.dbclient.execute(
                                           "select display_text, name from disk_offering where id = %s;" 
                                           % str(self.medium_disk_offering.id)
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
    
