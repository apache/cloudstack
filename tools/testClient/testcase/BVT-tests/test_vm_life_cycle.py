# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#

""" BVT tests for Virtual Machine Life Cycle
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from settings import *
import remoteSSHClient
from utils import *
from base import *
#Import System modules
import time


services = TEST_VM_LIFE_CYCLE_SERVICES

class TestDeployVM(cloudstackTestCase):
  
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def test_deploy_vm(self):
        """Test Deploy Virtual Machine
        """
        
        self.server = Server.create(self.apiclient, services["small"])
            #Validate the following:
        # 1. Server is accessible via SSH
        # 2. listVirtualMachines returns accurate information
        # 3. The Cloud Database contains the valid information

        self.debug("Verify SSH Access for server: %s" %self.server.id)
        try:
            self.server.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.server.ipaddress, e))

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.server.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)
        self.debug("Verify listVirtualMachines response for server: %s" %self.server.id)

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].id,
                            self.server.id,
                            "Check server id in listVirtualMachines"
                        )

        self.assertEqual(
                            list_vm_response[0].displayname,
                            self.server.displayname,
                            "Check server displayname in listVirtualMachines"
                        )

        self.debug("Verify the database entry for server: %s" %self.server.id)
        
        self.debug("select id, state, private_ip_address from vm_instance where id = %s;" %self.server.id)
        qresultset = self.dbclient.execute("select id, state, private_ip_address from vm_instance where id = %s;" %self.server.id)

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]
       
        self.assertEqual(
                            qresult[0],
                            self.server.id,
                            "Compare server id with database record"
                        )
        self.assertEqual(
                            qresult[1],
                            'Running',
                            "Check server state in the database"
                        )
        self.assertEqual(
                            qresult[2],
                            self.server.ipaddress,
                            "Check IP Address in the database"
                        )
        return

    def tearDown(self):
        try:
            self.server.delete(self.apiclient)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" %e)



class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()

        #create small and large servers
        cls.small_server = Server.create(
                                        cls.api_client, 
                                        TEST_VM_LIFE_CYCLE_SERVICES["small"]
                                        )
        cls.medium_server = Server.create(
                                        cls.api_client, 
                                        TEST_VM_LIFE_CYCLE_SERVICES["medium"]
                                        )
    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def test_01_stop_vm(self):
        """Test Stop Virtual Machine
        """
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.api_client.stopVirtualMachine(cmd)

        #Wait before server has be successfully stopped
        self.debug("Verify SSH Access for server: %s,%s" %(self.small_server.id, self.small_server.ipaddress))
        time.sleep(30)
        with self.assertRaises(Exception):
            remoteSSHClient.remoteSSHClient(
                                            self.small_server.ipaddress,
                                            self.small_server.ssh_port,
                                            self.small_server.username,
                                            self.small_server.password
                                            )

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id
        list_vm_response = self.api_client.listVirtualMachines(cmd)
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check server is in stopped state"
                        )

        self.debug("Verify the database entry for server: %s" %self.small_server.id)
        
        self.debug("select state from vm_instance where id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_server.id)

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )


        qresult = qresultset[0]
       
        self.assertEqual(
                            qresult[0],
                            "Stopped",
                            "Compare server state with database record"
                        )
        return

    def test_02_start_vm(self):
        """Test Start Virtual Machine
        """
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.startVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id

        list_vm_response = self.apiclient.listVirtualMachines(cmd)
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.debug("Verify listVirtualMachines response for server: %s" %self.small_server.id)
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check server is in running state"
                        )

        self.debug("Verify SSH Access for server: %s" %self.small_server.id)
        try:
            self.small_server.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.small_server.ipaddress, e))

        self.debug("Verify the database entry for server: %s" %self.small_server.id)
        self.debug("select state from vm_instance where id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_server.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )


        qresult = qresultset[0]
       
        self.assertEqual(
                            qresult[0],
                            'Running',
                            "Compare server state with database record"
                        )
        return

    def test_03_reboot_vm(self):
        """Test Reboot Virtual Machine
        """
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.rebootVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id

        list_vm_response = self.apiclient.listVirtualMachines(cmd)
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )


        self.debug("Verify SSH Access for server: %s" %self.small_server.id)
        try:
           self.small_server.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.small_server.ipaddress, e))


        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check server is in running state"
                        )
        return

    def test_04_change_offering_medium(self):
        """Change Offering to a medium capacity
        """

        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.stopVirtualMachine(cmd)
      
        #Sleep for 60 seconds to ensure the machine has stopped
        time.sleep(60) 
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.small_server.id
        cmd.serviceofferingid = services["service_offerings"]["medium"]["id"]

        self.apiclient.changeServiceForVirtualMachine(cmd)

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.startVirtualMachine(cmd)

        try:
            ssh = self.small_server.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.small_server.ipaddress, e))

       
        cpuinfo = ssh.execute("cat /proc/cpuinfo")
       
        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        #'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i][0].split()[3]

        meminfo = ssh.execute("cat /proc/meminfo")
        #MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]

        self.assertEqual(
                            cpu_cnt,
                            services["service_offerings"]["medium"]["cpunumber"],
                            "Check CPU Count for medium offering"
                        )

        self.assertEqual(
                            cpu_speed,
                            services["service_offerings"]["medium"]["cpuspeed"],
                            "Check CPU Speed for medium offering"
                        )
        self.assertEqual(
                            total_mem,
                            services["service_offerings"]["medium"]["memory"],
                            "Check Memory(kb) for medium offering"
                        )

    def test_05_change_offering_small(self):
        """Change Offering to a small capacity
        """
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.medium_server.id
        self.apiclient.stopVirtualMachine(cmd)

        #Sleep before the changes are reflected
        time.sleep(60) 
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.medium_server.id
        cmd.serviceofferingid = services["service_offerings"]["small"]["id"]

        self.apiclient.changeServiceForVirtualMachine(cmd)

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.medium_server.id
        self.apiclient.startVirtualMachine(cmd)
              
        try:
            ssh = self.medium_server.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.medium_server.ipaddress, e))


        cpuinfo = ssh.execute("cat /proc/cpuinfo")
       
        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        #'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i ][0].split()[3]

        meminfo = ssh.execute("cat /proc/meminfo")
        #MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]

        self.assertEqual(
                            cpu_cnt,
                            services["service_offerings"]["small"]["cpunumber"],
                            "Check CPU Count for small offering"
                        )

        self.assertEqual(
                            cpu_speed,
                            services["service_offerings"]["small"]["cpuspeed"],
                            "Check CPU Speed for small offering"
                        )
        self.assertEqual(
                            total_mem,
                            services["service_offerings"]["small"]["memory"],
                            "Check Memory(kb) for small offering"
                        )


    def test_06_destroy_vm(self):
        """Test destroy Virtual Machine
        """
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.destroyVirtualMachine(cmd)

        self.debug("Verify SSH Access for server: %s" %self.small_server.id)
        with self.assertRaises(Exception):
            remoteSSHClient.remoteSSHClient(
                                            self.small_server.ipaddress,
                                            self.small_server.ssh_port,
                                            self.small_server.username,
                                            self.small_server.password
                                            )

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Destroyed",
                            "Check server is in destroyed state"
                        )

        self.debug("Verify the database entry for server: %s" %self.small_server.id)
        
        self.debug("select state from vm_instance where id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_server.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )


        qresult = qresultset[0]
       
        self.assertEqual(
                            qresult[0],
                            'Destroyed',
                            "Compare server state with database record"
                        )
        return


    def test_07_restore_vm(self):
        """Test recover Virtual Machine
        """
        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.recoverVirtualMachine(cmd)
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id

        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check server is in Stopped state"
                        )

        self.debug("Verify the database entry for server: %s" %self.small_server.id)
        self.debug("select state from vm_instance where id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_server.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )


        qresult = qresultset[0]
        self.assertEqual(
                            qresult[0],
                            'Stopped',
                            "Compare server state with database record"
                        )
        return

    def test_08_expunge_vm(self):
        """Test destroy(expunge) Virtual Machine
        """
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.small_server.id
        self.apiclient.destroyVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_server.id

        timeout = 50
        while True :
            list_vm_response = self.apiclient.listVirtualMachines(cmd)
            if not list_vm_response:
                break;
            else:
                if timeout == 0:
                    break
            time.sleep(100)
            timeout = timeout - 1

        self.assertEqual(
                            list_vm_response,
                            None,
                            "Check Expunged server is listVirtualMachines"
                        )

        self.debug("Verify the database entry for server: %s" %self.small_server.id)
        self.debug("select state from vm_instance where id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_server.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]
        self.assertEqual(
                            qresult[0],
                            "Expunging",
                            "Check server state in VM_INSTANCES table"
                        )

        self.debug("select instance_id from nics where instance_id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select instance_id from nics where instance_id = %s;" %self.small_server.id)
        qresult = qresultset[0]
        self.assertEqual(
                            
                            len(qresult),
                            0,
                            "Check server entry in NICS table"
                        )


        self.debug("select instance_id from volumes where instance_id = %s;" %self.small_server.id)
        qresultset = self.dbclient.execute("select instance_id from volumes where instance_id = %s;" %self.small_server.id)
        qresult = qresultset[0]
        self.assertEqual(
                            
                            len(qresult),
                            0,
                            "Check server entry in VOLUMES table"
                        )
        return

