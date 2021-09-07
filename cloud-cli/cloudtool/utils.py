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

 

'''
Created on Aug 2, 2010

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
        cmd_name = cmd_callable.__name__.replace("_","-")
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

    parser.api_dests = []
    if api_callable and api_options:
        group = parser.add_option_group("Options for the %s API"%api_name)
        for a in api_options:
            group.add_option(a[0][0],**a[1])
            parser.api_dests.append(a[1]["dest"])
 
    parser.cmd_dests = []
    if cmd_callable and cmd_options:
        group = parser.add_option_group("Options for the %s command"%cmd_name)
        for a in cmd_options:
            group.add_option(a[0][0],**a[1])
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
		if cmd.__doc__:docstring = cmd.__doc__
		else:docstring = ''
		cmds.append( "    %s" % (cmd_name.replace('_','-')) )
        return cmds
