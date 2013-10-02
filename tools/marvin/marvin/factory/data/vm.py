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

import factory
from marvin.factory.virtualmachine import VirtualMachineFactory
from marvin.entity.ipaddress import IpAddress
from marvin.entity.network import Network
from marvin.factory.data.firewallrule import SshFirewallRule
from marvin.factory.data.vpc import DefaultVpc
from marvin.factory.data.network import DefaultVpcNetwork

class VirtualMachineWithStaticNat(VirtualMachineFactory):
    """VirtualMachine in an isolated network of an advanced zone

    Open a static-nat rule to connect to the guest over port 22
    """

    @factory.post_generation
    def staticNat(self, create, extracted, **kwargs):
        if not create:
            return
        ipassoc = IpAddress(
            apiclient=self.apiclient,
            account=self.account,
            domainid=self.domainid,
            zoneid=self.zoneid,
        )
        self.enableStaticNat(
            apiclient=self.apiclient,
            ipaddressid=ipassoc.id,
            virtualmachineid=self.id
        )
        self.ssh_ip = ipassoc.ipaddress
        self.public_ip = ipassoc.ipaddress


class VirtualMachineWithIngress(VirtualMachineFactory):
    """VirtualMachine created in a basic zone with security groups

    Allow port 22 (ingress) into the guest
    """
    @factory.post_generation
    def allowIngress(self, create, extracted, **kwargs):
        if not create:
            return


class VpcVirtualMachine(VirtualMachineFactory):
    """
    VirtualMachine within a VPC created by DefaultVPC offering
    """

    vpc = factory.SubFactory(
        DefaultVpc,
        apiclient=factory.SelfAttribute('..apiclient'),
        account=factory.SelfAttribute('..account'),
        domainid=factory.SelfAttribute('..domainid'),
        zoneid=factory.SelfAttribute('..zoneid')
    )
    ntwk = factory.SubFactory(
        DefaultVpcNetwork,
        apiclient=factory.SelfAttribute('..apiclient'),
        account=factory.SelfAttribute('..account'),
        domainid=factory.SelfAttribute('..domainid'),
        zoneid=factory.SelfAttribute('..zoneid'),
        vpcid=factory.SelfAttribute('..vpc.id')
    )
    networkid=factory.LazyAttribute(lambda n: n.ntwk.id if n else None)