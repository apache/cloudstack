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


"""Configures an Internal Load Balancer element."""
from baseCmd import *
from baseResponse import *
class configureInternalLoadBalancerElementCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the internal lb provider"""
        """Required"""
        self.id = None
        """Enables/Disables the Internal Load Balancer element"""
        """Required"""
        self.enabled = None
        self.required = ["id","enabled",]

class configureInternalLoadBalancerElementResponse (baseResponse):
    def __init__(self):
        """the id of the internal load balancer element"""
        self.id = None
        """Enabled/Disabled the element"""
        self.enabled = None
        """the physical network service provider id of the element"""
        self.nspid = None

