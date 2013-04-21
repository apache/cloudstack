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
from marvin.cloudstackAPI import listNics
from marvin.cloudstackAPI import addIpToNic
from marvin.cloudstackAPI import removeIpFromNic

class Nic(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def list(self, apiclient, virtualmachineid, **kwargs):
        cmd = listNics.listNicsCmd()
        cmd.virtualmachineid = virtualmachineid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        nics = apiclient.listNics(cmd)
        return map(lambda e: Nic(e.__dict__), nics)


    def add_ip(self, apiclient, **kwargs):
        cmd = addIpToNic.addIpToNicCmd()
        cmd.nicid = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        iptonic = apiclient.addIpToNic(cmd)
        return iptonic


    def remove_ip(self, apiclient, **kwargs):
        cmd = removeIpFromNic.removeIpFromNicCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        ipfromnic = apiclient.removeIpFromNic(cmd)
        return ipfromnic
