#!/usr/bin/python
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


# Usage: dhcpd_edithosts.py mac ip hostname dns gateway nextserver
import sys, os
from os.path import exists
from time import sleep
from os import remove

usage = '''dhcpd_edithosts.py mac ip hostname dns gateway nextserver'''
conf_path = "/etc/dhcpd.conf"
file_lock = "/etc/dhcpd.conf_locked"
sleep_max = 20
host_entry = 'host %s { hardware ethernet %s; fixed-address %s; option domain-name-servers %s; option domain-name "%s"; option routers %s; default-lease-time infinite; max-lease-time infinite; min-lease-time infinite; filename "pxelinux.0";}'
host_entry1 = 'host %s { hardware ethernet %s; fixed-address %s; option domain-name-servers %s; option domain-name "%s"; option routers %s; default-lease-time infinite; max-lease-time infinite; min-lease-time infinite; next-server %s; filename "pxelinux.0";}'
def lock():
	if exists(file_lock):
		count = 0
		while(exists(file_lock)):
			sleep(1)
			count = count + 1
			if count > sleep_max:
				print "Can not get file lock at %s, time expired" % file_lock
				return False

	try:
		f = open(file_lock, "w")
		f.close()
		return True
	except IOError,e:
		print "Cannot create file lock at /etc/dhcpd.conf_locked,", e
		return False


def unlock():
	if exists(file_lock) == False:
		print "Cannot find %s when unlocking, race condition happens" % file_lock
	else:
		try:
			remove(file_lock)
			return True
		except IOError, e:
			print "Cannot remove file lock at %s" % file_lock
			return False

def insert_host_entry(mac, ip, hostname, dns, gateway, next_server):
	if lock() == False:
		return 1

	cmd = 'sed -i /"fixed-address %s"/d %s' % (ip, conf_path)
	ret = os.system(cmd)
	if ret != 0:
		print "Command %s failed" % cmd
		unlock()
		return 1

	cmd = 'sed -i /"hardware ethernet %s"/d %s' % (mac, conf_path)
	ret = os.system(cmd)
	if ret != 0:
		print "Command %s failed" % cmd
		unlock()
		return 1

	if next_server != "null":
		entry = host_entry1 % (hostname, mac, ip, dns, "cloudnine.internal", gateway, next_server)
	else:
		entry = host_entry % (hostname, mac, ip, dns, "cloudnine.internal", gateway)
	cmd = '''echo '%s' >> %s''' % (entry, conf_path)
	ret = os.system(cmd)
	if ret != 0:
		print "Command %s failed" % cmd
		unlock()
		return 1

	cmd = 'service dhcpd restart'
	ret = os.system(cmd)
	if ret != 0:
		print "Command %s failed" % cmd
		unlock()
		return 1

	if unlock() == False:
		return 1

	return 0

if __name__ == "__main__":
	if len(sys.argv) < 7:
		print usage
		sys.exit(1)

	mac = sys.argv[1]
	ip = sys.argv[2]
	hostname = sys.argv[3]
	dns = sys.argv[4]
	gateway = sys.argv[5]
	next_server = sys.argv[6]

	if exists(conf_path) == False:
		conf_path = "/etc/dhcp/dhcpd.conf"
	if exists(conf_path) == False:
		print "Cannot find dhcpd.conf"
		sys.exit(1)

	ret = insert_host_entry(mac, ip, hostname, dns, gateway, next_server)
	sys.exit(ret)
