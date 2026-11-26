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

import os
import sys
import unittest

HEALTH_CHECKS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                 "..", "debian", "root", "health_checks")
sys.path.insert(0, HEALTH_CHECKS_DIR)

import haproxy_check  # noqa: E402


SSL_BIND_OPTIONS = ("ssl crt /etc/cloudstack/ssl/10_0_62_24-443.pem alpn h2,http/1.1 "
                    "ssl-min-ver TLSv1.2 no-tls-tickets")


def build_lb_rule():
    return {
        "sourceIp": "10.0.62.24",
        "sourcePortStart": "443",
        "sourcePortEnd": "443",
        "algorithm": "roundrobin",
        "keepAliveEnabled": "true",
        "stickiness": "",
        "vmIps": "10.1.1.10",
        "destPortStart": "80",
        "destPortEnd": "80",
    }


def build_haproxy_section(bind_value):
    return {
        "balance": ["roundrobin"],
        "bind": [bind_value],
        "server": ["10_0_62_24-443_1 10.1.1.10:80 check"],
    }


class TestHaproxyCheckBind(unittest.TestCase):

    def check(self, bind_value):
        haCfgSections = {
            "listen 10_0_62_24-443": build_haproxy_section(bind_value),
        }
        return haproxy_check.checkLoadBalance([build_lb_rule()], haCfgSections)

    def test_plain_bind_passes(self):
        self.assertTrue(self.check("10.0.62.24:443"))

    def test_ssl_bind_with_options_passes(self):
        self.assertTrue(self.check("10.0.62.24:443 " + SSL_BIND_OPTIONS))

    def test_ssl_bind_with_wrong_port_fails(self):
        self.assertFalse(self.check("10.0.62.24:4430 " + SSL_BIND_OPTIONS))

    def test_ssl_bind_with_wrong_ip_fails(self):
        self.assertFalse(self.check("10.0.62.25:443 " + SSL_BIND_OPTIONS))


if __name__ == '__main__':
    unittest.main()
