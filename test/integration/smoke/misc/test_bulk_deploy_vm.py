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

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries
from marvin.cloudstackAPI import (bulkDeployVirtualMachine,
                                  queryAsyncJobResult)
from marvin.codes import (FAILED, JOB_FAILED,
                          JOB_CANCELLED, JOB_SUCCEEDED)
# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import (Account, Cluster, Host, Pod,
                             ServiceOffering, VirtualMachine)
# utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources
# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template

from nose.plugins.attrib import attr
import time


class TestBulkDeployVM(cloudstackTestCase):
    """Test bulk VM deploy into user account
    """

    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()

        # Get zone, domain and default built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        # create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        # create a service offering
        self.testdata["service_offerings"]["tiny"]["deploymentplanner"] = "UserDispersingPlanner"
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["tiny"]
        )
        # build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account
        ]

    def query_async_job(self, jobid):
        """Query the status for Async Job"""
        try:
            async_timeout = 300
            cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
            cmd.jobid = jobid
            timeout = async_timeout
            async_response = FAILED
            while timeout > 0:
                async_response = self.apiclient.queryAsyncJobResult(cmd)
                if async_response != FAILED:
                    job_status = async_response.jobstatus
                    if job_status in [JOB_CANCELLED, JOB_SUCCEEDED]:
                        break
                    elif job_status == JOB_FAILED:
                        raise Exception("Job failed: %s" % async_response)
                time.sleep(5)
                timeout -= 5
            return async_response
        except Exception as e:
            return FAILED

    def bulk_deploy_vm(self, clusters, clusterid=None, podid=None):
        """Test bulk deploy of VM
        """

        self.assertTrue(isinstance(clusters, list) and len(clusters) > 0, msg="No clusters present")

        vm_count = len(clusters) * 2 - 1
        avg_vm_count_per_cluster = (vm_count + 1)/(len(clusters))

        cmd = bulkDeployVirtualMachine.bulkDeployVirtualMachineCmd()
        cmd.serviceofferingid = self.service_offering.id
        cmd.templateid = self.template.id
        cmd.virtualmachinecount = vm_count
        cmd.zoneid = self.zone.id
        cmd.account = self.account.name
        cmd.domainid = self.account.domainid
        if clusterid is not None:
            cmd.clusterid = clusterid
        if podid is not None:
            cmd.podid = podid

        jobs = self.apiclient.bulkDeployVirtualMachine(cmd)
        self.assertTrue(isinstance(jobs, list) and len(jobs) > 0, msg="Bulk VM deploy response is empty")
        self.assertEqual(len(jobs), vm_count, msg="Required number of VM creation jobs not scheduled")
        for job in jobs:
            self.query_async_job(job.jobid)

        for cluster in clusters:
            cluster_vm_count = 0
            hosts = Host.list(self.apiclient,
                              clusterid=cluster.id)
            for host in hosts:
                vms = VirtualMachine.list(self.apiclient,
                                          account=self.account.name,
                                          domainid=self.account.domainid,
                                          hostid=host.id)
                if isinstance(vms, list):
                    cluster_vm_count += len(vms)
            self.assertTrue(avg_vm_count_per_cluster-1 <= cluster_vm_count <= avg_vm_count_per_cluster+1,
                            "VM distribution is not proper")

    @attr(tags=['advanced', 'basic'], required_hardware="false")
    def test_bulk_deploy_vm(self):
        """Test bulk deploy of VM
        """

        clusters = Cluster.list(self.apiclient)
        self.bulk_deploy_vm(clusters)

    @attr(tags=['advanced', 'basic'], required_hardware="false")
    def test_bulk_deploy_vm_with_cluster(self):
        """Test bulk deploy of VM in specific cluster
        """

        clusters = Cluster.list(self.apiclient)
        self.assertTrue(isinstance(clusters, list) and len(clusters) > 0, msg="No clusters present")
        cluster = clusters[0]
        self.bulk_deploy_vm([cluster], clusterid=cluster.id)

    def test_bulk_deploy_vm_with_pod(self):
        """Test bulk deploy of VM in specific pod
        """

        pods = Pod.list(self.apiclient)
        self.assertTrue(isinstance(pods, list) and len(pods) > 0, msg="No pods present")
        pod = pods[0]
        clusters = Cluster.list(self.apiclient, podid=pod.id)
        self.bulk_deploy_vm(clusters, clusterid=None, podid=pod.id)

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
