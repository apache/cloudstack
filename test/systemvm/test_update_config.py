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
                "new_nic": False
            }
        ],
        "type": "ips"
    }

    def update_config(self, config):
        config_json = json.dumps(config, indent=2)
        print_doc('config.json', config_json)
        file_write('/etc/cloudstack/update_config_test.json', config_json)
        with hide("everything"):
            result = run("python /opt/cloud/bin/update_config.py update_config_test.json",
                         timeout=600, warn_only=True)
            print result
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
        r = random.Random()
        r.seed()
        for i in range(0, 10):
            # todo need to know what kind of configurations are valid!
            config = deep_copy(self.basic_config)
            ip_address = deep_copy(self.basic_config["ip_address"][0])
            ip_address["public_ip"] = "10.0.2.%d" % (i + 103,)
            ip_address["source_nat"] = r.choice((True, False))
            ip_address["add"] = r.choice((True, False))
            ip_address["one_to_one_nat"] = r.choice((True, False))
            ip_address["first_i_p"] = r.choice((True, False))
            ip_address["nic_dev_id"] = r.choice((2, 3))
            if ip_address["nic_dev_id"] > 0:
                ip_address["new_nic"] = True
            else:
                ip_address["new_nic"] = False
            config["ip_address"].append(ip_address)
            # runs a bunch of times adding an IP address each time
            self.update_config(config)
            self.check_no_errors()
            self.clear_log()
        # run again with just the basic config; this should remove the IP addresses?
        self.update_config(self.basic_config)


if __name__ == '__main__':
    import unittest
    unittest.main()
