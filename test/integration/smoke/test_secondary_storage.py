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
""" BVT tests for Secondary Storage
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

#Import System modules
import time
_multiprocess_shared_ = True

class TestSecStorageServices(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = super(TestSecStorageServices, cls).getClsTestClient().getApiClient()
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        # Get Zone and pod
        self.zones = []
        self.pods = []
        for zone in self.config.zones:
            cmd = listZones.listZonesCmd()
            cmd.name = zone.name
            z = self.apiclient.listZones(cmd)
            if isinstance(z, list) and len(z) > 0:
                self.zones.append(z[0].id)
            for pod in zone.pods:
                podcmd = listPods.listPodsCmd()
                podcmd.zoneid = z[0].id
                p = self.apiclient.listPods(podcmd)
                if isinstance(p, list) and len(p) >0:
                    self.pods.append(p[0].id)

        self.domains = []
        dcmd = listDomains.listDomainsCmd()
        domains = self.apiclient.listDomains(dcmd)
        assert isinstance(domains, list) and len(domains) > 0
        for domain in domains:
            self.domains.append(domain.id)
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_01_sys_vm_start(self):
        """Test system VM start
        """

        # 1. verify listHosts has all 'routing' hosts in UP state
        # 2. verify listStoragePools shows all primary storage pools
        #    in UP state
        # 3. verify that secondary storage was added successfully

        list_hosts_response = list_hosts(
                           self.apiclient,
                           type='Routing',
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
        for _ in range(2):
            list_ssvm_response = list_ssvms(
                                    self.apiclient,
                                    systemvmtype='secondarystoragevm',
                                    )

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
                if ssvm.state != 'Running':
                    time.sleep(30)
                    continue
        for ssvm in list_ssvm_response:
            self.assertEqual(
                            ssvm.state,
                            'Running',
                            "Check whether state of SSVM is running"
                        )

        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic", "eip", "sg"], required_hardware="false")
    def test_02_sys_template_ready(self):
        """Test system templates are ready
        """

        # Validate the following
        # If SSVM is in UP state and running
        # 1. wait for listTemplates to show all builtin templates downloaded and
        # in Ready state

        hypervisors = {}
        for zone in self.config.zones:
            for pod in zone.pods:
                for cluster in pod.clusters:
                    hypervisors[cluster.hypervisor] = "self"

        for zid in self.zones:
            for k, v in hypervisors.items():
                self.debug("Checking BUILTIN templates in zone: %s" %zid)
                list_template_response = list_templates(
                                        self.apiclient,
                                        hypervisor=k,
                                        zoneid=zid,
                                        templatefilter=v,
                                        listall=True,
                                        account='system'
                                        )
                self.assertEqual(validateList(list_template_response)[0], PASS,\
                        "templates list validation failed")

                # Ensure all BUILTIN templates are downloaded
                templateid = None
                for template in list_template_response:
                    if template.templatetype == "BUILTIN":
                        templateid = template.id

                    template_response = list_templates(
                                    self.apiclient,
                                    id=templateid,
                                    zoneid=zid,
                                    templatefilter=v,
                                    listall=True,
                                    account='system'
                                    )
                    if isinstance(template_response, list):
                        template = template_response[0]
                    else:
                        raise Exception("ListTemplate API returned invalid list")

                    if template.status == 'Download Complete':
                        self.debug("Template %s is ready in zone %s"%(template.templatetype, zid))
                    elif 'Downloaded' not in template.status.split():
                        self.debug("templates status is %s"%template.status)

                    self.assertEqual(
                        template.isready,
                        True,
                        "Builtin template is not ready %s in zone %s"%(template.status, zid)
                    )
