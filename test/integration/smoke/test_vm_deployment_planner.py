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
""" BVT tests for CM Deployment Planner
"""
# Import Local Modules
from marvin.cloudstackAPI import (deployVirtualMachine, destroyVirtualMachine)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             Host, Pod, Cluster)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr

class TestVMDeploymentPlanner(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestVMDeploymentPlanner, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )

        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]

    @classmethod
    def tearDownClass(cls):
        super(TestVMDeploymentPlanner, cls).tearDownClass()

    def deploy_vm(self, destination_id):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        template = get_template(
            self.apiclient,
            self.zone.id,
            hypervisor=self.hypervisor
        )
        cmd.zoneid = self.zone.id
        cmd.templateid = template.id
        cmd.serviceofferingid = self.service_offering.id
        cmd.hostid = destination_id
        return self.apiclient.deployVirtualMachine(cmd)

    def destroy_vm(self, vm_id):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.expunge = True
        cmd.id = vm_id
        return self.apiclient.destroyVirtualMachine(cmd)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_01_deploy_vm_on_specific_host(self):
        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing'
        )
        target_id = hosts[0].id

        vm = self.deploy_vm(target_id)

        self.assertEqual(
            target_id,
            vm.hostid,
            "VM instance was not deployed on target host ID")

        self.destroy_vm(vm.id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_02_deploy_vm_on_specific_cluster(self):

        # Select deployment cluster
        clusters = Cluster.list(
            self.apiclient,
        )
        target_cluster = clusters[0]
        target_id = target_cluster.id
        cluster_hypervisor = target_cluster.hypervisortype

        template = get_template(
            self.apiclient,
            hypervisor=cluster_hypervisor
        )

        # deploy vm on cluster
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.zoneid = target_cluster.zoneid
        cmd.serviceofferingid = self.service_offering.id
        cmd.templateid = template.id
        cmd.clusterid = target_id
        vm = self.apiclient.deployVirtualMachine(cmd)

        vm_host = Host.list(self.apiclient,
                            id=vm.hostid
                            )

        self.assertEqual(
            target_id,
            vm_host[0].clusterid,
            "VM was not deployed on the provided cluster"
        )
        self.destroy_vm(vm.id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_03_deploy_vm_on_specific_pod(self):
        pods = Pod.list(
            self.apiclient,
        )
        target_pod = pods[0]

        # Get host by Pod ID
        host = Host.list(
            self.apiclient,
            podid=target_pod.id)

        # deploy vm on pod
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.zoneid = target_pod.zoneid
        cmd.serviceofferingid = self.service_offering.id

        template = get_template(
            self.apiclient,
            hypervisor=host[0].hypervisortype
        )

        cmd.templateid = template.id
        cmd.podid = target_pod.id
        vm = self.apiclient.deployVirtualMachine(cmd)

        vm_host = Host.list(self.apiclient,
                            id=vm.hostid
                            )

        self.assertEqual(
            target_pod.id,
            vm_host[0].podid,
            "VM was not deployed on the target pod"
        )
        self.destroy_vm(vm.id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_04_deploy_vm_on_host_override_pod_and_cluster(self):

        # Optional parameters pod, cluster and host
        pod = Pod.list(self.apiclient, zoneid=self.zone.id)[0]
        clusters = Cluster.list(self.apiclient, zoneid=self.zone.id, podid=pod.id)

        self.assertEqual(
            isinstance(clusters, list),
            True,
            "Check list response returns a valid list"
        )

        host = Host.list(self.apiclient, zoneid=self.zone.id, clusterid=clusters[0].id, type='Routing')[0]

        cmd = deployVirtualMachine.deployVirtualMachineCmd()

        # Required parameters
        cmd.zoneid = self.zone.id
        cmd.serviceofferingid = self.service_offering.id
        template = get_template(self.apiclient, zone_id=self.zone.id, hypervisor=host.hypervisor)
        cmd.templateid = template.id

        # Add optional deployment params
        cmd.podid = pod.id
        cmd.clusterid = clusters[1].id if len(clusters) > 1 else clusters[0].id
        cmd.hostid = host.id

        vm = self.apiclient.deployVirtualMachine(cmd)

        self.assertEqual(
            vm.hostid,
            host.id,
            "VM was not deployed on the target host ID"
        )

        self.destroy_vm(vm.id)

    @attr(tags=["advanced", "advancedns", "ssh", "smoke"], required_hardware="false")
    def test_05_deploy_vm_on_cluster_override_pod(self):

        # Optional parameters pod, cluster and host
        pod = Pod.list(self.apiclient, zoneid=self.zone.id)[0]
        clusters = Cluster.list(self.apiclient, zoneid=self.zone.id, podid=pod.id)

        self.assertEqual(
            isinstance(clusters, list),
            True,
            "Check list response returns a valid list"
        )

        cmd = deployVirtualMachine.deployVirtualMachineCmd()

        # Required parameters
        cmd.zoneid = self.zone.id
        cmd.serviceofferingid = self.service_offering.id
        template = get_template(self.apiclient, zone_id=self.zone.id, hypervisor=clusters[0].hypervisortype)
        cmd.templateid = template.id

        # Add optional deployment params
        cmd.podid = pod.id
        cmd.clusterid = clusters[0].id

        vm = self.apiclient.deployVirtualMachine(cmd)

        vm_host = Host.list(self.apiclient,
                            id=vm.hostid
                            )

        self.assertEqual(
            vm_host[0].clusterid,
            clusters[0].id,
            "VM was not deployed on the target cluster"
        )

        self.destroy_vm(vm.id)
