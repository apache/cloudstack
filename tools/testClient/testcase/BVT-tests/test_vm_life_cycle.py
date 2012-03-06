# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

""" BVT tests for Virtual Machine Life Cycle
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
import remoteSSHClient
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *
#Import System modules
import time

class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {
                "disk_offering":{
                    "displaytext": "Small",
                    "name": "Small",
                    "disksize": 1
                },
                "account": {
                    "email": "test@test.com",
                    "firstname": "Test",
                    "lastname": "User",
                    "username": "test",
                    # Random characters are appended in create account to 
                    # ensure unique username generated each time
                    "password": "fr3sca",
                },
                "small":
                # Create a small virtual machine instance with disk offering 
                {
                    "displayname": "testserver",
                    "username": "root", # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "domainid": '9ee36d2e-8b8f-432e-a927-a678ebec1d6b',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "medium":   # Create a medium virtual machine instance 
                {
                    "displayname": "testserver",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "domainid": '9ee36d2e-8b8f-432e-a927-a678ebec1d6b',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "service_offerings":
                {
                 "tiny":
                   {
                        "name": "Tiny Instance",
                        "displaytext": "Tiny Instance",
                        "cpunumber": 1,
                        "cpuspeed": 100, # in MHz
                        "memory": 64, # In MBs
                    },
                 "small":
                    {
                     # Small service offering ID to for change VM 
                     # service offering from medium to small
                        "name": "Small Instance",
                        "displaytext": "Small Instance",
                        "cpunumber": 1,
                        "cpuspeed": 500,
                        "memory": 256
                    },
                "medium":
                    {
                    # Medium service offering ID to for
                    # change VM service offering from small to medium
                        "name": "Medium Instance",
                        "displaytext": "Medium Instance",
                        "cpunumber": 1,
                        "cpuspeed": 1000,
                        "memory": 1024
                    }
                },
                "iso":  # ISO settings for Attach/Detach ISO tests
                {
                    "displaytext": "Test ISO",
                    "name": "testISO",
                    "url": "http://iso.linuxquestions.org/download/504/1819/http/gd4.tuwien.ac.at/dsl-4.4.10.iso",
                     # Source URL where ISO is located
                    "ostypeid": '0c2c5d19-525b-41be-a8c3-c6607412f82b',
                    "mode": 'HTTP_DOWNLOAD', # Downloading existing ISO 
                },
            "diskdevice": '/dev/xvdd',
            # Disk device where ISO is attached to instance
            "mount_dir": "/mnt/tmp",
            "sleep": 60,
            "timeout": 10,
            "hostid": 5,
            #Migrate VM to hostid
            "ostypeid": '0c2c5d19-525b-41be-a8c3-c6607412f82b',
            # CentOS 5.3 (64-bit)
            "zoneid": '4a6c0290-e64d-40fc-afbb-4a05cab6fa4b',
            # Optional, if specified the mentioned zone will be
            # used for tests
            "mode":'advanced',
            # Networking mode: Basic or Advanced
        }


class TestDeployVM(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = Services().services
        # Get Zone, Domain and templates
        zone = get_zone(self.apiclient, self.services)

        template = get_template(
                            self.apiclient,
                            zone.id,
                            self.services["ostypeid"]
                            )
        # Set Zones and disk offerings
        self.services["small"]["zoneid"] = zone.id
        self.services["small"]["template"] = template.id

        self.services["medium"]["zoneid"] = zone.id
        self.services["medium"]["template"] = template.id
        self.services["iso"]["zoneid"] = zone.id

        # Create Account, VMs, NAT Rules etc
        self.account = Account.create(
                            self.apiclient,
                            self.services["account"]
                            )

        self.service_offering = ServiceOffering.create(
                                    self.apiclient,
                                    self.services["service_offerings"]["tiny"]
                                    )
        # Cleanup
        self.cleanup = [
                        self.service_offering,
                        self.account
                        ]

    def test_deploy_vm(self):
        """Test Deploy Virtual Machine
        """

        # Validate the following:
        # 1. Virtual Machine is accessible via SSH
        # 2. listVirtualMachines returns accurate information
        # 3. The Cloud Database contains the valid information

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["small"],
                                    accountid=self.account.account.name,
                                    serviceofferingid=self.service_offering.id
                                )

        list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.virtual_machine.id
                                                 )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
        
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )
        vm_response = list_vm_response[0]
        
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
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


class TestVMLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = fetch_api_client()
        cls.services = Services().services

        # Get Zone, Domain and templates
        zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            zone.id,
                            cls.services["ostypeid"]
                            )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = zone.id
        cls.services["small"]["template"] = template.id

        cls.services["medium"]["zoneid"] = zone.id
        cls.services["medium"]["template"] = template.id
        cls.services["iso"]["zoneid"] = zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"]
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["small"]
                                    )

        cls.medium_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["medium"]
                                    )
        #create small and large virtual machines
        cls.small_virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.account.name,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls.medium_virtual_machine = VirtualMachine.create(
                                       cls.api_client,
                                       cls.services["medium"],
                                       accountid=cls.account.account.name,
                                       serviceofferingid=cls.medium_offering.id,
                                       mode=cls.services["mode"]
                                    )
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.account.name,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        cls._cleanup = [
                        cls.small_offering,
                        cls.medium_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = fetch_api_client()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    def test_01_stop_vm(self):
        """Test Stop Virtual Machine
        """

        # Validate the following
        # 1. Should Not be able to login to the VM.
        # 2. listVM command should return
        #    this VM.State of this VM should be ""Stopped"".

        self.debug("Stopping VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.stop(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
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
        return

    def test_02_start_vm(self):
        """Test Start Virtual Machine
        """
        # Validate the following
        # 1. listVM command should return this VM.State
        #    of this VM should be Running".
        
        self.debug("Starting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.start(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.small_virtual_machine.id
                )
        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return

    def test_03_reboot_vm(self):
        """Test Reboot Virtual Machine
        """

        # Validate the following
        # 1. Should be able to login to the VM.
        # 2. listVM command should return the deployed VM.
        #    State of this VM should be "Running"

        self.debug("Rebooting VM - ID: %s" % self.virtual_machine.id)
        self.small_virtual_machine.reboot(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM avaliable in List Virtual Machines"
                        )

        self.assertEqual(
                            list_vm_response[0].state,
                            "Running",
                            "Check virtual machine is in running state"
                        )
        return

    def test_04_change_offering_small(self):
        """Change Offering to a small capacity
        """

        # Validate the following
        # 1. Log in to the Vm .We should see that the CPU and memory Info of
        #    this Vm matches the one specified for "Small" service offering. 
        # 2. Using  listVM command verify that this Vm 
        #    has Small service offering Id.
        
        self.debug("Stopping VM - ID: %s" % self.medium_virtual_machine.id)
        
        self.medium_virtual_machine.stop(self.apiclient)
        
        # Poll listVM to ensure VM is stopped properly
        timeout = self.services["timeout"]
        
        while True:
            time.sleep(self.services["sleep"])
        
            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.medium_virtual_machine.id
                                            )
            
            if isinstance(list_vm_response, list):
                
                vm = list_vm_response[0]
                if vm.state == 'Stopped':
                    self.debug("VM state: %s" % vm.state)
                    break
            
            if timeout == 0: 
                    raise Exception(
                        "Failed to stop VM (ID: %s) in change service offering" % vm.id)
                    
            timeout = timeout - 1
        
        self.debug("Change Service offering VM - ID: %s" % 
                                    self.medium_virtual_machine.id)
        
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.medium_virtual_machine.id
        cmd.serviceofferingid = self.small_offering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % self.medium_virtual_machine.id)
        self.medium_virtual_machine.start(self.apiclient)
        
        # Poll listVM to ensure VM is started properly
        timeout = self.services["timeout"]
        
        while True:
            time.sleep(self.services["sleep"])
        
            # Ensure that VM is in running state
            list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.medium_virtual_machine.id
                                            )
            
            if isinstance(list_vm_response, list):
                
                vm = list_vm_response[0]
                if vm.state == 'Running':
                    self.debug("VM state: %s" % vm.state)
                    break
            
            if timeout == 0: 
                    raise Exception(
                        "Failed to start VM (ID: %s) after changing service offering" % vm.id)
                    
            timeout = timeout - 1
        
        try:
            ssh = self.medium_virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail(
                      "SSH Access failed for %s: %s" % \
                      (self.medium_virtual_machine.ipaddress, e)
                      )

        cpuinfo = ssh.execute("cat /proc/cpuinfo")

        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        #'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i ][0].split()[3]

        meminfo = ssh.execute("cat /proc/meminfo")
        #MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]
        
        self.debug(
            "CPU count: %s, CPU Speed: %s, Mem Info: %s" % (
                                                            cpu_cnt,
                                                            cpu_speed,
                                                            total_mem
                                                            ))
        self.assertAlmostEqual(
                            int(cpu_cnt),
                            self.small_offering.cpunumber,
                            "Check CPU Count for small offering"
                        )

        self.assertAlmostEqual(
                            list_vm_response[0].cpuspeed,
                            self.small_offering.cpuspeed,
                            "Check CPU Speed for small offering"
                        )
        self.assertAlmostEqual(
                            int(total_mem) / 1024, # In MBs
                            self.small_offering.memory,
                            "Check Memory(kb) for small offering"
                        )
        return

    def test_05_change_offering_medium(self):
        """Change Offering to a medium capacity
        """
        # Validate the following
        # 1. Log in to the Vm .We should see that the CPU and memory Info of
        #    this Vm matches the one specified for "Medium" service offering. 
        # 2. Using  listVM command verify that this Vm 
        #    has Medium service offering Id.
        
        self.debug("Stopping VM - ID: %s" % self.small_virtual_machine.id)
        self.small_virtual_machine.stop(self.apiclient)
        
        # Poll listVM to ensure VM is stopped properly
        timeout = self.services["timeout"]
        
        while True:
            time.sleep(self.services["sleep"])
        
            # Ensure that VM is in stopped state
            list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
            
            if isinstance(list_vm_response, list):
                
                vm = list_vm_response[0]
                if vm.state == 'Stopped':
                    self.debug("VM state: %s" % vm.state)
                    break
            
            if timeout == 0: 
                    raise Exception(
                        "Failed to stop VM (ID: %s) in change service offering" % vm.id)
                    
            timeout = timeout - 1
        
        self.debug("Change service offering VM - ID: %s" % 
                                            self.small_virtual_machine.id)
        
        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        cmd.serviceofferingid = self.medium_offering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)
        
        self.debug("Starting VM - ID: %s" % self.small_virtual_machine.id)
        self.small_virtual_machine.start(self.apiclient)
        
        # Poll listVM to ensure VM is started properly
        timeout = self.services["timeout"]
        
        while True:
            time.sleep(self.services["sleep"])
        
            # Ensure that VM is in running state
            list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
            
            if isinstance(list_vm_response, list):
                
                vm = list_vm_response[0]
                if vm.state == 'Running':
                    self.debug("VM state: %s" % vm.state)
                    break
            
            if timeout == 0: 
                    raise Exception(
                        "Failed to start VM (ID: %s) after changing service offering" % vm.id)
                    
            timeout = timeout - 1
        
        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        
        try:
            ssh_client = self.small_virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail(
                    "SSH Access failed for %s: %s" % \
                    (self.small_virtual_machine.ipaddress, e)
                    )
        
        cpuinfo = ssh_client.execute("cat /proc/cpuinfo")

        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        #'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i][0].split()[3]

        meminfo = ssh_client.execute("cat /proc/meminfo")
        #MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]
        
        self.debug(
            "CPU count: %s, CPU Speed: %s, Mem Info: %s" % (
                                                            cpu_cnt,
                                                            cpu_speed,
                                                            total_mem
                                                            ))
        self.assertAlmostEqual(
                            int(cpu_cnt),
                            self.medium_offering.cpunumber,
                            "Check CPU Count for medium offering"
                        )

        self.assertAlmostEqual(
                            list_vm_response[0].cpuspeed,
                            self.medium_offering.cpuspeed,
                            "Check CPU Speed for medium offering"
                        )
    
        self.assertAlmostEqual(
                            int(total_mem) / 1024, # In MBs
                            self.medium_offering.memory,
                            "Check Memory(kb) for medium offering"
                        )
        return

    def test_06_destroy_vm(self):
        """Test destroy Virtual Machine
        """

        # Validate the following
        # 1. Should not be able to login to the VM.
        # 2. listVM command should return this VM.State
        #    of this VM should be "Destroyed".
        
        self.debug("Destroy VM - ID: %s" % self.small_virtual_machine.id)
        self.small_virtual_machine.delete(self.apiclient)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
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
        return


    def test_07_restore_vm(self):
        """Test recover Virtual Machine
        """

        # Validate the following
        # 1. listVM command should return this VM.
        #    State of this VM should be "Stopped".
        # 2. We should be able to Start this VM successfully.
        
        self.debug("Recovering VM - ID: %s" % self.small_virtual_machine.id)
        
        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.recoverVirtualMachine(cmd)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
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

        return

    def test_08_migrate_vm(self):
        """Test migrate VM
        """
        # Validate the following
        # 1. Should be able to login to the VM.
        # 2. listVM command should return this VM.State of this VM
        #    should be "Running" and the host should be the host 
        #    to which the VM was migrated to
        
        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                        self.medium_virtual_machine.id,
                                        self.services["hostid"]
                                        ))
        
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.hostid = self.services["hostid"]
        cmd.virtualmachineid = self.medium_virtual_machine.id
        self.apiclient.migrateVirtualMachine(cmd)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.medium_virtual_machine.id
                                            )
        self.assertEqual(
                            isinstance(list_vm_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        
        self.assertNotEqual(
                            list_vm_response,
                            None,
                            "Check virtual machine is listVirtualMachines"
                        )

        vm_response = list_vm_response[0]

        self.assertEqual(
                            vm_response.id,
                            self.medium_virtual_machine.id,
                            "Check virtual machine ID of migrated VM"
                        )

        self.assertEqual(
                            vm_response.hostid,
                            self.services["hostid"],
                            "Check destination hostID of migrated VM"
                        )
        return

    def test_09_expunge_vm(self):
        """Test destroy(expunge) Virtual Machine
        """
        # Validate the following
        # 1. listVM command should NOT  return this VM any more.
        
        self.debug("Expunge VM-ID: %s" % self.small_virtual_machine.id)
        
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.small_virtual_machine.id
        self.apiclient.destroyVirtualMachine(cmd)

        config = list_configurations(
                                     self.apiclient,
                                     name='expunge.delay'
                                     )

        response = config[0]
        # Wait for some time more than expunge.delay
        time.sleep(int(response.value) * 2)

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.small_virtual_machine.id
                                            )
        self.assertEqual(
                        list_vm_response,
                        None,
                        "Check Expunged virtual machine is listVirtualMachines"
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
        # 6. Check the device is properly detached by logging into VM

        iso = Iso.create(
                         self.apiclient,
                         self.services["iso"],
                         account=self.account.account.name,
                         domainid=self.account.account.domainid
                         )
        
        self.debug("Successfully created ISO with ID: %s" % iso.id)
        try:
            iso.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s"\
                      % (iso.id, e))
        
        self.debug("Attach ISO with ID: %s to VM ID: %s" % (
                                                    iso.id,
                                                    self.virtual_machine.id
                                                    ))
        #Attach ISO to virtual machine
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.attachIso(cmd)
        
        try:
            ssh_client = self.virtual_machine.get_ssh_client()
            
            cmds = [
                        "mkdir -p %s" % self.services["mount_dir"],
                        "mount -rt iso9660 %s %s" \
                                    % (
                                       self.services["diskdevice"],
                                       self.services["mount_dir"]
                                       ),
                ]
        
            for c in cmds:
                res = ssh_client.execute(c)

                self.assertEqual(res, [], "Check mount is successful or not")

                c = "fdisk -l|grep %s|head -1" % self.services["diskdevice"]
                res = ssh_client.execute(c)
                #Disk /dev/xvdd: 4393 MB, 4393723904 bytes

        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % 
                                (self.virtual_machine.ipaddress, e))
            
        # Res may contain more than one strings depending on environment
        # Split strings to form new list which is used for assertion on ISO size 
        result = []
        for i in res:
            for k in i.split():
                result.append(k)

        # Get ISO size
        iso_response = list_isos(
                                 self.apiclient,
                                 id=iso.id
                                 )
        self.assertEqual(
                            isinstance(iso_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        iso_size = iso_response[0].size

        self.assertEqual(
                         str(iso_size) in result,
                         True,
                         "Check size of the attached ISO"
                         )
        try:
            #Unmount ISO
            command = "umount %s" % self.services["mount_dir"]
            ssh_client.execute(command)
        
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % 
                                (self.virtual_machine.ipaddress, e))
            
        #Detach from VM
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.detachIso(cmd)
        
        try:
            res = ssh_client.execute(c)
        
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % 
                                (self.virtual_machine.ipaddress, e))
            
        # Check if ISO is properly detached from VM (using fdisk)
        result = self.services["diskdevice"] in str(res)

        self.assertEqual(
                         result,
                         False,
                         "Check if ISO is detached from virtual machine"
                         )
        return
