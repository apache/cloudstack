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


"""Registers an existing template into the CloudStack cloud."""
from baseCmd import *
from baseResponse import *
class registerTemplateCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the display text of the template. This is usually used for display purposes."""
        """Required"""
        self.displaytext = None
        """the format for the template. Possible values include QCOW2, RAW, and VHD."""
        """Required"""
        self.format = None
        """the target hypervisor for the template"""
        """Required"""
        self.hypervisor = None
        """the name of the template"""
        """Required"""
        self.name = None
        """the ID of the OS Type that best represents the OS of this template."""
        """Required"""
        self.ostypeid = None
        """the URL of where the template is hosted. Possible URL include http:// and https://"""
        """Required"""
        self.url = None
        """the ID of the zone the template is to be hosted on"""
        """Required"""
        self.zoneid = None
        """an optional accountName. Must be used with domainId."""
        self.account = None
        """32 or 64 bits support. 64 by default"""
        self.bits = None
        """the MD5 checksum value of this template"""
        self.checksum = None
        """Template details in key/value pairs."""
        self.details = []
        """an optional domainId. If the account parameter is used, domainId must also be used."""
        self.domainid = None
        """true if template contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory"""
        self.isdynamicallyscalable = None
        """true if the template or its derivatives are extractable; default is false"""
        self.isextractable = None
        """true if this template is a featured template, false otherwise"""
        self.isfeatured = None
        """true if the template is available to all accounts; default is true"""
        self.ispublic = None
        """true if the template type is routing i.e., if template is used to deploy router"""
        self.isrouting = None
        """true if the template supports the password reset feature; default is false"""
        self.passwordenabled = None
        """Register template for the project"""
        self.projectid = None
        """true if this template requires HVM"""
        self.requireshvm = None
        """true if the template supports the sshkey upload feature; default is false"""
        self.sshkeyenabled = None
        """the tag for this template."""
        self.templatetag = None
        self.required = ["displaytext","format","hypervisor","name","ostypeid","url","zoneid",]

class registerTemplateResponse (baseResponse):
    def __init__(self):
        """the template ID"""
        self.id = None
        """the account name to which the template belongs"""
        self.account = None
        """the account id to which the template belongs"""
        self.accountid = None
        """true if the ISO is bootable, false otherwise"""
        self.bootable = None
        """checksum of the template"""
        self.checksum = None
        """the date this template was created"""
        self.created = None
        """true if the template is managed across all Zones, false otherwise"""
        self.crossZones = None
        """additional key/value details tied with template"""
        self.details = None
        """the template display text"""
        self.displaytext = None
        """the name of the domain to which the template belongs"""
        self.domain = None
        """the ID of the domain to which the template belongs"""
        self.domainid = None
        """the format of the template."""
        self.format = None
        """the ID of the secondary storage host for the template"""
        self.hostid = None
        """the name of the secondary storage host for the template"""
        self.hostname = None
        """the hypervisor on which the template runs"""
        self.hypervisor = None
        """true if template contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory"""
        self.isdynamicallyscalable = None
        """true if the template is extractable, false otherwise"""
        self.isextractable = None
        """true if this template is a featured template, false otherwise"""
        self.isfeatured = None
        """true if this template is a public template, false otherwise"""
        self.ispublic = None
        """true if the template is ready to be deployed from, false otherwise."""
        self.isready = None
        """the template name"""
        self.name = None
        """the ID of the OS type for this template."""
        self.ostypeid = None
        """the name of the OS type for this template."""
        self.ostypename = None
        """true if the reset password feature is enabled, false otherwise"""
        self.passwordenabled = None
        """the project name of the template"""
        self.project = None
        """the project id of the template"""
        self.projectid = None
        """the date this template was removed"""
        self.removed = None
        """the size of the template"""
        self.size = None
        """the template ID of the parent template if present"""
        self.sourcetemplateid = None
        """true if template is sshkey enabled, false otherwise"""
        self.sshkeyenabled = None
        """the status of the template"""
        self.status = None
        """the tag of this template"""
        self.templatetag = None
        """the type of the template"""
        self.templatetype = None
        """the ID of the zone for this template"""
        self.zoneid = None
        """the name of the zone for this template"""
        self.zonename = None
        """the list of resource tags associated with tempate"""
        self.tags = []
        """the ID of the latest async job acting on this object"""
        self.jobid = None
        """the current status of the latest async job acting on this object"""
        self.jobstatus = None

class tags:
    def __init__(self):
        """"the account associated with the tag"""
        self.account = None
        """"customer associated with the tag"""
        self.customer = None
        """"the domain associated with the tag"""
        self.domain = None
        """"the ID of the domain associated with the tag"""
        self.domainid = None
        """"tag key name"""
        self.key = None
        """"the project name where tag belongs to"""
        self.project = None
        """"the project id the tag belongs to"""
        self.projectid = None
        """"id of the resource"""
        self.resourceid = None
        """"resource type"""
        self.resourcetype = None
        """"tag value"""
        self.value = None

