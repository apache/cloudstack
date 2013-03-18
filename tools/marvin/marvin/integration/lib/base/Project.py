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
from marvin.cloudstackAPI import suspendProject
from marvin.cloudstackAPI import createProject
from marvin.cloudstackAPI import listProjects
from marvin.cloudstackAPI import updateProject
from marvin.cloudstackAPI import activateProject
from marvin.cloudstackAPI import deleteProject

class Project(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    def suspend(self, apiclient, id, **kwargs):
        cmd = suspendProject.suspendProjectCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        project = apiclient.suspendProject(cmd)


    @classmethod
    def create(cls, apiclient, ProjectFactory, **kwargs):
        cmd = createProject.createProjectCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in ProjectFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        project = apiclient.createProject(cmd)
        return Project(project.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listProjects.listProjectsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        project = apiclient.listProjects(cmd)
        return map(lambda e: Project(e.__dict__), project)


    def update(self, apiclient, id, **kwargs):
        cmd = updateProject.updateProjectCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        project = apiclient.updateProject(cmd)


    def activate(self, apiclient, id, **kwargs):
        cmd = activateProject.activateProjectCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        project = apiclient.activateProject(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteProject.deleteProjectCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        project = apiclient.deleteProject(cmd)
