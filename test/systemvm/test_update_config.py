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
import subprocess
from envassert import file, process, package, user, group, port, cron, detect, ip
import copy
from fabric import state

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

    basic_acl = {
        "device":"eth2",
        "mac_address":"02:00:5d:8d:00:03",
        "private_gateway_acl":False,
        "nic_ip":"172.16.1.1",
        "nic_netmask":"24",
        "ingress_rules":
        [
            {"type":"all",
            "cidr":"0.0.0.0/0",
            "allowed":False}
        ],
        "egress_rules":
        [   
            {"type":"all",
            "cidr":"0.0.0.0/0",
            "allowed":False}
        ],
        "type":"networkacl"
    }

    basic_dhcp_entry = {
        "host_name":"VM-58976c22-0832-451e-9ab2-039e9f27e415",
        "mac_address":"02:00:26:c3:00:02",
        "ipv4_adress":"172.16.1.102",
        "ipv6_duid":"00:03:00:01:02:00:26:c3:00:02",
        "default_gateway":"172.16.1.1",
        "default_entry":True,
        "type":"dhcpentry"
    }

    basic_network_acl = {
        "device":"eth2",
        "mac_address":"02:00:5d:8d:00:03",
        "private_gateway_acl":False,
        "nic_ip":"172.16.1.1",
        "nic_netmask":"24",
        "ingress_rules":
        [ ],
        "egress_rules":
        [ ],
        "type":"networkacl"
    }

    basic_acl_rules = [
        # block range tcp
        {
            "allowed": False, 
            "cidr": "1.2.3.0/24", 
            "first_port": 60, 
            "last_port": 70, 
            "type": "tcp"
        },
        # block range udp
        {
            "allowed": False, 
            "cidr": "1.2.3.0/24", 
            "first_port": 60, 
            "last_port": 70, 
            "type": "udp"
        },
        # ipv6
        {
            "allowed": True, 
            "cidr": "1.2.3.0/24", 
            "protocol": 41, 
            "type": "protocol"
        }, 
        # Single port
        {
            "allowed": True, 
            "cidr": "1.2.3.0/24", 
            "first_port": 30, 
            "last_port": 30, 
            "type": "tcp"
        },
        # Icmp
        {
            "allowed": True, 
            "cidr": "10.0.0.0/8", 
            "icmp_code": -1, 
            "icmp_type": -1, 
            "type": "icmp"
        }
    ]

    def redundant(self, what):
        with hide("everything"):
            result = run("python /opt/cloud/bin/set_redundant.py %s" % what,
                         timeout=600, warn_only=True)
            assert result.succeeded, 'Set redundancy to %s' % what

    def configure(self):
        with hide("everything"):
            result = run("python /opt/cloud/bin/configure.py",
                         timeout=600, warn_only=True)
            assert result.succeeded, "Configure ran"

    def update_config(self, config):
        config_json = json.dumps(config, indent=2)
        #print_doc('config.json', config_json)
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
            #print_doc('/var/log/cloud.log', context)
            pass
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
            ip_address["nic_dev_id"] = r.choice((2,3))
            config["ip_address"].append(ip_address)
            # runs a bunch of times adding an IP address each time
            self.update_config(config)
            ip_address["add"] = False
            buffer.append(copy.deepcopy(ip_address))
            self.check_no_errors()
            self.clear_log()
            assert ip.has_ip("%s/24" % ip_address["public_ip"], "eth%s" % ip_address["nic_dev_id"]), \
                    "Configure %s on eth%s failed" % (ip_address["public_ip"], ip_address["nic_dev_id"])
        # Create some acls for the IPs we just created
        # This will lead to multiple attempts to add the same acl - *this is intentional*
        self.check_acl(buffer)
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
        config['add'] = False
        # Clear up from any failed test runs
        self.update_config(config)
        config['add'] = True
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

    def check_acl(self, list):
        clear1 = self.clear_all_acls()
        clear2 = self.clear_all_acls()
        assert clear1 == clear2, "Clear all acls called twice and produced different results"
        unique = {}

        # How many unique devices
        for ips in list:
            unique["eth%s" % ips["nic_dev_id"]] = 1

        # If this is the first run, the drops will not be there yet
        # this is so I can get get a true count of what is explicitly added
        drops = len(unique)
        for dev in unique:
            drops -= ip.count_fw_rules('ACL_INBOUND_%s -j DROP' % dev)

        for ips in list:
            config = copy.deepcopy(self.basic_network_acl)
            config['device'] = "eth%s" % ips["nic_dev_id"]
            config['nic_ip'] = ips["public_ip"]
            for rule in self.basic_acl_rules:
                config['ingress_rules'].append(rule)
                config['egress_rules'].append(rule)
            self.update_config(config)

        # Check the default drop rules are there
        for dev in unique:
            drop = ip.count_fw_rules('ACL_INBOUND_%s -j DROP' % dev)
            assert drop == 1, "ACL_INBOUND_%s does not have a default drop rule" % dev

        after = ip.count_fw_rules()
        # How many new acls should we get?
        # The number of rules * the number of devices * 2 (in and out)
        expected = len(unique) * 2 * len(self.basic_acl_rules) + clear2 + drops
        assert expected == after, "Number of acl rules does not match what I expected to see"
        for dev in range(6):
            config = copy.deepcopy(self.basic_network_acl)
            config['device'] = "eth%s" % dev
            self.update_config(config)
        clear2 = self.clear_all_acls() - drops
        assert clear1 == clear2, "Clear all acls appears to have failed"

    def clear_all_acls(self):
        for dev in range(6):
            config = copy.deepcopy(self.basic_network_acl)
            config['device'] = "eth%s" % dev
            self.update_config(config)
        return ip.count_fw_rules()

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
        vpn_config = {
            "local_public_ip": config['router_guest_ip'],
            "local_guest_cidr":"%s/%s" % (config['router_guest_gateway'], config['cidr']),
            "local_public_gateway":"172.16.1.1",
            "peer_gateway_ip":"10.200.200.1",
            "peer_guest_cidr_list":"10.0.0.0/24",
            "esp_policy":"3des-md5",
            "ike_policy":"3des-md5",
            "ipsec_psk":"vpnblabla",
            "ike_lifetime":86400,
            "esp_lifetime":3600,
            "create":True,
            "dpd":False,
            "passive":False,
            "type":"site2sitevpn"
        }
        octets = config['router_guest_ip'].split('.')
        configs = []

        # This should fail because the network does not yet exist
        self.update_config(vpn_config)
        assert not file.exists("/etc/ipsec.d/ipsec.vpn-%s.conf" % vpn_config['peer_gateway_ip'])

        self.update_config(config)
        self.update_config(vpn_config)
        assert ip.has_ip("%s/%s" % (config['router_guest_ip'], config['cidr']), config['device'])
        assert process.is_up("apache2"), "Apache2 should be running after adding a guest network"
        assert process.is_up("dnsmasq"), "Dnsmasq should be running after adding a guest network"

        assert file.exists("/etc/ipsec.d/ipsec.vpn-%s.conf" % vpn_config['peer_gateway_ip'])
        assert file.mode_is("/etc/ipsec.d/ipsec.vpn-%s.secrets" % vpn_config['peer_gateway_ip'], "400")
        result = run("/usr/sbin/ipsec setup status", timeout=600, warn_only=True)
        assert result.succeeded, 'ipsec returned non zero status %s' % config['router_guest_ip']
		# Add a host to the dhcp server
		# This must happen in order for dnsmasq to be listening
        for n in range(3,13):
            ipb = ".".join(octets[0:3])
            ipa = "%s.%s" % (ipb, n)
            gw = "%s.1" % ipb
            self.basic_dhcp_entry['ipv4_adress'] =  ipa
            self.basic_dhcp_entry['default_gateway'] =  gw
            self.basic_dhcp_entry['host_name'] =  "host_%s" % (ipa)
            self.update_config(self.basic_dhcp_entry)
            configs.append(copy.deepcopy(self.basic_dhcp_entry))
        assert port.is_listening(80)
        assert port.is_listening(53)
        assert port.is_listening(53)
        assert port.is_listening(67)
        for o in configs:
            line = "%s,%s,%s,infinite" % (o['mac_address'], o['ipv4_adress'], o['host_name'])
            assert file.has_line("/etc/dhcphosts.txt", line)
        config['add'] = False
        self.update_config(config)
        assert not ip.has_ip("%s/%s" % (config['router_guest_ip'], config['cidr']), config['device'])
        # Now setup what we have redundant
        self.redundant("-e")
        self.configure()
        assert process.is_up("keepalived"), "Keepalived should be running after enabling redundancy"
        assert process.is_up("conntrackd"), "Conntrackd should be running after enabling redundancy"
        self.redundant("-d")
        self.configure()
        assert not process.is_up("keepalived"), "Keepalived should be not running after disabling redundancy"
        assert not process.is_up("conntrackd"), "Conntrackd should be not running after disabling redundancy"
        for o in configs:
            o['add'] = False
            self.update_config(o)
        for o in configs:
            line = "%s,%s,%s,infinite" % (o['mac_address'], o['ipv4_adress'], o['host_name'])
            assert file.has_line("/etc/dhcphosts.txt", line) is False
        # If the network gets deleted so should the vpn
        assert not file.exists("/etc/ipsec.d/ipsec.vpn-%s.conf" % vpn_config['peer_gateway_ip'])

if __name__ == '__main__':
    unittest.main()
