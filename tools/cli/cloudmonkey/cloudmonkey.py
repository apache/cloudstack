#!/usr/bin/env python
# -*- coding: utf-8 -*-
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

try:
    import atexit
    import cmd
    import clint
    import codecs
    import logging
    import os
    import pdb
    import readline
    import rlcompleter
    import sys
    import types

    from clint.textui import colored
    from ConfigParser import ConfigParser, SafeConfigParser

    from marvin.cloudstackConnection import cloudConnection
    from marvin.cloudstackException import cloudstackAPIException
    from marvin.cloudstackAPI import *
    from marvin import cloudstackAPI
except ImportError, e:
    print "Import error in %s : %s" % (__name__, e)
    import sys
    sys.exit()

log_fmt = '%(asctime)s - %(filename)s:%(lineno)s - [%(levelname)s] %(message)s'
logger = logging.getLogger(__name__)
completions = cloudstackAPI.__all__


class CloudStackShell(cmd.Cmd):
    intro = "☁ Apache CloudStack CLI. Type help or ? to list commands.\n"
    ruler = "-"
    config_file = os.path.expanduser('~/.cloudmonkey_config')

    def __init__(self):
        self.config_fields = {'host': 'localhost', 'port': '8080',
                              'apiKey': '', 'secretKey': '',
                              'prompt': '🙉 cloudmonkey> ', 'color': 'true',
                              'log_file':
                              os.path.expanduser('~/.cloudmonkey_log'),
                              'history_file':
                              os.path.expanduser('~/.cloudmonkey_history')}
        if os.path.exists(self.config_file):
            config = self.read_config()
        else:
            for key in self.config_fields.keys():
                setattr(self, key, self.config_fields[key])
            config = self.write_config()
            print("Set your api and secret keys, host, port etc. using the set command!")

        for key in self.config_fields.keys():
            setattr(self, key, config.get('CLI', key))

        self.prompt += " "  # Cosmetic fix for prompt
        logging.basicConfig(filename=self.log_file,
                            level=logging.DEBUG, format=log_fmt)
        self.logger = logging.getLogger(self.__class__.__name__)

        cmd.Cmd.__init__(self)
        # Update config if config_file does not exist
        if not os.path.exists(self.config_file):
            config = self.write_config()

        # Fix autocompletion issue
        if sys.platform == "darwin":
            readline.parse_and_bind("bind ^I rl_complete")
        else:
            readline.parse_and_bind("tab: complete")

        # Enable history support
        try:
            if os.path.exists(self.history_file):
                readline.read_history_file(self.history_file)
            atexit.register(readline.write_history_file, self.history_file)
        except IOError:
            print("Error: history support")

    def read_config(self):
        config = ConfigParser()
        try:
            with open(self.config_file, 'r') as cfg:
                config.readfp(cfg)
            for section in config.sections():
                for option in config.options(section):
                    logger.debug("[%s] %s=%s" % (section, option,
                                                 config.get(section, option)))
        except IOError, e:
            self.print_shell("Error: config_file not found", e)
        return config

    def write_config(self):
        config = ConfigParser()
        config.add_section('CLI')
        for key in self.config_fields.keys():
            config.set('CLI', key, getattr(self, key))
        with open(self.config_file, 'w') as cfg:
            config.write(cfg)
        return config

    def emptyline(self):
        pass

    def print_shell(self, *args):
        try:
            for arg in args:
                if isinstance(type(args), types.NoneType):
                    continue
                if self.color == 'true':
                    if str(arg).count(self.ruler) == len(str(arg)):
                        print colored.green(arg),
                    elif 'type' in arg:
                        print colored.green(arg),
                    elif 'state' in arg:
                        print colored.yellow(arg),
                    elif 'id =' in arg:
                        print colored.cyan(arg),
                    elif 'name =' in arg:
                        print colored.magenta(arg),
                    elif ':' in arg:
                        print colored.blue(arg),
                    elif 'Error' in arg:
                        print colored.red(arg),
                    else:
                        print arg,
                else:
                    print arg,
            print
        except Exception, e:
            print colored.red("Error: "), e

    # FIXME: Fix result processing and printing
    def print_result(self, result, response, api_mod):
        def print_result_as_list():
            if result is None:
                return
            for node in result:
                print_result_as_instance(node)

        def print_result_as_instance(node):
            for attribute in dir(response):
                if "__" not in attribute:
                    attribute_value = getattr(node, attribute)
                    if isinstance(attribute_value, list):
                        self.print_shell("\n%s:" % attribute)
                        try:
                            self.print_result(attribute_value,
                                              getattr(api_mod, attribute)(),
                                              api_mod)
                        except AttributeError, e:
                            pass
                    elif attribute_value is not None:
                        self.print_shell("%s = %s" %
                                         (attribute, attribute_value))
            self.print_shell(self.ruler * 80)

        if result is None:
            return

        if type(result) is types.InstanceType:
            print_result_as_instance(result)
        elif isinstance(result, list):
            print_result_as_list()
        elif isinstance(result, str):
            print result
        elif isinstance(type(result), types.NoneType):
            print_result_as_instance(result)
        elif not (str(result) is None):
            self.print_shell(result)

    def do_quit(self, s):
        """
        Quit Apache CloudStack CLI
        """
        self.print_shell("Bye!")
        return True

    def do_shell(self, args):
        """
        Execute shell commands using shell <command> or !<command>
        Example: !ls or shell ls
        """
        os.system(args)

    def make_request(self, command, requests={}):
        conn = cloudConnection(self.host, port=int(self.port),
                               apiKey=self.apiKey, securityKey=self.secretKey,
                               logging=logging.getLogger("cloudConnection"))
        try:
            response = conn.make_request(command, requests)
        except cloudstackAPIException, e:
            self.print_shell("API Error", e)
            return None
        return response

    def default(self, args):
        args = args.split(" ")
        api_name = args[0]

        try:
            api_cmd_str = "%sCmd" % api_name
            api_rsp_str = "%sResponse" % api_name
            api_mod = __import__("marvin.cloudstackAPI.%s" % api_name,
                                 globals(), locals(), [api_cmd_str], -1)
            api_cmd = getattr(api_mod, api_cmd_str)
            api_rsp = getattr(api_mod, api_rsp_str)
        except ImportError, e:
            self.print_shell("Error: API %s not found!" % e)
            return

        command = api_cmd()
        response = api_rsp()
        #FIXME: Parsing logic
        args_dict = dict(map(lambda x: x.split("="),
                             args[1:])[x] for x in range(len(args) - 1))

        for attribute in dir(command):
            if attribute in args_dict:
                setattr(command, attribute, args_dict[attribute])

        result = self.make_request(command, response)
        try:
            self.print_result(result, response, api_mod)
        except Exception as e:
            self.print_shell("🙈  Error on parsing and printing", e)

    def completedefault(self, text, line, begidx, endidx):
        mline = line.partition(" ")[2]
        offs = len(mline) - len(text)
        return [s[offs:] for s in completions if s.startswith(mline)]

    def do_api(self, args):
        """
        Make raw api calls. Syntax: api <apiName> <args>=<values>. Example:
        api listAccount listall=true
        """
        if len(args) > 0:
            return self.default(args)
        else:
            self.print_shell("Please use a valid syntax")

    def complete_api(self, text, line, begidx, endidx):
        return self.completedefault(text, line, begidx, endidx)

    def do_set(self, args):
        """
        Set config for CloudStack CLI. Available options are:
        host, port, apiKey, secretKey, log_file, history_file
        """
        args = args.split(' ')
        if len(args) == 2:
            key, value = args
            # Note: keys and fields should have same names
            setattr(self, key, value)
            self.write_config()
        else:
            self.print_shell("Please use the syntax: set valid-key value")

    def complete_set(self, text, line, begidx, endidx):
        mline = line.partition(" ")[2]
        offs = len(mline) - len(text)
        return [s[offs:] for s in
               ['host', 'port', 'apiKey', 'secretKey', 'prompt', 'color',
                'log_file', 'history_file'] if s.startswith(mline)]


def main():
    grammar = ['create', 'list', 'delete', 'update',
               'enable', 'disable', 'add', 'remove', 'attach', 'detach',
               'assign', 'authorize', 'change', 'register',
               'start', 'restart', 'reboot', 'stop', 'reconnect',
               'cancel', 'destroy', 'revoke',
               'copy', 'extract', 'migrate', 'restore',
               'get', 'prepare', 'deploy', 'upload']

    self = CloudStackShell
    for rule in grammar:
        setattr(self, 'completions_' + rule, map(lambda x: x.replace(rule, ''),
                                                 filter(lambda x: rule in x,
                                                        completions)))

        def add_grammar(rule):
            def grammar_closure(self, args):
                self.default(rule + args)
            return grammar_closure

        grammar_handler = add_grammar(rule)
        grammar_handler.__doc__ = "%ss resources" % rule.capitalize()
        grammar_handler.__name__ = 'do_' + rule
        setattr(self, grammar_handler.__name__, grammar_handler)

        def add_completer(rule):
            def completer_closure(self, text, line, begidx, endidx):
                mline = line.partition(" ")[2]
                offs = len(mline) - len(text)
                return [s[offs:] for s in getattr(self, 'completions_' + rule)
                        if s.startswith(mline)]
            return completer_closure

        completion_handler = add_completer(rule)
        completion_handler.__name__ = 'complete_' + rule
        setattr(self, completion_handler.__name__, completion_handler)

    if len(sys.argv) > 1:
        CloudStackShell().onecmd(' '.join(sys.argv[1:]))
    else:
        CloudStackShell().cmdloop()

if __name__ == "__main__":
    main()
