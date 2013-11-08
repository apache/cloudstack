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


"""Lists usage records for accounts"""
from baseCmd import *
from baseResponse import *
class listUsageRecordsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """End date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03."""
        """Required"""
        self.enddate = None
        """Start date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-01."""
        """Required"""
        self.startdate = None
        """List usage records for the specified user."""
        self.account = None
        """List usage records for the specified account"""
        self.accountid = None
        """List usage records for the specified domain."""
        self.domainid = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """List usage records for specified project"""
        self.projectid = None
        """List usage records for the specified usage type"""
        self.type = None
        self.required = ["enddate","startdate",]

class listUsageRecordsResponse (baseResponse):
    def __init__(self):
        """the user account name"""
        self.account = None
        """the user account Id"""
        self.accountid = None
        """description of the usage record"""
        self.description = None
        """the domain the resource is associated with"""
        self.domain = None
        """the domain ID"""
        self.domainid = None
        """end date of the usage record"""
        self.enddate = None
        """True if the resource is default"""
        self.isdefault = None
        """True if the IPAddress is source NAT"""
        self.issourcenat = None
        """True if the IPAddress is system IP - allocated during vm deploy or lb rule create"""
        self.issystem = None
        """virtual machine name"""
        self.name = None
        """id of the network"""
        self.networkid = None
        """offering ID"""
        self.offeringid = None
        """the project name of the resource"""
        self.project = None
        """the project id of the resource"""
        self.projectid = None
        """raw usage in hours"""
        self.rawusage = None
        """resource size"""
        self.size = None
        """start date of the usage record"""
        self.startdate = None
        """template ID"""
        self.templateid = None
        """resource type"""
        self.type = None
        """usage in hours"""
        self.usage = None
        """id of the resource"""
        self.usageid = None
        """usage type ID"""
        self.usagetype = None
        """virtual machine ID"""
        self.virtualmachineid = None
        """virtual size of resource"""
        self.virtualsize = None
        """the zone ID"""
        self.zoneid = None

