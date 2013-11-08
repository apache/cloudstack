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


"""Stops a system VM."""
from baseCmd import *
from baseResponse import *
class stopSystemVmCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """The ID of the system virtual machine"""
        """Required"""
        self.id = None
        """Force stop the VM.  The caller knows the VM is stopped."""
        self.forced = None
        self.required = ["id",]

class stopSystemVmResponse (baseResponse):
    def __init__(self):
        """the ID of the system VM"""
        self.id = None
        """the number of active console sessions for the console proxy system vm"""
        self.activeviewersessions = None
        """the date and time the system VM was created"""
        self.created = None
        """the first DNS for the system VM"""
        self.dns1 = None
        """the second DNS for the system VM"""
        self.dns2 = None
        """the gateway for the system VM"""
        self.gateway = None
        """the host ID for the system VM"""
        self.hostid = None
        """the hostname for the system VM"""
        self.hostname = None
        """the job ID associated with the system VM. This is only displayed if the router listed is part of a currently running asynchronous job."""
        self.jobid = None
        """the job status associated with the system VM.  This is only displayed if the router listed is part of a currently running asynchronous job."""
        self.jobstatus = None
        """the link local IP address for the system vm"""
        self.linklocalip = None
        """the link local MAC address for the system vm"""
        self.linklocalmacaddress = None
        """the link local netmask for the system vm"""
        self.linklocalnetmask = None
        """the name of the system VM"""
        self.name = None
        """the network domain for the system VM"""
        self.networkdomain = None
        """the Pod ID for the system VM"""
        self.podid = None
        """the private IP address for the system VM"""
        self.privateip = None
        """the private MAC address for the system VM"""
        self.privatemacaddress = None
        """the private netmask for the system VM"""
        self.privatenetmask = None
        """the public IP address for the system VM"""
        self.publicip = None
        """the public MAC address for the system VM"""
        self.publicmacaddress = None
        """the public netmask for the system VM"""
        self.publicnetmask = None
        """the state of the system VM"""
        self.state = None
        """the system VM type"""
        self.systemvmtype = None
        """the template ID for the system VM"""
        self.templateid = None
        """the Zone ID for the system VM"""
        self.zoneid = None
        """the Zone name for the system VM"""
        self.zonename = None

