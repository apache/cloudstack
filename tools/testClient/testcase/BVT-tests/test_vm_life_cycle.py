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

        self.cleanup = []
        self.virtual_machine = VirtualMachine.create(self.apiclient, services["small"])
        self.cleanup.append(self.virtual_machine)
        self.nat_rule = NATRule.create(self.apiclient, self.virtual_machine, services["small"])
        self.cleanup.append(self.nat_rule)

    def test_deploy_vm(self):
        """Test Deploy Virtual Machine
        """

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. listVirtualMachines returns accurate information
        # 3. The Cloud Database contains the valid information
        ipaddress = self.virtual_machine.ipaddress
        self.debug("Verify SSH Access for virtual machine: %s" %self.virtual_machine.id)
        try:
            self.virtual_machine.get_ssh_client(services["small"]["ipaddress"])
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.virtual_machine.ipaddress, e))

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.virtual_machine.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)
        self.debug("Verify listVirtualMachines response for virtual machine: %s" %self.virtual_machine.id)

        vm_response = list_vm_response[0]
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )

        self.assertEqual(
                            vm_response.displayname,
                            self.virtual_machine.displayname,
                            "Check virtual machine displayname in listVirtualMachines"
                        )

        self.debug("Verify the database entry for virtual machine: %s" %self.virtual_machine.id)

        self.debug("select id, state, private_ip_address from vm_instance where id = %s;" %self.virtual_machine.id)
        qresultset = self.dbclient.execute("select id, state, private_ip_address from vm_instance where id = %s;" %self.virtual_machine.id)

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            self.virtual_machine.id,
                            "Compare virtual machine id with database record"
                        )
        self.assertEqual(
                            qresult[1],
                            'Running',
                            "Check virtual machine state in the database"
                        )
        self.assertEqual(
                            qresult[2],
                            ipaddress,
                            "Check IP Address in the database"
                        )
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" %e)


class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()

        #create small and large virtual machines
        cls.small_virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        TEST_VM_LIFE_CYCLE_SERVICES["small"]
                                        )
        cls.medium_virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        TEST_VM_LIFE_CYCLE_SERVICES["medium"]
                                        )
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        TEST_VM_LIFE_CYCLE_SERVICES["small"]
                                        )
        cls.nat_rule = NATRule.create(cls.api_client, cls.virtual_machine, services["small"])

    @classmethod
    def tearDownClass(cls):
        cls.api_client = fetch_api_client()
        cls.nat_rule.delete(cls.api_client)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        self.dbclient.close()
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    def test_01_stop_vm(self):
        """Test Stop Virtual Machine
        """
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.api_client.stopVirtualMachine(cmd)

        #Wait before server has be successfully stopped
        self.debug("Verify SSH Access for virtual machine: %s,%s" %(self.small_virtual_machine.id, self.small_virtual_machine.ipaddress))
        time.sleep(30)
        with self.assertRaises(Exception):
            remoteSSHClient.remoteSSHClient(
                                            self.small_virtual_machine.ipaddress,
                                            self.small_virtual_machine.ssh_port,
                                            self.small_virtual_machine.username,
                                            self.small_virtual_machine.password
                                            )

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id
        list_vm_response = self.api_client.listVirtualMachines(cmd)
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in stopped state"
                        )

        self.debug("Verify the database entry for virtual machine: %s" %self.small_virtual_machine.id)

        self.debug("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)

        self.assertNotEqual(
                                len(qresultset),
                                0,
                                "Check DB Query result set"
                            )


        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            "Stopped",
                            "Compare virtual machine state with database record"
                        )
        return

    def test_02_start_vm(self):
        """Test Start Virtual Machine
        """
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.startVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id

        list_vm_response = self.apiclient.listVirtualMachines(cmd)
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.debug("Verify listVirtualMachines response for virtual machine: %s" %self.small_virtual_machine.id)
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )

        self.debug("Verify SSH Access for virtual machine: %s" %self.small_virtual_machine.id)
        try:
            self.small_virtual_machine.get_ssh_client(services["small"]["ipaddress"])
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.small_virtual_machine.ipaddress, e))

        self.debug("Verify the database entry for virtual machine: %s" %self.small_virtual_machine.id)
        self.debug("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )


        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            'Running',
                            "Compare virtual machine state with database record"
                        )
        return

    def test_03_reboot_vm(self):
        """Test Reboot Virtual Machine
        """
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.rebootVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id

        list_vm_response = self.apiclient.listVirtualMachines(cmd)
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )


        self.debug("Verify SSH Access for virtual machine: %s" %self.small_virtual_machine.id)
        try:
           self.small_virtual_machine.get_ssh_client(services["small"]["ipaddress"])
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.small_virtual_machine.ipaddress, e))


        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return

    def test_04_change_offering_medium(self):
        """Change Offering to a medium capacity
        """

        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.stopVirtualMachine(cmd)

        #Sleep for 60 seconds to ensure the machine has stopped
        time.sleep(60)
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        cmd.serviceofferingid = services["service_offerings"]["medium"]["id"]

        self.apiclient.changeServiceForVirtualMachine(cmd)

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.startVirtualMachine(cmd)

        try:
            ssh = self.small_virtual_machine.get_ssh_client(services["small"]["ipaddress"])
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.small_virtual_machine.ipaddress, e))


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
        cmd.id = self.medium_virtual_machine.id
        self.apiclient.stopVirtualMachine(cmd)

        #Sleep before the changes are reflected
        time.sleep(60)
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.medium_virtual_machine.id
        cmd.serviceofferingid = services["service_offerings"]["small"]["id"]

        self.apiclient.changeServiceForVirtualMachine(cmd)

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.medium_virtual_machine.id
        self.apiclient.startVirtualMachine(cmd)

        try:
            ssh = self.medium_virtual_machine.get_ssh_client(services["medium"]["ipaddress"])
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %(self.medium_virtual_machine.ipaddress, e))


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
        cmd.id = self.small_virtual_machine.id
        self.apiclient.destroyVirtualMachine(cmd)

        self.debug("Verify SSH Access for virtual machine: %s" %self.small_virtual_machine.id)
        with self.assertRaises(Exception):
            remoteSSHClient.remoteSSHClient(
                                            self.small_virtual_machine.ipaddress,
                                            self.small_virtual_machine.ssh_port,
                                            self.small_virtual_machine.username,
                                            self.small_virtual_machine.password
                                            )

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Destroyed",
                            "Check virtual machine is in destroyed state"
                        )

        self.debug("Verify the database entry for virtual machine: %s" %self.small_virtual_machine.id)

        self.debug("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )


        qresult = qresultset[0]

        self.assertEqual(
                            qresult[0],
                            'Destroyed',
                            "Compare virtual machine state with database record"
                        )
        return


    def test_07_restore_vm(self):
        """Test recover Virtual Machine
        """
        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.recoverVirtualMachine(cmd)
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id

        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Stopped",
                            "Check virtual machine is in Stopped state"
                        )

        self.debug("Verify the database entry for virtual machine: %s" %self.small_virtual_machine.id)
        self.debug("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )


        qresult = qresultset[0]
        self.assertEqual(
                            qresult[0],
                            'Stopped',
                            "Compare virtual_machine state with database record"
                        )
        return

    def test_08_migrate_vm(self):
        """Test migrate VM
        """
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.hostid = services["hostid"]
        cmd.virtualmachineid = self.small_virtual_machine.id
        self.apiclient.migrateVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id
        list_vm_response = self.apiclient.listVirtualMachines(cmd)

        self.assertNotEqual(
                            list_vm_response,
                            None,
                            "Check virtual machine is listVirtualMachines"
                        )

        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.id,
                            self.small_virtual_machine.id,
                            "Check virtual machine ID of migrated VM"
                        )

        self.assertEqual(
                            vm_response.hostid,
                            services["hostid"],
                            "Check destination hostID of migrated VM"
                        )
        return

    def test_09_expunge_vm(self):
        """Test destroy(expunge) Virtual Machine
        """
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.destroyVirtualMachine(cmd)

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.id = self.small_virtual_machine.id

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
                            "Check Expunged virtual machine is listVirtualMachines"
                        )

        self.debug("Verify the database entry for virtual machine: %s" %self.small_virtual_machine.id)
        self.debug("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select state from vm_instance where id = %s;" %self.small_virtual_machine.id)
        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        qresult = qresultset[0]
        self.assertEqual(
                            qresult[0],
                            "Expunging",
                            "Check virtual machine state in VM_INSTANCES table"
                        )

        self.debug("select instance_id from nics where instance_id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select instance_id from nics where instance_id = %s;" %self.small_virtual_machine.id)
        qresult = qresultset[0]
        self.assertEqual(

                            len(qresult),
                            0,
                            "Check virtual_machine entry in NICS table"
                        )


        self.debug("select instance_id from volumes where instance_id = %s;" %self.small_virtual_machine.id)
        qresultset = self.dbclient.execute("select instance_id from volumes where instance_id = %s;" %self.small_virtual_machine.id)
        qresult = qresultset[0]
        self.assertEqual(

                            len(qresult),
                            0,
                            "Check virtual machine entry in VOLUMES table"
                        )
        return

    def test_10_attachAndDetach_iso(self):
        """Test for detach ISO to virtual machine"""

        # Validate the following
        # 1. Create ISO
        # 2. Attach ISO to VM
        # 3. Log in to the VM.
        # 4. The device should be available for use
        # 5. Detach ISO
        #6. Check the device is properly detached by logging into VM

        iso = Iso.create(self.apiclient, services["iso"])
        self.cleanup.append(iso)
        iso.download(self.apiclient)

        #Attach ISO to virtual machine
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.attachIso(cmd)

        ssh_client = self.virtual_machine.get_ssh_client(services["small"]["ipaddress"])

        cmds = [    "mkdir -p %s" %services["mount_dir"],
                    "mount -rt iso9660 %s %s" %(services["diskdevice"], services["mount_dir"]),
                ]
        for c in cmds:
            res = ssh_client.execute(c)

        self.assertEqual(res, [], "Check mount is successful or not")

        c = "fdisk -l|grep %s|head -1" % services["diskdevice"]
        res = ssh_client.execute(c)
        #Disk /dev/xvdd: 4393 MB, 4393723904 bytes
        actual_disk_size = res[0].split()[4]

        self.assertEqual(str(iso.size), actual_disk_size, "Check size of the attached ISO")

        #Unmount ISO
        command =  "umount %s" %  services["diskdevice"]
        ssh_client.execute(command)

        #Detach from VM
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.detachIso(cmd)

        res = ssh_client.execute(c)
        result = services["diskdevice"] in res[0].split()

        self.assertEqual(result, False, "Check if ISO is detached from virtual machine")
        return
