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
from sys import stdout, exit
import logging
import time
import os
import nose.core
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.marvinInit import MarvinInit
from nose.plugins.base import Plugin
from marvin.codes import (SUCCESS,
                          FAILED,
                          EXCEPTION)
from marvin.cloudstackException import GetDetailExceptionInfo


class MarvinPlugin(Plugin):

    """
    Custom plugin for the cloudstackTestCases to be run using nose
    """

    name = "marvin"

    def __init__(self):
        self.__identifier = None
        self.__testClient = None
        self.__parsedConfig = None
        '''
        Contains Config File
        '''
        self.__configFile = None
        '''
        Signifies the Zone against which all tests will be Run
        '''
        self.__zoneForTests = None
        '''
        Signifies the flag whether to deploy the New DC or Not
        '''
        self.__deployDcFlag = None
        self.conf = None
        self.__resultStream = stdout
        self.__testRunner = None
        self.__testResult = SUCCESS
        self.__startTime = None
        self.__testName = None
        self.__tcRunLogger = None
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
        self.__configFile = options.configFile
        self.__deployDcFlag = options.deployDc
        self.__zoneForTests = options.zone
        self.conf = conf
        if self.startMarvin() == FAILED:
            print "\nExiting Marvin. Please Check"
            exit(1)

    def options(self, parser, env):
        """
        Register command line options
        """
        parser.add_option("--marvin-config", action="store",
                          default=env.get('MARVIN_CONFIG',
                                          './datacenter.cfg'),
                          dest="configFile",
                          help="Marvin's configuration file is required."
                               "The config file containing the datacenter and "
                               "other management server "
                               "information is specified")
        parser.add_option("--deploy", action="store_true",
                          default=False,
                          dest="deployDc",
                          help="Deploys the DC with Given Configuration."
                               "Requires only when DC needs to be deployed")
        parser.add_option("--zone", action="store",
                          default=None,
                          dest="zone",
                          help="Runs all tests against this specified zone")
        Plugin.options(self, parser, env)

    def wantClass(self, cls):
        if cls.__name__ == 'cloudstackTestCase':
            return False
        if issubclass(cls, cloudstackTestCase):
            return True
        return None

    def __checkImport(self, filename):
        '''
        @Name : __checkImport
        @Desc : Verifies to run the available test module for any Import
                Errors before running and check
                whether if it is importable.
                This will check for test modules which has some issues to be
                getting imported.
                Returns False or True based upon the result.
        '''
        try:
            if os.path.isfile(filename):
                ret = os.path.splitext(filename)
                if ret[1] == ".py":
                    os.system("python " + filename)
                    return True
            return False
        except ImportError as e:
            print "FileName :%s : Error : %s" % \
                  (filename, GetDetailExceptionInfo(e))
            return False

    def wantFile(self, filename):
        '''
        @Desc : Only python files will be used as test modules
        '''
        return self.__checkImport(filename)

    def loadTestsFromTestCase(self, cls):
        if cls.__name__ != 'cloudstackTestCase':
            self.__identifier = cls.__name__
            self._injectClients(cls)

    def beforeTest(self, test):
        self.__testName = test.__str__().split()[0]
        self.__testClient.identifier = '-'.\
            join([self.__identifier, self.__testName])
        if self.__tcRunLogger:
            self.__tcRunLogger.name = test.__str__()

    def startTest(self, test):
        """
        Currently used to record start time for tests
        Dump Start Msg of TestCase to Log
        """
        if self.__tcRunLogger:
            self.__tcRunLogger.debug("\n\n::::::::::::STARTED : TC: " +
                                     str(self.__testName) + " :::::::::::")
        self.__startTime = time.time()

    def printMsg(self, status, tname, err):
        if self.__tcRunLogger:
            self.__tcRunLogger.\
                fatal("%s: %s: %s" % (status,
                                      tname,
                                      GetDetailExceptionInfo(err)))
        write_str = "=== TestName: %s | Status : %s ===\n" % (tname, status)
        self.__resultStream.write(write_str)
        print write_str

    def addSuccess(test):
        '''
        Adds the Success Messages to logs
        '''
        printMsg(SUCCESS, self.__testName, "Test Case Passed")
        self.__testresult = SUCCESS

    def handleError(self, test, err):
        '''
        Adds Exception throwing test cases and information to log.
        '''
        self.printMsg(EXCEPTION, self.__testName, GetDetailExceptionInfo(err))
        self.__testResult = EXCEPTION

    def prepareTestRunner(self, runner):
        if self.__testRunner:
            return self.__testRunner

    def handleFailure(self, test, err):
        '''
        Adds Failing test cases and information to log.
        '''
        self.printMsg(FAILED, self.__testName, GetDetailExceptionInfo(err))
        self.__testResult = FAILED

    def __getModName(self, inp, type='file'):
        '''
        @Desc : Returns the module name from the path
        @Output: trimmed down module name, used for logging
        @Input: type:Whether the type is file or dir
                inp:input element
        '''
        if type == 'file':
            temp = os.path.splitext(inp)[0]
            return os.path.split(temp)[-1]
        if type == 'dir':
            return os.path.split(inp)[-1]

    def __runSuite(self, test_suite=None):
        try:
            if test_suite:
                if self.wantFile(test_suite):
                    test_mod_name = self.__getModName(test_suite)
                    temp_obj = MarvinInit(self.__configFile,
                                          None, test_mod_name,
                                          self.__zoneForTests)
                    if temp_obj and temp_obj.init() == SUCCESS:
                        print "\nMarvin Initialization Successful." \
                              "Test Suite:%s" % str(test_suite)
                        self.__testClient = temp_obj.getTestClient()
                        self.__tcRunLogger = temp_obj.getLogger()
                        self.__parsedConfig = temp_obj.getParsedConfig()
                        self.__resultStream = temp_obj.getResultFile()
                        self.__testRunner = nose.core.\
                            TextTestRunner(stream=self.__resultStream,
                                           descriptions=True,
                                           verbosity=2)
                        return SUCCESS
            return FAILED
        except Exception as e:
            print "\n Exception Occurred when running suite :%s Error : %s" \
                % (test_suite, GetDetailExceptionInfo(e))
            return FAILED

    def __runSuites(self, suites):
        for suite in suites:
            if os.path.isdir(suite):
                return self.__runSuites(suite)
            self.__runSuite(suite)

    def startMarvin(self):
        '''
        @Name : startMarvin
        @Desc : Initializes the Marvin
                creates the test Client
                creates the runlogger for logging
                Parses the config and creates a parsedconfig
                Creates a debugstream for tc debug log
        '''
        try:
            if self.__deployDcFlag:
                print "\n***Step1 :Deploy Flag is Enabled, will deployDC****"
                obj_marvininit = MarvinInit(self.__configFile,
                                            self.__deployDcFlag,
                                            "DeployDc",
                                            self.__zoneForTests)
                if not obj_marvininit or obj_marvininit.init() != SUCCESS:
                    return FAILED
            print "\n====Now Start Running Test Suites===="
            if len(self.conf.testNames) == 0:
                if self.conf.workingDir == '':
                    print "\n==== " \
                          "No Test Suites are provided, please check ===="
                    return FAILED
            else:
                if self.conf.workingDir != '':
                    test_names = self.conf.workingDir
                else:
                    test_names = self.conf.testNames
            for suites in test_names:
                if os.path.isdir(suites):
                    self.__runSuites(suites)
                if os.path.isfile(suites):
                    self.__runSuite(suites)
            return SUCCESS
        except Exception as e:
            print "Exception Occurred under startMarvin: %s" % \
                  GetDetailExceptionInfo(e)
            return FAILED

    def stopTest(self, test):
        """
        Currently used to record end time for tests
        """
        endTime = time.time()
        if self.__startTime:
            totTime = int(endTime - self.__startTime)
            if self.__tcRunLogger:
                self.__tcRunLogger.\
                    debug("TestCaseName: %s; "
                          "Time Taken: %s Seconds; StartTime: %s; "
                          "EndTime: %s; Result: %s" %
                          (self.__testName, str(totTime),
                           str(time.ctime(self.__startTime)),
                           str(time.ctime(endTime)),
                           self.__testResult))

    def _injectClients(self, test):
        setattr(test, "debug", self.__tcRunLogger.debug)
        setattr(test, "info", self.__tcRunLogger.info)
        setattr(test, "warn", self.__tcRunLogger.warning)
        setattr(test, "error", self.__tcRunLogger.error)
        setattr(test, "testClient", self.__testClient)
        setattr(test, "config", self.__parsedConfig)
        if self.__testClient.identifier is None:
            self.__testClient.identifier = self.__identifier
        setattr(test, "clstestclient", self.__testClient)
        if hasattr(test, "user"):
            # when the class-level attr applied. all test runs as 'user'
            self.__testClient.getUserApiClient(test.UserName,
                                               test.DomainName,
                                               test.AcctType)
