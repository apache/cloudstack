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

"""Basic integration test that runs update_config.py."""

from nose.plugins.attrib import attr
from cuisine import file_write, run
from fabric.api import hide
import json
import random
import datetime
from envassert import file, process, package, user, group, port, cron, detect, ip
import copy

try:
    from . import SystemVMTestCase, has_line, print_doc
except (ImportError, ValueError):
    from systemvm import SystemVMTestCase, has_line, print_doc


def deep_copy(obj):
    return json.loads(json.dumps(obj))


class UpdateConfigTestCase(SystemVMTestCase):
    basic_config = {
        "ip_address": [
            {
                "public_ip": "10.0.2.102",
                "source_nat": True,
                "add": True,
                "one_to_one_nat": False,
                "first_i_p": False,
                "gateway": "10.0.2.1",
                "netmask": "255.255.255.0",
                "vif_mac_address": "06:cb:aa:00:00:03",
                "nic_dev_id": 1,
                "new_nic": False,
                "nw_type": "public"
            }
        ],
        "type": "ips"
    }

    def update_config(self, config):
        config_json = json.dumps(config, indent=2)
        print_doc('config.json', config_json)
        file_write('/var/cache/cloud/update_config_test.json', config_json)
        with hide("everything"):
            result = run("python /opt/cloud/bin/update_config.py update_config_test.json",
                         timeout=600, warn_only=True)
            assert result.succeeded, 'update_config.py ran without errors'
            assert result.find("Convergence is achieved") >= 0, 'update_config.py should report convergence'

    def clear_log(self):
        tstamp = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
        run("test -f /var/log/cloud.log && mv /var/log/cloud.log /var/log/cloud.log.%s || true" % tstamp)

    def setUp(self):
        super(UpdateConfigTestCase, self).setUp()
        self.clear_log()

    def check_no_errors(self):
        # todo config update should exit 1 on convergence errors!
        found, context = has_line('/var/log/cloud.log', 'cannot be configured')
        if found:
            print_doc('/var/log/cloud.log', context)
        assert not found, 'cloud.log should not contain "cannot be configured"'

    @attr(tags=["systemvm"], required_hardware="true")
    def test_basic_config(self):
        self.update_config(self.basic_config)
        self.check_no_errors()
        # should be able to run twice with same config
        self.clear_log()
        self.update_config(self.basic_config)
        self.check_no_errors()

    @attr(tags=["systemvm"], required_hardware="true")
    def test_various_random_ip_addresses(self):
        buffer = []
        r = random.Random()
        r.seed()
        for i in range(0, 10):
            ip_address = {}
            # todo need to know what kind of configurations are valid!
            config = deep_copy(self.basic_config)
            ip_address = deep_copy(self.basic_config["ip_address"][0])
            ip_address["public_ip"] = "10.0.2.%d" % (i + 103)
            ip_address["source_nat"] = r.choice((True, False))
            ip_address["add"] = True
            ip_address["one_to_one_nat"] = r.choice((True, False))
            ip_address["first_i_p"] = r.choice((True, False))
            ip_address["nic_dev_id"] = 3
            config["ip_address"].append(ip_address)
            # runs a bunch of times adding an IP address each time
            self.update_config(config)
            ip_address["add"] = False
            buffer.append(copy.deepcopy(ip_address))
            self.check_no_errors()
            #self.clear_log()
            assert ip.has_ip("%s/24" % ip_address["public_ip"], "eth%s" % ip_address["nic_dev_id"]), \
                    "Configure %s on eth%s failed" % (ip_address["public_ip"], ip_address["nic_dev_id"])
        # Now delete all the IPs we just made
        for ips in buffer:
            config = copy.deepcopy(self.basic_config)
            config["ip_address"].append(ips)
            self.update_config(config)
            assert not ip.has_ip("%s/24" % ips["public_ip"], "eth%s" % ips["nic_dev_id"]), \
                    "Delete %s on eth%s failed" % (ips["public_ip"], ips["nic_dev_id"])

    def test_create_guest_network(self):
        config = { "add":True,
                   "mac_address":"02:00:56:36:00:02",
                   "device":"eth4",
                   "router_guest_ip":"172.16.1.1",
                   "router_guest_gateway":"172.16.1.0",
                   "router_guest_netmask":"255.255.255.0",
                   "cidr":"24",
                   "dns":"8.8.8.8,8.8.8.4",
                   "domain_name":"devcloud.local",
                   "type":"guestnetwork"
                   }
        self.guest_network(config)
        passw = { "172.16.1.20" : "20",
                  "172.16.1.21" : "21",
                  "172.16.1.22" : "22"
                  }
        self.check_password(passw)

        passw = { "172.16.1.20" : "120",
                  "172.16.1.21" : "121",
                  "172.16.1.22" : "122"
                  }
        self.check_password(passw)

        config = { "add":True,
                   "mac_address":"02:00:56:36:00:02",
                   "device":"eth4",
                   "router_guest_ip":"172.16.2.1",
                   "router_guest_gateway":"172.16.2.0",
                   "router_guest_netmask":"255.255.255.0",
                   "cidr":"24",
                   "dns":"8.8.8.8,8.8.8.4",
                   "domain_name":"devcloud2.local",
                   "type":"guestnetwork"
                   }
        self.guest_network(config)

    def check_password(self,passw):
        for val in passw:
            self.add_password(val, passw[val])
        for val in passw:
            assert file.has_line("/var/cache/cloud/passwords", "%s=%s" % (val, passw[val]))

    def add_password(self, ip, password):
        config = { "ip_address": ip,
                   "password":password,
                   "type":"vmpassword"
                 }
        self.update_config(config)
        assert file.has_line("/var/cache/cloud/passwords", "%s=%s" % (ip, password))

    def guest_network(self,config):
        self.update_config(config)
        assert ip.has_ip("%s/%s" % (config['router_guest_ip'], config['cidr']), config['device'])
        assert process.is_up("apache2"), "Apache2 should be running after adding a guest network"
        assert process.is_up("dnsmasq"), "Dnsmasq should be running after adding a guest network"
        assert port.is_listening(80)
        assert port.is_listening(53)
        assert port.is_listening(53)
        assert port.is_listening(67)
        config['add'] = False
        self.update_config(config)
        assert not ip.has_ip("%s/%s" % (config['router_guest_ip'], config['cidr']), config['device'])


if __name__ == '__main__':
    import unittest
    unittest.main()
