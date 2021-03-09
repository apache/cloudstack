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

import sys
import os
from optparse import OptionParser
import cmd
import random
from collections import OrderedDict
from marvin.marvinInit import MarvinInit
from marvin.deployDataCenter import DeployDataCenters
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.codegenerator import CodeGenerator
from marvin.codes import (SUCCESS,
                          FAILED,
                          EXCEPTION
                          )
from marvin.tcExecuteEngine import TestCaseExecuteEngine


class VerifyAndExit(object):

    def __init__(self, msg):
        self.msg = msg

    def __call__(self, original_func):
        def new_function(*args, **kwargs):
            exit_check = False
            try:
                if original_func(*args, **kwargs) == FAILED:
                    exit_check = True
            except Exception as e:
                print("===Exception.Please Check:===", e)
                exit_check = True
            finally:
                if exit_check:
                    print("==== %s ====" % self.msg)
                    MarvinCliHelp.print_cmds_help()
                    sys.exit(1)
        return new_function


class MarvinCliCommands(object):
    cmds_info = OrderedDict({
        'deploydc':
            {
                'summary': 'for deploying a datacenter',
                'options': ['*config-file'],
                'help': 'marvincli deploydc config-file=<marvin-config-file EX: setup/dev/advanced.cfg file>',
                'desc': 'deploys a data center using the config file provided'
            },
        'deploydc_and_runtest':
            {
                'summary': 'for deploying a datacenter (and) running tests, either test suite (or) directory of test suites',
                'options': ['*config-file', '*tc-path', 'zone', 'hyp-type', 'required_hardware'],
                'help': 'marvincli deploydc_and_runtest config-file=<path_to_marvin_cfg EX: setup/dev/advanced.cfg>'
                'tc-path=<test suite or test suite folder path EX: test/integration/smoke/>'
                'zone=<name of the zone> hyp-type=<hypervisor_type EX: xen,kvm,vmware etc> required_hardware=<true\\false>',
                'desc': 'deploys a data center using the config file provided, and runs test cases using the test suite or directory of test suites provided. '
                'If zone to run against is not provided, then default zone mentioned in config file is provided '
                'If hyp-type information is not provided, first hypervisor from config file is taken. '
                'If required_hardware option is not provided, then it is set to false'
            },
        'generateapis_from_endpoint':
            {
                'summary': 'for generating apis from cs end point',
                'options': ['*cs-folder-path', 'end-point'],
                'help': 'marvincli generateapis_from_endpoint cs-folder-path=<cloudstack code root dir EX: /root/cs-4.5/cloudstack/>'
                'end-point=<CS Endpoint ip EX: localhost>',
                'desc': 'generates cloudstackAPI directory with CS apis information from cloudstack endpoint. '
                'If end-point information is not provided, localhost is considered as default'

            },
        'generateapis_from_apispecfile':
            {
                'summary': 'for generating apis from api spec file',
                'options': ['*cs-folder-path', 'api-spec-file'],
                'help': 'marvincli generateapis_from_apispecfile cs-folder-path=<cloudstack code root dir EX: /root/cs-4.5/cloudstack/>'
                'api-spec-file=<api spec file EX: /etc/cloud/cli/commands.xml>',
                'desc': 'generates cloudstackAPI directory with CS apis information from cloudstack api spec file. '
                'If spec file information is not provided, /etc/cloud/cli/commands.xml is considered as default'
            },
        'runtest':
            {
                'summary': 'for running test cases, either test suite (or) directory of test suites',
                'options': ['*config-file', '*tc-path', 'required_hardware', 'zone', 'hyp-type'],
                'help': 'marvincli runtest config-file=<path_to_marvin_config> tc-path=test/integration/smoke'
                'required_hardware=<true\\false> zone=<name of zone> hyp-type=<xenserver\\kvm\\vmware> etc',
                'desc': 'runs marvin integration tests against CS using config file, test suite path or directory of test suites are provided as input for running tests',
            },
        'sync_and_install':
            {
                'summary': 'for syncing apis and installing marvin using cs endpoint',
                'options': ['*cs-folder-path', 'end-point'],
                'help': 'marvincli sync_and_install cs-folder-path = <cloudstack code root dir EX: /root/cs-4.5/cloudstack/>'
                'end-point = <CS installed host ip EX: localhost>',
                'desc': 'generates cloudstackAPI directory with CS apis information from cloudstack end-point (and) installs new marvin.'
                'If end-point information is not provided, localhost is considered as default'
            },
        'build_and_install':
            {
                'summary': 'for building and installing marvin using spec file',
                'options': ['*cs-folder-path', 'api-sync-file'],
                'help': 'marvincli build_and_install cs-folder-path = <cloudstack code root dir EX: /root/cs-4.5/cloudstack/>'
                'api-sync-file = <api spec file generated by cs EX: /etc/cloud/cli/commands.xml>',
                'desc': 'generates cloudstackAPI directory with CS apis information from cloudstack api-spec-file (and) installs new marvin.'
                'If api spec file information is not provided, /etc/cloud/cli/commands.xml is considered as default'
            },
        'version':
            {
                'summary': 'for printing marvincli version',
                'options': ['-v (or) --version'],
                'help': 'marvincli -v (or) marvincli --version',
                'desc': 'prints the version of marvincli'
            }
    })


class MarvinCliHelp(object):

    @classmethod
    def print_cmds_help(cls):
        msg = ''
        for cmd_name, cmd_txt in list(MarvinCliCommands.cmds_info.items()):
            msg = msg + \
                '\n----------------------------------------------------\n'
            cmd_info = ShellColor.BOLD + ShellColor.RED + \
                'cmd_name:%s' % str(cmd_name) + ShellColor.END
            for key, value in cmd_txt.items():
                cmd_info = cmd_info + '\n' + \
                    str(key) + ' : ' + str(value).strip('\n')
            msg = msg + cmd_info
        # return ShellColor.BOLD + ShellColor.RED + msg + ShellColor.END
        return msg

    @classmethod
    def print_msg(cls, msg):
        if msg:
            return ShellColor.BOLD + ShellColor.RED + msg + ShellColor.END


class ShellColor(object):
    BLUE = '\033[94m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    END = '\033[0m'
    ITALICS = '\x1B[3m'

#VERSION = "4.7.0"


class MarvinCli(cmd.Cmd, object):

    def __init__(self):
        self.__configFile = None
        self.__deployFlag = False
        self.__zone = None
        self.__hypervisorType = None
        self.__tcPath = None
        self.__testClient = None
        self.__tcRunLogger = None
        self.__parsedConfig = None
        self.__resultStream = None
        self.__logFolderPath = None
        self.__testRunner = None
        self.__requiredHw = False
        self.__csFolder = "."
        cmd.Cmd.__init__(self)

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def parse_input_deploy(self, inputs=None):
        '''
        Parses,reads the options and verifies for the config file
        '''
        if inputs:
            out_dict = {}
            args = inputs.strip().split(' ')
            for item in args:
                (key, value) = item.split('=')
                out_dict[key] = value
            self.__configFile = out_dict.get('config-file', '')
            if not self.__configFile:
                return FAILED
            print("\n==== Parsing Input Options Successful ====")
            return SUCCESS
        return FAILED

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def parse_input_runtcs(self, inputs):
        '''
        Parses,reads the options and verifies for the config file
        '''
        if inputs:
            out_dict = {}
            args = inputs.strip().split(' ')
            for item in args:
                (key, value) = item.split('=')
                out_dict[key] = value
            self.__configFile = out_dict.get('config-file', None)
            self.__zone = out_dict.get("zone", None)
            self.__hypervisorType = out_dict.get("hyp-type", None)
            self.__tcPath = out_dict.get("tc-path",)
            self.__requiredHw = out_dict.get("required-hardware")
            if not all([self.__tcPath, self.__configFile]):
                return FAILED
            print("\n==== Parsing Input Options Successful ====")
            return SUCCESS
        return FAILED

    @VerifyAndExit("Marvin initialization failed, please check")
    def start_marvin(self):
        '''
        Initialize the Marvin
        '''
        try:
            obj_marvininit = MarvinInit(config_file=self.__configFile,
                                        deploy_dc_flag=self.__deployFlag,
                                        zone=self.__zone,
                                        hypervisor_type=self.__hypervisorType,
                                        user_logfolder_path=None)
            if obj_marvininit and obj_marvininit.init() == SUCCESS:
                self.__testClient = obj_marvininit.getTestClient()
                self.__tcRunLogger = obj_marvininit.getLogger()
                self.__parsedConfig = obj_marvininit.getParsedConfig()
                self.__resultStream = obj_marvininit.getResultFile()
                self.__logFolderPath = obj_marvininit.getLogFolderPath()
                return SUCCESS
            return FAILED
        except Exception as e:
            print("====Exception Occurred under start_marvin: %s ====" % \
                  GetDetailExceptionInfo(e))
            return FAILED

    def run_test_suites(self):
        print("\n==== Started Running Test Cases ====")
        xunit_out_path = "/tmp/marvin_xunit_out" + \
                         str(random.randrange(1, 10000)) + ".xml"
        marvin_tc_run_cmd = "nosetests-2.7 -s --with-marvin --marvin-config=%s --with-xunit --xunit-file=%s  %s  -a tags=advanced, required_hardware=%s  --zone=%s --hypervisor=%s"
        if os.path.isfile(self.__tcPath):
            marvin_tc_run_cmd = marvin_tc_run_cmd % (self.__configFile,
                                                     xunit_out_path, self.__requiredHw, self.__zone, self.__hypervisorType)
        if os.path.isdir(self.__tcPath):
            marvin_tc_run_cmd = marvin_tc_run_cmd % (self.__configFile,
                                                     xunit_out_path, self.__requiredHw, self.__zone, self.__hypervisorType)
        os.system(marvin_tc_run_cmd)
        '''
        engine = TestCaseExecuteEngine(self.__testClient,
                                       self.__parsedConfig,
                                       tc_logger=self.__tcRunLogger)
        if os.path.isfile(self.__tcPath):
            engine.loadTestsFromFile(self.__tcPath)
        elif os.path.isdir(self.__tcPath):
            engine.loadTestsFromDir(self.__tcPath)
        engine.run()
        '''
        print("\n==== Running Test Cases Successful ====")

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_deploydc(self, args):
        try:
            self.__deployFlag = True
            self.parse_input_deploy(inputs=args)
            self.start_marvin()
            return SUCCESS
        except Exception as e:
            print("==== deploy cmd failed :%s ==== " % str(e))
            return FAILED

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_deploydc_and_runtest(self, args):
        try:
            self.do_deploy(inputs=args)
            self.parse_input_runtcs()
            self.run_test_suites()
            return SUCCESS
        except Exception as e:
            print("==== deploydc cmd failed:%s ==== " % str(e))
            return FAILED

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_generateapis_from_apispecfile(self, args):
        api_spec_file = "/etc/cloud/cli/commands.xml"
        cs_api_folder = "."
        if args:
            inp = args.strip().split(' ')
            for items in inp:
                (key, value) = items.split('=')
                if key.lower() == 'api-spec-file':
                    if os.path.exists(value):
                        api_spec_file = value
                    elif not os.path.exists(api_spec_file):
                        print("=== Mentioned api spec file :%s does not exists ===" % str(api_spec_file))
                        sys.exit(1)
                    if key.lower() == 'cs-folder-path':
                        cs_api_folder = self.create_marvin_api_folder(value)
        cg = CodeGenerator(cs_api_folder)
        if api_spec_file:
            try:
                cg.generateCodeFromXML(api_spec_file)
                return SUCCESS
            except Exception as e:
                print("==== Generating apis from api spec file failed: %s ====" % str(e.message()))
                return FAILED
        return FAILED

    def create_marvin_api_folder(self, cs_folder_path='.'):
        cs_api_folder = cs_folder_path + "/tools/marvin/marvin/cloudstackAPI"
        if os.path.exists(cs_api_folder):
            os.rmdir(cs_api_folder)
        else:
            os.makedirs(cs_api_folder)
        return cs_api_folder

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_generateapis_from_endpoint(self, args):
        endpoint_url = 'http://%s:8096/client/api?command=listApis&\
response=json'
        cs_api_folder = "."
        if args:
            inp = args.strip().split(' ')
            for items in inp:
                (key, value) = items.split('=')
                if key.lower() == 'endpoint':
                    cs_end_point = value
                if key.lower() == 'cs-folder-path':
                    cs_api_folder = self.create_marvin_api_folder(value)
        cg = CodeGenerator(cs_api_folder)
        if cs_end_point:
            try:
                endpoint_url = endpoint_url % str(cs_end_point)
                cg.generateCodeFromJSON(endpoint_url)
                return SUCCESS
            except Exception as e:
                print("==== Generating apis from end point failed: %s ====" % str(e.message()))
                return FAILED
        return FAILED

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_runtest(self, args):
        try:
            self.parse_input_runtcs(args)
            self.start_marvin()
            self.run_test_suites()
            return SUCCESS
        except Exception as e:
            print("==== run test failed: %s ====" % str(e.message()))
            return FAILED

    def install_marvin(self):
        if self.__csFolder:
            marvin_setup_file_path = self.__csFolder + "/tools/marvin/setup.py"
        try:
            os.system("python %s install" % str(marvin_setup_file_path))
            print("==== Marvin Installed Successfully ====")
        except Exception as e:
            print("==== Marvin Installation Failed ====")

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_build_and_install(self, args):
        try:
            self.do_generateapis_from_apispecfile(args)
            self.install_marvin()
            return SUCCESS
        except Exception as e:
            print("==== build from end point and install marvin failed: %s ====" % str(e))
            return FAILED

    @VerifyAndExit(
        "cmd failed, may be invalid input options, please check help")
    def do_sync_and_install(self, args):
        try:
            self.do_generateapis_from_endpoint(args)
            self.install_marvin()
            return SUCCESS
        except Exception as e:
            print("==== sync from spec file and install  marvin failed: %s ====" % str(e))
            return FAILED


class MarvinCliParser(OptionParser):

    def format_help(self, formatter=None):
        if formatter is None:
            formatter = self.formatter
        result = []
        if self.usage:
            result.append(MarvinCliHelp.print_msg("\nUsage: marvincli [cmd] [options]. See, the below cmds for more information."
                                                  "(*) signifies mandatory fields \n\n"))
        self.description = MarvinCliHelp.print_cmds_help()
        if self.description:
            result.append(self.format_description(formatter) + "\n")
        return "".join(result)


def main():
    parser = MarvinCliParser()
    parser.add_option("-v", "--version",
                      action="store_true", dest="version", default=False,
                      help="prints marvin cli version information")
    (options, args) = parser.parse_args()
    if options.version:
        MarvinCliHelp.help_printversion()
        sys.exit(0)
    if len(sys.argv) > 1:
        if sys.argv[1].lower() not in list(MarvinCliCommands.cmds_info.keys()):
            print("\n==== Invalid Command ====")
            sys.exit(1)
        args = ' '.join(args)
        if '-h' in args or '--help' in args:
            print(MarvinCliCommands.cmds_info[sys.argv[0]])
        else:
            MarvinCli().onecmd(args)
    sys.exit(0)

if __name__ == "__main__":
    main()
