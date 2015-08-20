#!/usr/bin/env python
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



 

# -*- coding: utf-8 -*-
"""CloudStack Python utility library"""

import sys, os, subprocess, errno, re, time, glob
import urllib2
import xml.dom.minidom
import logging
import socket

# exit() error constants
E_GENERIC= 1
E_NOKVM = 2
E_NODEFROUTE = 3
E_DHCP = 4
E_NOPERSISTENTNET = 5
E_NETRECONFIGFAILED = 6
E_VIRTRECONFIGFAILED = 7
E_FWRECONFIGFAILED = 8
E_AGENTRECONFIGFAILED = 9
E_AGENTFAILEDTOSTART = 10
E_NOFQDN = 11
E_SELINUXENABLED = 12
try: E_USAGE = os.EX_USAGE
except AttributeError: E_USAGE = 64

E_NEEDSMANUALINTERVENTION = 13
E_INTERRUPTED = 14
E_SETUPFAILED = 15
E_UNHANDLEDEXCEPTION = 16
E_MISSINGDEP = 17

Unknown = 0
Fedora = 1
CentOS = 2
Ubuntu = 3
RHEL6 = 4

IPV4 = 4
IPV6 = 6

#=================== DISTRIBUTION DETECTION =================

if os.path.exists("/etc/fedora-release"): distro = Fedora
elif os.path.exists("/etc/centos-release"): distro = CentOS
elif os.path.exists("/etc/redhat-release"):
    version = file("/etc/redhat-release").readline()
    if version.find("Red Hat Enterprise Linux Server release 6") != -1:
        distro = RHEL6
    elif version.find("CentOS release") != -1:
        distro = CentOS
    else:
        distro = CentOS
elif os.path.exists("/etc/legal") and "Ubuntu" in file("/etc/legal").read(-1): distro = Ubuntu
else: distro = Unknown
logFileName=None
# ==================  LIBRARY UTILITY CODE=============
def setLogFile(logFile):
	global logFileName
	logFileName=logFile
def read_properties(propfile):
	if not hasattr(propfile,"read"): propfile = file(propfile)
	properties = propfile.read().splitlines()
	properties = [ s.strip() for s in properties ]
	properties = [ s for s in properties if
			s and
			not s.startswith("#") and
			not s.startswith(";") ]
	#[ logging.debug("Valid config file line: %s",s) for s in properties ]
	proppairs = [ s.split("=",1) for s in properties ]
	return dict(proppairs)

def stderr(msgfmt,*args):
	"""Print a message to stderr, optionally interpolating the arguments into it"""
	msgfmt += "\n"
	if logFileName != None:
		sys.stderr = open(logFileName, 'a+')
	if args: sys.stderr.write(msgfmt%args)
	else: sys.stderr.write(msgfmt)

def exit(errno=E_GENERIC,message=None,*args):
	"""Exit with an error status code, printing a message to stderr if specified"""
	if message: stderr(message,*args)
	sys.exit(errno)

def resolve(host,port):
	return [ (x[4][0],len(x[4])+2) for x in socket.getaddrinfo(host,port,socket.AF_UNSPEC,socket.SOCK_STREAM, 0, socket.AI_PASSIVE) ]
	
def resolves_to_ipv6(host,port):
	return resolve(host,port)[0][1] == IPV6

###add this to Python 2.4, patching the subprocess module at runtime
if hasattr(subprocess,"check_call"):
	from subprocess import CalledProcessError, check_call
else:
	class CalledProcessError(Exception):
		def __init__(self, returncode, cmd):
			self.returncode = returncode ; self.cmd = cmd
		def __str__(self): return "Command '%s' returned non-zero exit status %d" % (self.cmd, self.returncode)
	subprocess.CalledProcessError = CalledProcessError
	
	def check_call(*popenargs, **kwargs):
		retcode = subprocess.call(*popenargs, **kwargs)
		cmd = kwargs.get("args")
		if cmd is None: cmd = popenargs[0]
		if retcode: raise subprocess.CalledProcessError(retcode, cmd)
		return retcode
	subprocess.check_call = check_call

# python 2.4 does not have this
try:
	any = any
	all = all
except NameError:
	def any(sequence):
		for i in sequence:
			if i: return True
		return False
	def all(sequence):
		for i in sequence:
			if not i: return False
		return True

class Command:
	"""This class simulates a shell command"""
	def __init__(self,name,parent=None):
		self.__name = name
		self.__parent = parent
	def __getattr__(self,name):
		if name == "_print": name = "print"
		return Command(name,self)
	def __call__(self,*args,**kwargs):
		cmd = self.__get_recursive_name() + list(args)
		#print "	",cmd
		kwargs = dict(kwargs)
		if "stdout" not in kwargs: kwargs["stdout"] = subprocess.PIPE
		if "stderr" not in kwargs: kwargs["stderr"] = subprocess.PIPE
		popen = subprocess.Popen(cmd,**kwargs)
		m = popen.communicate()
		ret = popen.wait()
		if ret:
			e = CalledProcessError(ret,cmd)
			e.stdout,e.stderr = m
			raise e
		class CommandOutput:
			def __init__(self,stdout,stderr):
				self.stdout = stdout
				self.stderr = stderr
		return CommandOutput(*m)
	def __lt__(self,other):
		cmd = self.__get_recursive_name()
		#print "	",cmd,"<",other
		popen = subprocess.Popen(cmd,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
		m = popen.communicate(other)
		ret = popen.wait()
		if ret:
			e = CalledProcessError(ret,cmd)
			e.stdout,e.stderr = m
			raise e
		class CommandOutput:
			def __init__(self,stdout,stderr):
				self.stdout = stdout
				self.stderr = stderr
		return CommandOutput(*m)
		
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

kvmok = Command("kvm-ok")
getenforce = Command("/usr/sbin/getenforce")
ip = Command("ip")
service = Command("service")
chkconfig = Command("chkconfig")
updatercd = Command("update-rc.d")
ufw = Command("ufw")
iptables = Command("iptables")
iptablessave = Command("iptables-save")
augtool = Command("augtool")
ifconfig = Command("ifconfig")
ifdown = Command("ifdown")
ifup = Command("ifup")
brctl = Command("brctl")
uuidgen = Command("uuidgen")


def is_service_running(servicename):
	try:
		o = service(servicename,"status")
		if distro is Ubuntu:
			# status in ubuntu does not signal service status via return code
			if "start/running" in o.stdout: return True
			return False
		else:
			# retcode 0, service running
			return True
	except CalledProcessError,e:
		# retcode nonzero, service not running
		return False


def stop_service(servicename,force=False):
	# This function is idempotent.  N number of calls have the same result as N+1 number of calls.
	if is_service_running(servicename) or force: service(servicename,"stop",stdout=None,stderr=None)


def disable_service(servicename):
	# Stops AND disables the service
	stop_service(servicename)
	if distro is Ubuntu:
		updatercd("-f",servicename,"remove",stdout=None,stderr=None)
	else:
		chkconfig("--del",servicename,stdout=None,stderr=None)


def start_service(servicename,force=False):
	# This function is idempotent unless force is True.  N number of calls have the same result as N+1 number of calls.
	if not is_service_running(servicename) or force: service(servicename,"start",stdout=None,stderr=None)


def enable_service(servicename,forcestart=False):
	# Stops AND disables the service
	if distro is Ubuntu:
		updatercd("-f",servicename,"remove",stdout=None,stderr=None)
		updatercd("-f",servicename,"start","2","3","4","5",".",stdout=None,stderr=None)
	else:
		chkconfig("--add",servicename,stdout=None,stderr=None)
		chkconfig("--level","345",servicename,"on",stdout=None,stderr=None)
	start_service(servicename,force=forcestart)


def replace_line(f,startswith,stanza,always_add=False):
	lines = [ s.strip() for s in file(f).readlines() ]
	newlines = []
	replaced = False
	for line in lines:
		if line.startswith(startswith):
			newlines.append(stanza)
			replaced = True
		else: newlines.append(line)
	if not replaced and always_add: newlines.append(stanza)
	newlines = [ s + '\n' for s in newlines ]
	file(f,"w").writelines(newlines)

def replace_or_add_line(f,startswith,stanza):
	return replace_line(f,startswith,stanza,always_add=True)
	
# ==================================== CHECK FUNCTIONS ==========================

# If they return without exception, it's okay.  If they raise a CheckFailed exception, that means a condition
# (generallly one that needs administrator intervention) was detected.

class CheckFailed(Exception): pass

#check function
def check_hostname():
	"""If the hostname is a non-fqdn, fail with CalledProcessError.  Else return 0."""
	try: check_call(["hostname",'--fqdn'])
	except CalledProcessError:
		raise CheckFailed("This machine does not have an FQDN (fully-qualified domain name) for a hostname")

#check function
def check_kvm():
	if distro in (Fedora,CentOS,RHEL6):
		if os.path.exists("/dev/kvm"): return True
		raise CheckFailed("KVM is not correctly installed on this system, or support for it is not enabled in the BIOS")
	else:
		try:
			kvmok()
			return True
		except CalledProcessError:
			raise CheckFailed("KVM is not correctly installed on this system, or support for it is not enabled in the BIOS")
		except OSError,e:
			if e.errno is errno.ENOENT: raise CheckFailed("KVM is not correctly installed on this system, or support for it is not enabled in the BIOS")
			raise
		return True
	raise AssertionError, "check_kvm() should have never reached this part"

def check_cgroups():
	return glob.glob("/*/cpu.shares")

#check function
def check_selinux():
	if distro not in [Fedora,CentOS,RHEL6]: return # no selinux outside of those
	enforcing = False
	config_enforcing = False
	try:
		output = getenforce().stdout.strip()
		if "nforcing" in output:
			enforcing = True
		if any ( [ s.startswith("SELINUX=enforcing") for s in file("/etc/selinux/config").readlines() ] ):
			config_enforcing = True
		else:
			config_enforcing = False
	except (IOError,OSError),e:
		if e.errno == 2: pass
		else: raise CheckFailed("An unknown error (%s) took place while checking for SELinux"%str(e))
	if enforcing:
		raise CheckFailed('''SELinux is set to enforcing. There are two options:
1> Set it permissive in /etc/selinux/config, then reboot the machine.
2> Type 'setenforce Permissive' in commandline, after which you can run this program again.

We strongly suggest you doing the option 1 that makes sure SELinux goes into permissive after system reboot.\n''')

	if config_enforcing:
		print "WARNING: We detected that your SELinux is not configured in permissive. to make sure cloudstack won't block by \
SELinux after system reboot, we strongly suggest you setting it in permissive in /etc/selinux/config, then reboot the machine."


def preflight_checks(do_check_kvm=True):
	if distro is Ubuntu:
		preflight_checks = [
			(check_hostname,"Checking hostname"),
		]
	else:
		preflight_checks = [
			(check_hostname,"Checking hostname"),
			(check_selinux,"Checking if SELinux is disabled"),
		]
	#preflight_checks.append( (check_cgroups,"Checking if the control groups /cgroup filesystem is mounted") )
	if do_check_kvm: preflight_checks.append( (check_kvm,"Checking for KVM") )
	return preflight_checks


# ========================== CONFIGURATION TASKS ================================

# A Task is a function that runs within the context of its run() function that runs the function execute(), which does several things, reporting back to the caller as it goes with the use of yield
# the done() method ought to return true if the task has run in the past
# the execute() method must implement the configuration act itself
# run() wraps the output of execute() within a Starting taskname and a Completed taskname message
# tasks have a name

class TaskFailed(Exception): pass
	#def __init__(self,code,msg):
		#Exception.__init__(self,msg)
		#self.code = code

class ConfigTask:
	name = "generic config task"
	autoMode=False
	def __init__(self): pass
	def done(self):
		"""Returns true if the config task has already been done in the past, false if it hasn't"""
		return False
	def execute(self):
		"""Executes the configuration task.  Must not be run if test() returned true.
		Must yield strings that describe the steps in the task.
		Raises TaskFailed if the task failed at some step.
		"""
	def run (self):
		stderr("Starting %s"%self.name)
		it = self.execute()
		if not it:
			pass # not a yielding iterable
		else:
			for msg in it: stderr(msg)
		stderr("Completed %s"%self.name)
	def setAutoMode(self, autoMode):
		self.autoMode = autoMode
	def  isAutoMode(self):
		return self.autoMode


# ============== these are some configuration tasks ==================

class SetupNetworking(ConfigTask):
	name = "network setup"
	def __init__(self,brname, pubNic, prvNic):
		ConfigTask.__init__(self)
		self.brname = brname
  	        self.pubNic = pubNic
		self.prvNic = prvNic
		self.runtime_state_changed = False
		self.was_nm_service_running = None
		self.was_net_service_running = None
		if distro in (Fedora, CentOS, RHEL6):
			self.nmservice = 'NetworkManager'
			self.netservice = 'network'
		else:
			self.nmservice = 'network-manager'
			self.netservice = 'networking'
		
		
	def done(self):
		try:
			alreadysetup = False
			if distro in (Fedora,CentOS, RHEL6):
				if self.pubNic != None:
					alreadysetup = alreadysetup or augtool._print("/files/etc/sysconfig/network-scripts/ifcfg-%s"%self.pubNic).stdout.strip()
				if self.prvNic != None:
					alreadysetup = alreadysetup or augtool._print("/files/etc/sysconfig/network-scripts/ifcfg-%s"%self.prvNic).stdout.strip()
				if not alreadysetup:
					alreadysetup = augtool._print("/files/etc/sysconfig/network-scripts/ifcfg-%s"%self.brname).stdout.strip()
				
			else:
				if self.pubNic != None:
					alreadysetup = alreadysetup or augtool._print("/files/etc/network/interfaces/iface",self.pubNic).stdout.strip()
				if self.prvNic != None:
					alreadysetup = alreadysetup or augtool._print("/files/etc/network/interfaces/iface",self.prvNic).stdout.strip()
				if not alreadysetup:
					alreadysetup = augtool.match("/files/etc/network/interfaces/iface",self.brname).stdout.strip()
			return alreadysetup
		except OSError,e:
			if e.errno is 2: raise TaskFailed("augtool has not been properly installed on this system")
			raise

	def restore_state(self):
		if not self.runtime_state_changed: return
		
		try:
			o = ifconfig(self.brname)
			bridge_exists = True
		except CalledProcessError,e:
			print e.stdout + e.stderr
			bridge_exists = False
			
		if bridge_exists:
			ifconfig(self.brname,"0.0.0.0")
			if hasattr(self,"old_net_device"):
				ifdown(self.old_net_device)
				ifup(self.old_net_device)
			try: ifdown(self.brname)
			except CalledProcessError: pass
			try: ifconfig(self.brname,"down")
			except CalledProcessError: pass
			try: brctl("delbr",self.brname)
			except CalledProcessError: pass
			try: ifdown("--force",self.brname)
			except CalledProcessError: pass
		
		
		if self.was_net_service_running is None:
			# we do nothing
			pass
		elif self.was_net_service_running == False:
			stop_service(self.netservice,force=True)
			time.sleep(1)
		else:
			# we altered service configuration
			stop_service(self.netservice,force=True)
			time.sleep(1)
			try: start_service(self.netservice,force=True)
			except CalledProcessError,e:
				if e.returncode == 1: pass
				else: raise
			time.sleep(1)
		
		if self.was_nm_service_running is None:
			 # we do nothing
			 pass
		elif self.was_nm_service_running == False:
			stop_service(self.nmservice,force=True)
			time.sleep(1)
		else:
			# we altered service configuration
			stop_service(self.nmservice,force=True)
			time.sleep(1)
			start_service(self.nmservice,force=True)
			time.sleep(1)
		
		self.runtime_state_changed = False

	def execute(self):
		yield "Determining default route"
		routes = ip.route().stdout.splitlines()
		defaultroute = [ x for x in routes if x.startswith("default") ]
		if not defaultroute: raise TaskFailed("Your network configuration does not have a default route")
		
		dev = defaultroute[0].split()[4]
		yield "Default route assigned to device %s"%dev
		
		self.old_net_device = dev
		
		if distro in (Fedora, CentOS, RHEL6):
			inconfigfile = "/".join(augtool.match("/files/etc/sysconfig/network-scripts/*/DEVICE",dev).stdout.strip().split("/")[:-1])
			if not inconfigfile: raise TaskFailed("Device %s has not been set up in /etc/sysconfig/network-scripts"%dev)
			pathtoconfigfile = inconfigfile[6:]

		if distro in (Fedora, CentOS, RHEL6):
			automatic = augtool.match("%s/ONBOOT"%inconfigfile,"yes").stdout.strip()
		else:
			automatic = augtool.match("/files/etc/network/interfaces/auto/*/",dev).stdout.strip()
		if not automatic:
			if distro is Fedora: raise TaskFailed("Device %s has not been set up in %s as automatic on boot"%dev,pathtoconfigfile)
			else: raise TaskFailed("Device %s has not been set up in /etc/network/interfaces as automatic on boot"%dev)
			
		if distro not in (Fedora , CentOS, RHEL6):
			inconfigfile = augtool.match("/files/etc/network/interfaces/iface",dev).stdout.strip()
			if not inconfigfile: raise TaskFailed("Device %s has not been set up in /etc/network/interfaces"%dev)

		if distro in (Fedora, CentOS, RHEL6):
			isstatic = augtool.match(inconfigfile + "/BOOTPROTO","none").stdout.strip()
			if not isstatic: isstatic = augtool.match(inconfigfile + "/BOOTPROTO","static").stdout.strip()
		else:
			isstatic = augtool.match(inconfigfile + "/method","static").stdout.strip()
		if not isstatic:
			if distro in (Fedora, CentOS, RHEL6): raise TaskFailed("Device %s has not been set up as a static device in %s"%(dev,pathtoconfigfile))
			else: raise TaskFailed("Device %s has not been set up as a static device in /etc/network/interfaces"%dev)

		if is_service_running(self.nmservice):
			self.was_nm_service_running = True
			yield "Stopping NetworkManager to avoid automatic network reconfiguration"
			disable_service(self.nmservice)
		else:
			self.was_nm_service_running = False
			
		if is_service_running(self.netservice):
			self.was_net_service_running = True
		else:
			self.was_net_service_running = False
			
		yield "Creating Cloud bridging device and making device %s member of this bridge"%dev

		if distro in (Fedora, CentOS, RHEL6):
			ifcfgtext = file(pathtoconfigfile).read()
			newf = "/etc/sysconfig/network-scripts/ifcfg-%s"%self.brname
			#def restore():
				#try: os.unlink(newf)
				#except OSError,e:
					#if errno == 2: pass
					#raise
				#try: file(pathtoconfigfile,"w").write(ifcfgtext)
				#except OSError,e: raise

			f = file(newf,"w") ; f.write(ifcfgtext) ; f.flush() ; f.close()
			innewconfigfile = "/files" + newf

			script = """set %s/DEVICE %s
set %s/NAME %s
set %s/BRIDGE_PORTS %s
set %s/TYPE Bridge
rm %s/HWADDR
rm %s/UUID
rm %s/HWADDR
rm %s/IPADDR
rm %s/DEFROUTE
rm %s/NETMASK
rm %s/GATEWAY
rm %s/BROADCAST
rm %s/NETWORK
set %s/BRIDGE %s
save"""%(innewconfigfile,self.brname,innewconfigfile,self.brname,innewconfigfile,dev,
			innewconfigfile,innewconfigfile,innewconfigfile,innewconfigfile,
			inconfigfile,inconfigfile,inconfigfile,inconfigfile,inconfigfile,inconfigfile,
			inconfigfile,self.brname)
			
			yield "Executing the following reconfiguration script:\n%s"%script
			
			try:
				returned = augtool < script
				if "Saved 2 file" not in returned.stdout:
					print returned.stdout + returned.stderr
					#restore()
					raise TaskFailed("Network reconfiguration failed.")
				else:
					yield "Network reconfiguration complete"
			except CalledProcessError,e:
				#restore()
				print e.stdout + e.stderr
				raise TaskFailed("Network reconfiguration failed")
		else: # Not fedora
			backup = file("/etc/network/interfaces").read(-1)
			#restore = lambda: file("/etc/network/interfaces","w").write(backup)

			script = """set %s %s
set %s %s
set %s/bridge_ports %s
save"""%(automatic,self.brname,inconfigfile,self.brname,inconfigfile,dev)
			
			yield "Executing the following reconfiguration script:\n%s"%script
			
			try:
				returned = augtool < script
				if "Saved 1 file" not in returned.stdout:
					#restore()
					raise TaskFailed("Network reconfiguration failed.")
				else:
					yield "Network reconfiguration complete"
			except CalledProcessError,e:
				#restore()
				print e.stdout + e.stderr
				raise TaskFailed("Network reconfiguration failed")
		
		yield "We are going to restart network services now, to make the network changes take effect.  Hit ENTER when you are ready."
		if self.isAutoMode(): pass
        	else:
		    raw_input()
		
		# if we reach here, then if something goes wrong we should attempt to revert the runinng state
		# if not, then no point
		self.runtime_state_changed = True
		
		yield "Enabling and restarting non-NetworkManager networking"
		if distro is Ubuntu: ifup(self.brname,stdout=None,stderr=None)
		stop_service(self.netservice)
		try: enable_service(self.netservice,forcestart=True)
		except CalledProcessError,e:
			if e.returncode == 1: pass
			else: raise
		
		yield "Verifying that the bridge is up"
		try:
			o = ifconfig(self.brname)
		except CalledProcessError,e:
			print e.stdout + e.stderr
			raise TaskFailed("The bridge could not be set up properly")
		
		yield "Networking restart done"


class SetupCgConfig(ConfigTask):
	name = "control groups configuration"
	
	def done(self):
		
		try:
			return "group virt" in file("/etc/cgconfig.conf","r").read(-1)
		except IOError,e:
			if e.errno is 2: raise TaskFailed("cgconfig has not been properly installed on this system")
			raise
		
	def execute(self):
		cgconfig = file("/etc/cgconfig.conf","r").read(-1)
		cgconfig = cgconfig + """
group virt {
	cpu {
		cpu.shares = 9216;
	}
}
"""
		file("/etc/cgconfig.conf","w").write(cgconfig)
		
		stop_service("cgconfig")
		enable_service("cgconfig",forcestart=True)


class SetupCgRules(ConfigTask):
	name = "control group rules setup"
	cfgline = "root:/usr/sbin/libvirtd	cpu	virt/"
	
	def done(self):
		try:
			return self.cfgline in file("/etc/cgrules.conf","r").read(-1)
		except IOError,e:
			if e.errno is 2: raise TaskFailed("cgrulesd has not been properly installed on this system")
			raise
	
	def execute(self):
		cgrules = file("/etc/cgrules.conf","r").read(-1)
		cgrules = cgrules + "\n" + self.cfgline + "\n"
		file("/etc/cgrules.conf","w").write(cgrules)
		
		stop_service("cgred")
		enable_service("cgred")


class SetupSecurityDriver(ConfigTask):
	name = "security driver setup"
	cfgline = "security_driver = \"none\""
	filename = "/etc/libvirt/qemu.conf"
	
	def done(self):
		try:
			return self.cfgline in file(self.filename,"r").read(-1)
		except IOError,e:
			if e.errno is 2: raise TaskFailed("qemu has not been properly installed on this system")
			raise
	
	def execute(self):
		libvirtqemu = file(self.filename,"r").read(-1)
		libvirtqemu = libvirtqemu + "\n" + self.cfgline + "\n"
		file("/etc/libvirt/qemu.conf","w").write(libvirtqemu)


class SetupLibvirt(ConfigTask):
	name = "libvirt setup"
	cfgline = "export CGROUP_DAEMON='cpu:/virt'"
	def done(self):
		try:
			if distro in (Fedora,CentOS, RHEL6): 	 libvirtfile = "/etc/sysconfig/libvirtd"
			elif distro is Ubuntu:	 libvirtfile = "/etc/default/libvirt-bin"
			else: raise AssertionError, "We should not reach this"
			return self.cfgline in file(libvirtfile,"r").read(-1)
		except IOError,e:
			if e.errno is 2: raise TaskFailed("libvirt has not been properly installed on this system")
			raise
	
	def execute(self):
		if distro in (Fedora,CentOS, RHEL6): 	 libvirtfile = "/etc/sysconfig/libvirtd"
		elif distro is Ubuntu:	 libvirtfile = "/etc/default/libvirt-bin"
		else: raise AssertionError, "We should not reach this"
		libvirtbin = file(libvirtfile,"r").read(-1)
		libvirtbin = libvirtbin + "\n" + self.cfgline + "\n"
		file(libvirtfile,"w").write(libvirtbin)
		
		if distro in (CentOS, Fedora, RHEL6):	svc = "libvirtd"
		else:					svc = "libvirt-bin"
		stop_service(svc)
		enable_service(svc)

class SetupLiveMigration(ConfigTask):
	name = "live migration setup"
	stanzas = (
			"listen_tcp=1",
			'tcp_port="16509"',
			'auth_tcp="none"',
			"listen_tls=0",
	)
	
	def done(self):
		try:
			lines = [ s.strip() for s in file("/etc/libvirt/libvirtd.conf").readlines() ]
			if all( [ stanza in lines for stanza in self.stanzas ] ): return True
		except IOError,e:
			if e.errno is 2: raise TaskFailed("libvirt has not been properly installed on this system")
			raise
	
	def execute(self):
		
		for stanza in self.stanzas:
			startswith = stanza.split("=")[0] + '='
			replace_or_add_line("/etc/libvirt/libvirtd.conf",startswith,stanza)

		if distro in (Fedora, RHEL6):
			replace_or_add_line("/etc/sysconfig/libvirtd","LIBVIRTD_ARGS=","LIBVIRTD_ARGS=-l")
		
		elif distro is Ubuntu:
			if os.path.exists("/etc/init/libvirt-bin.conf"):
				replace_line("/etc/init/libvirt-bin.conf", "exec /usr/sbin/libvirtd","exec /usr/sbin/libvirtd -d -l")
			else:
				replace_or_add_line("/etc/default/libvirt-bin","libvirtd_opts=","libvirtd_opts='-l -d'")
			
		else:
			raise AssertionError("Unsupported distribution")
		
		if distro in (CentOS, Fedora, RHEL6):	svc = "libvirtd"
		else:						svc = "libvirt-bin"
		stop_service(svc)
		enable_service(svc)


class SetupRequiredServices(ConfigTask):
	name = "required services setup"
	
	def done(self):
		if distro in (Fedora, RHEL6):  nfsrelated = "rpcbind nfslock"
		elif distro is CentOS: nfsrelated = "portmap nfslock"
		else: return True
		return all( [ is_service_running(svc) for svc in nfsrelated.split() ] )
		
	def execute(self):

		if distro in (Fedora, RHEL6):  nfsrelated = "rpcbind nfslock"
		elif distro is CentOS: nfsrelated = "portmap nfslock"
		else: raise AssertionError("Unsupported distribution")

		for svc in nfsrelated.split(): enable_service(svc)


class SetupFirewall(ConfigTask):
	name = "firewall setup"
	
	def done(self):
		
		if distro in (Fedora, CentOS,RHEL6):
			if not os.path.exists("/etc/sysconfig/iptables"): return True
			if ":on" not in chkconfig("--list","iptables").stdout: return True
		else:
			if "Status: active" not in ufw.status().stdout: return True
			if not os.path.exists("/etc/ufw/before.rules"): return True
		rule = "-p tcp -m tcp --dport 16509 -j ACCEPT"
		if rule in iptablessave().stdout: return True
		return False
	
	def execute(self):
		ports = "22 1798 16509".split()
		if distro in (Fedora , CentOS, RHEL6):
			for p in ports: iptables("-I","INPUT","1","-p","tcp","--dport",p,'-j','ACCEPT')
			o = service.iptables.save() ; print o.stdout + o.stderr
		else:
			for p in ports: ufw.allow(p)


class SetupFirewall2(ConfigTask):
	# this closes bug 4371
	name = "additional firewall setup"
	def __init__(self,brname):
		ConfigTask.__init__(self)
		self.brname = brname
	
	def done(self):
		
		if distro in (Fedora, CentOS, RHEL6):
			if not os.path.exists("/etc/sysconfig/iptables"): return True
			if ":on" not in chkconfig("--list","iptables").stdout: return True
			return False
		else:
			if "Status: active" not in ufw.status().stdout: return True
			if not os.path.exists("/etc/ufw/before.rules"): return True
			return False
		
	def execute(self):
		
		yield "Permitting traffic in the bridge interface, migration port and for VNC ports"
		
		if distro in (Fedora , CentOS, RHEL6):
			
			for rule in (
				"-I INPUT 1 -p tcp --dport 5900:6100 -j ACCEPT",
				"-I INPUT 1 -p tcp --dport 49152:49216 -j ACCEPT",
				):
				args = rule.split()
				o = iptables(*args)
			service.iptables.save(stdout=None,stderr=None)
			
		else:
			
			ufw.allow.proto.tcp("from","any","to","any","port","5900:6100")
			ufw.allow.proto.tcp("from","any","to","any","port","49152:49216")

			stop_service("ufw")
			start_service("ufw")


# Tasks according to distribution -- at some point we will split them in separate modules

def config_tasks(brname, pubNic, prvNic):
	if distro is CentOS:
		config_tasks = (
			SetupNetworking(brname, pubNic, prvNic),
			SetupLibvirt(),
			SetupRequiredServices(),
			SetupFirewall(),
			SetupFirewall2(brname),
		)
	elif distro in (Ubuntu,Fedora, RHEL6):
		config_tasks = (
			SetupNetworking(brname, pubNic, prvNic),
			SetupCgConfig(),
			SetupCgRules(),
			SetupSecurityDriver(),
			SetupLibvirt(),
			SetupLiveMigration(),
			SetupRequiredServices(),
			SetupFirewall(),
			SetupFirewall2(brname),
		)
	else:
		raise AssertionError("Unknown distribution")
	return config_tasks


def backup_etc(targetdir):
	if not targetdir.endswith("/"): targetdir += "/"
	check_call( ["mkdir","-p",targetdir] )
	rsynccall = ["rsync","-ax","--delete"] + ["/etc/",targetdir]
	check_call( rsynccall )
def restore_etc(targetdir):
	if not targetdir.endswith("/"): targetdir += "/"
	rsynccall = ["rsync","-ax","--delete"] + [targetdir,"/etc/"]
	check_call( rsynccall )
def remove_backup(targetdir):
	check_call( ["rm","-rf",targetdir] )

def list_zonespods(host):
	text = urllib2.urlopen('http://%s:8096/client/api?command=listPods'%host).read(-1)
	dom = xml.dom.minidom.parseString(text) 
	x = [ (zonename,podname)
		for pod in dom.childNodes[0].childNodes  
		for podname in [ x.childNodes[0].wholeText for x in pod.childNodes if x.tagName == "name" ] 
		for zonename in  [ x.childNodes[0].wholeText for x in pod.childNodes if x.tagName == "zonename" ]
		]
	return x
	
def prompt_for_hostpods(zonespods):
	"""Ask user to select one from those zonespods
	Returns (zone,pod) or None if the user made the default selection."""
	while True:
		stderr("Type the number of the zone and pod combination this host belongs to (hit ENTER to skip this step)")
		print "  N) ZONE, POD" 
		print "================"
		for n,(z,p) in enumerate(zonespods):
			print "%3d) %s, %s"%(n,z,p)
		print "================"
		print "> ",
		zoneandpod = raw_input().strip()
		
		if not zoneandpod:
			# we go with default, do not touch anything, just break
			return None
		
		try:
			# if parsing fails as an int, just vomit and retry
			zoneandpod = int(zoneandpod)
			if zoneandpod >= len(zonespods) or zoneandpod < 0: raise ValueError, "%s out of bounds"%zoneandpod
		except ValueError,e:
			stderr(str(e))
			continue # re-ask
		
		# oh yeah, the int represents an valid zone and pod index in the array
		return zonespods[zoneandpod]
	
# this configures the agent

def device_exist(devName):
	try:
		alreadysetup = False
		if distro in (Fedora,CentOS, RHEL6):
			alreadysetup = augtool._print("/files/etc/sysconfig/network-scripts/ifcfg-%s"%devName).stdout.strip()
		else:
			alreadysetup = augtool.match("/files/etc/network/interfaces/iface",devName).stdout.strip()
		return alreadysetup
	except OSError,e:
		return False		
	
def setup_agent_config(configfile, host, zone, pod, cluster, guid, pubNic, prvNic):
	stderr("Examining Agent configuration")
	fn = configfile
	text = file(fn).read(-1)
	lines = [ s.strip() for s in text.splitlines() ]
	confopts = dict([ m.split("=",1) for m in lines if "=" in m and not m.startswith("#") ])
	confposes = dict([ (m.split("=",1)[0],n) for n,m in enumerate(lines) if "=" in m and not m.startswith("#") ])
	
	if guid != None:
		confopts['guid'] = guid
	else:
		if not "guid" in confopts:
			stderr("Generating GUID for this Agent")
			confopts['guid'] = uuidgen().stdout.strip()
	
	if host == None:
		try: host = confopts["host"]
		except KeyError: host = "localhost"
		stderr("Please enter the host name of the management server that this agent will connect to: (just hit ENTER to go with %s)",host)
		print "> ",
		newhost = raw_input().strip()
		if newhost: host = newhost

	confopts["host"] = host
	
	if pubNic != None and device_exist(pubNic):
		confopts["public.network.device"] = pubNic	
		if prvNic == None or not device_exist(prvNic):
			confopts["private.network.device"] = pubNic	
		
	if prvNic != None and device_exist(prvNic):
		confopts["private.network.device"] = prvNic	
		if pubNic == None or not device_exist(pubNic):
			confopts["public.network.device"] = prvNic	

	stderr("Querying %s for zones and pods",host)
	
	try:
	    if zone == None or pod == None:
			x = list_zonespods(confopts['host'])
			zoneandpod = prompt_for_hostpods(x)
			if zoneandpod:
				confopts["zone"],confopts["pod"] = zoneandpod
				stderr("You selected zone %s pod %s",confopts["zone"],confopts["pod"])
			else:
				stderr("Skipped -- using the previous zone %s pod %s",confopts["zone"],confopts["pod"])
	    else:
			confopts["zone"] = zone
			confopts["pod"] = pod
			confopts["cluster"] = cluster
	except (urllib2.URLError,urllib2.HTTPError),e:
		stderr("Query failed: %s.  Defaulting to zone %s pod %s",str(e),confopts["zone"],confopts["pod"])

	for opt,val in confopts.items():
		line = "=".join([opt,val])
		if opt not in confposes: lines.append(line)
		else: lines[confposes[opt]] = line
	
	text = "\n".join(lines)
	file(fn,"w").write(text)

def setup_consoleproxy_config(configfile, host, zone, pod):
	stderr("Examining Console Proxy configuration")
	fn = configfile
	text = file(fn).read(-1)
	lines = [ s.strip() for s in text.splitlines() ]
	confopts = dict([ m.split("=",1) for m in lines if "=" in m and not m.startswith("#") ])
	confposes = dict([ (m.split("=",1)[0],n) for n,m in enumerate(lines) if "=" in m and not m.startswith("#") ])

	if not "guid" in confopts:
		stderr("Generating GUID for this Console Proxy")
		confopts['guid'] = uuidgen().stdout.strip()

        if host == None:
		try: host = confopts["host"]
		except KeyError: host = "localhost"
		stderr("Please enter the host name of the management server that this console-proxy will connect to: (just hit ENTER to go with %s)",host)
		print "> ",
		newhost = raw_input().strip()
		if newhost: host = newhost
	confopts["host"] = host

	stderr("Querying %s for zones and pods",host)
	
	try:
                if zone == None or pod == None:
			x = list_zonespods(confopts['host'])
			zoneandpod = prompt_for_hostpods(x)
			if zoneandpod:
				confopts["zone"],confopts["pod"] = zoneandpod
				stderr("You selected zone %s pod %s",confopts["zone"],confopts["pod"])
			else:
				stderr("Skipped -- using the previous zone %s pod %s",confopts["zone"],confopts["pod"])
		else:
			confopts["zone"] = zone
			confopts["pod"] = pod
	except (urllib2.URLError,urllib2.HTTPError),e:
		stderr("Query failed: %s.  Defaulting to zone %s pod %s",str(e),confopts["zone"],confopts["pod"])

	for opt,val in confopts.items():
		line = "=".join([opt,val])
		if opt not in confposes: lines.append(line)
		else: lines[confposes[opt]] = line
	
	text = "\n".join(lines)
	file(fn,"w").write(text)

# =========================== DATABASE MIGRATION SUPPORT CODE ===================

# Migrator, Migratee and Evolvers -- this is the generic infrastructure.


class MigratorException(Exception): pass
class NoMigrationPath(MigratorException): pass
class NoMigrator(MigratorException): pass

INITIAL_LEVEL = '-'

class Migrator:
	"""Migrator class.
	
	The migrator gets a list of Python objects, and discovers MigrationSteps in it. It then sorts the steps into a chain, based on the attributes from_level and to_level in each one of the steps.
	
	When the migrator's run(context) is called, the chain of steps is applied sequentially on the context supplied to run(), in the order of the chain of steps found at discovery time.  See the documentation for the MigrationStep class for information on how that happens.
	"""
	
	def __init__(self,evolver_source):
		self.discover_evolvers(evolver_source)
		self.sort_evolvers()
		
	def discover_evolvers(self,source):
		self.evolvers = []
		for val in source:
			if hasattr(val,"from_level") and hasattr(val,"to_level") and val.to_level:
				self.evolvers.append(val)
	
	def sort_evolvers(self):
		new = []
		while self.evolvers:
			if not new:
				try: idx= [ i for i,s in enumerate(self.evolvers)
					if s.from_level == INITIAL_LEVEL ][0] # initial evolver
				except IndexError,e:
					raise IndexError, "no initial evolver (from_level is None) could be found"
			else:
				try: idx= [ i for i,s in enumerate(self.evolvers)
					if new[-1].to_level == s.from_level ][0]
				except IndexError,e:
					raise IndexError, "no evolver could be found to evolve from level %s"%new[-1].to_level
			new.append(self.evolvers.pop(idx))
		self.evolvers = new
	
	def get_evolver_chain(self):
		return [ (s.from_level, s.to_level, s) for s in self.evolvers ]
		
	def get_evolver_by_starting_level(self,level):
		try: return [ s for s in self.evolvers if s.from_level == level][0]
		except IndexError: raise NoMigrator, "No evolver knows how to evolve the database from schema level %r"%level
	
	def get_evolver_by_ending_level(self,level):
		try: return [ s for s in self.evolvers if s.to_level == level][0]
		except IndexError: raise NoMigrator, "No evolver knows how to evolve the database to schema level %r"%level
	
	def run(self, context, dryrun = False, starting_level = None, ending_level = None):
		"""Runs each one of the steps in sequence, passing the migration context to each. At the end of the process, context.commit() is called to save the changes, or context.rollback() is called if dryrun = True.
		
		If starting_level is not specified, then the context.get_schema_level() is used to find out at what level the context is at.  Then starting_level is set to that.
		
		If ending_level is not specified, then the evolvers will run till the end of the chain."""
		
		assert dryrun is False # NOT IMPLEMENTED, prolly gonna implement by asking the context itself to remember its state
		
		starting_level = starting_level or context.get_schema_level() or self.evolvers[0].from_level
		ending_level = ending_level or self.evolvers[-1].to_level
		
		evolution_path = self.evolvers
		idx = evolution_path.index(self.get_evolver_by_starting_level(starting_level))
		evolution_path = evolution_path[idx:]
		try: idx = evolution_path.index(self.get_evolver_by_ending_level(ending_level))
		except ValueError:
			raise NoEvolutionPath, "No evolution path from schema level %r to schema level %r" % \
				(starting_level,ending_level)
		evolution_path = evolution_path[:idx+1]
		
		logging.info("Starting migration on %s"%context)
		
		for ec in evolution_path:
			assert ec.from_level == context.get_schema_level()
			evolver = ec(context=context)
			logging.info("%s (from level %s to level %s)",
				evolver,
				evolver.from_level,
				evolver.to_level)
			#try:
			evolver.run()
			#except:
				#context.rollback()
				#raise
			context.set_schema_level(evolver.to_level)
			#context.commit()
			logging.info("%s is now at level %s",context,context.get_schema_level())
		
		#if dryrun: # implement me with backup and restore
			#logging.info("Rolling back changes on %s",context)
			#context.rollback()
		#else:
			#logging.info("Committing changes on %s",context)
			#context.commit()
		
		logging.info("Migration finished")
		

class MigrationStep:
	"""Base MigrationStep class, aka evolver.
	
	You develop your own steps, and then pass a list of those steps to the
	Migrator instance that will run them in order.
	
	When the migrator runs, it will take the list of steps you gave him,
	and, for each step:
	
	a) instantiate it, passing the context you gave to the migrator
	   into the step's __init__().
	b) run() the method in the migration step.
	
	As you can see, the default MigrationStep constructor makes the passed
	context available as self.context in the methods of your step.
	
	Each step has two member vars that determine in which order they
	are run, and if they need to run:
	
	- from_level = the schema level that the database should be at,
		       before running the evolver
		       The value None has special meaning here, it
		       means the first evolver that should be run if the
		       database does not have a schema level yet.
	- to_level =   the schema level number that the database will be at
		       after the evolver has run
	"""
	
	# Implement these attributes in your steps
	from_level = None
	to_level = None
	
	def __init__(self,context):
		self.context = context
		
	def run(self):
		raise NotImplementedError


class MigrationContext:
	def __init__(self): pass
	def commit(self):raise NotImplementedError
	def rollback(self):raise NotImplementedError
	def get_schema_level(self):raise NotImplementedError
	def set_schema_level(self,l):raise NotImplementedError


