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
""" Tests for DHCP DNS offload feature
"""
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (random_gen, validateList, cleanup_resources, get_hypervisor_type)
from marvin.lib.base import (Account,
                             NIC,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             Network,
                             NetworkOffering,
                             Configurations,
                             Host,
                             Template,
                             SSHKeyPair,
                             Router)
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain
                               )
from nose.plugins.attrib import attr
from marvin.codes import PASS,FAIL
from random import randint
import random,string,time,tempfile,os,re

@unittest.skip("Skipping dhcp-dns-offload tests for now, since it requires external dhcp server")
class TestDeployVMs(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeployVMs, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.hypervisor = (get_hypervisor_type(cls.api_client)).lower()
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        if cls.hypervisor not in ["xenserver", "kvm"]:
            cls.unsupportedHypervisor = True
            return
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype
        cls.userdata = "Deploy vm with userdata in shared network"
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        try:
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)
            cls.disk_offering = DiskOffering.create(
                cls.api_client,
                cls.services["disk_offering"]
            )
            cls._cleanup.append(cls.disk_offering)
            cls.network_offering1 = NetworkOffering.create(
                cls.api_client,
                cls.services["nw_off_no_services"],
            )
            cls._cleanup.append(cls.network_offering1)
            # Enable Network offering
            cls.network_offering1.update(cls.api_client, state='Enabled')
            cls.network_offering3 = NetworkOffering.create(
                cls.api_client,
                cls.services["network_offering_shared"],
            )
            cls._cleanup.append(cls.network_offering3)
            # Enable Network offering
            cls.network_offering3.update(cls.api_client, state='Enabled')
            #Create shared network without any services
            cls.shared_network = Network.create(
                cls.api_client,
                cls.services["network2"],
                networkofferingid=cls.network_offering1.id,
                zoneid=cls.zone.id
            )
            cls._cleanup.append(cls.shared_network)
            #Verify network services in a created network
            assert TestDeployVMs.list_nw_services(cls.shared_network.id, cls.network_offering1.id),\
                 "Network Services does not match with the offering services"
            #Register passowrd and ssh-key reset enabled template
            template = "ssh-password-template-xen" if cls.hypervisor == "xenserver" else "ssh-password-template-kvm"
            cls.pwd_template = Template.register(cls.api_client,
                                                 cls.services[template],
                                                 zoneid=cls.zone.id)
            cls.pwd_template.download(cls.api_client)
            cls._cleanup.append(cls.pwd_template)
        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls._cleanup = cls._cleanup[::-1]
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.tmp_files = []
        if self.unsupportedHypervisor:
            self.skipTest("Skipping tests because suitable hypervisors not present")

    def tearDown(self):
        try:
            self.cleanup = self.cleanup[::-1]
            cleanup_resources(self.apiclient, self.cleanup)
            for tmp_file in self.tmp_files:
                os.remove(tmp_file)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def getVMIPAddress(self,id):
        """
        This method retrieves the IP address from a deployed vm.
        """
        params = ["externaldhcp.vmip.retrieval.interval","externaldhcp.vmip.max.retry"]
        offload_params = {}
        for param in params:
            configs = Configurations.list(
                self.apiclient,
                name=param,
            )
            self.assertEqual(validateList(configs)[0],PASS,"List config returned invalid response")
            config = configs[0]
            offload_params[param] = config
        max_wait = int(offload_params[params[0]].value)*int(offload_params[params[1]].value)
        wait = 0
        status = 0
        while(wait <= max_wait):
            vm_res = VirtualMachine.list(self.apiclient,id=id)
            self.assertEqual(validateList(vm_res)[0], PASS, "vm list returned invalid response")
            for nic in vm_res[0].nic:
                if nic.type == "Shared":
                    if nic.ipaddress is None:
                        time.sleep(int(offload_params[params[0]].value))
                        wait += int(offload_params[params[0]].value)
                        status = 0
                        break
                    else:
                        status = 1
                else:
                    continue
            if status == 1:
                self.debug("CS has retrieved the vm ip address from the hypervisor")
                break
        return [status, vm_res[0].nic[0].ipaddress]

    def list_nics(self, vm_id):
        list_vm_res = VirtualMachine.list(self.apiclient, id=vm_id)
        self.assertEqual(validateList(list_vm_res)[0], PASS, "List vms returned invalid response")
        nics = list_vm_res[0].nic
        for nic in nics:
            if nic.type == "Shared":
                nic_res = NIC.list(
                    self.apiclient,
                    virtualmachineid=vm_id,
                    nicid=nic.id
                )
                nic_ip = nic_res[0].ipaddress
                self.assertIsNotNone(nic_ip, "listNics API response does not have the ip address")
            else:
                continue
        return

    def isConfigDriveIsoAttached(self, vm_ip):
        """
        This method is to verify whether configdrive iso is attached to vm or not
        Takes vm ip as the input
        Returns mount path if config drive is attached else False
        """
        mountdir = "/root/iso"
        cmd = "blkid -t LABEL='config-2' /dev/sr? /dev/hd? /dev/sd? /dev/xvd? -o device"
        try:
            self.debug("SSH into VM: %s" % vm_ip)
            ssh = self.vm.get_ssh_client(ipaddress=vm_ip, reconnect=True)
            tmp_cmd = ['bash -c "if [ ! -d /root/iso ] ; then mkdir /root/iso ; fi"', "umount /root/iso"]
            for tcmd in tmp_cmd:
                ssh.execute(tcmd)
            configDrive = ssh.execute(cmd)
            res = ssh.execute("mount {} {}".format(str(configDrive[0]), mountdir))
            if str(res).lower().find("mounting read-only") > -1:
                self.debug("configDrive iso has been mounted at location %s" % mountdir)
                return mountdir
            else:
                return False
        except Exception as e:
            self.debug("SSH Access failed for vm {}: Exception {}".format(vm_ip, e))
            return False

    def verifyUserData(self, vm_ip, iso_path):
        """
        Connect to vm and verify Userdata
        """
        userdata_path = iso_path+"/cloudstack/userdata/user_data.txt"
        try:
            self.debug("SSH into VM: %s" % vm_ip)
            ssh = self.vm.get_ssh_client(ipaddress=vm_ip, reconnect=True)
            cmd = "cat %s" % userdata_path
            res = ssh.execute(cmd)
            if str(res).lower().find(self.userdata):
                self.debug("Userdata is found in the attached configDriveIso")
                return True
            else:
                self.debug("Userdata is not found in the attached configDriveIso")
                return False
        except Exception as e:
            self.debug("SSH Access failed for vm {}: Exception {}".format(vm_ip, e))
            return False

        return

    def verifyPassword(self, vm_ip, iso_path):
        password_file = iso_path+"/cloudstack/password/vm_password.txt"
        try:
            self.debug("SSH into VM: %s" % vm_ip)
            ssh = self.vm.get_ssh_client(ipaddress=vm_ip, reconnect=True)
            cmd = "cat %s" % password_file
            res = ssh.execute(cmd)
            if str(res).find("No such file or directory"):
                self.debug("Password file is not found")
                return False
            elif str(res).lower().find("saved_password"):
                self.debug("Reset password is not found in the attached configDriveIso")
                return False
            else:
                self.debug("Reset Password is found in the attached configDriveIso")
                return True
        except Exception as e:
            self.debug("SSH Access failed for vm {}: Exception {}".format(vm_ip, e))
            return False
        return

    def verifySshKey(self, vm_ip, iso_path):

        publicKey_file = iso_path+"/cloudstack/metadata/public-keys.txt"
        try:
            self.debug("SSH into VM: %s" % vm_ip)
            ssh = self.vm.get_ssh_client(ipaddress=vm_ip)
            cmd = "cat %s" % publicKey_file
            res = ssh.execute(cmd)
            if str(res).lower().find("saved_password"):
                self.debug("Public Key is found in the attached configDriveIso")
                return True
            else:
                self.debug("Public Key is not found in the attached configDriveIso")
                return False
        except Exception as e:
            self.debug("SSH Access failed for vm {}: Exception {}".format(vm_ip, e))
            return False
        return

    def getVMMetaData(self, vm_ip, iso_path):

        metadata_dir = iso_path+"/cloudstack/metadata/"
        try:
            response = {}
            self.debug("SSH into VM: %s" % vm_ip)
            ssh = self.vm.get_ssh_client(ipaddress=vm_ip, reconnect=True)
            vm_files = ["availability-zone.txt", "instance-id.txt", "service-offering.txt", "vm-id.txt"]
            for file in vm_files:
                cmd = "cat %s" % metadata_dir+file
                res = ssh.execute(cmd)
                response[file] = res
        except Exception as e:
            self.debug("SSH Access failed for vm {}: Exception {}".format(vm_ip, e))
        return response

    def verifyMetaData(self, metadata):

        metadata_files = ["availability-zone.txt", "instance-id.txt", "service-offering.txt", "vm-id.txt"]
        for mfile in metadata_files:
            if mfile not in metadata:
                self.fail("{} file is not found in vm metadata".format(mfile))
        self.assertEqual(
            str(metadata["availability-zone.txt"][0]),
            self.zone.name,
            "Zone name inside metadata does not match with the zone"
        )
        self.assertEqual(
            str(metadata["instance-id.txt"][0]),
            self.vm.instancename,
            "vm name inside metadata does not match with the instance name"
        )
        self.assertEqual(
            str(metadata["service-offering.txt"][0]),
            self.vm.serviceofferingname,
            "Service offering inside metadata does not match with the instance offering"
        )
        qresultset = self.dbclient.execute(
            "select id from vm_instance where instance_name='%s';" % \
            self.vm.instancename)
        self.assertEqual(validateList(qresultset)[0], PASS, "sql query returned invalid response")
        self.assertEqual(
            metadata["vm-id.txt"][0],
            str(qresultset[0][0]),
            "vm id in metadata does not match with the vm id from cloud db"
        )
        return

    def generate_ssh_keys(self):
        """
        This method generates ssh key pair and writes the private key into a temp file and returns the file name
        """
        self.keypair = SSHKeyPair.create(
            self.apiclient,
            name=random_gen() + ".pem",
            account="admin",
            domainid=1
        )
        self.debug("Created keypair with name: %s" % self.keypair.name)
        self.debug("Writing the private key to local file")
        keyPairFilePath = tempfile.gettempdir() + os.sep + self.keypair.name
        self.tmp_files.append(keyPairFilePath)
        self.debug("File path: %s" % keyPairFilePath)
        f = open(keyPairFilePath, "w+")
        f.write(self.keypair.privatekey)
        f.close()
        os.system("chmod 400 " + keyPairFilePath)
        return keyPairFilePath

    def verifyIPFetchEvent(self, ipaddr):

        id = self.dbclient.execute(
            "select instance_id,id from nics where ip4_address='%s' and removed is null;" % ipaddr
        )
        self.assertEqual(
            validateList(id)[0],
            PASS,
            "sql query to select nic id returned invalid response"
        )
        qresultset = self.dbclient.execute(
            "select description from event where type='EXTERNAL.DHCP.VM.IP.FETCH' order by id desc limit 1;"
        )
        self.assertEqual(
            validateList(qresultset)[0],
            PASS,
            "sql query for IP Fetch event returned invalid response"
        )
        return True if re.search("VM %s nic id %s ip address %s got fetched successfully" % \
                                 (id[0][0], id[0][1], ipaddr), str(qresultset[0])) else False

    def umountConfigDrive(self, vm_ip, iso_path):
        """umount config drive iso attached inside guest vm"""
        try:
            self.debug("SSH into VM: %s" % vm_ip)
            ssh = self.vm.get_ssh_client(ipaddress=vm_ip, reconnect=True)
            ssh.execute("umount %s" % iso_path)
        except Exception as e:
            self.debug("SSH access failed for vm")

    @classmethod
    def list_nw_services(cls, nw_id, nw_off_id):
        """
        Verify network services in a network. List of services supported should be as per the
        network offering with which it has been created.
        return True if created network and network offering has same services else False
        """
        nw_response = Network.list(cls.api_client, id = nw_id)
        assert validateList(nw_response)[0] == PASS, "list networks returned invalid response"
        nw_services = [service.name for service in nw_response[0].service]
        nw_off_res = NetworkOffering.list(cls.api_client, id = nw_off_id)
        assert validateList(nw_off_res)[0] == PASS, "list network offerings returned invalid response"
        nw_off_services = [service.name for service in nw_off_res[0].service]
        if len(nw_services) == len(nw_off_services) and len(nw_services) == 0:
            return True
        elif len(nw_services) != len(nw_off_services):
            return False
        for srv in nw_services:
            if srv not in nw_off_services:
                return False
        return True

    @attr(tags=["advanced"], required_hardware='True')
    def test_01_createNetworks_TC3(self):

        """
        Create shared networks with all the network offerings created in the setupClass method
        """
        nw_offs = [self.network_offering3]
        vlan = self.services["network2"]["vlan"]
        name = self.services["network2"]["name"]
        for nw_off in nw_offs:
            self.services["network2"]["name"]="SharedNetwork%s" % randint(1, 10)
            try:
                self.services["network2"]["vlan"] += 1
                self.shared_network = Network.create(
                    self.apiclient,
                    self.services["network2"],
                    networkofferingid=nw_off.id,
                    zoneid=self.zone.id
                )
                self.cleanup.append(self.shared_network)
                self.assertTrue(
                    TestDeployVMs.list_nw_services(self.shared_network.id, nw_off.id),
                    "Network services not matched with network offering services"
                )
            except Exception as e:
                self.fail("Shared network creation with network offering %s failed with error: %s" % (nw_off, e))
        self.services["network2"]["vlan"] = vlan
        self.services["network2"]["name"] = name
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_02_deployVM_nw_without_services_TC6(self):
        """
        1.Create shared network without any services
        2.Deploy vm in that network
        3.Verify vm deployment
        4.Verify IP address retrieval
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify config drive is attached to vm or not
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_03_deployVM_with_userdata_TC7(self):
        """
        1.Create shared network without any services
        2.Deploy vm with userdata in the above network
        3.Verify vm deployment
        4.Verify IP Address retrieval
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation with userdata failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify config drive is attached to vm or not
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_04_deploy_win_VM_with_userdata_TC8(self):
        """
        1.Create shared network without any services
        2.Deploy windows vm in the above network
        3.Verify IP address retrieval
        """
        self.skipTest("Skip windows tests for now")
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.shared_network = Network.create(
            self.apiclient,
            self.services["network2"],
            networkofferingid=self.network_offering1.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.shared_network)
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.win_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "FAIL:VM Creation failed with windows template in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm_id)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_06_deployVM_with_userdata_TC11(self):
        """
        1.Create shared network with DHCP,DNS and userdata services provided by VR
        2.Deploy vm in that network
        3.Verify vm deployment
        4.Verify IP address retrieval
        """
        vlan = self.services["network2"]["vlan"]
        self.services["network2"]["vlan"] += 1
        self.shared_network = Network.create(
            self.apiclient,
            self.services["network2"],
            networkofferingid=self.network_offering3.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.shared_network)
        self.services["network2"]["vlan"] = vlan
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm with in a shared network with DHCP and userdata services provided by VR"
        )
        self.cleanup.append(self.vm)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #To make sure that vm got the ip address as part of the cs allocation process check user_ip_address table
        #for allocated state for the assigned ip address
        qresultset = self.dbclient.execute(
            "select allocated from user_ip_address where public_ip_address='%s' and removed is null;" % \
            self.vm.nic[0].ipaddress)
        self.assertEqual(validateList(qresultset)[0],PASS,"sql query returned invalid response")
        self.assertNotEqual(
            qresultset[0],
            'NULL',
            "VM didn't get ip address as part of CS IP allocation process\
                        It might have got from external dhcp server in that network")
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_07_deployVM_with_userdata_VR_TC12(self):
        """
        1.Create shared network with only userdata service and VR as the provider
        2.Verify that network offering creation should fail
        """
        try:
            self.network_offering4 = NetworkOffering.create(
                self.api_client,
                self.services["nw_off_userdata_VR"],
            )
            self.fail("NO creation with vr as the userdata provider should not be allowed\
             since VR in not the dhcp provider")
        except Exception as e:
            self.debug("NO creation failed as expected with error message %s" % e)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_09_StopStartVMAndGetVMIpAddress_TC16(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval and userdata
        5.Stop start vm and repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm stop/start
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm stop start"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed in vm deployment"
        )
        #Stop/Start VM and verify the IP address retrieval
        self.vm.stop(self.apiclient)
        self.vm.start(self.apiclient)
        time.sleep(60)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm stop start"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm stop start"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm stop start"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_10_RebootVMAndGetVMIpAddress_TC17(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval and userdata
        5.Reboot vm and repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.cleanup.append(self.vm)
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm reboot
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before reboot"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed while deploying"
        )
        #Reboot VM and verify the IP address retrieval
        self.vm.reboot(self.apiclient)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm reboot"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm reboot"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm reboot"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_11_DestroyVMAndGetVMIpAddress_TC18(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval and userdata
        5.Destroy,restore vm and repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm destroy
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before destroy"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed while deploying"
        )
        #Destroy VM and verify the IP address retrieval
        self.vm.delete(self.apiclient,expunge=False)
        self.vm.recover(self.apiclient)
        self.vm.start(self.apiclient)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm reboot"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm reboot"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm reboot"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_12_ReInstallVMAndGetVMIpAddress_TC19(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval and userdata
        5.Reinstall vm to same template and repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm reinstall
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before reinstall"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr,configDrive_iso),
            True,
            "Userdata is not matching with the data passed while deploying"
        )
        #Reinstall VM and verify the IP address retrieval
        self.vm.restore(self.apiclient)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm reinstall"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm reinstall"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm reinstall"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_13_MigrateVMAndGetVMIpAddress_TC20(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval and userdata
        5.Live Migrate vm to another host in the cluster and repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before live migrate"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed while deploying"
        )
        self.umountConfigDrive(ip_addr, configDrive_iso)
        hosts = Host.listForMigration(self.apiclient,virtualmachineid=self.vm.id)
        self.assertEqual(validateList(hosts)[0], PASS, "Find hosts for migration returned invalid response")
        if len(hosts) < 1:
            self.skipTest("No suitable hosts found for vm migration..so skipping test")
        hostid = hosts[0].id
        self.vm.migrate(self.apiclient,hostid=hostid)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm live migrate"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm live migrate"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm live migrate"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_14_VM_with_password_enabled_template_TC24(self):
        """
        1.Create shared network without any services
        2.Deploy vm with password enabled template
        3.Verify accessing vm with the CS generated password
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.pwd_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services with password enabled template"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        try:
            self.debug("SSHing into VM: %s" % ip_addr)
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True)
        except Exception as e:
            self.fail("SSH into VM: %s failed with new password: %s" %
                      (self.vm.ssh_ip, e))
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_15_reset_password_TC27(self):
        """
        1.Create shared network without any services
        2.Deploy vm with password enabled template
        3.Reset password
        4.Verify accessing vm with the new password
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.pwd_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        self.vm.stop(self.apiclient)
        self.debug("Resetting VM password for VM: %s" % self.vm.name)
        self.vm_password = self.vm.resetPassword(self.apiclient)
        self.debug("Password reset to: %s" % self.vm_password)
        self.debug("Starting VM to verify new password..")
        self.vm.start(self.apiclient)
        self.debug("VM - %s stated!" % self.vm.name)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        try:
            self.debug("SSHing into VM: %s" % ip_addr)
            self.vm.password = self.vm_password
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True)
        except Exception as e:
            self.fail("SSH into VM: %s failed with new password: %s" %
                      (self.vm.ssh_ip, e))
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_16_deployVM_with_sshkeys_TC26(self):
        """
        1.Create shared network without any services
        2.Deploy vm with ssh key reset script enabled template
        3.Verify vm access with new keys
        """
        #generate key pair and get private key file name by calling below function
        ssh_pvt_key = self.generate_ssh_keys()
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.pwd_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
            keypair=self.keypair.name
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm with ssh key pair"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        try:
            self.debug("SSHing into VM: %s" % ip_addr)
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True, keyPairFileLocation=str(ssh_pvt_key))
        except Exception as e:
            self.fail("SSH into VM: %s failed with ssh key pair: %s" %
                      (self.vm.ssh_ip, e))
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_17_reset_sshkeys_TC29(self):
        """
        1.Create shared network without any services
        2.Deploy vm with ssh key reset script enabled template
        3.Reset keys
        4.Verify vm access with new keys
        """
        #generate key pair and get private key file name by calling below function
        self.generate_ssh_keys()
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.pwd_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
            keypair=self.keypair.name
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm with ssh key pair"
        )
        self.cleanup.append(self.vm)
        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine %s with error %s" % (self.vm.id, e))
        self.debug("Creating a new SSH keypair")
        new_pvt_key = self.generate_ssh_keys()
        try:
            self.vm.resetSshKey(
                self.apiclient,
                keypair=self.keypair.name,
                name=self.keypair.name,
                account="admin",
                domainid=1
            )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" % (self.vm.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" % (self.vm.name, e))
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        try:
            self.debug("SSHing into VM: %s" % ip_addr)
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True, keyPairFileLocation=str(new_pvt_key))
        except Exception as e:
            self.fail("SSH into VM: %s failed with ssh key pair with error: %s" %
                      (self.vm.ssh_ip, e))
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_18_reset_password_verify_after_vm_restart_TC36(self):
        """
        1.Create shared network without any services
        2.Deploy vm with password enabled template
        3.Reset password
        4.Verify accessing vm with the new password
        5.Stop/Start vm and repeat step 4
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.pwd_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        self.vm.stop(self.apiclient)
        self.debug("Resetting VM password for VM: %s" % self.vm.name)
        self.vm_password = self.vm.resetPassword(self.apiclient)
        self.debug("Password reset to: %s" % self.vm_password)
        self.debug("Starting VM to verify new password..")
        self.vm.start(self.apiclient)
        self.debug("VM - %s stated!" % self.vm.name)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        try:
            self.debug("SSHing into VM: %s" % ip_addr)
            self.vm.password = self.vm_password
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True)
        except Exception as e:
            self.fail("SSH into VM: %s failed with new password: %s" %
                      (self.vm.ssh_ip, e))
        #Stop start vm and verify connecting to vm with the same password
        self.vm.stop(self.apiclient)
        self.vm.start(self.apiclient)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        try:
            self.debug("SSH into vm after stop start")
            self.vm.password = self.vm_password
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True)
        except Exception as e:
            self.fail("SSH into vm failed with the same password after vm stop start")
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_19_reset_sshkeys_verify_after_vm_restartTC37(self):
        """
        1.Create shared network without any services
        2.Deploy vm with ssh key reset script enabled template
        3.Reset keys
        4.Verify vm access with new keys
        5.Stop/start vm and repeat step4
        """
        #generate key pair and get private key file name by calling below function
        self.generate_ssh_keys()
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.pwd_template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
            keypair=self.keypair.name
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm with ssh key pair"
        )
        self.cleanup.append(self.vm)
        try:
            self.vm.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop virtual machine %s with error %s" % (self.vm.id, e))
        self.debug("Creating a new SSH keypair")
        new_pvt_key = self.generate_ssh_keys()
        try:
            self.vm.resetSshKey(
                self.apiclient,
                keypair=self.keypair.name,
                name=self.keypair.name,
                account="admin",
                domainid=1
            )
        except Exception as e:
            self.fail("Failed to reset SSH key: %s, %s" % (self.vm.name, e))
        self.debug("Starting the virtual machine after resetting the keypair")
        try:
            self.vm.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start virtual machine: %s, %s" % (self.vm.name, e))
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        try:
            self.debug("SSHing into VM: %s" % ip_addr)
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True, keyPairFileLocation=str(new_pvt_key))
        except Exception as e:
            self.fail("SSH into VM: %s failed with ssh key pair with error: %s" %
                      (self.vm.ssh_ip, e))
        self.vm.stop(self.apiclient)
        self.vm.start(self.apiclient)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        try:
            self.debug("SSH into vm after vm stop/start")
            self.vm.get_ssh_client(ipaddress=ip_addr, reconnect=True, keyPairFileLocation=str(new_pvt_key))
        except Exception as e:
            self.fail("SSH into vm failed with new ssh keypair after vm stop start")
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_20_verify_VM_metadata_TC30(self):
        """
        1.Create shared network without any services
        2.Deploy vm with userdata in the above network
        3.Verify vm deployment
        4.Verify IP Address retrieval
        5.Verify metadata inside configdrive iso
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm with in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify config drive is attached to vm or not
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_21_StopStartVM_verify_ConfigDrive_TC31(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval and userdata
        5.Stop start vm
        6.Verify userdata and metadata
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm stop/start
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before vm stop start"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr,configDrive_iso),
            True,
            "Userdata is not matching with the data passed in vm deployment"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        #Stop/Start VM and verify IP address retrieval,userdata and metadata
        self.vm.stop(self.apiclient)
        self.vm.start(self.apiclient)
        time.sleep(60)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm stop start"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm stop start"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm stop start"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_22_RebootVM_verify_ConfigDrive_TC32(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval,userdata and metadata
        5.Reboot vm
        6.Repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm stop/start
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before vm reboot"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed in vm deployment"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        #Reboot VM and verify IP address retrieval,userdata and metadata
        self.vm.reboot(self.apiclient)
        time.sleep(60)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm reboot"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm reboot"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm reboot"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_23_DestroyVM_verify_ConfigDrive_TC33(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval,userdata and metadata
        5.Destroy and restore vm
        6.Repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm stop/start
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before vm destroy"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed in vm deployment"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        #Destroy VM and verify IP address retrieval,userdata and metadata
        self.vm.delete(self.apiclient, expunge=False)
        self.vm.recover(self.apiclient)
        self.vm.start(self.apiclient)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm destroy"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm destroy"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching after vm destroy"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_24_ReInstallVM_verify_ConfigDrive_TC34(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval,userdata and metadata
        5.Reinstall vm to the same template
        6.Repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm stop/start
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before vm reinstall"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed in vm reinstall"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        #Reinstall VM and verify the IP address retrieval
        self.vm.restore(self.apiclient)
        time.sleep(60)
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm reinstall"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm reinstall"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr,configDrive_iso),
            True,
            "Userdata is not matching after vm reinstall"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_25_MigrateVMVM_verify_ConfigDrive_TC35(self):
        """
        1.Create shared network without any services
        2.Deploy vm with default cent os template and pass user data
        3.Verify vm deployment
        4.Verify IP address retrieval,userdata and metadata
        5.Migrate VM and Repeat step4
        """
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "Failed to create vm in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        self.list_nics(self.vm.id)
        #Verify configdrive iso attach before vm stop/start
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm before vm reinstall"
        )
        #Add userdata verification test
        self.assertEqual(
            self.verifyUserData(ip_addr, configDrive_iso),
            True,
            "Userdata is not matching with the data passed in vm reinstall"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        #Migrate VM and verify ip address retrieval,userdata and metadata
        hosts = Host.listForMigration(self.apiclient,virtualmachineid=self.vm.id)
        self.assertEqual(validateList(hosts)[0], PASS, "Find hosts for migration returned invalid response")
        if len(hosts) < 1:
            self.skipTest("No suitable hosts found for vm migration..so skipping test")
        hostid = hosts[0].id
        self.umountConfigDrive(ip_addr, configDrive_iso)
        self.vm.migrate(self.apiclient, hostid=hostid)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm reinstall"
        )
        self.list_nics(self.vm.id)
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm after vm reinstall"
        )
        self.assertEqual(
            self.verifyUserData(ip_addr,configDrive_iso),
            True,
            "Userdata is not matching after vm reinstall"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_26_DeployVM_verify_IPFetchEvent_TC56(self):
        """
        1.Create shared network without any services
        2.Deploy vm in that network
        3.Verify vm deployment
        4.Verify IP address retrieval
        5.Verify EXTERNAL.DHCP.VM.IP.FETCH event after IP address retrieval
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify config drive is attached to vm or not
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm"
        )
        #Verify EXTERNAL.DHCP.VM.IP.FETCH event from DB
        self.assertEqual(
            self.verifyIPFetchEvent(ip_addr),
            True,
            "EXTERNAL.DHCP.VM.IP.FETCH is not generated"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_27_StopStartVM_verify_IPFetchEvent_TC57(self):
        """
        1.Create shared network without any services
        2.Deploy vm in that network
        3.Verify vm deployment
        4.Verify IP address retrieval
        5.Verify EXTERNAL.DHCP.VM.IP.FETCH event after IP address retrieval
        6.Stop/Start vm and repeat step5
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify EXTERNAL.DHCP.VM.IP.FETCH event from DB
        self.assertEqual(
            self.verifyIPFetchEvent(ip_addr),
            True,
            "EXTERNAL.DHCP.VM.IP.FETCH is not generated"
        )
        #Stop/Start VM and verify IP address retrieval,event notification
        self.vm.stop(self.apiclient)
        self.vm.start(self.apiclient)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm stop start"
        )
        #Verify EXTERNAL.DHCP.VM.IP.FETCH event from DB
        self.assertEqual(
            self.verifyIPFetchEvent(ip_addr),
            True,
            "EXTERNAL.DHCP.VM.IP.FETCH is not generated"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_28_RebootVM_verify_IPFetchEvent_TC58(self):
        """
        1.Create shared network without any services
        2.Deploy vm in that network
        3.Verify vm deployment
        4.Verify IP address retrieval
        5.Verify EXTERNAL.DHCP.VM.IP.FETCH event after IP address retrieval
        6.Reboot vm and repeat step5
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify EXTERNAL.DHCP.VM.IP.FETCH event from DB
        self.assertEqual(
            self.verifyIPFetchEvent(ip_addr),
            True,
            "EXTERNAL.DHCP.VM.IP.FETCH is not generated"
        )
        #Reboot VM and verify IP address retrieval,event notification
        self.vm.reboot(self.apiclient)
        time.sleep(60)
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address after vm stop start"
        )
        #Verify EXTERNAL.DHCP.VM.IP.FETCH event from DB
        self.assertEqual(
            self.verifyIPFetchEvent(ip_addr),
            True,
            "EXTERNAL.DHCP.VM.IP.FETCH is not generated"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_29_deployVM_In_Two_Networks_TC49(self):
        """
        1.Create shared network without any services
        2.Create isolated network with VR as the service provider
        2.Deploy vm in both the networks
        3.Verify vm deployment
        4.Verify IP address retrieval
        5.Verify metadata
        """
        self.no_isolate = NetworkOffering.create(
            self.apiclient,
            self.services["isolated_network_offering"]
        )
        self.no_isolate.update(self.apiclient, state='Enabled')
        self.isolated_network = Network.create(
            self.apiclient,
            self.services["network"],
            networkofferingid=self.no_isolate.id,
            zoneid=self.zone.id,
            accountid="admin",
            domainid=1
        )
        self.cleanup.append(self.isolated_network)
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id, self.isolated_network.id],
            )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify config drive is attached to vm or not
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_30_deployVM_In_Two_Networks_TC50(self):
        """
        1.Create shared network without any services
        2.Create another shared network with VR as the service provider
        2.Deploy vm in both the networks
        3.Verify vm deployment
        4.Verify IP address retrieval
        5.Verify metadata
        """
        self.services["shared_network_offering_all_services"]["specifyVlan"]="True"
        self.services["shared_network_offering_all_services"]["specifyIpRanges"]="True"
        self.no_shared_vr = NetworkOffering.create(
            self.apiclient,
            self.services["shared_network_offering_all_services"]
        )
        self.no_shared_vr.update(self.apiclient, state='Enabled')
        self.services["shared_network"]["vlan"] = self.services["vlan"]
        self.services["shared_network"]["gateway"] = self.services["gateway"]
        self.services["shared_network"]["netmask"] = self.services["netmask"]
        self.services["shared_network"]["startip"] = self.services["startip"]
        self.services["shared_network"]["endip"] = self.services["endip"]
        self.shared_network2 = Network.create(
            self.apiclient,
            self.services["shared_network"],
            networkofferingid=self.no_shared_vr.id,
            zoneid=self.zone.id,
            accountid="admin",
            domainid=1
        )
        self.cleanup.append(self.shared_network2)
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id, self.shared_network2.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status,ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        #Verify config drive is attached to vm or not
        configDrive_iso = self.isConfigDriveIsoAttached(ip_addr)
        self.assertNotEqual(
            configDrive_iso,
            False,
            "Config drive iso is not attached to vm"
        )
        metadata = self.getVMMetaData(ip_addr, configDrive_iso)
        self.verifyMetaData(metadata)
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_31_deployVM_with_userdataByVR_TC38(self):
        """
        1.Create shared network with userdata service and VR as the provider
        2.Deploy vm with userdata in the above network
        3.Verify vm deployment
        4.Verify IP Address retrieval
        """
        self.skipTest("this combination is not supported")
        self.services["virtual_machine"]["userdata"] = self.userdata
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation with userdata failed in a shared network with VR as the provider"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        vr_res = Router.list(
            self.apiclient,
            networkid=self.shared_network.id,
            listAll=True
        )
        self.assertEqual(validateList(vr_res)[0],PASS,"List Routers returned invalid response")
        vr_ip = vr_res[0].guestipaddress
        ssh = self.vm.get_ssh_client(ipaddress=ip_addr)
        cmd = "curl http://%s/latest/user-data" % vr_ip
        res = ssh.execute(cmd)
        self.assertEqual(
            str(res),
            self.userdata,
            "Failed to get the userdata from VR in shared network"
        )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_32_restart_network_TC6(self):
        """
        1.Create shared network without any services
        2.Deploy vm in that network
        3.Verify vm deployment
        4.Verify IP address retrieval
        5.Restart network and verify access to VM
        """
        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.shared_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in a shared network without any services"
        )
        self.cleanup.append(self.vm)
        #Verify IP address retrieval using listVirtualMachines API
        ip_status, ip_addr = self.getVMIPAddress(self.vm.id)
        self.assertEqual(
            ip_status,
            1,
            "Failed to retrieve vm ip address"
        )
        #Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        res = self.shared_network.restart(self.apiclient, cleanup=True)
        if res.success:
            self.assertEqual(
                res.success,
                True,
                "Failed to restart shared network without any services"
            )
        else:
            self.fail("Failed restarting shared network without any services")
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_33_verify_config_params_TC65(self):
        """
        #@desc: Validate external dhcp config parameters against invalid values
        #1.Generate random values using special characters, strings and numbers
        #2.Try to update the config params with those values.
        #3.Verify that invalid values are not allowed
        """
        params = ["externaldhcp.vmip.retrieval.interval","externaldhcp.vmip.max.retry",\
                  "externaldhcp.vmipFetch.threadPool.max"]
        for param in params:
            random_num = ''.join(random.choice(string.digits + string.punctuation + string.ascii_letters) \
                                 for _ in range(3))
            try:
                Configurations.update(
                    self.apiclient,
                    name=param,
                    value=random_num
                )
                self.fail("FAIL:{} takes invalid value".format(param))
            except Exception as e:
                self.debug("Success: Updating dhcp config params with invalid values is not allowed\
                           and throws following message: %s" % e)
        return

