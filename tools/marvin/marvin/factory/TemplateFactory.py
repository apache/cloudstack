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
from marvin.base import Template
from marvin.factory.CloudStackBaseFactory import CloudStackBaseFactory

class TemplateFactory(CloudStackBaseFactory):

    FACTORY_FOR = Template.Template

    name = None
    displaytext = None
    format = None
    url = None
    hypervisor = None
    zoneid = None
    ostypeid = None

class DefaultBuiltInTemplateFactory(TemplateFactory):
    ostype = 'CentOS 5.3 (64-bit)'
    displaytext = 'CentOS 5.3 (64-bit)'
    name = 'CentOS 5.3 (64-bit)'
    format = 'VHD'
    hypervisor = 'XenServer'
