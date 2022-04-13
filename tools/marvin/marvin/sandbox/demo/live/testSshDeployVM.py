#!/usr/bin/env python
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



import marvin
from marvin.cloudstackTestCase import *
from marvin.sshClient import SshClient


@UserName('demo', 'ROOT', '0')
class TestSshDeployVm(cloudstackTestCase):
    """
    This test deploys a virtual machine into a user account
    using the small service offering and builtin template
    """
    def setUp(self):
        self.apiClient = self.testClient.getApiClient()

        self.zone = listZones.listZonesCmd()
        self.zone.uuid = self.apiClient.listZones(self.zone)[0].id

        self.service_offering = listServiceOfferings.listServiceOfferingsCmd()
        self.service_offering.uuid = self.apiClient.listServiceOfferings(self.service_offering)[0].id

        self.template = listTemplates.listTemplatesCmd()
        self.template.templatefilter = 'featured'
        self.template.name = 'CentOS'
        self.template.uuid = self.apiClient.listTemplates(self.template)[0].id

    def test_DeployVm(self):
        """
        Let's start by defining the attributes of our VM that we will be
        deploying on CloudStack. We will be assuming a single zone is available
        and is configured and all templates are Ready

        The hardcoded values are used only for brevity.
        """
        deployVmCmd = deployVirtualMachine.deployVirtualMachineCmd()
        deployVmCmd.zoneid = self.zone.uuid
        deployVmCmd.templateid = self.template.uuid #CentOS 5.6 builtin
        deployVmCmd.serviceofferingid = self.service_offering.uuid

        deployVmResponse = self.apiClient.deployVirtualMachine(deployVmCmd)
        self.debug("VM %s was deployed in the job %s"%(deployVmResponse.id, deployVmResponse.jobid))

        # At this point our VM is expected to be Running. Let's find out what
        # listVirtualMachines tells us about VMs in this account

        listVmCmd = listVirtualMachines.listVirtualMachinesCmd()
        listVmCmd.id = deployVmResponse.id
        listVmResponse = self.apiClient.listVirtualMachines(listVmCmd)

        self.assertNotEqual(len(listVmResponse), 0, "Check if the list API \
                            returns a non-empty response")

        vm = listVmResponse[0]
        self.assertEqual(vm.state, "Running", "Check if VM has reached Running state in CS")

        hostname = vm.name
        nattedip = self.setUpNAT(vm.id)

        self.assertEqual(vm.id, deployVmResponse.id, "Check if the VM returned \
                         is the same as the one we deployed")


        self.assertEqual(vm.state, "Running", "Check if VM has reached \
                         a state of running")

        # SSH login and compare hostname
        self.debug("Attempting to SSH into %s over %s of %s"%(nattedip, "22", vm.name))
        ssh_client = SshClient(nattedip, "22", "root", "password")
        stdout = ssh_client.execute("hostname")

        self.assertEqual(hostname, stdout[0], "cloudstack VM name and hostname \
                         do not match")

    def setUpNAT(self, virtualmachineid):
        listSourceNat = listPublicIpAddresses.listPublicIpAddressesCmd()
        listSourceNat.issourcenat = True

        listsnatresponse = self.apiClient.listPublicIpAddresses(listSourceNat)
        self.assertNotEqual(len(listsnatresponse), 0, "Found a source NAT for the account user")

        snatid = listsnatresponse[0].id
        snatip = listsnatresponse[0].ipaddress

        try:
            createFwRule = createFirewallRule.createFirewallRuleCmd()
            createFwRule.cidrlist = "0.0.0.0/0"
            createFwRule.startport = 22
            createFwRule.endport = 22
            createFwRule.ipaddressid = snatid
            createFwRule.protocol = "tcp"
            createfwresponse = self.apiClient.createFirewallRule(createFwRule)

            createPfRule = createPortForwardingRule.createPortForwardingRuleCmd()
            createPfRule.publicport = 22
            createPfRule.privateport = 22
            createPfRule.virtualmachineid = virtualmachineid
            createPfRule.ipaddressid = snatid
            createPfRule.protocol = "tcp"

            createpfresponse = self.apiClient.createPortForwardingRule(createPfRule)
        except e:
            self.debug("Failed to create PF rule in the account due to %s"%e)
            raise e
        finally:
            self.debug("Successfully programmed PF rule for :%s"%snatip)
            return snatip

    def tearDown(self):
        self.testClient.close()
