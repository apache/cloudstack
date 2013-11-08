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


"""Creates a template of a virtual machine. The virtual machine must be in a STOPPED state. A template created from this command is automatically designated as a private template visible to the account that created it."""
from baseCmd import *
from baseResponse import *
class createTemplateCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the display text of the template. This is usually used for display purposes."""
        """Required"""
        self.displaytext = None
        """the name of the template"""
        """Required"""
        self.name = None
        """the ID of the OS Type that best represents the OS of this template."""
        """Required"""
        self.ostypeid = None
        """32 or 64 bit"""
        self.bits = None
        """Template details in key/value pairs."""
        self.details = []
        """true if template contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory"""
        self.isdynamicallyscalable = None
        """true if this template is a featured template, false otherwise"""
        self.isfeatured = None
        """true if this template is a public template, false otherwise"""
        self.ispublic = None
        """true if the template supports the password reset feature; default is false"""
        self.passwordenabled = None
        """true if the template requres HVM, false otherwise"""
        self.requireshvm = None
        """the ID of the snapshot the template is being created from. Either this parameter, or volumeId has to be passed in"""
        self.snapshotid = None
        """the tag for this template."""
        self.templatetag = None
        """Optional, only for baremetal hypervisor. The directory name where template stored on CIFS server"""
        self.url = None
        """Optional, VM ID. If this presents, it is going to create a baremetal template for VM this ID refers to. This is only for VM whose hypervisor type is BareMetal"""
        self.virtualmachineid = None
        """the ID of the disk volume the template is being created from. Either this parameter, or snapshotId has to be passed in"""
        self.volumeid = None
        self.required = ["displaytext","name","ostypeid",]

class createTemplateResponse (baseResponse):
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

