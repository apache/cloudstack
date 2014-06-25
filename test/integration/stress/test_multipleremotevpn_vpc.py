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

""" Component tests Multiple Connections for Remote Access VPN on VPC Functionality.
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (Vpn,
                             VpnUser,
                             VPC,
                             Account,
                             User,
                             VpcOffering,
                             VPC,
                             ServiceOffering,
                             NATRule,
                             NetworkACL,
                             PublicIPAddress,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             StaticNATRule,
                             Template,
                             Configurations
                             )
from marvin.lib.common import (list_publicIP,
                               get_domain,
                               get_zone,
                               get_template,
                               list_networks,
                               list_templates,
                               list_service_offering,
                               list_vpc_offerings,
                               list_network_offerings,
                               list_routers,
                               list_hosts                               
                               )
from marvin.lib.utils import (cleanup_resources,
                              random_gen,
                              get_process_status,
                              get_host_credentials
                              )
import time
import string
from marvin.sshClient import SshClient
import traceback
import thread
import json
from marvin.codes import (
    SUCCESS, FAILED
)

class TestMultipleVPNAccessonVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
                    
        
        cloudstackTestClient = super(
                                     TestMultipleVPNAccessonVPC,
                                     cls
                                    ).getClsTestClient()
        
        cls.debug("Obtain the Admin's API Client")                                
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.debug("Get the dictionary information that will be used during CCP tests, from test_data.py present on the Client")
        cls.services = cloudstackTestClient.getParsedTestDataConfig()
        
        if cls.services is None:
            cls.debug("Services Object is None")
            raise Exception("Services Object is None")
        
        cls.debug("Procure the CloudStack Setup configuration Information")
        with open(cls.services["config_path"], 'rb') as fp:
            cls.pullconfig = json.load(fp)
        
        cls.debug("Update 'remote.access.vpn.client.iprange','remote.access.vpn.user.limit','max.account.primary.storage','max.account.public.ips','max.account.user.vms','max.account.volumes','max.account.cpus', Global Configuration Parameters")
        
        update_vpn_client_iprange = Configurations.update(
            cls.api_client, 
            name="remote.access.vpn.client.iprange", 
            value="10.1.2.1-10.1.2.120")
        
        cls.debug("'remote.access.vpn.client.iprange' Global Configuration Parameter Updated Successfully")
        
        update_vpn_user_limit = Configurations.update(
            cls.api_client,
            name="remote.access.vpn.user.limit", 
            value=str(int(cls.services["vpnclient_count"]*2))
        )    
        
        cls.debug("'remote.access.vpn.user.limit' Global Configuration Parameter Updated Successfully")

        update_max_account_primary_stg_limit = Configurations.update(
            cls.api_client,
            name="max.account.primary.storage", 
            value=str(int(cls.services["vpnclient_count"]*20 + 100))
        )
        cls.debug("'max.account.primary.storage' Global Configuration Parameter Updated Successfully")

        update_max_account_public_ips_limit = Configurations.update(
            cls.api_client,
            name="max.account.public.ips", 
            value=str(int(cls.services["vpnclient_count"]*2 + 10))
        )
        cls.debug("'max.account.public.ips' Global Configuration Parameter Updated Successfully")
        
        update_max_account_user_vms_limit = Configurations.update(
            cls.api_client,
            name="max.account.user.vms", 
            value=str(int(cls.services["vpnclient_count"]*2))
        )
        cls.debug("'max.account.user.vms' Global Configuration Parameter Updated Successfully")
        
        update_max_account_volumes_limit = Configurations.update(
            cls.api_client,
            name="max.account.volumes", 
            value=str(int(cls.services["vpnclient_count"]*2))
        )
        cls.debug("'max.account.volumes' Global Configuration Parameter Updated Successfully")

        update_max_account_cpus_limit = Configurations.update(
            cls.api_client,
            name="max.account.cpus", 
            value=str(int(cls.services["vpnclient_count"]*2))
        )
        cls.debug("'max.account.cpus' Global Configuration Parameter Updated Successfully")
        
        cls.debug("Restart the Management Server")
        TestMultipleVPNAccessonVPC.restart_mgmt_server(cls.services["config_path"])
        cls.debug("Completed restarting the Management Server")
        
        cls.debug("Wait for 120 seconds...")
        time.sleep(120)
        cls.debug("End of 120 seconds wait time....")
        
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            zone_name = cls.services["zone_vpn"]["name"])
        
        cls.debug("Use an Existing 'Tiny Instance' Service Offering on the Setup")
        list_service_offerings = []
        list_service_offerings = list_service_offering(
            cls.api_client,
            keyword="Tiny Instance",
        )
        
        cls._cleanup = []
            
        if list_service_offerings is not None:
            cls.debug("Found an Existing 'Tiny Instance' Service Offering on the Setup")
            cls.service_offering = list_service_offerings[0]
            
        else:
            cls.debug("Create a service offering which will be used for VM deployments in this test")
            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"]
            )
            
            cls.debug("Add the created service offering to the _cleanup queue")
            cls._cleanup.append(cls.service_offering)
        
        try:
            cls.debug("Create or Use Existing Account to own the VPN Clients, which is used to test Remote VPN Access to VPC")
            cls.api_client_vpn_client_reg_user = cloudstackTestClient.getUserApiClient(
                UserName="CSRegularVPNClientUser",
                DomainName="ROOT"
            )

            list_vpn_client_regular_user = User.list(
                cls.api_client,
                username="CSRegularVPNClientUser"
            )
            
            cls.debug("Procure the Account Name and DomainID Information of the Regular Account")
            cls.vpn_client_reg_acct_name = list_vpn_client_regular_user[0].account
            cls.vpn_client_reg_domain_id = list_vpn_client_regular_user[0].domainid
            
            list_vpn_client_regular_user_acct = Account.list(
                cls.api_client,
                name = cls.vpn_client_reg_acct_name,
                listall = True
            )
            
            cls._cleanup.append(Account(list_vpn_client_regular_user_acct[0].__dict__))
            
            # Register a Template that already has VPN client installed on it. The template registered here
            # has extra scripts to facilitate automated operations to execute Test Cases.
            # Template has pre-configured configuration files required for the VPN Client operations.
            # The following files are present on the registered template. The location of the files are locations
            # on a VM deployed from this template
            #            1. "/tmp/ipsec.conf"
            #            2. "/tmp/ipsec.secrets"
            #            3. "/tmp/options.xl2tpd.client"
            #            4. "/tmp/xl2tpd.conf"
            #            5  "/tmp/vpnclient_services.sh"
            #            6. "/tmp/firstconn_expectscript.exp"
            #            7. "/tmp/secondconn_expectscript.exp"
            
            cls.debug("Use an Existing VPN Client Template on the Setup")
            list_vpn_client_templates = list_templates(
                cls.api_client_vpn_client_reg_user,
                keyword="VPNClient",
                templatefilter="featured",
                zoneid = cls.zone.id
            )
            
            if list_vpn_client_templates is not None:
                cls.debug("Found an Existing VPN Client Template on the Setup")
                cls.template = list_vpn_client_templates[0]
            
            else:
                cls.debug("Register a Template that already has VPN client installed on it")
                cls.template = Template.register(
                    cls.api_client, 
                    cls.services["vpn_template"], 
                    zoneid=cls.zone.id,
                    hypervisor='XenServer' 
                )

                cls._cleanup.append(cls.template)

                cls.debug("Sleep for {0} seconds specified in the dictionary before checking for Template's Availability".format(cls.services["sleep"]))
                time.sleep(cls.services["sleep"])
                
            cls.debug("Procure Timeout Value from the dictionary")
            timeout = cls.services["timeout"]
            
            while True:
                list_template_response = list_templates(
                    cls.api_client_vpn_client_reg_user,
                    templatefilter='featured',
                    id=cls.template.id,
                )
            
                if isinstance(list_template_response, list):
                    break
                elif timeout == 0:
                    raise Exception("List template failed!")

                time.sleep(5)
                timeout = timeout - 1
            
            cls.debug("Verify template response to check whether template is present")
            
            if list_template_response is None: 
                raise Exception("Check whether the VPN Client Template is available")
            template_response = list_template_response[0]
            if template_response.isready == False: 
                raise Exception("Template state is not ready, it is %r" % template_response.isready)
            
            # Queue that holds all the VPN Client VMs Information
            cls.vpnclientvms = []
            
            cls.debug("Deploy {0} VPN Clients in the account".format(int(cls.services["vpnclient_count"])))
            
            for vm in xrange(0,int(cls.services["vpnclient_count"])):
                
                cls.debug("Deploy a new VM {0} in first account. This VM which will be configured as VPN Client".format(int(vm)))
                new_vpnclient_vm = VirtualMachine.create(
                    cls.api_client_vpn_client_reg_user,
                    cls.services["virtual_machine"],
                    zoneid=cls.zone.id,
                    serviceofferingid=cls.service_offering.id,
                    templateid=cls.template.id,
                )
                
                cls.debug("Add new VM {0} to the vpnclientvms Queue".format(int(vm)))
                cls.vpnclientvms.append(new_vpnclient_vm)                

                cls.debug("Allow SSH Access to the new VPN Client VM {0}".format(int(vm)))
                new_vpnclient_vm.access_ssh_over_nat(
                    cls.api_client_vpn_client_reg_user, 
                    cls.services, 
                    new_vpnclient_vm, 
                    allow_egress=True
                )
                cls.debug("VM for VPNClient Access Got Created with Public IP Address %s" % new_vpnclient_vm.public_ip)
            
            cls.debug("Create or Use existing Account in which we deploy VPCs and test remote access to them from the First Account's VMs present on isolated Network")          
            cls.api_client_vpn_server_reg_user = cloudstackTestClient.getUserApiClient(
                UserName="CSRegularVPNServerUser",
                DomainName="ROOT"
            )

            list_vpn_server_regular_user = User.list(
                cls.api_client,
                username="CSRegularVPNServerUser"
            )
            
            cls.debug("Procure the Account Name and DomainID Information of the Regular Account")
            cls.vpn_server_reg_acct_name = list_vpn_server_regular_user[0].account
            cls.vpn_server_reg_domain_id = list_vpn_server_regular_user[0].domainid
        
            list_vpn_server_regular_user_acct = Account.list(
                cls.api_client,
                name = cls.vpn_server_reg_acct_name,
                listall = True
            )
            
            cls._cleanup.append(Account(list_vpn_server_regular_user_acct[0].__dict__))        
        
            cls.debug("Use an Existing 'VPC off-' Service Offering on the Setup")
            list_available_vpc_offerings = list_vpc_offerings(
                cls.api_client,
                keyword="VPC off-",
            )
            
            if list_available_vpc_offerings is not None:
                cls.debug("Found an Existing 'VPC off-' Service Offering on the Setup")
                cls.vpc_offering = VpcOffering(list_available_vpc_offerings[0].__dict__)
            
            else:
                cls.debug("Creating a VPC offering..")
                cls.vpc_offering = VpcOffering.create(
                    cls.api_client,
                    cls.services["vpc_offering"]
                )

                # Add the created VPC Offering to __cleanup queue
                cls._cleanup.append(cls.vpc_offering)

            # Enable to created VPC Offering inorder to deploy VPCs with it       
            cls.debug("Enabling the VPC offering created")
            cls.vpc_offering.update(cls.api_client, state='Enabled')
            cls.debug("Enabled the VPC Offering")

            # Create a VPC for the second account
            cls.debug("Creating a VPC in the account: %s" % cls.vpn_server_reg_acct_name)
            cls.firstvpc = VPC.create(
                cls.api_client_vpn_server_reg_user,
                cls.services["vpc_remote_vpn"],
                vpcofferingid=cls.vpc_offering.id,
                zoneid=cls.zone.id
            )        
        
            cls.debug("Use an Existing 'NET_OFF-RemoteAccessVPNTest-' Network Offering on the Setup")
            list_available_network_offerings = list_network_offerings(
                cls.api_client,
                keyword="NET_OFF-RemoteAccessVPNTest-",
            )
            
            if list_available_network_offerings is not None:
                cls.debug("Found an Existing 'NET_OFF-RemoteAccessVPNTest-' Network Offering on the Setup")
                cls.network_off = NetworkOffering(list_available_network_offerings[0].__dict__)
            
            else:
                cls.debug('Create NetworkOffering for Networks in VPC')
                cls.services["vpc_network_offering"]["name"] = "NET_OFF-RemoteAccessVPNTest-"+ random_gen()
                cls.network_off = NetworkOffering.create(
                    cls.api_client,
                    cls.services["vpc_network_offering"],
                    conservemode=False
                )

                # Add the created Network Offering to __cleanup queue
                cls._cleanup.append(cls.network_off)
        
            # Enable Network offering
            cls.network_off.update(cls.api_client, state='Enabled')
                    
            cls.debug('Created and Enabled NetworkOffering')
            cls.services["network"]["name"] = "NETWORK-" + random_gen()
            
            # Create First Network Tier in the First VPC created for second account using the network offering created above.    
            cls.debug('Adding Network=%s' % cls.services["network"])
            cls.firstnetworktier = Network.create(
                cls.api_client_vpn_server_reg_user,
                cls.services["network"],
                networkofferingid=cls.network_off.id,
                zoneid=cls.zone.id,
                gateway=cls.services["firstnetwork_tier"]["gateway"],
                netmask=cls.services["firstnetwork_tier"]["netmask"],
                vpcid=cls.firstvpc.id 
            )
        
            cls.debug("Created network with ID: %s" % cls.firstnetworktier.id)
        
            # Create Ingress and Egress NetworkACL rules for First Network Tier in the First VPC created for second account.
            cls.debug("Adding NetworkACL rules to make Network accessible for all Protocols and all CIDRs ")
            NetworkACL.create(
                cls.api_client_vpn_server_reg_user,
                cls.services["all_rule"],
                networkid=cls.firstnetworktier.id,
                traffictype='Ingress'
            )
        
            NetworkACL.create(
                cls.api_client_vpn_server_reg_user,
                cls.services["all_rule"],
                networkid=cls.firstnetworktier.id,
                traffictype='Egress'
            )
         
            listFirstVPC = VPC.list(
                cls.api_client_vpn_server_reg_user,
                id=cls.firstvpc.id
            )
            
            cls.debug("Information about the VPC: {0}".format(str(listFirstVPC)))
        
            cls.debug("Obtain the source nat IP Address of the first VPC.")
            cls.listFirstVPCPublicIpAddress = list_publicIP(
                cls.api_client_vpn_server_reg_user,
                issourcenat="true",
                vpcid=listFirstVPC[0].id,
                listall="true"
            )
            cls.debug("Information about the VPC's Source NAT IP Address: {0}".format(str(cls.listFirstVPCPublicIpAddress)))
        
            cls.debug("Enable Remote Access VPN on the source nat Public IP Address of the first VPC")
            cls.FirstVPNonFirstVPC = Vpn.create(
                cls.api_client_vpn_server_reg_user,
                cls.listFirstVPCPublicIpAddress[0].id
            )

            cls.debug("Successfully Created First VPN on VPC with preshared key:"+ cls.FirstVPNonFirstVPC.presharedkey)
            cls.listfirstNetworkTier = list_networks(
                cls.api_client_vpn_server_reg_user,
                id=cls.firstnetworktier.id,
                listall=True
            )


            cls.debug("Create a VM using the default template on the First Network Tier in the First VPC of the Second Account")
            cls.vm1 = VirtualMachine.create(
                cls.api_client_vpn_server_reg_user,
                cls.services["virtual_machine"],
                zoneid=cls.zone.id,
                serviceofferingid=cls.service_offering.id,
                templateid=cls.template.id,
                networkids=[str(cls.firstnetworktier.id)]
            )
        
            cls.debug("First VM deployed in the first Network Tier")
        
        except Exception as e:
            cleanup_resources(cls.api_client, cls._cleanup)
            printex = traceback.format_exc()
            cls.debug("Exception Occurred : {0}".format(printex))
            raise Exception("Warning: Exception during Setting Up the Test Suite Configuration : %s" % e)
        
        return
            
    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            print("Warning: Exception during cleanup : %s" % e)
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return
    
    @classmethod
    def filecopy(cls,virtual_machine,localfile=None,remotefilelocation=None,permissions="644"):
        cls.ssh = SshClient(
            host=virtual_machine.public_ip,
            port=TestMultipleVPNAccessonVPC.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')
        
        cls.ssh.scp(localfile,remotefilelocation)
        cls.ssh.runCommand('chmod %s %s' % (permissions,remotefilelocation))
        cls.debug("%s file successfully copied to %s " % (localfile, remotefilelocation))
        
        cls.ssh.close()

    @classmethod
    def restart_mgmt_server(cls,config):
        
        cls.ssh = SshClient(
            host=cls.pullconfig["mgtSvr"][0]["mgtSvrIp"],
            port=22,
            user=cls.pullconfig["mgtSvr"][0]["user"],
            passwd=cls.pullconfig["mgtSvr"][0]["passwd"]
                             )
        
        result = cls.ssh.runCommand("/etc/init.d/cloudstack-management restart")
        
        if result["status"] == SUCCESS:
            time.sleep(120)
        else:
            raise Exception("Failure in restarting cloudstack Management Server")
            
        cls.ssh.close()
 
    @staticmethod
    def char_xrange(firstchar, lastchar):
        #Generates a range of Alphabetical Characters, lastchar inclusive
        for c in xrange(ord(firstchar),ord(lastchar)+1):
            yield chr(c)
            
    @staticmethod
    def get_guest_ip_address(guest_ip_address):
        
        for ch in ["'","[","]","\\","n"]:
            if ch in guest_ip_address:
                guest_ip_address = guest_ip_address.replace(ch,"")
    
        return guest_ip_address

    @staticmethod
    def configureVPNClientServicesFile(virtual_machine,vpnclient_services_script,vpnserverip=None,vpnnetworkcidr="192.168.10.0/24",psk=None,vpnuser=None,vpnuserpassword=None):
        
        ssh = SshClient(
            host=virtual_machine.public_ip,
            port=TestMultipleVPNAccessonVPC.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')
        
        
        cidr = "\/".join(vpnnetworkcidr.rsplit("/",1))
                
        ssh.execute(''' sed -i "s/VPN_ADDR=.*/VPN_ADDR='%s'/g" %s ''' % (vpnserverip,vpnclient_services_script))
        ssh.execute(''' sed -i "s/CIDR=.*/CIDR='%s'/g" %s ''' % (cidr,vpnclient_services_script))
        ssh.execute(''' sed -i "s/PSK=.*/PSK='%s'/g" %s ''' % (psk,vpnclient_services_script))
        ssh.execute(''' sed -i "s/VPN_USR=.*/VPN_USR='%s'/g" %s ''' % (vpnuser,vpnclient_services_script))
        ssh.execute(''' sed -i "s/VPN_USR_PWD=.*/VPN_USR_PWD='%s'/g" %s ''' % (vpnuserpassword,vpnclient_services_script))
        ssh.execute('echo "VPN Client Services File Ready" >> /tmp/executionoutput.txt')
        
        ssh.close()

    @staticmethod
    def vpnClientServicesInit(virtual_machine,vpnclient_services_script):
        
        ssh = SshClient(
            host=virtual_machine.public_ip,
            port=TestMultipleVPNAccessonVPC.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')
                                     
        ssh.execute('%s init >> /tmp/executionoutput.txt' % (vpnclient_services_script))
        ssh.execute('echo "VPN Client Services Configuration Files Initiated" >> /tmp/executionoutput.txt')
        
        ssh.close()
                                   
    @staticmethod
    def vpnClientServicesStart(virtual_machine,vpnclient_services_script):
        
        ssh = SshClient(
            host=virtual_machine.public_ip,
            port=TestMultipleVPNAccessonVPC.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')
                                     
        ssh.execute('%s start >> /tmp/executionoutput.txt' % (vpnclient_services_script))
        ssh.execute('echo "VPN Client Services Started" >> /tmp/executionoutput.txt')
        
        ssh.close()
        
        
    def exec_script_on_user_vm(self, virtual_machine,script, exec_cmd_params, expected_result, negative_test=False,public_ip=None):
        try:
            exec_success = False

            if public_ip is not None:
                self.debug("getting SSH client for Vm: %s with ip %s" % (virtual_machine.id,public_ip))
                sshClient = virtual_machine.get_ssh_client(ipaddress=public_ip)
            else:
                self.debug("getting SSH client for Vm: %s with ip %s" % (virtual_machine.id,virtual_machine.public_ip))
                sshClient = virtual_machine.get_ssh_client(ipaddress=virtual_machine.public_ip)
            
            result = sshClient.execute(script+exec_cmd_params)

            self.debug("script: %s" % script+exec_cmd_params)
            self.debug("result: %s" % result)

            str_result = str(str(result).strip())
            str_expected_result = str(expected_result).strip()
            if str_result == str_expected_result:
                exec_success = True

            if negative_test:
                self.assertEqual(exec_success,
                                 True,
                                 "Script result is %s matching with %s" % (str_result, str_expected_result))
            else:
                self.assertEqual(exec_success,
                                 True,
                                 "Script result is %s is not matching with %s" % (str_result, str_expected_result))

        except Exception as e:
            self.debug('Error=%s' % e)
            printex = traceback.format_exc()
            self.debug("Exception Occurred : {0}".format(printex))
            raise e
    
    def ping_vm(self, virtual_machine, guest_ip_address, count=1, interval=15, thread_name="Thread-None"):
        
        try:
            self.debug("{4} : Check whether VM with ID: {0} with Public IP: {1} is able to ping Guest VM with IP: {2} - {3} Times !!!".format(virtual_machine.id, virtual_machine.public_ip, guest_ip_address, count, thread_name))
            iteration = 1
            total_times = count
            
            while count > 0:
                self.debug("{4} : Test whether VM with ID: {0} with Public IP: {1} is able to ping Guest VM with IP: {2} for {3} iteration".format(virtual_machine.id, virtual_machine.public_ip, guest_ip_address, iteration, thread_name))
                self.exec_script_on_user_vm(
                    virtual_machine,
                    'ping -c 1 {0}'.format(guest_ip_address),
                    "| grep -oP \'\d+(?=% packet loss)\'",
                    "[u'0']",
                    negative_test=False
                )
                self.debug("{4} : Verified Successfully that VM with ID: {0} with Public IP: {1} is able to ping Guest VM with IP: {2} for {3} iteration".format(virtual_machine.id, virtual_machine.public_ip, guest_ip_address, iteration, thread_name))
                count = count-1
                iteration = iteration + 1
                
                self.debug("{1} : Wait for {0} seconds before next ping".format(interval,thread_name))
                time.sleep(interval)
                self.debug("Still {0} iterations left for Thread {1}".format(count,thread_name))
                
            if count == 0:
                self.debug("Ping {0} Times is Completed for Thread {1} ".format(total_times,thread_name))
            else:
                raise Exception("Count value is still at {0}".format(count))
            
        except Exception as e:
        
            printex = traceback.format_exc()
            self.debug("Exception Occurred : {0}".format(printex))
            raise Exception("Warning: Exception during pinging VM : %s" % e)
        

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup=None
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "intervlan"],required_hardware="true")
    def test_01_Multiple_RemoteAccessVPN_Connections_To_VPC_Ping_Guest_VM_Multiple_Times(self):
        """ Test case no : Test Multiple VPN Connections to a VPN Server on VPC


        # Validate the following for Each VPN VM Client
        # 1. Create VPN User on the VPC
        # 2. Configure the VPN Client VM with the required Information
        # 3. Initialize the VPN Client Services on the VPN Client
        # 4. Start the VPN Client Services on the VPN Client
        # 5. Wait for 30 seconds before attempting to ping
        # 6. Conduct the Ping Test on the VM
        
        # After the deployment VPN Client VMs and the post deployment steps, do the following steps: 
        # 7. Wait for 60 seconds
        # 8. Check Routers pppX NICs Information
        """        
        
        for vm in xrange(0,int(TestMultipleVPNAccessonVPC.services["vpnclient_count"])):
                
            vpn_user_name = ''.join((str(vm),"-user"))
            vpn_password = ''.join((str(vm),"-pass"))
                
            self.debug("VPN User Name created with %s " % vpn_user_name)
            self.debug("VPN Password created with %s " % vpn_password)
            
            self.debug("Create new VPN User to use the Remote Access Service enabled on the VPC") 
            newVPNUser = VpnUser.create(
                TestMultipleVPNAccessonVPC.api_client_vpn_server_reg_user, 
                vpn_user_name, 
                vpn_password, 
                rand_name=False
            )
            self.debug("VPN User %s got created Successfully " % vpn_user_name)

            self.debug("Configure the VPN Client Services on the VM deployed for VPN client purpose.")
            TestMultipleVPNAccessonVPC.configureVPNClientServicesFile(
                TestMultipleVPNAccessonVPC.vpnclientvms[vm],
                "/tmp/vpnclient_services.sh",
                TestMultipleVPNAccessonVPC.listFirstVPCPublicIpAddress[0].ipaddress,
                TestMultipleVPNAccessonVPC.listfirstNetworkTier[0].cidr,
                TestMultipleVPNAccessonVPC.FirstVPNonFirstVPC.presharedkey,
                vpn_user_name,
                vpn_password
            )        
            self.debug("Configuration of VPN Client VM %d Done " % (vm))
                
            self.debug("Initialize the VPN Client Services on the VPN Client")
            TestMultipleVPNAccessonVPC.vpnClientServicesInit(
                TestMultipleVPNAccessonVPC.vpnclientvms[vm],
                "/tmp/vpnclient_services.sh"
            )
            self.debug("Initiation of VPN Client Services on VM %d Done " % (vm))
            
            self.debug("Start the VPN Client Services on the VPN Client")
            TestMultipleVPNAccessonVPC.vpnClientServicesStart(
                TestMultipleVPNAccessonVPC.vpnclientvms[vm],
                "/tmp/vpnclient_services.sh"
            )
            self.debug("VPN Client Services on VM %d Started Successfully " % (vm))
            
            self.debug("Wait for 30 seconds before attempting to ping")
            time.sleep(30)
            
            self.debug("Conduct the Ping Test on the VM %d" % (vm))
            thread.start_new_thread(self.ping_vm,(
                TestMultipleVPNAccessonVPC.vpnclientvms[vm], 
                TestMultipleVPNAccessonVPC.vm1.nic[0].ipaddress,
                25000,
                15,
                "Thread-{0}".format(vm)
            ))
            
        self.debug("Waiting for 60 seconds.........")
        time.sleep(60)
        self.debug("End of 60 seconds.........")
        
        # Find router associated with user account
        list_router_response = list_routers(
            self.apiclient,
            vpcid= TestMultipleVPNAccessonVPC.firstvpc.id,
            listall=True
        )
        
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        
        router = list_router_response[0]

        hosts = list_hosts(
            self.apiclient,
            zoneid=router.zoneid,
            type='Routing',
            state='Up',
            id=router.hostid
        )
        
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )
        
        host = hosts[0]

        self.debug("Router ID: %s, state: %s" % (router.id, router.state))
        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )

        if self.hypervisor.lower() == 'vmware':
           result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                router.linklocalip,
                "ifconfig | grep ppp",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    "ifconfig | grep ppp"
                )
                self.debug("Routers pppX NICs Information : %s" % str(result))
            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check router services")
        
