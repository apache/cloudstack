#!/usr/bin/python



  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
  # 
  # This software is licensed under the GNU General Public License v3 or later.
  # 
  # It is free software: you can redistribute it and/or modify
  # it under the terms of the GNU General Public License as published by
  # the Free Software Foundation, either version 3 of the License, or any later version.
  # This program is distributed in the hope that it will be useful,
  # but WITHOUT ANY WARRANTY; without even the implied warranty of
  # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  # GNU General Public License for more details.
  # 
  # You should have received a copy of the GNU General Public License
  # along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #
 


import sys, os, subprocess, errno, re
from os.path import exists

TOOl_PATH = "/usr/bin/ipmitool"

try:
	from subprocess import check_call
	from subprocess import CalledProcessError
except ImportError:
	def check_call(*popenargs, **kwargs):
		import subprocess
		retcode = subprocess.call(*popenargs, **kwargs)
		cmd = kwargs.get("args")
		if cmd is None: cmd = popenargs[0]
		if retcode: raise CalledProcessError(retcode, cmd)
		return retcode

	class CalledProcessError(Exception):
		def __init__(self, returncode, cmd):
			self.returncode = returncode ; self.cmd = cmd
		def __str__(self): return "Command '%s' returned non-zero exit status %d" % (self.cmd, self.returncode)

class Command:
	def __init__(self,name,parent=None):
		self.__name = name
		self.__parent = parent
	def __getattr__(self,name):
		if name == "_print": name = "print"
		return Command(name,self)
	def __call__(self,*args):
		class CommandOutput:
			def __init__(self,ret,stdout,stderr):
				self.stdout = stdout
				self.stderr = stderr
				self.ret = ret

		cmd = self.__get_recursive_name() + list(args)
		#print "	",cmd
		popen = subprocess.Popen(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
		m = popen.communicate()
		ret = popen.wait()
		return CommandOutput(ret,*m)
	def __get_recursive_name(self,sep=None):
		m = self
		l = []
		while m is not None:
			l.append(m.__name)
			m = m.__parent
		l.reverse()
		if sep: return sep.join(l)
		else: return l
	def __str__(self):
		return '<Command %r>'%self.__get_recursive_name(sep=" ")
		
	def __repr__(self): return self.__str__()


ipmitool = Command("ipmitool")

def check_tool():
	if exists(TOOl_PATH) == False:
		print "Can not find ipmitool"
		return False

def ping(args):
	hostname = args.get("hostname")
	usrname = args.get("usrname")
	password = args.get("password")

	if hostname == None:
		print "No hostname"
		return 1

	o = ipmitool("-H", hostname, "-U", usrname, "-P", password, "chassis", "power", "status")
	if o.ret:
		print o.stderr
		return 1
	else:
		print o.stdout
		return 0

def boot_dev(args):
	hostname = args.get("hostname")
	usrname = args.get("usrname")
	password = args.get("password")
	dev = args.get("dev")

	if hostname == None:
		print "No hostname"
		return 1

	if dev == None:
		print "No boot device specified"
		return 1

	o = ipmitool("-H", hostname, "-U", usrname, "-P", password, "chassis", "bootdev", dev)
	if o.ret:
		print o.stderr
		return 1
	else:
		return 0

def reboot(args):
	hostname = args.get("hostname")
	usrname = args.get("usrname")
	password = args.get("password")

	if hostname == None:
		print "No hostname"
		return 1

	o = ipmitool("-H", hostname, "-U", usrname, "-P", password, "chassis", "power", "reset")
	if o.ret:
		print o.stderr
		return 1
	else:
		return 0

def power(args):
	hostname = args.get("hostname")
	usrname = args.get("usrname")
	password = args.get("password")
	action = args.get("action")

	if hostname == None:
		print "No hostname"
		return 1

	o = ipmitool("-H", hostname, "-U", usrname, "-P", password, "chassis", "power", action)
	if o.ret:
		print o.stderr
		return 1
	else:
		return 0

call_table = {"ping":ping, "boot_dev":boot_dev, "reboot":reboot, "power":power}
def dispatch(args):
	cmd = args[1]
	params = args[2:]
	func_params = {}

	if call_table.has_key(cmd) == False:
		print "No function %s" % cmd
		return 1

	for p in params:
		pairs = p.split("=")
		if len(pairs) != 2:
			print "Invalid parameter %s" % p
			return 1
		func_params[pairs[0]] = pairs[1]

	func = call_table[cmd]
	return func(func_params)

if __name__ == "__main__":
	if check_tool() == False:
		sys.exit(1)
	if len(sys.argv) < 2:
		print "Not enough arguments, at least one"
		sys.exit(1)
	sys.exit(dispatch(sys.argv))
