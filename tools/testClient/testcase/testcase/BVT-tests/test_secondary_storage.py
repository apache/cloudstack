# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Secondary Storage
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *

#Import System modules
import time

class Services:
    """Test secondary storage Services
    """

    def __init__(self):
        self.services = {
                         "storage": {
                                "url": "nfs://192.168.100.131/SecondaryStorage"
                                # Format: File_System_Type/Location/Path
                            },
                        "hypervisors": {
                            0: {
                                    "hypervisor": "XenServer",
                                    "templatefilter": "self",
                                },
                            1: {
                                    "hypervisor": "KVM",
                                    "templatefilter": "self",
                                },
                            2: {
                                    "hypervisor": "VMWare",
                                    "templatefilter": "self",
                                },
                            },
                         "sleep": 60,
                         "timeout": 5,
                        }

class TestSecStorageServices(cloudstackTestCase):
    
    @classmethod
    def setUpClass(cls):
        cls.api_client = super(
                               TestSecStorageServices,
                               cls
                               ).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls._cleanup = []
        return
    
    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        self.services = Services().services
        # Get Zone and pod
        self.domain = get_domain(self.apiclient, self.services)
        self.zone = get_zone(self.apiclient, self.services)
        self.pod = get_pod(self.apiclient, self.zone.id)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def test_01_add_sec_storage(self):
        """Test secondary storage
        """

        # Validate the following:
        # 1. secondary storage should be added to the zone.
        # 2. Verify with listHosts and type secondarystorage

        cmd = addSecondaryStorage.addSecondaryStorageCmd()
        cmd.zoneid = self.zone.id
        cmd.url = self.services["storage"]["url"]
        sec_storage = self.apiclient.addSecondaryStorage(cmd)
        
        self.debug("Added secondary storage to zone: %s" % self.zone.id)
        # Cleanup at the end
        self._cleanup.append(sec_storage)
        
        self.assertEqual(
                            sec_storage.zoneid,
                            self.zone.id,
                            "Check zoneid where sec storage is added"
                        )

        list_hosts_response = list_hosts(
                                         self.apiclient,
                                         type='SecondaryStorage',
                                         id=sec_storage.id
                           )
        self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                        len(list_hosts_response),
                        0,
                        "Check list Hosts response"
                        )

        host_response = list_hosts_response[0]
        #Check if host is Up and running
        self.assertEqual(
                        host_response.id,
                        sec_storage.id,
                        "Check ID of secondary storage"
                        )
        self.assertEqual(
                        sec_storage.type,
                        host_response.type,
                        "Check type of host from list hosts response"
                        )
        return

    def test_02_sys_vm_start(self):
        """Test system VM start
        """

        # 1. verify listHosts has all 'routing' hosts in UP state
        # 2. verify listStoragePools shows all primary storage pools
        #    in UP state
        # 3. verify that secondary storage was added successfully

        list_hosts_response = list_hosts(
                           self.apiclient,
                           type='Routing',
                           zoneid=self.zone.id,
                           podid=self.pod.id
                           )
        self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        # ListHosts has all 'routing' hosts in UP state
        self.assertNotEqual(
                                len(list_hosts_response),
                                0,
                                "Check list host response"
                            )
        for host in list_hosts_response:
            self.assertEqual(
                        host.state,
                        'Up',
                        "Check state of routing hosts is Up or not"
                        )

        # ListStoragePools shows all primary storage pools in UP state
        list_storage_response = list_storage_pools(
                                                   self.apiclient,
                                                   zoneid=self.zone.id,
                                                   podid=self.pod.id
                                                   )
        self.assertEqual(
                            isinstance(list_storage_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                                len(list_storage_response),
                                0,
                                "Check list storage pools response"
                            )

        for primary_storage in list_hosts_response:
            self.assertEqual(
                        primary_storage.state,
                        'Up',
                        "Check state of primary storage pools is Up or not"
                        )

        # Secondary storage is added successfully
        timeout = self.services["timeout"]
        while True:
            list_hosts_response = list_hosts(
                           self.apiclient,
                           type='SecondaryStorage',
                           zoneid=self.zone.id,
                           )

            if not isinstance(list_hosts_response, list):
                # Sleep to ensure Secondary storage is Up
                time.sleep(int(self.services["sleep"]))
                timeout = timeout - 1
            elif timeout == 0 or isinstance(list_hosts_response, list):
                break
        
        self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
        self.assertNotEqual(
                        len(list_hosts_response),
                        0,
                        "Check list Hosts response"
                        )

        host_response = list_hosts_response[0]
        #Check if host is Up and running
        self.assertEqual(
                        host_response.state,
                        'Up',
                        "Check state of secondary storage"
                        )
        self.debug("Checking SSVM status in zone: %s" % self.zone.id)

        timeout = self.services["timeout"]

        while True:
            list_ssvm_response = list_ssvms(
                                        self.apiclient,
                                        systemvmtype='secondarystoragevm',
                                        zoneid=self.zone.id,
                                        podid=self.pod.id
                                        )
            if not isinstance(list_ssvm_response, list):
                # Sleep to ensure SSVMs are Up and Running
                time.sleep(int(self.services["sleep"]))
                timeout = timeout - 1
            elif timeout == 0 or isinstance(list_ssvm_response, list):
                break
            
        self.assertEqual(
                            isinstance(list_ssvm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #Verify SSVM response
        self.assertNotEqual(
                            len(list_ssvm_response),
                            0,
                            "Check list System VMs response"
                        )

        for ssvm in list_ssvm_response:
            self.assertEqual(
                            ssvm.state,
                            'Running',
                            "Check whether state of SSVM is running"
                        )
        return

    def test_03_sys_template_ready(self):
        """Test system templates are ready
        """

        # Validate the following
        # If SSVM is in UP state and running
        # 1. wait for listTemplates to show all builtin templates
        #    downloaded for all added hypervisors and in “Ready” state"

        for k, v in self.services["hypervisors"].items():

            self.debug("Downloading BUILTIN templates in zone: %s" % 
                                                                self.zone.id)
            
            list_template_response = list_templates(
                                    self.apiclient,
                                    hypervisor=v["hypervisor"],
                                    zoneid=self.zone.id,
                                    templatefilter=v["templatefilter"],
                                    account='system',
                                    domainid=self.domain.id
                                    )

            # Ensure all BUILTIN templates are downloaded
            templateid = None
            for template in list_template_response:
                if template.templatetype == "BUILTIN":
                    templateid = template.id

            # Wait to start a downloading of template
            time.sleep(self.services["sleep"])
            
            while True and (templateid != None):
                
                timeout = self.services["timeout"]
                while True: 
                    template_response = list_templates(
                                    self.apiclient,
                                    id=templateid,
                                    zoneid=self.zone.id,
                                    templatefilter=v["templatefilter"],
                                    account='system',
                                    domainid=self.domain.id
                                    )
                
                    if isinstance(template_response, list):
                        template = template_response[0]
                        break
                    
                    elif timeout == 0:
                        raise Exception("List template API call failed.")
                    
                    time.sleep(1)
                    timeout = timeout - 1
                     
                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # Error - Any other string 
                if template.status == 'Download Complete'  :
                    break
                elif 'Downloaded' not in template.status.split():
                    raise Exception
                elif 'Downloaded' in template.status.split():
                    time.sleep(self.services["sleep"])

            #Ensuring the template is in ready state
            time.sleep(self.services["sleep"])
            
            timeout = self.services["timeout"]
            while True: 
                template_response = list_templates(
                                    self.apiclient,
                                    id=templateid,
                                    zoneid=self.zone.id,
                                    templatefilter=v["templatefilter"],
                                    account='system',
                                    domainid=self.domain.id
                                    )
                
                if isinstance(template_response, list):
                    template = template_response[0]
                    break
                    
                elif timeout == 0:
                    raise Exception("List template API call failed.")
                
                time.sleep(1)
                timeout = timeout - 1
            
            self.assertEqual(
                            isinstance(template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            template = template_response[0]

            self.assertEqual(
                            template.isready,
                            True,
                            "Check whether state of template is ready or not"
                        )
        return