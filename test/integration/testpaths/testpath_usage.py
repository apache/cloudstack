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
""" Test cases for Usage Test Path
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              verifyRouterState,
                              get_process_status)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Template,
                             Iso,
                             DiskOffering,
                             Volume,
                             Snapshot,
                             PublicIPAddress,
                             LoadBalancerRule,
                             EgressFireWallRule,
                             Router,
                             VmSnapshot,
                             Usage,
                             Configurations,
                             FireWallRule,
                             NATRule,
                             StaticNATRule,
                             Network,
                             Vpn,
                             VpnUser,
                             VpcOffering,
                             VPC,
                             NetworkACL)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               createEnabledNetworkOffering,
                               get_builtin_template_info,
                               findSuitableHostForMigration,
                               list_hosts,
                               list_volumes,
                               list_routers)
from marvin.codes import (PASS, FAIL, ERROR_NO_HOST_FOR_MIGRATION)
from marvin.sshClient import SshClient
import time


def CreateEnabledNetworkOffering(apiclient, networkServices):
    """Create network offering of given services and enable it"""

    result = createEnabledNetworkOffering(apiclient, networkServices)
    assert result[0] == PASS,\
        "Network offering creation/enabling failed due to %s" % result[2]
    return result[1]


class TestUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestUsage, cls).getClsTestClient()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls._cleanup = []
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        isUsageJobRunning = cls.IsUsageJobRunning()
        cls.usageJobNotRunning = False
        if not isUsageJobRunning:
            cls.usageJobNotRunning = True
            return

        if cls.testdata["configurableData"][
                "setUsageConfigurationThroughTestCase"]:
            cls.setUsageConfiguration()
            cls.RestartServers()
        else:
            currentMgtSvrTime = cls.getCurrentMgtSvrTime()
            dateTimeSplit = currentMgtSvrTime.split("/")
            cls.curDate = dateTimeSplit[0]

        cls.hypervisor = testClient.getHypervisorInfo()

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        try:
            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenable:
                cls.testdata["service_offering"]["storagetype"] = 'local'

            # Create 2 service offerings with different values for
            # for cpunumber, cpuspeed, and memory

            cls.testdata["service_offering"]["cpunumber"] = "1"
            cls.testdata["service_offering"]["cpuspeed"] = "128"
            cls.testdata["service_offering"]["memory"] = "256"

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            cls.testdata["service_offering"]["cpunumber"] = "2"
            cls.testdata["service_offering"]["cpuspeed"] = "256"
            cls.testdata["service_offering"]["memory"] = "512"

            cls.service_offering_2 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering_2)

            # Create isolated network offering
            cls.isolated_network_offering = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["isolated_network_offering"]
            )
            cls._cleanup.append(cls.isolated_network_offering)

            cls.isolated_network_offering_2 = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["isolated_network_offering"]
            )
            cls._cleanup.append(cls.isolated_network_offering_2)

            cls.isolated_network_offering_vpc = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["nw_offering_isolated_vpc"]
            )
            cls._cleanup.append(cls.isolated_network_offering_vpc)

            cls.testdata["shared_network_offering_all_services"][
                "specifyVlan"] = "True"
            cls.testdata["shared_network_offering_all_services"][
                "specifyIpRanges"] = "True"

            cls.shared_network_offering = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["shared_network_offering_all_services"]
            )
            cls._cleanup.append(cls.shared_network_offering)

            configs = Configurations.list(
                cls.apiclient,
                name='usage.stats.job.aggregation.range'
            )

            # Set the value for one more minute than
            # actual range to be on safer side
            cls.usageJobAggregationRange = (
                int(configs[0].value) + 1) * 60  # in seconds
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        if self.usageJobNotRunning:
            self.skipTest("Skipping test because usage job not running")
        # Create an account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)
        # Create user api client of the account
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUsageConfiguration(cls):
        """ Set the configuration parameters so that usage job runs
            every 10 miuntes """

        Configurations.update(
            cls.apiclient,
            name="enable.usage.server",
            value="true"
        )

        Configurations.update(
            cls.apiclient,
            name="usage.aggregation.timezone",
            value="GMT"
        )

        Configurations.update(
            cls.apiclient,
            name="usage.execution.timezone",
            value="GMT"
        )

        Configurations.update(
            cls.apiclient,
            name="usage.stats.job.aggregation.range",
            value="10"
        )

        currentMgtSvrTime = cls.getCurrentMgtSvrTime()
        dateTimeSplit = currentMgtSvrTime.split("/")
        cls.curDate = dateTimeSplit[0]
        timeSplit = dateTimeSplit[1].split(":")
        minutes = int(timeSplit[1])
        minutes += 5
        usageJobExecTime = timeSplit[0] + ":" + str(minutes)

        Configurations.update(
            cls.apiclient,
            name="usage.stats.job.exec.time",
            value=usageJobExecTime
        )

        return

    @classmethod
    def getCurrentMgtSvrTime(cls, format='%Y-%m-%d/%H:%M'):
        """ Get the current time from Management Server """

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "date +%s" % format
        return sshClient.execute(command)[0]

    @classmethod
    def RestartServers(cls):
        """ Restart management server and usage server """

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)

        command = "service cloudstack-usage restart"
        sshClient.execute(command)
        return

    @classmethod
    def IsUsageJobRunning(cls):
        """ Check that usage job is running on Management server or not"""

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )

        command = "service cloudstack-usage status"
        response = str(sshClient.execute(command)).lower()
        if "running" not in response:
            return False
        return True

    def getLatestUsageJobExecutionTime(self):
        """ Get the end time of latest usage job that has run successfully"""

        try:
            qresultset = self.dbclient.execute(
                "SELECT max(end_date) FROM usage_job WHERE success=1;",
                db="cloud_usage")

            self.assertNotEqual(
                len(qresultset),
                0,
                "Check DB Query result set"
            )

            lastUsageJobExecutionTime = qresultset[0][0]
            self.debug(
                "last usage job exec time: %s" %
                lastUsageJobExecutionTime)
            return [PASS, lastUsageJobExecutionTime]
        except Exception as e:
            return [FAIL, e]

    def getEventCreatedDateTime(self, resourceName):
        """ Get the created date/time of particular entity
            from cloud_usage.usage_event table """

        try:
            # Checking exact entity creation time
            qresultset = self.dbclient.execute(
                "select created from usage_event where resource_name = '%s';" %
                str(resourceName), db="cloud_usage")
            self.assertNotEqual(
                len(qresultset),
                0,
                "Check DB Query result set"
            )
            eventCreatedDateTime = qresultset[0][0]
        except Exception as e:
            return [FAIL, e]
        return [PASS, eventCreatedDateTime]

    def listUsageRecords(self, usagetype, apiclient=None, startdate=None,
                         enddate=None, account=None, sleep=True):
        """List and return the usage record for given account
        and given usage type"""

        if sleep:
            # Sleep till usage job has run at least once after the operation
            self.debug(
                "Sleeping for %s seconds" %
                self.usageJobAggregationRange)
            time.sleep(self.usageJobAggregationRange)

        if not startdate:
            startdate = self.curDate
        if not enddate:
            enddate = self.curDate
        if not account:
            account = self.account
        if not apiclient:
            self.apiclient

        Usage.generateRecords(
            self.apiclient,
            startdate=startdate,
            enddate=enddate)

        try:
            usageRecords = Usage.listRecords(
                self.apiclient,
                startdate=startdate,
                enddate=enddate,
                account=account.name,
                domainid=account.domainid,
                type=usagetype)

            self.assertEqual(
                validateList(usageRecords)[0],
                PASS,
                "usage records list validation failed")

            return [PASS, usageRecords]
        except Exception as e:
            return [FAIL, e]
        return

    def getCommandResultFromRouter(self, router, command):
        """Run given command on router and return the result"""

        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'hyperv'):
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                command,
                hypervisor=self.hypervisor
            )
        else:
            hosts = list_hosts(
                self.apiclient,
                id=router.hostid,
            )
            self.assertEqual(
                isinstance(hosts, list),
                True,
                "Check for list hosts response return valid data"
            )
            host = hosts[0]
            host.user = self.testdata["configurableData"]["host"]["username"]
            host.passwd = self.testdata["configurableData"]["host"]["password"]

            result = get_process_status(
                host.ipaddress,
                22,
                host.user,
                host.passwd,
                router.linklocalip,
                command
            )
        return result

    @attr(tags=["advanced"], required_hardware="True")
    def test_01_positive_tests_usage(self):
        """ Positive test for usage test path

        # 1.  Register a template and verify that usage usage is generated
              for correct size of template
        # 2.  Register an ISO, verify usage is generate for the correct size
              of ISO
        # 3.  Deploy a VM from the template and verify usage is generated
              for the VM with correct Service Offering and template id
        # 4.  Delete template and iso
        # 5.  Stop and start the VM
        # 6.  Verify that allocated VM usage should be greater than
              running VM usage
        # 7.  Destroy the Vm and recover it
        # 8.  Verify that the running VM usage stays the same after delete and
              and after recover operation
        # 9.  Verify that allocated VM usage should be greater after recover
              operation than after destroy operation
        # 10. Change service offering of the VM
        # 11. Verify that VM usage is generated for the VM with correct
              service offering
        # 12. Start the VM
        # 13. Verify that the running VM usage after start operation is less
              than the allocated VM usage
        # 14. Verify that the running VM usage after start vm opearation
              is greater running VM usage after recover VM operation
        """

        # Step 1
        # Register a private template in the account
        builtin_info = get_builtin_template_info(
            self.apiclient,
            self.zone.id
        )

        self.testdata["privatetemplate"]["url"] = builtin_info[0]
        self.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        self.testdata["privatetemplate"]["format"] = builtin_info[2]

        # Register new template
        template = Template.register(
            self.userapiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(template)
        template.download(self.userapiclient)

        templates = Template.list(
            self.userapiclient,
            listall=True,
            id=template.id,
            templatefilter="self")

        self.assertEqual(
            validateList(templates)[0],
            PASS,
            "Templates list validation failed")

        # Checking template usage
        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = [record for record in response[1]
                                if template.id == record.usageid]

        self.assertEqual(templateUsageRecords[0].virtualsize,
                         templates[0].size,
                         "The template size in the usage record and \
                        does not match with the created template size")

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact template creation time
        response = self.getEventCreatedDateTime(template.name)
        self.assertEqual(response[0], PASS, response[1])
        templateCreatedDateTime = response[1]
        self.debug("Template creation date: %s" % templateCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - templateCreatedDateTime).total_seconds() / 3600),
            ".2f")

        actualUsage = format(sum(float(record.rawusage)
                                 for record in templateUsageRecords), ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # step 2
        iso = Iso.create(
            self.userapiclient,
            self.testdata["iso"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        self.cleanup.append(iso)

        iso.download(self.apiclient)

        isos = Iso.list(
            self.userapiclient,
            id=iso.id,
            listall=True)

        self.assertEqual(
            validateList(isos)[0],
            PASS,
            "Iso list validation failed"
        )

        # Checking usage for Iso
        response = self.listUsageRecords(usagetype=8)
        self.assertEqual(response[0], PASS, response[1])
        isoUsageRecords = [record for record in response[1]
                           if iso.id == record.usageid]

        self.assertEqual(isoUsageRecords[0].size,
                         isos[0].size,
                         "The iso size in the usage record and \
                        does not match with the created iso size")

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact Iso creation time
        response = self.getEventCreatedDateTime(iso.name)
        self.assertEqual(response[0], PASS, response[1])
        isoCreatedDateTime = response[1]
        self.debug("Iso creation date: %s" % isoCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - isoCreatedDateTime).total_seconds() / 3600),
            ".2f")

        actualUsage = format(sum(float(record.rawusage)
                                 for record in isoUsageRecords), ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # step 3
        # Create VM in account
        vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        # Checking running VM usage
        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmRunningUsageRecords = [record for record in response[1]
                                 if record.virtualmachineid == vm.id]

        vmRunningRawUsage = sum(float(record.rawusage)
                                for record in vmRunningUsageRecords)

        self.assertEqual(vmRunningUsageRecords[0].offeringid,
                         self.service_offering.id,
                         "The service offering id in the usage record\
                        does not match with id of service offering\
                        with which the VM was created")

        self.assertEqual(vmRunningUsageRecords[0].templateid,
                         template.id,
                         "The template id in the usage record\
                        does not match with id of template\
                        with which the VM was created")

        response = self.listUsageRecords(usagetype=2, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecords = [record for record in response[1]
                                   if record.virtualmachineid == vm.id]

        vmAllocatedRawUsage = sum(float(record.rawusage)
                                  for record in vmAllocatedUsageRecords)

        self.debug("running vm usage: %s" % vmRunningRawUsage)
        self.debug("allocated vm usage: %s" % vmAllocatedRawUsage)

        self.assertTrue(
            vmRunningRawUsage < vmAllocatedRawUsage,
            "Allocated VM usage should be greater than Running VM usage")

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact VM creation time
        response = self.getEventCreatedDateTime(vm.name)
        self.assertEqual(response[0], PASS, response[1])
        vmCreatedDateTime = response[1]
        self.debug("Vm creation date: %s" % vmCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - vmCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("VM expected usage: %s" % expectedUsage)

        actualUsage = format(vmAllocatedRawUsage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Step 4 - Deleting template and ISO
        template.delete(self.userapiclient)
        self.cleanup.remove(template)

        iso.delete(self.userapiclient)
        self.cleanup.remove(iso)

        # Verifying that usage for template and ISO is stopped
        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForTemplateAfterDeletion_1 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if template.id == record.usageid])

        response = self.listUsageRecords(usagetype=8, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        isoUsageRecords = response[1]

        usageForIsoAfterDeletion_1 = sum(
            float(
                record.rawusage) for record in [
                record for record in isoUsageRecords
                if iso.id == record.usageid])

        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForTemplateAfterDeletion_2 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if template.id == record.usageid])

        response = self.listUsageRecords(usagetype=8, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        isoUsageRecords = response[1]

        usageForIsoAfterDeletion_2 = sum(
            float(
                record.rawusage) for record in [
                record for record in isoUsageRecords
                if iso.id == record.usageid])

        self.assertTrue(usageForTemplateAfterDeletion_1 ==
                        usageForTemplateAfterDeletion_2,
                        "usage for template after deletion should remain the same\
                        after specific intervals of time")

        self.assertTrue(usageForIsoAfterDeletion_1 ==
                        usageForIsoAfterDeletion_2,
                        "usage for iso after deletion should remain the same\
                        after specific intervals of time")

        # Step 5
        vm.stop(self.userapiclient)

        # Sleep to get difference between allocated and running usage
        time.sleep(120)

        vm.start(self.userapiclient)

        # Step 6: Verifying allocated usage is greater than running usage
        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmRunningUsageRecords = [record for record in response[1]
                                 if record.virtualmachineid == vm.id]

        vmRunningRawUsage = sum(float(record.rawusage)
                                for record in vmRunningUsageRecords)

        response = self.listUsageRecords(usagetype=2, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecords = [record for record in response[1]
                                   if record.virtualmachineid == vm.id]

        vmAllocatedRawUsage = sum(float(record.rawusage)
                                  for record in vmAllocatedUsageRecords)

        self.debug("running vm usage: %s" % vmRunningRawUsage)
        self.debug("allocated vm usage: %s" % vmAllocatedRawUsage)

        self.assertTrue(
            vmRunningRawUsage < vmAllocatedRawUsage,
            "Allocated VM usage should be greater than Running VM usage")

        # Step 7
        vm.delete(self.userapiclient, expunge=False)

        response = self.listUsageRecords(usagetype=1, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmRunningUsageRecordAfterDestroy = sum(
            float(
                record.rawusage) for record in response[1] if
            record.virtualmachineid == vm.id)

        response = self.listUsageRecords(usagetype=2, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecordAfterDestroy = sum(
            float(
                record.rawusage) for record in response[1] if record.virtualmachineid == vm.id)

        vm.recover(self.apiclient)

        # Step 8
        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmRunningUsageRecordAfterRecover = sum(
            float(
                record.rawusage) for record in response[1] if
            record.virtualmachineid == vm.id)

        response = self.listUsageRecords(usagetype=2, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecordAfterRecover = sum(
            float(
                record.rawusage) for record in response[1] if
            record.virtualmachineid == vm.id)

        self.debug(
            "running vm usage T1: %s" %
            vmRunningUsageRecordAfterDestroy)
        self.debug(
            "allocated vm usage T1: %s" %
            vmRunningUsageRecordAfterRecover)

        self.assertEqual(
            format(vmRunningUsageRecordAfterDestroy, ".1f"),
            format(vmRunningUsageRecordAfterRecover, ".1f"),
            "Running usage should remain the same")

        self.debug(
            "allocated vm usage T2: %s" %
            vmAllocatedUsageRecordAfterDestroy)
        self.debug(
            "allocated vm usage T2: %s" %
            vmAllocatedUsageRecordAfterRecover)

        # Step 9
        self.assertTrue(
            vmAllocatedUsageRecordAfterDestroy <
            vmAllocatedUsageRecordAfterRecover,
            "Allocated VM usage after recover should be greater than\
                        before")

        # Step 10
        # Change service offering of VM and verify that it is changed
        vm.change_service_offering(
            self.userapiclient,
            serviceOfferingId=self.service_offering_2.id
        )

        response = self.listUsageRecords(usagetype=2)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecord = response[1][-1]

        # Step 11: Veriying vm usage for new service offering
        self.assertEqual(vmAllocatedUsageRecord.offeringid,
                         self.service_offering_2.id,
                         "The service offering id in the usage record\
                        does not match with id of new service offering")

        # Step 12
        vm.start(self.userapiclient)

        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmRunningUsageRecordAfterStart = sum(
            float(
                record.rawusage) for record in response[1] if
            record.virtualmachineid == vm.id)

        response = self.listUsageRecords(usagetype=2, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecordAfterStart = sum(
            float(
                record.rawusage) for record in response[1] if
            record.virtualmachineid == vm.id)

        self.debug("running vm usage T3: %s" % vmRunningUsageRecordAfterStart)
        self.debug(
            "allocated vm usage T3: %s" %
            vmAllocatedUsageRecordAfterStart)

        # Step 13
        self.assertTrue(
            vmRunningUsageRecordAfterStart <
            vmAllocatedUsageRecordAfterStart,
            "Allocated VM usage should be greater than Running usage")

        # Step 14
        self.assertTrue(
            vmRunningUsageRecordAfterRecover <
            vmRunningUsageRecordAfterStart,
            "Running VM usage after start VM should be greater than\
                        that after recover operation")
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_positive_tests_usage(self):
        """ Positive test for usage test path

        # 1.  Scale up VM and check that usage is generated for
              new cpu and ram value (Check in usage_vm_instance table)
        # 2.  Scale down VM and check that usage is generated for
              new cpu and ram value (Check in usage_vm_instance table)
        # 3.  Attach disk to VM and check that volume usage is
              generated for correct disk offering
        # 4.  Detach volume from and verify that usage for volue remains
              the same there afterwards
        # 5.  Create snapshot of the root disk and verify correct usage is
              generated for snapshot with correct size
        # 6.  Create template from root disk and check correct usage is
              generated for template with correct size
        # 7.  Delete the template and verify that usage is stopped for
              template
        # 8.  Create volume from snaopshot and verify correct disk usage
              is generated
        # 9.  Delete the volume and verify that the usage is stopped
        # 10. Create template from snapshot and verify correct usage
              is generated for the template with correct size
        """

        # Step 1
        # Create dynamic and static service offering
        self.testdata["service_offering"]["cpunumber"] = ""
        self.testdata["service_offering"]["cpuspeed"] = ""
        self.testdata["service_offering"]["memory"] = ""

        serviceOffering_dynamic = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )
        self.cleanup.append(serviceOffering_dynamic)

        customcpunumber = 1
        customcpuspeed = 256
        custommemory = 128

        # Deploy VM with dynamic service offering
        virtualMachine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["virtual_machine"],
            serviceofferingid=serviceOffering_dynamic.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            customcpunumber=customcpunumber,
            customcpuspeed=customcpuspeed,
            custommemory=custommemory
        )

        # Stop VM and verify that it is in stopped state
        virtualMachine.stop(self.userapiclient)

        scaledcpunumber = 2
        scaledcpuspeed = 512
        scaledmemory = 256

        # Scale up VM
        virtualMachine.scale(
            self.userapiclient,
            serviceOfferingId=serviceOffering_dynamic.id,
            customcpunumber=scaledcpunumber,
            customcpuspeed=scaledcpuspeed,
            custommemory=scaledmemory
        )

        self.listUsageRecords(usagetype=1)

        qresultset = self.dbclient.execute(
            "select cpu_cores, memory, cpu_speed from usage_vm_instance where vm_name = '%s';" %
            str(virtualMachine.name), db="cloud_usage")
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        dbcpucores = qresultset[-1][0]
        dbmemory = qresultset[-1][1]
        dbcpuspeed = qresultset[-1][2]

        self.assertEqual(int(dbcpucores), scaledcpunumber,
                         "scaled cpu number not matching with db record")

        self.assertEqual(int(dbmemory), scaledmemory,
                         "scaled memory not matching with db record")

        self.assertEqual(int(dbcpuspeed), scaledcpuspeed,
                         "scaled cpu speed not matching with db record")

        scaledcpunumber = 1
        scaledcpuspeed = 512
        scaledmemory = 256

        # Step 2
        # Scale down VM
        virtualMachine.scale(
            self.userapiclient,
            serviceOfferingId=serviceOffering_dynamic.id,
            customcpunumber=scaledcpunumber,
            customcpuspeed=scaledcpuspeed,
            custommemory=scaledmemory
        )

        self.listUsageRecords(usagetype=1)

        qresultset = self.dbclient.execute(
            "select cpu_cores, memory, cpu_speed from usage_vm_instance where vm_name = '%s';" %
            str(virtualMachine.name), db="cloud_usage")
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        dbcpucores = qresultset[-1][0]
        dbmemory = qresultset[-1][1]
        dbcpuspeed = qresultset[-1][2]

        self.assertEqual(int(dbcpucores), scaledcpunumber,
                         "scaled cpu number not matching with db record")

        self.assertEqual(int(dbmemory), scaledmemory,
                         "scaled memory not matching with db record")

        self.assertEqual(int(dbcpuspeed), scaledcpuspeed,
                         "scaled cpu speed not matching with db record")

        disk_offering = DiskOffering.create(
            self.apiclient,
            self.testdata["disk_offering"]
        )
        self.cleanup.append(disk_offering)

        # Step 3
        volume = Volume.create(
            self.userapiclient, self.testdata["volume"],
            zoneid=self.zone.id, account=self.account.name,
            domainid=self.account.domainid, diskofferingid=disk_offering.id
        )

        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        virtual_machine.attach_volume(self.userapiclient, volume=volume)

        # Verifying usage for Volume - START
        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        volumeUsageRecords = [record for record in response[1]
                              if volume.id == record.usageid]

        self.assertTrue(
            len(volumeUsageRecords) >= 1,
            "Volume usage record for attached volume is not generated")

        volumeRawUsageBeforeDetach = sum(float(record.rawusage) for
                                         record in [
            record for record in volumeUsageRecords
            if volume.id == record.usageid])

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact Volume creation time
        response = self.getEventCreatedDateTime(volume.name)
        self.assertEqual(response[0], PASS, response[1])
        volumeCreatedDateTime = response[1]
        self.debug("Volume creation date: %s" % volumeCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - volumeCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("Volume expected usage: %s" % expectedUsage)

        actualUsage = format(volumeRawUsageBeforeDetach, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Verifying usage for Volume - END

        # Step 4
        virtual_machine.detach_volume(self.userapiclient, volume=volume)

        # Verifying usage for Volume after detaching - START
        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        volumeUsageRecords = response[1]

        volumeRawUsageAfterDetach_time_1 = sum(float(record.rawusage) for
                                               record in [
            record for record in volumeUsageRecords
            if volume.id == record.usageid])

        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        volumeUsageRecords = response[1]

        volumeRawUsageAfterDetach_time_2 = sum(float(record.rawusage) for
                                               record in [
            record for record in volumeUsageRecords
            if volume.id == record.usageid])

        self.debug(volumeRawUsageAfterDetach_time_1)
        self.debug(volumeRawUsageAfterDetach_time_2)

        self.assertTrue(
            volumeRawUsageAfterDetach_time_1 <
            volumeRawUsageAfterDetach_time_2,
            "Raw volume usage should continue running after detach operation"
        )

        # Verifying usage for Volume after detaching - END

        volumes = Volume.list(
            self.userapiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            validateList(volumes)[0],
            PASS,
            "Volumes list validation failed"
        )

        rootVolume = volumes[0]

        # Step 5
        # Create a snapshot from the ROOTDISK
        snapshotFromRootVolume = Snapshot.create(
            self.userapiclient,
            rootVolume.id)

        # Verifying usage for Snapshot - START
        response = self.listUsageRecords(usagetype=9)
        self.assertEqual(response[0], PASS, response[1])
        snapshotUsageRecords = [record for record in response[1]
                                if snapshotFromRootVolume.id == record.usageid]

        self.assertEqual(snapshotUsageRecords[0].size,
                         snapshotFromRootVolume.physicalsize,
                         "The snapshot size in the usage record and \
                        does not match with the created snapshot size")

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact snapshot creation time
        response = self.getEventCreatedDateTime(snapshotFromRootVolume.name)
        self.assertEqual(response[0], PASS, response[1])
        snapshotCreatedDateTime = response[1]
        self.debug("Snapshot creation date: %s" % snapshotCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - snapshotCreatedDateTime).total_seconds() / 3600),
            ".2f")

        actualUsage = format(sum(float(record.rawusage)
                                 for record in snapshotUsageRecords), ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Verifying usage for Snapshot - END

        virtual_machine.stop(self.userapiclient)

        # Step 6
        templateFromVolume = Template.create(
            self.userapiclient,
            self.testdata["templates"],
            rootVolume.id,
            self.account.name,
            self.account.domainid
        )

        templates = Template.list(
            self.userapiclient,
            listall=True,
            id=templateFromVolume.id,
            templatefilter="self"
        )

        self.assertEqual(
            validateList(templates)[0],
            PASS,
            "templates list validation failed"
        )

        # Verifying usage for Template - START
        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = [record for record in response[1]
                                if templateFromVolume.id == record.usageid]

        self.assertEqual(templateUsageRecords[0].virtualsize,
                         templates[-1].size,
                         "The template size in the usage record and \
                        does not match with the created template size")

        templateRawUsage = sum(float(record.rawusage)
                               for record in templateUsageRecords)

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact Template creation time
        response = self.getEventCreatedDateTime(templateFromVolume.name)
        self.assertEqual(response[0], PASS, response[1])
        templateCreatedDateTime = response[1]
        self.debug("Template creation date: %s" % templateCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - templateCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("Template expected usage: %s" % expectedUsage)

        actualUsage = format(templateRawUsage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Verifying usage for Template - END

        # Step 7
        templateFromVolume.delete(self.userapiclient)

        # Verifying usage for Template is stoppd after deleting it - START
        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForTemplateFromVolumeAfterDeletion_1 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if templateFromVolume.id == record.usageid])

        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForTemplateFromVolumeAfterDeletion_2 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if templateFromVolume.id == record.usageid])

        self.assertTrue(usageForTemplateFromVolumeAfterDeletion_1 ==
                        usageForTemplateFromVolumeAfterDeletion_2,
                        "usage for template after deletion should remain the same\
                        after specific intervals of time")

        # Verifying usage for Template is stoppd after deleting it - END

        # Step 8
        self.testdata["volume_from_snapshot"]["zoneid"] = self.zone.id

        volumeFromSnapshot = Volume.create_from_snapshot(
            self.userapiclient,
            snapshot_id=snapshotFromRootVolume.id,
            services=self.testdata["volume_from_snapshot"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        # Verifying usage for Volume from Snapshot - START
        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        volumeUsageRecords = response[1]

        usageForVolumeFromSnapshotBeforeDeletion = sum(
            float(
                record.rawusage) for record in [
                record for record in volumeUsageRecords
                if volumeFromSnapshot.id == record.usageid])

        self.debug(usageForVolumeFromSnapshotBeforeDeletion)

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact Volume creation time
        response = self.getEventCreatedDateTime(volumeFromSnapshot.name)
        self.assertEqual(response[0], PASS, response[1])
        volumeCreatedDateTime = response[1]
        self.debug("Volume creation date: %s" % volumeCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - volumeCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("Volume expected usage: %s" % expectedUsage)

        actualUsage = format(usageForVolumeFromSnapshotBeforeDeletion, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Verifying usage for Volume from Snapshot - END

        # Step 9
        volumeFromSnapshot.delete(self.userapiclient)

        # Verifying usage for Volume from Snapshot is stopped after delete -
        # START
        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        volumeUsageRecords = response[1]

        usageForVolumeFromSnapshotAfterDeletion_1 = sum(
            float(
                record.rawusage) for record in [
                record for record in volumeUsageRecords
                if volumeFromSnapshot.id == record.usageid])

        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        volumeUsageRecords = response[1]

        usageForVolumeFromSnapshotAfterDeletion_2 = sum(
            float(
                record.rawusage) for record in [
                record for record in volumeUsageRecords
                if volumeFromSnapshot.id == record.usageid])

        self.debug(usageForVolumeFromSnapshotAfterDeletion_1)
        self.debug(usageForVolumeFromSnapshotAfterDeletion_2)

        self.assertTrue(usageForVolumeFromSnapshotAfterDeletion_1 ==
                        usageForVolumeFromSnapshotAfterDeletion_2,
                        "usage for volume after deletion should remain the same\
                        after specific intervals of time")

        # Verifying usage for Volume from Snapshot is stopped after delete -
        # END

        # Step 10
        templateFromSnapshot = Template.create_from_snapshot(
            self.userapiclient,
            snapshotFromRootVolume,
            self.testdata["privatetemplate"]
        )

        templates = Template.list(
            self.userapiclient,
            listall=True,
            id=templateFromSnapshot.id,
            templatefilter="self"
        )

        self.assertEqual(
            validateList(templates)[0],
            PASS,
            "templates list validation failed"
        )

        # Verifying usage for Template from Snapshot - START
        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        templateUsageRecords = [record for record in usageRecords
                                if templateFromSnapshot.id == record.usageid]

        self.assertTrue(len(templateUsageRecords) >= 1,
                        "template usage record list is empty")

        self.assertEqual(templateUsageRecords[-1].virtualsize,
                         templates[0].size,
                         "The template size in the usage record and \
                        does not match with the created template size")

        templateRawUsage = sum(float(record.rawusage)
                               for record in templateUsageRecords)

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact Template creation time
        response = self.getEventCreatedDateTime(templateFromSnapshot.name)
        self.assertEqual(response[0], PASS, response[1])
        templateCreatedDateTime = response[1]
        self.debug("Template creation date: %s" % templateCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - templateCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("Template expected usage: %s" % expectedUsage)

        actualUsage = format(templateRawUsage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Verifying usage for Template from Snapshot - END

        templateFromSnapshot.delete(self.userapiclient)

        # Verifying usage for Template from Snapshot is stopped after delete -
        # START
        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForTemplateAfterDeletion_1 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if templateFromSnapshot.id == record.usageid])

        response = self.listUsageRecords(usagetype=7)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForTemplateAfterDeletion_2 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if templateFromSnapshot.id == record.usageid])

        self.assertTrue(usageForTemplateAfterDeletion_1 ==
                        usageForTemplateAfterDeletion_2,
                        "usage for volume after deletion should remain the same\
                        after specific intervals of time")

        # Verifying usage for Template from Snapshot is stopped after delete -
        # END

        snapshotFromRootVolume.delete(self.userapiclient)

        # Verifying usage for Snapshot from volume is stopped after delete -
        # START
        response = self.listUsageRecords(usagetype=9)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForSnapshotAfterDeletion_1 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if snapshotFromRootVolume.id == record.usageid])

        response = self.listUsageRecords(usagetype=9)
        self.assertEqual(response[0], PASS, response[1])
        templateUsageRecords = response[1]

        usageForSnapshotAfterDeletion_2 = sum(
            float(
                record.rawusage) for record in [
                record for record in templateUsageRecords
                if snapshotFromRootVolume.id == record.usageid])

        self.assertTrue(usageForSnapshotAfterDeletion_1 ==
                        usageForSnapshotAfterDeletion_2,
                        "usage for volume after deletion should remain the same\
                        after specific intervals of time")

        # Verifying usage for Snapshot from volume is stopped after delete -
        # END
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_positive_tests_usage(self):
        """ Positive test for usage test path T28 - T35

        Steps:
        # 1. Add an isolated network to VM and verify that network offering
             usage is generated for account
             Also verify that IP usage is generated for source NAT IP of
             network
        # 2. Enabled VPN on source nat IP of default network of VM
        # 3. Add two VPN users and check that usage is generated for VPN users
        # 4. Acquire public IP in the network and verify that IP usage
             is generated for the acquired IP
        # 5. Create two PF rules on this IP and verify that PF rules usage
             is generated for the account
        # 6. Acquire another IP and enabled static NAT on it and create
             egress firewall rule on it
        # 7. Verify IP usage is generated for above acquired IP
        # 8. SSH to VM with above IP and ping to google.com
        # 9. Verify that Network bytes usage is generated for account
             and it matches with the actual number of bytes
        # 10. Repeat the same for other acquired IP
        # 11. Delete one of the PF rules and verify that usage is stopped for the PF rule
        # 12. Also verify that usage is not stopped for other PF rule which
        #     is still present
        """

        # Step 1
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        self.testdata["isolated_network"]["zoneid"] = self.zone.id
        isolated_network = Network.create(
            self.userapiclient,
            self.testdata["isolated_network"],
            self.account.name,
            self.account.domainid,
            networkofferingid=self.isolated_network_offering_2.id)

        virtual_machine.add_nic(self.userapiclient, isolated_network.id)

        # Usages for steps are checked together in batch after the operations are done
        # to avoid waiting for usage job to run for each operation separately

        # Listing source nat ip of newly added network
        ipAddresses = PublicIPAddress.list(
            self.apiclient,
            associatednetworkid=isolated_network.id,
            listall=True)

        sourceNatIP = ipAddresses[0]

        ipAddressesDefaultNetwork = PublicIPAddress.list(
            self.apiclient,
            associatednetworkid=virtual_machine.nic[0].networkid,
            listall=True)

        sourceNatIPDefaultNetwork = ipAddressesDefaultNetwork[0]

        # Step 2
        # Create VPN for source NAT ip
        Vpn.create(self.apiclient,
                   sourceNatIPDefaultNetwork.id,
                   account=self.account.name,
                   domainid=self.account.domainid)

        self.debug("Verifying the remote VPN access")
        vpns = Vpn.list(self.apiclient,
                        publicipid=sourceNatIPDefaultNetwork.id,
                        listall=True)
        self.assertEqual(
            isinstance(vpns, list),
            True,
            "List VPNs shall return a valid response"
        )

        # Step 3:
        vpnuser_1 = VpnUser.create(
            self.apiclient,
            self.testdata["vpn_user"]["username"],
            self.testdata["vpn_user"]["password"],
            account=self.account.name,
            domainid=self.account.domainid,
            rand_name=True
        )

        vpnuser_2 = VpnUser.create(
            self.apiclient,
            self.testdata["vpn_user"]["username"],
            self.testdata["vpn_user"]["password"],
            account=self.account.name,
            domainid=self.account.domainid,
            rand_name=True
        )

        # Step 4
        public_ip_1 = PublicIPAddress.create(
            self.userapiclient,
            accountid=virtual_machine.account,
            zoneid=virtual_machine.zoneid,
            domainid=virtual_machine.domainid,
            services=self.testdata["server"],
            networkid=virtual_machine.nic[0].networkid
        )

        FireWallRule.create(
            self.userapiclient,
            ipaddressid=public_ip_1.ipaddress.id,
            protocol=self.testdata["fwrule"]["protocol"],
            cidrlist=[self.testdata["fwrule"]["cidr"]],
            startport=self.testdata["fwrule"]["startport"],
            endport=self.testdata["fwrule"]["endport"]
        )

        # Step 5
        self.testdata["natrule"]["startport"] = 22
        self.testdata["natrule"]["endport"] = 22

        nat_rule_1 = NATRule.create(
            self.userapiclient,
            virtual_machine,
            self.testdata["natrule"],
            public_ip_1.ipaddress.id
        )

        self.testdata["natrule"]["privateport"] = 23
        self.testdata["natrule"]["publicport"] = 23

        nat_rule_2 = NATRule.create(
            self.userapiclient,
            virtual_machine,
            self.testdata["natrule"],
            public_ip_1.ipaddress.id
        )

        # Usages for above operations are checked here together

        # Checking usage for source nat IP of added network
        response = self.listUsageRecords(usagetype=13)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        nwOfferingUsageRecords = [
            record for record in usageRecords if self.isolated_network_offering_2.id == record.offeringid]

        self.assertTrue(validateList(nwOfferingUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(nwOfferingUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for source NAT ip")

        # Checking usage for source nat IP of default VM network
        response = self.listUsageRecords(usagetype=3, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        ipUsageRecords = [record for record in usageRecords
                          if sourceNatIP.id == record.usageid]

        self.assertTrue(validateList(ipUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(ipUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for source NAT ip")

        # Checking usage for acquired public IP
        response = self.listUsageRecords(usagetype=3, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        ipUsageRecords = [record for record
                          in usageRecords
                          if public_ip_1.ipaddress.id == record.usageid
                          ]

        self.assertTrue(validateList(ipUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(ipUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for acquired public ip")

        # Checking usage for NAT rules
        response = self.listUsageRecords(usagetype=12, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRuleUsageRecords = [record for record in usageRecords
                               if nat_rule_1.id == record.usageid]

        self.assertTrue(validateList(natRuleUsageRecords)[0] == PASS,
                        "NAT rule usage record list validation failed")

        self.assertTrue(float(natRuleUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for nat rule")

        natRuleUsageRecords = [record for record in usageRecords
                               if nat_rule_2.id == record.usageid]

        self.assertTrue(validateList(natRuleUsageRecords)[0] == PASS,
                        "NAT rule usage record list validation failed")

        self.assertTrue(float(natRuleUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for nat rule")

        # Checking VPN usage
        response = self.listUsageRecords(usagetype=14, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        vpnUserUsageRecords_1 = [record for record in usageRecords
                                 if vpnuser_1.id == record.usageid]

        self.assertTrue(validateList(vpnUserUsageRecords_1)[0] == PASS,
                        "VPN user usage record list validation failed")

        vpnuser1_rawusage = sum(float(record.rawusage)
                                for record in vpnUserUsageRecords_1)

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact VPN user creation time
        response = self.getEventCreatedDateTime(vpnuser_1.username)
        self.assertEqual(response[0], PASS, response[1])
        vpnUserCreatedDateTime = response[1]
        self.debug("VPN creation date: %s" % vpnUserCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - vpnUserCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("VPN user expected usage: %s" % expectedUsage)

        actualUsage = format(vpnuser1_rawusage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        vpnUserUsageRecords_2 = [record for record in usageRecords
                                 if vpnuser_2.id == record.usageid]

        self.assertTrue(validateList(vpnUserUsageRecords_2)[0] == PASS,
                        "VPN user usage record list validation failed")

        vpnuser2_rawusage = sum(float(record.rawusage)
                                for record in vpnUserUsageRecords_2)

        # Checking exact VPN user creation time
        response = self.getEventCreatedDateTime(vpnuser_2.username)
        self.assertEqual(response[0], PASS, response[1])
        vpnUserCreatedDateTime = response[1]
        self.debug("VPN creation date: %s" % vpnUserCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - vpnUserCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("VPN user expected usage: %s" % expectedUsage)

        actualUsage = format(vpnuser2_rawusage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Acquire another public IP and check usage
        public_ip_2 = PublicIPAddress.create(
            self.userapiclient,
            accountid=virtual_machine.account,
            zoneid=virtual_machine.zoneid,
            domainid=virtual_machine.domainid,
            services=self.testdata["server"],
            networkid=virtual_machine.nic[0].networkid
        )

        # Step 6
        # Enabling static Nat for Ip Address associated
        StaticNATRule.enable(
            self.userapiclient,
            ipaddressid=public_ip_2.ipaddress.id,
            virtualmachineid=virtual_machine.id,
        )

        # Step 7
        response = self.listUsageRecords(usagetype=3)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        ipUsageRecords = [record for record
                          in usageRecords
                          if public_ip_2.ipaddress.id == record.usageid
                          ]

        self.assertTrue(validateList(ipUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(ipUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for public ip")

        FireWallRule.create(
            self.userapiclient,
            ipaddressid=public_ip_2.ipaddress.id,
            protocol=self.testdata["fwrule"]["protocol"],
            cidrlist=[self.testdata["fwrule"]["cidr"]],
            startport=self.testdata["fwrule"]["startport"],
            endport=self.testdata["fwrule"]["endport"]
        )

        EgressFireWallRule.create(
            self.userapiclient,
            networkid=virtual_machine.nic[0].networkid,
            protocol=self.testdata["icmprule"]["protocol"],
            type=self.testdata["icmprule"]["icmptype"],
            code=self.testdata["icmprule"]["icmpcode"],
            cidrlist=self.testdata["icmprule"]["cidrlist"])

        # Step 8:
        ssh_client = virtual_machine.get_ssh_client(
            ipaddress=public_ip_1.ipaddress.ipaddress
        )

        # Ping Internet and check the bytes received
        res = ssh_client.execute("ping -c 1 www.google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        routers = list_routers(
            self.apiclient,
            networkid=virtual_machine.nic[0].networkid,
            listall=True
        )
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed")
        router = routers[0]

        result = self.getCommandResultFromRouter(
            router,
            "iptables -L NETWORK_STATS -n -v -x")

        self.debug("iptables -L NETWORK_STATS -n -v -x: %s" % result)

        bytesReceivedIptableRows = [record for record in result if
                                    "eth2   eth0" in record]
        self.debug("bytes received rows: %s" % bytesReceivedIptableRows)
        bytesReceivedOnRouter = sum(
            int(record[1]) for record in [x.split() for x in bytesReceivedIptableRows])

        self.debug(
            "Bytes received extracted from router: %s" %
            bytesReceivedOnRouter)

        # Step 9:
        # Verify that bytes received in usage are equal to
        # as shown on router
        response = self.listUsageRecords(usagetype=5)
        self.assertEqual(response[0], PASS, response[1])
        bytesReceivedUsage = sum(
            int(record.rawusage) for record in response[1])

        self.assertTrue(bytesReceivedUsage ==
                        bytesReceivedOnRouter,
                        "Total bytes received usage should be \
                        equal to bytes received on router")

        # Step 10:
        # Repeat the same for other public IP
        ssh_client = virtual_machine.get_ssh_client(
            ipaddress=public_ip_2.ipaddress.ipaddress
        )

        res = ssh_client.execute("ping -c 1 www.google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        result = self.getCommandResultFromRouter(
            router,
            "iptables -L NETWORK_STATS -n -v -x")

        self.debug("iptables -L NETWORK_STATS -n -v -x: %s" % result)

        bytesReceivedIptableRows = [record for record in result if
                                    "eth2   eth0" in record]
        self.debug("bytes received rows: %s" % bytesReceivedIptableRows)
        bytesReceivedOnRouter = sum(
            int(record[1]) for record in [x.split() for x in bytesReceivedIptableRows])

        self.debug(
            "Bytes received extracted from router: %s" %
            bytesReceivedOnRouter)

        # Step 9:
        # Verify that bytes received in usage are equal to
        # as shown on router
        response = self.listUsageRecords(usagetype=5)
        self.assertEqual(response[0], PASS, response[1])
        bytesReceivedUsage = sum(
            int(record.rawusage) for record in response[1])

        self.assertTrue(bytesReceivedUsage ==
                        bytesReceivedOnRouter,
                        "Total bytes received usage should be \
                        equal to bytes received on router")

        # Step 11:
        # Delete NAT rule and verify that usage is stopped for the NAT rule
        nat_rule_1.delete(self.userapiclient)

        response = self.listUsageRecords(usagetype=12, sleep=True)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRule_1_Usage_t1 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if nat_rule_1.id == record.usageid])

        response = self.listUsageRecords(usagetype=12)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRule_1_Usage_t2 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if nat_rule_1.id == record.usageid])

        self.assertTrue(
            natRule_1_Usage_t1 == natRule_1_Usage_t2,
            "NAT rule usage should be stopped once the rule is deleted")

        # Also verify that usage for other nat rule is running
        natRule_2_Usage_t1 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if nat_rule_2.id == record.usageid])

        # Step 12:
        response = self.listUsageRecords(usagetype=12)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRule_2_Usage_t2 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if nat_rule_1.id == record.usageid])

        self.assertTrue(natRule_2_Usage_t1 > natRule_2_Usage_t2,
                        "NAT rule usage for second rule should be running")
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_positive_tests_usage(self):
        """ Positive test for usage test path
        Steps:
        # 1. Create a VM in the account
        # 2. Acquire public IP in VM network and verify correct usage
             is generated for IP
        # 3. Create LB rule for the IP address and verify LB rule usage
             is generated for the account
        # 4. Create another LB rule with different ports and verify
             seperate usage is generated for new LB rule

        # 5. Create egress firewall rule for VM and SSH to VM
        # 6. Ping external network from the VM and verify that
             network byte usage is genrated correctly
        # 7. Delete one LB rule and verify that the usage
             is stopped for the LB rule
        # 8. Stop the network router and
        #    Verify iptables counters are reset when domR stops
        #    Verify current_bytes in user_statistics table are moved to
             net_bytes
        #    Verify currnt_bytes becomes zero
        # 9. Start the router and
        #    Verify iptables counters are reset when domR starts
        #    Verify a diff of total (current_bytes + net_bytes) in previous
             aggregation period and current period will give the network usage

        """

        # Step 1
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        # Step 2
        public_ip_1 = PublicIPAddress.create(
            self.userapiclient,
            accountid=virtual_machine.account,
            zoneid=virtual_machine.zoneid,
            domainid=virtual_machine.domainid,
            services=self.testdata["server"]
        )

        self.testdata["lbrule"]["privateport"] = 22
        self.testdata["lbrule"]["publicport"] = 2222
        publicport = self.testdata["lbrule"]["publicport"]

        # Step 3
        # Create LB Rule
        lbrule_1 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            accountid=self.account.name,
            networkid=virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        self.testdata["lbrule"]["privateport"] = 23
        self.testdata["lbrule"]["publicport"] = 2223

        # Step 4
        # Create another LB Rule
        lbrule_2 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip_1.ipaddress.id,
            accountid=self.account.name,
            networkid=virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        response = self.listUsageRecords(usagetype=3)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        ipUsageRecords = [record for record in usageRecords
                          if public_ip_1.ipaddress.id == record.usageid
                          ]

        self.assertTrue(validateList(ipUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(ipUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for ip address")

        response = self.listUsageRecords(usagetype=11, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        lbRule_1_UsageRecords = [record for record in usageRecords
                                 if lbrule_1.id == record.usageid]

        self.assertTrue(validateList(lbRule_1_UsageRecords)[0] == PASS,
                        "LB rule usage record list validation failed")

        self.assertTrue(float(lbRule_1_UsageRecords[0].rawusage) > 0,
                        "LB usage not started for nat rule")

        lbRule_2_UsageRecords = [record for record in usageRecords
                                 if lbrule_2.id == record.usageid]

        self.assertTrue(validateList(lbRule_2_UsageRecords)[0] == PASS,
                        "LB rule usage record list validation failed")

        self.assertTrue(float(lbRule_2_UsageRecords[0].rawusage) > 0,
                        "LB usage not started for nat rule")

        # Step 5
        EgressFireWallRule.create(
            self.userapiclient,
            networkid=virtual_machine.nic[0].networkid,
            protocol=self.testdata["icmprule"]["protocol"],
            type=self.testdata["icmprule"]["icmptype"],
            code=self.testdata["icmprule"]["icmpcode"],
            cidrlist=self.testdata["icmprule"]["cidrlist"])

        lbrule_1.assign(self.userapiclient, [virtual_machine])

        ssh_client = virtual_machine.get_ssh_client(
            ipaddress=public_ip_1.ipaddress.ipaddress,
            port=publicport
        )

        # Step 6
        res = ssh_client.execute("ping -c 1 www.google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        # Verifying usage for bytes received - START
        routers = list_routers(
            self.apiclient,
            networkid=virtual_machine.nic[0].networkid,
            listall=True
        )
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed")
        router = routers[0]
        result = self.getCommandResultFromRouter(
            router,
            "iptables -L NETWORK_STATS -n -v -x")

        self.debug("iptables -L NETWORK_STATS -n -v -x: %s" % result)

        bytesReceivedIptableRows = [record for record in result if
                                    "eth2   eth0" in record]
        self.debug("bytes received rows: %s" % bytesReceivedIptableRows)
        bytesReceivedOnRouter = sum(
            int(record[1]) for record in [x.split() for x in bytesReceivedIptableRows])

        self.debug(
            "Bytes received extracted from router: %s" %
            bytesReceivedOnRouter)

        # Verify that bytes received in usage are equal to
        # as shown on router
        response = self.listUsageRecords(usagetype=5)
        self.assertEqual(response[0], PASS, response[1])
        bytesReceivedUsage = sum(
            int(record.rawusage) for record in response[1])

        self.assertTrue(bytesReceivedUsage ==
                        bytesReceivedOnRouter,
                        "Total bytes received usage should be \
                        equal to bytes received on router")

        # Verifying usage for bytes received - END

        lbrule_1.delete(self.userapiclient)
        # Step 7 Verify that usage is stopped for the LB rule

        response = self.listUsageRecords(usagetype=11)
        self.assertEqual(response[0], PASS, response[1])
        lbUsageRecords = response[1]

        usageForLbRuleAfterDeletion_t1 = sum(
            float(
                record.rawusage) for record in [
                record for record in lbUsageRecords
                if lbrule_1.id == record.usageid])

        response = self.listUsageRecords(usagetype=11)
        self.assertEqual(response[0], PASS, response[1])
        lbUsageRecords = response[1]

        usageForLbRuleAfterDeletion_t2 = sum(
            float(
                record.rawusage) for record in [
                record for record in lbUsageRecords
                if lbrule_1.id == record.usageid])

        self.assertTrue(usageForLbRuleAfterDeletion_t1 ==
                        usageForLbRuleAfterDeletion_t2,
                        "usage for LB rule after deletion should remain the same\
                        after specific intervals of time")

        qresultset = self.dbclient.execute(
            "select id from account where account_name = '%s';"
            % self.account.name
        )
        accountid = qresultset[0][0]
        self.debug("accountid: %s" % accountid)

        qresultset = self.dbclient.execute(
            "select current_bytes_sent, current_bytes_received  from user_statistics where account_id = '%s';" %
            accountid,
            db="cloud_usage")[0]

        currentBytesSentBeforeRouterStop = qresultset[0]
        currentBytesReceivedBeforeRouterStop = qresultset[1]

        self.debug(currentBytesSentBeforeRouterStop)
        self.debug(currentBytesReceivedBeforeRouterStop)

        # Step 8
        routers = Router.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
        )

        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Check for list routers response return valid data"
        )
        router = routers[0]

        # Stop the router
        Router.stop(
            self.apiclient,
            id=router.id
        )

        response = verifyRouterState(
            self.apiclient,
            router.id,
            "stopped")
        self.assertEqual(response[0], PASS, response[1])

        qresultset = self.dbclient.execute(
            "select current_bytes_sent, current_bytes_received, net_bytes_sent, net_bytes_received from user_statistics where account_id = '%s';" %
            accountid,
            db="cloud_usage")[0]

        currentBytesSentAfterRouterStop = int(qresultset[0])
        currentBytesReceivedAfterRouterStop = int(qresultset[1])
        netBytesSentAfterRouterStop = int(qresultset[0])
        netBytesReceivedAfterRouterStop = int(qresultset[1])

        self.debug(currentBytesSentAfterRouterStop)
        self.debug(currentBytesReceivedAfterRouterStop)
        self.debug(netBytesSentAfterRouterStop)
        self.debug(netBytesReceivedAfterRouterStop)

        self.assertTrue(
            (currentBytesSentAfterRouterStop +
             currentBytesReceivedAfterRouterStop) == 0,
            "Current bytes should be 0")

        self.assertTrue(
            (currentBytesSentBeforeRouterStop +
             currentBytesReceivedBeforeRouterStop) == (
                netBytesSentAfterRouterStop +
                netBytesReceivedAfterRouterStop),
            "current bytes should be moved to net bytes")

        # TODO: Verify iptables counters are reset when domR starts

        # Step 9
        # Start the router
        Router.start(
            self.apiclient,
            id=router.id
        )

        response = verifyRouterState(
            self.apiclient,
            router.id,
            "running")
        self.assertEqual(response[0], PASS, response[1])

        #  TODO: Verify iptables counters are reset when domR starts
        #  Verify a diff of total (current_bytes + net_bytes) in previous
        #  aggregation period and current period will give the network usage
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_05_positive_tests_usage(self):
        """ Positive test for usage test path T61 - T62
        Steps:
        # 1. Deploy a VM
        # 2. Take Vm snapshot and verify usage is generated for VM snapshot
        # 3. Delete VM snapshot and verify that usage stops
        """

        time.sleep(180)

        if self.hypervisor.lower() in ['kvm', 'hyperv']:
            self.skipTest("This feature is not supported on %s" %
                          self.hypervisor)

        # Step 1
        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        # Step 2
        vmsnapshot = VmSnapshot.create(
            self.userapiclient,
            virtual_machine.id)

        response = self.listUsageRecords(usagetype=25)
        self.assertEqual(response[0], PASS, response[1])

        # Step 3
        VmSnapshot.deleteVMSnapshot(
            self.userapiclient,
            vmsnapshot.id
        )

        response = self.listUsageRecords(usagetype=25)
        self.assertEqual(response[0], PASS, response[1])
        vmSnapshotUsageRecords_t1 = response[1]

        vmSnapshotUsage_t1 = sum(float(record.rawusage)
                                 for record in vmSnapshotUsageRecords_t1)

        response = self.listUsageRecords(usagetype=25)
        self.assertEqual(response[0], PASS, response[1])
        vmSnapshotUsageRecords_t2 = response[1]

        vmSnapshotUsage_t2 = sum(float(record.rawusage)
                                 for record in vmSnapshotUsageRecords_t2)

        self.debug(vmSnapshotUsage_t1)
        self.debug(vmSnapshotUsage_t2)

        self.assertEqual(
            vmSnapshotUsage_t1,
            vmSnapshotUsage_t2,
            "VmSnapshot usage should remain the same\
                    once snapshot is deleted")
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_06_positive_tests_usage(self):
        """Migrate VM and verify usage"""

        # Validate the following
        # 1. Create a VM, and verify that usage is generated for it
        #    with correct service offering and template id
        # 2. Migrate the VM to suitable host
        # 3. Verify that after migration, VM usage continues to be running

        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "vm migrate feature is not supported on %s" %
                self.hypervisor.lower())

        # Step 1:
        self.vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )

        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmUsageRecord = response[1][0]

        self.assertEqual(vmUsageRecord.offeringid,
                         self.service_offering.id,
                         "The service offering id in the usage record\
                        does not match with id of service offering\
                        with which the VM was created")

        self.assertEqual(vmUsageRecord.templateid,
                         self.template.id,
                         "The template id in the usage record\
                        does not match with id of template\
                        with which the VM was created")

        # Step 2:
        host = findSuitableHostForMigration(self.apiclient, self.vm.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)

        try:
            self.vm.migrate(self.apiclient, host.id)
        except Exception as e:
            self.fail("Failed to migrate instance: %s" % e)

        # Step 3:
        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmUsageRecords_t1 = response[1]

        vmUsage_t1 = sum(float(record.rawusage)
                         for record in vmUsageRecords_t1)

        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmUsageRecords_t2 = response[1]

        vmUsage_t2 = sum(float(record.rawusage)
                         for record in vmUsageRecords_t2)

        self.debug(vmUsage_t1)
        self.debug(vmUsage_t2)

        self.assertTrue(
            vmUsage_t1 <
            vmUsage_t2,
            "Vm usage should be running after\
                vm is migrated")

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_positive_tests_usage(self):
        """
        Steps:
        # 1.  Add VM in VPC network, verify that
        #     usage is genrated for source nat ip pf network in vpc
        # 2.  Acquire a public ip in VPC network and verify
              usage is genrated for the public ip
        # 3.  Create multiple PF rule on this ip in VPC network,
              and verify that usage is generated for both pf rules
        # 4.  Enable vpn on source nat ip in vpc network
        # 5.  Add 2 vpn user
              And verify that usage is genrated for both the vpn users
        # 6.  Delete one VPn user, and verify that usage is stopped
              for deleted user
        # 7.  Open Egress rules on this VPC network
        # 8.  Create network traffic on this network ping www.google.com,
              and verify that usage is genrated for network traffic
        # 9.  Delete onePF rule in VPC network
              And verify that usage is stopped for the pf rule
        # 10. Stop  router for VPC network
              Verify iptables counters are reset when domR stops
        #     Verify current_bytes in user_statistics table are moved to
              net_bytes
        #     Verify currnt_bytes becomes zero
        # 11. Start router for VPC network
              Verify iptables counters are reset when domR starts
        #     Verify a diff of total (current_bytes + net_bytes) in previous
              aggregation period and current period will give the network usage
        """

        # Step 1
        # Create VM in account
        vpc_off = VpcOffering.create(
            self.apiclient,
            self.testdata["vpc_offering"]
        )

        vpc_off.update(self.apiclient, state='Enabled')

        self.testdata["vpc"]["cidr"] = '10.1.1.0/24'
        vpc = VPC.create(
            self.userapiclient,
            self.testdata["vpc"],
            vpcofferingid=vpc_off.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.testdata["isolated_network"]["zoneid"] = self.zone.id
        isolated_network = Network.create(
            self.userapiclient,
            self.testdata["isolated_network"],
            self.account.name,
            self.account.domainid,
            vpcid=vpc.id,
            networkofferingid=self.isolated_network_offering_vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0")

        # Create VM in account
        virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[isolated_network.id]
        )

        # Checking usage for newly added network in VPC
        response = self.listUsageRecords(usagetype=13)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        nwOfferingUsageRecords = [
            record for record in usageRecords if self.isolated_network_offering_vpc.id == record.offeringid]

        self.assertTrue(validateList(nwOfferingUsageRecords)[0] == PASS,
                        "Network Offering usage record list validation failed")

        self.assertTrue(float(nwOfferingUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for isolated network offering")

        # Step 2 (Verification of usage is done together for
        # multiple steps)
        # Acquiring public IP
        public_ip = PublicIPAddress.create(self.userapiclient,
                                           accountid=self.account.name,
                                           zoneid=self.zone.id,
                                           domainid=self.account.domainid,
                                           networkid=isolated_network.id,
                                           vpcid=vpc.id
                                           )

        # Step 3
        # Create NAT rule
        nat_rule_1 = NATRule.create(self.userapiclient,
                                    virtual_machine,
                                    self.testdata["natrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    openfirewall=False,
                                    networkid=isolated_network.id,
                                    vpcid=vpc.id
                                    )

        self.debug("Adding NetworkACL rules to make NAT rule accessible")
        NetworkACL.create(self.userapiclient,
                          networkid=isolated_network.id,
                          services=self.testdata["natrule"],
                          traffictype='Ingress'
                          )

        # Step 7:
        NetworkACL.create(self.userapiclient,
                          networkid=isolated_network.id,
                          services=self.testdata["natrule"],
                          traffictype='Egress'
                          )

        self.testdata["natrule"]["privateport"] = 23
        self.testdata["natrule"]["publicport"] = 23

        nat_rule_2 = NATRule.create(self.userapiclient,
                                    virtual_machine,
                                    self.testdata["natrule"],
                                    ipaddressid=public_ip.ipaddress.id,
                                    openfirewall=False,
                                    networkid=isolated_network.id,
                                    vpcid=vpc.id
                                    )

        ipAddresses = PublicIPAddress.list(
            self.userapiclient,
            vpcid=vpc.id,
            issourcenat=True,
            listall=True,
            forvirtualnetwork=True)

        sourceNatIP = ipAddresses[0]

        # Usage verification section
        # Checking source nat IP usage
        response = self.listUsageRecords(usagetype=3)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        ipUsageRecords = [record for record in usageRecords
                          if sourceNatIP.id == record.usageid]

        self.assertTrue(validateList(ipUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(ipUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for source NAT ip")

        # Checking public IP usage
        ipUsageRecords = [record for record in usageRecords
                          if public_ip.ipaddress.id == record.usageid]

        self.assertTrue(validateList(ipUsageRecords)[0] == PASS,
                        "IP usage record list validation failed")

        self.assertTrue(float(ipUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for source NAT ip")

        # Verifying NAT rule usage
        response = self.listUsageRecords(usagetype=12, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRuleUsageRecords = [record for record in usageRecords
                               if nat_rule_1.id == record.usageid]

        self.assertTrue(validateList(natRuleUsageRecords)[0] == PASS,
                        "NAT rule usage record list validation failed")

        self.assertTrue(float(natRuleUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for nat rule")

        natRuleUsageRecords = [record for record in usageRecords
                               if nat_rule_2.id == record.usageid]

        self.assertTrue(validateList(natRuleUsageRecords)[0] == PASS,
                        "NAT rule usage record list validation failed")

        self.assertTrue(float(natRuleUsageRecords[0].rawusage) > 0,
                        "Raw usage not started for nat rule")

        # Step 4:
        # Create VPN for source NAT ip
        Vpn.create(self.apiclient,
                   sourceNatIP.id,
                   account=self.account.name,
                   domainid=self.account.domainid)

        self.debug("Verifying the remote VPN access")
        vpns = Vpn.list(self.apiclient,
                        publicipid=sourceNatIP.id,
                        listall=True)
        self.assertEqual(
            isinstance(vpns, list),
            True,
            "List VPNs shall return a valid response"
        )

        # Step 5:
        vpnuser_1 = VpnUser.create(
            self.apiclient,
            self.testdata["vpn_user"]["username"],
            self.testdata["vpn_user"]["password"],
            account=self.account.name,
            domainid=self.account.domainid,
            rand_name=True
        )

        vpnuser_2 = VpnUser.create(
            self.apiclient,
            self.testdata["vpn_user"]["username"],
            self.testdata["vpn_user"]["password"],
            account=self.account.name,
            domainid=self.account.domainid,
            rand_name=True
        )

        # Checking VPN usage
        response = self.listUsageRecords(usagetype=14)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        vpnUserUsageRecords_1 = [record for record in usageRecords
                                 if vpnuser_1.id == record.usageid]

        vpnuser1_rawusage = sum(float(record.rawusage)
                                for record in vpnUserUsageRecords_1)

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact VPN user creation time
        response = self.getEventCreatedDateTime(vpnuser_1.username)
        self.assertEqual(response[0], PASS, response[1])
        vpnUserCreatedDateTime = response[1]
        self.debug("VPN creation date: %s" % vpnUserCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - vpnUserCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("VPN user expected usage: %s" % expectedUsage)

        actualUsage = format(vpnuser1_rawusage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        vpnUserUsageRecords_2 = [record for record in usageRecords
                                 if vpnuser_2.id == record.usageid]

        self.assertTrue(validateList(vpnUserUsageRecords_2)[0] == PASS,
                        "VPN user usage record list validation failed")

        vpnuser2_rawusage = sum(float(record.rawusage)
                                for record in vpnUserUsageRecords_2)

        # Checking exact VPN user creation time
        response = self.getEventCreatedDateTime(vpnuser_2.username)
        self.assertEqual(response[0], PASS, response[1])
        vpnUserCreatedDateTime = response[1]
        self.debug("VPN creation date: %s" % vpnUserCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - vpnUserCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("VPN user expected usage: %s" % expectedUsage)

        actualUsage = format(vpnuser2_rawusage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # Step 6:
        vpnuser_1.delete(self.apiclient)

        # Verify that VPN usage for user stopped
        response = self.listUsageRecords(usagetype=14)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        vpnuser_1_Usage_t1 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if vpnuser_1.id == record.usageid])

        response = self.listUsageRecords(usagetype=14)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        vpnuser_1_Usage_t2 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if vpnuser_1.id == record.usageid])

        self.assertTrue(
            vpnuser_1_Usage_t1 == vpnuser_1_Usage_t2,
            "vpn user usage should be stopped once the user is deleted")

        # Step 7:

        # Step 8:
        ssh_client = virtual_machine.get_ssh_client(
            ipaddress=public_ip.ipaddress.ipaddress
        )

        res = ssh_client.execute("ping -c 1 www.google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )
        # Verifying usage for bytes received - START
        routers = list_routers(
            self.apiclient,
            networkid=isolated_network.id,
            listall=True
        )
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed")
        router = routers[0]
        result = self.getCommandResultFromRouter(
            router,
            "iptables -L NETWORK_STATS -n -v -x")

        self.debug("iptables -L NETWORK_STATS -n -v -x: %s" % result)

        bytesReceivedIptableRows = [record for record in result if
                                    "eth2   eth0" in record]
        self.debug("bytes received rows: %s" % bytesReceivedIptableRows)
        bytesReceivedOnRouter = sum(
            int(record[1]) for record in [x.split() for x in bytesReceivedIptableRows])

        self.debug(
            "Bytes received extracted from router: %s" %
            bytesReceivedOnRouter)

        # Verify that bytes received in usage are equal to
        # as shown on router
        response = self.listUsageRecords(usagetype=5)
        self.assertEqual(response[0], PASS, response[1])
        bytesReceivedUsage = sum(
            int(record.rawusage) for record in response[1])

        self.assertTrue(bytesReceivedUsage ==
                        bytesReceivedOnRouter,
                        "Total bytes received usage should be \
                        equal to bytes received on router")

        # Verifying usage for bytes received - END

        # Step 9:
        # Delete one NAT rule
        nat_rule_2.delete(self.userapiclient)

        response = self.listUsageRecords(usagetype=12)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRule_2_Usage_t1 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if nat_rule_2.id == record.usageid])

        response = self.listUsageRecords(usagetype=12)
        self.assertEqual(response[0], PASS, response[1])
        usageRecords = response[1]

        natRule_2_Usage_t2 = sum(float(record.rawusage) for record
                                 in [record for record in usageRecords
                                     if nat_rule_2.id == record.usageid])

        self.assertTrue(
            natRule_2_Usage_t1 == natRule_2_Usage_t2,
            "NAT rule usage should be stopped once the rule is deleted")

        # Step 10:

        qresultset = self.dbclient.execute(
            "select id from account where account_name = '%s';"
            % self.account.name
        )
        accountid = qresultset[0][0]
        self.debug("accountid: %s" % accountid)

        qresultset = self.dbclient.execute(
            "select current_bytes_sent, current_bytes_received  from user_statistics where account_id = '%s';" %
            accountid,
            db="cloud_usage")[0]

        currentBytesSentBeforeRouterStop = qresultset[0]
        currentBytesReceivedBeforeRouterStop = qresultset[1]

        self.debug(currentBytesSentBeforeRouterStop)
        self.debug(currentBytesReceivedBeforeRouterStop)

        # Stop the VPC Router
        routers = Router.list(
            self.api_client,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "List Routers should return a valid list"
        )
        router = routers[0]
        self.debug("Stopping the router with ID: %s" % router.id)

        Router.stop(
            self.apiclient,
            id=router.id
        )

        response = verifyRouterState(
            self.apiclient,
            router.id,
            "stopped")
        self.assertEqual(response[0], PASS, response[1])

        # TODO: Verify iptables counters are reset when domR stops

        qresultset = self.dbclient.execute(
            "select current_bytes_sent, current_bytes_received, net_bytes_sent, net_bytes_received from user_statistics where account_id = '%s';" %
            accountid,
            db="cloud_usage")[0]

        currentBytesSentAfterRouterStop = int(qresultset[0])
        currentBytesReceivedAfterRouterStop = int(qresultset[1])
        netBytesSentAfterRouterStop = int(qresultset[0])
        netBytesReceivedAfterRouterStop = int(qresultset[1])

        self.debug(currentBytesSentAfterRouterStop)
        self.debug(currentBytesReceivedAfterRouterStop)
        self.debug(netBytesSentAfterRouterStop)
        self.debug(netBytesReceivedAfterRouterStop)

        self.assertTrue(
            (currentBytesSentAfterRouterStop +
             currentBytesReceivedAfterRouterStop) == 0,
            "Current bytes should be 0")

        self.assertTrue(
            (currentBytesSentBeforeRouterStop +
             currentBytesReceivedBeforeRouterStop) == (
                netBytesSentAfterRouterStop +
                netBytesReceivedAfterRouterStop),
            "current bytes should be moved to net bytes")

        # Step 11:
        # Start the router
        Router.start(
            self.apiclient,
            id=router.id
        )

        response = verifyRouterState(
            self.apiclient,
            router.id,
            "running")
        self.assertEqual(response[0], PASS, response[1])

        # TODO
        # Verify iptables counters are reset when domR starts
        # Verify a diff of total (current_bytes + net_bytes) in previous
        # aggregation period and current period will give the network usage
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_08_checkNewVolumein_listUsageRecords(self):
        """ Test case to check if new volume crated after
        restore VM is listed in listUsageRecords
        # 1. Launch a VM
        # 2. Restore the VM
        # 3. Check if the new volume created is listed in listUsageRecords API
        """

        # Step 1
        vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        volumes_root_list = list_volumes(
            self.apiclient,
            virtualmachineid=vm.id,
            type='ROOT',
            listall=True
        )
        list_validation = validateList(volumes_root_list)

        self.assertEqual(
            list_validation[0],
            PASS,
            "Volume list validation failed due to %s" %
            list_validation[2])

        root_volume = volumes_root_list[0]

        # Step 2
        vm.restore(self.apiclient)

        qresultset = self.dbclient.execute(
            "select id from volumes where name='%s' and state='Ready';" %
            root_volume.name)

        db_list_validation = validateList(qresultset)

        self.assertEqual(
            db_list_validation[0],
            PASS,
            "Database list validation failed due to %s" %
            db_list_validation[2])

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        volumeCheck = "Volume Id: " + str(qresultset[0][0]) + " usage time"

        response = self.listUsageRecords(usagetype=6)
        self.assertEqual(response[0], PASS, response[1])
        UsageRecords = [record for record in response[1]
                        if volumeCheck in record.description]
        # Step 3
        if not UsageRecords:
            self.fail(
                "listUsageRecords not returning usage for newly created volume")


class TestUsageDataAggregatior(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestUsageDataAggregatior, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def listUsageTypes(self, apiclient=None):
        """ List Usage Types
        """
        try:
            usageTypes = Usage.listTypes(
                self.apiclient
            )

            self.assertEqual(
                validateList(usageTypes)[0],
                PASS,
                "usage types list validation failed")

            return [PASS, usageTypes]
        except Exception as e:
            return [FAIL, e]

        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_positive_tests_usagetypes_listTypes(self):
        """ 1. List Usage Types
            2. Verify Usage Id and Type mapping
        """
        usageTypes = [
            {
                "usagetypeid": 1, "description": 'Running Vm Usage'}, {
                "usagetypeid": 2, "description": 'Allocated Vm Usage'}, {
                "usagetypeid": 3, "description": 'IP Address Usage'}, {
                "usagetypeid": 4, "description": 'Network Usage (Bytes Sent)'}, {
                "usagetypeid": 5, "description": 'Network Usage (Bytes Received)'}, {
                "usagetypeid": 6, "description": 'Volume Usage'}, {
                "usagetypeid": 7, "description": 'Template Usage'}, {
                "usagetypeid": 8, "description": 'ISO Usage'}, {
                "usagetypeid": 9, "description": 'Snapshot Usage'}, {
                "usagetypeid": 11, "description": 'Load Balancer Usage'}, {
                "usagetypeid": 12, "description": 'Port Forwarding Usage'}, {
                "usagetypeid": 13, "description": 'Network Offering Usage'}, {
                "usagetypeid": 14, "description": 'VPN users usage'
            }
        ]
        listTypes = []
        response = self.listUsageTypes()
        respTypes = response[1]

        for res in respTypes:
            dictTypes = {
                "usagetypeid": res.usagetypeid,
                "description": res.description}
            listTypes.append(dictTypes)

        for type in usageTypes:
            if type not in listTypes:
                self.fail("Usage Type %s not present in list" % type)

        return


class TestUsageDirectMeteringBasicZone(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(
            TestUsageDirectMeteringBasicZone,
            cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls._cleanup = []
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        isUsageJobRunning = cls.IsUsageJobRunning()
        cls.usageJobNotRunning = False
        if not isUsageJobRunning:
            cls.usageJobNotRunning = True
            return

        if cls.testdata["configurableData"][
                "setUsageConfigurationThroughTestCase"]:
            cls.setUsageConfiguration()
            cls.RestartServers()
        else:
            currentMgtSvrTime = cls.getCurrentMgtSvrTime()
            dateTimeSplit = currentMgtSvrTime.split("/")
            cls.curDate = dateTimeSplit[0]

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        try:
            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenable:
                cls.testdata["service_offering"]["storagetype"] = 'local'

            # Create 2 service offerings with different values for
            # for cpunumber, cpuspeed, and memory

            cls.testdata["service_offering"]["cpunumber"] = "1"
            cls.testdata["service_offering"]["cpuspeed"] = "128"
            cls.testdata["service_offering"]["memory"] = "256"

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            configs = Configurations.list(
                cls.apiclient,
                name='usage.stats.job.aggregation.range'
            )

            # Set the value for one more minute than
            # actual range to be on safer side
            cls.usageJobAggregationRange = (
                int(configs[0].value) + 1) * 60  # in seconds
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        if self.usageJobNotRunning:
            self.skipTest("Skipping test because usage job not running")
        # Create an account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(self.account)
        # Create user api client of the account
        self.userapiclient = self.testClient.getUserApiClient(
            UserName=self.account.name,
            DomainName=self.account.domain
        )

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUsageConfiguration(cls):
        """ Set the configuration parameters so that usage job runs
            every 10 miuntes """

        Configurations.update(
            cls.apiclient,
            name="enable.usage.server",
            value="true"
        )

        Configurations.update(
            cls.apiclient,
            name="usage.aggregation.timezone",
            value="GMT"
        )

        Configurations.update(
            cls.apiclient,
            name="usage.execution.timezone",
            value="GMT"
        )

        Configurations.update(
            cls.apiclient,
            name="usage.stats.job.aggregation.range",
            value="10"
        )

        currentMgtSvrTime = cls.getCurrentMgtSvrTime()
        dateTimeSplit = currentMgtSvrTime.split("/")
        cls.curDate = dateTimeSplit[0]
        timeSplit = dateTimeSplit[1].split(":")
        minutes = int(timeSplit[1])
        minutes += 5
        usageJobExecTime = timeSplit[0] + ":" + str(minutes)

        Configurations.update(
            cls.apiclient,
            name="usage.stats.job.exec.time",
            value=usageJobExecTime
        )

        return

    @classmethod
    def getCurrentMgtSvrTime(cls, format='%Y-%m-%d/%H:%M'):
        """ Get the current time from Management Server """

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "date +%s" % format
        return sshClient.execute(command)[0]

    @classmethod
    def RestartServers(cls):
        """ Restart management server and usage server """

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)

        command = "service cloudstack-usage restart"
        sshClient.execute(command)
        return

    @classmethod
    def IsUsageJobRunning(cls):
        """ Check that usage job is running on Management server or not"""

        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )

        command = "service cloudstack-usage status"
        response = str(sshClient.execute(command)).lower()
        if "unknown" in response:
            return False
        return True

    def listUsageRecords(self, usagetype, apiclient=None, startdate=None,
                         enddate=None, account=None, sleep=True):
        """List and return the usage record for given account
        and given usage type"""

        if sleep:
            # Sleep till usage job has run at least once after the operation
            self.debug(
                "Sleeping for %s seconds" %
                self.usageJobAggregationRange)
            time.sleep(self.usageJobAggregationRange)

        if not startdate:
            startdate = self.curDate
        if not enddate:
            enddate = self.curDate
        if not account:
            account = self.account
        if not apiclient:
            self.apiclient

        Usage.generateRecords(
            self.apiclient,
            startdate=startdate,
            enddate=enddate)

        try:
            usageRecords = Usage.listRecords(
                self.apiclient,
                startdate=startdate,
                enddate=enddate,
                account=account.name,
                domainid=account.domainid,
                type=usagetype)

            self.assertEqual(
                validateList(usageRecords)[0],
                PASS,
                "usage records list validation failed")

            return [PASS, usageRecords]
        except Exception as e:
            return [FAIL, e]
        return

    def getLatestUsageJobExecutionTime(self):
        """ Get the end time of latest usage job that has run successfully"""

        try:
            qresultset = self.dbclient.execute(
                "SELECT max(end_date) FROM usage_job WHERE success=1;",
                db="cloud_usage")

            self.assertNotEqual(
                len(qresultset),
                0,
                "Check DB Query result set"
            )

            lastUsageJobExecutionTime = qresultset[0][0]
            self.debug(
                "last usage job exec time: %s" %
                lastUsageJobExecutionTime)
            return [PASS, lastUsageJobExecutionTime]
        except Exception as e:
            return [FAIL, e]

    def getEventCreatedDateTime(self, resourceName):
        """ Get the created date/time of particular entity
            from cloud_usage.usage_event table """

        try:
            # Checking exact entity creation time
            qresultset = self.dbclient.execute(
                "select created from usage_event where resource_name = '%s';" %
                str(resourceName), db="cloud_usage")
            self.assertNotEqual(
                len(qresultset),
                0,
                "Check DB Query result set"
            )
            eventCreatedDateTime = qresultset[0][0]
        except Exception as e:
            return [FAIL, e]
        return [PASS, eventCreatedDateTime]

    @attr(tags=["basic"], required_hardware="true")
    def test_01_positive_tests_usage_basic_zone(self):
        """ Positive test for usage test path Basic Zone

        # 1.  Deploy VM in basic zone and verify that VM usage is generated
              for the account with correct service offering
        # 2.  SSH to VM and ping to external network
        # 3.  Verify correct network byte usage is generated for the account
        """
        # Create VM in account
        vm = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            mode=self.zone.networktype
        )

        # Checking running VM usage
        response = self.listUsageRecords(usagetype=1)
        self.assertEqual(response[0], PASS, response[1])
        vmRunningUsageRecords = [record for record in response[1]
                                 if record.virtualmachineid == vm.id]

        vmRunningRawUsage = sum(float(record.rawusage)
                                for record in vmRunningUsageRecords)

        self.assertEqual(vmRunningUsageRecords[0].offeringid,
                         self.service_offering.id,
                         "The service offering id in the usage record\
                        does not match with id of service offering\
                        with which the VM was created")

        self.assertEqual(vmRunningUsageRecords[0].templateid,
                         self.template.id,
                         "The template id in the usage record\
                        does not match with id of template\
                        with which the VM was created")

        response = self.listUsageRecords(usagetype=2, sleep=False)
        self.assertEqual(response[0], PASS, response[1])
        vmAllocatedUsageRecords = [record for record in response[1]
                                   if record.virtualmachineid == vm.id]

        vmAllocatedRawUsage = sum(float(record.rawusage)
                                  for record in vmAllocatedUsageRecords)

        self.debug("running vm usage: %s" % vmRunningRawUsage)
        self.debug("allocated vm usage: %s" % vmAllocatedRawUsage)

        self.assertTrue(
            vmRunningRawUsage < vmAllocatedRawUsage,
            "Allocated VM usage should be greater than Running VM usage")

        # Getting last usage job execution time
        response = self.getLatestUsageJobExecutionTime()
        self.assertEqual(response[0], PASS, response[1])
        lastUsageJobExecTime = response[1]

        # Checking exact VM creation time
        response = self.getEventCreatedDateTime(vm.name)
        self.assertEqual(response[0], PASS, response[1])
        vmCreatedDateTime = response[1]
        self.debug("Vm creation date: %s" % vmCreatedDateTime)

        # We have to get the expected usage count in hours as the rawusage returned by listUsageRecords
        # is also in hours
        expectedUsage = format(
            ((lastUsageJobExecTime - vmCreatedDateTime).total_seconds() / 3600),
            ".2f")
        self.debug("VM expected usage: %s" % expectedUsage)

        actualUsage = format(vmAllocatedRawUsage, ".2f")

        self.assertEqual(
            expectedUsage,
            actualUsage,
            "expected usage %s and actual usage %s not matching" %
            (expectedUsage,
             actualUsage))

        # TODO: Add traffic sentinel, because it is needed in basic zone
        # to gather network traffic values
        """ssh_client = vm.get_ssh_client()

        res = ssh_client.execute("ping -c 1 www.google.com")
        result = str(res)

        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful"
        )

        result = str(res[1])
        bytesReceived = int(result.split("bytes", 1)[0])
        response = self.listUsageRecords(usagetype=5)
        self.assertEqual(response[0], PASS, response[1])
        bytesReceivedUsageRecord = sum(
            int(record.rawusage) for record in response[1])

        self.assertTrue(bytesReceivedUsageRecord >=
                        bytesReceived,
                        "Total bytes received usage should be greater than\
                        or equal to bytes received by pinging\
                        www.google.com")"""
        return
