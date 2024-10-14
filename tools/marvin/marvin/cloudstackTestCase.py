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
import time
import unittest
from marvin.lib.utils import verifyElementInList
from marvin.codes import PASS
from marvin.lib.base import (
    NATRule,
    Network,
    NetworkACL,
    NetworkOffering,
    VirtualMachine,
    Volume
)


def user(Name, DomainName, AcctType):
    def wrapper(cls):
        orig_init = cls.__init__

        def __init__(self, *args, **kws):
            cls.UserName = Name
            cls.DomainName = DomainName
            cls.AcctType = AcctType
            orig_init(self, *args, **kws)
        cls.__init__ = __init__
        return cls
    return wrapper


class cloudstackTestCase(unittest.case.TestCase):
    clstestclient = None

    def assertElementInList(self, inp, toverify, responsevar=None, pos=0,
                            assertmsg="TC Failed for reason"):
        '''
        @Name: assertElementInList
        @desc:Uses the utility function verifyElementInList and
        asserts based upon PASS\FAIL value of the output.
        Takes one additional argument of what message to assert with
        when faileddef start_vpcrouter(
        '''
        out = verifyElementInList(inp, toverify, responsevar, pos)
        unittest.TestCase.assertEqual(out[0], PASS, "msg:%s" % out[1])

    @classmethod
    def getClsTestClient(cls):
        return cls.clstestclient

    @classmethod
    def getClsConfig(cls):
        return cls.config

    @classmethod
    def tearDownClass(cls):
        cls.debug("Cleaning up the resources")
        try:
            if hasattr(cls,'_cleanup'):
                if hasattr(cls,'apiclient'):
                    cls.cleanup_resources(cls.apiclient, reversed(cls._cleanup))
                elif hasattr(cls,'api_client'):
                    cls.cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        self.debug("Cleaning up the resources")
        try:
            if hasattr(self,'apiclient') and hasattr(self,'cleanup'):
                self.cleanup_resources(self.apiclient, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def assertEqual(self, first, second, msg=None):
        """Fail if the two objects are unequal as determined by the '=='
           operator.
        """
        if isinstance(msg, str):
            msg = msg.encode()
        super(cloudstackTestCase,self).assertEqual(first,second,msg)

    @classmethod
    def cleanup_resources(cls, api_client, resources):
        """
            Delete resources (created during tests)
        """
        volume_list = []
        for obj in resources:
            if isinstance(obj, VirtualMachine):
                obj.delete(api_client, expunge=True)
            elif isinstance(obj, Volume):
                obj.destroy(api_client, expunge=True)
                volume_list.append(obj)
            else:
                obj.delete(api_client)

        cls.wait_for_volumes_cleanup(api_client, volume_list)

    def wait_for_volumes_cleanup(cls, api_client, volume_list=[]):
        """Wait for volumes to be deleted"""
        for volume in volume_list:
            max_retries = 24  # Max wait time will be 5 * 24 = 120 seconds
            while max_retries > 0:
                volumes = Volume.list(
                    api_client,
                    id=volume.id
                )
                if volumes is None or len(volumes) == 0:
                    break
                max_retries = max_retries - 1
                time.sleep(5)

    def check_wget_from_vm(self, vm, public_ip, network=None, testnegative=False, isVmAccessible=True):
        import urllib.request, urllib.error
        self.debug(f"Checking if we can wget from a VM={vm.name} http server on public_ip={public_ip.ipaddress.ipaddress}, expecting failure == {testnegative} and vm is acceccible == {isVmAccessible}")
        try:
            if not isVmAccessible:
                self.create_natrule_for_services(vm, public_ip, network)
            self.setup_webserver(vm, public_ip)

            urllib.request.urlretrieve(f"http://{public_ip.ipaddress.ipaddress}/test.html", filename="test.html")
            if not testnegative:
                self.debug("Successesfull to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
            else:
                self.fail("Successesfull to wget from VM=%s http server on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        except Exception as e:
            if not testnegative:
                self.fail("Failed to wget from VM=%s http server on public_ip=%s: %s" % (vm.name, public_ip.ipaddress.ipaddress, e))
            else:
                self.debug("Failed to wget from VM=%s http server on public_ip=%s: %s" % (vm.name, public_ip.ipaddress.ipaddress, e))

    def setup_webserver(self, vm, public_ip=None):
        # Start httpd service on VM first
        if public_ip == None:
            sshClient = vm.get_ssh_client()
        else:
            sshClient = vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
        # Test to see if we are on a tiny linux box (using busybox)
        res = "apache"
        try:
            res = str(sshClient.execute("busybox")).lower()
        except Exception as e:
            self.debug("no busybox {e}")
        if "httpd" in res:
            self.debug(f"using busybox httpd on VM {vm.name}/{vm.id}")
            self.setup_busybox(sshClient)
        else:
            self.debug(f"using apache httpd on VM {vm.name}/{vm.id}")
            self.setup_apache(sshClient)

    def setup_busybox(self, sshClient):
        """ Create a dummy test.html file and fire up the busybox web server """
        sshClient.execute('echo test > test.html')
        sshClient.execute("/usr/sbin/httpd")
        self.debug("Setup webserver using busybox")

    def setup_apache(self, sshClient):
        sshClient.execute("iptables -F")
        sshClient.execute('echo test > /var/www/html/test.html')
        sshClient.execute("service httpd start")
        time.sleep(5)
        ssh_response = str(sshClient.execute("service httpd status")).lower()
        self.debug("httpd service status is: %s" % ssh_response)
        if "httpd: unrecognized service" in ssh_response or "inactive" in ssh_response:
            ssh_res = sshClient.execute("yum install httpd -y")
            if "Complete!" not in ssh_res:
                raise Exception("Failed to install http server")
            sshClient.execute("service httpd start")
            time.sleep(5)
            ssh_response = str(sshClient.execute("service httpd status")).lower()
        if "running" not in ssh_response:
            raise Exception("Failed to start httpd service")
        self.debug("Setup webserver using apache")

    def check_ssh_into_vm(self, vm, public_ip, testnegative=False):
        self.debug(f"Checking if we can SSH into VM={vm.name} on public_ip={public_ip.ipaddress.ipaddress}")
        try:
            vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
            if not testnegative:
                self.debug(f"SSH into VM={vm.name} on public_ip={public_ip.ipaddress.ipaddress} was successful")
            else:
                self.fail(f"SSH into VM={vm.name} on public_ip={public_ip.ipaddress.ipaddress} succeeded, but should have been rejected")
        except:
            if not testnegative:
                self.fail(f"Failed to SSH into VM {vm.name} with ip {public_ip.ipaddress.ipaddress}")
            else:
                self.debug(f"Failed to SSH into VM {vm.name} with {public_ip.ipaddress.ipaddress}, as expected")

    def create_natrule_for_services(self, vm, public_ip, network, services=None):
        self.debug(f"Creating NAT rule in network for vm {vm.name} with public IP {public_ip.ipaddress.ipaddress}")
        if not services:
            services = self.services["natrule"]
        nat_rule = NATRule.create(self.apiclient,
                                  vm,
                                  services,
                                  ipaddressid=public_ip.ipaddress.id,
                                  openfirewall=False,
                                  networkid=network.id,
                                  vpcid=self.vpc.id
                                  )
        self.cleanup.append(nat_rule)
        self.debug("Adding NetworkACL rules to make NAT rule accessible")
        nwacl_nat = NetworkACL.create(self.apiclient,
                                      networkid=network.id,
                                      services=services,
                                      traffictype='Ingress'
                                      )
        self.cleanup.append(nwacl_nat)
        self.debug(f'nwacl_nat={nwacl_nat.__dict__}')
        return nat_rule

    def create_network(self, net_offerring, gateway='10.1.1.1', vpc=None):
        try:
            self.debug('Create NetworkOffering')
            net_offerring["name"] = "NET_OFF-" + str(gateway)
            nw_off = NetworkOffering.create(self.apiclient,
                                            net_offerring,
                                            conservemode=False
                                            )
            self.cleanup.append(nw_off)
            # Enable Network offering
            nw_off.update(self.apiclient, state='Enabled')
            self.debug('Created and Enabled NetworkOffering')

            self.services["network"]["name"] = "NETWORK-" + str(gateway)
            self.debug('Adding Network=%s' % self.services["network"])
            obj_network = Network.create(self.apiclient,
                                         self.services["network"],
                                         accountid=self.account.name,
                                         domainid=self.account.domainid,
                                         networkofferingid=nw_off.id,
                                         zoneid=self.zone.id,
                                         gateway=gateway,
                                         vpcid=vpc.id if vpc else self.vpc.id
                                         )
            self.cleanup.append(obj_network)
            self.debug("Created network with ID: %s" % obj_network.id)
            return obj_network
        except Exception as e:
            self.fail('Unable to create a Network with offering=%s because of %s ' % (net_offerring, e))
