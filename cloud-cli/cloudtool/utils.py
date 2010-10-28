'''
Created on Aug 2, 2010

@author: rudd-o
'''


import sys
import os
import inspect
from optparse import OptionParser, OptParseError, BadOptionError, OptionError, OptionConflictError, OptionValueError
import cloudapis as apis


def describe(name,desc):
    def inner(decoratee):
        if not hasattr(decoratee,"descriptions"): decoratee.descriptions = {}
        decoratee.descriptions[name] = desc
        return decoratee
    return inner


def error(msg):
    sys.stderr.write(msg)
    sys.stderr.write("\n")


class MyOptionParser(OptionParser):
    def error(self, msg):
        error("%s: %s\n" % (self.get_prog_name(),msg))
        self.print_usage(sys.stderr)
        self.exit(os.EX_USAGE)
        
    def parse_args(self,*args,**kwargs):
        options,arguments = OptionParser.parse_args(self,*args,**kwargs)
        
        def prune_options(options,alist):
            """Given 'options' -- a list of arguments to OptionParser.add_option,
            and a set of optparse Values, return a dictionary of only those values
            that apply exclusively to 'options'"""
            return dict( [ (k,getattr(options,k)) for k in dir(options) if k in alist ] )
        
        api_options = prune_options(options,self.api_dests)
        cmd_options = prune_options(options,self.cmd_dests)
        
        return options,arguments,api_options,cmd_options


def get_parser(api_callable=None,cmd_callable=None): # this should probably be the __init__ method of myoptionparser
    
    def getdefaulttag(default):
        if default is not None: return " [Default: %default]"
        return ''
    
    def get_arguments_and_options(callable):
        """Infers and returns arguments and options based on a callable's signature.
        Cooperates with decorator @describe"""
	try:
        	funcargs = inspect.getargspec(callable).args
        	defaults = inspect.getargspec(callable).defaults
	except:
		funcargs = inspect.getargspec(callable)[0]
        	defaults = inspect.getargspec(callable)[3]
        if not defaults: defaults = []
        args = funcargs[1:len(funcargs)-len(defaults)] # this assumes self, so assumes methods
        opts = funcargs[len(funcargs)-len(defaults):]
        try: descriptions = callable.descriptions
        except AttributeError: descriptions = {}
        arguments = [ (argname, descriptions.get(argname,'') ) for argname in args ]
        options = [ [
                          ("--%s"%argname.replace("_","-"),),
                          {
                           "dest":argname,
                           "help":descriptions.get(argname,'') + getdefaulttag(default),
                           "default":default,
                          }
                        ] for argname,default in zip(opts,defaults) ]
        return arguments,options

    basic_usage = "usage: %prog [options...] "
    
    api_name = "<api>"
    cmd_name = "<command>"
    description = "%prog is a command-line tool to access several cloud APIs."
    arguments = ''
    argexp = ""
    
    if api_callable:
        api_name = api_callable.__module__.split(".")[-1].replace("_","-")
        api_arguments,api_options = get_arguments_and_options(api_callable)
        assert len(api_arguments) is 0 # no mandatory arguments for class initializers
        
    if cmd_callable:
        cmd_name = cmd_callable.func_name.replace("_","-")
        cmd_arguments,cmd_options = get_arguments_and_options(cmd_callable)
        if cmd_arguments:
            arguments   = " " + " ".join( [ s[0].upper() for s in cmd_arguments ] )
            argexp = "\n\nArguments:\n" + "\n".join ( "  %s\n                        %s"%(s.upper(),u) for s,u in cmd_arguments )
        description = cmd_callable.__doc__
       
    api_command = "%s %s"%(api_name,cmd_name)

    if description: description = "\n\n" + description
    else: description = ''
        
    usage = basic_usage + api_command + arguments + description + argexp

    parser = MyOptionParser(usage=usage, add_help_option=False)
    
    parser.add_option('--help', action="help")
 
    group = parser.add_option_group("General options")
    group.add_option('-v', '--verbose', dest="verbose", help="Print extra output")

    # now we need to derive the short options.  we initialize the known fixed longopts and shortopts
    longopts = [ '--verbose' ]

    # we add the known long options, sorted and grouped to guarantee a stable short opt set regardless of order
    if api_callable and api_options:
        longopts += sorted([ x[0][0] for x in api_options ])
    if cmd_callable and cmd_options:
        longopts += sorted([ x[0][0] for x in cmd_options ])

    # we use this function to derive a suitable short option and remember the already-used short options
    def derive_shortopt(longopt,usedopts):
        """longopt begins with a dash"""
        shortopt = None
        for x in xrange(2,10000):
            try: shortopt = "-" + longopt[x]
            except IndexError:
                shortopt = None
                break
            if shortopt in usedopts: continue
            usedopts.append(shortopt)
            break
        return shortopt

    # now we loop through the long options and assign a suitable short option, saving the short option for later use
    long_to_short = {}
    alreadyusedshorts = []
    for longopt in longopts:
        long_to_short[longopt] = derive_shortopt(longopt,alreadyusedshorts)

    parser.api_dests = []
    if api_callable and api_options:
        group = parser.add_option_group("Options for the %s API"%api_name)
        for a in api_options:
            shortopt = long_to_short[a[0][0]]
            if shortopt: group.add_option(shortopt,a[0][0],**a[1])
            else: group.add_option(a[0][0],**a[1])
            parser.api_dests.append(a[1]["dest"])
 
    parser.cmd_dests = []
    if cmd_callable and cmd_options:
        group = parser.add_option_group("Options for the %s command"%cmd_name)
        for a in cmd_options:
            shortopt = long_to_short[a[0][0]]
            if shortopt: group.add_option(shortopt,a[0][0],**a[1])
            else: group.add_option(a[0][0],**a[1])
            parser.cmd_dests.append(a[1]["dest"])
 
    return parser

def lookup_command_in_api(api,command_name):
    command = getattr(api,command_name.replace("-","_"),None)
    return command

def get_api_list(api):
	apilist = []
	for cmd_name in dir(api):
		cmd = getattr(api,cmd_name)
            	if callable(cmd) and not cmd_name.startswith("_"):
			apilist.append(cmd_name)	
	return apilist

def get_command_list(api):
        cmds = []
        for cmd_name in dir(api):
            cmd = getattr(api,cmd_name)
            if callable(cmd) and not cmd_name.startswith("_"):
		if cmd.__doc__: docstring = cmd.__doc__
		else: docstring = ''
		cmds.append( "    %s	%s"%(cmd_name.replace('_','-'),docstring) )
        return cmds
