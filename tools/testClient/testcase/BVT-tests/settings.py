# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#
"""Test Information Services 
"""

TEST_VM_LIFE_CYCLE_SERVICES = {
                                "small" :   
                                    {
                                        "template" : 7,
                                        "zoneid" : 1,
                                        "serviceoffering" : 1,
                                        "diskoffering" : 3,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
 
                                },

                                "medium" :   
                                    {
                                        "template" : 7,
                                        "zoneid" : 1,
                                        "serviceoffering" : 2,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
 
                                },
                            "service_offerings" :
                                { "small" :
                                    {
                                        "id": 1,
                                        "cpunumber" : 1,
                                        "cpuspeed" : 500,
                                        "memory" : 524288
                                    },
                                  "medium" :
                                    {
                                        "id" : 2,
                                        "cpunumber" : 1,
                                        "cpuspeed" : 1000,
                                        "memory" : 1048576
                                    }   
                                }   
                            }


TEST_SNAPSHOT_SERVICES = {
                                "server_with_disk" :   
                                    {
                                        "template" : 7,
                                        "zoneid" : 1,
                                        "serviceoffering" : 1,
                                        "diskoffering" : 3,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
 
                                },

                                "server_without_disk" :   
                                    {
                                        "template" : 7,
                                        "zoneid" : 1,
                                        "serviceoffering" : 2,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
 
                                },
                                "recurring_snapshot" :
                                    {
                                     "intervaltype" : 'HOURLY',
                                     "maxsnaps" : 8,
                                     "schedule" : 1,
                                     "timezone" : 'US/Arizona',
                                },
                                "template":
                                {
                                    "displaytext": "Test template from snapshot",
                                    "name" : "template_from_snapshot",
                                    "ostypeid": 12,
                                    "templatefilter": 'featured',
                                },
                                "small_instance":
                                {
                                 "zoneid": 1,
                                 "serviceofferingid": 2,
                                },
                            "diskdevice" : "/dev/sda",
                            "offerings" : 1,
                            "template"  : 7,
                            "zoneid" : 1,
                            "diskoffering" : 3,
                            "diskname" : "TestDiskServ",
                            "size" : 1, #GBs
                         }

TEST_VOLUME_SERVICES = {
                        "volume_offerings" : {
                            0: {
                                "offerings" : 1,
                                "volumeoffering" : 3,
                                "diskname" : "TestDiskServ",
                                "zoneid" : 1,
                                "diskofferingid": 3,
                            },

                            1: {
                                "offerings" : 1,
                                "volumeoffering" : 4,
                                "diskname" : "TestDiskServ",
                                "zoneid" : 1,
                                "diskofferingid": 3,
                            },
                            2: {
                                "offerings" : 1,
                                "volumeoffering" : 5,
                                "diskname" : "TestDiskServ",
                                "zoneid" : 1,
                                "diskofferingid": 3,
                            },
                        },
                            "customdiskofferingid":22,                              
                            "customdisksize" : 7, #GBs
                            "serviceoffering" : 1,
                            "template"  : 7,
                            "zoneid" : 1,
                            "username" : "root",
                            "password" : "password",
                            "ssh_port" : 22,
                            "diskname" : "TestDiskServ",
                        }

TEST_SERVICE_OFFERING = {
                         "off_1" :   
                                    {
                                        "id": 32,
                                        "name":"small_service_offering",
                                        "displaytext" : "Small service offering",
                                        "cpunumber":1,
                                        "cpuspeed": 200,
                                        "memory": 200,
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
 
                                },

                         "off_2" :   
                                    {
                                        "id":33,
                                        "name":"medium_service_offering",
                                        "displaytext" : "Medium service offering",
                                        "cpunumber":1,
                                        "cpuspeed": 200,
                                        "memory": 200,
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                }
                         }

TEST_DISK_OFFERING = {
                         "off_1" :   
                                    {
                                        "id": 31,
                                        "name":"small_disk_offering",
                                        "displaytext" : "Small disk offering",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "disksize": 1
                                },

                         "off_2" :   
                                    {
                                        "id":32,
                                        "name":"medium_disk_offering",
                                        "displaytext" : "Medium disk offering",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "disksize": 1
                                }
                         }
