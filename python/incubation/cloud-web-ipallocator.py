#!/usr/bin/env python3
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

import web
import socket
import struct
import cloud_utils
from cloud_utils import Command

# Constants
DNSMASQ_SERVICE = "dnsmasq"
DNSMASQ_CONFIG = "/files/etc/dnsmasq.conf"
RESTART = "restart"

# URL mapping
urls = ("/ipallocator", "ipallocator")
app = web.application(urls, globals())

# Command wrappers
augtool = Command("augtool")
service = Command("service")

class dhcp:
    _instance = None

    def __init__(self):
        self.availIP = []
        self.router = None
        self.netmask = None
        self.initialized = False

        # Load DHCP options
        options = augtool.match(f"{DNSMASQ_CONFIG}/dhcp-option").stdout.decode('utf-8').strip()
        for option in options.splitlines():
            if "option:router" in option:
                self.router = option.split("=")[1].strip().split(",")[1]
                print(self.router)

        # Load DHCP range
        dhcp_range = augtool.get(f"{DNSMASQ_CONFIG}/dhcp-range").stdout.decode('utf-8').strip()
        dhcp_start = dhcp_range.split("=")[1].strip().split(",")[0]
        dhcp_end = dhcp_range.split("=")[1].strip().split(",")[1]
        self.netmask = dhcp_range.split("=")[1].strip().split(",")[2]
        print(dhcp_start, dhcp_end, self.netmask)

        # Convert IP range to numbers
        start_ip_num = self.ipToNum(dhcp_start)
        end_ip_num = self.ipToNum(dhcp_end)
        print(start_ip_num, end_ip_num)

        # Populate available IPs
        for ip in range(start_ip_num, end_ip_num + 1):
            self.availIP.append(ip)
        print(self.availIP[0], self.availIP[-1])

        # Load already allocated IPs
        self.reloadAllocatedIP()

    def ipToNum(self, ip):
        """Convert IP address string to a number."""
        return struct.unpack("!I", socket.inet_aton(ip))[0]

    def numToIp(self, num):
        """Convert a number back to an IP address."""
        return socket.inet_ntoa(struct.pack('!I', num))

    def getFreeIP(self):
        """Return the next available IP address."""
        if len(self.availIP) > 0:
            ip = self.numToIp(self.availIP.pop(0))
            return ip
        else:
            return None

    def getNetmask(self):
        return self.netmask

    def getRouter(self):
        return self.router

    @staticmethod
    def getInstance():
        if not dhcp._instance:
            dhcp._instance = dhcp()
        return dhcp._instance

    def reloadAllocatedIP(self):
        """Reload already allocated IPs from the config file."""
        dhcp_hosts = augtool.match(f"{DNSMASQ_CONFIG}/dhcp-host").stdout.decode('utf-8').strip().splitlines()
        for host in dhcp_hosts:
            if "dhcp-host" in host:
                allocatedIP = self.ipToNum(host.split("=")[1].strip().split(",")[1])
                if allocatedIP in self.availIP:
                    self.availIP.remove(allocatedIP)

    def allocateIP(self, mac):
        """Allocate an IP address to the given MAC address."""
        newIP = self.getFreeIP()
        dhcp_host = augtool.match(f"{DNSMASQ_CONFIG}/dhcp-host").stdout.decode('utf-8').strip()
        cnt = len(dhcp_host.splitlines()) + 1
        script = f"""set {DNSMASQ_CONFIG}/dhcp-host[{cnt}] {mac},{newIP}
                     save"""
        with open("/path/to/script", "w") as script_file:
            script_file.write(script)
        augtool < script_file

        # Restart dnsmasq service
        service(DNSMASQ_SERVICE, RESTART, stdout=None, stderr=None)
        return newIP

    def releaseIP(self, ip):
        """Release the given IP address and remove it from the config."""
        dhcp_host = augtool.match(f"{DNSMASQ_CONFIG}/dhcp-host").stdout.decode('utf-8').strip()
        path = None
        for host in dhcp_host.splitlines():
            if ip in host:
                path = host.split("=")[0].strip()

        if not path:
            print(f"Can't find {ip} in conf file")
            return None

        script = f"rm {path}\n save"
        with open("/path/to/script", "w") as script_file:
            script_file.write(script)
        augtool < script_file

        # Remove from available IPs
        self.availIP.append(self.ipToNum(ip))

        # Restart dnsmasq service
        service(DNSMASQ_SERVICE, RESTART, stdout=None, stderr=None)

class ipallocator:
    def GET(self):
        try:
            user_data = web.input()
            command = user_data.command
            print(f"Processing: {command}")

            dhcpInit = dhcp.getInstance()

            if command == "getIpAddr":
                mac = user_data.mac
                zone_id = user_data.dc
                pod_id = user_data.pod
                print(mac, zone_id, pod_id)
                freeIP = dhcpInit.allocateIP(mac)
                if not freeIP:
                    return "0,0,0"
                print(f"Find an available IP: {freeIP}")
                return f"{freeIP},{dhcpInit.getNetmask()},{dhcpInit.getRouter()}"

            elif command == "releaseIpAddr":
                ip = user_data.ip
                zone_id = user_data.dc
                pod_id = user_data.pod
                dhcpInit.releaseIP(ip)

        except Exception as e:
            print(f"Error: {e}")
            return None

if __name__ == "__main__":
    app.run()
