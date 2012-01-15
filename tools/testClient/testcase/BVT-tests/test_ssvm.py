# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for SSVM
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *
import telnetlib

#Import System modules
import time

services = TEST_SSVM_SERVICES

class TestSSVMs(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" %e)
        return

    def test_01_list_sec_storage_vm(self):
        """Test List secondary storage VMs
        """

        # Validate the following:
        # 1. listSystemVM (systemvmtype=secondarystoragevm) should return only ONE SSVM per zone
        # 2. The returned SSVM should be in Running state
        # 3. listSystemVM for secondarystoragevm should list publicip, privateip and link-localip
        # 4. The gateway programmed on the ssvm by listSystemVm should be the same as the gateway returned by listVlanIpRanges
        # 5. DNS entries must match those given for the zone

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.systemvmtype = 'secondarystoragevm'
        list_ssvm_response = self.apiclient.listSystemVms(cmd)

        #Verify SSVM response
        self.assertNotEqual(
                            len(list_ssvm_response),
                            0,
                            "Check list System VMs response"
                        )

        cmd = listZones.listZonesCmd()
        list_zones_response = self.apiclient.listZones(cmd)

        self.assertEqual(
                            len(list_ssvm_response),
                            len(list_zones_response),
                            "Check number of SSVMs with number of zones"
                        )
        #For each secondary storage VM check private IP, public IP, link local IP and DNS
        for i in range(len(list_ssvm_response)):

            self.assertEqual(
                            list_ssvm_response[i].state,
                            'Running',
                            "Check whether state of SSVM is running"
                        )

            self.assertEqual(
                            hasattr(list_ssvm_response[i],'privateip'),
                            True,
                            "Check whether SSVM has private IP field"
                            )

            self.assertEqual(
                            hasattr(list_ssvm_response[i],'linklocalip'),
                            True,
                            "Check whether SSVM has link local IP field"
                            )

            self.assertEqual(
                            hasattr(list_ssvm_response[i],'publicip'),
                            True,
                            "Check whether SSVM has public IP field"
                            )

            #Fetch corresponding ip ranges information from listVlanIpRanges
            cmd = listVlanIpRanges.listVlanIpRangesCmd()
            cmd.id = list_ssvm_response[i].zoneid
            ipranges_response = self.apiclient.listVlanIpRanges(cmd)[0]

            self.assertEqual(
                            list_ssvm_response[i].gateway,
                            ipranges_response.gateway,
                            "Check gateway with that of corresponding ip range"
                            )

            #Fetch corresponding zone information from listZones
            cmd = listZones.listZonesCmd()
            cmd.id = list_ssvm_response[i].zoneid
            zone_response = self.apiclient.listZones(cmd)[0]

            self.assertEqual(
                            list_ssvm_response[i].dns1,
                            zone_response.dns1,
                            "Check DNS1 with that of corresponding zone"
                            )

            self.assertEqual(
                            list_ssvm_response[i].dns2,
                            zone_response.dns2,
                            "Check DNS2 with that of corresponding zone"
                            )
        return

    def test_02_list_cpvm_vm(self):
        """Test List console proxy VMs
        """

        # Validate the following:
        # 1. listSystemVM (systemvmtype=consoleproxy) should return at least ONE CPVM per zone
        # 2. The returned ConsoleProxyVM should be in Running state
        # 3. listSystemVM for console proxy should list publicip, privateip and link-localip
        # 4. The gateway programmed on the console proxy should be the same as the gateway returned by listZones
        # 5. DNS entries must match those given for the zone

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.systemvmtype = 'consoleproxy'
        list_cpvm_response = self.apiclient.listSystemVms(cmd)

        #Verify CPVM response
        self.assertNotEqual(
                            len(list_cpvm_response),
                            0,
                            "Check list System VMs response"
                        )

        cmd = listZones.listZonesCmd()
        list_zones_response = self.apiclient.listZones(cmd)

        self.assertEqual(
                            len(list_cpvm_response),
                            len(list_zones_response),
                            "Check number of CPVMs with number of zones"
                        )
        #For each CPVM check private IP, public IP, link local IP and DNS
        for i in range(len(list_cpvm_response)):

            self.assertEqual(
                            list_cpvm_response[i].state,
                            'Running',
                            "Check whether state of CPVM is running"
                        )

            self.assertEqual(
                            hasattr(list_cpvm_response[i],'privateip'),
                            True,
                            "Check whether CPVM has private IP field"
                            )

            self.assertEqual(
                            hasattr(list_cpvm_response[i],'linklocalip'),
                            True,
                            "Check whether CPVM has link local IP field"
                            )

            self.assertEqual(
                            hasattr(list_cpvm_response[i],'publicip'),
                            True,
                            "Check whether CPVM has public IP field"
                            )
            #Fetch corresponding ip ranges information from listVlanIpRanges
            cmd = listVlanIpRanges.listVlanIpRangesCmd()
            cmd.id = list_ssvm_response[i].zoneid
            ipranges_response = self.apiclient.listVlanIpRanges(cmd)[0]

            self.assertEqual(
                            list_cpvm_response[i].gateway,
                            ipranges_response.gateway,
                            "Check gateway with that of corresponding ip range"
                            )

            #Fetch corresponding zone information from listZones
            cmd = listZones.listZonesCmd()
            cmd.id = list_cpvm_response[i].zoneid
            zone_response = self.apiclient.listZones(cmd)[0]

            self.assertEqual(
                            list_cpvm_response[i].dns1,
                            zone_response.dns1,
                            "Check DNS1 with that of corresponding zone"
                            )

            self.assertEqual(
                            list_cpvm_response[i].dns2,
                            zone_response.dns2,
                            "Check DNS2 with that of corresponding zone"
                            )
        return

    def test_03_ssvm_internals(self):
        """Test SSVM Internals"""

        # Validate the following
        # 1. The SSVM check script should not return any WARN|ERROR|FAIL messages
        # 2. If you are unable to login to the SSVM with the signed key then test is deemed a failure
        # 3. There should be only one ""cloud"" process running within the SSVM
        # 4. If no process is running/multiple process are running then the test is a failure

        cmd = listHosts.listHostsCmd()
        cmd.zoneid = services["ssvm"]["zoneid"]
        cmd.type = 'Routing'
        cmd.state = 'Up'
        host = self.apiclient.listHosts(cmd)[0]

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.systemvmtype = 'secondarystoragevm'
        cmd.hostid = host.id
        ssvm = self.apiclient.listSystemVms(cmd)[0]

        self.debug("Cheking cloud process status")
        #SSH to the machine
        ssh = remoteSSHClient.remoteSSHClient(
                                                host.ipaddress,
                                                services['host']["publicport"],
                                                services['host']["username"],
                                                services['host']["password"]
                                            )

        timeout = 5
        ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -p 3922 %s " % ssvm.linklocalip
        c = ssh_command + "/usr/local/cloud/systemvm/ssvm-check.sh |grep -e ERROR -e WARNING -e FAIL"

        # Ensure the SSH login is successful
        while True:
            res = ssh.execute(c)[0]
            #Output:Tests Complete. Look for ERROR or WARNING above.

            if res != "Host key verification failed.":
                break
            elif timeout == 0:
                break
            time.sleep(5)
            timeout = timeout - 1

        self.assertEqual(
                            res.count("ERROR"),
                            1,
                            "Check for Errors in tests"
                        )

        self.assertEqual(
                            res.count("WARNING"),
                            1,
                            "Check for warnings in tests"
                        )

        self.assertEqual(
                            res.count("FAIL"),
                            1,
                            "Check for failed tests"
                        )

        #Check status of cloud service
        c = ssh_command + "service cloud status"
        res = ssh.execute(c)[0]
        # cloud.com service (type=secstorage) is running: process id: 2346
        self.assertEqual(
                            res.count("is running"),
                            1,
                            "Check cloud service is running or not"
                        )
        return

    def test_04_cpvm_internals(self):
        """Test CPVM Internals"""

        # Validate the following
        # 1. test that telnet access on 8250 is available to the management server for the CPVM
        # 2. No telnet access, test FAIL
        # 3. Service cloud status should report cloud agent status to be running

        cmd = listHosts.listHostsCmd()
        cmd.zoneid = services["cpvm"]["zoneid"]
        cmd.type = 'Routing'
        cmd.state = 'Up'
        host = self.apiclient.listHosts(cmd)[0]

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.systemvmtype = 'consoleproxy'
        cmd.hostid = host.id
        cpvm = self.apiclient.listSystemVms(cmd)[0]

        with assertNotRaises(Exception):
            telnet = telnetlib.Telnet( services["cpvm"]["mgmtserverIP"] , '8250')

        self.debug("Cheking cloud process status")
        #SSH to the machine
        ssh = remoteSSHClient.remoteSSHClient(
                                                host.ipaddress,
                                                services['host']["publicport"],
                                                services['host']["username"],
                                                services['host']["password"]
                                            )
        timeout = 5
        # Check status of cloud service
        ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -p 3922 %s " %  cpvm.linklocalip
        c = ssh_command + "service cloud status"

        while True:
            res = ssh.execute(c)[0]
            # Response: cloud.com service (type=secstorage) is running: process id: 2346

            #Check if double hop is successful
            if res != "Host key verification failed.":
                break
            elif timeout == 0:
                break
            time.sleep(5)
            timeout = timeout - 1

        self.assertEqual(
                            res.count("is running"),
                            1,
                            "Check cloud service is running or not"
                        )
        return

    def test_05_stop_ssvm(self):
        """Test stop SSVM
        """

        # Validate the following
        # 1. The SSVM should go to stop state
        # 2. After a brief delay of say one minute, the SSVM should be restarted once again and return to Running state with previous two test cases still passing
        # 3. If either of the two above steps fail the test is a failure

        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = services["ssvm"]["id"]
        cmd.systemvmtype = 'secondarystoragevm'
        self.apiclient.stopSystemVm(cmd)

        #Sleep to ensure that SSVM is properly restarted
        time.sleep(90)
        cmd = listSystemVms.listSystemVmsCmd()
        cmd.id = services["ssvm"]["id"]
        list_ssvm_response = self.apiclient.listSystemVms(cmd)

        ssvm_response = list_ssvm_response[0]
        self.assertEqual(
                        ssvm_response.state,
                        'Running',
                        "Check whether SSVM is running or not"
                        )
        # Call above tests to ensure SSVM is properly running
        self.test_01_list_sec_storage_vm()
        self.test_03_ssvm_internals()
        return

    def test_06_stop_cpvm(self):
        """Test stop CPVM
        """

        # Validate the following
        # 1. The CPVM should go to stop state
        # 2. After a brief delay of say one minute, the SSVM should be restarted once again and return to Running state with previous two test cases still passing
        # 3. If either of the two above steps fail the test is a failure

        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = services["cpvm"]["id"]
        cmd.systemvmtype = 'consoleproxy'
        self.apiclient.stopSystemVm(cmd)

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.id = services["cpvm"]["id"]

        timeout = 10
        while True :
            list_cpvm_response = self.apiclient.listSystemVms(cmd)
            if not list_cpvm_response:
                break;
            else:
                if timeout == 0:
                    break
            #Sleep to ensure that SSVM is properly restarted
            time.sleep(10)
            timeout = timeout - 1

        cpvm_response = list_cpvm_response[0]
        self.assertEqual(
                        cpvm_response.state,
                        'Running',
                        "Check whether CPVM is running or not"
                        )
        # Call above tests to ensure CPVM is properly running
        self.test_02_list_cpvm_vm()
        self.test_04_cpvm_internals()
        return

    def test_07_reboot_ssvm(self):
        """Test reboot SSVM
        """
        # Validate the following
        # 1. The SSVM should go to stop and return to Running state
        # 2. SSVM's public-ip and private-ip must remain the same before and after reboot
        # 3. The cloud process should still be running within the SSVM

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.id = services["ssvm"]["id"]
        cmd.systemvmtype = 'secondarystoragevm'
        ssvm_response = self.apiclient.listSystemVms(cmd)[0]

        #Store the public & private IP values before reboot
        old_public_ip = ssvm_response.publicip
        old_private_ip = ssvm_response.privateip

        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = services["ssvm"]["id"]
        self.apiclient.rebootSystemVm(cmd)

        #Sleep to ensure that SSVM is properly stopped/started
        time.sleep(60)
        cmd = listSystemVms.listSystemVmsCmd()
        cmd.id = services["ssvm"]["id"]
        ssvm_response = self.apiclient.listSystemVms(cmd)[0]

        self.assertEqual(
                        'Running',
                        str(ssvm_response.state),
                        "Check whether CPVM is running or not"
                        )

        self.assertEqual(
                        ssvm_response.publicip,
                        old_public_ip,
                        "Check Public IP after reboot with that of before reboot"
                        )

        self.assertEqual(
                        ssvm_response.privateip,
                        old_private_ip,
                        "Check Private IP after reboot with that of before reboot"
                        )
        #Call to verify cloud process is running
        self.test_03_ssvm_internals()
        return

    def test_08_reboot_cpvm(self):
        """Test reboot CPVM
        """
        # Validate the following
        # 1. The CPVM should go to stop and return to Running state
        # 2. CPVM's public-ip and private-ip must remain the same before and after reboot
        # 3. the cloud process should still be running within the CPVM

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.id = services["cpvm"]["id"]
        cmd.systemvmtype = 'consoleproxy'
        cpvm_response = self.apiclient.listSystemVms(cmd)[0]

        #Store the public & private IP values before reboot
        old_public_ip = cpvm_response.publicip
        old_private_ip = cpvm_response.privateip

        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = services["cpvm"]["id"]
        self.apiclient.rebootSystemVm(cmd)

        #Sleep to ensure that SSVM is properly stopped/started
        time.sleep(60)

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.id = services["cpvm"]["id"]
        cpvm_response = self.apiclient.listSystemVms(cmd)[0]

        self.assertEqual(
                        'Running',
                        str(cpvm_response.state),
                        "Check whether CPVM is running or not"
                        )

        self.assertEqual(
                        cpvm_response.publicip,
                        old_public_ip,
                        "Check Public IP after reboot with that of before reboot"
                        )

        self.assertEqual(
                        cpvm_response.privateip,
                        old_private_ip,
                        "Check Private IP after reboot with that of before reboot"
                        )
        #Call to verify cloud process is running
        self.test_04_cpvm_internals()
        return

    def test_09_destroy_ssvm(self):
        """Test destroy SSVM
        """

        # Validate the following
        # 1. SSVM should be completely destroyed and a new one will spin up
        # 2. listSystemVMs will show a different name for the systemVM from what it was before
        # 3. new SSVM will have a public/private and link-local-ip
        # 4. cloud process within SSVM must be up and running

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.zoneid = services["ssvm"]["zoneid"]
        cmd.systemvmtype = 'secondarystoragevm'
        ssvm_response = self.apiclient.listSystemVms(cmd)[0]

        old_name = ssvm_response.name

        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = ssvm_response.id
        self.apiclient.destroySystemVm(cmd)

        #Sleep to ensure that new SSVM is created
        time.sleep(60)

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.zoneid = services["ssvm"]["zoneid"]
        cmd.systemvmtype = 'secondarystoragevm'
        ssvm_response = self.apiclient.listSystemVms(cmd)[0]

        # Verify Name, Public IP, Private IP and Link local IP for newly created SSVM
        self.assertNotEqual(
                        ssvm_response.name,
                        old_name,
                        "Check SSVM new name with name of destroyed SSVM"
                        )
        self.assertEqual(
                        hasattr(ssvm_response,'privateip'),
                        True,
                        "Check whether SSVM has private IP field"
                        )

        self.assertEqual(
                        hasattr(ssvm_response,'linklocalip'),
                        True,
                        "Check whether SSVM has link local IP field"
                        )

        self.assertEqual(
                        hasattr(ssvm_response,'publicip'),
                        True,
                        "Check whether SSVM has public IP field"
                        )
        #Call to verify cloud process is running
        self.test_03_ssvm_internals()
        return

    def test_10_destroy_cpvm(self):
        """Test destroy CPVM
        """

        # Validate the following
        # 1. CPVM should be completely destroyed and a new one will spin up
        # 2. listSystemVMs will show a different name for the systemVM from what it was before
        # 3. new CPVM will have a public/private and link-local-ip
        # 4. cloud process within CPVM must be up and running

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.zoneid = services["cpvm"]["zoneid"]
        cmd.systemvmtype = 'consoleproxy'
        cpvm_response = self.apiclient.listSystemVms(cmd)[0]

        old_name = cpvm_response.name

        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = cpvm_response.id
        self.apiclient.destroySystemVm(cmd)

        #Sleep to ensure that new CPVM is created
        time.sleep(60)

        cmd = listSystemVms.listSystemVmsCmd()
        cmd.zoneid = services["cpvm"]["zoneid"]
        cmd.systemvmtype = 'consoleproxy'
        cpvm_response = self.apiclient.listSystemVms(cmd)[0]

        # Verify Name, Public IP, Private IP and Link local IP for newly created CPVM
        self.assertNotEqual(
                        cpvm_response.name,
                        old_name,
                        "Check SSVM new name with name of destroyed CPVM"
                        )
        self.assertEqual(
                        hasattr(cpvm_response,'privateip'),
                        True,
                        "Check whether CPVM has private IP field"
                        )

        self.assertEqual(
                        hasattr(cpvm_response,'linklocalip'),
                        True,
                        "Check whether CPVM has link local IP field"
                        )

        self.assertEqual(
                        hasattr(cpvm_response,'publicip'),
                        True,
                        "Check whether CPVM has public IP field"
                        )
        #Call to verify cloud process is running
        self.test_04_cpvm_internals()
        return