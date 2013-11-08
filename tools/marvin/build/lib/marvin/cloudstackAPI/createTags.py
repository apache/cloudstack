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


"""Creates resource tag(s)"""
from baseCmd import *
from baseResponse import *
class createTagsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """list of resources to create the tags for"""
        """Required"""
        self.resourceids = []
        """type of the resource"""
        """Required"""
        self.resourcetype = None
        """Map of tags (key/value pairs)"""
        """Required"""
        self.tags = []
        """identifies client specific tag. When the value is not null, the tag can't be used by cloudStack code internally"""
        self.customer = None
        self.required = ["resourceids","resourcetype","tags",]

class createTagsResponse (baseResponse):
    def __init__(self):
        """any text associated with the success or failure"""
        self.displaytext = None
        """true if operation is executed successfully"""
        self.success = None

