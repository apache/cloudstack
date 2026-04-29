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

from marvin.cloudstackAPI import *
from marvin.lib.utils import random_gen
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Volume,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             Snapshot)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone,
                               get_pod,
                               list_snapshots,
                               list_volumes)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.codes import (
    FAILED,
    JOB_FAILED,
    JOB_CANCELLED,
    JOB_SUCCEEDED
)
import time
from marvin.codes import PASS

class TestSnapshotRootDisk(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        testClient = super(TestSnapshotRootDisk, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.services['mode'] = cls.zone.networktype


        cls.hypervisorNotSupported = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc'] or 'kvm-centos6' in cls.testClient.getZoneForTests():
            cls.hypervisorNotSupported = True

        cls._cleanup = []
        if not cls.hypervisorNotSupported:
            cls.template = get_template(cls.apiclient, template_type='BUILTIN')
            cls.services["domainid"] = cls.domain.id
            cls.services["small"]["zoneid"] = cls.zone.id
            cls.services["templates"]["ostypeid"] = cls.template.ostypeid
            cls.services["zoneid"] = cls.zone.id
            # Create VMs, NAT Rules etc
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"]
            )
            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.services["disk_offering"]
            )
            cls.virtual_machine = cls.virtual_machine_with_disk = \
                VirtualMachine.create(
                    cls.apiclient,
                    cls.services["small"],
                    templateid=cls.template.id,
                    accountid=cls.account.name,
                    domainid=cls.account.domainid,
                    zoneid=cls.zone.id,
                    serviceofferingid=cls.service_offering.id,
                    mode=cls.services["mode"]
                )

            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.account)
            # cls._cleanup.append(cls.template)
            cls._cleanup.append(cls.disk_offering)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            # cleanup_resources(self.apiclient, self.cleanup)

            for obj in self.cleanup:
                """Delete Volume"""
                cmd = deleteVolume.deleteVolumeCmd()
                cmd.id = obj.id
                self.apiclient.deleteVolume(cmd)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return


    # Method to create volume from snapshot but will return immediately as an asynchronous task does.
    def create_from_snapshot(cls, apiclient, snapshot_id, services,
                             account=None, domainid=None):
            """Create volume from snapshot"""
            cmd = createVolume.createVolumeCmd()
            cmd.isAsync = "false"
            cmd.name = "-".join([services["diskname"], random_gen()])
            cmd.snapshotid = snapshot_id
            cmd.zoneid = services["zoneid"]
            if "size" in services:
                cmd.size = services["size"]
            if services["ispublic"]:
                cmd.ispublic = services["ispublic"]
            else:
                cmd.ispublic = False
            if account:
                cmd.account = account
            else:
                cmd.account = services["account"]
            if domainid:
                cmd.domainid = domainid
            else:
                cmd.domainid = services["domainid"]
            return Volume(apiclient.createVolume(cmd).__dict__)


    def query_async_job(self, apiclient, jobid):
        """Query the status for Async Job"""
        try:
            asyncTimeout = 5600
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

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_snapshot_root_disk(self):
        """Test Snapshot Root Disk
        """

        # Validate the following
        # 1. Account List should list the accounts that was existed.
        # 2. List Volumes
        # 3. Create Snapshot From the volume[0] from volume list
        # 4. List Snapshots
        # 5. Create Volume V1,V2,V3 from Snapshot List[0]
        # 6. Verify that Async Job id's status
        # 7. List all the volumes
        # 8. Add Volumes V1,V2,V3 to cleanup
        # 9. Check list response returns a valid list
        # 10. Check if result exists in list item call

        # 1. Account List should list the accounts that was existed.
        account_list = Account.list(
            self.apiclient,
            listAll = True,
            roleType ='Admin'
            );

        # 2. List Volumes
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine_with_disk.id,
            type='ROOT',
            listall=True
        )

        # 3. Create Snapshot From the volume[0] from volume list
        snapshot = Snapshot.create(
            self.apiclient,
            volumes[0].id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        #self._cleanup.append(snapshot)
        self.debug("Snapshot created: ID - %s" % snapshot.id)

        # 4. List Snapshots
        snapshots = list_snapshots(
            self.apiclient,listall=True
            )

        # 5. Create Volume V1,V2,V3 from Snapshot List[0]
        services = {"diskname": "Vol", "zoneid": self.zone.id, "size": 10, "ispublic": True}
        vol1_jobId = self.create_from_snapshot(
             self.apiclient,snapshots[0].id,
            services,

            account_list[0].name,
            account_list[0].domainid
         );

        vol2_jobId = self.create_from_snapshot(
            self.apiclient, snapshots[0].id,
            services,

            account_list[0].name,
            account_list[0].domainid
        );

        vol3_jobId = self.create_from_snapshot(
             self.apiclient, snapshots[0].id,
             services,

             account_list[0].name,
             account_list[0].domainid
         );

        # 6. Verify that Async Job id's status
        self.query_async_job(self.apiclient, vol1_jobId.jobid)
        self.query_async_job(self.apiclient, vol2_jobId.jobid)
        self.query_async_job(self.apiclient, vol3_jobId.jobid)

        # 7. List all the volumes
        list_volume_response = Volume.list(
            self.apiclient,
            type="DATADISK",
            account=account_list[0].name,
            domainid=account_list[0].domainid
        )

        # 8. Add Volumes V1,V2,V3 to cleanup
        self.cleanup.append(list_volume_response[0]);
        self.cleanup.append(list_volume_response[1]);
        self.cleanup.append(list_volume_response[2]);

        # 9. Check list response returns a valid list
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list response returns a valid list"
        )


        # 10.Check if result exists in list item call
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if result exists in list item call"
        )

        self.assertIsNotNone(snapshots[0].zoneid,
                             "Zone id is not none in listSnapshots")
        self.assertEqual(
            snapshots[0].zoneid,
            self.zone.id,
            "Check zone id in the list snapshots"
        )

        return
