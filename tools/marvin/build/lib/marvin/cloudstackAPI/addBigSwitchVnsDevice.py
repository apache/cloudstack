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


"""Adds a BigSwitch VNS device"""
from baseCmd import *
from baseResponse import *
class addBigSwitchVnsDeviceCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """Hostname of ip address of the BigSwitch VNS Controller."""
        """Required"""
        self.hostname = None
        """the Physical Network ID"""
        """Required"""
        self.physicalnetworkid = None
        self.required = ["hostname","physicalnetworkid",]

class addBigSwitchVnsDeviceResponse (baseResponse):
    def __init__(self):
        """device name"""
        self.bigswitchdevicename = None
        """the controller Ip address"""
        self.hostname = None
        """the physical network to which this BigSwitch Vns belongs to"""
        self.physicalnetworkid = None
        """name of the provider"""
        self.provider = None
        """device id of the BigSwitch Vns"""
        self.vnsdeviceid = None

