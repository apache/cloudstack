#! /usr/bin/env python
# -*- coding: utf-8 -*-

# the following two variables are used by the target "waf dist"
# if you change 'em here, you need to change it also in cloud.spec, add a %changelog entry there, and add an entry in debian/changelog
VERSION = '2.2.2'
APPNAME = 'cloud'

import shutil,os
import email,time
import optparse
import Utils,Node,Options,Logs,Scripting,Environment,Build,Configure
from subprocess import Popen as _Popen,PIPE
import os
import sys
from os import unlink as _unlink, makedirs as _makedirs, getcwd as _getcwd, chdir as _chdir
from os.path import abspath as _abspath, basename as _basename, dirname as _dirname, exists as _exists, isdir as _isdir, split as _split, join as _join, sep, pathsep, pardir, curdir
from glob import glob as _glob
import zipfile,tarfile
try:
  from os import chmod as _chmod,chown as _chown
  import pwd,stat,grp
except ImportError:
  _chmod,_chown,pwd,stat,grp = (None,None,None,None,None)
import xml.dom.minidom
import re

# these variables are mandatory ('/' are converted automatically)
srcdir = '.'
blddir = 'artifacts'

Configure.autoconfig = True

# things not to include in the source tarball
# exclude by file name or by _glob (wildcard matching)
for _globber in [
	["dist",    # does not belong in the source tarball
	"system",  # for windows
	"override",  # not needed
	"eclipse", # only there to please eclipse
	"repomanagement",  # internal management stuff
	"client-api",  # obsolete
	"cloud-bridge",  # not compiled and packaged yet
	"target",  # eclipse workdir
        "apache-log4j-1.2.16",
        "apache-log4j-extras-1.1",
        "cglib",
        "gson",
        "ehcache",
        "vhd-tools",
        "xmlrpc",
        "PreviousDatabaseSchema",
        "mockito",
        "gcc",
        "junit" ],
	_glob("./*.disabledblahxml"),
	]:
	for f in _globber: Scripting.excludes.append(_basename(f)) # _basename() only the filename

# things never to consider when building or installing
for pattern in ["**/.project","**/.classpath","**/.pydevproject"]: Node.exclude_regs += "\n%s"%pattern

# Support functions

def inspectobj(x):
	"""Look inside an object"""
	for m in dir(x): print m,":	",getattr(x,m)
Utils.inspectobj = inspectobj

def _trm(x,y):
	if len(x) > y: return x[:y] + "..."
	return x

def getrpmdeps():
	def rpmdeps(fileset):
		for f in fileset:
			lines = file(f).readlines()
			lines = [ x[len("BuildRequires: "):] for x in lines if x.startswith("BuildRequires") ]
			for l in lines:
				deps = [ x.strip() for x in l.split(",") ]
				for d in deps:
					if "%s-"%APPNAME in d: continue
					yield d
		yield "rpm-build"

	deps = set(rpmdeps(_glob("./*.spec")))
	return deps

def getdebdeps():
	def debdeps(fileset):
		for f in fileset:
			lines = file(f).readlines()
			lines = [ x[len("Build-Depends: "):] for x in lines if x.startswith("Build-Depends") ]
			for l in lines:
				deps = [ x.strip() for x in l.split(",") ]
				for d in deps:
					if "%s-"%APPNAME in d: continue
					yield d
		yield "build-essential"
		yield "devscripts"
		yield "debhelper"

	deps = set(debdeps(["debian/control"]))
	return deps

# CENTOS does not have this -- we have to put this here
try:
	from subprocess import check_call as _check_call
	from subprocess import CalledProcessError
except ImportError:
	def _check_call(*popenargs, **kwargs):
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
def throws_command_errors(f):
	def g(*args,**kwargs):
		try: return f(*args,**kwargs)
		except CalledProcessError,e:
			raise Utils.WafError("system command %s failed with error value %s"%(e.cmd[0],e.returncode))
		except IOError,e:
			if e.errno is 32:
				raise Utils.WafError("system command %s terminated abruptly, closing communications with parent's pipe"%e.cmd[0])
			raise
	return g

def c(cmdlist,cwd=None):
	# Run a command with _check_call, pretty-printing the cmd list
	Utils.pprint("BLUE"," ".join(cmdlist))
	return _check_call(cmdlist,cwd=cwd)

def svninfo(*args):
	try: p = _Popen(['svn','info']+list(args),stdin=PIPE,stdout=PIPE,stderr=PIPE)
	except OSError,e:
		if e.errno == 2: return '' # svn command is not installed
		raise
	stdout,stderr = p.communicate('')
	retcode = p.wait()
	# If the guess fails, just return nothing.
	if retcode: return
	# SVN available
	rev = [ x for x in stdout.splitlines() if x.startswith('Revision') ]
	if not rev: rev = ''
	else: rev = "SVN " + rev[0].strip()
	url = [ x for x in stdout.splitlines() if x.startswith('URL') ]
	if not url: url = ''
	else: url = "SVN " + url[0].strip()
	return rev + "\n" + url

def gitinfo(dir=None):
	if dir and not _isdir(dir): return ''
	try: p = _Popen(['git','remote','show','-n','origin'],stdin=PIPE,stdout=PIPE,stderr=PIPE,cwd=dir)
	except OSError,e:
		if e.errno == 2: return '' # svn command is not installed
		raise
	stdout,stderr = p.communicate('')
	retcode = p.wait()
	# If the guess fails, just return nothing.
	if retcode: return
	stdout = [ s.strip() for s in stdout.splitlines() ]
	try: url = [ s[11:] for s in stdout if s.startswith("Fetch URL") ][0]
	except IndexError: url = [ s[5:] for s in stdout if s.startswith("URL") ][0]
	assert url
	
	p = _Popen(['git','log','-1'],stdin=PIPE,stdout=PIPE,stderr=PIPE,cwd=dir)
	stdout,stderr = p.communicate('')
	retcode = p.wait()
	if retcode: return
	# If the guess fails, just return nothing.
	stdout = [ s.strip() for s in stdout.splitlines() ]
	commitid = [ s.split()[1] for s in stdout if s.startswith("commit") ][0]
	assert commitid
	
	return "Git Revision: %s"%commitid + "\n" + "Git URL: %s"%url

def allgitinfo():
	t = gitinfo()
	if not t: return t
	
	u = gitinfo(_join(pardir,"cloudstack-proprietary"))
	if not u: return t
	
	return t + "\n\ncloustack-proprietary:\n" + u

def _getbuildnumber(): # FIXME implement for git
	n = Options.options.BUILDNUMBER
	if n:
		# luntbuild prepends "build-" to the build number.  we work around this here:
		if n.startswith("build-"): n = n[6:]
		# SVN identifiers prepend "$Revision:" to the build number.  we work around this here:
		if n.startswith("$Revision:"): n = n[11:-2].strip()
		return n
	else:
		# Try to guess the SVN revision number by calling SVN info.
		stdout = svninfo()
		if not stdout: return ''
		# Filter lines.
		rev = [ x for x in stdout.splitlines() if x.startswith('SVN Revision') ]
		if not rev: return ''
		# Parse revision number.
		rev = rev[0][14:].strip()
		return rev
Utils.getbuildnumber = _getbuildnumber

def mkdir_p(directory):
	if not _isdir(directory):
		Utils.pprint("GREEN","Creating directory %s and necessary parents"%directory)
		_makedirs(directory)

def relpath(path, start="."):
    if not path: raise ValueError("no path specified")

    start_list = os.path.abspath(start).split(sep)
    path_list = os.path.abspath(path).split(sep)

    # Work out how much of the filepath is shared by start and path.
    i = len(os.path.commonprefix([start_list, path_list]))

    rel_list = [pardir] * (len(start_list)-i) + path_list[i:]
    if not rel_list:
        return curdir
    return os.path.join(*rel_list)
Utils.relpath = relpath

def dev_override(pathname):
	p,e = _split(pathname)
	overridden = _join(p,"override",e)
	if _exists(overridden): return overridden
	return pathname

def discover_ant_targets_and_properties(files):
	doms = [ xml.dom.minidom.parseString(file(f).read(-1)) for f in files if f.endswith(".xml") ]
	targets = dict( [ (target.getAttribute("name"),target) for dom in doms for target in dom.getElementsByTagName("target") if target.getElementsByTagName("compile-java") ] )
	propsinxml = [ (prop.getAttribute("name"),prop.getAttribute("value") or prop.getAttribute("location")) for dom in doms for prop in dom.getElementsByTagName("property") ]
	propsinpropfiles = [ l.strip().split("=",1) for f in files if f.endswith(".properties") for l in file(f).readlines() if "=" in l and not l.startswith("#") ]
	props = dict( propsinxml + propsinpropfiles )
	props["base.dir"] = '.'
	props["p.base.dir"] = '.'

	result = []
	for name,target in targets.items():
		sourcedir = target.getElementsByTagName("compile-java")[0].getAttribute("top.dir") + "/src"
		classdir = "${classes.dir}/" + target.getElementsByTagName("compile-java")[0].getAttribute("jar.name")
		jarpath = "${jar.dir}/" + target.getElementsByTagName("compile-java")[0].getAttribute("jar.name")
		def lookup(matchobject): return props[matchobject.group(1)]
		while "$" in sourcedir: sourcedir = re.sub("\${(.+?)}",lookup,sourcedir)
		while "$" in classdir:  classdir = re.sub("\${(.+?)}",lookup,classdir)
		while "$" in jarpath:   jarpath= re.sub("\${(.+?)}",lookup,jarpath)
		dependencies = [ dep.strip() for dep in target.getAttribute("depends").split(",") if dep.strip() in targets ]
		result.append([str(name),str(relpath(sourcedir)),str(relpath(classdir)),str(relpath(jarpath)),[str(s) for s in dependencies]])
	# hardcoded here because the discovery process does not get it
	result.append( ["build-console-viewer","console-viewer/src", "target/classes/console-viewer", "target/jar/VMOpsConsoleApplet.jar", ["compile-utils","compile-console-common"] ] )
	return result,props
Utils.discover_ant_targets_and_properties = discover_ant_targets_and_properties

# this snippet of code runs a list of ant targets
# then it expects a certain set of JAR files to be output in the artifacts/default/ant/jar directory
# this set of jar files is defined here in the variable jartgts, and must match the definitions at the bottom of
# build/package.xml and build/premium/package-premium.xml
# this is NOT a task for a task generator -- it is a plain function.
# If you want to use it as a task function in a task generator, use a lambda x: runant("targetname")
def runant(tsk):
	environ = dict(os.environ)
	environ["CATALINA_HOME"] = tsk.env.TOMCATHOME
	environ["ANT_HOME"] = _join("tools","ant","apache-ant-1.7.1")
	if tsk.generator.env.DISTRO == "Windows":
		stanzas = [
			_join(environ["ANT_HOME"],"bin","ant.bat"),
			"-Dthirdparty.classpath=\"%s\""%(tsk.env.CLASSPATH.replace(os.pathsep,",")),
		]
	else:
		stanzas = [
			_join(environ["ANT_HOME"],"bin","ant"),
			"-Dthirdparty.classpath=%s"%(tsk.env.CLASSPATH.replace(os.pathsep,",")),
		]
	stanzas += tsk.generator.antargs
	ret = Utils.exec_command(" ".join(stanzas),cwd=tsk.generator.bld.srcnode.abspath(),env=environ,log=True)
	if ret != 0: raise Utils.WafError("Ant command %s failed with error value %s"%(stanzas,ret))
	return ret
Utils.runant = runant

@throws_command_errors
def run_java(classname,classpath,options=None,arguments=None):
	if not options: options = []
	if not arguments: arguments = []
	if type(classpath) in [list,tuple]: classpath = pathsep.join(classpath)
	
	Utils.pprint("BLUE","Run-time CLASSPATH:")
	for v in classpath.split(pathsep): Utils.pprint("BLUE","     %s"%v)

	cmd = ["java","-classpath",classpath] + options + [classname] + arguments
	Utils.pprint("BLUE"," ".join([ _trm(x,32) for x in cmd ]))
	_check_call(cmd)

# this will enforce the after= ordering constraints in the javac task generators
from TaskGen import after, feature
@feature('*')
@after('apply_core', 'apply_java', 'apply_subst')
def process_after(self):
	lst = self.to_list(getattr(self, 'after', []))
	for x in lst:
		obj = self.bld.name_to_obj(x,self.bld.env)
		if not obj: break
		obj.post()
		for a in obj.tasks:
			for b in self.tasks:
				b.set_run_after(a)

Build.BuildContext.process_after = staticmethod(process_after)

def _getbuildcontext():
	ctx = Build.BuildContext()
	ctx.load_dirs(_abspath(srcdir),_abspath(blddir))
	ctx.load_envs()
	return ctx

def _install_files_filtered(self,destdir,listoffiles,**kwargs):
	if "cwd" in kwargs: cwd = kwargs["cwd"]
	else: cwd = self.path
	if isinstance(listoffiles,str) and '**' in listoffiles:
		listoffiles = cwd.ant_glob(listoffiles,flat=True)
	elif isinstance(listoffiles,str) and '*' in listoffiles:
		listoffiles = [ n for x in listoffiles.split() for n in _glob(cwd.abspath() + os.sep + x.replace("/",os.sep))  ]
	listoffiles = Utils.to_list(listoffiles)[:]
	listoffiles = [ x for x in listoffiles if not ( x.endswith("~") or x == "override" or "%soverride"%os.sep in x ) ]
	for n,f in enumerate(listoffiles):
		f = os.path.abspath(f)
		f = dev_override(f)
		if _isdir(f): continue
		if f.endswith(".in"):
			source = f ; target = f[:-3]
			tgen = self(features='subst', source=source[len(self.path.abspath())+1:], target=target[len(self.path.abspath())+1:], name="filtered_%s"%source)
			tgen.dict = self.env.get_merged_dict()
		else:
			source = f ; target = f
		listoffiles[n] = target[len(cwd.abspath())+1:]
	if "postpone" not in kwargs: kwargs["postpone"] = True
	ret = self.install_files(destdir,listoffiles,**kwargs)
	return ret
Build.BuildContext.install_files_filtered = _install_files_filtered

def _substitute(self,listoffiles,install_to=None,cwd=None,dict=None,name=None,**kwargs):
	if cwd is None: cwd = self.path
	tgenkwargs = {}
	if name is not None: tgenkwargs["name"] = name
	if isinstance(listoffiles,str) and '**' in listoffiles:
		listoffiles = cwd.ant_glob(listoffiles,flat=True)
	elif isinstance(listoffiles,str) and '*' in listoffiles:
		listoffiles = [ n for x in listoffiles.split() for n in _glob(cwd.abspath() + os.sep + x.replace("/",os.sep))  ]
	for src in Utils.to_list(listoffiles):
		tgt = src + ".subst"
		inst = src # Utils.relpath(src,relative_to) <- disabled

		# Use cwd path when creating task and shift back later
		tmp = self.path
		self.path = cwd
		tgen = self(features='subst', source=src, target=tgt, **tgenkwargs)
		self.path = tmp

		if dict is not None: tgen.dict = dict
		else: tgen.dict = self.env.get_merged_dict()
		self.path.find_or_declare(tgt)
		if install_to is not None: self.install_as("%s/%s"%(install_to,inst), tgt, cwd=cwd, **kwargs)
Build.BuildContext.substitute = _substitute

def set_options(opt):
	"""Register command line options"""
	opt.tool_options('gnu_dirs')
	opt.tool_options('python')
	opt.tool_options('tar',tooldir='tools/waf')
	opt.tool_options('mkisofs',tooldir='tools/waf')
	if Options.platform not in ['darwin','win32']: opt.tool_options('usermgmt',tooldir='tools/waf')
	if Options.platform not in ['darwin','win32']: opt.tool_options('javadir',tooldir='tools/waf')
	opt.tool_options('tomcat',tooldir='tools/waf')
	if Options.platform not in ['darwin',"win32"]: opt.tool_options('compiler_cc')
	
        inst_dir = opt.get_option_group('--srcdir')
	inst_dir.add_option('--with-db-host',
		help = 'Database host to use for waf deploydb [Default: 127.0.0.1]',
		default = '127.0.0.1',
		dest = 'DBHOST')
	inst_dir.add_option('--with-db-user',
		help = 'Database user to use for waf deploydb [Default: root]',
		default = 'root',
		dest = 'DBUSER')
	inst_dir.add_option('--with-db-pw',
		help = 'Database password to use for waf deploydb [Default: ""]',
		default = '',
		dest = 'DBPW')
	inst_dir.add_option('--tomcat-user',
		help = 'UNIX user that the management server initscript will switch to [Default: <autodetected>]',
		default = '',
		dest = 'MSUSER')
	inst_dir.add_option('--no-dep-check',
		action='store_true',
		help = 'Skip dependency check and assume JARs already exist',
		default = False,
		dest = 'NODEPCHECK')
	inst_dir.add_option('--fast',
		action='store_true',
		help = 'does ---no-dep-check',
		default = False,
		dest = 'NODEPCHECK')
        inst_dir = opt.get_option_group('--force')
	inst_dir.add_option('--preserve-config',
		action='store_true',
		help = 'do not install configuration files',
		default = False,
		dest = 'PRESERVECONFIG')

	debugopts = optparse.OptionGroup(opt.parser,'run/debug options')
	opt.add_option_group(debugopts)
	debugopts.add_option('--debug-port',
		help = 'Port on which the debugger will listen when running waf debug [Default: 8787]',
		default = '8787',
		dest = 'DEBUGPORT')
	debugopts.add_option('--debug-suspend',
		action='store_true',
		help = 'Suspend the process upon startup so that a debugger can attach and set breakpoints',
		default = False,
		dest = 'DEBUGSUSPEND')
	debugopts.add_option('--run-verbose',
		action='store_true',
		help = 'Run Tomcat in verbose mode (java option -verbose)',
		default = False,
		dest = 'RUNVERBOSE')
	
	rpmopts = optparse.OptionGroup(opt.parser,'RPM/DEB build options')
	opt.add_option_group(rpmopts)
	rpmopts.add_option('--build-number',
		help = 'Build number [Default: SVN revision number for builds from checkouts, or empty for builds from source releases]',
		default = '',
		dest = 'BUILDNUMBER')
	rpmopts.add_option('--package-version',
		help = 'package version',
		default = '',
		dest = 'VERNUM')
	rpmopts.add_option('--release-version',
		help = 'release version',
		default = '',
		dest = 'RELEASENUM')
	rpmopts.add_option('--prerelease',
		help = 'Branch name to append to the release number (if specified, alter release number to be a prerelease); this option requires --build-number=X [Default: nothing]',
		default = '',
		dest = 'PRERELEASE')
	rpmopts.add_option('--skip-dist',
		action='store_true',
		help = 'Normally, dist() is called during package build.  This makes the package build assume that a distribution tarball has already been made, and use that.  This option is also valid during distcheck and dist.',
		default = False,
		dest = 'DONTDIST')
	
	distopts = optparse.OptionGroup(opt.parser,'dist options')
	opt.add_option_group(distopts)
	distopts.add_option('--oss',
		help = 'Only include open source components',
		action = 'store_true',
		default = False,
		dest = 'OSS')

def showconfig(conf):
	"""prints out the current configure environment configuration"""
	conf = _getbuildcontext()
	
	Utils.pprint("WHITE","Build environment:")
	for key,val in sorted(conf.env.get_merged_dict().items()):
		if "CLASSPATH" in key:
			Utils.pprint("BLUE","  %s:"%key)
			for v in val.split(pathsep):
				Utils.pprint("BLUE","     %s"%v)
			continue
		Utils.pprint("BLUE","  %s:	%s"%(key,val))

def _getconfig(self):
	lines = []
	for key,val in sorted(self.env.get_merged_dict().items()):
		if "CLASSPATH" in key:
			lines.append("  %s:"%key)
			for v in val.split(pathsep):
				lines.append("     %s"%v)
			continue
		lines.append("  %s:	%s"%(key,val))
	return "\n".join(lines)
Build.BuildContext.getconfig = _getconfig

def list_targets(ctx):
        """return the list of buildable and installable targets"""

	bld = Build.BuildContext()
	proj = Environment.Environment(Options.lockfile)
	bld.load_dirs(proj['srcdir'], proj['blddir'])
	bld.load_envs()
	
	bld.add_subdirs([os.path.split(Utils.g_module.root_path)[0]])

	names = set([])
	for x in bld.all_task_gen:
		try:
			names.add(x.name or x.target)
		except AttributeError:
			pass

	lst = list(names)
	lst.sort()
	for name in lst:
		print(name)

def decorate_dist(f):
	def dist(appname='',version=''):
		'''makes a tarball for redistributing the sources -- if --skip-dist is specified, does nothing'''
		if Options.options.DONTDIST:
			if not appname: appname=Utils.g_module.APPNAME
			if not version: version=Utils.g_module.VERSION
			tmp_folder=appname+'-'+version
			if Scripting.g_gz in['gz','bz2']:
				arch_name=tmp_folder+'.tar.'+Scripting.g_gz
			else:
				arch_name=tmp_folder+'.'+'zip'
			Logs.info('New archive skipped: %s'%(arch_name))
			return arch_name
		else:
			return f(appname,version)
	return dist
Scripting.dist = decorate_dist(Scripting.dist)

def dist_hook():
	# Clean the GARBAGE that clogs our repo to the tune of 300 MB
	# so downloaders won't have to cry every time they download a "source"
	# package over 90 MB in size
	[ shutil.rmtree(f) for f in _glob(_join("*","bin")) if _isdir(f) ]
	[ shutil.rmtree(f) for f in _glob(_join("cloudstack-proprietary","thirdparty","*")) if _isdir(f) ]
	[ shutil.rmtree(f) for f in [ _join("cloudstack-proprietary","tools") ] if _isdir(f) ]
	
	if Options.options.OSS:
		[ shutil.rmtree(f) for f in "cloudstack-proprietary".split() if _exists(f) ]
		
	stdout = svninfo("..") or allgitinfo()
	if stdout:
		f = file("sccs-info","w")
		f.write(stdout)
		f.flush()
		f.close()
	else:
		# No SVN available
		if _exists("sccs-info"):
			# If the file already existed, we preserve it
			return
		else:
			f = file("sccs-info","w")
			f.write("No revision control information could be detected when the source distribution was built.")
			f.flush()
			f.close()

def bindist(ctx):
	"""creates a binary distribution that, when unzipped in the root directory of a machine, deploys the entire stack"""
	ctx = _getbuildcontext()

	if Options.options.VERNUM:
		VERSION = Options.options.VERNUM

	tarball = "%s-bindist-%s.tar.%s"%(APPNAME,VERSION,Scripting.g_gz)
	zf = _join(ctx.bldnode.abspath(),tarball)
	Options.options.destdir = _join(ctx.bldnode.abspath(),"bindist-destdir")
	Scripting.install(ctx)
	
	if _exists(zf): _unlink(zf)
	Utils.pprint("GREEN","Creating %s"%(zf))
	z = tarfile.open(zf,"w:bz2")
	cwd = _getcwd()
	_chdir(Options.options.destdir)
	z.add(".")
	z.close()
	_chdir(cwd)

@throws_command_errors
def rpm(context):
	buildnumber = Utils.getbuildnumber()
	if buildnumber: buildnumber = ["--define","_build_number %s"%buildnumber]
	else: buildnumber = []

	if Options.options.PRERELEASE:
		if not buildnumber:
			raise Utils.WafError("Please specify a build number to go along with --prerelease")
		prerelease = ["--define","_prerelease %s"%Options.options.PRERELEASE]
	else: prerelease = []

	if Options.options.RELEASENUM:
		release = Options.options.RELEASENUM
	else: release = "1"
	
	releasever = ["--define", "_rel %s" % release]

	if Options.options.VERNUM:
		ver = Options.options.VERNUM
	else: ver = "2.2"

	packagever = ["--define", "_ver %s" % ver]
	
	# FIXME wrap the source tarball in POSIX locking!
	if not Options.options.blddir: outputdir = _join(context.curdir,blddir,"rpmbuild")
	else:			   outputdir = _join(_abspath(Options.options.blddir),"rpmbuild")
	Utils.pprint("GREEN","Building RPMs")

	tarball = Scripting.dist('', ver)
	sourcedir = _join(outputdir,"SOURCES")
	
	if _exists(sourcedir): shutil.rmtree(sourcedir)
	for a in ["RPMS/noarch","SRPMS","BUILD","SPECS","SOURCES"]: mkdir_p(_join(outputdir,a))
	shutil.move(tarball,_join(sourcedir,tarball))

	specfile = "%s.spec"%APPNAME
	checkdeps = lambda: c(["rpmbuild","--define","_topdir %s"%outputdir,"--nobuild",specfile]+packagever+releasever)
	dorpm = lambda: c(["rpmbuild","--define","_topdir %s"%outputdir,"-bb",specfile]+buildnumber+prerelease+packagever+releasever)
	try: checkdeps()
	except (CalledProcessError,OSError),e:
		Utils.pprint("YELLOW","Dependencies might be missing.  Trying to auto-install them...")
		installrpmdeps(context)
	dorpm()

@throws_command_errors
def deb(context):
	buildnumber = Utils.getbuildnumber()
	if buildnumber: buildnumber = ["--set-envvar=BUILDNUMBER=%s"%buildnumber]
	else: buildnumber = []
	
	if Options.options.VERNUM:
		VERSION = Options.options.VERNUM
	else:
		VERSION = "2.2"

	version = ["--set-envvar=PACKAGEVERSION=%s"%VERSION]

	if Options.options.PRERELEASE:
		if not buildnumber:
			raise Utils.WafError("Please specify a build number to go along with --prerelease")
		# version/release numbers are read by dpkg-buildpackage from line 1 of debian/changelog
		# http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version
		tempchangelog = """%s (%s-~%s%s) unstable; urgency=low

  * Automatic prerelease build

 -- Automated build system <noreply@cloud.com>  %s"""%(
			APPNAME,
			VERSION,
			Utils.getbuildnumber(),
			Options.options.PRERELEASE,
			email.Utils.formatdate(time.time())
		)
	else:
		tempchangelog = """%s (%s-1) stable; urgency=low

  * Automatic release build

 -- Automated build system <noreply@cloud.com>  %s"""%(
			APPNAME,
			VERSION,
			email.Utils.formatdate(time.time())
		)
	
	# FIXME wrap the source tarball in POSIX locking!
	if not Options.options.blddir: outputdir = _join(context.curdir,blddir,"debbuild")
	else:			   outputdir = _join(_abspath(Options.options.blddir),"debbuild")
	Utils.pprint("GREEN","Building DEBs")

	tarball = Scripting.dist('', VERSION)	
	srcdir = "%s/%s-%s"%(outputdir,APPNAME,VERSION)

	if _exists(srcdir): shutil.rmtree(srcdir)
	mkdir_p(outputdir)

	f = tarfile.open(tarball,'r:bz2')
	f.extractall(path=outputdir)
	if tempchangelog:
		f = file(_join(srcdir,"debian","changelog"),"w")
		f.write(tempchangelog)
		f.flush()
		f.close()
	
	checkdeps = lambda: c(["dpkg-checkbuilddeps"],srcdir)
	dodeb = lambda: c(["debuild",'-e','WAFCACHE','--no-lintian', '--no-tgz-check']+buildnumber+version+["-us","-uc"],srcdir)
	try: checkdeps()
	except (CalledProcessError,OSError),e:
		Utils.pprint("YELLOW","Dependencies might be missing.  Trying to auto-install them...")
		installdebdeps(context)
	dodeb()

def uninstallrpms(context):
	"""uninstalls any Cloud Stack RPMs on this system"""
	Utils.pprint("GREEN","Uninstalling any installed RPMs")
	cmd = "rpm -qa | grep %s- | xargs -r sudo rpm -e"%APPNAME
	Utils.pprint("BLUE",cmd)
	system(cmd)

def uninstalldebs(context):
	"""uninstalls any Cloud Stack DEBs on this system"""
	Utils.pprint("GREEN","Uninstalling any installed DEBs")
	cmd = "dpkg -l '%s-*' | grep ^i | awk '{ print $2 } ' | xargs aptitude purge -y"%APPNAME
	Utils.pprint("BLUE",cmd)
	system(cmd)

def viewrpmdeps(context):
	"""shows all the necessary dependencies to build the RPM packages of the stack"""
	for dep in getrpmdeps(): print dep

def viewdebdeps(context):
	"""shows all the necessary dependencies to build the DEB packages of the stack"""
	for dep in getdebdeps(): print dep

@throws_command_errors
def installrpmdeps(context):
	"""installs all the necessary dependencies to build the RPM packages of the stack"""
	runnable = ["sudo","yum","install","-y"]+list(getrpmdeps())
	Utils.pprint("GREEN","Installing RPM build dependencies")
	Utils.pprint("BLUE"," ".join(runnable))
	_check_call(runnable)

@throws_command_errors
def installdebdeps(context):
	"""installs all the necessary dependencies to build the DEB packages of the stack"""
	runnable = ["sudo","aptitude","install","-y"]+list( [ x.split()[0] for x in getdebdeps() ] )
	Utils.pprint("GREEN","Installing DEB build dependencies")
	Utils.pprint("BLUE"," ".join(runnable))
	_check_call(runnable)

@throws_command_errors
def deploydb(ctx,virttech=None):
	if not virttech: raise Utils.WafError('use deploydb_xenserver, deploydb_vmware or deploydb_kvm rather than deploydb')
	
	ctx = _getbuildcontext()
	setupdatabases = _join(ctx.env.BINDIR,"cloud-setup-databases")
	serversetup = _join(ctx.env.SETUPDATADIR,"server-setup.xml")
	
	if not _exists(setupdatabases): # Needs install!
		Scripting.install(ctx)

	cmd = [
		ctx.env.PYTHON,
		setupdatabases,
		"cloud@%s"%ctx.env.DBHOST,
		virttech,
		"--auto=%s"%serversetup,
                "--deploy-as=%s:%s"%(ctx.env.DBUSER,ctx.env.DBPW),
		]
	
	Utils.pprint("BLUE"," ".join(cmd))
	retcode = Utils.exec_command(cmd,shell=False,stdout=None,stderr=None,log=True)
	if retcode: raise CalledProcessError(retcode,cmd)
	
def deploydb_xenserver(ctx):
	"""re-deploys the database using the MySQL connection information and the XenServer templates.sql"""
	return deploydb(ctx,"xenserver")
def deploydb_kvm(ctx):
	"""re-deploys the database using the MySQL connection information and the KVM templates.sql"""
	return deploydb(ctx,"kvm")
def deploydb_vmware(ctx):
	"""re-deploys the database using the MySQL connection information and the KVM templates.sql"""
	return deploydb(ctx,"vmware")

def run(args):
	"""runs the management server"""
	conf = _getbuildcontext()

	runverbose = []
	if Options.options.RUNVERBOSE: runverbose = ['-verbose']
	if args == "debug":
		suspend = "n"
		if Options.options.DEBUGSUSPEND: suspend = "y"
		debugargs = [
			"-Xdebug","-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=%s"%(
				Options.options.DEBUGPORT,suspend),
			"-ea"]
		Utils.pprint("GREEN","Starting Tomcat in debug mode")
	else:
		Utils.pprint("GREEN","Starting Tomcat in foreground mode")
		debugargs = []

	options = runverbose + debugargs + [
		"-Dcatalina.base=" + conf.env.MSENVIRON,
		"-Dcatalina.home=" + conf.env.MSENVIRON,
		"-Djava.io.tmpdir="+_join(conf.env.MSENVIRON,"temp"), ]

	if not _exists(_join(conf.env.BINDIR,"cloud-setup-databases")): Scripting.install(conf)

	cp = [conf.env.MSCONF]
	cp += _glob(_join(conf.env.MSENVIRON,'bin',"*.jar"))
	cp += _glob(_join(conf.env.MSENVIRON,'lib',"*.jar"))
	cp += _glob( _join ( conf.env.PREMIUMJAVADIR , "*" ) )
	cp += [conf.env.SYSTEMCLASSPATH]
	cp += [conf.env.DEPSCLASSPATH]
	cp += [conf.env.MSCLASSPATH]

	# FIXME Make selectable at runtime
	#plugins = _glob(  _join(conf.env.PLUGINJAVADIR,"*")  )
	#if plugins: cp = plugins + cp
	#vendorconfs = _glob(  _join(conf.env.MSCONF,"vendor","*")  )
	#if vendorconfs: cp = plugins + cp

	run_java("org.apache.catalina.startup.Bootstrap",cp,options,["start"])

def debug(ctx):
	"""runs the management server in debug mode"""
	run("debug")

@throws_command_errors
def run_agent(args):
	"""runs the agent""" # FIXME: make this use the run/debug options
	conf = _getbuildcontext()
	if not _exists(_join(conf.env.LIBEXECDIR,"agent-runner")): Scripting.install(conf)
	_check_call("sudo",[_join(conf.env.LIBEXECDIR,"agent-runner")])

@throws_command_errors
def run_console_proxy(args):
	"""runs the console proxy""" # FIXME: make this use the run/debug options
	conf = _getbuildcontext()
	if not _exists(_join(conf.env.LIBEXECDIR,"console-proxy-runner")): Scripting.install(conf)
	_check_call("sudo",[_join(conf.env.LIBEXECDIR,"console-proxy-runner")])

def simulate_agent(args):
	"""runs the agent simulator, compiling and installing files as needed
	     - Any parameter specified after the simulate_agent is appended to
	       the java command line.  To inhibit waf from interpreting the
	       command-line options that you specify to the agent, put a --
	       (double-dash) between the waf simulate_agent and the options, like this:

                    python waf simulate_agent -- -z KY -p KY
"""
	
	# to get this to work smoothly from the configure onwards, you need to
	# create an override directory in java/agent/conf, then add an agent.properties
	# there, with the correct configuration that you desire
	# that is it -- you are now ready to simulate_agent
	conf = _getbuildcontext()
	args = sys.argv[sys.argv.index("simulate_agent")+1:]
	if '--' in args: args.remove('--')
	
	cp = [conf.env.AGENTSYSCONFDIR]
	cp += _glob( _join ( conf.env.PREMIUMJAVADIR , "*" ) )
	cp += [conf.env.SYSTEMCLASSPATH]
	cp += [conf.env.DEPSCLASSPATH]
	cp += [conf.env.AGENTSIMULATORCLASSPATH]

	if not _exists(_join(conf.env.LIBEXECDIR,"agent-runner")): Scripting.install(conf)

	run_java("com.cloud.agent.AgentSimulator",cp,arguments=args)

