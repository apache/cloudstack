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


"""Marks a default zone for this account"""
from baseCmd import *
from baseResponse import *
class markDefaultZoneForAccountCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """Name of the account that is to be marked."""
        """Required"""
        self.account = None
        """Marks the account that belongs to the specified domain."""
        """Required"""
        self.domainid = None
        """The Zone ID with which the account is to be marked."""
        """Required"""
        self.zoneid = None
        self.required = ["account","domainid","zoneid",]

class markDefaultZoneForAccountResponse (baseResponse):
    def __init__(self):
        """the id of the account"""
        self.id = None
        """details for the account"""
        self.accountdetails = None
        """account type (admin, domain-admin, user)"""
        self.accounttype = None
        """the total number of cpu cores available to be created for this account"""
        self.cpuavailable = None
        """the total number of cpu cores the account can own"""
        self.cpulimit = None
        """the total number of cpu cores owned by account"""
        self.cputotal = None
        """the default zone of the account"""
        self.defaultzoneid = None
        """name of the Domain the account belongs too"""
        self.domain = None
        """id of the Domain the account belongs too"""
        self.domainid = None
        """the total number of public ip addresses available for this account to acquire"""
        self.ipavailable = None
        """the total number of public ip addresses this account can acquire"""
        self.iplimit = None
        """the total number of public ip addresses allocated for this account"""
        self.iptotal = None
        """true if the account requires cleanup"""
        self.iscleanuprequired = None
        """true if account is default, false otherwise"""
        self.isdefault = None
        """the total memory (in MB) available to be created for this account"""
        self.memoryavailable = None
        """the total memory (in MB) the account can own"""
        self.memorylimit = None
        """the total memory (in MB) owned by account"""
        self.memorytotal = None
        """the name of the account"""
        self.name = None
        """the total number of networks available to be created for this account"""
        self.networkavailable = None
        """the network domain"""
        self.networkdomain = None
        """the total number of networks the account can own"""
        self.networklimit = None
        """the total number of networks owned by account"""
        self.networktotal = None
        """the total primary storage space (in GiB) available to be used for this account"""
        self.primarystorageavailable = None
        """the total primary storage space (in GiB) the account can own"""
        self.primarystoragelimit = None
        """the total primary storage space (in GiB) owned by account"""
        self.primarystoragetotal = None
        """the total number of projects available for administration by this account"""
        self.projectavailable = None
        """the total number of projects the account can own"""
        self.projectlimit = None
        """the total number of projects being administrated by this account"""
        self.projecttotal = None
        """the total number of network traffic bytes received"""
        self.receivedbytes = None
        """the total secondary storage space (in GiB) available to be used for this account"""
        self.secondarystorageavailable = None
        """the total secondary storage space (in GiB) the account can own"""
        self.secondarystoragelimit = None
        """the total secondary storage space (in GiB) owned by account"""
        self.secondarystoragetotal = None
        """the total number of network traffic bytes sent"""
        self.sentbytes = None
        """the total number of snapshots available for this account"""
        self.snapshotavailable = None
        """the total number of snapshots which can be stored by this account"""
        self.snapshotlimit = None
        """the total number of snapshots stored by this account"""
        self.snapshottotal = None
        """the state of the account"""
        self.state = None
        """the total number of templates available to be created by this account"""
        self.templateavailable = None
        """the total number of templates which can be created by this account"""
        self.templatelimit = None
        """the total number of templates which have been created by this account"""
        self.templatetotal = None
        """the total number of virtual machines available for this account to acquire"""
        self.vmavailable = None
        """the total number of virtual machines that can be deployed by this account"""
        self.vmlimit = None
        """the total number of virtual machines running for this account"""
        self.vmrunning = None
        """the total number of virtual machines stopped for this account"""
        self.vmstopped = None
        """the total number of virtual machines deployed by this account"""
        self.vmtotal = None
        """the total volume available for this account"""
        self.volumeavailable = None
        """the total volume which can be used by this account"""
        self.volumelimit = None
        """the total volume being used by this account"""
        self.volumetotal = None
        """the total number of vpcs available to be created for this account"""
        self.vpcavailable = None
        """the total number of vpcs the account can own"""
        self.vpclimit = None
        """the total number of vpcs owned by account"""
        self.vpctotal = None
        """the list of users associated with account"""
        self.user = []

class user:
    def __init__(self):
        """"the user ID"""
        self.id = None
        """"the account name of the user"""
        self.account = None
        """"the account ID of the user"""
        self.accountid = None
        """"the account type of the user"""
        self.accounttype = None
        """"the api key of the user"""
        self.apikey = None
        """"the date and time the user account was created"""
        self.created = None
        """"the domain name of the user"""
        self.domain = None
        """"the domain ID of the user"""
        self.domainid = None
        """"the user email address"""
        self.email = None
        """"the user firstname"""
        self.firstname = None
        """"the boolean value representing if the updating target is in caller's child domain"""
        self.iscallerchilddomain = None
        """"true if user is default, false otherwise"""
        self.isdefault = None
        """"the user lastname"""
        self.lastname = None
        """"the secret key of the user"""
        self.secretkey = None
        """"the user state"""
        self.state = None
        """"the timezone user was created in"""
        self.timezone = None
        """"the user name"""
        self.username = None

