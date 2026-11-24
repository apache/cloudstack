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
import os
from . import CsHelper
from .CsDatabag import CsDataBag
from .CsFile import CsFile
from .CsFrrConfig import mask_passwords

FRR_DIR = "/etc/frr/"
FRR_DAEMONS = "/etc/frr/daemons"
FRR_CONFIG = "/etc/frr/frr.conf"
FRR_RELOAD = "/usr/lib/frr/frr-reload.py"
IPV6_FORWARDING = "/proc/sys/net/ipv6/conf/all/forwarding"
# A copy of frr.conf without passwords, which getDiagnosticsData collects.
MASKED_COPY = "/var/cache/cloud/frr.conf.masked"


class CsBgpPeers(CsDataBag):

    def process(self):
        logging.info("Processing CsBgpPeers file ==> %s" % self.dbag)

        if self.config.is_vpc():
            self.public_ip = self.cl.get_source_nat_ip()
        else:
            self.public_ip = self.cl.get_eth2_ip()

        self.min_holdtime = self.dbag.get("min_holdtime")
        self.peers = {}
        for item in self.dbag:
            if item in ("id", "min_holdtime"):
                continue
            self._process_dbag_item(self.dbag[item])

        CsHelper.mkdir(FRR_DIR, 0o755, False)
        self.frr_daemon = CsFile(FRR_DAEMONS)
        self.frr_daemon.replaceIfFound("bgpd=no", "bgpd=yes")
        start_bgpd = self.frr_daemon.commit()

        self.frr_conf = CsFile(FRR_CONFIG)
        self.frr_conf.repopulate()
        self._pre_set()
        self._access_list_set()
        self._process_peers()
        self._route_map_set()
        self._post_set()
        conf_changed = self.frr_conf.commit()
        self._write_masked_copy()

        if start_bgpd:
            # bgpd only starts with frr, so turning it on needs a restart.
            self._restart_frr()
        elif conf_changed:
            self._reload_frr()

    def _restart_frr(self):
        CsHelper.execute("systemctl enable frr")
        CsHelper.execute("systemctl restart frr")

    def _reload_frr(self):
        # frr-reload.py applies only the differences, so sessions to unchanged peers stay up.
        # Templates without frr-pythontools have no frr-reload.py, and restart as before.
        if not os.path.isfile(FRR_RELOAD):
            logging.info("%s not found, restarting frr" % FRR_RELOAD)
            self._restart_frr()
            return
        # communicate() reads the output while waiting, so a long output cannot fill the pipe and block.
        result = CsHelper.execute2("%s --reload %s" % (FRR_RELOAD, FRR_CONFIG), False)
        # The output is not logged, since it can echo config lines with peer passwords.
        result.communicate()
        if result.returncode != 0:
            logging.error("frr-reload.py failed with exit code %s, restarting frr" % result.returncode)
            self._restart_frr()

    def _write_masked_copy(self):
        try:
            with open(MASKED_COPY, "w") as f:
                for line in mask_passwords([line.rstrip("\n") for line in self.frr_conf.new_config]):
                    f.write(line + "\n")
        except IOError as e:
            logging.error("Unable to write %s: %s" % (MASKED_COPY, e))

    def _ipv6_forwarding(self):
        # State the kernel's current setting, so that frr-reload.py never changes it.
        try:
            with open(IPV6_FORWARDING) as f:
                return f.read().strip() == "1"
        except IOError:
            return False

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
        # frr-reload.py changes every setting the file leaves out, so the file states them.
        self.frr_conf.add("frr defaults traditional")
        self.frr_conf.add("hostname {}".format(CsHelper.get_hostname()))
        self.frr_conf.add("service integrated-vtysh-config")
        self.frr_conf.add("ip forwarding")
        self.frr_conf.add("ipv6 forwarding" if self._ipv6_forwarding() else "no ipv6 forwarding")
        self.frr_conf.add("ip nht resolve-via-default")
        self.frr_conf.add("ipv6 nht resolve-via-default")
        return

    def _access_list_set(self):
        self.frr_conf.add("ip prefix-list all-v4 seq 1 permit any")
        self.frr_conf.add("ip prefix-list default-v4 seq 1 permit 0.0.0.0/0")
        self.frr_conf.add("ipv6 prefix-list all-v6 seq 1 permit any")
        self.frr_conf.add("ipv6 prefix-list default-v6 seq 1 permit ::/0")

        ip4_cidrs = set()
        ip6_cidrs = set()
        for as_number in self.peers.keys():
            ip4_cidrs.update(ip4_peer['guest_ip4_cidr'] for ip4_peer in self.peers[as_number]['ip4_peers'])
            ip6_cidrs.update(ip6_peer['guest_ip6_cidr'] for ip6_peer in self.peers[as_number]['ip6_peers'])

        for seq, ip4_cidr in enumerate(sorted(ip4_cidrs), start=1):
            self.frr_conf.add("ip prefix-list local-v4 seq {} permit {}".format(seq, ip4_cidr))

        for seq, ip6_cidr in enumerate(sorted(ip6_cidrs), start=1):
            self.frr_conf.add("ipv6 prefix-list local-v6 seq {} permit {}".format(seq, ip6_cidr))

        return

    def _process_peers(self):
        for as_number in self.peers.keys():
            self.frr_conf.add("router bgp {}".format(as_number))
            self.frr_conf.add(" bgp router-id {}".format(self.public_ip))
            self.frr_conf.add(" bgp ebgp-requires-policy")
            self.frr_conf.add(" bgp network import-check")
            # FRR accepts 1-65535 here. 0 turns off the minimum and renders no line.
            if self.min_holdtime and 1 <= int(self.min_holdtime) <= 65535:
                self.frr_conf.add(" bgp minimum-holdtime {}".format(self.min_holdtime))
            elif self.min_holdtime:
                logging.error("Ignoring minimum hold time %s, which is outside 1-65535" % self.min_holdtime)
            if self.peers[as_number]['ip6_peers']:
                self.frr_conf.add(" bgp default ipv6-unicast")
            for ip4_peer in self.peers[as_number]['ip4_peers']:
                self.frr_conf.add(" neighbor {} remote-as {}".format(ip4_peer['ip4_address'], ip4_peer['peer_as_number']))
                if 'peer_password' in ip4_peer:
                    self.frr_conf.add(" neighbor {} password {}".format(ip4_peer['ip4_address'], ip4_peer['peer_password']))
                if 'details' in ip4_peer:
                    if 'EBGP_MultiHop' in ip4_peer['details']:
                        self.frr_conf.add(" neighbor {} ebgp-multihop {}".format(ip4_peer['ip4_address'], ip4_peer['details']['EBGP_MultiHop']))
            for ip6_peer in self.peers[as_number]['ip6_peers']:
                self.frr_conf.add(" neighbor {} remote-as {}".format(ip6_peer['ip6_address'], ip6_peer['peer_as_number']))
                if 'peer_password' in ip6_peer:
                    self.frr_conf.add(" neighbor {} password {}".format(ip6_peer['ip6_address'], ip6_peer['peer_password']))
                if 'details' in ip6_peer:
                    if 'EBGP_MultiHop' in ip6_peer['details']:
                        self.frr_conf.add(" neighbor {} ebgp-multihop {}".format(ip6_peer['ip6_address'], ip6_peer['details']['EBGP_MultiHop']))
            if self.peers[as_number]['ip4_peers']:
                self.frr_conf.add(" address-family ipv4 unicast")
                for ip4_peer in self.peers[as_number]['ip4_peers']:
                    self.frr_conf.add("  neighbor {} route-map upstream-v4-in in".format(ip4_peer['ip4_address']))
                    self.frr_conf.add("  neighbor {} route-map upstream-v4-out out".format(ip4_peer['ip4_address']))
                    self.frr_conf.add("  neighbor {} soft-reconfiguration inbound".format(ip4_peer['ip4_address']))
                ip4_cidrs = set({ip4_peer['guest_ip4_cidr'] for ip4_peer in self.peers[as_number]['ip4_peers']})
                for ip4_cidr in ip4_cidrs:
                    self.frr_conf.add("  network {}".format(ip4_cidr))
                self.frr_conf.add(" exit-address-family")
            if self.peers[as_number]['ip6_peers']:
                self.frr_conf.add(" address-family ipv6 unicast")
                for ip6_peer in self.peers[as_number]['ip6_peers']:
                    self.frr_conf.add("  neighbor {} route-map upstream-v6-in in".format(ip6_peer['ip6_address']))
                    self.frr_conf.add("  neighbor {} route-map upstream-v6-out out".format(ip6_peer['ip6_address']))
                    self.frr_conf.add("  neighbor {} soft-reconfiguration inbound".format(ip6_peer['ip6_address']))
                ip6_cidrs = set({ip6_peer['guest_ip6_cidr'] for ip6_peer in self.peers[as_number]['ip6_peers']})
                for ip6_cidr in ip6_cidrs:
                    self.frr_conf.add("  network {}".format(ip6_cidr))
                self.frr_conf.add(" exit-address-family")

    def _route_map_set(self):
        self.frr_conf.add("route-map upstream-v4-in permit 10")
        self.frr_conf.add("  match ip address prefix-list default-v4")
        self.frr_conf.add("route-map upstream-v4-in deny 1000")
        self.frr_conf.add("  match ip address prefix-list all-v4")

        self.frr_conf.add("route-map upstream-v4-out permit 10")
        self.frr_conf.add("  match ip address prefix-list local-v4")
        self.frr_conf.add("route-map upstream-v4-out deny 1000")
        self.frr_conf.add("  match ip address prefix-list all-v4")

        self.frr_conf.add("route-map upstream-v6-in permit 10")
        self.frr_conf.add("  match ipv6 address prefix-list default-v6")
        self.frr_conf.add("route-map upstream-v6-in deny 1000")
        self.frr_conf.add("  match ipv6 address prefix-list all-v6")

        self.frr_conf.add("route-map upstream-v6-out permit 10")
        self.frr_conf.add("  match ipv6 address prefix-list local-v6")
        self.frr_conf.add("route-map upstream-v6-out deny 1000")
        self.frr_conf.add("  match ipv6 address prefix-list all-v6")
        return

    def _post_set(self):
        self.frr_conf.add("line vty")
