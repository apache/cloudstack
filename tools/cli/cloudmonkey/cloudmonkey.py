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
    import codecs
    import json
    import logging
    import os
    import pdb
    import re
    import shlex
    import sys
    import time
    import types

    from ConfigParser import ConfigParser, SafeConfigParser
    from urllib2 import HTTPError, URLError
    from httplib import BadStatusLine

    from prettytable import PrettyTable
    from common import __version__, config_dir, config_file, config_fields
    from common import precached_verbs
    from lexer import monkeyprint

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


class CloudMonkeyShell(cmd.Cmd, object):
    intro = ("☁ Apache CloudStack 🐵 cloudmonkey " + __version__ +
             ". Type help or ? to list commands.\n")
    ruler = "="
    config_dir = config_dir
    config_file = config_file
    # datastructure {'verb': {cmd': ['api', [params], doc, required=[]]}}
    cache_verbs = precached_verbs
    config_options = []

    def __init__(self, pname, verbs):
        self.program_name = pname
        self.verbs = verbs
        global config_fields
        first_time = False
        if not os.path.exists(self.config_dir):
            os.makedirs(self.config_dir)
        if os.path.exists(self.config_file):
            config = self.read_config()
        else:
            first_time = True
            config = self.write_config(first_time)

        for section in config_fields.keys():
            for key in config_fields[section].keys():
                try:
                    self.config_options.append(key)
                    setattr(self, key, config.get(section, key))
                except Exception:
                    print "Please fix `%s` in %s" % (key, self.config_file)
                    sys.exit()

        if first_time:
            print "Welcome! Using `set` configure the necessary settings:"
            print " ".join(sorted(self.config_options))
            print "Config file:", self.config_file
            print "For debugging, tail -f", self.log_file, "\n"

        self.prompt = self.prompt.strip() + " "  # Cosmetic fix for prompt

        logging.basicConfig(filename=self.log_file,
                            level=logging.DEBUG, format=log_fmt)
        logger.debug("Loaded config fields:\n%s" % map(lambda x: "%s=%s" %
                                                       (x, getattr(self, x)),
                                                       self.config_options))

        cmd.Cmd.__init__(self)
        if not os.path.exists(self.config_file):
            config = self.write_config()

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
        except IOError, e:
            self.print_shell("Error: config_file not found", e)
        return config

    def write_config(self, first_time=False):
        global config_fields
        config = ConfigParser()
        for section in config_fields.keys():
            config.add_section(section)
            for key in config_fields[section].keys():
                if first_time:
                    config.set(section, key, config_fields[section][key])
                else:
                    config.set(section, key, getattr(self, key))
        with open(self.config_file, 'w') as cfg:
            config.write(cfg)
        return config

    def emptyline(self):
        pass

    def cmdloop(self, intro=None):
        print self.intro
        while True:
            try:
                super(CloudMonkeyShell, self).cmdloop(intro="")
                self.postloop()
            except KeyboardInterrupt:
                print("^C")

    def print_shell(self, *args):
        output = ""
        try:
            for arg in args:
                arg = str(arg)
                if isinstance(type(args), types.NoneType):
                    continue
                output += arg
            if self.color == 'true':
                monkeyprint(output)
            else:
                print output
        except Exception, e:
            self.print_shell("Error: " + e)

    def print_result(self, result, result_filter=None):
        if result is None or len(result) == 0:
            return

        def printer_helper(printer, toprow):
            if printer:
                self.print_shell(printer)
            return PrettyTable(toprow)

        def print_result_tabular(result, result_filter=None):
            toprow = None
            printer = None
            for node in result:
                if toprow != node.keys():
                    if result_filter is not None and len(result_filter) != 0:
                        commonkeys = filter(lambda x: x in node.keys(),
                                            result_filter)
                        if commonkeys != toprow:
                            toprow = commonkeys
                            printer = printer_helper(printer, toprow)
                    else:
                        toprow = node.keys()
                        printer = printer_helper(printer, toprow)
                row = map(lambda x: node[x], toprow)
                if printer and row:
                    printer.add_row(row)
            if printer:
                self.print_shell(printer)

        def print_result_as_dict(result, result_filter=None):
            for key in sorted(result.keys(), key=lambda x:
                              x not in ['id', 'count', 'name'] and x):
                if not (isinstance(result[key], list) or
                        isinstance(result[key], dict)):
                    self.print_shell("%s = %s" % (key, result[key]))
                else:
                    self.print_shell(key + ":")
                    self.print_result(result[key], result_filter)

        def print_result_as_list(result, result_filter=None):
            for node in result:
                # Tabular print if it's a list of dict and tabularize is true
                if isinstance(node, dict) and self.tabularize == 'true':
                    print_result_tabular(result, result_filter)
                    break
                self.print_result(node)
                if len(result) > 1:
                    self.print_shell(self.ruler * 80)

        if isinstance(result, dict):
            print_result_as_dict(result, result_filter)
        elif isinstance(result, list):
            print_result_as_list(result, result_filter)
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
        logger.debug("====START Request====")
        logger.debug("Requesting command=%s, args=%s" % (command, requests))
        try:
            response = conn.make_request_with_auth(command, requests)
        except cloudstackAPIException, e:
            self.print_shell("API Error:", e)
        except HTTPError, e:
            self.print_shell(e)
        except (URLError, BadStatusLine), e:
            self.print_shell("Connection Error:", e)
        logger.debug("====END Request====\n")

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
        responsekey = filter(lambda x: 'response' in x, response.keys())[0]
        if isAsync and 'jobid' in response[responsekey]:
            jobId = response[responsekey]['jobid']
            command = "queryAsyncJobResult"
            requests = {'jobid': jobId}
            timeout = int(self.timeout)
            pollperiod = 3
            progress = 1
            while timeout > 0:
                print '\r' + '.' * progress,
                sys.stdout.flush()
                response = process_json(conn.make_request_with_auth(command,
                                                                    requests))
                responsekeys = filter(lambda x: 'response' in x,
                                      response.keys())
                if len(responsekeys) < 1:
                    continue
                result = response[responsekeys[0]]
                jobstatus = result['jobstatus']
                if jobstatus == 2:
                    jobresult = result["jobresult"]
                    self.print_shell("\rAsync query failed for jobid",
                                     jobId, "\nError", jobresult["errorcode"],
                                     jobresult["errortext"])
                    return
                elif jobstatus == 1:
                    print '\r',
                    return response
                time.sleep(pollperiod)
                timeout = timeout - pollperiod
                progress += 1
                logger.debug("job: %s to timeout in %ds" % (jobId, timeout))
            self.print_shell("Error:", "Async query timeout for jobid", jobId)

        return response

    def get_api_module(self, api_name, api_class_strs=[]):
        try:
            api_mod = __import__("marvin.cloudstackAPI.%s" % api_name,
                                 globals(), locals(), api_class_strs, -1)
        except ImportError, e:
            self.print_shell("Error: API not found", e)
            return None
        return api_mod

    def pipe_runner(self, args):
        if args.find(' |') > -1:
            pname = self.program_name
            if '.py' in pname:
                pname = "python " + pname
            self.do_shell("%s %s" % (pname, args))
            return True
        return False

    def default(self, args):
        if self.pipe_runner(args):
            return

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
        field_filter = None
        if 'filter' in args_dict:
            field_filter = filter(lambda x: x is not '',
                                  map(lambda x: x.strip(),
                                      args_dict.pop('filter').split(',')))

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
        missing_args = filter(lambda x: x not in args_dict.keys(),
                              command.required)

        if len(missing_args) > 0:
            self.print_shell("Missing arguments: ", ' '.join(missing_args))
            return

        isAsync = False
        if "isAsync" in dir(command):
            isAsync = (command.isAsync == "true")

        result = self.make_request(api_name, args_dict, isAsync)
        if result is None:
            return
        try:
            responsekeys = filter(lambda x: 'response' in x, result.keys())
            for responsekey in responsekeys:
                self.print_result(result[responsekey], field_filter)
            print
        except Exception as e:
            self.print_shell("🙈  Error on parsing and printing", e)

    def completedefault(self, text, line, begidx, endidx):
        partitions = line.partition(" ")
        verb = partitions[0].strip()
        rline = partitions[2].lstrip().partition(" ")
        subject = rline[0]
        separator = rline[1]
        params = rline[2].lstrip()

        if verb not in self.verbs:
            return []

        autocompletions = []
        search_string = ""

        if separator != " ":   # Complete verb subjects
            autocompletions = self.cache_verbs[verb].keys()
            search_string = subject
        else:                  # Complete subject params
            autocompletions = map(lambda x: x + "=",
                                  self.cache_verbs[verb][subject][1])
            search_string = text

        if self.tabularize == "true" and subject != "":
            autocompletions.append("filter=")
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
        Set config for cloudmonkey. For example, options can be:
        host, port, apikey, secretkey, log_file, history_file
        You may also edit your ~/.cloudmonkey_config instead of using set.

        Example:
        set host 192.168.56.2
        set prompt 🐵 cloudmonkey>
        set log_file /var/log/cloudmonkey.log
        """
        args = args.strip().partition(" ")
        key, value = (args[0], args[2])
        setattr(self, key, value)  # keys and attributes should have same names
        self.prompt = self.prompt.strip() + " "  # prompt fix
        self.write_config()

    def complete_set(self, text, line, begidx, endidx):
        mline = line.partition(" ")[2]
        offs = len(mline) - len(text)
        return [s[offs:] for s in self.config_options
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

    def do_exit(self, args):
        """
        Quit CloudMonkey CLI
        """
        return self.do_quit(args)

    def do_quit(self, args):
        """
        Quit CloudMonkey CLI
        """
        self.print_shell("Bye!")
        return self.do_EOF(args)

    def do_EOF(self, args):
        """
        Quit on Ctrl+d or EOF
        """
        sys.exit()


def main():
    pattern = re.compile("[A-Z]")
    verbs = list(set([x[:pattern.search(x).start()] for x in completions
                 if pattern.search(x) is not None]).difference(['cloudstack']))
    for verb in verbs:
        def add_grammar(verb):
            def grammar_closure(self, args):
                if self.pipe_runner("%s %s" % (verb, args)):
                    return
                try:
                    args_partition = args.partition(" ")
                    res = self.cache_verbs[verb][args_partition[0]]
                    cmd = res[0]
                    helpdoc = res[2]
                    args = args_partition[2]
                except KeyError, e:
                    self.print_shell("Error: invalid %s api arg" % verb, e)
                    return
                if ' --help' in args or ' -h' in args:
                    self.print_shell(helpdoc)
                    return
                self.default("%s %s" % (cmd, args))
            return grammar_closure

        grammar_handler = add_grammar(verb)
        grammar_handler.__doc__ = "%ss resources" % verb.capitalize()
        grammar_handler.__name__ = 'do_' + verb
        setattr(CloudMonkeyShell, grammar_handler.__name__, grammar_handler)

    shell = CloudMonkeyShell(sys.argv[0], verbs)
    if len(sys.argv) > 1:
        shell.onecmd(' '.join(sys.argv[1:]))
    else:
        shell.cmdloop()

if __name__ == "__main__":
    main()
