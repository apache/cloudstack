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
Test cases for zone level settings "system.vm.use.local.storage"
"""
# Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.codes import FAILED, PASS
from requests.exceptions import ConnectionError

import time
from nose.plugins.attrib import attr
from ddt import ddt, data


def destroy_systemvm(self, type):
    """
    Destroy system vms
    #1-List  system vms for current zone
    #2-Destroy system vm
    #3-Check if system vm came up after destroy
    #4-check  system vm storage type in disk offering
    """
    list_response = list_ssvms(
        self.apiclient,
        systemvmtype=type,
        zoneid=self.zone.id
    )

    self.assertEqual(
        validateList(list_response)[0],
        PASS,
        "Check List ssvm response for  %s" %
        type)

    response = list_response[0]
    self.debug("Destroying CPVM: %s" % response.id)
    cmd = destroySystemVm.destroySystemVmCmd()
    cmd.id = response.id
    self.apiclient.destroySystemVm(cmd)

    timeout = self.testdata["timeout"]
    while True:
        time.sleep(self.testdata["sleep"])
        list_response = list_ssvms(
            self.apiclient,
            systemvmtype=type,
            zoneid=self.zone.id
        )
        if validateList(list_response)[0] == PASS:
            if list_response[0].state == 'Running':
                break
        if timeout == 0:
            raise Exception("List %s call failed!" % type)
        timeout = timeout - 1


def storage_check(self, type, value):
    """test if system vms are using local or shared storage
    #1-Get zone id from db using self.zone.id
    #2-Get service offering id from vm_instance table for running system vms
    #3-Get use_local_storage value from disk_offering table
    #4-Verify storage type"""
    query_zone_id = self.dbclient.execute(
        "select id from data_center where uuid= '%s';" % self.zone.id
    )
    query_so_id = self.dbclient.execute(
        "select service_offering_id from vm_instance where type='%s'and "
        "state='Running' and data_center_id= '%s';" %
        (type, query_zone_id[0][0]))

    query_disk_offering = self.dbclient.execute(
        "select use_local_storage from disk_offering  where id= '%s';" %
        query_so_id[0][0])

    if value == 1:
        self.assertEqual(query_disk_offering[0][0],
                         1,
                         "system vm is not using local storage"
                         )
    elif value == 0:
        self.assertEqual(query_disk_offering[0][0],
                         0,
                         "system vm is not using shared storage"
                         )
    else:
        # evil ValueError that doesn't tell you what the wrong value was
        raise ValueError


def create_system_so(self, offering_type, storage_type):
    """Create system offerings """
    self.testdata["service_offerings"]["tiny"]["issystem"] = "true"
    self.testdata["service_offerings"]["tiny"]["systemvmtype"] = offering_type
    self.testdata["service_offerings"]["tiny"]["storagetype"] = storage_type

    service_offering = ServiceOffering.create(
        self.apiclient,
        self.testdata["service_offerings"]["tiny"]
    )

    if service_offering is None:
        raise Exception("service offering not created")

    list_service_response = list_service_offering(
        self.apiclient,
        id=service_offering.id,
        issystem='true'
    )
    self.assertEqual(
        validateList(list_service_response)[0],
        PASS,
        "Check List srvice offering response for  %s" %
        type)

    self.debug(
        "Created service offering with ID: %s" %
        service_offering.id)

    self.assertEqual(
        list_service_response[0].cpunumber,
        self.testdata["service_offerings"]["tiny"]["cpunumber"],
        "Check server id in createServiceOffering"
    )
    self.assertEqual(
        list_service_response[0].cpuspeed,
        self.testdata["service_offerings"]["tiny"]["cpuspeed"],
        "Check cpuspeed in createServiceOffering"
    )
    self.assertEqual(
        list_service_response[0].displaytext,
        self.testdata["service_offerings"]["tiny"]["displaytext"],
        "Check server displaytext in createServiceOfferings"
    )
    self.assertEqual(
        list_service_response[0].memory,
        self.testdata["service_offerings"]["tiny"]["memory"],
        "Check memory in createServiceOffering"
    )
    self.assertEqual(
        list_service_response[0].name,
        self.testdata["service_offerings"]["tiny"]["name"],
        "Check name in createServiceOffering"
    )
    self.assertEqual(
        list_service_response[0].storagetype,
        self.testdata["service_offerings"]["tiny"]["storagetype"],
        "Check storagetype in createServiceOffering"
    )
    self._cleanup.append(service_offering)
    return service_offering.id


def restart_ms(self):
    """Restart MS
    #1-ssh into m/c running MS
    #2-restart ms
    #3-verify the response
    #4-loop unitl you get list_zone api answer """
    sshClient = SshClient(
        self.mgtSvrDetails["mgtSvrIp"],
        22,
        self.mgtSvrDetails["user"],
        self.mgtSvrDetails["passwd"]
    )
    command = "service cloudstack-management restart"
    ms_restart_response = sshClient.execute(command)
    self.assertEqual(
        validateList(ms_restart_response)[0],
        PASS,
        "Check the MS restart response")
    self.assertEqual(
        ms_restart_response[0],
        'Stopping cloudstack-management:[  OK  ]',
        "MS i not stopped"
    )
    self.assertEqual(
        ms_restart_response[1],
        'Starting cloudstack-management: [  OK  ]',
        "MS not started"
    )
    timeout = self.testdata["timeout"]
    while True:
        time.sleep(self.testdata["sleep"])
        try:
            list_response = Zone.list(
                self.apiclient
            )
            if validateList(list_response)[0] == PASS:
                break
        except ConnectionError as e:
            self.debug("list zone response is not available due to  %s" % e)

        if timeout == 0:
            raise Exception("Ms is not comming up !")
        timeout = timeout - 1


def update_global_settings(self, value, name, zoneid=None):
    """Update Gloabal/zonelevel settings and verify
    #1-Update configuration
    #2-Restart ms if zone id is None"""
    Configurations.update(self.apiclient,
                          name=name,
                          zoneid=zoneid,
                          value=value
                          )
    if zoneid is None:
        restart_ms(self)

    list_conf = Configurations.list(self.apiclient,
                                    name=name,
                                    zoneid=zoneid)

    self.assertEqual(
        validateList(list_conf)[0],
        PASS,
        "Check List configuration  response for  %s" % name)

    self.assertEqual(
        str(list_conf[0].value),
        str(value),
        "Check if configuration values are equal"
    )


def str_to_bool(s):
    """Converts str "True/False to Boolean TRUE/FALSE"""
    if s == 'true':
        return True
    elif s == 'false':
        return False
    else:
        # evil ValueError that doesn't tell you what the wrong value was
        raise ValueError


@ddt
class TestSystemVmLocalStorage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSystemVmLocalStorage, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.testdata = testClient.getParsedTestDataConfig()
        # Get Zone, and template Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata["mode"] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        list_local_storage_pool = StoragePool.list(
            cls.apiclient,
            scope='HOST',
            zoneid=cls.zone.id,

        )

        if list_local_storage_pool is None:

            Configurations.update(
                cls.apiclient,
                value='true',
                name='system.vm.use.local.storage',
                zoneid=cls.zone.id
            )

            # Restart MS
            sshClient = SshClient(
                cls.mgtSvrDetails["mgtSvrIp"],
                22,
                cls.mgtSvrDetails["user"],
                cls.mgtSvrDetails["passwd"]
            )
            command = "service cloudstack-management restart"
            ms_restart_response = sshClient.execute(command)

            if validateList(ms_restart_response)[0] != PASS:
                raise Exception("Check the MS restart response")
            if ms_restart_response[
                    0] != 'Stopping cloudstack-management:[  OK  ]':
                raise Exception("MS i not stopped")

            if ms_restart_response[
                    1] != 'Starting cloudstack-management: [  OK  ]':
                raise Exception("MS not started")

            timeout = cls.testdata["timeout"]
            while True:
                # time.sleep(cls.testdata["sleep"])
                try:
                    list_response = Zone.list(
                        cls.apiclient
                    )
                    if validateList(list_response)[0] == PASS:
                        break
                except ConnectionError as e:
                    cls.debug(
                        "list zone response is not available due to  %s" %
                        e)

                if timeout == 0:
                    raise Exception("Ms is not comming up !")

                time.sleep(cls.testdata["sleep"])
                timeout = timeout - 1

            list_local_storage_pool = StoragePool.list(
                cls.apiclient,
                scope='HOST',
                zoneid=cls.zone.id,

            )
            if list_local_storage_pool is None:
                raise Exception("Could not discover local storage pool")

        try:
            cls.account = Account.create(cls.apiclient,
                                         cls.testdata["account"],
                                         domainid=cls.domain.id
                                         )
            cls._cleanup.append(cls.account)

        except Exception as e:
            cls.tearDownClass()
            raise e

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning:Exception during cleanup: %s" % e)

    @attr(tags=["advanced", "basic"])
    @data(
        'consoleproxy',
        'secondarystoragevm',
        'domainrouter',
        'internalloadbalancervm')
    def test_01_list_system_offerngs(self, value):
        """List service offerings for systemvms and verify there should be two
        (local and shared) SO  for each system vm"""

        list_custom_so = ServiceOffering.list(self.apiclient,
                                              issystem='true',
                                              listall='true',
                                              systemvmtype=value
                                              )

        self.assertEqual(
            validateList(list_custom_so)[0],
            PASS,
            "Check List service offerings response for  %s" %
            value)

        local_custom_so = []
        for item in list_custom_so:
            if(str(item.defaultuse) == 'True'):
                local_custom_so.append(item.storagetype)

        self.assertEqual(
            len(local_custom_so),
            2,
            "Check default system offering for system vm type %s" % value)
        if 'local' in local_custom_so and 'shared' in local_custom_so:
            self.debug(
                "there are exactly to Service offerings{share,local} are "
                "there for system vm %s" %
                value)
        else:
            raise Exception(
                "check local and shared service offerings for %s" %
                value)

    @attr(tags=["advanced", "basic"])
    @data('consoleproxy', 'secondarystoragevm')
    def test_02_system_vm_storage(self, value):
        """ Check if system vms are honouring zone level setting
        system.vm.use.local.storage
        1-List zone level config
        2-update the zone level config with service offering uuid
        3-destroy system vms
        4-check used storage by system vms
        """
        # 1 List zone level config
        if value == "consoleproxy":
            update_global_settings(
                self,
                value=None,
                name="consoleproxy.service.offering")

        if value == "secondarystoragevm":
            update_global_settings(
                self,
                value=None,
                name="secstorage.service.offering")

        list_conf = Configurations.list(self.apiclient,
                                        name="system.vm.use.local.storage",
                                        zoneid=self.zone.id)
        self.assertEqual(
            validateList(list_conf)[0],
            PASS,
            "Check List configuration response for "
            "system.vm.use.local.storage")

        val = str_to_bool(list_conf[0].value)
        # 2 update the zone level config with service offering uuid
        update_global_settings(self,
                               value=((str(not(val)).lower())),
                               name='system.vm.use.local.storage',
                               zoneid=self.zone.id)

        # 3,4 for cpvm
        destroy_systemvm(self, value)
        storage_check(self, value, int(not(val)))

        # 2 update the zone level config with service offering uuid
        update_global_settings(
            self,
            value=(
                str(val).lower()),
            name='system.vm.use.local.storage',
            zoneid=self.zone.id)
        # 3,4 for cpvm
        destroy_systemvm(self, value)
        storage_check(self, value, int(val))

        # 1 List zone level config
        if value == "consoleproxy":
            update_global_settings(
                self,
                value=None,
                name="consoleproxy.service.offering")

        if value == "secondarystoragevm":
            update_global_settings(
                self,
                value=None,
                name="secstorage.service.offering")

    @attr(tags=["advanced", "basic"])
    @data('consoleproxy', 'secondarystoragevm')
    def test_03_custom_so(self, value):
        """
        update global setting with system offering and check if it is being
        honoured
        1-update zone level settings "system.vm.use.local.storage={true,false}}
        to use local storage
        2-create system offerings{shared,local}
        3-update global settings with system offering uuid and restart ms
        4-destroy system vms
        5-Check if new system vms are using offering  updated in global
        settings
        """

        # 1-update zone level settings "system.vm.use.local.storage"
        # to use local storage
        update_global_settings(
            self,
            value='true',
            name='system.vm.use.local.storage',
            zoneid=self.zone.id)
        # 2-create system offerings

        created_so_id = create_system_so(self, value, "shared")

        if value == "consoleproxy":
            name = "consoleproxy.service.offering"
        elif value == 'secondarystoragevm':
            name = 'secstorage.service.offering'
        else:
            raise Exception(
                "type paramter is not correct it should be  system vm "
                "type{console proxy,secsroragevm}")

        # 3-update global settings with system offering uuid
        update_global_settings(self, value=created_so_id, name=name)

        # 4-destroy system vms
        destroy_systemvm(self, value)

        # 5-Check if new system vms are using offering  updated in global
        # settings

        query_zone_id = self.dbclient.execute(
            "select id from data_center where uuid= '%s';" % self.zone.id
        )
        query_so_id = self.dbclient.execute(
            "select service_offering_id from vm_instance where type='%s'and "
            "state='Running' and data_center_id= '%s';" %
            (value, query_zone_id[0][0]))
        query_disk_offering = self.dbclient.execute(
            "select uuid from disk_offering  where id= '%s';" %
            query_so_id[0][0])

        self.assertEqual(
            created_so_id,
            query_disk_offering[0][0],
            "system vms are not using service offering mentioned in "
            "global settings")

        # 6-repeate 1 with system.vm.use.local.storage=false
        update_global_settings(
            self,
            value='false',
            name='system.vm.use.local.storage',
            zoneid=self.zone.id)
        # 7-repeate 2 with storage type local
        created_so_id = create_system_so(self, value, "local")
        # 8-repeate 3
        update_global_settings(self, value=created_so_id, name=name)

        # 9-repeate 4
        destroy_systemvm(self, value)
        # repeate 5
        query_zone_id = self.dbclient.execute(
            "select id from data_center where uuid= '%s';" % self.zone.id
        )
        query_so_id = self.dbclient.execute(
            "select service_offering_id from vm_instance where type='%s'and "
            "state='Running' and data_center_id= '%s';" %
            (value, query_zone_id[0][0]))
        query_disk_offering = self.dbclient.execute(
            "select uuid from disk_offering  where id= '%s';" %
            query_so_id[0][0])

        self.assertEqual(
            created_so_id,
            query_disk_offering[0][0],
            "system vms are not using service offering mentioned in"
            " global settings")

    @attr(tags=["advanced"])
    def test_04_router_vms(self):
        """ Check if router  vm is honouring zone level setting
        system.vm.use.local.storage"""

        # 1-list configurations
        list_conf = Configurations.list(self.apiclient,
                                        name="system.vm.use.local.storage",
                                        zoneid=self.zone.id)
        self.assertEqual(
            validateList(list_conf)[0],
            PASS,
            "Check List configuration response for "
            "system.vm.use.local.storage")

        # 2-create network offering
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.testdata["network_offering"],
            ispersistent='true'
        )

        # 3-list netwrok offerings
        list_nw_of = NetworkOffering.list(self.apiclient,
                                          id=self.network_offering.id)
        self.assertEqual(
            validateList(list_nw_of)[0],
            PASS,
            "Check the list network response"
        )
        self.assertEqual(
            str(list_nw_of[0].id),
            str(self.network_offering.id),
            "Check the created network offering id and "
            "listed network offering id"
        )
        self._cleanup.append(self.network_offering)

        # 4-Enable network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        # 5-List network offering
        list_nw_of1 = NetworkOffering.list(self.apiclient,
                                           id=self.network_offering.id)
        self.assertEqual(
            validateList(list_nw_of1)[0],
            PASS,
            "Check the list network response"
        )
        self.assertEqual(
            str(list_nw_of1[0].state),
            "Enabled",
            "Check the created network state"
        )

        # 6-crete network using network offering
        self.network = Network.create(
            self.apiclient,
            self.testdata["network"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        # 7-List network
        list_network = Network.list(self.apiclient,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    id=self.network.id)
        self.assertEqual(validateList(list_network)[0],
                         PASS,
                         "check list netwok response ")
        self.assertEqual(
            list_network[0].id,
            self.network.id,
            "List network id %s and created network id %s  does not match" %
            (list_network[0].id,
             self.network.id))

        # 8-List router
        list_router = Router.list(self.apiclient,
                                  networkid=self.network.id,
                                  accountid=self.account.name,
                                  domainid=self.account.domainid)

        self.assertEqual(
            validateList(list_router)[0],
            PASS,
            "check list router response")

        # 9-List service offerings
        list_so = ServiceOffering.list(self.apiclient,
                                       issystem='true',
                                       id=list_router[0].serviceofferingid
                                       )
        self.assertEqual(
            validateList(list_so)[0],
            PASS,
            "check list service offering response")
        if list_conf[0].value == 'true':
            storage_type = 'local'
            value1 = 'false'
        elif list_conf[0].value == 'false':
            storage_type = 'shared'
            value1 = 'true'
        else:
            raise Exception("check list_conf[0].value")
        self.assertEqual(
            list_so[0].storagetype,
            storage_type,
            "Check VR storage type and zone level settig"
        )

        # 10-Update zone level setting
        update_global_settings(
            self,
            value=value1,
            name="system.vm.use.local.storage",
            zoneid=self.zone.id)

        # 11-List configurations
        list_conf1 = Configurations.list(self.apiclient,
                                         name="system.vm.use.local.storage",
                                         zoneid=self.zone.id)
        self.assertEqual(
            validateList(list_conf1)[0],
            PASS,
            "Check List configuration response for "
            "system.vm.use.local.storage")

        self.assertEqual(
            list_conf1[0].value,
            value1,
            "Check the system.vm.use.local.storage value"
        )
        self.network.restart(self.apiclient,
                             cleanup='true'
                             )
        # 12-List network
        list_network1 = Network.list(self.apiclient,
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     id=self.network.id)
        self.assertEqual(validateList(list_network1)[0],
                         PASS,
                         "check list netwok response ")

        # 13-list VR
        list_router1 = Router.list(self.apiclient,
                                   networkid=list_network1[0].id,
                                   accountid=self.account.name,
                                   domainid=self.account.domainid)
        self.assertEqual(
            validateList(list_router1)[0],
            PASS,
            "check list router response"
        )
        # 14-list service offerings
        list_so1 = ServiceOffering.list(self.apiclient,
                                        issystem='true',
                                        id=list_router1[0].serviceofferingid
                                        )
        self.assertEqual(
            validateList(list_so1)[0],
            PASS,
            "check list service offering response"
        )
        if list_conf1[0].value == 'true':
            storage_type1 = 'local'
        elif list_conf1[0].value == 'false':
            storage_type1 = 'shared'
        else:
            raise Exception("check list_conf[0].value")
        self.assertEqual(
            list_so1[0].storagetype,
            storage_type1,
            "Check VR storage type and zone level settings"
        )
