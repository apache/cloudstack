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


"""Updates traffic type of a physical network"""
from baseCmd import *
from baseResponse import *
class updateTrafficTypeCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """traffic type id"""
        """Required"""
        self.id = None
        """The network name label of the physical device dedicated to this traffic on a KVM host"""
        self.kvmnetworklabel = None
        """The network name label of the physical device dedicated to this traffic on a VMware host"""
        self.vmwarenetworklabel = None
        """The network name label of the physical device dedicated to this traffic on a XenServer host"""
        self.xennetworklabel = None
        self.required = ["id",]

class updateTrafficTypeResponse (baseResponse):
    def __init__(self):
        """id of the network provider"""
        self.id = None
        """The network name label of the physical device dedicated to this traffic on a KVM host"""
        self.kvmnetworklabel = None
        """the physical network this belongs to"""
        self.physicalnetworkid = None
        """the trafficType to be added to the physical network"""
        self.traffictype = None
        """The network name label of the physical device dedicated to this traffic on a VMware host"""
        self.vmwarenetworklabel = None
        """The network name label of the physical device dedicated to this traffic on a XenServer host"""
        self.xennetworklabel = None

