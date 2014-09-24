# -- coding: utf-8 --
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
from merge import dataBag

class CsDataBag(object):

    def __init__(self, key, config = None):
        self.data = {}
        self.db = dataBag()
        self.db.setKey(key)
        self.db.load()
        self.dbag = self.db.getDataBag()
        if config:
            self.fw = config.get_fw()
            self.cl = config.get_cmdline()
            self.config = config

    def dump(self):
        print self.dbag

    def get_bag(self):
        return self.dbag

    def process(self):
        pass

    def save(self):
        """
        Call to the databag save routine
        Use sparingly!
        """
        self.db.save(self.dbag)

class CsCmdLine(CsDataBag):
    """ Get cmdline config parameters """

    def is_redundant(self):
        if "redundant_router" in self.dbag['config']:
            return self.dbag['config']['redundant_router'] == "true"
        return False

    def get_name(self):
        if "name" in self.dbag['config']:
            return self.dbag['config']['name']
        else:
            return "unloved-router"

    def get_type(self):
        if "type" in self.dbag['config']:
            return self.dbag['config']['type']
        else:
            return "unknown"

    def get_vpccidr(self):
        if "vpccidr" in self.dbag['config']:
            return self.dbag['config']['vpccidr']
        else:
            return "unknown"

    def is_master(self):
        if not self.is_redundant():
            return False
        if "redundant_master" in self.dbag['config']:
            return self.dbag['config']['redundant_master'] == "true"
        return False

