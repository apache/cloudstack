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
from marvin.cloudstackAPI import createDomain
from marvin.cloudstackAPI import listDomains
from marvin.cloudstackAPI import updateDomain
from marvin.cloudstackAPI import deleteDomain

class Domain(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    @classmethod
    def create(cls, apiclient, DomainFactory, **kwargs):
        cmd = createDomain.createDomainCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in DomainFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        domain = apiclient.createDomain(cmd)
        return Domain(domain.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listDomains.listDomainsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        domain = apiclient.listDomains(cmd)
        return map(lambda e: Domain(e.__dict__), domain)


    def update(self, apiclient, id, **kwargs):
        cmd = updateDomain.updateDomainCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        domain = apiclient.updateDomain(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteDomain.deleteDomainCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        domain = apiclient.deleteDomain(cmd)
