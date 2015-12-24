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
from cs.CsDatabag import CsDataBag
from CsProcess import CsProcess
from CsFile import CsFile
import CsHelper

OSPFD_CONF = "/etc/quagga/ospfd.conf"
OSPFD_CONF_NEW = "/etc/quagga/ospfd.conf.new"
ZEBRA_CONF = "/etc/quagga/zebra.conf"
ZEBRA_CONF_NEW = "/etc/quagga/zebra.conf.new"


class CsQuagga(CsDataBag):
    """ Manage Load Balancer entries """

    def process(self):
        #create quagga config
        logging.debug("CsQuagga: zebra" + str(self.dbag["zebra_config"]))
        logging.debug("CsQuagga: ospfd" + str(self.dbag["ospf_config"]))

        zebra_config = self.dbag["zebra_config"].split(',')
        ospf_config = self.dbag["ospf_config"].split(',')
        changed = False
        #process zebra
        zebra_conf_new = CsFile(ZEBRA_CONF_NEW)
        zebra_conf_new.empty()
        for x in zebra_config:
            [zebra_conf_new.append(w, -1) for w in x.split('\n')]

        zebra_conf_new.commit()
        zebra_conf = CsFile(ZEBRA_CONF)
        if not zebra_conf.compare(zebra_conf_new):
            CsHelper.copy(ZEBRA_CONF_NEW, ZEBRA_CONF)
            changed = True

        #process ospfd
        ospfd_conf_new = CsFile(OSPFD_CONF_NEW)
        ospfd_conf_new.empty()
        for x in ospf_config:
            [ospfd_conf_new.append(w, -1) for w in x.split('\n')]

        ospfd_conf_new.commit()
        ospfd_conf = CsFile(OSPFD_CONF)
        if not ospfd_conf.compare(ospfd_conf_new):
            CsHelper.copy(OSPFD_CONF_NEW, OSPFD_CONF)
            changed = True

        if changed:
            #reset zebra and quagga
            proc = CsProcess(['/var/run/quagga.pid'])
            logging.debug("CsQuagga: Resetted quagga ")
            if not proc.find():
                logging.debug("CsQuagga:: will restart Quagga!")
                CsHelper.service("quagga", "restart")
            else:
                logging.debug("CsQuagga:: will reload Quagga!")
                CsHelper.service("quagga", "reload")


    def applyFwRules(self):
        # Firewall rules to allow OSPF LSA broadcasts
        firewall = self.config.get_fw()
        logging.debug("CsQuagga: Applying iptable rules for quagga ospf LSA adv:")
        firewall.append(["", "", "-A INPUT -p 89 -j ACCEPT -m comment --comment 'quagga fw rule'"])
        firewall.append(["", "", "-I INPUT 2 -d 224.0.0.0/24 -j ACCEPT -m comment --comment 'quagga fw rule'"])


