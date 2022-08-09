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
from marvin.lib.utils import (validateList)
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Host,
                             StoragePool,
                             Account,
                             Volume,
                             DiskOffering,
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template,
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
        cls._cleanup.append(cls.disk_offering)

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor)
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

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
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offering"]
                                        )
        cls._cleanup.append(cls.service_offering)

        cls.virtual_machine = VirtualMachine.create(
                                    cls.apiclient,
                                    cls.services,
                                    accountid=cls.account.name,
                                    domainid=cls.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    mode=cls.services["mode"]
                                )
        cls._cleanup.append(cls.virtual_machine)

        #Create volumes (data disks)
        cls.volume1 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup.append(cls.volume1)

        cls.volume2 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup.append(cls.volume2)

        cls.volume3 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup.append(cls.volume3)

        cls.volume4 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup.append(cls.volume4)

        cls.volume5 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup.append(cls.volume5)

        cls.volume6 = Volume.create(
                                   cls.apiclient,
                                   cls.services,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid
                                   )
        cls._cleanup.append(cls.volume6)

    @classmethod
    def tearDownClass(cls):
        super(TestMultipleVolumeAttach, cls).tearDownClass()

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.invalidStoragePoolType:
            self.skipTest("Skipping test because valid storage pool not\
                    available")

    def tearDown(self):

        self.detach_volume(self.volume1)
        self.detach_volume(self.volume2)
        self.detach_volume(self.volume3)
        self.detach_volume(self.volume4)
        self.detach_volume(self.volume5)
        self.detach_volume(self.volume6)

        super(TestMultipleVolumeAttach, self).tearDown

    # Method to attach volume but will return immediately as an asynchronous task does.
    def attach_volume(self, virtualmachineid, volume):
        """Attach volume to instance"""
        cmd = attachVolume.attachVolumeCmd()
        cmd.isAsync = "false"
        cmd.id = volume.id
        cmd.virtualmachineid = virtualmachineid
        return self.apiClient.attachVolume(cmd)

    # Method to detach a volume.
    def detach_volume(self, volume):
        """Detach volume from instance"""
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = volume.id
        try:
            self.apiClient.detachVolume(cmd)
        except Exception as e:
            self.debug("======exception e %s " % e)
            if "The specified volume is not attached to a VM." not in str(e):
                raise e

    # list Primare storage pools
    def check_storage_pools(self, virtualmachineid):
        """
        list storage pools available to the VM
        """
        vm = VirtualMachine.list(self.apiClient, id=virtualmachineid)[0]
        hostid = vm.histid
        host = Host.list(self.apiClient, id=hostid)[0]
        clusterid = host.clusterid
        storage_pools = StoragePool.list(self.apiClient, clusterid=clusterid)
        if len(storage_pools) < 2:
            self.skipTest("at least two accesible primary storage pools needed for the vm to perform this test")
        return storage_pools


    # Method to check the volume attach async jobs' status
    def query_async_job(self, jobid):
        """Query the status for Async Job"""
        try:
            asyncTimeout = 3600
            cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
            cmd.jobid = jobid
            timeout = asyncTimeout
            async_response = FAILED
            while timeout > 0:
                async_response = self.apiClient.queryAsyncJobResult(cmd)
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

    @attr(tags = ["advanced", "advancedns", "basic", "blob"], required_hardware="true")
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
        vol1_jobId = self.attach_volume(self.virtual_machine.id,self.volume1)

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume2.id,
                                                    self.virtual_machine.id
                                                    ))
        vol2_jobId = self.attach_volume(self.virtual_machine.id, self.volume2)

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume3.id,
                                                    self.virtual_machine.id
                                                    ))
        vol3_jobId = self.attach_volume(self.virtual_machine.id, self.volume3)

        self.debug(
                "Attaching volume (ID: %s) to VM (ID: %s)" % (
                                                    self.volume4.id,
                                                    self.virtual_machine.id
                                                    ))
        vol4_jobId = self.attach_volume(self.virtual_machine.id, self.volume4)

        self.query_async_job(vol1_jobId.jobid)
        self.query_async_job(vol2_jobId.jobid)
        self.query_async_job(vol3_jobId.jobid)
        self.query_async_job(vol4_jobId.jobid)

        # List all the volumes attached to the instance. Includes even the Root disk.
        list_volume_response = Volume.list(
                                            self.apiClient,
                                            virtualmachineid=self.virtual_machine.id,
                                            type="DATADISK",
                                            account=self.account.name,
                                            domainid=self.account.domainid
                                          )
        self.debug("====== test paralalisation : %s ===" % str(list_volume_response).replace("}, {", "},\n {"))
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

    @attr(tags = ["advanced", "advancedns", "basic", "bla"], required_hardware="true")
    def test_attach_and_distribute_multiple_volumes(self):
        """
        Test volume distribution over storages

        Given multiple primary storage pools
        Attach multiple Volumes to a VM
        check to see if these do not all end up on the same storage pool
        """
        storage_pools = self.check_storage_pools(self.virtual_machine.id)
        storage_usage={}
        for storage_pool in storage_pools:
            storage_usage[storage_pool.name] = 0
        self.debug("====== test pools for usage : %s ===" % str(storage_usage).replace("}, {", "},\n {"))

        vol1_jobId = self.attach_volume(self.virtual_machine.id,self.volume1)
        vol2_jobId = self.attach_volume(self.virtual_machine.id,self.volume2)
        vol3_jobId = self.attach_volume(self.virtual_machine.id,self.volume3)
        vol4_jobId = self.attach_volume(self.virtual_machine.id,self.volume4)
        vol5_jobId = self.attach_volume(self.virtual_machine.id,self.volume5)
        vol6_jobId = self.attach_volume(self.virtual_machine.id,self.volume6)

        self.query_async_job(vol1_jobId.jobid)
        self.query_async_job(vol2_jobId.jobid)
        self.query_async_job(vol3_jobId.jobid)
        self.query_async_job(vol4_jobId.jobid)
        self.query_async_job(vol5_jobId.jobid)
        self.query_async_job(vol6_jobId.jobid)

        volumes = Volume.list(self.apiClient,
                              virtualmachineid=self.virtual_machine.id,
                              type="DATADISK",
                              account=self.account.name,
                              domainid=self.account.domainid
                             )
        self.debug("====== test distribution : %s ===" % str(volumes).replace("}, {", "},\n {"))
        for volume in volumes:
            self.debug("====== checking storage : %s ===" % str(volume))
            storage_usage[volume.storage] += 1

        self.debug("====== test distribution : %s ===" % str(storage_usage).replace("}, {", "},\n {"))
        # TODO decide what an acceptable 'random' distribution is
        # all of the loads on storages should be less than the number of volumes for a distribution to exist (statistically this may fail once every 6⁵ runs (7776)
        self.assertEqual(
            len(volumes),
            6,
            "All 6 data disks are not attached to VM Successfully"
        )
        for storage_use in storage_usage:
            self.assertTrue(
                storage_usage[storage_use] < 6,
                "not all volumes should be on one storage: %s" % storage_use)
        return
