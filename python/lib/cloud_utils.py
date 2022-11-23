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





# -*- coding: utf-8 -*-
"""CloudStack Python utility library"""

import sys, os, subprocess, errno, re, time, glob
import urllib.request, urllib.error, urllib.parse
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
SUSE = 5

IPV4 = 4
IPV6 = 6

#=================== DISTRIBUTION DETECTION =================

if os.path.exists("/etc/fedora-release"): distro = Fedora
elif os.path.exists("/etc/centos-release"): distro = CentOS
elif os.path.exists("/etc/redhat-release"):
    version = open("/etc/redhat-release").readline()
    if version.find("Red Hat Enterprise Linux Server release 6") != -1:
        distro = RHEL6
    elif version.find("CentOS") != -1:
        distro = CentOS
    else:
        distro = CentOS
elif os.path.exists("/etc/legal") and "Ubuntu" in open("/etc/legal").read(-1): distro = Ubuntu
elif os.path.exists("/etc/os-release") and "SUSE" in open("/etc/os-release").read(-1): distro = SUSE
else: distro = Unknown
logFileName=None

# ==================  LIBRARY UTILITY CODE=============
def setLogFile(logFile):
	global logFileName
	logFileName=logFile

def read_properties(propfile):
	if not hasattr(propfile,"read"): propfile = open(propfile)
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
	except CalledProcessError as e:
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
	lines = [ s.strip() for s in open(f).readlines() ]
	newlines = []
	replaced = False
	for line in lines:
		if line.startswith(startswith):
			newlines.append(stanza)
			replaced = True
		else: newlines.append(line)
	if not replaced and always_add: newlines.append(stanza)
	newlines = [ s + '\n' for s in newlines ]
	open(f,"w").writelines(newlines)

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
	if distro in (Fedora,CentOS,RHEL6,SUSE):
		if os.path.exists("/dev/kvm"): return True
		raise CheckFailed("KVM is not correctly installed on this system, or support for it is not enabled in the BIOS")
	else:
		try:
			kvmok()
			return True
		except CalledProcessError:
			raise CheckFailed("KVM is not correctly installed on this system, or support for it is not enabled in the BIOS")
		except OSError as e:
			if e.errno == errno.ENOENT: raise CheckFailed("KVM is not correctly installed on this system, or support for it is not enabled in the BIOS")
			raise
		return True
	raise AssertionError("check_kvm() should have never reached this part")

def check_cgroups():
	return glob.glob("/*/cpu.shares")

#check function
def check_selinux():
	if distro not in [Fedora,CentOS,RHEL6,SUSE]: return # no selinux outside of those
	enforcing = False
	config_enforcing = False
	try:
		output = getenforce().stdout.decode('utf-8').strip()
		if "nforcing" in output:
			enforcing = True
		if any ( [ s.startswith("SELINUX=enforcing") for s in open("/etc/selinux/config").readlines() ] ):
			config_enforcing = True
		else:
			config_enforcing = False
	except (IOError,OSError) as e:
		if e.errno == 2: pass
		else: raise CheckFailed("An unknown error (%s) took place while checking for SELinux"%str(e))
	if enforcing:
		raise CheckFailed('''SELinux is set to enforcing. There are two options:
1> Set it permissive in /etc/selinux/config, then reboot the machine.
2> Type 'setenforce Permissive' in commandline, after which you can run this program again.

We strongly suggest you doing the option 1 that makes sure SELinux goes into permissive after system reboot.\n''')

	if config_enforcing:
		print("WARNING: We detected that your SELinux is not configured in permissive. to make sure cloudstack won't block by \
SELinux after system reboot, we strongly suggest you setting it in permissive in /etc/selinux/config, then reboot the machine.")


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
	text = urllib.request.urlopen('http://%s:8096/client/api?command=listPods'%host).read(-1)
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
		print("  N) ZONE, POD")
		print("================")
		for n,(z,p) in enumerate(zonespods):
			print("%3d) %s, %s"%(n,z,p))
		print("================")
		print("> ", end=' ')
		zoneandpod = input().strip()

		if not zoneandpod:
			# we go with default, do not touch anything, just break
			return None

		try:
			# if parsing fails as an int, just vomit and retry
			zoneandpod = int(zoneandpod)
			if zoneandpod >= len(zonespods) or zoneandpod < 0: raise ValueError("%s out of bounds"%zoneandpod)
		except ValueError as e:
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
		elif distro == SUSE:
			alreadysetup = augtool._print("/files/etc/sysconfig/network/ifcfg-%s"%devName).stdout.strip()
		elif distro == Ubuntu:
			alreadysetup = augtool.match("/files/etc/network/interfaces/iface",devName).stdout.strip()
		return alreadysetup
	except OSError as e:
		return False

def setup_agent_config(configfile, host, zone, pod, cluster, guid, pubNic, prvNic):
	stderr("Examining Agent configuration")
	fn = configfile
	text = open(fn).read(-1)
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
		print("> ", end=' ')
		newhost = input().strip()
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
	except (urllib.error.URLError,urllib.error.HTTPError) as e:
		stderr("Query failed: %s.  Defaulting to zone %s pod %s",str(e),confopts["zone"],confopts["pod"])

	for opt,val in list(confopts.items()):
		line = "=".join([opt,val])
		if opt not in confposes: lines.append(line)
		else: lines[confposes[opt]] = line

	text = "\n".join(lines)
	open(fn,"w").write(text)

def setup_consoleproxy_config(configfile, host, zone, pod):
	stderr("Examining Console Proxy configuration")
	fn = configfile
	text = open(fn).read(-1)
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
		print("> ", end=' ')
		newhost = input().strip()
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
	except (urllib.error.URLError,urllib.error.HTTPError) as e:
		stderr("Query failed: %s.  Defaulting to zone %s pod %s",str(e),confopts["zone"],confopts["pod"])

	for opt,val in list(confopts.items()):
		line = "=".join([opt,val])
		if opt not in confposes: lines.append(line)
		else: lines[confposes[opt]] = line

	text = "\n".join(lines)
	open(fn,"w").write(text)

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
				except IndexError as e:
					raise IndexError("no initial evolver (from_level is None) could be found")
			else:
				try: idx= [ i for i,s in enumerate(self.evolvers)
					if new[-1].to_level == s.from_level ][0]
				except IndexError as e:
					raise IndexError("no evolver could be found to evolve from level %s"%new[-1].to_level)
			new.append(self.evolvers.pop(idx))
		self.evolvers = new

	def get_evolver_chain(self):
		return [ (s.from_level, s.to_level, s) for s in self.evolvers ]

	def get_evolver_by_starting_level(self,level):
		try: return [ s for s in self.evolvers if s.from_level == level][0]
		except IndexError: raise NoMigrator("No evolver knows how to evolve the database from schema level %r"%level)

	def get_evolver_by_ending_level(self,level):
		try: return [ s for s in self.evolvers if s.to_level == level][0]
		except IndexError: raise NoMigrator("No evolver knows how to evolve the database to schema level %r"%level)

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
			raise NoEvolutionPath("No evolution path from schema level %r to schema level %r" % \
				(starting_level,ending_level))
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

	When the migrator runs, it will take the list of steps you gave,
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
