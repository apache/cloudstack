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


"""Lists all available network offerings."""
from baseCmd import *
from baseResponse import *
class listNetworkOfferingsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the availability of network offering. Default value is Required"""
        self.availability = None
        """list network offerings by display text"""
        self.displaytext = None
        """the network offering can be used only for network creation inside the VPC"""
        self.forvpc = None
        """list network offerings by guest type: Shared or Isolated"""
        self.guestiptype = None
        """list network offerings by id"""
        self.id = None
        """true if need to list only default network offerings. Default value is false"""
        self.isdefault = None
        """true if offering has tags specified"""
        self.istagged = None
        """List by keyword"""
        self.keyword = None
        """list network offerings by name"""
        self.name = None
        """the ID of the network. Pass this in if you want to see the available network offering that a network can be changed to."""
        self.networkid = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """true if need to list only netwok offerings where source nat is supported, false otherwise"""
        self.sourcenatsupported = None
        """true if need to list only network offerings which support specifying ip ranges"""
        self.specifyipranges = None
        """the tags for the network offering."""
        self.specifyvlan = None
        """list network offerings by state"""
        self.state = None
        """list network offerings supporting certain services"""
        self.supportedservices = []
        """list network offerings by tags"""
        self.tags = None
        """list by traffic type"""
        self.traffictype = None
        """list netowrk offerings available for network creation in specific zone"""
        self.zoneid = None
        self.required = []

class listNetworkOfferingsResponse (baseResponse):
    def __init__(self):
        """the id of the network offering"""
        self.id = None
        """availability of the network offering"""
        self.availability = None
        """true if network offering is ip conserve mode enabled"""
        self.conservemode = None
        """the date this network offering was created"""
        self.created = None
        """additional key/value details tied with network offering"""
        self.details = None
        """an alternate display text of the network offering."""
        self.displaytext = None
        """true if network offering supports persistent networks, false otherwise"""
        self.egressdefaultpolicy = None
        """true if network offering can be used by VPC networks only"""
        self.forvpc = None
        """guest type of the network offering, can be Shared or Isolated"""
        self.guestiptype = None
        """true if network offering is default, false otherwise"""
        self.isdefault = None
        """true if network offering supports persistent networks, false otherwise"""
        self.ispersistent = None
        """maximum number of concurrents connections to be handled by lb"""
        self.maxconnections = None
        """the name of the network offering"""
        self.name = None
        """data transfer rate in megabits per second allowed."""
        self.networkrate = None
        """the ID of the service offering used by virtual router provider"""
        self.serviceofferingid = None
        """true if network offering supports specifying ip ranges, false otherwise"""
        self.specifyipranges = None
        """true if network offering supports vlans, false otherwise"""
        self.specifyvlan = None
        """state of the network offering. Can be Disabled/Enabled/Inactive"""
        self.state = None
        """the tags for the network offering"""
        self.tags = None
        """the traffic type for the network offering, supported types are Public, Management, Control, Guest, Vlan or Storage."""
        self.traffictype = None
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

