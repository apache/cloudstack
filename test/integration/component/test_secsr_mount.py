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
from builtins import False
""" Tests for Secondary Storage with Local Storage
"""

# Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

_multiprocess_shared_ = False


class TestSecSRMount(cloudstackTestCase):

    def setUp(self):
        self.logger = logging.getLogger('TestSecSRMount')
        self.stream_handler = logging.StreamHandler()
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.stream_handler)
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.cleanup = []
        self.services = {
                            "service_offering_local": {
                                "name": "Ultra Tiny Local Instance",
                                "displaytext": "Ultra Tiny Local Instance",
                                "cpunumber": 1,
                                "cpuspeed": 100,
                                "memory": 128,
                                "storagetype": "local"
                            },
                            "vm": {
                                "username": "root",
                                "password": "password",
                                "ssh_port": 22,
                                # Hypervisor type should be same as
                                # hypervisor type of cluster
                                "privateport": 22,
                                "publicport": 22,
                                "protocol": 'TCP',
                            },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         "sleep": 30,
                         "timeout": 10,
                         }


    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def isOnlyLocalStorageAvailable(self):
        if not self.zone.localstorageenabled:
            self.skipTest("Local Storage not enabled")

        storage_pools = StoragePool.list(
                   self.apiclient,
                   zoneid=self.zone.id,
                   listall=True
                    )
        self.assertEqual(
                           isinstance(storage_pools, list),
                           True,
                           "Check if listStoragePools returns a valid response"
                           )
        for storage_pool in storage_pools:
            if storage_pool.type == 'NetworkFilesystem':
                return False
            
        return True

    def download(self, apiclient, template_id, retries=12, interval=5):
        """Check if template download will finish in 1 minute"""
        while retries > -1:
            time.sleep(interval)
            template_response = Template.list(
                apiclient,
                id=template_id,
                zoneid=self.zone.id,
                templatefilter='self'
            )

            if isinstance(template_response, list):
                template = template_response[0]
                if not hasattr(template, 'status') or not template or not template.status:
                    retries = retries - 1
                    continue

                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # if Failed
                # Error - Any other string
                if 'Failed' in template.status:
                    raise Exception(
                        "Failed to download template: status - %s" %
                        template.status)

                elif template.status == 'Download Complete' and template.isready:
                    return

                elif 'Downloaded' in template.status:
                    retries = retries - 1
                    continue

                elif 'Installing' not in template.status:
                    if retries >= 0:
                        retries = retries - 1
                        continue
                    raise Exception(
                        "Error in downloading template: status - %s" %
                        template.status)

            else:
                retries = retries - 1
        raise Exception("Template download failed exception.")

    @attr(
        tags=[
            "advanced",
            "xenserver"],
        required_hardware="true")
    def test_01_prepare_template_local_storage(self):
    
        if not self.isOnlyLocalStorageAvailable():
            self.skipTest("Skipping this test as this is for Local storage on only.")
        
        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Host id %s, hypervisor %s, localstorage %s' % (host.id, host.hypervisor, host.islocalstorageactive))
                  
        if len(listHost) < 2:
            self.logger.debug("Prepare secondary storage race condition can be tested with two or more host only %s, found" % len(listHost));
            self.skipTest("Prepare secondary storage can be tested with two host only %s, found" % len(listHost))
        
        list_template_response = Template.list(
                                            self.apiclient,
                                            templatefilter='all',
                                            zoneid=self.zone.id)
        
        template_response = list_template_response[0]
        
        self.logger.debug('Template id %s is Ready %s' % (template_response.id, template_response.isready))
        
        if template_response.isready != True:
            self.skipTest('Template id %s is Not Ready' % (template_response.id))

        try:
            cmd = prepareTemplate.prepareTemplateCmd()
            cmd.zoneid = self.zone.id
            cmd.templateid = template_response.id
            result = self.apiclient.prepareTemplate(cmd)
            self.logger.debug('Prepare Template result %s' % result)
        except Exception as e:
           raise Exception("Warning: Exception during prepare template : %s" % e)

        self.download(self.apiclient, template_response.id)
        return
