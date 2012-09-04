#! /usr/bin/python
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
import socket, struct
import cloud_utils
from cloud_utils import Command
urls = ("/ipallocator", "ipallocator")
app = web.application(urls, globals())

augtool = Command("augtool")
service = Command("service")
class dhcp:
	_instance = None
	def __init__(self):
		self.availIP=[]
		self.router=None
		self.netmask=None
		self.initialized=False

		options = augtool.match("/files/etc/dnsmasq.conf/dhcp-option").stdout.strip()
		for option in options.splitlines():
			if option.find("option:router") != -1:
				self.router = option.split("=")[1].strip().split(",")[1]
				print self.router

		dhcp_range = augtool.get("/files/etc/dnsmasq.conf/dhcp-range").stdout.strip()
		dhcp_start = dhcp_range.split("=")[1].strip().split(",")[0]
		dhcp_end = dhcp_range.split("=")[1].strip().split(",")[1]
		self.netmask = dhcp_range.split("=")[1].strip().split(",")[2]
		print dhcp_start, dhcp_end, self.netmask

 		start_ip_num = self.ipToNum(dhcp_start);
		end_ip_num =  self.ipToNum(dhcp_end)
		print start_ip_num, end_ip_num
	
		for ip in range(start_ip_num, end_ip_num + 1):
			self.availIP.append(ip)	
		print self.availIP[0], self.availIP[len(self.availIP) - 1]	
		
		#load the ip already allocated
		self.reloadAllocatedIP()

	def ipToNum(self, ip):
		return struct.unpack("!I", socket.inet_aton(ip))[0]

	def numToIp(self, num):
		return socket.inet_ntoa(struct.pack('!I', num))

	def getFreeIP(self):
		if len(self.availIP) > 0:
			ip = self.numToIp(self.availIP[0])
			self.availIP.remove(self.availIP[0])	
			return ip
		else:
			return None

	def getNetmask(self):
		return self.netmask

	def getRouter(self):
		return self.router

	def getInstance():
		if not dhcp._instance:
			dhcp._instance = dhcp()
		return dhcp._instance
	getInstance = staticmethod(getInstance)

	def reloadAllocatedIP(self):
		dhcp_hosts = augtool.match("/files/etc/dnsmasq.conf/dhcp-host").stdout.strip().splitlines()
		
		for host in dhcp_hosts:
			if host.find("dhcp-host") != -1:
				allocatedIP = self.ipToNum(host.split("=")[1].strip().split(",")[1])
				if allocatedIP in self.availIP:	
					self.availIP.remove(allocatedIP)
		
	def allocateIP(self, mac):
		newIP = self.getFreeIP()
		dhcp_host = augtool.match("/files/etc/dnsmasq.conf/dhcp-host").stdout.strip()
		cnt = len(dhcp_host.splitlines()) + 1
		script = """set %s %s
			    save"""%("/files/etc/dnsmasq.conf/dhcp-host[" + str(cnt) + "]", str(mac) + "," + newIP)
		augtool < script
		#reset dnsmasq
		service("dnsmasq", "restart", stdout=None, stderr=None)
		return newIP

	def releaseIP(self, ip):
		dhcp_host = augtool.match("/files/etc/dnsmasq.conf/dhcp-host").stdout.strip()
		path = None
		for host in dhcp_host.splitlines():
			if host.find(ip) != -1:
				path = host.split("=")[0].strip()
				
		if path == None:
			print "Can't find " + str(ip) + " in conf file"
			return None

		print path
		script = """rm %s
			    save"""%(path)
		augtool < script
		
		self.availIP.remove(ip)
		
		#reset dnsmasq
		service("dnsmasq", "restart", stdout=None, stderr=None)

class ipallocator:
	def GET(self):
		try:
			user_data = web.input()
			command = user_data.command
			print "Processing: " + command

			dhcpInit = dhcp.getInstance()

			if command == "getIpAddr":
				mac = user_data.mac
				zone_id = user_data.dc
				pod_id = user_data.pod
				print mac, zone_id, pod_id
				freeIP = dhcpInit.allocateIP(mac)
				if not freeIP:
					return "0,0,0"
				print "Find an available IP: " + freeIP
		
				return freeIP + "," + dhcpInit.getNetmask() + "," + dhcpInit.getRouter()
			elif command == "releaseIpAddr":
				ip = user_data.ip
				zone_id = user_data.dc
				pod_id = user_data.pod
				dhcpInit.releaseIP(ip)
		except:
			return None

if __name__ == "__main__":
	app.run()
