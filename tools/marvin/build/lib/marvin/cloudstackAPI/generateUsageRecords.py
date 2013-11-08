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


"""Generates usage records. This will generate records only if there any records to be generated, i.e if the scheduled usage job was not run or failed"""
from baseCmd import *
from baseResponse import *
class generateUsageRecordsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """End date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03."""
        """Required"""
        self.enddate = None
        """Start date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-01."""
        """Required"""
        self.startdate = None
        """List events for the specified domain."""
        self.domainid = None
        self.required = ["enddate","startdate",]

class generateUsageRecordsResponse (baseResponse):
    def __init__(self):
        """any text associated with the success or failure"""
        self.displaytext = None
        """true if operation is executed successfully"""
        self.success = None

