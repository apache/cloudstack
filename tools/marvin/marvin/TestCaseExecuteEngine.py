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

import unittest
import os
import sys
import logging
from functools import partial

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)

class TestCaseExecuteEngine(object):
    def __init__(self, testclient, config, testcaseLogFile=None, testResultLogFile=None):
        """
        Initialize the testcase execution engine, just the basics here
        @var testcaseLogFile: client log file
        @var testResultLogFile: summary report file  
        """
        self.testclient = testclient
        self.config = config
        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")
        self.loader = unittest.loader.TestLoader()
        self.suite = None

        if testcaseLogFile is not None:
            self.logfile = testcaseLogFile
            self.logger = logging.getLogger("TestCaseExecuteEngine")
            fh = logging.FileHandler(self.logfile) 
            fh.setFormatter(self.logformat)
            self.logger.addHandler(fh)
            self.logger.setLevel(logging.DEBUG)
        if testResultLogFile is not None:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(self.logformat)
            self.logger.addHandler(ch)
            fp = open(testResultLogFile, "w")
            self.testResultLogFile = fp
        else:
            self.testResultLogFile = sys.stdout

    def loadTestsFromDir(self, testDirectory):
        """ Load the test suites from a package with multiple test files """
        self.suite = self.loader.discover(testDirectory)
        self.injectTestCase(self.suite)
        
    def loadTestsFromFile(self, file_name):
        """ Load the tests from a single script/module """
        if os.path.isfile(file_name):
            self.suite = self.loader.discover(os.path.dirname(file_name), os.path.basename(file_name))
            self.injectTestCase(self.suite)
        
    def injectTestCase(self, testSuites):
        for test in testSuites:
            if isinstance(test, unittest.BaseTestSuite):
                self.injectTestCase(test)
            else:
                #logger bears the name of the test class
                testcaselogger = logging.getLogger("testclient.testcase.%s"%test.__class__.__name__)
                fh = logging.FileHandler(self.logfile) 
                fh.setFormatter(self.logformat)
                testcaselogger.addHandler(fh)
                testcaselogger.setLevel(logging.DEBUG)
                
                #inject testclient and logger into each unittest 
                setattr(test, "testClient", self.testclient)
                setattr(test, "config", self.config)
                setattr(test, "debug", partial(testCaseLogger, logger=testcaselogger))
                setattr(test.__class__, "clstestclient", self.testclient)
                if hasattr(test, "user"): #attribute when test is entirely executed as user
                    self.testclient.createUserApiClient(test.UserName, test.DomainName, test.AcctType)

    def run(self):
        if self.suite:
            unittest.TextTestRunner(stream=self.testResultLogFile, verbosity=2).run(self.suite)
