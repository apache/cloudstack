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
from marvin.cloudstackAPI import createCounter
from marvin.cloudstackAPI import listCounters
from marvin.cloudstackAPI import deleteCounter

class Counter(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, CounterFactory, **kwargs):
        cmd = createCounter.createCounterCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in CounterFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        counter = apiclient.createCounter(cmd)
        return Counter(counter.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listCounters.listCountersCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        counter = apiclient.listCounters(cmd)
        return map(lambda e: Counter(e.__dict__), counter)


    def delete(self, apiclient, **kwargs):
        cmd = deleteCounter.deleteCounterCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        counter = apiclient.deleteCounter(cmd)
        return counter
