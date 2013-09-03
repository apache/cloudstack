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
from marvin.factory.networkoffering import NetworkOfferingFactory
from marvin.utils import random_gen


class DefaultIsolatedNetworkOfferingWithSourceNatServiceFactory(NetworkOfferingFactory):
    #FIXME: Service Capability Lists with CapabilityTypes (ElasticIP, RvR etc) needs handling

    displaytext = factory.Sequence(lambda n : "DefaultIsolatedNetworkOfferingWithSourceNatService" + random_gen())
    name = factory.Sequence(lambda n : "DefaultIsolatedNetworkOfferingWithSourceNatService" + random_gen())
    supportedservices = "Lb,Dns,PortForwarding,StaticNat,Dhcp,Firewall,Vpn,UserData,SourceNat"
    traffictype = "GUEST"
    availability = "Optional"
    guestiptype = "Isolated"

    specifyVlan = False
    specifyIpRanges = False
    isPersistent = False
    conserveMode = True

    serviceProviderList = []
    for service in map(lambda l: l.strip(' '), supportedservices.split(',')):
        serviceProviderList.append(
            {
                'service': service,
                'provider': 'VirtualRouter'
            }
        )


class DefaultSharedNetworkOfferingWithSGServiceFactory(NetworkOfferingFactory):

    displaytext = factory.Sequence(lambda n : "DefaultSharedNetworkOfferingWithSGService" + random_gen())
    name = factory.Sequence(lambda n : "DefaultSharedNetworkOfferingWithSGService" + random_gen())
    availability = "Optional"
    supportedservices = "SecurityGroup,Dns,Dhcp,UserData"
    guestiptype = "Shared"
    traffictype = "GUEST"

    specifyVlan = True
    specifyIpRanges = True
    isPersistent = False
    conserveMode = True

    serviceProviderList = []
    for service in map(lambda l: l.strip(' '), supportedservices.split(',')):
        if service == 'SecurityGroup':
            provider = 'SecurityGroupProvider'
        else:
            provider = 'VirtualRouter'
        serviceProviderList.append(
            {
                'service': service,
                'provider': provider
            }
        )
