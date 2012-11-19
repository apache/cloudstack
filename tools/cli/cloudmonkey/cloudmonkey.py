#!/usr/bin/python
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
    import json
    import logging
    import os
    import pdb
    import sets
    import shlex
    import sys
    import time
    import types

    from clint.textui import colored
    from ConfigParser import ConfigParser, SafeConfigParser
    from urllib2 import HTTPError, URLError

    from common import __version__, config_file, config_fields
    from common import grammar, precached_verbs
    from marvin.cloudstackConnection import cloudConnection
    from marvin.cloudstackException import cloudstackAPIException
    from marvin.cloudstackAPI import *
    from marvin import cloudstackAPI
except ImportError, e:
    print "Import error in %s : %s" % (__name__, e)
    import sys
    sys.exit()

# Fix autocompletion issue, can be put in .pythonstartup
try:
    import readline
except ImportError, e:
    print "Module readline not found, autocompletions will fail", e
else:
    import rlcompleter
    if 'libedit' in readline.__doc__:
        readline.parse_and_bind("bind ^I rl_complete")
    else:
        readline.parse_and_bind("tab: complete")

log_fmt = '%(asctime)s - %(filename)s:%(lineno)s - [%(levelname)s] %(message)s'
logger = logging.getLogger(__name__)
completions = cloudstackAPI.__all__


class CloudStackShell(cmd.Cmd):
    intro = ("☁ Apache CloudStack 🐵 cloudmonkey " + __version__ +
             ". Type help or ? to list commands.\n")
    ruler = "="
    config_file = config_file
    config_fields = config_fields
    grammar = grammar
    # datastructure {'verb': {cmd': ['api', [params], doc, required=[]]}}
    cache_verbs = precached_verbs

    def __init__(self):
        if os.path.exists(self.config_file):
            config = self.read_config()
        else:
            for key in self.config_fields.keys():
                setattr(self, key, self.config_fields[key])
            config = self.write_config()
            print("Set your apikey, secretkey, host, port, prompt,"
                  " protocol, path, color, log_file and history_file"
                  " using the set command!")

        for key in self.config_fields.keys():
            try:
                setattr(self, key, config.get('CLI', key))
            except Exception:
                print "Please fix `%s` config in %s" % (key, self.config_file)
                sys.exit()

        self.prompt += " "  # Cosmetic fix for prompt
        logging.basicConfig(filename=self.log_file,
                            level=logging.DEBUG, format=log_fmt)

        cmd.Cmd.__init__(self)
        # Update config if config_file does not exist
        if not os.path.exists(self.config_file):
            config = self.write_config()

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
                arg = str(arg)
                if isinstance(type(args), types.NoneType):
                    continue
                if self.color == 'true':
                    if str(arg).count(self.ruler) == len(str(arg)):
                        print colored.green(arg),
                    elif ":\n=" in arg:
                        print colored.red(arg),
                    elif ':' in arg:
                        print colored.blue(arg),
                    elif 'type' in arg:
                        print colored.green(arg),
                    elif 'state' in arg or 'count' in arg:
                        print colored.yellow(arg),
                    elif 'id =' in arg:
                        print colored.cyan(arg),
                    elif 'name =' in arg:
                        print colored.magenta(arg),
                    elif 'Error' in arg:
                        print colored.red(arg),
                    else:
                        print arg,
                else:
                    print arg,
            print
        except Exception, e:
            print colored.red("Error: "), e

    def print_result(self, result):
        if result is None or len(result) == 0:
            return

        def print_result_as_dict(result):
            for key in result.keys():
                if not (isinstance(result[key], list) or
                        isinstance(result[key], dict)):
                    self.print_shell("%s = %s" % (key, result[key]))
                else:
                    self.print_shell(key + ":\n" + len(key) * "=")
                    self.print_result(result[key])

        def print_result_as_list(result):
            for node in result:
                self.print_result(node)
                if len(result) > 1:
                    self.print_shell(self.ruler * 80)

        if isinstance(result, dict):
            print_result_as_dict(result)
        elif isinstance(result, list):
            print_result_as_list(result)
        elif isinstance(result, str):
            print result
        elif not (str(result) is None):
            self.print_shell(result)

    def make_request(self, command, requests={}, isAsync=False):
        conn = cloudConnection(self.host, port=int(self.port),
                               apiKey=self.apikey, securityKey=self.secretkey,
                               asyncTimeout=self.timeout, logging=logger,
                               protocol=self.protocol, path=self.path)
        response = None
        try:
            self.print_shell("Starting call")
            response = conn.make_request_with_auth(command, requests)
            self.print_shell("Ending call")
        except cloudstackAPIException, e:
            self.print_shell("API Error:", e)
        except HTTPError, e:
            self.print_shell(e)
        except URLError, e:
            self.print_shell("Connection Error:", e)

        def process_json(response):
            try:
                response = json.loads(str(response))
            except ValueError, e:
                pass
            return response

        response = process_json(response)
        if response is None:
            return

        isAsync = isAsync and (self.asyncblock == "true")
        if isAsync and 'jobid' in response[response.keys()[0]]:
            jobId = response[response.keys()[0]]['jobid']
            command = "queryAsyncJobResult"
            requests = {'jobid': jobId}
            timeout = int(self.timeout)
            while timeout > 0:
                response = process_json(conn.make_request_with_auth(command,
                                                                    requests))
                result = response[response.keys()[0]]
                jobstatus = result['jobstatus']
                if jobstatus == 2:
                    jobresult = result["jobresult"]
                    self.print_shell("Async query failed for jobid=",
                                     jobId, "\nError", jobresult["errorcode"],
                                     jobresult["errortext"])
                    return
                elif jobstatus == 1:
                    return response
                time.sleep(4)
                timeout = timeout - 4
                logger.debug("job: %s to timeout in %ds" % (jobId, timeout))
            self.print_shell("Error:", "Async query timeout for jobid=", jobId)

        return response

    def get_api_module(self, api_name, api_class_strs=[]):
        try:
            api_mod = __import__("marvin.cloudstackAPI.%s" % api_name,
                                 globals(), locals(), api_class_strs, -1)
        except ImportError, e:
            self.print_shell("Error: API not found", e)
            return None
        return api_mod

    def default(self, args):
        lexp = shlex.shlex(args.strip())
        lexp.whitespace = " "
        lexp.whitespace_split = True
        lexp.posix = True
        args = []
        while True:
            next_val = lexp.next()
            if next_val is None:
                break
            args.append(next_val)
        api_name = args[0]

        args_dict = dict(map(lambda x: [x.partition("=")[0],
                                        x.partition("=")[2]],
                             args[1:])[x] for x in range(len(args) - 1))

        # FIXME: With precaching, dynamic loading can be removed
        api_cmd_str = "%sCmd" % api_name
        api_mod = self.get_api_module(api_name, [api_cmd_str])
        if api_mod is None:
            return

        try:
            api_cmd = getattr(api_mod, api_cmd_str)
        except AttributeError, e:
            self.print_shell("Error: API attribute %s not found!" % e)
            return

        for attribute in args_dict.keys():
            setattr(api_cmd, attribute, args_dict[attribute])

        command = api_cmd()
        missing_args = list(sets.Set(command.required).difference(
                            sets.Set(args_dict.keys())))

        if len(missing_args) > 0:
            self.print_shell("Missing arguments:", ' '.join(missing_args))
            return

        isAsync = False
        if "isAsync" in dir(command):
            isAsync = (command.isAsync == "true")

        result = self.make_request(api_name, args_dict, isAsync)
        if result is None:
            return
        try:
            self.print_result(result.values())
            print
        except Exception as e:
            self.print_shell("🙈  Error on parsing and printing", e)

    def cache_verb_miss(self, verb):
        self.print_shell("Oops: Verb %s should have been precached" % verb)
        completions_found = filter(lambda x: x.startswith(verb), completions)
        self.cache_verbs[verb] = {}
        for api_name in completions_found:
            api_cmd_str = "%sCmd" % api_name
            api_mod = self.get_api_module(api_name, [api_cmd_str])
            if api_mod is None:
                continue
            try:
                api_cmd = getattr(api_mod, api_cmd_str)()
                required = api_cmd.required
                doc = api_mod.__doc__
            except AttributeError, e:
                self.print_shell("Error: API attribute %s not found!" % e)
            params = filter(lambda x: '__' not in x and 'required' not in x,
                            dir(api_cmd))
            if len(required) > 0:
                doc += "\nRequired args: %s" % " ".join(required)
            doc += "\nArgs: %s" % " ".join(params)
            api_name_lower = api_name.replace(verb, '').lower()
            self.cache_verbs[verb][api_name_lower] = [api_name, params, doc,
                                                      required]

    def completedefault(self, text, line, begidx, endidx):
        partitions = line.partition(" ")
        verb = partitions[0]
        rline = partitions[2].partition(" ")
        subject = rline[0]
        separator = rline[1]
        params = rline[2]

        if verb not in self.grammar:
            return []

        autocompletions = []
        search_string = ""

        if verb not in self.cache_verbs:
            self.cache_verb_miss(verb)

        if separator != " ":   # Complete verb subjects
            autocompletions = self.cache_verbs[verb].keys()
            search_string = subject
        else:                  # Complete subject params
            autocompletions = map(lambda x: x + "=",
                                  self.cache_verbs[verb][subject][1])
            search_string = text

        return [s for s in autocompletions if s.startswith(search_string)]

    def do_api(self, args):
        """
        Make raw api calls. Syntax: api <apiName> <args>=<values>.

        Example:
        api listAccount listall=true
        """
        if len(args) > 0:
            return self.default(args)
        else:
            self.print_shell("Please use a valid syntax")

    def complete_api(self, text, line, begidx, endidx):
        mline = line.partition(" ")[2]
        offs = len(mline) - len(text)
        return [s[offs:] for s in completions if s.startswith(mline)]

    def do_set(self, args):
        """
        Set config for CloudStack CLI. Available options are:
        host, port, apikey, secretkey, log_file, history_file
        You may also edit your ~/.cloudmonkey_config instead of using set.

        Example:
        set host 192.168.56.2
        set prompt 🐵 cloudmonkey>
        set log_file /var/log/cloudmonkey.log
        """
        args = args.strip().partition(" ")
        key, value = (args[0], args[2])
        # Note: keys and class attributes should have same names
        setattr(self, key, value)
        self.write_config()

    def complete_set(self, text, line, begidx, endidx):
        mline = line.partition(" ")[2]
        offs = len(mline) - len(text)
        return [s[offs:] for s in self.config_fields.keys()
                if s.startswith(mline)]

    def do_shell(self, args):
        """
        Execute shell commands using shell <command> or !<command>

        Example:
        !ls
        shell ls
        !for((i=0; i<10; i++)); do cloudmonkey create user account=admin \
            email=test@test.tt firstname=user$i lastname=user$i \
            password=password username=user$i; done
        """
        os.system(args)

    def do_help(self, args):
        """
        Show help docs for various topics

        Example:
        help list
        help list users
        ?list
        ?list users
        """
        fields = args.partition(" ")
        if fields[2] == "":
            cmd.Cmd.do_help(self, args)
        else:
            verb = fields[0]
            subject = fields[2].partition(" ")[0]
            if verb not in self.cache_verbs:
                self.cache_verb_miss(verb)

            if subject in self.cache_verbs[verb]:
                self.print_shell(self.cache_verbs[verb][subject][2])
            else:
                self.print_shell("Error: no such api (%s) on %s" %
                                 (subject, verb))

    def complete_help(self, text, line, begidx, endidx):
        fields = line.partition(" ")
        subfields = fields[2].partition(" ")

        if subfields[1] != " ":
            return cmd.Cmd.complete_help(self, text, line, begidx, endidx)
        else:
            line = fields[2]
            text = subfields[2]
            return self.completedefault(text, line, begidx, endidx)

    def do_quit(self, args):
        """
        Quit Apache CloudStack CLI
        """
        self.print_shell("Bye!")
        return self.do_EOF(args)

    def do_EOF(self, args):
        """
        Quit on Ctrl+d or EOF
        """
        return True


def main():
    # Create handlers on the fly using closures
    self = CloudStackShell
    global grammar
    for rule in grammar:
        def add_grammar(rule):
            def grammar_closure(self, args):
                if '|' in args:  # FIXME: Consider parsing issues
                    prog_name = sys.argv[0]
                    if '.py' in prog_name:
                        prog_name = "python " + prog_name
                    self.do_shell("%s %s %s" % (prog_name, rule, args))
                    return
                if not rule in self.cache_verbs:
                    self.cache_verb_miss(rule)
                try:
                    args_partition = args.partition(" ")
                    res = self.cache_verbs[rule][args_partition[0]]

                except KeyError, e:
                    self.print_shell("Error: no such command on %s" % rule)
                    return
                if ' --help' in args or ' -h' in args:
                    self.print_shell(res[2])
                    return
                self.default(res[0] + " " + args_partition[2])
            return grammar_closure

        grammar_handler = add_grammar(rule)
        grammar_handler.__doc__ = "%ss resources" % rule.capitalize()
        grammar_handler.__name__ = 'do_' + rule
        setattr(self, grammar_handler.__name__, grammar_handler)

    shell = CloudStackShell()
    if len(sys.argv) > 1:
        shell.onecmd(' '.join(sys.argv[1:]))
    else:
        shell.cmdloop()

if __name__ == "__main__":
    main()
