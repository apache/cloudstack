#!/usr/bin/env python

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.integration.lib.base import *
from marvin.integration.lib.common import get_template, get_zone, list_virtual_machines, cleanup_resources

import random
import string

class Services:
    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256,
            },
        }


class TestDeployVmWithUserData(cloudstackTestCase):
    """Test Deploy VM with UserData > 2k
    """
    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.services = Services().services
        self.service_offering = ServiceOffering.create(
            self.apiClient,
            self.services["service_offering"]
        )
        self.account = Account.create(self.apiClient, services=self.services["account"])
        self.zone = get_zone(self.apiClient, self.services)
        self.template = get_template(
            self.apiClient,
            self.zone.id,
            self.services["ostype"]
        )
        self.debug("Successfully created account: %s, id: \
                   %s" % (self.account.name,\
                          self.account.id))
        self.cleanup = [self.account]

        # Generate userdata of 2500 bytes. This is larger than the 2048 bytes limit.
        # CS however allows for upto 4K bytes in the code. So this must succeed.
        # Overall, the query length must not exceed 4K, for then the json decoder
        # will fail this operation at the marvin client side itself.
        user_data = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2500))
        self.services["virtual_machine"]["userdata"] = user_data

    def test_deployvm_userdata(self):
        # Userdata is passed in the virtual_machine dictionary.
        VirtualMachine.create(self.apiClient, self.services["virtual_machine"], method='POST')
        deployVmResponse = VirtualMachine.create(
            self.apiClient,
            self.virtual_machine,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id
        )
        vms = list_virtual_machines(
            self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assert_(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0].id
        self.assertEqual(vm.id, deployVmResponse.id, "Vm deployed is different from the test")
        self.assertEqual(vm.state, "Running", "VM is not in Running state")

    def tearDown(self):
        try:
            #Cleanup resources used
            cleanup_resources(self.apiClient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
