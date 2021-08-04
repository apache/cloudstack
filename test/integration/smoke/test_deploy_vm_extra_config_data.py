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
""" BVT tests for Virtual Machine additional configuration
"""
# Import System modules
import urllib.request, urllib.parse, urllib.error
import xml.etree.ElementTree as ET

from lxml import etree
from marvin.cloudstackAPI import (updateVirtualMachine,
                                  deployVirtualMachine,
                                  destroyVirtualMachine,
                                  stopVirtualMachine,
                                  startVirtualMachine,
                                  updateConfiguration,
                                  listVirtualMachines)
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             Host)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_hosts)
from marvin.lib.utils import *
from nose.plugins.attrib import attr

class TestAddConfigtoDeployVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestAddConfigtoDeployVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Create an account, network, and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls.cleanup = [
            cls.account,
            cls.service_offering
        ]

        cls.hosts_hugepages = cls.set_hosts_hugepages()

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestAddConfigtoDeployVM, cls).getClsTestClient().getApiClient()
            cls.reset_hosts_hugepages()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def set_hosts_hugepages(cls):
        hosts_hugepages = []
        listHost = Host.list(
               cls.apiclient,
               type='Routing',
               zoneid=cls.zone.id
           )
        for host in listHost:
            if host.hypervisor.lower() == 'kvm':
                sshClient = SshClient(host.ipaddress, port=22, user=cls.hostConfig["username"], passwd=cls.hostConfig["password"])
                result = sshClient.execute("sysctl -n vm.nr_hugepages")
                sshClient.execute("sysctl -w vm.nr_hugepages=1024")
                if result and len(result) > 0:
                    hosts_hugepages.append({ "ipaddress": host.ipaddress, "vm.nr_hugepages": result[0].strip()})
        return hosts_hugepages

    @classmethod
    def reset_hosts_hugepages(cls):
        for host in cls.hosts_hugepages:
            sshClient = SshClient(host["ipaddress"], port=22, user=cls.hostConfig["username"], passwd=cls.hostConfig["password"])
            sshClient.execute("sysctl -w vm.nr_hugepages=%s" % host["vm.nr_hugepages"])

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()

        """
        Set EnableAdditionalData to true
        """
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "enable.additional.vm.configuration"
        updateConfigurationCmd.value = "true"
        updateConfigurationCmd.scopename = "account"
        updateConfigurationResponse = self.apiclient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s" % (
            updateConfigurationResponse.name, updateConfigurationResponse.value))

    # Ste Global Config value
    def add_global_config(self, name, value):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()

        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        return self.apiclient.updateConfiguration(cmd)

    # Compare XML Element objects
    def elements_equal(self, e1, e2):
        if e1.tag != e2.tag:
            return False
        if e1.attrib != e2.attrib:
            return False
        if len(e1) != len(e2):
            return False
        return all(self.elements_equal(c1, c2) for c1, c2 in zip(e1, e2))

    def destroy_vm(self, vm_id):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.expunge = True
        cmd.id = vm_id
        return self.apiclient.destroyVirtualMachine(cmd)

    def deploy_vm(self, hypervisor, extra_config=None):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        if extra_config is not None:
            cmd.extraconfig = extra_config

        template = get_template(
            self.apiclient,
            self.zone.id,
            hypervisor=hypervisor
        )
        cmd.zoneid = self.zone.id
        cmd.templateid = template.id
        cmd.serviceofferingid = self.service_offering.id
        return self.apiclient.deployVirtualMachine(cmd)

    def list_vm(self):
        cmd = listVirtualMachines.listVirtualMachinesCmd()
        cmd.hypervisor = self.hypervisor
        return self.apiclient.listVirtualMachines(cmd)[0]

    def update_vm(self, id, extra_config):
        cmd = updateVirtualMachine.updateVirtualMachineCmd()
        cmd.id = id
        cmd.extraconfig = extra_config
        return self.apiclient.updateVirtualMachine(cmd)

    def stop_vm(self, id):
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = id
        return self.apiclient.stopVirtualMachine(cmd)

    def start_vm(self, id):
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = id
        return self.apiclient.startVirtualMachine(cmd)

    # Parse extraconfig for config with that returned by xe vm-param-get ...
    def get_xen_param_values(self, config):
        equal_sign_index = config.index("=")
        cmd_option = config[:equal_sign_index]
        cmd_value = config[equal_sign_index + 1:]
        return cmd_option, cmd_value

    # Format vm config such that it equals the one from vmx file
    def prepare_vmware_config(self, config):
        equal_sign_index = config.index("=")
        cmd_option = config[:equal_sign_index]
        cmd_value = config[equal_sign_index + 1:]
        return cmd_option + ' = '  '"{}"'.format(cmd_value)

    # Get vm uuid from xenserver host
    def get_vm_uuid(self, instance_name, ssh_client):
        cmd = 'xe vm-list name-label={} params=uuid '.format(instance_name)
        result = ssh_client.execute(cmd)
        uuid_str = result[0]
        i = uuid_str.index(":")
        return uuid_str[i + 1:].strip()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_01_deploy_vm_with_extraconfig_throws_exception_kvm(self):
        '''
        Test that extra config is not added when element tag is not added on the allowed list global config on KVM hosts
        '''

        hypervisor = self.hypervisor.lower()
        if hypervisor != 'kvm':
            raise self.skipTest("Skipping test case for non-kvm hypervisor")

        '''
        The following extraconfig is required for enabling hugepages on kvm
        <memoryBacking>
            <hugepages/>
        </memoryBacking>
        url encoded extra_config = '%3CmemoryBacking%3E%0D%0A++%3Chugepages%2F%3E%0D%0A%3C%2FmemoryBacking%3E'
        '''
        extraconfig = "%3CmemoryBacking%3E%0D%0A++%3Chugepages%2F%3E%0D%0A%3C%2FmemoryBacking%3E"

        try:
            # Clear KVM allow list to show that code throws exception when command is not included in the list
            name = 'allow.additional.vm.configuration.list.kvm'

            self.add_global_config(name, "")
            self.assertRaises(Exception,
                              self.deploy_vm(hypervisor, extraconfig),
                              "Exception was not thrown, check kvm global configuration")
        except Exception as e:
            logging.debug(e)
        finally:
            self.destroy_vm(self.list_vm().id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_02_deploy_vm_with_extraconfig_kvm(self):
        '''
        Test that extra config is added on KVM hosts
        '''

        hypervisor = self.hypervisor.lower()
        if hypervisor != 'kvm':
            raise self.skipTest("Skipping test case for non-kvm hypervisor")

        name = 'allow.additional.vm.configuration.list.kvm'
        value = 'memoryBacking, hugepages, unsedConfigKey'

        add_config_response = self.add_global_config(name, value)

        if add_config_response.name:
            try:
                '''
                The following extraconfig is required for enabling hugepages on kvm
                <memoryBacking>
                    <hugepages/>
                </memoryBacking>
                url encoded extra_config = '%3CmemoryBacking%3E%0D%0A++%3Chugepages%2F%3E%0D%0A%3C%2FmemoryBacking%3E'
                '''
                extraconfig = "%3CmemoryBacking%3E%0D%0A++%3Chugepages%2F%3E%0D%0A%3C%2FmemoryBacking%3E"

                response = self.deploy_vm(hypervisor, extraconfig)

                host_id = response.hostid
                host = list_hosts(
                    self.apiclient,
                    id=host_id,
                    hypervisor=hypervisor)

                instance_name = response.instancename
                host_ipaddress = host[0].ipaddress

                ssh_client = SshClient(host_ipaddress, port=22,
                                       user=self.hostConfig['username'],
                                       passwd=self.hostConfig['password'])
                virsh_cmd = 'virsh dumpxml %s' % instance_name
                xml_res = ssh_client.execute(virsh_cmd)
                xml_as_str = ''.join(xml_res)

                extraconfig_decoded_xml = '<config>' + urllib.parse.unquote(extraconfig) + '</config>'

                # Root XML Elements
                parser = etree.XMLParser(remove_blank_text=True)
                domain_xml_root = ET.fromstring(xml_as_str, parser=parser)
                decoded_xml_root = ET.fromstring(extraconfig_decoded_xml, parser=parser)
                for child in decoded_xml_root:
                    find_element_in_domain_xml = domain_xml_root.find(child.tag)

                    # Fail if extra config is not found in domain xml
                    self.assertNotEqual(
                        0,
                        len(find_element_in_domain_xml),
                        'Element tag from extra config not added to VM'
                    )

                    # Compare found XML node with extra config node
                    is_a_match = self.elements_equal(child, find_element_in_domain_xml)
                    self.assertEqual(
                        True,
                        is_a_match,
                        'The element from tags from extra config do not match with those found in domain xml'
                    )
            finally:
                self.destroy_vm(response.id)
                self.add_global_config(name, "")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_03_update_vm_with_extraconfig_kvm(self):
        '''
        Test that extra config is added on KVM hosts
        '''

        hypervisor = self.hypervisor.lower()
        if hypervisor != 'kvm':
            raise self.skipTest("Skipping test case for non-kvm hypervisor")

        name = 'allow.additional.vm.configuration.list.kvm'
        value = 'memoryBacking, hugepages'

        add_config_response = self.add_global_config(name, value)

        if add_config_response.name:
            try:
                '''
                The following extraconfig is required for enabling hugepages on kvm
                <memoryBacking>
                    <hugepages/>
                </memoryBacking>
                url encoded extra_config = '%3CmemoryBacking%3E%0D%0A++%3Chugepages%2F%3E%0D%0A%3C%2FmemoryBacking%3E'
                '''
                extraconfig = "%3CmemoryBacking%3E%0D%0A++%3Chugepages%2F%3E%0D%0A%3C%2FmemoryBacking%3E"

                response = self.deploy_vm(hypervisor)
                vm_id = response.id

                '''
                For updateVirtualMachineCmd, the VM must be stopped and restarted for changes to take effect
                '''
                self.stop_vm(vm_id)
                self.update_vm(vm_id, extraconfig)
                start_resp = self.start_vm(vm_id)

                host_id = start_resp.hostid
                host = list_hosts(
                    self.apiclient,
                    id=host_id,
                    hypervisor=hypervisor)

                instance_name = response.instancename
                host_ipaddress = host[0].ipaddress

                ssh_client = SshClient(host_ipaddress, port=22,
                                       user=self.hostConfig['username'],
                                       passwd=self.hostConfig['password'])
                virsh_cmd = 'virsh dumpxml %s' % instance_name
                xml_res = ssh_client.execute(virsh_cmd)
                xml_as_str = ''.join(xml_res)

                extraconfig_decoded_xml = '<config>' + urllib.parse.unquote(extraconfig) + '</config>'

                # Root XML Elements
                parser = etree.XMLParser(remove_blank_text=True)
                domain_xml_root = ET.fromstring(xml_as_str, parser=parser)
                decoded_xml_root = ET.fromstring(extraconfig_decoded_xml, parser=parser)
                for child in decoded_xml_root:
                    find_element_in_domain_xml = domain_xml_root.find(child.tag)

                    # Fail if extra config is not found in domain xml
                    self.assertNotEqual(
                        0,
                        len(find_element_in_domain_xml),
                        'Element tag from extra config not added to VM'
                    )

                    # Compare found XML node with extra config node
                    is_a_match = self.elements_equal(child, find_element_in_domain_xml)
                    self.assertEqual(
                        True,
                        is_a_match,
                        'The element from tags from extra config do not match with those found in domain xml'
                    )
            finally:
                self.destroy_vm(vm_id)
                self.add_global_config(name, "")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_04_deploy_vm_with_extraconfig_throws_exception_vmware(self):
        '''
        Test that extra config is not added when configuration key is not added on the allowed list global config for VMWARE hosts
        '''

        hypervisor = self.hypervisor.lower()
        if hypervisor != 'vmware':
            raise self.skipTest("Skipping test case for non-vmware hypervisor")

        '''
        The following extra configuration is used to set Hyper-V instance to run on ESXi host
        hypervisor.cpuid.v0 = FALSE
        '''
        extraconfig = 'hypervisor.cpuid.v0%3DFALSE'

        try:
            # Clear VMWARE allow list to show that code throws exception when command is not included in the list
            name = 'allow.additional.vm.configuration.list.vmware'

            self.add_global_config(name, "")
            self.assertRaises(Exception,
                              self.deploy_vm(hypervisor, extraconfig),
                              "Exception was not thrown, check VMWARE global configuration")
        except Exception as e:
            logging.debug(e)
        finally:
            self.destroy_vm(self.list_vm().id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_05_deploy_vm_with_extraconfig_vmware(self):
        '''
        Test that extra config is added on VMware hosts
        '''
        hypervisor = self.hypervisor.lower()
        if hypervisor != 'vmware':
            raise self.skipTest("Skipping test case for non-vmware hypervisor")

        name = 'allow.additional.vm.configuration.list.vmware'
        value = 'hypervisor.cpuid.v0'

        add_config_response = self.add_global_config(name, value)

        if add_config_response.name:

            '''
            The following extra configuration is used to set Hyper-V instance to run on ESXi host
            hypervisor.cpuid.v0 = FALSE
            '''
            extraconfig = 'hypervisor.cpuid.v0%3DFALSE'
            try:
                response = self.deploy_vm(hypervisor, extraconfig)
                host_id = response.hostid
                host = list_hosts(
                    self.apiclient,
                    id=host_id)

                instance_name = response.instancename
                host_ipaddress = host[0].ipaddress

                ssh_client = SshClient(host_ipaddress, port=22,
                                       user=self.hostConfig['username'],
                                       passwd=self.hostConfig['password'])

                extraconfig_decoded = urllib.parse.unquote(extraconfig)
                config_arr = extraconfig_decoded.splitlines()

                for config in config_arr:
                    vmx_config = self.prepare_vmware_config(config)
                    vmx_file_name = "\"$(esxcli vm process list | grep %s | tail -1 | awk '{print $3}')\"" % instance_name
                    # parse vm instance vmx file to see if extraconfig has been added
                    grep_config = "cat %s | grep -w '%s'" % (vmx_file_name, vmx_config)
                    result = ssh_client.execute(grep_config)
                    # Match exact configuration from vmx file, return empty result array if configuration is not found
                    self.assertNotEqual(
                        0,
                        len(result),
                        'Extra  configuration not found in instance vmx file'
                    )
            finally:
                self.destroy_vm(response.id)
                self.add_global_config(name, "")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_06_deploy_vm_with_extraconfig_throws_exception_xenserver(self):
        '''
        Test that extra config is not added when configuration key is not added on the allowed list global config for XenServer hosts
        '''

        hypervisor = self.hypervisor.lower()
        if hypervisor != 'xenserver':
            raise self.skipTest("Skipping test case for non-xenserver hypervisor")

        '''
        Following commands are used to convert a VM from HVM to PV and set using vm-param-set
        HVM-boot-policy=
        PV-bootloader=pygrub
        PV-args=hvc0
        '''

        extraconfig = 'HVM-boot-policy%3D%0APV-bootloader%3Dpygrub%0APV-args%3Dhvc0'

        try:
            # Clear VMWARE allow list to show that code throws exception when command is not included in the list
            name = 'allow.additional.vm.configuration.list.xenserver'

            self.add_global_config(name, "")
            self.assertRaises(Exception,
                              self.deploy_vm(hypervisor, extraconfig),
                              "Exception was not thrown, check XenServer global configuration")

        except Exception as e:
            logging.debug(e)
        finally:
            self.destroy_vm(self.list_vm().id)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_07_deploy_vm_with_extraconfig_xenserver(self):
        hypervisor = self.hypervisor.lower()
        if hypervisor != 'xenserver':
            raise self.skipTest("Skipping test case for non-xenserver hypervisor")
        """
        Following commands are used to convert a VM from HVM to PV and set using vm-param-set
        HVM-boot-policy=
        PV-bootloader=pygrub
        PV-args=hvc0
        """

        name = 'allow.additional.vm.configuration.list.xenserver'
        value = 'HVM-boot-policy, PV-bootloader, PV-args'

        add_config_response = self.add_global_config(name, value)

        if add_config_response.name:
            extraconfig = 'HVM-boot-policy%3D%0APV-bootloader%3Dpygrub%0APV-args%3Dhvc0'
            try:
                response = self.deploy_vm(hypervisor, extraconfig)
                host_id = response.hostid
                host = list_hosts(
                    self.apiclient,
                    id=host_id)

                host_ipaddress = host[0].ipaddress

                ssh_client = SshClient(host_ipaddress, port=22,
                                       user=self.hostConfig['username'],
                                       passwd=self.hostConfig['password'])

                extraconfig_decoded = urllib.parse.unquote(extraconfig)
                config_arr = extraconfig_decoded.splitlines()

                # Get vm instance uuid
                instance_uuid = self.get_vm_uuid(response.instancename, ssh_client)
                for config in config_arr:
                    config_tuple = self.get_xen_param_values(config)
                    # Log on to XenServer host and check the vm-param-get
                    vm_config_check = 'xe vm-param-get param-name={} uuid={}'.format(config_tuple[0], instance_uuid)
                    result = ssh_client.execute(vm_config_check)
                    param_value = config_tuple[1].strip()
                    # Check if each configuration command has set the configuration as sent with extraconfig
                    self.assertEqual(
                        param_value,
                        result[0],
                        'Extra  configuration not found in VM param list'
                    )
            finally:
                self.destroy_vm(response.id)
                self.add_global_config(name, "")
