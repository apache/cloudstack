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
from marvin.factory.vpcoffering import VpcOfferingFactory
from marvin.legacy.utils import random_gen

class DefaultVpcOffering(VpcOfferingFactory):

    apiclient = None
    displaytext = factory.Sequence(lambda n: 'Default VPC offering' + random_gen())
    name = factory.Sequence(lambda n: 'Default VPC offering' + random_gen())
    supportedservices = 'SourceNat,Dns,Lb,PortForwarding,StaticNat,NetworkACL,Dhcp,Vpn,UserData'

    serviceProviderList = []
    for service in map(lambda l: l.strip(' '), supportedservices.split(',')):
        serviceProviderList.append(
            {
                'service': service,
                'provider': 'VpcVirtualRouter'
            }
        )

    # enable the offering post generation
    @factory.post_generation
    def enable(self, create, extracted, **kwargs):
        if not create:
            return
        self.update(id=self.id, state='Enabled')
