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
import logging
import os.path
import re
import shutil
from cs.CsDatabag import CsDataBag
from CsFile import CsFile
import CsHelper

HAPROXY_CONF_T = "/etc/haproxy/haproxy.cfg.new"
HAPROXY_CONF_P = "/etc/haproxy/haproxy.cfg"


class CsLoadBalancer(CsDataBag):
    """ Manage Load Balance entries """

    def process(self):
        if "config" not in self.dbag.keys():
            return
        if 'configuration' not in self.dbag['config'][0].keys():
            return
        config = self.dbag['config'][0]['configuration']
        file1 = CsFile(HAPROXY_CONF_T)
        file2 = CsFile(HAPROXY_CONF_P)
        file1.empty()
        for x in config:
            [file1.append(w, -1) for w in x.split('\n')]
        if not file2.compare(file1):
            file1.commit()
            shutil.copy2(HAPROXY_CONF_T, HAPROXY_CONF_P)
            CsHelper.service("haproxy", "restart")
        
        add_rules = self.dbag['config'][0]['add_rules']
        remove_rules = self.dbag['config'][0]['remove_rules']
        self._configure_firewall(add_rules, remove_rules)

    def _configure_firewall(self, add_rules, remove_rules):
        firewall = self.fw
        
        for rules in add_rules:
            path = rules.split(':')
            ip = path[0]
            port = path[1]
            fw.append(["filter", "", "-A INPUT -p tcp -m tcp -d %s --dport %s -m state --state NEW -j ACCEPT" % (ip, port)])

        for rules in remove_rules:
            path = rules.split(':')
            ip = path[0]
            port = path[1]
            fw.append(["filter", "", "-D INPUT -p tcp -m tcp -d %s --dport %s -m state --state NEW -j ACCEPT" % (ip, port)])
