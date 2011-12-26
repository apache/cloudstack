# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#
"""Data for the BVT tests 
"""

TEST_VM_LIFE_CYCLE_SERVICES = {
                                "small" :   
                                    {
                                        "template" : 7,
                                        "zoneid" : 2,
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
                                        "zoneid" : 2,
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
                            "diskdevice" : "/dev/sda",
                            "offerings" : 1,
                            "template"  : 7,
                            "zoneid" : 2,
                            "diskoffering" : 3,
                            "username" : "root",
                            "password" : "password",
                            "ssh_port" : 22,
                            "diskname" : "TestDiskServ",
                            "server2": {
                                        "offerings" : 1,
                                        "template"  : 7,
                                        "zoneid" : 2,
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                }
                         }

TEST_VOLUME_SERVICES = {
                            "offerings" : 1,
                            "volumeoffering" : 3,
                            "diskname" : "TestDiskServ",
                            "template"  : 7,
                            "zoneid" : 2,
                            "username" : "root",
                            "password" : "password",
                            "ssh_port" : 22,
                        }
