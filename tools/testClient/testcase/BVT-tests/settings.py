# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#
"""Test Information Services
"""

TEST_VM_LIFE_CYCLE_SERVICES = {
                                "small" :
                                    {
                                        "template" : 256,
                                        "zoneid" : 1,
                                        "serviceoffering" : 27,
                                        "diskoffering" : 3,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "networkids":205,
                                        "hypervisor":'XenServer',
                                        "account":'admin',
                                        "domainid":1,
                                        "ipaddressid":3,
                                        "privateport":22,
                                        "publicport":22,
                                        "ipaddress":'69.41.185.229',
                                        "protocol":'TCP',
                                },
                                "medium" :
                                    {
                                        "template" : 256,
                                        "zoneid" : 1,
                                        "serviceoffering" : 27,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "networkids":205,
                                        "hypervisor":'XenServer',
                                        "account":'admin',
                                        "domainid":1,
                                        "ipaddressid":3,
                                        "privateport":22,
                                        "publicport":22,
                                        "ipaddress":'69.41.185.229',
                                        "protocol":'TCP',
                                },
                            "service_offerings" :
                                { "small" :
                                    {
                                        "id": 40,
                                        "cpunumber" : 1,
                                        "cpuspeed" : 500,
                                        "memory" : 524288
                                    },
                                  "medium" :
                                    {
                                        "id" : 39,
                                        "cpunumber" : 1,
                                        "cpuspeed" : 1000,
                                        "memory" : 1048576
                                    }
                                },
                            "iso":
                                {
                                 "displaytext" : "Test ISO type",
                                 "name": "testISO",
                                 "url": "http://iso.linuxquestions.org/download/504/1819/http/gd4.tuwien.ac.at/dsl-4.4.10.iso",
                                 "zoneid" : 1,
                                 "isextractable": True,
                                 "isfeatured":True,
                                 "ispublic": True,
                                 "ostypeid":12,
                                 "mode":'HTTP_DOWNLOAD',
                                 "ostypeid":12,
                                 },
                            "diskdevice": '/dev/xvdd',
                            "mount_dir": "/mnt/tmp",
                            "hostid": 1,
                            }

TEST_SNAPSHOT_SERVICES = {
                                "server_with_disk" :
                                    {
                                        "template" : 256,
                                        "zoneid" : 1,
                                        "serviceoffering" : 27,
                                        "diskoffering" : 3,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "networkids":205,
                                        "hypervisor":'XenServer',
                                        "account":'admin',
                                        "domainid":1,
                                        "ipaddressid":3,
                                        "privateport":22,
                                        "publicport":22,
                                        "ipaddress":'69.41.185.229',
                                        "protocol":'TCP',
                                },

                                "server_without_disk" :
                                    {
                                        "template" : 256,
                                        "zoneid" : 1,
                                        "serviceoffering" : 27,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "networkids":205,
                                        "hypervisor":'XenServer',
                                        "account":'admin',
                                        "domainid":1,
                                        "ipaddressid":3,
                                        "privateport":22,
                                        "publicport":22,
                                        "ipaddress":'69.41.185.229',
                                        "protocol":'TCP',
                                },

                                "recurring_snapshot" :
                                    {
                                     "intervaltype" : 'HOURLY',
                                     "maxsnaps" : 2,
                                     "schedule" : 1,
                                     "timezone" : 'US/Arizona',
                                },

                                "templates" :
                                {
                                    "displaytext": 'Test template snapshot',
                                    "name" : 'template_from_snapshot_3',
                                    "ostypeid" : 12,
                                    "templatefilter" : 'self',
                                },
                                "small_instance":
                                {
                                 "zoneid": 1,
                                 "serviceofferingid": 2,
                                },
                            "diskdevice" : "/dev/xvda",
                            "offerings" : 1,
                            "template"  : 256,
                            "zoneid" : 1,
                            "diskoffering" : 3,
                            "diskname" : "TestDiskServ",
                            "size" : 1, #GBs
                            "account":'admin',
                            "domainid":1,
                            "mount_dir": "/mnt/tmp",
                            "sub_dir":"test",
                            "sub_lvl_dir1": "test1",
                            "sub_lvl_dir2": "test2",
                            "random_data" : "random.data",

                         }

TEST_VOLUME_SERVICES = {
                        "volume_offerings" : {
                            0: {
                                "offerings" : 1,
                                "volumeoffering" : 3,
                                "diskname" : "TestDiskServ",
                                "zoneid" : 1,
                                "diskofferingid": 3,
                                "account":'admin',
                                "domainid":1,
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
                            "customdiskofferingid":41,
                            "customdisksize" : 2, #GBs
                            "serviceoffering" : 27,
                            "template"  : 256,
                            "zoneid" : 1,
                            "username" : "root",
                            "password" : "password",
                            "ssh_port" : 22,
                            "diskname" : "TestDiskServ",
                            "networkids":205,
                            "hypervisor":'XenServer',
                            "account":'admin',
                            "domainid":1,
                            "ipaddressid":3,
                            "privateport":22,
                            "publicport":22,
                            "ipaddress":'69.41.185.229',
                            "protocol":'TCP',
                            "diskdevice" : "/dev/sda",
                        }

TEST_SERVICE_OFFERING = {
                         "off_1" :
                                    {
                                        "name":"Service Offering 1",
                                        "displaytext" : "Service Offering 1",
                                        "cpunumber":1,
                                        "cpuspeed": 200,
                                        "memory": 200,
                                },

                         "off_2" :
                                    {
                                        "name":"Service Offering 2",
                                        "displaytext" : "Service Offering 2",
                                        "cpunumber":1,
                                        "cpuspeed": 200,
                                        "memory": 200,
                                }
                         }

TEST_DISK_OFFERING = {
                         "off_1" :
                                    {
                                        "name":"Disk offering 1",
                                        "displaytext" : "Disk offering 1",
                                        "disksize": 1
                                },

                         "off_2" :
                                    {
                                        "name":"Disk offering 2",
                                        "displaytext" : "Disk offering 2",
                                        "disksize": 1
                                }
                         }
TEST_TEMPLATE_SERVICES ={
                            "virtual_machine" :
                                        {
                                            "template" : 256,
                                            "zoneid" : 1,
                                            "serviceoffering" : 27,
                                            "displayname" : "testVM",
                                            "username" : "root",
                                            "password" : "password",
                                            "ssh_port" : 22,
                                            "networkids":205,
                                            "hypervisor":'XenServer',
                                            "account":'admin',
                                            "domainid":1,
                                            "ipaddressid":9,
                                            "privateport":22,
                                            "publicport":22,
                                            "ipaddress":'69.41.185.229',
                                            "protocol":'TCP',
                                         },
                            "volume":
                                        {
                                            "offerings" : 1,
                                            "volumeoffering" : 3,
                                            "diskname" : "TestVolumeTemplate",
                                            "zoneid" : 1,
                                            "diskofferingid": 3,
                                         },
                            "template_1":
                                        {
                                            "displaytext": "Test Template Type 1",
                                            "name": "testTemplate",
                                            "ostypeid":12,
                                            "isfeatured" : False,
                                            "ispublic" : False,
                                            "isextractable":False,
                                         },
                            "template_2":
                                        {
                                            "displaytext": "Test Template Type 2",
                                            "name": "testTemplate",
                                            "ostypeid":12,
                                            "isfeatured" : True,
                                            "ispublic" :True,
                                            "isextractable":True,
                                            "mode": "HTTP_DOWNLOAD",
                                            "zoneid":1,
                                         },
                            "templatefilter": 'self',
                            "destzoneid": 2,
                            "sourcezoneid": 1,
                            "isfeatured" : True,
                            "ispublic" : True,
                            "isextractable":False,
                            "bootable":True,
                            "passwordenabled":True,
                            "ostypeid":15,
                            "account":'bhavin333',
                            "domainid":1,
                         }

TEST_ISO_SERVICES = {
                      "iso_1":
                                {
                                 "displaytext" : "Test ISO type 1",
                                 "name": "testISOType_1",
                                 "url": "http://iso.linuxquestions.org/download/504/1819/http/gd4.tuwien.ac.at/dsl-4.4.10.iso",
                                 "zoneid" : 1,
                                 "isextractable": True,
                                 "isfeatured":True,
                                 "ispublic": True,
                                 "ostypeid":12,
                                 },
                        "iso_2":
                                {
                                 "displaytext" : "Test ISO type 2",
                                 "name": "testISOType_2",
                                 "url": "http://iso.linuxquestions.org/download/504/1819/http/gd4.tuwien.ac.at/dsl-4.4.10.iso",
                                 "zoneid" : 1,
                                 "isextractable": True,
                                 "isfeatured":True,
                                 "ispublic": True,
                                 "ostypeid":12,
                                 "mode":'HTTP_DOWNLOAD',
                                 "ostypeid":12,
                                 },
                            "destzoneid": 2,
                            "sourcezoneid": 1,
                            "isfeatured" : True,
                            "ispublic" : True,
                            "isextractable":True,
                            "bootable":True,
                            "passwordenabled":True,
                            "ostypeid":15,
                            "account":'bhavin333',
                            "domainid":1,
                     }

TEST_NETWORK_SERVICES = {
                            "admin_account" : "admin",
                            "user_account" : "gaurav_cl4",
                            "zoneid" : 1,
                            "domainid" : 1,
                            "account" : {
                                            "email" : "test@test.com",
                                            "firstname" : "Test",
                                            "lastname" : "User",
                                            "username" : "testuser79",
                                            "password" : "fr3sca",
                                            "zoneid" : 1,
                                            "networkofferingid" : 6,
                                        },
                            "server" :
                                    {
                                        "template" : 256,
                                        "zoneid" : 1,
                                        "serviceoffering" : 40,
                                        "diskoffering" : 3,
                                        "displayname" : "testserver",
                                        "username" : "root",
                                        "password" : "password",
                                        "ssh_port" : 22,
                                        "hypervisor":'XenServer',
                                        "account":'admin',
                                        "domainid":1,
                                        "ipaddressid":3,
                                        "privateport":22,
                                        "publicport":22,
                                        "ipaddress":'69.41.185.229',
                                        "protocol":'TCP',
                                },
                        "natrule" :
                                {
                                    "privateport" : 22,
                                    "publicport" : 22,
                                    "protocol" : "TCP"
                                },
                        "lbrule" :
                                {
                                    "name" : "SSH",
                                    "alg" : "roundrobin",
                                    "privateport" : 22,
                                    "publicport" : 22,
                                }


                        }
