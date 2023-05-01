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
from os import listdir
import re
from cs.CsDatabag import CsDataBag
from CsProcess import CsProcess
from CsFile import CsFile
import CsHelper

HAPROXY_CONF_T = "/etc/haproxy/haproxy.cfg.new"
HAPROXY_CONF_P = "/etc/haproxy/haproxy.cfg"

IP_ROUTE_TABLE_NUMBER_FOR_TRANSPARENCY = 99
SSL_CERTS_DIR = "/etc/ssl/cloudstack/"

class CsLoadBalancer(CsDataBag):
    """ Manage Load Balancer entries """

    def process(self):
        if "config" not in self.dbag.keys():
            return
        if 'configuration' not in self.dbag['config'][0].keys():
            return
        if 'ssl_certs' in self.dbag['config'][0].keys():
            self._create_pem_for_sslcert(self.dbag['config'][0]['ssl_certs'])

        config = self.dbag['config'][0]['configuration']
        file1 = CsFile(HAPROXY_CONF_T)
        file1.empty()
        for x in config:
            [file1.append(w, -1) for w in x.split('\n')]

        file1.commit()
        file2 = CsFile(HAPROXY_CONF_P)
        if not file2.compare(file1):
            # Verify new haproxy config before haproxy restart/reload
            haproxy_err = self._verify_haproxy_config(HAPROXY_CONF_T)
            if haproxy_err:
                raise Exception("haproxy config is invalid with error \n%s" % haproxy_err)

            CsHelper.copy(HAPROXY_CONF_T, HAPROXY_CONF_P)

            proc = CsProcess(['/run/haproxy.pid'])
            if not proc.find():
                logging.debug("CsLoadBalancer:: will restart HAproxy!")
                CsHelper.service("haproxy", "restart")
            else:
                logging.debug("CsLoadBalancer:: will reload HAproxy!")
                CsHelper.service("haproxy", "reload")

        add_rules = self.dbag['config'][0]['add_rules']
        remove_rules = self.dbag['config'][0]['remove_rules']
        stat_rules = self.dbag['config'][0]['stat_rules']
        self._configure_firewall(add_rules, remove_rules, stat_rules)
        self._configure_firewall_for_transparent(self.dbag['config'][0]['is_transparent'])

    def _configure_firewall(self, add_rules, remove_rules, stat_rules):
        firewall = self.config.get_fw()

        logging.debug("CsLoadBalancer:: configuring firewall. Add rules ==> %s" % add_rules)
        logging.debug("CsLoadBalancer:: configuring firewall. Remove rules ==> %s" % remove_rules)
        logging.debug("CsLoadBalancer:: configuring firewall. Stat rules ==> %s" % stat_rules)

        for rules in add_rules:
            path = rules.split(':')
            ip = path[0]
            port = path[1]
            firewall.append(["filter", "", "-A INPUT -p tcp -m tcp -d %s --dport %s -m state --state NEW -j ACCEPT" % (ip, port)])

        for rules in remove_rules:
            path = rules.split(':')
            ip = path[0]
            port = path[1]
            firewall.append(["filter", "", "-D INPUT -p tcp -m tcp -d %s --dport %s -m state --state NEW -j ACCEPT" % (ip, port)])

        for rules in stat_rules:
            path = rules.split(':')
            ip = path[0]
            port = path[1]
            firewall.append(["filter", "", "-A INPUT -p tcp -m tcp -d %s --dport %s -m state --state NEW -j ACCEPT" % (ip, port)])

    def _configure_firewall_for_transparent(self, is_transparent):
        tableNo = IP_ROUTE_TABLE_NUMBER_FOR_TRANSPARENCY
        firewall = self.config.get_fw()
        if is_transparent is None or not is_transparent:
            if ["mangle", "", "-A PREROUTING -p tcp -m socket -j DIVERT"] in firewall:
                firewall.remove(["mangle", "", "-A PREROUTING -p tcp -m socket -j DIVERT"])
                firewall.remove(["mangle", "", "-A DIVERT -j MARK --set-xmark %s/0xffffffff" % hex(tableNo)])
                firewall.remove(["mangle", "", "-A DIVERT -j ACCEPT"])
                firewall.remove(["mangle", "", "-N DIVERT"])
            if CsHelper.execute("ip rule show fwmark %s lookup %s" % (tableNo, tableNo)):
                CsHelper.execute("ip route del local 0.0.0.0/0 dev lo table %s" % tableNo)
                CsHelper.execute("ip rule del fwmark %s lookup %s" % (tableNo, tableNo))
        elif is_transparent:
            if ["mangle", "", "-A PREROUTING -p tcp -m socket -j DIVERT"] not in firewall:
                firewall.append(["mangle", "", "-N DIVERT"])
                firewall.append(["mangle", "", "-A PREROUTING -p tcp -m socket -j DIVERT"])
                firewall.append(["mangle", "", "-A DIVERT -j MARK --set-xmark %s/0xffffffff" % hex(tableNo)])
                firewall.append(["mangle", "", "-A DIVERT -j ACCEPT"])
            if not CsHelper.execute("ip rule show fwmark %s lookup %s" % (tableNo, tableNo)):
                CsHelper.execute("ip rule add fwmark %s lookup %s" % (tableNo, tableNo))
                CsHelper.execute("ip route add local 0.0.0.0/0 dev lo table %s" % tableNo)

    def _create_pem_for_sslcert(self, ssl_certs):
        logging.debug("CsLoadBalancer:: creating new pem files in %s and cleaning up it" % SSL_CERTS_DIR)
        if not os.path.exists(SSL_CERTS_DIR):
            CsHelper.execute("mkdir -p %s" % SSL_CERTS_DIR)
        cert_names = []
        for cert in ssl_certs:
            cert_names.append(cert['name'] + ".pem")
            file = CsFile("%s/%s.pem" % (SSL_CERTS_DIR, cert['name']))
            file.empty()
            file.add("%s\n" % cert['cert'].replace("\r\n", "\n"))
            if 'chain' in cert.keys():
                file.add("%s\n" % cert['chain'].replace("\r\n", "\n"))
            file.add("%s\n" % cert['key'].replace("\r\n", "\n"))
            file.commit()
        for f in listdir(SSL_CERTS_DIR):
            if f not in cert_names:
                CsHelper.execute("rm -rf %s/%s" % (SSL_CERTS_DIR, f))

    def _verify_haproxy_config(self, config):
        ret = CsHelper.execute2("haproxy -c -f %s" % config)
        if ret.returncode:
            stdout, stderr = ret.communicate()
            logging.error("haproxy config is invalid with error: %s" % stderr)
            return stderr
        return ""
