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
from marvin.legacy.utils import random_gen
from marvin.entity.securitygroup import SecurityGroup
from marvin.factory.securitygroup import SecurityGroupFactory

class SecurityGroupSshIngress(SecurityGroupFactory):
    """
    Allow port 22 (ingress) into the guest
    """
    name = factory.Sequence(lambda n: 'SshSecurityGroupIngress-' + random_gen())
    protocol = 'tcp'
    cidrlist = '0.0.0.0/0'
    startport = 22
    endport = 22

    @factory.post_generation
    def authorizeIngress(self, create, extracted, **kwargs):
        if not create:
            return
        sg = SecurityGroup.list(name=self.name)
        if not sg:
            self.authorizeSecurityGroupIngress(
                self.apiclient,
                name=self.name,
                protocol=self.protocol,
                cidrlist=self.cidrlist,
                startport=self.startport,
                endport=self.endport
            )


class SecurityGroupSshEgress(SecurityGroupFactory):
    """
    Allow port 22 (egress) out of the guest
    """
    name = factory.Sequence(lambda n: 'SshSecurityGroupEgress-' + random_gen())
    protocol = 'tcp'
    cidrlist = '0.0.0.0/0'
    startport = 22
    endport = 22

    @factory.post_generation
    def authorizeEgress(self, create, extracted, **kwargs):
        if not create:
            return
        sg = SecurityGroup.list(name=self.name)
        if not sg:
            self.authorizeSecurityGroupEgress(
                self.apiclient,
                name=self.name,
                protocol=self.protocol,
                cidrlist=self.cidrlist,
                startport=self.startport,
                endport=self.endport
            )
