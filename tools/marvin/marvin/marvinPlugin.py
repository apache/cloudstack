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

import marvin
import sys
import logging
import nose.core
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.marvinInit import MarvinInit
from nose.plugins.base import Plugin
from marvin.codes import (SUCCESS,
                          FAILED,
                          EXCEPTION)
from marvin.cloudstackException import GetDetailExceptionInfo
import time
import os


class MarvinPlugin(Plugin):
    """
    Custom plugin for the cloudstackTestCases to be run using nose
    """

    name = "marvin"

    def __init__(self):
        self.identifier = None
        self.testClient = None
        self.parsedConfig = None
        '''
        Contains Config File
        '''
        self.__configFile = None
        '''
        Signifies the flag whether to load new API Information
        '''
        self.__loadNewApiFlag = None
        '''
        Signifies the Zone against which all tests will be Run
        '''
        self.__zoneForTests = None
        '''
        Signifies the flag whether to deploy the New DC or Not
        '''
        self.__deployDcFlag
        self.conf = None
        self.debugStream = sys.stdout
        self.testRunner = None
        self.testResult = SUCCESS
        self.startTime = None
        self.testName = None
        self.tcRunLogger = None
        Plugin.__init__(self)

    def configure(self, options, conf):
        """enable the marvin plugin when the --with-marvin directive is given
        to nose. The enableOpt value is set from the command line directive and
        self.enabled (True|False) determines whether marvin's tests will run.
        By default non-default plugins like marvin will be disabled
        """
        if hasattr(options, self.enableOpt):
            if not getattr(options, self.enableOpt):
                self.enabled = False
                return
            else:
                self.enabled = True

        self.__configFile = options.config_file
        self.__loadNewApiFlag = options.loadNewApiFlag
        self.__deployDcFlag = options.deployDc
        self.__zoneForTests = options.zone
        self.conf = conf

    def options(self, parser, env):
        """
        Register command line options
        """
        parser.add_option("--marvin-config", action="store",
                          default=env.get('MARVIN_CONFIG',
                                          './datacenter.cfg'),
                          dest="config_file",
                          help="Marvin's configuration file is required."
                               "The config file containing the datacenter and "
                               "other management server "
                               "information is specified")
        parser.add_option("--deploy-dc", action="store_true",
                          default=False,
                          dest="deployDc",
                          help="Deploys the DC with Given Configuration."
                               "Requires only when DC needs to be deployed")
        parser.add_option("--zone", action="zone_tests",
                          default=None,
                          dest="zone",
                          help="Runs all tests against this specified zone")
        parser.add_option("--load-new-apis", action="store_true",
                          default=False,
                          dest="loadNewApiFlag",
                          help="Loads the New Apis with Given Api Xml File."
                               "Creates the new Api's from commands.xml File")
        '''
        Check if the configuration file is not valid,print and exit
        '''
        (options, args) = parser.parse_args()
        if options.config_file is None:
            parser.print_usage()
            sys.exit(1)
        Plugin.options(self, parser, env)

    def wantClass(self, cls):
        if cls.__name__ == 'cloudstackTestCase':
            return False
        if issubclass(cls, cloudstackTestCase):
            return True
        return None

    def prepareTest(self, test):
        '''
        @Desc : Initializes the marvin with required settings
        '''
        test_module_name = test.__str__()
        if self.startMarvin(test_module_name) == FAILED:
            print "Starting Marvin FAILED. Please Check Config and " \
                  "Arguments Supplied"

    def __checkImport(self, filename):
        '''
        @Desc : Verifies to Import the test Module before running and check
                whether if it is importable.
                This will check for test modules which has some issues to be
                getting imported.
                Returns False or True based upon the result.
        '''
        try:
            __import__(filename)
            return True
        except ImportError, e:
            self.tcRunLogger.exception("Module : %s Import "
                                       "Failed Reason :%s"
                                       % (filename, GetDetailExceptionInfo(e)))
            return False

    def wantFile(self, filename):
        '''
        @Desc : Only python files will be used as test modules
        '''
        if filename is None or filename == '':
            return False
        parts = filename.split(os.path.sep)
        base, ext = os.path.splitext(parts[-1])
        if ext != '.py':
            return False
        else:
            return self.__checkImport(filename)

    def loadTestsFromTestCase(self, cls):
        if cls.__name__ != 'cloudstackTestCase':
            self.identifier = cls.__name__
            self._injectClients(cls)

    def beforeTest(self, test):
        self.testName = test.__str__().split()[0]
        self.testClient.identifier = '-'.join([self.identifier, self.testName])
        self.tcRunLogger.name = test.__str__()

    def prepareTestRunner(self, runner):
        return self.testRunner

    def startTest(self, test):
        """
        Currently used to record start time for tests
        Dump Start Msg of TestCase to Log
        """
        self.tcRunLogger.debug("\n\n::::::::::::STARTED : TC: " +
                               str(self.testName) + " :::::::::::")
        self.startTime = time.time()

    def handleError(self, test, err):
        '''
        Adds Exception throwing test cases and information to log.
        '''
        err_msg = GetDetailExceptionInfo(err)
        self.tcRunLogger.fatal("%s: %s: %s" %
                               (EXCEPTION, self.testName, err_msg))
        self.testResult = EXCEPTION

    def handleFailure(self, test, err):
        '''
        Adds Failing test cases and information to log.
        '''
        err_msg = GetDetailExceptionInfo(err)
        self.tcRunLogger.fatal("%s: %s: %s" %
                               (FAILED, self.testName, err_msg))
        self.testResult = FAILED

    def startMarvin(self, test_module_name):
        '''
        Initializes the Marvin
        creates the test Client
        creates the runlogger for logging
        Parses the config and creates a parsedconfig
        Creates a debugstream for tc debug log
        '''
        try:
            obj_marvininit = MarvinInit(self.__configFile,
                                        self.__loadNewApiFlag,
                                        self.__deployDcFlag,
                                        test_module_name,
                                        self.__zoneForoTests)
            if obj_marvininit.init() == SUCCESS:
                self.testClient = obj_marvininit.getTestClient()
                self.tcRunLogger = obj_marvininit.getLogger()
                self.parsedConfig = obj_marvininit.getParsedConfig()
                self.debugStream = obj_marvininit.getDebugFile()
                self.testRunner = nose.core.TextTestRunner(stream=
                                                           self.debugStream,
                                                           descriptions=True,
                                                           verbosity=2,
                                                           config=self.conf)
                return SUCCESS
            else:
                return FAILED
        except Exception, e:
            print "Exception Occurred under startMarvin: %s" % \
                  GetDetailExceptionInfo(e)
            return FAILED

    def stopTest(self, test):
        """
        Currently used to record end time for tests
        """
        endTime = time.time()
        if self.startTime is not None:
            totTime = int(endTime - self.startTime)
            self.tcRunLogger.debug("TestCaseName: %s; Time Taken: "
                                   "%s Seconds; "
                                   "StartTime: %s; EndTime: %s; Result: %s"
                                   % (self.testName, str(totTime),
                                      str(time.ctime(self.startTime)),
                                      str(time.ctime(endTime)),
                                      self.testResult))

    def _injectClients(self, test):
        setattr(test, "debug", self.tcRunLogger.debug)
        setattr(test, "info", self.tcRunLogger.info)
        setattr(test, "warn", self.tcRunLogger.warning)
        setattr(test, "error", self.tcRunLogger.error)
        setattr(test, "testClient", self.testClient)
        setattr(test, "config", self.parsedConfig)
        if self.testClient.identifier is None:
            self.testClient.identifier = self.identifier
        setattr(test, "clstestclient", self.testClient)
        if hasattr(test, "user"):
            # when the class-level attr applied. all test runs as 'user'
            self.testClient.getUserApiClient(test.UserName,
                                             test.DomainName,
                                             test.AcctType)
