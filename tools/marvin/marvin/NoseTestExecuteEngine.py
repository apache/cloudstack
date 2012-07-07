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

from functools import partial
import unittest
import nose
import nose.core
import os
import sys
import logging
import xmlrunner
from cloudstackTestCase import cloudstackTestCase

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)
        
class NoseCloudStackTestLoader(nose.loader.TestLoader):
    """
    Custom test loader for the cloudstackTestCase to be loaded into nose
    """
    
    def loadTestsFromTestCase(self, testCaseClass):
        if issubclass(testCaseClass, cloudstackTestCase):
            testCaseNames = self.getTestCaseNames(testCaseClass)
            tests = []
            for testCaseName in testCaseNames:
                testCase = testCaseClass(testCaseName)
                self._injectClients(testCase)
                tests.append(testCase)
            return self.suiteClass(tests)
        else:
            return super(NoseCloudStackTestLoader, self).loadTestsFromTestCase(testCaseClass)
    
    def loadTestsFromName(self, name, module=None, discovered=False):
        return nose.loader.TestLoader.loadTestsFromName(self, name, module=module, discovered=discovered)
    
    def loadTestsFromNames(self, names, module=None):
        return nose.loader.TestLoader.loadTestsFromNames(self, names, module=module)

    def setClient(self, client):
        self.testclient = client

    def setClientLog(self, clientlog):
        self.log = clientlog

    def _injectClients(self, test):
        testcaselogger = logging.getLogger("testclient.testcase.%s"%test.__class__.__name__)
        fh = logging.FileHandler(self.log) 
        fh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
        testcaselogger.addHandler(fh)
        testcaselogger.setLevel(logging.DEBUG)
        
        setattr(test, "testClient", self.testclient)
        setattr(test, "debug", partial(testCaseLogger, logger=testcaselogger))
        setattr(test.__class__, "clstestclient", self.testclient)
        if hasattr(test, "UserName"):
            self.testclient.createNewApiClient(test.UserName, test.DomainName, test.AcctType)           

class NoseTestExecuteEngine(object):
    """
    Runs the CloudStack tests using nose as the execution engine
    """
    
    def __init__(self, testclient=None, workingdir=None, filename=None, clientLog=None, resultLog=None, format="text"):
        self.testclient = testclient
        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")
        self.suite = []

        if clientLog is not None:
            self.logfile = clientLog
            self.logger = logging.getLogger("NoseTestExecuteEngine")
            fh = logging.FileHandler(self.logfile) 
            fh.setFormatter(self.logformat)
            self.logger.addHandler(fh)
            self.logger.setLevel(logging.DEBUG)
        if resultLog is not None:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(self.logformat)
            self.logger.addHandler(ch)
            fp = open(resultLog, "w")
            self.testResultLogFile = fp
        else:
            self.testResultLogFile = sys.stderr
 
        if workingdir is not None:
            self.loader = NoseCloudStackTestLoader()
            self.loader.setClient(self.testclient)
            self.loader.setClientLog(self.logfile)
            self.suite = self.loader.loadTestsFromDir(workingdir)
        elif filename is not None:
            self.loader = NoseCloudStackTestLoader()
            self.loader.setClient(self.testclient)
            self.loader.setClientLog(self.logfile)
            self.suite = self.loader.loadTestsFromFile(filename)
        else:
            raise EnvironmentError("Need to give either a test directory or a test file")
        
        if format == "text":
            self.runner = nose.core.TextTestRunner(stream=self.testResultLogFile, descriptions=1, verbosity=2, config=None)
        else:
            self.runner = xmlrunner.XMLTestRunner(output='xml-reports', verbose=True)
            
    def runTests(self):
         #nose.core.TestProgram(argv=["--process-timeout=3600"], testRunner=self.runner, testLoader=self.loader)
         nose.core.TestProgram(argv=["--process-timeout=3600"], \
                               testRunner=self.runner, suite=self.suite)
