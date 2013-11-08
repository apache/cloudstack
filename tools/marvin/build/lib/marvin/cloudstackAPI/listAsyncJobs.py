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


"""Lists all pending asynchronous jobs for the account."""
from baseCmd import *
from baseResponse import *
class listAsyncJobsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """the start date of the async job"""
        self.startdate = None
        self.required = []

class listAsyncJobsResponse (baseResponse):
    def __init__(self):
        """the account that executed the async command"""
        self.accountid = None
        """the async command executed"""
        self.cmd = None
        """the created date of the job"""
        self.created = None
        """the unique ID of the instance/entity object related to the job"""
        self.jobinstanceid = None
        """the instance/entity object related to the job"""
        self.jobinstancetype = None
        """the progress information of the PENDING job"""
        self.jobprocstatus = None
        """the result reason"""
        self.jobresult = None
        """the result code for the job"""
        self.jobresultcode = None
        """the result type"""
        self.jobresulttype = None
        """the current job status-should be 0 for PENDING"""
        self.jobstatus = None
        """the user that executed the async command"""
        self.userid = None
        """the ID of the async job"""
        self.jobid = None

