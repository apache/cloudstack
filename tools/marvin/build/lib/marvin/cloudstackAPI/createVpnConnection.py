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


"""Create site to site vpn connection"""
from baseCmd import *
from baseResponse import *
class createVpnConnectionCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """id of the customer gateway"""
        """Required"""
        self.s2scustomergatewayid = None
        """id of the vpn gateway"""
        """Required"""
        self.s2svpngatewayid = None
        self.required = ["s2scustomergatewayid","s2svpngatewayid",]

class createVpnConnectionResponse (baseResponse):
    def __init__(self):
        """the vpn gateway ID"""
        self.id = None
        """the owner"""
        self.account = None
        """guest cidr list of the customer gateway"""
        self.cidrlist = None
        """the date and time the host was created"""
        self.created = None
        """the domain name of the owner"""
        self.domain = None
        """the domain id of the owner"""
        self.domainid = None
        """if DPD is enabled for customer gateway"""
        self.dpd = None
        """Lifetime of ESP SA of customer gateway"""
        self.esplifetime = None
        """ESP policy of the customer gateway"""
        self.esppolicy = None
        """public ip address id of the customer gateway"""
        self.gateway = None
        """Lifetime of IKE SA of customer gateway"""
        self.ikelifetime = None
        """IKE policy of the customer gateway"""
        self.ikepolicy = None
        """IPsec Preshared-Key of the customer gateway"""
        self.ipsecpsk = None
        """the project name"""
        self.project = None
        """the project id"""
        self.projectid = None
        """the public IP address"""
        self.publicip = None
        """the date and time the host was removed"""
        self.removed = None
        """the customer gateway ID"""
        self.s2scustomergatewayid = None
        """the vpn gateway ID"""
        self.s2svpngatewayid = None
        """State of vpn connection"""
        self.state = None

