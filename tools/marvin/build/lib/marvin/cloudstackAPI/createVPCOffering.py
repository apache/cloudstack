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


"""Creates VPC offering"""
from baseCmd import *
from baseResponse import *
class createVPCOfferingCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the display text of the vpc offering"""
        """Required"""
        self.displaytext = None
        """the name of the vpc offering"""
        """Required"""
        self.name = None
        """services supported by the vpc offering"""
        """Required"""
        self.supportedservices = []
        """provider to service mapping. If not specified, the provider for the service will be mapped to the default provider on the physical network"""
        self.serviceproviderlist = []
        self.required = ["displaytext","name","supportedservices",]

class createVPCOfferingResponse (baseResponse):
    def __init__(self):
        """the id of the vpc offering"""
        self.id = None
        """the date this vpc offering was created"""
        self.created = None
        """an alternate display text of the vpc offering."""
        self.displaytext = None
        """true if vpc offering is default, false otherwise"""
        self.isdefault = None
        """the name of the vpc offering"""
        self.name = None
        """state of the vpc offering. Can be Disabled/Enabled"""
        self.state = None
        """the list of supported services"""
        self.service = []

class capability:
    def __init__(self):
        """"can this service capability value can be choosable while creatine network offerings"""
        self.canchooseservicecapability = None
        """"the capability name"""
        self.name = None
        """"the capability value"""
        self.value = None

class provider:
    def __init__(self):
        """"uuid of the network provider"""
        self.id = None
        """"true if individual services can be enabled/disabled"""
        self.canenableindividualservice = None
        """"the destination physical network"""
        self.destinationphysicalnetworkid = None
        """"the provider name"""
        self.name = None
        """"the physical network this belongs to"""
        self.physicalnetworkid = None
        """"services for this provider"""
        self.servicelist = None
        """"state of the network provider"""
        self.state = None

class service:
    def __init__(self):
        """"the service name"""
        self.name = None
        """"the list of capabilities"""
        self.capability = []
        """"can this service capability value can be choosable while creatine network offerings"""
        self.canchooseservicecapability = None
        """"the capability name"""
        self.name = None
        """"the capability value"""
        self.value = None
        """"the service provider name"""
        self.provider = []
        """"uuid of the network provider"""
        self.id = None
        """"true if individual services can be enabled/disabled"""
        self.canenableindividualservice = None
        """"the destination physical network"""
        self.destinationphysicalnetworkid = None
        """"the provider name"""
        self.name = None
        """"the physical network this belongs to"""
        self.physicalnetworkid = None
        """"services for this provider"""
        self.servicelist = None
        """"state of the network provider"""
        self.state = None

