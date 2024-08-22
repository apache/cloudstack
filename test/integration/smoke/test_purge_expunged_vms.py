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
""" BVT tests for purging expunged VMs and their resources
"""
# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackAPI import (purgeExpungedResources,
                                  listInfrastructure,
                                  listManagementServers)
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain,
                             ServiceOffering,
                             DiskOffering,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             Configurations)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import (random_gen)
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
import logging
# Import System modules
import time
from datetime import datetime, timedelta
import pytz
import threading


_multiprocess_shared_ = True
DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"

class TestPurgeExpungedVms(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestPurgeExpungedVms, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.dbConnection = cls.testClient.getDbConnection()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__


        cls.hypervisor = cls.testClient.getHypervisorInfo().lower()
        cls.hypervisorIsSimulator = False
        if cls.hypervisor == 'simulator':
            cls.hypervisorIsSimulator = True

        cls._cleanup = []
        cls.logger = logging.getLogger('TestPurgeExpungedVms')
        cls.logger.setLevel(logging.DEBUG)

        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"])
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"])
        cls._cleanup.append(cls.compute_offering)

        cls.purge_resource_compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"],
            purgeresources=True)
        cls._cleanup.append(cls.purge_resource_compute_offering)

        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-network_offering"],
        )
        cls._cleanup.append(cls.network_offering)
        cls.network_offering.update(cls.apiclient, state='Enabled')
        cls.services["network"]["networkoffering"] = cls.network_offering.id

        cls.domain1 = Domain.create(
            cls.apiclient,
            cls.services["domain"])
        cls._cleanup.append(cls.domain1)
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain1.id)
        cls._cleanup.append(cls.account)
        cls.userapiclient = cls.testClient.getUserApiClient(
            UserName=cls.account.name,
            DomainName=cls.account.domain
        )
        cls.l2_network = Network.create(
            cls.userapiclient,
            cls.services["l2-network"],
            zoneid=cls.zone.id,
            networkofferingid=cls.network_offering.id
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

    @classmethod
    def tearDownClass(cls):
        super(TestPurgeExpungedVms, cls).tearDownClass()

    def updateVmCreatedRemovedInDb(self, vm_id, timestamp):
        # Assuming DB is UTC
        utc_timestamp = datetime.strptime(timestamp, DATETIME_FORMAT).astimezone(pytz.utc).strftime(DATETIME_FORMAT)
        logging.info("Updating VM: %s created and removed in DB with timestamp: %s" % (vm_id, timestamp))
        query = "UPDATE cloud.vm_instance SET created='%s', removed='%s' WHERE uuid='%s'" % (utc_timestamp, utc_timestamp, vm_id)
        self.dbConnection.execute(query)

    def setupExpungedVm(self, timestamp):
        logging.info("Setting up expunged VM with timestamp: %s" % timestamp)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.compute_offering.id,
            networkids=self.l2_network.id
        )
        self.cleanup.append(vm)
        vm_id = vm.id
        self.vm_ids[timestamp] = vm_id
        vm.delete(self.apiclient, expunge=True)
        self.cleanup.remove(vm)
        self.updateVmCreatedRemovedInDb(vm_id, timestamp)

    def setupExpungedVms(self):
        logging.info("Setup VMs")
        self.vm_ids = {}
        self.threads = []
        days = 3
        for i in range(days):
            logging.info("Setting up expunged VMs for day: %d" % (i + 1))
            thread = threading.Thread(target=self.setupExpungedVm, args=(self.timestamps[i],))
            self.threads.append(thread)
            thread.start()

        for index, thread in enumerate(self.threads):
            logging.info("Before joining thread %d." % index)
            thread.join()
            logging.info("Thread %d done" % index)

    def setUp(self):
        self.cleanup = []
        self.changedConfigurations = {}
        self.staticConfigurations = []
        if 'service_offering' in self._testMethodName:
            return
        if 'background_task' in self._testMethodName and self.hypervisorIsSimulator:
            return
        self.days = 3
        self.timestamps = []
        for i in range(self.days):
            days_ago = (self.days - i) * 2
            now = (datetime.now() - timedelta(days = days_ago))
            timestamp = now.strftime(DATETIME_FORMAT)
            self.timestamps.append(timestamp)
        self.setupExpungedVms()

    def tearDown(self):
        restartServer = False
        for config in self.changedConfigurations:
            value = self.changedConfigurations[config]
            logging.info("Reverting value of config: %s to %s" % (config, value))
            Configurations.update(self.apiclient,
                config,
                value=value)
            if config in self.staticConfigurations:
                restartServer = True
        if restartServer:
            self.restartAllManagementServers()
        super(TestPurgeExpungedVms, self).tearDown()

    def executePurgeExpungedResources(self, start_date, end_date):
        cmd = purgeExpungedResources.purgeExpungedResourcesCmd()
        if start_date is not None:
            cmd.startdate = start_date
        if end_date is not None:
            cmd.enddate = end_date
        self.apiclient.purgeExpungedResources(cmd)

    def getVmsInDb(self, vm_ids):
        vm_id_str = "','".join(vm_ids)
        vm_id_str = "'" + vm_id_str + "'"
        query = "SELECT * FROM cloud.vm_instance WHERE uuid IN (%s)" % vm_id_str
        response = self.dbConnection.execute(query)
        logging.info("DB response from VM: %s:: %s" % (vm_id_str, response))
        return response

    def validatePurgedVmEntriesInDb(self, purged, not_purged):
        if purged is not None:
            response = self.getVmsInDb(purged)
            self.assertTrue(response is None or len(response) == 0,
                "Purged VMs still present in DB")
        if not_purged is not None:
            response = self.getVmsInDb(not_purged)
            self.assertTrue(response is not None or len(response) == len(not_purged),
                "Not purged VM not present in DB")

    def changeConfiguration(self, name, value):
        current_config = Configurations.list(self.apiclient, name=name)[0]
        if current_config.value == value:
            return
        logging.info("Current value for config: %s is %s, changing it to %s" % (name, current_config.value, value))
        self.changedConfigurations[name] = current_config.value
        if current_config.isdynamic == False:
            self.staticConfigurations.append(name)
        Configurations.update(self.apiclient,
            name,
            value=value)

    def isManagementUp(self):
        try:
            self.apiclient.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

    def getManagementServerIps(self):
        if self.mgtSvrDetails["mgtSvrIp"] == 'localhost':
            return None
        cmd = listManagementServers.listManagementServersCmd()
        servers = self.apiclient.listManagementServers(cmd)
        active_server_ips = []
        active_server_ips.append(self.mgtSvrDetails["mgtSvrIp"])
        for idx, server in enumerate(servers):
            if server.state == 'Up' and server.serviceip != self.mgtSvrDetails["mgtSvrIp"]:
                active_server_ips.append(server.serviceip)
        return active_server_ips

    def restartAllManagementServers(self):
        """Restart all management servers
         Assumes all servers have same username and password"""
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            self.staticConfigurations.clear()
            self.fail("MS restarts cannot be done on %s" % self.mgtSvrDetails["mgtSvrIp"])
            return False
        self.debug("Restarting all management server")
        for idx, server_ip in enumerate(server_ips):
            self.debug(f"Restarting management server #{idx} with IP {server_ip}")
            sshClient = SshClient(
                server_ip,
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"]
            )
            command = "service cloudstack-management stop"
            sshClient.execute(command)
            command = "service cloudstack-management start"
            sshClient.execute(command)
            if idx == 0:
                # Wait before restarting other management servers to make the first as oldest running
                time.sleep(10)

        # Waits for management to come up in 10 mins, when it's up it will continue
        timeout = time.time() + (10 * 60)
        while time.time() < timeout:
            if self.isManagementUp() is True: return True
            time.sleep(5)
        self.debug("Management server did not come up, failing")
        return False

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_purge_expunged_api_vm_start_date(self):
        self.executePurgeExpungedResources(self.timestamps[1], None)
        self.validatePurgedVmEntriesInDb(
            [self.vm_ids[self.timestamps[1]], self.vm_ids[self.timestamps[2]]],
            [self.vm_ids[self.timestamps[0]]]
        )

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_purge_expunged_api_vm_end_date(self):
        self.executePurgeExpungedResources(None, self.timestamps[1])
        self.validatePurgedVmEntriesInDb(
            [self.vm_ids[self.timestamps[0]], self.vm_ids[self.timestamps[1]]],
            [self.vm_ids[self.timestamps[2]]]
        )

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_purge_expunged_api_vm_start_end_date(self):
        self.executePurgeExpungedResources(self.timestamps[0], self.timestamps[2])
        self.validatePurgedVmEntriesInDb(
            [self.vm_ids[self.timestamps[0]], self.vm_ids[self.timestamps[1]], self.vm_ids[self.timestamps[2]]],
            None
        )

    @attr(tags=["advanced"], required_hardware="true")
    def test_04_purge_expunged_api_vm_no_date(self):
        self.executePurgeExpungedResources(None, None)
        self.validatePurgedVmEntriesInDb(
            [self.vm_ids[self.timestamps[0]], self.vm_ids[self.timestamps[1]], self.vm_ids[self.timestamps[2]]],
            None
        )

    @attr(tags=["advanced", "skip_setup_vms"], required_hardware="true")
    def test_05_purge_expunged_vm_service_offering(self):
        purge_delay = 181
        self.changeConfiguration('expunged.resource.purge.job.delay', purge_delay)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.purge_resource_compute_offering.id,
            networkids=self.l2_network.id
        )
        self.cleanup.append(vm)
        vm_id = vm.id
        vm.delete(self.apiclient, expunge=True)
        self.cleanup.remove(vm)
        wait = 1.25 * purge_delay
        logging.info("Waiting for 1.25x%d = %d seconds for VM to get purged" % (purge_delay, wait))
        time.sleep(wait)
        self.validatePurgedVmEntriesInDb(
            [vm_id],
            None
        )

    @skipTestIf("hypervisorIsSimulator")
    @attr(tags=["advanced"], required_hardware="true")
    def test_06_purge_expunged_vm_background_task(self):
        purge_task_delay = 120
        self.changeConfiguration('expunged.resources.purge.enabled', 'true')
        self.changeConfiguration('expunged.resources.purge.delay', purge_task_delay)
        self.changeConfiguration('expunged.resources.purge.interval', int(purge_task_delay/2))
        self.changeConfiguration('expunged.resources.purge.keep.past.days', 1)
        if len(self.staticConfigurations) > 0:
            self.restartAllManagementServers()
        wait_multiple = 2
        wait = wait_multiple * purge_task_delay
        logging.info(f"Waiting for {wait_multiple}x{purge_task_delay} = {wait} seconds for background task to execute")
        time.sleep(wait)
        logging.debug("Validating expunged VMs")
        self.validatePurgedVmEntriesInDb(
            [self.vm_ids[self.timestamps[0]], self.vm_ids[self.timestamps[1]], self.vm_ids[self.timestamps[2]]],
            None
        )
