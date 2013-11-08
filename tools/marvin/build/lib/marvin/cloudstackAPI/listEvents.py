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


"""A command to list events."""
from baseCmd import *
from baseResponse import *
class listEventsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """the duration of the event"""
        self.duration = None
        """the end date range of the list you want to retrieve (use format "yyyy-MM-dd" or the new format "yyyy-MM-dd HH:mm:ss")"""
        self.enddate = None
        """the time the event was entered"""
        self.entrytime = None
        """the ID of the event"""
        self.id = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """the event level (INFO, WARN, ERROR)"""
        self.level = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list objects by project"""
        self.projectid = None
        """the start date range of the list you want to retrieve (use format "yyyy-MM-dd" or the new format "yyyy-MM-dd HH:mm:ss")"""
        self.startdate = None
        """the event type (see event types)"""
        self.type = None
        self.required = []

class listEventsResponse (baseResponse):
    def __init__(self):
        """the ID of the event"""
        self.id = None
        """the account name for the account that owns the object being acted on in the event (e.g. the owner of the virtual machine, ip address, or security group)"""
        self.account = None
        """the date the event was created"""
        self.created = None
        """a brief description of the event"""
        self.description = None
        """the name of the account's domain"""
        self.domain = None
        """the id of the account's domain"""
        self.domainid = None
        """the event level (INFO, WARN, ERROR)"""
        self.level = None
        """whether the event is parented"""
        self.parentid = None
        """the project name of the address"""
        self.project = None
        """the project id of the ipaddress"""
        self.projectid = None
        """the state of the event"""
        self.state = None
        """the type of the event (see event types)"""
        self.type = None
        """the name of the user who performed the action (can be different from the account if an admin is performing an action for a user, e.g. starting/stopping a user's virtual machine)"""
        self.username = None

