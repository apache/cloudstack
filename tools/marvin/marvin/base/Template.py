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
from marvin.base import CloudStackEntity
from marvin.cloudstackAPI import prepareTemplate
from marvin.cloudstackAPI import createTemplate
from marvin.cloudstackAPI import registerTemplate
from marvin.cloudstackAPI import listTemplates
from marvin.cloudstackAPI import updateTemplate
from marvin.cloudstackAPI import copyTemplate
from marvin.cloudstackAPI import extractTemplate
from marvin.cloudstackAPI import deleteTemplate

class Template(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def prepare(self, apiclient, zoneid, templateid, **kwargs):
        cmd = prepareTemplate.prepareTemplateCmd()
        cmd.id = self.id
        cmd.templateid = templateid
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.prepareTemplate(cmd)
        return template


    def template_of_vm(self, apiclient, name, displaytext, ostypeid, **kwargs):
        cmd = createTemplate.createTemplateCmd()
        cmd.id = self.id
        cmd.name = name
        cmd.displaytext = displaytext
        cmd.ostypeid = ostypeid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.createTemplate(cmd)
        return Template(template.__dict__)


    @classmethod
    def create(self, apiclient, factory, **kwargs):
        cmd = registerTemplate.registerTemplateCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.registerTemplate(cmd)
        return template


    @classmethod
    def list(self, apiclient, templatefilter, **kwargs):
        cmd = listTemplates.listTemplatesCmd()
        cmd.templatefilter = templatefilter
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.listTemplates(cmd)
        return map(lambda e: Template(e.__dict__), template)


    def update(self, apiclient, **kwargs):
        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.updateTemplate(cmd)
        return template


    def copy(self, apiclient, sourcezoneid, destzoneid, **kwargs):
        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = self.id
        cmd.destzoneid = destzoneid
        cmd.sourcezoneid = sourcezoneid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.copyTemplate(cmd)
        return template


    def extract(self, apiclient, mode, **kwargs):
        cmd = extractTemplate.extractTemplateCmd()
        cmd.id = self.id
        cmd.mode = mode
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.extractTemplate(cmd)
        return template


    def delete(self, apiclient, **kwargs):
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        template = apiclient.deleteTemplate(cmd)
        return template
