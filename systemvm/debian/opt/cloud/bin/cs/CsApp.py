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
import os
from CsFile import CsFile
from CsProcess import CsProcess
import CsHelper


class CsApp:
    def __init__(self, ip):
        self.dev = ip.getDevice()
        self.ip = ip.get_ip_address()
        self.type = ip.get_type()
        self.fw = ip.fw
        self.config = ip.config


class CsApache(CsApp):
    """ Set up Apache """

    def remove(self):
        file = "/etc/apache2/sites-enabled/vhost-%s.conf" % self.dev
        if os.path.isfile(file):
            os.remove(file)
            CsHelper.service("apache2", "restart")

    def setup(self):
        CsHelper.copy_if_needed("/etc/apache2/vhost.template",
                                "/etc/apache2/sites-enabled/vhost-%s.conf" % self.ip)

        file = CsFile("/etc/apache2/sites-enabled/vhost-%s.conf" % (self.ip))
        file.search("<VirtualHost.*:80>", "\t<VirtualHost %s:80>" % (self.ip))
        file.search("<VirtualHost.*:443>", "\t<VirtualHost %s:443>" % (self.ip))
        file.search("Listen .*:80", "Listen %s:80" % (self.ip))
        file.search("Listen .*:443", "Listen %s:443" % (self.ip))
        file.search("ServerName.*", "\tServerName %s.%s" % (self.config.cl.get_type(), self.config.get_domain()))
        if file.is_changed():
            file.commit()
            CsHelper.service("apache2", "restart")

        self.fw.append([
            "", "front",
            "-A INPUT -i %s -d %s/32 -p tcp -m tcp -m state --state NEW --dport 80 -j ACCEPT" % (self.dev, self.ip)
        ])

        self.fw.append([
            "", "front",
            "-A INPUT -i %s -d %s/32 -p tcp -m tcp -m state --state NEW --dport 443 -j ACCEPT" % (self.dev, self.ip)
        ])


class CsPasswdSvc():
    """
      CloudStack VR password server
    """

    def __init__(self, ip):
        self.ip = ip

    def start(self):
        CsHelper.service("cloud-password-server@%s" % self.ip, "start")

    def stop(self):
        CsHelper.service("cloud-password-server@%s" % self.ip, "stop")

    def restart(self):
        CsHelper.service("cloud-password-server@%s" % self.ip, "restart")


class CsDnsmasq(CsApp):
    """ Set up dnsmasq """

    def add_firewall_rules(self):
        """ Add the necessary firewall rules
        """
        self.fw.append(["", "front",
                        "-A INPUT -i %s -p udp -m udp --dport 67 -j ACCEPT" % self.dev
                        ])

        if self.config.has_dns():
            self.fw.append([
                "", "front",
                "-A INPUT -i %s -d %s/32 -p udp -m udp --dport 53 -j ACCEPT" % (self.dev, self.ip)
            ])

            self.fw.append([
                "", "front",
                "-A INPUT -i %s -d %s/32 -p tcp -m tcp --dport 53 -j ACCEPT" % (self.dev, self.ip)
            ])
