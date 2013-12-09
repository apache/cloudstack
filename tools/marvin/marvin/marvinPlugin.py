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
                          EXCEPTION,
                          UNKNOWN_ERROR
                          )
import traceback
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
        self.configFile = None
        self.loadFlag = None
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
        self.configFile = options.config_file
        self.loadFlag = options.load
        self.conf = conf
        '''
        Initializes the marvin with required settings
        '''
        self.startMarvin()

    def options(self, parser, env):
        """
        Register command line options
        """
        parser.add_option("--marvin-config", action="store",
                          default=env.get('MARVIN_CONFIG', './datacenter.cfg'),
                          dest="config_file",
                          help="Marvin's configuration file where the " +
                               "datacenter information is specified " +
                               "[MARVIN_CONFIG]")
        parser.add_option("--load", action="store_true", default=False,
                          dest="load",
                          help="Only load the deployment configuration given")
        Plugin.options(self, parser, env)

    def wantClass(self, cls):
        if cls.__name__ == 'cloudstackTestCase':
            return False
        if issubclass(cls, cloudstackTestCase):
            return True
        return None

    def wantFile(self, filename):
        '''
        Only python files will be used as test modules
        '''
        parts = filename.split(os.path.sep)
        base, ext = os.path.splitext(parts[-1])
        if ext == '.py':
            return True
        else:
            return False

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
        self.tcRunLogger.debug("::::::::::::STARTED : TC: " +
                               str(self.testName) + " :::::::::::")
        self.startTime = time.time()

    def getErrorInfo(self, err):
        '''
        Extracts and returns the sanitized error message
        '''
        if err is not None:
            return str(traceback.format_exc())
        else:
            return UNKNOWN_ERROR

    def handleError(self, test, err):
        '''
        Adds Exception throwing test cases and information to log.
        '''
        err_msg = self.getErrorInfo(err)
        self.tcRunLogger.fatal("%s: %s: %s" %
                               (EXCEPTION, self.testName, err_msg))
        self.testResult = EXCEPTION

    def handleFailure(self, test, err):
        '''
        Adds Failing test cases and information to log.
        '''
        err_msg = self.getErrorInfo(err)
        self.tcRunLogger.fatal("%s: %s: %s" %
                               (FAILED, self.testName, err_msg))
        self.testResult = FAILED

    def startMarvin(self):
        '''
        Initializes the Marvin
        creates the test Client
        creates the runlogger for logging
        Parses the config and creates a parsedconfig
        Creates a debugstream for tc debug log
        '''
        try:
            obj_marvininit = MarvinInit(self.configFile, self.loadFlag)
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
            print "Exception Occurred under startMarvin: %s" % str(e)
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
            self.testClient.createUserApiClient(test.UserName, test.DomainName,
                                                test.AcctType)
