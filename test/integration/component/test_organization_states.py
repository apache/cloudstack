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
""" 

Test cases relating to enabling/diabling of zone/pod/cluster/host

"""
# Import System modules
# Import System modules
import traceback

from marvin.cloudstackAPI import *
from marvin.cloudstackAPI import *
# Import Local Modules
from marvin.cloudstackTestCase import *
# Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.lib.base import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.common import *
from marvin.lib.utils import *
from marvin.lib.utils import *
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestOrganizationStates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls.testclient = super(TestOrganizationStates, cls).getClsTestClient()
            cls.apiclient = cls.testclient.getApiClient()
            cls.testdata = cls.testClient.getParsedTestDataConfig()
            cls.cleanup = []
            zone = get_zone(cls.apiclient, cls.testclient.getZoneForTests())
            cls.zone = Zone(zone.__dict__)
            cls.template = get_template(cls.apiclient, cls.zone.id, cls.testdata["ostype"])

            hostList = Host.list(cls.apiclient, zoneid=cls.zone.id, type="routing")
            cls.host = Host(hostList[0].__dict__)

            clusterList = Cluster.list(cls.apiclient, id=hostList[0].clusterid)
            cls.cluster = Cluster(clusterList[0].__dict__)

            podList = Pod.list(cls.apiclient, id=hostList[0].podid)
            cls.pod = Pod(podList[0].__dict__)

            cls.serviceOffering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
                hosttags="test"
            )

            hostupdResp = Host.update(cls.apiclient,
                                      id=cls.host.id,
                                      hosttags="test")

            userAccountName = "-".join(("TestOrgUser", random_gen()))
            adminAccountName = "-".join(("TestOrgAdmin", random_gen()))

            cls.user_apiclient = cls.testclient.getUserApiClient(
                UserName=userAccountName,
                DomainName="ROOT"
            )

            cls.admin_apiclient = cls.testclient.getUserApiClient(
                UserName=adminAccountName,
                DomainName="ROOT",
                type=1
            )

            accountList = Account.list(
                cls.apiclient,
                name=userAccountName,
                listAll="true"
            )
            cls.account = Account(accountList[0].__dict__)

            accountList = Account.list(
                cls.apiclient,
                name=adminAccountName,
                listAll="true"
            )
            cls.adminAccount = Account(accountList[0].__dict__)

            cls.cleanup = [
                cls.account,
                cls.adminAccount,
                cls.serviceOffering
            ]

            cls.vm_admin = VirtualMachine.create(
                cls.admin_apiclient,
                {},
                zoneid=cls.zone.id,
                serviceofferingid=cls.serviceOffering.id,
                templateid=cls.template.id
            )

            cls.vm_user = VirtualMachine.create(
                cls.user_apiclient,
                {},
                zoneid=cls.zone.id,
                serviceofferingid=cls.serviceOffering.id,
                templateid=cls.template.id
            )

        except Exception as e:
            printex = traceback.format_exc()
            cls.debug("Exception Occurred : {0}".format(printex))
            cleanup_resources(cls.apiclient, cls.cleanup)
            raise Exception("Failed to create the setup required to execute the test cases: %s" % e)

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestOrganizationStates, cls).getClsTestClient().getApiClient()
        hostupdResp = Host.update(cls.apiclient,
                                  id=cls.host.id,
                                  hosttags="")
        cleanup_resources(cls.apiclient, cls.cleanup)
        return

    def setUp(cls):
        return

    def tearDown(cls):
        return

    ## Test cases relating to disabling and enabling zone

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_11_disableZone(self):
        """
        Disable Zone
            Validate that listZones() returns the  allocationstate as "Disabled"
        """
        self.debug("Zone to be disabled: " + self.zone.id)

        zoneupdResp = self.zone.update(self.apiclient, allocationstate="Disabled")

        self.assertEqual(zoneupdResp.allocationstate,
                         "Disabled",
                         "Disabling Zone did not set the alloctionstate to Disabled")

        zonelistResp = Zone.list(self.apiclient, id=self.zone.id)

        self.assertEqual(zonelistResp[0].allocationstate,
                         "Disabled",
                         "Disabling Zone did not set the alloctionstate to Disabled")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_12_disableZone_admin_deployVM(self):
        """
            Validate that admin is allowed to deploy VM in a disabled zone
        """
        vm = VirtualMachine.create(
            self.admin_apiclient,
            {},
            zoneid=self.zone.id,
            serviceofferingid=self.serviceOffering.id,
            templateid=self.template.id
        )

        self.assertEqual(vm.state,
                         "Running",
                         "Admin is not able to deploy Vm in a disabled Zone! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_13_disableZone_admin_stop_startVM(self):
        """
            Validate that admin is allowed to stop and start existing VMs that are running on a disabled zone
        """
        self.vm_admin.stop(self.apiclient)

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Admin is not able to Stop Vm in a disabled Zone! ")

        self.vm_admin.start(self.apiclient)

        listResp = VirtualMachine.list(self.admin_apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Admin is not able to Stop Vm in a disabled Zone! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_14_disableZone_user_deployVM(self):
        """
            Validate that regular user is not allowed to deploy VM in a disabled zone
        """
        try:
            vm = VirtualMachine.create(
                self.user_apiclient,
                {},
                zoneid=self.zone.id,
                serviceofferingid=self.serviceOffering.id,
                templateid=self.template.id
            )
            self.fail("Regular user is allowed to deploy VM in a zone that is disabled")
        except Exception as e:
            self.debug("Exception thrown when deploying Virtual Machine on a disabled zone -  %s" % e)

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_15_disableZone_user_stop_startVM(self):
        """
            Validate that regular user is allowed to stop and start existing VMs in a disabled zone
        """
        self.vm_user.stop(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Regular user is not able to Stop Vm in a disabled Zone! ")

        self.vm_user.start(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Regular is not able to Stop Vm in a disabled Zone! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_16_enableZone(self):
        """
        Enable Zone that is diabled
            Validate that listZones() returns the  allocationstate as "Enabled"
        """
        self.debug("Zone to be enabled: " + self.zone.id)

        zoneupdResp = self.zone.update(self.apiclient, allocationstate="Enabled")

        self.assertEqual(zoneupdResp.allocationstate,
                         "Enabled",
                         "Enabling Zone did not set the alloctionstate to Enabled")

        zonelistResp = Zone.list(self.apiclient, id=self.zone.id)

        self.assertEqual(zonelistResp[0].allocationstate,
                         "Enabled",
                         "Enabling Zone did not set the alloctionstate to Enabled")

    ## Test cases relating to disabling and enabling pod

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_21_disablePod(self):
        """
            Disable Pod
            Validate that listPods() returns the  allocationstate as "Disabled"
        """
        self.debug("Pod to be disabled: " + self.zone.id)

        podupdResp = self.pod.update(self.apiclient, allocationstate="Disabled", id=self.pod.id)

        self.assertEqual(podupdResp.allocationstate,
                         "Disabled",
                         "Disabling Pod did not set the alloctionstate to Disabled")

        podlistResp = Pod.list(self.apiclient, id=self.pod.id)

        self.assertEqual(podlistResp[0].allocationstate,
                         "Disabled",
                         "Disabling Pod did not set the alloctionstate to Disabled")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_22_disablePod_admin_deployVM(self):
        """
            Validate that admin is allowed to deploy VM in a disabled pod
        """
        vm = VirtualMachine.create(
            self.admin_apiclient,
            {},
            zoneid=self.zone.id,
            serviceofferingid=self.serviceOffering.id,
            templateid=self.template.id
        )

        self.assertEqual(vm.state,
                         "Running",
                         "Admin is not able to deploy Vm in a disabled Pod! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_23_disablePod_admin_stop_startVM(self):
        """
            Validate that admin is allowed to stop and start existing VMs running in a disabled pod
        """
        self.vm_admin.stop(self.admin_apiclient)

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Admin is not able to Stop Vm in a disabled Pod! ")

        self.vm_admin.start(self.admin_apiclient)

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Admin is not able to Stop Vm in a disabled Pod! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_24_disablePod_user_deployVM(self):
        """
            Validate that regular user is not allowed to deploy VM in a disabled pod
        """
        try:
            vm = VirtualMachine.create(
                self.user_apiclient,
                {},
                zoneid=self.zone.id,
                serviceofferingid=self.serviceOffering.id,
                templateid=self.template.id
            )
            self.fail("Regular user is allowed to deploy VM in a zone that is disabled")
        except Exception as e:
            self.debug("Exception thrown when deploying Virtual Machine on a disabled zone -  %s" % e)

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_25_disablePod_user_stop_startVM(self):
        """
            Validate that regular user is allowed to stop and start existing VMs runing in a disabled pod
        """
        self.vm_user.stop(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Regular user is not able to Stop Vm in a disabled Pod! ")

        self.vm_user.start(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Regular is not able to Stop Vm in a disabled Pod! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_26_enablePod(self):
        """
        Enable Pod that is diabled
            Validate that listPods() returns the  allocationstate as "Enabled"
        """
        self.debug("Pod to be enabled: " + self.zone.id)

        podupdResp = self.pod.update(self.apiclient, allocationstate="Enabled", id=self.pod.id)

        self.assertEqual(podupdResp.allocationstate,
                         "Enabled",
                         "Enabling Pod did not set the alloctionstate to Enabled")

        podlistResp = Pod.list(self.apiclient, id=self.pod.id)

        self.assertEqual(podlistResp[0].allocationstate,
                         "Enabled",
                         "Enabling Pod did not set the alloctionstate to Enabled")

    ## Test cases relating to disabling and enabling cluster

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_31_disableCluster(self):
        """
        Disable Cluster
            Validate that listClusters() returns the  allocationstate as "Disabled"
        """
        self.debug("Cluster to be disabled: " + self.cluster.id)

        clusterupdResp = self.cluster.update(self.apiclient, allocationstate="Disabled", id=self.cluster.id)

        self.assertEqual(clusterupdResp.allocationstate,
                         "Disabled",
                         "Disabling Cluster did not set the alloctionstate to Disabled")

        clusterlistResp = Cluster.list(self.apiclient, id=self.cluster.id)

        self.assertEqual(clusterlistResp[0].allocationstate,
                         "Disabled",
                         "Disabling Cluster did not set the alloctionstate to Disabled")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_32_disableCluster_admin_deployVM(self):
        """
            Validate that admin is allowed to deploy VM in a disabled cluster
        """
        vm = VirtualMachine.create(
            self.admin_apiclient,
            {},
            zoneid=self.zone.id,
            serviceofferingid=self.serviceOffering.id,
            templateid=self.template.id
        )

        self.assertEqual(vm.state,
                         "Running",
                         "Admin is not able to deploy Vm in a disabled Cluster! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_33_disableCluster_admin_stop_startVM(self):
        """
            Validate that admin is allowed to stop and start existing VMs that are running in a disabled cluster
        """
        self.vm_admin.stop(self.admin_apiclient)

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Admin is not able to Stop Vm in a disabled Cluster! ")

        self.vm_admin.start(self.admin_apiclient)

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Admin is not able to Stop Vm in a disabled Cluster! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_34_disableCluster_user_deployVM(self):
        """
            Validate that regular user is not allowed to deploy VM in a disabled cluster
        """
        try:
            vm = VirtualMachine.create(
                self.user_apiclient,
                {},
                zoneid=self.zone.id,
                serviceofferingid=self.serviceOffering.id,
                templateid=self.template.id
            )
            self.fail("Regular user is allowed to deploy VM in a cluster that is disabled")
        except Exception as e:
            self.debug("Exception thrown when deploying Virtual Machine on a disabled cluster -  %s" % e)

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_35_disableCluster_user_stop_startVM(self):
        """
            Validate that regular user is allowed to stop and start existing VMs that are running in a disabled cluster
        """
        self.vm_user.stop(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Regular user is not able to Stop Vm in a disabled Cluster! ")

        self.vm_user.start(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Regular is not able to Stop Vm in a disabled Cluster! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_36_enableCluster(self):
        """
        Enable Cluster that is diabled
            Validate that listClusters() returns the  allocationstate as "Enabled"
        """
        self.debug("Cluster to be enabled: " + self.cluster.id)

        clusterupdResp = self.cluster.update(self.apiclient, allocationstate="Enabled", id=self.cluster.id)

        self.assertEqual(clusterupdResp.allocationstate,
                         "Enabled",
                         "Enabling Cluster did not set the alloctionstate to Enabled")

        clusterlistResp = Cluster.list(self.apiclient, id=self.cluster.id)

        self.assertEqual(clusterlistResp[0].allocationstate,
                         "Enabled",
                         "Enabling Cluster did not set the alloctionstate to Enabled")

    ## Test cases relating to disabling and enabling host

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_41_disableHost(self):
        """
        Disable Host
            Validate that listHosts() returns the  allocationstate as "Disabled"
        """
        self.debug("Host to be disabled: " + self.host.id)

        hostupdResp = Host.update(self.apiclient, id=self.host.id, allocationstate="Disable")

        self.assertEqual(hostupdResp.resourcestate,
                         "Disabled",
                         "Disabling Host did not set the alloctionstate to Disabled")

        hostlistResp = Host.list(self.apiclient, id=self.host.id)

        self.assertEqual(hostlistResp[0].resourcestate,
                         "Disabled",
                         "Disabling Host did not set the alloctionstate to Disabled")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_42_disableHost_admin_deployVM(self):
        """
            Validate that admin is allowed to deploy VM in a disabled host by passing hostId parameter
        """
        vm = VirtualMachine.create(
            self.admin_apiclient,
            {},
            zoneid=self.zone.id,
            serviceofferingid=self.serviceOffering.id,
            templateid=self.template.id,
            hostid=self.host.id
        )

        self.assertEqual(vm.state,
                         "Running",
                         "Admin is not able to deploy Vm in a disabled Host! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_43_disableHost_admin_deployVM(self):
        """
            Validate that admin is allowed to deploy VM in a disabled host without passing hostId parameter
        """
        try:
            vm = VirtualMachine.create(
                self.admin_apiclient,
                {},
                zoneid=self.zone.id,
                serviceofferingid=self.serviceOffering.id,
                templateid=self.template.id
            )
        except Exception:
            raise self.fail("Failed to deploy VM, this issue was hit: https://issues.apache.org/jira/browse/CLOUDSTACK-7735")

        self.assertEqual(vm.state,
                         "Running",
                         "Admin is not able to deploy Vm in a disabled Host! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_44_disableHost_admin_stop_startVM(self):
        """
            Validate that admin is allowed to stop and start existing VMs running in a disabled host
        """
        self.vm_admin.stop(self.admin_apiclient)

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Admin is not able to Stop Vm in a disabled Host! ")
        try:
            self.vm_admin.start(self.admin_apiclient)
        except Exception:
            raise self.fail("Failed to deploy VM, this issue was hit: https://issues.apache.org/jira/browse/CLOUDSTACK-7735")

        listResp = VirtualMachine.list(self.apiclient, id=self.vm_admin.id)
        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Admin is not able to Stop Vm in a disabled Host! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_45_disableHost_user_deployVM(self):
        """
            Validate that regular user is not allowed to deploy VM in a disabled host
        """
        try:
            vm = VirtualMachine.create(
                self.user_apiclient,
                {},
                zoneid=self.zone.id,
                serviceofferingid=self.serviceOffering.id,
                templateid=self.template.id
            )
            self.fail("Regular user is allowed to deploy VM in a host that is disabled")
        except Exception as e:
            self.debug("Exception thrown when deploying Virtual Machine on a disabled host -  %s" % e)

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_46_disableHost_user_stop_startVM(self):
        """
            Validate that regular user is allowed to stop and start existing VMs running in a disabled host
        """
        self.vm_user.stop(self.user_apiclient)
        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.STOPPED,
                         "Regular user is not able to Stop Vm in a disabled Host! ")
        try:
            self.vm_user.start(self.user_apiclient)
        except Exception:
            raise self.fail("Failed to deploy VM, this issue was hit: https://issues.apache.org/jira/browse/CLOUDSTACK-7735")

        listResp = VirtualMachine.list(self.user_apiclient, id=self.vm_user.id)

        self.assertEqual(listResp[0].state,
                         VirtualMachine.RUNNING,
                         "Regular is not able to Stop Vm in a disabled Host! ")

    @attr("disruptive", "simulator_only", tags=["advanced"], required_hardware="false")
    def test_47_enableHost(self):
        """
        Enable Host that is diabled
            Validate that listHosts() returns the  allocationstate as "Enabled"
        """
        self.debug("Host to be enabled: " + self.host.id)

        hostupdResp = Host.update(self.apiclient, id=self.host.id, allocationstate="Enable")

        self.assertEqual(hostupdResp.resourcestate,
                         "Enabled",
                         "Enabling Host did not set the alloctionstate to Enabled")

        hostlistResp = Host.list(self.apiclient, id=self.host.id)

        self.assertEqual(hostlistResp[0].resourcestate,
                         "Enabled",
                         "Enabling Host did not set the alloctionstate to Enabled")
