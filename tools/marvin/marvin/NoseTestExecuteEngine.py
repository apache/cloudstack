from functools import partial
import unittest
import nose
import nose.config
import nose.core
import os
import sys
import logging
from cloudstackTestCase import cloudstackTestCase
from nose.suite import ContextSuite, ContextSuiteFactory

class CloudStackTestSelector(nose.selector.Selector):
    """
    custom test selector for cloudstackTestCase
    """
    
    def wantClass(self, cls):
        if issubclass(cls, cloudstackTestCase):
            return nose.selector.Selector.wantClass(self, cls)

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)

class NoseTestExecuteEngine(object):
    def __init__(self, testclient=None, workingdir=None, clientLog=None, resultLog=None):
        self.testclient = testclient
        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")
        self.suite = []
        realPath = os.path.abspath(workingdir)

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
            self.testResultLogFile = sys.stdout
 
        if workingdir is not None and os.path.exists(realPath + '/' + '__init__.py'):
            self.loader = nose.loader.TestLoader()
            [self.suite.append(test) for test in self.loader.discover(workingdir, "test*.py")]
            for test in self.suite:
                self.injectTestCase(test)
            print self.suite[0].countTestCases()
        else:
            print "Single module test runs unsupported using Nose"
            raise

        self.runner = nose.core.TextTestRunner(stream=self.testResultLogFile, descriptions=1, verbosity=2, config=None)
            
    def runTests(self):
        #testProgram = nose.core.TestProgram(argv=["--process-timeout=3600"], testRunner = self.runner, testLoader = self.loader)
        testProgram = nose.core.TestProgram(argv=["--process-timeout=3600"], testRunner = self.runner, suite = self.suite)
        testProgram.runTests()
        
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
                setattr(test, "debug", partial(testCaseLogger, logger=testcaselogger))
                setattr(test.__class__, "clstestclient", self.testclient)
                if hasattr(test, "UserName"):
                    self.testclient.createNewApiClient(test.UserName, test.DomainName, test.AcctType)
