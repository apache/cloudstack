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

#Import Local Modules
from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             Volume,
                             DiskOffering,
                             )
from marvin.lib.common import (get_domain,
                                get_zone,
                                get_template,
                                find_storage_pool_type)
from marvin.codes import (
    PASS,
    FAILED,
    JOB_FAILED,
    JOB_CANCELLED,
    JOB_SUCCEEDED
)
from nose.plugins.attrib import attr
import time


class TestMultipleVolumeAttach(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestMultipleVolumeAttach, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls._cleanup = []
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.invalidStoragePoolType = False
        #for LXC if the storage pool of type 'rbd' ex: ceph is not available, skip the test
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                # RBD storage type is required for data volumes for LXC
                cls.invalidStoragePoolType = True
                return

        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )

        template = get_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["template"] = template.id
        cls.services["diskofferingid"] = cls.disk_offering.id

        # Create VMs, VMs etc
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offering"]
                                        )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                )

        #Create volumes (data disks)
        cls.volume1 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )

        cls.volume2 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )

        cls.volume3 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )

        cls.volume4 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup = [
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.invalidStoragePoolType:
            self.skipTest("Skipping test because valid storage pool not\
                    available")

    def tearDown(self):
        cleanup_resources(self.apiClient, self.cleanup)
        return

    # Method to attach volume but will return immediately as an asynchronous task does.
    def attach_volume(self, apiclient,virtualmachineid, volume):
        """Attach volume to instance"""
        cmd = attachVolume.attachVolumeCmd()
        cmd.isAsync = "false"
        cmd.id = volume.id
        cmd.virtualmachineid = virtualmachineid
        return apiclient.attachVolume(cmd)

    # Method to check the volume attach async jobs' status
    def query_async_job(self, apiclient, jobid):
        """Query the status for Async Job"""
        try:
            asyncTimeout = 3600
            cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
            cmd.jobid = jobid
            timeout = asyncTimeout
            async_response = FAILED
            while timeout > 0:
                async_response = apiclient.queryAsyncJobResult(cmd)
                if async_response != FAILED:
                    job_status = async_response.jobstatus
                    if job_status in [JOB_CANCELLED,
                                  JOB_SUCCEEDED]:
                        break
                    elif job_status == JOB_FAILED:
                        raise Exception("Job failed: %s" \
                                    % async_response)
                time.sleep(5)
                timeout -= 5
                self.debug("=== JobId: %s is Still Processing, "
                              "Will TimeOut in: %s ====" % (str(jobid),
                                                           str(timeout)))
            return async_response
        except Exception as e:
            self.debug("==== Exception Occurred for Job: %s ====" %
                  str(e))
            return FAILED

    @attr(tags = ["advanced", "advancedns", "basic"], required_hardware="true")
    def test_attach_multiple_volumes(self):
        """Attach multiple Volumes simultaneously to a Running VM
        """
        # Validate the following
        # 1. All data disks attached successfully without any exception

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume1.id,
                                                    self.virtual_machine.id
                                                    ))
        vol1_jobId = self.attach_volume(self.apiClient, self.virtual_machine.id,self.volume1)

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume2.id,
                                                    self.virtual_machine.id
                                                    ))
        vol2_jobId = self.attach_volume(self.apiClient,self.virtual_machine.id, self.volume2)

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume3.id,
                                                    self.virtual_machine.id
                                                    ))
        vol3_jobId = self.attach_volume(self.apiClient,self.virtual_machine.id, self.volume3)

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume4.id,
                                                    self.virtual_machine.id
                                                    ))
        vol4_jobId = self.attach_volume(self.apiClient,self.virtual_machine.id, self.volume4)

        self.query_async_job(self.apiClient,vol1_jobId.jobid)
        self.query_async_job(self.apiClient,vol2_jobId.jobid)
        self.query_async_job(self.apiClient,vol3_jobId.jobid)
        self.query_async_job(self.apiClient,vol4_jobId.jobid)

        # List all the volumes attached to the instance. Includes even the Root disk.
        list_volume_response = Volume.list(
                                            self.apiClient,
                                            virtualmachineid=self.virtual_machine.id,
                                            type="DATADISK",
                                            account=self.account.name,
                                            domainid=self.account.domainid
                                          )
        self.assertEqual(
            validateList(list_volume_response)[0],
            PASS,
            "Check list response returns a valid list"
        )

        self.assertEqual(
                            len(list_volume_response),
                            4,
                            "All 4 data disks are not attached to VM Successfully"
                            )

        return
