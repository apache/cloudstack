#!/usr/bin/python
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

import logging
from . import CsHelper
from .CsDatabag import CsDataBag
from .CsFile import CsFile

FRR_DIR = "/etc/frr/"
FRR_DAEMONS = "/etc/frr/daemons"
FRR_CONFIG = "/etc/frr/frr.conf"


class CsBgpPeers(CsDataBag):

    def process(self):
        logging.info("Processing CsBgpPeers file ==> %s" % self.dbag)

        if self.config.is_vpc():
            self.public_ip = self.cl.get_source_nat_ip()
        else:
            self.public_ip = self.cl.get_eth2_ip()

        self.peers = {}
        for item in self.dbag:
            if item == "id":
                continue
            self._process_dbag_item(self.dbag[item])

        restart_frr = False

        CsHelper.mkdir("FRR_DIR", 0o755, False)
        self.frr_daemon = CsFile(FRR_DAEMONS)
        self.frr_daemon.replaceIfFound("bgpd=no", "bgpd=yes")
        if self.frr_daemon.commit():
            restart_frr = True

        self.frr_conf = CsFile(FRR_CONFIG)
        self.frr_conf.repopulate()
        self._pre_set()
        self._process_peers()
        self._post_set()
        if self.frr_conf.commit():
            restart_frr = True

        if restart_frr:
            CsHelper.execute("systemctl enable frr")
            CsHelper.execute("systemctl restart frr")

    def _process_dbag_item(self, item):
        as_number = item['network_as_number']
        if as_number not in self.peers.keys():
            self.peers[as_number] = {}
            self.peers[as_number]['ip4_peers'] = []
            self.peers[as_number]['ip6_peers'] = []
        if 'ip4_address' in item and 'guest_ip4_cidr' in item:
            self.peers[as_number]['ip4_peers'].append(item)
        if 'ip6_address' in item and 'guest_ip6_cidr' in item:
            self.peers[as_number]['ip6_peers'].append(item)

    def _pre_set(self):
        self.frr_conf.add("frr version 6.0")
        self.frr_conf.add("frr defaults traditional")
        self.frr_conf.add("hostname {}".format(CsHelper.get_hostname()))
        self.frr_conf.add("service integrated-vtysh-config")
        return

    def _process_peers(self):
        for as_number in self.peers.keys():
            self.frr_conf.add("router bgp {}".format(as_number))
            self.frr_conf.add(" bgp router-id {}".format(self.public_ip))
            for ip4_peer in self.peers[as_number]['ip4_peers']:
                self.frr_conf.add(" neighbor {} remote-as {}".format(ip4_peer['ip4_address'], ip4_peer['peer_as_number']))
                if 'peer_password' in ip4_peer:
                    self.frr_conf.add(" neighbor {} password {}".format(ip4_peer['ip4_address'], ip4_peer['peer_password']))
            for ip6_peer in self.peers[as_number]['ip6_peers']:
                self.frr_conf.add(" neighbor {} remote-as {}".format(ip6_peer['ip6_address'], ip6_peer['peer_as_number']))
                if 'peer_password' in ip6_peer:
                    self.frr_conf.add(" neighbor {} password {}".format(ip6_peer['ip4_address'], ip6_peer['peer_password']))
            if self.peers[as_number]['ip4_peers']:
                self.frr_conf.add(" address-family ipv4 unicast")
                for ip4_peer in self.peers[as_number]['ip4_peers']:
                    self.frr_conf.add("  network {}".format(ip4_peer['guest_ip4_cidr']))
                self.frr_conf.add(" exit-address-family")
            if self.peers[as_number]['ip6_peers']:
                self.frr_conf.add(" address-family ipv6 unicast")
                for ip6_peer in self.peers[as_number]['ip6_peers']:
                    self.frr_conf.add("  network {}".format(ip6_peer['guest_ip6_cidr']))
                self.frr_conf.add(" exit-address-family")

    def _post_set(self):
        self.frr_conf.add("line vty")
