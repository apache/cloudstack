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
import subprocess

TABLE = "cloudstack_usage"
DIRECTIONS = ["out", "in", "tcp_out", "tcp_in"]


class CsIpv6Usage(object):
    """ Counts IPv6 traffic for network usage. Each rule copies one of the IPv4 NETWORK_STATS rules, so both
    families count the same traffic. netusage.sh and vpc_netusage.sh add these counts to the IPv4 ones they report.

    The counts are named nftables counters in their own table. The rules are rebuilt on every run, but named
    counters live outside the rules, so rebuilding them keeps the counts. Exempt addresses are skipped on
    forwarded traffic, the same as the IPv4 exempt chain. """

    def __init__(self, config):
        self.config = config

    def process(self):
        rules = self.build()
        if rules is None:
            return
        result = subprocess.run(["nft", "-f", "-"], input=rules, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                universal_newlines=True)
        if result.returncode != 0:
            logging.error("Failed to apply the IPv6 usage counters: %s", result.stderr.strip())

    def build(self):
        """ The nft commands for the table, or None on routers whose usage isn't counted here. """
        if self.config.is_routed() or self.config.is_dhcp():
            return None
        if self.config.is_vpc():
            counters = [(dev, dev) for dev in self.public_devices()]
        elif self.config.is_router():
            # The global NETWORK_STATS chain always counts eth2, and is read and reset on its own
            counters = [("stats", "eth2")] + [(dev, dev) for dev in self.public_devices()]
        else:
            return None

        lines = ["add table ip6 %s" % TABLE]
        for name in ["exempt", "vpc"]:
            lines.append("add set ip6 %s %s { type ipv6_addr; flags interval; auto-merge; }" % (TABLE, name))
        for prefix, dev in counters:
            for direction in DIRECTIONS:
                lines.append("add counter ip6 %s %s_%s" % (TABLE, prefix, direction))
        for hook in ["forward", "input", "output"]:
            lines.append("add chain ip6 %s %s { type filter hook %s priority -1; policy accept; }" % (TABLE, hook, hook))
            lines.append("flush chain ip6 %s %s" % (TABLE, hook))
        self.append_set(lines, "exempt", self.config.get_usage_exempt_cidrs(version=6))
        self.append_set(lines, "vpc", self.vpc_prefixes() if self.config.is_vpc() else [])

        rule = "add rule ip6 %s %s " % (TABLE, "%s")
        lines.append(rule % "forward" + "ip6 daddr @exempt return")
        lines.append(rule % "forward" + "ip6 saddr @exempt return")
        for prefix, dev in counters:
            if self.config.is_vpc():
                matches = self.vpc_matches(dev)
            else:
                matches = self.router_matches(dev)
            for hook, direction, match in matches:
                lines.append(rule % hook + "%s counter name %s_%s" % (match, prefix, direction))
        return "\n".join(lines) + "\n"

    @staticmethod
    def router_matches(dev):
        """ The same traffic as NETWORK_STATS on an isolated router: guest (eth0) to and from the public
        interface, and TCP between the public interface and anything else, such as the router itself. """
        return [
            ("forward", "out", 'iifname "eth0" oifname "%s"' % dev),
            ("forward", "in", 'iifname "%s" oifname "eth0"' % dev),
            ("forward", "tcp_out", 'oifname "%s" iifname != "eth0" meta l4proto tcp' % dev),
            ("forward", "tcp_in", 'iifname "%s" oifname != "eth0" meta l4proto tcp' % dev),
            ("output", "tcp_out", 'oifname "%s" meta l4proto tcp' % dev),
            ("input", "tcp_in", 'iifname "%s" meta l4proto tcp' % dev),
        ]

    @staticmethod
    def vpc_matches(dev):
        """ The same traffic as NETWORK_STATS_<dev> on a VPC router: the tiers' prefixes to and from the public
        interface, and TCP between the public interface and anything else, such as the router itself. The router's
        own traffic counts too, as the IPv4 chain is also run from INPUT and OUTPUT. """
        return [
            ("forward", "out", 'ip6 saddr @vpc oifname "%s"' % dev),
            ("forward", "in", 'ip6 daddr @vpc iifname "%s"' % dev),
            ("forward", "tcp_out", 'ip6 saddr != @vpc oifname "%s" meta l4proto tcp' % dev),
            ("forward", "tcp_in", 'ip6 daddr != @vpc iifname "%s" meta l4proto tcp' % dev),
            ("output", "out", 'ip6 saddr @vpc oifname "%s"' % dev),
            ("input", "in", 'ip6 daddr @vpc iifname "%s"' % dev),
            ("output", "tcp_out", 'ip6 saddr != @vpc oifname "%s" meta l4proto tcp' % dev),
            ("input", "tcp_in", 'ip6 daddr != @vpc iifname "%s" meta l4proto tcp' % dev),
        ]

    @staticmethod
    def append_set(lines, name, cidrs):
        lines.append("flush set ip6 %s %s" % (TABLE, name))
        if cidrs:
            lines.append("add element ip6 %s %s { %s }" % (TABLE, name, ", ".join(cidrs)))

    def public_devices(self):
        devices = []
        for interface in self.config.address().get_interfaces():
            if interface.is_public() and interface.is_added() and interface.get_device() not in devices:
                devices.append(interface.get_device())
        return sorted(devices)

    def vpc_prefixes(self):
        """ The IPv6 prefixes of the VPC's tiers. """
        prefixes = []
        guest = self.config.guestnetwork()
        for interface in self.config.address().get_interfaces():
            if not interface.is_guest() or not interface.is_added():
                continue
            cidr = guest.get_dev_ip6cidr(interface.get_device())
            if cidr and cidr not in prefixes:
                prefixes.append(cidr)
        return sorted(prefixes)
