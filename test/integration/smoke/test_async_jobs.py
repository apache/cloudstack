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
                                  listApis,
                                  createSnapshot,
                                  createVolume,
                                  listAsyncJobs,
                                  listEvents,
                                  listUsageJobs,
                                  migrateVirtualMachine,
                                  migrateVirtualMachineWithVolume,
                                  migrateVolume,
                                  queryAsyncJobResult,
                                  stopVirtualMachine)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import (JOB_CANCELLED,
                          JOB_FAILED,
                          JOB_INPROGRESS,
                          JOB_SUCCEEDED,
                          PASS)
from marvin.lib.base import (Account,
                             DiskOffering,
                             Host,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             VirtualMachine,
                             Volume)
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
        cls.dbclient = cls.testClient.getDbConnection()
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

    def work_was_stopped(self, job, cancelled):
        """An accepted cancel ends the job CANCELLED, or SUCCEEDED with the request recorded when the work finished before the cancel took effect; a refused cancel lets the job run its course"""
        if cancelled:
            self.assertIn(job.jobstatus, [JOB_CANCELLED, JOB_SUCCEEDED], "Accepted cancel left job %s in status %s" % (job.jobid, job.jobstatus))
            self.assertTrue(job.cancelrequested, "Accepted cancel is not recorded on job %s" % job.jobid)
        else:
            self.assertIn(job.jobstatus, [JOB_SUCCEEDED, JOB_FAILED], "Refused cancel left job %s in status %s" % (job.jobid, job.jobstatus))
        return job.jobstatus == JOB_CANCELLED

    def start_instance_again(self):
        """A start right after a volume copy can lose the race for the disk's lock on the host, so it is retried"""
        for attempt in range(1, 4):
            try:
                self.virtual_machine.start(self.apiclient)
                return
            except Exception as e:
                self.debug("Start of instance %s failed on attempt %d: %s" % (self.virtual_machine.id, attempt, e))
                time.sleep(20)
        self.virtual_machine.start(self.apiclient)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_00_cancellable_is_published(self):
        """listApis and listAsyncJobs say in advance whether a command can be cancelled"""
        cmd = listApis.listApisCmd()
        apis = {api.name: api for api in (self.apiclient.listApis(cmd) or [])}
        self.assertTrue(apis["createVolume"].cancellable, "createVolume is not published as cancellable")
        self.assertTrue(apis["migrateVirtualMachine"].cancellable, "migrateVirtualMachine is not published as cancellable")
        self.assertFalse(apis["deployVirtualMachine"].cancellable, "deployVirtualMachine is published as cancellable")

        deploy_job = self.find_job(self.list_jobs(jobstatus=ALL_STATUSES), self.deploy_job_id)
        self.assertIsNotNone(deploy_job, "Deploy job %s not listed" % self.deploy_job_id)
        self.assertFalse(deploy_job.cancellable, "The deploy job is published as cancellable")

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

        self.assertEqual(job.jobinstancename, self.virtual_machine.name)

        by_id = self.list_jobs(id=self.deploy_job_id)

        self.assertEqual(len(by_id), 1, "listAsyncJobs id= did not return exactly the deploy job")

        self.assertEqual(by_id[0].jobid, self.deploy_job_id)
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
        states = set(event.state for event in events)
        self.assertIn("Scheduled", states, "The Scheduled event of the deploy job is not linked to it")
        self.assertIn("Created", states, "The Created event of the deploy job is not linked to it")
        self.assertIn("Completed", states, "The Completed event of the deploy job is not linked to it")
        for event in events:
            self.assertEqual(event.jobid, self.deploy_job_id, "Event %s of another job listed" % event.id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_cancel_completed_job_is_refused(self):
        """A finished job cannot be cancelled and is left as it was"""
        with self.assertRaises(Exception):
            self.cancel_job(self.deploy_job_id)
        self.assertEqual(self.query_job(self.deploy_job_id).jobstatus, JOB_SUCCEEDED, "Refused cancel changed the job")
        cmd = listEvents.listEventsCmd()
        cmd.listall = True
        cmd.type = "JOB.CANCEL"
        cmd.jobid = self.deploy_job_id
        events = self.apiclient.listEvents(cmd) or []
        self.assertTrue(events, "No JOB.CANCEL event recorded for the refused cancel of job %s" % self.deploy_job_id)
        self.assertTrue(all(event.resourceid == self.deploy_job_id for event in events), "JOB.CANCEL event not tied to the job")
        self.assertTrue(any("Cancelled by" in (event.description or "") for event in events), "JOB.CANCEL event does not name who cancelled")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_cancel_unknown_job_is_refused(self):
        with self.assertRaises(Exception):
            self.cancel_job(str(uuid.uuid4()))

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_08_cancel_running_migration(self):
        """Cancelling a live migration stops it, finds it finished, or is refused; the instance ends Running either way"""
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
        stopped = self.work_was_stopped(job, cancelled)

        response = self.virtual_machine.getState(self.apiclient, VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        if stopped:
            vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)[0]
            self.assertEqual(vm.hostid, source_host.id, "Cancelled migration left the instance on the target host")

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_09_cancelled_storage_migration_leaves_no_volume_behind(self):
        """Cancelling a migration with storage rolls back the volume created on the target pool; one that finished first, or was refused, leaves one usable root volume"""
        if self.hypervisor.lower() not in CANCELLABLE_HYPERVISORS:
            self.skipTest("Cancellation of hypervisor jobs is not implemented for %s" % self.hypervisor)

        vm = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)[0]
        root_volume = [v for v in Volume.list(self.apiclient, virtualmachineid=vm.id, listall=True) if v.type == "ROOT"][0]
        target_hosts = Host.listForMigration(self.apiclient, virtualmachineid=vm.id) or []
        target_pools = StoragePool.listForMigration(self.apiclient, id=root_volume.id) or []
        if not target_hosts or not target_pools:
            self.skipTest("Need a second host and a second primary storage pool to migrate to")

        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()
        cmd.virtualmachineid = vm.id
        cmd.hostid = target_hosts[0].id
        cmd.migrateto = [{"volume": str(root_volume.id), "pool": str(target_pools[0].id)}]
        cmd.isAsync = "false"
        jobid = self.apiclient.migrateVirtualMachineWithVolume(cmd, method="GET").jobid

        # let the migration create its target volume and start copying before cancelling
        time.sleep(10)
        cancelled = True
        try:
            self.cancel_job(jobid)
        except Exception as e:
            self.debug("Cancel of storage migration job %s refused: %s" % (jobid, e))
            cancelled = False

        job = self.wait_for_job(jobid)
        stopped = self.work_was_stopped(job, cancelled)

        response = self.virtual_machine.getState(self.apiclient, VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        # the rollback runs after the job is reported cancelled; give it a moment
        deadline = time.time() + 120
        while True:
            volumes = Volume.list(self.apiclient, virtualmachineid=vm.id, listall=True) or []
            roots = [v for v in volumes if v.type == "ROOT"]
            if len(roots) == 1 and roots[0].state != "Destroy" or time.time() > deadline:
                break
            time.sleep(5)
        self.assertEqual(len(roots), 1, "A volume created for the cancelled migration was left behind: %s"
                         % [(v.id, v.state, v.storageid) for v in roots])
        self.assertNotEqual(roots[0].state, "Destroy", "The instance's root volume is left in Destroy")
        if stopped:
            self.assertEqual(roots[0].id, root_volume.id, "Cancelled migration replaced the root volume")
            self.assertEqual(roots[0].storageid, root_volume.storageid, "Cancelled migration moved the root volume")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_10_cancel_of_power_operation_is_refused(self):
        """A stop's command cannot be stopped: the cancel is refused while it runs, or accepted only when nothing of the stop was in flight"""
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.virtual_machine.id
        cmd.isAsync = "false"
        jobid = self.apiclient.stopVirtualMachine(cmd, method="GET").jobid

        try:
            cancelled = True
            try:
                self.cancel_job(jobid)
            except Exception as e:
                self.debug("Cancel of stop job %s refused: %s" % (jobid, e))
                cancelled = False

            job = self.wait_for_job(jobid)
            if cancelled:
                # the cancel landed before the stop command went out (a stop takes milliseconds on the simulator): either nothing ran, or it finished regardless
                self.work_was_stopped(job, cancelled)
            else:
                self.assertEqual(job.jobstatus, JOB_SUCCEEDED, "Stop did not complete after its cancel was refused")
                response = self.virtual_machine.getState(self.apiclient, VirtualMachine.STOPPED)
                self.assertEqual(response[0], PASS, response[1])
        finally:
            if VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)[0].state != VirtualMachine.RUNNING:
                self.start_instance_again()

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_11_cancelled_offline_volume_migration_keeps_the_volume(self):
        """Cancelling an offline volume migration leaves the volume Ready on its source pool; one that finished first, or was refused, leaves it Ready on the target"""
        if self.hypervisor.lower() not in CANCELLABLE_HYPERVISORS:
            self.skipTest("Cancellation of hypervisor jobs is not implemented for %s" % self.hypervisor)

        root_volume = [v for v in Volume.list(self.apiclient, virtualmachineid=self.virtual_machine.id, listall=True) if v.type == "ROOT"][0]
        self.virtual_machine.stop(self.apiclient)
        try:
            target_pools = StoragePool.listForMigration(self.apiclient, id=root_volume.id) or []
            if not target_pools:
                self.skipTest("Need a second primary storage pool to migrate to")

            cmd = migrateVolume.migrateVolumeCmd()
            cmd.volumeid = root_volume.id
            cmd.storageid = target_pools[0].id
            cmd.isAsync = "false"
            jobid = self.apiclient.migrateVolume(cmd, method="GET").jobid

            time.sleep(5)
            cancelled = True
            try:
                self.cancel_job(jobid)
            except Exception as e:
                self.debug("Cancel of volume migration job %s refused: %s" % (jobid, e))
                cancelled = False

            job = self.wait_for_job(jobid)
            stopped = self.work_was_stopped(job, cancelled)
            self.assertNotIn("NullPointerException", str(job.jobresult), "The cancel surfaced as a server error")

            volume = Volume.list(self.apiclient, id=root_volume.id)[0]
            self.assertEqual(volume.state, "Ready", "Volume left in state %s after the migration job ended" % volume.state)
            if stopped:
                self.assertEqual(volume.storageid, root_volume.storageid, "Cancelled migration moved the volume")
        finally:
            self.start_instance_again()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_12_cancelled_volume_creation_leaves_no_volume(self):
        """A cancelled createVolume leaves no volume behind; a refused cancel leaves a usable one"""
        pools = [p for p in (StoragePool.list(self.apiclient, zoneid=self.zone.id) or []) if p.state == "Up"]
        if not pools:
            self.skipTest("No primary storage pool in zone %s" % self.zone.id)

        cmd = createVolume.createVolumeCmd()
        cmd.name = "cancelled-" + str(uuid.uuid4())[:8]
        cmd.zoneid = self.zone.id
        cmd.diskofferingid = self.disk_offering.id
        cmd.storageid = pools[0].id
        cmd.isAsync = "false"
        response = self.apiclient.createVolume(cmd, method="GET")
        jobid, volumeid = response.jobid, response.id

        cancelled = True
        try:
            self.cancel_job(jobid)
        except Exception as e:
            self.debug("Cancel of create volume job %s refused: %s" % (jobid, e))
            cancelled = False

        job = self.wait_for_job(jobid)
        stopped = self.work_was_stopped(job, cancelled)
        volumes = Volume.list(self.apiclient, id=volumeid, listall=True) or []
        if stopped:
            self.assertFalse(volumes, "Cancelled creation left volume %s in state %s" % (volumeid, volumes[0].state if volumes else None))
            # the usage record opened at allocation must be closed, or never opened
            usage_events = [row[0] for row in (self.dbclient.execute(
                "select type from cloud.usage_event where resource_name = '%s' order by id" % cmd.name) or [])]
            self.assertIn(usage_events, [[], ["VOLUME.CREATE", "VOLUME.DELETE"]],
                          "Cancelled creation left usage events %s for volume %s" % (usage_events, volumeid))
        else:
            self.assertNotEqual([v.state for v in volumes], ["Destroy"], "Volume %s left in Destroy" % volumeid)
            if volumes and volumes[0].state == "Ready":
                Volume(volumes[0].__dict__).delete(self.apiclient)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_13_cancelled_volume_snapshot_leaves_nothing(self):
        """Cancelling a volume snapshot during its backup leaves no snapshot behind; one that finished first, or was refused, completes"""
        if self.hypervisor.lower() not in CANCELLABLE_HYPERVISORS:
            self.skipTest("Cancellation of hypervisor jobs is not implemented for %s" % self.hypervisor)

        root_volume = [v for v in Volume.list(self.apiclient, virtualmachineid=self.virtual_machine.id, listall=True) if v.type == "ROOT"][0]
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = root_volume.id
        cmd.isAsync = "false"
        response = self.apiclient.createSnapshot(cmd, method="GET")
        jobid, snapshotid = response.jobid, response.id

        # let the backup to secondary storage start before cancelling
        time.sleep(5)
        cancelled = True
        try:
            self.cancel_job(jobid)
        except Exception as e:
            self.debug("Cancel of snapshot job %s refused: %s" % (jobid, e))
            cancelled = False

        job = self.wait_for_job(jobid, timeout=1800)
        stopped = self.work_was_stopped(job, cancelled)
        snapshots = Snapshot.list(self.apiclient, id=snapshotid, listall=True) or []
        if stopped:
            self.assertFalse(snapshots, "Cancelled snapshot %s left behind in state %s" % (snapshotid, snapshots[0].state if snapshots else None))
        else:
            self.assertNotEqual([s.state for s in snapshots], ["BackingUp"], "Snapshot %s stuck in BackingUp" % snapshotid)
            if snapshots and snapshots[0].state == "BackedUp":
                Snapshot(snapshots[0].__dict__).delete(self.apiclient)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_14_list_usage_jobs(self):
        """Usage server jobs list without error, and filter by usage server"""
        cmd = listUsageJobs.listUsageJobsCmd()
        cmd.duration = 24
        self.apiclient.listUsageJobs(cmd)

        cmd = listUsageJobs.listUsageJobsCmd()
        cmd.usageserver = "no-such-usage-server"
        self.assertFalse(self.apiclient.listUsageJobs(cmd), "Jobs listed for a usage server that does not exist")
