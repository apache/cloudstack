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


"""Adds a Nicira NVP device"""
from baseCmd import *
from baseResponse import *
class addNiciraNvpDeviceCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """Hostname of ip address of the Nicira NVP Controller."""
        """Required"""
        self.hostname = None
        """Credentials to access the Nicira Controller API"""
        """Required"""
        self.password = None
        """the Physical Network ID"""
        """Required"""
        self.physicalnetworkid = None
        """The Transportzone UUID configured on the Nicira Controller"""
        """Required"""
        self.transportzoneuuid = None
        """Credentials to access the Nicira Controller API"""
        """Required"""
        self.username = None
        """The L3 Gateway Service UUID configured on the Nicira Controller"""
        self.l3gatewayserviceuuid = None
        self.required = ["hostname","password","physicalnetworkid","transportzoneuuid","username",]

class addNiciraNvpDeviceResponse (baseResponse):
    def __init__(self):
        """the controller Ip address"""
        self.hostname = None
        """this L3 gateway service Uuid"""
        self.l3gatewayserviceuuid = None
        """device name"""
        self.niciradevicename = None
        """device id of the Nicire Nvp"""
        self.nvpdeviceid = None
        """the physical network to which this Nirica Nvp belongs to"""
        self.physicalnetworkid = None
        """name of the provider"""
        self.provider = None
        """the transport zone Uuid"""
        self.transportzoneuuid = None

