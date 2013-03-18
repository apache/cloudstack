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
from marvin.integration.lib.base import CloudStackEntity
from marvin.cloudstackAPI import prepareTemplate
from marvin.cloudstackAPI import createTemplate
from marvin.cloudstackAPI import registerTemplate
from marvin.cloudstackAPI import listTemplates
from marvin.cloudstackAPI import updateTemplate
from marvin.cloudstackAPI import copyTemplate
from marvin.cloudstackAPI import extractTemplate
from marvin.cloudstackAPI import deleteTemplate

class Template(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    def prepare(self, apiclient, zoneid, templateid, **kwargs):
        cmd = prepareTemplate.prepareTemplateCmd()
        cmd.templateid = templateid
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.prepareTemplate(cmd)


    @classmethod
    def create(cls, apiclient, TemplateFactory, **kwargs):
        cmd = createTemplate.createTemplateCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in TemplateFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.createTemplate(cmd)
        return Template(template.__dict__)


    def register(self, apiclient, name, format, url, hypervisor, zoneid, displaytext, ostypeid, **kwargs):
        cmd = registerTemplate.registerTemplateCmd()
        cmd.displaytext = displaytext
        cmd.format = format
        cmd.hypervisor = hypervisor
        cmd.name = name
        cmd.ostypeid = ostypeid
        cmd.url = url
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.registerTemplate(cmd)


    @classmethod
    def list(self, apiclient, templatefilter, **kwargs):
        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = templatefilter
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.listTemplates(cmd)
        return map(lambda e: Template(e.__dict__), template)


    def update(self, apiclient, id, **kwargs):
        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.updateTemplate(cmd)


    def copy(self, apiclient, sourcezoneid, id, destzoneid, **kwargs):
        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = id
        cmd.destzoneid = destzoneid
        cmd.sourcezoneid = sourcezoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.copyTemplate(cmd)


    def extract(self, apiclient, id, mode, **kwargs):
        cmd = extractTemplate.extractTemplateCmd()
        cmd.id = id
        cmd.mode = mode
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.extractTemplate(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        template = apiclient.deleteTemplate(cmd)
