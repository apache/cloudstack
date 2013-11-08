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


"""Lists all supported OS types for this cloud."""
from baseCmd import *
from baseResponse import *
class listOsTypesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list os by description"""
        self.description = None
        """list by Os type Id"""
        self.id = None
        """List by keyword"""
        self.keyword = None
        """list by Os Category id"""
        self.oscategoryid = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = []

class listOsTypesResponse (baseResponse):
    def __init__(self):
        """the ID of the OS type"""
        self.id = None
        """the name/description of the OS type"""
        self.description = None
        """the ID of the OS category"""
        self.oscategoryid = None

