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
""" Tests for listing async jobs by status, period, resource and management server, and for cancelling them """

import time
import uuid

from marvin.cloudstackAPI import (cancelAsyncJob,
                                  listAsyncJobs,
                                  listEvents,
                                  listUsageJobs,
                                  migrateVirtualMachine,
                                  queryAsyncJobResult)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import (JOB_CANCELLED,
                          JOB_FAILED,
                          JOB_INPROGRESS,
                          JOB_SUCCEEDED,
                          PASS)
from marvin.lib.base import (Account,
                             DiskOffering,
                             ServiceOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                               get_test_template,
                               get_zone,
                               list_hosts)
from nose.plugins.attrib import attr

ALL_STATUSES = "%d,%d,%d,%d" % (JOB_INPROGRESS, JOB_SUCCEEDED, JOB_FAILED, JOB_CANCELLED)
CANCELLABLE_HYPERVISORS = ["kvm", "vmware", "xenserver"]


class TestAsyncJobsManagement(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAsyncJobsManagement, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
        cls._cleanup = []

        cls.service_offering = ServiceOffering.create(cls.apiclient, cls.testdata["service_offering"])
        cls._cleanup.append(cls.service_offering)
        cls.disk_offering = DiskOffering.create(cls.apiclient, cls.testdata["disk_offering"])
        cls._cleanup.append(cls.disk_offering)
        cls.account = Account.create(cls.apiclient, cls.testdata["account"], domainid=cls.domain.id)
        cls._cleanup.append(cls.account)

        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = cls.template.id
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            diskofferingid=cls.disk_offering.id,
            hypervisor=cls.hypervisor
        )
        cls._cleanup.append(cls.virtual_machine)
        # the deploy job is the completed job every listing test looks for
        cls.deploy_job_id = cls.virtual_machine.jobid

    @classmethod
    def tearDownClass(cls):
        super(TestAsyncJobsManagement, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestAsyncJobsManagement, self).tearDown()

    def list_jobs(self, **kwargs):
        cmd = listAsyncJobs.listAsyncJobsCmd()
        cmd.listall = True
        for key, value in kwargs.items():
            setattr(cmd, key, value)
        return self.apiclient.listAsyncJobs(cmd) or []

    @staticmethod
    def find_job(jobs, jobid):
        return next((job for job in jobs if job.jobid == jobid), None)

    def query_job(self, jobid):
        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = jobid
        return self.apiclient.queryAsyncJobResult(cmd)

    def wait_for_job(self, jobid, timeout=300):
        deadline = time.time() + timeout
        while time.time() < deadline:
            job = self.query_job(jobid)
            if job.jobstatus != JOB_INPROGRESS:
                return job
            time.sleep(3)
        self.fail("Job %s still in progress after %d seconds" % (jobid, timeout))

    def cancel_job(self, jobid):
        cmd = cancelAsyncJob.cancelAsyncJobCmd()
        cmd.jobid = jobid
        return self.apiclient.cancelAsyncJob(cmd)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_default_listing_is_pending_only(self):
        """Without a status filter only pending jobs are listed, so a completed job is absent"""
        jobs = self.list_jobs()
        self.assertIsNone(self.find_job(jobs, self.deploy_job_id),
                          "Completed deploy job %s listed without a status filter" % self.deploy_job_id)
        for job in jobs:
            self.assertEqual(job.jobstatus, JOB_INPROGRESS, "Non-pending job %s in the default listing" % job.jobid)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_completed_jobs_by_status_and_duration(self):
        """A completed job is reachable by status, with the period, by ordinal or by name"""
        job = self.find_job(self.list_jobs(jobstatus=JOB_SUCCEEDED, duration=1), self.deploy_job_id)
        self.assertIsNotNone(job, "Deploy job %s not listed with jobstatus=%d duration=1" % (self.deploy_job_id, JOB_SUCCEEDED))
        self.assertEqual(job.jobstatus, JOB_SUCCEEDED)
        self.assertTrue(job.completed, "Completed job has no completion time")
        self.assertIn("DeployVMCmd", job.cmd)
        self.assertEqual(job.jobinstanceid, self.virtual_machine.id)

        by_name = self.find_job(self.list_jobs(jobstatus="SUCCEEDED", duration=1), self.deploy_job_id)
        self.assertIsNotNone(by_name, "Status filter by name did not match the deploy job")

        failed = self.find_job(self.list_jobs(jobstatus=JOB_FAILED, duration=1), self.deploy_job_id)
        self.assertIsNone(failed, "Succeeded deploy job listed under the failed status")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_jobs_by_resource(self):
        """Jobs are reachable from the resource they acted on"""
        jobs = self.list_jobs(resourcetype="VirtualMachine", resourceid=self.virtual_machine.id,
                              jobstatus=ALL_STATUSES, duration=1)
        self.assertIsNotNone(self.find_job(jobs, self.deploy_job_id), "Deploy job not listed for its instance")
        for job in jobs:
            self.assertEqual(job.jobinstanceid, self.virtual_machine.id, "Job %s for another resource listed" % job.jobid)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_jobs_by_management_server(self):
        """Jobs are reachable from the management server that ran them"""
        job = self.find_job(self.list_jobs(jobstatus=JOB_SUCCEEDED, duration=1), self.deploy_job_id)
        self.assertIsNotNone(job)
        msid = getattr(job, "managementserverid", None)
        if not msid:
            self.skipTest("Deploy job carries no management server id")

        jobs = self.list_jobs(managementserverid=msid, jobstatus=JOB_SUCCEEDED, duration=1)
        self.assertIsNotNone(self.find_job(jobs, self.deploy_job_id), "Deploy job not listed for its management server")
        for entry in jobs:
            self.assertEqual(entry.managementserverid, msid, "Job %s of another management server listed" % entry.jobid)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_events_link_to_job(self):
        """The events a job raised are reachable by its id"""
        cmd = listEvents.listEventsCmd()
        cmd.listall = True
        cmd.jobid = self.deploy_job_id
        events = self.apiclient.listEvents(cmd) or []
        self.assertTrue(events, "No events listed for deploy job %s" % self.deploy_job_id)
        self.assertTrue(any(event.type.startswith("VM.CREATE") for event in events), "No VM.CREATE event for the deploy job")
        for event in events:
            self.assertEqual(event.jobid, self.deploy_job_id, "Event %s of another job listed" % event.id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_cancel_completed_job_is_refused(self):
        """A finished job cannot be cancelled and is left as it was"""
        with self.assertRaises(Exception):
            self.cancel_job(self.deploy_job_id)
        self.assertEqual(self.query_job(self.deploy_job_id).jobstatus, JOB_SUCCEEDED, "Refused cancel changed the job")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_cancel_unknown_job_is_refused(self):
        with self.assertRaises(Exception):
            self.cancel_job(str(uuid.uuid4()))

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_cancel_running_migration(self):
        """Cancelling a live migration either stops it or is refused; the instance ends Running either way"""
        if self.hypervisor.lower() not in CANCELLABLE_HYPERVISORS:
            self.skipTest("Cancellation of hypervisor jobs is not implemented for %s" % self.hypervisor)

        vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)[0]
        source_host = list_hosts(self.apiclient, id=vm.hostid)[0]
        targets = [h for h in (list_hosts(self.apiclient, clusterid=source_host.clusterid, type="Routing",
                                          state="Up", resourcestate="Enabled") or []) if h.id != source_host.id]
        if not targets:
            self.skipTest("Need a second host in cluster %s to migrate to" % source_host.clusterid)

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = vm.id
        cmd.hostid = targets[0].id
        cmd.isAsync = "false"
        jobid = self.apiclient.migrateVirtualMachine(cmd, method="GET").jobid

        cancelled = True
        try:
            self.cancel_job(jobid)
        except Exception as e:
            # the migration finished, or the hypervisor would not stop it: a refusal, not a failure
            self.debug("Cancel of migration job %s refused: %s" % (jobid, e))
            cancelled = False

        job = self.wait_for_job(jobid)
        if cancelled:
            self.assertEqual(job.jobstatus, JOB_CANCELLED, "Accepted cancel did not end the job as cancelled")
        else:
            self.assertIn(job.jobstatus, [JOB_SUCCEEDED, JOB_FAILED], "Refused cancel left the job in an odd state")

        response = self.virtual_machine.getState(self.apiclient, VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        if cancelled:
            vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)[0]
            self.assertEqual(vm.hostid, source_host.id, "Cancelled migration left the instance on the target host")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_09_list_usage_jobs(self):
        """Usage server jobs list without error, and filter by usage server"""
        cmd = listUsageJobs.listUsageJobsCmd()
        cmd.duration = 24
        self.apiclient.listUsageJobs(cmd)

        cmd = listUsageJobs.listUsageJobsCmd()
        cmd.usageserver = "no-such-usage-server"
        self.assertFalse(self.apiclient.listUsageJobs(cmd), "Jobs listed for a usage server that does not exist")
