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


"""Lists storage providers."""
from baseCmd import *
from baseResponse import *
class listStorageProvidersCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the type of storage provider: either primary or image"""
        """Required"""
        self.type = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = ["type",]

class listStorageProvidersResponse (baseResponse):
    def __init__(self):
        """the name of the storage provider"""
        self.name = None
        """the type of the storage provider: primary or image provider"""
        self.type = None

