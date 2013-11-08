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


"""adds a baremetal dhcp server"""
from baseCmd import *
from baseResponse import *
class addBaremetalDhcpCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """Type of dhcp device"""
        """Required"""
        self.dhcpservertype = None
        """Credentials to reach external dhcp device"""
        """Required"""
        self.password = None
        """the Physical Network ID"""
        """Required"""
        self.physicalnetworkid = None
        """URL of the external dhcp appliance."""
        """Required"""
        self.url = None
        """Credentials to reach external dhcp device"""
        """Required"""
        self.username = None
        self.required = ["dhcpservertype","password","physicalnetworkid","url","username",]

class addBaremetalDhcpResponse (baseResponse):
    def __init__(self):
        """device id of"""
        self.id = None
        """name of the provider"""
        self.dhcpservertype = None
        """the physical network to which this external dhcp device belongs to"""
        self.physicalnetworkid = None
        """name of the provider"""
        self.provider = None
        """url"""
        self.url = None

